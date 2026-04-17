import type { CalendarSlot } from './InfiniteScrollCalendar'

// Stripes use `color-mix` against `currentColor` (= the slot's text color) so
// they inherit theme-aware contrast: dark stripes on light theme, light
// stripes on dark theme. Previous fixed `rgba(0,0,0,0.18)` was invisible on
// dark tints. `color-mix` has been supported in all evergreen browsers since
// 2023.
const STRIPE_COLOR = 'color-mix(in srgb, currentColor 32%, transparent)'
const STRIPE_GRADIENT = `repeating-linear-gradient(45deg, transparent 0 6px, ${STRIPE_COLOR} 6px 10px)`

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
