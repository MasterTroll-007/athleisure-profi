import { useState, useCallback, useRef, useEffect } from 'react'
import type { CalendarSlot } from '@/components/ui'
import { SNAP_MINUTES, formatDateISO } from './constants'

interface DragState {
  isDragging: boolean
  slot: CalendarSlot | null
  ghostX: number
  ghostY: number
}

interface UseDragDropOptions {
  enabled: boolean
  days: Date[]
  startHour: number
  hourHeight: number
  containerRef: React.RefObject<HTMLElement | null>
  bodyRef: React.RefObject<HTMLElement | null>
  onDrop: (slot: CalendarSlot, newDate: string, newStartTime: string) => void
}

export function useDragDrop({
  enabled,
  days,
  startHour,
  hourHeight,
  containerRef,
  bodyRef,
  onDrop,
}: UseDragDropOptions) {
  const [dragState, setDragState] = useState<DragState>({
    isDragging: false,
    slot: null,
    ghostX: 0,
    ghostY: 0,
  })

  const startPos = useRef<{ x: number; y: number } | null>(null)
  const hasMoved = useRef(false)

  const onPointerDown = useCallback((e: React.PointerEvent, slot: CalendarSlot) => {
    if (!enabled) return
    e.preventDefault()
    ;(e.target as HTMLElement).setPointerCapture(e.pointerId)
    startPos.current = { x: e.clientX, y: e.clientY }
    hasMoved.current = false
    setDragState({ isDragging: false, slot, ghostX: e.clientX, ghostY: e.clientY })
  }, [enabled])

  const onPointerMove = useCallback((e: React.PointerEvent) => {
    if (!startPos.current || !dragState.slot) return

    const dx = e.clientX - startPos.current.x
    const dy = e.clientY - startPos.current.y

    // Start drag after 5px threshold
    if (!hasMoved.current && Math.abs(dx) + Math.abs(dy) < 5) return
    hasMoved.current = true

    setDragState(prev => ({
      ...prev,
      isDragging: true,
      ghostX: e.clientX,
      ghostY: e.clientY,
    }))
  }, [dragState.slot])

  const onPointerUp = useCallback((e: React.PointerEvent) => {
    if (!dragState.slot || !hasMoved.current) {
      startPos.current = null
      setDragState({ isDragging: false, slot: null, ghostX: 0, ghostY: 0 })
      return
    }

    // Calculate drop target
    const container = containerRef.current
    const body = bodyRef.current
    if (container && body) {
      const bodyRect = body.getBoundingClientRect()
      const relX = e.clientX - bodyRect.left
      const relY = e.clientY - bodyRect.top + body.scrollTop

      // Determine which day column
      const colWidth = (bodyRect.width) / days.length
      const dayIndex = Math.max(0, Math.min(days.length - 1, Math.floor(relX / colWidth)))
      const targetDate = days[dayIndex]

      // Determine time from Y position
      const totalMinutes = (relY / hourHeight) * 60 + startHour * 60
      const snapped = Math.round(totalMinutes / SNAP_MINUTES) * SNAP_MINUTES
      const hours = Math.floor(snapped / 60)
      const minutes = snapped % 60
      const timeStr = `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`

      onDrop(dragState.slot, formatDateISO(targetDate), timeStr)
    }

    startPos.current = null
    hasMoved.current = false
    setDragState({ isDragging: false, slot: null, ghostX: 0, ghostY: 0 })
  }, [dragState.slot, containerRef, bodyRef, days, hourHeight, startHour, onDrop])

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      startPos.current = null
    }
  }, [])

  return {
    dragState,
    onPointerDown,
    onPointerMove,
    onPointerUp,
  }
}
