import { createContext, useContext, useState, ReactNode, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { X, CheckCircle, AlertCircle, Info } from 'lucide-react'
import { cn } from '@/utils/cn'

type ToastType = 'success' | 'error' | 'info'

interface Toast {
  id: string
  type: ToastType
  message: string
}

interface ToastContextValue {
  toasts: Toast[]
  showToast: (type: ToastType, message: string) => void
  hideToast: (id: string) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

export function useToast() {
  const context = useContext(ToastContext)
  if (!context) {
    throw new Error('useToast must be used within ToastProvider')
  }
  return context
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])

  const showToast = useCallback((type: ToastType, message: string) => {
    const id = Math.random().toString(36).substring(2, 11)
    console.log('Toast showToast called:', { id, type, message })
    setToasts((prev) => {
      console.log('Toast state update, prev:', prev.length, 'adding:', message)
      return [...prev, { id, type, message }]
    })

    // Auto dismiss after 4 seconds
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id))
    }, 4000)
  }, [])

  const hideToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const icons = {
    success: <CheckCircle size={20} className="text-green-500" />,
    error: <AlertCircle size={20} className="text-red-500" />,
    info: <Info size={20} className="text-blue-500" />,
  }

  return (
    <ToastContext.Provider value={{ toasts, showToast, hideToast }}>
      {children}
      {/* Render toasts directly in provider */}
      <div className="fixed bottom-20 left-1/2 -translate-x-1/2 z-[100] flex flex-col gap-2 w-full max-w-sm px-4 md:bottom-8">
        <AnimatePresence>
          {toasts.map((toast) => (
            <motion.div
              key={toast.id}
              initial={{ opacity: 0, y: 20, scale: 0.95 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -20, scale: 0.95 }}
              transition={{ duration: 0.2 }}
              className={cn(
                'flex items-center gap-3 px-4 py-3 rounded-lg glass dark:glass-dark shadow-lg',
                'text-neutral-900 dark:text-white'
              )}
            >
              {icons[toast.type]}
              <p className="flex-1 text-sm">{toast.message}</p>
              <button
                onClick={() => hideToast(toast.id)}
                className="p-1 rounded text-neutral-500 hover:text-neutral-700 dark:text-neutral-400 dark:hover:text-white transition-colors"
              >
                <X size={16} />
              </button>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  )
}

export function Toaster() {
  const { toasts, hideToast } = useToast()
  console.log('Toaster render, toasts count:', toasts.length)

  const icons = {
    success: <CheckCircle size={20} className="text-green-500" />,
    error: <AlertCircle size={20} className="text-red-500" />,
    info: <Info size={20} className="text-blue-500" />,
  }

  return (
    <div className="fixed bottom-20 left-1/2 -translate-x-1/2 z-[100] flex flex-col gap-2 w-full max-w-sm px-4 md:bottom-8">
      <AnimatePresence>
        {toasts.map((toast) => (
          <motion.div
            key={toast.id}
            initial={{ opacity: 0, y: 20, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -20, scale: 0.95 }}
            transition={{ duration: 0.2 }}
            className={cn(
              'flex items-center gap-3 px-4 py-3 rounded-lg glass dark:glass-dark shadow-lg',
              'text-neutral-900 dark:text-white'
            )}
          >
            {icons[toast.type]}
            <p className="flex-1 text-sm">{toast.message}</p>
            <button
              onClick={() => hideToast(toast.id)}
              className="p-1 rounded text-neutral-500 hover:text-neutral-700 dark:text-neutral-400 dark:hover:text-white transition-colors"
            >
              <X size={16} />
            </button>
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  )
}

// Wrap App with ToastProvider
export function withToastProvider(App: React.ComponentType) {
  return function WrappedApp() {
    return (
      <ToastProvider>
        <App />
        <Toaster />
      </ToastProvider>
    )
  }
}
