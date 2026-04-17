import { describe, it, expect } from 'vitest'
import { formatDate, formatShortDate, formatTime, formatCredits, getDayName } from './formatters'

describe('formatters', () => {
  describe('formatDate — timezone-safe', () => {
    it('formats ISO date as local day without UTC shift', () => {
      // In Prague (UTC+1/+2) a bare date without Tz would shift back one day.
      // parseDateLocal appends T00:00:00 so the calendar day stays intact.
      const out = formatDate('2026-04-17', 'cs')
      expect(out.toLowerCase()).toContain('17')
      // Czech inflects "duben" → "dubna" in genitive; accept either form.
      expect(out.toLowerCase()).toMatch(/dubn[aueá]/)
    })
  })

  describe('formatShortDate', () => {
    it('returns a short day + month label', () => {
      const out = formatShortDate('2026-12-25', 'cs')
      expect(out).toContain('25')
    })
  })

  describe('formatTime', () => {
    it('strips seconds', () => {
      expect(formatTime('09:30:00')).toBe('09:30')
      expect(formatTime('09:30')).toBe('09:30')
    })

    it('handles null / undefined / empty', () => {
      expect(formatTime(null)).toBe('--:--')
      expect(formatTime(undefined)).toBe('--:--')
      expect(formatTime('')).toBe('--:--')
    })
  })

  describe('formatCredits', () => {
    it('English singular vs plural', () => {
      expect(formatCredits(1, 'en')).toBe('1 credit')
      expect(formatCredits(5, 'en')).toBe('5 credits')
    })

    it('Czech 1 / 2-4 / 5+ plural forms', () => {
      expect(formatCredits(1, 'cs')).toBe('1 kredit')
      expect(formatCredits(3, 'cs')).toBe('3 kredity')
      expect(formatCredits(10, 'cs')).toBe('10 kreditů')
    })
  })

  describe('getDayName', () => {
    it('returns day in given locale', () => {
      // Monday is day 1 in our calendar scheme
      const cs = getDayName(1, 'cs', 'short').toLowerCase()
      const en = getDayName(1, 'en', 'short').toLowerCase()
      expect(cs).toMatch(/po/)
      expect(en).toMatch(/mon/)
    })
  })
})
