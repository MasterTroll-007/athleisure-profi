export function formatDate(date: string, locale = 'cs'): string {
  return new Date(date).toLocaleDateString(locale, {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

export function formatShortDate(date: string, locale = 'cs'): string {
  return new Date(date).toLocaleDateString(locale, {
    day: 'numeric',
    month: 'short',
  })
}

export function formatTime(time: string): string {
  // Time comes as "HH:mm:ss" or "HH:mm"
  const parts = time.split(':')
  return `${parts[0]}:${parts[1]}`
}

export function formatDateTime(date: string, time: string, locale = 'cs'): string {
  return `${formatDate(date, locale)} ${formatTime(time)}`
}

export function formatCurrency(amount: number, currency = 'CZK'): string {
  return new Intl.NumberFormat('cs-CZ', {
    style: 'currency',
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount)
}

export function formatCredits(credits: number): string {
  if (credits === 1) return '1 kredit'
  if (credits >= 2 && credits <= 4) return `${credits} kredity`
  return `${credits} kreditů`
}

export function getDayName(dayOfWeek: number, locale = 'cs', format: 'short' | 'long' = 'short'): string {
  // dayOfWeek: 1 = Monday, 7 = Sunday
  const date = new Date(2024, 0, dayOfWeek) // January 2024 starts on Monday
  return date.toLocaleDateString(locale, { weekday: format })
}

export function getRelativeTime(date: string): string {
  const now = new Date()
  const target = new Date(date)
  const diff = target.getTime() - now.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  const hours = Math.floor(diff / (1000 * 60 * 60))

  if (days > 1) return `za ${days} dní`
  if (days === 1) return 'zítra'
  if (hours > 1) return `za ${hours} hodin`
  if (hours === 1) return 'za hodinu'
  return 'brzy'
}
