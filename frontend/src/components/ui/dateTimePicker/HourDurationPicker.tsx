import { useEffect, useMemo, useRef, useState } from 'react'
import { Clock } from 'lucide-react'
import { WheelPicker, WheelPickerWrapper, type WheelPickerOption } from '@ncdai/react-wheel-picker'
import '@ncdai/react-wheel-picker/style.css'
import { useTranslation } from 'react-i18next'
import type { HourDurationPickerProps } from './types'
import { PickerPanel, PickerTrigger } from './dateTimePickerChrome'
import { useDismissPicker } from './useDismissPicker'

export function HourDurationPicker({
  label,
  value,
  onChange,
  min = 0,
  max = 168,
  allowNone = false,
  noneLabel,
  unitLabel = 'h',
  id,
  error,
  disabled,
  required,
  className,
  placeholder,
}: HourDurationPickerProps) {
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)
  const suppressNextTriggerClickRef = useRef(false)
  const anchorRef = useRef<HTMLButtonElement>(null)
  const panelRef = useRef<HTMLDivElement>(null)
  const normalizedMin = Math.max(0, Math.floor(min))
  const normalizedMax = Math.max(-1, Math.floor(max))
  const numericValues = useMemo(
    () =>
      normalizedMax >= normalizedMin
        ? Array.from({ length: normalizedMax - normalizedMin + 1 }, (_, index) => normalizedMin + index)
        : [],
    [normalizedMax, normalizedMin]
  )

  const noneOptionLabel = noneLabel ?? placeholder ?? t('common.none', 'Bez omezeni')
  const fallbackKey = allowNone ? 'none' : String(numericValues[0] ?? normalizedMin)
  const valueKey =
    value === null || !numericValues.includes(value)
      ? fallbackKey
      : String(value)
  const [draftKey, setDraftKey] = useState(valueKey)

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
    if (open) setDraftKey(valueKey)
  }, [open, valueKey])

  const wheelOptions = useMemo<WheelPickerOption<string>[]>(() => {
    const options = numericValues.map((hours) => {
      const optionLabel = `${hours} ${unitLabel}`
      return { value: String(hours), label: optionLabel, textValue: optionLabel }
    })

    if (!allowNone) return options

    return [
      { value: 'none', label: noneOptionLabel, textValue: noneOptionLabel },
      ...options,
    ]
  }, [allowNone, noneOptionLabel, numericValues, unitLabel])

  const displayValue = valueKey === 'none'
    ? noneOptionLabel
    : `${Number(valueKey)} ${unitLabel}`

  const handleSelect = (nextKey: string) => {
    setDraftKey(nextKey)
    onChange(nextKey === 'none' ? null : Number(nextKey))
  }

  return (
    <div className="relative">
      <PickerTrigger
        id={id}
        triggerRef={anchorRef}
        label={label}
        value={displayValue}
        placeholder={placeholder ?? label ?? t('reservation.time')}
        icon={<Clock size={18} />}
        open={open}
        error={error}
        disabled={disabled || wheelOptions.length === 0}
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
              <p className="mt-0.5 text-lg font-semibold text-white">{displayValue}</p>
            </div>
            <span className="flex h-10 w-10 items-center justify-center rounded-lg border border-white/10 bg-white/8 text-primary-200">
              <Clock size={18} />
            </span>
          </div>

          <WheelPickerWrapper className="rounded-lg border border-white/10 bg-white/[0.04] p-1.5">
            <WheelPicker
              value={draftKey}
              onValueChange={handleSelect}
              options={wheelOptions}
              infinite={wheelOptions.length > 1}
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

