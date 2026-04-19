import type { CalendarSlot } from '@/components/ui'
import { slotVisualStyle } from '@/components/ui/slotVisualStyle'
import type { SlotWithPosition } from './useTimeGrid'

interface TimeGridEventProps {
  slot: SlotWithPosition
  onClick: (slot: CalendarSlot) => void
  onPointerDown?: (e: React.PointerEvent, slot: CalendarSlot) => void
  draggable?: boolean
  // True while this specific slot is being dragged — fades the original so
  // the floating preview is the visually dominant element.
  isBeingDragged?: boolean
}

export function TimeGridEvent({ slot, onClick, onPointerDown, draggable, isBeingDragged }: TimeGridEventProps) {
  const widthPercent = 100 / slot.totalColumns
  const leftPercent = (slot.column / slot.totalColumns) * 100
  const visual = slotVisualStyle(slot)

  return (
    <div
      className={`absolute rounded overflow-hidden select-none transition-opacity ${draggable ? 'cursor-grab active:cursor-grabbing' : 'cursor-pointer'}`}
      style={{
        top: `${slot.top}px`,
        height: `${slot.height}px`,
        left: `calc(${leftPercent}% + 3px)`,
        width: `calc(${widthPercent}% - 6px)`,
        ...visual,
        opacity: isBeingDragged ? 0 : (visual.opacity ?? 1),
        zIndex: 10,
      }}
      onClick={(e) => {
        e.stopPropagation()
        onClick(slot)
      }}
      onPointerDown={draggable ? (e) => onPointerDown?.(e, slot) : undefined}
    >
      <div className="p-1 text-xs overflow-hidden h-full">
        {slot.title.split('\n').map((line, idx) => (
          <div key={idx} className={idx === 0 ? 'font-medium truncate' : 'truncate'}>{line}</div>
        ))}
      </div>
    </div>
  )
}
