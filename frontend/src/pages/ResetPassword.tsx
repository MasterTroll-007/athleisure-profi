import { useState, useEffect } from 'react'
import { Link, useSearchParams, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useTranslation } from 'react-i18next'
import { Lock, Eye, EyeOff, CheckCircle, XCircle, ArrowLeft } from 'lucide-react'
import { Button, Input, Card } from '@/components/ui'
import { authApi } from '@/services/api'
import ThemeToggle from '@/components/layout/ThemeToggle'
import LanguageSwitch from '@/components/layout/LanguageSwitch'

const resetPasswordSchema = z.object({
  password: z
    .string()
    .min(8)
    .regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/),
  confirmPassword: z.string().min(1),
}).refine((data) => data.password === data.confirmPassword, {
  message: "Passwords don't match",
  path: ['confirmPassword'],
})

type ResetPasswordForm = z.infer<typeof resetPasswordSchema>

export default function ResetPassword() {
  const { t } = useTranslation()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const token = searchParams.get('token')

  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordForm>({
    resolver: zodResolver(resetPasswordSchema),
  })

  useEffect(() => {
    if (!token) {
      setError(t('auth.invalidResetLink'))
    }
  }, [token, t])

  const onSubmit = async (data: ResetPasswordForm) => {
    if (!token) {
      setError(t('auth.invalidResetLink'))
      return
    }

    try {
      setError(null)
      await authApi.resetPassword(token, data.password)
      setSuccess(true)
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } }
      if (error?.response?.data?.message?.includes('expired')) {
        setError(t('auth.resetTokenExpired'))
      } else if (error?.response?.data?.message?.includes('Invalid')) {
        setError(t('auth.invalidResetLink'))
      } else {
        setError(t('errors.somethingWrong'))
      }
    }
  }

  // Invalid token state
  if (!token && !success) {
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
              <div className="mx-auto w-16 h-16 bg-red-100 dark:bg-red-900/20 rounded-full flex items-center justify-center mb-6">
                <XCircle className="w-8 h-8 text-red-600 dark:text-red-400" />
              </div>
              <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white mb-4">
                {t('auth.invalidResetLink')}
              </h1>
              <p className="text-neutral-600 dark:text-neutral-400 mb-6">
                {t('auth.invalidResetLinkDesc')}
              </p>
              <Link
                to="/forgot-password"
                className="inline-flex items-center justify-center gap-2 w-full px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 font-medium"
              >
                {t('auth.requestNewLink')}
              </Link>
              <Link
                to="/login"
                className="inline-flex items-center gap-2 mt-4 text-primary-500 hover:text-primary-600 font-medium"
              >
                <ArrowLeft size={18} />
                {t('register.backToLogin')}
              </Link>
            </div>
          </Card>
        </div>
      </div>
    )
  }

  // Success state
  if (success) {
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
              <div className="mx-auto w-16 h-16 bg-green-100 dark:bg-green-900/20 rounded-full flex items-center justify-center mb-6">
                <CheckCircle className="w-8 h-8 text-green-600 dark:text-green-400" />
              </div>
              <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white mb-4">
                {t('auth.passwordResetSuccess')}
              </h1>
              <p className="text-neutral-600 dark:text-neutral-400 mb-6">
                {t('auth.passwordResetSuccessDesc')}
              </p>
              <Button
                onClick={() => navigate('/login')}
                className="w-full"
              >
                {t('auth.loginButton')}
              </Button>
            </div>
          </Card>
        </div>
      </div>
    )
  }

  // Reset password form
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
          <div className="text-center mb-6">
            <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
              {t('auth.resetPasswordTitle')}
            </h1>
            <p className="text-neutral-600 dark:text-neutral-400 mt-2">
              {t('auth.resetPasswordDesc')}
            </p>
          </div>

          {error && (
            <div className="mb-6 p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg text-red-600 dark:text-red-400 text-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <Input
              label={t('profile.newPassword')}
              type={showPassword ? 'text' : 'password'}
              placeholder="********"
              leftIcon={<Lock size={18} />}
              rightIcon={
                <button
                  type="button"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => setShowPassword(!showPassword)}
                  className="text-neutral-400 hover:text-neutral-600 dark:hover:text-neutral-300"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              }
              error={
                errors.password?.message &&
                (errors.password.type === 'too_small'
                  ? t('errors.passwordTooShort')
                  : t('errors.passwordRequirements'))
              }
              {...register('password')}
            />

            <Input
              label={t('auth.confirmPassword')}
              type={showConfirmPassword ? 'text' : 'password'}
              placeholder="********"
              leftIcon={<Lock size={18} />}
              rightIcon={
                <button
                  type="button"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="text-neutral-400 hover:text-neutral-600 dark:hover:text-neutral-300"
                >
                  {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              }
              error={errors.confirmPassword?.message && t('errors.passwordsDontMatch')}
              {...register('confirmPassword')}
            />

            <Button type="submit" className="w-full" isLoading={isSubmitting}>
              {t('auth.resetPasswordButton')}
            </Button>
          </form>

          <div className="mt-6 text-center">
            <Link
              to="/login"
              className="inline-flex items-center gap-2 text-sm text-primary-500 hover:text-primary-600 font-medium"
            >
              <ArrowLeft size={16} />
              {t('register.backToLogin')}
            </Link>
          </div>
        </Card>
      </div>
    </div>
  )
}
