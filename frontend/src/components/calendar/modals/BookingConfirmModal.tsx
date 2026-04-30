import { useTranslation } from 'react-i18next'
import { MapPin } from 'lucide-react'
import { Modal, Button } from '@/components/ui'
import { TrainingTypeAccordion } from './TrainingTypeAccordion'
import { formatCredits, formatTime } from '@/utils/formatters'
import type { AvailableSlot, PricingItemSummary } from '@/types/api'

interface BookingConfirmModalProps {
  isOpen: boolean
  slot: AvailableSlot | null
  userCredits: number
  isLoading: boolean
  pricingItems: PricingItemSummary[]
  selectedPricingItemId: string | null
  onPricingItemChange: (id: string) => void
  onConfirm: () => void
  onClose: () => void
}

export function BookingConfirmModal({
  isOpen,
  slot,
  userCredits,
  isLoading,
  pricingItems,
  selectedPricingItemId,
  onPricingItemChange,
  onConfirm,
  onClose,
}: BookingConfirmModalProps) {
  const { t, i18n } = useTranslation()

  const selectedItem = pricingItems.find((p) => p.id === selectedPricingItemId)
  const creditCost = selectedItem?.credits ?? 1
  const hasEnoughCredits = userCredits >= creditCost

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

          {slot.locationName && (
            <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg">
              <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">
                {t('admin.locations.label')}
              </p>
              <div className="flex items-start gap-3">
                <span
                  className="inline-flex items-center justify-center w-8 h-8 rounded-full flex-shrink-0 mt-0.5"
                  style={{ backgroundColor: slot.locationColor || '#9CA3AF' }}
                >
                  <MapPin size={16} className="text-white" />
                </span>
                <div className="min-w-0">
                  <p className="font-medium text-neutral-900 dark:text-white">{slot.locationName}</p>
                  {slot.locationAddress && (
                    <p className="text-sm text-neutral-600 dark:text-neutral-300 whitespace-pre-line">
                      {slot.locationAddress}
                    </p>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* Training type selection */}
          {pricingItems.length > 1 && (
            <TrainingTypeAccordion
              items={pricingItems}
              selectedIds={selectedPricingItemId ? [selectedPricingItemId] : []}
              onSelectedIdsChange={(ids) => {
                const [id] = ids
                if (id) onPricingItemChange(id)
              }}
              selectionMode="single"
            />
          )}

          {pricingItems.length === 1 && selectedItem && (
            <TrainingTypeAccordion
              items={pricingItems}
              selectedIds={[selectedItem.id]}
              selectionMode="single"
              readOnly
            />
          )}

          <div className="p-4 bg-primary-50 dark:bg-primary-900/20 rounded-lg">
            <p className="text-sm text-primary-600 dark:text-primary-400 mb-1">
              {t('reservation.cost', { credits: formatCredits(creditCost, i18n.language) })}
            </p>
            <p className="font-semibold text-primary-700 dark:text-primary-300">
              {t('calendar.creditFrom', {
                used: formatCredits(creditCost, i18n.language),
                total: formatCredits(userCredits, i18n.language),
              })}
            </p>
          </div>
          <div className="flex flex-col-reverse gap-3 pt-2 sm:flex-row">
            <Button variant="secondary" className="flex-1" onClick={onClose}>
              {t('common.cancel')}
            </Button>
            <Button
              className="flex-1"
              onClick={onConfirm}
              isLoading={isLoading}
              disabled={(pricingItems.length > 0 && !selectedPricingItemId) || !hasEnoughCredits}
              data-testid="reservation-confirm-button"
            >
              {t('reservation.book')}
            </Button>
          </div>
        </div>
      )}
    </Modal>
  )
}
