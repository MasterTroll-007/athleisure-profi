import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'

export default function CookieConsent() {
  const { t } = useTranslation()
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    const consent = localStorage.getItem('cookieConsent')
    if (!consent) {
      setVisible(true)
    }
  }, [])

  const handleAccept = () => {
    localStorage.setItem('cookieConsent', 'accepted')
    setVisible(false)
  }

  if (!visible) return null

  return (
    <div className="fixed bottom-0 left-0 right-0 z-50 p-4 bg-white dark:bg-dark-surface border-t border-neutral-200 dark:border-dark-border shadow-lg">
      <div className="max-w-4xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-3">
        <p className="text-sm text-neutral-700 dark:text-neutral-300">
          {t('cookie.message')}
        </p>
        <div className="flex items-center gap-3 shrink-0">
          <Link
            to="/privacy"
            className="text-sm text-primary-500 hover:text-primary-600 underline"
          >
            {t('cookie.moreInfo')}
          </Link>
          <button
            onClick={handleAccept}
            className="px-4 py-2 text-sm font-medium text-white bg-primary-500 hover:bg-primary-600 rounded-lg transition-colors"
          >
            {t('cookie.accept')}
          </button>
        </div>
      </div>
    </div>
  )
}
