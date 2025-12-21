import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Calendar, Dumbbell, Plus, Download, CreditCard } from 'lucide-react'
import { Card, Button, Badge, Spinner } from '@/components/ui'
import { planApi } from '@/services/api'

export default function MyPlans() {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()

  const { data: purchasedPlans, isLoading } = useQuery({
    queryKey: ['myPlans'],
    queryFn: planApi.getMyPlans,
  })

  const handleDownload = (planId: string) => {
    window.open(planApi.getDownloadUrl(planId), '_blank')
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {t('plans.myPlans')}
        </h1>
        <Button variant="secondary" size="sm" leftIcon={<Plus size={16} />} onClick={() => navigate('/plans')}>
          {t('plans.browse')}
        </Button>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12">
          <Spinner size="lg" />
        </div>
      ) : purchasedPlans && purchasedPlans.length > 0 ? (
        <div className="space-y-4">
          {purchasedPlans.map((purchased) => (
            <Card key={purchased.id} variant="bordered">
              <div className="space-y-4">
                <div className="flex items-start gap-4">
                  <div className="flex-shrink-0 w-14 h-14 bg-primary-100 dark:bg-primary-900/30 rounded-lg flex items-center justify-center">
                    <Dumbbell size={24} className="text-primary-500" />
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2">
                      <h3 className="font-semibold text-neutral-900 dark:text-white">
                        {purchased.planName}
                      </h3>
                      <Badge variant="success">{t('plans.purchased')}</Badge>
                    </div>
                  </div>
                </div>

                {/* Details */}
                <div className="flex items-center justify-between pt-2 border-t border-neutral-100 dark:border-dark-border">
                  <div className="flex items-center gap-4 text-sm text-neutral-500 dark:text-neutral-400">
                    <div className="flex items-center gap-1">
                      <Calendar size={14} />
                      <span>
                        {new Date(purchased.purchasedAt).toLocaleDateString(i18n.language)}
                      </span>
                    </div>
                    <div className="flex items-center gap-1">
                      <CreditCard size={14} />
                      <span>
                        {purchased.creditsUsed} kr.
                      </span>
                    </div>
                  </div>
                  <Button
                    size="sm"
                    variant="secondary"
                    leftIcon={<Download size={14} />}
                    onClick={() => handleDownload(purchased.planId)}
                  >
                    {t('plans.download')}
                  </Button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      ) : (
        <Card variant="bordered">
          <div className="text-center py-12">
            <Dumbbell className="mx-auto mb-4 text-neutral-300 dark:text-neutral-600" size={48} />
            <p className="text-neutral-500 dark:text-neutral-400 mb-4">
              {t('plans.noMyPlans')}
            </p>
            <Button onClick={() => navigate('/plans')}>{t('plans.browsePlans')}</Button>
          </div>
        </Card>
      )}
    </div>
  )
}
