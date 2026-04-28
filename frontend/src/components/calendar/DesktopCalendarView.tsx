import { useRef, useState, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { CreditCard, Lock, Unlock, LayoutTemplate } from 'lucide-react'
import type { CalendarSlot } from '@/components/ui'
import { Card, Button, Spinner } from '@/components/ui'
import { CalendarLegend } from './CalendarLegend'
import { MonthCalendarGrid } from './MonthCalendarGrid'
import { DesktopTimeGrid, type DesktopTimeGridRef } from './desktop/DesktopTimeGrid'
import type { MonthSlotInfo } from '@/types/calendar'
import type { TrainingLocation } from '@/types/api'

interface DesktopCalendarViewProps {
  slots: CalendarSlot[]
  currentDate: Date
  calendarSettings: { calendarStartHour?: number; calendarEndHour?: number } | undefined
  locations?: TrainingLocation[]
  isAdmin: boolean
  userCredits: number
  isLoading: boolean
  isFetching: boolean
  isViewLocked: boolean
  getSlotsForDay: (year: number, month: number, day: number) => MonthSlotInfo[]
  onSlotClick: (slot: CalendarSlot) => void
  onDateClick?: (date: string, time: string) => void
  onSlotDrop?: (slot: CalendarSlot, newDate: string, newStartTime: string) => void
  onDatesChange: (start: string, end: string, startDate: Date) => void
  onLockToggle: () => void
  onTemplateClick: () => void
  onUnlockWeek: () => void
  unlockWeekLoading: boolean
}

export function DesktopCalendarView({
  slots,
  currentDate,
  calendarSettings,
  locations,
  isAdmin,
  userCredits,
  isLoading,
  isFetching,
  isViewLocked,
  getSlotsForDay,
  onSlotClick,
  onDateClick,
  onSlotDrop,
  onDatesChange,
  onLockToggle,
  onTemplateClick,
  onUnlockWeek,
  unlockWeekLoading,
}: DesktopCalendarViewProps) {
  const { t } = useTranslation()
  const timeGridRef = useRef<DesktopTimeGridRef>(null)

  const [showMonthView, setShowMonthView] = useState(false)
  const [monthViewDate, setMonthViewDate] = useState(new Date())
  const [isTransitioning, setIsTransitioning] = useState(false)
  const [transitionDay, setTransitionDay] = useState<number | null>(null)
  // Track current date from time grid
  const [internalDate, setInternalDate] = useState(currentDate)
  // Pending navigation from month view (applied when DesktopTimeGrid mounts)
  const [pendingNavDate, setPendingNavDate] = useState<Date | null>(null)
  const showEmptyWeek = isAdmin && !showMonthView && slots.length === 0 && !isLoading

  // Fetch data for entire month
  const fetchMonthRange = useCallback((date: Date) => {
    const year = date.getFullYear()
    const month = date.getMonth()
    const firstDay = new Date(year, month, 1)
    const lastDay = new Date(year, month + 1, 0)
    const pad = (n: number) => String(n).padStart(2, '0')
    const start = `${year}-${pad(month + 1)}-01`
    const end = `${year}-${pad(month + 1)}-${pad(lastDay.getDate())}`
    onDatesChange(start, end, firstDay)
  }, [onDatesChange])

  // Month view handlers
  const handlePrevMonth = useCallback(() => {
    setMonthViewDate(prev => {
      const d = new Date(prev)
      d.setMonth(d.getMonth() - 1)
      fetchMonthRange(d)
      return d
    })
  }, [fetchMonthRange])

  const handleNextMonth = useCallback(() => {
    setMonthViewDate(prev => {
      const d = new Date(prev)
      d.setMonth(d.getMonth() + 1)
      fetchMonthRange(d)
      return d
    })
  }, [fetchMonthRange])

  const handleMonthToday = useCallback(() => {
    const today = new Date()
    setMonthViewDate(today)
    fetchMonthRange(today)
  }, [fetchMonthRange])

  const handleMonthDayClick = useCallback((day: number) => {
    const selectedDate = new Date(monthViewDate.getFullYear(), monthViewDate.getMonth(), day)
    setTransitionDay(day)
    setIsTransitioning(true)

    setTimeout(() => {
      setPendingNavDate(selectedDate)
      setShowMonthView(false)
      setIsTransitioning(false)
      setTransitionDay(null)
    }, 300)
  }, [monthViewDate])

  const handleBackToWeek = useCallback(() => {
    setShowMonthView(false)
  }, [])

  const openMonthView = useCallback(() => {
    setMonthViewDate(internalDate)
    setShowMonthView(true)
    fetchMonthRange(internalDate)
  }, [internalDate, fetchMonthRange])

  const handleDatesChange = useCallback((start: string, end: string, startDate: Date) => {
    setInternalDate(startDate)
    onDatesChange(start, end, startDate)
  }, [onDatesChange])

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {isAdmin ? t('admin.calendar') : t('reservation.title')}
        </h1>
        <div className="flex flex-wrap items-center gap-2">
          {!isAdmin && (
            <div className="flex items-center gap-2 px-3 py-2 bg-primary-50 dark:bg-primary-900/20 rounded-lg">
              <CreditCard size={18} className="text-primary-500" />
              <span className="text-sm text-primary-700 dark:text-primary-300">
                {t('home.yourCredits')}: <strong>{userCredits}</strong>
              </span>
            </div>
          )}

          {isAdmin && (
            <>
              <button
                onClick={onLockToggle}
                className={`flex items-center gap-2 px-3 py-1.5 rounded-full transition-all duration-200 select-none ${
                  isViewLocked
                    ? 'bg-primary-500 text-white shadow-md'
                    : 'bg-neutral-100 dark:bg-dark-surface text-neutral-600 dark:text-neutral-400 hover:bg-neutral-200 dark:hover:bg-dark-surfaceHover'
                }`}
              >
                <div className={`relative w-8 h-4 rounded-full transition-colors duration-200 ${
                  isViewLocked ? 'bg-primary-300' : 'bg-neutral-300 dark:bg-neutral-600'
                }`}>
                  <div className={`absolute top-0.5 w-3 h-3 rounded-full bg-white shadow-sm transition-transform duration-200 ${
                    isViewLocked ? 'translate-x-4' : 'translate-x-0.5'
                  }`} />
                </div>
                <Lock size={14} />
                <span className="text-sm font-medium">{t('calendar.lock')}</span>
              </button>

              <Button variant="secondary" size="sm" onClick={onTemplateClick}>
                <LayoutTemplate size={16} className="mr-1" />
                {t('calendar.template')}
              </Button>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => onUnlockWeek()}
                isLoading={unlockWeekLoading}
              >
                <Unlock size={16} className="mr-1" />
                {t('calendar.unlockWeek')}
              </Button>
            </>
          )}
        </div>
      </div>

      {/* Legend */}
      <CalendarLegend isAdmin={isAdmin} locations={locations} />

      {/* Calendar Card — `overflow-clip` keeps the rounded corners visually
          tight while, unlike `overflow-hidden`, not creating a scrolling
          context that would break the sticky day-of-week header. */}
      <Card variant="bordered" padding="none" className="[overflow:clip] rounded-xl">
        {isLoading ? (
          <div className="flex justify-center py-12">
            <Spinner size="lg" />
          </div>
        ) : showMonthView ? (
          <div className="p-2">
            <MonthCalendarGrid
              monthViewDate={monthViewDate}
              isDesktop={true}
              isAdmin={isAdmin}
              isTransitioning={isTransitioning}
              transitionDay={transitionDay}
              getSlotsForDay={getSlotsForDay}
              onPrevMonth={handlePrevMonth}
              onNextMonth={handleNextMonth}
              onToday={handleMonthToday}
              onDayClick={handleMonthDayClick}
              onBackToWeek={handleBackToWeek}
            />
          </div>
        ) : (
          <>
            {showEmptyWeek && (
              <div className="m-3 flex flex-col gap-3 rounded-lg border border-dashed border-primary-200 bg-primary-50/70 p-4 dark:border-primary-900/60 dark:bg-primary-900/20 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="font-medium text-primary-900 dark:text-primary-100">
                    {t('calendar.emptyWeekTitle', 'Tento týden nemá žádné sloty')}
                  </p>
                  <p className="mt-1 text-sm text-primary-700/80 dark:text-primary-200/80">
                    {t('calendar.emptyWeekDescription', 'Začněte šablonou nebo odemkněte pracovní dobu pro tento týden.')}
                  </p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Button variant="secondary" size="sm" onClick={onTemplateClick}>
                    <LayoutTemplate size={16} className="mr-1" />
                    {t('calendar.template')}
                  </Button>
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => onUnlockWeek()}
                    isLoading={unlockWeekLoading}
                  >
                    <Unlock size={16} className="mr-1" />
                    {t('calendar.unlockWeek')}
                  </Button>
                </div>
              </div>
            )}
            <DesktopTimeGrid
              ref={timeGridRef}
              slots={slots}
              initialDate={pendingNavDate}
              startHour={calendarSettings?.calendarStartHour ?? 6}
              endHour={calendarSettings?.calendarEndHour ?? 22}
              isAdmin={isAdmin}
              editable={isAdmin && !isViewLocked}
              isFetching={isFetching}
              onSlotClick={onSlotClick}
              onDateClick={isAdmin ? onDateClick : undefined}
              onSlotDrop={isAdmin ? onSlotDrop : undefined}
              onDatesChange={handleDatesChange}
              onMonthView={openMonthView}
            />
          </>
        )}
      </Card>
    </div>
  )
}
