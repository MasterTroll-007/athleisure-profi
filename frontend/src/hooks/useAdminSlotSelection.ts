import { useState, useCallback } from 'react'
import type { Slot } from '@/types/api'

export function useAdminSlotSelection() {
  const [selectedAdminSlot, setSelectedAdminSlot] = useState<Slot | null>(null)
  const [deductCredits, setDeductCredits] = useState(false)
  const [noteText, setNoteText] = useState('')
  const [isEditingNote, setIsEditingNote] = useState(false)
  const [showCancelConfirm, setShowCancelConfirm] = useState(false)
  const [cancelWithRefund, setCancelWithRefund] = useState(true)

  // In-place edit of existing slot (date, time, duration, location, pricing)
  const [isEditingSlot, setIsEditingSlot] = useState(false)
  const [editDate, setEditDate] = useState('')
  const [editTime, setEditTime] = useState('')
  const [editDuration, setEditDuration] = useState(60)
  const [editLocationId, setEditLocationId] = useState<string | null>(null)
  const [editPricingItemIds, setEditPricingItemIds] = useState<string[]>([])

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
    setIsEditingSlot(false)
  }, [])

  const closeSlotModal = useCallback(() => {
    setSelectedAdminSlot(null)
    setNoteText('')
    setIsEditingNote(false)
    setDeductCredits(false)
    setShowCancelConfirm(false)
    setCancelWithRefund(true)
    setIsEditingSlot(false)
  }, [])

  const startEditSlot = useCallback(() => {
    setSelectedAdminSlot((current) => {
      if (!current) return current
      setEditDate(current.date)
      setEditTime(current.startTime.substring(0, 5))
      setEditDuration(current.durationMinutes)
      setEditLocationId(current.locationId ?? null)
      setEditPricingItemIds(current.pricingItems.map((p) => p.id))
      setIsEditingSlot(true)
      return current
    })
  }, [])

  const cancelEditSlot = useCallback(() => {
    setIsEditingSlot(false)
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
    // Slot in-place edit state
    isEditingSlot,
    editDate,
    editTime,
    editDuration,
    editLocationId,
    editPricingItemIds,
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
    setNoteText,
    setIsEditingNote,
    setEditDate,
    setEditTime,
    setEditDuration,
    setEditLocationId,
    setEditPricingItemIds,
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
    openCreateModal,
    closeCreateModal,
    openTemplateModal,
    closeTemplateModal,
  }
}
