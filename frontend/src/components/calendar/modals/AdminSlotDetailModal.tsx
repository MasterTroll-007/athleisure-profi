import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useQuery } from '@tanstack/react-query'
import { AlertTriangle, CheckCircle, Dumbbell, Lock, MapPin, Pencil, Unlock, UserMinus, UserPlus, X } from 'lucide-react'
import { Modal, Button, Badge, Input } from '@/components/ui'
import { WorkoutLogModal } from './WorkoutLogModal'
import { WorkoutExerciseSummaryTable } from '@/components/workouts/WorkoutExerciseSummaryTable'
import { adminApi, locationsApi } from '@/services/api'
import { formatCredits } from '@/utils/formatters'
import type { Slot, User, PricingItem } from '@/types/api'

interface AdminSlotDetailModalProps {
  isOpen: boolean
  slot: Slot | null
  // Note editing
  noteText: string
  isEditingNote: boolean
  onNoteChange: (note: string) => void
  onStartEditNote: () => void
  onCancelEditNote: () => void
  onSaveNote: () => void
  isNoteSaving: boolean
  // User selection
  selectedUser: User | null
  deductCredits: boolean
  onDeductCreditsChange: (value: boolean) => void
  onClearUser: () => void
  onOpenUserSearch: () => void
  // Actions
  onUnlockSlot: () => void
  onLockSlot: () => void
  onDeleteSlot: () => void
  onCreateReservation: () => void
  onOpenCancelConfirm: (withRefund: boolean) => void
  // Slot in-place edit
  isEditingSlot: boolean
  editDate: string
  editTime: string
  editDuration: number
  editLocationId: string | null
  editPricingItemIds: string[]
  pricingItems?: PricingItem[]
  onStartEditSlot: () => void
  onCancelEditSlot: () => void
  onSaveSlotEdit: () => void
  isRescheduling: boolean
  rescheduleDate: string
  rescheduleTime: string
  rescheduleDuration: number
  onCancelReschedule: () => void
  onSaveReschedule: (date: string, time: string, duration: number) => void
  onRescheduleDateChange: (date: string) => void
  onRescheduleTimeChange: (time: string) => void
  onRescheduleDurationChange: (duration: number) => void
  onMarkAttendance: (status: 'completed' | 'no_show') => void
  onEditDateChange: (date: string) => void
  onEditTimeChange: (time: string) => void
  onEditDurationChange: (duration: number) => void
  onEditLocationIdChange: (id: string | null) => void
  onEditPricingItemIdsChange: (ids: string[]) => void
  // Loading states
  isUpdating: boolean
  isDeleting: boolean
  isCreatingReservation: boolean
  isReschedulingReservation: boolean
  isMarkingAttendance: boolean
  // Cancel confirmation
  showCancelConfirm: boolean
  cancelWithRefund: boolean
  onCloseCancelConfirm: () => void
  onConfirmCancel: () => void
  isCancelling: boolean
  // Modal close
  onClose: () => void
}

export function AdminSlotDetailModal({
  isOpen,
  slot,
  noteText,
  isEditingNote,
  onNoteChange,
  onStartEditNote,
  onCancelEditNote,
  onSaveNote,
  isNoteSaving,
  selectedUser,
  deductCredits,
  onDeductCreditsChange,
  onClearUser,
  onOpenUserSearch,
  onUnlockSlot,
  onLockSlot,
  onDeleteSlot,
  onCreateReservation,
  onOpenCancelConfirm,
  isEditingSlot,
  editDate,
  editTime,
  editDuration,
  editLocationId,
  editPricingItemIds,
  pricingItems,
  onStartEditSlot,
  onCancelEditSlot,
  onSaveSlotEdit,
  isRescheduling,
  rescheduleDate,
  rescheduleTime,
  rescheduleDuration,
  onCancelReschedule,
  onSaveReschedule,
  onRescheduleDateChange,
  onRescheduleTimeChange,
  onRescheduleDurationChange,
  onMarkAttendance,
  onEditDateChange,
  onEditTimeChange,
  onEditDurationChange,
  onEditLocationIdChange,
  onEditPricingItemIdsChange,
  isUpdating,
  isDeleting,
  isCreatingReservation,
  isReschedulingReservation,
  isMarkingAttendance,
  showCancelConfirm,
  cancelWithRefund,
  onCloseCancelConfirm,
  onConfirmCancel,
  isCancelling,
  onClose,
}: AdminSlotDetailModalProps) {
  const { t, i18n } = useTranslation()
  const [isWorkoutLogOpen, setIsWorkoutLogOpen] = useState(false)

  const formatSlotTime = (time: string) => time.substring(0, 5)

  const activePricingItems = pricingItems?.filter((p) => p.isActive) || []
  const { data: locations } = useQuery({
    queryKey: ['admin', 'locations'],
    queryFn: locationsApi.listAdmin,
    enabled: isOpen && isEditingSlot,
  })
  const activeLocations = locations?.filter((l) => l.isActive) || []
  const reservationId = slot?.reservationId ?? null
  const isPastReservation = !!slot && new Date(`${slot.date}T${slot.endTime}`) < new Date()

  const { data: workoutLog } = useQuery({
    queryKey: ['admin', 'reservation', reservationId, 'workout'],
    queryFn: () => adminApi.getWorkoutLog(reservationId!),
    enabled: isOpen && !!reservationId,
  })
  const workoutExerciseCount = workoutLog?.exercises?.filter((exercise) => exercise.name.trim().length > 0).length ?? 0
  const workoutHasContent = workoutExerciseCount > 0 || !!workoutLog?.notes?.trim()

  const togglePricingItem = (id: string) => {
    onEditPricingItemIdsChange(
      editPricingItemIds.includes(id)
        ? editPricingItemIds.filter((x) => x !== id)
        : [...editPricingItemIds, id]
    )
  }

  // Only slots without an active reservation can be edited in-place.
  const canEdit = !!slot && (slot.status === 'locked' || slot.status === 'unlocked')

  return (
    <>
    <Modal isOpen={isOpen} onClose={onClose} title={t('calendar.slotDetail')} size="md">
      {slot && (
        <div className="space-y-4">
          {/* Edit form (only for unlocked/locked slots) */}
          {isEditingSlot && canEdit ? (
            <>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
                    {t('calendar.date')}
                  </label>
                  <Input type="date" value={editDate} onChange={(e) => onEditDateChange(e.target.value)} />
                </div>
                <div>
                  <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
                    {t('calendar.time')}
                  </label>
                  <Input type="time" value={editTime} onChange={(e) => onEditTimeChange(e.target.value)} />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
                  {t('calendar.duration')}
                </label>
                <select
                  value={editDuration}
                  onChange={(e) => onEditDurationChange(Number(e.target.value))}
                  className="w-full px-3 py-2 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white"
                >
                  <option value={30}>30 {t('calendar.minutes')}</option>
                  <option value={45}>45 {t('calendar.minutes')}</option>
                  <option value={60}>60 {t('calendar.minutes')}</option>
                  <option value={90}>90 {t('calendar.minutes')}</option>
                  <option value={120}>120 {t('calendar.minutes')}</option>
                </select>
              </div>
              {activeLocations.length > 0 && (
                <div>
                  <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
                    {t('admin.locations.label')}
                  </label>
                  <select
                    value={editLocationId ?? ''}
                    onChange={(e) => onEditLocationIdChange(e.target.value === '' ? null : e.target.value)}
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
                      const isSelected = editPricingItemIds.includes(item.id)
                      return (
                        <button
                          key={item.id}
                          type="button"
                          onClick={() => togglePricingItem(item.id)}
                          className={`flex items-center justify-between px-3 py-2 rounded-xl border-2 transition-all text-left ${
                            isSelected
                              ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/30'
                              : 'border-neutral-200 dark:border-neutral-700 bg-white dark:bg-dark-surface hover:border-neutral-300'
                          }`}
                        >
                          <span className={`text-sm truncate ${isSelected ? 'font-medium text-primary-700 dark:text-primary-300' : 'text-neutral-700 dark:text-neutral-300'}`}>
                            {i18n.language === 'cs' ? item.nameCs : (item.nameEn || item.nameCs)}
                          </span>
                          <span className={`text-xs font-medium ml-2 flex-shrink-0 px-2 py-0.5 rounded-full ${
                            isSelected
                              ? 'bg-primary-100 dark:bg-primary-800/50 text-primary-700 dark:text-primary-300'
                              : 'bg-neutral-100 dark:bg-neutral-700 text-neutral-500 dark:text-neutral-400'
                          }`}>
                            {formatCredits(item.credits, i18n.language)}
                          </span>
                        </button>
                      )
                    })}
                  </div>
                </div>
              )}
              <div className="flex flex-col-reverse gap-2 pt-2 sm:flex-row">
                <Button variant="secondary" className="flex-1" onClick={onCancelEditSlot}>
                  {t('common.cancel')}
                </Button>
                <Button className="flex-1" onClick={onSaveSlotEdit} isLoading={isUpdating}>
                  {t('common.save')}
                </Button>
              </div>
            </>
          ) : (
            <>
              {/* Date and time */}
              <div>
                <p className="text-sm text-neutral-500 dark:text-neutral-400">{t('calendar.dateAndTime')}</p>
                <p className="font-medium text-neutral-900 dark:text-white">
                  {new Date(slot.date).toLocaleDateString(i18n.language, {
                    weekday: 'long',
                    day: 'numeric',
                    month: 'long',
                    year: 'numeric',
                  })}
                </p>
                <p className="font-mono text-neutral-900 dark:text-white">
                  {formatSlotTime(slot.startTime)} - {formatSlotTime(slot.endTime)}
                </p>
              </div>

              {/* Location */}
              {slot.locationName && (
                <div>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">{t('admin.locations.label')}</p>
                  <div className="flex items-start gap-2">
                    <span
                      className="inline-flex items-center justify-center w-8 h-8 rounded-full flex-shrink-0 mt-0.5"
                      style={{ backgroundColor: slot.locationColor || '#9CA3AF' }}
                    >
                      <MapPin size={16} className="text-white" />
                    </span>
                    <div className="min-w-0">
                      <p className="font-medium text-neutral-900 dark:text-white">{slot.locationName}</p>
                      {slot.locationAddress && (
                        <p className="text-sm text-neutral-600 dark:text-neutral-300 whitespace-pre-line">
                          {slot.locationAddress}
                        </p>
                      )}
                    </div>
                  </div>
                </div>
              )}

              {/* Pricing items summary */}
              {slot.pricingItems && slot.pricingItems.length > 0 && (
                <div>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">
                    {t('calendar.selectTrainingType')}
                  </p>
                  <div className="flex flex-wrap gap-1.5">
                    {slot.pricingItems.map((p) => (
                      <span
                        key={p.id}
                        className="text-xs px-2 py-0.5 rounded-full bg-neutral-100 dark:bg-neutral-700 text-neutral-700 dark:text-neutral-200"
                      >
                        {p.nameCs} · {formatCredits(p.credits, i18n.language)}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* Status */}
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">{t('calendar.status')}</p>
                  <Badge
                    variant={
                      slot.status === 'reserved'
                        ? 'primary'
                        : slot.status === 'cancelled'
                          ? 'danger'
                          : slot.status === 'locked'
                            ? 'default'
                            : 'success'
                    }
                  >
                    {slot.status === 'reserved'
                      ? t('calendar.reserved')
                      : slot.status === 'cancelled'
                        ? t('calendar.cancelled')
                        : slot.status === 'locked'
                          ? t('calendar.locked')
                          : t('calendar.available')}
                  </Badge>
                </div>
                {canEdit && (
                  <Button
                    variant="secondary"
                    size="sm"
                    leftIcon={<Pencil size={14} />}
                    onClick={onStartEditSlot}
                  >
                    {t('common.edit')}
                  </Button>
                )}
              </div>
            </>
          )}

          {/* Reserved slot section */}
          {!isEditingSlot && slot.status === 'reserved' && slot.reservationId && (
            <>
              {isRescheduling ? (
                <div className="space-y-3 p-3 rounded-lg bg-neutral-50 dark:bg-dark-surface border border-neutral-200 dark:border-neutral-700">
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
                        {t('calendar.date')}
                      </label>
                      <Input type="date" value={rescheduleDate} onChange={(e) => onRescheduleDateChange(e.target.value)} />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
                        {t('calendar.time')}
                      </label>
                      <Input type="time" value={rescheduleTime} onChange={(e) => onRescheduleTimeChange(e.target.value)} />
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
                      {t('calendar.duration')}
                    </label>
                    <select
                      value={rescheduleDuration}
                      onChange={(e) => onRescheduleDurationChange(Number(e.target.value))}
                      className="w-full px-3 py-2 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white"
                    >
                      <option value={30}>30 {t('calendar.minutes')}</option>
                      <option value={45}>45 {t('calendar.minutes')}</option>
                      <option value={60}>60 {t('calendar.minutes')}</option>
                      <option value={90}>90 {t('calendar.minutes')}</option>
                      <option value={120}>120 {t('calendar.minutes')}</option>
                    </select>
                  </div>
                  <p className="text-xs text-neutral-500 dark:text-neutral-400">
                    {t('calendar.rescheduleCreatesSlot', 'Pokud v cílovém čase není slot, vytvoří se automaticky podle původního slotu.')}
                  </p>
                  <div className="flex flex-col-reverse gap-2 sm:flex-row">
                    <Button variant="secondary" className="flex-1" onClick={onCancelReschedule}>
                      {t('common.cancel')}
                    </Button>
                    <Button className="flex-1" onClick={() => onSaveReschedule(rescheduleDate, rescheduleTime, rescheduleDuration)} isLoading={isReschedulingReservation}>
                      {t('common.save')}
                    </Button>
                  </div>
                </div>
              ) : null}

              <div className="p-3 rounded-lg bg-blue-50 dark:bg-blue-900/20">
                <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">
                  {t('calendar.reservationInfo')}
                </p>
                <p className="font-medium text-neutral-900 dark:text-white">
                  {slot.assignedUserName || slot.assignedUserEmail || t('calendar.unknown')}
                </p>
                {slot.assignedUserName && slot.assignedUserEmail && (
                  <p className="text-sm text-neutral-600 dark:text-neutral-300">{slot.assignedUserEmail}</p>
                )}
              </div>

              {/* Attendance */}
              {isPastReservation && (
                <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                  <Button
                    variant="secondary"
                    leftIcon={<CheckCircle size={16} />}
                    onClick={() => onMarkAttendance('completed')}
                    isLoading={isMarkingAttendance}
                  >
                    {t('attendance.markCompleted')}
                  </Button>
                  <Button
                    variant="secondary"
                    leftIcon={<AlertTriangle size={16} />}
                    onClick={() => onMarkAttendance('no_show')}
                    isLoading={isMarkingAttendance}
                  >
                    {t('attendance.markNoShow')}
                  </Button>
                </div>
              )}

              {/* Note section */}
              <div>
                <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">{t('calendar.note')}</p>
                {isEditingNote ? (
                  <div className="space-y-2">
                    <textarea
                      value={noteText}
                      onChange={(e) => onNoteChange(e.target.value)}
                      placeholder={t('calendar.writeNote')}
                      className="w-full px-3 py-2 border border-neutral-300 dark:border-neutral-600 rounded-lg bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
                      rows={3}
                    />
                    <div className="flex flex-col-reverse gap-2 sm:flex-row">
                      <Button variant="secondary" size="sm" className="flex-1" onClick={onCancelEditNote}>
                        {t('common.cancel')}
                      </Button>
                      <Button size="sm" className="flex-1" onClick={onSaveNote} isLoading={isNoteSaving}>
                        {t('common.save')}
                      </Button>
                    </div>
                  </div>
                ) : (
                  <div
                    onClick={onStartEditNote}
                    className="p-2 rounded-lg bg-neutral-50 dark:bg-dark-surface border border-neutral-200 dark:border-neutral-700 cursor-pointer hover:bg-neutral-100 dark:hover:bg-dark-surfaceHover min-h-[60px]"
                  >
                    {slot.note ? (
                      <p className="text-sm text-neutral-700 dark:text-neutral-300 whitespace-pre-wrap">
                        {slot.note}
                      </p>
                    ) : (
                      <p className="text-sm text-neutral-400 italic">{t('calendar.clickToAddNote')}</p>
                    )}
                  </div>
                )}
              </div>

              {/* Workout log preview */}
              <div className="space-y-3 rounded-lg border border-white/10 bg-white/[0.04] p-3">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-neutral-900 dark:text-white">
                      {t('workouts.title')}
                    </p>
                    <p className="mt-1 text-xs text-neutral-500 dark:text-neutral-400">
                      {workoutHasContent
                        ? t('workouts.exerciseCount', { count: workoutExerciseCount })
                        : t('workouts.emptyPreview')}
                    </p>
                  </div>
                  <Button
                    variant="secondary"
                    size="sm"
                    leftIcon={<Dumbbell size={14} />}
                    onClick={() => setIsWorkoutLogOpen(true)}
                  >
                    {t('workouts.openLog')}
                  </Button>
                </div>

                {workoutHasContent && (
                  <div className="space-y-2">
                    <WorkoutExerciseSummaryTable exercises={workoutLog?.exercises ?? []} maxRows={3} />
                    {workoutLog?.notes && (
                      <p className="line-clamp-2 text-sm text-neutral-600 dark:text-neutral-300">
                        {workoutLog.notes}
                      </p>
                    )}
                  </div>
                )}
              </div>

              {/* Cancel actions */}
              <div className="pt-2 space-y-2">
                <Button
                  className="w-full"
                  variant="danger"
                  leftIcon={<UserMinus size={18} />}
                  onClick={() => onOpenCancelConfirm(true)}
                >
                  {t('calendar.cancelWithRefund')}
                </Button>
                <Button className="w-full" variant="secondary" onClick={() => onOpenCancelConfirm(false)}>
                  {t('calendar.cancelWithoutRefund')}
                </Button>
              </div>
            </>
          )}

          {/* Cancelled slot section */}
          {!isEditingSlot && slot.status === 'cancelled' && (
            <>
              <div className="p-3 rounded-lg bg-red-50 dark:bg-red-900/20">
                <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-1">
                  {t('calendar.cancelledReservation')}
                </p>
                <p className="font-medium text-neutral-900 dark:text-white">
                  {slot.assignedUserName || slot.assignedUserEmail || t('calendar.unknown')}
                </p>
                {slot.assignedUserName && slot.assignedUserEmail && (
                  <p className="text-sm text-neutral-600 dark:text-neutral-300">{slot.assignedUserEmail}</p>
                )}
                {slot.cancelledAt && (
                  <p className="text-xs text-red-600 dark:text-red-400 mt-1">
                    {t('calendar.cancelledAt')}: {new Date(slot.cancelledAt).toLocaleString(i18n.language)}
                  </p>
                )}
              </div>

              <div className="pt-2 space-y-2">
                <Button
                  className="w-full"
                  variant="primary"
                  leftIcon={<UserPlus size={18} />}
                  onClick={onOpenUserSearch}
                >
                  {t('calendar.registerUser')}
                </Button>
                <Button
                  className="w-full"
                  variant="secondary"
                  leftIcon={<Lock size={18} />}
                  onClick={onLockSlot}
                  isLoading={isUpdating}
                >
                  {t('calendar.lockSlot')}
                </Button>
              </div>
            </>
          )}

          {/* Locked or Unlocked slot actions */}
          {!isEditingSlot && (slot.status === 'locked' || slot.status === 'unlocked') && (
            <div className="pt-2 space-y-3">
              {selectedUser ? (
                <>
                  <div className="p-3 rounded-lg bg-green-50 dark:bg-green-900/20 flex items-center justify-between">
                    <div>
                      <p className="font-medium text-neutral-900 dark:text-white">
                        {selectedUser.firstName} {selectedUser.lastName}
                      </p>
                      <p className="text-sm text-neutral-600 dark:text-neutral-300">{selectedUser.email}</p>
                      <p className="text-xs text-neutral-500">{t('nav.credits')}: {selectedUser.credits}</p>
                    </div>
                    <button
                      onClick={onClearUser}
                      className="p-1 hover:bg-neutral-200 dark:hover:bg-neutral-700 rounded"
                    >
                      <X size={18} />
                    </button>
                  </div>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={deductCredits}
                      onChange={(e) => onDeductCreditsChange(e.target.checked)}
                      className="w-4 h-4 rounded border-neutral-300 text-primary-500 focus:ring-primary-500"
                    />
                    <span className="text-sm text-neutral-700 dark:text-neutral-300">
                      {t('calendar.deductCredit', { available: selectedUser.credits })}
                    </span>
                  </label>
                  <div className="flex flex-col-reverse gap-2 sm:flex-row">
                    <Button variant="secondary" className="flex-1" onClick={onClearUser}>
                      {t('calendar.change')}
                    </Button>
                    <Button className="flex-1" onClick={onCreateReservation} isLoading={isCreatingReservation}>
                      {t('calendar.register')}
                    </Button>
                  </div>
                </>
              ) : (
                <>
                  <Button
                    className="w-full"
                    variant="primary"
                    leftIcon={<UserPlus size={18} />}
                    onClick={onOpenUserSearch}
                  >
                    {t('calendar.registerUser')}
                  </Button>
                  {slot.status === 'locked' ? (
                    <>
                      <Button
                        className="w-full"
                        variant="secondary"
                        leftIcon={<Unlock size={18} />}
                        onClick={onUnlockSlot}
                        isLoading={isUpdating}
                      >
                        {t('calendar.unlockSlot')}
                      </Button>
                      <Button
                        className="w-full"
                        variant="danger"
                        onClick={onDeleteSlot}
                        isLoading={isDeleting}
                      >
                        {t('calendar.deleteSlot')}
                      </Button>
                    </>
                  ) : (
                    <Button
                      className="w-full"
                      variant="secondary"
                      leftIcon={<Lock size={18} />}
                      onClick={onLockSlot}
                      isLoading={isUpdating}
                    >
                      {t('calendar.lockSlot')}
                    </Button>
                  )}
                </>
              )}
            </div>
          )}

          {/* Cancel confirmation overlay */}
          {showCancelConfirm && slot.reservationId && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
              <div className="bg-white dark:bg-dark-surface rounded-lg p-6 max-w-sm mx-4 shadow-xl">
                <h3 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
                  {t('calendar.confirmCancel')}
                </h3>
                <p className="text-neutral-600 dark:text-neutral-300 mb-4">
                  {t('calendar.confirmCancelText')}{' '}
                  <strong>{slot.assignedUserName || slot.assignedUserEmail}</strong>?
                  {cancelWithRefund && ' ' + t('calendar.creditsWillBeRefunded')}
                  {!cancelWithRefund && ' ' + t('calendar.creditsWontBeRefunded')}
                </p>
                <div className="flex flex-col-reverse gap-3 sm:flex-row">
                  <Button variant="secondary" className="flex-1" onClick={onCloseCancelConfirm}>
                    {t('calendar.no')}
                  </Button>
                  <Button variant="danger" className="flex-1" onClick={onConfirmCancel} isLoading={isCancelling}>
                    {t('calendar.yesCancel')}
                  </Button>
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </Modal>
    <WorkoutLogModal
      isOpen={isWorkoutLogOpen && !!reservationId}
      onClose={() => setIsWorkoutLogOpen(false)}
      reservationId={reservationId}
      workoutLog={workoutLog}
      subtitle={slot ? `${slot.assignedUserName || slot.assignedUserEmail || t('calendar.unknown')} · ${new Date(slot.date).toLocaleDateString(i18n.language)} · ${formatSlotTime(slot.startTime)} - ${formatSlotTime(slot.endTime)}` : undefined}
    />
    </>
  )
}
