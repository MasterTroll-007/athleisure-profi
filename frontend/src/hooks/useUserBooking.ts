import { useState, useCallback } from 'react'
import type { AvailableSlot, Reservation } from '@/types/api'

export function useUserBooking() {
  const [selectedSlot, setSelectedSlot] = useState<AvailableSlot | null>(null)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [selectedReservation, setSelectedReservation] = useState<Reservation | null>(null)
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false)
  const [selectedPricingItemId, setSelectedPricingItemId] = useState<string | null>(null)

  const openBookingModal = useCallback((slot: AvailableSlot) => {
    setSelectedSlot(slot)
    setIsModalOpen(true)
    // Auto-select if exactly one pricing item
    if (slot.pricingItems?.length === 1) {
      setSelectedPricingItemId(slot.pricingItems[0].id)
    } else {
      setSelectedPricingItemId(null)
    }
  }, [])

  const closeBookingModal = useCallback(() => {
    setIsModalOpen(false)
    setSelectedSlot(null)
    setSelectedPricingItemId(null)
  }, [])

  const openCancelModal = useCallback((reservation: Reservation) => {
    setSelectedReservation(reservation)
    setIsCancelModalOpen(true)
  }, [])

  const closeCancelModal = useCallback(() => {
    setIsCancelModalOpen(false)
    setSelectedReservation(null)
  }, [])

  const reset = useCallback(() => {
    setSelectedSlot(null)
    setIsModalOpen(false)
    setSelectedReservation(null)
    setIsCancelModalOpen(false)
    setSelectedPricingItemId(null)
  }, [])

  return {
    // State
    selectedSlot,
    isModalOpen,
    selectedReservation,
    isCancelModalOpen,
    selectedPricingItemId,
    // Actions
    openBookingModal,
    closeBookingModal,
    openCancelModal,
    closeCancelModal,
    setSelectedPricingItemId,
    reset,
  }
}
