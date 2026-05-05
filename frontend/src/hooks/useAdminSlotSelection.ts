import { useState, useCallback } from 'react'
import type { Slot } from '@/types/api'

const minutesBetween = (startTime: string, endTime: string, fallback: number) => {
  const [startHour, startMinute] = startTime.substring(0, 5).split(':').map(Number)
  const [endHour, endMinute] = endTime.substring(0, 5).split(':').map(Number)
  if (
    !Number.isFinite(startHour) ||
    !Number.isFinite(startMinute) ||
    !Number.isFinite(endHour) ||
    !Number.isFinite(endMinute)
  ) {
    return fallback
  }

  const diff = endHour * 60 + endMinute - (startHour * 60 + startMinute)
  return diff > 0 ? diff : fallback
}

export function useAdminSlotSelection() {
  const [selectedAdminSlot, setSelectedAdminSlot] = useState<Slot | null>(null)
  const [deductCredits, setDeductCredits] = useState(false)
  const [noteText, setNoteText] = useState('')
  const [isEditingNote, setIsEditingNote] = useState(false)
  const [showCancelConfirm, setShowCancelConfirm] = useState(false)
  const [cancelWithRefund, setCancelWithRefund] = useState(true)
  const [reservationPricingItemId, setReservationPricingItemId] = useState<string | null>(null)

  // In-place edit of existing slot (date, time, duration, location, pricing)
  const [isEditingSlot, setIsEditingSlot] = useState(false)
  const [editDate, setEditDate] = useState('')
  const [editTime, setEditTime] = useState('')
  const [editDuration, setEditDuration] = useState(60)
  const [editLocationId, setEditLocationId] = useState<string | null>(null)
  const [editPricingItemIds, setEditPricingItemIds] = useState<string[]>([])

  const [isRescheduling, setIsRescheduling] = useState(false)
  const [rescheduleDate, setRescheduleDate] = useState('')
  const [rescheduleTime, setRescheduleTime] = useState('')
  const [rescheduleDuration, setRescheduleDuration] = useState(60)

  // Create slot modal state
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [createDate, setCreateDate] = useState('')
  const [createTime, setCreateTime] = useState('09:00')
  const [createDuration, setCreateDuration] = useState(60)
  const [createNote, setCreateNote] = useState('')
  const [createPricingItemIds, setCreatePricingItemIds] = useState<string[]>([])
  const [createLocationId, setCreateLocationId] = useState<string | null>(null)

  // Template modal state
  const [showTemplateModal, setShowTemplateModal] = useState(false)
  const [selectedTemplateId, setSelectedTemplateId] = useState<string | null>(null)

  const selectSlot = useCallback((slot: Slot) => {
    setSelectedAdminSlot(slot)
    setNoteText(slot.note || '')
    setIsEditingNote(false)
    setDeductCredits(false)
    setShowCancelConfirm(false)
    setCancelWithRefund(true)
    setReservationPricingItemId(slot.pricingItems.length > 0 ? slot.pricingItems[0].id : null)
    setIsEditingSlot(false)
    setIsRescheduling(false)
  }, [])

  const closeSlotModal = useCallback(() => {
    setSelectedAdminSlot(null)
    setNoteText('')
    setIsEditingNote(false)
    setDeductCredits(false)
    setShowCancelConfirm(false)
    setCancelWithRefund(true)
    setReservationPricingItemId(null)
    setIsEditingSlot(false)
    setIsRescheduling(false)
  }, [])

  const startEditSlot = useCallback(() => {
    setSelectedAdminSlot((current) => {
      if (!current) return current
      setEditDate(current.date)
      setEditTime(current.startTime.substring(0, 5))
      setEditDuration(minutesBetween(current.startTime, current.endTime, current.durationMinutes))
      setEditLocationId(current.locationId ?? null)
      setEditPricingItemIds(current.pricingItems.map((p) => p.id))
      setIsEditingSlot(true)
      return current
    })
  }, [])

  const cancelEditSlot = useCallback(() => {
    setIsEditingSlot(false)
  }, [])

  const startReschedule = useCallback(() => {
    setSelectedAdminSlot((current) => {
      if (!current) return current
      setRescheduleDate(current.date)
      setRescheduleTime(current.startTime.substring(0, 5))
      setRescheduleDuration(minutesBetween(current.startTime, current.endTime, current.durationMinutes))
      setIsRescheduling(true)
      return current
    })
  }, [])

  const cancelReschedule = useCallback(() => {
    setIsRescheduling(false)
  }, [])

  const openCancelConfirm = useCallback((withRefund: boolean) => {
    setCancelWithRefund(withRefund)
    setShowCancelConfirm(true)
  }, [])

  const closeCancelConfirm = useCallback(() => {
    setShowCancelConfirm(false)
  }, [])

  const openCreateModal = useCallback((date: string, time: string) => {
    setCreateDate(date)
    setCreateTime(time)
    setShowCreateModal(true)
  }, [])

  const closeCreateModal = useCallback(() => {
    setShowCreateModal(false)
    setCreateDate('')
    setCreateTime('09:00')
    setCreateDuration(60)
    setCreateNote('')
    setCreatePricingItemIds([])
    setCreateLocationId(null)
  }, [])

  const openTemplateModal = useCallback(() => {
    setShowTemplateModal(true)
  }, [])

  const closeTemplateModal = useCallback(() => {
    setShowTemplateModal(false)
    setSelectedTemplateId(null)
  }, [])

  return {
    // Slot selection state
    selectedAdminSlot,
    deductCredits,
    noteText,
    isEditingNote,
    showCancelConfirm,
    cancelWithRefund,
    reservationPricingItemId,
    // Slot in-place edit state
    isEditingSlot,
    editDate,
    editTime,
    editDuration,
    editLocationId,
    editPricingItemIds,
    isRescheduling,
    rescheduleDate,
    rescheduleTime,
    rescheduleDuration,
    // Create slot state
    showCreateModal,
    createDate,
    createTime,
    createDuration,
    createNote,
    createPricingItemIds,
    createLocationId,
    // Template state
    showTemplateModal,
    selectedTemplateId,
    // Setters
    setDeductCredits,
    setReservationPricingItemId,
    setNoteText,
    setIsEditingNote,
    setEditDate,
    setEditTime,
    setEditDuration,
    setEditLocationId,
    setEditPricingItemIds,
    setRescheduleDate,
    setRescheduleTime,
    setRescheduleDuration,
    setCreateDate,
    setCreateTime,
    setCreateDuration,
    setCreateNote,
    setCreatePricingItemIds,
    setCreateLocationId,
    setSelectedTemplateId,
    // Actions
    selectSlot,
    closeSlotModal,
    openCancelConfirm,
    closeCancelConfirm,
    startEditSlot,
    cancelEditSlot,
    startReschedule,
    cancelReschedule,
    openCreateModal,
    closeCreateModal,
    openTemplateModal,
    closeTemplateModal,
  }
}
