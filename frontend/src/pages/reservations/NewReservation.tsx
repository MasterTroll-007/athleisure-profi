import { useState, useRef } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import csLocale from '@fullcalendar/core/locales/cs'
import { CreditCard } from 'lucide-react'
import { Card, Button, Modal, Spinner } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { reservationApi, creditApi } from '@/services/api'
import { useAuthStore } from '@/stores/authStore'
import { formatTime } from '@/utils/formatters'
import type { EventClickArg } from '@fullcalendar/core'
import type { AvailableSlot, Reservation } from '@/types/api'

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
    type: 'slot' | 'reservation'
  }
}

export default function NewReservation() {
  const { t, i18n } = useTranslation()
  const { user, updateUser } = useAuthStore()
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const calendarRef = useRef<FullCalendar>(null)

  const [selectedSlot, setSelectedSlot] = useState<AvailableSlot | null>(null)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [currentDate, setCurrentDate] = useState<Date>(new Date())
  const [dateRange, setDateRange] = useState<{ start: string; end: string }>({
    start: new Date().toISOString().split('T')[0],
    end: new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
  })

  const { data: slotsResponse, isLoading } = useQuery({
    queryKey: ['availableSlots', 'range', dateRange],
    queryFn: () => reservationApi.getAvailableSlotsRange(dateRange.start, dateRange.end),
  })

  const { data: myReservations } = useQuery({
    queryKey: ['myReservations'],
    queryFn: () => reservationApi.getMyReservations(),
  })

  const { data: pricingItems } = useQuery({
    queryKey: ['pricing'],
    queryFn: creditApi.getPricing,
  })

  const defaultPricing = pricingItems?.find((p) => p.credits === 1)

  const createMutation = useMutation({
    mutationFn: reservationApi.create,
    onSuccess: () => {
      showToast('success', t('reservation.success'))
      setIsModalOpen(false)
      setSelectedSlot(null)
      // Invalidate all relevant queries
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

  const getSlotColors = (slot: AvailableSlot) => {
    if (!slot.isAvailable) {
      return { bg: '#f3f4f6', border: '#9ca3af', text: '#6b7280' }
    }
    return { bg: '#dcfce7', border: '#22c55e', text: '#166534' }
  }

  const reservationEvents: CalendarEvent[] = myReservations?.map((reservation) => ({
    id: `reservation-${reservation.id}`,
    title: 'Trénink',
    start: `${reservation.date}T${reservation.startTime}`,
    end: `${reservation.date}T${reservation.endTime}`,
    backgroundColor: '#dbeafe',
    borderColor: '#3b82f6',
    textColor: '#1e40af',
    extendedProps: { reservation, type: 'reservation' },
  })) || []

  // Filter out slots that match user reservations (to avoid duplicates)
  const reservedSlotTimes = new Set(
    myReservations?.map(r => `${r.date}T${r.startTime}`) || []
  )

  const slotEvents: CalendarEvent[] = slotsResponse?.slots
    ?.filter(slot => !reservedSlotTimes.has(slot.start))
    .map((slot, index) => {
      const colors = getSlotColors(slot)
      return {
        id: `slot-${slot.blockId}-${index}`,
        title: slot.isAvailable ? 'Volne' : 'Nedostupne',
        start: slot.start,
        end: slot.end,
        backgroundColor: colors.bg,
        borderColor: colors.border,
        textColor: colors.text,
        extendedProps: { slot, type: 'slot' },
      }
    }) || []

  const events: CalendarEvent[] = [...slotEvents, ...reservationEvents]

  const handleEventClick = (info: EventClickArg) => {
    // If it's a reservation, don't allow rebooking
    if (info.event.extendedProps.type === 'reservation') {
      return
    }

    const slot = info.event.extendedProps.slot
    if (!slot || !slot.isAvailable) {
      showToast('error', 'Tento slot neni k dispozici')
      return
    }
    if ((user?.credits || 0) < 1) {
      showToast('error', t('reservation.notEnoughCredits'))
      return
    }
    setSelectedSlot(slot)
    setIsModalOpen(true)
  }

  const handleDatesSet = (info: { startStr: string; endStr: string; start: Date }) => {
    setCurrentDate(info.start)
    setDateRange({
      start: info.startStr.split('T')[0],
      end: info.endStr.split('T')[0],
    })
  }

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

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {t('reservation.title')}
        </h1>
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2 px-3 py-2 bg-primary-50 dark:bg-primary-900/20 rounded-lg">
            <CreditCard size={18} className="text-primary-500" />
            <span className="text-sm text-primary-700 dark:text-primary-300">
              {t('home.yourCredits')}: <strong>{user?.credits || 0}</strong>
            </span>
          </div>
        </div>
      </div>

      {/* Legend */}
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
          <div className="w-3 h-3 rounded bg-gray-200 border border-gray-400"></div>
          <span className="text-neutral-600 dark:text-neutral-400">Nedostupne</span>
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
              initialDate={currentDate}
              locale={i18n.language === 'cs' ? csLocale : undefined}
              views={{
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
              }}
              headerToolbar={{
                left: 'prev,next today',
                center: 'title',
                right: 'timeGridWeek,timeGrid5Day,timeGrid3Day,timeGridDay',
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
                <div className="p-1 text-xs overflow-hidden cursor-pointer">
                  <div className="font-medium truncate">{eventInfo.event.title}</div>
                </div>
              )}
            />
          </div>
        )}
      </Card>

      {/* Confirmation Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={t('reservation.confirm')}
        size="sm"
      >
        {selectedSlot && (
          <div className="space-y-4">
            <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg">
              <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">
                {t('reservation.date')}
              </p>
              <p className="font-medium text-neutral-900 dark:text-white">
                {new Date(selectedSlot.start).toLocaleDateString(i18n.language, {
                  weekday: 'long',
                  day: 'numeric',
                  month: 'long',
                  year: 'numeric',
                })}
              </p>
            </div>

            <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg">
              <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">
                {t('reservation.time')}
              </p>
              <p className="font-mono font-semibold text-lg text-neutral-900 dark:text-white">
                {formatTime(selectedSlot.start.split('T')[1])} -{' '}
                {formatTime(selectedSlot.end.split('T')[1])}
              </p>
            </div>

            <div className="p-4 bg-primary-50 dark:bg-primary-900/20 rounded-lg">
              <p className="text-sm text-primary-600 dark:text-primary-400 mb-1">
                {t('reservation.cost', { credits: 1 })}
              </p>
              <p className="font-semibold text-primary-700 dark:text-primary-300">
                1 kredit z {user?.credits || 0}
              </p>
            </div>

            <div className="flex gap-3 pt-2">
              <Button
                variant="secondary"
                className="flex-1"
                onClick={() => setIsModalOpen(false)}
              >
                {t('common.cancel')}
              </Button>
              <Button
                className="flex-1"
                onClick={handleConfirm}
                isLoading={createMutation.isPending}
              >
                {t('reservation.book')}
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
