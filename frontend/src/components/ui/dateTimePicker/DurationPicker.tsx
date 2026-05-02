import { useEffect, useMemo, useRef, useState } from 'react'
import { Clock } from 'lucide-react'
import { WheelPicker, WheelPickerWrapper, type WheelPickerOption } from '@ncdai/react-wheel-picker'
import '@ncdai/react-wheel-picker/style.css'
import { useTranslation } from 'react-i18next'
import type { DurationPickerProps } from './types'
import { PickerPanel, PickerTrigger } from './dateTimePickerChrome'
import { TIME_STEP_MINUTES, buildDurationValues, formatDurationDisplay } from './dateTimePickerUtils'
import { useDismissPicker } from './useDismissPicker'

export function DurationPicker({
  label,
  value,
  onChange,
  min = 15,
  max = 480,
  minuteStep = TIME_STEP_MINUTES,
  values,
  hourUnitLabel = 'h',
  minuteUnitLabel = 'min',
  id,
  name,
  error,
  disabled,
  required,
  className,
  placeholder,
}: DurationPickerProps) {
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)
  const suppressNextTriggerClickRef = useRef(false)
  const anchorRef = useRef<HTMLButtonElement>(null)
  const panelRef = useRef<HTMLDivElement>(null)

  const durationValues = useMemo(
    () => buildDurationValues(values, min, max, minuteStep),
    [max, min, minuteStep, values]
  )
  const normalizedValue = durationValues.includes(value)
    ? value
    : durationValues[0] ?? Math.max(0, min)
  const valueHour = Math.floor(normalizedValue / 60)
  const valueMinute = normalizedValue % 60
  const [draftHour, setDraftHour] = useState(valueHour)
  const [draftMinute, setDraftMinute] = useState(valueMinute)

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
    if (!open) return
    setDraftHour(valueHour)
    setDraftMinute(valueMinute)
  }, [open, valueHour, valueMinute])

  const hourOptions = useMemo(
    () => [...new Set(durationValues.map((minutes) => Math.floor(minutes / 60)))],
    [durationValues]
  )

  const minuteOptions = useMemo(
    () => durationValues
      .filter((minutes) => Math.floor(minutes / 60) === draftHour)
      .map((minutes) => minutes % 60),
    [draftHour, durationValues]
  )

  const hourWheelOptions = useMemo<WheelPickerOption<number>[]>(
    () => hourOptions.map((hour) => {
      const optionLabel = String(hour).padStart(2, '0')
      return { value: hour, label: optionLabel, textValue: optionLabel }
    }),
    [hourOptions]
  )

  const minuteWheelOptions = useMemo<WheelPickerOption<number>[]>(
    () => minuteOptions.map((minute) => {
      const optionLabel = String(minute).padStart(2, '0')
      return { value: minute, label: optionLabel, textValue: optionLabel }
    }),
    [minuteOptions]
  )

  const commitDuration = (hour: number, minute: number) => {
    const nextValue = hour * 60 + minute
    if (!durationValues.includes(nextValue)) return
    onChange(nextValue)
  }

  const handleHourSelect = (hour: number) => {
    const minutesForHour = durationValues
      .filter((minutes) => Math.floor(minutes / 60) === hour)
      .map((minutes) => minutes % 60)
    const nextMinute = minutesForHour.includes(draftMinute) ? draftMinute : minutesForHour[0]
    if (nextMinute === undefined) return

    setDraftHour(hour)
    setDraftMinute(nextMinute)
    commitDuration(hour, nextMinute)
  }

  const handleMinuteSelect = (minute: number) => {
    setDraftMinute(minute)
    commitDuration(draftHour, minute)
  }

  const displayValue = durationValues.length
    ? formatDurationDisplay(normalizedValue, hourUnitLabel, minuteUnitLabel)
    : ''

  return (
    <div className="relative">
      {name && <input type="hidden" name={name} value={normalizedValue} required={required} />}
      <PickerTrigger
        id={id}
        triggerRef={anchorRef}
        label={label}
        value={displayValue}
        placeholder={placeholder ?? label ?? t('reservation.time')}
        icon={<Clock size={18} />}
        open={open}
        error={error}
        disabled={disabled || durationValues.length === 0}
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
      <PickerPanel open={open} anchorRef={anchorRef} panelRef={panelRef} estimatedHeight={390} minWidth={300}>
        <div className="p-3 sm:p-4">
          <div className="mb-3 flex items-center justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.08em] text-white/42">
                {label ?? t('reservation.time')}
              </p>
              <p className="mt-0.5 text-lg font-semibold text-white">{displayValue || '--'}</p>
            </div>
            <span className="flex h-10 w-10 items-center justify-center rounded-lg border border-white/10 bg-white/8 text-primary-200">
              <Clock size={18} />
            </span>
          </div>

          <WheelPickerWrapper className="gap-3 rounded-lg border border-white/10 bg-white/[0.04] p-1.5">
            <div className="min-w-0 flex-1" data-testid="duration-picker-hours">
              <p className="mb-1.5 text-center text-[11px] font-semibold uppercase tracking-[0.08em] text-white/42">
                {hourUnitLabel}
              </p>
              <WheelPicker
                value={draftHour}
                onValueChange={handleHourSelect}
                options={hourWheelOptions}
                infinite={hourWheelOptions.length > 1}
                visibleCount={12}
                optionItemHeight={36}
                dragSensitivity={3}
                scrollSensitivity={5}
                classNames={{
                  optionItem: 'text-white/50 text-sm font-semibold tabular-nums',
                  highlightWrapper: 'rounded-md border border-primary-200/45 bg-[linear-gradient(180deg,rgba(255,255,255,0.74)_0%,rgba(255,255,255,0.24)_35%,rgba(255,255,255,0.08)_100%),linear-gradient(180deg,#fff0c2_0%,#d9b56d_58%,#ecd09a_100%)] shadow-[inset_0_1px_0_rgba(255,255,255,0.78),0_12px_26px_-18px_rgba(255,179,71,0.45)]',
                  highlightItem: 'text-sm font-semibold tabular-nums text-neutral-950 [text-shadow:0_1px_0_rgba(255,255,255,0.35)]',
                }}
              />
            </div>

            <div className="min-w-0 flex-1" data-testid="duration-picker-minutes">
              <p className="mb-1.5 text-center text-[11px] font-semibold uppercase tracking-[0.08em] text-white/42">
                {minuteUnitLabel}
              </p>
              <WheelPicker
                value={draftMinute}
                onValueChange={handleMinuteSelect}
                options={minuteWheelOptions}
                infinite={minuteWheelOptions.length > 1}
                visibleCount={12}
                optionItemHeight={36}
                dragSensitivity={3}
                scrollSensitivity={5}
                classNames={{
                  optionItem: 'text-white/50 text-sm font-semibold tabular-nums',
                  highlightWrapper: 'rounded-md border border-primary-200/45 bg-[linear-gradient(180deg,rgba(255,255,255,0.74)_0%,rgba(255,255,255,0.24)_35%,rgba(255,255,255,0.08)_100%),linear-gradient(180deg,#fff0c2_0%,#d9b56d_58%,#ecd09a_100%)] shadow-[inset_0_1px_0_rgba(255,255,255,0.78),0_12px_26px_-18px_rgba(255,179,71,0.45)]',
                  highlightItem: 'text-sm font-semibold tabular-nums text-neutral-950 [text-shadow:0_1px_0_rgba(255,255,255,0.35)]',
                }}
              />
            </div>
          </WheelPickerWrapper>

          <div className="mt-3 flex justify-end">
            <button
              type="button"
              onClick={() => setOpen(false)}
              className="btn-metal btn-metal-silver min-h-[40px] rounded-lg px-4 py-2 text-sm font-medium"
            >
              <span className="btn-content relative z-10">{t('common.confirm')}</span>
            </button>
          </div>
        </div>
      </PickerPanel>
    </div>
  )
}
