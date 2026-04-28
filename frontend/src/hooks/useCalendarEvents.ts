import { useMemo, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import type { AvailableSlot, AvailableSlotsResponse, Reservation, Slot, User } from '@/types/api'
import type { CalendarEvent, SlotColors, MonthSlotInfo } from '@/types/calendar'
import type { CalendarSlot } from '@/components/ui'
import {
  NEUTRAL_LOCATION_COLOR,
  darken,
  hexWithAlpha,
  isValidHex,
  lighten,
  neutralTextForTheme,
  readableTextOn,
} from '@/utils/color'
import { useThemeStore } from '@/stores/themeStore'

interface UseCalendarEventsOptions {
  isAdmin: boolean
  user: User | null
  slotsResponse: AvailableSlotsResponse | undefined
  adminSlots: Slot[] | undefined
  myReservations: Reservation[] | undefined
}

// Pure helper — module-level so the reference stays stable across renders
// and the hook's memoised callbacks don't need to include it in deps.
const resolveBaseColor = (color: string | null | undefined) =>
  isValidHex(color) ? color : NEUTRAL_LOCATION_COLOR

const OCCUPIED_SLOT_COLOR = '#6B7280'
const OCCUPIED_SLOT_BORDER = '#4B5563'

export function useCalendarEvents({
  isAdmin,
  user,
  slotsResponse,
  adminSlots,
  myReservations,
}: UseCalendarEventsOptions) {
  const { t } = useTranslation()
  const resolvedTheme = useThemeStore((s) => s.resolvedTheme)
  // Neutral text that stays readable over the 20% tinted slot background in
  // both color schemes.
  const neutralText = neutralTextForTheme(resolvedTheme)


  // Get colors for user slots (booking view).
  const getSlotColors = useCallback(
    (slot: AvailableSlot): SlotColors => {
      const base = resolveBaseColor(slot.locationColor)

      if (!slot.isAvailable) {
        // Reserved by someone else: fixed gray, no hatching.
        if (slot.reservedByUserId && slot.reservedByUserId !== user?.id) {
          return {
            bg: OCCUPIED_SLOT_COLOR,
            border: OCCUPIED_SLOT_BORDER,
            text: readableTextOn(OCCUPIED_SLOT_COLOR),
            opacity: 1,
            label: t('calendar.occupiedSlot'),
          }
        }
        // Past or otherwise unavailable → faded but text stays readable.
        return {
          bg: hexWithAlpha(base, 0.3),
          border: darken(base, 0.15),
          text: '#374151',
          opacity: 1,
          icon: '🔒',
          label: t('calendar.unavailable'),
        }
      }
      // Available: light tint of location color, strong neutral text.
      return {
        bg: hexWithAlpha(base, 0.2),
        border: base,
        text: neutralText,
        opacity: 1,
        label: t('calendar.freeSlot'),
      }
    },
    [user?.id, t, neutralText]
  )

  // Get colors for admin slots.
  const getAdminSlotColors = useCallback((slot: Slot): SlotColors => {
    const base = resolveBaseColor(slot.locationColor)

    switch (slot.status) {
      case 'reserved':
        return {
          bg: OCCUPIED_SLOT_COLOR,
          border: OCCUPIED_SLOT_BORDER,
          text: readableTextOn(OCCUPIED_SLOT_COLOR),
          opacity: 1,
        }
      case 'cancelled':
        // Keep the diagonal stripes + red border + ❌ icon for cancelled
        // semantics, but use the theme-aware neutral text so the label stays
        // legible on top of the tint + stripe pattern in both dark and light
        // themes (dark red on dark mode tint was hard to read).
        return {
          bg: hexWithAlpha(base, 0.55),
          border: '#EF4444',
          text: neutralText,
          pattern: 'stripes',
          opacity: 1,
          icon: '❌',
        }
      case 'locked':
        return {
          bg: hexWithAlpha(base, 0.25),
          border: darken(base, 0.1),
          text: neutralText,
          opacity: 1,
          icon: '🔒',
        }
      case 'blocked':
        return {
          bg: hexWithAlpha(base, 0.3),
          border: '#6B7280',
          text: neutralText,
          opacity: 1,
          icon: '⛔',
        }
      case 'unlocked':
      default:
        return {
          bg: hexWithAlpha(base, 0.2),
          border: base,
          // Keep text in a strong neutral tone so it stays readable on any
          // location-tinted background (saturated OR muted hues).
          text: neutralText,
          opacity: 1,
        }
    }
  }, [neutralText])

  // Build events for FullCalendar
  const events: CalendarEvent[] = useMemo(() => {
    if (isAdmin) {
      return adminSlots?.map((slot) => {
        const colors = getAdminSlotColors(slot)
        let title = ''
        switch (slot.status) {
          case 'reserved':
            title = t('calendar.occupiedSlot')
            break
          case 'cancelled':
            title = '❌ ' + (slot.assignedUserName || slot.assignedUserEmail || t('calendar.cancelled'))
            break
          case 'locked':
            title = '🔒 ' + t('calendar.locked')
            break
          case 'unlocked':
            title = t('calendar.freeSlot')
            break
        }

        return {
          id: slot.id,
          title,
          start: `${slot.date}T${slot.startTime}`,
          end: `${slot.date}T${slot.endTime}`,
          backgroundColor: colors.bg,
          borderColor: colors.border,
          textColor: colors.text,
          pattern: colors.pattern ?? null,
          opacity: colors.opacity ?? 1,
          extendedProps: { adminSlot: slot, type: 'adminSlot' as const },
        }
      }) || []
    } else {
      const confirmedReservations = myReservations?.filter(r => r.status === 'confirmed') || []

      const reservationEvents: CalendarEvent[] = confirmedReservations.map((reservation) => {
        return {
          id: `reservation-${reservation.id}`,
          title: reservation.pricingItemName || t('calendar.training'),
          start: `${reservation.date}T${reservation.startTime}`,
          end: `${reservation.date}T${reservation.endTime}`,
          backgroundColor: OCCUPIED_SLOT_COLOR,
          borderColor: OCCUPIED_SLOT_BORDER,
          textColor: readableTextOn(OCCUPIED_SLOT_COLOR),
          pattern: null,
          opacity: 1,
          extendedProps: { reservation, type: 'reservation' as const },
        }
      })

      const slotEvents: CalendarEvent[] = slotsResponse?.slots
        ?.filter(slot => slot.reservedByUserId !== user?.id && (slot.isAvailable || Boolean(slot.reservedByUserId)))
        .map((slot, index) => {
          const colors = getSlotColors(slot)
          return {
            id: `slot-${slot.blockId}-${index}`,
            title: colors.label || '',
            start: slot.start,
            end: slot.end,
            backgroundColor: colors.bg,
            borderColor: colors.border,
            textColor: colors.text,
            pattern: colors.pattern ?? null,
            opacity: colors.opacity ?? 1,
            extendedProps: { slot, type: 'slot' as const },
          }
        }) || []

      return [...slotEvents, ...reservationEvents]
    }
  }, [isAdmin, adminSlots, slotsResponse, myReservations, user, t, getAdminSlotColors, getSlotColors])

  // Convert events to CalendarSlot format for InfiniteScrollCalendar
  const calendarSlots: CalendarSlot[] = useMemo(() => {
    return events.map(event => ({
      id: event.id,
      date: event.start.split('T')[0],
      startTime: event.start.split('T')[1]?.substring(0, 5) || '00:00',
      endTime: event.end.split('T')[1]?.substring(0, 5) || '00:00',
      title: event.title,
      backgroundColor: event.backgroundColor,
      borderColor: event.borderColor,
      textColor: event.textColor,
      pattern: event.pattern,
      opacity: event.opacity,
      data: event.extendedProps,
    }))
  }, [events])

  // Get slots for a specific day in month view
  const getSlotsForDay = useCallback((year: number, month: number, day: number): MonthSlotInfo[] => {
    const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`

    const today = new Date()
    const cellDate = new Date(year, month, day)
    const todayMidnight = new Date(today.getFullYear(), today.getMonth(), today.getDate())
    if (cellDate < todayMidnight) {
      return []
    }

    return calendarSlots
      .filter(slot => slot.date === dateStr)
      .map(slot => {
        const data = slot.data
        const isReserved = data.type === 'reservation' || data.adminSlot?.status === 'reserved'
        const isLocked = data.adminSlot?.status === 'locked'
        const isCancelled = data.adminSlot?.status === 'cancelled'
        const isMyReservation = data.type === 'reservation'
        const isUnlocked = data.adminSlot?.status === 'unlocked'

        let label: string | null = null
        if (isAdmin && data.adminSlot?.assignedUserName) {
          const name = data.adminSlot.assignedUserName
          const parts = name.split(' ')
          if (parts.length >= 2) {
            label = `${parts[0].charAt(0)}. ${parts[parts.length - 1]}`
          } else {
            label = name
          }
        }

        return {
          time: slot.startTime.substring(0, 5),
          label,
          isReserved,
          isLocked,
          isCancelled,
          isMyReservation,
          isUnlocked,
        }
      })
      // Show reserved slots + the current user's reservations. For admin the
      // month dots also include locked/cancelled/unlocked states so the
      // overview reflects the day's full status, not just confirmed bookings.
      .filter(slot => isAdmin
        ? (slot.isReserved || slot.isLocked || slot.isCancelled || slot.isUnlocked || slot.isMyReservation)
        : (slot.isReserved || slot.isMyReservation)
      )
      .sort((a, b) => a.time.localeCompare(b.time))
  }, [calendarSlots, isAdmin])

  // Build a legend of locations present in the current view.
  const locationLegend = useMemo(() => {
    const map = new Map<string, { color: string; name: string }>()
    const addFromColor = (id?: string | null, name?: string | null, color?: string | null) => {
      if (!id || !isValidHex(color)) return
      if (!map.has(id)) map.set(id, { color: color as string, name: name || '' })
    }
    if (isAdmin) {
      adminSlots?.forEach(s => addFromColor(s.locationId, s.locationName, s.locationColor))
    } else {
      slotsResponse?.slots?.forEach(s => addFromColor(s.locationId, s.locationName, s.locationColor))
      myReservations?.forEach(r => addFromColor(r.locationId, r.locationName, r.locationColor))
    }
    return Array.from(map.values())
  }, [isAdmin, adminSlots, slotsResponse, myReservations])

  return {
    events,
    calendarSlots,
    getSlotsForDay,
    getSlotColors,
    getAdminSlotColors,
    locationLegend,
    // Exposed for legend / reuse in other components.
    _colorUtils: { darken, lighten, hexWithAlpha, readableTextOn },
  }
}
