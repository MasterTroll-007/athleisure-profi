import { Fragment, ReactNode, useEffect, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Settings as SettingsIcon,
  Clock,
  Save,
  Link2,
  Copy,
  RefreshCw,
  Check,
  AlertTriangle,
  Ban,
  SlidersHorizontal,
  ArrowRight,
  X,
} from 'lucide-react'
import { Button, Spinner, Modal, TimePicker, HourDurationPicker } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { adminApi } from '@/services/api'
import { cn } from '@/utils/cn'
import type { CancellationPolicy } from '@/types/api'

type CardId = 'invite' | 'hours' | 'adjacent' | 'cancel'
type Tone = 'amber' | 'green' | 'red' | 'muted'

type CancellationPolicyUpdate = Partial<CancellationPolicy> & {
  clearPartialRefund?: boolean
}

const toHourTime = (hour: number) => `${String(hour).padStart(2, '0')}:00`
const hourFromTime = (value: string) => Number(value.split(':')[0])

const toneStyles: Record<Tone, { iconBg: string; iconBorder: string; iconColor: string; accent: string; statColor: string }> = {
  amber: {
    iconBg: 'linear-gradient(135deg, rgba(255,179,71,0.25), rgba(242,155,47,0.08))',
    iconBorder: 'rgba(255,179,71,0.35)',
    iconColor: '#ffcb73',
    accent: 'rgba(255,179,71,0.4)',
    statColor: '#ffcb73',
  },
  green: {
    iconBg: 'linear-gradient(135deg, rgba(134,199,137,0.22), rgba(134,199,137,0.06))',
    iconBorder: 'rgba(134,199,137,0.35)',
    iconColor: '#9fdba2',
    accent: 'rgba(134,199,137,0.4)',
    statColor: '#9fdba2',
  },
  red: {
    iconBg: 'linear-gradient(135deg, rgba(214,165,165,0.22), rgba(214,165,165,0.06))',
    iconBorder: 'rgba(214,165,165,0.35)',
    iconColor: '#e8b8b8',
    accent: 'rgba(214,165,165,0.4)',
    statColor: '#e8b8b8',
  },
  muted: {
    iconBg: 'rgba(255,255,255,0.04)',
    iconBorder: 'rgba(255,255,255,0.1)',
    iconColor: 'rgba(255,255,255,0.7)',
    accent: 'rgba(255,255,255,0.2)',
    statColor: 'rgba(255,255,255,0.85)',
  },
}

interface StatusCardProps {
  icon: ReactNode
  tone: Tone
  title: string
  summary: string
  stat: string
  statLabel: string
  footer: string
  monoStat?: boolean
  onClick: () => void
}

function StatusCard({ icon, tone, title, summary, stat, statLabel, footer, monoStat, onClick }: StatusCardProps) {
  const t = toneStyles[tone]
  const [hover, setHover] = useState(false)

  return (
    <button
      type="button"
      onClick={onClick}
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
      className="app-card relative flex flex-col text-left rounded-2xl px-5 py-5 sm:px-6 sm:py-6 transition-[transform,border-color,box-shadow] duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-400/60"
      style={{
        borderColor: hover ? t.accent : 'rgba(255,255,255,0.1)',
        transform: hover ? 'translateY(-2px)' : 'translateY(0)',
      }}
    >
      <div className="flex items-start justify-between mb-4">
        <div
          className="inline-flex items-center justify-center"
          style={{
            width: 44,
            height: 44,
            borderRadius: 12,
            background: t.iconBg,
            border: `1px solid ${t.iconBorder}`,
            color: t.iconColor,
          }}
        >
          {icon}
        </div>
        <ArrowRight size={16} className="text-white/30 mt-2" />
      </div>

      <div className="text-base font-semibold mb-1 text-white">{title}</div>
      <p className="text-[13px] leading-relaxed text-white/50 m-0 min-h-[38px]">{summary}</p>

      <div className="mt-4 pt-3 border-t border-white/[0.06]">
        <div className="flex items-baseline justify-between gap-2">
          <span
            className="text-[22px] font-bold"
            style={{
              color: t.statColor,
              letterSpacing: monoStat ? '0.5px' : '-0.5px',
              fontFamily: monoStat ? "'JetBrains Mono', ui-monospace, monospace" : 'inherit',
            }}
          >
            {stat}
          </span>
          <span className="text-[11px] uppercase tracking-[0.5px] text-white/45">{statLabel}</span>
        </div>
        <div className="text-xs text-white/40 mt-2 truncate">{footer}</div>
      </div>
    </button>
  )
}

interface HeroStatProps {
  label: string
  value: string
  sub?: string
  tone?: 'ok' | 'default'
}

function HeroStat({ label, value, sub, tone = 'default' }: HeroStatProps) {
  return (
    <div
      className="rounded-xl border border-white/10 bg-white/[0.04] px-3 py-2 sm:px-4 sm:py-3 min-w-[96px] sm:min-w-[110px]"
    >
      <div className="text-[10px] sm:text-[11px] uppercase tracking-[0.6px] text-white/50">{label}</div>
      <div
        className={cn(
          'text-lg sm:text-[22px] font-bold mt-0.5 leading-none',
          tone === 'ok' ? 'text-[#9fdba2]' : 'text-white'
        )}
      >
        {value}
        {sub && <span className="ml-1 text-xs font-medium text-white/40">{sub}</span>}
      </div>
    </div>
  )
}

interface DrawerProps {
  open: boolean
  title: string
  eyebrow: string
  eyebrowIcon: ReactNode
  onClose: () => void
  children: ReactNode
  footer?: ReactNode
}

function Drawer({ open, title, eyebrow, eyebrowIcon, onClose, children, footer }: DrawerProps) {
  useEffect(() => {
    if (!open) return
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleKey)
    return () => {
      document.body.style.overflow = previousOverflow
      document.removeEventListener('keydown', handleKey)
    }
  }, [open, onClose])

  return (
    <AnimatePresence>
      {open && (
        <Fragment>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="fixed inset-0 z-[90] bg-black/60 backdrop-blur-sm"
            onClick={onClose}
            aria-hidden
          />

          <motion.aside
            role="dialog"
            aria-modal="true"
            aria-label={title}
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ duration: 0.24, ease: [0.32, 0.72, 0, 1] }}
            className={cn(
              'fixed z-[100] right-0 top-0 bottom-0 flex h-[100dvh] w-full flex-col sm:w-[460px]',
              'bg-[#07060d]/96 backdrop-blur-xl border-l border-white/10',
              'shadow-[0_30px_100px_-45px_rgba(0,0,0,0.9)]'
            )}
          >
            <div className="flex items-start justify-between gap-3 px-5 sm:px-6 pt-5 sm:pt-6 pb-4 border-b border-white/10">
              <div className="min-w-0">
                <div className="flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.8px] text-[#ffdaa5]/85">
                  {eyebrowIcon}
                  <span className="truncate">{eyebrow}</span>
                </div>
                <h2 className="mt-2 text-xl sm:text-[22px] font-bold text-white leading-tight">{title}</h2>
              </div>
              <button
                type="button"
                onClick={onClose}
                aria-label="Zavřít"
                className="inline-flex items-center justify-center w-9 h-9 rounded-lg border border-white/10 bg-white/[0.04] text-white/70 hover:text-white hover:bg-white/[0.08] transition-colors"
              >
                <X size={16} />
              </button>
            </div>

            <div className="flex-1 min-h-0 overflow-y-auto px-5 sm:px-6 py-5">
              <div className="mx-auto w-full max-w-2xl">
                {children}
              </div>
            </div>

            {footer && (
              <div className="px-5 sm:px-6 py-4 border-t border-white/10 bg-gradient-to-t from-[#07060d] to-transparent">
                <div className="mx-auto w-full max-w-2xl">
                  {footer}
                </div>
              </div>
            )}
          </motion.aside>
        </Fragment>
      )}
    </AnimatePresence>
  )
}

export default function Settings() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const { data: settings, isLoading } = useQuery({
    queryKey: ['admin', 'settings'],
    queryFn: adminApi.getSettings,
  })

  const { data: cancellationPolicy, isLoading: isPolicyLoading } = useQuery({
    queryKey: ['admin', 'cancellation-policy'],
    queryFn: adminApi.getCancellationPolicy,
  })

  const { data: stats } = useQuery({
    queryKey: ['admin', 'statistics', 1],
    queryFn: () => adminApi.getStatistics(1),
  })

  const [openCard, setOpenCard] = useState<CardId | null>(null)

  // Hours drawer local state
  const [startHour, setStartHour] = useState<number | null>(null)
  const [endHour, setEndHour] = useState<number | null>(null)

  // Invite drawer local state
  const [copied, setCopied] = useState(false)
  const [showRegenerateModal, setShowRegenerateModal] = useState(false)

  // Cancellation policy drawer local state
  const [policyFullRefundHours, setPolicyFullRefundHours] = useState<number | null>(null)
  const [policyPartialRefundHours, setPolicyPartialRefundHours] = useState<number | null | undefined>(undefined)
  const [policyPartialRefundPercentage, setPolicyPartialRefundPercentage] = useState<number | null>(null)
  const [policyIsActive, setPolicyIsActive] = useState<boolean | null>(null)

  const effectiveStartHour = startHour ?? settings?.calendarStartHour ?? 6
  const effectiveEndHour = endHour ?? settings?.calendarEndHour ?? 22
  const effectiveAdjacentBooking = settings?.adjacentBookingRequired ?? true

  const updateMutation = useMutation({
    mutationFn: adminApi.updateSettings,
    onSuccess: (savedSettings) => {
      queryClient.setQueryData(['admin', 'settings'], savedSettings)
      queryClient.setQueryData(['calendarSettings'], savedSettings)
      queryClient.invalidateQueries({ queryKey: ['admin', 'settings'] })
      queryClient.invalidateQueries({ queryKey: ['calendarSettings'] })
      queryClient.invalidateQueries({ queryKey: ['availableSlots'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'slots'] })
      setStartHour(null)
      setEndHour(null)
      showToast('success', t('admin.settings.saved'))
    },
    onError: () => {
      showToast('error', t('admin.settings.saveError'))
    },
  })

  const regenerateMutation = useMutation({
    mutationFn: adminApi.regenerateInviteCode,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'settings'] })
      showToast('success', t('admin.settings.codeRegenerated'))
      setShowRegenerateModal(false)
    },
    onError: () => {
      showToast('error', t('admin.settings.regenerateError'))
    },
  })

  const updatePolicyMutation = useMutation({
    mutationFn: (params: CancellationPolicyUpdate) => adminApi.updateCancellationPolicy(params),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'cancellation-policy'] })
      showToast('success', t('admin.settings.cancellationPolicy.saved'))
      setPolicyFullRefundHours(null)
      setPolicyPartialRefundHours(undefined)
      setPolicyPartialRefundPercentage(null)
      setPolicyIsActive(null)
    },
    onError: () => {
      showToast('error', t('admin.settings.cancellationPolicy.saveError'))
    },
  })

  const handleSaveHours = () => {
    updateMutation.mutate({
      calendarStartHour: effectiveStartHour,
      calendarEndHour: effectiveEndHour,
    })
  }

  const handleToggleAdjacent = (newValue: boolean) => {
    updateMutation.mutate({ adjacentBookingRequired: newValue })
  }

  const handleSavePolicy = () => {
    const updates: CancellationPolicyUpdate = {}
    if (policyFullRefundHours !== null) updates.fullRefundHours = policyFullRefundHours
    if (policyPartialRefundHours !== undefined) {
      updates.partialRefundHours = policyPartialRefundHours
      if (policyPartialRefundHours === null) {
        updates.partialRefundPercentage = null
        updates.clearPartialRefund = true
      }
    }
    if (policyPartialRefundPercentage !== null) updates.partialRefundPercentage = policyPartialRefundPercentage
    if (policyIsActive !== null) updates.isActive = policyIsActive
    updatePolicyMutation.mutate(updates)
  }

  const handleCopyLink = async () => {
    if (!settings?.inviteLink) return
    const fullUrl = `${window.location.origin}${settings.inviteLink}`
    try {
      await navigator.clipboard.writeText(fullUrl)
      setCopied(true)
      showToast('success', t('admin.settings.linkCopied'))
      setTimeout(() => setCopied(false), 2000)
    } catch {
      showToast('error', t('errors.somethingWrong'))
    }
  }

  const handleRegenerateCode = () => {
    regenerateMutation.mutate()
  }

  const hasPolicyChanges =
    (policyFullRefundHours !== null && policyFullRefundHours !== cancellationPolicy?.fullRefundHours) ||
    (policyPartialRefundHours !== undefined && policyPartialRefundHours !== cancellationPolicy?.partialRefundHours) ||
    (policyPartialRefundPercentage !== null && policyPartialRefundPercentage !== cancellationPolicy?.partialRefundPercentage) ||
    (policyIsActive !== null && policyIsActive !== cancellationPolicy?.isActive)

  const effectivePolicyFullRefundHours = policyFullRefundHours ?? cancellationPolicy?.fullRefundHours ?? 24
  const effectivePolicyPartialRefundHours = policyPartialRefundHours !== undefined
    ? policyPartialRefundHours
    : cancellationPolicy?.partialRefundHours ?? null
  const effectivePolicyPartialRefundPercentage = policyPartialRefundPercentage ?? cancellationPolicy?.partialRefundPercentage ?? null
  const effectivePolicyIsActive = policyIsActive ?? cancellationPolicy?.isActive ?? true

  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-20">
        <Spinner size="lg" />
      </div>
    )
  }

  const fullInviteUrl = settings?.inviteLink
    ? `${window.location.origin}${settings.inviteLink}`
    : null
  const startHourMax = Math.max(0, Math.min(23, effectiveEndHour - 1))
  const endHourMin = Math.max(0, Math.min(23, effectiveStartHour + 1))
  const partialRefundMax = Math.min(168, effectivePolicyFullRefundHours - 1)
  const policyHoursLabel = t('admin.settings.cancellationPolicy.hours')
  const hoursPerDay = effectiveEndHour - effectiveStartHour

  const closeDrawer = () => {
    setOpenCard(null)
    setStartHour(null)
    setEndHour(null)
    setPolicyFullRefundHours(null)
    setPolicyPartialRefundHours(undefined)
    setPolicyPartialRefundPercentage(null)
    setPolicyIsActive(null)
  }

  const cards = [
    {
      id: 'invite' as CardId,
      icon: <Link2 size={20} />,
      tone: 'amber' as Tone,
      title: t('admin.settings.inviteLink'),
      summary: t('admin.settings.cards.invite.summary'),
      stat: settings?.inviteCode ?? '—',
      statLabel: t('admin.settings.cards.invite.statLabel'),
      footer: fullInviteUrl ?? t('admin.settings.noInviteCode'),
      monoStat: true,
    },
    {
      id: 'hours' as CardId,
      icon: <Clock size={20} />,
      tone: 'amber' as Tone,
      title: t('admin.settings.calendarHours'),
      summary: t('admin.settings.cards.hours.summary'),
      stat: `${String(effectiveStartHour).padStart(2, '0')} — ${String(effectiveEndHour).padStart(2, '0')}`,
      statLabel: t('admin.settings.cards.hours.statLabel', { count: hoursPerDay }),
      footer: t('admin.settings.cards.hours.footer'),
    },
    {
      id: 'adjacent' as CardId,
      icon: <SlidersHorizontal size={20} />,
      tone: (effectiveAdjacentBooking ? 'green' : 'muted') as Tone,
      title: t('admin.settings.adjacentBooking'),
      summary: t('admin.settings.cards.adjacent.summary'),
      stat: effectiveAdjacentBooking ? t('admin.settings.cards.adjacent.statOn') : t('admin.settings.cards.adjacent.statOff'),
      statLabel: effectiveAdjacentBooking ? t('admin.settings.cards.adjacent.statLabelOn') : t('admin.settings.cards.adjacent.statLabelOff'),
      footer: t('admin.settings.cards.adjacent.footer'),
    },
    {
      id: 'cancel' as CardId,
      icon: <Ban size={20} />,
      tone: (effectivePolicyIsActive ? 'red' : 'muted') as Tone,
      title: t('admin.settings.cancellationPolicy.title'),
      summary: t('admin.settings.cards.cancel.summary'),
      stat: !effectivePolicyIsActive
        ? t('admin.settings.cards.adjacent.statOff')
        : effectivePolicyPartialRefundHours !== null
          ? `${effectivePolicyFullRefundHours} / ${effectivePolicyPartialRefundHours} h`
          : `${effectivePolicyFullRefundHours} h`,
      statLabel: !effectivePolicyIsActive
        ? t('admin.settings.cards.cancel.statLabelOff')
        : effectivePolicyPartialRefundHours !== null
          ? t('admin.settings.cards.cancel.statLabelOn')
          : t('admin.settings.cards.cancel.statLabelFull'),
      footer: effectivePolicyPartialRefundPercentage !== null
        ? t('admin.settings.cards.cancel.footer', { percentage: effectivePolicyPartialRefundPercentage })
        : t('admin.settings.cards.cancel.footerNoPartial'),
    },
  ]

  const noShowRatePct = stats ? (stats.noShowRate * 100).toFixed(1) : null

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Hero */}
      <div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <div className="flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.8px] text-[#ffdaa5]/85">
            <SettingsIcon size={12} />
            <span>{t('admin.settings.eyebrow')}</span>
          </div>
          <h1 className="mt-2 text-2xl sm:text-[32px] font-bold text-white leading-tight tracking-tight">
            {t('admin.settings.heroTitle')}
          </h1>
          <p className="mt-1 text-sm text-white/55 max-w-xl">
            {t('admin.settings.subtitle')}
          </p>
        </div>

        <div className="flex flex-wrap gap-2 sm:gap-3">
          <HeroStat
            label={t('admin.settings.stats.clients')}
            value={stats?.totalClients?.toString() ?? '—'}
          />
          <HeroStat
            label={t('admin.settings.stats.thisMonth')}
            value={stats?.totalReservations?.toString() ?? '—'}
            sub={t('admin.settings.stats.reservations')}
          />
          {noShowRatePct !== null && (
            <HeroStat
              label={t('admin.settings.stats.noShowRate')}
              value={noShowRatePct}
              sub="%"
              tone="ok"
            />
          )}
        </div>
      </div>

      {/* Card grid */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {cards.map((c) => (
          <StatusCard
            key={c.id}
            icon={c.icon}
            tone={c.tone}
            title={c.title}
            summary={c.summary}
            stat={c.stat}
            statLabel={c.statLabel}
            footer={c.footer}
            monoStat={c.monoStat}
            onClick={() => setOpenCard(c.id)}
          />
        ))}
      </div>

      {/* Drawer: Working hours */}
      <Drawer
        open={openCard === 'hours'}
        title={t('admin.settings.editHeading')}
        eyebrow={t('admin.settings.calendarHours')}
        eyebrowIcon={<Clock size={12} />}
        onClose={closeDrawer}
      >
        <p className="text-sm text-white/55 mb-5">{t('admin.settings.calendarHoursDesc')}</p>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <TimePicker
            label={t('admin.settings.startHour')}
            value={toHourTime(effectiveStartHour)}
            onChange={(value) => {
              const nextHour = hourFromTime(value)
              if (nextHour < effectiveEndHour) setStartHour(nextHour)
            }}
            min="00:00"
            max={toHourTime(startHourMax)}
            hourOnly
          />
          <TimePicker
            label={t('admin.settings.endHour')}
            value={toHourTime(effectiveEndHour)}
            onChange={(value) => {
              const nextHour = hourFromTime(value)
              if (nextHour > effectiveStartHour) setEndHour(nextHour)
            }}
            min={toHourTime(endHourMin)}
            max="23:00"
            hourOnly
          />
        </div>
        <Button
          onClick={handleSaveHours}
          disabled={updateMutation.isPending || (startHour === null && endHour === null)}
          className="mt-4 w-full sm:w-auto"
          leftIcon={updateMutation.isPending ? <Spinner size="sm" /> : <Save size={16} />}
        >
          {t('common.save')}
        </Button>
        <p className="mt-4 text-xs text-white/45">
          {t('admin.settings.cards.hours.helper')}
        </p>
      </Drawer>

      {/* Drawer: Invite link */}
      <Drawer
        open={openCard === 'invite'}
        title={t('admin.settings.editHeading')}
        eyebrow={t('admin.settings.inviteLink')}
        eyebrowIcon={<Link2 size={12} />}
        onClose={closeDrawer}
      >
        <p className="text-sm text-white/55 mb-5">{t('admin.settings.inviteLinkDesc')}</p>

        {settings?.inviteCode ? (
          <div className="space-y-5">
            <div>
              <label className="block text-[11px] font-medium text-white/55 uppercase tracking-[0.6px] mb-2">
                {t('admin.settings.inviteCode')}
              </label>
              <code className="inline-flex items-center px-3 py-2 rounded-lg bg-[rgba(242,155,47,0.1)] border border-[rgba(242,155,47,0.24)] text-primary-200 text-base font-mono tracking-wider">
                {settings.inviteCode}
              </code>
            </div>

            <div>
              <label className="block text-[11px] font-medium text-white/55 uppercase tracking-[0.6px] mb-2">
                {t('admin.settings.fullLink')}
              </label>
              <div className="relative">
                <div
                  className="rounded-lg bg-white/[0.05] border border-white/10 pl-3 pr-12 py-2 text-sm text-white/80 truncate"
                  title={fullInviteUrl ?? undefined}
                >
                  {fullInviteUrl}
                </div>
                <button
                  type="button"
                  onClick={handleCopyLink}
                  aria-label={copied ? t('admin.settings.copied') : t('admin.settings.copyLink')}
                  title={copied ? t('admin.settings.copied') : t('admin.settings.copyLink')}
                  className="absolute right-1.5 top-1/2 -translate-y-1/2 inline-flex items-center justify-center w-8 h-8 rounded-md text-white/60 hover:text-white hover:bg-white/[0.08] transition-colors"
                >
                  {copied ? <Check size={15} className="text-[#9fdba2]" /> : <Copy size={15} />}
                </button>
              </div>
            </div>

            <div className="pt-4 border-t border-white/10">
              <Button
                variant="ghost"
                onClick={() => setShowRegenerateModal(true)}
                className="w-full"
                leftIcon={<RefreshCw size={16} />}
              >
                {t('admin.settings.regenerateCode')}
              </Button>
              <p className="mt-2 text-xs text-white/45">{t('admin.settings.regenerateWarning')}</p>
            </div>
          </div>
        ) : (
          <div className="text-center py-6">
            <p className="text-white/55 mb-4">{t('admin.settings.noInviteCode')}</p>
            <Button
              onClick={handleRegenerateCode}
              disabled={regenerateMutation.isPending}
              leftIcon={regenerateMutation.isPending ? <Spinner size="sm" /> : <RefreshCw size={16} />}
            >
              {t('admin.settings.generateCode')}
            </Button>
          </div>
        )}
      </Drawer>

      {/* Drawer: Adjacent booking */}
      <Drawer
        open={openCard === 'adjacent'}
        title={t('admin.settings.editHeading')}
        eyebrow={t('admin.settings.adjacentBooking')}
        eyebrowIcon={<SlidersHorizontal size={12} />}
        onClose={closeDrawer}
      >
        <div className="rounded-xl bg-white/[0.04] border border-white/[0.06] p-4 flex items-start justify-between gap-4">
          <div className="min-w-0">
            <div className="text-sm font-medium text-white">{t('admin.settings.adjacentBooking')}</div>
            <p className="text-xs text-white/55 mt-1.5 leading-relaxed">
              {effectiveAdjacentBooking
                ? t('admin.settings.adjacentBookingDesc')
                : t('admin.settings.adjacentBookingOffDesc')}
            </p>
          </div>
          <label className="relative inline-flex items-center cursor-pointer mt-0.5">
            <input
              type="checkbox"
              checked={effectiveAdjacentBooking}
              onChange={() => handleToggleAdjacent(!effectiveAdjacentBooking)}
              disabled={updateMutation.isPending}
              className="sr-only peer"
            />
            <div className="w-11 h-6 bg-white/15 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300/40 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-neutral-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-500" />
          </label>
        </div>
        <p className="mt-4 text-xs text-white/45">{t('admin.settings.cards.adjacent.helper')}</p>
      </Drawer>

      {/* Drawer: Cancellation policy */}
      <Drawer
        open={openCard === 'cancel'}
        title={t('admin.settings.editHeading')}
        eyebrow={t('admin.settings.cancellationPolicy.title')}
        eyebrowIcon={<Ban size={12} />}
        onClose={closeDrawer}
      >
        {isPolicyLoading ? (
          <div className="flex justify-center py-6">
            <Spinner size="lg" />
          </div>
        ) : (
          <div className="space-y-5">
            <p className="text-sm text-white/55">{t('admin.settings.cancellationPolicy.description')}</p>

            <div className="rounded-xl bg-white/[0.04] border border-white/[0.06] p-4 flex items-start justify-between gap-4">
              <div className="min-w-0">
                <div className="text-sm font-medium text-white">{t('admin.settings.cancellationPolicy.enablePolicy')}</div>
                <p className="text-xs text-white/55 mt-1.5 leading-relaxed">{t('admin.settings.cancellationPolicy.enablePolicyDesc')}</p>
              </div>
              <label className="relative inline-flex items-center cursor-pointer mt-0.5">
                <input
                  type="checkbox"
                  checked={effectivePolicyIsActive}
                  onChange={(e) => setPolicyIsActive(e.target.checked)}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-white/15 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300/40 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-neutral-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-500" />
              </label>
            </div>

            <div className="grid grid-cols-1 gap-3">
              <div>
                <HourDurationPicker
                  label={t('admin.settings.cancellationPolicy.fullRefundHours')}
                  value={effectivePolicyFullRefundHours}
                  onChange={(value) => {
                    if (value === null) return
                    setPolicyFullRefundHours(value)
                    if (effectivePolicyPartialRefundHours !== null && effectivePolicyPartialRefundHours >= value) {
                      setPolicyPartialRefundHours(null)
                    }
                  }}
                  min={0}
                  max={168}
                  unitLabel={policyHoursLabel}
                />
                <p className="mt-1 text-xs text-white/45">{t('admin.settings.cancellationPolicy.fullRefundHoursDesc')}</p>
              </div>

              <div>
                <HourDurationPicker
                  label={t('admin.settings.cancellationPolicy.partialRefundHours')}
                  value={effectivePolicyPartialRefundHours}
                  onChange={(value) => {
                    setPolicyPartialRefundHours(value)
                    if (value !== null && effectivePolicyPartialRefundPercentage === null) {
                      setPolicyPartialRefundPercentage(50)
                    }
                  }}
                  min={0}
                  max={partialRefundMax}
                  allowNone
                  noneLabel={t('admin.settings.cancellationPolicy.noPartialRefund')}
                  unitLabel={policyHoursLabel}
                />
                <p className="mt-1 text-xs text-white/45">{t('admin.settings.cancellationPolicy.partialRefundHoursDesc')}</p>
              </div>

              {effectivePolicyPartialRefundHours !== null && (
                <div>
                  <label className="block text-[11px] font-medium text-white/55 uppercase tracking-[0.6px] mb-2">
                    {t('admin.settings.cancellationPolicy.partialRefundPercentage')}
                  </label>
                  <div className="flex items-center gap-2">
                    <input
                      type="number"
                      min="0"
                      max="100"
                      value={effectivePolicyPartialRefundPercentage ?? 50}
                      onChange={(e) => setPolicyPartialRefundPercentage(Number(e.target.value))}
                      className="w-full px-3 py-2 rounded-lg bg-white/[0.05] border border-white/10 text-white focus:outline-none focus:border-primary-500/60 focus:ring-2 focus:ring-primary-500/15"
                    />
                    <span className="text-white/55">%</span>
                  </div>
                  <p className="mt-1 text-xs text-white/45">{t('admin.settings.cancellationPolicy.partialRefundPercentageDesc')}</p>
                </div>
              )}
            </div>

            <Button
              onClick={handleSavePolicy}
              disabled={!hasPolicyChanges || updatePolicyMutation.isPending}
              className="w-full sm:w-auto"
              leftIcon={updatePolicyMutation.isPending ? <Spinner size="sm" /> : <Save size={16} />}
            >
              {t('common.save')}
            </Button>

            <div className="rounded-xl border border-white/10 bg-white/[0.03] p-4">
              <h3 className="text-[11px] font-semibold uppercase tracking-[0.6px] text-[#ffdaa5]/85 mb-2">
                {t('admin.settings.cancellationPolicy.summary')}
              </h3>
              <ul className="text-sm text-white/75 space-y-1.5">
                <li>• {t('admin.settings.cancellationPolicy.summaryFullRefund', { hours: effectivePolicyFullRefundHours })}</li>
                {effectivePolicyPartialRefundHours !== null && effectivePolicyPartialRefundPercentage !== null && (
                  <li>• {t('admin.settings.cancellationPolicy.summaryPartialRefund', {
                    hours: effectivePolicyPartialRefundHours,
                    percentage: effectivePolicyPartialRefundPercentage,
                  })}</li>
                )}
                <li>• {t('admin.settings.cancellationPolicy.summaryNoRefund', {
                  hours: effectivePolicyPartialRefundHours ?? effectivePolicyFullRefundHours,
                })}</li>
              </ul>
            </div>
          </div>
        )}
      </Drawer>

      {/* Regenerate confirmation modal — uses centered Modal because it's a confirm dialog */}
      <Modal
        isOpen={showRegenerateModal}
        onClose={() => setShowRegenerateModal(false)}
        title={t('admin.settings.regenerateConfirmTitle')}
        layerClassName="z-[120]"
      >
        <div className="space-y-4">
          <div className="flex items-start gap-3 p-4 bg-amber-500/10 border border-amber-500/30 rounded-lg">
            <AlertTriangle size={20} className="text-amber-400 mt-0.5 shrink-0" />
            <p className="text-sm text-amber-200">{t('admin.settings.regenerateConfirmDesc')}</p>
          </div>

          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => setShowRegenerateModal(false)}>
              {t('common.cancel')}
            </Button>
            <Button
              variant="primary"
              onClick={handleRegenerateCode}
              disabled={regenerateMutation.isPending}
              leftIcon={regenerateMutation.isPending ? <Spinner size="sm" /> : <RefreshCw size={16} />}
            >
              {t('admin.settings.regenerateCode')}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
