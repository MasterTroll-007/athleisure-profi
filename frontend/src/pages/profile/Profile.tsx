import { ReactNode, useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
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
  Sparkles,
  CreditCard,
  Edit2,
  Clock,
  ChevronRight,
  SlidersHorizontal,
  Palette,
  Shield,
  TrendingDown,
} from 'lucide-react'
import { Button, Input, Modal } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { authApi, creditApi, reservationApi } from '@/services/api'
import { useAuthStore } from '@/stores/authStore'
import LanguageSwitch from '@/components/layout/LanguageSwitch'
import type { BodyMeasurement, CreditTransaction, Reservation } from '@/types/api'

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

type SettingsRow = 'profile' | 'language' | 'notifications' | 'password' | null

interface TimelineEvent {
  id: string
  date: Date
  iconName: 'edit' | 'calendar' | 'sparkles' | 'creditCard' | 'user'
  color: string
  label: string
  hint?: string
  timeText?: string
}

const eventIcon = (name: TimelineEvent['iconName'], size = 13) => {
  switch (name) {
    case 'edit':
      return <Edit2 size={size} />
    case 'calendar':
      return <Calendar size={size} />
    case 'sparkles':
      return <Sparkles size={size} />
    case 'creditCard':
      return <CreditCard size={size} />
    case 'user':
    default:
      return <User size={size} />
  }
}

export default function Profile() {
  const { t, i18n } = useTranslation()
  const { user, updateUser, logout } = useAuthStore()
  const { showToast } = useToast()

  const [openRow, setOpenRow] = useState<SettingsRow>(null)
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const [emailRemindersEnabled, setEmailRemindersEnabled] = useState(user?.emailRemindersEnabled ?? true)
  const [reminderHoursBefore, setReminderHoursBefore] = useState(user?.reminderHoursBefore ?? 24)

  const { data: measurementsPage } = useQuery({
    queryKey: ['myMeasurements', 'timeline'],
    queryFn: () => authApi.getMyMeasurements(0, 10),
  })

  const { data: pastReservations } = useQuery({
    queryKey: ['myReservations', 'past', 'timeline'],
    queryFn: () => reservationApi.getMyReservations('past', 0, 10),
  })

  const { data: transactions } = useQuery({
    queryKey: ['credits', 'history', 'timeline'],
    queryFn: () => creditApi.getHistory(10, 0),
  })

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
  const initials = useMemo(() => {
    const f = (user?.firstName ?? '').trim()
    const l = (user?.lastName ?? '').trim()
    const fromName = `${f.charAt(0)}${l.charAt(0)}`.toUpperCase()
    if (fromName) return fromName
    const e = user?.email ?? ''
    return e.slice(0, 2).toUpperCase()
  }, [user])

  // Build timeline events from merged sources
  const events: TimelineEvent[] = useMemo(() => {
    const list: TimelineEvent[] = []

    const sortedMeasurements = (measurementsPage?.content ?? []).slice().sort(
      (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime()
    )
    sortedMeasurements.forEach((m: BodyMeasurement, idx) => {
      const prev = idx > 0 ? sortedMeasurements[idx - 1] : null
      const weightDelta = m.weight != null && prev?.weight != null ? m.weight - prev.weight : null
      list.push({
        id: `meas-${m.id}`,
        date: new Date(m.date),
        iconName: 'sparkles',
        color: '#9fdba2',
        label: m.weight != null
          ? t('profile.timeline.measurementLabel', { weight: m.weight })
          : t('profile.timeline.measurementGenericLabel'),
        hint: weightDelta !== null
          ? t('profile.timeline.measurementDelta', {
              sign: weightDelta > 0 ? '+' : weightDelta < 0 ? '−' : '±',
              delta: Math.abs(weightDelta).toFixed(1),
            })
          : undefined,
      })
    })

    ;(pastReservations?.content ?? []).forEach((r: Reservation) => {
      list.push({
        id: `res-${r.id}`,
        date: new Date(`${r.date}T${r.startTime}`),
        iconName: 'calendar',
        color: r.status === 'no_show' ? '#e8b8b8' : r.status === 'cancelled' ? 'rgba(255,255,255,0.45)' : '#ffcb73',
        label: r.pricingItemName || t('profile.timeline.trainingLabel'),
        hint: r.status === 'no_show'
          ? t('profile.timeline.statusNoShow')
          : r.status === 'cancelled'
            ? t('profile.timeline.statusCancelled')
            : r.locationName
              ? t('profile.timeline.trainingHint', { location: r.locationName })
              : undefined,
        timeText: r.startTime?.slice(0, 5),
      })
    })

    ;(transactions?.content ?? []).forEach((tx: CreditTransaction) => {
      if (tx.type === 'reservation') return
      const sign = tx.amount >= 0 ? '+' : '−'
      const amount = Math.abs(tx.amount)
      list.push({
        id: `tx-${tx.id}`,
        date: new Date(tx.createdAt),
        iconName: 'creditCard',
        color: '#a78bcc',
        label: t('profile.timeline.creditChange', { sign, amount }),
        hint: tx.type === 'purchase'
          ? t('credits.purchase')
          : tx.type === 'refund'
            ? t('credits.refund')
            : t('credits.adminAdjustment'),
      })
    })

    if (user?.createdAt) {
      list.push({
        id: 'registration',
        date: new Date(user.createdAt),
        iconName: 'user',
        color: '#ffb347',
        label: t('profile.timeline.joinedLabel'),
        hint: t('profile.timeline.joinedHint', {
          date: new Date(user.createdAt).toLocaleDateString(locale, { day: 'numeric', month: 'long', year: 'numeric' }),
        }),
      })
    }

    return list.sort((a, b) => b.date.getTime() - a.date.getTime())
  }, [measurementsPage, pastReservations, transactions, user, locale, t])

  // Stats
  const totalReservations = pastReservations?.totalElements ?? 0
  const credits = user?.credits ?? 0
  const completedCount = (pastReservations?.content ?? []).filter((r) => r.status === 'completed').length
  const noShowCount = (pastReservations?.content ?? []).filter((r) => r.status === 'no_show').length
  const attendanceRate = completedCount + noShowCount > 0
    ? Math.round((completedCount / (completedCount + noShowCount)) * 100)
    : null

  // Latest weight measurement (for progress card)
  const latestWeight = useMemo(() => {
    const m = (measurementsPage?.content ?? []).find((mm) => mm.weight != null)
    return m?.weight ?? null
  }, [measurementsPage])

  const formatRelativeDate = (date: Date) => {
    const now = new Date()
    const startOfDay = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate())
    const diffDays = Math.round((startOfDay(now).getTime() - startOfDay(date).getTime()) / (1000 * 60 * 60 * 24))
    if (diffDays === 0) return t('profile.timeline.today')
    if (diffDays === 1) return t('profile.timeline.yesterday')
    if (diffDays > 1 && diffDays < 7) {
      return new Intl.DateTimeFormat(locale, { weekday: 'short' }).format(date)
    }
    if (date.getFullYear() === now.getFullYear()) {
      return new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'numeric' }).format(date)
    }
    return new Intl.DateTimeFormat(locale, { month: 'short', year: '2-digit' }).format(date)
  }

  const formatTime = (event: TimelineEvent) => {
    if (event.timeText) return event.timeText
    return new Intl.DateTimeFormat(locale, { hour: '2-digit', minute: '2-digit' }).format(event.date)
  }

  // Settings row metadata
  const passwordTimeAgo = '—'
  const settingsRows: Array<{
    key: SettingsRow
    icon: ReactNode
    label: string
    value: string
    onClick: () => void
  }> = [
    {
      key: 'profile',
      icon: <User size={14} />,
      label: t('profile.timeline.settings.personalInfo'),
      value: fullName,
      onClick: () => setOpenRow(openRow === 'profile' ? null : 'profile'),
    },
    {
      key: 'language',
      icon: <Palette size={14} />,
      label: t('profile.language'),
      value: i18n.language === 'en' ? 'English' : 'Čeština',
      onClick: () => setOpenRow(openRow === 'language' ? null : 'language'),
    },
    {
      key: 'notifications',
      icon: <Bell size={14} />,
      label: t('profile.timeline.settings.notifications'),
      value: emailRemindersEnabled
        ? reminderHoursBefore === 1
          ? t('profile.reminder1h')
          : t('profile.reminder24h')
        : t('profile.timeline.settings.disabled'),
      onClick: () => setOpenRow(openRow === 'notifications' ? null : 'notifications'),
    },
    {
      key: 'password',
      icon: <Shield size={14} />,
      label: t('profile.timeline.settings.password'),
      value: passwordTimeAgo,
      onClick: () => setIsPasswordModalOpen(true),
    },
  ]

  return (
    <div className="animate-fade-in -mx-4 sm:mx-0">
      <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_340px] gap-0 lg:gap-6">
        {/* Main timeline column */}
        <main className="px-4 sm:px-2 lg:pr-2">
          {/* Hero */}
          <div className="flex items-center gap-4 mb-7">
            <div
              className="inline-flex items-center justify-center text-white font-extrabold flex-shrink-0"
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
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.8px] text-[#ffdaa5]/85">
                <User size={11} />
                <span>{user?.role === 'admin' ? t('profile.timeline.admin') : t('profile.timeline.client')}</span>
              </div>
              <h1 className="mt-1 text-xl sm:text-2xl font-bold text-white leading-tight truncate">
                {fullName}
              </h1>
              <div className="text-xs sm:text-[13px] text-white/55 mt-0.5">
                <span>{t('profile.timeline.statsLessons', { count: totalReservations })}</span>
                <span className="mx-1.5">·</span>
                <span>{t('profile.timeline.statsCredits', { count: credits })}</span>
                {attendanceRate !== null && (
                  <>
                    <span className="mx-1.5">·</span>
                    <span>{t('profile.timeline.statsAttendance', { rate: attendanceRate })}</span>
                  </>
                )}
              </div>
            </div>
            <Button
              variant="primary"
              size="sm"
              leftIcon={<Edit2 size={14} />}
              onClick={() => setOpenRow('profile')}
              className="hidden sm:inline-flex"
            >
              {t('common.edit')}
            </Button>
          </div>

          {/* Activity eyebrow */}
          <div className="flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.8px] text-[#ffdaa5]/85 mb-4">
            <Clock size={11} />
            <span>{t('profile.timeline.activity')}</span>
          </div>

          {/* Timeline */}
          {events.length === 0 ? (
            <div className="app-card rounded-2xl p-6 text-center text-white/55">
              {t('profile.timeline.empty')}
            </div>
          ) : (
            <div className="relative pl-8">
              <div
                className="absolute top-2 bottom-3 w-px"
                style={{
                  left: 15,
                  background: 'linear-gradient(180deg, rgba(242,155,47,0.4), rgba(255,255,255,0.05))',
                }}
              />
              {events.map((e) => (
                <div key={e.id} className="relative mb-4">
                  <div
                    className="absolute inline-flex items-center justify-center"
                    style={{
                      left: -32,
                      top: 8,
                      width: 32,
                      height: 32,
                      borderRadius: '50%',
                      background: 'rgba(9,8,16,0.95)',
                      border: `2px solid ${e.color}`,
                      color: e.color,
                      boxShadow: `0 0 0 4px rgba(9,8,16,0.95), 0 0 20px ${e.color}40`,
                    }}
                  >
                    {eventIcon(e.iconName)}
                  </div>
                  <div className="app-card rounded-xl flex items-center gap-3 sm:gap-4 px-3 sm:px-5 py-3.5">
                    <div className="min-w-[64px] sm:min-w-[72px] text-[11px] text-white/45 leading-tight">
                      <div className="font-semibold text-white/70 capitalize">{formatRelativeDate(e.date)}</div>
                      <div className="mt-0.5">{formatTime(e)}</div>
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="text-sm font-medium text-white/90 truncate">{e.label}</div>
                      {e.hint && <div className="text-xs text-white/50 mt-0.5 truncate">{e.hint}</div>}
                    </div>
                    <ChevronRight size={14} className="text-white/30 flex-shrink-0" />
                  </div>
                </div>
              ))}
            </div>
          )}
        </main>

        {/* Right rail */}
        <aside className="px-4 sm:px-2 lg:pl-4 lg:border-l lg:border-white/[0.06] mt-8 lg:mt-0">
          {/* Settings */}
          <div className="flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.8px] text-[#ffdaa5]/85 mb-3">
            <SlidersHorizontal size={11} />
            <span>{t('profile.timeline.profileSettings')}</span>
          </div>

          <div className="app-card rounded-xl overflow-hidden">
            {settingsRows.map((row, i) => {
              const isOpen = row.key !== null && openRow === row.key
              const isLast = i === settingsRows.length - 1
              return (
                <div key={row.label} className={isLast ? '' : 'border-b border-white/[0.05]'}>
                  <button
                    type="button"
                    onClick={row.onClick}
                    className="w-full px-4 py-3 flex items-center gap-2.5 text-left hover:bg-white/[0.02] transition-colors"
                  >
                    <span className="text-[#ffdaa5]/85 flex-shrink-0">{row.icon}</span>
                    <span className="flex-1 text-[13px] text-white/90">{row.label}</span>
                    <span className="text-xs text-white/55 truncate max-w-[40%]">{row.value}</span>
                    <ChevronRight
                      size={12}
                      className={`text-white/30 flex-shrink-0 transition-transform ${isOpen ? 'rotate-90' : ''}`}
                    />
                  </button>

                  {isOpen && row.key === 'profile' && (
                    <form
                      onSubmit={handleProfileSubmit(onProfileSubmit)}
                      className="px-4 pb-4 pt-1 space-y-3 border-t border-white/[0.05]"
                    >
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        <Input label={t('auth.firstName')} leftIcon={<User size={16} />} {...registerProfile('firstName')} />
                        <Input label={t('auth.lastName')} {...registerProfile('lastName')} />
                      </div>
                      <Input label={t('auth.phone')} type="tel" leftIcon={<Phone size={16} />} {...registerProfile('phone')} />
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
                  )}

                  {isOpen && row.key === 'language' && (
                    <div className="px-4 pb-4 pt-3 border-t border-white/[0.05]">
                      <LanguageSwitch />
                    </div>
                  )}

                  {isOpen && row.key === 'notifications' && (
                    <div className="px-4 pb-4 pt-3 border-t border-white/[0.05] space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-white/85">{t('profile.emailReminders')}</span>
                        <label
                          className={`inline-flex min-h-[36px] cursor-pointer select-none items-center justify-center gap-2 rounded-lg border border-white/10 bg-neutral-50 px-3 py-1.5 text-sm font-medium text-neutral-700 transition-colors hover:bg-neutral-100 dark:bg-dark-surface dark:text-neutral-300 dark:hover:bg-dark-surfaceHover ${
                            reminderMutation.isPending ? 'pointer-events-none opacity-60' : ''
                          }`}
                        >
                          <input
                            type="checkbox"
                            checked={emailRemindersEnabled}
                            onChange={handleReminderToggle}
                            disabled={reminderMutation.isPending}
                            className="sr-only peer"
                            aria-label={t('profile.emailReminders')}
                          />
                          <span className="relative h-6 w-11 flex-shrink-0 rounded-full bg-neutral-300 transition-all after:absolute after:left-[2px] after:top-[2px] after:h-5 after:w-5 after:rounded-full after:border after:border-neutral-300 after:bg-white after:transition-all after:content-[''] peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300 dark:bg-neutral-600 dark:after:border-neutral-600 dark:peer-focus:ring-primary-800 peer-checked:bg-primary-500 peer-checked:after:translate-x-full peer-checked:after:border-white" />
                        </label>
                      </div>
                      {emailRemindersEnabled && (
                        <div>
                          <span className="block text-xs text-white/55 mb-2">{t('profile.reminderTiming')}</span>
                          <div className="flex gap-1 p-1 bg-white/[0.04] border border-white/10 rounded-lg">
                            {reminderOptions.map((option) => (
                              <button
                                key={option.value}
                                type="button"
                                onClick={() => handleReminderHoursChange(option.value)}
                                disabled={reminderMutation.isPending}
                                className={`flex-1 px-3 py-1.5 text-xs font-medium rounded-md transition-colors ${
                                  reminderHoursBefore === option.value
                                    ? 'bg-white/10 text-white shadow-sm'
                                    : 'text-white/55 hover:text-white/80'
                                }`}
                              >
                                {option.label}
                              </button>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )
            })}
          </div>

          {/* Progress card */}
          {latestWeight !== null && (
            <>
              <div className="flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.8px] text-[#ffdaa5]/85 mb-3 mt-6">
                <TrendingDown size={11} />
                <span>{t('profile.timeline.progressTitle')}</span>
              </div>
              <div className="app-card rounded-xl p-4">
                <div className="text-xs text-white/55 mb-2">{t('profile.timeline.weightLabel')}</div>
                <div className="flex items-baseline justify-between mb-2">
                  <span
                    className="text-2xl font-bold text-white"
                    style={{ fontFamily: "'JetBrains Mono', ui-monospace, monospace" }}
                  >
                    {latestWeight}
                  </span>
                  <span className="text-xs text-white/55">{t('profile.timeline.weightUnit')}</span>
                </div>
              </div>
            </>
          )}

          {/* Data & Privacy */}
          <div className="flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.8px] text-[#ffdaa5]/85 mb-3 mt-6">
            <Shield size={11} />
            <span>{t('gdpr.title', 'Data & Privacy')}</span>
          </div>
          <div className="app-card rounded-xl overflow-hidden">
            <button
              type="button"
              className="w-full px-4 py-3 flex items-center gap-2.5 text-left hover:bg-white/[0.02] transition-colors border-b border-white/[0.05]"
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
                  showToast('success', t('gdpr.exportSuccess', 'Data exported'))
                } catch {
                  showToast('error', t('errors.somethingWrong'))
                }
              }}
            >
              <Download size={14} className="text-[#ffdaa5]/85" />
              <div className="flex-1 min-w-0">
                <div className="text-[13px] text-white/90">{t('gdpr.exportData')}</div>
                <div className="text-xs text-white/45 mt-0.5">{t('gdpr.exportDescription')}</div>
              </div>
            </button>
            <button
              type="button"
              className="w-full px-4 py-3 flex items-center gap-2.5 text-left hover:bg-red-500/[0.06] transition-colors"
              onClick={() => setShowDeleteModal(true)}
            >
              <Trash2 size={14} className="text-red-400" />
              <div className="flex-1 min-w-0">
                <div className="text-[13px] text-red-300">{t('gdpr.deleteAccount')}</div>
                <div className="text-xs text-white/45 mt-0.5">{t('gdpr.deleteDescription')}</div>
              </div>
            </button>
          </div>
        </aside>
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
                aria-label={showPassword ? 'Hide password' : 'Show password'}
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
