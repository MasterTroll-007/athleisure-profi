import { useState, useCallback, useMemo, useRef, forwardRef, useImperativeHandle } from 'react'
import { useTranslation } from 'react-i18next'
import type { CalendarSlot } from '@/components/ui'
import { TimeGridToolbar } from './TimeGridToolbar'
import { TimeGridBody, type TimeGridBodyRef } from './TimeGridBody'
import { useTimeGrid } from './useTimeGrid'
import { useSlideAnimation } from './useSlideAnimation'
import { useDragDrop } from './useDragDrop'
import { formatDateISO, computeDays, getStartDate } from './constants'

interface DesktopTimeGridProps {
  slots: CalendarSlot[]
  initialDate?: Date | null
  startHour: number
  endHour: number
  isAdmin: boolean
  editable: boolean
  isFetching: boolean
  onSlotClick: (slot: CalendarSlot) => void
  onDateClick?: (date: string, time: string) => void
  onSlotDrop?: (slot: CalendarSlot, newDate: string, newStartTime: string) => void
  onDatesChange: (start: string, end: string, startDate: Date) => void
  onMonthView: () => void
}

export interface DesktopTimeGridRef {
  navigateToDate: (date: Date, view?: number) => void
}

function getInitialView(): number {
  if (typeof window === 'undefined') return 7
  const saved = localStorage.getItem('calendarView')
  if (saved && ['1', '3', '5', '7'].includes(saved)) return parseInt(saved)
  return 7
}

export const DesktopTimeGrid = forwardRef<DesktopTimeGridRef, DesktopTimeGridProps>(({
  slots,
  initialDate,
  startHour,
  endHour,
  isAdmin,
  editable,
  isFetching,
  onSlotClick,
  onDateClick,
  onSlotDrop,
  onDatesChange,
  onMonthView,
}, ref) => {
  const { i18n } = useTranslation()
  const cs = i18n.language === 'cs'

  const [viewDays, setViewDays] = useState(getInitialView)
  const [currentDays, setCurrentDays] = useState<Date[]>(() => {
    const baseDate = initialDate || new Date()
    const v = getInitialView()
    return computeDays(getStartDate(baseDate, v), v)
  })

  const bodyRef = useRef<TimeGridBodyRef>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const bodyScrollRef = useRef<HTMLDivElement>(null)
  // Ref to the actual column grid — needed so drag-drop can map pointer
  // positions to (day, time). Passed down to TimeGridBody and attached to the
  // inner flex container that spans the day columns.
  const gridRef = useRef<HTMLDivElement>(null)
  const datesReportedRef = useRef('')

  const { hourHeight, totalHeight, resolveOverlaps, timeLabels } = useTimeGrid(startHour, endHour, isAdmin)
  const slide = useSlideAnimation()

  // Report date range changes to parent
  const reportDates = useCallback((days: Date[]) => {
    const start = formatDateISO(days[0])
    const end = formatDateISO(days[days.length - 1])
    const key = `${start}_${end}`
    if (datesReportedRef.current !== key) {
      datesReportedRef.current = key
      onDatesChange(start, end, days[0])
    }
  }, [onDatesChange])

  // Report initial dates
  useMemo(() => {
    reportDates(currentDays)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Build slots map by day
  const buildSlotsByDay = useCallback((days: Date[], allSlots: CalendarSlot[]) => {
    const map = new Map<string, ReturnType<typeof resolveOverlaps>>()
    for (const day of days) {
      const dateStr = formatDateISO(day)
      const daySlots = allSlots.filter(s => s.date === dateStr)
      map.set(dateStr, resolveOverlaps(daySlots))
    }
    return map
  }, [resolveOverlaps])

  const slotsByDay = useMemo(() => buildSlotsByDay(currentDays, slots), [buildSlotsByDay, currentDays, slots])

  // Navigation with slide animation
  const navigate = useCallback((direction: 'prev' | 'next') => {
    const increment = direction === 'next' ? viewDays : -viewDays
    const newStart = new Date(currentDays[0])
    newStart.setDate(newStart.getDate() + increment)
    const newDays = computeDays(newStart, viewDays)

    slide.triggerSlide(
      direction === 'next' ? 'left' : 'right',
      () => {
        setCurrentDays(newDays)
        reportDates(newDays)
      },
    )
  }, [currentDays, viewDays, slide, reportDates])

  const goToToday = useCallback(() => {
    const start = getStartDate(new Date(), viewDays)
    const newDays = computeDays(start, viewDays)

    // Determine direction based on whether today is before or after current view
    const currentStart = currentDays[0]
    const todayStart = newDays[0]
    if (formatDateISO(currentStart) === formatDateISO(todayStart)) {
      // Already on today's page, just scroll to now
      bodyRef.current?.scrollToNow()
      return
    }

    const direction = todayStart > currentStart ? 'left' : 'right'
    slide.triggerSlide(direction, () => {
      setCurrentDays(newDays)
      reportDates(newDays)
      requestAnimationFrame(() => {
        bodyRef.current?.scrollToNow()
      })
    })
  }, [viewDays, currentDays, slide, reportDates])

  const changeView = useCallback((newViewDays: number) => {
    setViewDays(newViewDays)
    localStorage.setItem('calendarView', String(newViewDays))
    const baseDate = currentDays[0]
    const start = getStartDate(baseDate, newViewDays)
    const newDays = computeDays(start, newViewDays)
    setCurrentDays(newDays)
    reportDates(newDays)
  }, [currentDays, reportDates])

  // Imperative handle
  useImperativeHandle(ref, () => ({
    navigateToDate: (date: Date, view?: number) => {
      const v = view ?? viewDays
      if (view && view !== viewDays) {
        setViewDays(v)
        localStorage.setItem('calendarView', String(v))
      }
      const newDays = computeDays(getStartDate(date, v), v)
      setCurrentDays(newDays)
      reportDates(newDays)
      requestAnimationFrame(() => {
        bodyRef.current?.scrollToNow()
      })
    },
  }), [viewDays, reportDates])

  // Drag-drop
  const dragDrop = useDragDrop({
    enabled: editable,
    days: currentDays,
    startHour,
    hourHeight,
    containerRef,
    bodyRef: bodyScrollRef,
    gridRef,
    onDrop: onSlotDrop || (() => {}),
  })

  // Title
  const title = useMemo(() => {
    if (currentDays.length === 0) return ''
    const first = currentDays[0]
    const last = currentDays[currentDays.length - 1]
    const locale = cs ? 'cs-CZ' : 'en-US'

    if (viewDays === 1) {
      return first.toLocaleDateString(locale, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
    }

    const sameMonth = first.getMonth() === last.getMonth()
    if (sameMonth) {
      return `${first.getDate()}. – ${last.getDate()}. ${first.toLocaleDateString(locale, { month: 'long', year: 'numeric' })}`
    }
    return `${first.getDate()}. ${first.toLocaleDateString(locale, { month: 'short' })} – ${last.getDate()}. ${last.toLocaleDateString(locale, { month: 'short', year: 'numeric' })}`
  }, [currentDays, viewDays, cs])

  return (
    <div
      ref={containerRef}
      className={`flex flex-col bg-white dark:bg-dark-surface rounded-xl overflow-visible ${isFetching ? 'opacity-60' : ''}`}
      onPointerMove={dragDrop.onPointerMove}
      onPointerUp={dragDrop.onPointerUp}
    >
      <TimeGridToolbar
        title={title}
        viewDays={viewDays}
        onPrev={() => navigate('prev')}
        onNext={() => navigate('next')}
        onToday={goToToday}
        onViewChange={changeView}
        onMonthView={onMonthView}
      />

      <TimeGridBody
        ref={bodyRef}
        gridRef={gridRef}
        days={currentDays}
        slotsByDay={slotsByDay}
        startHour={startHour}
        endHour={endHour}
        hourHeight={hourHeight}
        totalHeight={totalHeight}
        timeLabels={timeLabels}
        isAdmin={isAdmin}
        editable={editable}
        onSlotClick={onSlotClick}
        onDateClick={onDateClick}
        onPointerDown={editable ? dragDrop.onPointerDown : undefined}
        animClass={slide.animClass}
        draggedSlotId={dragDrop.dragState.isDragging ? dragDrop.dragState.slot?.id ?? null : null}
      />

      {/* Live drag preview — the slot floats at the current pointer minus
          its grab offset (so it stays glued to the cursor), and a dashed
          outline shows where it would land if released. */}
      {dragDrop.dragState.isDragging && dragDrop.dragState.slot && (() => {
        const slot = dragDrop.dragState.slot
        const snap = dragDrop.dragState.snap
        const { pointerX, pointerY, grabOffsetX, grabOffsetY } = dragDrop.dragState
        const [sh, sm] = slot.startTime.split(':').map(Number)
        const [eh, em] = slot.endTime.split(':').map(Number)
        const durationMin = Math.max(15, (eh * 60 + em) - (sh * 60 + sm))
        const heightPx = (durationMin / 60) * hourHeight - 4
        const grid = gridRef.current
        const gridRect = grid?.getBoundingClientRect()
        const previewWidth = snap ? snap.colWidthPx - 6 : 120
        return (
          <>
            {snap && gridRect && (
              <div
                className="fixed pointer-events-none z-40 rounded border-2 border-dashed"
                style={{
                  left: gridRect.left + snap.leftPx + 3,
                  top: gridRect.top + snap.topPx,
                  width: snap.colWidthPx - 6,
                  height: heightPx,
                  borderColor: slot.borderColor,
                  opacity: 0.55,
                }}
              />
            )}
            <div
              className="fixed pointer-events-none z-50 rounded shadow-2xl ring-2 ring-white/40 dark:ring-black/40"
              style={{
                left: pointerX - grabOffsetX,
                top: pointerY - grabOffsetY,
                width: previewWidth,
                height: heightPx,
                backgroundColor: slot.backgroundColor,
                borderLeft: `3px solid ${slot.borderColor}`,
                color: slot.textColor,
                opacity: 0.95,
              }}
            >
              <div className="p-1 text-xs overflow-hidden h-full">
                {slot.title.split('\n').map((line, idx) => (
                  <div key={idx} className={idx === 0 ? 'font-medium truncate' : 'truncate'}>{line}</div>
                ))}
                {snap && (
                  <div className="mt-0.5 font-mono text-[10px] opacity-80">{snap.time}</div>
                )}
              </div>
            </div>
          </>
        )
      })()}
    </div>
  )
})

DesktopTimeGrid.displayName = 'DesktopTimeGrid'
