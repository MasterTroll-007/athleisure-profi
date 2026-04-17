import type { CalendarSlot } from './InfiniteScrollCalendar'

const STRIPE_GRADIENT =
  'repeating-linear-gradient(45deg, transparent 0 6px, rgba(0,0,0,0.18) 6px 10px)'

// NOTE: we never apply CSS `opacity` to the slot container — that would fade
// the text as well. Fading is done purely through the alpha channel in
// backgroundColor, leaving the text fully opaque and always readable.
export function slotVisualStyle(slot: CalendarSlot): React.CSSProperties {
  const base: React.CSSProperties = {
    backgroundColor: slot.backgroundColor,
    borderLeft: `3px solid ${slot.borderColor}`,
    color: slot.textColor,
  }
  if (slot.pattern === 'stripes') {
    base.backgroundImage = STRIPE_GRADIENT
  }
  return base
}
