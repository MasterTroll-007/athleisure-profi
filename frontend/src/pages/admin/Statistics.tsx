import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Download } from 'lucide-react'
import { adminApi } from '@/services/api'
import { Button } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'

export default function Statistics() {
  const { t, i18n } = useTranslation()
  const { showToast } = useToast()

  const { data: stats, isLoading, isError } = useQuery({
    queryKey: ['admin', 'statistics'],
    queryFn: () => adminApi.getStatistics(6),
  })

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500" />
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

  const handleReservationsExport = async () => {
    try {
      const blob = await adminApi.exportCsv('reservations')
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'reservations.csv'
      link.click()
      URL.revokeObjectURL(url)
    } catch {
      showToast('error', t('errors.somethingWrong'))
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl font-bold text-neutral-900 dark:text-white">
          {t('admin.statistics', 'Statistiky')}
        </h1>
        <Button variant="secondary" size="sm" leftIcon={<Download size={16} />} onClick={handleReservationsExport}>
          {t('admin.exportReservationsCsv', 'Export rezervací CSV')}
        </Button>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white dark:bg-dark-surface rounded-lg p-6 shadow-sm border border-neutral-200 dark:border-neutral-700">
          <p className="text-sm text-neutral-500 dark:text-neutral-400">{t('admin.totalClients', 'Celkem klientů')}</p>
          <p className="text-3xl font-bold text-primary-600 dark:text-primary-400">{stats?.totalClients ?? 0}</p>
        </div>
        <div className="bg-white dark:bg-dark-surface rounded-lg p-6 shadow-sm border border-neutral-200 dark:border-neutral-700">
          <p className="text-sm text-neutral-500 dark:text-neutral-400">{t('admin.totalReservations', 'Celkem rezervací (6 měsíců)')}</p>
          <p className="text-3xl font-bold text-green-600 dark:text-green-400">{stats?.totalReservations ?? 0}</p>
        </div>
        <div className="bg-white dark:bg-dark-surface rounded-lg p-6 shadow-sm border border-neutral-200 dark:border-neutral-700">
          <p className="text-sm text-neutral-500 dark:text-neutral-400">{t('admin.attendanceRate')}</p>
          <p className="text-3xl font-bold text-blue-600 dark:text-blue-400">{stats?.attendanceRate ?? 0}%</p>
        </div>
        <div className="bg-white dark:bg-dark-surface rounded-lg p-6 shadow-sm border border-neutral-200 dark:border-neutral-700">
          <p className="text-sm text-neutral-500 dark:text-neutral-400">{t('reservationStatus.no_show')}</p>
          <p className="text-3xl font-bold text-red-600 dark:text-red-400">{stats?.noShowRate ?? 0}%</p>
        </div>
        <div className="bg-white dark:bg-dark-surface rounded-lg p-6 shadow-sm border border-neutral-200 dark:border-neutral-700">
          <p className="text-sm text-neutral-500 dark:text-neutral-400">{t('feedback.totalRatings')}</p>
          <p className="text-3xl font-bold text-amber-600 dark:text-amber-400">{stats?.averageRating?.toFixed?.(1) ?? '0.0'}</p>
          <p className="text-xs text-neutral-500 dark:text-neutral-400">{stats?.totalFeedback ?? 0}x</p>
        </div>
        <div className="bg-white dark:bg-dark-surface rounded-lg p-6 shadow-sm border border-neutral-200 dark:border-neutral-700">
          <p className="text-sm text-neutral-500 dark:text-neutral-400">{t('admin.creditsSold', 'Prodané kredity')}</p>
          <p className="text-3xl font-bold text-violet-600 dark:text-violet-400">{stats?.creditsSold ?? 0}</p>
        </div>
      </div>

      {/* Monthly Chart (simple bar chart) */}
      <div className="bg-white dark:bg-dark-surface rounded-lg p-6 shadow-sm border border-neutral-200 dark:border-neutral-700">
        <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-4">
          {t('admin.monthlyReservations', 'Měsíční rezervace')}
        </h2>
        <div className="space-y-3">
          {stats?.monthlyStats?.map((stat) => {
            // API returns "2025-10-01" (full date) or "2025-10" (month only)
            const monthStr = stat.month.length <= 7 ? stat.month + '-01' : stat.month
            const month = new Date(monthStr + 'T00:00:00')
            const monthName = month.toLocaleDateString(locale, { month: 'long', year: 'numeric' })
            const width = maxReservations > 0 ? (stat.reservations / maxReservations) * 100 : 0
            const hasReservations = stat.reservations > 0

            return (
              <div key={stat.month} className="flex items-center gap-3">
                <span className="text-sm text-neutral-600 dark:text-neutral-400 w-32 shrink-0">{monthName}</span>
                <div className="flex-1 bg-neutral-100 dark:bg-neutral-700 rounded-full h-6 overflow-hidden">
                  {hasReservations && (
                    <div
                      className="bg-primary-500 h-full rounded-full flex items-center justify-end px-2 text-xs text-white font-medium transition-all duration-500"
                      style={{ width: `${Math.max(width, 8)}%` }}
                    >
                      {stat.reservations}
                    </div>
                  )}
                </div>
                <span className="text-sm font-medium text-neutral-900 dark:text-white w-8 text-right">
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
