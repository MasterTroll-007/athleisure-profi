import { ReactNode, useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { AnimatePresence, motion } from 'framer-motion'
import {
  User,
  Phone,
  Lock,
  Bell,
  Eye,
  EyeOff,
  Download,
  Trash2,
  Calendar,
  CreditCard,
  Edit2,
  Clock,
  ChevronRight,
  Palette,
  Shield,
} from 'lucide-react'
import { Button, Input, Modal } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { authApi } from '@/services/api'
import { useAuthStore } from '@/stores/authStore'
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

type ExpandableRow = 'profile' | 'language' | 'timing' | null

function GroupTitle({ children }: { children: ReactNode }) {
  return (
    <div className="px-1 pb-2 text-[11px] font-semibold uppercase tracking-[0.8px] text-[#ffdaa5]/70">
      {children}
    </div>
  )
}

function Group({ title, children, className = '' }: { title?: string; children: ReactNode; className?: string }) {
  return (
    <div className={`mb-7 ${className}`}>
      {title && <GroupTitle>{title}</GroupTitle>}
      <div className="app-card overflow-hidden rounded-2xl p-0">{children}</div>
    </div>
  )
}

interface RowBaseProps {
  icon: ReactNode
  label: string
  last?: boolean
}

interface LinkRowProps extends RowBaseProps {
  type?: 'link'
  value?: string
  onClick?: () => void
  expanded?: ReactNode
  expandedOpen?: boolean
  danger?: boolean
}

interface ToggleRowProps extends RowBaseProps {
  type: 'toggle'
  checked: boolean
  onToggle: () => void
  disabled?: boolean
}

type RowProps = LinkRowProps | ToggleRowProps

function IconTile({ children }: { children: ReactNode }) {
  return (
    <span className="inline-flex h-[30px] w-[30px] flex-shrink-0 items-center justify-center rounded-lg bg-[#ffdaa5]/10 text-[#ffcb73]">
      {children}
    </span>
  )
}

function Row(props: RowProps) {
  const borderClass = props.last ? '' : 'border-b border-white/[0.05]'

  if (props.type === 'toggle') {
    return (
      <div className={`flex items-center gap-3.5 px-[18px] py-3.5 ${borderClass}`}>
        <IconTile>{props.icon}</IconTile>
        <span className="flex-1 text-sm font-medium text-white/90">{props.label}</span>
        <label
          className={`inline-flex cursor-pointer select-none items-center ${
            props.disabled ? 'pointer-events-none opacity-60' : ''
          }`}
        >
          <input
            type="checkbox"
            checked={props.checked}
            onChange={props.onToggle}
            disabled={props.disabled}
            className="peer sr-only"
            aria-label={props.label}
          />
          <span className="relative h-6 w-11 flex-shrink-0 rounded-full bg-white/15 transition-all after:absolute after:left-[2px] after:top-[2px] after:h-5 after:w-5 after:rounded-full after:bg-white after:shadow after:transition-all after:content-[''] peer-checked:bg-primary-500 peer-checked:after:translate-x-5" />
        </label>
      </div>
    )
  }

  const { icon, label, value, onClick, expanded, expandedOpen, danger } = props
  const interactive = Boolean(onClick)
  return (
    <div className={borderClass}>
      <button
        type="button"
        onClick={onClick}
        disabled={!interactive}
        className={`flex w-full items-center gap-3.5 px-[18px] py-3.5 text-left transition-colors ${
          interactive ? 'hover:bg-white/[0.02]' : 'cursor-default'
        }`}
      >
        <IconTile>{icon}</IconTile>
        <span className={`flex-1 text-sm font-medium ${danger ? 'text-red-300' : 'text-white/90'}`}>{label}</span>
        {value && (
          <span className="max-w-[45%] truncate text-[13px] text-white/50">{value}</span>
        )}
        {interactive && (
          <ChevronRight
            size={14}
            className={`flex-shrink-0 text-white/30 transition-transform ${expandedOpen ? 'rotate-90' : ''}`}
          />
        )}
      </button>
      <AnimatePresence initial={false}>
        {expandedOpen && expanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.18, ease: 'easeOut' }}
            className="overflow-hidden border-t border-white/[0.05]"
          >
            <div className="px-[18px] pb-4 pt-3">{expanded}</div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

export default function Profile() {
  const { t, i18n } = useTranslation()
  const { user, updateUser, logout } = useAuthStore()
  const { showToast } = useToast()

  const [openRow, setOpenRow] = useState<ExpandableRow>(null)
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const [emailRemindersEnabled, setEmailRemindersEnabled] = useState(user?.emailRemindersEnabled ?? true)
  const [reminderHoursBefore, setReminderHoursBefore] = useState(user?.reminderHoursBefore ?? 24)

  const {
    register: registerProfile,
    handleSubmit: handleProfileSubmit,
    reset: resetProfileForm,
    formState: { isDirty: isProfileDirty },
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
      resetProfileForm({
        firstName: data.firstName ?? '',
        lastName: data.lastName ?? '',
        phone: data.phone ?? '',
      })
      setOpenRow(null)
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

  const handleReminderToggle = () => {
    const newValue = !emailRemindersEnabled
    setEmailRemindersEnabled(newValue)
    if (!newValue && openRow === 'timing') setOpenRow(null)
    reminderMutation.mutate({ emailRemindersEnabled: newValue, reminderHoursBefore })
  }

  const handleReminderHoursChange = (hours: number) => {
    setReminderHoursBefore(hours)
    reminderMutation.mutate({ emailRemindersEnabled, reminderHoursBefore: hours })
  }

  const onProfileSubmit = (data: ProfileForm) => {
    profileMutation.mutate(data)
  }

  const onPasswordSubmit = (data: PasswordForm) => {
    passwordMutation.mutate({
      currentPassword: data.currentPassword,
      newPassword: data.newPassword,
    })
  }

  const reminderOptions = [
    { value: 1, label: t('profile.reminder1h') },
    { value: 24, label: t('profile.reminder24h') },
  ] as const

  const locale = i18n.language === 'en' ? 'en-US' : 'cs-CZ'
  const fullName = [user?.firstName, user?.lastName].filter(Boolean).join(' ').trim() || user?.email || ''
  const credits = user?.credits ?? 0
  const initials = useMemo(() => {
    const f = (user?.firstName ?? '').trim()
    const l = (user?.lastName ?? '').trim()
    const fromName = `${f.charAt(0)}${l.charAt(0)}`.toUpperCase()
    if (fromName) return fromName
    const e = user?.email ?? ''
    return e.slice(0, 2).toUpperCase()
  }, [user])

  const memberSinceText = user?.createdAt
    ? new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'numeric', year: 'numeric' }).format(
        new Date(user.createdAt)
      )
    : '—'

  const currentTimingLabel = reminderHoursBefore === 1 ? t('profile.reminder1h') : t('profile.reminder24h')

  const toggleRow = (row: Exclude<ExpandableRow, null>) => setOpenRow(openRow === row ? null : row)

  return (
    <div className="animate-fade-in">
      <div className="mx-auto max-w-[620px] px-4 py-6 sm:px-6 sm:py-9">
        {/* Identity card */}
        <div className="app-card mb-7 flex items-center gap-4 rounded-2xl p-5 sm:p-[22px]">
          <div
            className="inline-flex flex-shrink-0 items-center justify-center font-extrabold text-white"
            style={{
              width: 64,
              height: 64,
              borderRadius: '50%',
              background: 'linear-gradient(135deg, #f29b2f, #b55d19)',
              fontSize: 22,
              boxShadow: '0 16px 40px -10px rgba(242,155,47,0.5)',
            }}
          >
            {initials}
          </div>
          <div className="min-w-0 flex-1">
            <div className="truncate text-base font-bold text-white sm:text-lg">{fullName}</div>
            <div className="truncate text-[13px] text-white/55">{user?.email}</div>
          </div>
          <Button
            variant="secondary"
            size="sm"
            leftIcon={<Edit2 size={13} />}
            onClick={() => setOpenRow('profile')}
            className="hidden sm:inline-flex"
          >
            {t('common.edit')}
          </Button>
        </div>

        {/* Účet */}
        <Group title={t('nav.profile')}>
          <Row
            icon={<User size={14} />}
            label={t('profile.timeline.settings.personalInfo')}
            value={fullName}
            onClick={() => toggleRow('profile')}
            expandedOpen={openRow === 'profile'}
            expanded={
              <form onSubmit={handleProfileSubmit(onProfileSubmit)} className="space-y-3">
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <Input label={t('auth.firstName')} leftIcon={<User size={16} />} {...registerProfile('firstName')} />
                  <Input label={t('auth.lastName')} {...registerProfile('lastName')} />
                </div>
                <Input
                  label={t('auth.phone')}
                  type="tel"
                  leftIcon={<Phone size={16} />}
                  {...registerProfile('phone')}
                />
                <Button
                  type="submit"
                  size="sm"
                  disabled={!isProfileDirty}
                  isLoading={profileMutation.isPending}
                  className="w-full"
                >
                  {t('common.save')}
                </Button>
              </form>
            }
          />
          <Row
            icon={<CreditCard size={14} />}
            label={t('common.credits')}
            value={t('profile.timeline.statsLessons', { count: credits })}
          />
          <Row
            icon={<Calendar size={14} />}
            label={t('profile.timeline.joinedLabel')}
            value={memberSinceText}
            last
          />
        </Group>

        {/* Předvolby */}
        <Group title={t('profile.settings')}>
          <Row
            icon={<Palette size={14} />}
            label={t('profile.language')}
            value={i18n.language === 'en' ? t('profile.languageEnglish') : t('profile.languageCzech')}
            onClick={() => toggleRow('language')}
            expandedOpen={openRow === 'language'}
            expanded={<LanguageSwitch />}
            last
          />
        </Group>

        {/* Notifikace */}
        <Group title={t('profile.timeline.settings.notifications')}>
          <Row
            type="toggle"
            icon={<Bell size={14} />}
            label={t('profile.emailReminders')}
            checked={emailRemindersEnabled}
            onToggle={handleReminderToggle}
            disabled={reminderMutation.isPending}
            last={!emailRemindersEnabled}
          />
          {emailRemindersEnabled && (
            <Row
              icon={<Clock size={14} />}
              label={t('profile.reminderTiming')}
              value={currentTimingLabel}
              onClick={() => toggleRow('timing')}
              expandedOpen={openRow === 'timing'}
              expanded={
                <div className="flex gap-1 rounded-lg border border-white/10 bg-white/[0.04] p-1">
                  {reminderOptions.map((option) => (
                    <button
                      key={option.value}
                      type="button"
                      onClick={() => handleReminderHoursChange(option.value)}
                      disabled={reminderMutation.isPending}
                      className={`flex-1 rounded-md px-3 py-1.5 text-xs font-medium transition-colors ${
                        reminderHoursBefore === option.value
                          ? 'bg-white/10 text-white shadow-sm'
                          : 'text-white/55 hover:text-white/80'
                      }`}
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
              }
              last
            />
          )}
        </Group>

        {/* Bezpečnost */}
        <Group title={t('auth.password')}>
          <Row
            icon={<Shield size={14} />}
            label={t('profile.changePassword')}
            onClick={() => setIsPasswordModalOpen(true)}
            last
          />
        </Group>

        {/* Data a soukromí */}
        <Group title={t('gdpr.title')}>
          <Row
            icon={<Download size={14} />}
            label={t('gdpr.exportData')}
            onClick={async () => {
              try {
                const blob = await authApi.exportMyData()
                const url = URL.createObjectURL(blob)
                const link = document.createElement('a')
                link.href = url
                link.download = `my-data-${new Date().toISOString().split('T')[0]}.json`
                document.body.appendChild(link)
                link.click()
                link.remove()
                window.setTimeout(() => URL.revokeObjectURL(url), 0)
                showToast('success', t('gdpr.exportSuccess'))
              } catch {
                showToast('error', t('errors.somethingWrong'))
              }
            }}
          />
          <Row
            icon={<Trash2 size={14} />}
            label={t('gdpr.deleteAccount')}
            onClick={() => setShowDeleteModal(true)}
            danger
            last
          />
        </Group>

        {/* Logout */}
        <Group className="mb-2">
          <button
            type="button"
            onClick={logout}
            className="w-full px-[18px] py-3.5 text-center text-sm font-medium text-[#f5b8b8] transition-colors hover:bg-white/[0.02]"
          >
            {t('nav.logout')}
          </button>
        </Group>
      </div>

      {/* Delete Account Modal */}
      <Modal
        isOpen={showDeleteModal}
        onClose={() => setShowDeleteModal(false)}
        title={t('gdpr.deleteAccount')}
        size="sm"
      >
        <div className="space-y-4">
          <p className="text-neutral-600 dark:text-neutral-300">{t('gdpr.deleteConfirm')}</p>
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
                aria-label={showPassword ? t('auth.hidePassword') : t('auth.showPassword')}
                className="text-neutral-400 hover:text-neutral-600 dark:hover:text-neutral-300"
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            }
            error={passwordErrors.currentPassword?.message}
            {...registerPassword('currentPassword', {
              onChange: () => clearPasswordErrors('currentPassword'),
            })}
          />
          <Input
            label={t('profile.newPassword')}
            type={showPassword ? 'text' : 'password'}
            leftIcon={<Lock size={18} />}
            error={passwordErrors.newPassword?.message}
            {...registerPassword('newPassword', {
              onChange: () => clearPasswordErrors('newPassword'),
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
