import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { reservationApi, adminApi } from '@/services/api'
import { useToast } from '@/components/ui/Toast'
import { useAuthStore } from '@/stores/authStore'

interface UseCalendarMutationsOptions {
  onUserBookingSuccess?: () => void
  onUserCancelSuccess?: () => void
  onAdminSlotSuccess?: () => void
  onAdminReservationSuccess?: () => void
}

export function useCalendarMutations(options: UseCalendarMutationsOptions = {}) {
  const { t } = useTranslation()
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const { user, updateUser } = useAuthStore()

  const invalidateCalendarQueries = () => {
    queryClient.invalidateQueries({ queryKey: ['availableSlots'] })
    queryClient.invalidateQueries({ queryKey: ['myReservations'] })
    queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
  }

  // User: Create reservation
  const createReservationMutation = useMutation({
    mutationFn: reservationApi.create,
    onSuccess: () => {
      showToast('success', t('reservation.success'))
      invalidateCalendarQueries()
      if (user) {
        updateUser({ ...user, credits: user.credits - 1 })
      }
      options.onUserBookingSuccess?.()
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      showToast('error', error.response?.data?.message || t('errors.somethingWrong'))
    },
  })

  // User: Cancel reservation
  const cancelReservationMutation = useMutation({
    mutationFn: reservationApi.cancel,
    onSuccess: (data) => {
      showToast('success', t('myReservations.cancelSuccess'))
      invalidateCalendarQueries()
      if (user) {
        updateUser({ ...user, credits: user.credits + data.creditsUsed })
      }
      options.onUserCancelSuccess?.()
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      showToast('error', error.response?.data?.message || t('myReservations.cancelTooLate'))
      options.onUserCancelSuccess?.()
    },
  })

  // Admin: Create slot
  const createSlotMutation = useMutation({
    mutationFn: adminApi.createSlot,
    onSuccess: () => {
      showToast('success', t('calendar.slotCreated'))
      invalidateCalendarQueries()
      options.onAdminSlotSuccess?.()
    },
    onError: (error: { response?: { data?: { error?: string } } }) => {
      showToast('error', error.response?.data?.error || t('calendar.slotCreateError'))
    },
  })

  // Admin: Update slot
  const updateSlotMutation = useMutation({
    mutationFn: ({ id, params }: { id: string; params: { status?: string; note?: string; date?: string; startTime?: string; endTime?: string } }) =>
      adminApi.updateSlot(id, params),
    onSuccess: () => {
      showToast('success', t('calendar.slotUpdated'))
      invalidateCalendarQueries()
      options.onAdminSlotSuccess?.()
    },
    onError: () => {
      showToast('error', t('calendar.slotUpdateError'))
    },
  })

  // Admin: Delete slot
  const deleteSlotMutation = useMutation({
    mutationFn: adminApi.deleteSlot,
    onSuccess: () => {
      showToast('success', t('calendar.slotDeleted'))
      invalidateCalendarQueries()
      options.onAdminSlotSuccess?.()
    },
    onError: () => {
      showToast('error', t('calendar.slotDeleteError'))
    },
  })

  // Admin: Unlock week
  const unlockWeekMutation = useMutation({
    mutationFn: adminApi.unlockWeek,
    onSuccess: (data) => {
      showToast('success', t('calendar.unlockedSlots', { count: data.unlockedCount }))
      invalidateCalendarQueries()
    },
    onError: () => {
      showToast('error', t('calendar.unlockWeekError'))
    },
  })

  // Admin: Apply template
  const applyTemplateMutation = useMutation({
    mutationFn: ({ templateId, weekStartDate }: { templateId: string; weekStartDate: string }) =>
      adminApi.applyTemplate(templateId, weekStartDate),
    onSuccess: (data) => {
      showToast('success', t('calendar.templateApplied', { count: data.createdSlots }))
      invalidateCalendarQueries()
      options.onAdminSlotSuccess?.()
    },
    onError: () => {
      showToast('error', t('calendar.templateApplyError'))
    },
  })

  // Admin: Create reservation for user
  const adminCreateReservationMutation = useMutation({
    mutationFn: adminApi.createReservation,
    onSuccess: () => {
      showToast('success', t('calendar.reservationCreated'))
      invalidateCalendarQueries()
      options.onAdminReservationSuccess?.()
    },
    onError: (error: { response?: { data?: { error?: string } } }) => {
      showToast('error', error.response?.data?.error || t('calendar.reservationCreateError'))
    },
  })

  // Admin: Cancel reservation
  const adminCancelReservationMutation = useMutation({
    mutationFn: ({ id, refund }: { id: string; refund: boolean }) =>
      adminApi.cancelReservation(id, refund),
    onSuccess: () => {
      showToast('success', t('calendar.reservationCancelled'))
      invalidateCalendarQueries()
      options.onAdminReservationSuccess?.()
    },
    onError: () => {
      showToast('error', t('calendar.reservationCancelError'))
    },
  })

  // Admin: Update reservation note
  const updateNoteMutation = useMutation({
    mutationFn: ({ id, note }: { id: string; note: string | null }) =>
      adminApi.updateReservationNote(id, note),
    onSuccess: () => {
      showToast('success', t('calendar.noteSaved'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
    },
    onError: () => {
      showToast('error', t('calendar.noteSaveError'))
    },
  })

  return {
    // User mutations
    createReservation: createReservationMutation,
    cancelReservation: cancelReservationMutation,
    // Admin mutations
    createSlot: createSlotMutation,
    updateSlot: updateSlotMutation,
    deleteSlot: deleteSlotMutation,
    unlockWeek: unlockWeekMutation,
    applyTemplate: applyTemplateMutation,
    adminCreateReservation: adminCreateReservationMutation,
    adminCancelReservation: adminCancelReservationMutation,
    updateNote: updateNoteMutation,
  }
}
