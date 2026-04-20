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

  // Compute snap target from a slot anchor position (top-left of the slot in
  // viewport coords).
  const computeSnapFromAnchor = useCallback((anchorX: number, anchorY: number): DragSnap | null => {
    const target = gridRef?.current ?? containerRef.current
    if (!target) return null
    const rect = target.getBoundingClientRect()
    const relX = clampNum(anchorX - rect.left, 0, rect.width - 1)
    const relY = clampNum(anchorY - rect.top + (bodyRef.current?.scrollTop ?? 0), 0, Number.MAX_SAFE_INTEGER)

    const colWidth = rect.width / days.length
    // Drop into the day that contains the slot's CENTER, not its left edge.
    // Slot width ≈ colWidth, so shift relX by half a column before bucketing —
    // whichever column owns the majority (>50%) of the slot wins on release.
    const dayIndex = Math.max(0, Math.min(days.length - 1, Math.floor((relX + colWidth / 2) / colWidth)))
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

  // Edge auto-scroll while the finger hovers near the edge of the scrollable
  // area. Thresholds are measured against bodyRef's own rect (not the full
  // viewport) so a bottom nav bar or sticky header doesn't swallow the
  // trigger zone. Speed ramps smoothly from 0 at THRESHOLD to MAX at the
  // very edge.
  const edgeScrollTick = useCallback(() => {
    if (!isActive.current) {
      rafId.current = null
      return
    }
    const scroller = bodyRef.current as HTMLElement | null
    if (!scroller) {
      rafId.current = requestAnimationFrame(edgeScrollTick)
      return
    }
    const { x, y } = lastPointer.current
    const rect = scroller.getBoundingClientRect()
    const THRESHOLD = 70
    const MAX_H_SPEED = 6
    const MAX_V_SPEED = 14

    let dx = 0
    let dy = 0
    const topD = y - rect.top
    const bottomD = rect.bottom - y
    const leftD = x - rect.left
    const rightD = rect.right - x

    if (topD < THRESHOLD) dy = -MAX_V_SPEED * (1 - Math.max(0, topD) / THRESHOLD)
    else if (bottomD < THRESHOLD) dy = MAX_V_SPEED * (1 - Math.max(0, bottomD) / THRESHOLD)
    if (leftD < THRESHOLD) dx = -MAX_H_SPEED * (1 - Math.max(0, leftD) / THRESHOLD)
    else if (rightD < THRESHOLD) dx = MAX_H_SPEED * (1 - Math.max(0, rightD) / THRESHOLD)

    if (dx || dy) {
      if (dx) scroller.scrollBy({ left: dx })
      if (dy) {
        scroller.scrollBy({ top: dy })
        // Some parent layouts put the actual vertical scroll on window
        // instead of the inner scroller; dispatch both so whichever one is
        // really scrollable responds. scrollBy on a non-scrollable element
        // is a safe no-op.
        window.scrollBy(0, dy)
      }
      // Grid moved under the (stationary) finger — refresh snap preview.
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
    // Scroll-snap-type can't be reliably toggled imperatively — React rewrites
    // inline styles on each re-render. Instead, consumers read `isDragging`
    // from dragState and pass the snap-type through their own style prop.
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
