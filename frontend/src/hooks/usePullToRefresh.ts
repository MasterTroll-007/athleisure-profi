import { useState, useEffect, useRef, useCallback } from 'react'

interface UsePullToRefreshOptions {
  onRefresh: () => Promise<void>
  threshold?: number
  disabled?: boolean
}

interface UsePullToRefreshReturn {
  isRefreshing: boolean
  pullProgress: number
  containerRef: React.RefObject<HTMLDivElement>
}

export function usePullToRefresh({
  onRefresh,
  threshold = 80,
  disabled = false,
}: UsePullToRefreshOptions): UsePullToRefreshReturn {
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [pullProgress, setPullProgress] = useState(0)
  const containerRef = useRef<HTMLDivElement>(null)
  const startY = useRef(0)
  const isPulling = useRef(false)

  const handleTouchStart = useCallback(
    (e: TouchEvent) => {
      if (disabled || isRefreshing) return

      const container = containerRef.current
      if (!container) return

      // Only trigger if at top of scroll
      if (container.scrollTop > 0) return

      startY.current = e.touches[0].clientY
      isPulling.current = true
    },
    [disabled, isRefreshing]
  )

  const handleTouchMove = useCallback(
    (e: TouchEvent) => {
      if (!isPulling.current || disabled || isRefreshing) return

      const currentY = e.touches[0].clientY
      const diff = currentY - startY.current

      if (diff > 0) {
        // Prevent default only when pulling down
        e.preventDefault()
        // Apply resistance
        const progress = Math.min(diff / threshold, 1.5) * 100
        setPullProgress(progress)
      }
    },
    [disabled, isRefreshing, threshold]
  )

  const handleTouchEnd = useCallback(async () => {
    if (!isPulling.current || disabled) return

    isPulling.current = false

    if (pullProgress >= 100 && !isRefreshing) {
      setIsRefreshing(true)
      try {
        await onRefresh()
      } finally {
        setIsRefreshing(false)
      }
    }

    setPullProgress(0)
  }, [disabled, isRefreshing, onRefresh, pullProgress])

  useEffect(() => {
    const container = containerRef.current
    if (!container) return

    container.addEventListener('touchstart', handleTouchStart, { passive: true })
    container.addEventListener('touchmove', handleTouchMove, { passive: false })
    container.addEventListener('touchend', handleTouchEnd, { passive: true })

    return () => {
      container.removeEventListener('touchstart', handleTouchStart)
      container.removeEventListener('touchmove', handleTouchMove)
      container.removeEventListener('touchend', handleTouchEnd)
    }
  }, [handleTouchStart, handleTouchMove, handleTouchEnd])

  return {
    isRefreshing,
    pullProgress,
    containerRef,
  }
}
