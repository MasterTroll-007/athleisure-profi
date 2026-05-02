export const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max)

export const parseDateValue = (value: string): Date | null => {
  const [year, month, day] = value.split('-').map(Number)
  if (!year || !month || !day) return null

  const date = new Date(year, month - 1, day)
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) {
    return null
  }
  return date
}

export const formatDateValue = (date: Date): string => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export const addMonths = (date: Date, months: number): Date =>
  new Date(date.getFullYear(), date.getMonth() + months, 1)

export const isValueAllowed = (value: string, min?: string, max?: string) =>
  (!min || value >= min) && (!max || value <= max)

export const parseTimeToMinutes = (time?: string): number | null => {
  if (!time) return null
  const [hours, minutes] = time.split(':').map(Number)
  if (!Number.isFinite(hours) || !Number.isFinite(minutes)) return null
  if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) return null
  return hours * 60 + minutes
}

export const formatTimeValue = (totalMinutes: number): string => {
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`
}

export const TIME_STEP_MINUTES = 15

export const buildDurationValues = (
  values: number[] | undefined,
  min: number,
  max: number,
  step: number
) => {
  const safeMin = Math.max(0, Math.floor(min))
  const safeMax = Math.max(safeMin, Math.floor(max))
  const safeStep = Number.isInteger(step) && step > 0 ? step : TIME_STEP_MINUTES

  if (values?.length) {
    return [...new Set(values.map((value) => Math.floor(value)))]
      .filter((value) => value >= safeMin && value <= safeMax)
      .sort((a, b) => a - b)
  }

  const result: number[] = []
  for (let value = safeMin; value <= safeMax; value += safeStep) {
    result.push(value)
  }
  return result
}

export const formatDurationDisplay = (minutesTotal: number, hourLabel: string, minuteLabel: string) => {
  const hours = Math.floor(minutesTotal / 60)
  const minutes = minutesTotal % 60
  if (hours > 0 && minutes > 0) return `${hours} ${hourLabel} ${minutes} ${minuteLabel}`
  if (hours > 0) return `${hours} ${hourLabel}`
  return `${minutes} ${minuteLabel}`
}

export const buildMinuteCandidates = (
  step: number,
  ...extraMinutes: Array<number | null | undefined>
): number[] => {
  const safeStep = Number.isInteger(step) && step > 0 && step <= 60 && 60 % step === 0
    ? step
    : TIME_STEP_MINUTES
  const values = new Set(
    Array.from({ length: 60 / safeStep }, (_, index) => index * safeStep)
  )
  extraMinutes.forEach((minute) => {
    if (Number.isInteger(minute) && minute !== null && minute !== undefined && minute >= 0 && minute <= 59) {
      values.add(minute)
    }
  })
  return [...values].sort((a, b) => a - b)
}

export const isTimeAllowed = (hour: number, minute: number, minMinutes: number, maxMinutes: number) => {
  const totalMinutes = hour * 60 + minute
  return totalMinutes >= minMinutes && totalMinutes <= maxMinutes
}

export const getValidMinutesForHour = (
  hour: number,
  minuteCandidates: number[],
  minMinutes: number,
  maxMinutes: number
) => minuteCandidates.filter((minute) => isTimeAllowed(hour, minute, minMinutes, maxMinutes))

