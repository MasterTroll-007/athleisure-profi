import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useTranslation } from 'react-i18next'
import { Mail, Lock, Eye, EyeOff } from 'lucide-react'
import { Button, Input, Card } from '@/components/ui'
import { authApi } from '@/services/api'
import { useAuthStore } from '@/stores/authStore'
import ThemeToggle from '@/components/layout/ThemeToggle'
import LanguageSwitch from '@/components/layout/LanguageSwitch'

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
})

type LoginForm = z.infer<typeof loginSchema>

export default function Login() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { setAuth } = useAuthStore()
  const [showPassword, setShowPassword] = useState(false)
  const [rememberMe, setRememberMe] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
  })

  const onSubmit = async (data: LoginForm) => {
    try {
      setError(null)
      const response = await authApi.login(data.email, data.password, rememberMe)
      setAuth(response)
      navigate('/')
    } catch {
      setError(t('errors.invalidCredentials'))
    }
  }

  return (
    <div className="min-h-screen bg-neutral-50 dark:bg-dark-bg flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between p-4">
        <span className="font-heading font-bold text-xl text-neutral-900 dark:text-white">
          Fitness
        </span>
        <div className="flex items-center gap-2">
          <LanguageSwitch />
          <ThemeToggle />
        </div>
      </div>

      {/* Form */}
      <div className="flex-1 flex items-center justify-center p-4">
        <Card variant="bordered" className="w-full max-w-md" padding="lg">
          <div className="text-center mb-8">
            <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
              {t('auth.login')}
            </h1>
          </div>

          {error && (
            <div className="mb-6 p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg text-red-600 dark:text-red-400 text-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <Input
              label={t('auth.email')}
              type="email"
              placeholder="email@example.com"
              leftIcon={<Mail size={18} />}
              error={errors.email?.message && t('errors.invalidEmail')}
              {...register('email')}
            />

            <Input
              label={t('auth.password')}
              type={showPassword ? 'text' : 'password'}
              placeholder="********"
              leftIcon={<Lock size={18} />}
              rightIcon={
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="text-neutral-400 hover:text-neutral-600 dark:hover:text-neutral-300"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              }
              error={errors.password?.message && t('errors.required')}
              {...register('password')}
            />

            <div className="flex items-center justify-between">
              <label className="flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  className="w-4 h-4 rounded border-neutral-300 dark:border-neutral-600 text-primary-500 focus:ring-2 focus:ring-primary-500 focus:ring-offset-0 dark:bg-dark-card dark:checked:bg-primary-500 cursor-pointer"
                />
                <span className="ml-2 text-sm text-neutral-700 dark:text-neutral-300">
                  {t('auth.rememberMe')}
                </span>
              </label>

              <button
                type="button"
                className="text-sm text-primary-500 hover:text-primary-600 dark:text-primary-400"
                onClick={() => alert(t('common.comingSoon') || 'Tato funkce bude brzy k dispozici')}
              >
                {t('auth.forgotPassword')}
              </button>
            </div>

            <Button type="submit" className="w-full" isLoading={isSubmitting}>
              {t('auth.loginButton')}
            </Button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-neutral-600 dark:text-neutral-400">
              {t('auth.noAccount')}{' '}
              <Link
                to="/register"
                className="font-medium text-primary-500 hover:text-primary-600"
              >
                {t('auth.registerButton')}
              </Link>
            </p>
          </div>
        </Card>
      </div>
    </div>
  )
}
