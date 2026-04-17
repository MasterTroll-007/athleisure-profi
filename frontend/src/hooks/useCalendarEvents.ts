import { useMemo, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import type { AvailableSlot, AvailableSlotsResponse, Reservation, Slot, User } from '@/types/api'
import type { CalendarEvent, SlotColors, MonthSlotInfo } from '@/types/calendar'
import type { CalendarSlot } from '@/components/ui'
import { darken, hexWithAlpha, isValidHex, lighten, readableTextOn } from '@/utils/color'
import { useThemeStore } from '@/stores/themeStore'

interface UseCalendarEventsOptions {
  isAdmin: boolean
  user: User | null
  slotsResponse: AvailableSlotsResponse | undefined
  adminSlots: Slot[] | undefined
  myReservations: Reservation[] | undefined
}

const NEUTRAL_GRAY = '#9CA3AF'

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
  // both color schemes. `#111827` (neutral-900) for light theme, `#F9FAFB`
  // (neutral-50) for dark theme.
  const neutralText = resolvedTheme === 'dark' ? '#F9FAFB' : '#111827'

  // Resolve base color from location (or neutral fallback).
  const resolveBaseColor = (color: string | null | undefined) =>
    isValidHex(color) ? color : NEUTRAL_GRAY

  // Get colors for user slots (booking view).
  const getSlotColors = useCallback(
    (slot: AvailableSlot): SlotColors => {
      const base = resolveBaseColor(slot.locationColor)

      if (!slot.isAvailable) {
        // Reserved by someone else → same spot is taken; keep red semantics for clarity.
        if (slot.reservedByUserId && slot.reservedByUserId !== user?.id) {
          return {
            bg: hexWithAlpha(base, 0.55),
            border: '#EF4444',
            text: '#991B1B',
            pattern: 'stripes',
            opacity: 1,
            label: t('calendar.reserved'),
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
        label: t('calendar.available'),
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
          bg: base,
          border: darken(base, 0.2),
          text: readableTextOn(base),
          opacity: 1,
        }
      case 'cancelled':
        return {
          bg: hexWithAlpha(base, 0.55),
          border: '#EF4444',
          text: '#991B1B',
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
            title = slot.assignedUserName || slot.assignedUserEmail || t('calendar.reserved')
            break
          case 'cancelled':
            title = '❌ ' + (slot.assignedUserName || slot.assignedUserEmail || t('calendar.cancelled'))
            break
          case 'locked':
            title = '🔒 ' + t('calendar.locked')
            break
          case 'unlocked':
            title = slot.locationName || t('calendar.available')
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
        const base = resolveBaseColor(reservation.locationColor)
        return {
          id: `reservation-${reservation.id}`,
          title: reservation.locationName || t('calendar.training'),
          start: `${reservation.date}T${reservation.startTime}`,
          end: `${reservation.date}T${reservation.endTime}`,
          backgroundColor: base,
          borderColor: darken(base, 0.2),
          textColor: readableTextOn(base),
          pattern: null,
          opacity: 1,
          extendedProps: { reservation, type: 'reservation' as const },
        }
      })

      const slotEvents: CalendarEvent[] = slotsResponse?.slots
        ?.filter(slot => slot.reservedByUserId !== user?.id)
        .map((slot, index) => {
          const colors = getSlotColors(slot)
          return {
            id: `slot-${slot.blockId}-${index}`,
            title: slot.locationName || colors.label || '',
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
      .filter(slot => slot.isReserved || slot.isMyReservation)
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
      slotsResponse?.slots.forEach(s => addFromColor(s.locationId, s.locationName, s.locationColor))
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
