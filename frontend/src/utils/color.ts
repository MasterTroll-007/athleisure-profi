/**
 * Color utilities for location-aware calendar rendering.
 * All functions accept hex strings in the format #RRGGBB (uppercase or lowercase).
 */

const HEX_PATTERN = /^#([0-9A-Fa-f]{6})$/

export function isValidHex(hex: string | null | undefined): hex is string {
  return !!hex && HEX_PATTERN.test(hex)
}

function hexToRgb(hex: string): { r: number; g: number; b: number } {
  const match = HEX_PATTERN.exec(hex)
  if (!match) return { r: 156, g: 163, b: 175 } // neutral gray fallback
  const int = parseInt(match[1], 16)
  return {
    r: (int >> 16) & 0xff,
    g: (int >> 8) & 0xff,
    b: int & 0xff,
  }
}

function clamp(v: number, min = 0, max = 255) {
  return Math.max(min, Math.min(max, v))
}

function rgbToHex(r: number, g: number, b: number): string {
  const toHex = (v: number) => clamp(Math.round(v)).toString(16).padStart(2, '0')
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`.toUpperCase()
}

/** Return the color with an alpha channel (0-1). */
export function hexWithAlpha(hex: string, alpha: number): string {
  const { r, g, b } = hexToRgb(hex)
  const a = clamp(alpha, 0, 1)
  return `rgba(${r}, ${g}, ${b}, ${a})`
}

/** Darken a hex color by the given amount (0-1). */
export function darken(hex: string, amount: number): string {
  const { r, g, b } = hexToRgb(hex)
  const factor = 1 - clamp(amount, 0, 1)
  return rgbToHex(r * factor, g * factor, b * factor)
}

/** Lighten a hex color by the given amount (0-1). */
export function lighten(hex: string, amount: number): string {
  const { r, g, b } = hexToRgb(hex)
  const factor = clamp(amount, 0, 1)
  return rgbToHex(r + (255 - r) * factor, g + (255 - g) * factor, b + (255 - b) * factor)
}

/** Pick a readable text color (dark or light) based on the background luminance. */
export function readableTextOn(hex: string): string {
  const { r, g, b } = hexToRgb(hex)
  // Relative luminance per WCAG
  const lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255
  return lum > 0.6 ? '#1F2937' /* neutral-800 */ : '#FFFFFF'
}
