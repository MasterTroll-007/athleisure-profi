import { useTranslation } from 'react-i18next'
import { Modal, Button } from '@/components/ui'
import { formatTime } from '@/utils/formatters'
import type { Reservation } from '@/types/api'

interface CancelReservationModalProps {
  isOpen: boolean
  reservation: Reservation | null
  isLoading: boolean
  onConfirm: () => void
  onClose: () => void
}

export function CancelReservationModal({
  isOpen,
  reservation,
  isLoading,
  onConfirm,
  onClose,
}: CancelReservationModalProps) {
  const { t, i18n } = useTranslation()

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={t('myReservations.cancelReservation')} size="sm">
      {reservation && (
        <div className="space-y-4">
          <p className="text-neutral-700 dark:text-neutral-300">{t('myReservations.cancelConfirm')}</p>
          <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg">
            <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">{t('reservation.date')}</p>
            <p className="font-medium text-neutral-900 dark:text-white">
              {new Date(`${reservation.date}T${reservation.startTime}`).toLocaleDateString(i18n.language, {
                weekday: 'long',
                day: 'numeric',
                month: 'long',
                year: 'numeric',
              })}
            </p>
          </div>
          <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg">
            <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">{t('reservation.time')}</p>
            <p className="font-mono font-semibold text-lg text-neutral-900 dark:text-white">
              {formatTime(reservation.startTime)} - {formatTime(reservation.endTime)}
            </p>
          </div>
          <div className="p-4 bg-green-50 dark:bg-green-900/20 rounded-lg">
            <p className="text-sm text-green-600 dark:text-green-400 mb-1">{t('credits.refund')}</p>
            <p className="font-semibold text-green-700 dark:text-green-300">
              +{reservation.creditsUsed} {reservation.creditsUsed === 1 ? t('calendar.credit') : t('calendar.credits')}
            </p>
          </div>
          <div className="flex gap-3 pt-2">
            <Button variant="secondary" className="flex-1" onClick={onClose}>
              {t('common.cancel')}
            </Button>
            <Button variant="danger" className="flex-1" onClick={onConfirm} isLoading={isLoading}>
              {t('myReservations.cancelReservation')}
            </Button>
          </div>
        </div>
      )}
    </Modal>
  )
}
