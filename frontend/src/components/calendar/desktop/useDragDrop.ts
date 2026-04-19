import { useState, useCallback, useRef, useEffect } from 'react'
import type { CalendarSlot } from '@/components/ui'
import { SNAP_MINUTES, formatDateISO } from './constants'

export interface DragSnap {
  // Pixel offsets relative to the grid container (gridRef). Used to position
  // the floating preview event.
  topPx: number
  leftPx: number
  // Pixel size of one column — convenience for the preview width.
  colWidthPx: number
  // Logical drop target.
  date: string
  time: string
}

interface DragState {
  isDragging: boolean
  slot: CalendarSlot | null
  snap: DragSnap | null
}

interface UseDragDropOptions {
  enabled: boolean
  days: Date[]
  startHour: number
  hourHeight: number
  containerRef: React.RefObject<HTMLElement | null>
  bodyRef: React.RefObject<HTMLElement | null>
  // Ref to the inner column grid. When provided, drop targets are computed
  // against it so x/y map cleanly to (day, time). Falls back to containerRef.
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
    snap: null,
  })

  const startPos = useRef<{ x: number; y: number } | null>(null)
  const hasMoved = useRef(false)

  // Compute snap target from a pointer position. Returns null if no grid is
  // available — the caller should treat that as "nothing to drop on".
  const computeSnap = useCallback((clientX: number, clientY: number): DragSnap | null => {
    const target = gridRef?.current ?? containerRef.current
    if (!target) return null
    const rect = target.getBoundingClientRect()
    const relX = e_clamp(clientX - rect.left, 0, rect.width - 1)
    const relY = e_clamp(clientY - rect.top + (bodyRef.current?.scrollTop ?? 0), 0, Number.MAX_SAFE_INTEGER)

    const colWidth = rect.width / days.length
    const dayIndex = Math.max(0, Math.min(days.length - 1, Math.floor(relX / colWidth)))
    const targetDate = days[dayIndex]

    const totalMinutes = (relY / hourHeight) * 60 + startHour * 60
    const snappedMin = Math.round(totalMinutes / SNAP_MINUTES) * SNAP_MINUTES
    const hours = Math.floor(snappedMin / 60)
    const minutes = snappedMin % 60
    const time = `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`

    // Pixel position relative to the grid for live preview rendering.
    const topPx = ((snappedMin - startHour * 60) / 60) * hourHeight
    const leftPx = dayIndex * colWidth

    return {
      topPx,
      leftPx,
      colWidthPx: colWidth,
      date: formatDateISO(targetDate),
      time,
    }
  }, [containerRef, gridRef, bodyRef, days, hourHeight, startHour])

  const onPointerDown = useCallback((e: React.PointerEvent, slot: CalendarSlot) => {
    if (!enabled) return
    e.preventDefault()
    ;(e.target as HTMLElement).setPointerCapture(e.pointerId)
    startPos.current = { x: e.clientX, y: e.clientY }
    hasMoved.current = false
    setDragState({ isDragging: false, slot, snap: null })
  }, [enabled])

  const onPointerMove = useCallback((e: React.PointerEvent) => {
    if (!startPos.current || !dragState.slot) return

    const dx = e.clientX - startPos.current.x
    const dy = e.clientY - startPos.current.y

    // Start drag after 5px threshold to distinguish from a click.
    if (!hasMoved.current && Math.abs(dx) + Math.abs(dy) < 5) return
    hasMoved.current = true

    const snap = computeSnap(e.clientX, e.clientY)
    setDragState(prev => ({
      ...prev,
      isDragging: true,
      snap,
    }))
  }, [dragState.slot, computeSnap])

  const onPointerUp = useCallback((e: React.PointerEvent) => {
    if (!dragState.slot || !hasMoved.current) {
      startPos.current = null
      setDragState({ isDragging: false, slot: null, snap: null })
      return
    }

    const snap = computeSnap(e.clientX, e.clientY)
    if (snap) onDrop(dragState.slot, snap.date, snap.time)

    startPos.current = null
    hasMoved.current = false
    setDragState({ isDragging: false, slot: null, snap: null })
  }, [dragState.slot, computeSnap, onDrop])

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

// Inline clamp — keeps computeSnap free of an external dependency.
function e_clamp(v: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, v))
}
