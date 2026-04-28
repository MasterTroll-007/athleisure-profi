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
    <div className={`overflow-x-auto rounded-lg border border-white/10 bg-black/10 dark:bg-white/[0.03] ${className}`}>
      <div className="min-w-[360px]">
        <div className="grid grid-cols-[minmax(130px,1fr)_56px_84px_72px] gap-2 border-b border-white/10 px-3 py-2 text-[11px] font-medium uppercase text-neutral-500 dark:text-neutral-400">
          <span>{t('workouts.exercise')}</span>
          <span className="text-right">{t('workouts.sets')}</span>
          <span className="text-right">{t('workouts.reps')}</span>
          <span className="text-right">{t('workouts.weight')}</span>
        </div>
        <div className="divide-y divide-white/10">
          {visibleExercises.map((exercise, index) => (
            <div
              key={`${exercise.name}-${index}`}
              className="grid grid-cols-[minmax(130px,1fr)_56px_84px_72px] gap-2 px-3 py-2 text-sm"
            >
              <span className="truncate text-neutral-800 dark:text-neutral-100">{exercise.name}</span>
              <span className="text-right font-mono text-neutral-600 dark:text-neutral-300">{formatNumber(exercise.sets)}</span>
              <span className="text-right font-mono text-neutral-600 dark:text-neutral-300">{formatNumber(exercise.reps)}</span>
              <span className="text-right font-mono text-neutral-600 dark:text-neutral-300">
                {exercise.weight != null ? `${exercise.weight} kg` : '-'}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
