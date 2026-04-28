import { useTranslation } from 'react-i18next'
import { cn } from '@/utils/cn'

interface LanguageSwitchProps {
  compact?: boolean
  tone?: 'default' | 'header'
  className?: string
}

export default function LanguageSwitch({ compact = false, tone = 'default', className }: LanguageSwitchProps) {
  const { i18n } = useTranslation()

  const languages = [
    { code: 'cs', label: 'CZ' },
    { code: 'en', label: 'EN' },
  ]

  const changeLanguage = (lng: string) => {
    i18n.changeLanguage(lng)
    localStorage.setItem('locale', lng)
  }

  return (
    <div
      className={cn(
        'flex items-center gap-1 rounded-lg p-1',
        tone === 'header'
          ? 'bg-white/10 ring-1 ring-white/10'
          : 'bg-neutral-100 dark:bg-dark-surface',
        className
      )}
    >
      {languages.map((lang) => (
        <button
          key={lang.code}
          onClick={() => changeLanguage(lang.code)}
          className={cn(
            'rounded-md font-medium transition-colors',
            compact ? 'px-2 py-1 text-xs' : 'px-3 py-1.5 text-sm',
            tone === 'header'
              ? i18n.language === lang.code
                ? 'bg-white text-neutral-950 shadow-sm'
                : 'text-white/65 hover:bg-white/10 hover:text-white'
              : i18n.language === lang.code
                ? 'bg-white dark:bg-dark-surfaceHover text-neutral-900 dark:text-white shadow-sm'
                : 'text-neutral-500 dark:text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-300'
          )}
        >
          {lang.label}
        </button>
      ))}
    </div>
  )
}
