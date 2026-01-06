import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Settings as SettingsIcon, Clock, Save, Link2, Copy, RefreshCw, Check, AlertTriangle } from 'lucide-react'
import { Card, Button, Spinner, Modal } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { adminApi } from '@/services/api'

export default function Settings() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const { data: settings, isLoading } = useQuery({
    queryKey: ['admin', 'settings'],
    queryFn: adminApi.getSettings,
  })

  const [startHour, setStartHour] = useState<number | null>(null)
  const [endHour, setEndHour] = useState<number | null>(null)
  const [copied, setCopied] = useState(false)
  const [showRegenerateModal, setShowRegenerateModal] = useState(false)

  // Initialize local state when settings load
  const effectiveStartHour = startHour ?? settings?.calendarStartHour ?? 6
  const effectiveEndHour = endHour ?? settings?.calendarEndHour ?? 22

  const updateMutation = useMutation({
    mutationFn: adminApi.updateSettings,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'settings'] })
      queryClient.invalidateQueries({ queryKey: ['calendarSettings'] })
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

  const handleSave = () => {
    updateMutation.mutate({
      calendarStartHour: effectiveStartHour,
      calendarEndHour: effectiveEndHour,
    })
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

  const hasChanges =
    (startHour !== null && startHour !== settings?.calendarStartHour) ||
    (endHour !== null && endHour !== settings?.calendarEndHour)

  const hourOptions = Array.from({ length: 24 }, (_, i) => i)

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

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-lg bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
          <SettingsIcon size={20} className="text-primary-500" />
        </div>
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {t('admin.settings.title')}
        </h1>
      </div>

      {/* Invite Link Section */}
      <Card variant="bordered">
        <div className="space-y-6">
          <div className="flex items-center gap-3 pb-4 border-b border-neutral-200 dark:border-dark-border">
            <Link2 size={20} className="text-neutral-500" />
            <div>
              <h2 className="font-semibold text-neutral-900 dark:text-white">
                {t('admin.settings.inviteLink')}
              </h2>
              <p className="text-sm text-neutral-500 dark:text-neutral-400">
                {t('admin.settings.inviteLinkDesc')}
              </p>
            </div>
          </div>

          {settings?.inviteCode ? (
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-2">
                  {t('admin.settings.inviteCode')}
                </label>
                <div className="flex items-center gap-3">
                  <code className="px-4 py-2 bg-neutral-100 dark:bg-dark-surfaceHover rounded-lg text-lg font-mono text-neutral-900 dark:text-white">
                    {settings.inviteCode}
                  </code>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-2">
                  {t('admin.settings.fullLink')}
                </label>
                <div className="flex items-stretch gap-2">
                  <div className="flex-1 px-3 py-2 bg-neutral-100 dark:bg-dark-surfaceHover rounded-lg text-sm text-neutral-600 dark:text-neutral-400 overflow-hidden">
                    <span className="break-all">{fullInviteUrl}</span>
                  </div>
                  <Button
                    variant="secondary"
                    onClick={handleCopyLink}
                    className="flex items-center gap-2 shrink-0"
                  >
                    {copied ? <Check size={18} /> : <Copy size={18} />}
                    {copied ? t('admin.settings.copied') : t('admin.settings.copyLink')}
                  </Button>
                </div>
              </div>

              <div className="pt-4 border-t border-neutral-200 dark:border-dark-border">
                <Button
                  variant="ghost"
                  onClick={() => setShowRegenerateModal(true)}
                  className="flex items-center gap-2 text-amber-600 hover:bg-amber-50 dark:text-amber-400 dark:hover:bg-amber-900/20"
                >
                  <RefreshCw size={18} />
                  {t('admin.settings.regenerateCode')}
                </Button>
                <p className="mt-2 text-sm text-neutral-500 dark:text-neutral-400">
                  {t('admin.settings.regenerateWarning')}
                </p>
              </div>
            </div>
          ) : (
            <div className="text-center py-8">
              <p className="text-neutral-500 dark:text-neutral-400 mb-4">
                {t('admin.settings.noInviteCode')}
              </p>
              <Button
                onClick={handleRegenerateCode}
                disabled={regenerateMutation.isPending}
                className="flex items-center gap-2 mx-auto"
              >
                {regenerateMutation.isPending ? (
                  <Spinner size="sm" />
                ) : (
                  <RefreshCw size={18} />
                )}
                {t('admin.settings.generateCode')}
              </Button>
            </div>
          )}
        </div>
      </Card>

      {/* Calendar Hours Section */}
      <Card variant="bordered">
        <div className="space-y-6">
          <div className="flex items-center gap-3 pb-4 border-b border-neutral-200 dark:border-dark-border">
            <Clock size={20} className="text-neutral-500" />
            <div>
              <h2 className="font-semibold text-neutral-900 dark:text-white">
                {t('admin.settings.calendarHours')}
              </h2>
              <p className="text-sm text-neutral-500 dark:text-neutral-400">
                {t('admin.settings.calendarHoursDesc')}
              </p>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-2">
                {t('admin.settings.startHour')}
              </label>
              <select
                value={effectiveStartHour}
                onChange={(e) => setStartHour(Number(e.target.value))}
                className="w-full px-3 py-2 rounded-lg border border-neutral-300 dark:border-dark-border bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              >
                {hourOptions.map((hour) => (
                  <option key={hour} value={hour} disabled={hour >= effectiveEndHour}>
                    {hour.toString().padStart(2, '0')}:00
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-2">
                {t('admin.settings.endHour')}
              </label>
              <select
                value={effectiveEndHour}
                onChange={(e) => setEndHour(Number(e.target.value))}
                className="w-full px-3 py-2 rounded-lg border border-neutral-300 dark:border-dark-border bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              >
                {hourOptions.map((hour) => (
                  <option key={hour} value={hour} disabled={hour <= effectiveStartHour}>
                    {hour.toString().padStart(2, '0')}:00
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="pt-4 border-t border-neutral-200 dark:border-dark-border">
            <Button
              onClick={handleSave}
              disabled={!hasChanges || updateMutation.isPending}
              className="flex items-center gap-2"
            >
              {updateMutation.isPending ? (
                <Spinner size="sm" />
              ) : (
                <Save size={18} />
              )}
              {t('common.save')}
            </Button>
          </div>
        </div>
      </Card>

      {/* Regenerate Code Confirmation Modal */}
      <Modal
        isOpen={showRegenerateModal}
        onClose={() => setShowRegenerateModal(false)}
        title={t('admin.settings.regenerateConfirmTitle')}
      >
        <div className="space-y-4">
          <div className="flex items-start gap-3 p-4 bg-amber-50 dark:bg-amber-900/20 rounded-lg">
            <AlertTriangle size={20} className="text-amber-500 mt-0.5 shrink-0" />
            <p className="text-sm text-amber-800 dark:text-amber-200">
              {t('admin.settings.regenerateConfirmDesc')}
            </p>
          </div>

          <div className="flex justify-end gap-3">
            <Button
              variant="secondary"
              onClick={() => setShowRegenerateModal(false)}
            >
              {t('common.cancel')}
            </Button>
            <Button
              variant="primary"
              onClick={handleRegenerateCode}
              disabled={regenerateMutation.isPending}
              className="flex items-center gap-2 bg-amber-500 hover:bg-amber-600"
            >
              {regenerateMutation.isPending ? (
                <Spinner size="sm" />
              ) : (
                <RefreshCw size={18} />
              )}
              {t('admin.settings.regenerateCode')}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
