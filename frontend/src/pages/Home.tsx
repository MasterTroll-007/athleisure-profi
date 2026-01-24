import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { CalendarPlus, CreditCard, Calendar, Clock, Users } from 'lucide-react'
import { Card, Badge, Button, Spinner } from '@/components/ui'
import { useAuthStore } from '@/stores/authStore'
import { reservationApi, creditApi, adminApi } from '@/services/api'
import { formatDate, formatTime } from '@/utils/formatters'
import type { Slot } from '@/types/api'

// Helper to get slot status color
const getSlotColor = (status: string) => {
  switch (status) {
    case 'reserved':
      return 'bg-blue-100 dark:bg-blue-900/30 border-blue-300 dark:border-blue-700'
    case 'cancelled':
      return 'bg-red-100 dark:bg-red-900/30 border-red-300 dark:border-red-700'
    case 'unlocked':
      return 'bg-green-100 dark:bg-green-900/30 border-green-300 dark:border-green-700'
    default:
      return 'bg-neutral-100 dark:bg-neutral-800 border-neutral-300 dark:border-neutral-600'
  }
}

const getSlotTextColor = (status: string) => {
  switch (status) {
    case 'reserved':
      return 'text-blue-700 dark:text-blue-300'
    case 'cancelled':
      return 'text-red-700 dark:text-red-300'
    case 'unlocked':
      return 'text-green-700 dark:text-green-300'
    default:
      return 'text-neutral-600 dark:text-neutral-400'
  }
}

// Admin Home Component
function AdminHome() {
  const { t, i18n } = useTranslation()
  const { user } = useAuthStore()

  // Get today and tomorrow dates
  const today = new Date()
  const tomorrow = new Date(today)
  tomorrow.setDate(tomorrow.getDate() + 1)

  // Format date using local timezone to avoid UTC date shifting
  const formatDateStr = (date: Date) => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  const { data: slots, isLoading } = useQuery({
    queryKey: ['admin', 'slots', 'home', formatDateStr(today), formatDateStr(tomorrow)],
    queryFn: () => adminApi.getSlots(formatDateStr(today), formatDateStr(tomorrow)),
  })

  // Group slots by date
  const slotsByDate = (slots || []).reduce((acc, slot) => {
    if (!acc[slot.date]) {
      acc[slot.date] = []
    }
    acc[slot.date].push(slot)
    return acc
  }, {} as Record<string, Slot[]>)

  // Get dates for display
  const dates = [formatDateStr(today), formatDateStr(tomorrow)]

  const formatDayHeader = (dateStr: string) => {
    const date = new Date(dateStr + 'T00:00:00')
    const dayName = date.toLocaleDateString(i18n.language, { weekday: 'long' })
    const dayNum = date.getDate()
    const month = date.getMonth() + 1
    return { dayName, dayNum, month }
  }

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Welcome */}
      <div>
        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
          {t('home.welcome', { name: user?.firstName || user?.email?.split('@')[0] })}
        </h1>
      </div>

      {/* Mini Calendar - 2 Days */}
      <div>
        <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white mb-4">
          {t('home.upcomingTrainings')}
        </h2>

        {isLoading ? (
          <div className="flex justify-center py-8">
            <Spinner size="lg" />
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-4">
            {dates.map((dateStr) => {
              const { dayName, dayNum, month } = formatDayHeader(dateStr)
              const daySlots = slotsByDate[dateStr] || []
              const sortedSlots = [...daySlots].sort((a, b) => a.startTime.localeCompare(b.startTime))

              // Filter to only show reserved or cancelled slots (not available/locked)
              const relevantSlots = sortedSlots.filter(slot =>
                slot.status === 'reserved' || slot.status === 'cancelled'
              )

              return (
                <Card key={dateStr} variant="bordered" padding="none" className="overflow-hidden">
                  {/* Day header */}
                  <div className="bg-primary-500 text-white p-3 text-center">
                    <p className="text-sm font-medium capitalize">{dayName}</p>
                    <p className="text-2xl font-bold">{dayNum}. {month}.</p>
                  </div>

                  {/* Slots - only reserved or cancelled */}
                  <div className="p-2 space-y-2 max-h-80 overflow-y-auto">
                    {relevantSlots.length === 0 ? (
                      <p className="text-center py-4 text-neutral-500 dark:text-neutral-400 text-sm">
                        {t('home.noSlots')}
                      </p>
                    ) : (
                      relevantSlots.map((slot) => (
                        <div
                          key={slot.id}
                          className={`p-2 rounded-lg border ${getSlotColor(slot.status)}`}
                        >
                          <div className="flex items-center justify-between">
                            <span className="font-mono text-sm font-medium text-neutral-900 dark:text-white">
                              {formatTime(slot.startTime)}
                            </span>
                            {slot.status === 'cancelled' && (
                              <span className="text-xs px-1.5 py-0.5 bg-red-500 text-white rounded">
                                {t('calendar.cancelled')}
                              </span>
                            )}
                          </div>
                          <div className={`text-sm mt-1 leading-tight ${getSlotTextColor(slot.status)}`}>
                            {slot.assignedUserName ? (
                              slot.assignedUserName.split('\n').map((line, idx) => (
                                <p key={idx} className="truncate">{line}</p>
                              ))
                            ) : (
                              <p className="truncate">{slot.assignedUserEmail || t('calendar.reserved')}</p>
                            )}
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </Card>
              )
            })}
          </div>
        )}
      </div>

      {/* Legend */}
      <div className="flex flex-wrap items-center gap-4 text-sm">
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded bg-blue-200 border border-blue-500"></div>
          <span className="text-neutral-600 dark:text-neutral-400">{t('calendar.reserved')}</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded bg-red-200 border border-red-500"></div>
          <span className="text-neutral-600 dark:text-neutral-400">{t('calendar.cancelled')}</span>
        </div>
      </div>

      {/* Quick actions */}
      <div>
        <h2 className="text-lg font-heading font-semibold text-neutral-900 dark:text-white mb-4">
          {t('home.quickActions')}
        </h2>
        <div className="grid grid-cols-2 gap-4">
          <Link to="/calendar">
            <Card
              variant="bordered"
              className="h-full hover:border-primary-300 dark:hover:border-primary-700 transition-colors cursor-pointer"
            >
              <Calendar className="text-primary-500 mb-2" size={28} />
              <p className="font-medium text-neutral-900 dark:text-white">
                {t('admin.calendar')}
              </p>
            </Card>
          </Link>
          <Link to="/admin/clients">
            <Card
              variant="bordered"
              className="h-full hover:border-primary-300 dark:hover:border-primary-700 transition-colors cursor-pointer"
            >
              <Users className="text-primary-500 mb-2" size={28} />
              <p className="font-medium text-neutral-900 dark:text-white">
                {t('admin.clients')}
              </p>
            </Card>
          </Link>
        </div>
      </div>
    </div>
  )
}

// Client Home Component
function ClientHome() {
  const { t, i18n } = useTranslation()
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
                {new Date(nextReservation.date).toLocaleDateString(i18n.language, { weekday: 'short' })}
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
          <Link to="/calendar">
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

// Main Home Component - renders based on user role
export default function Home() {
  const { user } = useAuthStore()
  const isAdmin = user?.role === 'admin'

  return isAdmin ? <AdminHome /> : <ClientHome />
}
