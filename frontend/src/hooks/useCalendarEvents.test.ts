import { describe, it, expect, vi } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useCalendarEvents } from './useCalendarEvents'
import type { Slot, AvailableSlot, User, Reservation } from '@/types/api'

// Theme store returns 'light' by default for these tests.
vi.mock('@/stores/themeStore', () => ({
  useThemeStore: (selector: (s: { resolvedTheme: 'light' | 'dark' }) => unknown) =>
    selector({ resolvedTheme: 'light' }),
}))

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

const baseAdminSlot = (o: Partial<Slot> = {}): Slot => ({
  id: 'slot-1',
  date: '2026-05-10',
  startTime: '10:00',
  endTime: '11:00',
  durationMinutes: 60,
  status: 'unlocked',
  assignedUserId: null,
  assignedUserName: null,
  assignedUserEmail: null,
  note: null,
  reservationId: null,
  createdAt: '2026-04-01T00:00:00Z',
  cancelledAt: null,
  pricingItems: [],
  capacity: 1,
  currentBookings: 0,
  locationId: null,
  locationName: null,
  locationColor: null,
  ...o,
})

describe('useCalendarEvents — admin colors', () => {
  it('unlocked slot uses a tinted location color with stable border', () => {
    const { result } = renderHook(() =>
      useCalendarEvents({
        isAdmin: true,
        user: null,
        slotsResponse: undefined,
        adminSlots: [baseAdminSlot({ locationColor: '#3B82F6' })],
        myReservations: [],
      })
    )

    const [event] = result.current.events
    expect(event.borderColor).toBe('#3B82F6')
    // bg tinted at 20%
    expect(event.backgroundColor).toMatch(/rgba\(59, 130, 246,\s*0\.2\)/)
    expect(event.title).toBe('calendar.freeSlot')
    // No stripes on a normal unlocked slot
    expect(event.pattern).toBeNull()
  })

  it('cancelled slot uses stripes pattern and red border', () => {
    const { result } = renderHook(() =>
      useCalendarEvents({
        isAdmin: true,
        user: null,
        slotsResponse: undefined,
        adminSlots: [baseAdminSlot({ status: 'cancelled', locationColor: '#10B981' })],
        myReservations: [],
      })
    )

    const [event] = result.current.events
    expect(event.pattern).toBe('stripes')
    expect(event.borderColor).toBe('#EF4444')
  })

  it('locked slot uses neutral-gray text color even without location', () => {
    const { result } = renderHook(() =>
      useCalendarEvents({
        isAdmin: true,
        user: null,
        slotsResponse: undefined,
        adminSlots: [baseAdminSlot({ status: 'locked', locationColor: null })],
        myReservations: [],
      })
    )

    const [event] = result.current.events
    // Neutral gray fallback = #9CA3AF
    expect(event.backgroundColor).toMatch(/rgba\(156, 163, 175/)
  })

  it('locationLegend lists unique locations across slots', () => {
    const { result } = renderHook(() =>
      useCalendarEvents({
        isAdmin: true,
        user: null,
        slotsResponse: undefined,
        adminSlots: [
          baseAdminSlot({ locationId: 'A', locationName: 'Gym A', locationColor: '#3B82F6' }),
          baseAdminSlot({ locationId: 'B', locationName: 'Gym B', locationColor: '#EF4444' }),
          baseAdminSlot({ locationId: 'A', locationName: 'Gym A', locationColor: '#3B82F6' }),
        ],
        myReservations: [],
      })
    )

    expect(result.current.locationLegend).toHaveLength(2)
  })
})

describe('useCalendarEvents — user view', () => {
  const user: User = {
    id: 'u1',
    email: 'u@e.com',
    firstName: null,
    lastName: null,
    phone: null,
    role: 'client',
    credits: 5,
    locale: 'cs',
    theme: 'system',
    trainerId: null,
    trainerName: null,
    calendarStartHour: 6,
    calendarEndHour: 22,
    emailRemindersEnabled: true,
    reminderHoursBefore: 24,
    createdAt: '2026-01-01',
  }

  it('own confirmed reservation renders as gray event named by training type', () => {
    const reservation: Reservation = {
      id: 'r1',
      userId: 'u1',
      userName: null,
      userEmail: null,
      blockId: null,
      date: '2026-05-10',
      startTime: '10:00',
      endTime: '11:00',
      status: 'confirmed',
      creditsUsed: 1,
      pricingItemId: null,
      pricingItemName: 'Silový trénink',
      createdAt: '',
      cancelledAt: null,
      completedAt: null,
      locationColor: '#8B5CF6',
      locationName: 'Gym Praha',
    }

    const { result } = renderHook(() =>
      useCalendarEvents({
        isAdmin: false,
        user,
        slotsResponse: undefined,
        adminSlots: undefined,
        myReservations: [reservation],
      })
    )

    const [event] = result.current.events
    expect(event.backgroundColor).toBe('#6B7280')
    expect(event.borderColor).toBe('#4B5563')
    expect(event.title).toBe('Silový trénink')
  })

  it('other-user slot uses a gray occupied style without stripes', () => {
    const slot: AvailableSlot = {
      start: '2026-05-10T10:00',
      end: '2026-05-10T11:00',
      blockId: 's1',
      isAvailable: false,
      reservedByUserId: 'other-user',
      pricingItems: [],
    }

    const { result } = renderHook(() =>
      useCalendarEvents({
        isAdmin: false,
        user,
        slotsResponse: { slots: [slot] },
        adminSlots: undefined,
        myReservations: [],
      })
    )

    const [event] = result.current.events
    expect(event.pattern).toBeNull()
    expect(event.backgroundColor).toBe('#6B7280')
    expect(event.borderColor).toBe('#4B5563')
    expect(event.title).toBe('calendar.occupiedSlot')
  })
})
