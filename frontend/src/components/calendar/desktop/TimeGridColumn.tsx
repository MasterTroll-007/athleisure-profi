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
          className="absolute w-full border-b border-neutral-100 dark:border-neutral-800 cursor-pointer hover:bg-neutral-50/50 dark:hover:bg-neutral-800/30"
          style={{ top: idx * hourHeight, height: hourHeight }}
          onClick={() => handleTimeClick(hour)}
        >
          {isAdmin && (
            <>
              <div
                className="absolute w-full border-b border-neutral-50 dark:border-neutral-800/50"
                style={{ top: hourHeight / 4 }}
                onClick={(e) => { e.stopPropagation(); handleTimeClick(hour, 15) }}
              />
              <div
                className="absolute w-full border-b border-neutral-100 dark:border-neutral-800"
                style={{ top: hourHeight / 2 }}
                onClick={(e) => { e.stopPropagation(); handleTimeClick(hour, 30) }}
              />
              <div
                className="absolute w-full border-b border-neutral-50 dark:border-neutral-800/50"
                style={{ top: (hourHeight / 4) * 3 }}
                onClick={(e) => { e.stopPropagation(); handleTimeClick(hour, 45) }}
              />
            </>
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
