import { useState, useCallback, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import type { CalendarSlot } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { useAuthStore } from '@/stores/authStore'
import { useCalendarQueries } from '@/hooks/useCalendarQueries'
import { useCalendarMutations } from '@/hooks/useCalendarMutations'
import { useCalendarEvents } from '@/hooks/useCalendarEvents'
import { useUserSearch } from '@/hooks/useUserSearch'
import { useAdminSlotSelection } from '@/hooks/useAdminSlotSelection'
import { useUserBooking } from '@/hooks/useUserBooking'
import { getWeekStartForVisibleRange } from '@/utils/calendarWeek'
import {
  MobileCalendarView,
  DesktopCalendarView,
  BookingConfirmModal,
  CancelReservationModal,
  AdminCreateSlotModal,
  AdminTemplateModal,
  AdminUserSearchOverlay,
  AdminSlotDetailModal,
} from '@/components/calendar'
import type { AvailableSlot, Reservation, Slot } from '@/types/api'

// Format date to ISO string (YYYY-MM-DD) using local timezone
const formatDateLocal = (date: Date): string => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export default function NewReservation() {
  const { t } = useTranslation()
  const { user } = useAuthStore()
  const { showToast } = useToast()

  const isAdmin = user?.role === 'admin'

  // View state
  const [currentDate, setCurrentDate] = useState<Date>(new Date())
  const [dateRange, setDateRange] = useState({
    start: formatDateLocal(new Date()),
    end: formatDateLocal(new Date(Date.now() + 14 * 24 * 60 * 60 * 1000)),
  })
  const [visibleDateRange, setVisibleDateRange] = useState({
    start: formatDateLocal(new Date()),
    end: formatDateLocal(new Date()),
  })
  const [isMobile, setIsMobile] = useState(typeof window !== 'undefined' && window.innerWidth < 640)
  const [isViewLocked, setIsViewLocked] = useState(false)

  // Mobile detection
  useEffect(() => {
    const handleResize = () => {
      setIsMobile(window.innerWidth < 640)
    }
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  // User booking state
  const userBooking = useUserBooking()

  // Admin slot selection state
  const adminSlot = useAdminSlotSelection()

  // User search state
  const userSearch = useUserSearch()

  // Queries
  const queries = useCalendarQueries({ isAdmin, dateRange })

  // Mutations with callbacks
  const mutations = useCalendarMutations({
    onUserBookingSuccess: () => {
      userBooking.closeBookingModal()
    },
    onUserCancelSuccess: () => {
      userBooking.closeCancelModal()
    },
    onAdminSlotSuccess: () => {
      adminSlot.closeSlotModal()
      adminSlot.closeCreateModal()
      adminSlot.closeTemplateModal()
      userSearch.reset()
    },
    onAdminReservationSuccess: () => {
      adminSlot.closeSlotModal()
      userSearch.reset()
    },
  })

  // Events
  const { calendarSlots, getSlotsForDay } = useCalendarEvents({
    isAdmin,
    user,
    calendarSettings: queries.calendarSettings,
    slotsResponse: queries.slotsResponse,
    adminSlots: queries.adminSlots,
    myReservations: queries.myReservations,
  })

  // Shared slot click handler (used by both mobile and desktop)
  const handleSlotClick = useCallback((slot: CalendarSlot) => {
    if (isAdmin) {
      const adminSlotData = slot.data.adminSlot as Slot
      if (adminSlotData) {
        adminSlot.selectSlot(adminSlotData)
        userSearch.reset()
      }
    } else {
      if (slot.data.type === 'reservation') {
        const reservation = slot.data.reservation as Reservation
        const reservationDateTime = new Date(`${reservation.date}T${reservation.startTime}`)
        const hoursUntil = (reservationDateTime.getTime() - Date.now()) / (1000 * 60 * 60)

        if (hoursUntil < 24) {
          showToast('error', t('myReservations.cancelTooLate'))
          return
        }

        userBooking.openCancelModal(reservation)
        return
      }

      const availableSlot = slot.data.slot as AvailableSlot
      if (!availableSlot || !availableSlot.isAvailable) {
        showToast('error', t('calendar.slotNotAvailable'))
        return
      }
      if ((user?.credits || 0) < 1) {
        showToast('error', t('reservation.notEnoughCredits'))
        return
      }
      userBooking.openBookingModal(availableSlot)
    }
  }, [isAdmin, user, showToast, t, adminSlot, userSearch, userBooking])

  // Shared date click handler (admin only - create slot)
  const handleDateClick = useCallback((date: string, time: string) => {
    if (!isAdmin || isViewLocked) return
    adminSlot.openCreateModal(date, time)
  }, [isAdmin, isViewLocked, adminSlot])

  // Handle date range change (mobile)
  const handleDateRangeChange = useCallback((start: string, end: string) => {
    setDateRange({ start, end })
  }, [])

  const handleVisibleDateRangeChange = useCallback((start: string, end: string) => {
    setVisibleDateRange({ start, end })
  }, [])

  // Handle dates change (desktop)
  const handleDatesChange = useCallback((start: string, end: string, startDate: Date) => {
    setCurrentDate(startDate)
    setDateRange({ start, end })
    setVisibleDateRange({ start, end })
  }, [])

  // Handle slot drop (desktop admin drag-drop)
  const handleSlotDrop = useCallback((slot: CalendarSlot, newDate: string, newStartTime: string) => {
    if (!isAdmin) return
    const adminSlotData = slot.data.adminSlot as Slot
    if (!adminSlotData) return

    const durationMinutes = adminSlotData.durationMinutes || 60
    const [h, m] = newStartTime.split(':').map(Number)
    const totalMinutes = h * 60 + m + durationMinutes
    const endHours = Math.floor(totalMinutes / 60) % 24
    const endMinutes = totalMinutes % 60
    const newEndTime = `${endHours.toString().padStart(2, '0')}:${endMinutes.toString().padStart(2, '0')}`

    mutations.updateSlot.mutate(
      { id: adminSlotData.id, params: { date: newDate, startTime: newStartTime, endTime: newEndTime } },
      { onError: () => showToast('error', t('calendar.slotMoveError')) }
    )
  }, [isAdmin, mutations.updateSlot, showToast, t])

  // User booking handlers
  const handleConfirmBooking = useCallback(() => {
    if (!userBooking.selectedSlot) return
    const slot = userBooking.selectedSlot
    const date = slot.start.split('T')[0]
    const startTime = slot.start.split('T')[1].substring(0, 5)
    const endTime = slot.end.split('T')[1].substring(0, 5)

    mutations.createReservation.mutate({
      date,
      startTime,
      endTime,
      slotId: slot.slotId,
      pricingItemId: userBooking.selectedPricingItemId ?? undefined,
    })
  }, [userBooking, mutations.createReservation])

  const handleCancelReservation = useCallback(() => {
    if (!userBooking.selectedReservation) return
    mutations.cancelReservation.mutate(userBooking.selectedReservation.id)
  }, [userBooking.selectedReservation, mutations.cancelReservation])

  // Admin slot handlers
  const handleCreateSlot = useCallback(() => {
    mutations.createSlot.mutate({
      date: adminSlot.createDate,
      startTime: adminSlot.createTime,
      durationMinutes: adminSlot.createDuration,
      note: adminSlot.createNote || undefined,
      pricingItemIds: adminSlot.createPricingItemIds.length > 0 ? adminSlot.createPricingItemIds : undefined,
      locationId: adminSlot.createLocationId,
    })
  }, [mutations.createSlot, adminSlot])

  const handleApplyTemplate = useCallback(() => {
    if (!adminSlot.selectedTemplateId) return
    const weekStartDate = getWeekStartForVisibleRange(visibleDateRange)
    mutations.applyTemplate.mutate({
      templateId: adminSlot.selectedTemplateId,
      weekStartDate,
    })
  }, [adminSlot.selectedTemplateId, visibleDateRange, mutations.applyTemplate])

  const handleUnlockWeek = useCallback(() => {
    mutations.unlockWeek.mutate({
      weekStartDate: dateRange.start,
      endDate: dateRange.end,
    })
  }, [dateRange, mutations.unlockWeek])

  const handleAdminSlotAction = useCallback((action: 'unlock' | 'lock' | 'delete') => {
    if (!adminSlot.selectedAdminSlot) return
    switch (action) {
      case 'unlock':
        mutations.updateSlot.mutate({ id: adminSlot.selectedAdminSlot.id, params: { status: 'unlocked' } })
        break
      case 'lock':
        mutations.updateSlot.mutate({ id: adminSlot.selectedAdminSlot.id, params: { status: 'locked' } })
        break
      case 'delete':
        mutations.deleteSlot.mutate(adminSlot.selectedAdminSlot.id)
        break
    }
  }, [adminSlot.selectedAdminSlot, mutations.updateSlot, mutations.deleteSlot])

  const handleCreateAdminReservation = useCallback(() => {
    if (!adminSlot.selectedAdminSlot || !userSearch.selectedUser) return
    mutations.adminCreateReservation.mutate({
      userId: userSearch.selectedUser.id,
      date: adminSlot.selectedAdminSlot.date,
      startTime: adminSlot.selectedAdminSlot.startTime,
      endTime: adminSlot.selectedAdminSlot.endTime,
      slotId: adminSlot.selectedAdminSlot.id,
      deductCredits: adminSlot.deductCredits,
      pricingItemId: adminSlot.selectedAdminSlot.pricingItems.length === 1
        ? adminSlot.selectedAdminSlot.pricingItems[0].id
        : undefined,
    })
  }, [adminSlot.selectedAdminSlot, adminSlot.deductCredits, userSearch.selectedUser, mutations.adminCreateReservation])

  const handleAdminCancelReservation = useCallback(() => {
    if (!adminSlot.selectedAdminSlot?.reservationId) return
    mutations.adminCancelReservation.mutate({
      id: adminSlot.selectedAdminSlot.reservationId,
      refund: adminSlot.cancelWithRefund,
    })
  }, [adminSlot.selectedAdminSlot, adminSlot.cancelWithRefund, mutations.adminCancelReservation])

  const handleSaveNote = useCallback(() => {
    if (!adminSlot.selectedAdminSlot?.reservationId) return
    mutations.updateNote.mutate({
      id: adminSlot.selectedAdminSlot.reservationId,
      note: adminSlot.noteText || null,
    })
    adminSlot.setIsEditingNote(false)
  }, [adminSlot, mutations.updateNote])

  const handleSaveSlotEdit = useCallback(() => {
    const current = adminSlot.selectedAdminSlot
    if (!current) return
    // Compute end time from start + duration so the grid placement matches.
    const [h, m] = adminSlot.editTime.split(':').map(Number)
    const total = h * 60 + m + adminSlot.editDuration
    const endH = Math.floor(total / 60) % 24
    const endM = total % 60
    const endTime = `${endH.toString().padStart(2, '0')}:${endM.toString().padStart(2, '0')}`

    const params: {
      date: string
      startTime: string
      endTime: string
      pricingItemIds: string[]
      locationId?: string | null
      clearLocation?: boolean
    } = {
      date: adminSlot.editDate,
      startTime: adminSlot.editTime,
      endTime,
      pricingItemIds: adminSlot.editPricingItemIds,
    }
    if (adminSlot.editLocationId) {
      params.locationId = adminSlot.editLocationId
    } else {
      params.clearLocation = true
    }
    mutations.updateSlot.mutate(
      { id: current.id, params },
      { onSuccess: () => adminSlot.cancelEditSlot() }
    )
  }, [adminSlot, mutations.updateSlot])

  const handleRescheduleReservation = useCallback((date: string, time: string, duration: number) => {
    const reservationId = adminSlot.selectedAdminSlot?.reservationId
    if (!reservationId) return
    const [h, m] = time.split(':').map(Number)
    const total = h * 60 + m + duration
    const endTime = `${String(Math.floor(total / 60) % 24).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`
    mutations.rescheduleReservation.mutate({
      id: reservationId,
      params: {
        date,
        startTime: time,
        endTime,
        createSlotIfMissing: true,
      },
    }, {
      onSuccess: () => adminSlot.cancelReschedule(),
    })
  }, [adminSlot, mutations.rescheduleReservation])

  // Render modals
  const renderModals = () => (
    <>
      <BookingConfirmModal
        isOpen={userBooking.isModalOpen}
        slot={userBooking.selectedSlot}
        userCredits={user?.credits || 0}
        isLoading={mutations.createReservation.isPending}
        pricingItems={userBooking.selectedSlot?.pricingItems ?? []}
        selectedPricingItemId={userBooking.selectedPricingItemId}
        onPricingItemChange={userBooking.setSelectedPricingItemId}
        onConfirm={handleConfirmBooking}
        onClose={userBooking.closeBookingModal}
      />

      <CancelReservationModal
        isOpen={userBooking.isCancelModalOpen}
        reservation={userBooking.selectedReservation}
        isLoading={mutations.cancelReservation.isPending}
        onConfirm={handleCancelReservation}
        onClose={userBooking.closeCancelModal}
      />

      <AdminCreateSlotModal
        isOpen={adminSlot.showCreateModal}
        date={adminSlot.createDate}
        time={adminSlot.createTime}
        duration={adminSlot.createDuration}
        note={adminSlot.createNote}
        pricingItems={queries.pricingItems}
        selectedPricingItemIds={adminSlot.createPricingItemIds}
        locationId={adminSlot.createLocationId}
        isLoading={mutations.createSlot.isPending}
        onDateChange={adminSlot.setCreateDate}
        onTimeChange={adminSlot.setCreateTime}
        onDurationChange={adminSlot.setCreateDuration}
        onNoteChange={adminSlot.setCreateNote}
        onPricingItemIdsChange={adminSlot.setCreatePricingItemIds}
        onLocationIdChange={adminSlot.setCreateLocationId}
        onSubmit={handleCreateSlot}
        onClose={adminSlot.closeCreateModal}
      />

      <AdminTemplateModal
        isOpen={adminSlot.showTemplateModal}
        templates={queries.templates}
        selectedTemplateId={adminSlot.selectedTemplateId}
        isLoading={mutations.applyTemplate.isPending}
        onSelectTemplate={adminSlot.setSelectedTemplateId}
        onApply={handleApplyTemplate}
        onClose={adminSlot.closeTemplateModal}
      />

      <AdminUserSearchOverlay
        isOpen={userSearch.showUserSearch}
        searchQuery={userSearch.searchQuery}
        searchResults={userSearch.searchResults}
        isSearching={userSearch.isSearching}
        searchInputRef={userSearch.searchInputRef}
        onSearchChange={userSearch.setSearchQuery}
        onSelectUser={(u) => {
          userSearch.handleSelectUser(u)
          userSearch.closeUserSearch()
        }}
        onClose={userSearch.closeUserSearch}
      />

      <AdminSlotDetailModal
        isOpen={!!adminSlot.selectedAdminSlot}
        slot={adminSlot.selectedAdminSlot}
        noteText={adminSlot.noteText}
        isEditingNote={adminSlot.isEditingNote}
        onNoteChange={adminSlot.setNoteText}
        onStartEditNote={() => adminSlot.setIsEditingNote(true)}
        onCancelEditNote={() => {
          adminSlot.setIsEditingNote(false)
          adminSlot.setNoteText(adminSlot.selectedAdminSlot?.note || '')
        }}
        onSaveNote={handleSaveNote}
        isNoteSaving={mutations.updateNote.isPending}
        selectedUser={userSearch.selectedUser}
        deductCredits={adminSlot.deductCredits}
        onDeductCreditsChange={adminSlot.setDeductCredits}
        onClearUser={userSearch.clearSelectedUser}
        onOpenUserSearch={userSearch.openUserSearch}
        onUnlockSlot={() => handleAdminSlotAction('unlock')}
        onLockSlot={() => handleAdminSlotAction('lock')}
        onDeleteSlot={() => handleAdminSlotAction('delete')}
        onCreateReservation={handleCreateAdminReservation}
        onOpenCancelConfirm={adminSlot.openCancelConfirm}
        isEditingSlot={adminSlot.isEditingSlot}
        editDate={adminSlot.editDate}
        editTime={adminSlot.editTime}
        editDuration={adminSlot.editDuration}
        editLocationId={adminSlot.editLocationId}
        editPricingItemIds={adminSlot.editPricingItemIds}
        pricingItems={queries.pricingItems}
        onStartEditSlot={adminSlot.startEditSlot}
        onCancelEditSlot={adminSlot.cancelEditSlot}
        onSaveSlotEdit={handleSaveSlotEdit}
        isRescheduling={adminSlot.isRescheduling}
        rescheduleDate={adminSlot.rescheduleDate}
        rescheduleTime={adminSlot.rescheduleTime}
        rescheduleDuration={adminSlot.rescheduleDuration}
        onCancelReschedule={adminSlot.cancelReschedule}
        onSaveReschedule={handleRescheduleReservation}
        onRescheduleDateChange={adminSlot.setRescheduleDate}
        onRescheduleTimeChange={adminSlot.setRescheduleTime}
        onRescheduleDurationChange={adminSlot.setRescheduleDuration}
        onEditDateChange={adminSlot.setEditDate}
        onEditTimeChange={adminSlot.setEditTime}
        onEditDurationChange={adminSlot.setEditDuration}
        onEditLocationIdChange={adminSlot.setEditLocationId}
        onEditPricingItemIdsChange={adminSlot.setEditPricingItemIds}
        isUpdating={mutations.updateSlot.isPending}
        isDeleting={mutations.deleteSlot.isPending}
        isCreatingReservation={mutations.adminCreateReservation.isPending}
        isReschedulingReservation={mutations.rescheduleReservation.isPending}
        showCancelConfirm={adminSlot.showCancelConfirm}
        cancelWithRefund={adminSlot.cancelWithRefund}
        onCloseCancelConfirm={adminSlot.closeCancelConfirm}
        onConfirmCancel={handleAdminCancelReservation}
        isCancelling={mutations.adminCancelReservation.isPending}
        isObscured={userSearch.showUserSearch}
        onClose={() => {
          adminSlot.closeSlotModal()
          userSearch.reset()
        }}
      />
    </>
  )

  // Mobile layout
  if (isMobile) {
    return (
      <>
        <MobileCalendarView
          calendarSlots={calendarSlots}
          currentDate={currentDate}
          calendarSettings={queries.calendarSettings}
          locations={queries.locations}
          isAdmin={isAdmin}
          isLoading={queries.isLoading && !queries.slotsResponse && !queries.adminSlots}
          isFetching={queries.isFetching}
          isViewLocked={isViewLocked}
          getSlotsForDay={getSlotsForDay}
          onSlotClick={handleSlotClick}
          onDateClick={handleDateClick}
          onDateRangeChange={handleDateRangeChange}
          onVisibleDateRangeChange={handleVisibleDateRangeChange}
          onCurrentDateChange={setCurrentDate}
          onLockToggle={() => setIsViewLocked(!isViewLocked)}
          onTemplateClick={adminSlot.openTemplateModal}
          onUnlockWeek={handleUnlockWeek}
          unlockWeekLoading={mutations.unlockWeek.isPending}
          onSlotDrop={handleSlotDrop}
        />
        {renderModals()}
      </>
    )
  }

  // Desktop layout
  return (
    <>
      <DesktopCalendarView
        slots={calendarSlots}
        currentDate={currentDate}
        calendarSettings={queries.calendarSettings}
        locations={queries.locations}
        isAdmin={isAdmin}
        userCredits={user?.credits || 0}
        isLoading={queries.isLoading && !queries.slotsResponse && !queries.adminSlots}
        isFetching={queries.isFetching}
        isViewLocked={isViewLocked}
        getSlotsForDay={getSlotsForDay}
        onSlotClick={handleSlotClick}
        onDateClick={handleDateClick}
        onSlotDrop={handleSlotDrop}
        onDatesChange={handleDatesChange}
        onLockToggle={() => setIsViewLocked(!isViewLocked)}
        onTemplateClick={adminSlot.openTemplateModal}
        onUnlockWeek={handleUnlockWeek}
        unlockWeekLoading={mutations.unlockWeek.isPending}
      />
      {renderModals()}
    </>
  )
}
