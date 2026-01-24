import { useTranslation } from 'react-i18next'

interface CalendarLegendProps {
  isAdmin: boolean
}

export function CalendarLegend({ isAdmin }: CalendarLegendProps) {
  const { t } = useTranslation()

  if (isAdmin) {
    return (
      <div className="flex flex-wrap items-center gap-4 text-sm">
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded bg-gray-200 border border-gray-400"></div>
          <span className="text-neutral-600 dark:text-neutral-400">{t('calendar.locked')}</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded bg-green-200 border border-green-500"></div>
          <span className="text-neutral-600 dark:text-neutral-400">{t('calendar.available')}</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded bg-blue-200 border border-blue-500"></div>
          <span className="text-neutral-600 dark:text-neutral-400">{t('calendar.reserved')}</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded bg-red-200 border border-red-500"></div>
          <span className="text-neutral-600 dark:text-neutral-400">{t('calendar.cancelled')}</span>
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-wrap items-center gap-4 text-sm">
      <div className="flex items-center gap-2">
        <div className="w-3 h-3 rounded bg-green-200 border border-green-500"></div>
        <span className="text-neutral-600 dark:text-neutral-400">{t('calendar.available')}</span>
      </div>
      <div className="flex items-center gap-2">
        <div className="w-3 h-3 rounded bg-blue-200 border border-blue-500"></div>
        <span className="text-neutral-600 dark:text-neutral-400">{t('calendar.yourReservation')}</span>
      </div>
      <div className="flex items-center gap-2">
        <div className="w-3 h-3 rounded bg-red-200 border border-red-500"></div>
        <span className="text-neutral-600 dark:text-neutral-400">{t('calendar.reserved')}</span>
      </div>
      <div className="flex items-center gap-2">
        <div className="w-3 h-3 rounded bg-gray-200 border border-gray-400"></div>
        <span className="text-neutral-600 dark:text-neutral-400">{t('calendar.unavailable')}</span>
      </div>
    </div>
  )
}
