import { useTranslation } from 'react-i18next'
import { Modal, Button, Input } from '@/components/ui'

interface AdminCreateSlotModalProps {
  isOpen: boolean
  date: string
  time: string
  duration: number
  note: string
  isLoading: boolean
  onDateChange: (date: string) => void
  onTimeChange: (time: string) => void
  onDurationChange: (duration: number) => void
  onNoteChange: (note: string) => void
  onSubmit: () => void
  onClose: () => void
}

export function AdminCreateSlotModal({
  isOpen,
  date,
  time,
  duration,
  note,
  isLoading,
  onDateChange,
  onTimeChange,
  onDurationChange,
  onNoteChange,
  onSubmit,
  onClose,
}: AdminCreateSlotModalProps) {
  const { t } = useTranslation()

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
          <select
            value={duration}
            onChange={(e) => onDurationChange(Number(e.target.value))}
            className="w-full px-3 py-2 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white"
          >
            <option value={30}>30 {t('calendar.minutes')}</option>
            <option value={45}>45 {t('calendar.minutes')}</option>
            <option value={60}>60 {t('calendar.minutes')}</option>
            <option value={90}>90 {t('calendar.minutes')}</option>
            <option value={120}>120 {t('calendar.minutes')}</option>
          </select>
        </div>
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
