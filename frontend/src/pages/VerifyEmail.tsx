import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
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

  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const verifyToken = async () => {
      if (!token) {
        setStatus('error')
        setError('Chybí ověřovací token')
        return
      }

      try {
        const response = await authApi.verifyEmail(token)
        setAuth(response)
        setStatus('success')
      } catch (err: unknown) {
        setStatus('error')
        const error = err as { response?: { data?: { message?: string } } }
        setError(error.response?.data?.message || 'Nepodařilo se ověřit email')
      }
    }

    verifyToken()
  }, [token, setAuth])

  return (
    <div className="min-h-screen bg-neutral-50 dark:bg-dark-bg flex flex-col">
      <div className="flex items-center justify-between p-4">
        <span className="font-heading font-bold text-xl text-neutral-900 dark:text-white">
          Fitness
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
                  Ověřování emailu...
                </h1>
                <p className="text-neutral-600 dark:text-neutral-400">
                  Počkejte prosím, ověřujeme váš email.
                </p>
              </>
            )}

            {status === 'success' && (
              <>
                <div className="mb-4 mx-auto w-16 h-16 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
                  <CheckCircle className="w-8 h-8 text-green-500" />
                </div>
                <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white mb-2">
                  Email ověřen!
                </h1>
                <p className="text-neutral-600 dark:text-neutral-400 mb-6">
                  Váš účet byl úspěšně aktivován. Nyní se můžete přihlásit.
                </p>
                <Link to="/">
                  <Button variant="primary" className="w-full">
                    Pokračovat do aplikace
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
                  Ověření selhalo
                </h1>
                <p className="text-neutral-600 dark:text-neutral-400 mb-6">
                  {error || 'Nepodařilo se ověřit váš email. Zkuste to prosím znovu.'}
                </p>
                <Link to="/login">
                  <Button variant="primary" className="w-full">
                    Zpět na přihlášení
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
