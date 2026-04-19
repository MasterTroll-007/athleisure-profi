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
  // Offset of the finger within the slot at grab time — used to anchor the
  // preview so the slot stays "under the finger" during drag.
  const grabOffset = useRef<{ x: number; y: number }>({ x: 0, y: 0 })
  // Most recent pointer position. Needed by the edge-auto-scroll RAF loop,
  // which fires on its own tick schedule.
  const lastPointer = useRef<{ x: number; y: number }>({ x: 0, y: 0 })
  const hasMoved = useRef(false)
  // True once the drag is actually "live" — immediately on desktop, after the
  // long-press timer on mobile.
  const isActive = useRef(false)
  const longPressTimer = useRef<number | null>(null)
  const rafId = useRef<number | null>(null)
  // Remember the touch-action style we clobbered so we can restore it.
  const prevTouchAction = useRef<string | null>(null)

  // Compute snap target from a slot anchor position (top-left of the slot in
  // viewport coords). On mobile we snap the slot's effective top-left, not the
  // raw pointer, so the visible preview stays locked to the finger.
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

  // Edge auto-scroll: while finger hovers near a viewport edge, pan the
  // bodyRef scroll container so the user can move slots across days or to
  // another part of the day without lifting.
  const edgeScrollTick = useCallback(() => {
    if (!isActive.current) {
      rafId.current = null
      return
    }
    const { x, y } = lastPointer.current
    const w = window.innerWidth
    const h = window.innerHeight
    const THRESHOLD = 70 // px from edge where auto-scroll starts
    const MAX_SPEED = 12 // px per frame at the very edge

    let dx = 0
    let dy = 0
    if (y < THRESHOLD) dy = -MAX_SPEED * (1 - y / THRESHOLD)
    else if (y > h - THRESHOLD) dy = MAX_SPEED * (1 - (h - y) / THRESHOLD)
    if (x < THRESHOLD) dx = -MAX_SPEED * (1 - x / THRESHOLD)
    else if (x > w - THRESHOLD) dx = MAX_SPEED * (1 - (w - x) / THRESHOLD)

    const scroller = bodyRef.current
    if (scroller && (dx || dy)) {
      scroller.scrollBy({ left: dx, top: dy })
      // Recompute snap — grid moved under the finger even though the finger
      // didn't — so the preview needs to pick up the new target.
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
    // Kick off the RAF loop so auto-scroll is responsive as soon as drag starts.
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
    setDragState({ isDragging: false, slot: null, snap: null })
  }

  const onPointerDown = useCallback((e: React.PointerEvent, slot: CalendarSlot) => {
    if (!enabled) return
    // Use currentTarget — the slot element the listener is bound to — rather
    // than e.target, which could be an inner text/icon node. Getting the wrong
    // element here corrupts grabOffset and makes the snap preview jump far
    // away from the finger (which is exactly why drag appeared to stop
    // working on mobile after this offset was introduced).
    const slotEl = e.currentTarget as HTMLElement
    const slotRect = slotEl.getBoundingClientRect()
    grabOffset.current = { x: e.clientX - slotRect.left, y: e.clientY - slotRect.top }

    startPos.current = {
      x: e.clientX,
      y: e.clientY,
      pointerId: e.pointerId,
      target: slotEl,
    }
    lastPointer.current = { x: e.clientX, y: e.clientY }
    hasMoved.current = false
    isActive.current = false
    setDragState({ isDragging: false, slot, snap: null })

    if (longPressMs > 0) {
      longPressTimer.current = window.setTimeout(() => {
        longPressTimer.current = null
        if (startPos.current) {
          activate(startPos.current.pointerId, startPos.current.target)
          const snap = snapFromPointer(startPos.current.x, startPos.current.y)
          setDragState(prev => ({ ...prev, isDragging: true, snap }))
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
