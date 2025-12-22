import { LucideIcon } from 'lucide-react'
import Button from './Button'
import { cn } from '@/utils/cn'

interface EmptyStateProps {
  icon: LucideIcon
  title: string
  description?: string
  action?: {
    label: string
    onClick: () => void
  }
  className?: string
}

export default function EmptyState({
  icon: Icon,
  title,
  description,
  action,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center py-12 px-4 text-center',
        className
      )}
    >
      <div className="mb-4 p-4 rounded-full bg-neutral-100 dark:bg-neutral-800">
        <Icon
          size={48}
          className="text-neutral-300 dark:text-neutral-600"
          strokeWidth={1.5}
        />
      </div>
      <h3 className="text-lg font-medium text-neutral-700 dark:text-neutral-300 mb-1">
        {title}
      </h3>
      {description && (
        <p className="text-sm text-neutral-500 dark:text-neutral-400 max-w-xs mb-4">
          {description}
        </p>
      )}
      {action && (
        <Button onClick={action.onClick} variant="primary" size="sm">
          {action.label}
        </Button>
      )}
    </div>
  )
}
