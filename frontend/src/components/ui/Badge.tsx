import { HTMLAttributes, forwardRef } from 'react'
import { cn } from '@/utils/cn'

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'info'
  size?: 'sm' | 'md'
}

const Badge = forwardRef<HTMLSpanElement, BadgeProps>(
  ({ className, variant = 'default', size = 'md', children, ...props }, ref) => {
    const variants = {
      default: 'border border-white/10 bg-white/10 text-white/75 dark:bg-white/10 dark:text-white/75',
      primary: 'border border-primary-300/25 bg-primary-300/15 text-primary-100 dark:bg-primary-300/15 dark:text-primary-100',
      success: 'border border-emerald-300/20 bg-emerald-300/12 text-emerald-100 dark:bg-emerald-300/12 dark:text-emerald-100',
      warning: 'border border-amber-300/25 bg-amber-300/15 text-amber-100 dark:bg-amber-300/15 dark:text-amber-100',
      danger: 'border border-red-300/25 bg-red-300/15 text-red-100 dark:bg-red-300/15 dark:text-red-100',
      info: 'border border-sky-300/25 bg-sky-300/15 text-sky-100 dark:bg-sky-300/15 dark:text-sky-100',
    }

    const sizes = {
      sm: 'px-2 py-0.5 text-xs',
      md: 'px-2.5 py-1 text-sm',
    }

    return (
      <span
        ref={ref}
        className={cn(
          'inline-flex items-center font-medium rounded-full',
          variants[variant],
          sizes[size],
          className
        )}
        {...props}
      >
        {children}
      </span>
    )
  }
)

Badge.displayName = 'Badge'

export default Badge
