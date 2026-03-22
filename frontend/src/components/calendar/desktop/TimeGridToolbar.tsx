import { useTranslation } from 'react-i18next'
import { ChevronLeft, ChevronRight } from 'lucide-react'

interface TimeGridToolbarProps {
  title: string
  viewDays: number
  onPrev: () => void
  onNext: () => void
  onToday: () => void
  onViewChange: (days: number) => void
  onMonthView: () => void
}

const VIEW_OPTIONS = [1, 3, 5, 7] as const

export function TimeGridToolbar({
  title,
  viewDays,
  onPrev,
  onNext,
  onToday,
  onViewChange,
  onMonthView,
}: TimeGridToolbarProps) {
  const { t, i18n } = useTranslation()
  const cs = i18n.language === 'cs'

  const viewLabels: Record<number, string> = {
    1: cs ? '1 den' : '1 day',
    3: cs ? '3 dny' : '3 days',
    5: cs ? '5 dnů' : '5 days',
    7: cs ? '7 dnů' : '7 days',
  }

  return (
    <div className="flex items-center justify-between px-4 py-2.5 border-b border-neutral-200 dark:border-neutral-700 bg-white dark:bg-dark-surface">
      {/* Left: navigation */}
      <div className="flex items-center gap-1">
        <button
          onClick={onPrev}
          className="p-1.5 rounded-lg hover:bg-neutral-100 dark:hover:bg-dark-surfaceHover transition-colors"
          aria-label="Previous"
        >
          <ChevronLeft size={20} className="text-neutral-600 dark:text-neutral-300" />
        </button>
        <button
          onClick={onNext}
          className="p-1.5 rounded-lg hover:bg-neutral-100 dark:hover:bg-dark-surfaceHover transition-colors"
          aria-label="Next"
        >
          <ChevronRight size={20} className="text-neutral-600 dark:text-neutral-300" />
        </button>
        <button
          onClick={onToday}
          className="ml-1 px-3 py-1 text-sm font-medium text-primary-600 dark:text-primary-400 bg-primary-50 dark:bg-primary-900/30 rounded-md hover:bg-primary-100 dark:hover:bg-primary-900/50 transition-colors"
        >
          {t('calendar.today')}
        </button>
        <span className="ml-3 text-base font-semibold text-neutral-900 dark:text-white">
          {title}
        </span>
      </div>

      {/* Right: view toggles */}
      <div className="flex items-center gap-1 bg-neutral-100 dark:bg-dark-surfaceHover rounded-lg p-0.5">
        {VIEW_OPTIONS.map(v => (
          <button
            key={v}
            onClick={() => onViewChange(v)}
            className={`px-2.5 py-1 text-xs font-medium rounded-md transition-colors ${
              viewDays === v
                ? 'bg-white dark:bg-dark-surface text-neutral-900 dark:text-white shadow-sm'
                : 'text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-white'
            }`}
          >
            {viewLabels[v]}
          </button>
        ))}
        <button
          onClick={onMonthView}
          className="px-2.5 py-1 text-xs font-medium rounded-md text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-white transition-colors"
        >
          {cs ? 'Měsíc' : 'Month'}
        </button>
      </div>
    </div>
  )
}
