import { useRef, useCallback, useEffect, forwardRef, useImperativeHandle } from 'react'
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
  const bodyRef = useRef<HTMLDivElement>(null)
  const timeColRef = useRef<HTMLDivElement>(null)

  useImperativeHandle(ref, () => ({
    getScrollTop: () => bodyRef.current?.scrollTop ?? 0,
    setScrollTop: (v: number) => {
      if (bodyRef.current) bodyRef.current.scrollTop = v
      if (timeColRef.current) timeColRef.current.scrollTop = v
    },
    scrollToNow: () => {
      const now = new Date()
      const currentHour = now.getHours()
      if (currentHour >= startHour && currentHour < endHour && bodyRef.current) {
        const targetTop = ((currentHour - startHour) * 60 + now.getMinutes() - 30) / 60 * hourHeight
        bodyRef.current.scrollTop = Math.max(0, targetTop)
        if (timeColRef.current) timeColRef.current.scrollTop = Math.max(0, targetTop)
      }
    },
  }), [startHour, endHour, hourHeight])

  // Sync time column scroll
  const handleScroll = useCallback(() => {
    if (!bodyRef.current || !timeColRef.current) return
    timeColRef.current.scrollTop = bodyRef.current.scrollTop
  }, [])

  // Scroll to current time on mount
  useEffect(() => {
    const now = new Date()
    const currentHour = now.getHours()
    if (currentHour >= startHour && currentHour < endHour && bodyRef.current) {
      const targetTop = ((currentHour - startHour) * 60 + now.getMinutes() - 30) / 60 * hourHeight
      bodyRef.current.scrollTop = Math.max(0, targetTop)
      if (timeColRef.current) timeColRef.current.scrollTop = Math.max(0, targetTop)
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div className="flex flex-1 overflow-hidden">
      {/* Time column */}
      <div
        ref={timeColRef}
        className="flex-shrink-0 overflow-hidden border-r border-neutral-200 dark:border-neutral-700"
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

      {/* Main area */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Header */}
        <div className="flex-shrink-0 border-b border-neutral-200 dark:border-neutral-700">
          <div className={animClass}>
            <TimeGridHeader days={days} />
          </div>
        </div>

        {/* Body (scrolls vertically) */}
        <div
          ref={bodyRef}
          className="flex-1 overflow-y-auto overflow-x-hidden"
          style={{ scrollbarWidth: 'thin' }}
          onScroll={handleScroll}
        >
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
    </div>
  )
})

TimeGridBody.displayName = 'TimeGridBody'
