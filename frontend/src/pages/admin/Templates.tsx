import { useState, useRef, useCallback } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Plus, Edit2, Trash2, Save } from 'lucide-react'
import { Card, Modal, Button, Spinner, Input } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { adminApi, calendarApi, locationsApi } from '@/services/api'
import type { SlotTemplate, TemplateSlot } from '@/types/api'
import { darken, hexWithAlpha, isValidHex } from '@/utils/color'
import { useThemeStore } from '@/stores/themeStore'

const NEUTRAL_GRAY = '#9CA3AF'

const DAYS_OF_WEEK_KEYS = [
  { value: 1, key: 'monday' },
  { value: 2, key: 'tuesday' },
  { value: 3, key: 'wednesday' },
  { value: 4, key: 'thursday' },
  { value: 5, key: 'friday' },
]

const TEMPLATE_HOUR_HEIGHT = 48
const TEMPLATE_TIME_COL = 48

interface TemplateGridSlot {
  index: number
  slot: TemplateSlot
  top: number
  height: number
}

export default function AdminTemplates() {
  const { t } = useTranslation()
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
  const [templateLocationId, setTemplateLocationId] = useState<string | null>(null)

  // Slot creation modal
  const [showSlotModal, setShowSlotModal] = useState(false)
  const [slotDay, setSlotDay] = useState(1)
  const [slotTime, setSlotTime] = useState('09:00')
  const [slotDuration, setSlotDuration] = useState(60)
  const [slotPricingItemIds, setSlotPricingItemIds] = useState<string[]>([])
  const [slotLocationId, setSlotLocationId] = useState<string | null>(null)
  const [editingSlotIndex, setEditingSlotIndex] = useState<number | null>(null)

  // Drag state
  const dragRef = useRef<{ index: number; startX: number; startY: number } | null>(null)
  const gridRef = useRef<HTMLDivElement>(null)

  const { data: templates, isLoading } = useQuery({
    queryKey: ['admin', 'templates'],
    queryFn: () => adminApi.getTemplates(),
  })

  const { data: locations } = useQuery({
    queryKey: ['admin', 'locations'],
    queryFn: locationsApi.listAdmin,
  })
  const activeLocations = locations?.filter((l) => l.isActive) || []

  const { data: pricingItems } = useQuery({
    queryKey: ['adminPricingItems'],
    queryFn: adminApi.getAllPricing,
  })
  const activePricingItems = pricingItems?.filter((p) => p.isActive) || []

  // Resolve color for a template slot — falls back to the template-level
  // location when the slot doesn't override, or to neutral gray otherwise.
  const resolvedTheme = useThemeStore((s) => s.resolvedTheme)
  const neutralText = resolvedTheme === 'dark' ? '#F9FAFB' : '#111827'
  const locationById = new Map((locations ?? []).map((l) => [l.id, l]))
  const resolveSlotColor = (slot: TemplateSlot): string => {
    const perSlot = slot.locationId ? locationById.get(slot.locationId)?.color : null
    const fromTemplate = templateLocationId
      ? locationById.get(templateLocationId)?.color ?? null
      : null
    const color = perSlot ?? fromTemplate
    return isValidHex(color) ? color : NEUTRAL_GRAY
  }

  const { data: calendarSettings } = useQuery({
    queryKey: ['calendarSettings'],
    queryFn: calendarApi.getSettings,
  })

  const startHour = calendarSettings?.calendarStartHour ?? 6
  const endHour = calendarSettings?.calendarEndHour ?? 22
  const totalHours = endHour - startHour
  const totalHeight = totalHours * TEMPLATE_HOUR_HEIGHT

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
    mutationFn: ({ id, params }: { id: string; params: { name?: string; slots?: TemplateSlot[]; isActive?: boolean; locationId?: string | null; clearLocation?: boolean } }) =>
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
    setTemplateLocationId(null)
  }

  const startNewTemplate = () => {
    setIsEditing(true)
    setEditingTemplate(null)
    setTemplateName('')
    setTemplateSlots([])
    setTemplateLocationId(null)
  }

  const startEditTemplate = (template: SlotTemplate) => {
    setIsEditing(true)
    setEditingTemplate(template)
    setTemplateName(template.name)
    setTemplateSlots([...template.slots])
    setTemplateLocationId(template.locationId ?? null)
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
      const params: { name: string; slots: TemplateSlot[]; locationId?: string | null; clearLocation?: boolean } = {
        name: templateName,
        slots: templateSlots,
      }
      if (templateLocationId) params.locationId = templateLocationId
      else params.clearLocation = true
      updateMutation.mutate({ id: editingTemplate.id, params })
    } else {
      createMutation.mutate({
        name: templateName,
        slots: templateSlots,
        locationId: templateLocationId,
      })
    }
  }

  const calculateEndTime = (st: string, dur: number): string => {
    const [h, m] = st.split(':').map(Number)
    const total = h * 60 + m + dur
    return `${Math.floor(total / 60).toString().padStart(2, '0')}:${(total % 60).toString().padStart(2, '0')}`
  }

  // Click on empty grid area
  const handleGridClick = useCallback((dayOfWeek: number, hour: number, minutes: number) => {
    setSlotDay(dayOfWeek)
    setSlotTime(`${hour.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`)
    setSlotDuration(60)
    setSlotPricingItemIds([])
    setSlotLocationId(null)
    setEditingSlotIndex(null)
    setShowSlotModal(true)
  }, [])

  // Click on existing slot
  const handleSlotClick = useCallback((index: number) => {
    const slot = templateSlots[index]
    if (!slot) return
    setSlotDay(slot.dayOfWeek)
    setSlotTime(slot.startTime)
    setSlotDuration(slot.durationMinutes)
    setSlotPricingItemIds(slot.pricingItemIds ?? [])
    setSlotLocationId(slot.locationId ?? null)
    setEditingSlotIndex(index)
    setShowSlotModal(true)
  }, [templateSlots])

  const handleAddSlot = () => {
    const endTime = calculateEndTime(slotTime, slotDuration)
    const newSlot: TemplateSlot = {
      dayOfWeek: slotDay,
      startTime: slotTime,
      endTime,
      durationMinutes: slotDuration,
      pricingItemIds: slotPricingItemIds,
      locationId: slotLocationId,
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
      setTemplateSlots(templateSlots.filter((_, i) => i !== editingSlotIndex))
      setShowSlotModal(false)
    }
  }

  // Drag-drop for template slots
  const handlePointerDown = useCallback((e: React.PointerEvent, index: number) => {
    e.preventDefault()
    e.stopPropagation()
    ;(e.target as HTMLElement).setPointerCapture(e.pointerId)
    dragRef.current = { index, startX: e.clientX, startY: e.clientY }
  }, [])

  const handlePointerUp = useCallback((e: React.PointerEvent) => {
    if (!dragRef.current || !gridRef.current) {
      dragRef.current = null
      return
    }

    const dx = Math.abs(e.clientX - dragRef.current.startX)
    const dy = Math.abs(e.clientY - dragRef.current.startY)

    if (dx + dy < 5) {
      // It was a click, not a drag
      dragRef.current = null
      return
    }

    const rect = gridRef.current.getBoundingClientRect()
    const relX = e.clientX - rect.left - TEMPLATE_TIME_COL
    const relY = e.clientY - rect.top + gridRef.current.scrollTop

    const colWidth = (rect.width - TEMPLATE_TIME_COL) / 5
    const dayIndex = Math.max(0, Math.min(4, Math.floor(relX / colWidth)))
    const newDayOfWeek = dayIndex + 1

    const totalMinutes = (relY / TEMPLATE_HOUR_HEIGHT) * 60 + startHour * 60
    const snapped = Math.round(totalMinutes / 15) * 15
    const hours = Math.floor(snapped / 60)
    const minutes = snapped % 60
    const newStartTime = `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`

    const slot = templateSlots[dragRef.current.index]
    if (slot) {
      const updated = [...templateSlots]
      updated[dragRef.current.index] = {
        ...slot,
        dayOfWeek: newDayOfWeek,
        startTime: newStartTime,
        endTime: calculateEndTime(newStartTime, slot.durationMinutes),
      }
      setTemplateSlots(updated)
    }

    dragRef.current = null
  }, [templateSlots, startHour])

  // Position template slots for rendering
  const getPositionedSlots = (dayOfWeek: number): TemplateGridSlot[] => {
    return templateSlots
      .map((slot, index) => ({ slot, index }))
      .filter(({ slot }) => slot.dayOfWeek === dayOfWeek)
      .map(({ slot, index }) => {
        const [sh, sm] = slot.startTime.split(':').map(Number)
        const [eh, em] = slot.endTime.split(':').map(Number)
        const startMin = (sh - startHour) * 60 + sm
        const endMin = (eh - startHour) * 60 + em
        return {
          index,
          slot,
          top: (startMin / 60) * TEMPLATE_HOUR_HEIGHT + 2,
          height: Math.max(((endMin - startMin) / 60) * TEMPLATE_HOUR_HEIGHT - 4, 16),
        }
      })
  }

  const timeLabels: number[] = []
  for (let h = startHour; h < endHour; h++) timeLabels.push(h)

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

        <div className="grid gap-4 sm:grid-cols-2 max-w-2xl">
          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
              {t('admin.templates.templateName')}
            </label>
            <Input
              value={templateName}
              onChange={(e) => setTemplateName(e.target.value)}
              placeholder={t('admin.templates.templateNamePlaceholder')}
            />
          </div>
          {activeLocations.length > 0 && (
            <div>
              <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
                {t('admin.locations.label')}
              </label>
              <select
                value={templateLocationId ?? ''}
                onChange={(e) => setTemplateLocationId(e.target.value === '' ? null : e.target.value)}
                className="w-full px-3 py-3 border border-neutral-200 dark:border-dark-border rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white min-h-[48px]"
              >
                <option value="">{t('admin.locations.selectPlaceholder')}</option>
                {activeLocations.map((loc) => (
                  <option key={loc.id} value={loc.id}>
                    {loc.nameCs}
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>

        <p className="text-sm text-neutral-600 dark:text-neutral-400">
          {t('admin.templates.clickToAddSlot')}
        </p>

        <Card variant="bordered" padding="none" className="overflow-hidden">
          {/* Custom template grid */}
          <div className="flex">
            {/* Time column */}
            <div className="flex-shrink-0 border-r border-neutral-200 dark:border-neutral-700" style={{ width: TEMPLATE_TIME_COL }}>
              {/* Header spacer */}
              <div className="h-9 border-b border-neutral-200 dark:border-neutral-700" />
              {timeLabels.map(hour => (
                <div
                  key={hour}
                  className="text-[10px] text-neutral-500 dark:text-neutral-400 text-right pr-2 border-b border-neutral-100 dark:border-neutral-800 pt-0.5"
                  style={{ height: TEMPLATE_HOUR_HEIGHT }}
                >
                  {hour}:00
                </div>
              ))}
            </div>

            {/* Day columns */}
            <div
              ref={gridRef}
              className="flex-1 flex flex-col overflow-y-auto"
              style={{ maxHeight: 600 }}
              onPointerUp={handlePointerUp}
            >
              {/* Day headers */}
              <div className="flex border-b border-neutral-200 dark:border-neutral-700 sticky top-0 bg-white dark:bg-dark-surface z-10">
                {DAYS_OF_WEEK_KEYS.map((day) => (
                  <div
                    key={day.value}
                    className="flex-1 py-2 text-center text-xs font-medium text-neutral-600 dark:text-neutral-400 border-r border-neutral-200 dark:border-neutral-700 last:border-r-0"
                  >
                    {t(`days.${day.key}`)}
                  </div>
                ))}
              </div>

              {/* Grid body */}
              <div className="flex flex-1">
                {DAYS_OF_WEEK_KEYS.map((day) => {
                  const positioned = getPositionedSlots(day.value)
                  return (
                    <div
                      key={day.value}
                      className="flex-1 relative border-r border-neutral-200 dark:border-neutral-700 last:border-r-0"
                      style={{ height: totalHeight }}
                    >
                      {/* Hour grid lines */}
                      {timeLabels.map((hour, idx) => (
                        <div
                          key={hour}
                          className="absolute w-full border-b border-neutral-100 dark:border-neutral-800 cursor-pointer hover:bg-neutral-50/50 dark:hover:bg-neutral-800/30"
                          style={{ top: idx * TEMPLATE_HOUR_HEIGHT, height: TEMPLATE_HOUR_HEIGHT }}
                          onClick={() => handleGridClick(day.value, hour, 0)}
                        >
                          <div
                            className="absolute w-full border-b border-neutral-50 dark:border-neutral-800/50"
                            style={{ top: TEMPLATE_HOUR_HEIGHT / 4 }}
                            onClick={(e) => { e.stopPropagation(); handleGridClick(day.value, hour, 15) }}
                          />
                          <div
                            className="absolute w-full border-b border-neutral-100 dark:border-neutral-800"
                            style={{ top: TEMPLATE_HOUR_HEIGHT / 2 }}
                            onClick={(e) => { e.stopPropagation(); handleGridClick(day.value, hour, 30) }}
                          />
                          <div
                            className="absolute w-full border-b border-neutral-50 dark:border-neutral-800/50"
                            style={{ top: (TEMPLATE_HOUR_HEIGHT / 4) * 3 }}
                            onClick={(e) => { e.stopPropagation(); handleGridClick(day.value, hour, 45) }}
                          />
                        </div>
                      ))}

                      {/* Slots — styled identically to the calendar's
                          unlocked state: location color at 20% alpha + full-
                          color left stripe + neutral readable text. */}
                      {positioned.map(({ index, slot, top, height }) => {
                        const base = resolveSlotColor(slot)
                        const locName = slot.locationId
                          ? locationById.get(slot.locationId)?.nameCs
                          : (templateLocationId
                              ? locationById.get(templateLocationId)?.nameCs
                              : null)
                        return (
                          <div
                            key={index}
                            className="absolute left-1 right-1 rounded overflow-hidden cursor-grab active:cursor-grabbing select-none z-10"
                            style={{
                              top,
                              height,
                              backgroundColor: hexWithAlpha(base, 0.2),
                              borderLeft: `3px solid ${base}`,
                              color: neutralText,
                            }}
                            onClick={(e) => { e.stopPropagation(); handleSlotClick(index) }}
                            onPointerDown={(e) => handlePointerDown(e, index)}
                          >
                            <div className="p-1 text-xs font-medium truncate">
                              {slot.startTime} - {slot.endTime}
                            </div>
                            {locName && (
                              <div
                                className="px-1 text-[10px] truncate"
                                style={{ color: darken(base, 0.35) }}
                              >
                                {locName}
                              </div>
                            )}
                          </div>
                        )
                      })}
                    </div>
                  )
                })}
              </div>
            </div>
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
            {activeLocations.length > 0 && (
              <div>
                <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
                  {t('admin.locations.label')}
                </label>
                <select
                  value={slotLocationId ?? ''}
                  onChange={(e) => setSlotLocationId(e.target.value === '' ? null : e.target.value)}
                  className="w-full px-3 py-2 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white"
                >
                  <option value="">{t('admin.locations.selectPlaceholder')}</option>
                  {activeLocations.map((loc) => (
                    <option key={loc.id} value={loc.id}>{loc.nameCs}</option>
                  ))}
                </select>
              </div>
            )}
            {activePricingItems.length > 0 && (
              <div>
                <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
                  {t('calendar.selectTrainingType')}
                </label>
                <div className="grid gap-2 max-h-44 overflow-y-auto p-1">
                  {activePricingItems.map((item) => {
                    const isSelected = slotPricingItemIds.includes(item.id)
                    const toggle = () =>
                      setSlotPricingItemIds((prev) =>
                        prev.includes(item.id) ? prev.filter((x) => x !== item.id) : [...prev, item.id]
                      )
                    return (
                      <button
                        key={item.id}
                        type="button"
                        onClick={toggle}
                        className={`flex items-center justify-between px-3 py-2 rounded-xl border-2 transition-all text-left ${
                          isSelected
                            ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/30'
                            : 'border-neutral-200 dark:border-neutral-700 bg-white dark:bg-dark-surface hover:border-neutral-300'
                        }`}
                      >
                        <span className={`text-sm truncate ${isSelected ? 'font-medium text-primary-700 dark:text-primary-300' : 'text-neutral-700 dark:text-neutral-300'}`}>
                          {item.nameCs}
                        </span>
                        <span className={`text-xs font-medium ml-2 flex-shrink-0 px-2 py-0.5 rounded-full ${
                          isSelected
                            ? 'bg-primary-100 dark:bg-primary-800/50 text-primary-700 dark:text-primary-300'
                            : 'bg-neutral-100 dark:bg-neutral-700 text-neutral-500 dark:text-neutral-400'
                        }`}>
                          {item.credits} kr.
                        </span>
                      </button>
                    )
                  })}
                </div>
              </div>
            )}
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
      <Modal
        isOpen={showDeleteConfirm && !!selectedTemplate}
        onClose={() => {
          setShowDeleteConfirm(false)
          setSelectedTemplate(null)
        }}
        title={t('admin.templates.deleteTemplate')}
        size="sm"
      >
        {selectedTemplate && (
          <div className="space-y-4">
            <p className="text-neutral-600 dark:text-neutral-300">
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
        )}
      </Modal>
    </div>
  )
}
