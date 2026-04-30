import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import Select from './Select'

describe('Select', () => {
  it('uses the custom trigger and menu instead of exposing the native select UI', async () => {
    render(
      <Select label="Mistnost" value="a" onChange={() => {}}>
        <option value="a">Studio A</option>
        <option value="b">Studio B</option>
      </Select>
    )

    const trigger = screen.getByRole('button', { name: /studio a/i })
    expect(trigger).toHaveClass('app-control-trigger')

    await userEvent.click(trigger)

    const menu = screen.getByRole('listbox')
    expect(menu.parentElement).toHaveClass('app-dropdown-panel')
    expect(screen.getByRole('option', { name: /studio b/i })).toBeInTheDocument()
  })

  it('emits the selected option value through the select-shaped change event', async () => {
    const onChange = vi.fn()
    render(
      <Select label="Mistnost" value="a" onChange={onChange}>
        <option value="a">Studio A</option>
        <option value="b">Studio B</option>
      </Select>
    )

    await userEvent.click(screen.getByRole('button', { name: /studio a/i }))
    await userEvent.click(screen.getByRole('option', { name: /studio b/i }))

    expect(onChange).toHaveBeenCalledWith(
      expect.objectContaining({
        target: expect.objectContaining({ value: 'b' }),
      })
    )
  })

  it('keeps disabled options inert', async () => {
    const onChange = vi.fn()
    render(
      <Select label="Mistnost" value="a" onChange={onChange}>
        <option value="a">Studio A</option>
        <option value="b" disabled>
          Studio B
        </option>
      </Select>
    )

    await userEvent.click(screen.getByRole('button', { name: /studio a/i }))
    await userEvent.click(screen.getByRole('option', { name: /studio b/i }))

    expect(onChange).not.toHaveBeenCalled()
  })
})
