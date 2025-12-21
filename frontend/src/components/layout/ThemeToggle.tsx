import { Sun, Moon } from 'lucide-react'
import { motion } from 'framer-motion'
import { useThemeStore } from '@/stores/themeStore'
import { useTranslation } from 'react-i18next'
import { cn } from '@/utils/cn'

interface ThemeToggleProps {
  showLabel?: boolean
}

export default function ThemeToggle({ showLabel = false }: ThemeToggleProps) {
  const { t } = useTranslation()
  const { theme, setTheme, resolvedTheme } = useThemeStore()

  const toggleTheme = () => {
    if (theme === 'system') {
      setTheme(resolvedTheme === 'dark' ? 'light' : 'dark')
    } else {
      setTheme(theme === 'dark' ? 'light' : 'dark')
    }
  }

  const isDark = resolvedTheme === 'dark'

  return (
    <button
      onClick={toggleTheme}
      className={cn(
        'relative p-2 rounded-lg transition-colors touch-target',
        'text-neutral-600 hover:bg-neutral-100/50',
        'dark:text-neutral-300 dark:hover:bg-white/10',
        showLabel && 'flex items-center gap-2'
      )}
      aria-label={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
    >
      <div className="relative w-5 h-5">
        <motion.div
          initial={false}
          animate={{
            scale: isDark ? 0 : 1,
            rotate: isDark ? 90 : 0,
            opacity: isDark ? 0 : 1,
          }}
          transition={{ duration: 0.2 }}
          className="absolute inset-0"
        >
          <Sun size={20} />
        </motion.div>
        <motion.div
          initial={false}
          animate={{
            scale: isDark ? 1 : 0,
            rotate: isDark ? 0 : -90,
            opacity: isDark ? 1 : 0,
          }}
          transition={{ duration: 0.2 }}
          className="absolute inset-0"
        >
          <Moon size={20} />
        </motion.div>
      </div>
      {showLabel && (
        <span className="text-sm font-medium">
          {isDark ? t('profile.themeDark') : t('profile.themeLight')}
        </span>
      )}
    </button>
  )
}
