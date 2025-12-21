import { useTranslation } from 'react-i18next'
import { cn } from '@/utils/cn'

export default function LanguageSwitch() {
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
    <div className="flex items-center gap-1 p-1 bg-neutral-100 dark:bg-dark-surface rounded-lg">
      {languages.map((lang) => (
        <button
          key={lang.code}
          onClick={() => changeLanguage(lang.code)}
          className={cn(
            'px-3 py-1.5 text-sm font-medium rounded-md transition-colors',
            i18n.language === lang.code
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
