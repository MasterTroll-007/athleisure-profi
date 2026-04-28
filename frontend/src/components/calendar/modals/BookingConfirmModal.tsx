import { useTranslation } from 'react-i18next'
import { MapPin } from 'lucide-react'
import { Modal, Button } from '@/components/ui'
import { formatTime } from '@/utils/formatters'
import type { AvailableSlot, PricingItemSummary } from '@/types/api'

interface BookingConfirmModalProps {
  isOpen: boolean
  slot: AvailableSlot | null
  userCredits: number
  isLoading: boolean
  pricingItems: PricingItemSummary[]
  selectedPricingItemId: string | null
  repeatWeekly: boolean
  weeksCount: number
  onPricingItemChange: (id: string) => void
  onRepeatWeeklyChange: (value: boolean) => void
  onWeeksCountChange: (value: number) => void
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
  repeatWeekly,
  weeksCount,
  onPricingItemChange,
  onRepeatWeeklyChange,
  onWeeksCountChange,
  onConfirm,
  onClose,
}: BookingConfirmModalProps) {
  const { t, i18n } = useTranslation()

  const selectedItem = pricingItems.find((p) => p.id === selectedPricingItemId)
  const creditCost = selectedItem?.credits ?? 1
  const getPricingName = (item: PricingItemSummary) =>
    i18n.language === 'cs' ? item.nameCs : (item.nameEn ?? item.nameCs)

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
            <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg">
              <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-2">
                {t('calendar.selectTrainingType')}
              </p>
              <div className="space-y-2">
                {pricingItems.map((item) => (
                  <label
                    key={item.id}
                    className="flex items-center gap-3 p-2 rounded-md cursor-pointer hover:bg-neutral-100 dark:hover:bg-dark-hover transition-colors"
                  >
                    <input
                      type="radio"
                      name="pricingItem"
                      value={item.id}
                      checked={selectedPricingItemId === item.id}
                      onChange={() => onPricingItemChange(item.id)}
                      className="text-primary-600 focus:ring-primary-500"
                    />
                    <span className="text-neutral-900 dark:text-white">
                      {getPricingName(item)}
                    </span>
                    <span className="ml-auto text-sm text-neutral-500 dark:text-neutral-400">
                      {t('calendar.creditCost', { credits: item.credits })}
                    </span>
                  </label>
                ))}
              </div>
            </div>
          )}

          {pricingItems.length === 1 && selectedItem && (
            <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg">
              <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">
                {t('calendar.trainingType')}
              </p>
              <p className="font-medium text-neutral-900 dark:text-white">
                {getPricingName(selectedItem)}
                <span className="ml-2 text-sm text-neutral-500 dark:text-neutral-400">
                  ({t('calendar.creditCost', { credits: selectedItem.credits })})
                </span>
              </p>
            </div>
          )}

          <div className="p-4 bg-primary-50 dark:bg-primary-900/20 rounded-lg">
            <p className="text-sm text-primary-600 dark:text-primary-400 mb-1">
              {t('reservation.cost', { credits: repeatWeekly ? creditCost * weeksCount : creditCost })}
            </p>
            <p className="font-semibold text-primary-700 dark:text-primary-300">
              {t('calendar.creditFrom', { used: repeatWeekly ? creditCost * weeksCount : creditCost, total: userCredits })}
            </p>
          </div>
          <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg space-y-3">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={repeatWeekly}
                onChange={(e) => onRepeatWeeklyChange(e.target.checked)}
                className="w-4 h-4 rounded border-neutral-300 text-primary-500 focus:ring-primary-500"
              />
              <span className="text-sm font-medium text-neutral-900 dark:text-white">
                {t('recurring.repeat')}
              </span>
            </label>
            {repeatWeekly && (
              <div>
                <label className="block text-sm text-neutral-500 dark:text-neutral-400 mb-1">
                  {t('recurring.weeks')}
                </label>
                <input
                  type="number"
                  min={2}
                  max={12}
                  value={weeksCount}
                  onChange={(e) => onWeeksCountChange(Number(e.target.value))}
                  className="w-full px-3 py-2 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white"
                />
              </div>
            )}
          </div>
          <div className="flex gap-3 pt-2">
            <Button variant="secondary" className="flex-1" onClick={onClose}>
              {t('common.cancel')}
            </Button>
            <Button
              className="flex-1"
              onClick={onConfirm}
              isLoading={isLoading}
              disabled={(pricingItems.length > 0 && !selectedPricingItemId) || (repeatWeekly && (weeksCount < 2 || weeksCount > 12 || userCredits < creditCost * weeksCount))}
            >
              {t('reservation.book')}
            </Button>
          </div>
        </div>
      )}
    </Modal>
  )
}
