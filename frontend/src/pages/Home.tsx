import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { CalendarPlus, CreditCard, Calendar, Clock } from 'lucide-react'
import { Card, Badge, Button, Spinner } from '@/components/ui'
import { useAuthStore } from '@/stores/authStore'
import { reservationApi, creditApi } from '@/services/api'
import { formatDate, formatTime } from '@/utils/formatters'

export default function Home() {
  const { t } = useTranslation()
  const { user } = useAuthStore()

  const { data: upcomingReservations, isLoading: reservationsLoading } = useQuery({
    queryKey: ['reservations', 'upcoming'],
    queryFn: reservationApi.getUpcoming,
  })

  const { data: transactions, isLoading: transactionsLoading } = useQuery({
    queryKey: ['credits', 'history'],
    queryFn: () => creditApi.getHistory(5),
  })

  const nextReservation = upcomingReservations?.[0]

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Welcome */}
      <div>
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {t('home.welcome', { name: user?.firstName || user?.email?.split('@')[0] })}
        </h1>
      </div>

      {/* Credits card */}
      <Card variant="elevated" className="bg-gradient-to-br from-primary-500 to-primary-600 text-white">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm text-white/80">{t('home.yourCredits')}</p>
            <p className="text-4xl font-heading font-bold mt-1">{user?.credits || 0}</p>
          </div>
          <CreditCard size={48} className="text-white/30" />
        </div>
        <Link to="/credits">
          <Button
            variant="secondary"
            size="sm"
            className="mt-4 bg-white/20 hover:bg-white/30 text-white border-0"
          >
            {t('home.buyCredits')}
          </Button>
        </Link>
      </Card>

      {/* Next training */}
      <Card variant="bordered">
        <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white mb-4">
          {t('home.nextTraining')}
        </h2>

        {reservationsLoading ? (
          <div className="flex justify-center py-4">
            <Spinner />
          </div>
        ) : nextReservation ? (
          <div className="flex items-center gap-4">
            <div className="flex-shrink-0 w-14 h-14 bg-primary-100 dark:bg-primary-900/30 rounded-lg flex flex-col items-center justify-center">
              <span className="text-xs text-primary-600 dark:text-primary-400 font-medium">
                {new Date(nextReservation.date).toLocaleDateString('cs', { weekday: 'short' })}
              </span>
              <span className="text-lg font-bold text-primary-700 dark:text-primary-300">
                {new Date(nextReservation.date).getDate()}
              </span>
            </div>
            <div className="flex-1">
              <p className="font-medium text-neutral-900 dark:text-white">
                {formatDate(nextReservation.date)}
              </p>
              <div className="flex items-center gap-1 text-sm text-neutral-500 dark:text-neutral-400">
                <Clock size={14} />
                <span>
                  {formatTime(nextReservation.startTime)} - {formatTime(nextReservation.endTime)}
                </span>
              </div>
            </div>
            <Badge variant="success">{nextReservation.status}</Badge>
          </div>
        ) : (
          <div className="text-center py-4">
            <Calendar className="mx-auto mb-2 text-neutral-300 dark:text-neutral-600" size={40} />
            <p className="text-neutral-500 dark:text-neutral-400">{t('home.noUpcoming')}</p>
          </div>
        )}
      </Card>

      {/* Quick actions */}
      <div>
        <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white mb-4">
          {t('home.quickActions')}
        </h2>
        <div className="grid grid-cols-2 gap-4">
          <Link to="/reservations/new">
            <Card
              variant="bordered"
              className="h-full hover:border-primary-300 dark:hover:border-primary-700 transition-colors cursor-pointer"
            >
              <CalendarPlus className="text-primary-500 mb-2" size={28} />
              <p className="font-medium text-neutral-900 dark:text-white">
                {t('home.bookTraining')}
              </p>
            </Card>
          </Link>
          <Link to="/credits">
            <Card
              variant="bordered"
              className="h-full hover:border-primary-300 dark:hover:border-primary-700 transition-colors cursor-pointer"
            >
              <CreditCard className="text-primary-500 mb-2" size={28} />
              <p className="font-medium text-neutral-900 dark:text-white">
                {t('home.buyCredits')}
              </p>
            </Card>
          </Link>
        </div>
      </div>

      {/* Recent activity */}
      <div>
        <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white mb-4">
          {t('home.recentActivity')}
        </h2>
        <Card variant="bordered" padding="none">
          {transactionsLoading ? (
            <div className="flex justify-center py-8">
              <Spinner />
            </div>
          ) : transactions && transactions.length > 0 ? (
            <div className="divide-y divide-neutral-100 dark:divide-dark-border">
              {transactions.map((transaction) => (
                <div key={transaction.id} className="flex items-center justify-between p-4">
                  <div>
                    <p className="font-medium text-neutral-900 dark:text-white">
                      {t(`credits.${transaction.type}`)}
                    </p>
                    <p className="text-sm text-neutral-500 dark:text-neutral-400">
                      {(() => {
                        const d = new Date(transaction.createdAt)
                        return `${d.getDate()}. ${d.getMonth() + 1}. ${d.getFullYear()} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
                      })()}
                    </p>
                  </div>
                  <span
                    className={
                      transaction.amount > 0
                        ? 'text-green-600 dark:text-green-400 font-semibold'
                        : 'text-red-600 dark:text-red-400 font-semibold'
                    }
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
