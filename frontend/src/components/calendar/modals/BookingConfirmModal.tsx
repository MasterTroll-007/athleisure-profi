import { useTranslation } from 'react-i18next'
import { Modal, Button } from '@/components/ui'
import { formatTime } from '@/utils/formatters'
import type { AvailableSlot } from '@/types/api'

interface BookingConfirmModalProps {
  isOpen: boolean
  slot: AvailableSlot | null
  userCredits: number
  isLoading: boolean
  onConfirm: () => void
  onClose: () => void
}

export function BookingConfirmModal({
  isOpen,
  slot,
  userCredits,
  isLoading,
  onConfirm,
  onClose,
}: BookingConfirmModalProps) {
  const { t, i18n } = useTranslation()

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={t('reservation.confirm')} size="sm">
      {slot && (
        <div className="space-y-4">
          <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg">
            <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">{t('reservation.date')}</p>
            <p className="font-medium text-neutral-900 dark:text-white">
              {new Date(slot.start).toLocaleDateString(i18n.language, {
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
              {formatTime(slot.start.split('T')[1])} - {formatTime(slot.end.split('T')[1])}
            </p>
          </div>
          <div className="p-4 bg-primary-50 dark:bg-primary-900/20 rounded-lg">
            <p className="text-sm text-primary-600 dark:text-primary-400 mb-1">
              {t('reservation.cost', { credits: 1 })}
            </p>
            <p className="font-semibold text-primary-700 dark:text-primary-300">
              {t('calendar.creditFrom', { used: 1, total: userCredits })}
            </p>
          </div>
          <div className="flex gap-3 pt-2">
            <Button variant="secondary" className="flex-1" onClick={onClose}>
              {t('common.cancel')}
            </Button>
            <Button className="flex-1" onClick={onConfirm} isLoading={isLoading}>
              {t('reservation.book')}
            </Button>
          </div>
        </div>
      )}
    </Modal>
  )
}
