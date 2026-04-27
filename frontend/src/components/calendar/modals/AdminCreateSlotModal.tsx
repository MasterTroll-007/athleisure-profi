import { useTranslation } from 'react-i18next'
import { useQuery } from '@tanstack/react-query'
import { Modal, Button, Input } from '@/components/ui'
import { locationsApi } from '@/services/api'
import { formatCredits } from '@/utils/formatters'
import type { PricingItem } from '@/types/api'

interface AdminCreateSlotModalProps {
  isOpen: boolean
  date: string
  time: string
  duration: number
  note: string
  pricingItems?: PricingItem[]
  selectedPricingItemIds: string[]
  locationId: string | null
  isLoading: boolean
  onDateChange: (date: string) => void
  onTimeChange: (time: string) => void
  onDurationChange: (duration: number) => void
  onNoteChange: (note: string) => void
  onPricingItemIdsChange: (ids: string[]) => void
  onLocationIdChange: (id: string | null) => void
  onSubmit: () => void
  onClose: () => void
}

export function AdminCreateSlotModal({
  isOpen,
  date,
  time,
  duration,
  note,
  pricingItems,
  selectedPricingItemIds,
  locationId,
  isLoading,
  onDateChange,
  onTimeChange,
  onDurationChange,
  onNoteChange,
  onPricingItemIdsChange,
  onLocationIdChange,
  onSubmit,
  onClose,
}: AdminCreateSlotModalProps) {
  const { t, i18n } = useTranslation()

  const activePricingItems = pricingItems?.filter((p) => p.isActive) || []

  const { data: locations } = useQuery({
    queryKey: ['admin', 'locations'],
    queryFn: locationsApi.listAdmin,
    enabled: isOpen,
  })
  const activeLocations = locations?.filter((l) => l.isActive) || []

  const handleTogglePricingItem = (id: string) => {
    if (selectedPricingItemIds.includes(id)) {
      onPricingItemIdsChange(selectedPricingItemIds.filter((pid) => pid !== id))
    } else {
      onPricingItemIdsChange([...selectedPricingItemIds, id])
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={t('calendar.createSlot')} size="sm">
      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
            {t('calendar.date')}
          </label>
          <Input type="date" value={date} onChange={(e) => onDateChange(e.target.value)} />
        </div>
        <div>
          <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
            {t('calendar.time')}
          </label>
          <Input type="time" value={time} onChange={(e) => onTimeChange(e.target.value)} />
        </div>
        <div>
          <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
            {t('calendar.duration')}
          </label>
          <div className="relative">
            <select
              value={duration}
              onChange={(e) => onDurationChange(Number(e.target.value))}
              className="w-full px-3 py-2 pr-9 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white appearance-none"
            >
              <option value={30}>30 {t('calendar.minutes')}</option>
              <option value={45}>45 {t('calendar.minutes')}</option>
              <option value={60}>60 {t('calendar.minutes')}</option>
              <option value={90}>90 {t('calendar.minutes')}</option>
              <option value={120}>120 {t('calendar.minutes')}</option>
            </select>
            <svg className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-500 dark:text-neutral-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
            </svg>
          </div>
        </div>
        {activeLocations.length > 0 && (
          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
              {t('admin.locations.label')}
            </label>
            <div className="relative">
              <select
                value={locationId ?? ''}
                onChange={(e) => onLocationIdChange(e.target.value === '' ? null : e.target.value)}
                className="w-full px-3 py-2 pr-9 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white appearance-none"
              >
                <option value="">{t('admin.locations.selectPlaceholder')}</option>
                {activeLocations.map((loc) => (
                  <option key={loc.id} value={loc.id}>
                    {loc.nameCs}
                  </option>
                ))}
              </select>
              <svg className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-500 dark:text-neutral-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
              </svg>
            </div>
          </div>
        )}
        {activePricingItems.length > 0 && (
          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
              {t('calendar.selectTrainingType')}
            </label>
            <div className="grid gap-2 max-h-48 overflow-y-auto p-1">
              {activePricingItems.map((item) => {
                const isSelected = selectedPricingItemIds.includes(item.id)
                return (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => handleTogglePricingItem(item.id)}
                    className={`flex items-center justify-between px-3 py-2.5 rounded-xl border-2 transition-all duration-150 text-left ${
                      isSelected
                        ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/30 shadow-sm ring-1 ring-primary-500/20'
                        : 'border-neutral-200 dark:border-neutral-700 bg-white dark:bg-dark-surface hover:border-neutral-300 dark:hover:border-neutral-600'
                    }`}
                  >
                    <div className="flex items-center gap-2.5 min-w-0">
                      <div
                        className={`flex-shrink-0 w-5 h-5 rounded-md flex items-center justify-center transition-colors ${
                          isSelected
                            ? 'bg-primary-500 text-white'
                            : 'bg-neutral-100 dark:bg-neutral-700'
                        }`}
                      >
                        {isSelected && (
                          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                            <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                          </svg>
                        )}
                      </div>
                      <span className={`text-sm truncate ${
                        isSelected
                          ? 'font-medium text-primary-700 dark:text-primary-300'
                          : 'text-neutral-700 dark:text-neutral-300'
                      }`}>
                        {i18n.language === 'cs' ? item.nameCs : (item.nameEn || item.nameCs)}
                      </span>
                    </div>
                    <span className={`text-xs font-medium ml-2 flex-shrink-0 px-2 py-0.5 rounded-full ${
                      isSelected
                        ? 'bg-primary-100 dark:bg-primary-800/50 text-primary-700 dark:text-primary-300'
                        : 'bg-neutral-100 dark:bg-neutral-700 text-neutral-500 dark:text-neutral-400'
                    }`}>
                      {formatCredits(item.credits, i18n.language)}
                    </span>
                  </button>
                )
              })}
            </div>
          </div>
        )}
        <div>
          <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
            {t('calendar.noteOptional')}
          </label>
          <textarea
            value={note}
            onChange={(e) => onNoteChange(e.target.value)}
            className="w-full px-3 py-2 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white resize-none"
            rows={2}
          />
        </div>
        <Button className="w-full" onClick={onSubmit} isLoading={isLoading}>
          {t('calendar.createSlot')}
        </Button>
      </div>
    </Modal>
  )
}
