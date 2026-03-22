import { useState, useCallback, useRef } from 'react'
import { SLIDE_DURATION } from './constants'

export function useSlideAnimation() {
  const [animClass, setAnimClass] = useState('')
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const triggerSlide = useCallback((
    direction: 'left' | 'right',
    swapData: () => void,
  ) => {
    if (animClass !== '') return // already animating

    const cls = direction === 'left'
      ? 'animate-slide-nav-left'
      : 'animate-slide-nav-right'

    setAnimClass(cls)

    // Swap data at 40% of animation (when opacity = 0)
    timerRef.current = setTimeout(() => {
      swapData()
    }, SLIDE_DURATION * 0.4)

    // Remove animation class after completion
    setTimeout(() => {
      setAnimClass('')
      timerRef.current = null
    }, SLIDE_DURATION)
  }, [animClass])

  return {
    animClass,
    isAnimating: animClass !== '',
    triggerSlide,
  }
}
