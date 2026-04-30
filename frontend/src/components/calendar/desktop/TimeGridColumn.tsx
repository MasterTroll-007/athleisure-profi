import type { CalendarSlot } from '@/components/ui'
import type { SlotWithPosition } from './useTimeGrid'
import { TimeGridEvent } from './TimeGridEvent'
import { TimeGridNowIndicator } from './TimeGridNowIndicator'
import { isSameDay, formatDateISO } from './constants'

interface TimeGridColumnProps {
  date: Date
  slots: SlotWithPosition[]
  startHour: number
  endHour: number
  hourHeight: number
  timeLabels: number[]
  isAdmin: boolean
  editable: boolean
  onSlotClick: (slot: CalendarSlot) => void
  onDateClick?: (date: string, time: string) => void
  onPointerDown?: (e: React.PointerEvent, slot: CalendarSlot) => void
  draggedSlotId?: string | null
}

const ADMIN_QUARTER_MINUTES = [0, 15, 30, 45]

export function TimeGridColumn({
  date,
  slots,
  startHour,
  endHour,
  hourHeight,
  timeLabels,
  isAdmin,
  editable,
  onSlotClick,
  onDateClick,
  onPointerDown,
  draggedSlotId,
}: TimeGridColumnProps) {
  const today = new Date()
  const isToday = isSameDay(date, today)
  const totalHeight = (endHour - startHour) * hourHeight

  const handleTimeClick = (hour: number, minutes: number = 0) => {
    if (!onDateClick) return
    const dateStr = formatDateISO(date)
    const timeStr = `${hour.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`
    onDateClick(dateStr, timeStr)
  }

  const formatTimeLabel = (hour: number, minutes: number) =>
    `${hour.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`

  return (
    <div
      className={`relative border-r border-neutral-200 dark:border-neutral-700 flex-1 min-w-0 ${
        isToday ? 'bg-primary-50/50 dark:bg-primary-900/10' : ''
      }`}
      style={{ height: totalHeight }}
    >
      {/* Hour grid lines */}
      {timeLabels.map((hour, idx) => (
        <div
          key={hour}
          className="absolute w-full border-b border-neutral-100 dark:border-neutral-800"
          style={{ top: idx * hourHeight, height: hourHeight }}
        >
          {isAdmin ? (
            ADMIN_QUARTER_MINUTES.map((minutes, quarterIndex) => (
              <button
                key={`${hour}-${minutes}`}
                type="button"
                aria-label={formatTimeLabel(hour, minutes)}
                className="absolute left-0 w-full cursor-pointer border-b border-neutral-100/70 text-left transition-colors hover:bg-primary-50/70 focus-visible:bg-primary-50/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-primary-300 dark:border-white/[0.06] dark:hover:bg-primary-900/20 dark:focus-visible:bg-primary-900/25"
                style={{
                  top: quarterIndex * (hourHeight / 4),
                  height: hourHeight / 4,
                }}
                onClick={() => handleTimeClick(hour, minutes)}
              />
            ))
          ) : (
            <button
              type="button"
              aria-label={formatTimeLabel(hour, 0)}
              className="absolute inset-0 w-full cursor-pointer transition-colors hover:bg-primary-50/70 focus-visible:bg-primary-50/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-primary-300 dark:hover:bg-primary-900/20 dark:focus-visible:bg-primary-900/25"
              onClick={() => handleTimeClick(hour)}
            />
          )}
        </div>
      ))}

      {/* Events */}
      {slots.map((slot) => (
        <TimeGridEvent
          key={slot.id}
          slot={slot}
          onClick={onSlotClick}
          onPointerDown={onPointerDown}
          draggable={editable}
          isBeingDragged={draggedSlotId === slot.id}
        />
      ))}

      {/* Now indicator */}
      {isToday && (
        <TimeGridNowIndicator
          startHour={startHour}
          endHour={endHour}
          hourHeight={hourHeight}
        />
      )}
    </div>
  )
}
