import { useTranslation } from 'react-i18next'
import { MapPin } from 'lucide-react'
import { Modal, Button } from '@/components/ui'
import { formatCredits, formatTime } from '@/utils/formatters'
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
          {reservation.locationName && (
            <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg">
              <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">
                {t('admin.locations.label')}
              </p>
              <div className="flex items-start gap-3">
                <span
                  className="inline-flex items-center justify-center w-8 h-8 rounded-full flex-shrink-0 mt-0.5"
                  style={{ backgroundColor: reservation.locationColor || '#9CA3AF' }}
                >
                  <MapPin size={16} className="text-white" />
                </span>
                <div className="min-w-0">
                  <p className="font-medium text-neutral-900 dark:text-white">{reservation.locationName}</p>
                  {reservation.locationAddress && (
                    <p className="text-sm text-neutral-600 dark:text-neutral-300 whitespace-pre-line">
                      {reservation.locationAddress}
                    </p>
                  )}
                </div>
              </div>
            </div>
          )}
          <div className="p-4 bg-green-50 dark:bg-green-900/20 rounded-lg">
            <p className="text-sm text-green-600 dark:text-green-400 mb-1">{t('credits.refund')}</p>
            <p className="font-semibold text-green-700 dark:text-green-300">
              +{formatCredits(reservation.creditsUsed, i18n.language)}
            </p>
          </div>
          <div className="flex flex-col-reverse gap-3 pt-2 sm:flex-row">
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
