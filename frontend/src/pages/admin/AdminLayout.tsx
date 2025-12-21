import { useState } from 'react'
import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  LayoutDashboard,
  Calendar,
  Clock,
  Users,
  Dumbbell,
  CreditCard,
  DollarSign,
  ChevronLeft,
  Menu,
  X,
} from 'lucide-react'
import { cn } from '@/utils/cn'
import ThemeToggle from '@/components/layout/ThemeToggle'

const navItems = [
  { path: '/admin', icon: LayoutDashboard, labelKey: 'admin.dashboard', end: true },
  { path: '/admin/calendar', icon: Calendar, labelKey: 'admin.calendar' },
  { path: '/admin/availability', icon: Clock, labelKey: 'admin.availability' },
  { path: '/admin/clients', icon: Users, labelKey: 'admin.clients' },
  { path: '/admin/plans', icon: Dumbbell, labelKey: 'admin.plans' },
  { path: '/admin/pricing', icon: CreditCard, labelKey: 'admin.pricing' },
  { path: '/admin/payments', icon: DollarSign, labelKey: 'admin.payments' },
]

export default function AdminLayout() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [isSidebarOpen, setIsSidebarOpen] = useState(false)

  return (
    <div className="min-h-screen bg-neutral-50 dark:bg-dark-bg">
      {/* Mobile header */}
      <header className="lg:hidden fixed top-0 left-0 right-0 z-40 h-14 bg-white/80 dark:bg-dark-surface/80 backdrop-blur-lg border-b border-neutral-100 dark:border-dark-border">
        <div className="flex items-center justify-between h-full px-4">
          <button
            onClick={() => setIsSidebarOpen(true)}
            className="p-2 -ml-2 text-neutral-600 dark:text-neutral-300"
          >
            <Menu size={24} />
          </button>
          <span className="font-heading font-semibold text-neutral-900 dark:text-white">
            Admin
          </span>
          <ThemeToggle />
        </div>
      </header>

      {/* Mobile sidebar overlay */}
      {isSidebarOpen && (
        <div
          className="lg:hidden fixed inset-0 z-50 bg-black/50"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={cn(
          'fixed top-0 left-0 z-50 h-full w-64 bg-white dark:bg-dark-surface border-r border-neutral-100 dark:border-dark-border transform transition-transform duration-200',
          'lg:translate-x-0',
          isSidebarOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        <div className="flex flex-col h-full">
          {/* Sidebar header */}
          <div className="flex items-center justify-between h-14 px-4 border-b border-neutral-100 dark:border-dark-border">
            <span className="font-heading font-bold text-lg text-neutral-900 dark:text-white">
              Admin Panel
            </span>
            <button
              onClick={() => setIsSidebarOpen(false)}
              className="lg:hidden p-2 -mr-2 text-neutral-600 dark:text-neutral-300"
            >
              <X size={20} />
            </button>
          </div>

          {/* Navigation */}
          <nav className="flex-1 py-4 overflow-y-auto">
            <ul className="space-y-1 px-2">
              {navItems.map((item) => (
                <li key={item.path}>
                  <NavLink
                    to={item.path}
                    end={item.end}
                    onClick={() => setIsSidebarOpen(false)}
                    className={({ isActive }) =>
                      cn(
                        'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors',
                        isActive
                          ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-400'
                          : 'text-neutral-600 dark:text-neutral-400 hover:bg-neutral-100 dark:hover:bg-dark-surfaceHover'
                      )
                    }
                  >
                    <item.icon size={18} />
                    {t(item.labelKey)}
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>

          {/* Back to app */}
          <div className="p-4 border-t border-neutral-100 dark:border-dark-border">
            <button
              onClick={() => navigate('/')}
              className="flex items-center gap-2 w-full px-3 py-2 text-sm font-medium text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-white"
            >
              <ChevronLeft size={18} />
              Zpět do aplikace
            </button>
          </div>
        </div>
      </aside>

      {/* Main content */}
      <main className="lg:ml-64 pt-14 lg:pt-0 min-h-screen">
        <div className="p-4 lg:p-6 max-w-7xl mx-auto">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
