import { useState, useRef, useEffect, useMemo, useCallback } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import csLocale from '@fullcalendar/core/locales/cs'
import { CreditCard, Search, X, UserPlus, UserMinus, Lock, Unlock, LayoutTemplate, Calendar as CalendarIcon } from 'lucide-react'
import { Card, Button, Modal, Spinner, Badge, Input } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { reservationApi, creditApi, adminApi } from '@/services/api'
import { useAuthStore } from '@/stores/authStore'
import { formatTime } from '@/utils/formatters'
import type { EventClickArg } from '@fullcalendar/core'
import type { AvailableSlot, Reservation, Slot, User } from '@/types/api'

interface DateClickArg {
  date: Date
  dateStr: string
  allDay: boolean
}

interface EventDropArg {
  event: {
    id: string
    start: Date | null
    extendedProps: { slot?: Slot; adminSlot?: Slot }
  }
  revert: () => void
}

interface CalendarEvent {
  id: string
  title: string
  start: string
  end: string
  backgroundColor: string
  borderColor: string
  textColor: string
  extendedProps: {
    slot?: AvailableSlot
    reservation?: Reservation
    adminSlot?: Slot
    type: 'slot' | 'reservation' | 'adminSlot'
  }
}

export default function NewReservation() {
  const { t, i18n } = useTranslation()
  const { user, updateUser } = useAuthStore()
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const calendarRef = useRef<FullCalendar>(null)

  const isAdmin = user?.role === 'admin'

  // Common state
  const [currentDate, setCurrentDate] = useState<Date>(new Date())
  const [dateRange, setDateRange] = useState<{ start: string; end: string }>({
    start: new Date().toISOString().split('T')[0],
    end: new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
  })

  // User booking state
  const [selectedSlot, setSelectedSlot] = useState<AvailableSlot | null>(null)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [selectedReservation, setSelectedReservation] = useState<Reservation | null>(null)
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false)

  // Admin state
  const [selectedAdminSlot, setSelectedAdminSlot] = useState<Slot | null>(null)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [createDate, setCreateDate] = useState('')
  const [createTime, setCreateTime] = useState('09:00')
  const [createDuration, setCreateDuration] = useState(60)
  const [createNote, setCreateNote] = useState('')
  const [showTemplateModal, setShowTemplateModal] = useState(false)
  const [selectedTemplateId, setSelectedTemplateId] = useState<string | null>(null)
  const [showUserSearch, setShowUserSearch] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState<User[]>([])
  const [isSearching, setIsSearching] = useState(false)
  const [selectedUser, setSelectedUser] = useState<User | null>(null)
  const [deductCredits, setDeductCredits] = useState(false)
  const [noteText, setNoteText] = useState('')
  const [isEditingNote, setIsEditingNote] = useState(false)
  const [showCancelConfirm, setShowCancelConfirm] = useState(false)
  const [cancelWithRefund, setCancelWithRefund] = useState(true)
  const [isViewLocked, setIsViewLocked] = useState(false)
  const [showWeekends, setShowWeekends] = useState(false)
  const searchInputRef = useRef<HTMLInputElement>(null)

  // Queries - different data sources for admin vs user
  const { data: slotsResponse, isLoading: isUserLoading } = useQuery({
    queryKey: ['availableSlots', 'range', dateRange],
    queryFn: () => reservationApi.getAvailableSlotsRange(dateRange.start, dateRange.end),
    enabled: !isAdmin,
  })

  const { data: adminSlots, isLoading: isAdminLoading, isFetching } = useQuery({
    queryKey: ['admin', 'slots', dateRange],
    queryFn: () => adminApi.getSlots(dateRange.start, dateRange.end),
    enabled: isAdmin,
    placeholderData: (previousData) => previousData,
    staleTime: 30000,
  })

  const { data: myReservations } = useQuery({
    queryKey: ['myReservations'],
    queryFn: () => reservationApi.getMyReservations(),
    enabled: !isAdmin,
  })

  const { data: pricingItems } = useQuery({
    queryKey: ['pricing'],
    queryFn: creditApi.getPricing,
  })

  const { data: templates } = useQuery({
    queryKey: ['admin', 'templates'],
    queryFn: () => adminApi.getTemplates(),
    enabled: isAdmin,
  })

  const defaultPricing = pricingItems?.find((p) => p.credits === 1)
  const isLoading = isAdmin ? isAdminLoading : isUserLoading

  // User mutations
  const createMutation = useMutation({
    mutationFn: reservationApi.create,
    onSuccess: () => {
      showToast('success', t('reservation.success'))
      setIsModalOpen(false)
      setSelectedSlot(null)
      queryClient.invalidateQueries({ queryKey: ['availableSlots'] })
      queryClient.invalidateQueries({ queryKey: ['myReservations'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      if (user) {
        updateUser({ ...user, credits: user.credits - 1 })
      }
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      showToast('error', error.response?.data?.message || t('errors.somethingWrong'))
    },
  })

  const cancelMutation = useMutation({
    mutationFn: reservationApi.cancel,
    onSuccess: (data) => {
      showToast('success', t('myReservations.cancelSuccess'))
      setIsCancelModalOpen(false)
      setSelectedReservation(null)
      queryClient.invalidateQueries({ queryKey: ['availableSlots'] })
      queryClient.invalidateQueries({ queryKey: ['myReservations'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      if (user) {
        updateUser({ ...user, credits: user.credits + data.creditsUsed })
      }
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      showToast('error', error.response?.data?.message || t('myReservations.cancelTooLate'))
      setIsCancelModalOpen(false)
      setSelectedReservation(null)
    },
  })

  // Admin mutations
  const createSlotMutation = useMutation({
    mutationFn: adminApi.createSlot,
    onSuccess: () => {
      showToast('success', t('calendar.slotCreated'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      queryClient.invalidateQueries({ queryKey: ['availableSlots'] })
      setShowCreateModal(false)
      resetCreateForm()
    },
    onError: (error: { response?: { data?: { error?: string } } }) => {
      showToast('error', error.response?.data?.error || t('calendar.slotCreateError'))
    },
  })

  const updateSlotMutation = useMutation({
    mutationFn: ({ id, params }: { id: string; params: { status?: string; note?: string; date?: string; startTime?: string; endTime?: string } }) =>
      adminApi.updateSlot(id, params),
    onSuccess: () => {
      showToast('success', t('calendar.slotUpdated'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      queryClient.invalidateQueries({ queryKey: ['availableSlots'] })
      queryClient.invalidateQueries({ queryKey: ['myReservations'] })
      setSelectedAdminSlot(null)
    },
    onError: () => {
      showToast('error', t('calendar.slotUpdateError'))
    },
  })

  const deleteSlotMutation = useMutation({
    mutationFn: adminApi.deleteSlot,
    onSuccess: () => {
      showToast('success', t('calendar.slotDeleted'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      queryClient.invalidateQueries({ queryKey: ['availableSlots'] })
      queryClient.invalidateQueries({ queryKey: ['myReservations'] })
      setSelectedAdminSlot(null)
    },
    onError: () => {
      showToast('error', t('calendar.slotDeleteError'))
    },
  })

  const unlockWeekMutation = useMutation({
    mutationFn: adminApi.unlockWeek,
    onSuccess: (data) => {
      showToast('success', t('calendar.unlockedSlots', { count: data.unlockedCount }))
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      queryClient.invalidateQueries({ queryKey: ['availableSlots'] })
    },
    onError: () => {
      showToast('error', t('calendar.unlockWeekError'))
    },
  })

  const applyTemplateMutation = useMutation({
    mutationFn: ({ templateId, weekStartDate }: { templateId: string; weekStartDate: string }) =>
      adminApi.applyTemplate(templateId, weekStartDate),
    onSuccess: (data) => {
      showToast('success', t('calendar.templateApplied', { count: data.createdSlots }))
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      queryClient.invalidateQueries({ queryKey: ['availableSlots'] })
      setShowTemplateModal(false)
      setSelectedTemplateId(null)
    },
    onError: () => {
      showToast('error', t('calendar.templateApplyError'))
    },
  })

  const createReservationMutation = useMutation({
    mutationFn: adminApi.createReservation,
    onSuccess: () => {
      showToast('success', t('calendar.reservationCreated'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      queryClient.invalidateQueries({ queryKey: ['availableSlots'] })
      queryClient.invalidateQueries({ queryKey: ['myReservations'] })
      handleCloseAdminModal()
    },
    onError: (error: { response?: { data?: { error?: string } } }) => {
      showToast('error', error.response?.data?.error || t('calendar.reservationCreateError'))
    },
  })

  const cancelReservationMutation = useMutation({
    mutationFn: ({ id, refund }: { id: string; refund: boolean }) =>
      adminApi.cancelReservation(id, refund),
    onSuccess: () => {
      showToast('success', t('calendar.reservationCancelled'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      queryClient.invalidateQueries({ queryKey: ['availableSlots'] })
      queryClient.invalidateQueries({ queryKey: ['myReservations'] })
      handleCloseAdminModal()
    },
    onError: () => {
      showToast('error', t('calendar.reservationCancelError'))
    },
  })

  const updateNoteMutation = useMutation({
    mutationFn: ({ id, note }: { id: string; note: string | null }) =>
      adminApi.updateReservationNote(id, note),
    onSuccess: () => {
      showToast('success', t('calendar.noteSaved'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      setIsEditingNote(false)
    },
    onError: () => {
      showToast('error', t('calendar.noteSaveError'))
    },
  })

  // Effects
  useEffect(() => {
    if (searchQuery.length < 2) {
      setSearchResults([])
      return
    }

    const timeoutId = setTimeout(async () => {
      setIsSearching(true)
      try {
        const results = await adminApi.searchClients(searchQuery)
        setSearchResults(results)
      } catch {
        setSearchResults([])
      } finally {
        setIsSearching(false)
      }
    }, 300)

    return () => clearTimeout(timeoutId)
  }, [searchQuery])

  useEffect(() => {
    if (showUserSearch && searchInputRef.current) {
      searchInputRef.current.focus()
    }
  }, [showUserSearch])

  useEffect(() => {
    if (!showWeekends) {
      const calendarApi = calendarRef.current?.getApi()
      if (calendarApi && calendarApi.view.type === 'timeGridWeek') {
        calendarApi.changeView('timeGrid5Day')
      }
    }
  }, [showWeekends])

  // Helper functions
  const getSlotColors = (slot: AvailableSlot) => {
    if (!slot.isAvailable) {
      if (slot.reservedByUserId && slot.reservedByUserId !== user?.id) {
        return { bg: '#fee2e2', border: '#ef4444', text: '#991b1b', label: t('calendar.reserved') }
      }
      return { bg: '#f3f4f6', border: '#9ca3af', text: '#6b7280', label: t('calendar.unavailable') }
    }
    return { bg: '#dcfce7', border: '#22c55e', text: '#166534', label: t('calendar.available') }
  }

  const getAdminSlotColors = useCallback((slot: Slot) => {
    switch (slot.status) {
      case 'reserved':
        return { bg: '#dbeafe', border: '#3b82f6', text: '#1e40af' }
      case 'locked':
        return { bg: '#e5e7eb', border: '#9ca3af', text: '#6b7280' }
      case 'unlocked':
      default:
        return { bg: '#dcfce7', border: '#22c55e', text: '#166534' }
    }
  }, [])

  // Build events based on user type
  const events: CalendarEvent[] = useMemo(() => {
    if (isAdmin) {
      // Admin view - use admin slots
      return adminSlots?.map((slot) => {
        const colors = getAdminSlotColors(slot)
        let title = ''
        switch (slot.status) {
          case 'reserved':
            title = slot.assignedUserName || slot.assignedUserEmail || t('calendar.reserved')
            break
          case 'locked':
            title = '🔒 ' + t('calendar.locked')
            break
          case 'unlocked':
            title = t('calendar.available')
            break
        }

        return {
          id: slot.id,
          title,
          start: `${slot.date}T${slot.startTime}`,
          end: `${slot.date}T${slot.endTime}`,
          backgroundColor: colors.bg,
          borderColor: colors.border,
          textColor: colors.text,
          extendedProps: { adminSlot: slot, type: 'adminSlot' },
        }
      }) || []
    } else {
      // User view - available slots + own reservations
      const confirmedReservations = myReservations?.filter(r => r.status === 'confirmed') || []

      const reservationEvents: CalendarEvent[] = confirmedReservations.map((reservation) => ({
        id: `reservation-${reservation.id}`,
        title: t('calendar.training'),
        start: `${reservation.date}T${reservation.startTime}`,
        end: `${reservation.date}T${reservation.endTime}`,
        backgroundColor: '#dbeafe',
        borderColor: '#3b82f6',
        textColor: '#1e40af',
        extendedProps: { reservation, type: 'reservation' },
      }))

      const slotEvents: CalendarEvent[] = slotsResponse?.slots
        ?.filter(slot => slot.reservedByUserId !== user?.id)
        .map((slot, index) => {
          const colors = getSlotColors(slot)
          return {
            id: `slot-${slot.blockId}-${index}`,
            title: colors.label,
            start: slot.start,
            end: slot.end,
            backgroundColor: colors.bg,
            borderColor: colors.border,
            textColor: colors.text,
            extendedProps: { slot, type: 'slot' },
          }
        }) || []

      return [...slotEvents, ...reservationEvents]
    }
  }, [isAdmin, adminSlots, slotsResponse, myReservations, user, t, getAdminSlotColors])

  // Event handlers
  const handleEventClick = (info: EventClickArg) => {
    if (isAdmin) {
      // Admin click handling
      const adminSlot = info.event.extendedProps.adminSlot as Slot
      setSelectedAdminSlot(adminSlot)
      setShowUserSearch(false)
      setSearchQuery('')
      setSelectedUser(null)
      setDeductCredits(false)
      setNoteText(adminSlot.note || '')
      setIsEditingNote(false)
    } else {
      // User click handling
      if (info.event.extendedProps.type === 'reservation') {
        const reservation = info.event.extendedProps.reservation as Reservation
        const reservationDateTime = new Date(`${reservation.date}T${reservation.startTime}`)
        const hoursUntil = (reservationDateTime.getTime() - Date.now()) / (1000 * 60 * 60)

        if (hoursUntil < 24) {
          showToast('error', t('myReservations.cancelTooLate'))
          return
        }

        setSelectedReservation(reservation)
        setIsCancelModalOpen(true)
        return
      }

      const slot = info.event.extendedProps.slot
      if (!slot || !slot.isAvailable) {
        showToast('error', t('calendar.slotNotAvailable'))
        return
      }
      if ((user?.credits || 0) < 1) {
        showToast('error', t('reservation.notEnoughCredits'))
        return
      }
      setSelectedSlot(slot)
      setIsModalOpen(true)
    }
  }

  const handleDateClick = (info: DateClickArg) => {
    if (!isAdmin || isViewLocked) return
    const date = info.dateStr.split('T')[0]
    const time = info.dateStr.includes('T') ? info.dateStr.split('T')[1].substring(0, 5) : '09:00'
    setCreateDate(date)
    setCreateTime(time)
    setShowCreateModal(true)
  }

  const handleEventDrop = (info: EventDropArg) => {
    if (!isAdmin) return
    const slot = info.event.extendedProps.adminSlot
    if (!info.event.start || !slot) {
      info.revert()
      return
    }

    const newDate = info.event.start
    const year = newDate.getFullYear()
    const month = (newDate.getMonth() + 1).toString().padStart(2, '0')
    const day = newDate.getDate().toString().padStart(2, '0')
    const hours = newDate.getHours().toString().padStart(2, '0')
    const minutes = newDate.getMinutes().toString().padStart(2, '0')

    const newDateStr = `${year}-${month}-${day}`
    const newStartTime = `${hours}:${minutes}`

    const durationMinutes = slot.durationMinutes || 60
    const totalMinutes = newDate.getHours() * 60 + newDate.getMinutes() + durationMinutes
    const endHours = Math.floor(totalMinutes / 60) % 24
    const endMinutes = totalMinutes % 60
    const newEndTime = `${endHours.toString().padStart(2, '0')}:${endMinutes.toString().padStart(2, '0')}`

    updateSlotMutation.mutate(
      { id: slot.id, params: { date: newDateStr, startTime: newStartTime, endTime: newEndTime } },
      {
        onError: () => {
          info.revert()
          showToast('error', t('calendar.slotMoveError'))
        },
      }
    )
  }

  const handleDatesSet = (info: { startStr: string; endStr: string; start: Date }) => {
    setCurrentDate(info.start)
    setDateRange({
      start: info.startStr.split('T')[0],
      end: info.endStr.split('T')[0],
    })
  }

  // Admin helper functions
  const resetCreateForm = () => {
    setCreateDate('')
    setCreateTime('09:00')
    setCreateDuration(60)
    setCreateNote('')
  }

  const handleCloseAdminModal = () => {
    setSelectedAdminSlot(null)
    setShowUserSearch(false)
    setSearchQuery('')
    setSearchResults([])
    setSelectedUser(null)
    setDeductCredits(false)
    setNoteText('')
    setIsEditingNote(false)
    setShowCancelConfirm(false)
    setCancelWithRefund(true)
  }

  const getWeekMonday = () => {
    const calendarApi = calendarRef.current?.getApi()
    const viewStart = calendarApi?.view?.activeStart
    const baseDate = viewStart ? new Date(viewStart) : new Date()
    const day = baseDate.getDay()
    const diff = day === 0 ? -6 : 1 - day
    baseDate.setDate(baseDate.getDate() + diff)
    const year = baseDate.getFullYear()
    const month = String(baseDate.getMonth() + 1).padStart(2, '0')
    const dayOfMonth = String(baseDate.getDate()).padStart(2, '0')
    return `${year}-${month}-${dayOfMonth}`
  }

  const formatSlotTime = (time: string) => time.substring(0, 5)

  // User handlers
  const handleConfirm = () => {
    if (!selectedSlot) return
    const date = selectedSlot.start.split('T')[0]
    const startTime = selectedSlot.start.split('T')[1].substring(0, 5)
    const endTime = selectedSlot.end.split('T')[1].substring(0, 5)

    createMutation.mutate({
      date,
      startTime,
      endTime,
      blockId: selectedSlot.blockId,
      pricingItemId: defaultPricing?.id,
    })
  }

  const handleCancelReservation = () => {
    if (!selectedReservation) return
    cancelMutation.mutate(selectedReservation.id)
  }

  // Admin handlers
  const handleCreateSlot = () => {
    createSlotMutation.mutate({
      date: createDate,
      startTime: createTime,
      durationMinutes: createDuration,
      note: createNote || undefined,
    })
  }

  const handleUnlockSlot = () => {
    if (!selectedAdminSlot) return
    updateSlotMutation.mutate({ id: selectedAdminSlot.id, params: { status: 'unlocked' } })
  }

  const handleLockSlot = () => {
    if (!selectedAdminSlot) return
    updateSlotMutation.mutate({ id: selectedAdminSlot.id, params: { status: 'locked' } })
  }

  const handleDeleteSlot = () => {
    if (!selectedAdminSlot) return
    deleteSlotMutation.mutate(selectedAdminSlot.id)
  }

  const handleSelectUser = (searchedUser: User) => {
    setSelectedUser(searchedUser)
    setSearchQuery('')
    setSearchResults([])
  }

  const handleCreateReservation = () => {
    if (!selectedAdminSlot || !selectedUser) return
    createReservationMutation.mutate({
      userId: selectedUser.id,
      date: selectedAdminSlot.date,
      startTime: selectedAdminSlot.startTime,
      endTime: selectedAdminSlot.endTime,
      blockId: selectedAdminSlot.id,
      deductCredits,
    })
  }

  const handleAdminCancelReservation = () => {
    if (!selectedAdminSlot?.reservationId) return
    cancelReservationMutation.mutate({ id: selectedAdminSlot.reservationId, refund: cancelWithRefund })
  }

  const handleSaveNote = () => {
    if (!selectedAdminSlot?.reservationId) return
    updateNoteMutation.mutate({ id: selectedAdminSlot.reservationId, note: noteText || null })
  }

  const openCancelConfirm = (withRefund: boolean) => {
    setCancelWithRefund(withRefund)
    setShowCancelConfirm(true)
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {isAdmin ? t('admin.calendar') : t('reservation.title')}
        </h1>
        <div className="flex flex-wrap items-center gap-2">
          {/* Credits display for users */}
          {!isAdmin && (
            <div className="flex items-center gap-2 px-3 py-2 bg-primary-50 dark:bg-primary-900/20 rounded-lg">
              <CreditCard size={18} className="text-primary-500" />
              <span className="text-sm text-primary-700 dark:text-primary-300">
                {t('home.yourCredits')}: <strong>{user?.credits || 0}</strong>
              </span>
            </div>
          )}

          {/* Admin toolbar */}
          {isAdmin && (
            <>
              <button
                onClick={() => setIsViewLocked(!isViewLocked)}
                className={`flex items-center gap-2 px-3 py-1.5 rounded-full transition-all duration-200 select-none ${
                  isViewLocked
                    ? 'bg-primary-500 text-white shadow-md'
                    : 'bg-neutral-100 dark:bg-dark-surface text-neutral-600 dark:text-neutral-400 hover:bg-neutral-200 dark:hover:bg-dark-surfaceHover'
                }`}
              >
                <div className={`relative w-8 h-4 rounded-full transition-colors duration-200 ${
                  isViewLocked ? 'bg-primary-300' : 'bg-neutral-300 dark:bg-neutral-600'
                }`}>
                  <div className={`absolute top-0.5 w-3 h-3 rounded-full bg-white shadow-sm transition-transform duration-200 ${
                    isViewLocked ? 'translate-x-4' : 'translate-x-0.5'
                  }`} />
                </div>
                <Lock size={14} />
                <span className="text-sm font-medium">{t('calendar.lock')}</span>
              </button>

              <button
                onClick={() => setShowWeekends(!showWeekends)}
                className={`flex items-center gap-2 px-3 py-1.5 rounded-full transition-all duration-200 select-none ${
                  showWeekends
                    ? 'bg-primary-500 text-white shadow-md'
                    : 'bg-neutral-100 dark:bg-dark-surface text-neutral-600 dark:text-neutral-400 hover:bg-neutral-200 dark:hover:bg-dark-surfaceHover'
                }`}
              >
                <div className={`relative w-8 h-4 rounded-full transition-colors duration-200 ${
                  showWeekends ? 'bg-primary-300' : 'bg-neutral-300 dark:bg-neutral-600'
                }`}>
                  <div className={`absolute top-0.5 w-3 h-3 rounded-full bg-white shadow-sm transition-transform duration-200 ${
                    showWeekends ? 'translate-x-4' : 'translate-x-0.5'
                  }`} />
                </div>
                <CalendarIcon size={14} />
                <span className="text-sm font-medium">{t('calendar.weekends')}</span>
              </button>

              <Button variant="secondary" size="sm" onClick={() => setShowTemplateModal(true)}>
                <LayoutTemplate size={16} className="mr-1" />
                {t('calendar.template')}
              </Button>
              <Button variant="secondary" size="sm" onClick={() => unlockWeekMutation.mutate(getWeekMonday())} isLoading={unlockWeekMutation.isPending}>
                <Unlock size={16} className="mr-1" />
                {t('calendar.unlockWeek')}
              </Button>
            </>
          )}
        </div>
      </div>

      {/* Legend */}
      <div className="flex flex-wrap items-center gap-4 text-sm">
        {isAdmin ? (
          <>
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded bg-gray-200 border border-gray-400"></div>
              <span className="text-neutral-600 dark:text-neutral-400">{t('calendar.locked')}</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded bg-green-200 border border-green-500"></div>
              <span className="text-neutral-600 dark:text-neutral-400">{t('calendar.available')}</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded bg-blue-200 border border-blue-500"></div>
              <span className="text-neutral-600 dark:text-neutral-400">{t('calendar.reserved')}</span>
            </div>
          </>
        ) : (
          <>
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded bg-green-200 border border-green-500"></div>
              <span className="text-neutral-600 dark:text-neutral-400">{t('calendar.available')}</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded bg-blue-200 border border-blue-500"></div>
              <span className="text-neutral-600 dark:text-neutral-400">{t('calendar.yourReservation')}</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded bg-red-200 border border-red-500"></div>
              <span className="text-neutral-600 dark:text-neutral-400">{t('calendar.reserved')}</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded bg-gray-200 border border-gray-400"></div>
              <span className="text-neutral-600 dark:text-neutral-400">{t('calendar.unavailable')}</span>
            </div>
          </>
        )}
      </div>

      <Card variant="bordered" padding="none" className="overflow-hidden">
        {isLoading && !(isAdmin && adminSlots) ? (
          <div className="flex justify-center py-12">
            <Spinner size="lg" />
          </div>
        ) : (
          <div className={`p-4 [&_.fc-timegrid-slot]:!h-4 md:[&_.fc-timegrid-slot]:!h-8 transition-opacity ${isFetching ? 'opacity-60' : ''}`}>
            <FullCalendar
              ref={calendarRef}
              plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
              initialView={isAdmin ? (showWeekends ? "timeGridWeek" : "timeGrid5Day") : "timeGridWeek"}
              initialDate={currentDate}
              locale={i18n.language === 'cs' ? csLocale : undefined}
              views={{
                timeGridDay: {
                  type: 'timeGrid',
                  duration: { days: 1 },
                  buttonText: i18n.language === 'cs' ? '1 den' : '1 day',
                },
                timeGrid3Day: {
                  type: 'timeGrid',
                  dayCount: 3,
                  buttonText: i18n.language === 'cs' ? '3 dny' : '3 days',
                },
                timeGrid5Day: {
                  type: 'timeGrid',
                  dayCount: 5,
                  buttonText: i18n.language === 'cs' ? '5 dnů' : '5 days',
                },
                ...(isAdmin && showWeekends ? {
                  timeGridWeek: {
                    type: 'timeGrid',
                    duration: { weeks: 1 },
                    buttonText: i18n.language === 'cs' ? '7 dnů' : '7 days',
                  },
                } : {}),
              }}
              headerToolbar={{
                left: 'prev,next today',
                center: 'title',
                right: isAdmin
                  ? (showWeekends ? 'timeGridWeek,timeGrid5Day,timeGrid3Day,timeGridDay' : 'timeGrid5Day,timeGrid3Day,timeGridDay')
                  : 'timeGridWeek,timeGrid5Day,timeGrid3Day,timeGridDay',
              }}
              events={events}
              eventClick={handleEventClick}
              dateClick={isAdmin ? handleDateClick : undefined}
              eventDrop={isAdmin ? handleEventDrop as any : undefined}
              datesSet={handleDatesSet}
              editable={isAdmin && !isViewLocked}
              droppable={isAdmin && !isViewLocked}
              slotMinTime="06:00:00"
              slotMaxTime="22:00:00"
              allDaySlot={false}
              weekends={isAdmin ? showWeekends : false}
              nowIndicator={true}
              eventDisplay="block"
              height="auto"
              slotDuration={isAdmin ? "00:15:00" : "01:00:00"}
              snapDuration={isAdmin ? "00:15:00" : undefined}
              slotLabelInterval={isAdmin ? undefined : "01:00:00"}
              selectable={isAdmin && !isViewLocked}
              longPressDelay={300}
              eventLongPressDelay={300}
              selectLongPressDelay={300}
              eventContent={(eventInfo) => (
                <div className="p-1 text-xs overflow-hidden cursor-pointer">
                  <div className="font-medium truncate">{eventInfo.event.title}</div>
                </div>
              )}
            />
          </div>
        )}
      </Card>

      {/* USER MODALS */}
      {/* Booking Confirmation Modal */}
      <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title={t('reservation.confirm')} size="sm">
        {selectedSlot && (
          <div className="space-y-4">
            <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg">
              <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">{t('reservation.date')}</p>
              <p className="font-medium text-neutral-900 dark:text-white">
                {new Date(selectedSlot.start).toLocaleDateString(i18n.language, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
              </p>
            </div>
            <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg">
              <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">{t('reservation.time')}</p>
              <p className="font-mono font-semibold text-lg text-neutral-900 dark:text-white">
                {formatTime(selectedSlot.start.split('T')[1])} - {formatTime(selectedSlot.end.split('T')[1])}
              </p>
            </div>
            <div className="p-4 bg-primary-50 dark:bg-primary-900/20 rounded-lg">
              <p className="text-sm text-primary-600 dark:text-primary-400 mb-1">{t('reservation.cost', { credits: 1 })}</p>
              <p className="font-semibold text-primary-700 dark:text-primary-300">{t('calendar.creditFrom', { used: 1, total: user?.credits || 0 })}</p>
            </div>
            <div className="flex gap-3 pt-2">
              <Button variant="secondary" className="flex-1" onClick={() => setIsModalOpen(false)}>{t('common.cancel')}</Button>
              <Button className="flex-1" onClick={handleConfirm} isLoading={createMutation.isPending}>{t('reservation.book')}</Button>
            </div>
          </div>
        )}
      </Modal>

      {/* User Cancel Reservation Modal */}
      <Modal isOpen={isCancelModalOpen} onClose={() => { setIsCancelModalOpen(false); setSelectedReservation(null); }} title={t('myReservations.cancelReservation')} size="sm">
        {selectedReservation && (
          <div className="space-y-4">
            <p className="text-neutral-700 dark:text-neutral-300">{t('myReservations.cancelConfirm')}</p>
            <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg">
              <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">{t('reservation.date')}</p>
              <p className="font-medium text-neutral-900 dark:text-white">
                {new Date(`${selectedReservation.date}T${selectedReservation.startTime}`).toLocaleDateString(i18n.language, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
              </p>
            </div>
            <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg">
              <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">{t('reservation.time')}</p>
              <p className="font-mono font-semibold text-lg text-neutral-900 dark:text-white">{formatTime(selectedReservation.startTime)} - {formatTime(selectedReservation.endTime)}</p>
            </div>
            <div className="p-4 bg-green-50 dark:bg-green-900/20 rounded-lg">
              <p className="text-sm text-green-600 dark:text-green-400 mb-1">{t('credits.refund')}</p>
              <p className="font-semibold text-green-700 dark:text-green-300">+{selectedReservation.creditsUsed} {selectedReservation.creditsUsed === 1 ? t('calendar.credit') : t('calendar.credits')}</p>
            </div>
            <div className="flex gap-3 pt-2">
              <Button variant="secondary" className="flex-1" onClick={() => { setIsCancelModalOpen(false); setSelectedReservation(null); }}>{t('common.cancel')}</Button>
              <Button variant="danger" className="flex-1" onClick={handleCancelReservation} isLoading={cancelMutation.isPending}>{t('myReservations.cancelReservation')}</Button>
            </div>
          </div>
        )}
      </Modal>

      {/* ADMIN MODALS */}
      {/* Create slot modal */}
      <Modal isOpen={showCreateModal} onClose={() => { setShowCreateModal(false); resetCreateForm(); }} title={t('calendar.createSlot')} size="sm">
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">{t('calendar.date')}</label>
            <Input type="date" value={createDate} onChange={(e) => setCreateDate(e.target.value)} />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">{t('calendar.time')}</label>
            <Input type="time" value={createTime} onChange={(e) => setCreateTime(e.target.value)} />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">{t('calendar.duration')}</label>
            <select value={createDuration} onChange={(e) => setCreateDuration(Number(e.target.value))} className="w-full px-3 py-2 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white">
              <option value={30}>30 {t('calendar.minutes')}</option>
              <option value={45}>45 {t('calendar.minutes')}</option>
              <option value={60}>60 {t('calendar.minutes')}</option>
              <option value={90}>90 {t('calendar.minutes')}</option>
              <option value={120}>120 {t('calendar.minutes')}</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">{t('calendar.noteOptional')}</label>
            <textarea value={createNote} onChange={(e) => setCreateNote(e.target.value)} className="w-full px-3 py-2 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white resize-none" rows={2} />
          </div>
          <Button className="w-full" onClick={handleCreateSlot} isLoading={createSlotMutation.isPending}>{t('calendar.createSlot')}</Button>
        </div>
      </Modal>

      {/* Template selection modal */}
      <Modal isOpen={showTemplateModal} onClose={() => { setShowTemplateModal(false); setSelectedTemplateId(null); }} title={t('calendar.applyTemplate')} size="md">
        <div className="space-y-4">
          <p className="text-sm text-neutral-600 dark:text-neutral-400">{t('calendar.selectTemplateDescription')}</p>
          {templates && templates.length > 0 ? (
            <div className="space-y-2">
              {templates.filter(tmpl => tmpl.isActive).map((template) => (
                <button key={template.id} onClick={() => setSelectedTemplateId(template.id)} className={`w-full p-3 text-left rounded-lg border transition-colors ${selectedTemplateId === template.id ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20' : 'border-neutral-200 dark:border-neutral-700 hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover'}`}>
                  <p className="font-medium text-neutral-900 dark:text-white">{template.name}</p>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400">{template.slots.length} {t('calendar.slots')}</p>
                </button>
              ))}
            </div>
          ) : (
            <p className="text-center text-neutral-500 dark:text-neutral-400 py-4">{t('calendar.noTemplates')}</p>
          )}
          <Button className="w-full" onClick={() => { if (selectedTemplateId) applyTemplateMutation.mutate({ templateId: selectedTemplateId, weekStartDate: getWeekMonday() }); }} disabled={!selectedTemplateId} isLoading={applyTemplateMutation.isPending}>{t('calendar.applyTemplate')}</Button>
        </div>
      </Modal>

      {/* Fullscreen user search popup */}
      {showUserSearch && (
        <div className="fixed inset-0 z-[60] bg-white dark:bg-dark-bg flex flex-col">
          <div className="flex items-center gap-3 p-4 border-b border-neutral-200 dark:border-neutral-700">
            <button onClick={() => { setShowUserSearch(false); setSearchQuery(''); setSearchResults([]); }} className="p-2 -ml-2 hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-lg">
              <X size={24} className="text-neutral-600 dark:text-neutral-300" />
            </button>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white">{t('calendar.selectUser')}</h2>
          </div>
          <div className="p-4 border-b border-neutral-200 dark:border-neutral-700">
            <div className="relative">
              <Search size={20} className="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400" />
              <input ref={searchInputRef} type="text" value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} placeholder={t('calendar.searchPlaceholder')} className="w-full pl-10 pr-4 py-3 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 text-base" autoComplete="off" />
              {searchQuery && (
                <button onClick={() => { setSearchQuery(''); setSearchResults([]); }} className="absolute right-3 top-1/2 -translate-y-1/2 p-1 hover:bg-neutral-100 dark:hover:bg-neutral-700 rounded">
                  <X size={18} className="text-neutral-400" />
                </button>
              )}
            </div>
          </div>
          <div className="flex-1 overflow-y-auto">
            {isSearching && <div className="flex justify-center py-8"><Spinner size="md" /></div>}
            {!isSearching && searchQuery.length >= 2 && searchResults.length === 0 && <div className="text-center py-8 text-neutral-500 dark:text-neutral-400">{t('calendar.noUsersFound')}</div>}
            {!isSearching && searchQuery.length < 2 && <div className="text-center py-8 text-neutral-500 dark:text-neutral-400">{t('calendar.searchMinChars')}</div>}
            {!isSearching && searchResults.length > 0 && (
              <div className="divide-y divide-neutral-200 dark:divide-neutral-700">
                {searchResults.map((searchedUser) => (
                  <button key={searchedUser.id} onClick={() => { handleSelectUser(searchedUser); setShowUserSearch(false); }} className="w-full p-4 text-left hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover transition-colors">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center flex-shrink-0">
                        <span className="text-primary-600 dark:text-primary-400 font-medium">{(searchedUser.firstName?.[0] || searchedUser.email[0]).toUpperCase()}</span>
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-neutral-900 dark:text-white truncate">{searchedUser.firstName && searchedUser.lastName ? `${searchedUser.firstName} ${searchedUser.lastName}` : searchedUser.email}</p>
                        {searchedUser.firstName && searchedUser.lastName && <p className="text-sm text-neutral-500 dark:text-neutral-400 truncate">{searchedUser.email}</p>}
                        <p className="text-xs text-neutral-400">{t('nav.credits')}: {searchedUser.credits}</p>
                      </div>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Admin Slot detail modal */}
      <Modal isOpen={!!selectedAdminSlot} onClose={handleCloseAdminModal} title={t('calendar.slotDetail')} size="md">
        {selectedAdminSlot && (
          <div className="space-y-4">
            <div>
              <p className="text-sm text-neutral-500 dark:text-neutral-400">{t('calendar.dateAndTime')}</p>
              <p className="font-medium text-neutral-900 dark:text-white">
                {new Date(selectedAdminSlot.date).toLocaleDateString(i18n.language, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
              </p>
              <p className="font-mono text-neutral-900 dark:text-white">{formatSlotTime(selectedAdminSlot.startTime)} - {formatSlotTime(selectedAdminSlot.endTime)}</p>
            </div>

            <div>
              <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">{t('calendar.status')}</p>
              <Badge variant={selectedAdminSlot.status === 'reserved' ? 'primary' : selectedAdminSlot.status === 'locked' ? 'default' : 'success'}>
                {selectedAdminSlot.status === 'reserved' ? t('calendar.reserved') : selectedAdminSlot.status === 'locked' ? t('calendar.locked') : t('calendar.available')}
              </Badge>
            </div>

            {/* Reserved slot */}
            {selectedAdminSlot.status === 'reserved' && selectedAdminSlot.reservationId && (
              <>
                <div className="p-3 rounded-lg bg-blue-50 dark:bg-blue-900/20">
                  <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">{t('calendar.reservationInfo')}</p>
                  <p className="font-medium text-neutral-900 dark:text-white">{selectedAdminSlot.assignedUserName || selectedAdminSlot.assignedUserEmail || t('calendar.unknown')}</p>
                  {selectedAdminSlot.assignedUserName && selectedAdminSlot.assignedUserEmail && <p className="text-sm text-neutral-600 dark:text-neutral-300">{selectedAdminSlot.assignedUserEmail}</p>}
                </div>

                <div>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">{t('calendar.note')}</p>
                  {isEditingNote ? (
                    <div className="space-y-2">
                      <textarea value={noteText} onChange={(e) => setNoteText(e.target.value)} placeholder={t('calendar.writeNote')} className="w-full px-3 py-2 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none" rows={3} />
                      <div className="flex gap-2">
                        <Button variant="secondary" size="sm" className="flex-1" onClick={() => { setIsEditingNote(false); setNoteText(selectedAdminSlot.note || ''); }}>{t('common.cancel')}</Button>
                        <Button size="sm" className="flex-1" onClick={handleSaveNote} isLoading={updateNoteMutation.isPending}>{t('common.save')}</Button>
                      </div>
                    </div>
                  ) : (
                    <div onClick={() => setIsEditingNote(true)} className="p-2 rounded-lg bg-neutral-50 dark:bg-dark-surface border border-neutral-200 dark:border-neutral-700 cursor-pointer hover:bg-neutral-100 dark:hover:bg-dark-surfaceHover min-h-[60px]">
                      {selectedAdminSlot.note ? <p className="text-sm text-neutral-700 dark:text-neutral-300 whitespace-pre-wrap">{selectedAdminSlot.note}</p> : <p className="text-sm text-neutral-400 italic">{t('calendar.clickToAddNote')}</p>}
                    </div>
                  )}
                </div>

                <div className="pt-2 space-y-2">
                  <Button className="w-full" variant="danger" onClick={() => openCancelConfirm(true)}><UserMinus size={18} className="mr-2" />{t('calendar.cancelWithRefund')}</Button>
                  <Button className="w-full" variant="secondary" onClick={() => openCancelConfirm(false)}>{t('calendar.cancelWithoutRefund')}</Button>
                </div>
              </>
            )}

            {/* Cancel confirmation */}
            {showCancelConfirm && selectedAdminSlot.reservationId && (
              <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
                <div className="bg-white dark:bg-dark-surface rounded-lg p-6 max-w-sm mx-4 shadow-xl">
                  <h3 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">{t('calendar.confirmCancel')}</h3>
                  <p className="text-neutral-600 dark:text-neutral-300 mb-4">
                    {t('calendar.confirmCancelText')} <strong>{selectedAdminSlot.assignedUserName || selectedAdminSlot.assignedUserEmail}</strong>?
                    {cancelWithRefund && ' ' + t('calendar.creditsWillBeRefunded')}
                    {!cancelWithRefund && ' ' + t('calendar.creditsWontBeRefunded')}
                  </p>
                  <div className="flex gap-3">
                    <Button variant="secondary" className="flex-1" onClick={() => setShowCancelConfirm(false)}>{t('calendar.no')}</Button>
                    <Button variant="danger" className="flex-1" onClick={handleAdminCancelReservation} isLoading={cancelReservationMutation.isPending}>{t('calendar.yesCancel')}</Button>
                  </div>
                </div>
              </div>
            )}

            {/* Locked or Unlocked slot actions */}
            {(selectedAdminSlot.status === 'locked' || selectedAdminSlot.status === 'unlocked') && (
              <div className="pt-2 space-y-3">
                {selectedUser ? (
                  <>
                    <div className="p-3 rounded-lg bg-green-50 dark:bg-green-900/20 flex items-center justify-between">
                      <div>
                        <p className="font-medium text-neutral-900 dark:text-white">{selectedUser.firstName} {selectedUser.lastName}</p>
                        <p className="text-sm text-neutral-600 dark:text-neutral-300">{selectedUser.email}</p>
                        <p className="text-xs text-neutral-500">{t('nav.credits')}: {selectedUser.credits}</p>
                      </div>
                      <button onClick={() => setSelectedUser(null)} className="p-1 hover:bg-neutral-200 dark:hover:bg-neutral-700 rounded"><X size={18} /></button>
                    </div>
                    <label className="flex items-center gap-2 cursor-pointer">
                      <input type="checkbox" checked={deductCredits} onChange={(e) => setDeductCredits(e.target.checked)} className="w-4 h-4 rounded border-neutral-300 text-primary-500 focus:ring-primary-500" />
                      <span className="text-sm text-neutral-700 dark:text-neutral-300">{t('calendar.deductCredit', { available: selectedUser.credits })}</span>
                    </label>
                    <div className="flex gap-2">
                      <Button variant="secondary" className="flex-1" onClick={() => setSelectedUser(null)}>{t('calendar.change')}</Button>
                      <Button className="flex-1" onClick={handleCreateReservation} isLoading={createReservationMutation.isPending}>{t('calendar.register')}</Button>
                    </div>
                  </>
                ) : (
                  <>
                    <Button className="w-full" variant="primary" onClick={() => setShowUserSearch(true)}><UserPlus size={18} className="mr-2" />{t('calendar.registerUser')}</Button>
                    {selectedAdminSlot.status === 'locked' ? (
                      <>
                        <Button className="w-full" variant="secondary" onClick={handleUnlockSlot} isLoading={updateSlotMutation.isPending}><Unlock size={18} className="mr-2" />{t('calendar.unlockSlot')}</Button>
                        <Button className="w-full" variant="danger" onClick={handleDeleteSlot} isLoading={deleteSlotMutation.isPending}>{t('calendar.deleteSlot')}</Button>
                      </>
                    ) : (
                      <Button className="w-full" variant="secondary" onClick={handleLockSlot} isLoading={updateSlotMutation.isPending}><Lock size={18} className="mr-2" />{t('calendar.lockSlot')}</Button>
                    )}
                  </>
                )}
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  )
}
