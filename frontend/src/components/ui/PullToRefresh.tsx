import { ReactNode } from 'react'
import { RefreshCw } from 'lucide-react'
import { usePullToRefresh } from '@/hooks/usePullToRefresh'
import { cn } from '@/utils/cn'

interface PullToRefreshProps {
  onRefresh: () => Promise<void>
  children: ReactNode
  className?: string
  disabled?: boolean
}

export default function PullToRefresh({
  onRefresh,
  children,
  className,
  disabled = false,
}: PullToRefreshProps) {
  const { isRefreshing, pullProgress, containerRef } = usePullToRefresh({
    onRefresh,
    disabled,
  })

  const showIndicator = pullProgress > 0 || isRefreshing
  const indicatorScale = Math.min(pullProgress / 100, 1)
  const rotation = isRefreshing ? 0 : pullProgress * 3.6

  return (
    <div
      ref={containerRef}
      className={cn('relative overflow-auto', className)}
      style={{ WebkitOverflowScrolling: 'touch' }}
    >
      {/* Pull indicator */}
      <div
        className={cn(
          'absolute left-1/2 -translate-x-1/2 z-10 transition-opacity duration-200',
          showIndicator ? 'opacity-100' : 'opacity-0'
        )}
        style={{
          top: Math.max(8, (pullProgress / 100) * 40 - 32),
          transform: `translateX(-50%) scale(${indicatorScale})`,
        }}
      >
        <div
          className={cn(
            'p-2 rounded-full bg-white dark:bg-neutral-800 shadow-lg',
            isRefreshing && 'animate-spin'
          )}
          style={{
            transform: isRefreshing ? undefined : `rotate(${rotation}deg)`,
          }}
        >
          <RefreshCw
            size={20}
            className={cn(
              'text-primary-500',
              pullProgress >= 100 && !isRefreshing && 'text-green-500'
            )}
          />
        </div>
      </div>

      {/* Content with pull offset */}
      <div
        style={{
          transform: pullProgress > 0 ? `translateY(${Math.min(pullProgress / 2, 40)}px)` : undefined,
          transition: pullProgress === 0 ? 'transform 0.2s ease-out' : undefined,
        }}
      >
        {children}
      </div>
    </div>
  )
}
