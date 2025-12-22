import { useState, useRef, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import csLocale from '@fullcalendar/core/locales/cs'
import { Search, X, UserPlus, UserMinus } from 'lucide-react'
import { Card, Modal, Badge, Button, Spinner } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { adminApi } from '@/services/api'
import type { EventClickArg } from '@fullcalendar/core'
import type { AdminCalendarSlot, User } from '@/types/api'

interface CalendarEvent {
  id: string
  title: string
  start: string
  end: string
  backgroundColor: string
  borderColor: string
  textColor: string
  extendedProps: {
    slot: AdminCalendarSlot
  }
}

export default function AdminCalendar() {
  const { t, i18n } = useTranslation()
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const calendarRef = useRef<FullCalendar>(null)
  const [selectedSlot, setSelectedSlot] = useState<AdminCalendarSlot | null>(null)
  const [dateRange, setDateRange] = useState<{ start: string; end: string }>({
    start: new Date().toISOString().split('T')[0],
    end: new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
  })

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

  const { data: slots, isLoading } = useQuery({
    queryKey: ['admin', 'calendar', 'slots', dateRange],
    queryFn: () => adminApi.getCalendarSlots(dateRange.start, dateRange.end),
  })

  const blockMutation = useMutation({
    mutationFn: adminApi.blockSlot,
    onSuccess: () => {
      showToast('success', 'Slot byl upraven')
      queryClient.invalidateQueries({ queryKey: ['admin', 'calendar', 'slots'] })
      setSelectedSlot(null)
    },
    onError: () => {
      showToast('error', 'Nepodarilo se upravit slot')
    },
  })

  const createReservationMutation = useMutation({
    mutationFn: adminApi.createReservation,
    onSuccess: () => {
      showToast('success', 'Rezervace byla vytvorena')
      queryClient.invalidateQueries({ queryKey: ['admin', 'calendar', 'slots'] })
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
      queryClient.invalidateQueries({ queryKey: ['admin', 'calendar', 'slots'] })
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
      queryClient.invalidateQueries({ queryKey: ['admin', 'calendar', 'slots'] })
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

  const getSlotColors = (slot: AdminCalendarSlot) => {
    switch (slot.status) {
      case 'reserved':
        return { bg: '#dbeafe', border: '#3b82f6', text: '#1e40af' }
      case 'blocked':
        return { bg: '#fee2e2', border: '#ef4444', text: '#991b1b' }
      case 'past':
        return { bg: '#f3f4f6', border: '#9ca3af', text: '#6b7280' }
      case 'available':
      default:
        return { bg: '#dcfce7', border: '#22c55e', text: '#166534' }
    }
  }

  const events: CalendarEvent[] = slots?.map((slot) => {
    const colors = getSlotColors(slot)
    const title = slot.status === 'reserved'
      ? (slot.reservation?.userName || slot.reservation?.userEmail || 'Rezervovano')
      : slot.status === 'blocked'
        ? 'Blokovano'
        : slot.status === 'past'
          ? 'Minule'
          : 'Volne'

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

  const handleEventClick = (info: EventClickArg) => {
    const slot = info.event.extendedProps.slot as AdminCalendarSlot
    setSelectedSlot(slot)
    setShowUserSearch(false)
    setSearchQuery('')
    setSelectedUser(null)
    setDeductCredits(false)
    setNoteText(slot.reservation?.note || '')
    setIsEditingNote(false)
  }

  const handleDatesSet = (info: { startStr: string; endStr: string }) => {
    setDateRange({
      start: info.startStr.split('T')[0],
      end: info.endStr.split('T')[0],
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

  const handleBlockSlot = () => {
    if (!selectedSlot) return
    blockMutation.mutate({
      date: selectedSlot.date,
      startTime: selectedSlot.startTime,
      endTime: selectedSlot.endTime,
      isBlocked: selectedSlot.status !== 'blocked',
    })
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
      blockId: selectedSlot.blockId,
      deductCredits,
    })
  }

  const handleCancelReservation = () => {
    if (!selectedSlot?.reservation) return
    cancelReservationMutation.mutate({
      id: selectedSlot.reservation.id,
      refund: cancelWithRefund,
    })
  }

  const openCancelConfirm = (withRefund: boolean) => {
    setCancelWithRefund(withRefund)
    setShowCancelConfirm(true)
  }

  const handleSaveNote = () => {
    if (!selectedSlot?.reservation) return
    updateNoteMutation.mutate({
      id: selectedSlot.reservation.id,
      note: noteText || null,
    })
  }

  const formatSlotTime = (time: string) => {
    return time.substring(0, 5)
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {t('admin.calendar')}
        </h1>
        <div className="flex items-center gap-4 text-sm">
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
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded bg-gray-200 border border-gray-400"></div>
            <span className="text-neutral-600 dark:text-neutral-400">Minule</span>
          </div>
        </div>
      </div>

      <Card variant="bordered" padding="none" className="overflow-hidden">
        {isLoading ? (
          <div className="flex justify-center py-12">
            <Spinner size="lg" />
          </div>
        ) : (
          <div className="p-4 [&_.fc-timegrid-slot]:!h-8 md:[&_.fc-timegrid-slot]:!h-16">
            <FullCalendar
              ref={calendarRef}
              plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
              initialView="timeGridWeek"
              locale={i18n.language === 'cs' ? csLocale : undefined}
              headerToolbar={{
                left: 'prev,next today',
                center: 'title',
                right: 'timeGridWeek,timeGridDay',
              }}
              events={events}
              eventClick={handleEventClick}
              datesSet={handleDatesSet}
              slotMinTime="06:00:00"
              slotMaxTime="22:00:00"
              allDaySlot={false}
              weekends={false}
              nowIndicator={true}
              eventDisplay="block"
              height="auto"
              slotDuration="01:00:00"
              eventContent={(eventInfo) => (
                <div className="p-1 text-xs overflow-hidden">
                  <div className="font-medium truncate">{eventInfo.event.title}</div>
                </div>
              )}
            />
          </div>
        )}
      </Card>

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
                      : selectedSlot.status === 'past'
                        ? 'default'
                        : 'success'
                }
              >
                {selectedSlot.status === 'reserved' ? 'Rezervovano' :
                 selectedSlot.status === 'blocked' ? 'Blokovano' :
                 selectedSlot.status === 'past' ? 'Minule' : 'Volne'}
              </Badge>
            </div>

            {/* Reserved slot - show reservation info and cancel option */}
            {selectedSlot.reservation && (
              <>
                <div className="p-3 rounded-lg bg-blue-50 dark:bg-blue-900/20">
                  <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">Rezervace</p>
                  <p className="font-medium text-neutral-900 dark:text-white">
                    {selectedSlot.reservation.userName || selectedSlot.reservation.userEmail || 'Nezname'}
                  </p>
                  {selectedSlot.reservation.userName && selectedSlot.reservation.userEmail && (
                    <p className="text-sm text-neutral-600 dark:text-neutral-300">
                      {selectedSlot.reservation.userEmail}
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
                            setNoteText(selectedSlot.reservation?.note || '')
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
                      {selectedSlot.reservation.note ? (
                        <p className="text-sm text-neutral-700 dark:text-neutral-300 whitespace-pre-wrap">
                          {selectedSlot.reservation.note}
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
            {showCancelConfirm && selectedSlot?.reservation && (
              <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
                <div className="bg-white dark:bg-dark-surface rounded-lg p-6 max-w-sm mx-4 shadow-xl">
                  <h3 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
                    Potvrdit zruseni
                  </h3>
                  <p className="text-neutral-600 dark:text-neutral-300 mb-4">
                    Opravdu chcete zrusit rezervaci pro{' '}
                    <strong>{selectedSlot.reservation.userName || selectedSlot.reservation.userEmail}</strong>?
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

            {/* Available or blocked slot - show assign user option */}
            {selectedSlot.status !== 'reserved' && selectedSlot.status !== 'past' && (
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
                      variant={selectedSlot.status === 'blocked' ? 'secondary' : 'danger'}
                      onClick={handleBlockSlot}
                      isLoading={blockMutation.isPending}
                    >
                      {selectedSlot.status === 'blocked' ? 'Odblokovat slot' : 'Zablokovat slot'}
                    </Button>
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
