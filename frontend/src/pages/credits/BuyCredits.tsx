import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { CreditCard, Gift, Check, TrendingDown, Star } from 'lucide-react'
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
          (() => {
            // Calculate price per credit for each package
            const packagesWithPricing = packages?.map((pkg) => {
              const totalCredits = pkg.credits + (pkg.bonusCredits || 0)
              const pricePerCredit = pkg.priceCzk / totalCredits
              return { ...pkg, totalCredits, pricePerCredit }
            }) || []

            // Find best value (lowest price per credit)
            const bestValueId = packagesWithPricing.length > 0
              ? packagesWithPricing.reduce((best, pkg) =>
                  pkg.pricePerCredit < best.pricePerCredit ? pkg : best
                ).id
              : null

            // Find most popular (middle package or one with most bonus)
            const mostPopularId = packagesWithPricing.length >= 3
              ? packagesWithPricing[Math.floor(packagesWithPricing.length / 2)].id
              : packagesWithPricing.length > 0
                ? packagesWithPricing.reduce((best, pkg) =>
                    (pkg.bonusCredits || 0) > (best.bonusCredits || 0) ? pkg : best
                  ).id
                : null

            // Base price per credit (smallest package, usually highest price per credit)
            const basePricePerCredit = packagesWithPricing.length > 0
              ? Math.max(...packagesWithPricing.map((p) => p.pricePerCredit))
              : 0

            return (
              <div className="grid gap-4 sm:grid-cols-2">
                {packagesWithPricing.map((pkg) => {
                  const isBestValue = pkg.id === bestValueId
                  const isPopular = pkg.id === mostPopularId && !isBestValue
                  const savingsPercent = basePricePerCredit > 0
                    ? Math.round((1 - pkg.pricePerCredit / basePricePerCredit) * 100)
                    : 0

                  return (
                    <Card
                      key={pkg.id}
                      variant="bordered"
                      className={`relative ${
                        isBestValue
                          ? 'border-green-500 dark:border-green-500 ring-1 ring-green-500/20'
                          : isPopular
                            ? 'border-primary-500 dark:border-primary-500'
                            : ''
                      }`}
                    >
                      <div className="text-center pt-2">
                        {(isBestValue || isPopular) && (
                          <div className="mb-2">
                            {isBestValue && (
                              <Badge
                                variant="success"
                                className="inline-flex items-center gap-1"
                              >
                                <TrendingDown size={12} />
                                {i18n.language === 'cs' ? 'Nejlepší hodnota' : 'Best Value'}
                              </Badge>
                            )}
                            {isPopular && (
                              <Badge
                                variant="primary"
                                className="inline-flex items-center gap-1"
                              >
                                <Star size={12} />
                                {i18n.language === 'cs' ? 'Oblíbené' : 'Popular'}
                              </Badge>
                            )}
                          </div>
                        )}
                        <p className="text-3xl font-heading font-bold text-neutral-900 dark:text-white">
                          {pkg.credits}
                        </p>
                        <p className="text-sm text-neutral-500 dark:text-neutral-400">
                          {i18n.language === 'cs' ? pkg.nameCs : pkg.nameEn || pkg.nameCs}
                        </p>

                        {pkg.bonusCredits > 0 && (
                          <div className="flex items-center justify-center gap-1 mt-2">
                            <Gift size={14} className="text-green-500" />
                            <span className="text-sm font-medium text-green-600 dark:text-green-400">
                              {t('credits.bonus', { bonus: pkg.bonusCredits })}
                            </span>
                          </div>
                        )}

                        <div className="mt-4 py-3 border-t border-neutral-100 dark:border-dark-border">
                          <p className="text-2xl font-bold text-neutral-900 dark:text-white">
                            {formatCurrency(pkg.priceCzk)}
                          </p>
                          <p className="text-sm text-neutral-500 dark:text-neutral-400">
                            {formatCurrency(pkg.pricePerCredit)}/kredit
                          </p>
                          {savingsPercent > 0 && (
                            <p className="text-xs font-medium text-green-600 dark:text-green-400 mt-1">
                              {i18n.language === 'cs' ? `Ušetříte ${savingsPercent}%` : `Save ${savingsPercent}%`}
                            </p>
                          )}
                        </div>

                        <Button
                          className="w-full mt-4"
                          variant={isBestValue ? 'primary' : isPopular ? 'primary' : 'secondary'}
                          onClick={() => handlePurchase(pkg.id)}
                          isLoading={purchaseMutation.isPending}
                        >
                          {i18n.language === 'cs' ? 'Koupit' : 'Buy'}
                        </Button>
                      </div>
                    </Card>
                  )
                })}
              </div>
            )
          })()
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
