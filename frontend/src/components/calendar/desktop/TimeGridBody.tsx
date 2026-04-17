import { forwardRef, useImperativeHandle } from 'react'
import type { CalendarSlot } from '@/components/ui'
import type { SlotWithPosition } from './useTimeGrid'
import { TimeGridColumn } from './TimeGridColumn'
import { TimeGridHeader } from './TimeGridHeader'
import { TIME_COL_WIDTH } from './constants'

interface TimeGridBodyProps {
  days: Date[]
  slotsByDay: Map<string, SlotWithPosition[]>
  startHour: number
  endHour: number
  hourHeight: number
  totalHeight: number
  timeLabels: number[]
  isAdmin: boolean
  editable: boolean
  onSlotClick: (slot: CalendarSlot) => void
  onDateClick?: (date: string, time: string) => void
  onPointerDown?: (e: React.PointerEvent, slot: CalendarSlot) => void
  animClass: string
}

export interface TimeGridBodyRef {
  getScrollTop: () => number
  setScrollTop: (v: number) => void
  scrollToNow: () => void
}

export const TimeGridBody = forwardRef<TimeGridBodyRef, TimeGridBodyProps>(({
  days,
  slotsByDay,
  startHour,
  endHour,
  hourHeight,
  totalHeight,
  timeLabels,
  isAdmin,
  editable,
  onSlotClick,
  onDateClick,
  onPointerDown,
  animClass,
}, ref) => {
  // The calendar now renders at its natural height; the page itself scrolls.
  // Imperative ref is kept as a no-op so callers (`scrollToNow` etc.) compile,
  // but internal scrolling is gone.
  useImperativeHandle(ref, () => ({
    getScrollTop: () => 0,
    setScrollTop: () => {},
    scrollToNow: () => {
      const now = new Date()
      const currentHour = now.getHours()
      if (currentHour < startHour || currentHour >= endHour) return
      // Scroll the whole page so the current hour row is visible.
      const el = document.querySelector('[data-time-grid-body]') as HTMLElement | null
      if (!el) return
      const topInPage = el.getBoundingClientRect().top + window.scrollY
      const target = topInPage + ((currentHour - startHour) * 60 + now.getMinutes() - 30) / 60 * hourHeight
      window.scrollTo({ top: Math.max(0, target), behavior: 'smooth' })
    },
  }), [startHour, endHour, hourHeight])

  return (
    <div className="flex" data-time-grid-body>
      {/* Time column */}
      <div
        className="flex-shrink-0 border-r border-neutral-200 dark:border-neutral-700"
        style={{ width: TIME_COL_WIDTH }}
      >
        {/* Spacer for header */}
        <div className="border-b border-neutral-200 dark:border-neutral-700" style={{ height: 36 }} />
        {/* Time labels */}
        <div>
          {timeLabels.map(hour => (
            <div
              key={hour}
              className="text-[11px] text-neutral-500 dark:text-neutral-400 text-right pr-2 border-b border-neutral-100 dark:border-neutral-800 flex items-start justify-end pt-0.5"
              style={{ height: hourHeight }}
            >
              {hour}:00
            </div>
          ))}
        </div>
      </div>

      {/* Main area: header + full-height grid body, no internal scroll */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Header — sticks to top of page as user scrolls, sitting just below
            the fixed app header (h-14 = 56px). */}
        <div
          className="border-b border-neutral-200 dark:border-neutral-700 bg-white dark:bg-dark-surface sticky z-20"
          style={{ top: 56 }}
        >
          <div className={animClass}>
            <TimeGridHeader days={days} />
          </div>
        </div>

        {/* Grid body renders at its natural height */}
        <div
          className={`flex ${animClass}`}
          style={{ minHeight: totalHeight }}
        >
          {days.map((date, i) => {
            const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
            return (
              <TimeGridColumn
                key={`${key}-${i}`}
                date={date}
                slots={slotsByDay.get(key) || []}
                startHour={startHour}
                endHour={endHour}
                hourHeight={hourHeight}
                timeLabels={timeLabels}
                isAdmin={isAdmin}
                editable={editable}
                onSlotClick={onSlotClick}
                onDateClick={onDateClick}
                onPointerDown={onPointerDown}
              />
            )
          })}
        </div>
      </div>
    </div>
  )
})

TimeGridBody.displayName = 'TimeGridBody'
