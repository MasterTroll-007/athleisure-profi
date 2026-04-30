import {
  Children,
  ChangeEvent,
  OptionHTMLAttributes,
  ReactElement,
  ReactNode,
  SelectHTMLAttributes,
  forwardRef,
  isValidElement,
  useCallback,
  useEffect,
  useId,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
} from 'react'
import { createPortal } from 'react-dom'
import { AnimatePresence, motion } from 'framer-motion'
import { Check, ChevronDown } from 'lucide-react'
import { cn } from '@/utils/cn'

type SelectOption = {
  key: string
  value: string
  label: ReactNode
  disabled: boolean
}

export interface SelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'size'> {
  label?: string
  error?: string
}

const isOptionElement = (
  child: ReactNode
): child is ReactElement<OptionHTMLAttributes<HTMLOptionElement>> =>
  isValidElement(child) && child.type === 'option'

const optionToText = (node: ReactNode): string => {
  if (typeof node === 'string' || typeof node === 'number') return String(node)
  if (Array.isArray(node)) return node.map(optionToText).join('')
  return ''
}

const normalizeSelectValue = (value: SelectHTMLAttributes<HTMLSelectElement>['value']): string => {
  if (Array.isArray(value)) return String(value[0] ?? '')
  if (value === undefined || value === null) return ''
  return String(value)
}

const Select = forwardRef<HTMLSelectElement, SelectProps>(
  (
    {
      className,
      label,
      error,
      id,
      children,
      value,
      defaultValue,
      onChange,
      disabled,
      name,
      required,
      ...props
    },
    ref
  ) => {
    const generatedId = useId()
    const selectId = id || generatedId
    const rootRef = useRef<HTMLDivElement>(null)
    const triggerRef = useRef<HTMLButtonElement>(null)
    const menuRef = useRef<HTMLDivElement>(null)
    const nativeSelectRef = useRef<HTMLSelectElement | null>(null)
    const [isOpen, setIsOpen] = useState(false)
    const [internalValue, setInternalValue] = useState(() => normalizeSelectValue(defaultValue))
    const [menuStyle, setMenuStyle] = useState<React.CSSProperties | null>(null)

    const options = useMemo<SelectOption[]>(
      () =>
        Children.toArray(children)
          .filter(isOptionElement)
          .map((child, index) => {
            const optionValue =
              child.props.value === undefined
                ? optionToText(child.props.children)
                : String(child.props.value)

            return {
              key: child.key ?? `${optionValue}-${index}`,
              value: optionValue,
              label: child.props.children,
              disabled: !!child.props.disabled,
            }
          }),
      [children]
    )

    const selectedValue = value !== undefined ? normalizeSelectValue(value) : internalValue
    const selectedOption = options.find((option) => option.value === selectedValue) ?? options[0]

    const setNativeSelectRef = useCallback(
      (node: HTMLSelectElement | null) => {
        nativeSelectRef.current = node
        if (typeof ref === 'function') {
          ref(node)
        } else if (ref) {
          ref.current = node
        }
      },
      [ref]
    )

    const updateMenuPosition = useCallback(() => {
      const trigger = triggerRef.current
      if (!trigger) return

      const rect = trigger.getBoundingClientRect()
      const viewportHeight = window.innerHeight
      const viewportWidth = window.innerWidth
      const gap = 8
      const preferredHeight = 288
      const spaceBelow = viewportHeight - rect.bottom - gap
      const spaceAbove = rect.top - gap
      const placeAbove = spaceBelow < 180 && spaceAbove > spaceBelow
      const availableHeight = Math.max(140, Math.min(preferredHeight, (placeAbove ? spaceAbove : spaceBelow) - gap))
      const width = Math.min(rect.width, viewportWidth - 16)
      const left = Math.max(8, Math.min(rect.left, viewportWidth - width - 8))
      const top = placeAbove
        ? Math.max(8, rect.top - availableHeight - gap)
        : Math.min(viewportHeight - availableHeight - 8, rect.bottom + gap)

      setMenuStyle({
        position: 'fixed',
        top,
        left,
        width,
        maxHeight: availableHeight,
        zIndex: 90,
      })
    }, [])

    const close = useCallback(() => setIsOpen(false), [])

    const open = useCallback(() => {
      if (disabled) return
      updateMenuPosition()
      setIsOpen(true)
    }, [disabled, updateMenuPosition])

    const selectOption = useCallback(
      (option: SelectOption) => {
        if (option.disabled || disabled) return

        if (value === undefined) {
          setInternalValue(option.value)
        }

        const eventTarget = {
          name,
          value: option.value,
        } as HTMLSelectElement

        onChange?.({
          target: eventTarget,
          currentTarget: eventTarget,
        } as ChangeEvent<HTMLSelectElement>)

        close()
        triggerRef.current?.focus()
      },
      [close, disabled, name, onChange, value]
    )

    useLayoutEffect(() => {
      if (isOpen) updateMenuPosition()
    }, [isOpen, selectedValue, updateMenuPosition])

    useEffect(() => {
      if (!isOpen) return undefined

      const handlePointerDown = (event: PointerEvent) => {
        const target = event.target as Node
        if (rootRef.current?.contains(target) || menuRef.current?.contains(target)) return
        close()
      }

      const handleKeyDown = (event: KeyboardEvent) => {
        if (event.key === 'Escape') {
          close()
          triggerRef.current?.focus()
        }
      }

      document.addEventListener('pointerdown', handlePointerDown)
      document.addEventListener('keydown', handleKeyDown)
      document.addEventListener('scroll', updateMenuPosition, true)
      window.addEventListener('resize', updateMenuPosition)

      return () => {
        document.removeEventListener('pointerdown', handlePointerDown)
        document.removeEventListener('keydown', handleKeyDown)
        document.removeEventListener('scroll', updateMenuPosition, true)
        window.removeEventListener('resize', updateMenuPosition)
      }
    }, [close, isOpen, updateMenuPosition])

    const portalTarget = typeof document === 'undefined' ? null : document.body

    return (
      <div ref={rootRef} className="w-full">
        {label && (
          <label htmlFor={selectId} className="mb-1.5 block text-sm font-medium text-white/75">
            {label}
          </label>
        )}

        <select
          ref={setNativeSelectRef}
          name={name}
          value={selectedValue}
          required={required}
          disabled={disabled}
          tabIndex={-1}
          aria-hidden="true"
          className="sr-only"
          onChange={onChange}
          {...props}
        >
          {children}
        </select>

        <button
          id={selectId}
          ref={triggerRef}
          type="button"
          disabled={disabled}
          aria-haspopup="listbox"
          aria-expanded={isOpen}
          aria-invalid={!!error}
          onClick={() => (isOpen ? close() : open())}
          className={cn(
            'group relative flex min-h-[48px] w-full min-w-0 items-center justify-between gap-3 overflow-hidden rounded-lg px-4 py-3 text-left text-sm font-medium transition-colors',
            'border border-white/12 bg-[linear-gradient(135deg,rgba(255,255,255,0.18),rgba(255,255,255,0.065)_44%,rgba(255,255,255,0.12))]',
            'text-white shadow-[inset_0_1px_0_rgba(255,255,255,0.18),0_12px_30px_-24px_rgba(255,255,255,0.38)] backdrop-blur',
            'hover:border-white/24 hover:bg-white/[0.14]',
            'focus:outline-none focus:ring-2 focus:ring-primary-300 focus:ring-offset-2 focus:ring-offset-dark-bg',
            'disabled:cursor-not-allowed disabled:opacity-60',
            error && 'border-red-400 focus:ring-red-400',
            className
          )}
        >
          <span className="pointer-events-none absolute inset-x-0 top-0 h-px bg-white/40" />
          <span className="min-w-0 flex-1 truncate">
            {selectedOption?.label ?? <span className="text-white/45">-</span>}
          </span>
          <span className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-md border border-white/10 bg-black/18 text-primary-100 shadow-[inset_0_1px_0_rgba(255,255,255,0.12)] transition-colors group-hover:border-white/18 group-hover:bg-white/10">
            <ChevronDown
              size={17}
              className={cn('transition-transform duration-200', isOpen && 'rotate-180')}
            />
          </span>
        </button>

        {error && <p className="mt-1.5 text-sm text-red-400">{error}</p>}

        {portalTarget &&
          createPortal(
            <AnimatePresence>
              {isOpen && menuStyle && (
                <motion.div
                  ref={menuRef}
                  style={menuStyle}
                  initial={{ opacity: 0, y: -4, scale: 0.98 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: -4, scale: 0.98 }}
                  transition={{ duration: 0.14, ease: 'easeOut' }}
                  className="overflow-hidden rounded-lg border border-white/14 bg-[#080710]/95 shadow-[0_28px_80px_-38px_rgba(0,0,0,0.95),inset_0_1px_0_rgba(255,255,255,0.12)] backdrop-blur-xl"
                >
                  <div
                    role="listbox"
                    aria-labelledby={selectId}
                    className="max-h-[inherit] overflow-y-auto p-1.5"
                  >
                    {options.map((option) => {
                      const isSelected = option.value === selectedValue

                      return (
                        <button
                          key={option.key}
                          type="button"
                          role="option"
                          aria-selected={isSelected}
                          disabled={option.disabled}
                          onClick={() => selectOption(option)}
                          className={cn(
                            'flex min-h-[40px] w-full min-w-0 items-center gap-3 rounded-md px-3 py-2 text-left text-sm transition-colors',
                            option.disabled
                              ? 'cursor-not-allowed text-white/28'
                              : 'text-white/82 hover:bg-white/10 hover:text-white',
                            isSelected && 'bg-primary-400/18 text-white'
                          )}
                        >
                          <span className="min-w-0 flex-1 truncate">{option.label}</span>
                          <span className="flex h-5 w-5 flex-shrink-0 items-center justify-center text-primary-200">
                            {isSelected && <Check size={16} />}
                          </span>
                        </button>
                      )
                    })}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>,
            portalTarget
          )}
      </div>
    )
  }
)

Select.displayName = 'Select'

export default Select
