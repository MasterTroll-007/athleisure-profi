import { useTranslation } from 'react-i18next'
import type { TrainingLocation } from '@/types/api'
import { NEUTRAL_LOCATION_COLOR, darken, isValidHex } from '@/utils/color'

interface CalendarLegendProps {
  locations?: TrainingLocation[]
  compact?: boolean
}

function Swatch({
  color,
  label,
  compact = false,
}: {
  color: string
  label: string
  compact?: boolean
}) {
  const base = isValidHex(color) ? color : NEUTRAL_LOCATION_COLOR
  const style = { backgroundColor: base, borderColor: darken(base, 0.18) }

  return (
    <span className={compact
      ? 'inline-flex min-w-0 shrink-0 items-center gap-1.5 rounded-full border border-neutral-200 bg-white/75 px-2.5 py-1 dark:border-white/10 dark:bg-white/[0.05]'
      : 'inline-flex min-w-0 items-center gap-2'
    }>
      <span
        className={compact ? 'h-3 w-3 shrink-0 rounded border' : 'h-3.5 w-3.5 shrink-0 rounded border'}
        style={style}
      />
      <span className="truncate text-neutral-600 dark:text-neutral-400">{label}</span>
    </span>
  )
}

export function CalendarLegend({ locations, compact = false }: CalendarLegendProps) {
  const { t, i18n } = useTranslation()
  const activeLocations = (locations ?? [])
    .filter((location) => location.isActive && isValidHex(location.color))
    .sort((a, b) => a.nameCs.localeCompare(b.nameCs, i18n.language))
  return (
    <div className={compact
      ? 'border-b border-neutral-200 bg-neutral-50 px-3 py-2 text-xs dark:border-neutral-700 dark:bg-dark-surface'
      : 'rounded-lg border border-white/10 bg-white/[0.04] px-3 py-2 text-sm'
    }>
      <div className={compact ? 'flex min-w-0 items-center gap-2' : 'flex flex-wrap gap-x-5 gap-y-2'}>
        <div className={compact ? 'flex min-w-0 items-center gap-2' : 'flex min-w-fit items-center gap-2'}>
          <span className="shrink-0 text-xs font-semibold uppercase text-neutral-500 dark:text-neutral-500">
            {t('calendar.locationLegend')}
          </span>
          {activeLocations.length > 0 ? (
            <div className={compact
              ? '-mr-3 flex min-w-0 flex-1 items-center gap-2 overflow-x-auto pr-3 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden'
              : 'flex flex-wrap items-center gap-x-4 gap-y-2'
            }>
              {activeLocations.map((location) => (
                <Swatch
                  key={location.id}
                  color={location.color}
                  label={i18n.language === 'cs' ? location.nameCs : (location.nameEn || location.nameCs)}
                  compact={compact}
                />
              ))}
            </div>
          ) : (
            <Swatch color={NEUTRAL_LOCATION_COLOR} label={t('calendar.noLocationsLegend')} compact={compact} />
          )}
        </div>
      </div>
    </div>
  )
}
