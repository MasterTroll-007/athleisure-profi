import { useTranslation } from 'react-i18next'
import { isSameDay } from './constants'

interface TimeGridHeaderProps {
  days: Date[]
}

export function TimeGridHeader({ days }: TimeGridHeaderProps) {
  const { i18n } = useTranslation()
  const today = new Date()

  const dayNames = i18n.language === 'cs'
    ? ['ne', 'po', 'út', 'st', 'čt', 'pá', 'so']
    : ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

  const formatHeader = (date: Date): string => {
    const dayName = dayNames[date.getDay()]
    return `${dayName} ${date.getDate()}.${date.getMonth() + 1}.`
  }

  return (
    <div className="flex">
      {days.map((date, i) => {
        const isToday = isSameDay(date, today)
        return (
          <div
            key={i}
            className={`flex-1 min-w-0 py-2.5 text-center text-xs font-medium border-r border-neutral-200 dark:border-neutral-700 last:border-r-0 ${
              isToday
                ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-400 font-semibold'
                : 'text-neutral-600 dark:text-neutral-400'
            }`}
          >
            {formatHeader(date)}
          </div>
        )
      })}
    </div>
  )
}
