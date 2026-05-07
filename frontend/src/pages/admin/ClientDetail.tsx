import { useMemo, useState } from 'react'
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
  Pencil,
  Plus,
  Ruler,
  Save,
  Sparkles,
  Trash2,
  UserCog,
} from 'lucide-react'
import { Card, Button, Input, Modal, Badge, Spinner, Pagination, DatePicker, Textarea } from '@/components/ui'
import { WorkoutLogModal } from '@/components/calendar/modals/WorkoutLogModal'
import { WorkoutExerciseSummaryTable } from '@/components/workouts/WorkoutExerciseSummaryTable'
import { useToast } from '@/components/ui/Toast'
import { adminApi } from '@/services/api'
import { formatDate, formatTime } from '@/utils/formatters'
import type { BodyMeasurement, ClientNote, CreditPackage, Reservation, WorkoutLog } from '@/types/api'

const noteSchema = z.object({
  note: z.string().min(1),
})

const creditSchema = z.object({
  amount: z.coerce.number().int().min(-1_000_000).max(1_000_000).refine((value) => value !== 0),
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

type QuickCreditOption = {
  amount: number
  title: string
}

const fallbackQuickCreditAmounts = [500, 1000]

function packageDisplayName(pkg: CreditPackage, language: string) {
  if (language === 'en' && pkg.nameEn) return pkg.nameEn
  return pkg.nameCs
}

export default function ClientDetail() {
  const { id } = useParams<{ id: string }>()
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const defaultCreditReason = t('admin.manualCreditDefaultReason')

  const [isNoteModalOpen, setIsNoteModalOpen] = useState(false)
  const [isCreditModalOpen, setIsCreditModalOpen] = useState(false)
  const [isMeasurementModalOpen, setIsMeasurementModalOpen] = useState(false)
  const [selectedWorkoutLog, setSelectedWorkoutLog] = useState<WorkoutLog | null>(null)
  const [notePage, setNotePage] = useState(0)
  const [reservationPage, setReservationPage] = useState(0)
  const [workoutPage, setWorkoutPage] = useState(0)
  const [measurementPage, setMeasurementPage] = useState(0)

  const { data: client, isLoading } = useQuery({
    queryKey: ['admin', 'client', id],
    queryFn: () => adminApi.getClient(id!),
    enabled: !!id,
  })

  const { data: notes, isFetching: isNotesFetching } = useQuery({
    queryKey: ['admin', 'client', id, 'notes', notePage],
    queryFn: () => adminApi.getClientNotes(id!, notePage, 10),
    enabled: !!id,
  })

  const { data: reservations, isFetching: isReservationsFetching } = useQuery({
    queryKey: ['admin', 'client', id, 'reservations', reservationPage],
    queryFn: () => adminApi.getClientReservations(id!, reservationPage, 10),
    enabled: !!id,
  })

  const { data: workoutLogs, isFetching: isWorkoutsFetching } = useQuery({
    queryKey: ['admin', 'client', id, 'workouts', workoutPage],
    queryFn: () => adminApi.getClientWorkouts(id!, workoutPage, 10),
    enabled: !!id,
  })

  const { data: measurements, isFetching: isMeasurementsFetching } = useQuery({
    queryKey: ['admin', 'client', id, 'measurements', measurementPage],
    queryFn: () => adminApi.getClientMeasurements(id!, measurementPage, 8),
    enabled: !!id,
  })

  const { data: creditPackages = [] } = useQuery({
    queryKey: ['adminCreditPackages'],
    queryFn: adminApi.getAllPackages,
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
    setValue: setCreditValue,
    watch: watchCredit,
    formState: { errors: creditErrors },
  } = useForm<CreditForm>({
    resolver: zodResolver(creditSchema),
    defaultValues: {
      amount: 0,
      reason: defaultCreditReason,
    },
  })

  const {
    register: registerMeasurement,
    handleSubmit: handleMeasurementSubmit,
    reset: resetMeasurement,
    setValue: setMeasurementValue,
    watch: watchMeasurement,
    formState: { errors: measurementErrors },
  } = useForm<MeasurementForm>({
    resolver: zodResolver(measurementSchema),
    defaultValues: {
      date: new Date().toISOString().split('T')[0],
    },
  })
  const measurementDate = watchMeasurement('date')

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
      resetCredit({ amount: 0, reason: defaultCreditReason })
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

  const noteItems = notes?.content ?? []
  const reservationItems = reservations?.content ?? []
  const workoutItems = workoutLogs?.content ?? []
  const measurementItems = measurements?.content ?? []
  const quickCreditOptions = useMemo<QuickCreditOption[]>(() => {
    const activePackages = creditPackages
      .filter((pkg) => pkg.isActive && pkg.credits > 0)
      .sort((a, b) => a.sortOrder - b.sortOrder || a.credits - b.credits)

    const uniquePackageByCredits = new Map<number, QuickCreditOption>()
    activePackages.forEach((pkg) => {
      if (!uniquePackageByCredits.has(pkg.credits)) {
        uniquePackageByCredits.set(pkg.credits, {
          amount: pkg.credits,
          title: packageDisplayName(pkg, i18n.language),
        })
      }
    })

    const positiveOptions = uniquePackageByCredits.size > 0
      ? Array.from(uniquePackageByCredits.values())
      : fallbackQuickCreditAmounts.map((amount) => ({
          amount,
          title: `${amount} ${t('credits.title').toLocaleLowerCase(i18n.language)}`,
        }))

    return [
      ...positiveOptions,
      ...positiveOptions.map((option) => ({
        amount: -option.amount,
        title: option.title,
      })),
    ]
  }, [creditPackages, i18n.language, t])
  const creditAmount = Number(watchCredit('amount') ?? 0)
  const projectedCreditBalance = client ? client.credits + creditAmount : 0
  const isCreditAdjustmentInvalid = !creditAmount || projectedCreditBalance < 0

  const applyQuickCreditOption = (option: QuickCreditOption) => {
    if (client && client.credits + option.amount < 0) return
    setCreditValue('amount', option.amount, { shouldDirty: true, shouldValidate: true })
    setCreditValue(
      'reason',
      t(option.amount > 0 ? 'admin.manualCreditPackageAddReason' : 'admin.manualCreditPackageRemoveReason', {
        packageName: option.title,
      }),
      { shouldDirty: true, shouldValidate: true }
    )
  }

  const submitCreditAdjustment = (data: CreditForm) => {
    if (client && client.credits + data.amount < 0) {
      showToast('error', t('admin.creditAdjustmentInvalid'))
      return
    }
    adjustCreditsMutation.mutate(data)
  }

  const sortedWorkoutLogs = [...workoutItems].sort((a, b) => {
    const aDate = a.date ?? a.createdAt
    const bDate = b.date ?? b.createdAt
    return new Date(bDate).getTime() - new Date(aDate).getTime()
  })

  const getWorkoutReservation = (reservationId: string) =>
    reservationItems.find((reservation) => reservation.id === reservationId)

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
        <div className="flex items-center gap-4">
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
      </Card>

      {/* Stats */}
      <div className="grid gap-4 sm:grid-cols-2">
        <Card variant="bordered">
          <div className="flex items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <CreditCard size={16} className="text-primary-500" />
                <span className="text-sm text-neutral-500 dark:text-neutral-400">{t('credits.title')}</span>
              </div>
              <p className="text-2xl font-bold text-neutral-900 dark:text-white">
                {client.credits}
              </p>
            </div>
            <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-primary-500/10 text-primary-300">
              <CreditCard size={16} className="text-primary-500" />
            </div>
          </div>
        </Card>
        <Card variant="bordered">
          <div className="flex items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <Calendar size={16} className="text-primary-500" />
                <span className="text-sm text-neutral-500 dark:text-neutral-400">{t('nav.reservations')}</span>
              </div>
              <p className="text-2xl font-bold text-neutral-900 dark:text-white">
                {reservations?.totalElements || 0}
              </p>
            </div>
            <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-primary-500/10 text-primary-300">
              <Calendar size={16} className="text-primary-500" />
            </div>
          </div>
        </Card>
      </div>

      {/* Quick credit adjustment */}
      <Card variant="bordered">
        <form
          onSubmit={handleCreditSubmit(submitCreditAdjustment)}
          className="space-y-5"
        >
          <input type="hidden" {...registerCredit('amount')} />

          <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white">
                {t('admin.quickAdjustCredits')}
              </h2>
              <p className="mt-1 text-xs text-neutral-500 dark:text-neutral-400">
                {t('admin.quickAdjustCreditsHint')}
              </p>
            </div>
            <div className="flex items-center gap-3 text-sm text-neutral-500 dark:text-neutral-400">
              <span>
                {t('admin.currentBalance')}{' '}
                <strong className="text-neutral-900 dark:text-white/85">{client.credits}</strong>
              </span>
              <span className="text-white/30">→</span>
              <span>
                {t('admin.newBalance')}:{' '}
                <strong
                  className={`text-base ${
                    projectedCreditBalance < 0
                      ? 'text-red-400'
                      : creditAmount === 0
                        ? 'text-neutral-500 dark:text-white/55'
                        : 'text-amber-300'
                  }`}
                >
                  {projectedCreditBalance}
                </strong>
              </span>
            </div>
          </div>

          <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_280px]">
            <div>
              <div className="mb-2 text-[11px] font-semibold uppercase tracking-wider text-neutral-500 dark:text-white/55">
                {t('admin.selectPackage')}
              </div>
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
                {quickCreditOptions.map((option) => {
                  const isNegative = option.amount < 0
                  const isDisabled = !!client && client.credits + option.amount < 0
                  const isSelected = creditAmount === option.amount
                  return (
                    <Button
                      key={`${option.amount}-${option.title}`}
                      type="button"
                      variant={isNegative ? 'ghost' : 'secondary'}
                      size="sm"
                      className={`min-h-[52px] px-2 py-2 text-xs ${isSelected ? 'ring-2 ring-amber-300/70 ring-offset-0' : ''}`}
                      disabled={isDisabled}
                      onClick={() => applyQuickCreditOption(option)}
                    >
                      <span className="flex min-w-0 flex-col items-center gap-0.5 leading-tight">
                        <span className="text-sm font-semibold">
                          {option.amount > 0 ? `+${option.amount}` : option.amount}
                        </span>
                        <span className="max-w-full truncate text-[10px] opacity-75">
                          {option.title}
                        </span>
                      </span>
                    </Button>
                  )
                })}
              </div>
            </div>

            <div className="flex flex-col border-t border-white/10 pt-4 lg:border-l lg:border-t-0 lg:pl-5 lg:pt-0">
              <div className="mb-4 flex items-center justify-between">
                <span className="text-[13px] font-semibold uppercase tracking-wider text-neutral-900 dark:text-white/85">
                  {t('admin.adjustmentSummary')}
                </span>
                <span
                  className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] ${
                    creditAmount === 0
                      ? 'border-white/10 bg-white/[0.06] text-neutral-500 dark:text-white/75'
                      : 'border-amber-300/30 bg-gradient-to-br from-amber-300/[0.18] to-white/[0.04] text-amber-100/95'
                  }`}
                >
                  <Sparkles size={12} />
                  {creditAmount === 0
                    ? '—'
                    : `${creditAmount > 0 ? '+' : ''}${creditAmount}`}
                </span>
              </div>

              <div className="space-y-2 text-sm">
                <div className="flex items-center justify-between">
                  <span className="text-neutral-500 dark:text-white/55">
                    {t('admin.currentBalance')}
                  </span>
                  <span className="font-medium text-neutral-900 dark:text-white">
                    {client.credits}
                  </span>
                </div>
                <div className="flex items-center justify-between border-t border-white/10 pt-2">
                  <span className="text-neutral-500 dark:text-white/55">
                    {t('admin.afterAdjustment')}
                  </span>
                  <span
                    className={`text-lg font-bold ${
                      projectedCreditBalance < 0 ? 'text-red-400' : 'text-amber-200'
                    }`}
                  >
                    {projectedCreditBalance}
                  </span>
                </div>
              </div>

              <div className="mt-5 flex flex-col gap-2 sm:flex-row lg:mt-auto lg:flex-col">
                <Button
                  type="submit"
                  variant="primary"
                  className="flex-1"
                  isLoading={adjustCreditsMutation.isPending}
                  disabled={isCreditAdjustmentInvalid}
                  leftIcon={<Save size={16} />}
                >
                  {t('common.save')}
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  className="flex-1"
                  onClick={() => setIsCreditModalOpen(true)}
                  leftIcon={<Pencil size={14} />}
                >
                  {t('admin.detailedCreditEdit')}
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  className="flex-1"
                  disabled={!creditAmount}
                  onClick={() => resetCredit({ amount: 0, reason: defaultCreditReason })}
                >
                  {t('common.cancel')}
                </Button>
              </div>
            </div>
          </div>
        </form>
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

        {noteItems.length > 0 ? (
          <div className="space-y-3">
            {noteItems.map((note: ClientNote) => (
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
            {notes && (
              <Pagination
                page={notes.page}
                totalPages={notes.totalPages}
                totalElements={notes.totalElements}
                size={notes.size}
                onPageChange={setNotePage}
                isLoading={isNotesFetching}
              />
            )}
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

        {measurementItems.length > 0 ? (
          <div className="space-y-3">
            {measurementItems.map((measurement: BodyMeasurement) => (
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
            {measurements && (
              <Pagination
                page={measurements.page}
                totalPages={measurements.totalPages}
                totalElements={measurements.totalElements}
                size={measurements.size}
                onPageChange={setMeasurementPage}
                isLoading={isMeasurementsFetching}
              />
            )}
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
              {t('workouts.exerciseLogCount', { count: workoutLogs?.totalElements ?? sortedWorkoutLogs.length })}
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
                    <WorkoutExerciseSummaryTable
                      exercises={workoutLog.exercises}
                      maxRows={4}
                      className="mt-3"
                    />
                  )}

                  {workoutLog.notes && (
                    <p className="mt-3 line-clamp-2 text-sm text-neutral-500 dark:text-neutral-400">
                      {workoutLog.notes}
                    </p>
                  )}
                </button>
              )
            })}
            {workoutLogs && (
              <Pagination
                page={workoutLogs.page}
                totalPages={workoutLogs.totalPages}
                totalElements={workoutLogs.totalElements}
                size={workoutLogs.size}
                onPageChange={setWorkoutPage}
                isLoading={isWorkoutsFetching}
              />
            )}
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

        {reservationItems.length > 0 ? (
          <div className="space-y-2">
            {reservationItems.map((reservation: Reservation) => (
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
            {reservations && (
              <Pagination
                page={reservations.page}
                totalPages={reservations.totalPages}
                totalElements={reservations.totalElements}
                size={reservations.size}
                onPageChange={setReservationPage}
                isLoading={isReservationsFetching}
              />
            )}
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
            <Textarea
              {...registerNote('note')}
              rows={4}
              className="resize-none"
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
          <DatePicker
            label={t('calendar.date')}
            value={measurementDate ?? ''}
            onChange={(nextDate) =>
              setMeasurementValue('date', nextDate, { shouldDirty: true, shouldValidate: true })
            }
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
            <Textarea
              {...registerMeasurement('notes')}
              rows={3}
              className="resize-none"
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
        <form onSubmit={handleCreditSubmit(submitCreditAdjustment)} className="space-y-4" noValidate>
          <p className="text-sm text-neutral-500 dark:text-neutral-400">
            {t('admin.currentBalance')}: <strong>{client.credits}</strong>
          </p>

          <Input
            label={t('admin.creditAmount')}
            type="number"
            step={100}
            {...registerCredit('amount')}
            error={creditErrors.amount?.message}
          />

          <Textarea
            label={t('admin.reasonLabel')}
            {...registerCredit('reason')}
            rows={3}
            maxWidth="full"
            className="min-h-[108px] resize-none"
            placeholder={t('admin.reasonPlaceholder')}
            error={creditErrors.reason ? t('errors.required') : undefined}
          />

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
            <Button
              type="submit"
              className="flex-1"
              isLoading={adjustCreditsMutation.isPending}
              disabled={isCreditAdjustmentInvalid}
            >
              {t('common.save')}
            </Button>
          </div>
        </form>
      </Modal>

    </div>
  )
}
