import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { CreditCard, Check, TrendingUp, Star, Percent } from 'lucide-react'
import { Card, Button, Badge, Spinner } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { creditApi } from '@/services/api'
import { useAuthStore } from '@/stores/authStore'
import { formatCurrency } from '@/utils/formatters'

export default function BuyCredits() {
  const { t, i18n } = useTranslation()
  const { user, updateUser } = useAuthStore()
  const { showToast } = useToast()
  const queryClient = useQueryClient()

  const { data: packages, isLoading: packagesLoading } = useQuery({
    queryKey: ['creditPackages'],
    queryFn: creditApi.getPackages,
  })

  const { data: transactions } = useQuery({
    queryKey: ['credits', 'history'],
    queryFn: () => creditApi.getHistory(10),
  })

  const purchaseMutation = useMutation({
    mutationFn: creditApi.purchase,
    onSuccess: async (data) => {
      // Check if payment was completed directly (demo mode)
      if (data.status === 'completed') {
        showToast('success', 'Kredity byly připsány!')
        queryClient.invalidateQueries({ queryKey: ['credits'] })
        // Refresh user data
        const userData = await import('@/services/api').then((m) => m.authApi.getMe())
        updateUser(userData)
      } else if (data.gwUrl) {
        // Redirect to GoPay
        window.location.href = data.gwUrl
      } else {
        // Fallback: simulate payment (GoPay not implemented yet)
        try {
          await creditApi.simulatePayment(data.paymentId)
          showToast('success', 'Kredity byly připsány!')
          queryClient.invalidateQueries({ queryKey: ['credits'] })
          const userData = await import('@/services/api').then((m) => m.authApi.getMe())
          updateUser(userData)
        } catch {
          showToast('error', 'Platba se nezdařila')
        }
      }
    },
    onError: () => {
      showToast('error', t('errors.somethingWrong'))
    },
  })

  const handlePurchase = (packageId: string) => {
    purchaseMutation.mutate(packageId)
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
        {t('credits.buyCredits')}
      </h1>

      {/* Current balance */}
      <Card variant="elevated" className="bg-gradient-to-br from-primary-500 to-primary-600 text-white">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm text-white/80">{t('credits.balance')}</p>
            <p className="text-4xl font-heading font-bold mt-1">{user?.credits || 0}</p>
          </div>
          <CreditCard size={48} className="text-white/30" />
        </div>
      </Card>

      {/* Packages */}
      <div>
        <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white mb-4">
          {t('credits.packages')}
        </h2>

        {packagesLoading ? (
          <div className="flex justify-center py-12">
            <Spinner size="lg" />
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2">
            {packages?.map((pkg) => {
              const pricePerCredit = pkg.credits > 0 ? pkg.priceCzk / pkg.credits : 0
              const isBestValue = pkg.highlightType === 'BEST_VALUE'
              const isBestSeller = pkg.highlightType === 'BEST_SELLER'

              return (
                <Card
                  key={pkg.id}
                  variant="bordered"
                  className={`relative ${
                    isBestValue
                      ? 'border-green-500 dark:border-green-500 ring-1 ring-green-500/20'
                      : isBestSeller
                        ? 'border-amber-500 dark:border-amber-500 ring-1 ring-amber-500/20'
                        : ''
                  }`}
                >
                  <div className="text-center pt-2">
                    {/* Highlight badges - reserve space for consistent card heights */}
                    <div className="mb-2 min-h-[24px]">
                      {isBestValue && (
                        <Badge
                          variant="success"
                          className="inline-flex items-center gap-1"
                        >
                          <TrendingUp size={12} />
                          {t('admin.bestValue')}
                        </Badge>
                      )}
                      {isBestSeller && (
                        <Badge
                          variant="warning"
                          className="inline-flex items-center gap-1"
                        >
                          <Star size={12} />
                          {t('admin.bestSeller')}
                        </Badge>
                      )}
                    </div>

                    <p className="text-3xl font-heading font-bold text-neutral-900 dark:text-white">
                      {pkg.credits}
                    </p>
                    <p
                      className="text-sm text-neutral-500 dark:text-neutral-400 truncate"
                      title={i18n.language === 'cs' ? pkg.nameCs : pkg.nameEn || pkg.nameCs}
                    >
                      {i18n.language === 'cs' ? pkg.nameCs : pkg.nameEn || pkg.nameCs}
                    </p>

                    {/* Discount percentage */}
                    {pkg.discountPercent && pkg.discountPercent > 0 && (
                      <div className="flex items-center justify-center gap-1 mt-2">
                        <Percent size={14} className="text-green-500" />
                        <span className="text-sm font-medium text-green-600 dark:text-green-400">
                          {t('credits.savePercent', { percent: pkg.discountPercent })}
                        </span>
                      </div>
                    )}

                    <div className="mt-4 py-3 border-t border-neutral-100 dark:border-dark-border">
                      <p className="text-2xl font-bold text-neutral-900 dark:text-white">
                        {formatCurrency(pkg.priceCzk)}
                      </p>
                      <p className="text-sm text-neutral-500 dark:text-neutral-400">
                        {formatCurrency(Math.round(pricePerCredit))}{t('admin.perTraining')}
                      </p>
                    </div>

                    <Button
                      className="w-full mt-4"
                      variant={(isBestValue || isBestSeller) ? 'primary' : 'secondary'}
                      onClick={() => handlePurchase(pkg.id)}
                      isLoading={purchaseMutation.isPending}
                    >
                      {t('plans.buy')}
                    </Button>
                  </div>
                </Card>
              )
            })}
          </div>
        )}
      </div>

      {/* Transaction history */}
      <div>
        <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white mb-4">
          {t('credits.history')}
        </h2>

        <Card variant="bordered" padding="none">
          {transactions && transactions.length > 0 ? (
            <div className="divide-y divide-neutral-100 dark:divide-dark-border">
              {transactions.map((transaction) => (
                <div key={transaction.id} className="flex items-center justify-between p-4">
                  <div className="flex items-center gap-3">
                    <div
                      className={`w-8 h-8 rounded-full flex items-center justify-center ${
                        transaction.amount > 0
                          ? 'bg-green-100 dark:bg-green-900/30'
                          : 'bg-red-100 dark:bg-red-900/30'
                      }`}
                    >
                      {transaction.amount > 0 ? (
                        <Check size={16} className="text-green-600" />
                      ) : (
                        <CreditCard size={16} className="text-red-600" />
                      )}
                    </div>
                    <div>
                      <p className="font-medium text-neutral-900 dark:text-white">
                        {t(`credits.${transaction.type}`)}
                      </p>
                      <p className="text-sm text-neutral-500 dark:text-neutral-400">
                        {new Date(transaction.createdAt).toLocaleDateString(i18n.language)}
                      </p>
                    </div>
                  </div>
                  <span
                    className={`font-semibold ${
                      transaction.amount > 0
                        ? 'text-green-600 dark:text-green-400'
                        : 'text-red-600 dark:text-red-400'
                    }`}
                  >
                    {transaction.amount > 0 ? '+' : ''}
                    {transaction.amount}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-center py-8 text-neutral-500 dark:text-neutral-400">
              {t('credits.noTransactions')}
            </p>
          )}
        </Card>
      </div>
    </div>
  )
}
