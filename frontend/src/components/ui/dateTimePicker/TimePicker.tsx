import { useEffect, useMemo, useRef, useState } from 'react'
import { Clock } from 'lucide-react'
import { WheelPicker, WheelPickerWrapper, type WheelPickerOption } from '@ncdai/react-wheel-picker'
import '@ncdai/react-wheel-picker/style.css'
import { useTranslation } from 'react-i18next'
import { cn } from '@/utils/cn'
import type { PickerBaseProps } from './types'
import { PickerPanel, PickerTrigger } from './dateTimePickerChrome'
import {
  TIME_STEP_MINUTES,
  buildMinuteCandidates,
  formatTimeValue,
  getValidMinutesForHour,
  isTimeAllowed,
  parseTimeToMinutes,
} from './dateTimePickerUtils'
import { useDismissPicker } from './useDismissPicker'

export function TimePicker({
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
  minuteStep = TIME_STEP_MINUTES,
  hourOnly = false,
}: PickerBaseProps) {
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)
  const suppressNextTriggerClickRef = useRef(false)
  const anchorRef = useRef<HTMLButtonElement>(null)
  const panelRef = useRef<HTMLDivElement>(null)
  const minMinutes = parseTimeToMinutes(min) ?? 0
  const maxMinutes = parseTimeToMinutes(max) ?? 23 * 60 + 45
  const valueMinutes = parseTimeToMinutes(value)

  const fallbackMinutes =
    valueMinutes !== null && valueMinutes >= minMinutes && valueMinutes <= maxMinutes
      ? valueMinutes
      : minMinutes
  const valueHour = Math.floor(fallbackMinutes / 60)
  const valueMinute = fallbackMinutes % 60
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

  const minuteCandidates = useMemo(() => {
    if (hourOnly) return [0]
    const parsedMinute = valueMinutes === null ? null : valueMinutes % 60
    return buildMinuteCandidates(minuteStep, parsedMinute, minMinutes % 60, maxMinutes % 60)
  }, [hourOnly, maxMinutes, minMinutes, minuteStep, valueMinutes])

  const hourOptions = useMemo(() => {
    const hours: number[] = []
    for (let hour = 0; hour <= 23; hour += 1) {
      if (getValidMinutesForHour(hour, minuteCandidates, minMinutes, maxMinutes).length > 0) {
        hours.push(hour)
      }
    }
    return hours
  }, [maxMinutes, minMinutes, minuteCandidates])

  const minuteOptions = useMemo(
    () => getValidMinutesForHour(draftHour, minuteCandidates, minMinutes, maxMinutes),
    [draftHour, maxMinutes, minMinutes, minuteCandidates]
  )

  const hourWheelOptions = useMemo<WheelPickerOption<number>[]>(
    () => hourOptions.map((hour) => {
      const label = String(hour).padStart(2, '0')
      return { value: hour, label, textValue: label }
    }),
    [hourOptions]
  )

  const minuteWheelOptions = useMemo<WheelPickerOption<number>[]>(
    () => minuteOptions.map((minute) => {
      const label = String(minute).padStart(2, '0')
      return { value: minute, label, textValue: label }
    }),
    [minuteOptions]
  )

  const commitTime = (hour: number, minute: number) => {
    if (!isTimeAllowed(hour, minute, minMinutes, maxMinutes)) return
    onChange(formatTimeValue(hour * 60 + minute))
  }

  const handleHourSelect = (hour: number) => {
    const validMinutes = getValidMinutesForHour(hour, minuteCandidates, minMinutes, maxMinutes)
    const nextMinute = hourOnly && validMinutes.includes(0)
      ? 0
      : validMinutes.includes(draftMinute)
        ? draftMinute
        : validMinutes[0]
    if (nextMinute === undefined) return

    setDraftHour(hour)
    setDraftMinute(nextMinute)
    commitTime(hour, nextMinute)
  }

  const handleMinuteSelect = (minute: number) => {
    setDraftMinute(minute)
    commitTime(draftHour, minute)
  }

  const displayValue = hourOnly && valueMinutes !== null
    ? formatTimeValue(valueHour * 60)
    : value

  return (
    <div className="relative">
      {name && <input type="hidden" name={name} value={value} required={required} />}
      <PickerTrigger
        id={id}
        triggerRef={anchorRef}
        label={label}
        value={displayValue}
        placeholder={placeholder ?? t('reservation.time')}
        icon={<Clock size={18} />}
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
      <PickerPanel open={open} anchorRef={anchorRef} panelRef={panelRef} estimatedHeight={390} minWidth={hourOnly ? 220 : 300}>
        <div className="p-3 sm:p-4">
          <div className="mb-3 flex items-center justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.08em] text-white/42">
                {label ?? t('reservation.time')}
              </p>
              <p className="mt-0.5 text-lg font-semibold text-white">{displayValue || '--:--'}</p>
            </div>
            <span className="flex h-10 w-10 items-center justify-center rounded-lg border border-white/10 bg-white/8 text-primary-200">
              <Clock size={18} />
            </span>
          </div>

          <WheelPickerWrapper className={cn('rounded-lg border border-white/10 bg-white/[0.04] p-1.5', !hourOnly && 'gap-3')}>
            <div className="min-w-0 flex-1" data-testid="time-picker-hours">
              <p className="mb-1.5 text-center text-[11px] font-semibold uppercase tracking-[0.08em] text-white/42">
                HH
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

            {!hourOnly && (
              <div className="min-w-0 flex-1" data-testid="time-picker-minutes">
                <p className="mb-1.5 text-center text-[11px] font-semibold uppercase tracking-[0.08em] text-white/42">
                  MM
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
            )}
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

