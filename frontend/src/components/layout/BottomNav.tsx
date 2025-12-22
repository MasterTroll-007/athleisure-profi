import { NavLink, useLocation } from 'react-router-dom'
import { Home, CalendarPlus, Calendar, User } from 'lucide-react'
import { cn } from '@/utils/cn'

const navItems = [
  { path: '/', icon: Home, shortLabel: 'Domů' },
  { path: '/reservations/new', icon: CalendarPlus, shortLabel: 'Nová' },
  { path: '/reservations', icon: Calendar, shortLabel: 'Moje' },
  { path: '/profile', icon: User, shortLabel: 'Profil' },
]

export default function BottomNav() {
  const location = useLocation()

  const isItemActive = (path: string) => {
    // Exact match for all paths
    return location.pathname === path
  }

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 glass dark:glass-dark border-t border-white/10 md:hidden">
      <div className="flex items-center justify-around h-16 pb-safe">
        {navItems.map((item) => {
          const isActive = isItemActive(item.path)
          return (
            <NavLink
              key={item.path}
              to={item.path}
              className={cn(
                'flex flex-col items-center justify-center w-full h-full gap-1 transition-colors',
                isActive
                  ? 'text-primary-500'
                  : 'text-neutral-500 dark:text-neutral-400'
              )}
            >
              <item.icon
                size={22}
                strokeWidth={isActive ? 2.5 : 2}
                className={isActive ? 'fill-primary-100 dark:fill-primary-900/30' : ''}
              />
              <span className="text-[10px] font-medium">{item.shortLabel}</span>
            </NavLink>
          )
        })}
      </div>
    </nav>
  )
}
