import { Outlet } from 'react-router-dom'
import { ToastProvider } from '@/components/ui/Toast'
import Header from './Header'
import BottomNav from './BottomNav'

export default function Layout() {
  return (
    <ToastProvider>
      <div className="app-stage min-h-screen">
        <Header />

        {/* Main content */}
        <main className="pt-14 pb-[calc(6.5rem_+_var(--safe-area-inset-bottom))] md:pb-8">
          <div className="max-w-7xl mx-auto px-4 py-6">
            <Outlet />
          </div>
        </main>

        <BottomNav />
      </div>
    </ToastProvider>
  )
}
