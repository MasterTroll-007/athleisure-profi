import { useEffect, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Dumbbell, Plus, Trash2 } from 'lucide-react'
import { Button, Input, Modal } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { adminApi } from '@/services/api'
import type { WorkoutExercise, WorkoutLog } from '@/types/api'

interface WorkoutLogModalProps {
  isOpen: boolean
  onClose: () => void
  reservationId: string | null
  workoutLog?: WorkoutLog | null
  title?: string
  subtitle?: string
  onSaved?: (workoutLog: WorkoutLog) => void
}

const emptyExercise = (): WorkoutExercise => ({
  name: '',
  sets: undefined,
  reps: undefined,
  weight: undefined,
})

export function WorkoutLogModal({
  isOpen,
  onClose,
  reservationId,
  workoutLog,
  title,
  subtitle,
  onSaved,
}: WorkoutLogModalProps) {
  const { t } = useTranslation()
  const { showToast } = useToast()
  const queryClient = useQueryClient()
  const [exercises, setExercises] = useState<WorkoutExercise[]>([emptyExercise()])
  const [notes, setNotes] = useState('')

  useEffect(() => {
    if (!isOpen) return

    setExercises(workoutLog?.exercises?.length ? workoutLog.exercises : [emptyExercise()])
    setNotes(workoutLog?.notes ?? '')
  }, [isOpen, workoutLog])

  const updateExercise = (index: number, patch: Partial<WorkoutExercise>) => {
    setExercises((current) => current.map((exercise, i) => i === index ? { ...exercise, ...patch } : exercise))
  }

  const saveMutation = useMutation({
    mutationFn: () => {
      if (!reservationId) {
        throw new Error('Missing reservation id')
      }

      const cleanedExercises = exercises
        .map((exercise) => ({
          ...exercise,
          name: exercise.name.trim(),
          notes: exercise.notes?.trim() || undefined,
        }))
        .filter((exercise) => exercise.name.length > 0)

      return workoutLog
        ? adminApi.updateWorkoutLog(reservationId, cleanedExercises, notes.trim() || undefined)
        : adminApi.createWorkoutLog(reservationId, cleanedExercises, notes.trim() || undefined)
    },
    onSuccess: (savedWorkoutLog) => {
      showToast('success', t('workouts.saved'))
      queryClient.invalidateQueries({ queryKey: ['admin', 'reservation', reservationId, 'workout'] })
      onSaved?.(savedWorkoutLog)
      onClose()
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const canRemoveExercise = exercises.length > 1

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={title || t('workouts.title')}
      size="xl"
      mobileFullScreen
      backdropClassName="bg-black/35 backdrop-blur-[1px]"
    >
      <div className="space-y-5">
        {subtitle && (
          <p className="text-sm text-white/60">{subtitle}</p>
        )}

        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-2 text-sm text-white/70">
            <Dumbbell size={16} className="text-primary-300" />
            <span>{t('workouts.exerciseCount', { count: exercises.filter((exercise) => exercise.name.trim()).length })}</span>
          </div>
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={() => setExercises((current) => [...current, emptyExercise()])}
          >
            <Plus size={16} />
            {t('workouts.addExercise')}
          </Button>
        </div>

        <div className="space-y-3">
          {exercises.map((exercise, index) => (
            <div
              key={index}
              className="rounded-lg border border-white/10 bg-white/[0.04] p-3"
            >
              <div className="grid gap-3 md:grid-cols-[minmax(220px,1fr)_96px_116px_108px_44px] md:items-end">
                <Input
                  label={t('workouts.exercise')}
                  value={exercise.name}
                  placeholder={t('workouts.exercisePlaceholder')}
                  onChange={(e) => updateExercise(index, { name: e.target.value })}
                />
                <Input
                  label={t('workouts.sets')}
                  type="number"
                  min={0}
                  value={exercise.sets ?? ''}
                  onChange={(e) => updateExercise(index, { sets: e.target.value ? Number(e.target.value) : undefined })}
                />
                <Input
                  label={t('workouts.reps')}
                  type="number"
                  min={0}
                  value={exercise.reps ?? ''}
                  onChange={(e) => updateExercise(index, { reps: e.target.value ? Number(e.target.value) : undefined })}
                />
                <Input
                  label={t('workouts.weight')}
                  type="number"
                  min={0}
                  step="0.5"
                  value={exercise.weight ?? ''}
                  onChange={(e) => updateExercise(index, { weight: e.target.value ? Number(e.target.value) : undefined })}
                />
                <button
                  type="button"
                  aria-label={t('workouts.removeExercise')}
                  onClick={() => setExercises((current) => current.filter((_, i) => i !== index))}
                  disabled={!canRemoveExercise}
                  className="flex min-h-[48px] items-center justify-center rounded-lg border border-white/10 text-white/45 transition-colors hover:border-red-400/40 hover:text-red-300 disabled:cursor-not-allowed disabled:opacity-30"
                >
                  <Trash2 size={18} />
                </button>
              </div>
            </div>
          ))}
        </div>

        <div>
          <label className="mb-1.5 block text-sm font-medium text-white/75">
            {t('workouts.notes')}
          </label>
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder={t('workouts.notesPlaceholder')}
            rows={5}
            className="min-h-[132px] w-full resize-y rounded-lg border border-white/10 bg-white/10 px-4 py-3 text-white placeholder:text-white/40 focus:outline-none focus:ring-2 focus:ring-primary-300"
          />
        </div>

        <div className="flex flex-col-reverse gap-3 border-t border-white/10 pt-4 sm:flex-row sm:justify-end">
          <Button type="button" variant="secondary" onClick={onClose}>
            {t('common.cancel')}
          </Button>
          <Button
            type="button"
            onClick={() => saveMutation.mutate()}
            isLoading={saveMutation.isPending}
            disabled={!reservationId}
          >
            {t('common.save')}
          </Button>
        </div>
      </div>
    </Modal>
  )
}
