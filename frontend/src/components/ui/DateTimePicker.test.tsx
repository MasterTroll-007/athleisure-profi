import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import '@/i18n'
import {
  DatePicker,
  DurationPicker,
  HourDurationPicker,
  TimePicker,
} from './DateTimePicker'

describe('DateTimePicker controls', () => {
  it('renders the date picker with the shared app trigger and dropdown panel', async () => {
    render(<DatePicker label="Datum" value="2026-05-01" onChange={() => {}} />)

    const trigger = screen.getByRole('button', { name: /datum/i })
    expect(trigger).toHaveClass('app-control-trigger')

    await userEvent.click(trigger)

    expect(await screen.findByTestId('picker-panel')).toHaveClass('app-dropdown-panel')
    expect(trigger).toHaveClass('app-control-trigger-open')
  })

  it('renders the time picker as a custom wheel dropdown and toggles it from the trigger', async () => {
    render(<TimePicker label="Cas" value="10:15" onChange={() => {}} />)

    const trigger = screen.getByRole('button', { name: /cas/i })
    expect(trigger).toHaveClass('app-control-trigger')

    await userEvent.click(trigger)
    expect(await screen.findByTestId('picker-panel')).toHaveClass('app-dropdown-panel')

    await userEvent.click(trigger)
    expect(screen.queryByTestId('picker-panel')).not.toBeInTheDocument()
  })

  it('uses the custom trigger for duration pickers too', async () => {
    render(<DurationPicker label="Delka" value={75} onChange={vi.fn()} />)

    const trigger = screen.getByRole('button', { name: /delka/i })
    expect(trigger).toHaveClass('app-control-trigger')

    await userEvent.click(trigger)

    expect(await screen.findByTestId('picker-panel')).toHaveClass('app-dropdown-panel')
  })

  it('uses the custom trigger for hour-duration pickers', async () => {
    render(<HourDurationPicker label="Vraceni" value={24} onChange={vi.fn()} />)

    const trigger = screen.getByRole('button', { name: /vraceni/i })
    expect(trigger).toHaveClass('app-control-trigger')

    await userEvent.click(trigger)

    expect(await screen.findByTestId('picker-panel')).toHaveClass('app-dropdown-panel')
  })
})
