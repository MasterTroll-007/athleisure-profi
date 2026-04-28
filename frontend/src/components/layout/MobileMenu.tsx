import { NavLink, useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { X, CreditCard, LogOut } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/utils/cn'
import { useAuthStore } from '@/stores/authStore'
import { formatCredits } from '@/utils/formatters'
import ThemeToggle from './ThemeToggle'
import LanguageSwitch from './LanguageSwitch'
import { adminMenuItems } from './adminNavConfig'

interface MobileMenuProps {
  isOpen: boolean
  onClose: () => void
}

export default function MobileMenu({ isOpen, onClose }: MobileMenuProps) {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const { user, logout } = useAuthStore()

  const isAdmin = user?.role === 'admin'

  const handleLogout = () => {
    logout()
    onClose()
    navigate('/login')
  }

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="fixed inset-0 z-50 bg-black/50"
            onClick={onClose}
          />

          {/* Menu */}
          <motion.div
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ type: 'tween', duration: 0.3, ease: 'easeOut' }}
            className="fixed inset-y-0 right-0 z-50 w-full max-w-xs border-l border-white/10 bg-[#07060d]/94 shadow-[0_30px_100px_rgba(0,0,0,0.7)] backdrop-blur-xl"
          >
            <div className="flex flex-col h-full">
              {/* Header */}
              <div className="flex items-center justify-between px-5 py-4 border-b border-white/10">
                <span className="font-heading font-semibold text-lg text-white">
                  {t('profile.settings')}
                </span>
                <button
                  onClick={onClose}
                  className="p-2 -mr-2 rounded-lg text-white/55 hover:text-white hover:bg-white/10 transition-colors touch-target"
                >
                  <X size={24} />
                </button>
              </div>

              {/* User info */}
              {user && (
                <div className="px-5 py-4 border-b border-white/10">
                  <p className="font-medium text-white">
                    {user.firstName} {user.lastName}
                  </p>
                  <p className="text-sm text-white/50">
                    {user.email}
                  </p>
                  <div className="mt-2 inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full border border-primary-300/20 bg-primary-300/12 text-primary-100 text-sm font-medium">
                    <CreditCard size={14} />
                    {formatCredits(user.credits, i18n.language)}
                  </div>
                </div>
              )}

              {/* Content */}
              <div className="flex-1 overflow-y-auto">
                {/* Admin section */}
                {isAdmin && (
                  <div className="px-3 py-4 border-b border-white/10">
                    <p className="px-4 mb-2 text-xs font-medium text-white/45 uppercase tracking-wider">
                      {t('nav.admin')}
                    </p>
                    <nav className="space-y-1">
                      {adminMenuItems.map((item) => (
                        <NavLink
                          key={item.path}
                          to={item.path}
                          onClick={onClose}
                          className={({ isActive }) =>
                            cn(
                              'flex items-center gap-3 px-4 py-3 rounded-lg transition-colors touch-target',
                              isActive
                                ? 'bg-white/12 text-primary-100 ring-1 ring-primary-300/20'
                                : 'text-white/70 hover:bg-white/10 hover:text-white'
                            )
                          }
                        >
                          <item.icon size={20} />
                          <span className="font-medium">{t(item.labelKey)}</span>
                        </NavLink>
                      ))}
                    </nav>
                  </div>
                )}

                {/* Settings */}
                <div className="px-5 py-6 space-y-6">
                  <div>
                    <label className="block text-sm font-medium text-white/50 mb-2">
                      {t('profile.language')}
                    </label>
                    <LanguageSwitch />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-white/50 mb-2">
                      {t('profile.theme')}
                    </label>
                    <ThemeToggle showLabel />
                  </div>
                </div>
              </div>

              {/* Footer */}
              <div className="px-5 py-4 border-t border-white/10">
                <button
                  onClick={handleLogout}
                  data-testid="logout-button"
                  className="flex items-center gap-3 w-full px-4 py-3 rounded-lg text-red-200 hover:bg-red-400/10 transition-colors touch-target"
                >
                  <LogOut size={20} />
                  <span className="font-medium">{t('nav.logout')}</span>
                </button>
              </div>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  )
}
