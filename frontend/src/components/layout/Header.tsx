import { useState, useEffect, useRef } from 'react'
import { Link, NavLink, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Menu, Home, Calendar, List, CreditCard, User, Settings, Users, LayoutTemplate, Dumbbell, Tag, DollarSign, ChevronDown, LogOut } from 'lucide-react'
import { cn } from '@/utils/cn'
import { useAuthStore } from '@/stores/authStore'
import ThemeToggle from './ThemeToggle'
import MobileMenu from './MobileMenu'

const userNavItems = [
  { path: '/', icon: Home, labelKey: 'nav.home' },
  { path: '/calendar', icon: Calendar, labelKey: 'nav.newReservation' },
  { path: '/reservations', icon: List, labelKey: 'nav.myReservations' },
  { path: '/credits', icon: CreditCard, labelKey: 'nav.credits' },
  { path: '/profile', icon: User, labelKey: 'nav.profile' },
]

const adminMenuItems = [
  { path: '/admin/clients', icon: Users, labelKey: 'admin.clients' },
  { path: '/admin/templates', icon: LayoutTemplate, labelKey: 'admin.templates' },
  { path: '/admin/plans', icon: Dumbbell, labelKey: 'admin.plans' },
  { path: '/admin/pricing', icon: Tag, labelKey: 'admin.pricing' },
  { path: '/admin/payments', icon: DollarSign, labelKey: 'admin.payments' },
  { path: '/admin/settings', icon: Settings, labelKey: 'admin.settings.title' },
]

export default function Header() {
  const { t } = useTranslation()
  const location = useLocation()
  const { user, logout } = useAuthStore()
  const [isScrolled, setIsScrolled] = useState(false)
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const [isAdminDropdownOpen, setIsAdminDropdownOpen] = useState(false)
  const [isProfileDropdownOpen, setIsProfileDropdownOpen] = useState(false)
  const adminDropdownRef = useRef<HTMLDivElement>(null)
  const profileDropdownRef = useRef<HTMLDivElement>(null)

  const isAdmin = user?.role === 'admin'

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 10)
    }

    window.addEventListener('scroll', handleScroll, { passive: true })
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  // Close dropdowns when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (adminDropdownRef.current && !adminDropdownRef.current.contains(event.target as Node)) {
        setIsAdminDropdownOpen(false)
      }
      if (profileDropdownRef.current && !profileDropdownRef.current.contains(event.target as Node)) {
        setIsProfileDropdownOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  // Close dropdowns on route change
  useEffect(() => {
    setIsAdminDropdownOpen(false)
    setIsProfileDropdownOpen(false)
  }, [location.pathname])

  const isItemActive = (path: string) => {
    return location.pathname === path
  }

  const isAdminActive = location.pathname.startsWith('/admin')

  return (
    <>
      <header
        className={cn(
          'fixed top-0 left-0 right-0 z-40 transition-all duration-200',
          'h-14',
          isScrolled ? 'glass dark:glass-dark shadow-sm' : 'bg-transparent'
        )}
      >
        <div className="h-full max-w-7xl mx-auto px-4 flex items-center justify-between">
          {/* Logo */}
          <Link
            to="/"
            className="font-heading font-bold text-xl text-neutral-900 dark:text-white"
          >
            Fitness
          </Link>

          {/* Desktop navigation - hidden on mobile */}
          <nav className="hidden md:flex items-center gap-1">
            {userNavItems.map((item) => {
              const isActive = isItemActive(item.path)
              return (
                <NavLink
                  key={item.path}
                  to={item.path}
                  className={cn(
                    'flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors',
                    isActive
                      ? 'bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300'
                      : 'text-neutral-600 dark:text-neutral-300 hover:bg-neutral-100 dark:hover:bg-white/10'
                  )}
                >
                  <item.icon size={18} />
                  <span>{t(item.labelKey)}</span>
                </NavLink>
              )
            })}

            {/* Admin dropdown for desktop */}
            {isAdmin && (
              <div ref={adminDropdownRef} className="relative">
                <button
                  onClick={() => setIsAdminDropdownOpen(!isAdminDropdownOpen)}
                  className={cn(
                    'flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors',
                    isAdminActive || isAdminDropdownOpen
                      ? 'bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300'
                      : 'text-neutral-600 dark:text-neutral-300 hover:bg-neutral-100 dark:hover:bg-white/10'
                  )}
                >
                  <Settings size={18} />
                  <span>{t('nav.admin')}</span>
                  <ChevronDown
                    size={16}
                    className={cn('transition-transform', isAdminDropdownOpen && 'rotate-180')}
                  />
                </button>

                {isAdminDropdownOpen && (
                  <div className="absolute top-full right-0 mt-1 w-52 py-2 bg-white dark:bg-dark-surface rounded-lg shadow-lg border border-neutral-200 dark:border-neutral-700 animate-fade-in">
                    {adminMenuItems.map((item) => {
                      const isActive = isItemActive(item.path)
                      return (
                        <NavLink
                          key={item.path}
                          to={item.path}
                          className={cn(
                            'flex items-center gap-3 px-4 py-2.5 transition-colors',
                            isActive
                              ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-400'
                              : 'text-neutral-700 dark:text-neutral-300 hover:bg-neutral-50 dark:hover:bg-dark-surfaceHover'
                          )}
                        >
                          <item.icon size={18} />
                          <span className="text-sm font-medium">{t(item.labelKey)}</span>
                        </NavLink>
                      )
                    })}
                  </div>
                )}
              </div>
            )}
          </nav>

          {/* Right side */}
          <div className="flex items-center gap-2">
            <ThemeToggle />

            {/* Profile dropdown - desktop only */}
            <div ref={profileDropdownRef} className="hidden md:block relative">
              <button
                onClick={() => setIsProfileDropdownOpen(!isProfileDropdownOpen)}
                className="flex items-center gap-2 p-2 rounded-lg text-neutral-600 hover:bg-neutral-100/50 dark:text-neutral-300 dark:hover:bg-white/10 transition-colors"
              >
                <div className="w-8 h-8 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
                  <span className="text-primary-600 dark:text-primary-400 text-sm font-medium">
                    {(user?.firstName?.[0] || user?.email?.[0] || 'U').toUpperCase()}
                  </span>
                </div>
                <ChevronDown
                  size={16}
                  className={cn('transition-transform', isProfileDropdownOpen && 'rotate-180')}
                />
              </button>

              {isProfileDropdownOpen && (
                <div className="absolute top-full right-0 mt-1 w-48 py-2 bg-white dark:bg-dark-surface rounded-lg shadow-lg border border-neutral-200 dark:border-neutral-700 animate-fade-in">
                  <div className="px-4 py-2 border-b border-neutral-200 dark:border-neutral-700">
                    <p className="text-sm font-medium text-neutral-900 dark:text-white truncate">
                      {user?.firstName} {user?.lastName}
                    </p>
                    <p className="text-xs text-neutral-500 truncate">{user?.email}</p>
                  </div>
                  <button
                    onClick={logout}
                    className="w-full flex items-center gap-3 px-4 py-2.5 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                  >
                    <LogOut size={18} />
                    <span className="text-sm font-medium">{t('nav.logout')}</span>
                  </button>
                </div>
              )}
            </div>

            {/* Mobile menu button */}
            <button
              onClick={() => setIsMenuOpen(true)}
              className="md:hidden p-2 rounded-lg text-neutral-600 hover:bg-neutral-100/50 dark:text-neutral-300 dark:hover:bg-white/10 transition-colors touch-target"
              aria-label="Menu"
            >
              <Menu size={24} />
            </button>
          </div>
        </div>
      </header>

      {/* Mobile Menu */}
      <MobileMenu isOpen={isMenuOpen} onClose={() => setIsMenuOpen(false)} />
    </>
  )
}
