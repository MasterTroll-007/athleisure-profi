import { useTranslation } from 'react-i18next'
import type { WorkoutExercise } from '@/types/api'

interface WorkoutExerciseSummaryTableProps {
  exercises: WorkoutExercise[]
  maxRows?: number
  className?: string
}

const formatNumber = (value: number | null | undefined) =>
  value != null ? String(value) : '-'

export function WorkoutExerciseSummaryTable({
  exercises,
  maxRows = 4,
  className = '',
}: WorkoutExerciseSummaryTableProps) {
  const { t } = useTranslation()
  const visibleExercises = exercises
    .filter((exercise) => exercise.name.trim().length > 0)
    .slice(0, maxRows)

  if (visibleExercises.length === 0) return null

  return (
    <div className={`w-full min-w-0 rounded-lg border border-white/10 bg-black/10 dark:bg-white/[0.03] ${className}`}>
      <div className="w-full min-w-0">
        <div className="grid grid-cols-[minmax(0,1fr)_2.5rem_3.25rem_3.5rem] gap-1.5 border-b border-white/10 px-2.5 py-2 text-[10px] font-medium uppercase text-neutral-500 dark:text-neutral-400 sm:grid-cols-[minmax(0,1fr)_3.5rem_4.75rem_4.5rem] sm:gap-3 sm:px-4 sm:text-[11px] md:px-5">
          <span className="min-w-0 truncate">{t('workouts.exercise')}</span>
          <span className="min-w-0 truncate text-right">{t('workouts.sets')}</span>
          <span className="min-w-0 truncate text-right">{t('workouts.reps')}</span>
          <span className="min-w-0 truncate text-right">{t('workouts.weight')}</span>
        </div>
        <div className="divide-y divide-white/10">
          {visibleExercises.map((exercise, index) => (
            <div
              key={`${exercise.name}-${index}`}
              className="grid grid-cols-[minmax(0,1fr)_2.5rem_3.25rem_3.5rem] gap-1.5 px-2.5 py-2 text-xs sm:grid-cols-[minmax(0,1fr)_3.5rem_4.75rem_4.5rem] sm:gap-3 sm:px-4 sm:py-2.5 sm:text-sm md:px-5"
            >
              <span className="truncate text-neutral-800 dark:text-neutral-100">{exercise.name}</span>
              <span className="min-w-0 truncate text-right font-mono text-neutral-600 dark:text-neutral-300">{formatNumber(exercise.sets)}</span>
              <span className="min-w-0 truncate text-right font-mono text-neutral-600 dark:text-neutral-300">{formatNumber(exercise.reps)}</span>
              <span className="min-w-0 truncate text-right font-mono text-neutral-600 dark:text-neutral-300">
                {exercise.weight != null ? `${exercise.weight} kg` : '-'}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
