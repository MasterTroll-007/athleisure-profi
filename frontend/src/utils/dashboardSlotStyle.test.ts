import { describe, expect, it } from 'vitest'
import { dashboardSlotStyle } from './dashboardSlotStyle'

describe('dashboardSlotStyle', () => {
  it('uses opaque location color for reserved dashboard slots', () => {
    expect(dashboardSlotStyle({ status: 'reserved', locationColor: '#10B981' }, 'dark')).toMatchObject({
      backgroundColor: '#10B981',
      borderColor: '#0D9467',
      color: '#FFFFFF',
    })
  })

  it('uses tinted location color for cancelled dashboard slots', () => {
    expect(dashboardSlotStyle({ status: 'cancelled', locationColor: '#3B82F6' }, 'dark')).toMatchObject({
      backgroundColor: 'rgba(59, 130, 246, 0.2)',
      borderColor: '#3B82F6',
      color: '#F9FAFB',
    })
  })

  it('falls back to neutral location color when slot has no valid color', () => {
    expect(dashboardSlotStyle({ status: 'reserved', locationColor: null }, 'light')).toMatchObject({
      backgroundColor: '#9CA3AF',
      borderColor: '#7D828C',
      color: '#1F2937',
    })
  })
})
