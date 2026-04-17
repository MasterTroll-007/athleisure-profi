import { describe, it, expect } from 'vitest'
import {
  isValidHex,
  hexWithAlpha,
  darken,
  lighten,
  readableTextOn,
  neutralTextForTheme,
  addMinutesToTime,
  NEUTRAL_LOCATION_COLOR,
} from './color'

describe('color utils', () => {
  describe('isValidHex', () => {
    it('accepts 6-digit hex with #', () => {
      expect(isValidHex('#3B82F6')).toBe(true)
      expect(isValidHex('#abcdef')).toBe(true)
    })

    it('rejects invalid shapes', () => {
      expect(isValidHex('#3B82F')).toBe(false) // 5 digits
      expect(isValidHex('3B82F6')).toBe(false) // missing #
      expect(isValidHex(null)).toBe(false)
      expect(isValidHex(undefined)).toBe(false)
      expect(isValidHex('')).toBe(false)
    })
  })

  describe('hexWithAlpha', () => {
    it('produces rgba with clamped alpha', () => {
      expect(hexWithAlpha('#FF0000', 0.5)).toBe('rgba(255, 0, 0, 0.5)')
      expect(hexWithAlpha('#FF0000', 1.5)).toBe('rgba(255, 0, 0, 1)')
      expect(hexWithAlpha('#FF0000', -1)).toBe('rgba(255, 0, 0, 0)')
    })

    it('falls back to neutral gray on invalid hex', () => {
      const out = hexWithAlpha('not-a-hex', 0.5)
      // Neutral gray #9CA3AF → rgb(156, 163, 175)
      expect(out).toBe('rgba(156, 163, 175, 0.5)')
    })
  })

  describe('darken / lighten', () => {
    it('darken reduces channels proportionally', () => {
      expect(darken('#FFFFFF', 0.5)).toBe('#808080')
      expect(darken('#FFFFFF', 0)).toBe('#FFFFFF')
      expect(darken('#FFFFFF', 1)).toBe('#000000')
    })

    it('lighten increases channels toward white', () => {
      expect(lighten('#000000', 0.5)).toBe('#808080')
      expect(lighten('#000000', 1)).toBe('#FFFFFF')
    })
  })

  describe('readableTextOn', () => {
    it('returns dark text on light bg', () => {
      expect(readableTextOn('#FFFFFF')).toBe('#1F2937')
      expect(readableTextOn('#FFFF00')).toBe('#1F2937') // yellow is light
    })

    it('returns white text on dark bg', () => {
      expect(readableTextOn('#000000')).toBe('#FFFFFF')
      expect(readableTextOn('#0000FF')).toBe('#FFFFFF') // blue is dark
    })
  })

  describe('neutralTextForTheme', () => {
    it('returns dark text for light theme', () => {
      expect(neutralTextForTheme('light')).toBe('#111827')
    })
    it('returns light text for dark theme', () => {
      expect(neutralTextForTheme('dark')).toBe('#F9FAFB')
    })
  })

  describe('addMinutesToTime', () => {
    it('adds minutes with hh:mm zero-padding', () => {
      expect(addMinutesToTime('09:00', 60)).toBe('10:00')
      expect(addMinutesToTime('09:15', 30)).toBe('09:45')
      expect(addMinutesToTime('23:30', 45)).toBe('24:15') // overflow acceptable
    })

    it('handles zero-minute increment', () => {
      expect(addMinutesToTime('07:00', 0)).toBe('07:00')
    })
  })

  it('exports NEUTRAL_LOCATION_COLOR constant', () => {
    expect(NEUTRAL_LOCATION_COLOR).toBe('#9CA3AF')
  })
})
