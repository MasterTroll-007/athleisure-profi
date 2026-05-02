export interface PickerBaseProps {
  label?: string
  value: string
  onChange: (value: string) => void
  min?: string
  max?: string
  minuteStep?: number
  hourOnly?: boolean
  id?: string
  name?: string
  error?: string
  disabled?: boolean
  required?: boolean
  className?: string
  placeholder?: string
}


export interface HourDurationPickerProps {
  label?: string
  value: number | null
  onChange: (value: number | null) => void
  min?: number
  max?: number
  allowNone?: boolean
  noneLabel?: string
  unitLabel?: string
  id?: string
  error?: string
  disabled?: boolean
  required?: boolean
  className?: string
  placeholder?: string
}


export interface DurationPickerProps {
  label?: string
  value: number
  onChange: (value: number) => void
  min?: number
  max?: number
  minuteStep?: number
  values?: number[]
  hourUnitLabel?: string
  minuteUnitLabel?: string
  id?: string
  name?: string
  error?: string
  disabled?: boolean
  required?: boolean
  className?: string
  placeholder?: string
}

export interface DismissPickerOptions {
  fromAnchorHit?: boolean
}
