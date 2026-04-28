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
    <div className="fixed bottom-4 left-4 right-4 z-50 rounded-xl border border-white/10 bg-neutral-950/90 p-3 shadow-lg backdrop-blur sm:left-1/2 sm:right-auto sm:w-[min(34rem,calc(100vw-2rem))] sm:-translate-x-1/2">
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
        <p className="text-sm text-white/85">
          {t('cookie.message')}
        </p>
        <div className="flex items-center gap-3 shrink-0">
          <Link
            to="/privacy"
            className="text-sm text-primary-200 hover:text-white underline"
          >
            {t('cookie.moreInfo')}
          </Link>
          <button
            onClick={handleAccept}
            className="btn-metal btn-metal-silver rounded-lg px-4 py-2 text-sm font-semibold"
          >
            <span className="relative z-10">{t('cookie.accept')}</span>
          </button>
        </div>
      </div>
    </div>
  )
}
