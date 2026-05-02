import type { CSSProperties } from 'react'
import type { Slot } from '@/types/api'
import {
  NEUTRAL_LOCATION_COLOR,
  darken,
  hexWithAlpha,
  isValidHex,
  neutralTextForTheme,
  readableTextOn,
} from '@/utils/color'

type DashboardSlot = Pick<Slot, 'locationColor' | 'status'>

const resolveLocationColor = (color: string | null | undefined) =>
  isValidHex(color) ? color : NEUTRAL_LOCATION_COLOR

export function dashboardSlotStyle(
  slot: DashboardSlot,
  theme: 'light' | 'dark'
): CSSProperties {
  const base = resolveLocationColor(slot.locationColor)

  if (slot.status === 'reserved') {
    return {
      backgroundColor: base,
      borderColor: darken(base, 0.2),
      color: readableTextOn(base),
    }
  }

  return {
    backgroundColor: hexWithAlpha(base, 0.2),
    borderColor: base,
    color: neutralTextForTheme(theme),
  }
}
