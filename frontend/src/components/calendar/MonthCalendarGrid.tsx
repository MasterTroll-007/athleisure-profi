import { useTranslation } from 'react-i18next'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import type { MonthSlotInfo } from '@/types/calendar'

interface MonthCalendarGridProps {
  monthViewDate: Date
  isDesktop?: boolean
  isAdmin: boolean
  isTransitioning: boolean
  transitionDay: number | null
  getSlotsForDay: (year: number, month: number, day: number) => MonthSlotInfo[]
  onPrevMonth: () => void
  onNextMonth: () => void
  onToday: () => void
  onDayClick: (day: number) => void
  onBackToWeek?: () => void
}

export function MonthCalendarGrid({
  monthViewDate,
  isDesktop = false,
  isAdmin: _isAdmin,
  isTransitioning,
  transitionDay,
  getSlotsForDay,
  onPrevMonth,
  onNextMonth,
  onToday,
  onDayClick,
  onBackToWeek,
}: MonthCalendarGridProps) {
  const { t, i18n } = useTranslation()

  const year = monthViewDate.getFullYear()
  const month = monthViewDate.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const daysInMonth = lastDay.getDate()

  // Get starting day of week (Monday = 0)
  let startingDay = firstDay.getDay() - 1
  if (startingDay < 0) startingDay = 6

  const today = new Date()
  const todayDate = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  const isToday = (day: number) =>
    today.getFullYear() === year && today.getMonth() === month && today.getDate() === day
  const isPast = (day: number) => {
    const cellDate = new Date(year, month, day)
    return cellDate < todayDate
  }

  const dayNames = i18n.language === 'cs'
    ? ['Po', 'Út', 'St', 'Čt', 'Pá', 'So', 'Ne']
    : ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

  const weeks: (number | null)[][] = []
  let currentWeek: (number | null)[] = []

  // Add empty cells for days before first day of month
  for (let i = 0; i < startingDay; i++) {
    currentWeek.push(null)
  }

  // Add days of month
  for (let day = 1; day <= daysInMonth; day++) {
    currentWeek.push(day)
    if (currentWeek.length === 7) {
      weeks.push(currentWeek)
      currentWeek = []
    }
  }

  // Fill remaining cells in last week
  while (currentWeek.length > 0 && currentWeek.length < 7) {
    currentWeek.push(null)
  }
  if (currentWeek.length > 0) {
    weeks.push(currentWeek)
  }

  const MAX_VISIBLE_SLOTS = isDesktop ? 4 : 2

  // Format month/year
  const formatMonthYear = (date: Date) => {
    return date.toLocaleDateString(i18n.language, { month: 'long', year: 'numeric' })
      .replace(/^\w/, c => c.toUpperCase())
  }

  // Render slot mini-block
  const renderSlotMiniBlock = (slot: MonthSlotInfo) => {
    let bgColor = ''
    let textColor = ''

    if (slot.isCancelled) {
      bgColor = 'bg-red-100 dark:bg-red-900/30'
      textColor = 'text-red-700 dark:text-red-300'
    } else if (slot.isLocked) {
      bgColor = 'bg-neutral-200 dark:bg-neutral-700'
      textColor = 'text-neutral-600 dark:text-neutral-400'
    } else if (slot.isMyReservation) {
      bgColor = 'bg-primary-500 dark:bg-primary-600'
      textColor = 'text-white'
    } else if (slot.isReserved) {
      bgColor = 'bg-primary-500 dark:bg-primary-600'
      textColor = 'text-white'
    } else {
      bgColor = 'bg-primary-100 dark:bg-primary-900/40'
      textColor = 'text-primary-700 dark:text-primary-300'
    }

    const displayText = slot.label ? `${slot.time} ${slot.label}` : slot.time

    return (
      <div
        className={`w-full rounded truncate ${bgColor} ${textColor} ${
          isDesktop
            ? 'px-2 py-1 text-xs leading-normal'
            : 'px-1 py-0.5 text-[7px] leading-tight'
        }`}
      >
        {displayText}
      </div>
    )
  }

  return (
    <div className={`flex flex-col bg-white dark:bg-dark-surface relative overflow-hidden ${isDesktop ? 'min-h-[700px]' : 'h-full'}`}>
      {/* Month navigation header */}
      <div className={`flex items-center justify-between border-b border-neutral-200 dark:border-neutral-700 flex-shrink-0 ${isDesktop ? 'px-6 py-4' : 'px-4 py-3'}`}>
        <button
          onClick={onPrevMonth}
          className="p-2 rounded-lg hover:bg-neutral-100 dark:hover:bg-dark-surfaceHover"
        >
          <ChevronLeft size={isDesktop ? 28 : 24} className="text-neutral-600 dark:text-neutral-300" />
        </button>
        <div className="flex items-center gap-3">
          <h2 className={`font-bold text-neutral-900 dark:text-white ${isDesktop ? 'text-xl' : 'text-lg'}`}>
            {formatMonthYear(monthViewDate)}
          </h2>
          <button
            onClick={onToday}
            className={`font-medium text-primary-600 dark:text-primary-400 bg-primary-50 dark:bg-primary-900/30 rounded-md hover:bg-primary-100 dark:hover:bg-primary-900/50 transition-colors ${isDesktop ? 'px-3 py-1.5 text-sm' : 'px-2 py-1 text-xs'}`}
          >
            {t('calendar.today')}
          </button>
          {isDesktop && onBackToWeek && (
            <button
              onClick={onBackToWeek}
              className="px-3 py-1.5 text-sm font-medium text-neutral-600 dark:text-neutral-400 bg-neutral-100 dark:bg-neutral-800 rounded-md hover:bg-neutral-200 dark:hover:bg-neutral-700 transition-colors"
            >
              {i18n.language === 'cs' ? 'Týden' : 'Week'}
            </button>
          )}
        </div>
        <button
          onClick={onNextMonth}
          className="p-2 rounded-lg hover:bg-neutral-100 dark:hover:bg-dark-surfaceHover"
        >
          <ChevronRight size={isDesktop ? 28 : 24} className="text-neutral-600 dark:text-neutral-300" />
        </button>
      </div>

      {/* Day names header */}
      <div className="grid grid-cols-7 border-b border-neutral-200 dark:border-neutral-700 flex-shrink-0">
        {dayNames.map((dayName, index) => (
          <div
            key={dayName}
            className={`text-center font-medium ${
              index >= 5 ? 'text-neutral-400' : 'text-neutral-600 dark:text-neutral-400'
            } ${isDesktop ? 'py-3 text-sm' : 'py-2 text-xs'}`}
          >
            {dayName}
          </div>
        ))}
      </div>

      {/* Calendar grid */}
      <div className="flex-1 grid overflow-hidden" style={{ gridTemplateRows: `repeat(${weeks.length}, 1fr)` }}>
        {weeks.map((week, weekIndex) => (
          <div key={weekIndex} className="grid grid-cols-7 border-b border-neutral-100 dark:border-neutral-800 last:border-b-0">
            {week.map((day, dayIndex) => {
              const slots = day ? getSlotsForDay(year, month, day) : []
              const visibleSlots = slots.slice(0, MAX_VISIBLE_SLOTS)
              const remainingCount = slots.length - MAX_VISIBLE_SLOTS
              const dayIsPast = day ? isPast(day) : false
              const dayIsToday = day ? isToday(day) : false

              return (
                <button
                  key={dayIndex}
                  onClick={() => day && onDayClick(day)}
                  disabled={!day || isTransitioning}
                  className={`
                    relative flex flex-col items-center h-full overflow-hidden transition-all duration-300 border-r border-neutral-100 dark:border-neutral-800 last:border-r-0
                    ${isDesktop ? 'p-2' : 'p-1'}
                    ${day ? 'hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover active:bg-neutral-100 dark:active:bg-neutral-700' : ''}
                    ${dayIsToday ? 'bg-primary-50/50 dark:bg-primary-900/20' : ''}
                    ${dayIsPast ? 'bg-neutral-100/50 dark:bg-neutral-800/50' : ''}
                    ${dayIndex >= 5 && !dayIsPast ? 'bg-neutral-50/30 dark:bg-neutral-800/20' : ''}
                    ${day !== null && transitionDay === day ? 'z-50 scale-110 bg-primary-500 rounded-lg shadow-xl' : ''}
                    ${isTransitioning && day !== null && transitionDay !== day ? 'opacity-0 scale-75' : ''}
                  `}
                >
                  {day && (
                    <>
                      {/* Day number */}
                      <span className={`transition-all duration-300 ${
                        isDesktop ? 'text-base mb-1' : 'text-sm mb-0.5'
                      } ${
                        transitionDay === day
                          ? 'font-bold text-white'
                          : dayIsToday
                            ? 'font-bold text-primary-600 dark:text-primary-400'
                            : dayIsPast
                              ? 'text-neutral-400 dark:text-neutral-500'
                              : dayIndex >= 5
                                ? 'text-neutral-400'
                                : 'text-neutral-900 dark:text-white'
                      }`}>
                        {day}
                      </span>

                      {/* Slot mini-blocks */}
                      {!isTransitioning && slots.length > 0 && (
                        <div className={`w-full flex flex-col ${isDesktop ? 'gap-1' : 'gap-0.5'}`}>
                          {visibleSlots.map((slot, idx) => (
                            <div key={idx}>
                              {renderSlotMiniBlock(slot)}
                            </div>
                          ))}
                          {/* "+N" indicator for remaining slots */}
                          {remainingCount > 0 && (
                            <div className={`font-medium text-primary-500 dark:text-primary-400 text-center ${isDesktop ? 'text-xs' : 'text-[8px]'}`}>
                              +{remainingCount}
                            </div>
                          )}
                        </div>
                      )}
                    </>
                  )}
                </button>
              )
            })}
          </div>
        ))}
      </div>
    </div>
  )
}
