import { useState, useCallback } from 'react'
import type { Slot } from '@/types/api'

export function useAdminSlotSelection() {
  const [selectedAdminSlot, setSelectedAdminSlot] = useState<Slot | null>(null)
  const [deductCredits, setDeductCredits] = useState(false)
  const [noteText, setNoteText] = useState('')
  const [isEditingNote, setIsEditingNote] = useState(false)
  const [showCancelConfirm, setShowCancelConfirm] = useState(false)
  const [cancelWithRefund, setCancelWithRefund] = useState(true)

  // Create slot modal state
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [createDate, setCreateDate] = useState('')
  const [createTime, setCreateTime] = useState('09:00')
  const [createDuration, setCreateDuration] = useState(60)
  const [createNote, setCreateNote] = useState('')

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
  }, [])

  const closeSlotModal = useCallback(() => {
    setSelectedAdminSlot(null)
    setNoteText('')
    setIsEditingNote(false)
    setDeductCredits(false)
    setShowCancelConfirm(false)
    setCancelWithRefund(true)
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
    // Create slot state
    showCreateModal,
    createDate,
    createTime,
    createDuration,
    createNote,
    // Template state
    showTemplateModal,
    selectedTemplateId,
    // Setters
    setDeductCredits,
    setNoteText,
    setIsEditingNote,
    setCreateDate,
    setCreateTime,
    setCreateDuration,
    setCreateNote,
    setSelectedTemplateId,
    // Actions
    selectSlot,
    closeSlotModal,
    openCancelConfirm,
    closeCancelConfirm,
    openCreateModal,
    closeCreateModal,
    openTemplateModal,
    closeTemplateModal,
  }
}
