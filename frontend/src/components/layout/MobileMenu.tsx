import { NavLink, useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { X, CreditCard, LogOut, Users, LayoutTemplate, Dumbbell, Tag, DollarSign } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/utils/cn'
import { useAuthStore } from '@/stores/authStore'
import ThemeToggle from './ThemeToggle'
import LanguageSwitch from './LanguageSwitch'

interface MobileMenuProps {
  isOpen: boolean
  onClose: () => void
}

const adminMenuItems = [
  { path: '/admin/clients', icon: Users, labelKey: 'admin.clients' },
  { path: '/admin/templates', icon: LayoutTemplate, labelKey: 'admin.templates' },
  { path: '/admin/plans', icon: Dumbbell, labelKey: 'admin.plans' },
  { path: '/admin/pricing', icon: Tag, labelKey: 'admin.pricing' },
  { path: '/admin/payments', icon: DollarSign, labelKey: 'admin.payments' },
]

export default function MobileMenu({ isOpen, onClose }: MobileMenuProps) {
  const { t } = useTranslation()
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
            className="fixed inset-y-0 right-0 z-50 w-full max-w-xs glass dark:glass-dark"
          >
            <div className="flex flex-col h-full">
              {/* Header */}
              <div className="flex items-center justify-between px-5 py-4 border-b border-white/10">
                <span className="font-heading font-semibold text-lg text-neutral-900 dark:text-white">
                  {t('profile.settings')}
                </span>
                <button
                  onClick={onClose}
                  className="p-2 -mr-2 rounded-lg text-neutral-500 hover:text-neutral-700 hover:bg-neutral-100/50 dark:text-neutral-400 dark:hover:text-white dark:hover:bg-white/10 transition-colors touch-target"
                >
                  <X size={24} />
                </button>
              </div>

              {/* User info */}
              {user && (
                <div className="px-5 py-4 border-b border-white/10">
                  <p className="font-medium text-neutral-900 dark:text-white">
                    {user.firstName} {user.lastName}
                  </p>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400">
                    {user.email}
                  </p>
                  <div className="mt-2 inline-flex items-center gap-1.5 px-2.5 py-1 bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-400 rounded-full text-sm font-medium">
                    <CreditCard size={14} />
                    {user.credits} {t('credits.title').toLowerCase()}
                  </div>
                </div>
              )}

              {/* Content */}
              <div className="flex-1 overflow-y-auto">
                {/* Admin section */}
                {isAdmin && (
                  <div className="px-3 py-4 border-b border-white/10">
                    <p className="px-4 mb-2 text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">
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
                                ? 'bg-primary-500 text-white'
                                : 'text-neutral-700 dark:text-neutral-300 hover:bg-neutral-100/50 dark:hover:bg-white/10'
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
                    <label className="block text-sm font-medium text-neutral-500 dark:text-neutral-400 mb-2">
                      {t('profile.language')}
                    </label>
                    <LanguageSwitch />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-neutral-500 dark:text-neutral-400 mb-2">
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
                  className="flex items-center gap-3 w-full px-4 py-3 rounded-lg text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors touch-target"
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
