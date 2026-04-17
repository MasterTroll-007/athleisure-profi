import type { CalendarSlot } from './InfiniteScrollCalendar'

const STRIPE_GRADIENT =
  'repeating-linear-gradient(45deg, transparent 0 6px, rgba(0,0,0,0.18) 6px 10px)'

export function slotVisualStyle(slot: CalendarSlot): React.CSSProperties {
  const base: React.CSSProperties = {
    backgroundColor: slot.backgroundColor,
    borderLeft: `3px solid ${slot.borderColor}`,
    color: slot.textColor,
    opacity: slot.opacity ?? 1,
  }
  if (slot.pattern === 'stripes') {
    base.backgroundImage = STRIPE_GRADIENT
  }
  return base
}
