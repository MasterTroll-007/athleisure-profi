import {
  useEffect,
  useLayoutEffect,
  useState,
  type CSSProperties,
  type ReactNode,
  type RefObject,
} from 'react'
import { createPortal } from 'react-dom'
import { cn } from '@/utils/cn'
import { clamp } from './dateTimePickerUtils'

interface PickerPanelProps {
  open: boolean
  anchorRef: RefObject<HTMLElement>
  panelRef: RefObject<HTMLDivElement>
  estimatedHeight: number
  minWidth?: number
  children: ReactNode
}

interface PickerPositionState {
  style: CSSProperties
  ready: boolean
  placement: 'top' | 'bottom'
}

function usePickerPosition(
  open: boolean,
  anchorRef: RefObject<HTMLElement>,
  estimatedHeight: number,
  minWidth = 320
) {
  const [position, setPosition] = useState<PickerPositionState>({
    ready: false,
    placement: 'bottom',
    style: {
      left: 0,
      top: 0,
      width: minWidth,
      maxHeight: estimatedHeight,
      visibility: 'hidden',
    },
  })

  useLayoutEffect(() => {
    if (!open) {
      setPosition((current) =>
        current.ready
          ? {
              ...current,
              ready: false,
              style: {
                ...current.style,
                visibility: 'hidden',
              },
            }
          : current
      )
      return undefined
    }

    const updatePosition = () => {
      const anchor = anchorRef.current
      if (!anchor) return

      const rect = anchor.getBoundingClientRect()
      const margin = 12
      const width = Math.min(
        Math.max(rect.width, minWidth),
        window.innerWidth - margin * 2
      )
      const left = clamp(rect.left, margin, window.innerWidth - width - margin)
      const spaceBelow = window.innerHeight - rect.bottom - margin
      const shouldOpenBelow = spaceBelow >= estimatedHeight || rect.top < estimatedHeight + margin
      const top = shouldOpenBelow ? rect.bottom + 8 : Math.max(margin, rect.top - estimatedHeight - 8)

      setPosition({
        ready: true,
        placement: shouldOpenBelow ? 'bottom' : 'top',
        style: {
          left,
          top,
          width,
          maxHeight: Math.min(estimatedHeight, window.innerHeight - margin * 2),
          visibility: 'visible',
        },
      })
    }

    updatePosition()
    const frameId = window.requestAnimationFrame(updatePosition)
    const settleTimeoutId = window.setTimeout(updatePosition, 240)
    window.addEventListener('resize', updatePosition)
    window.addEventListener('scroll', updatePosition, true)
    return () => {
      window.cancelAnimationFrame(frameId)
      window.clearTimeout(settleTimeoutId)
      window.removeEventListener('resize', updatePosition)
      window.removeEventListener('scroll', updatePosition, true)
    }
  }, [anchorRef, estimatedHeight, minWidth, open])

  return position
}

export function PickerPanel({
  open,
  anchorRef,
  panelRef,
  estimatedHeight,
  minWidth,
  children,
}: PickerPanelProps) {
  const [entered, setEntered] = useState(false)
  const position = usePickerPosition(open, anchorRef, estimatedHeight, minWidth)

  useEffect(() => {
    if (!open) return undefined

    const anchor = anchorRef.current
    document.dispatchEvent(
      new CustomEvent('app:picker-open-change', {
        detail: {
          open: true,
          anchor,
        },
      })
    )

    return () => {
      document.dispatchEvent(
        new CustomEvent('app:picker-open-change', {
          detail: {
            open: false,
            anchor,
          },
        })
      )
    }
  }, [anchorRef, open])

  useEffect(() => {
    if (!open || !position.ready) {
      setEntered(false)
      return undefined
    }

    const frameId = window.requestAnimationFrame(() => setEntered(true))
    return () => window.cancelAnimationFrame(frameId)
  }, [open, position.ready])

  if (!open) return null

  return createPortal(
    <div className="fixed inset-0 z-[90] pointer-events-none">
      <div className="absolute inset-0 pointer-events-auto bg-transparent" />
      <div
        ref={panelRef}
        data-testid="picker-panel"
        data-placement={position.placement}
        style={position.style}
        className={cn(
          'app-dropdown-panel pointer-events-auto fixed overflow-y-auto overscroll-contain text-white',
          'transition-[opacity,transform] duration-150 ease-out will-change-[opacity,transform]',
          entered && position.ready
            ? 'translate-y-0 scale-100 opacity-100'
            : position.placement === 'bottom'
              ? '-translate-y-1 scale-[0.985] opacity-0'
              : 'translate-y-1 scale-[0.985] opacity-0'
        )}
      >
        {children}
      </div>
    </div>,
    document.body
  )
}

function PickerLabel({ id, label }: { id?: string; label?: string }) {
  if (!label) return null

  return (
    <label htmlFor={id} className="block text-sm font-medium text-white/75 mb-1.5">
      {label}
    </label>
  )
}

export function PickerTrigger({
  id,
  triggerRef,
  label,
  value,
  placeholder,
  icon,
  open,
  error,
  disabled,
  required,
  className,
  onClick,
}: {
  id?: string
  triggerRef?: RefObject<HTMLButtonElement>
  label?: string
  value: string
  placeholder: string
  icon: ReactNode
  open: boolean
  error?: string
  disabled?: boolean
  required?: boolean
  className?: string
  onClick: () => void
}) {
  return (
    <div className="w-full">
      <PickerLabel id={id} label={label} />
      <button
        ref={triggerRef}
        id={id}
        type="button"
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-invalid={!!error}
        aria-required={required}
        aria-label={label ? `${label}: ${value || placeholder}` : value || placeholder}
        disabled={disabled}
        onClick={onClick}
        className={cn(
          'app-control-trigger group flex min-h-[48px] w-full min-w-0 items-center justify-between gap-3 rounded-lg px-4 py-3 text-left',
          'focus:outline-none focus:ring-2 focus:ring-primary-300 focus:border-transparent',
          error && 'app-control-trigger-error focus:ring-red-400',
          open && 'app-control-trigger-open ring-2 ring-primary-300',
          className
        )}
      >
        <span className={cn('truncate text-sm font-medium', value ? 'text-white' : 'app-control-placeholder')}>
          {value || placeholder}
        </span>
        <span className="app-control-icon flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg transition-colors">
          {icon}
        </span>
      </button>
      {error && <p className="mt-1.5 text-sm text-red-500">{error}</p>}
    </div>
  )
}

