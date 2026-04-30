import { TextareaHTMLAttributes, forwardRef, useId } from 'react'
import { cn } from '@/utils/cn'

type TextareaMaxWidth = 'sm' | 'md' | 'lg' | 'full'

export interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string
  error?: string
  maxWidth?: TextareaMaxWidth
}

const maxWidthClass: Record<TextareaMaxWidth, string> = {
  sm: 'max-w-sm',
  md: 'max-w-md',
  lg: 'max-w-xl',
  full: 'max-w-none',
}

const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, label, error, id, maxWidth = 'md', ...props }, ref) => {
    const generatedId = useId()
    const textareaId = id || generatedId

    return (
      <div className={cn('w-full', maxWidthClass[maxWidth])}>
        {label && (
          <label
            htmlFor={textareaId}
            className="mb-1.5 block text-sm font-medium text-white/75"
          >
            {label}
          </label>
        )}
        <textarea
          ref={ref}
          id={textareaId}
          className={cn(
            'w-full rounded-lg border px-4 py-3 transition-colors',
            'border-white/10 bg-white/10 text-white',
            'placeholder:text-white/40',
            'focus:outline-none focus:ring-2 focus:ring-primary-300 focus:border-transparent',
            'disabled:cursor-not-allowed disabled:opacity-60',
            error && 'border-red-400 focus:ring-red-400',
            className
          )}
          {...props}
        />
        {error && <p className="mt-1.5 text-sm text-red-400">{error}</p>}
      </div>
    )
  }
)

Textarea.displayName = 'Textarea'

export default Textarea
