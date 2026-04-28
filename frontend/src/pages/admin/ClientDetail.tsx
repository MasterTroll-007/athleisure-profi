import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  ArrowLeft,
  User,
  Mail,
  Phone,
  CreditCard,
  Calendar,
  Dumbbell,
  Plus,
  Ruler,
  Trash2,
  UserCog,
} from 'lucide-react'
import { Card, Button, Input, Modal, Badge, Spinner } from '@/components/ui'
import { WorkoutLogModal } from '@/components/calendar/modals/WorkoutLogModal'
import { useToast } from '@/components/ui/Toast'
import { adminApi } from '@/services/api'
import { formatDate, formatTime } from '@/utils/formatters'
import type { BodyMeasurement, ClientNote, Reservation, WorkoutLog } from '@/types/api'

const noteSchema = z.object({
  note: z.string().min(1),
})

const creditSchema = z.object({
  amount: z.coerce.number().min(-100).max(100),
  reason: z.string().min(1),
})

const optionalNumber = z.preprocess(
  (value) => value === '' || value === null ? undefined : value,
  z.coerce.number().min(0).optional()
)

const measurementSchema = z.object({
  date: z.string().min(1),
  weight: optionalNumber,
  bodyFat: optionalNumber,
  chest: optionalNumber,
  waist: optionalNumber,
  hips: optionalNumber,
  bicep: optionalNumber,
  thigh: optionalNumber,
  notes: z.string().optional(),
})

type NoteForm = z.infer<typeof noteSchema>
type CreditForm = z.infer<typeof creditSchema>
type MeasurementForm = z.infer<typeof measurementSchema>

export default function ClientDetail() {
  const { id } = useParams<{ id: string }>()
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const { showToast } = useToast()
  const queryClient = useQueryClient()

  const [isNoteModalOpen, setIsNoteModalOpen] = useState(false)
  const [isCreditModalOpen, setIsCreditModalOpen] = useState(false)
  const [isMeasurementModalOpen, setIsMeasurementModalOpen] = useState(false)
  const [selectedWorkoutLog, setSelectedWorkoutLog] = useState<WorkoutLog | null>(null)

  const { data: client, isLoading } = useQuery({
    queryKey: ['admin', 'client', id],
    queryFn: () => adminApi.getClient(id!),
    enabled: !!id,
  })

  const { data: notes } = useQuery({
    queryKey: ['admin', 'client', id, 'notes'],
    queryFn: () => adminApi.getClientNotes(id!),
    enabled: !!id,
  })

  const { data: reservations } = useQuery({
    queryKey: ['admin', 'client', id, 'reservations'],
    queryFn: () => adminApi.getClientReservations(id!),
    enabled: !!id,
  })

  const { data: workoutLogs } = useQuery({
    queryKey: ['admin', 'client', id, 'workouts'],
    queryFn: () => adminApi.getClientWorkouts(id!),
    enabled: !!id,
  })

  const { data: measurements } = useQuery({
    queryKey: ['admin', 'client', id, 'measurements'],
    queryFn: () => adminApi.getClientMeasurements(id!),
    enabled: !!id,
  })

  const {
    register: registerNote,
    handleSubmit: handleNoteSubmit,
    reset: resetNote,
    formState: { errors: noteErrors },
  } = useForm<NoteForm>({
    resolver: zodResolver(noteSchema),
  })

  const {
    register: registerCredit,
    handleSubmit: handleCreditSubmit,
    reset: resetCredit,
    formState: { errors: creditErrors },
  } = useForm<CreditForm>({
    resolver: zodResolver(creditSchema),
  })

  const {
    register: registerMeasurement,
    handleSubmit: handleMeasurementSubmit,
    reset: resetMeasurement,
    formState: { errors: measurementErrors },
  } = useForm<MeasurementForm>({
    resolver: zodResolver(measurementSchema),
    defaultValues: {
      date: new Date().toISOString().split('T')[0],
    },
  })

  const addNoteMutation = useMutation({
    mutationFn: (data: NoteForm) => adminApi.addClientNote(id!, data.note),
    onSuccess: () => {
      showToast('success', t('admin.noteAdded'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'client', id] })
      setIsNoteModalOpen(false)
      resetNote()
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const deleteNoteMutation = useMutation({
    mutationFn: (noteId: string) => adminApi.deleteClientNote(noteId),
    onSuccess: () => {
      showToast('success', t('admin.noteDeleted'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'client', id] })
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const adjustCreditsMutation = useMutation({
    mutationFn: (data: CreditForm) =>
      adminApi.adjustClientCredits(id!, data.amount, data.reason),
    onSuccess: () => {
      showToast('success', t('admin.creditsAdjusted'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'client', id] })
      setIsCreditModalOpen(false)
      resetCredit()
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const addMeasurementMutation = useMutation({
    mutationFn: (data: MeasurementForm) => adminApi.createClientMeasurement(id!, data),
    onSuccess: () => {
      showToast('success', t('measurements.saved'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'client', id, 'measurements'] })
      setIsMeasurementModalOpen(false)
      resetMeasurement({ date: new Date().toISOString().split('T')[0] })
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const sortedWorkoutLogs = [...(workoutLogs ?? [])].sort((a, b) => {
    const aDate = a.date ?? a.createdAt
    const bDate = b.date ?? b.createdAt
    return new Date(bDate).getTime() - new Date(aDate).getTime()
  })

  const getWorkoutReservation = (reservationId: string) =>
    reservations?.find((reservation) => reservation.id === reservationId)

  const getWorkoutSubtitle = (workoutLog: WorkoutLog | null) => {
    if (!workoutLog) return undefined
    const reservation = getWorkoutReservation(workoutLog.reservationId)
    const date = workoutLog.date ?? reservation?.date
    const time = reservation ? `${formatTime(reservation.startTime)} - ${formatTime(reservation.endTime)}` : null
    return [date ? formatDate(date) : null, time].filter(Boolean).join(' · ')
  }

  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner size="lg" />
      </div>
    )
  }

  if (!client) {
    return (
      <div className="text-center py-12">
        <p className="text-neutral-500 dark:text-neutral-400">{t('admin.clientNotFound')}</p>
      </div>
    )
  }

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate('/admin/clients')}
          className="p-2 -ml-2 text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-white"
        >
          <ArrowLeft size={24} />
        </button>
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {client.firstName} {client.lastName}
        </h1>
      </div>

      {/* Client info */}
      <Card variant="bordered">
        <div className="flex items-center gap-4 mb-6">
          <div className="w-16 h-16 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
            <User size={32} className="text-primary-500" />
          </div>
          <div className="flex-1">
            <p className="font-medium text-neutral-900 dark:text-white text-lg">
              {client.firstName} {client.lastName}
            </p>
            <div className="flex items-center gap-1 text-neutral-500 dark:text-neutral-400 mt-1">
              <Mail size={14} />
              <span className="text-sm">{client.email}</span>
            </div>
            {client.phone && (
              <div className="flex items-center gap-1 text-neutral-500 dark:text-neutral-400 mt-1">
                <Phone size={14} />
                <span className="text-sm">{client.phone}</span>
              </div>
            )}
            <div className="flex items-center gap-1 text-neutral-500 dark:text-neutral-400 mt-1">
              <UserCog size={14} />
              <span className="text-sm">
                {client.trainerName ? `${t('admin.trainer')}: ${client.trainerName}` : t('admin.noTrainer')}
              </span>
            </div>
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-2 gap-4">
          <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg">
            <div className="flex items-center gap-2 mb-1">
              <CreditCard size={16} className="text-primary-500" />
              <span className="text-sm text-neutral-500 dark:text-neutral-400">{t('credits.title')}</span>
            </div>
            <p className="text-2xl font-bold text-neutral-900 dark:text-white">
              {client.credits}
            </p>
          </div>
          <div className="p-4 bg-neutral-50 dark:bg-dark-surface rounded-lg">
            <div className="flex items-center gap-2 mb-1">
              <Calendar size={16} className="text-primary-500" />
              <span className="text-sm text-neutral-500 dark:text-neutral-400">{t('nav.reservations')}</span>
            </div>
            <p className="text-2xl font-bold text-neutral-900 dark:text-white">
              {reservations?.length || 0}
            </p>
          </div>
        </div>

        <div className="flex gap-3 mt-4 pt-4 border-t border-neutral-100 dark:border-dark-border">
          <Button
            variant="secondary"
            size="sm"
            onClick={() => setIsCreditModalOpen(true)}
          >
            {t('admin.adjustCredits')}
          </Button>
        </div>
      </Card>

      {/* Notes */}
      <Card variant="bordered">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white">
            {t('admin.notesLabel')}
          </h2>
          <Button
            variant="ghost"
            size="sm"
            leftIcon={<Plus size={16} />}
            onClick={() => setIsNoteModalOpen(true)}
          >
            {t('admin.addNote')}
          </Button>
        </div>

        {notes && notes.length > 0 ? (
          <div className="space-y-3">
            {notes.map((note: ClientNote) => (
              <div
                key={note.id}
                className="p-3 bg-neutral-50 dark:bg-dark-surface rounded-lg"
              >
                <div className="flex items-start justify-between">
                  <p className="text-neutral-900 dark:text-white whitespace-pre-wrap">
                    {note.content}
                  </p>
                  <button
                    aria-label={t('admin.deleteNoteConfirm')}
                    onClick={() => {
                      if (window.confirm(t('admin.deleteNoteConfirm'))) {
                        deleteNoteMutation.mutate(note.id)
                      }
                    }}
                    className="p-1 text-neutral-400 hover:text-red-500"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
                <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-2">
                  {new Date(note.createdAt).toLocaleDateString(i18n.language)}
                </p>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-sm text-neutral-500 dark:text-neutral-400">{t('admin.noNotes')}</p>
        )}
      </Card>

      {/* Measurements */}
      <Card variant="bordered">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white">
            {t('measurements.title')}
          </h2>
          <Button
            variant="ghost"
            size="sm"
            leftIcon={<Plus size={16} />}
            onClick={() => setIsMeasurementModalOpen(true)}
          >
            {t('measurements.addMeasurement')}
          </Button>
        </div>

        {measurements && measurements.length > 0 ? (
          <div className="space-y-3">
            {measurements.slice(0, 8).map((measurement: BodyMeasurement) => (
              <div key={measurement.id} className="p-3 bg-neutral-50 dark:bg-dark-surface rounded-lg">
                <div className="flex items-center justify-between gap-3">
                  <div className="flex items-center gap-2">
                    <Ruler size={16} className="text-primary-500" />
                    <p className="font-medium text-neutral-900 dark:text-white">
                      {formatDate(measurement.date)}
                    </p>
                  </div>
                  {measurement.weight != null && (
                    <span className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
                      {measurement.weight} kg
                    </span>
                  )}
                </div>
                <div className="mt-2 flex flex-wrap gap-2 text-xs text-neutral-600 dark:text-neutral-300">
                  {measurement.bodyFat != null && <span>{t('measurements.bodyFat')}: {measurement.bodyFat}%</span>}
                  {measurement.waist != null && <span>{t('measurements.waist')}: {measurement.waist} cm</span>}
                  {measurement.chest != null && <span>{t('measurements.chest')}: {measurement.chest} cm</span>}
                  {measurement.hips != null && <span>{t('measurements.hips')}: {measurement.hips} cm</span>}
                </div>
                {measurement.notes && (
                  <p className="mt-2 text-sm text-neutral-500 dark:text-neutral-400 whitespace-pre-wrap">
                    {measurement.notes}
                  </p>
                )}
              </div>
            ))}
          </div>
        ) : (
          <p className="text-sm text-neutral-500 dark:text-neutral-400">{t('measurements.noMeasurements')}</p>
        )}
      </Card>

      {/* Workout history */}
      <Card variant="bordered">
        <div className="mb-4 flex items-center justify-between gap-3">
          <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white">
            {t('workouts.historyTitle')}
          </h2>
          {sortedWorkoutLogs.length > 0 && (
            <Badge variant="default">
              {t('workouts.exerciseLogCount', { count: sortedWorkoutLogs.length })}
            </Badge>
          )}
        </div>

        {sortedWorkoutLogs.length > 0 ? (
          <div className="max-h-[520px] space-y-3 overflow-y-auto pr-1">
            {sortedWorkoutLogs.map((workoutLog) => {
              const reservation = getWorkoutReservation(workoutLog.reservationId)
              const date = workoutLog.date ?? reservation?.date
              const exerciseCount = workoutLog.exercises.filter((exercise) => exercise.name.trim().length > 0).length

              return (
                <button
                  key={workoutLog.id}
                  type="button"
                  onClick={() => setSelectedWorkoutLog(workoutLog)}
                  className="w-full rounded-lg border border-white/10 bg-white/[0.04] p-3 text-left transition-colors hover:border-primary-300/40 hover:bg-white/[0.07]"
                >
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary-500/15 text-primary-300">
                          <Dumbbell size={18} />
                        </span>
                        <div className="min-w-0">
                          <p className="font-medium text-neutral-900 dark:text-white">
                            {date ? formatDate(date) : t('workouts.title')}
                          </p>
                          {reservation && (
                            <p className="text-sm text-neutral-500 dark:text-neutral-400">
                              {formatTime(reservation.startTime)} - {formatTime(reservation.endTime)}
                            </p>
                          )}
                        </div>
                      </div>
                    </div>
                    <span className="shrink-0 rounded-full bg-white/10 px-2.5 py-1 text-xs font-medium text-white/70">
                      {t('workouts.exerciseCount', { count: exerciseCount })}
                    </span>
                  </div>

                  {workoutLog.exercises.length > 0 && (
                    <div className="mt-3 flex flex-wrap gap-2">
                      {workoutLog.exercises.slice(0, 4).map((exercise, index) => (
                        <span
                          key={`${workoutLog.id}-${exercise.name}-${index}`}
                          className="rounded-full bg-black/10 px-2.5 py-1 text-xs text-neutral-600 dark:bg-white/[0.06] dark:text-neutral-300"
                        >
                          {exercise.name}
                        </span>
                      ))}
                    </div>
                  )}

                  {workoutLog.notes && (
                    <p className="mt-3 line-clamp-2 text-sm text-neutral-500 dark:text-neutral-400">
                      {workoutLog.notes}
                    </p>
                  )}
                </button>
              )
            })}
          </div>
        ) : (
          <p className="text-sm text-neutral-500 dark:text-neutral-400">{t('workouts.noWorkouts')}</p>
        )}
      </Card>

      {/* Recent reservations */}
      <Card variant="bordered">
        <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white mb-4">
          {t('admin.lastReservations')}
        </h2>

        {reservations && reservations.length > 0 ? (
          <div className="space-y-2">
            {reservations.slice(0, 10).map((reservation: Reservation) => (
              <div
                key={reservation.id}
                className="flex items-center justify-between p-3 bg-neutral-50 dark:bg-dark-surface rounded-lg"
              >
                <div>
                  <p className="font-medium text-neutral-900 dark:text-white">
                    {formatDate(reservation.date)}
                  </p>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400">
                    {formatTime(reservation.startTime)} - {formatTime(reservation.endTime)}
                  </p>
                </div>
                <Badge
                  variant={
                    reservation.status === 'confirmed'
                      ? 'success'
                      : reservation.status === 'cancelled'
                        ? 'danger'
                        : 'default'
                  }
                >
                  {t(`reservationStatus.${reservation.status}`)}
                </Badge>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-sm text-neutral-500 dark:text-neutral-400">{t('admin.noReservations')}</p>
        )}
      </Card>

      <WorkoutLogModal
        isOpen={!!selectedWorkoutLog}
        onClose={() => setSelectedWorkoutLog(null)}
        reservationId={selectedWorkoutLog?.reservationId ?? null}
        workoutLog={selectedWorkoutLog}
        subtitle={getWorkoutSubtitle(selectedWorkoutLog)}
        onSaved={(savedWorkoutLog) => {
          setSelectedWorkoutLog(savedWorkoutLog)
          queryClient.invalidateQueries({ queryKey: ['admin', 'client', id, 'workouts'] })
        }}
      />

      {/* Add note modal */}
      <Modal
        isOpen={isNoteModalOpen}
        onClose={() => {
          setIsNoteModalOpen(false)
          resetNote()
        }}
        title={t('admin.addNote')}
      >
        <form onSubmit={handleNoteSubmit((data) => addNoteMutation.mutate(data))} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
              {t('admin.notesLabel')}
            </label>
            <textarea
              {...registerNote('note')}
              rows={4}
              className="w-full px-3 py-2 rounded-lg border border-neutral-200 dark:border-dark-border bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
              placeholder={t('admin.notePlaceholder')}
            />
            {noteErrors.note && (
              <p className="text-sm text-red-500 mt-1">{t('errors.required')}</p>
            )}
          </div>

          <div className="flex gap-3">
            <Button
              type="button"
              variant="secondary"
              className="flex-1"
              onClick={() => {
                setIsNoteModalOpen(false)
                resetNote()
              }}
            >
              {t('common.cancel')}
            </Button>
            <Button type="submit" className="flex-1" isLoading={addNoteMutation.isPending}>
              {t('common.save')}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Add measurement modal */}
      <Modal
        isOpen={isMeasurementModalOpen}
        onClose={() => {
          setIsMeasurementModalOpen(false)
          resetMeasurement({ date: new Date().toISOString().split('T')[0] })
        }}
        title={t('measurements.addMeasurement')}
      >
        <form onSubmit={handleMeasurementSubmit((data) => addMeasurementMutation.mutate(data))} className="space-y-4">
          <Input
            label={t('calendar.date')}
            type="date"
            {...registerMeasurement('date')}
            error={measurementErrors.date?.message}
          />
          <div className="grid grid-cols-2 gap-3">
            <Input label={t('measurements.weight')} type="number" step="0.1" {...registerMeasurement('weight')} />
            <Input label={t('measurements.bodyFat')} type="number" step="0.1" {...registerMeasurement('bodyFat')} />
            <Input label={t('measurements.chest')} type="number" step="0.1" {...registerMeasurement('chest')} />
            <Input label={t('measurements.waist')} type="number" step="0.1" {...registerMeasurement('waist')} />
            <Input label={t('measurements.hips')} type="number" step="0.1" {...registerMeasurement('hips')} />
            <Input label={t('measurements.bicep')} type="number" step="0.1" {...registerMeasurement('bicep')} />
            <Input label={t('measurements.thigh')} type="number" step="0.1" {...registerMeasurement('thigh')} />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
              {t('workouts.notes')}
            </label>
            <textarea
              {...registerMeasurement('notes')}
              rows={3}
              className="w-full px-3 py-2 rounded-lg border border-neutral-200 dark:border-dark-border bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
            />
          </div>
          <div className="flex gap-3">
            <Button type="button" variant="secondary" className="flex-1" onClick={() => setIsMeasurementModalOpen(false)}>
              {t('common.cancel')}
            </Button>
            <Button type="submit" className="flex-1" isLoading={addMeasurementMutation.isPending}>
              {t('common.save')}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Adjust credits modal */}
      <Modal
        isOpen={isCreditModalOpen}
        onClose={() => {
          setIsCreditModalOpen(false)
          resetCredit()
        }}
        title={t('admin.adjustCredits')}
      >
        <form onSubmit={handleCreditSubmit((data) => adjustCreditsMutation.mutate(data))} className="space-y-4">
          <p className="text-sm text-neutral-500 dark:text-neutral-400">
            {t('admin.currentBalance')}: <strong>{client.credits}</strong>
          </p>

          <Input
            label={t('admin.creditAmount')}
            type="number"
            {...registerCredit('amount')}
            error={creditErrors.amount?.message}
          />

          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
              {t('admin.reasonLabel')}
            </label>
            <textarea
              {...registerCredit('reason')}
              rows={2}
              className="w-full px-3 py-2 rounded-lg border border-neutral-200 dark:border-dark-border bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent resize-none"
              placeholder={t('admin.reasonPlaceholder')}
            />
            {creditErrors.reason && (
              <p className="text-sm text-red-500 mt-1">{t('errors.required')}</p>
            )}
          </div>

          <div className="flex gap-3">
            <Button
              type="button"
              variant="secondary"
              className="flex-1"
              onClick={() => {
                setIsCreditModalOpen(false)
                resetCredit()
              }}
            >
              {t('common.cancel')}
            </Button>
            <Button type="submit" className="flex-1" isLoading={adjustCreditsMutation.isPending}>
              {t('common.save')}
            </Button>
          </div>
        </form>
      </Modal>

    </div>
  )
}
