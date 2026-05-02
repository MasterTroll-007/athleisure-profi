import { useEffect, type RefObject } from 'react'
import type { DismissPickerOptions } from './types'

export function useDismissPicker(
  open: boolean,
  anchorRef: RefObject<HTMLElement>,
  panelRef: RefObject<HTMLElement>,
  onClose: (options?: DismissPickerOptions) => void
) {
  useEffect(() => {
    if (!open) return undefined

    const handlePointerDown = (event: PointerEvent) => {
      const target = event.target as Node
      const anchor = anchorRef.current
      if (anchor?.contains(target) || panelRef.current?.contains(target)) return

      if (anchor) {
        const rect = anchor.getBoundingClientRect()
        const isAnchorHit =
          event.clientX >= rect.left &&
          event.clientX <= rect.right &&
          event.clientY >= rect.top &&
          event.clientY <= rect.bottom

        if (isAnchorHit) {
          event.preventDefault()
          event.stopPropagation()
          onClose({ fromAnchorHit: true })
          return
        }
      }

      onClose()
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return
      event.preventDefault()
      event.stopPropagation()
      onClose()
    }

    document.addEventListener('pointerdown', handlePointerDown)
    document.addEventListener('keydown', handleKeyDown, true)
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown)
      document.removeEventListener('keydown', handleKeyDown, true)
    }
  }, [anchorRef, onClose, open, panelRef])
}
