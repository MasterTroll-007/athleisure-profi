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
  // When >0, drag only activates after the pointer stays within a small
  // radius for this long. Used on mobile so normal scrolls don't accidentally
  // start a drag. 0 (default) = activate immediately on pointerdown (desktop).
  longPressMs?: number
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
  longPressMs = 0,
}: UseDragDropOptions) {
  const [dragState, setDragState] = useState<DragState>({
    isDragging: false,
    slot: null,
    snap: null,
  })

  const startPos = useRef<{ x: number; y: number; pointerId: number; target: HTMLElement } | null>(null)
  const hasMoved = useRef(false)
  // True once the drag is actually "live" — immediately on desktop, after the
  // long-press timer on mobile.
  const isActive = useRef(false)
  const longPressTimer = useRef<number | null>(null)

  // Compute snap target from a pointer position. Returns null if no grid is
  // available — the caller should treat that as "nothing to drop on".
  const computeSnap = useCallback((clientX: number, clientY: number): DragSnap | null => {
    const target = gridRef?.current ?? containerRef.current
    if (!target) return null
    const rect = target.getBoundingClientRect()
    const relX = clampNum(clientX - rect.left, 0, rect.width - 1)
    const relY = clampNum(clientY - rect.top + (bodyRef.current?.scrollTop ?? 0), 0, Number.MAX_SAFE_INTEGER)

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

  const cancelLongPress = () => {
    if (longPressTimer.current !== null) {
      window.clearTimeout(longPressTimer.current)
      longPressTimer.current = null
    }
  }

  const activate = (pointerId: number, target: HTMLElement) => {
    try { target.setPointerCapture(pointerId) } catch { /* ignore */ }
    isActive.current = true
    // Short haptic so the user knows drag mode engaged.
    if (longPressMs > 0 && typeof navigator !== 'undefined' && 'vibrate' in navigator) {
      try { navigator.vibrate(10) } catch { /* ignore */ }
    }
  }

  const reset = () => {
    cancelLongPress()
    startPos.current = null
    hasMoved.current = false
    isActive.current = false
    setDragState({ isDragging: false, slot: null, snap: null })
  }

  const onPointerDown = useCallback((e: React.PointerEvent, slot: CalendarSlot) => {
    if (!enabled) return
    startPos.current = {
      x: e.clientX,
      y: e.clientY,
      pointerId: e.pointerId,
      target: e.target as HTMLElement,
    }
    hasMoved.current = false
    isActive.current = false
    setDragState({ isDragging: false, slot, snap: null })

    if (longPressMs > 0) {
      // Mobile: wait before claiming the pointer so scroll works naturally.
      longPressTimer.current = window.setTimeout(() => {
        longPressTimer.current = null
        if (startPos.current) {
          activate(startPos.current.pointerId, startPos.current.target)
          // Pop a preview at the current finger position so the drag feels
          // instant even if the user hasn't moved yet.
          const snap = computeSnap(startPos.current.x, startPos.current.y)
          setDragState(prev => ({ ...prev, isDragging: true, snap }))
        }
      }, longPressMs)
    } else {
      e.preventDefault()
      activate(e.pointerId, e.target as HTMLElement)
    }
  }, [enabled, longPressMs, computeSnap])

  const onPointerMove = useCallback((e: React.PointerEvent) => {
    if (!startPos.current || !dragState.slot) return

    const dx = e.clientX - startPos.current.x
    const dy = e.clientY - startPos.current.y
    const dist = Math.abs(dx) + Math.abs(dy)

    // Long-press in progress: moving more than a touch away cancels the
    // pending drag so native scroll can take over.
    if (longPressMs > 0 && !isActive.current) {
      if (dist > 10) {
        reset()
      }
      return
    }

    if (!hasMoved.current && dist < 5) return
    hasMoved.current = true

    const snap = computeSnap(e.clientX, e.clientY)
    setDragState(prev => ({
      ...prev,
      isDragging: true,
      snap,
    }))
  }, [dragState.slot, computeSnap, longPressMs])

  const onPointerUp = useCallback((e: React.PointerEvent) => {
    // Tap released before long-press fired: let the click handler take it.
    if (longPressMs > 0 && !isActive.current) {
      reset()
      return
    }

    if (!dragState.slot || !hasMoved.current) {
      reset()
      return
    }

    const snap = computeSnap(e.clientX, e.clientY)
    if (snap) onDrop(dragState.slot, snap.date, snap.time)
    reset()
  }, [dragState.slot, computeSnap, onDrop, longPressMs])

  const onPointerCancel = useCallback(() => {
    reset()
  }, [])

  useEffect(() => {
    return () => {
      reset()
    }
  }, [])

  return {
    dragState,
    onPointerDown,
    onPointerMove,
    onPointerUp,
    onPointerCancel,
  }
}

function clampNum(v: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, v))
}
