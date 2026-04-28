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
}: {
  color: string
  label: string
}) {
  const base = isValidHex(color) ? color : NEUTRAL_LOCATION_COLOR
  const style = { backgroundColor: base, borderColor: darken(base, 0.18) }

  return (
    <span className="inline-flex min-w-0 items-center gap-2">
      <span
        className="h-3.5 w-3.5 shrink-0 rounded border"
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
    <div className={`rounded-lg border border-white/10 bg-white/[0.04] ${compact ? 'mx-3 mb-2 px-3 py-2 text-xs' : 'px-3 py-2 text-sm'}`}>
      <div className={`flex ${compact ? 'gap-4 overflow-x-auto pb-0.5' : 'flex-wrap gap-x-5 gap-y-2'}`}>
        <div className="flex min-w-fit items-center gap-2">
          <span className="text-xs font-semibold uppercase text-neutral-500 dark:text-neutral-500">
            {t('calendar.locationLegend')}
          </span>
          {activeLocations.length > 0 ? (
            <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
              {activeLocations.map((location) => (
                <Swatch
                  key={location.id}
                  color={location.color}
                  label={i18n.language === 'cs' ? location.nameCs : (location.nameEn || location.nameCs)}
                />
              ))}
            </div>
          ) : (
            <Swatch color={NEUTRAL_LOCATION_COLOR} label={t('calendar.noLocationsLegend')} />
          )}
        </div>
      </div>
    </div>
  )
}
