import { useState, useCallback } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Calendar, Clock, X } from 'lucide-react'
import { Card, Button, Badge, Modal } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { ReservationSkeleton } from '@/components/ui/Skeleton'
import EmptyState from '@/components/ui/EmptyState'
import PullToRefresh from '@/components/ui/PullToRefresh'
import { reservationApi } from '@/services/api'
import { useAuthStore } from '@/stores/authStore'
import { formatDate, formatTime } from '@/utils/formatters'
import type { Reservation } from '@/types/api'

export default function MyReservations() {
  const { t } = useTranslation()
  const { user, updateUser } = useAuthStore()
  const { showToast } = useToast()
  const queryClient = useQueryClient()

  const [activeTab, setActiveTab] = useState<'upcoming' | 'past'>('upcoming')
  const [cancelingId, setCancelingId] = useState<string | null>(null)

  const { data: reservations, isLoading, refetch } = useQuery({
    queryKey: ['reservations'],
    queryFn: reservationApi.getMyReservations,
  })

  const handleRefresh = useCallback(async () => {
    await refetch()
  }, [refetch])

  const cancelMutation = useMutation({
    mutationFn: reservationApi.cancel,
    onSuccess: (data) => {
      showToast('success', t('myReservations.cancelSuccess'))
      setCancelingId(null)
      // Invalidate all relevant queries
      queryClient.invalidateQueries({ queryKey: ['reservations'] })
      queryClient.invalidateQueries({ queryKey: ['myReservations'] })
      queryClient.invalidateQueries({ queryKey: ['availableSlots'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      // Refund credit
      if (user) {
        updateUser({ ...user, credits: user.credits + data.creditsUsed })
      }
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      showToast('error', error.response?.data?.message || t('myReservations.cancelTooLate'))
      setCancelingId(null)
    },
  })

  const now = new Date()
  const upcoming = reservations?.filter((r) => {
    const reservationDate = new Date(`${r.date}T${r.startTime}`)
    return reservationDate >= now && r.status === 'confirmed'
  }) || []

  const past = reservations?.filter((r) => {
    const reservationDate = new Date(`${r.date}T${r.startTime}`)
    // Only show past confirmed/completed trainings, exclude cancelled
    return reservationDate < now && r.status !== 'cancelled'
  }) || []

  const displayedReservations = activeTab === 'upcoming' ? upcoming : past

  const canCancel = (reservation: Reservation) => {
    const reservationDateTime = new Date(`${reservation.date}T${reservation.startTime}`)
    const hoursUntil = (reservationDateTime.getTime() - now.getTime()) / (1000 * 60 * 60)
    return hoursUntil >= 24 && reservation.status === 'confirmed'
  }

  const handleCancelClick = (id: string) => {
    setCancelingId(id)
  }

  const confirmCancel = () => {
    if (cancelingId) {
      cancelMutation.mutate(cancelingId)
    }
  }

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'confirmed':
        return <Badge variant="success">{t(`reservationStatus.${status}`)}</Badge>
      case 'cancelled':
        return <Badge variant="danger">{t(`reservationStatus.${status}`)}</Badge>
      case 'completed':
        return <Badge variant="info">{t(`reservationStatus.${status}`)}</Badge>
      default:
        return <Badge>{t(`reservationStatus.${status}`)}</Badge>
    }
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
        {t('myReservations.title')}
      </h1>

      {/* Tabs */}
      <div className="flex gap-2 p-1 bg-neutral-100 dark:bg-dark-surface rounded-lg">
        <button
          onClick={() => setActiveTab('upcoming')}
          className={`flex-1 py-2 px-4 rounded-md text-sm font-medium transition-colors ${
            activeTab === 'upcoming'
              ? 'bg-white dark:bg-dark-surfaceHover text-neutral-900 dark:text-white shadow-sm'
              : 'text-neutral-500 dark:text-neutral-400'
          }`}
        >
          {t('myReservations.upcoming')} ({upcoming.length})
        </button>
        <button
          onClick={() => setActiveTab('past')}
          className={`flex-1 py-2 px-4 rounded-md text-sm font-medium transition-colors ${
            activeTab === 'past'
              ? 'bg-white dark:bg-dark-surfaceHover text-neutral-900 dark:text-white shadow-sm'
              : 'text-neutral-500 dark:text-neutral-400'
          }`}
        >
          {t('myReservations.past')} ({past.length})
        </button>
      </div>

      {/* Reservations list */}
      <PullToRefresh onRefresh={handleRefresh} className="min-h-[200px]">
        {isLoading ? (
          <div className="space-y-3">
            {[...Array(3)].map((_, i) => (
              <ReservationSkeleton key={i} />
            ))}
          </div>
        ) : displayedReservations.length > 0 ? (
          <div className="space-y-3">
            {displayedReservations.map((reservation) => (
              <Card key={reservation.id} variant="bordered">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex gap-4">
                    <div className="flex-shrink-0 w-14 h-14 bg-primary-100 dark:bg-primary-900/30 rounded-lg flex flex-col items-center justify-center">
                      <span className="text-xs text-primary-600 dark:text-primary-400 font-medium">
                        {new Date(reservation.date).toLocaleDateString('cs', {
                          weekday: 'short',
                        })}
                      </span>
                      <span className="text-lg font-bold text-primary-700 dark:text-primary-300">
                        {new Date(reservation.date).getDate()}
                      </span>
                    </div>

                    <div>
                      <p className="font-medium text-neutral-900 dark:text-white">
                        {formatDate(reservation.date)}
                      </p>
                      <div className="flex items-center gap-1 text-sm text-neutral-500 dark:text-neutral-400 mt-1">
                        <Clock size={14} />
                        <span>
                          {formatTime(reservation.startTime)} - {formatTime(reservation.endTime)}
                        </span>
                      </div>
                      <div className="mt-2">{getStatusBadge(reservation.status)}</div>
                    </div>
                  </div>

                  {activeTab === 'upcoming' && canCancel(reservation) && (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleCancelClick(reservation.id)}
                      className="text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20"
                    >
                      <X size={18} />
                    </Button>
                  )}
                </div>
              </Card>
            ))}
          </div>
        ) : (
          <EmptyState
            icon={Calendar}
            title={t('myReservations.noReservations')}
            description={activeTab === 'upcoming' ? t('myReservations.noUpcoming') : undefined}
          />
        )}
      </PullToRefresh>

      {/* Cancel confirmation modal */}
      <Modal
        isOpen={!!cancelingId}
        onClose={() => setCancelingId(null)}
        title={t('myReservations.cancel')}
        size="sm"
      >
        <div className="space-y-4">
          <p className="text-neutral-600 dark:text-neutral-300">
            {t('myReservations.cancelConfirm')}
          </p>
          <p className="text-sm text-green-600 dark:text-green-400">
            {t('myReservations.creditsRefunded')}
          </p>
          <div className="flex gap-3">
            <Button
              variant="secondary"
              className="flex-1"
              onClick={() => setCancelingId(null)}
            >
              {t('common.back')}
            </Button>
            <Button
              variant="danger"
              className="flex-1"
              onClick={confirmCancel}
              isLoading={cancelMutation.isPending}
            >
              {t('myReservations.cancel')}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
