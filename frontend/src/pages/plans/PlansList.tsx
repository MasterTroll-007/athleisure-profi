import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { CreditCard, FileText, Dumbbell } from 'lucide-react'
import { Card, Button, Badge, Spinner } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'
import { planApi } from '@/services/api'
import { useAuthStore } from '@/stores/authStore'

export default function PlansList() {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const { user, updateUser } = useAuthStore()
  const { showToast } = useToast()
  const queryClient = useQueryClient()

  const { data: plans, isLoading } = useQuery({
    queryKey: ['plans'],
    queryFn: planApi.getAll,
  })

  const purchaseMutation = useMutation({
    mutationFn: planApi.purchase,
    onSuccess: () => {
      showToast('success', t('plans.purchaseSuccess'))
      queryClient.invalidateQueries({ queryKey: ['plans'] })
      queryClient.invalidateQueries({ queryKey: ['myPlans'] })
      // Update user credits
      if (user) {
        const plan = plans?.find((p) => p.id === purchaseMutation.variables)
        if (plan) {
          updateUser({ ...user, credits: user.credits - plan.credits })
        }
      }
      navigate('/plans/my')
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      showToast('error', error.response?.data?.message || t('errors.somethingWrong'))
    },
  })

  const handlePurchase = (planId: string, credits: number) => {
    if ((user?.credits || 0) < credits) {
      showToast('error', t('plans.notEnoughCredits'))
      return
    }
    purchaseMutation.mutate(planId)
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {t('plans.title')}
        </h1>
        <Button variant="ghost" size="sm" onClick={() => navigate('/plans/my')}>
          {t('plans.myPlans')}
        </Button>
      </div>

      {/* Credit info */}
      <div className="flex items-center gap-2 p-3 bg-primary-50 dark:bg-primary-900/20 rounded-lg">
        <CreditCard size={18} className="text-primary-500" />
        <span className="text-sm text-primary-700 dark:text-primary-300">
          {t('home.yourCredits')}: <strong>{user?.credits || 0}</strong>
        </span>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12">
          <Spinner size="lg" />
        </div>
      ) : plans && plans.length > 0 ? (
        <div className="space-y-4">
          {plans.map((plan) => (
            <Card key={plan.id} variant="bordered">
              <div className="flex gap-4">
                <div className="flex-shrink-0 w-16 h-16 bg-primary-100 dark:bg-primary-900/30 rounded-lg flex items-center justify-center">
                  <Dumbbell size={28} className="text-primary-500" />
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <h3 className="font-semibold text-neutral-900 dark:text-white">
                        {i18n.language === 'cs' ? plan.nameCs : plan.nameEn || plan.nameCs}
                      </h3>
                      <p className="text-sm text-neutral-500 dark:text-neutral-400 mt-1 line-clamp-2">
                        {i18n.language === 'cs'
                          ? plan.descriptionCs
                          : plan.descriptionEn || plan.descriptionCs}
                      </p>
                    </div>
                    <Badge variant="primary">{plan.credits} kr.</Badge>
                  </div>

                  <div className="flex items-center gap-4 mt-3 text-sm text-neutral-500 dark:text-neutral-400">
                    {plan.hasFile && (
                      <div className="flex items-center gap-1">
                        <FileText size={14} />
                        <span>PDF</span>
                      </div>
                    )}
                  </div>

                  <div className="mt-4">
                    <Button
                      size="sm"
                      onClick={() => handlePurchase(plan.id, plan.credits)}
                      isLoading={purchaseMutation.isPending}
                      disabled={(user?.credits || 0) < plan.credits}
                    >
                      {t('plans.buy')}
                    </Button>
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      ) : (
        <Card variant="bordered">
          <div className="text-center py-12">
            <Dumbbell className="mx-auto mb-4 text-neutral-300 dark:text-neutral-600" size={48} />
            <p className="text-neutral-500 dark:text-neutral-400">{t('plans.noPlans')}</p>
          </div>
        </Card>
      )}
    </div>
  )
}
