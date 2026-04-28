import ThemeToggle from './ThemeToggle'
import LanguageSwitch from './LanguageSwitch'
import { useTranslation } from 'react-i18next'

interface AuthLayoutProps {
  children: React.ReactNode
}

export default function AuthLayout({ children }: AuthLayoutProps) {
  const { t } = useTranslation()

  return (
    <div className="app-stage min-h-screen flex flex-col">
      <div className="flex items-center justify-between p-4 flex-shrink-0">
        <span className="font-heading font-bold text-xl text-white">
          {t('common.appName')}
        </span>
        <div className="flex items-center gap-2">
          <LanguageSwitch />
          <ThemeToggle />
        </div>
      </div>
      <div className="flex-1 flex items-center justify-center p-4 min-h-0">
        <div className="w-full max-h-full overflow-y-auto flex justify-center">
          {children}
        </div>
      </div>
    </div>
  )
}
