import { useEffect, useMemo, useRef, useState } from 'react'
import { CalendarDays, ChevronLeft, ChevronRight } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/utils/cn'
import type { PickerBaseProps } from './types'
import { PickerPanel, PickerTrigger } from './dateTimePickerChrome'
import { addMonths, formatDateValue, isValueAllowed, parseDateValue } from './dateTimePickerUtils'
import { useDismissPicker } from './useDismissPicker'

export function DatePicker({
  label,
  value,
  onChange,
  min,
  max,
  id,
  name,
  error,
  disabled,
  required,
  className,
  placeholder,
}: PickerBaseProps) {
  const { t, i18n } = useTranslation()
  const [open, setOpen] = useState(false)
  const suppressNextTriggerClickRef = useRef(false)
  const anchorRef = useRef<HTMLButtonElement>(null)
  const panelRef = useRef<HTMLDivElement>(null)
  const selectedDate = useMemo(() => parseDateValue(value), [value])
  const [viewDate, setViewDate] = useState<Date>(selectedDate ?? new Date())
  const locale = i18n.language || undefined

  useDismissPicker(open, anchorRef, panelRef, (options) => {
    if (options?.fromAnchorHit) {
      suppressNextTriggerClickRef.current = true
      window.setTimeout(() => {
        suppressNextTriggerClickRef.current = false
      }, 250)
    }
    setOpen(false)
  })

  useEffect(() => {
    if (open) setViewDate(selectedDate ?? new Date())
  }, [open, selectedDate])

  const displayValue = selectedDate
    ? new Intl.DateTimeFormat(locale, {
        weekday: 'short',
        day: 'numeric',
        month: 'short',
        year: 'numeric',
      }).format(selectedDate)
    : ''

  const monthLabel = new Intl.DateTimeFormat(locale, {
    month: 'long',
    year: 'numeric',
  }).format(viewDate)

  const weekdays = useMemo(() => {
    const monday = new Date(2024, 0, 1)
    return Array.from({ length: 7 }, (_, index) =>
      new Intl.DateTimeFormat(locale, { weekday: 'short' }).format(
        new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + index)
      )
    )
  }, [locale])

  const days = useMemo(() => {
    const firstOfMonth = new Date(viewDate.getFullYear(), viewDate.getMonth(), 1)
    const mondayOffset = (firstOfMonth.getDay() + 6) % 7
    const gridStart = new Date(firstOfMonth)
    gridStart.setDate(firstOfMonth.getDate() - mondayOffset)

    return Array.from({ length: 42 }, (_, index) => {
      const day = new Date(gridStart)
      day.setDate(gridStart.getDate() + index)
      return day
    })
  }, [viewDate])

  const today = formatDateValue(new Date())
  const canUseToday = isValueAllowed(today, min, max)

  const handleSelect = (date: Date) => {
    const nextValue = formatDateValue(date)
    if (!isValueAllowed(nextValue, min, max)) return
    onChange(nextValue)
    setOpen(false)
  }

  return (
    <div className="relative">
      {name && <input type="hidden" name={name} value={value} required={required} />}
      <PickerTrigger
        id={id}
        triggerRef={anchorRef}
        label={label}
        value={displayValue}
        placeholder={placeholder ?? t('reservation.selectDate')}
        icon={<CalendarDays size={18} />}
        open={open}
        error={error}
        disabled={disabled}
        required={required}
        className={className}
        onClick={() => {
          if (suppressNextTriggerClickRef.current) {
            suppressNextTriggerClickRef.current = false
            return
          }
          setOpen((current) => !current)
        }}
      />
      <PickerPanel open={open} anchorRef={anchorRef} panelRef={panelRef} estimatedHeight={420}>
        <div className="p-3 pb-4 sm:p-4 sm:pb-5">
          <div className="mb-2.5 flex items-center justify-between gap-3">
            <button
              type="button"
              onClick={() => setViewDate((current) => addMonths(current, -1))}
              className="flex h-10 w-10 items-center justify-center rounded-lg border border-white/10 bg-white/8 text-white/80 transition-colors hover:bg-white/14"
              aria-label={t('common.previous')}
            >
              <ChevronLeft size={18} />
            </button>
            <div className="min-w-0 text-center">
              <p className="truncate text-sm font-semibold capitalize text-white">{monthLabel}</p>
            </div>
            <button
              type="button"
              onClick={() => setViewDate((current) => addMonths(current, 1))}
              className="flex h-10 w-10 items-center justify-center rounded-lg border border-white/10 bg-white/8 text-white/80 transition-colors hover:bg-white/14"
              aria-label={t('common.next')}
            >
              <ChevronRight size={18} />
            </button>
          </div>

          <div className="grid grid-cols-7 gap-1 text-center text-[11px] font-semibold uppercase tracking-[0.08em] text-white/42">
            {weekdays.map((weekday) => (
              <div key={weekday} className="py-1">
                {weekday}
              </div>
            ))}
          </div>

          <div className="mt-1 grid grid-cols-7 gap-0.5 sm:gap-1">
            {days.map((day) => {
              const dayValue = formatDateValue(day)
              const isSelected = value === dayValue
              const isToday = today === dayValue
              const isCurrentMonth = day.getMonth() === viewDate.getMonth()
              const isDisabled = !isValueAllowed(dayValue, min, max)

              return (
                <button
                  key={dayValue}
                  type="button"
                  disabled={isDisabled}
                  onClick={() => handleSelect(day)}
                  className={cn(
                    'flex aspect-square min-h-[34px] items-center justify-center rounded-lg border text-sm font-semibold transition-colors sm:min-h-[38px]',
                    isSelected
                      ? 'btn-metal btn-metal-gold border-primary-200 text-neutral-950'
                      : 'border-transparent bg-white/5 text-white/82 hover:border-white/15 hover:bg-white/12',
                    !isCurrentMonth && !isSelected && 'border-white/[0.03] bg-neutral-800/45 text-white/20 grayscale hover:border-white/[0.06] hover:bg-neutral-800/60 hover:text-white/40',
                    isToday && !isSelected && 'border-primary-300/50 text-primary-100',
                    isDisabled && 'cursor-not-allowed opacity-25 hover:border-transparent hover:bg-white/5'
                  )}
                >
                  <span className="relative z-10">{day.getDate()}</span>
                </button>
              )
            })}
          </div>

          <div className="mt-2.5 flex justify-end border-t border-white/10 pt-2.5">
            <button
              type="button"
              disabled={!canUseToday}
              onClick={() => handleSelect(new Date())}
              className="rounded-lg border border-white/10 bg-white/8 px-3 py-2 text-sm font-medium text-white/84 transition-colors hover:bg-white/14 disabled:cursor-not-allowed disabled:opacity-40"
            >
              {t('calendar.today')}
            </button>
          </div>
        </div>
      </PickerPanel>
    </div>
  )
}

