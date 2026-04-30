import { Fragment, ReactNode, useEffect, useRef, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { X } from 'lucide-react'
import { cn } from '@/utils/cn'

interface ModalProps {
  isOpen: boolean
  onClose: () => void
  title?: string
  children: ReactNode
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'full'
  showClose?: boolean
  mobileFullScreen?: boolean
  isObscured?: boolean
  backdropClassName?: string
}

let openModalCount = 0
let lockedScrollY = 0
let previousBodyStyles: Partial<CSSStyleDeclaration> | null = null

const lockDocumentScroll = () => {
  if (openModalCount === 0) {
    lockedScrollY = window.scrollY
    previousBodyStyles = {
      overflow: document.body.style.overflow,
      position: document.body.style.position,
      top: document.body.style.top,
      left: document.body.style.left,
      right: document.body.style.right,
      width: document.body.style.width,
    }

    document.body.style.overflow = 'hidden'
    document.body.style.position = 'fixed'
    document.body.style.top = `-${lockedScrollY}px`
    document.body.style.left = '0'
    document.body.style.right = '0'
    document.body.style.width = '100%'
  }

  openModalCount += 1
}

const unlockDocumentScroll = () => {
  openModalCount = Math.max(0, openModalCount - 1)
  if (openModalCount !== 0 || !previousBodyStyles) return

  document.body.style.overflow = previousBodyStyles.overflow ?? ''
  document.body.style.position = previousBodyStyles.position ?? ''
  document.body.style.top = previousBodyStyles.top ?? ''
  document.body.style.left = previousBodyStyles.left ?? ''
  document.body.style.right = previousBodyStyles.right ?? ''
  document.body.style.width = previousBodyStyles.width ?? ''
  window.scrollTo(0, lockedScrollY)
  previousBodyStyles = null
}

export default function Modal({
  isOpen,
  onClose,
  title,
  children,
  size = 'md',
  showClose = true,
  mobileFullScreen = false,
  isObscured = false,
  backdropClassName,
}: ModalProps) {
  const modalRef = useRef<HTMLDivElement>(null)
  const [isPickerOpenInside, setIsPickerOpenInside] = useState(false)

  // Close on ESC key
  useEffect(() => {
    if (!isOpen) return

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  useEffect(() => {
    if (!isOpen) {
      setIsPickerOpenInside(false)
      return undefined
    }

    const handlePickerOpenChange = (event: Event) => {
      const { open, anchor } = (event as CustomEvent<{ open: boolean; anchor: HTMLElement | null }>).detail ?? {}
      if (!anchor || !modalRef.current?.contains(anchor)) return
      setIsPickerOpenInside(open)
    }

    document.addEventListener('app:picker-open-change', handlePickerOpenChange)
    return () => document.removeEventListener('app:picker-open-change', handlePickerOpenChange)
  }, [isOpen])

  useEffect(() => {
    if (!isOpen) return undefined

    lockDocumentScroll()
    return unlockDocumentScroll
  }, [isOpen])

  const sizes = {
    sm: 'max-w-sm',
    md: 'max-w-md',
    lg: 'max-w-lg',
    xl: 'max-w-3xl',
    full: 'max-w-full mx-4',
  }
  const shouldObscure = isObscured || isPickerOpenInside

  return (
    <AnimatePresence>
      {isOpen && (
        <Fragment>
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className={cn('fixed inset-0 z-50 bg-black/70 backdrop-blur-sm', backdropClassName)}
            onClick={onClose}
          />

          {/* Modal */}
          <div
            className={cn(
              'fixed inset-0 z-50 flex items-center justify-center overflow-hidden overscroll-none sm:p-4',
              mobileFullScreen ? 'p-0' : 'p-2'
            )}
            onClick={onClose}
          >
            <motion.div
              ref={modalRef}
              role="dialog"
              aria-modal="true"
              aria-labelledby={title ? 'modal-title' : undefined}
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              transition={{ duration: 0.2, ease: 'easeOut' }}
              className={cn(
                'relative',
                'w-full rounded-xl border border-white/10 bg-[#07060d]/92 backdrop-blur-xl dark:border-white/10 dark:bg-[#07060d]/92',
                'shadow-[0_30px_100px_-45px_rgba(0,0,0,0.9)] overflow-hidden overscroll-contain flex flex-col max-h-[calc(100vh-1rem)] sm:max-h-[90vh]',
                mobileFullScreen && 'max-sm:h-full max-sm:max-h-none max-sm:rounded-none max-sm:border-x-0',
                sizes[size]
              )}
              onClick={(e) => e.stopPropagation()}
            >
              {/* Header */}
              {(title || showClose) && (
                <div className="flex items-center justify-between px-4 py-3 sm:px-5 sm:py-4 border-b border-white/10 flex-shrink-0">
                  {title && (
                    <h2 id="modal-title" className="text-base sm:text-lg font-heading font-semibold text-neutral-900 dark:text-white">
                      {title}
                    </h2>
                  )}
                  {showClose && (
                    <button
                      onClick={onClose}
                      aria-label="Close modal"
                      className="p-2 -mr-2 rounded-lg text-neutral-500 hover:text-neutral-700 hover:bg-neutral-100/50 dark:text-neutral-400 dark:hover:text-neutral-200 dark:hover:bg-white/10 transition-colors touch-target"
                    >
                      <X size={20} />
                    </button>
                  )}
                </div>
              )}

              {/* Content */}
              <div className="px-4 py-3 sm:px-5 sm:py-4 overflow-y-auto overscroll-contain touch-pan-y flex-1 min-h-0">{children}</div>

              {shouldObscure && (
                <div
                  aria-hidden="true"
                  className="absolute inset-0 z-30 rounded-[inherit] bg-neutral-950/58 backdrop-blur-[2px] backdrop-grayscale"
                />
              )}
            </motion.div>
          </div>
        </Fragment>
      )}
    </AnimatePresence>
  )
}
