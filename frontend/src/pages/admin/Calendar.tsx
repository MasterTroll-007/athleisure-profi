import { useState, useRef, useEffect, useMemo, useCallback } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import csLocale from '@fullcalendar/core/locales/cs'
import { Search, X, UserPlus, UserMinus, Lock, Unlock, LayoutTemplate, Calendar as CalendarIcon } from 'lucide-react'
import { Card, Modal, Badge, Button, Spinner, Input } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { adminApi } from '@/services/api'
import type { EventClickArg } from '@fullcalendar/core'
import type { Slot, User } from '@/types/api'

interface DateClickArg {
  date: Date
  dateStr: string
  allDay: boolean
}

interface EventDropArg {
  event: {
    id: string
    start: Date | null
    extendedProps: { slot: Slot }
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
    slot: Slot
  }
}

export default function AdminCalendar() {
  const { t, i18n } = useTranslation()
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const calendarRef = useRef<FullCalendar>(null)
  const [selectedSlot, setSelectedSlot] = useState<Slot | null>(null)
  const [dateRange, setDateRange] = useState<{ start: string; end: string }>({
    start: new Date().toISOString().split('T')[0],
    end: new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
  })

  // Create slot modal state
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [createDate, setCreateDate] = useState('')
  const [createTime, setCreateTime] = useState('09:00')
  const [createDuration, setCreateDuration] = useState(60)
  const [createNote, setCreateNote] = useState('')

  // Template modal state
  const [showTemplateModal, setShowTemplateModal] = useState(false)
  const [selectedTemplateId, setSelectedTemplateId] = useState<string | null>(null)

  // User search state
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
  const searchInputRef = useRef<HTMLInputElement>(null)

  // View settings
  const [isViewLocked, setIsViewLocked] = useState(false)
  const [showWeekends, setShowWeekends] = useState(false)

  const { data: slots, isLoading, isFetching } = useQuery({
    queryKey: ['admin', 'slots', dateRange],
    queryFn: () => adminApi.getSlots(dateRange.start, dateRange.end),
    placeholderData: (previousData) => previousData, // Keep previous data while fetching
    staleTime: 30000, // Consider data fresh for 30 seconds
  })

  const { data: templates } = useQuery({
    queryKey: ['admin', 'templates'],
    queryFn: () => adminApi.getTemplates(),
  })

  // Create slot mutation
  const createSlotMutation = useMutation({
    mutationFn: adminApi.createSlot,
    onSuccess: () => {
      showToast('success', 'Slot vytvoren')
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      setShowCreateModal(false)
      resetCreateForm()
    },
    onError: (error: { response?: { data?: { error?: string } } }) => {
      showToast('error', error.response?.data?.error || 'Nepodarilo se vytvorit slot')
    },
  })

  // Update slot mutation
  const updateSlotMutation = useMutation({
    mutationFn: ({ id, params }: { id: string; params: { status?: string; note?: string; date?: string; startTime?: string; endTime?: string } }) =>
      adminApi.updateSlot(id, params),
    onSuccess: () => {
      showToast('success', 'Slot upraven')
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      setSelectedSlot(null)
    },
    onError: () => {
      showToast('error', 'Nepodarilo se upravit slot')
    },
  })

  // Delete slot mutation
  const deleteSlotMutation = useMutation({
    mutationFn: adminApi.deleteSlot,
    onSuccess: () => {
      showToast('success', 'Slot smazan')
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      setSelectedSlot(null)
    },
    onError: () => {
      showToast('error', 'Nepodarilo se smazat slot')
    },
  })

  // Unlock week mutation
  const unlockWeekMutation = useMutation({
    mutationFn: adminApi.unlockWeek,
    onSuccess: (data) => {
      showToast('success', `Odemknuto ${data.unlockedCount} slotu`)
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
    },
    onError: () => {
      showToast('error', 'Nepodarilo se odemknout tyden')
    },
  })

  // Apply template mutation
  const applyTemplateMutation = useMutation({
    mutationFn: ({ templateId, weekStartDate }: { templateId: string; weekStartDate: string }) =>
      adminApi.applyTemplate(templateId, weekStartDate),
    onSuccess: (data) => {
      showToast('success', `Vytvoreno ${data.createdSlots} slotu ze sablony`)
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      setShowTemplateModal(false)
      setSelectedTemplateId(null)
    },
    onError: () => {
      showToast('error', 'Nepodarilo se aplikovat sablonu')
    },
  })

  const createReservationMutation = useMutation({
    mutationFn: adminApi.createReservation,
    onSuccess: () => {
      showToast('success', 'Rezervace byla vytvorena')
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      handleCloseModal()
    },
    onError: (error: { response?: { data?: { error?: string } } }) => {
      showToast('error', error.response?.data?.error || 'Nepodarilo se vytvorit rezervaci')
    },
  })

  const cancelReservationMutation = useMutation({
    mutationFn: ({ id, refund }: { id: string; refund: boolean }) =>
      adminApi.cancelReservation(id, refund),
    onSuccess: () => {
      showToast('success', 'Rezervace byla zrusena')
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      handleCloseModal()
    },
    onError: () => {
      showToast('error', 'Nepodarilo se zrusit rezervaci')
    },
  })

  const updateNoteMutation = useMutation({
    mutationFn: ({ id, note }: { id: string; note: string | null }) =>
      adminApi.updateReservationNote(id, note),
    onSuccess: () => {
      showToast('success', 'Poznamka ulozena')
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      setIsEditingNote(false)
    },
    onError: () => {
      showToast('error', 'Nepodarilo se ulozit poznamku')
    },
  })

  // Search users when query changes
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

  // Focus search input when user search is shown
  useEffect(() => {
    if (showUserSearch && searchInputRef.current) {
      searchInputRef.current.focus()
    }
  }, [showUserSearch])

  // Auto-switch from 7-day to 5-day view when weekends are disabled
  useEffect(() => {
    if (!showWeekends) {
      const calendarApi = calendarRef.current?.getApi()
      if (calendarApi && calendarApi.view.type === 'timeGridWeek') {
        calendarApi.changeView('timeGrid5Day')
      }
    }
  }, [showWeekends])

  const getSlotColors = useCallback((slot: Slot) => {
    switch (slot.status) {
      case 'reserved':
        return { bg: '#dbeafe', border: '#3b82f6', text: '#1e40af' }
      case 'blocked':
        return { bg: '#fee2e2', border: '#ef4444', text: '#991b1b' }
      case 'locked':
        return { bg: '#e5e7eb', border: '#9ca3af', text: '#6b7280' }
      case 'unlocked':
      default:
        return { bg: '#dcfce7', border: '#22c55e', text: '#166534' }
    }
  }, [])

  const events: CalendarEvent[] = useMemo(() => {
    return slots?.map((slot) => {
      const colors = getSlotColors(slot)
      let title = ''
      switch (slot.status) {
        case 'reserved':
          title = slot.assignedUserName || slot.assignedUserEmail || 'Rezervovano'
          break
        case 'blocked':
          title = 'Blokovano'
          break
        case 'locked':
          title = '🔒 Uzamceno'
          break
        case 'unlocked':
          title = 'Volne'
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
        extendedProps: { slot },
      }
    }) || []
  }, [slots, getSlotColors])

  const handleEventClick = (info: EventClickArg) => {
    const slot = info.event.extendedProps.slot as Slot
    setSelectedSlot(slot)
    setShowUserSearch(false)
    setSearchQuery('')
    setSelectedUser(null)
    setDeductCredits(false)
    setNoteText(slot.note || '')
    setIsEditingNote(false)
  }

  const handleDateClick = (info: DateClickArg) => {
    const date = info.dateStr.split('T')[0]
    const time = info.dateStr.includes('T') ? info.dateStr.split('T')[1].substring(0, 5) : '09:00'
    setCreateDate(date)
    setCreateTime(time)
    setShowCreateModal(true)
  }

  const handleEventDrop = (info: EventDropArg) => {
    const slot = info.event.extendedProps.slot
    if (!info.event.start) {
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

    // Calculate new end time based on duration
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
          showToast('error', 'Nepodařilo se přesunout slot')
        },
      }
    )
  }

  const handleDatesSet = (info: { startStr: string; endStr: string }) => {
    setDateRange({
      start: info.startStr.split('T')[0],
      end: info.endStr.split('T')[0],
    })
  }

  const resetCreateForm = () => {
    setCreateDate('')
    setCreateTime('09:00')
    setCreateDuration(60)
    setCreateNote('')
  }

  const handleCreateSlot = () => {
    createSlotMutation.mutate({
      date: createDate,
      startTime: createTime,
      durationMinutes: createDuration,
      note: createNote || undefined,
    })
  }

  const handleCloseModal = () => {
    setSelectedSlot(null)
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

  const handleUnlockSlot = () => {
    if (!selectedSlot) return
    updateSlotMutation.mutate({
      id: selectedSlot.id,
      params: { status: 'unlocked' },
    })
  }

  const handleLockSlot = () => {
    if (!selectedSlot) return
    updateSlotMutation.mutate({
      id: selectedSlot.id,
      params: { status: 'locked' },
    })
  }

  const handleBlockSlot = () => {
    if (!selectedSlot) return
    updateSlotMutation.mutate({
      id: selectedSlot.id,
      params: { status: selectedSlot.status === 'blocked' ? 'unlocked' : 'blocked' },
    })
  }

  const handleDeleteSlot = () => {
    if (!selectedSlot) return
    deleteSlotMutation.mutate(selectedSlot.id)
  }

  const handleSelectUser = (user: User) => {
    setSelectedUser(user)
    setSearchQuery('')
    setSearchResults([])
  }

  const handleCreateReservation = () => {
    if (!selectedSlot || !selectedUser) return
    createReservationMutation.mutate({
      userId: selectedUser.id,
      date: selectedSlot.date,
      startTime: selectedSlot.startTime,
      endTime: selectedSlot.endTime,
      blockId: selectedSlot.id,
      deductCredits,
    })
  }

  const handleCancelReservation = () => {
    if (!selectedSlot?.reservationId) return
    cancelReservationMutation.mutate({
      id: selectedSlot.reservationId,
      refund: cancelWithRefund,
    })
  }

  const openCancelConfirm = (withRefund: boolean) => {
    setCancelWithRefund(withRefund)
    setShowCancelConfirm(true)
  }

  const handleSaveNote = () => {
    if (!selectedSlot?.reservationId) return
    updateNoteMutation.mutate({
      id: selectedSlot.reservationId,
      note: noteText || null,
    })
  }

  const getWeekMonday = () => {
    const calendarApi = calendarRef.current?.getApi()
    // Use view.currentStart to get the actual displayed week's start date
    const viewStart = calendarApi?.view?.currentStart
    const baseDate = viewStart ? new Date(viewStart) : new Date()
    const day = baseDate.getDay()
    const diff = day === 0 ? -6 : 1 - day // Days to Monday (Sunday = -6, Mon = 0, Tue = -1, etc.)
    baseDate.setDate(baseDate.getDate() + diff)
    return baseDate.toISOString().split('T')[0]
  }

  const handleUnlockWeek = () => {
    const monday = getWeekMonday()
    unlockWeekMutation.mutate(monday)
  }

  const handleApplyTemplate = () => {
    if (!selectedTemplateId) return
    const monday = getWeekMonday()
    applyTemplateMutation.mutate({
      templateId: selectedTemplateId,
      weekStartDate: monday,
    })
  }

  const formatSlotTime = (time: string) => {
    return time.substring(0, 5)
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {t('admin.calendar')}
        </h1>
        <div className="flex flex-wrap items-center gap-2">
          {/* View lock toggle */}
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
            <span className="text-sm font-medium">Zamknout</span>
          </button>

          {/* Weekends toggle */}
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
            <span className="text-sm font-medium">Víkendy</span>
          </button>

          <Button
            variant="secondary"
            size="sm"
            onClick={() => setShowTemplateModal(true)}
          >
            <LayoutTemplate size={16} className="mr-1" />
            Šablona
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={handleUnlockWeek}
            isLoading={unlockWeekMutation.isPending}
          >
            <Unlock size={16} className="mr-1" />
            Odemknout týden
          </Button>
        </div>
      </div>

      {/* Legend */}
      <div className="flex flex-wrap items-center gap-4 text-sm">
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded bg-gray-200 border border-gray-400"></div>
          <span className="text-neutral-600 dark:text-neutral-400">Uzamceno</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded bg-green-200 border border-green-500"></div>
          <span className="text-neutral-600 dark:text-neutral-400">Volne</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded bg-blue-200 border border-blue-500"></div>
          <span className="text-neutral-600 dark:text-neutral-400">Rezervovano</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded bg-red-200 border border-red-500"></div>
          <span className="text-neutral-600 dark:text-neutral-400">Blokovano</span>
        </div>
      </div>

      <Card variant="bordered" padding="none" className="overflow-hidden">
        {isLoading && !slots ? (
          <div className="flex justify-center py-12">
            <Spinner size="lg" />
          </div>
        ) : (
          <div className={`p-4 [&_.fc-timegrid-slot]:!h-4 md:[&_.fc-timegrid-slot]:!h-8 transition-opacity ${isFetching ? 'opacity-60' : ''}`}>
            <FullCalendar
              ref={calendarRef}
              plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
              initialView={showWeekends ? "timeGridWeek" : "timeGrid5Day"}
              locale={i18n.language === 'cs' ? csLocale : undefined}
              views={{
                timeGridDay: {
                  type: 'timeGrid',
                  duration: { days: 1 },
                  buttonText: i18n.language === 'cs' ? '1 den' : '1 day',
                },
                timeGrid3Day: {
                  type: 'timeGrid',
                  duration: { days: 3 },
                  buttonText: i18n.language === 'cs' ? '3 dny' : '3 days',
                },
                timeGrid5Day: {
                  type: 'timeGrid',
                  duration: { days: 5 },
                  buttonText: i18n.language === 'cs' ? '5 dnů' : '5 days',
                },
                ...(showWeekends ? {
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
                right: showWeekends
                  ? 'timeGridWeek,timeGrid5Day,timeGrid3Day,timeGridDay'
                  : 'timeGrid5Day,timeGrid3Day,timeGridDay',
              }}
              events={events}
              eventClick={handleEventClick}
              dateClick={isViewLocked ? undefined : handleDateClick}
              eventDrop={handleEventDrop as any}
              datesSet={handleDatesSet}
              editable={!isViewLocked}
              droppable={!isViewLocked}
              slotMinTime="06:00:00"
              slotMaxTime="22:00:00"
              allDaySlot={false}
              weekends={showWeekends}
              nowIndicator={true}
              eventDisplay="block"
              height="auto"
              slotDuration="00:15:00"
              snapDuration="00:15:00"
              selectable={!isViewLocked}
              longPressDelay={300}
              eventLongPressDelay={300}
              selectLongPressDelay={300}
              eventContent={(eventInfo) => (
                <div className="p-1 text-xs overflow-hidden">
                  <div className="font-medium truncate">{eventInfo.event.title}</div>
                </div>
              )}
            />
          </div>
        )}
      </Card>

      {/* Create slot modal */}
      <Modal
        isOpen={showCreateModal}
        onClose={() => {
          setShowCreateModal(false)
          resetCreateForm()
        }}
        title="Vytvorit slot"
        size="sm"
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
              Datum
            </label>
            <Input
              type="date"
              value={createDate}
              onChange={(e) => setCreateDate(e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
              Cas
            </label>
            <Input
              type="time"
              value={createTime}
              onChange={(e) => setCreateTime(e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
              Delka (minuty)
            </label>
            <select
              value={createDuration}
              onChange={(e) => setCreateDuration(Number(e.target.value))}
              className="w-full px-3 py-2 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white"
            >
              <option value={30}>30 minut</option>
              <option value={45}>45 minut</option>
              <option value={60}>60 minut</option>
              <option value={90}>90 minut</option>
              <option value={120}>120 minut</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
              Poznamka (volitelne)
            </label>
            <textarea
              value={createNote}
              onChange={(e) => setCreateNote(e.target.value)}
              className="w-full px-3 py-2 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white resize-none"
              rows={2}
            />
          </div>
          <Button
            className="w-full"
            onClick={handleCreateSlot}
            isLoading={createSlotMutation.isPending}
          >
            Vytvorit slot
          </Button>
        </div>
      </Modal>

      {/* Template selection modal */}
      <Modal
        isOpen={showTemplateModal}
        onClose={() => {
          setShowTemplateModal(false)
          setSelectedTemplateId(null)
        }}
        title="Aplikovat sablonu"
        size="md"
      >
        <div className="space-y-4">
          <p className="text-sm text-neutral-600 dark:text-neutral-400">
            Vyberte sablonu pro vygenerovani slotu na aktualni tyden. Sloty budou vytvoreny jako uzamcene.
          </p>
          {templates && templates.length > 0 ? (
            <div className="space-y-2">
              {templates.filter(t => t.isActive).map((template) => (
                <button
                  key={template.id}
                  onClick={() => setSelectedTemplateId(template.id)}
                  className={`w-full p-3 text-left rounded-lg border transition-colors ${
                    selectedTemplateId === template.id
                      ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                      : 'border-neutral-200 dark:border-neutral-700 hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover'
                  }`}
                >
                  <p className="font-medium text-neutral-900 dark:text-white">{template.name}</p>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400">
                    {template.slots.length} slotu
                  </p>
                </button>
              ))}
            </div>
          ) : (
            <p className="text-center text-neutral-500 dark:text-neutral-400 py-4">
              Zadne sablony. Vytvorte sablonu v sekci Sablony.
            </p>
          )}
          <Button
            className="w-full"
            onClick={handleApplyTemplate}
            disabled={!selectedTemplateId}
            isLoading={applyTemplateMutation.isPending}
          >
            Aplikovat sablonu
          </Button>
        </div>
      </Modal>

      {/* Fullscreen user search popup */}
      {showUserSearch && (
        <div className="fixed inset-0 z-[60] bg-white dark:bg-dark-bg flex flex-col">
          {/* Header */}
          <div className="flex items-center gap-3 p-4 border-b border-neutral-200 dark:border-neutral-700">
            <button
              onClick={() => {
                setShowUserSearch(false)
                setSearchQuery('')
                setSearchResults([])
              }}
              className="p-2 -ml-2 hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-lg"
            >
              <X size={24} className="text-neutral-600 dark:text-neutral-300" />
            </button>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white">
              Vybrat uzivatele
            </h2>
          </div>

          {/* Search input */}
          <div className="p-4 border-b border-neutral-200 dark:border-neutral-700">
            <div className="relative">
              <Search size={20} className="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400" />
              <input
                ref={searchInputRef}
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Vyhledat podle jmena nebo emailu..."
                className="w-full pl-10 pr-4 py-3 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 text-base"
                autoComplete="off"
              />
              {searchQuery && (
                <button
                  onClick={() => {
                    setSearchQuery('')
                    setSearchResults([])
                  }}
                  className="absolute right-3 top-1/2 -translate-y-1/2 p-1 hover:bg-neutral-100 dark:hover:bg-neutral-700 rounded"
                >
                  <X size={18} className="text-neutral-400" />
                </button>
              )}
            </div>
          </div>

          {/* Results */}
          <div className="flex-1 overflow-y-auto">
            {isSearching && (
              <div className="flex justify-center py-8">
                <Spinner size="md" />
              </div>
            )}

            {!isSearching && searchQuery.length >= 2 && searchResults.length === 0 && (
              <div className="text-center py-8 text-neutral-500 dark:text-neutral-400">
                Zadni uzivatele nenalezeni
              </div>
            )}

            {!isSearching && searchQuery.length < 2 && (
              <div className="text-center py-8 text-neutral-500 dark:text-neutral-400">
                Zadejte alespon 2 znaky pro vyhledavani
              </div>
            )}

            {!isSearching && searchResults.length > 0 && (
              <div className="divide-y divide-neutral-200 dark:divide-neutral-700">
                {searchResults.map((user) => (
                  <button
                    key={user.id}
                    onClick={() => {
                      handleSelectUser(user)
                      setShowUserSearch(false)
                    }}
                    className="w-full p-4 text-left hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover transition-colors"
                  >
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center flex-shrink-0">
                        <span className="text-primary-600 dark:text-primary-400 font-medium">
                          {(user.firstName?.[0] || user.email[0]).toUpperCase()}
                        </span>
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-neutral-900 dark:text-white truncate">
                          {user.firstName && user.lastName
                            ? `${user.firstName} ${user.lastName}`
                            : user.email}
                        </p>
                        {user.firstName && user.lastName && (
                          <p className="text-sm text-neutral-500 dark:text-neutral-400 truncate">
                            {user.email}
                          </p>
                        )}
                        <p className="text-xs text-neutral-400">
                          Kredity: {user.credits}
                        </p>
                      </div>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Slot detail modal */}
      <Modal
        isOpen={!!selectedSlot}
        onClose={handleCloseModal}
        title="Detail slotu"
        size="md"
      >
        {selectedSlot && (
          <div className="space-y-4">
            <div>
              <p className="text-sm text-neutral-500 dark:text-neutral-400">Datum a cas</p>
              <p className="font-medium text-neutral-900 dark:text-white">
                {new Date(selectedSlot.date).toLocaleDateString(i18n.language, {
                  weekday: 'long',
                  day: 'numeric',
                  month: 'long',
                  year: 'numeric',
                })}
              </p>
              <p className="font-mono text-neutral-900 dark:text-white">
                {formatSlotTime(selectedSlot.startTime)} - {formatSlotTime(selectedSlot.endTime)}
              </p>
            </div>

            <div>
              <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">Status</p>
              <Badge
                variant={
                  selectedSlot.status === 'reserved'
                    ? 'primary'
                    : selectedSlot.status === 'blocked'
                      ? 'danger'
                      : selectedSlot.status === 'locked'
                        ? 'default'
                        : 'success'
                }
              >
                {selectedSlot.status === 'reserved' ? 'Rezervovano' :
                 selectedSlot.status === 'blocked' ? 'Blokovano' :
                 selectedSlot.status === 'locked' ? 'Uzamceno' : 'Volne'}
              </Badge>
            </div>

            {/* Reserved slot - show reservation info and cancel option */}
            {selectedSlot.status === 'reserved' && selectedSlot.reservationId && (
              <>
                <div className="p-3 rounded-lg bg-blue-50 dark:bg-blue-900/20">
                  <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">Rezervace</p>
                  <p className="font-medium text-neutral-900 dark:text-white">
                    {selectedSlot.assignedUserName || selectedSlot.assignedUserEmail || 'Nezname'}
                  </p>
                  {selectedSlot.assignedUserName && selectedSlot.assignedUserEmail && (
                    <p className="text-sm text-neutral-600 dark:text-neutral-300">
                      {selectedSlot.assignedUserEmail}
                    </p>
                  )}
                </div>

                {/* Note section */}
                <div>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">Poznamka</p>
                  {isEditingNote ? (
                    <div className="space-y-2">
                      <textarea
                        value={noteText}
                        onChange={(e) => setNoteText(e.target.value)}
                        placeholder="Napiste poznamku..."
                        className="w-full px-3 py-2 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
                        rows={3}
                      />
                      <div className="flex gap-2">
                        <Button
                          variant="secondary"
                          size="sm"
                          className="flex-1"
                          onClick={() => {
                            setIsEditingNote(false)
                            setNoteText(selectedSlot.note || '')
                          }}
                        >
                          Zrusit
                        </Button>
                        <Button
                          size="sm"
                          className="flex-1"
                          onClick={handleSaveNote}
                          isLoading={updateNoteMutation.isPending}
                        >
                          Ulozit
                        </Button>
                      </div>
                    </div>
                  ) : (
                    <div
                      onClick={() => setIsEditingNote(true)}
                      className="p-2 rounded-lg bg-neutral-50 dark:bg-dark-surface border border-neutral-200 dark:border-neutral-700 cursor-pointer hover:bg-neutral-100 dark:hover:bg-dark-surfaceHover min-h-[60px]"
                    >
                      {selectedSlot.note ? (
                        <p className="text-sm text-neutral-700 dark:text-neutral-300 whitespace-pre-wrap">
                          {selectedSlot.note}
                        </p>
                      ) : (
                        <p className="text-sm text-neutral-400 italic">
                          Kliknutim pridejte poznamku...
                        </p>
                      )}
                    </div>
                  )}
                </div>

                <div className="pt-2 space-y-2">
                  <Button
                    className="w-full"
                    variant="danger"
                    onClick={() => openCancelConfirm(true)}
                  >
                    <UserMinus size={18} className="mr-2" />
                    Zrusit rezervaci (vratit kredity)
                  </Button>
                  <Button
                    className="w-full"
                    variant="secondary"
                    onClick={() => openCancelConfirm(false)}
                  >
                    Zrusit bez vraceni kreditu
                  </Button>
                </div>
              </>
            )}

            {/* Cancel confirmation */}
            {showCancelConfirm && selectedSlot.reservationId && (
              <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
                <div className="bg-white dark:bg-dark-surface rounded-lg p-6 max-w-sm mx-4 shadow-xl">
                  <h3 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
                    Potvrdit zruseni
                  </h3>
                  <p className="text-neutral-600 dark:text-neutral-300 mb-4">
                    Opravdu chcete zrusit rezervaci pro{' '}
                    <strong>{selectedSlot.assignedUserName || selectedSlot.assignedUserEmail}</strong>?
                    {cancelWithRefund && ' Kredity budou vraceny.'}
                    {!cancelWithRefund && ' Kredity nebudou vraceny.'}
                  </p>
                  <div className="flex gap-3">
                    <Button
                      variant="secondary"
                      className="flex-1"
                      onClick={() => setShowCancelConfirm(false)}
                    >
                      Ne
                    </Button>
                    <Button
                      variant="danger"
                      className="flex-1"
                      onClick={handleCancelReservation}
                      isLoading={cancelReservationMutation.isPending}
                    >
                      Ano, zrusit
                    </Button>
                  </div>
                </div>
              </div>
            )}

            {/* Locked slot actions */}
            {selectedSlot.status === 'locked' && (
              <div className="pt-2 space-y-2">
                <Button
                  className="w-full"
                  variant="primary"
                  onClick={handleUnlockSlot}
                  isLoading={updateSlotMutation.isPending}
                >
                  <Unlock size={18} className="mr-2" />
                  Odemknout slot
                </Button>
                <Button
                  className="w-full"
                  variant="danger"
                  onClick={handleDeleteSlot}
                  isLoading={deleteSlotMutation.isPending}
                >
                  Smazat slot
                </Button>
              </div>
            )}

            {/* Unlocked slot actions */}
            {selectedSlot.status === 'unlocked' && (
              <div className="pt-2 space-y-3">
                {selectedUser ? (
                  <>
                    <div className="p-3 rounded-lg bg-green-50 dark:bg-green-900/20 flex items-center justify-between">
                      <div>
                        <p className="font-medium text-neutral-900 dark:text-white">
                          {selectedUser.firstName} {selectedUser.lastName}
                        </p>
                        <p className="text-sm text-neutral-600 dark:text-neutral-300">
                          {selectedUser.email}
                        </p>
                        <p className="text-xs text-neutral-500">
                          Kredity: {selectedUser.credits}
                        </p>
                      </div>
                      <button
                        onClick={() => setSelectedUser(null)}
                        className="p-1 hover:bg-neutral-200 dark:hover:bg-neutral-700 rounded"
                      >
                        <X size={18} />
                      </button>
                    </div>
                    <label className="flex items-center gap-2 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={deductCredits}
                        onChange={(e) => setDeductCredits(e.target.checked)}
                        className="w-4 h-4 rounded border-neutral-300 text-primary-500 focus:ring-primary-500"
                      />
                      <span className="text-sm text-neutral-700 dark:text-neutral-300">
                        Odecist kredit ({selectedUser.credits} dostupnych)
                      </span>
                    </label>
                    <div className="flex gap-2">
                      <Button
                        variant="secondary"
                        className="flex-1"
                        onClick={() => setSelectedUser(null)}
                      >
                        Zmenit
                      </Button>
                      <Button
                        className="flex-1"
                        onClick={handleCreateReservation}
                        isLoading={createReservationMutation.isPending}
                      >
                        Prihlasit
                      </Button>
                    </div>
                  </>
                ) : (
                  <>
                    <Button
                      className="w-full"
                      variant="primary"
                      onClick={() => setShowUserSearch(true)}
                    >
                      <UserPlus size={18} className="mr-2" />
                      Prihlasit uzivatele
                    </Button>
                    <Button
                      className="w-full"
                      variant="secondary"
                      onClick={handleLockSlot}
                      isLoading={updateSlotMutation.isPending}
                    >
                      <Lock size={18} className="mr-2" />
                      Zamknout slot
                    </Button>
                    <Button
                      className="w-full"
                      variant="danger"
                      onClick={handleBlockSlot}
                      isLoading={updateSlotMutation.isPending}
                    >
                      Zablokovat slot
                    </Button>
                  </>
                )}
              </div>
            )}

            {/* Blocked slot actions */}
            {selectedSlot.status === 'blocked' && (
              <div className="pt-2 space-y-2">
                <Button
                  className="w-full"
                  variant="secondary"
                  onClick={handleBlockSlot}
                  isLoading={updateSlotMutation.isPending}
                >
                  Odblokovat slot
                </Button>
                <Button
                  className="w-full"
                  variant="danger"
                  onClick={handleDeleteSlot}
                  isLoading={deleteSlotMutation.isPending}
                >
                  Smazat slot
                </Button>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  )
}
