import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Settings as SettingsIcon, Clock, Save, Link2, Copy, RefreshCw, Check, AlertTriangle, Ban } from 'lucide-react'
import { Card, Button, Spinner, Modal } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { adminApi } from '@/services/api'
import type { CancellationPolicy } from '@/types/api'

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

  const [startHour, setStartHour] = useState<number | null>(null)
  const [endHour, setEndHour] = useState<number | null>(null)
  const [copied, setCopied] = useState(false)
  const [showRegenerateModal, setShowRegenerateModal] = useState(false)

  // Cancellation policy state
  const [policyFullRefundHours, setPolicyFullRefundHours] = useState<number | null>(null)
  const [policyPartialRefundHours, setPolicyPartialRefundHours] = useState<number | null>(null)
  const [policyPartialRefundPercentage, setPolicyPartialRefundPercentage] = useState<number | null>(null)
  const [policyIsActive, setPolicyIsActive] = useState<boolean | null>(null)

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

  const updatePolicyMutation = useMutation({
    mutationFn: (params: Partial<CancellationPolicy>) => adminApi.updateCancellationPolicy(params),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'cancellation-policy'] })
      showToast('success', t('admin.settings.cancellationPolicy.saved'))
      // Reset local state
      setPolicyFullRefundHours(null)
      setPolicyPartialRefundHours(null)
      setPolicyPartialRefundPercentage(null)
      setPolicyIsActive(null)
    },
    onError: () => {
      showToast('error', t('admin.settings.cancellationPolicy.saveError'))
    },
  })

  const handleSave = () => {
    updateMutation.mutate({
      calendarStartHour: effectiveStartHour,
      calendarEndHour: effectiveEndHour,
    })
  }

  const handleSavePolicy = () => {
    const updates: Partial<CancellationPolicy> = {}
    if (policyFullRefundHours !== null) updates.fullRefundHours = policyFullRefundHours
    if (policyPartialRefundHours !== null) updates.partialRefundHours = policyPartialRefundHours
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

  const hasChanges =
    (startHour !== null && startHour !== settings?.calendarStartHour) ||
    (endHour !== null && endHour !== settings?.calendarEndHour)

  const hasPolicyChanges =
    (policyFullRefundHours !== null && policyFullRefundHours !== cancellationPolicy?.fullRefundHours) ||
    (policyPartialRefundHours !== null && policyPartialRefundHours !== cancellationPolicy?.partialRefundHours) ||
    (policyPartialRefundPercentage !== null && policyPartialRefundPercentage !== cancellationPolicy?.partialRefundPercentage) ||
    (policyIsActive !== null && policyIsActive !== cancellationPolicy?.isActive)

  const effectivePolicyFullRefundHours = policyFullRefundHours ?? cancellationPolicy?.fullRefundHours ?? 24
  const effectivePolicyPartialRefundHours = policyPartialRefundHours ?? cancellationPolicy?.partialRefundHours ?? null
  const effectivePolicyPartialRefundPercentage = policyPartialRefundPercentage ?? cancellationPolicy?.partialRefundPercentage ?? null
  const effectivePolicyIsActive = policyIsActive ?? cancellationPolicy?.isActive ?? true

  const hourOptions = Array.from({ length: 24 }, (_, i) => i)
  const policyHourOptions = Array.from({ length: 169 }, (_, i) => i) // 0-168 hours (1 week)

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

      {/* Cancellation Policy Section */}
      <Card variant="bordered">
        <div className="space-y-6">
          <div className="flex items-center gap-3 pb-4 border-b border-neutral-200 dark:border-dark-border">
            <Ban size={20} className="text-neutral-500" />
            <div>
              <h2 className="font-semibold text-neutral-900 dark:text-white">
                {t('admin.settings.cancellationPolicy.title')}
              </h2>
              <p className="text-sm text-neutral-500 dark:text-neutral-400">
                {t('admin.settings.cancellationPolicy.description')}
              </p>
            </div>
          </div>

          {isPolicyLoading ? (
            <div className="flex justify-center py-8">
              <Spinner size="lg" />
            </div>
          ) : (
            <>
              {/* Enable/Disable Policy */}
              <div className="flex items-center justify-between p-4 bg-neutral-50 dark:bg-dark-surfaceHover rounded-lg">
                <div>
                  <p className="font-medium text-neutral-900 dark:text-white">
                    {t('admin.settings.cancellationPolicy.enablePolicy')}
                  </p>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400">
                    {t('admin.settings.cancellationPolicy.enablePolicyDesc')}
                  </p>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input
                    type="checkbox"
                    checked={effectivePolicyIsActive}
                    onChange={(e) => setPolicyIsActive(e.target.checked)}
                    className="sr-only peer"
                  />
                  <div className="w-11 h-6 bg-neutral-300 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300 dark:peer-focus:ring-primary-800 rounded-full peer dark:bg-neutral-600 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-neutral-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-neutral-600 peer-checked:bg-primary-500"></div>
                </label>
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                {/* Full Refund Hours */}
                <div>
                  <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-2">
                    {t('admin.settings.cancellationPolicy.fullRefundHours')}
                  </label>
                  <select
                    value={effectivePolicyFullRefundHours}
                    onChange={(e) => setPolicyFullRefundHours(Number(e.target.value))}
                    className="w-full px-3 py-2 rounded-lg border border-neutral-300 dark:border-dark-border bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  >
                    {policyHourOptions.map((hour) => (
                      <option key={hour} value={hour}>
                        {hour} {t('admin.settings.cancellationPolicy.hours')}
                      </option>
                    ))}
                  </select>
                  <p className="mt-1 text-xs text-neutral-500 dark:text-neutral-400">
                    {t('admin.settings.cancellationPolicy.fullRefundHoursDesc')}
                  </p>
                </div>

                {/* Partial Refund Hours */}
                <div>
                  <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-2">
                    {t('admin.settings.cancellationPolicy.partialRefundHours')}
                  </label>
                  <select
                    value={effectivePolicyPartialRefundHours ?? ''}
                    onChange={(e) => setPolicyPartialRefundHours(e.target.value ? Number(e.target.value) : null)}
                    className="w-full px-3 py-2 rounded-lg border border-neutral-300 dark:border-dark-border bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  >
                    <option value="">{t('admin.settings.cancellationPolicy.noPartialRefund')}</option>
                    {policyHourOptions.filter(h => h < effectivePolicyFullRefundHours).map((hour) => (
                      <option key={hour} value={hour}>
                        {hour} {t('admin.settings.cancellationPolicy.hours')}
                      </option>
                    ))}
                  </select>
                  <p className="mt-1 text-xs text-neutral-500 dark:text-neutral-400">
                    {t('admin.settings.cancellationPolicy.partialRefundHoursDesc')}
                  </p>
                </div>

                {/* Partial Refund Percentage */}
                {effectivePolicyPartialRefundHours !== null && (
                  <div>
                    <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-2">
                      {t('admin.settings.cancellationPolicy.partialRefundPercentage')}
                    </label>
                    <div className="flex items-center gap-2">
                      <input
                        type="number"
                        min="0"
                        max="100"
                        value={effectivePolicyPartialRefundPercentage ?? 50}
                        onChange={(e) => setPolicyPartialRefundPercentage(Number(e.target.value))}
                        className="w-full px-3 py-2 rounded-lg border border-neutral-300 dark:border-dark-border bg-white dark:bg-dark-surface text-neutral-900 dark:text-white focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                      />
                      <span className="text-neutral-500">%</span>
                    </div>
                    <p className="mt-1 text-xs text-neutral-500 dark:text-neutral-400">
                      {t('admin.settings.cancellationPolicy.partialRefundPercentageDesc')}
                    </p>
                  </div>
                )}
              </div>

              {/* Policy Summary */}
              <div className="p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
                <h3 className="font-medium text-blue-800 dark:text-blue-200 mb-2">
                  {t('admin.settings.cancellationPolicy.summary')}
                </h3>
                <ul className="text-sm text-blue-700 dark:text-blue-300 space-y-1">
                  <li>• {t('admin.settings.cancellationPolicy.summaryFullRefund', { hours: effectivePolicyFullRefundHours })}</li>
                  {effectivePolicyPartialRefundHours !== null && effectivePolicyPartialRefundPercentage !== null && (
                    <li>• {t('admin.settings.cancellationPolicy.summaryPartialRefund', {
                      hours: effectivePolicyPartialRefundHours,
                      percentage: effectivePolicyPartialRefundPercentage
                    })}</li>
                  )}
                  <li>• {t('admin.settings.cancellationPolicy.summaryNoRefund', {
                    hours: effectivePolicyPartialRefundHours ?? effectivePolicyFullRefundHours
                  })}</li>
                </ul>
              </div>

              <div className="pt-4 border-t border-neutral-200 dark:border-dark-border">
                <Button
                  onClick={handleSavePolicy}
                  disabled={!hasPolicyChanges || updatePolicyMutation.isPending}
                  className="flex items-center gap-2"
                >
                  {updatePolicyMutation.isPending ? (
                    <Spinner size="sm" />
                  ) : (
                    <Save size={18} />
                  )}
                  {t('common.save')}
                </Button>
              </div>
            </>
          )}
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
