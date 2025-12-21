import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { User, Phone, Lock } from 'lucide-react'
import { Card, Button, Input, Modal } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { authApi } from '@/services/api'
import { useAuthStore } from '@/stores/authStore'
import { useThemeStore } from '@/stores/themeStore'
import LanguageSwitch from '@/components/layout/LanguageSwitch'

const profileSchema = z.object({
  firstName: z.string().optional(),
  lastName: z.string().optional(),
  phone: z.string().optional(),
})

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1),
    newPassword: z.string().min(8),
    confirmPassword: z.string().min(8),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    path: ['confirmPassword'],
  })

type ProfileForm = z.infer<typeof profileSchema>
type PasswordForm = z.infer<typeof passwordSchema>

export default function Profile() {
  const { t } = useTranslation()
  const { user, updateUser } = useAuthStore()
  const { theme, setTheme } = useThemeStore()
  const { showToast } = useToast()
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false)

  const {
    register: registerProfile,
    handleSubmit: handleProfileSubmit,
    formState: { isDirty },
  } = useForm<ProfileForm>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      firstName: user?.firstName || '',
      lastName: user?.lastName || '',
      phone: user?.phone || '',
    },
  })

  const {
    register: registerPassword,
    handleSubmit: handlePasswordSubmit,
    formState: { errors: passwordErrors },
    reset: resetPasswordForm,
  } = useForm<PasswordForm>({
    resolver: zodResolver(passwordSchema),
  })

  const profileMutation = useMutation({
    mutationFn: authApi.updateProfile,
    onSuccess: (data) => {
      updateUser(data)
      showToast('success', t('profile.saved'))
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const passwordMutation = useMutation({
    mutationFn: ({ currentPassword, newPassword }: { currentPassword: string; newPassword: string }) =>
      authApi.changePassword(currentPassword, newPassword),
    onSuccess: () => {
      showToast('success', t('profile.passwordChanged'))
      setIsPasswordModalOpen(false)
      resetPasswordForm()
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      showToast('error', error.response?.data?.message || t('errors.somethingWrong'))
    },
  })

  const onProfileSubmit = (data: ProfileForm) => {
    profileMutation.mutate(data)
  }

  const onPasswordSubmit = (data: PasswordForm) => {
    passwordMutation.mutate({
      currentPassword: data.currentPassword,
      newPassword: data.newPassword,
    })
  }

  const themes = [
    { value: 'light', label: t('profile.themeLight') },
    { value: 'dark', label: t('profile.themeDark') },
    { value: 'system', label: t('profile.themeSystem') },
  ] as const

  return (
    <div className="space-y-6 animate-fade-in">
      <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
        {t('profile.title')}
      </h1>

      {/* User info */}
      <Card variant="bordered">
        <div className="flex items-center gap-4 mb-6">
          <div className="w-16 h-16 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
            <User size={32} className="text-primary-500" />
          </div>
          <div>
            <p className="font-medium text-neutral-900 dark:text-white">
              {user?.firstName} {user?.lastName}
            </p>
            <p className="text-sm text-neutral-500 dark:text-neutral-400">{user?.email}</p>
          </div>
        </div>

        <form onSubmit={handleProfileSubmit(onProfileSubmit)} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Input
              label={t('auth.firstName')}
              leftIcon={<User size={18} />}
              {...registerProfile('firstName')}
            />
            <Input
              label={t('auth.lastName')}
              {...registerProfile('lastName')}
            />
          </div>

          <Input
            label={t('auth.phone')}
            type="tel"
            leftIcon={<Phone size={18} />}
            {...registerProfile('phone')}
          />

          <div className="pt-2">
            <Button type="submit" disabled={!isDirty} isLoading={profileMutation.isPending}>
              {t('common.save')}
            </Button>
          </div>
        </form>
      </Card>

      {/* Settings */}
      <Card variant="bordered">
        <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white mb-4">
          {t('profile.settings')}
        </h2>

        <div className="space-y-4">
          {/* Language */}
          <div className="flex items-center justify-between py-2">
            <span className="text-neutral-700 dark:text-neutral-300">{t('profile.language')}</span>
            <LanguageSwitch />
          </div>

          {/* Theme */}
          <div className="flex items-center justify-between py-2">
            <span className="text-neutral-700 dark:text-neutral-300">{t('profile.theme')}</span>
            <div className="flex gap-1 p-1 bg-neutral-100 dark:bg-dark-surface rounded-lg">
              {themes.map((t) => (
                <button
                  key={t.value}
                  onClick={() => setTheme(t.value)}
                  className={`px-3 py-1.5 text-sm font-medium rounded-md transition-colors ${
                    theme === t.value
                      ? 'bg-white dark:bg-dark-surfaceHover text-neutral-900 dark:text-white shadow-sm'
                      : 'text-neutral-500 dark:text-neutral-400'
                  }`}
                >
                  {t.label}
                </button>
              ))}
            </div>
          </div>

          {/* Change password */}
          <div className="flex items-center justify-between py-2 pt-4 border-t border-neutral-100 dark:border-dark-border">
            <span className="text-neutral-700 dark:text-neutral-300">
              {t('profile.changePassword')}
            </span>
            <Button
              variant="secondary"
              size="sm"
              leftIcon={<Lock size={16} />}
              onClick={() => setIsPasswordModalOpen(true)}
            >
              {t('common.edit')}
            </Button>
          </div>
        </div>
      </Card>

      {/* Password Modal */}
      <Modal
        isOpen={isPasswordModalOpen}
        onClose={() => {
          setIsPasswordModalOpen(false)
          resetPasswordForm()
        }}
        title={t('profile.changePassword')}
      >
        <form onSubmit={handlePasswordSubmit(onPasswordSubmit)} className="space-y-4">
          <Input
            label={t('profile.currentPassword')}
            type="password"
            leftIcon={<Lock size={18} />}
            error={passwordErrors.currentPassword?.message && t('errors.required')}
            {...registerPassword('currentPassword')}
          />

          <Input
            label={t('profile.newPassword')}
            type="password"
            leftIcon={<Lock size={18} />}
            error={passwordErrors.newPassword?.message && t('errors.passwordTooShort')}
            {...registerPassword('newPassword')}
          />

          <Input
            label={t('auth.confirmPassword')}
            type="password"
            leftIcon={<Lock size={18} />}
            error={passwordErrors.confirmPassword?.message && t('errors.passwordsDontMatch')}
            {...registerPassword('confirmPassword')}
          />

          <div className="flex gap-3 pt-2">
            <Button
              type="button"
              variant="secondary"
              className="flex-1"
              onClick={() => {
                setIsPasswordModalOpen(false)
                resetPasswordForm()
              }}
            >
              {t('common.cancel')}
            </Button>
            <Button type="submit" className="flex-1" isLoading={passwordMutation.isPending}>
              {t('common.save')}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
