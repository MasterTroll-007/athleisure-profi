import {
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type ReactNode,
  type RefObject,
} from 'react'
import { createPortal } from 'react-dom'
import { CalendarDays, ChevronLeft, ChevronRight, Clock } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/utils/cn'

interface PickerBaseProps {
  label?: string
  value: string
  onChange: (value: string) => void
  min?: string
  max?: string
  id?: string
  name?: string
  error?: string
  disabled?: boolean
  required?: boolean
  className?: string
  placeholder?: string
}

interface PickerPanelProps {
  open: boolean
  anchorRef: RefObject<HTMLElement>
  panelRef: RefObject<HTMLDivElement>
  estimatedHeight: number
  minWidth?: number
  children: ReactNode
}

interface PickerPositionState {
  style: CSSProperties
  ready: boolean
  placement: 'top' | 'bottom'
}

interface DismissPickerOptions {
  fromAnchorHit?: boolean
}

const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max)

const parseDateValue = (value: string): Date | null => {
  const [year, month, day] = value.split('-').map(Number)
  if (!year || !month || !day) return null

  const date = new Date(year, month - 1, day)
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) {
    return null
  }
  return date
}

const formatDateValue = (date: Date): string => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const addMonths = (date: Date, months: number): Date =>
  new Date(date.getFullYear(), date.getMonth() + months, 1)

const isValueAllowed = (value: string, min?: string, max?: string) =>
  (!min || value >= min) && (!max || value <= max)

const parseTimeToMinutes = (time?: string): number | null => {
  if (!time) return null
  const [hours, minutes] = time.split(':').map(Number)
  if (!Number.isFinite(hours) || !Number.isFinite(minutes)) return null
  if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) return null
  return hours * 60 + minutes
}

const formatTimeValue = (totalMinutes: number): string => {
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`
}

const TIME_STEP_MINUTES = 15

const buildMinuteCandidates = (...extraMinutes: Array<number | null | undefined>): number[] => {
  const values = new Set(
    Array.from({ length: 60 / TIME_STEP_MINUTES }, (_, index) => index * TIME_STEP_MINUTES)
  )
  extraMinutes.forEach((minute) => {
    if (Number.isInteger(minute) && minute !== null && minute !== undefined && minute >= 0 && minute <= 59) {
      values.add(minute)
    }
  })
  return [...values].sort((a, b) => a - b)
}

const isTimeAllowed = (hour: number, minute: number, minMinutes: number, maxMinutes: number) => {
  const totalMinutes = hour * 60 + minute
  return totalMinutes >= minMinutes && totalMinutes <= maxMinutes
}

const centerOptionInScrollContainer = (container: HTMLElement | null, selector: string) => {
  if (!container) return
  const selected = container.querySelector<HTMLElement>(selector)
  if (!selected) return

  const containerRect = container.getBoundingClientRect()
  const selectedRect = selected.getBoundingClientRect()
  const containerCenter = containerRect.top + container.clientHeight / 2
  const selectedCenter = selectedRect.top + selectedRect.height / 2
  const maxScrollTop = Math.max(0, container.scrollHeight - container.clientHeight)

  container.scrollTop = clamp(
    container.scrollTop + selectedCenter - containerCenter,
    0,
    maxScrollTop
  )
}

const getValidMinutesForHour = (
  hour: number,
  minuteCandidates: number[],
  minMinutes: number,
  maxMinutes: number
) => minuteCandidates.filter((minute) => isTimeAllowed(hour, minute, minMinutes, maxMinutes))

function usePickerPosition(
  open: boolean,
  anchorRef: RefObject<HTMLElement>,
  estimatedHeight: number,
  minWidth = 320
) {
  const [position, setPosition] = useState<PickerPositionState>({
    ready: false,
    placement: 'bottom',
    style: {
      left: 0,
      top: 0,
      width: minWidth,
      maxHeight: estimatedHeight,
      visibility: 'hidden',
    },
  })

  useLayoutEffect(() => {
    if (!open) {
      setPosition((current) =>
        current.ready
          ? {
              ...current,
              ready: false,
              style: {
                ...current.style,
                visibility: 'hidden',
              },
            }
          : current
      )
      return undefined
    }

    const updatePosition = () => {
      const anchor = anchorRef.current
      if (!anchor) return

      const rect = anchor.getBoundingClientRect()
      const margin = 12
      const width = Math.min(
        Math.max(rect.width, minWidth),
        window.innerWidth - margin * 2
      )
      const left = clamp(rect.left, margin, window.innerWidth - width - margin)
      const spaceBelow = window.innerHeight - rect.bottom - margin
      const shouldOpenBelow = spaceBelow >= estimatedHeight || rect.top < estimatedHeight + margin
      const top = shouldOpenBelow ? rect.bottom + 8 : Math.max(margin, rect.top - estimatedHeight - 8)

      setPosition({
        ready: true,
        placement: shouldOpenBelow ? 'bottom' : 'top',
        style: {
          left,
          top,
          width,
          maxHeight: Math.min(estimatedHeight, window.innerHeight - margin * 2),
          visibility: 'visible',
        },
      })
    }

    updatePosition()
    const frameId = window.requestAnimationFrame(updatePosition)
    const settleTimeoutId = window.setTimeout(updatePosition, 240)
    window.addEventListener('resize', updatePosition)
    window.addEventListener('scroll', updatePosition, true)
    return () => {
      window.cancelAnimationFrame(frameId)
      window.clearTimeout(settleTimeoutId)
      window.removeEventListener('resize', updatePosition)
      window.removeEventListener('scroll', updatePosition, true)
    }
  }, [anchorRef, estimatedHeight, minWidth, open])

  return position
}

function useDismissPicker(
  open: boolean,
  anchorRef: RefObject<HTMLElement>,
  panelRef: RefObject<HTMLElement>,
  onClose: (options?: DismissPickerOptions) => void
) {
  useEffect(() => {
    if (!open) return undefined

    const handlePointerDown = (event: PointerEvent) => {
      const target = event.target as Node
      const anchor = anchorRef.current
      if (anchor?.contains(target) || panelRef.current?.contains(target)) return

      if (anchor) {
        const rect = anchor.getBoundingClientRect()
        const isAnchorHit =
          event.clientX >= rect.left &&
          event.clientX <= rect.right &&
          event.clientY >= rect.top &&
          event.clientY <= rect.bottom

        if (isAnchorHit) {
          event.preventDefault()
          event.stopPropagation()
          onClose({ fromAnchorHit: true })
          return
        }
      }

      onClose()
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return
      event.preventDefault()
      event.stopPropagation()
      onClose()
    }

    document.addEventListener('pointerdown', handlePointerDown)
    document.addEventListener('keydown', handleKeyDown, true)
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown)
      document.removeEventListener('keydown', handleKeyDown, true)
    }
  }, [anchorRef, onClose, open, panelRef])
}

function PickerPanel({
  open,
  anchorRef,
  panelRef,
  estimatedHeight,
  minWidth,
  children,
}: PickerPanelProps) {
  const [entered, setEntered] = useState(false)
  const position = usePickerPosition(open, anchorRef, estimatedHeight, minWidth)

  useEffect(() => {
    if (!open) return undefined

    const anchor = anchorRef.current
    document.dispatchEvent(
      new CustomEvent('app:picker-open-change', {
        detail: {
          open: true,
          anchor,
        },
      })
    )

    return () => {
      document.dispatchEvent(
        new CustomEvent('app:picker-open-change', {
          detail: {
            open: false,
            anchor,
          },
        })
      )
    }
  }, [anchorRef, open])

  useEffect(() => {
    if (!open || !position.ready) {
      setEntered(false)
      return undefined
    }

    const frameId = window.requestAnimationFrame(() => setEntered(true))
    return () => window.cancelAnimationFrame(frameId)
  }, [open, position.ready])

  if (!open) return null

  return createPortal(
    <div className="fixed inset-0 z-[90] pointer-events-none">
      <div className="absolute inset-0 pointer-events-auto bg-transparent" />
      <div
        ref={panelRef}
        data-testid="picker-panel"
        data-placement={position.placement}
        style={position.style}
        className={cn(
          'pointer-events-auto fixed overflow-y-auto overscroll-contain rounded-xl border border-white/15 bg-[#07060d]/95 text-white shadow-[0_28px_90px_-38px_rgba(0,0,0,0.95)] backdrop-blur-xl',
          'transition-[opacity,transform] duration-150 ease-out will-change-[opacity,transform]',
          entered && position.ready
            ? 'translate-y-0 scale-100 opacity-100'
            : position.placement === 'bottom'
              ? '-translate-y-1 scale-[0.985] opacity-0'
              : 'translate-y-1 scale-[0.985] opacity-0'
        )}
      >
        {children}
      </div>
    </div>,
    document.body
  )
}

function PickerLabel({ id, label }: { id?: string; label?: string }) {
  if (!label) return null

  return (
    <label htmlFor={id} className="block text-sm font-medium text-white/75 mb-1.5">
      {label}
    </label>
  )
}

function PickerTrigger({
  id,
  triggerRef,
  label,
  value,
  placeholder,
  icon,
  open,
  error,
  disabled,
  required,
  className,
  onClick,
}: {
  id?: string
  triggerRef?: RefObject<HTMLButtonElement>
  label?: string
  value: string
  placeholder: string
  icon: ReactNode
  open: boolean
  error?: string
  disabled?: boolean
  required?: boolean
  className?: string
  onClick: () => void
}) {
  return (
    <div className="w-full">
      <PickerLabel id={id} label={label} />
      <button
        ref={triggerRef}
        id={id}
        type="button"
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-invalid={!!error}
        aria-required={required}
        disabled={disabled}
        onClick={onClick}
        className={cn(
          'group relative flex min-h-[48px] w-full min-w-0 items-center justify-between gap-3 rounded-lg border px-4 py-3 text-left transition-colors',
          'bg-white/10 text-white shadow-[inset_0_1px_0_rgba(255,255,255,0.08)]',
          'focus:outline-none focus:ring-2 focus:ring-primary-300 focus:border-transparent',
          'disabled:cursor-not-allowed disabled:opacity-60',
          error ? 'border-red-400 focus:ring-red-400' : 'border-white/10 hover:border-white/24',
          open && 'border-primary-300 ring-2 ring-primary-300',
          className
        )}
      >
        <span className={cn('truncate text-sm font-medium', value ? 'text-white' : 'text-white/42')}>
          {value || placeholder}
        </span>
        <span className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg border border-white/10 bg-white/8 text-primary-200 transition-colors group-hover:bg-white/12">
          {icon}
        </span>
      </button>
      {error && <p className="mt-1.5 text-sm text-red-500">{error}</p>}
    </div>
  )
}

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
}: PickerBaseProps) {
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)
  const suppressNextTriggerClickRef = useRef(false)
  const anchorRef = useRef<HTMLButtonElement>(null)
  const panelRef = useRef<HTMLDivElement>(null)
  const hourListRef = useRef<HTMLDivElement>(null)
  const minuteListRef = useRef<HTMLDivElement>(null)
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
    const parsedMinute = valueMinutes === null ? null : valueMinutes % 60
    return buildMinuteCandidates(parsedMinute, minMinutes % 60, maxMinutes % 60)
  }, [maxMinutes, minMinutes, valueMinutes])

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

  useLayoutEffect(() => {
    if (!open) return undefined

    const scrollToSelection = () => {
      centerOptionInScrollContainer(hourListRef.current, `[data-time-hour="${draftHour}"]`)
      centerOptionInScrollContainer(minuteListRef.current, `[data-time-minute="${draftMinute}"]`)
    }

    scrollToSelection()
    let timeoutId: number | undefined
    const firstFrame = window.requestAnimationFrame(() => {
      scrollToSelection()
      timeoutId = window.setTimeout(scrollToSelection, 0)
    })

    return () => {
      window.cancelAnimationFrame(firstFrame)
      if (timeoutId !== undefined) window.clearTimeout(timeoutId)
    }
  }, [draftHour, draftMinute, hourOptions.length, minuteOptions.length, open])

  const commitTime = (hour: number, minute: number) => {
    if (!isTimeAllowed(hour, minute, minMinutes, maxMinutes)) return
    onChange(formatTimeValue(hour * 60 + minute))
  }

  const handleHourSelect = (hour: number) => {
    const validMinutes = getValidMinutesForHour(hour, minuteCandidates, minMinutes, maxMinutes)
    const nextMinute = validMinutes.includes(draftMinute) ? draftMinute : validMinutes[0]
    if (nextMinute === undefined) return

    setDraftHour(hour)
    setDraftMinute(nextMinute)
    commitTime(hour, nextMinute)
  }

  const handleMinuteSelect = (minute: number) => {
    setDraftMinute(minute)
    commitTime(draftHour, minute)
  }

  return (
    <div className="relative">
      {name && <input type="hidden" name={name} value={value} required={required} />}
      <PickerTrigger
        id={id}
        triggerRef={anchorRef}
        label={label}
        value={value}
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
      <PickerPanel open={open} anchorRef={anchorRef} panelRef={panelRef} estimatedHeight={390} minWidth={300}>
        <div className="p-3 sm:p-4">
          <div className="mb-3 flex items-center justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.08em] text-white/42">
                {label ?? t('reservation.time')}
              </p>
              <p className="mt-0.5 text-lg font-semibold text-white">{value || '--:--'}</p>
            </div>
            <span className="flex h-10 w-10 items-center justify-center rounded-lg border border-white/10 bg-white/8 text-primary-200">
              <Clock size={18} />
            </span>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="min-w-0">
              <p className="mb-2 text-center text-[11px] font-semibold uppercase tracking-[0.08em] text-white/42">
                HH
              </p>
              <div
                ref={hourListRef}
                data-testid="time-picker-hours"
                className="h-[136px] overflow-y-auto rounded-lg border border-white/10 bg-white/[0.04] p-1"
              >
                {hourOptions.map((hour) => {
                  const isSelected = draftHour === hour
                  const hourLabel = String(hour).padStart(2, '0')
                  return (
                    <button
                      key={hour}
                      type="button"
                      aria-label={`HH ${hourLabel}`}
                      data-time-hour={hour}
                      onClick={() => handleHourSelect(hour)}
                      className={cn(
                        'mb-1 flex min-h-[42px] w-full items-center justify-center rounded-md border px-2 text-sm font-semibold tabular-nums transition-colors last:mb-0',
                        isSelected
                          ? 'btn-metal btn-metal-gold border-primary-200 text-neutral-950'
                          : 'border-transparent bg-white/[0.04] text-white/78 hover:border-white/12 hover:bg-white/[0.1]'
                      )}
                    >
                      <span className="relative z-10">{hourLabel}</span>
                    </button>
                  )
                })}
              </div>
            </div>

            <div className="min-w-0">
              <p className="mb-2 text-center text-[11px] font-semibold uppercase tracking-[0.08em] text-white/42">
                MM
              </p>
              <div
                ref={minuteListRef}
                data-testid="time-picker-minutes"
                className="h-[136px] overflow-y-auto rounded-lg border border-white/10 bg-white/[0.04] p-1"
              >
                {minuteOptions.map((minute) => {
                  const isSelected = draftMinute === minute
                  const minuteLabel = String(minute).padStart(2, '0')
                  return (
                    <button
                      key={minute}
                      type="button"
                      aria-label={`MM ${minuteLabel}`}
                      data-time-minute={minute}
                      onClick={() => handleMinuteSelect(minute)}
                      className={cn(
                        'mb-1 flex min-h-[42px] w-full items-center justify-center rounded-md border px-2 text-sm font-semibold tabular-nums transition-colors last:mb-0',
                        isSelected
                          ? 'btn-metal btn-metal-gold border-primary-200 text-neutral-950'
                          : 'border-transparent bg-white/[0.04] text-white/78 hover:border-white/12 hover:bg-white/[0.1]'
                      )}
                    >
                      <span className="relative z-10">{minuteLabel}</span>
                    </button>
                  )
                })}
              </div>
            </div>
          </div>

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
