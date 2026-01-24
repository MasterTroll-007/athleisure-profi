import { useRef, useState, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { MoreVertical, Check, Calendar, Lock, Unlock, LayoutTemplate } from 'lucide-react'
import { InfiniteScrollCalendar, Spinner } from '@/components/ui'
import type { CalendarSlot, InfiniteScrollCalendarRef } from '@/components/ui'
import { MonthCalendarGrid } from './MonthCalendarGrid'
import type { MonthSlotInfo } from '@/types/calendar'

// Format date to ISO string (YYYY-MM-DD) using local timezone
const formatDateLocal = (date: Date): string => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

interface MobileCalendarViewProps {
  calendarSlots: CalendarSlot[]
  currentDate: Date
  calendarSettings: { calendarStartHour?: number; calendarEndHour?: number } | undefined
  isAdmin: boolean
  isLoading: boolean
  isFetching: boolean
  isViewLocked: boolean
  getSlotsForDay: (year: number, month: number, day: number) => MonthSlotInfo[]
  onSlotClick: (slot: CalendarSlot) => void
  onDateClick?: (date: string, time: string) => void
  onDateRangeChange: (start: string, end: string) => void
  onCurrentDateChange: (date: Date) => void
  onLockToggle: () => void
  onTemplateClick: () => void
  onUnlockWeek: () => void
  unlockWeekLoading: boolean
}

export function MobileCalendarView({
  calendarSlots,
  currentDate,
  calendarSettings,
  isAdmin,
  isLoading,
  isFetching,
  isViewLocked,
  getSlotsForDay,
  onSlotClick,
  onDateClick,
  onDateRangeChange,
  onCurrentDateChange,
  onLockToggle,
  onTemplateClick,
  onUnlockWeek,
  unlockWeekLoading,
}: MobileCalendarViewProps) {
  const { t, i18n } = useTranslation()
  const infiniteCalendarRef = useRef<InfiniteScrollCalendarRef>(null)
  const mobileMenuRef = useRef<HTMLDivElement>(null)

  const [viewDays, setViewDays] = useState(3)
  const [showMobileMenu, setShowMobileMenu] = useState(false)
  const [showMonthView, setShowMonthView] = useState(false)
  const [monthViewDate, setMonthViewDate] = useState(new Date())
  const [isTransitioning, setIsTransitioning] = useState(false)
  const [transitionDay, setTransitionDay] = useState<number | null>(null)

  // Format month/year for mobile header
  const formatMonthYear = (date: Date) => {
    return date.toLocaleDateString(i18n.language, { month: 'long', year: 'numeric' })
      .replace(/^\w/, c => c.toUpperCase())
  }

  // Handle "Today" button click
  const handleScrollToToday = useCallback(() => {
    onCurrentDateChange(new Date())
    infiniteCalendarRef.current?.scrollToToday()
  }, [onCurrentDateChange])

  // Handle day click in month view - zoom to that day with transition
  const handleMonthDayClick = useCallback((day: number) => {
    const selectedDate = new Date(monthViewDate.getFullYear(), monthViewDate.getMonth(), day)

    // Start transition animation
    setTransitionDay(day)
    setIsTransitioning(true)

    // After animation, switch to day view
    setTimeout(() => {
      onCurrentDateChange(selectedDate)
      setViewDays(1)
      setShowMonthView(false)
      setIsTransitioning(false)
      setTransitionDay(null)
    }, 300)
  }, [monthViewDate, onCurrentDateChange])

  // Handle month navigation
  const handlePrevMonth = useCallback(() => {
    setMonthViewDate(prev => {
      const newDate = new Date(prev)
      newDate.setMonth(newDate.getMonth() - 1)
      const firstDay = new Date(newDate.getFullYear(), newDate.getMonth(), 1)
      const lastDay = new Date(newDate.getFullYear(), newDate.getMonth() + 1, 0)
      onDateRangeChange(formatDateLocal(firstDay), formatDateLocal(lastDay))
      return newDate
    })
  }, [onDateRangeChange])

  const handleNextMonth = useCallback(() => {
    setMonthViewDate(prev => {
      const newDate = new Date(prev)
      newDate.setMonth(newDate.getMonth() + 1)
      const firstDay = new Date(newDate.getFullYear(), newDate.getMonth(), 1)
      const lastDay = new Date(newDate.getFullYear(), newDate.getMonth() + 1, 0)
      onDateRangeChange(formatDateLocal(firstDay), formatDateLocal(lastDay))
      return newDate
    })
  }, [onDateRangeChange])

  const handleMonthToday = useCallback(() => {
    const today = new Date()
    setMonthViewDate(today)
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1)
    const lastDay = new Date(today.getFullYear(), today.getMonth() + 1, 0)
    onDateRangeChange(formatDateLocal(firstDay), formatDateLocal(lastDay))
  }, [onDateRangeChange])

  const handleCalendarDateRangeChange = useCallback((start: string, end: string) => {
    onDateRangeChange(start, end)
  }, [onDateRangeChange])

  return (
    <div className="flex flex-col h-[calc(100vh-112px)] -mx-4 -my-6">
      {/* Mobile header - hidden in month view */}
      {!showMonthView && (
        <div className="flex items-center justify-between px-3 py-2 bg-neutral-50 dark:bg-dark-surface border-b border-neutral-200 dark:border-neutral-700">
          <div className="flex items-center gap-2">
            <h2 className="text-lg font-bold text-neutral-900 dark:text-white">
              {formatMonthYear(currentDate)}
            </h2>
            <button
              onClick={handleScrollToToday}
              className="px-2 py-1 text-xs font-medium text-primary-600 dark:text-primary-400 bg-primary-50 dark:bg-primary-900/30 rounded-md hover:bg-primary-100 dark:hover:bg-primary-900/50 transition-colors"
            >
              {t('calendar.today')}
            </button>
          </div>

          {/* Three-dot menu */}
          <div className="relative" ref={mobileMenuRef}>
            <button
              onClick={() => setShowMobileMenu(!showMobileMenu)}
              className="p-2 rounded-lg hover:bg-neutral-200 dark:hover:bg-dark-surfaceHover transition-colors"
            >
              <MoreVertical size={24} className="text-neutral-600 dark:text-neutral-300" />
            </button>

            {/* Dropdown menu */}
            {showMobileMenu && (
              <div className="absolute right-0 top-full mt-1 w-56 bg-white dark:bg-dark-surface rounded-lg shadow-lg border border-neutral-200 dark:border-neutral-700 z-50 py-1">
                {/* View mode options */}
                <div className="px-3 py-1.5 text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase">
                  {t('calendar.view')}
                </div>
                {/* Month view option */}
                <button
                  onClick={() => {
                    const monthDate = currentDate
                    const firstDay = new Date(monthDate.getFullYear(), monthDate.getMonth(), 1)
                    const lastDay = new Date(monthDate.getFullYear(), monthDate.getMonth() + 1, 0)
                    onDateRangeChange(formatDateLocal(firstDay), formatDateLocal(lastDay))
                    setShowMonthView(true)
                    setMonthViewDate(monthDate)
                    setShowMobileMenu(false)
                  }}
                  className="w-full flex items-center gap-3 px-3 py-2 text-left hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover transition-colors"
                >
                  {showMonthView ? (
                    <Check size={16} className="text-primary-500" />
                  ) : (
                    <span className="w-4" />
                  )}
                  <Calendar size={16} className="text-neutral-500" />
                  <span className="text-sm text-neutral-700 dark:text-neutral-300">{t('calendar.month')}</span>
                </button>
                <div className="h-px bg-neutral-100 dark:bg-neutral-800 mx-3" />
                {[
                  { days: 1, label: i18n.language === 'cs' ? '1 den' : '1 day' },
                  { days: 3, label: i18n.language === 'cs' ? '3 dny' : '3 days' },
                  { days: 5, label: i18n.language === 'cs' ? '5 dnů' : '5 days' },
                  { days: 7, label: i18n.language === 'cs' ? '7 dnů' : '7 days' },
                ].map(({ days, label }) => (
                  <button
                    key={days}
                    onClick={() => { setViewDays(days); setShowMonthView(false); setShowMobileMenu(false) }}
                    className="w-full flex items-center gap-3 px-3 py-2 text-left hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover transition-colors"
                  >
                    {!showMonthView && viewDays === days ? (
                      <Check size={16} className="text-primary-500" />
                    ) : (
                      <span className="w-4" />
                    )}
                    <span className="text-sm text-neutral-700 dark:text-neutral-300">{label}</span>
                  </button>
                ))}

                {/* Admin options */}
                {isAdmin && (
                  <>
                    <div className="h-px bg-neutral-200 dark:bg-neutral-700 my-1" />

                    {/* Lock toggle */}
                    <button
                      onClick={() => { onLockToggle(); setShowMobileMenu(false) }}
                      className="w-full flex items-center gap-3 px-3 py-2 text-left hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover transition-colors"
                    >
                      {isViewLocked ? <Lock size={16} /> : <Unlock size={16} />}
                      <span className="text-sm text-neutral-700 dark:text-neutral-300">
                        {isViewLocked ? t('calendar.unlockDrag') : t('calendar.lockDrag')}
                      </span>
                    </button>

                    {/* Template */}
                    <button
                      onClick={() => { onTemplateClick(); setShowMobileMenu(false) }}
                      className="w-full flex items-center gap-3 px-3 py-2 text-left hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover transition-colors"
                    >
                      <LayoutTemplate size={16} />
                      <span className="text-sm text-neutral-700 dark:text-neutral-300">{t('calendar.template')}</span>
                    </button>

                    {/* Unlock week */}
                    <button
                      onClick={() => { onUnlockWeek(); setShowMobileMenu(false) }}
                      className="w-full flex items-center gap-3 px-3 py-2 text-left hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover transition-colors"
                      disabled={unlockWeekLoading}
                    >
                      <Unlock size={16} />
                      <span className="text-sm text-neutral-700 dark:text-neutral-300">{t('calendar.unlockWeek')}</span>
                    </button>
                  </>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Calendar takes remaining space */}
      <div className="flex-1 overflow-hidden">
        {isLoading ? (
          <div className="flex justify-center items-center h-full">
            <Spinner size="lg" />
          </div>
        ) : showMonthView ? (
          <MonthCalendarGrid
            monthViewDate={monthViewDate}
            isDesktop={false}
            isAdmin={isAdmin}
            isTransitioning={isTransitioning}
            transitionDay={transitionDay}
            getSlotsForDay={getSlotsForDay}
            onPrevMonth={handlePrevMonth}
            onNextMonth={handleNextMonth}
            onToday={handleMonthToday}
            onDayClick={handleMonthDayClick}
          />
        ) : (
          <InfiniteScrollCalendar
            ref={infiniteCalendarRef}
            slots={calendarSlots}
            initialDate={currentDate}
            viewDays={viewDays}
            startHour={calendarSettings?.calendarStartHour ?? 6}
            endHour={calendarSettings?.calendarEndHour ?? 22}
            onSlotClick={onSlotClick}
            onDateClick={isAdmin ? onDateClick : undefined}
            onDateRangeChange={handleCalendarDateRangeChange}
            isAdmin={isAdmin}
            isLoading={isFetching}
          />
        )}
      </div>
    </div>
  )
}
