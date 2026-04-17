import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ColorPicker, { PRESET_COLORS } from './ColorPicker'

describe('ColorPicker', () => {
  it('renders all preset swatches', () => {
    render(<ColorPicker value="#3B82F6" onChange={() => {}} />)
    const swatches = screen.getAllByRole('button')
    // One button per preset.
    expect(swatches).toHaveLength(PRESET_COLORS.length)
  })

  it('marks the active preset with aria-pressed=true', () => {
    render(<ColorPicker value="#3B82F6" onChange={() => {}} />)
    const active = screen.getByRole('button', { pressed: true })
    expect(active).toHaveAttribute('aria-label', expect.stringContaining('#3B82F6'))
  })

  it('calls onChange with preset hex when a swatch is clicked', async () => {
    const onChange = vi.fn()
    render(<ColorPicker value="#3B82F6" onChange={onChange} />)

    const orange = screen.getByRole('button', { name: /#F59E0B/i })
    await userEvent.click(orange)

    expect(onChange).toHaveBeenCalledWith('#F59E0B')
  })

  it('calls onChange for each typed char in the hex input', async () => {
    const onChange = vi.fn()
    render(<ColorPicker value="" onChange={onChange} />)

    const input = screen.getByPlaceholderText('#RRGGBB') as HTMLInputElement
    await userEvent.type(input, 'XY')

    // The parent controls `value`, so with a non-updating vi.fn the input
    // re-renders empty each keystroke — we just verify onChange fires.
    expect(onChange).toHaveBeenCalled()
    expect(onChange.mock.calls.length).toBeGreaterThanOrEqual(2)
  })

  it('shows label above the picker when provided', () => {
    render(<ColorPicker label="Barva" value="#3B82F6" onChange={() => {}} />)
    expect(screen.getByText('Barva')).toBeInTheDocument()
  })

  it('renders error message when passed', () => {
    render(<ColorPicker value="nothex" onChange={() => {}} error="invalid color" />)
    expect(screen.getByText('invalid color')).toBeInTheDocument()
  })
})
