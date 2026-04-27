import { NavLink } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Home, Calendar, List, CreditCard, User, Users } from 'lucide-react'
import { cn } from '@/utils/cn'
import { useAuthStore } from '@/stores/authStore'

const navItems = [
  { path: '/', icon: Home, labelKey: 'nav.home', end: true },
  { path: '/calendar', icon: Calendar, labelKey: 'nav.newReservation', end: true },
  { path: '/reservations', icon: List, labelKey: 'nav.myReservations', end: true },
  { path: '/credits', icon: CreditCard, labelKey: 'nav.credits', end: false },
  { path: '/profile', icon: User, labelKey: 'nav.profile', end: false },
]

const adminNavItems = [
  { path: '/', icon: Home, labelKey: 'nav.home', end: true },
  { path: '/calendar', icon: Calendar, labelKey: 'nav.newReservation', end: true },
  { path: '/admin/clients', icon: Users, labelKey: 'admin.clientsTitle', end: false },
  { path: '/profile', icon: User, labelKey: 'nav.profile', end: false },
]

export default function BottomNav() {
  const { t } = useTranslation()
  const { user } = useAuthStore()
  const isAdmin = user?.role === 'admin'
  const items = isAdmin ? adminNavItems : navItems

  return (
    <nav aria-label={t('nav.mobileNavigation', 'Mobile navigation')} className="fixed bottom-0 left-0 right-0 z-40 min-h-[72px] border-t border-neutral-200 bg-white/95 pb-safe shadow-[0_-12px_30px_rgba(0,0,0,0.06)] backdrop-blur dark:border-dark-border dark:bg-dark-surface/95 md:hidden">
      <div className="flex items-center justify-around py-2">
        {items.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            end={item.end}
            className={({ isActive }) =>
              cn(
                'flex min-h-[56px] flex-col items-center justify-center w-full h-full gap-1 transition-colors',
                isActive
                  ? 'text-primary-500'
                  : 'text-neutral-500 dark:text-neutral-400'
              )
            }
          >
            {({ isActive }) => (
              <>
                <item.icon
                  size={20}
                  strokeWidth={isActive ? 2.5 : 2}
                  className={isActive ? 'fill-primary-100 dark:fill-primary-900/30' : ''}
                />
                <span className="text-[11px] font-medium">{t(item.labelKey)}</span>
              </>
            )}
          </NavLink>
        ))}
      </div>
    </nav>
  )
}
