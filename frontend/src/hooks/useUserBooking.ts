import { useState, useCallback } from 'react'
import type { AvailableSlot, Reservation } from '@/types/api'

export function useUserBooking() {
  const [selectedSlot, setSelectedSlot] = useState<AvailableSlot | null>(null)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [selectedReservation, setSelectedReservation] = useState<Reservation | null>(null)
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false)

  const openBookingModal = useCallback((slot: AvailableSlot) => {
    setSelectedSlot(slot)
    setIsModalOpen(true)
  }, [])

  const closeBookingModal = useCallback(() => {
    setIsModalOpen(false)
    setSelectedSlot(null)
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
  }, [])

  return {
    // State
    selectedSlot,
    isModalOpen,
    selectedReservation,
    isCancelModalOpen,
    // Actions
    openBookingModal,
    closeBookingModal,
    openCancelModal,
    closeCancelModal,
    reset,
  }
}
