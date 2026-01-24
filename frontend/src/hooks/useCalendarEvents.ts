import { useMemo, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import type { AvailableSlot, AvailableSlotsResponse, Reservation, Slot, User } from '@/types/api'
import type { CalendarEvent, SlotColors, MonthSlotInfo } from '@/types/calendar'
import type { CalendarSlot } from '@/components/ui'

interface UseCalendarEventsOptions {
  isAdmin: boolean
  user: User | null
  slotsResponse: AvailableSlotsResponse | undefined
  adminSlots: Slot[] | undefined
  myReservations: Reservation[] | undefined
}

export function useCalendarEvents({
  isAdmin,
  user,
  slotsResponse,
  adminSlots,
  myReservations,
}: UseCalendarEventsOptions) {
  const { t } = useTranslation()

  // Get colors for user slots
  const getSlotColors = useCallback((slot: AvailableSlot): SlotColors => {
    if (!slot.isAvailable) {
      if (slot.reservedByUserId && slot.reservedByUserId !== user?.id) {
        return { bg: '#fee2e2', border: '#ef4444', text: '#991b1b', label: t('calendar.reserved') }
      }
      return { bg: '#f3f4f6', border: '#9ca3af', text: '#6b7280', label: t('calendar.unavailable') }
    }
    return { bg: '#dcfce7', border: '#22c55e', text: '#166534', label: t('calendar.available') }
  }, [user?.id, t])

  // Get colors for admin slots
  const getAdminSlotColors = useCallback((slot: Slot): SlotColors => {
    switch (slot.status) {
      case 'reserved':
        return { bg: '#dbeafe', border: '#3b82f6', text: '#1e40af' }
      case 'cancelled':
        return { bg: '#fee2e2', border: '#ef4444', text: '#991b1b' }
      case 'locked':
        return { bg: '#e5e7eb', border: '#9ca3af', text: '#6b7280' }
      case 'unlocked':
      default:
        return { bg: '#dcfce7', border: '#22c55e', text: '#166534' }
    }
  }, [])

  // Build events for FullCalendar
  const events: CalendarEvent[] = useMemo(() => {
    if (isAdmin) {
      // Admin view - use admin slots
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
            title = t('calendar.available')
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
          extendedProps: { adminSlot: slot, type: 'adminSlot' as const },
        }
      }) || []
    } else {
      // User view - available slots + own reservations
      const confirmedReservations = myReservations?.filter(r => r.status === 'confirmed') || []

      const reservationEvents: CalendarEvent[] = confirmedReservations.map((reservation) => ({
        id: `reservation-${reservation.id}`,
        title: t('calendar.training'),
        start: `${reservation.date}T${reservation.startTime}`,
        end: `${reservation.date}T${reservation.endTime}`,
        backgroundColor: '#dbeafe',
        borderColor: '#3b82f6',
        textColor: '#1e40af',
        extendedProps: { reservation, type: 'reservation' as const },
      }))

      const slotEvents: CalendarEvent[] = slotsResponse?.slots
        ?.filter(slot => slot.reservedByUserId !== user?.id)
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
      data: event.extendedProps,
    }))
  }, [events])

  // Get slots for a specific day in month view
  const getSlotsForDay = useCallback((year: number, month: number, day: number): MonthSlotInfo[] => {
    const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`

    // Skip past days entirely
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

        // Format label for admin view (like mobile app: "J. Novák")
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
      // Filter: only show reserved slots (with user) or user's own reservations
      // Hide empty/available/locked/unlocked/cancelled slots
      .filter(slot => slot.isReserved || slot.isMyReservation)
      .sort((a, b) => a.time.localeCompare(b.time))
  }, [calendarSlots, isAdmin])

  return {
    events,
    calendarSlots,
    getSlotsForDay,
    getSlotColors,
    getAdminSlotColors,
  }
}
