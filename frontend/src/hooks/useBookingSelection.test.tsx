import { act, renderHook } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { useAdminSlotSelection } from './useAdminSlotSelection'
import { useUserBooking } from './useUserBooking'
import type { AvailableSlot, Slot } from '@/types/api'

const pricingItems = [
  { id: 'type-a', nameCs: 'Osobni trenink', nameEn: 'Personal training', credits: 2 },
  { id: 'type-b', nameCs: 'Kondicni trenink', nameEn: 'Conditioning', credits: 1 },
]

function availableSlot(): AvailableSlot {
  return {
    start: '2026-05-10T10:00',
    end: '2026-05-10T11:00',
    slotId: 'slot-1',
    isAvailable: true,
    reservedByUserId: null,
    pricingItems,
  }
}

function adminSlot(): Slot {
  return {
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
    createdAt: '2026-05-01T10:00:00Z',
    cancelledAt: null,
    pricingItems,
    capacity: 1,
    currentBookings: 0,
  }
}

describe('booking training type selection', () => {
  it('auto-selects the first client booking training type so booking is not blocked', () => {
    const { result } = renderHook(() => useUserBooking())

    act(() => result.current.openBookingModal(availableSlot()))

    expect(result.current.selectedPricingItemId).toBe('type-a')
  })

  it('auto-selects the first admin reservation training type for a selected slot', () => {
    const { result } = renderHook(() => useAdminSlotSelection())

    act(() => result.current.selectSlot(adminSlot()))

    expect(result.current.reservationPricingItemId).toBe('type-a')
  })
})
