import { useTranslation } from 'react-i18next'
import { useQuery } from '@tanstack/react-query'
import { Modal, Button, DatePicker, DurationPicker, TimePicker, Select, Textarea } from '@/components/ui'
import { locationsApi } from '@/services/api'
import { TrainingTypeAccordion } from './TrainingTypeAccordion'
import type { PricingItem } from '@/types/api'

const SLOT_DURATION_MAX_MINUTES = 480

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
  const { t } = useTranslation()

  const activePricingItems = pricingItems?.filter((p) => p.isActive) || []

  const { data: locations } = useQuery({
    queryKey: ['admin', 'locations'],
    queryFn: locationsApi.listAdmin,
    enabled: isOpen,
  })
  const activeLocations = locations?.filter((l) => l.isActive) || []

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={t('calendar.createSlot')} size="sm">
      <div className="space-y-4">
        <DatePicker label={t('calendar.date')} value={date} onChange={onDateChange} />
        <TimePicker label={t('calendar.time')} value={time} onChange={onTimeChange} />
        <DurationPicker
          label={t('calendar.duration')}
          value={duration}
          onChange={onDurationChange}
          min={15}
          max={SLOT_DURATION_MAX_MINUTES}
          minuteStep={15}
        />
        {activeLocations.length > 0 && (
          <Select
            label={t('admin.locations.label')}
            value={locationId ?? ''}
            onChange={(e) => onLocationIdChange(e.target.value === '' ? null : e.target.value)}
          >
            <option value="">{t('admin.locations.selectPlaceholder')}</option>
            {activeLocations.map((loc) => (
              <option key={loc.id} value={loc.id}>
                {loc.nameCs}
              </option>
            ))}
          </Select>
        )}
        {activePricingItems.length > 0 && (
          <TrainingTypeAccordion
            items={activePricingItems}
            selectedIds={selectedPricingItemIds}
            onSelectedIdsChange={onPricingItemIdsChange}
          />
        )}
        <div>
          <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
            {t('calendar.noteOptional')}
          </label>
          <Textarea
            value={note}
            onChange={(e) => onNoteChange(e.target.value)}
            className="resize-none"
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
