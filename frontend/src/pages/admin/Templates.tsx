import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import FullCalendar from '@fullcalendar/react'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import csLocale from '@fullcalendar/core/locales/cs'
import { Plus, Edit2, Trash2, Save } from 'lucide-react'
import { Card, Modal, Button, Spinner, Input } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { adminApi, calendarApi } from '@/services/api'
import type { EventClickArg } from '@fullcalendar/core'
import type { SlotTemplate, TemplateSlot } from '@/types/api'

interface DateClickArg {
  date: Date
  dateStr: string
  allDay: boolean
}

interface EventDropArg {
  event: {
    id: string
    start: Date | null
  }
  revert: () => void
}

const DAYS_OF_WEEK_KEYS = [
  { value: 1, key: 'monday' },
  { value: 2, key: 'tuesday' },
  { value: 3, key: 'wednesday' },
  { value: 4, key: 'thursday' },
  { value: 5, key: 'friday' },
]

export default function AdminTemplates() {
  const { t, i18n } = useTranslation()
  const { showToast } = useToast()
  const queryClient = useQueryClient()

  // Template list state
  const [selectedTemplate, setSelectedTemplate] = useState<SlotTemplate | null>(null)
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)

  // Template editor state
  const [isEditing, setIsEditing] = useState(false)
  const [editingTemplate, setEditingTemplate] = useState<SlotTemplate | null>(null)
  const [templateName, setTemplateName] = useState('')
  const [templateSlots, setTemplateSlots] = useState<TemplateSlot[]>([])

  // Slot creation modal
  const [showSlotModal, setShowSlotModal] = useState(false)
  const [slotDay, setSlotDay] = useState(1)
  const [slotTime, setSlotTime] = useState('09:00')
  const [slotDuration, setSlotDuration] = useState(60)
  const [editingSlotIndex, setEditingSlotIndex] = useState<number | null>(null)

  const { data: templates, isLoading } = useQuery({
    queryKey: ['admin', 'templates'],
    queryFn: () => adminApi.getTemplates(),
  })

  const { data: calendarSettings } = useQuery({
    queryKey: ['calendarSettings'],
    queryFn: calendarApi.getSettings,
  })

  const createMutation = useMutation({
    mutationFn: adminApi.createTemplate,
    onSuccess: () => {
      showToast('success', t('admin.templates.templateCreated'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'templates'] })
      resetEditor()
    },
    onError: () => {
      showToast('error', t('admin.templates.templateCreateError'))
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, params }: { id: string; params: { name?: string; slots?: TemplateSlot[]; isActive?: boolean } }) =>
      adminApi.updateTemplate(id, params),
    onSuccess: () => {
      showToast('success', t('admin.templates.templateUpdated'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'templates'] })
      resetEditor()
    },
    onError: () => {
      showToast('error', t('admin.templates.templateUpdateError'))
    },
  })

  const deleteMutation = useMutation({
    mutationFn: adminApi.deleteTemplate,
    onSuccess: () => {
      showToast('success', t('admin.templates.templateDeleted'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'templates'] })
      setSelectedTemplate(null)
      setShowDeleteConfirm(false)
    },
    onError: () => {
      showToast('error', t('admin.templates.templateDeleteError'))
    },
  })

  const resetEditor = () => {
    setIsEditing(false)
    setEditingTemplate(null)
    setTemplateName('')
    setTemplateSlots([])
  }

  const startNewTemplate = () => {
    setIsEditing(true)
    setEditingTemplate(null)
    setTemplateName('')
    setTemplateSlots([])
  }

  const startEditTemplate = (template: SlotTemplate) => {
    setIsEditing(true)
    setEditingTemplate(template)
    setTemplateName(template.name)
    setTemplateSlots([...template.slots])
  }

  const handleSaveTemplate = () => {
    if (!templateName.trim()) {
      showToast('error', t('admin.templates.enterTemplateName'))
      return
    }
    if (templateSlots.length === 0) {
      showToast('error', t('admin.templates.addAtLeastOneSlot'))
      return
    }

    if (editingTemplate) {
      updateMutation.mutate({
        id: editingTemplate.id,
        params: {
          name: templateName,
          slots: templateSlots,
        },
      })
    } else {
      createMutation.mutate({
        name: templateName,
        slots: templateSlots,
      })
    }
  }

  const handleDateClick = (info: DateClickArg) => {
    const dateStr = info.dateStr
    // Parse date parts directly to avoid timezone issues
    const datePart = dateStr.split('T')[0]
    const [year, month, day] = datePart.split('-').map(Number)

    // Create date in local timezone
    const date = new Date(year, month - 1, day)
    const dayOfWeekRaw = date.getDay()
    // Convert Sunday (0) to 7, keep Monday-Saturday as is
    const dayOfWeek = dayOfWeekRaw === 0 ? 7 : dayOfWeekRaw

    // Only allow Mon-Fri (1-5)
    if (dayOfWeek > 5) return

    const time = dateStr.includes('T') ? dateStr.split('T')[1].substring(0, 5) : '09:00'

    setSlotDay(dayOfWeek)
    setSlotTime(time)
    setSlotDuration(60)
    setEditingSlotIndex(null)
    setShowSlotModal(true)
  }

  const handleEventClick = (info: EventClickArg) => {
    const slotIndex = parseInt(info.event.id)
    const slot = templateSlots[slotIndex]
    if (slot) {
      setSlotDay(slot.dayOfWeek)
      setSlotTime(slot.startTime)
      setSlotDuration(slot.durationMinutes)
      setEditingSlotIndex(slotIndex)
      setShowSlotModal(true)
    }
  }

  const handleAddSlot = () => {
    const endTime = calculateEndTime(slotTime, slotDuration)

    const newSlot: TemplateSlot = {
      dayOfWeek: slotDay,
      startTime: slotTime,
      endTime: endTime,
      durationMinutes: slotDuration,
    }

    if (editingSlotIndex !== null) {
      const updated = [...templateSlots]
      updated[editingSlotIndex] = newSlot
      setTemplateSlots(updated)
    } else {
      setTemplateSlots([...templateSlots, newSlot])
    }

    setShowSlotModal(false)
  }

  const handleDeleteSlot = () => {
    if (editingSlotIndex !== null) {
      const updated = templateSlots.filter((_, i) => i !== editingSlotIndex)
      setTemplateSlots(updated)
      setShowSlotModal(false)
    }
  }

  const handleEventDrop = (info: EventDropArg) => {
    const slotIndex = parseInt(info.event.id)
    const slot = templateSlots[slotIndex]
    if (!slot || !info.event.start) {
      info.revert()
      return
    }

    const newDate = info.event.start
    const newDayOfWeek = newDate.getDay() === 0 ? 7 : newDate.getDay()

    // Only allow Mon-Fri
    if (newDayOfWeek > 5) {
      info.revert()
      return
    }

    const hours = newDate.getHours().toString().padStart(2, '0')
    const minutes = newDate.getMinutes().toString().padStart(2, '0')
    const newStartTime = `${hours}:${minutes}`
    const newEndTime = calculateEndTime(newStartTime, slot.durationMinutes)

    const updated = [...templateSlots]
    updated[slotIndex] = {
      ...slot,
      dayOfWeek: newDayOfWeek,
      startTime: newStartTime,
      endTime: newEndTime,
    }
    setTemplateSlots(updated)
  }

  const calculateEndTime = (startTime: string, durationMinutes: number): string => {
    const [hours, minutes] = startTime.split(':').map(Number)
    const totalMinutes = hours * 60 + minutes + durationMinutes
    const endHours = Math.floor(totalMinutes / 60) % 24
    const endMinutes = totalMinutes % 60
    return `${endHours.toString().padStart(2, '0')}:${endMinutes.toString().padStart(2, '0')}`
  }

  // Generate calendar events from template slots
  // Use a fixed week (Jan 1, 2024 was a Monday)
  const calendarEvents = templateSlots.map((slot, index) => {
    // Map dayOfWeek (1=Mon, 5=Fri) to date in the week of Jan 1, 2024
    // Jan 1, 2024 = Monday, Jan 2 = Tuesday, etc.
    const dayOffset = slot.dayOfWeek - 1
    const day = 1 + dayOffset
    const dateStr = `2024-01-${day.toString().padStart(2, '0')}`

    return {
      id: index.toString(),
      title: `${slot.startTime} - ${slot.endTime}`,
      start: `${dateStr}T${slot.startTime}`,
      end: `${dateStr}T${slot.endTime}`,
      backgroundColor: '#dcfce7',
      borderColor: '#22c55e',
      textColor: '#166534',
    }
  })

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner size="lg" />
      </div>
    )
  }

  // Template editor view
  if (isEditing) {
    return (
      <div className="space-y-6 animate-fade-in">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
            {editingTemplate ? t('admin.templates.editTemplate') : t('admin.templates.newTemplate')}
          </h1>
          <div className="flex gap-2">
            <Button variant="secondary" onClick={resetEditor}>
              {t('common.cancel')}
            </Button>
            <Button
              onClick={handleSaveTemplate}
              isLoading={createMutation.isPending || updateMutation.isPending}
            >
              <Save size={16} className="mr-1" />
              {t('common.save')}
            </Button>
          </div>
        </div>

        <div className="max-w-md">
          <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
            {t('admin.templates.templateName')}
          </label>
          <Input
            value={templateName}
            onChange={(e) => setTemplateName(e.target.value)}
            placeholder={t('admin.templates.templateNamePlaceholder')}
          />
        </div>

        <p className="text-sm text-neutral-600 dark:text-neutral-400">
          {t('admin.templates.clickToAddSlot')}
        </p>

        <Card variant="bordered" padding="none" className="overflow-hidden">
          <div className="p-4 [&_.fc-timegrid-slot]:!h-4 md:[&_.fc-timegrid-slot]:!h-6">
            <FullCalendar
              plugins={[timeGridPlugin, interactionPlugin]}
              initialView="timeGridWeek"
              initialDate="2024-01-01"
              locale={i18n.language === 'cs' ? csLocale : undefined}
              headerToolbar={false}
              events={calendarEvents}
              dateClick={handleDateClick}
              eventClick={handleEventClick}
              eventDrop={handleEventDrop}
              editable={true}
              slotMinTime={`${(calendarSettings?.calendarStartHour ?? 6).toString().padStart(2, '0')}:00:00`}
              slotMaxTime={`${(calendarSettings?.calendarEndHour ?? 22).toString().padStart(2, '0')}:00:00`}
              allDaySlot={false}
              weekends={false}
              dayHeaderFormat={{ weekday: 'long' }}
              nowIndicator={false}
              eventDisplay="block"
              height="auto"
              slotDuration="00:15:00"
              snapDuration="00:15:00"
              selectable={true}
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
        </Card>

        {/* Slot modal */}
        <Modal
          isOpen={showSlotModal}
          onClose={() => setShowSlotModal(false)}
          title={editingSlotIndex !== null ? t('admin.templates.editSlot') : t('admin.templates.addSlot')}
          size="sm"
        >
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
                {t('admin.templates.day')}
              </label>
              <select
                value={slotDay}
                onChange={(e) => setSlotDay(Number(e.target.value))}
                className="w-full px-3 py-2 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white"
              >
                {DAYS_OF_WEEK_KEYS.map((day) => (
                  <option key={day.value} value={day.value}>
                    {t(`days.${day.key}`)}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
                {t('admin.templates.time')}
              </label>
              <Input
                type="time"
                value={slotTime}
                onChange={(e) => setSlotTime(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
                {t('admin.templates.duration')}
              </label>
              <select
                value={slotDuration}
                onChange={(e) => setSlotDuration(Number(e.target.value))}
                className="w-full px-3 py-2 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white"
              >
                <option value={30}>30 {t('admin.templates.minutes')}</option>
                <option value={45}>45 {t('admin.templates.minutes')}</option>
                <option value={60}>60 {t('admin.templates.minutes')}</option>
                <option value={90}>90 {t('admin.templates.minutes')}</option>
                <option value={120}>120 {t('admin.templates.minutes')}</option>
              </select>
            </div>
            <div className="flex gap-2">
              {editingSlotIndex !== null && (
                <Button
                  variant="danger"
                  className="flex-1"
                  onClick={handleDeleteSlot}
                >
                  <Trash2 size={16} className="mr-1" />
                  {t('common.delete')}
                </Button>
              )}
              <Button className="flex-1" onClick={handleAddSlot}>
                {editingSlotIndex !== null ? t('common.save') : t('admin.templates.addSlot')}
              </Button>
            </div>
          </div>
        </Modal>
      </div>
    )
  }

  // Template list view
  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {t('admin.templates.title')}
        </h1>
        <Button onClick={startNewTemplate}>
          <Plus size={16} className="mr-1" />
          {t('admin.templates.newTemplate')}
        </Button>
      </div>

      {templates && templates.length > 0 ? (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {templates.map((template) => (
            <Card
              key={template.id}
              variant="bordered"
              className="p-4 cursor-pointer hover:shadow-md transition-shadow"
              onClick={() => setSelectedTemplate(template)}
            >
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-medium text-neutral-900 dark:text-white">
                    {template.name}
                  </h3>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400 mt-1">
                    {template.slots.length} {t('admin.templates.slots')}
                  </p>
                  <p className="text-xs text-neutral-400 mt-1">
                    {template.isActive ? t('admin.templates.active') : t('admin.templates.inactive')}
                  </p>
                </div>
                <div className="flex gap-1">
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      startEditTemplate(template)
                    }}
                    className="p-2 hover:bg-neutral-100 dark:hover:bg-neutral-700 rounded"
                  >
                    <Edit2 size={16} className="text-neutral-500" />
                  </button>
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      setSelectedTemplate(template)
                      setShowDeleteConfirm(true)
                    }}
                    className="p-2 hover:bg-neutral-100 dark:hover:bg-neutral-700 rounded"
                  >
                    <Trash2 size={16} className="text-red-500" />
                  </button>
                </div>
              </div>

              {/* Slot summary */}
              <div className="mt-3 flex flex-wrap gap-1">
                {DAYS_OF_WEEK_KEYS.map((day) => {
                  const count = template.slots.filter(s => s.dayOfWeek === day.value).length
                  if (count === 0) return null
                  return (
                    <span
                      key={day.value}
                      className="px-2 py-0.5 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 text-xs rounded"
                    >
                      {t(`days.${day.key}`).substring(0, 2)}: {count}
                    </span>
                  )
                })}
              </div>
            </Card>
          ))}
        </div>
      ) : (
        <Card variant="bordered" className="p-8 text-center">
          <p className="text-neutral-500 dark:text-neutral-400">
            {t('admin.templates.noTemplates')}
          </p>
          <Button className="mt-4" onClick={startNewTemplate}>
            <Plus size={16} className="mr-1" />
            {t('admin.templates.createTemplate')}
          </Button>
        </Card>
      )}

      {/* Template detail modal */}
      <Modal
        isOpen={!!selectedTemplate && !showDeleteConfirm}
        onClose={() => setSelectedTemplate(null)}
        title={selectedTemplate?.name || ''}
        size="md"
      >
        {selectedTemplate && (
          <div className="space-y-4">
            <div className="grid gap-2">
              {DAYS_OF_WEEK_KEYS.map((day) => {
                const daySlots = selectedTemplate.slots
                  .filter(s => s.dayOfWeek === day.value)
                  .sort((a, b) => a.startTime.localeCompare(b.startTime))

                if (daySlots.length === 0) return null

                return (
                  <div key={day.value}>
                    <p className="font-medium text-neutral-900 dark:text-white mb-1">
                      {t(`days.${day.key}`)}
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {daySlots.map((slot, i) => (
                        <span
                          key={i}
                          className="px-2 py-1 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 text-sm rounded"
                        >
                          {slot.startTime} - {slot.endTime}
                        </span>
                      ))}
                    </div>
                  </div>
                )
              })}
            </div>

            <div className="flex gap-2 pt-2">
              <Button
                variant="secondary"
                className="flex-1"
                onClick={() => {
                  setSelectedTemplate(null)
                  startEditTemplate(selectedTemplate)
                }}
              >
                <Edit2 size={16} className="mr-1" />
                {t('common.edit')}
              </Button>
              <Button
                variant="danger"
                className="flex-1"
                onClick={() => setShowDeleteConfirm(true)}
              >
                <Trash2 size={16} className="mr-1" />
                {t('common.delete')}
              </Button>
            </div>
          </div>
        )}
      </Modal>

      {/* Delete confirmation */}
      {showDeleteConfirm && selectedTemplate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="bg-white dark:bg-dark-surface rounded-lg p-6 max-w-sm mx-4 shadow-xl">
            <h3 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
              {t('admin.templates.deleteTemplate')}
            </h3>
            <p className="text-neutral-600 dark:text-neutral-300 mb-4">
              {t('admin.templates.deleteTemplateConfirm')} <strong>{selectedTemplate.name}</strong>?
              {' '}{t('admin.templates.deleteTemplateWarning')}
            </p>
            <div className="flex gap-3">
              <Button
                variant="secondary"
                className="flex-1"
                onClick={() => {
                  setShowDeleteConfirm(false)
                  setSelectedTemplate(null)
                }}
              >
                {t('common.cancel')}
              </Button>
              <Button
                variant="danger"
                className="flex-1"
                onClick={() => deleteMutation.mutate(selectedTemplate.id)}
                isLoading={deleteMutation.isPending}
              >
                {t('common.delete')}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
