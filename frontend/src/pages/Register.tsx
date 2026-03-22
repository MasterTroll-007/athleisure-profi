import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useTranslation } from 'react-i18next'
import { useQuery } from '@tanstack/react-query'
import { Mail, Lock, User, Phone, CheckCircle, AlertCircle } from 'lucide-react'
import { Button, Input, Card } from '@/components/ui'
import { authApi } from '@/services/api'
import AuthLayout from '@/components/layout/AuthLayout'
import FormError from '@/components/ui/FormError'
import PasswordToggleButton from '@/components/ui/PasswordToggleButton'

// Password must be 8+ chars with uppercase, lowercase, and number
const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[A-Za-z\d@$!%*?&]{8,}$/

const registerSchema = z
  .object({
    email: z.string().email(),
    password: z.string()
      .min(8, 'Password must be at least 8 characters')
      .regex(passwordRegex, 'Password must contain uppercase, lowercase and number'),
    confirmPassword: z.string().min(8),
    firstName: z.string().optional(),
    lastName: z.string().optional(),
    phone: z.string().optional(),
    acceptTerms: z.literal(true, { errorMap: () => ({ message: 'Required' }) }),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords don't match",
    path: ['confirmPassword'],
  })

type RegisterForm = z.infer<typeof registerSchema>

export default function Register() {
  const { t } = useTranslation()
  const { trainerCode } = useParams<{ trainerCode: string }>()
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [registeredEmail, setRegisteredEmail] = useState<string | null>(null)

  // Fetch trainer info
  const { data: trainer, isLoading: isLoadingTrainer, error: trainerError } = useQuery({
    queryKey: ['trainer', trainerCode],
    queryFn: () => authApi.getTrainerByCode(trainerCode!),
    enabled: !!trainerCode,
    retry: false,
  })

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
  })

  const onSubmit = async (data: RegisterForm) => {
    if (!trainerCode) return

    try {
      setError(null)
      const response = await authApi.register({
        email: data.email,
        password: data.password,
        firstName: data.firstName,
        lastName: data.lastName,
        phone: data.phone,
        trainerCode: trainerCode,
      })
      setRegisteredEmail(response.email)
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } }
      setError(error.response?.data?.message || t('errors.somethingWrong'))
    }
  }

  const trainerName = trainer
    ? [trainer.firstName, trainer.lastName].filter(Boolean).join(' ') || t('register.trainer')
    : null

  // Loading state
  if (isLoadingTrainer) {
    return (
      <AuthLayout>
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500" />
      </AuthLayout>
    )
  }

  // Invalid trainer code
  if (trainerError || !trainerCode) {
    return (
      <AuthLayout>
        <Card variant="bordered" className="w-full max-w-md" padding="lg">
          <div className="text-center">
            <div className="mb-4 mx-auto w-16 h-16 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
              <AlertCircle className="w-8 h-8 text-red-500" />
            </div>
            <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white mb-2">
              {t('register.invalidCode')}
            </h1>
            <p className="text-neutral-600 dark:text-neutral-400 mb-6">
              {t('register.invalidCodeDesc')}
            </p>
            <Link to="/login">
              <Button variant="secondary" className="w-full">
                {t('auth.loginButton')}
              </Button>
            </Link>
          </div>
        </Card>
      </AuthLayout>
    )
  }

  // Show success message after registration
  if (registeredEmail) {
    return (
      <AuthLayout>
        <Card variant="bordered" className="w-full max-w-md" padding="lg">
          <div className="text-center">
            <div className="mb-4 mx-auto w-16 h-16 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
              <CheckCircle className="w-8 h-8 text-green-500" />
            </div>
            <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white mb-2">
              {t('register.verifyEmail')}
            </h1>
            <p className="text-neutral-600 dark:text-neutral-400 mb-4">
              {t('register.verifyEmailSent', { email: registeredEmail })}
            </p>
            <p className="text-sm text-neutral-500 dark:text-neutral-500 mb-6">
              {t('register.linkValid24h')}
            </p>
            <Link to="/login">
              <Button variant="primary" className="w-full">
                {t('register.backToLogin')}
              </Button>
            </Link>
          </div>
        </Card>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout>
      <Card variant="bordered" className="w-full max-w-md" padding="lg">
        <div className="text-center mb-8">
          <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
            {t('auth.register')}
          </h1>
          {trainerName && (
            <p className="mt-2 text-neutral-600 dark:text-neutral-400">
              {t('register.registerWith')} <strong>{trainerName}</strong>
            </p>
          )}
        </div>

        <FormError message={error} />

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <Input
              label={t('auth.firstName')}
              placeholder="Jana"
              leftIcon={<User size={18} />}
              {...register('firstName')}
            />

            <Input
              label={t('auth.lastName')}
              placeholder="Nováková"
              {...register('lastName')}
            />
          </div>

          <Input
            label={t('auth.email')}
            type="email"
            placeholder="email@example.com"
            leftIcon={<Mail size={18} />}
            error={errors.email?.message && t('errors.invalidEmail')}
            {...register('email')}
          />

          <Input
            label={t('auth.phone')}
            type="tel"
            placeholder="+420 123 456 789"
            leftIcon={<Phone size={18} />}
            {...register('phone')}
          />

          <Input
            label={t('auth.password')}
            type={showPassword ? 'text' : 'password'}
            placeholder="********"
            leftIcon={<Lock size={18} />}
            rightIcon={
              <PasswordToggleButton
                visible={showPassword}
                onToggle={() => setShowPassword(!showPassword)}
              />
            }
            error={errors.password?.message && t('errors.passwordRequirements')}
            {...register('password')}
          />

          <Input
            label={t('auth.confirmPassword')}
            type={showPassword ? 'text' : 'password'}
            placeholder="********"
            leftIcon={<Lock size={18} />}
            error={errors.confirmPassword?.message && t('errors.passwordsDontMatch')}
            {...register('confirmPassword')}
          />

          <div className="flex items-start gap-2">
            <input
              type="checkbox"
              id="acceptTerms"
              className="mt-1 h-4 w-4 rounded border-neutral-300 text-primary-500 focus:ring-primary-500"
              {...register('acceptTerms')}
            />
            <label htmlFor="acceptTerms" className="text-sm text-neutral-600 dark:text-neutral-400">
              {t('register.acceptTerms')}{' '}
              <Link to="/privacy" className="text-primary-500 hover:text-primary-600 underline">
                {t('register.privacyPolicy')}
              </Link>{' '}
              {t('register.and')}{' '}
              <Link to="/terms" className="text-primary-500 hover:text-primary-600 underline">
                {t('register.termsOfService')}
              </Link>
            </label>
          </div>
          {errors.acceptTerms && (
            <p className="text-sm text-red-500">{errors.acceptTerms.message}</p>
          )}

          <Button type="submit" className="w-full" isLoading={isSubmitting}>
            {t('auth.registerButton')}
          </Button>
        </form>

        <div className="mt-6 text-center">
          <p className="text-sm text-neutral-600 dark:text-neutral-400">
            {t('auth.hasAccount')}{' '}
            <Link
              to="/login"
              className="font-medium text-primary-500 hover:text-primary-600"
            >
              {t('auth.loginButton')}
            </Link>
          </p>
        </div>
      </Card>
    </AuthLayout>
  )
}
