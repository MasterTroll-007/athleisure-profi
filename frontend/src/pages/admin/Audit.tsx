import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { ClipboardList, Filter } from 'lucide-react'
import { adminApi } from '@/services/api'
import { Badge, Card, Pagination, Spinner } from '@/components/ui'
import type { AuditLog } from '@/types/api'

const actionOptions = ['', 'RESERVATION_CREATED', 'RESERVATION_CANCELLED'] as const

function actionVariant(action: string): 'success' | 'danger' | 'info' {
  switch (action) {
    case 'RESERVATION_CREATED':
      return 'success'
    case 'RESERVATION_CANCELLED':
      return 'danger'
    default:
      return 'info'
  }
}

export default function Audit() {
  const { t, i18n } = useTranslation()
  const [page, setPage] = useState(0)
  const [action, setAction] = useState('')

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ['admin', 'audit', page, action],
    queryFn: () => adminApi.getAuditLogs(page, 20, { action: action || undefined }),
  })

  const items = useMemo(() => data?.content ?? [], [data])

  const formatDateTime = (value: string) =>
    new Date(value).toLocaleString(i18n.language, {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })

  const formatSlot = (item: AuditLog) => {
    if (!item.date) return '-'
    const date = new Date(`${item.date}T00:00:00`).toLocaleDateString(i18n.language)
    const time = item.startTime && item.endTime ? `${item.startTime.slice(0, 5)}-${item.endTime.slice(0, 5)}` : ''
    return `${date}${time ? ` ${time}` : ''}`
  }

  const actorLabel = (item: AuditLog) => item.actorName || item.actorEmail || t('admin.audit.system')
  const clientLabel = (item: AuditLog) => item.clientName || item.clientEmail || t('admin.unknownClient')

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white">
            {t('admin.auditTitle')}
          </h1>
          <p className="mt-1 text-sm text-neutral-500 dark:text-neutral-400">
            {t('admin.audit.subtitle')}
          </p>
        </div>

        <label className="flex items-center gap-2 text-sm text-neutral-600 dark:text-neutral-300">
          <Filter size={16} className="shrink-0" />
          <select
            value={action}
            onChange={(event) => {
              setAction(event.target.value)
              setPage(0)
            }}
            className="min-h-[40px] rounded-lg border border-neutral-300 bg-white px-3 py-2 text-neutral-900 dark:border-dark-border dark:bg-dark-surface dark:text-white"
          >
            {actionOptions.map((option) => (
              <option key={option || 'all'} value={option}>
                {option ? t(`admin.audit.actions.${option}`) : t('admin.audit.allActions')}
              </option>
            ))}
          </select>
        </label>
      </div>

      <Card variant="bordered" padding="none" className="overflow-hidden">
        {isLoading ? (
          <div className="flex justify-center py-12">
            <Spinner size="lg" />
          </div>
        ) : items.length === 0 ? (
          <div className="flex flex-col items-center justify-center px-4 py-12 text-center">
            <ClipboardList className="mb-3 text-neutral-300 dark:text-neutral-600" size={44} />
            <p className="text-neutral-500 dark:text-neutral-400">{t('admin.audit.empty')}</p>
          </div>
        ) : (
          <>
            <div className="hidden divide-y divide-neutral-100 dark:divide-dark-border md:block">
              <div className="grid grid-cols-[10rem_11rem_minmax(0,1fr)_minmax(0,1fr)_11rem_6rem] gap-4 px-4 py-3 text-xs font-semibold uppercase tracking-normal text-neutral-500 dark:text-neutral-400">
                <span>{t('admin.audit.when')}</span>
                <span>{t('admin.audit.action')}</span>
                <span>{t('admin.audit.actor')}</span>
                <span>{t('admin.audit.client')}</span>
                <span>{t('admin.audit.slot')}</span>
                <span className="text-right">{t('admin.audit.credits')}</span>
              </div>
              {items.map((item) => (
                <div key={item.id} className="grid grid-cols-[10rem_11rem_minmax(0,1fr)_minmax(0,1fr)_11rem_6rem] gap-4 px-4 py-3 text-sm">
                  <span className="text-neutral-600 dark:text-neutral-300">{formatDateTime(item.createdAt)}</span>
                  <Badge variant={actionVariant(item.action)} size="sm" className="w-fit whitespace-nowrap">
                    {t(`admin.audit.actions.${item.action}`)}
                  </Badge>
                  <span className="min-w-0 truncate text-neutral-900 dark:text-white">{actorLabel(item)}</span>
                  <span className="min-w-0 truncate text-neutral-900 dark:text-white">{clientLabel(item)}</span>
                  <span className="text-neutral-600 dark:text-neutral-300">{formatSlot(item)}</span>
                  <span className="text-right font-medium text-neutral-900 dark:text-white">
                    {item.creditsChange === null ? '-' : item.creditsChange > 0 ? `+${item.creditsChange}` : item.creditsChange}
                  </span>
                </div>
              ))}
            </div>

            <div className="divide-y divide-neutral-100 dark:divide-dark-border md:hidden">
              {items.map((item) => (
                <div key={item.id} className="space-y-3 p-4">
                  <div className="flex items-start justify-between gap-3">
                    <Badge variant={actionVariant(item.action)} size="sm" className="whitespace-nowrap">
                      {t(`admin.audit.actions.${item.action}`)}
                    </Badge>
                    <span className="text-right text-xs text-neutral-500 dark:text-neutral-400">
                      {formatDateTime(item.createdAt)}
                    </span>
                  </div>
                  <div className="grid grid-cols-[5rem_minmax(0,1fr)] gap-x-3 gap-y-2 text-sm">
                    <span className="text-neutral-500 dark:text-neutral-400">{t('admin.audit.actor')}</span>
                    <span className="min-w-0 truncate text-neutral-900 dark:text-white">{actorLabel(item)}</span>
                    <span className="text-neutral-500 dark:text-neutral-400">{t('admin.audit.client')}</span>
                    <span className="min-w-0 truncate text-neutral-900 dark:text-white">{clientLabel(item)}</span>
                    <span className="text-neutral-500 dark:text-neutral-400">{t('admin.audit.slot')}</span>
                    <span className="text-neutral-900 dark:text-white">{formatSlot(item)}</span>
                  </div>
                </div>
              ))}
            </div>

            {data && (
              <div className="px-4 pb-4">
                <Pagination
                  page={data.page}
                  totalPages={data.totalPages}
                  totalElements={data.totalElements}
                  size={data.size}
                  onPageChange={setPage}
                  isLoading={isFetching}
                />
              </div>
            )}
          </>
        )}
      </Card>
    </div>
  )
}
