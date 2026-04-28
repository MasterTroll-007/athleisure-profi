import { ButtonHTMLAttributes, forwardRef } from 'react'
import { cn } from '@/utils/cn'
import Spinner from './Spinner'

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  isLoading?: boolean
  leftIcon?: React.ReactNode
  rightIcon?: React.ReactNode
}

const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      className,
      variant = 'primary',
      size = 'md',
      isLoading = false,
      leftIcon,
      rightIcon,
      children,
      disabled,
      ...props
    },
    ref
  ) => {
    const variants = {
      primary: 'btn-metal btn-metal-gold',
      secondary: 'btn-metal btn-metal-silver',
      ghost: 'btn-metal btn-metal-gunmetal',
      danger: 'btn-metal btn-metal-red',
    }

    const sizes = {
      sm: 'px-3 py-1.5 text-sm min-h-[36px]',
      md: 'px-4 py-2 text-sm min-h-[44px]',
      lg: 'px-6 py-3 text-base min-h-[52px]',
    }

    return (
      <button
        ref={ref}
        className={cn(
          'inline-flex min-w-0 max-w-full items-center justify-center whitespace-nowrap font-medium leading-none rounded-lg transition-all',
          'focus:outline-none focus:ring-2 focus:ring-primary-300 focus:ring-offset-2 focus:ring-offset-dark-bg',
          'disabled:cursor-not-allowed disabled:opacity-60',
          variants[variant],
          sizes[size],
          className
        )}
        disabled={disabled || isLoading}
        {...props}
      >
        <span className="btn-content relative z-10 inline-flex min-w-0 max-w-full items-center justify-center gap-2 whitespace-nowrap">
          {isLoading ? (
            <Spinner size="sm" />
          ) : (
            <>
              {leftIcon && <span className="btn-icon flex-shrink-0">{leftIcon}</span>}
              <span className="btn-label min-w-0 overflow-hidden text-ellipsis whitespace-nowrap">{children}</span>
              {rightIcon && <span className="btn-icon flex-shrink-0">{rightIcon}</span>}
            </>
          )}
        </span>
      </button>
    )
  }
)

Button.displayName = 'Button'

export default Button
