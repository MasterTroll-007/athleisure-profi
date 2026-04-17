import { useId } from 'react'
import { cn } from '@/utils/cn'

export const PRESET_COLORS = [
  '#3B82F6', // blue
  '#10B981', // green
  '#F59E0B', // orange
  '#8B5CF6', // purple
  '#EC4899', // pink
  '#EAB308', // yellow
  '#EF4444', // red
  '#06B6D4', // cyan
] as const

interface ColorPickerProps {
  label?: string
  value: string
  onChange: (hex: string) => void
  error?: string
}

const HEX_PATTERN = /^#[0-9A-Fa-f]{6}$/

const ColorPicker = ({ label, value, onChange, error }: ColorPickerProps) => {
  const inputId = useId()
  const normalized = value?.toUpperCase() ?? ''
  const isValid = HEX_PATTERN.test(normalized)

  return (
    <div className="w-full">
      {label && (
        <label
          htmlFor={inputId}
          className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1.5"
        >
          {label}
        </label>
      )}
      <div className="flex flex-wrap items-center gap-2">
        {PRESET_COLORS.map((preset) => {
          const active = normalized === preset
          return (
            <button
              key={preset}
              type="button"
              onClick={() => onChange(preset)}
              className={cn(
                'w-8 h-8 rounded-full transition-transform',
                'ring-offset-2 ring-offset-white dark:ring-offset-dark-surface',
                active ? 'ring-2 ring-neutral-900 dark:ring-white scale-110' : 'hover:scale-105'
              )}
              style={{ backgroundColor: preset }}
              aria-label={`Vybrat barvu ${preset}`}
              aria-pressed={active}
            />
          )
        })}
        <div className="flex items-center gap-2">
          <span
            className="w-8 h-8 rounded-full border border-neutral-300 dark:border-dark-border"
            style={{ backgroundColor: isValid ? normalized : '#E5E7EB' }}
            aria-hidden
          />
          <input
            id={inputId}
            type="text"
            value={value ?? ''}
            onChange={(e) => onChange(e.target.value.trim())}
            placeholder="#RRGGBB"
            maxLength={7}
            className={cn(
              'w-28 px-3 py-2 rounded-lg border text-sm font-mono uppercase',
              'bg-white dark:bg-dark-surface',
              'text-neutral-900 dark:text-neutral-100',
              'focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent',
              error || (value && !isValid)
                ? 'border-red-500 focus:ring-red-500'
                : 'border-neutral-200 dark:border-dark-border'
            )}
          />
        </div>
      </div>
      {error && <p className="mt-1.5 text-sm text-red-500">{error}</p>}
    </div>
  )
}

export default ColorPicker
