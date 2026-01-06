import { NavLink } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Home, Calendar, List, CreditCard, User } from 'lucide-react'
import { cn } from '@/utils/cn'

const navItems = [
  { path: '/', icon: Home, labelKey: 'nav.home', end: true },
  { path: '/calendar', icon: Calendar, labelKey: 'nav.newReservation', end: true },
  { path: '/reservations', icon: List, labelKey: 'nav.myReservations', end: true },
  { path: '/credits', icon: CreditCard, labelKey: 'nav.credits', end: false },
  { path: '/profile', icon: User, labelKey: 'nav.profile', end: false },
]

export default function BottomNav() {
  const { t } = useTranslation()

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 glass dark:glass-dark border-t border-white/10 pb-safe md:hidden">
      <div className="flex items-center justify-around py-2">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            end={item.end}
            className={({ isActive }) =>
              cn(
                'flex flex-col items-center justify-center w-full h-full gap-1 transition-colors',
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
                <span className="text-[10px] font-medium">{t(item.labelKey)}</span>
              </>
            )}
          </NavLink>
        ))}
      </div>
    </nav>
  )
}
