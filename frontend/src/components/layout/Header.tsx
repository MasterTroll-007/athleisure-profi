import { useState, useEffect, useRef } from 'react'
import { Link, NavLink, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Menu, Home, Calendar, List, CreditCard, User, Settings, ChevronDown, LogOut } from 'lucide-react'
import { cn } from '@/utils/cn'
import { useAuthStore } from '@/stores/authStore'
import ThemeToggle from './ThemeToggle'
import MobileMenu from './MobileMenu'
import { adminMenuItems } from './adminNavConfig'

// Client-only entries: admins don't book or buy credits, so these would only
// lead to empty screens for them.
const CLIENT_ONLY_PATHS = new Set(['/reservations', '/credits'])

const userNavItems = [
  { path: '/', icon: Home, labelKey: 'nav.home' },
  { path: '/calendar', icon: Calendar, labelKey: 'nav.newReservation' },
  { path: '/reservations', icon: List, labelKey: 'nav.myReservations' },
  { path: '/credits', icon: CreditCard, labelKey: 'nav.credits' },
  { path: '/profile', icon: User, labelKey: 'nav.profile' },
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
  const visibleNavItems = isAdmin
    ? userNavItems.filter((item) => !CLIENT_ONLY_PATHS.has(item.path))
    : userNavItems

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
          isScrolled ? 'bg-[#05040a]/82 backdrop-blur-xl shadow-sm border-b border-white/10' : 'bg-transparent'
        )}
      >
        <div className="h-full max-w-7xl mx-auto px-4 flex items-center justify-between">
          {/* Logo */}
          <Link
            to="/"
            className="font-heading font-bold text-xl text-white"
          >
            Fitness Domi
          </Link>

          {/* Desktop navigation - hidden on mobile */}
          <nav className="hidden md:flex items-center gap-1">
            {visibleNavItems.map((item) => {
              const isActive = isItemActive(item.path)
              return (
                <NavLink
                  key={item.path}
                  to={item.path}
                  className={cn(
                    'flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors',
                    isActive
                      ? 'bg-white/12 text-primary-100 ring-1 ring-primary-300/20'
                      : 'text-white/62 hover:bg-white/8 hover:text-white'
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
                  aria-expanded={isAdminDropdownOpen}
                  aria-haspopup="true"
                  className={cn(
                    'flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors',
                    isAdminActive || isAdminDropdownOpen
                      ? 'bg-white/12 text-primary-100 ring-1 ring-primary-300/20'
                      : 'text-white/62 hover:bg-white/8 hover:text-white'
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
                  <div className="absolute top-full right-0 mt-1 w-52 py-2 rounded-lg border border-white/10 bg-[#090810]/95 shadow-[0_25px_80px_-35px_rgba(0,0,0,0.9)] backdrop-blur-xl animate-fade-in">
                    {adminMenuItems.map((item) => {
                      const isActive = isItemActive(item.path)
                      return (
                        <NavLink
                          key={item.path}
                          to={item.path}
                          className={cn(
                            'flex items-center gap-3 px-4 py-2.5 transition-colors',
                            isActive
                              ? 'bg-white/10 text-primary-100'
                              : 'text-white/70 hover:bg-white/8 hover:text-white'
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
                aria-expanded={isProfileDropdownOpen}
                aria-haspopup="true"
                className="flex items-center gap-2 p-2 rounded-lg text-white/70 hover:bg-white/10 hover:text-white transition-colors"
              >
                <div className="w-8 h-8 rounded-full bg-white/10 ring-1 ring-primary-300/20 flex items-center justify-center">
                  <span className="text-primary-100 text-sm font-medium">
                    {(user?.firstName?.[0] || user?.email?.[0] || 'U').toUpperCase()}
                  </span>
                </div>
                <ChevronDown
                  size={16}
                  className={cn('transition-transform', isProfileDropdownOpen && 'rotate-180')}
                />
              </button>

              {isProfileDropdownOpen && (
                <div className="absolute top-full right-0 mt-1 w-48 py-2 rounded-lg border border-white/10 bg-[#090810]/95 shadow-[0_25px_80px_-35px_rgba(0,0,0,0.9)] backdrop-blur-xl animate-fade-in">
                  <div className="px-4 py-2 border-b border-white/10">
                    <p className="text-sm font-medium text-white truncate">
                      {user?.firstName} {user?.lastName}
                    </p>
                    <p className="text-xs text-white/50 truncate">{user?.email}</p>
                  </div>
                  <button
                    onClick={logout}
                    className="w-full flex items-center gap-3 px-4 py-2.5 text-red-200 hover:bg-red-400/10 transition-colors"
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
              className="md:hidden p-2 rounded-lg text-white/70 hover:bg-white/10 hover:text-white transition-colors touch-target"
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
