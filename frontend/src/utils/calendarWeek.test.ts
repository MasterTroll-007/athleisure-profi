import { describe, expect, it } from 'vitest'
import { getWeekStartForVisibleRange } from './calendarWeek'

describe('calendarWeek', () => {
  it('uses the week containing a one-day view', () => {
    expect(getWeekStartForVisibleRange({ start: '2026-04-28', end: '2026-04-28' })).toBe('2026-04-27')
  })

  it('uses the displayed Monday for a full Monday-Sunday week', () => {
    expect(getWeekStartForVisibleRange({ start: '2026-04-27', end: '2026-05-03' })).toBe('2026-04-27')
  })

  it('uses the week with more visible days in a three-day range crossing into next week', () => {
    expect(getWeekStartForVisibleRange({ start: '2026-05-03', end: '2026-05-05' })).toBe('2026-05-04')
  })

  it('keeps a five-day range on the previous week when four visible days are still there', () => {
    expect(getWeekStartForVisibleRange({ start: '2026-04-30', end: '2026-05-04' })).toBe('2026-04-27')
  })

  it('moves a five-day range to the next week when three visible days are there', () => {
    expect(getWeekStartForVisibleRange({ start: '2026-05-02', end: '2026-05-06' })).toBe('2026-05-04')
  })
})
