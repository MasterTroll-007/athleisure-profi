import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { User, Phone, Lock, Bell, Eye, EyeOff, Download, Trash2 } from 'lucide-react'
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
  const { user, updateUser, logout } = useAuthStore()
  const { theme, setTheme } = useThemeStore()
  const { showToast } = useToast()
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const [emailRemindersEnabled, setEmailRemindersEnabled] = useState(user?.emailRemindersEnabled ?? true)
  const [reminderHoursBefore, setReminderHoursBefore] = useState(user?.reminderHoursBefore ?? 24)

  const { data: measurements } = useQuery({
    queryKey: ['myMeasurements'],
    queryFn: authApi.getMyMeasurements,
  })

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
    setError: setPasswordError,
    clearErrors: clearPasswordErrors,
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

  const reminderMutation = useMutation({
    mutationFn: (data: { emailRemindersEnabled: boolean; reminderHoursBefore: number }) =>
      authApi.updateProfile(data),
    onSuccess: (data) => {
      updateUser(data)
      showToast('success', t('profile.saved'))
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const handleReminderToggle = () => {
    const newValue = !emailRemindersEnabled
    setEmailRemindersEnabled(newValue)
    reminderMutation.mutate({ emailRemindersEnabled: newValue, reminderHoursBefore })
  }

  const handleReminderHoursChange = (hours: number) => {
    setReminderHoursBefore(hours)
    reminderMutation.mutate({ emailRemindersEnabled, reminderHoursBefore: hours })
  }

  const passwordMutation = useMutation({
    mutationFn: ({ currentPassword, newPassword }: { currentPassword: string; newPassword: string }) =>
      authApi.changePassword(currentPassword, newPassword),
    onSuccess: () => {
      showToast('success', t('profile.passwordChanged'))
      setIsPasswordModalOpen(false)
      resetPasswordForm()
    },
    onError: (error: unknown) => {
      const axiosError = error as { response?: { data?: { error?: string } } }
      const errorMsg = axiosError.response?.data?.error
      if (errorMsg?.includes('Current password')) {
        setPasswordError('currentPassword', { message: t('errors.incorrectPassword') })
      } else if (errorMsg?.includes('uppercase') || errorMsg?.includes('lowercase') || errorMsg?.includes('number')) {
        setPasswordError('newPassword', { message: t('errors.passwordRequirements') })
      } else {
        setPasswordError('currentPassword', { message: errorMsg || t('errors.somethingWrong') })
      }
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

  const reminderOptions = [
    { value: 1, label: t('profile.reminder1h') },
    { value: 24, label: t('profile.reminder24h') },
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

      {/* Measurements */}
      {measurements && measurements.length > 0 && (
        <Card variant="bordered">
          <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white mb-4">
            {t('measurements.title')}
          </h2>
          <div className="space-y-3">
            {measurements.slice(0, 5).map((measurement) => (
              <div key={measurement.id} className="flex items-center justify-between rounded-lg bg-neutral-50 dark:bg-dark-surface p-3">
                <div>
                  <p className="font-medium text-neutral-900 dark:text-white">
                    {new Date(measurement.date).toLocaleDateString()}
                  </p>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400">
                    {[measurement.waist != null ? `${t('measurements.waist')}: ${measurement.waist} cm` : null, measurement.bodyFat != null ? `${t('measurements.bodyFat')}: ${measurement.bodyFat}%` : null].filter(Boolean).join(' · ')}
                  </p>
                </div>
                {measurement.weight != null && (
                  <span className="text-sm font-semibold text-neutral-900 dark:text-white">{measurement.weight} kg</span>
                )}
              </div>
            ))}
          </div>
        </Card>
      )}

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
          <div className="flex items-center justify-between py-2 gap-2">
            <span className="text-neutral-700 dark:text-neutral-300 shrink-0">{t('profile.theme')}</span>
            <div className="flex gap-1 p-1 bg-neutral-100 dark:bg-dark-surface rounded-lg overflow-hidden">
              {themes.map((t) => (
                <button
                  key={t.value}
                  onClick={() => setTheme(t.value)}
                  className={`px-2 sm:px-3 py-1.5 text-xs sm:text-sm font-medium rounded-md transition-colors whitespace-nowrap ${
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

          {/* Email Reminders */}
          <div className="flex items-center justify-between py-2 pt-4 border-t border-neutral-100 dark:border-dark-border">
            <div className="flex items-center gap-2">
              <Bell size={18} className="text-neutral-500" />
              <span className="text-neutral-700 dark:text-neutral-300">{t('profile.emailReminders')}</span>
            </div>
            <button
              onClick={handleReminderToggle}
              disabled={reminderMutation.isPending}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                emailRemindersEnabled ? 'bg-primary-500' : 'bg-neutral-300 dark:bg-neutral-600'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  emailRemindersEnabled ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>

          {/* Reminder Timing */}
          {emailRemindersEnabled && (
            <div className="flex items-center justify-between py-2">
              <span className="text-neutral-700 dark:text-neutral-300 ml-6">{t('profile.reminderTiming')}</span>
              <div className="flex gap-1 p-1 bg-neutral-100 dark:bg-dark-surface rounded-lg">
                {reminderOptions.map((option) => (
                  <button
                    key={option.value}
                    onClick={() => handleReminderHoursChange(option.value)}
                    disabled={reminderMutation.isPending}
                    className={`px-3 py-1.5 text-sm font-medium rounded-md transition-colors ${
                      reminderHoursBefore === option.value
                        ? 'bg-white dark:bg-dark-surfaceHover text-neutral-900 dark:text-white shadow-sm'
                        : 'text-neutral-500 dark:text-neutral-400'
                    }`}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>
          )}

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

      {/* Data & Privacy */}
      <Card variant="bordered">
        <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white mb-4">
          {t('gdpr.title', 'Data & Privacy')}
        </h2>

        <div className="space-y-4">
          <div className="flex items-center justify-between py-2">
            <div>
              <span className="text-neutral-700 dark:text-neutral-300">{t('gdpr.exportData')}</span>
              <p className="text-sm text-neutral-500 dark:text-neutral-400">{t('gdpr.exportDescription')}</p>
            </div>
            <Button
              variant="secondary"
              size="sm"
              leftIcon={<Download size={16} />}
              onClick={async () => {
                try {
                  const data = await authApi.exportMyData()
                  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
                  const url = URL.createObjectURL(blob)
                  const link = document.createElement('a')
                  link.href = url
                  link.download = `my-data-${new Date().toISOString().split('T')[0]}.json`
                  link.click()
                  URL.revokeObjectURL(url)
                  showToast('success', t('gdpr.exportSuccess', 'Data exported'))
                } catch {
                  showToast('error', t('errors.somethingWrong'))
                }
              }}
            >
              {t('gdpr.exportData')}
            </Button>
          </div>

          <div className="flex items-center justify-between py-2 pt-4 border-t border-red-100 dark:border-red-900/30">
            <div>
              <span className="text-red-600 dark:text-red-400 font-medium">{t('gdpr.deleteAccount')}</span>
              <p className="text-sm text-neutral-500 dark:text-neutral-400">{t('gdpr.deleteDescription')}</p>
            </div>
            <Button
              variant="danger"
              size="sm"
              leftIcon={<Trash2 size={16} />}
              onClick={() => setShowDeleteModal(true)}
            >
              {t('gdpr.deleteAccount')}
            </Button>
          </div>
        </div>
      </Card>

      {/* Delete Account Modal */}
      <Modal
        isOpen={showDeleteModal}
        onClose={() => setShowDeleteModal(false)}
        title={t('gdpr.deleteAccount')}
        size="sm"
      >
        <div className="space-y-4">
          <p className="text-neutral-600 dark:text-neutral-300">
            {t('gdpr.deleteConfirm')}
          </p>
          <div className="flex gap-3">
            <Button variant="secondary" className="flex-1" onClick={() => setShowDeleteModal(false)}>
              {t('common.cancel')}
            </Button>
            <Button
              variant="danger"
              className="flex-1"
              onClick={async () => {
                try {
                  await authApi.deleteMyAccount()
                  showToast('success', t('gdpr.accountDeleted'))
                  logout()
                } catch {
                  showToast('error', t('errors.somethingWrong'))
                }
              }}
            >
              {t('gdpr.deleteAccount')}
            </Button>
          </div>
        </div>
      </Modal>

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
            type={showPassword ? 'text' : 'password'}
            leftIcon={<Lock size={18} />}
            rightIcon={
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                className="text-neutral-400 hover:text-neutral-600 dark:hover:text-neutral-300"
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            }
            error={passwordErrors.currentPassword?.message}
            {...registerPassword('currentPassword', {
              onChange: () => clearPasswordErrors('currentPassword')
            })}
          />

          <Input
            label={t('profile.newPassword')}
            type={showPassword ? 'text' : 'password'}
            leftIcon={<Lock size={18} />}
            error={passwordErrors.newPassword?.message}
            {...registerPassword('newPassword', {
              onChange: () => clearPasswordErrors('newPassword')
            })}
          />

          <Input
            label={t('auth.confirmPassword')}
            type={showPassword ? 'text' : 'password'}
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
