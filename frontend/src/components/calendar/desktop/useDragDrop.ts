import { useState, useCallback, useRef, useEffect } from 'react'
import type { CalendarSlot } from '@/components/ui'
import { SNAP_MINUTES, formatDateISO } from './constants'

export interface DragSnap {
  // Pixel offsets relative to the grid container (gridRef). Used to paint a
  // subtle snap indicator at the would-be drop target.
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
  // Snapped drop target (used for the snap indicator + the actual drop).
  snap: DragSnap | null
  // Live pointer position — the floating preview is rendered at
  // `pointer - grabOffset`, so the slot stays glued to the finger exactly
  // where the user grabbed it. Snapping happens on release.
  pointerX: number
  pointerY: number
  grabOffsetX: number
  grabOffsetY: number
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

const INITIAL_DRAG_STATE: DragState = {
  isDragging: false,
  slot: null,
  snap: null,
  pointerX: 0,
  pointerY: 0,
  grabOffsetX: 0,
  grabOffsetY: 0,
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
  const [dragState, setDragState] = useState<DragState>(INITIAL_DRAG_STATE)

  const startPos = useRef<{ x: number; y: number; pointerId: number; target: HTMLElement } | null>(null)
  const grabOffset = useRef<{ x: number; y: number }>({ x: 0, y: 0 })
  const lastPointer = useRef<{ x: number; y: number }>({ x: 0, y: 0 })
  const hasMoved = useRef(false)
  const isActive = useRef(false)
  const longPressTimer = useRef<number | null>(null)
  const rafId = useRef<number | null>(null)
  const prevTouchAction = useRef<string | null>(null)
  // Remember scroll-snap-type on the scroller so we can restore it. The
  // mobile calendar sets `scroll-snap-type: x mandatory` on the horizontal
  // scroller, which turns any tiny programmatic scroll into a whole-column
  // jump — we have to turn it off for the duration of the drag.
  const prevSnapType = useRef<string | null>(null)

  // Compute snap target from a slot anchor position (top-left of the slot in
  // viewport coords).
  const computeSnapFromAnchor = useCallback((anchorX: number, anchorY: number): DragSnap | null => {
    const target = gridRef?.current ?? containerRef.current
    if (!target) return null
    const rect = target.getBoundingClientRect()
    const relX = clampNum(anchorX - rect.left, 0, rect.width - 1)
    const relY = clampNum(anchorY - rect.top + (bodyRef.current?.scrollTop ?? 0), 0, Number.MAX_SAFE_INTEGER)

    const colWidth = rect.width / days.length
    const dayIndex = Math.max(0, Math.min(days.length - 1, Math.floor(relX / colWidth)))
    const targetDate = days[dayIndex]

    const totalMinutes = (relY / hourHeight) * 60 + startHour * 60
    const snappedMin = Math.round(totalMinutes / SNAP_MINUTES) * SNAP_MINUTES
    const hours = Math.floor(snappedMin / 60)
    const minutes = snappedMin % 60
    const time = `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`

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

  const snapFromPointer = useCallback((clientX: number, clientY: number): DragSnap | null => {
    return computeSnapFromAnchor(clientX - grabOffset.current.x, clientY - grabOffset.current.y)
  }, [computeSnapFromAnchor])

  const cancelLongPress = () => {
    if (longPressTimer.current !== null) {
      window.clearTimeout(longPressTimer.current)
      longPressTimer.current = null
    }
  }

  const stopEdgeScroll = () => {
    if (rafId.current !== null) {
      cancelAnimationFrame(rafId.current)
      rafId.current = null
    }
  }

  // Edge auto-scroll while the finger hovers near a viewport edge. Scrolls the
  // bodyRef container horizontally (across days) and the window + bodyRef
  // vertically (through hours), so the user can reach anywhere without
  // lifting. Speed ramps smoothly from 0 at the threshold to MAX at the edge.
  const edgeScrollTick = useCallback(() => {
    if (!isActive.current) {
      rafId.current = null
      return
    }
    const { x, y } = lastPointer.current
    const w = window.innerWidth
    const h = window.innerHeight
    const THRESHOLD = 70
    const MAX_H_SPEED = 6 // px/frame; horizontal is sensitive with snap off
    const MAX_V_SPEED = 14

    let dx = 0
    let dy = 0
    if (y < THRESHOLD) dy = -MAX_V_SPEED * (1 - y / THRESHOLD)
    else if (y > h - THRESHOLD) dy = MAX_V_SPEED * (1 - (h - y) / THRESHOLD)
    if (x < THRESHOLD) dx = -MAX_H_SPEED * (1 - x / THRESHOLD)
    else if (x > w - THRESHOLD) dx = MAX_H_SPEED * (1 - (w - x) / THRESHOLD)

    const scroller = bodyRef.current
    if (scroller && (dx || dy)) {
      // Horizontal scroll goes to the dedicated scroller (day columns).
      if (dx) scroller.scrollBy({ left: dx })
      // Vertical: try the scroller first, but ALSO scroll the window so it
      // works regardless of which ancestor is the actual scroll viewport on
      // mobile. scrollBy on a non-scrollable element is a no-op, so this is
      // safe to double-dispatch.
      if (dy) {
        scroller.scrollBy({ top: dy })
        window.scrollBy(0, dy)
      }
      // Recompute snap since the grid moved under the (stationary) finger.
      const snap = snapFromPointer(x, y)
      setDragState(prev => ({ ...prev, snap }))
    }
    rafId.current = requestAnimationFrame(edgeScrollTick)
  }, [bodyRef, snapFromPointer])

  const lockScroll = () => {
    const el = containerRef.current
    if (el) {
      prevTouchAction.current = el.style.touchAction
      el.style.touchAction = 'none'
    }
    // Kill the mobile snap-to-column behavior for the duration of the drag,
    // otherwise every tiny scrollBy lurches a whole column.
    const scroller = bodyRef.current as HTMLElement | null
    if (scroller) {
      prevSnapType.current = scroller.style.scrollSnapType
      scroller.style.scrollSnapType = 'none'
    }
    if (rafId.current === null) {
      rafId.current = requestAnimationFrame(edgeScrollTick)
    }
  }

  const unlockScroll = () => {
    const el = containerRef.current
    if (el) {
      el.style.touchAction = prevTouchAction.current ?? ''
      prevTouchAction.current = null
    }
    const scroller = bodyRef.current as HTMLElement | null
    if (scroller) {
      scroller.style.scrollSnapType = prevSnapType.current ?? ''
      prevSnapType.current = null
    }
    stopEdgeScroll()
  }

  const activate = (pointerId: number, target: HTMLElement) => {
    try { target.setPointerCapture(pointerId) } catch { /* ignore */ }
    isActive.current = true
    lockScroll()
    if (longPressMs > 0 && typeof navigator !== 'undefined' && 'vibrate' in navigator) {
      try { navigator.vibrate(10) } catch { /* ignore */ }
    }
  }

  const reset = () => {
    cancelLongPress()
    unlockScroll()
    startPos.current = null
    hasMoved.current = false
    isActive.current = false
    grabOffset.current = { x: 0, y: 0 }
    setDragState(INITIAL_DRAG_STATE)
  }

  const onPointerDown = useCallback((e: React.PointerEvent, slot: CalendarSlot) => {
    if (!enabled) return
    const slotEl = e.currentTarget as HTMLElement
    const slotRect = slotEl.getBoundingClientRect()
    const offX = e.clientX - slotRect.left
    const offY = e.clientY - slotRect.top
    grabOffset.current = { x: offX, y: offY }

    startPos.current = {
      x: e.clientX,
      y: e.clientY,
      pointerId: e.pointerId,
      target: slotEl,
    }
    lastPointer.current = { x: e.clientX, y: e.clientY }
    hasMoved.current = false
    isActive.current = false
    setDragState({
      isDragging: false,
      slot,
      snap: null,
      pointerX: e.clientX,
      pointerY: e.clientY,
      grabOffsetX: offX,
      grabOffsetY: offY,
    })

    if (longPressMs > 0) {
      longPressTimer.current = window.setTimeout(() => {
        longPressTimer.current = null
        if (startPos.current) {
          activate(startPos.current.pointerId, startPos.current.target)
          const snap = snapFromPointer(startPos.current.x, startPos.current.y)
          setDragState(prev => ({
            ...prev,
            isDragging: true,
            snap,
            pointerX: startPos.current!.x,
            pointerY: startPos.current!.y,
          }))
        }
      }, longPressMs)
    } else {
      e.preventDefault()
      activate(e.pointerId, slotEl)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, longPressMs, snapFromPointer])

  const onPointerMove = useCallback((e: React.PointerEvent) => {
    if (!startPos.current || !dragState.slot) return

    lastPointer.current = { x: e.clientX, y: e.clientY }

    const dx = e.clientX - startPos.current.x
    const dy = e.clientY - startPos.current.y
    const dist = Math.abs(dx) + Math.abs(dy)

    if (longPressMs > 0 && !isActive.current) {
      if (dist > 10) {
        reset()
      }
      return
    }

    if (!hasMoved.current && dist < 5) return
    hasMoved.current = true

    const snap = snapFromPointer(e.clientX, e.clientY)
    setDragState(prev => ({
      ...prev,
      isDragging: true,
      snap,
      pointerX: e.clientX,
      pointerY: e.clientY,
    }))
  }, [dragState.slot, snapFromPointer, longPressMs])

  const onPointerUp = useCallback((e: React.PointerEvent) => {
    if (longPressMs > 0 && !isActive.current) {
      reset()
      return
    }

    if (!dragState.slot || !hasMoved.current) {
      reset()
      return
    }

    const snap = snapFromPointer(e.clientX, e.clientY)
    if (snap) onDrop(dragState.slot, snap.date, snap.time)
    reset()
  }, [dragState.slot, snapFromPointer, onDrop, longPressMs])

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
