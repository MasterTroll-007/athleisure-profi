export const HOUR_HEIGHT = 60 // px per hour (user view)
export const ADMIN_HOUR_HEIGHT = 80 // px per hour (admin view, 20px per 15min slot)
export const TIME_COL_WIDTH = 56 // px for time labels column
export const SNAP_MINUTES = 15
export const MIN_EVENT_HEIGHT = 18
export const EVENT_GAP = 2
export const SLIDE_DURATION = 300 // ms

export function formatDateISO(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function getMonday(date: Date): Date {
  const d = new Date(date)
  const day = d.getDay()
  const diff = day === 0 ? -6 : 1 - day
  d.setDate(d.getDate() + diff)
  return d
}

export function computeDays(startDate: Date, count: number): Date[] {
  const days: Date[] = []
  for (let i = 0; i < count; i++) {
    const d = new Date(startDate)
    d.setDate(d.getDate() + i)
    days.push(d)
  }
  return days
}

export function getStartDate(date: Date, viewDays: number): Date {
  if (viewDays === 7) return getMonday(date)
  return new Date(date)
}

export function isSameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
}
