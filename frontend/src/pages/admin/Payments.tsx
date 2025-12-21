import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { DollarSign, Check, X, Clock, ExternalLink } from 'lucide-react'
import { Card, Badge, Spinner } from '@/components/ui'
import { adminApi } from '@/services/api'
import { formatCurrency } from '@/utils/formatters'

export default function Payments() {
  const { t, i18n } = useTranslation()

  const { data: payments, isLoading } = useQuery({
    queryKey: ['admin', 'payments'],
    queryFn: adminApi.getPayments,
  })

  const getStatusBadge = (state: string) => {
    switch (state) {
      case 'PAID':
        return (
          <Badge variant="success" className="flex items-center gap-1">
            <Check size={12} />
            Zaplaceno
          </Badge>
        )
      case 'CREATED':
      case 'PAYMENT_METHOD_CHOSEN':
        return (
          <Badge variant="warning" className="flex items-center gap-1">
            <Clock size={12} />
            Čeká
          </Badge>
        )
      case 'CANCELED':
      case 'TIMEOUTED':
        return (
          <Badge variant="danger" className="flex items-center gap-1">
            <X size={12} />
            Zrušeno
          </Badge>
        )
      case 'REFUNDED':
        return (
          <Badge variant="info" className="flex items-center gap-1">
            Vráceno
          </Badge>
        )
      default:
        return <Badge>{state}</Badge>
    }
  }

  // Group payments by month
  const paymentsByMonth = payments?.reduce(
    (acc, payment) => {
      const date = new Date(payment.createdAt)
      const monthKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
      if (!acc[monthKey]) {
        acc[monthKey] = {
          month: date.toLocaleDateString(i18n.language, { year: 'numeric', month: 'long' }),
          payments: [],
          total: 0,
        }
      }
      acc[monthKey].payments.push(payment)
      if (payment.state === 'PAID') {
        acc[monthKey].total += payment.amount
      }
      return acc
    },
    {} as Record<string, { month: string; payments: typeof payments; total: number }>
  )

  const sortedMonths = paymentsByMonth
    ? Object.keys(paymentsByMonth).sort((a, b) => b.localeCompare(a))
    : []

  return (
    <div className="space-y-6 animate-fade-in">
      <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
        {t('admin.payments')}
      </h1>

      {/* Stats */}
      <div className="grid gap-4 sm:grid-cols-3">
        <Card variant="bordered">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-lg bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
              <DollarSign size={24} className="text-green-500" />
            </div>
            <div>
              <p className="text-sm text-neutral-500 dark:text-neutral-400">Tento měsíc</p>
              <p className="text-xl font-bold text-neutral-900 dark:text-white">
                {formatCurrency(
                  payments
                    ?.filter((p) => {
                      const now = new Date()
                      const paymentDate = new Date(p.createdAt)
                      return (
                        p.state === 'PAID' &&
                        paymentDate.getMonth() === now.getMonth() &&
                        paymentDate.getFullYear() === now.getFullYear()
                      )
                    })
                    .reduce((sum, p) => sum + p.amount, 0) || 0
                )}
              </p>
            </div>
          </div>
        </Card>

        <Card variant="bordered">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-lg bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center">
              <Check size={24} className="text-blue-500" />
            </div>
            <div>
              <p className="text-sm text-neutral-500 dark:text-neutral-400">Úspěšných plateb</p>
              <p className="text-xl font-bold text-neutral-900 dark:text-white">
                {payments?.filter((p) => p.state === 'PAID').length || 0}
              </p>
            </div>
          </div>
        </Card>

        <Card variant="bordered">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-lg bg-yellow-100 dark:bg-yellow-900/30 flex items-center justify-center">
              <Clock size={24} className="text-yellow-500" />
            </div>
            <div>
              <p className="text-sm text-neutral-500 dark:text-neutral-400">Čekající</p>
              <p className="text-xl font-bold text-neutral-900 dark:text-white">
                {payments?.filter((p) => ['CREATED', 'PAYMENT_METHOD_CHOSEN'].includes(p.state))
                  .length || 0}
              </p>
            </div>
          </div>
        </Card>
      </div>

      {/* Payments list by month */}
      {isLoading ? (
        <div className="flex justify-center py-12">
          <Spinner size="lg" />
        </div>
      ) : sortedMonths.length > 0 ? (
        <div className="space-y-6">
          {sortedMonths.map((monthKey) => {
            const { month, payments: monthPayments, total } = paymentsByMonth![monthKey]

            return (
              <div key={monthKey}>
                <div className="flex items-center justify-between mb-3">
                  <h2 className="font-heading font-semibold text-neutral-900 dark:text-white">
                    {month}
                  </h2>
                  <span className="text-sm font-medium text-green-600 dark:text-green-400">
                    {formatCurrency(total)}
                  </span>
                </div>

                <Card variant="bordered" padding="none">
                  <div className="divide-y divide-neutral-100 dark:divide-dark-border">
                    {monthPayments?.map((payment) => (
                      <div key={payment.id} className="flex items-center justify-between p-4">
                        <div className="flex items-center gap-4">
                          <div className="hidden sm:block w-10 h-10 rounded-full bg-neutral-100 dark:bg-dark-surface flex items-center justify-center">
                            <DollarSign size={18} className="text-neutral-500" />
                          </div>
                          <div>
                            <p className="font-medium text-neutral-900 dark:text-white">
                              {payment.userName || 'Neznámý'}
                            </p>
                            <div className="flex items-center gap-2 text-sm text-neutral-500 dark:text-neutral-400">
                              <span>
                                {new Date(payment.createdAt).toLocaleDateString(i18n.language)}
                              </span>
                              <span>·</span>
                              <span>{payment.creditPackageName || 'Balíček'}</span>
                            </div>
                          </div>
                        </div>

                        <div className="flex items-center gap-4">
                          <div className="text-right">
                            <p className="font-semibold text-neutral-900 dark:text-white">
                              {formatCurrency(payment.amount)}
                            </p>
                            {getStatusBadge(payment.state)}
                          </div>
                          {payment.gopayId && (
                            <a
                              href={`https://gate.gopay.cz/gp-gw/paymentStatus/${payment.gopayId}`}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="p-2 text-neutral-400 hover:text-neutral-600"
                            >
                              <ExternalLink size={16} />
                            </a>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </Card>
              </div>
            )
          })}
        </div>
      ) : (
        <Card variant="bordered">
          <div className="text-center py-12">
            <DollarSign className="mx-auto mb-4 text-neutral-300 dark:text-neutral-600" size={48} />
            <p className="text-neutral-500 dark:text-neutral-400">Žádné platby</p>
          </div>
        </Card>
      )}
    </div>
  )
}
