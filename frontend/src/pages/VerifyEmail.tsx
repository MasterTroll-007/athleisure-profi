import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { CheckCircle, XCircle, Loader2 } from 'lucide-react'
import { Button, Card } from '@/components/ui'
import { authApi } from '@/services/api'
import { useAuthStore } from '@/stores/authStore'
import ThemeToggle from '@/components/layout/ThemeToggle'
import LanguageSwitch from '@/components/layout/LanguageSwitch'

export default function VerifyEmail() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const { setAuth } = useAuthStore()
  const { t } = useTranslation()

  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const verifyToken = async () => {
      if (!token) {
        setStatus('error')
        setError(t('verifyEmail.missingToken'))
        return
      }

      try {
        const response = await authApi.verifyEmail(token)
        setAuth(response)
        setStatus('success')
      } catch (err: unknown) {
        setStatus('error')
        const error = err as { response?: { data?: { message?: string } } }
        setError(error.response?.data?.message || t('verifyEmail.errorMessage'))
      }
    }

    verifyToken()
  }, [token, setAuth, t])

  return (
    <div className="app-stage min-h-screen flex flex-col">
      <div className="flex items-center justify-between p-4">
        <span className="font-heading font-bold text-xl text-white">
          {t('common.appName', 'Fitness Rezervace')}
        </span>
        <div className="flex items-center gap-2">
          <LanguageSwitch />
          <ThemeToggle />
        </div>
      </div>
      <div className="flex-1 flex items-center justify-center p-4">
        <Card variant="bordered" className="w-full max-w-md" padding="lg">
          <div className="text-center">
            {status === 'loading' && (
              <>
                <div className="mb-4 mx-auto w-16 h-16 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
                  <Loader2 className="w-8 h-8 text-primary-500 animate-spin" />
                </div>
                <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white mb-2">
                  {t('verifyEmail.verifying')}
                </h1>
                <p className="text-neutral-600 dark:text-neutral-400">
                  {t('verifyEmail.pleaseWait')}
                </p>
              </>
            )}

            {status === 'success' && (
              <>
                <div className="mb-4 mx-auto w-16 h-16 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
                  <CheckCircle className="w-8 h-8 text-green-500" />
                </div>
                <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white mb-2">
                  {t('verifyEmail.success')}
                </h1>
                <p className="text-neutral-600 dark:text-neutral-400 mb-6">
                  {t('verifyEmail.successMessage')}
                </p>
                <Link to="/">
                  <Button variant="primary" className="w-full">
                    {t('verifyEmail.continue')}
                  </Button>
                </Link>
              </>
            )}

            {status === 'error' && (
              <>
                <div className="mb-4 mx-auto w-16 h-16 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
                  <XCircle className="w-8 h-8 text-red-500" />
                </div>
                <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white mb-2">
                  {t('verifyEmail.failed')}
                </h1>
                <p className="text-neutral-600 dark:text-neutral-400 mb-6">
                  {error || t('verifyEmail.errorMessage')}
                </p>
                <Link to="/login">
                  <Button variant="primary" className="w-full">
                    {t('verifyEmail.backToLogin')}
                  </Button>
                </Link>
              </>
            )}
          </div>
        </Card>
      </div>
    </div>
  )
}
