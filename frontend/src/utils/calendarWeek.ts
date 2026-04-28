export interface DateRangeLike {
  start: string
  end: string
}

export function formatDateLocal(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function parseLocalDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}

function getMonday(date: Date): Date {
  const monday = new Date(date)
  const day = monday.getDay()
  const diff = day === 0 ? -6 : 1 - day
  monday.setDate(monday.getDate() + diff)
  return monday
}

export function getWeekStartForVisibleRange(range: DateRangeLike): string {
  const start = parseLocalDate(range.start)
  const end = parseLocalDate(range.end)
  const first = start <= end ? start : end
  const last = start <= end ? end : start
  const weekDayCounts = new Map<string, number>()

  for (const cursor = new Date(first); cursor <= last; cursor.setDate(cursor.getDate() + 1)) {
    const weekStart = formatDateLocal(getMonday(cursor))
    weekDayCounts.set(weekStart, (weekDayCounts.get(weekStart) ?? 0) + 1)
  }

  let bestWeekStart = formatDateLocal(getMonday(first))
  let bestCount = -1
  for (const [weekStart, count] of weekDayCounts) {
    if (count > bestCount) {
      bestWeekStart = weekStart
      bestCount = count
    }
  }

  return bestWeekStart
}
