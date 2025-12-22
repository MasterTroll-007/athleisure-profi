import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { ChevronLeft, ChevronRight, Clock, CreditCard } from 'lucide-react'
import { Card, Button, Modal, Spinner, Badge } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { reservationApi, creditApi } from '@/services/api'
import { useAuthStore } from '@/stores/authStore'
import { formatTime } from '@/utils/formatters'

export default function NewReservation() {
  const { t, i18n } = useTranslation()
  const { user, updateUser } = useAuthStore()
  const { showToast } = useToast()
  const queryClient = useQueryClient()

  const [selectedDate, setSelectedDate] = useState<Date>(new Date())
  const [selectedSlot, setSelectedSlot] = useState<{
    start: string
    end: string
    blockId: string
  } | null>(null)
  const [isModalOpen, setIsModalOpen] = useState(false)

  const dateString = selectedDate.toISOString().split('T')[0]

  const { data: slotsResponse, isLoading: slotsLoading } = useQuery({
    queryKey: ['availableSlots', dateString],
    queryFn: () => reservationApi.getAvailableSlots(dateString),
  })

  const { data: pricingItems } = useQuery({
    queryKey: ['pricing'],
    queryFn: creditApi.getPricing,
  })

  const defaultPricing = pricingItems?.find((p) => p.credits === 1)

  const createMutation = useMutation({
    mutationFn: reservationApi.create,
    onSuccess: () => {
      showToast('success', t('reservation.success'))
      setIsModalOpen(false)
      setSelectedSlot(null)
      queryClient.invalidateQueries({ queryKey: ['reservations'] })
      queryClient.invalidateQueries({ queryKey: ['availableSlots'] })
      // Update user credits
      if (user) {
        updateUser({ ...user, credits: user.credits - 1 })
      }
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      showToast('error', error.response?.data?.message || t('errors.somethingWrong'))
    },
  })

  const changeDate = (days: number) => {
    const newDate = new Date(selectedDate)
    newDate.setDate(newDate.getDate() + days)
    setSelectedDate(newDate)
    setSelectedSlot(null)
  }

  const handleSlotClick = (slot: { start: string; end: string; blockId: string }) => {
    if ((user?.credits || 0) < 1) {
      showToast('error', t('reservation.notEnoughCredits'))
      return
    }
    setSelectedSlot(slot)
    setIsModalOpen(true)
  }

  const handleConfirm = () => {
    if (!selectedSlot) return

    const startTime = selectedSlot.start.split('T')[1].substring(0, 5)
    const endTime = selectedSlot.end.split('T')[1].substring(0, 5)

    createMutation.mutate({
      date: dateString,
      startTime,
      endTime,
      blockId: selectedSlot.blockId,
      pricingItemId: defaultPricing?.id,
    })
  }

  const isToday = selectedDate.toDateString() === new Date().toDateString()
  const isPast = selectedDate < new Date(new Date().setHours(0, 0, 0, 0))

  return (
    <div className="space-y-6 animate-fade-in">
      <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
        {t('reservation.title')}
      </h1>

      {/* Date selector */}
      <Card variant="bordered">
        <div className="flex items-center justify-between">
          <button
            onClick={() => changeDate(-1)}
            disabled={isPast}
            className="p-2 rounded-lg text-neutral-600 hover:bg-neutral-100 dark:text-neutral-300 dark:hover:bg-dark-surfaceHover disabled:opacity-30 disabled:cursor-not-allowed touch-target"
          >
            <ChevronLeft size={24} />
          </button>

          <div className="text-center">
            <p className="text-lg font-semibold text-neutral-900 dark:text-white">
              {selectedDate.toLocaleDateString(i18n.language, {
                weekday: 'long',
                day: 'numeric',
                month: 'long',
              })}
            </p>
            {isToday && (
              <Badge variant="primary" size="sm">
                Dnes
              </Badge>
            )}
          </div>

          <button
            onClick={() => changeDate(1)}
            className="p-2 rounded-lg text-neutral-600 hover:bg-neutral-100 dark:text-neutral-300 dark:hover:bg-dark-surfaceHover touch-target"
          >
            <ChevronRight size={24} />
          </button>
        </div>
      </Card>

      {/* Credit info */}
      <div className="flex items-center gap-2 p-3 bg-primary-50 dark:bg-primary-900/20 rounded-lg">
        <CreditCard size={18} className="text-primary-500" />
        <span className="text-sm text-primary-700 dark:text-primary-300">
          {t('home.yourCredits')}: <strong>{user?.credits || 0}</strong>
        </span>
      </div>

      {/* Available slots */}
      <div>
        <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white mb-4">
          {t('reservation.availableSlots')}
        </h2>

        {slotsLoading ? (
          <div className="flex justify-center py-12">
            <Spinner size="lg" />
          </div>
        ) : isPast ? (
          <Card variant="bordered">
            <p className="text-center py-8 text-neutral-500 dark:text-neutral-400">
              Nelze rezervovat v minulosti
            </p>
          </Card>
        ) : slotsResponse?.slots && slotsResponse.slots.length > 0 ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
            {slotsResponse.slots.map((slot, index) => {
              const startTime = slot.start.split('T')[1].substring(0, 5)
              const endTime = slot.end.split('T')[1].substring(0, 5)
              const isAvailable = slot.isAvailable !== false

              return (
                <button
                  key={index}
                  onClick={() => isAvailable && handleSlotClick(slot)}
                  disabled={!isAvailable}
                  className={
                    isAvailable
                      ? 'p-4 rounded-lg border-2 border-status-availableBorder bg-status-available text-neutral-800 hover:border-primary-500 hover:bg-primary-50 dark:hover:bg-primary-900/20 transition-colors touch-target'
                      : 'p-4 rounded-lg border-2 border-neutral-200 dark:border-neutral-700 bg-neutral-100 dark:bg-neutral-800 text-neutral-400 dark:text-neutral-500 cursor-not-allowed'
                  }
                >
                  <div className="flex items-center justify-center gap-1">
                    <Clock size={16} />
                    <span className="font-mono font-semibold">
                      {startTime} - {endTime}
                    </span>
                  </div>
                </button>
              )
            })}
          </div>
        ) : (
          <Card variant="bordered">
            <p className="text-center py-8 text-neutral-500 dark:text-neutral-400">
              {t('reservation.noSlots')}
            </p>
          </Card>
        )}
      </div>

      {/* Confirmation Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={t('reservation.confirm')}
      >
        {selectedSlot && (
          <div className="space-y-4">
            <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg">
              <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">
                {t('reservation.date')}
              </p>
              <p className="font-medium text-neutral-900 dark:text-white">
                {selectedDate.toLocaleDateString(i18n.language, {
                  weekday: 'long',
                  day: 'numeric',
                  month: 'long',
                  year: 'numeric',
                })}
              </p>
            </div>

            <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg">
              <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">
                {t('reservation.time')}
              </p>
              <p className="font-mono font-semibold text-lg text-neutral-900 dark:text-white">
                {formatTime(selectedSlot.start.split('T')[1])} -{' '}
                {formatTime(selectedSlot.end.split('T')[1])}
              </p>
            </div>

            <div className="p-4 bg-primary-50 dark:bg-primary-900/20 rounded-lg">
              <p className="text-sm text-primary-600 dark:text-primary-400 mb-1">
                {t('reservation.cost', { credits: 1 })}
              </p>
              <p className="font-semibold text-primary-700 dark:text-primary-300">
                1 kredit z {user?.credits || 0}
              </p>
            </div>

            <div className="flex gap-3 pt-2">
              <Button
                variant="secondary"
                className="flex-1"
                onClick={() => setIsModalOpen(false)}
              >
                {t('common.cancel')}
              </Button>
              <Button
                className="flex-1"
                onClick={handleConfirm}
                isLoading={createMutation.isPending}
              >
                {t('reservation.book')}
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
