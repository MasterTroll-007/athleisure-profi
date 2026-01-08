import { useState, useRef, useEffect, useMemo, useCallback } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import csLocale from '@fullcalendar/core/locales/cs'
import { CreditCard, Search, X, UserPlus, UserMinus, Lock, Unlock, LayoutTemplate, MoreVertical, Check, Calendar, ChevronLeft, ChevronRight } from 'lucide-react'
import { Card, Button, Modal, Spinner, Badge, Input, InfiniteScrollCalendar } from '@/components/ui'
import type { CalendarSlot, InfiniteScrollCalendarRef } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { reservationApi, creditApi, adminApi, calendarApi } from '@/services/api'
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

// Format date to ISO string (YYYY-MM-DD) using local timezone to avoid UTC date shifting
const formatDateLocal = (date: Date): string => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
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
    start: formatDateLocal(new Date()),
    end: formatDateLocal(new Date(Date.now() + 14 * 24 * 60 * 60 * 1000)),
  })

  // Detect mobile for default view
  const [isMobile, setIsMobile] = useState(typeof window !== 'undefined' && window.innerWidth < 640)
  const [viewDays, setViewDays] = useState(isMobile ? 3 : 7)

  // Update mobile detection on resize
  useEffect(() => {
    const handleResize = () => {
      const mobile = window.innerWidth < 640
      setIsMobile(mobile)
    }
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  // Load saved calendar view from localStorage, default to 3 days on mobile
  const savedView = typeof window !== 'undefined' ? localStorage.getItem('calendarView') : null
  const initialCalendarView = savedView && ['timeGridDay', 'timeGrid3Day', 'timeGrid5Day', 'timeGridWeek'].includes(savedView)
    ? savedView
    : (isMobile ? 'timeGrid3Day' : 'timeGridWeek')

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
  const [showMobileMenu, setShowMobileMenu] = useState(false)
  const [showMonthView, setShowMonthView] = useState(false)
  const [monthViewDate, setMonthViewDate] = useState(new Date())
  const [isTransitioning, setIsTransitioning] = useState(false)
  const [transitionDay, setTransitionDay] = useState<number | null>(null)
  const searchInputRef = useRef<HTMLInputElement>(null)
  const mobileMenuRef = useRef<HTMLDivElement>(null)
  const infiniteCalendarRef = useRef<InfiniteScrollCalendarRef>(null)

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

  const { data: calendarSettings } = useQuery({
    queryKey: ['calendarSettings'],
    queryFn: calendarApi.getSettings,
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

  // Close mobile menu on click outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (mobileMenuRef.current && !mobileMenuRef.current.contains(event.target as Node)) {
        setShowMobileMenu(false)
      }
    }
    if (showMobileMenu) {
      document.addEventListener('mousedown', handleClickOutside)
    }
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [showMobileMenu])


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
      case 'cancelled':
        return { bg: '#fee2e2', border: '#ef4444', text: '#991b1b' }
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
          case 'cancelled':
            title = '❌ ' + (slot.assignedUserName || slot.assignedUserEmail || t('calendar.cancelled'))
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

  // Convert events to CalendarSlot format for InfiniteScrollCalendar
  const calendarSlots: CalendarSlot[] = useMemo(() => {
    return events.map(event => ({
      id: event.id,
      date: event.start.split('T')[0],
      startTime: event.start.split('T')[1]?.substring(0, 5) || '00:00',
      endTime: event.end.split('T')[1]?.substring(0, 5) || '00:00',
      title: event.title,
      backgroundColor: event.backgroundColor,
      borderColor: event.borderColor,
      textColor: event.textColor,
      data: event.extendedProps,
    }))
  }, [events])

  // Handle slot click from InfiniteScrollCalendar
  const handleCalendarSlotClick = useCallback((slot: CalendarSlot) => {
    if (isAdmin) {
      const adminSlot = slot.data.adminSlot as Slot
      if (adminSlot) {
        setSelectedAdminSlot(adminSlot)
        setShowUserSearch(false)
        setSearchQuery('')
        setSelectedUser(null)
        setDeductCredits(false)
        setNoteText(adminSlot.note || '')
        setIsEditingNote(false)
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

        setSelectedReservation(reservation)
        setIsCancelModalOpen(true)
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
      setSelectedSlot(availableSlot)
      setIsModalOpen(true)
    }
  }, [isAdmin, user, showToast, t])

  // Handle date click from InfiniteScrollCalendar (admin only)
  const handleCalendarDateClick = useCallback((date: string, time: string) => {
    if (!isAdmin || isViewLocked) return
    setCreateDate(date)
    setCreateTime(time)
    setShowCreateModal(true)
  }, [isAdmin, isViewLocked])

  // Handle date range change from InfiniteScrollCalendar
  const handleCalendarDateRangeChange = useCallback((start: string, end: string) => {
    setDateRange({ start, end })
  }, [])

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

  const handleDatesSet = (info: { startStr: string; endStr: string; start: Date; view: { type: string } }) => {
    setCurrentDate(info.start)
    setDateRange({
      start: info.startStr.split('T')[0],
      end: info.endStr.split('T')[0],
    })
    // Save view type to localStorage
    if (info.view?.type) {
      localStorage.setItem('calendarView', info.view.type)
    }
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

  // Format month/year for mobile header
  const formatMonthYear = (date: Date) => {
    return date.toLocaleDateString(i18n.language, { month: 'long', year: 'numeric' })
      .replace(/^\w/, c => c.toUpperCase())
  }

  // Handle "Today" button click in mobile day/week view
  const handleScrollToToday = useCallback(() => {
    setCurrentDate(new Date())
    infiniteCalendarRef.current?.scrollToToday()
  }, [])

  // Get slots for a specific day in month view (similar to mobile app's MonthSlotInfo)
  interface MonthSlotInfo {
    time: string        // "14:00"
    label: string | null // "J. Novák" for admin, null for user
    isReserved: boolean
    isLocked: boolean
    isCancelled: boolean
    isMyReservation: boolean
    isUnlocked?: boolean
  }

  const getSlotsForDay = useCallback((year: number, month: number, day: number): MonthSlotInfo[] => {
    const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`

    // Skip past days entirely
    const today = new Date()
    const cellDate = new Date(year, month, day)
    const todayMidnight = new Date(today.getFullYear(), today.getMonth(), today.getDate())
    if (cellDate < todayMidnight) {
      return []
    }

    return calendarSlots
      .filter(slot => slot.date === dateStr)
      .map(slot => {
        const data = slot.data
        const isReserved = data.type === 'reservation' || data.adminSlot?.status === 'reserved'
        const isLocked = data.adminSlot?.status === 'locked'
        const isCancelled = data.adminSlot?.status === 'cancelled'
        const isMyReservation = data.type === 'reservation'
        const isUnlocked = data.adminSlot?.status === 'unlocked'

        // Format label for admin view (like mobile app: "J. Novák")
        let label: string | null = null
        if (isAdmin && data.adminSlot?.assignedUserName) {
          const name = data.adminSlot.assignedUserName
          const parts = name.split(' ')
          if (parts.length >= 2) {
            label = `${parts[0].charAt(0)}. ${parts[parts.length - 1]}`
          } else {
            label = name
          }
        }

        return {
          time: slot.startTime.substring(0, 5),
          label,
          isReserved,
          isLocked,
          isCancelled,
          isMyReservation,
          isUnlocked,
        }
      })
      // Filter: only show reserved slots (with user) or user's own reservations
      // Hide empty/available/locked/unlocked/cancelled slots
      .filter(slot => slot.isReserved || slot.isMyReservation)
      .sort((a, b) => a.time.localeCompare(b.time))
  }, [calendarSlots, isAdmin])

  // Handle month navigation - update date range to fetch data for displayed month
  const handlePrevMonth = () => {
    setMonthViewDate(prev => {
      const newDate = new Date(prev)
      newDate.setMonth(newDate.getMonth() - 1)
      // Update date range to fetch slots for the new month
      const firstDay = new Date(newDate.getFullYear(), newDate.getMonth(), 1)
      const lastDay = new Date(newDate.getFullYear(), newDate.getMonth() + 1, 0)
      setDateRange({
        start: formatDateLocal(firstDay),
        end: formatDateLocal(lastDay),
      })
      return newDate
    })
  }

  const handleNextMonth = () => {
    setMonthViewDate(prev => {
      const newDate = new Date(prev)
      newDate.setMonth(newDate.getMonth() + 1)
      // Update date range to fetch slots for the new month
      const firstDay = new Date(newDate.getFullYear(), newDate.getMonth(), 1)
      const lastDay = new Date(newDate.getFullYear(), newDate.getMonth() + 1, 0)
      setDateRange({
        start: formatDateLocal(firstDay),
        end: formatDateLocal(lastDay),
      })
      return newDate
    })
  }

  // Handle day click in month view - zoom to that day with transition
  const handleMonthDayClick = (day: number) => {
    const selectedDate = new Date(monthViewDate.getFullYear(), monthViewDate.getMonth(), day)

    // Start transition animation
    setTransitionDay(day)
    setIsTransitioning(true)

    // After animation, switch to day view
    setTimeout(() => {
      setCurrentDate(selectedDate)
      setViewDays(1)
      setShowMonthView(false)
      setIsTransitioning(false)
      setTransitionDay(null)
    }, 300)
  }

  // Render slot mini-block (like mobile app's SlotMiniBlock)
  const renderSlotMiniBlock = (slot: MonthSlotInfo) => {
    // Color scheme matching mobile app
    let bgColor = ''
    let textColor = ''

    if (slot.isCancelled) {
      bgColor = 'bg-red-100 dark:bg-red-900/30'
      textColor = 'text-red-700 dark:text-red-300'
    } else if (slot.isLocked) {
      bgColor = 'bg-neutral-200 dark:bg-neutral-700'
      textColor = 'text-neutral-600 dark:text-neutral-400'
    } else if (slot.isMyReservation) {
      bgColor = 'bg-primary-500 dark:bg-primary-600'
      textColor = 'text-white'
    } else if (slot.isReserved) {
      bgColor = 'bg-primary-500 dark:bg-primary-600'
      textColor = 'text-white'
    } else {
      // Available/unlocked
      bgColor = 'bg-primary-100 dark:bg-primary-900/40'
      textColor = 'text-primary-700 dark:text-primary-300'
    }

    const displayText = slot.label ? `${slot.time} ${slot.label}` : slot.time

    return (
      <div
        className={`w-full px-1 py-0.5 rounded text-[7px] leading-tight truncate ${bgColor} ${textColor}`}
      >
        {displayText}
      </div>
    )
  }

  // Render month calendar grid
  const renderMonthCalendar = () => {
    const year = monthViewDate.getFullYear()
    const month = monthViewDate.getMonth()
    const firstDay = new Date(year, month, 1)
    const lastDay = new Date(year, month + 1, 0)
    const daysInMonth = lastDay.getDate()

    // Get starting day of week (Monday = 0)
    let startingDay = firstDay.getDay() - 1
    if (startingDay < 0) startingDay = 6

    const today = new Date()
    const todayDate = new Date(today.getFullYear(), today.getMonth(), today.getDate())
    const isToday = (day: number) =>
      today.getFullYear() === year && today.getMonth() === month && today.getDate() === day
    const isPast = (day: number) => {
      const cellDate = new Date(year, month, day)
      return cellDate < todayDate
    }

    const dayNames = i18n.language === 'cs'
      ? ['Po', 'Út', 'St', 'Čt', 'Pá', 'So', 'Ne']
      : ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

    const weeks: (number | null)[][] = []
    let currentWeek: (number | null)[] = []

    // Add empty cells for days before first day of month
    for (let i = 0; i < startingDay; i++) {
      currentWeek.push(null)
    }

    // Add days of month
    for (let day = 1; day <= daysInMonth; day++) {
      currentWeek.push(day)
      if (currentWeek.length === 7) {
        weeks.push(currentWeek)
        currentWeek = []
      }
    }

    // Fill remaining cells in last week
    while (currentWeek.length > 0 && currentWeek.length < 7) {
      currentWeek.push(null)
    }
    if (currentWeek.length > 0) {
      weeks.push(currentWeek)
    }

    const MAX_VISIBLE_SLOTS = 2 // Show max 2 slots, like mobile app

    return (
      <div className="flex flex-col h-full bg-white dark:bg-dark-surface relative overflow-hidden">
        {/* Month navigation header */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-neutral-200 dark:border-neutral-700 flex-shrink-0">
          <button
            onClick={handlePrevMonth}
            className="p-2 rounded-lg hover:bg-neutral-100 dark:hover:bg-dark-surfaceHover"
          >
            <ChevronLeft size={24} className="text-neutral-600 dark:text-neutral-300" />
          </button>
          <div className="flex items-center gap-2">
            <h2 className="text-lg font-bold text-neutral-900 dark:text-white">
              {formatMonthYear(monthViewDate)}
            </h2>
            <button
              onClick={() => {
                const today = new Date()
                setMonthViewDate(today)
                // Update date range to fetch slots for the current month
                const firstDay = new Date(today.getFullYear(), today.getMonth(), 1)
                const lastDay = new Date(today.getFullYear(), today.getMonth() + 1, 0)
                setDateRange({
                  start: formatDateLocal(firstDay),
                  end: formatDateLocal(lastDay),
                })
              }}
              className="px-2 py-1 text-xs font-medium text-primary-600 dark:text-primary-400 bg-primary-50 dark:bg-primary-900/30 rounded-md hover:bg-primary-100 dark:hover:bg-primary-900/50 transition-colors"
            >
              {t('calendar.today')}
            </button>
          </div>
          <button
            onClick={handleNextMonth}
            className="p-2 rounded-lg hover:bg-neutral-100 dark:hover:bg-dark-surfaceHover"
          >
            <ChevronRight size={24} className="text-neutral-600 dark:text-neutral-300" />
          </button>
        </div>

        {/* Day names header */}
        <div className="grid grid-cols-7 border-b border-neutral-200 dark:border-neutral-700 flex-shrink-0">
          {dayNames.map((dayName, index) => (
            <div
              key={dayName}
              className={`py-2 text-center text-xs font-medium ${
                index >= 5 ? 'text-neutral-400' : 'text-neutral-600 dark:text-neutral-400'
              }`}
            >
              {dayName}
            </div>
          ))}
        </div>

        {/* Calendar grid - fixed height rows to prevent scrolling */}
        <div className="flex-1 grid overflow-hidden" style={{ gridTemplateRows: `repeat(${weeks.length}, 1fr)` }}>
          {weeks.map((week, weekIndex) => (
            <div key={weekIndex} className="grid grid-cols-7 border-b border-neutral-100 dark:border-neutral-800 last:border-b-0">
              {week.map((day, dayIndex) => {
                const slots = day ? getSlotsForDay(year, month, day) : []
                const visibleSlots = slots.slice(0, MAX_VISIBLE_SLOTS)
                const remainingCount = slots.length - MAX_VISIBLE_SLOTS
                const dayIsPast = day ? isPast(day) : false
                const dayIsToday = day ? isToday(day) : false

                return (
                  <button
                    key={dayIndex}
                    onClick={() => day && handleMonthDayClick(day)}
                    disabled={!day || isTransitioning}
                    className={`
                      relative flex flex-col items-center p-1 h-full overflow-hidden transition-all duration-300 border-r border-neutral-100 dark:border-neutral-800 last:border-r-0
                      ${day ? 'hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover active:bg-neutral-100 dark:active:bg-neutral-700' : ''}
                      ${dayIsToday ? 'bg-primary-50/50 dark:bg-primary-900/20' : ''}
                      ${dayIsPast ? 'bg-neutral-100/50 dark:bg-neutral-800/50' : ''}
                      ${dayIndex >= 5 && !dayIsPast ? 'bg-neutral-50/30 dark:bg-neutral-800/20' : ''}
                      ${day !== null && transitionDay === day ? 'z-50 scale-110 bg-primary-500 rounded-lg shadow-xl' : ''}
                      ${isTransitioning && day !== null && transitionDay !== day ? 'opacity-0 scale-75' : ''}
                    `}
                  >
                    {day && (
                      <>
                        {/* Day number */}
                        <span className={`text-sm mb-0.5 transition-all duration-300 ${
                          transitionDay === day
                            ? 'font-bold text-white'
                            : dayIsToday
                              ? 'font-bold text-primary-600 dark:text-primary-400'
                              : dayIsPast
                                ? 'text-neutral-400 dark:text-neutral-500'
                                : dayIndex >= 5
                                  ? 'text-neutral-400'
                                  : 'text-neutral-900 dark:text-white'
                        }`}>
                          {day}
                        </span>

                        {/* Slot mini-blocks */}
                        {!isTransitioning && slots.length > 0 && (
                          <div className="w-full flex flex-col gap-0.5">
                            {visibleSlots.map((slot, idx) => (
                              <div key={idx}>
                                {renderSlotMiniBlock(slot)}
                              </div>
                            ))}
                            {/* "+N" indicator for remaining slots */}
                            {remainingCount > 0 && (
                              <div className="text-[8px] font-medium text-primary-500 dark:text-primary-400 text-center">
                                +{remainingCount}
                              </div>
                            )}
                          </div>
                        )}
                      </>
                    )}
                  </button>
                )
              })}
            </div>
          ))}
        </div>
      </div>
    )
  }

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

  // Reusable modals component for both mobile and desktop
  const renderModals = () => (
    <>
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
              <Badge variant={selectedAdminSlot.status === 'reserved' ? 'primary' : selectedAdminSlot.status === 'cancelled' ? 'danger' : selectedAdminSlot.status === 'locked' ? 'default' : 'success'}>
                {selectedAdminSlot.status === 'reserved' ? t('calendar.reserved') : selectedAdminSlot.status === 'cancelled' ? t('calendar.cancelled') : selectedAdminSlot.status === 'locked' ? t('calendar.locked') : t('calendar.available')}
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

            {/* Cancelled slot - show user info */}
            {selectedAdminSlot.status === 'cancelled' && (
              <>
                <div className="p-3 rounded-lg bg-red-50 dark:bg-red-900/20">
                  <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">{t('calendar.cancelledReservation')}</p>
                  <p className="font-medium text-neutral-900 dark:text-white">{selectedAdminSlot.assignedUserName || selectedAdminSlot.assignedUserEmail || t('calendar.unknown')}</p>
                  {selectedAdminSlot.assignedUserName && selectedAdminSlot.assignedUserEmail && <p className="text-sm text-neutral-600 dark:text-neutral-300">{selectedAdminSlot.assignedUserEmail}</p>}
                  {selectedAdminSlot.cancelledAt && (
                    <p className="text-xs text-red-600 dark:text-red-400 mt-1">
                      {t('calendar.cancelledAt')}: {new Date(selectedAdminSlot.cancelledAt).toLocaleString(i18n.language)}
                    </p>
                  )}
                </div>

                <div className="pt-2 space-y-2">
                  <Button className="w-full" variant="primary" onClick={() => setShowUserSearch(true)}><UserPlus size={18} className="mr-2" />{t('calendar.registerUser')}</Button>
                  <Button className="w-full" variant="secondary" onClick={handleLockSlot} isLoading={updateSlotMutation.isPending}><Lock size={18} className="mr-2" />{t('calendar.lockSlot')}</Button>
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
    </>
  )

  // Mobile-optimized layout (matches mobile app design)
  // Height: 100vh - 56px (header/pt-14) - 56px (main pb-14) = 100vh - 112px, compensate wrapper py-6 with negative margins
  if (isMobile) {
    return (
      <div className="flex flex-col h-[calc(100vh-112px)] -mx-4 -my-6">
        {/* Mobile header: Month/Year + Today button + Menu - hidden in month view which has its own header */}
        {!showMonthView && (
          <div className="flex items-center justify-between px-3 py-2 bg-neutral-50 dark:bg-dark-surface border-b border-neutral-200 dark:border-neutral-700">
            <div className="flex items-center gap-2">
              <h2 className="text-lg font-bold text-neutral-900 dark:text-white">
                {formatMonthYear(currentDate)}
              </h2>
              <button
                onClick={handleScrollToToday}
                className="px-2 py-1 text-xs font-medium text-primary-600 dark:text-primary-400 bg-primary-50 dark:bg-primary-900/30 rounded-md hover:bg-primary-100 dark:hover:bg-primary-900/50 transition-colors"
              >
                {t('calendar.today')}
              </button>
            </div>

          {/* Three-dot menu */}
          <div className="relative" ref={mobileMenuRef}>
            <button
              onClick={() => setShowMobileMenu(!showMobileMenu)}
              className="p-2 rounded-lg hover:bg-neutral-200 dark:hover:bg-dark-surfaceHover transition-colors"
            >
              <MoreVertical size={24} className="text-neutral-600 dark:text-neutral-300" />
            </button>

            {/* Dropdown menu */}
            {showMobileMenu && (
              <div className="absolute right-0 top-full mt-1 w-56 bg-white dark:bg-dark-surface rounded-lg shadow-lg border border-neutral-200 dark:border-neutral-700 z-50 py-1">
                {/* View mode options */}
                <div className="px-3 py-1.5 text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase">
                  {t('calendar.view')}
                </div>
                {/* Month view option */}
                <button
                  onClick={() => {
                    const monthDate = currentDate;
                    const firstDay = new Date(monthDate.getFullYear(), monthDate.getMonth(), 1);
                    const lastDay = new Date(monthDate.getFullYear(), monthDate.getMonth() + 1, 0);
                    setDateRange({ start: formatDateLocal(firstDay), end: formatDateLocal(lastDay) });
                    setShowMonthView(true);
                    setMonthViewDate(monthDate);
                    setShowMobileMenu(false);
                  }}
                  className="w-full flex items-center gap-3 px-3 py-2 text-left hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover transition-colors"
                >
                  {showMonthView ? (
                    <Check size={16} className="text-primary-500" />
                  ) : (
                    <span className="w-4" />
                  )}
                  <Calendar size={16} className="text-neutral-500" />
                  <span className="text-sm text-neutral-700 dark:text-neutral-300">{t('calendar.month')}</span>
                </button>
                <div className="h-px bg-neutral-100 dark:bg-neutral-800 mx-3" />
                {[
                  { days: 1, label: i18n.language === 'cs' ? '1 den' : '1 day' },
                  { days: 3, label: i18n.language === 'cs' ? '3 dny' : '3 days' },
                  { days: 5, label: i18n.language === 'cs' ? '5 dnů' : '5 days' },
                  { days: 7, label: i18n.language === 'cs' ? '7 dnů' : '7 days' },
                ].map(({ days, label }) => (
                  <button
                    key={days}
                    onClick={() => { setViewDays(days); setShowMonthView(false); setShowMobileMenu(false); }}
                    className="w-full flex items-center gap-3 px-3 py-2 text-left hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover transition-colors"
                  >
                    {!showMonthView && viewDays === days ? (
                      <Check size={16} className="text-primary-500" />
                    ) : (
                      <span className="w-4" />
                    )}
                    <span className="text-sm text-neutral-700 dark:text-neutral-300">{label}</span>
                  </button>
                ))}

                {/* Admin options */}
                {isAdmin && (
                  <>
                    <div className="h-px bg-neutral-200 dark:bg-neutral-700 my-1" />

                    {/* Lock toggle */}
                    <button
                      onClick={() => { setIsViewLocked(!isViewLocked); setShowMobileMenu(false); }}
                      className="w-full flex items-center gap-3 px-3 py-2 text-left hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover transition-colors"
                    >
                      {isViewLocked ? <Lock size={16} /> : <Unlock size={16} />}
                      <span className="text-sm text-neutral-700 dark:text-neutral-300">
                        {isViewLocked ? t('calendar.unlockDrag') : t('calendar.lockDrag')}
                      </span>
                    </button>

                    {/* Template */}
                    <button
                      onClick={() => { setShowTemplateModal(true); setShowMobileMenu(false); }}
                      className="w-full flex items-center gap-3 px-3 py-2 text-left hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover transition-colors"
                    >
                      <LayoutTemplate size={16} />
                      <span className="text-sm text-neutral-700 dark:text-neutral-300">{t('calendar.template')}</span>
                    </button>

                    {/* Unlock week */}
                    <button
                      onClick={() => { unlockWeekMutation.mutate(getWeekMonday()); setShowMobileMenu(false); }}
                      className="w-full flex items-center gap-3 px-3 py-2 text-left hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover transition-colors"
                      disabled={unlockWeekMutation.isPending}
                    >
                      <Unlock size={16} />
                      <span className="text-sm text-neutral-700 dark:text-neutral-300">{t('calendar.unlockWeek')}</span>
                    </button>
                  </>
                )}
              </div>
            )}
          </div>
        </div>
        )}

        {/* Calendar takes remaining space */}
        <div className="flex-1 overflow-hidden">
          {isLoading && !(isAdmin && adminSlots) ? (
            <div className="flex justify-center items-center h-full">
              <Spinner size="lg" />
            </div>
          ) : showMonthView ? (
            renderMonthCalendar()
          ) : (
            <InfiniteScrollCalendar
              ref={infiniteCalendarRef}
              slots={calendarSlots}
              initialDate={currentDate}
              viewDays={viewDays}
              startHour={calendarSettings?.calendarStartHour ?? 6}
              endHour={calendarSettings?.calendarEndHour ?? 22}
              onSlotClick={handleCalendarSlotClick}
              onDateClick={isAdmin ? handleCalendarDateClick : undefined}
              onDateRangeChange={handleCalendarDateRangeChange}
              isAdmin={isAdmin}
              isLoading={isFetching}
            />
          )}
        </div>

        {/* Modals will be rendered below */}
        {renderModals()}
      </div>
    )
  }

  // Desktop layout (original design)
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
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded bg-red-200 border border-red-500"></div>
              <span className="text-neutral-600 dark:text-neutral-400">{t('calendar.cancelled')}</span>
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

      <Card variant="bordered" padding="none" className="overflow-hidden rounded-xl">
        {isLoading && !(isAdmin && adminSlots) ? (
          <div className="flex justify-center py-12">
            <Spinner size="lg" />
          </div>
        ) : (
          /* Desktop: FullCalendar */
          <div
            className={`p-1 md:p-4 ${isAdmin ? '[&_.fc-timegrid-slot]:!h-4' : '[&_.fc-timegrid-slot]:!h-12'} md:[&_.fc-timegrid-slot]:!h-5 [&_.fc-timegrid-axis]:!w-10 md:[&_.fc-timegrid-axis]:!w-14 [&_.fc-timegrid-slot-label]:!text-[10px] md:[&_.fc-timegrid-slot-label]:!text-xs [&_.fc-col-header-cell]:!text-[10px] md:[&_.fc-col-header-cell]:!text-xs transition-opacity ${isFetching ? 'opacity-60' : ''}`}
          >
            <FullCalendar
              ref={calendarRef}
              plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
              initialView={initialCalendarView}
              initialDate={currentDate}
              locale={i18n.language === 'cs' ? csLocale : undefined}
              firstDay={1}
              views={{
                timeGridDay: {
                  type: 'timeGrid',
                  duration: { days: 1 },
                  dateIncrement: { days: 1 },
                  buttonText: i18n.language === 'cs' ? '1 den' : '1 day',
                },
                timeGrid3Day: {
                  type: 'timeGrid',
                  duration: { days: 3 },
                  dateIncrement: { days: 1 }, // Swipe by 1 day like mobile app
                  buttonText: i18n.language === 'cs' ? '3 dny' : '3 days',
                },
                timeGrid5Day: {
                  type: 'timeGrid',
                  duration: { days: 5 },
                  dateIncrement: { days: 1 }, // Swipe by 1 day like mobile app
                  buttonText: i18n.language === 'cs' ? '5 dnů' : '5 days',
                },
                timeGridWeek: {
                  type: 'timeGrid',
                  duration: { weeks: 1 },
                  dateIncrement: { days: 1 }, // Swipe by 1 day like mobile app
                  buttonText: i18n.language === 'cs' ? '7 dnů' : '7 days',
                },
              }}
              headerToolbar={{
                left: 'prev,next today',
                center: 'title',
                right: 'timeGridWeek,timeGrid5Day,timeGrid3Day,timeGridDay',
              }}
              events={events}
              eventClick={handleEventClick}
              dateClick={isAdmin ? handleDateClick : undefined}
              eventDrop={isAdmin ? handleEventDrop as any : undefined}
              datesSet={handleDatesSet}
              editable={isAdmin && !isViewLocked}
              droppable={isAdmin && !isViewLocked}
              slotMinTime={`${(calendarSettings?.calendarStartHour ?? 6).toString().padStart(2, '0')}:00:00`}
              slotMaxTime={`${(calendarSettings?.calendarEndHour ?? 22).toString().padStart(2, '0')}:00:00`}
              allDaySlot={false}
              weekends={true}
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
              eventContent={(eventInfo) => {
                const title = eventInfo.event.title
                const lines = title.split('\n')
                return (
                  <div className="p-1 text-xs overflow-hidden cursor-pointer">
                    {lines.map((line, idx) => (
                      <div key={idx} className={idx === 0 ? 'font-medium truncate' : 'truncate'}>{line}</div>
                    ))}
                  </div>
                )
              }}
            />
          </div>
        )}
      </Card>

      {renderModals()}
    </div>
  )
}
