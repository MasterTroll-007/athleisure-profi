import { useState, useCallback, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import type { EventClickArg } from '@fullcalendar/core'
import type { CalendarSlot } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { useAuthStore } from '@/stores/authStore'
import { useCalendarQueries } from '@/hooks/useCalendarQueries'
import { useCalendarMutations } from '@/hooks/useCalendarMutations'
import { useCalendarEvents } from '@/hooks/useCalendarEvents'
import { useUserSearch } from '@/hooks/useUserSearch'
import { useAdminSlotSelection } from '@/hooks/useAdminSlotSelection'
import { useUserBooking } from '@/hooks/useUserBooking'
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
import type { DateClickArg, EventDropArg } from '@/types/calendar'
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
  const { events, calendarSlots, getSlotsForDay } = useCalendarEvents({
    isAdmin,
    user,
    slotsResponse: queries.slotsResponse,
    adminSlots: queries.adminSlots,
    myReservations: queries.myReservations,
  })

  // Handle slot click from InfiniteScrollCalendar (mobile)
  const handleCalendarSlotClick = useCallback((slot: CalendarSlot) => {
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

  // Handle date click (admin only - create slot)
  const handleCalendarDateClick = useCallback((date: string, time: string) => {
    if (!isAdmin || isViewLocked) return
    adminSlot.openCreateModal(date, time)
  }, [isAdmin, isViewLocked, adminSlot])

  // Handle date range change
  const handleDateRangeChange = useCallback((start: string, end: string) => {
    setDateRange({ start, end })
  }, [])

  // Handle event click (desktop FullCalendar)
  const handleEventClick = useCallback((info: EventClickArg) => {
    if (isAdmin) {
      const adminSlotData = info.event.extendedProps.adminSlot as Slot
      adminSlot.selectSlot(adminSlotData)
      userSearch.reset()
    } else {
      if (info.event.extendedProps.type === 'reservation') {
        const reservation = info.event.extendedProps.reservation as Reservation
        const reservationDateTime = new Date(`${reservation.date}T${reservation.startTime}`)
        const hoursUntil = (reservationDateTime.getTime() - Date.now()) / (1000 * 60 * 60)

        if (hoursUntil < 24) {
          showToast('error', t('myReservations.cancelTooLate'))
          return
        }

        userBooking.openCancelModal(reservation)
        return
      }

      const slot = info.event.extendedProps.slot as AvailableSlot
      if (!slot || !slot.isAvailable) {
        showToast('error', t('calendar.slotNotAvailable'))
        return
      }
      if ((user?.credits || 0) < 1) {
        showToast('error', t('reservation.notEnoughCredits'))
        return
      }
      userBooking.openBookingModal(slot)
    }
  }, [isAdmin, user, showToast, t, adminSlot, userSearch, userBooking])

  // Handle date click for desktop
  const handleDateClick = useCallback((info: DateClickArg) => {
    if (!isAdmin || isViewLocked) return
    const date = info.dateStr.split('T')[0]
    const time = info.dateStr.includes('T') ? info.dateStr.split('T')[1].substring(0, 5) : '09:00'
    adminSlot.openCreateModal(date, time)
  }, [isAdmin, isViewLocked, adminSlot])

  // Handle event drop (admin drag-drop)
  const handleEventDrop = useCallback((info: EventDropArg) => {
    if (!isAdmin) return
    const slot = info.event.extendedProps.adminSlot as Slot
    if (!info.event.start || !slot) {
      info.revert()
      return
    }

    const newDate = info.event.start
    const newDateStr = formatDateLocal(newDate)
    const newStartTime = `${newDate.getHours().toString().padStart(2, '0')}:${newDate.getMinutes().toString().padStart(2, '0')}`

    const durationMinutes = slot.durationMinutes || 60
    const totalMinutes = newDate.getHours() * 60 + newDate.getMinutes() + durationMinutes
    const endHours = Math.floor(totalMinutes / 60) % 24
    const endMinutes = totalMinutes % 60
    const newEndTime = `${endHours.toString().padStart(2, '0')}:${endMinutes.toString().padStart(2, '0')}`

    mutations.updateSlot.mutate(
      { id: slot.id, params: { date: newDateStr, startTime: newStartTime, endTime: newEndTime } },
      {
        onError: () => {
          info.revert()
          showToast('error', t('calendar.slotMoveError'))
        },
      }
    )
  }, [isAdmin, mutations.updateSlot, showToast, t])

  // Handle dates set (desktop calendar navigation)
  const handleDatesSet = useCallback((info: { startStr: string; endStr: string; start: Date; view: { type: string } }) => {
    setCurrentDate(info.start)
    setDateRange({
      start: info.startStr.split('T')[0],
      end: info.endStr.split('T')[0],
    })
  }, [])

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
      blockId: slot.blockId,
      pricingItemId: queries.defaultPricing?.id,
    })
  }, [userBooking.selectedSlot, mutations.createReservation, queries.defaultPricing])

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
    })
  }, [mutations.createSlot, adminSlot])

  const handleApplyTemplate = useCallback(() => {
    if (!adminSlot.selectedTemplateId) return
    // Get week monday for template application
    const baseDate = currentDate
    const day = baseDate.getDay()
    const diff = day === 0 ? -6 : 1 - day
    const monday = new Date(baseDate)
    monday.setDate(baseDate.getDate() + diff)
    mutations.applyTemplate.mutate({
      templateId: adminSlot.selectedTemplateId,
      weekStartDate: formatDateLocal(monday),
    })
  }, [adminSlot.selectedTemplateId, currentDate, mutations.applyTemplate])

  const handleUnlockWeek = useCallback(() => {
    const baseDate = currentDate
    const day = baseDate.getDay()
    const diff = day === 0 ? -6 : 1 - day
    const monday = new Date(baseDate)
    monday.setDate(baseDate.getDate() + diff)
    mutations.unlockWeek.mutate(formatDateLocal(monday))
  }, [currentDate, mutations.unlockWeek])

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
      blockId: adminSlot.selectedAdminSlot.id,
      deductCredits: adminSlot.deductCredits,
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

  // Render modals
  const renderModals = () => (
    <>
      {/* User Booking Modal */}
      <BookingConfirmModal
        isOpen={userBooking.isModalOpen}
        slot={userBooking.selectedSlot}
        userCredits={user?.credits || 0}
        isLoading={mutations.createReservation.isPending}
        onConfirm={handleConfirmBooking}
        onClose={userBooking.closeBookingModal}
      />

      {/* User Cancel Modal */}
      <CancelReservationModal
        isOpen={userBooking.isCancelModalOpen}
        reservation={userBooking.selectedReservation}
        isLoading={mutations.cancelReservation.isPending}
        onConfirm={handleCancelReservation}
        onClose={userBooking.closeCancelModal}
      />

      {/* Admin Create Slot Modal */}
      <AdminCreateSlotModal
        isOpen={adminSlot.showCreateModal}
        date={adminSlot.createDate}
        time={adminSlot.createTime}
        duration={adminSlot.createDuration}
        note={adminSlot.createNote}
        isLoading={mutations.createSlot.isPending}
        onDateChange={adminSlot.setCreateDate}
        onTimeChange={adminSlot.setCreateTime}
        onDurationChange={adminSlot.setCreateDuration}
        onNoteChange={adminSlot.setCreateNote}
        onSubmit={handleCreateSlot}
        onClose={adminSlot.closeCreateModal}
      />

      {/* Admin Template Modal */}
      <AdminTemplateModal
        isOpen={adminSlot.showTemplateModal}
        templates={queries.templates}
        selectedTemplateId={adminSlot.selectedTemplateId}
        isLoading={mutations.applyTemplate.isPending}
        onSelectTemplate={adminSlot.setSelectedTemplateId}
        onApply={handleApplyTemplate}
        onClose={adminSlot.closeTemplateModal}
      />

      {/* Admin User Search Overlay */}
      <AdminUserSearchOverlay
        isOpen={userSearch.showUserSearch}
        searchQuery={userSearch.searchQuery}
        searchResults={userSearch.searchResults}
        isSearching={userSearch.isSearching}
        searchInputRef={userSearch.searchInputRef as any}
        onSearchChange={userSearch.setSearchQuery}
        onSelectUser={(u) => {
          userSearch.handleSelectUser(u)
          userSearch.closeUserSearch()
        }}
        onClose={userSearch.closeUserSearch}
      />

      {/* Admin Slot Detail Modal */}
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
        isUpdating={mutations.updateSlot.isPending}
        isDeleting={mutations.deleteSlot.isPending}
        isCreatingReservation={mutations.adminCreateReservation.isPending}
        showCancelConfirm={adminSlot.showCancelConfirm}
        cancelWithRefund={adminSlot.cancelWithRefund}
        onCloseCancelConfirm={adminSlot.closeCancelConfirm}
        onConfirmCancel={handleAdminCancelReservation}
        isCancelling={mutations.adminCancelReservation.isPending}
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
          isAdmin={isAdmin}
          isLoading={queries.isLoading && !queries.slotsResponse && !queries.adminSlots}
          isFetching={queries.isFetching}
          isViewLocked={isViewLocked}
          getSlotsForDay={getSlotsForDay}
          onSlotClick={handleCalendarSlotClick}
          onDateClick={handleCalendarDateClick}
          onDateRangeChange={handleDateRangeChange}
          onCurrentDateChange={setCurrentDate}
          onLockToggle={() => setIsViewLocked(!isViewLocked)}
          onTemplateClick={adminSlot.openTemplateModal}
          onUnlockWeek={handleUnlockWeek}
          unlockWeekLoading={mutations.unlockWeek.isPending}
        />
        {renderModals()}
      </>
    )
  }

  // Desktop layout
  return (
    <>
      <DesktopCalendarView
        events={events}
        currentDate={currentDate}
        calendarSettings={queries.calendarSettings}
        isAdmin={isAdmin}
        userCredits={user?.credits || 0}
        isLoading={queries.isLoading && !queries.slotsResponse && !queries.adminSlots}
        isFetching={queries.isFetching}
        isViewLocked={isViewLocked}
        getSlotsForDay={getSlotsForDay}
        onEventClick={handleEventClick}
        onDateClick={handleDateClick}
        onEventDrop={handleEventDrop}
        onDatesSet={handleDatesSet}
        onLockToggle={() => setIsViewLocked(!isViewLocked)}
        onTemplateClick={adminSlot.openTemplateModal}
        onUnlockWeek={handleUnlockWeek}
        unlockWeekLoading={mutations.unlockWeek.isPending}
      />
      {renderModals()}
    </>
  )
}
