import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useTranslation } from 'react-i18next'
import { Mail, ArrowLeft, CheckCircle } from 'lucide-react'
import { Button, Input, Card } from '@/components/ui'
import { authApi } from '@/services/api'
import AuthLayout from '@/components/layout/AuthLayout'
import FormError from '@/components/ui/FormError'

const forgotPasswordSchema = z.object({
  email: z.string().email(),
})

type ForgotPasswordForm = z.infer<typeof forgotPasswordSchema>

export default function ForgotPassword() {
  const { t } = useTranslation()
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const [submittedEmail, setSubmittedEmail] = useState<string>('')

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordForm>({
    resolver: zodResolver(forgotPasswordSchema),
  })

  const onSubmit = async (data: ForgotPasswordForm) => {
    try {
      setError(null)
      await authApi.forgotPassword(data.email)
      setSubmittedEmail(data.email)
      setSuccess(true)
    } catch {
      setError(t('errors.somethingWrong'))
    }
  }

  if (success) {
    return (
      <AuthLayout>
        <Card variant="bordered" className="w-full max-w-md" padding="lg">
          <div className="text-center">
            <div className="mx-auto w-16 h-16 bg-green-100 dark:bg-green-900/20 rounded-full flex items-center justify-center mb-6">
              <CheckCircle className="w-8 h-8 text-green-600 dark:text-green-400" />
            </div>
            <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white mb-4">
              {t('auth.checkEmail')}
            </h1>
            <p className="text-neutral-600 dark:text-neutral-400 mb-2">
              {t('auth.resetEmailSent', { email: submittedEmail })}
            </p>
            <p className="text-sm text-neutral-500 dark:text-neutral-500 mb-6">
              {t('auth.resetLinkValid1h')}
            </p>
            <Link
              to="/login"
              className="inline-flex items-center gap-2 text-primary-500 hover:text-primary-600 font-medium"
            >
              <ArrowLeft size={18} />
              {t('register.backToLogin')}
            </Link>
          </div>
        </Card>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout>
      <Card variant="bordered" className="w-full max-w-md" padding="lg">
        <div className="text-center mb-6">
          <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
            {t('auth.forgotPasswordTitle')}
          </h1>
          <p className="text-neutral-600 dark:text-neutral-400 mt-2">
            {t('auth.forgotPasswordDesc')}
          </p>
        </div>

        <FormError message={error} />

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          <Input
            label={t('auth.email')}
            type="email"
            placeholder="email@example.com"
            leftIcon={<Mail size={18} />}
            error={errors.email?.message && t('errors.invalidEmail')}
            {...register('email')}
          />

          <Button type="submit" className="w-full" isLoading={isSubmitting}>
            {t('auth.sendResetLink')}
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
    </AuthLayout>
  )
}
