import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { CalendarDays, Download, FileText, RefreshCw, ShieldCheck } from 'lucide-react'
import { adminApi } from '@/services/api'
import { Button, Card, Select } from '@/components/ui'
import { useToast } from '@/components/ui/Toast'

function previousMonthDate() {
  const now = new Date()
  return new Date(now.getFullYear(), now.getMonth() - 1, 1)
}

function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

export default function AccountingExports() {
  const { t, i18n } = useTranslation()
  const { showToast } = useToast()
  const defaultDate = useMemo(() => previousMonthDate(), [])
  const [year, setYear] = useState(String(defaultDate.getFullYear()))
  const [month, setMonth] = useState(String(defaultDate.getMonth() + 1))
  const [syncStripe, setSyncStripe] = useState(true)
  const [isDownloading, setIsDownloading] = useState(false)

  const locale = i18n.language === 'en' ? 'en-US' : i18n.language === 'sk' ? 'sk-SK' : 'cs-CZ'
  const currentYear = new Date().getFullYear()
  const years = Array.from({ length: 6 }, (_, index) => currentYear - index)
  const months = Array.from({ length: 12 }, (_, index) => {
    const monthIndex = index + 1
    return {
      value: String(monthIndex),
      label: new Date(2024, index, 1).toLocaleDateString(locale, { month: 'long' }),
    }
  })

  const selectedFilename = `accounting-${year}-${month.padStart(2, '0')}.zip`

  const handleDownload = async () => {
    setIsDownloading(true)
    try {
      const blob = await adminApi.exportAccountingMonthly({
        year: Number(year),
        month: Number(month),
        syncStripe,
      })
      saveBlob(blob, selectedFilename)
      showToast('success', t('admin.accountingExport.downloadStarted'))
    } catch {
      showToast('error', t('admin.accountingExport.downloadFailed'))
    } finally {
      setIsDownloading(false)
    }
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p className="text-sm font-medium uppercase tracking-[0.18em] text-primary-200/75">
            {t('admin.accountingExport.eyebrow')}
          </p>
          <h1 className="mt-2 text-2xl font-heading font-bold text-white">
            {t('admin.accountingExportsTitle')}
          </h1>
          <p className="mt-2 max-w-2xl text-sm leading-relaxed text-white/58">
            {t('admin.accountingExport.subtitle')}
          </p>
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-[minmax(0,1.15fr)_minmax(320px,0.85fr)]">
        <Card variant="bordered" padding="lg" className="space-y-5">
          <div className="flex items-center gap-3">
            <span className="flex h-11 w-11 items-center justify-center rounded-lg bg-primary-300/12 text-primary-100">
              <CalendarDays size={22} />
            </span>
            <div>
              <h2 className="text-lg font-semibold text-white">
                {t('admin.accountingExport.monthlyExport')}
              </h2>
              <p className="text-sm text-white/52">
                {t('admin.accountingExport.monthlyExportDesc')}
              </p>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Select
              label={t('admin.accountingExport.month')}
              value={month}
              onChange={(event) => setMonth(event.target.value)}
            >
              {months.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </Select>
            <Select
              label={t('admin.accountingExport.year')}
              value={year}
              onChange={(event) => setYear(event.target.value)}
            >
              {years.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </Select>
          </div>

          <div className="rounded-xl border border-white/[0.07] bg-white/[0.04] p-4">
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <div className="flex items-center gap-2 text-sm font-medium text-white">
                  <RefreshCw size={16} className="text-primary-100" />
                  {t('admin.accountingExport.syncStripe')}
                </div>
                <p className="mt-1.5 text-xs leading-relaxed text-white/52">
                  {t('admin.accountingExport.syncStripeDesc')}
                </p>
              </div>
              <label className="relative mt-0.5 inline-flex cursor-pointer items-center">
                <input
                  type="checkbox"
                  checked={syncStripe}
                  onChange={(event) => setSyncStripe(event.target.checked)}
                  className="peer sr-only"
                />
                <div className="h-6 w-11 rounded-full bg-white/15 after:absolute after:left-[2px] after:top-[2px] after:h-5 after:w-5 after:rounded-full after:border after:border-neutral-300 after:bg-white after:transition-all after:content-[''] peer-checked:bg-primary-500 peer-checked:after:translate-x-full peer-checked:after:border-white peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300/40" />
              </label>
            </div>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <p className="text-sm text-white/50">
              {t('admin.accountingExport.filename')}: <span className="font-medium text-white/78">{selectedFilename}</span>
            </p>
            <Button
              type="button"
              leftIcon={<Download size={17} />}
              onClick={handleDownload}
              isLoading={isDownloading}
              className="w-full sm:w-auto"
            >
              {t('admin.accountingExport.downloadZip')}
            </Button>
          </div>
        </Card>

        <div className="space-y-4">
          <Card variant="bordered" padding="lg">
            <div className="flex items-start gap-3">
              <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-emerald-300/12 text-emerald-100">
                <ShieldCheck size={20} />
              </span>
              <div>
                <h2 className="font-semibold text-white">{t('admin.accountingExport.automationTitle')}</h2>
                <p className="mt-1.5 text-sm leading-relaxed text-white/55">
                  {t('admin.accountingExport.automationDesc')}
                </p>
              </div>
            </div>
          </Card>

          <Card variant="bordered" padding="lg">
            <div className="flex items-start gap-3">
              <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-white/[0.07] text-white/80">
                <FileText size={20} />
              </span>
              <div className="min-w-0">
                <h2 className="font-semibold text-white">{t('admin.accountingExport.zipContains')}</h2>
                <ul className="mt-3 space-y-2 text-sm text-white/58">
                  {['summary', 'payments', 'balanceTransactions', 'payouts', 'creditMovements'].map((key) => (
                    <li key={key} className="flex items-start gap-2">
                      <span className="mt-2 h-1.5 w-1.5 flex-shrink-0 rounded-full bg-primary-200" />
                      <span>{t(`admin.accountingExport.files.${key}`)}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  )
}
