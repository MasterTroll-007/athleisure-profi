import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Calendar, TrendingUp, Clock } from 'lucide-react'
import { Card, Badge, Spinner } from '@/components/ui'
import { adminApi } from '@/services/api'
import { formatTime } from '@/utils/formatters'

// Extract time from ISO datetime string (e.g., "2024-01-15T09:00:00" -> "09:00")
function extractTime(isoDateTime: string): string {
  if (!isoDateTime) return '--:--'
  const timePart = isoDateTime.split('T')[1]
  if (!timePart) return '--:--'
  return timePart.substring(0, 5)
}

export default function Dashboard() {
  const { t } = useTranslation()

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['admin', 'stats'],
    queryFn: adminApi.getStats,
  })

  const { data: todayReservations, isLoading: reservationsLoading } = useQuery({
    queryKey: ['admin', 'today'],
    queryFn: adminApi.getTodayReservations,
  })

  const statCards = [
    {
      label: t('admin.todayTrainings'),
      value: stats?.todayReservations || 0,
      icon: Calendar,
      color: 'text-blue-500',
      bg: 'bg-blue-100 dark:bg-blue-900/30',
    },
    {
      label: t('admin.weeklyStats'),
      value: stats?.weekReservations || 0,
      icon: TrendingUp,
      color: 'text-primary-500',
      bg: 'bg-primary-100 dark:bg-primary-900/30',
    },
  ]

  return (
    <div className="space-y-6 animate-fade-in">
      <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
        {t('admin.dashboard')}
      </h1>

      {/* Stats grid */}
      {statsLoading ? (
        <div className="flex justify-center py-8">
          <Spinner size="lg" />
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {statCards.map((stat, index) => (
            <Card key={index} variant="bordered">
              <div className="flex items-center gap-4">
                <div className={`w-12 h-12 rounded-lg ${stat.bg} flex items-center justify-center`}>
                  <stat.icon size={24} className={stat.color} />
                </div>
                <div>
                  <p className="text-2xl font-bold text-neutral-900 dark:text-white">
                    {stat.value}
                  </p>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400">{stat.label}</p>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Today's trainings */}
      <Card variant="bordered">
        <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white mb-4">
          {t('admin.todayTrainings')}
        </h2>

        {reservationsLoading ? (
          <div className="flex justify-center py-8">
            <Spinner />
          </div>
        ) : todayReservations && todayReservations.length > 0 ? (
          <div className="space-y-3">
            {todayReservations.map((reservation) => (
              <div
                key={reservation.id}
                className="flex items-center justify-between p-3 bg-neutral-50 dark:bg-dark-surface rounded-lg"
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
                    <Clock size={18} className="text-primary-500" />
                  </div>
                  <div>
                    <p className="font-medium text-neutral-900 dark:text-white">
                      {reservation.clientName || reservation.clientEmail || t('admin.unknownClient')}
                    </p>
                    <p className="text-sm text-neutral-500 dark:text-neutral-400">
                      {extractTime(reservation.start)} - {extractTime(reservation.end)}
                    </p>
                  </div>
                </div>
                <Badge
                  variant={
                    reservation.status === 'confirmed'
                      ? 'success'
                      : reservation.status === 'cancelled'
                        ? 'danger'
                        : 'default'
                  }
                >
                  {reservation.status}
                </Badge>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-center py-8 text-neutral-500 dark:text-neutral-400">
            {t('admin.noTrainingsToday')}
          </p>
        )}
      </Card>

      {/* Today's scheduled list from stats */}
      {stats?.todayList && stats.todayList.length > 0 && (
        <Card variant="bordered">
          <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white mb-4">
            {t('admin.upcomingTrainings')}
          </h2>
          <div className="space-y-2">
            {stats.todayList.map((res) => (
              <div key={res.id} className="flex justify-between items-center py-2 border-b border-neutral-100 dark:border-dark-border last:border-0">
                <span className="text-neutral-700 dark:text-neutral-300">
                  {res.userName || res.userEmail}
                </span>
                <span className="text-sm text-neutral-500 dark:text-neutral-400">
                  {formatTime(res.startTime)}
                </span>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  )
}
