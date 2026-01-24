import { useRef, useState, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { CreditCard, Lock, Unlock, LayoutTemplate } from 'lucide-react'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import csLocale from '@fullcalendar/core/locales/cs'
import type { EventClickArg } from '@fullcalendar/core'
import { Card, Button, Spinner } from '@/components/ui'
import { CalendarLegend } from './CalendarLegend'
import { MonthCalendarGrid } from './MonthCalendarGrid'
import type { CalendarEvent, DateClickArg, EventDropArg, MonthSlotInfo } from '@/types/calendar'

interface DesktopCalendarViewProps {
  events: CalendarEvent[]
  currentDate: Date
  calendarSettings: { calendarStartHour?: number; calendarEndHour?: number } | undefined
  isAdmin: boolean
  userCredits: number
  isLoading: boolean
  isFetching: boolean
  isViewLocked: boolean
  getSlotsForDay: (year: number, month: number, day: number) => MonthSlotInfo[]
  onEventClick: (info: EventClickArg) => void
  onDateClick?: (info: DateClickArg) => void
  onEventDrop?: (info: EventDropArg) => void
  onDatesSet: (info: { startStr: string; endStr: string; start: Date; view: { type: string } }) => void
  onLockToggle: () => void
  onTemplateClick: () => void
  onUnlockWeek: () => void
  unlockWeekLoading: boolean
}

export function DesktopCalendarView({
  events,
  currentDate,
  calendarSettings,
  isAdmin,
  userCredits,
  isLoading,
  isFetching,
  isViewLocked,
  getSlotsForDay,
  onEventClick,
  onDateClick,
  onEventDrop,
  onDatesSet,
  onLockToggle,
  onTemplateClick,
  onUnlockWeek,
  unlockWeekLoading,
}: DesktopCalendarViewProps) {
  const { t, i18n } = useTranslation()
  const calendarRef = useRef<FullCalendar>(null)

  const [showMonthView, setShowMonthView] = useState(false)
  const [monthViewDate, setMonthViewDate] = useState(new Date())
  const [isTransitioning, setIsTransitioning] = useState(false)
  const [transitionDay, setTransitionDay] = useState<number | null>(null)

  // Detect if mobile for default view
  const isMobile = typeof window !== 'undefined' && window.innerWidth < 640

  // Load saved calendar view from localStorage
  const savedView = typeof window !== 'undefined' ? localStorage.getItem('calendarView') : null
  const initialCalendarView = savedView && ['timeGridDay', 'timeGrid3Day', 'timeGrid5Day', 'timeGridWeek'].includes(savedView)
    ? savedView
    : (isMobile ? 'timeGrid3Day' : 'timeGridWeek')

  // Handle dates set from FullCalendar
  const handleDatesSet = useCallback((info: { startStr: string; endStr: string; start: Date; view: { type: string } }) => {
    // Save view type to localStorage
    if (info.view?.type) {
      localStorage.setItem('calendarView', info.view.type)
    }
    onDatesSet(info)
  }, [onDatesSet])

  // Month view handlers
  const handlePrevMonth = useCallback(() => {
    setMonthViewDate(prev => {
      const newDate = new Date(prev)
      newDate.setMonth(newDate.getMonth() - 1)
      return newDate
    })
  }, [])

  const handleNextMonth = useCallback(() => {
    setMonthViewDate(prev => {
      const newDate = new Date(prev)
      newDate.setMonth(newDate.getMonth() + 1)
      return newDate
    })
  }, [])

  const handleMonthToday = useCallback(() => {
    setMonthViewDate(new Date())
  }, [])

  const handleMonthDayClick = useCallback((day: number) => {
    const selectedDate = new Date(monthViewDate.getFullYear(), monthViewDate.getMonth(), day)

    // Start transition animation
    setTransitionDay(day)
    setIsTransitioning(true)

    // After animation, switch to day view in FullCalendar
    setTimeout(() => {
      setShowMonthView(false)
      setIsTransitioning(false)
      setTransitionDay(null)
      // Navigate FullCalendar to the selected date
      const calendarApi = calendarRef.current?.getApi()
      if (calendarApi) {
        calendarApi.gotoDate(selectedDate)
        calendarApi.changeView('timeGridDay')
      }
    }, 300)
  }, [monthViewDate])

  const handleBackToWeek = useCallback(() => {
    setShowMonthView(false)
  }, [])

  // Open month view
  const openMonthView = useCallback(() => {
    const monthDate = currentDate
    setMonthViewDate(monthDate)
    setShowMonthView(true)
  }, [currentDate])

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {isAdmin ? t('admin.calendar') : t('reservation.title')}
        </h1>
        <div className="flex flex-wrap items-center gap-2">
          {/* Credits display for users */}
          {!isAdmin && (
            <div className="flex items-center gap-2 px-3 py-2 bg-primary-50 dark:bg-primary-900/20 rounded-lg">
              <CreditCard size={18} className="text-primary-500" />
              <span className="text-sm text-primary-700 dark:text-primary-300">
                {t('home.yourCredits')}: <strong>{userCredits}</strong>
              </span>
            </div>
          )}

          {/* Admin toolbar */}
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
      <CalendarLegend isAdmin={isAdmin} />

      {/* Calendar Card */}
      <Card variant="bordered" padding="none" className="overflow-hidden rounded-xl">
        {isLoading ? (
          <div className="flex justify-center py-12">
            <Spinner size="lg" />
          </div>
        ) : showMonthView ? (
          /* Desktop: Month View */
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
          /* Desktop: FullCalendar */
          <div
            className={`p-1 md:p-4 ${isAdmin ? '[&_.fc-timegrid-slot]:!h-4' : '[&_.fc-timegrid-slot]:!h-12'} md:[&_.fc-timegrid-slot]:!h-5 [&_.fc-timegrid-axis]:!w-10 md:[&_.fc-timegrid-axis]:!w-14 [&_.fc-timegrid-slot-label]:!text-[10px] md:[&_.fc-timegrid-slot-label]:!text-xs [&_.fc-col-header-cell]:!text-[10px] md:[&_.fc-col-header-cell]:!text-xs transition-opacity ${isFetching ? 'opacity-60' : ''}`}
          >
            <FullCalendar
              ref={calendarRef}
              plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
              initialView={initialCalendarView}
              initialDate={currentDate}
              locale={i18n.language === 'cs' ? csLocale : undefined}
              firstDay={1}
              customButtons={{
                monthView: {
                  text: i18n.language === 'cs' ? 'Měsíc' : 'Month',
                  click: openMonthView,
                },
              }}
              views={{
                timeGridDay: {
                  type: 'timeGrid',
                  duration: { days: 1 },
                  dateIncrement: { days: 1 },
                  buttonText: i18n.language === 'cs' ? '1 den' : '1 day',
                },
                timeGrid3Day: {
                  type: 'timeGrid',
                  duration: { days: 3 },
                  dateIncrement: { days: 1 },
                  buttonText: i18n.language === 'cs' ? '3 dny' : '3 days',
                },
                timeGrid5Day: {
                  type: 'timeGrid',
                  duration: { days: 5 },
                  dateIncrement: { days: 1 },
                  buttonText: i18n.language === 'cs' ? '5 dnů' : '5 days',
                },
                timeGridWeek: {
                  type: 'timeGrid',
                  duration: { weeks: 1 },
                  dateIncrement: { days: 1 },
                  buttonText: i18n.language === 'cs' ? '7 dnů' : '7 days',
                },
              }}
              headerToolbar={{
                left: 'prev,next today',
                center: 'title',
                right: 'monthView,timeGridWeek,timeGrid5Day,timeGrid3Day,timeGridDay',
              }}
              events={events}
              eventClick={onEventClick}
              dateClick={isAdmin ? onDateClick as any : undefined}
              eventDrop={isAdmin ? onEventDrop as any : undefined}
              datesSet={handleDatesSet}
              editable={isAdmin && !isViewLocked}
              droppable={isAdmin && !isViewLocked}
              slotMinTime={`${(calendarSettings?.calendarStartHour ?? 6).toString().padStart(2, '0')}:00:00`}
              slotMaxTime={`${(calendarSettings?.calendarEndHour ?? 22).toString().padStart(2, '0')}:00:00`}
              allDaySlot={false}
              weekends={true}
              nowIndicator={true}
              eventDisplay="block"
              height="auto"
              slotDuration={isAdmin ? '00:15:00' : '01:00:00'}
              snapDuration={isAdmin ? '00:15:00' : undefined}
              slotLabelInterval={isAdmin ? undefined : '01:00:00'}
              selectable={isAdmin && !isViewLocked}
              longPressDelay={300}
              eventLongPressDelay={300}
              selectLongPressDelay={300}
              eventContent={(eventInfo) => {
                const title = eventInfo.event.title
                const lines = title.split('\n')
                return (
                  <div className="p-1 text-xs overflow-hidden cursor-pointer">
                    {lines.map((line, idx) => (
                      <div key={idx} className={idx === 0 ? 'font-medium truncate' : 'truncate'}>{line}</div>
                    ))}
                  </div>
                )
              }}
            />
          </div>
        )}
      </Card>
    </div>
  )
}
