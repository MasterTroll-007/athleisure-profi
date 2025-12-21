import { useState, useRef } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import csLocale from '@fullcalendar/core/locales/cs'
import { Card, Modal, Badge, Spinner } from '@/components/ui'
import { adminApi } from '@/services/api'
import { formatTime } from '@/utils/formatters'
import type { EventClickArg } from '@fullcalendar/core'
import type { Reservation } from '@/types/api'

interface CalendarEvent {
  id: string
  title: string
  start: string
  end: string
  backgroundColor: string
  borderColor: string
  extendedProps: {
    type: 'reservation' | 'availability'
    status?: string
    reservation?: Reservation
  }
}

export default function AdminCalendar() {
  const { t, i18n } = useTranslation()
  const calendarRef = useRef<FullCalendar>(null)
  const [selectedEvent, setSelectedEvent] = useState<CalendarEvent | null>(null)
  const [dateRange, setDateRange] = useState<{ start: string; end: string }>({
    start: new Date().toISOString().split('T')[0],
    end: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
  })

  const { data: reservations, isLoading: reservationsLoading } = useQuery({
    queryKey: ['admin', 'reservations', dateRange],
    queryFn: () => adminApi.getReservations(dateRange.start, dateRange.end),
  })

  const { data: availabilityBlocks, isLoading: blocksLoading } = useQuery({
    queryKey: ['admin', 'availability'],
    queryFn: adminApi.getBlocks,
  })

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'confirmed':
        return { bg: '#e0f0f5', border: '#60a5fa' }
      case 'cancelled':
        return { bg: '#f5e7e7', border: '#f87171' }
      case 'completed':
        return { bg: '#e7f5e8', border: '#4ade80' }
      default:
        return { bg: '#fff4e6', border: '#fbbf24' }
    }
  }

  const events: CalendarEvent[] = [
    // Reservations
    ...(reservations?.map((r) => {
      const colors = getStatusColor(r.status)
      return {
        id: r.id,
        title: r.userName || r.userEmail || 'Rezervace',
        start: `${r.date}T${r.startTime}`,
        end: `${r.date}T${r.endTime}`,
        backgroundColor: colors.bg,
        borderColor: colors.border,
        extendedProps: {
          type: 'reservation' as const,
          status: r.status,
          reservation: r,
        },
      }
    }) || []),
    // Availability blocks (shown as background events)
    ...(availabilityBlocks?.flatMap((block) => {
      // Generate recurring events for availability
      const blockEvents: CalendarEvent[] = []
      const today = new Date()
      const endDate = new Date(today)
      endDate.setDate(endDate.getDate() + 60) // Show 60 days ahead

      let current = new Date(today)
      while (current <= endDate) {
        // dayOfWeek: 1 = Monday, 7 = Sunday
        const currentDay = current.getDay() === 0 ? 7 : current.getDay()
        if (block.daysOfWeek.includes(currentDay)) {
          const dateStr = current.toISOString().split('T')[0]
          blockEvents.push({
            id: `avail-${block.id}-${dateStr}`,
            title: 'Dostupnost',
            start: `${dateStr}T${block.startTime}`,
            end: `${dateStr}T${block.endTime}`,
            backgroundColor: 'rgba(224, 93, 82, 0.1)',
            borderColor: 'rgba(224, 93, 82, 0.3)',
            extendedProps: {
              type: 'availability' as const,
            },
          })
        }
        current.setDate(current.getDate() + 1)
      }
      return blockEvents
    }) || []),
  ]

  const handleEventClick = (info: EventClickArg) => {
    const event = info.event
    if (event.extendedProps.type === 'reservation') {
      setSelectedEvent({
        id: event.id,
        title: event.title,
        start: event.startStr,
        end: event.endStr,
        backgroundColor: event.backgroundColor,
        borderColor: event.borderColor,
        extendedProps: event.extendedProps as CalendarEvent['extendedProps'],
      })
    }
  }

  const handleDatesSet = (info: { startStr: string; endStr: string }) => {
    setDateRange({
      start: info.startStr.split('T')[0],
      end: info.endStr.split('T')[0],
    })
  }

  const isLoading = reservationsLoading || blocksLoading

  return (
    <div className="space-y-6 animate-fade-in">
      <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
        {t('admin.calendar')}
      </h1>

      <Card variant="bordered" padding="none" className="overflow-hidden">
        {isLoading ? (
          <div className="flex justify-center py-12">
            <Spinner size="lg" />
          </div>
        ) : (
          <div className="p-4">
            <FullCalendar
              ref={calendarRef}
              plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
              initialView="timeGridWeek"
              locale={i18n.language === 'cs' ? csLocale : undefined}
              headerToolbar={{
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,timeGridWeek,timeGridDay',
              }}
              events={events}
              eventClick={handleEventClick}
              datesSet={handleDatesSet}
              slotMinTime="06:00:00"
              slotMaxTime="22:00:00"
              allDaySlot={false}
              weekends={true}
              nowIndicator={true}
              eventDisplay="block"
              height="auto"
              slotDuration="00:30:00"
            />
          </div>
        )}
      </Card>

      {/* Reservation detail modal */}
      <Modal
        isOpen={!!selectedEvent}
        onClose={() => setSelectedEvent(null)}
        title="Detail rezervace"
        size="sm"
      >
        {selectedEvent && selectedEvent.extendedProps.type === 'reservation' && (
          <div className="space-y-4">
            <div>
              <p className="text-sm text-neutral-500 dark:text-neutral-400">Klientka</p>
              <p className="font-medium text-neutral-900 dark:text-white">
                {selectedEvent.extendedProps.reservation?.userName || 'Neznámý'}
              </p>
              <p className="text-sm text-neutral-500 dark:text-neutral-400">
                {selectedEvent.extendedProps.reservation?.userEmail}
              </p>
            </div>

            <div>
              <p className="text-sm text-neutral-500 dark:text-neutral-400">Datum a čas</p>
              <p className="font-medium text-neutral-900 dark:text-white">
                {new Date(selectedEvent.start).toLocaleDateString(i18n.language, {
                  weekday: 'long',
                  day: 'numeric',
                  month: 'long',
                })}
              </p>
              <p className="font-mono text-neutral-900 dark:text-white">
                {formatTime(selectedEvent.start.split('T')[1])} -{' '}
                {formatTime(selectedEvent.end.split('T')[1])}
              </p>
            </div>

            <div>
              <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">Status</p>
              <Badge
                variant={
                  selectedEvent.extendedProps.status === 'confirmed'
                    ? 'success'
                    : selectedEvent.extendedProps.status === 'cancelled'
                      ? 'danger'
                      : 'default'
                }
              >
                {selectedEvent.extendedProps.status}
              </Badge>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
