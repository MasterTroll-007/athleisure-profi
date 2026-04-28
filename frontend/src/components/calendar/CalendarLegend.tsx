import { useTranslation } from 'react-i18next'
import { Lock } from 'lucide-react'
import type { TrainingLocation } from '@/types/api'
import { NEUTRAL_LOCATION_COLOR, darken, hexWithAlpha, isValidHex } from '@/utils/color'

interface CalendarLegendProps {
  isAdmin: boolean
  locations?: TrainingLocation[]
  compact?: boolean
}

function Swatch({
  color,
  label,
  variant = 'solid',
}: {
  color: string
  label: string
  variant?: 'solid' | 'tint' | 'reserved' | 'locked' | 'striped'
}) {
  const base = isValidHex(color) ? color : NEUTRAL_LOCATION_COLOR
  const style =
    variant === 'solid'
      ? { backgroundColor: base, borderColor: darken(base, 0.18) }
      : variant === 'tint'
        ? { backgroundColor: hexWithAlpha(base, 0.2), borderColor: base }
        : variant === 'reserved'
          ? {
              backgroundColor: hexWithAlpha(base, 0.48),
              borderColor: darken(base, 0.22),
              boxShadow: `inset 0 0 0 1px ${hexWithAlpha(base, 0.24)}`,
            }
          : variant === 'striped'
            ? {
                backgroundColor: hexWithAlpha(base, 0.45),
                borderColor: '#EF4444',
                backgroundImage: 'repeating-linear-gradient(135deg, rgba(239, 68, 68, 0.35) 0 4px, transparent 4px 8px)',
              }
            : { backgroundColor: hexWithAlpha(base, 0.25), borderColor: darken(base, 0.1) }

  return (
    <span className="inline-flex min-w-0 items-center gap-2">
      <span
        className="relative h-3.5 w-3.5 shrink-0 rounded border"
        style={style}
      >
        {variant === 'locked' && (
          <Lock size={9} className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 text-white/90" />
        )}
      </span>
      <span className="truncate text-neutral-600 dark:text-neutral-400">{label}</span>
    </span>
  )
}

export function CalendarLegend({ isAdmin, locations, compact = false }: CalendarLegendProps) {
  const { t, i18n } = useTranslation()
  const activeLocations = (locations ?? [])
    .filter((location) => location.isActive && isValidHex(location.color))
    .sort((a, b) => a.nameCs.localeCompare(b.nameCs, i18n.language))
  const statusPreviewColor = activeLocations[0]?.color ?? NEUTRAL_LOCATION_COLOR

  const statusItems = isAdmin
    ? [
        { label: t('calendar.available'), variant: 'tint' as const },
        { label: t('calendar.reserved'), variant: 'solid' as const },
        { label: t('calendar.locked'), variant: 'locked' as const },
        { label: t('calendar.cancelled'), variant: 'striped' as const },
      ]
    : [
        { label: t('calendar.available'), variant: 'tint' as const },
        { label: t('calendar.yourReservation'), variant: 'solid' as const },
        { label: t('calendar.reserved'), variant: 'reserved' as const },
      ]

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

        <div className="flex min-w-fit items-center gap-2 border-white/10 md:border-l md:pl-5">
          <span className="text-xs font-semibold uppercase text-neutral-500 dark:text-neutral-500">
            {t('calendar.statusLegend')}
          </span>
          <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
            {statusItems.map((item) => (
              <Swatch
                key={item.label}
                color={statusPreviewColor}
                label={item.label}
                variant={item.variant}
              />
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
