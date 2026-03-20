import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { adminApi } from '@/services/api'

export default function Statistics() {
  const { t, i18n } = useTranslation()

  const { data: stats, isLoading, isError } = useQuery({
    queryKey: ['admin', 'statistics'],
    queryFn: () => adminApi.getStatistics(6),
  })

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600" />
      </div>
    )
  }

  if (isError) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <p className="text-red-500">{t('common.error', 'Nastala chyba')}</p>
      </div>
    )
  }

  const reservationValues = stats?.monthlyStats?.map(s => s.reservations) || []
  const maxReservations = reservationValues.length > 0 ? Math.max(...reservationValues) : 1

  const locale = i18n.language === 'en' ? 'en-US' : i18n.language === 'sk' ? 'sk-SK' : 'cs-CZ'

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
        {t('admin.statistics', 'Statistiky')}
      </h1>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-white dark:bg-gray-800 rounded-lg p-6 shadow-sm border border-gray-200 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400">{t('admin.totalClients', 'Celkem klientů')}</p>
          <p className="text-3xl font-bold text-indigo-600 dark:text-indigo-400">{stats?.totalClients ?? 0}</p>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg p-6 shadow-sm border border-gray-200 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400">{t('admin.totalReservations', 'Celkem rezervací (6 měsíců)')}</p>
          <p className="text-3xl font-bold text-green-600 dark:text-green-400">{stats?.totalReservations ?? 0}</p>
        </div>
      </div>

      {/* Monthly Chart (simple bar chart) */}
      <div className="bg-white dark:bg-gray-800 rounded-lg p-6 shadow-sm border border-gray-200 dark:border-gray-700">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
          {t('admin.monthlyReservations', 'Měsíční rezervace')}
        </h2>
        <div className="space-y-3">
          {stats?.monthlyStats?.map((stat) => {
            const month = new Date(stat.month + '-01')
            const monthName = month.toLocaleDateString(locale, { month: 'long', year: 'numeric' })
            const width = maxReservations > 0 ? (stat.reservations / maxReservations) * 100 : 0

            return (
              <div key={stat.month} className="flex items-center gap-3">
                <span className="text-sm text-gray-600 dark:text-gray-400 w-32 shrink-0">{monthName}</span>
                <div className="flex-1 bg-gray-100 dark:bg-gray-700 rounded-full h-6 overflow-hidden">
                  <div
                    className="bg-indigo-500 h-full rounded-full flex items-center justify-end px-2 text-xs text-white font-medium transition-all duration-500"
                    style={{ width: `${Math.max(width, stat.reservations > 0 ? 8 : 0)}%` }}
                  >
                    {stat.reservations > 0 ? stat.reservations : ''}
                  </div>
                </div>
                <span className="text-sm font-medium text-gray-900 dark:text-white w-8 text-right">
                  {stat.reservations}
                </span>
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}
