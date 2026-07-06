import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { AlertTriangle, ChevronRight, CreditCard, UserRound, WalletCards } from 'lucide-react'
import { Badge, Card } from '@/components/ui'
import EmptyState from '@/components/ui/EmptyState'
import { ClientSkeleton } from '@/components/ui/Skeleton'
import { adminApi } from '@/services/api'
import { formatCredits } from '@/utils/formatters'

export default function Debtors() {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()

  const { data: debtors = [], isLoading } = useQuery({
    queryKey: ['admin', 'clients', 'debtors'],
    queryFn: adminApi.getDebtors,
  })

  const summary = useMemo(() => {
    const totalDebt = debtors.reduce((sum, client) => sum + Math.abs(Math.min(client.credits, 0)), 0)
    const largestDebt = debtors.reduce((max, client) => Math.max(max, Math.abs(Math.min(client.credits, 0))), 0)
    return { totalDebt, largestDebt }
  }, [debtors])

  const maxDebt = Math.max(summary.largestDebt, 1)

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
            {t('admin.debtorsTitle')}
          </h1>
          <p className="mt-1 text-sm text-neutral-500 dark:text-neutral-400">
            {t('admin.debtorsSubtitle')}
          </p>
        </div>
        {debtors.length > 0 && (
          <Badge variant="danger" size="md">
            {t('admin.debtorsBadge', { count: debtors.length })}
          </Badge>
        )}
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        <Card variant="bordered" className="min-h-[108px]">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-sm text-neutral-500 dark:text-neutral-400">{t('admin.totalDebt')}</p>
              <p className="mt-2 text-3xl font-heading font-bold text-red-300">
                {summary.totalDebt}
              </p>
            </div>
            <div className="rounded-lg bg-red-300/12 p-2 text-red-200">
              <WalletCards size={20} />
            </div>
          </div>
          <p className="mt-2 text-xs text-neutral-500 dark:text-neutral-400">
            {formatCredits(summary.totalDebt, i18n.language)}
          </p>
        </Card>

        <Card variant="bordered" className="min-h-[108px]">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-sm text-neutral-500 dark:text-neutral-400">{t('admin.debtorsCount')}</p>
              <p className="mt-2 text-3xl font-heading font-bold text-neutral-900 dark:text-white">
                {debtors.length}
              </p>
            </div>
            <div className="rounded-lg bg-amber-300/12 p-2 text-amber-200">
              <UserRound size={20} />
            </div>
          </div>
          <p className="mt-2 text-xs text-neutral-500 dark:text-neutral-400">
            {t('admin.clientsWithNegativeCredits')}
          </p>
        </Card>

        <Card variant="bordered" className="min-h-[108px]">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-sm text-neutral-500 dark:text-neutral-400">{t('admin.largestDebt')}</p>
              <p className="mt-2 text-3xl font-heading font-bold text-neutral-900 dark:text-white">
                {summary.largestDebt}
              </p>
            </div>
            <div className="rounded-lg bg-white/10 p-2 text-white/75">
              <CreditCard size={20} />
            </div>
          </div>
          <p className="mt-2 text-xs text-neutral-500 dark:text-neutral-400">
            {formatCredits(summary.largestDebt, i18n.language)}
          </p>
        </Card>
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {[...Array(4)].map((_, index) => (
            <ClientSkeleton key={index} />
          ))}
        </div>
      ) : debtors.length > 0 ? (
        <div className="space-y-3">
          {debtors.map((client) => {
            const owedCredits = Math.abs(client.credits)
            const width = `${Math.max(12, Math.round((owedCredits / maxDebt) * 100))}%`

            return (
              <Card
                key={client.id}
                variant="bordered"
                className="cursor-pointer transition-colors hover:border-red-300/40"
                onClick={() => navigate(`/admin/clients/${client.id}`)}
              >
                <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex min-w-0 items-center gap-4">
                    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg bg-red-300/12 text-red-200">
                      <AlertTriangle size={20} />
                    </div>
                    <div className="min-w-0">
                      <p className="truncate font-medium text-neutral-900 dark:text-white">
                        {client.firstName} {client.lastName}
                      </p>
                      <p className="truncate text-sm text-neutral-500 dark:text-neutral-400">
                        {client.email}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center justify-between gap-4 sm:min-w-[300px]">
                    <div className="min-w-0 flex-1">
                      <div className="mb-2 flex items-center justify-between gap-3 text-sm">
                        <span className="text-neutral-500 dark:text-neutral-400">
                          {t('admin.owes')}
                        </span>
                        <span className="font-semibold text-red-300">
                          {formatCredits(owedCredits, i18n.language)}
                        </span>
                      </div>
                      <div className="h-2 overflow-hidden rounded-full bg-white/10">
                        <div className="h-full rounded-full bg-red-300/70" style={{ width }} />
                      </div>
                    </div>
                    <Badge variant="danger" size="md" className="shrink-0">
                      {client.credits}
                    </Badge>
                    <ChevronRight size={20} className="shrink-0 text-neutral-400" />
                  </div>
                </div>
              </Card>
            )
          })}
        </div>
      ) : (
        <EmptyState
          icon={WalletCards}
          title={t('admin.noDebtors')}
          description={t('admin.noDebtorsDesc')}
        />
      )}
    </div>
  )
}
