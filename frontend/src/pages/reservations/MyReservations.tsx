import { useState, useCallback, useEffect, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Calendar, Clock, Download, Repeat, Star, X } from 'lucide-react'
import { Card, Button, Badge, Modal, Spinner } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { ReservationSkeleton } from '@/components/ui/Skeleton'
import EmptyState from '@/components/ui/EmptyState'
import PullToRefresh from '@/components/ui/PullToRefresh'
import { authApi, feedbackApi, reservationApi } from '@/services/api'
import { useAuthStore } from '@/stores/authStore'
import { formatDate, formatTime } from '@/utils/formatters'
import type { Reservation, CancellationRefundPreview } from '@/types/api'

export default function MyReservations() {
  const { t, i18n } = useTranslation()
  const { user, updateUser } = useAuthStore()
  const { showToast } = useToast()
  const queryClient = useQueryClient()

  const [activeTab, setActiveTab] = useState<'upcoming' | 'past'>('upcoming')
  const [cancelingId, setCancelingId] = useState<string | null>(null)
  const [refundPreview, setRefundPreview] = useState<CancellationRefundPreview | null>(null)
  const [isLoadingPreview, setIsLoadingPreview] = useState(false)
  const [feedbackReservation, setFeedbackReservation] = useState<Reservation | null>(null)
  const [feedbackRating, setFeedbackRating] = useState(5)
  const [feedbackComment, setFeedbackComment] = useState('')

  const { data: reservations, isLoading, refetch } = useQuery({
    queryKey: ['reservations'],
    queryFn: reservationApi.getMyReservations,
  })

  const { data: recurringReservations } = useQuery({
    queryKey: ['myRecurring'],
    queryFn: reservationApi.getMyRecurring,
  })

  const { data: waitlist } = useQuery({
    queryKey: ['myWaitlist'],
    queryFn: reservationApi.getMyWaitlist,
  })

  const { data: workouts } = useQuery({
    queryKey: ['myWorkouts'],
    queryFn: reservationApi.getMyWorkouts,
  })

  const { data: myFeedback } = useQuery({
    queryKey: ['myFeedback'],
    queryFn: feedbackApi.getMyFeedback,
  })

  const feedbackByReservation = useMemo(() => {
    return new Set((myFeedback || []).map((feedback) => feedback.reservationId))
  }, [myFeedback])

  const handleRefresh = useCallback(async () => {
    await refetch()
  }, [refetch])

  const cancelMutation = useMutation({
    mutationFn: reservationApi.cancel,
    onSuccess: (data) => {
      showToast('success', t('myReservations.cancelSuccess'))
      setCancelingId(null)
      setRefundPreview(null)
      // Invalidate all relevant queries
      queryClient.invalidateQueries({ queryKey: ['reservations'] })
      queryClient.invalidateQueries({ queryKey: ['myReservations'] })
      queryClient.invalidateQueries({ queryKey: ['availableSlots'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      // Refund credit based on actual refund amount
      if (user) {
        updateUser({ ...user, credits: user.credits + data.refundAmount })
      }
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      showToast('error', error.response?.data?.message || t('myReservations.cancelTooLate'))
      setCancelingId(null)
      setRefundPreview(null)
    },
  })

  const cancelRecurringMutation = useMutation({
    mutationFn: reservationApi.cancelRecurring,
    onSuccess: async () => {
      showToast('success', t('recurring.cancelled'))
      queryClient.invalidateQueries({ queryKey: ['reservations'] })
      queryClient.invalidateQueries({ queryKey: ['myRecurring'] })
      queryClient.invalidateQueries({ queryKey: ['availableSlots'] })
      const freshUser = await authApi.getMe().catch(() => null)
      if (freshUser) updateUser(freshUser)
    },
    onError: (error: { response?: { data?: { error?: string; message?: string } } }) => {
      showToast('error', error.response?.data?.error || error.response?.data?.message || t('errors.somethingWrong'))
    },
  })

  const leaveWaitlistMutation = useMutation({
    mutationFn: reservationApi.leaveWaitlist,
    onSuccess: () => {
      showToast('success', t('waitlist.left'))
      queryClient.invalidateQueries({ queryKey: ['myWaitlist'] })
      queryClient.invalidateQueries({ queryKey: ['availableSlots'] })
    },
    onError: () => showToast('error', t('errors.somethingWrong')),
  })

  const feedbackMutation = useMutation({
    mutationFn: () => feedbackApi.create(feedbackReservation!.id, feedbackRating, feedbackComment || undefined),
    onSuccess: () => {
      showToast('success', t('feedback.saved', 'Hodnocení uloženo'))
      queryClient.invalidateQueries({ queryKey: ['myFeedback'] })
      setFeedbackReservation(null)
      setFeedbackRating(5)
      setFeedbackComment('')
    },
    onError: (error: { response?: { data?: { error?: string; message?: string } } }) => {
      showToast('error', error.response?.data?.error || error.response?.data?.message || t('errors.somethingWrong'))
    },
  })

  // Fetch refund preview when canceling modal opens
  useEffect(() => {
    if (cancelingId) {
      setIsLoadingPreview(true)
      reservationApi.getRefundPreview(cancelingId)
        .then(preview => {
          setRefundPreview(preview)
        })
        .catch(() => {
          // If preview fails, still allow cancellation but show generic message
          setRefundPreview(null)
        })
        .finally(() => {
          setIsLoadingPreview(false)
        })
    } else {
      setRefundPreview(null)
    }
  }, [cancelingId])

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

  const downloadBlob = (blob: Blob, filename: string) => {
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    link.click()
    URL.revokeObjectURL(url)
  }

  const handleCalendarExport = async () => {
    try {
      const blob = await reservationApi.downloadIcal()
      downloadBlob(blob, 'reservations.ics')
    } catch {
      showToast('error', t('errors.somethingWrong'))
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
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {t('myReservations.title')}
        </h1>
        <Button variant="secondary" size="sm" leftIcon={<Download size={16} />} onClick={handleCalendarExport}>
          {t('admin.exportCalendar')}
        </Button>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 p-1 bg-neutral-100 dark:bg-dark-surface rounded-lg" role="tablist">
        <button
          role="tab"
          aria-selected={activeTab === 'upcoming'}
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
          role="tab"
          aria-selected={activeTab === 'past'}
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
                        {new Date(reservation.date).toLocaleDateString(i18n.language, {
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

                  {activeTab === 'upcoming' && canCancel(reservation) ? (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleCancelClick(reservation.id)}
                      className="text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 min-h-[44px] min-w-[44px]"
                    >
                      <X size={18} />
                    </Button>
                  ) : activeTab === 'past' && !feedbackByReservation.has(reservation.id) ? (
                    <Button
                      variant="secondary"
                      size="sm"
                      leftIcon={<Star size={16} />}
                      onClick={() => setFeedbackReservation(reservation)}
                    >
                      {t('feedback.rate', 'Ohodnotit')}
                    </Button>
                  ) : activeTab === 'past' && feedbackByReservation.has(reservation.id) ? (
                    <Badge variant="info">{t('feedback.rated', 'Ohodnoceno')}</Badge>
                  ) : null}
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

      {activeTab === 'upcoming' && recurringReservations?.some((item) => item.status !== 'cancelled') && (
        <Card variant="bordered">
          <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white mb-4">
            {t('recurring.myRecurring')}
          </h2>
          <div className="space-y-3">
            {recurringReservations.filter((item) => item.status !== 'cancelled').map((item) => (
              <div key={item.id} className="flex items-center justify-between gap-3 rounded-lg bg-neutral-50 dark:bg-dark-surface p-3">
                <div className="flex items-center gap-3">
                  <Repeat size={18} className="text-primary-500" />
                  <div>
                    <p className="font-medium text-neutral-900 dark:text-white">
                      {formatDate(item.startDate)} - {formatDate(item.endDate)}
                    </p>
                    <p className="text-sm text-neutral-500 dark:text-neutral-400">
                      {formatTime(item.startTime)} - {formatTime(item.endTime)} · {item.weeksCount}x
                    </p>
                  </div>
                </div>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => {
                    if (window.confirm(t('recurring.cancelSeries'))) {
                      cancelRecurringMutation.mutate(item.id)
                    }
                  }}
                  isLoading={cancelRecurringMutation.isPending}
                >
                  {t('recurring.cancelSeries')}
                </Button>
              </div>
            ))}
          </div>
        </Card>
      )}

      {activeTab === 'upcoming' && waitlist && waitlist.length > 0 && (
        <Card variant="bordered">
          <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white mb-4">
            {t('waitlist.myWaitlist')}
          </h2>
          <div className="space-y-3">
            {waitlist.map((item) => (
              <div key={item.id} className="flex items-center justify-between gap-3 rounded-lg bg-neutral-50 dark:bg-dark-surface p-3">
                <div>
                  <p className="font-medium text-neutral-900 dark:text-white">
                    {item.date ? formatDate(item.date) : t('calendar.unknown')}
                  </p>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400">
                    {formatTime(item.startTime)} - {formatTime(item.endTime)} · {item.status}
                  </p>
                </div>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => leaveWaitlistMutation.mutate(item.slotId)}
                  isLoading={leaveWaitlistMutation.isPending}
                >
                  {t('waitlist.leave')}
                </Button>
              </div>
            ))}
          </div>
        </Card>
      )}

      {activeTab === 'past' && workouts && workouts.length > 0 && (
        <Card variant="bordered">
          <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white mb-4">
            {t('workouts.title')}
          </h2>
          <div className="space-y-3">
            {workouts.slice(0, 10).map((workout) => (
              <div key={workout.id} className="rounded-lg bg-neutral-50 dark:bg-dark-surface p-3">
                <p className="font-medium text-neutral-900 dark:text-white">
                  {workout.date ? formatDate(workout.date) : t('workouts.title')}
                </p>
                {workout.exercises.length > 0 && (
                  <p className="text-sm text-neutral-600 dark:text-neutral-300 mt-1">
                    {workout.exercises.map((exercise) => exercise.name).filter(Boolean).join(', ')}
                  </p>
                )}
                {workout.notes && (
                  <p className="text-sm text-neutral-500 dark:text-neutral-400 mt-1 whitespace-pre-wrap">
                    {workout.notes}
                  </p>
                )}
              </div>
            ))}
          </div>
        </Card>
      )}

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

          {isLoadingPreview ? (
            <div className="flex items-center justify-center py-4">
              <Spinner size="md" />
            </div>
          ) : refundPreview ? (
            <div className={`p-3 rounded-lg ${
              refundPreview.refundPercentage === 100
                ? 'bg-green-50 dark:bg-green-900/20'
                : refundPreview.refundPercentage > 0
                ? 'bg-amber-50 dark:bg-amber-900/20'
                : 'bg-red-50 dark:bg-red-900/20'
            }`}>
              <p className={`text-sm font-medium ${
                refundPreview.refundPercentage === 100
                  ? 'text-green-700 dark:text-green-300'
                  : refundPreview.refundPercentage > 0
                  ? 'text-amber-700 dark:text-amber-300'
                  : 'text-red-700 dark:text-red-300'
              }`}>
                {refundPreview.refundPercentage === 100
                  ? t('myReservations.fullRefund', { credits: refundPreview.refundAmount })
                  : refundPreview.refundPercentage > 0
                  ? t('myReservations.partialRefund', {
                      credits: refundPreview.refundAmount,
                      percentage: refundPreview.refundPercentage
                    })
                  : t('myReservations.noRefund')}
              </p>
              <p className={`text-xs mt-1 ${
                refundPreview.refundPercentage === 100
                  ? 'text-green-600 dark:text-green-400'
                  : refundPreview.refundPercentage > 0
                  ? 'text-amber-600 dark:text-amber-400'
                  : 'text-red-600 dark:text-red-400'
              }`}>
                {t('myReservations.hoursUntilReservation', {
                  hours: Math.max(0, Math.floor(refundPreview.hoursUntilReservation))
                })}
              </p>
            </div>
          ) : (
            <p className="text-sm text-green-600 dark:text-green-400">
              {t('myReservations.creditsRefunded')}
            </p>
          )}

          <div className="flex flex-col-reverse gap-3 sm:flex-row">
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

      <Modal
        isOpen={!!feedbackReservation}
        onClose={() => setFeedbackReservation(null)}
        title={t('feedback.rate', 'Ohodnotit trénink')}
        size="sm"
      >
        <div className="space-y-4">
          <div className="flex justify-center gap-1">
            {[1, 2, 3, 4, 5].map((rating) => (
              <button
                key={rating}
                type="button"
                onClick={() => setFeedbackRating(rating)}
                className={rating <= feedbackRating ? 'text-amber-500' : 'text-neutral-300 dark:text-neutral-600'}
              >
                <Star size={28} fill="currentColor" />
              </button>
            ))}
          </div>
          <textarea
            value={feedbackComment}
            onChange={(e) => setFeedbackComment(e.target.value)}
            rows={4}
            className="w-full px-3 py-2 rounded-lg border border-neutral-200 dark:border-dark-border bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
            placeholder={t('feedback.commentPlaceholder', 'Poznámka k tréninku')}
          />
          <div className="flex flex-col-reverse gap-3 sm:flex-row">
            <Button variant="secondary" className="flex-1" onClick={() => setFeedbackReservation(null)}>
              {t('common.cancel')}
            </Button>
            <Button className="flex-1" onClick={() => feedbackMutation.mutate()} isLoading={feedbackMutation.isPending}>
              {t('common.save')}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
