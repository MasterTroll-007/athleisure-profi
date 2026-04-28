import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Calendar, Dumbbell, Plus, Download, Clock } from 'lucide-react'
import { Card, Button, Badge, Spinner } from '@/components/ui'
import { planApi } from '@/services/api'
import { formatShortDate } from '@/utils/formatters'

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

  const formatSessions = (sessionsRemaining: number | null) => {
    if (sessionsRemaining === null || sessionsRemaining === undefined) {
      return i18n.language === 'en' ? 'Unlimited sessions' : 'Neomezeně lekcí'
    }
    if (i18n.language === 'en') {
      return sessionsRemaining === 1 ? '1 session left' : `${sessionsRemaining} sessions left`
    }
    if (sessionsRemaining === 1) return 'Zbývá 1 lekce'
    if (sessionsRemaining >= 2 && sessionsRemaining <= 4) return `Zbývají ${sessionsRemaining} lekce`
    return `Zbývá ${sessionsRemaining} lekcí`
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
                        {purchased.planName || t('plans.title')}
                      </h3>
                      <Badge variant="success">{t('plans.purchased')}</Badge>
                    </div>
                  </div>
                </div>

                {/* Details */}
                <div className="flex flex-col gap-3 pt-2 border-t border-neutral-100 dark:border-dark-border sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex min-w-0 flex-wrap items-center gap-x-4 gap-y-2 text-sm text-neutral-500 dark:text-neutral-400">
                    <div className="flex items-center gap-1">
                      <Calendar size={14} />
                      <span>
                        {formatShortDate(purchased.purchaseDate, i18n.language)}
                      </span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Clock size={14} />
                      <span>
                        {formatShortDate(purchased.expiryDate, i18n.language)}
                      </span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Dumbbell size={14} />
                      <span>
                        {formatSessions(purchased.sessionsRemaining)}
                      </span>
                    </div>
                  </div>
                  <Button
                    size="sm"
                    variant="secondary"
                    className="w-full sm:w-auto"
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
