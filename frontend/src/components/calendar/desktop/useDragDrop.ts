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
  // Ref to the column grid — preferred drop target; when attached, drops are
  // computed against it so x/y map cleanly to (day, time). Falls back to
  // containerRef for backwards compatibility.
  gridRef?: React.RefObject<HTMLElement | null>
  onDrop: (slot: CalendarSlot, newDate: string, newStartTime: string) => void
}

export function useDragDrop({
  enabled,
  days,
  startHour,
  hourHeight,
  containerRef,
  bodyRef,
  gridRef,
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

    // Calculate drop target. Prefer gridRef (the inner column area) because it
    // gives a clean x/y mapping; fall back to containerRef (which also includes
    // the toolbar + time column) if the grid ref isn't wired up.
    const target = gridRef?.current ?? containerRef.current
    if (target) {
      const rect = target.getBoundingClientRect()
      const relX = e.clientX - rect.left
      const relY = e.clientY - rect.top + (bodyRef.current?.scrollTop ?? 0)

      const colWidth = rect.width / days.length
      const dayIndex = Math.max(0, Math.min(days.length - 1, Math.floor(relX / colWidth)))
      const targetDate = days[dayIndex]

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
  }, [dragState.slot, containerRef, bodyRef, gridRef, days, hourHeight, startHour, onDrop])

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
