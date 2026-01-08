import { Outlet } from 'react-router-dom'
import { ToastProvider } from '@/components/ui/Toast'
import Header from './Header'
import BottomNav from './BottomNav'

export default function Layout() {
  return (
    <ToastProvider>
      <div className="min-h-screen bg-neutral-50 dark:bg-dark-bg">
        <Header />

        {/* Main content */}
        <main className="pt-14 pb-14 md:pb-8">
          <div className="max-w-7xl mx-auto px-4 py-6">
            <Outlet />
          </div>
        </main>

        <BottomNav />
      </div>
    </ToastProvider>
  )
}
