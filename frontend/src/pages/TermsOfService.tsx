import { Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Card } from '@/components/ui'
import LanguageSwitch from '@/components/layout/LanguageSwitch'

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="inline font-medium">{label}: </dt>
      <dd className="inline">{value}</dd>
    </div>
  )
}

function TranslatedList({ i18nKey }: { i18nKey: string }) {
  const { t } = useTranslation()
  const value = t(i18nKey, { returnObjects: true }) as unknown
  const items = Array.isArray(value) ? value.map(String) : []

  return (
    <ul className="list-disc space-y-1 pl-5">
      {items.map((item) => (
        <li key={item}>{item}</li>
      ))}
    </ul>
  )
}

function ProviderDetails() {
  const { t } = useTranslation()

  return (
    <dl className="grid gap-2 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-950 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-100">
      <div className="font-semibold">{t('legal.terms.details.notice')}</div>
      <DetailRow label={t('legal.terms.details.nameLabel')} value={t('legal.terms.details.nameValue')} />
      <DetailRow
        label={t('legal.terms.details.businessIdLabel')}
        value={t('legal.terms.details.businessIdValue')}
      />
      <DetailRow label={t('legal.terms.details.taxIdLabel')} value={t('legal.terms.details.taxIdValue')} />
      <DetailRow label={t('legal.terms.details.addressLabel')} value={t('legal.terms.details.addressValue')} />
      <DetailRow label={t('legal.terms.details.emailLabel')} value={t('legal.terms.details.emailValue')} />
      <DetailRow label={t('legal.terms.details.phoneLabel')} value={t('legal.terms.details.phoneValue')} />
    </dl>
  )
}

export default function TermsOfService() {
  const { t } = useTranslation()

  return (
    <div className="app-stage min-h-screen">
      <div className="flex items-center justify-end p-4">
        <div className="flex items-center gap-2">
          <LanguageSwitch />
        </div>
      </div>

      <div className="mx-auto max-w-2xl px-4 pb-24">
        <Link
          to="/login"
          className="mb-4 inline-flex items-center gap-1 text-sm text-primary-500 hover:text-primary-600"
        >
          <ArrowLeft size={16} />
          {t('common.back')}
        </Link>

        <Card variant="bordered" padding="lg">
          <h1 className="mb-6 font-heading text-2xl font-bold text-neutral-900 dark:text-white">
            {t('legal.terms.title')}
          </h1>

          <div className="space-y-6 text-sm leading-relaxed text-neutral-700 dark:text-neutral-300">
            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.terms.provider.title')}
              </h2>
              <p>{t('legal.terms.provider.intro')}</p>
              <ProviderDetails />
              <p className="mt-3">{t('legal.terms.provider.technicalAdmin')}</p>
            </section>

            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.terms.account.title')}
              </h2>
              <TranslatedList i18nKey="legal.terms.account.items" />
            </section>

            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.terms.credits.title')}
              </h2>
              <TranslatedList i18nKey="legal.terms.credits.items" />
            </section>

            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.terms.booking.title')}
              </h2>
              <TranslatedList i18nKey="legal.terms.booking.items" />
            </section>

            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.terms.cancellation.title')}
              </h2>
              <TranslatedList i18nKey="legal.terms.cancellation.items" />
            </section>

            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.terms.withdrawal.title')}
              </h2>
              <p>{t('legal.terms.withdrawal.body')}</p>
            </section>

            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.terms.complaints.title')}
              </h2>
              <TranslatedList i18nKey="legal.terms.complaints.items" />
            </section>

            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.terms.health.title')}
              </h2>
              <TranslatedList i18nKey="legal.terms.health.items" />
            </section>

            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.terms.privacy.title')}
              </h2>
              <p>
                {t('legal.terms.privacy.prefix')}{' '}
                <Link to="/privacy" className="text-primary-500 underline hover:text-primary-600">
                  {t('legal.terms.privacy.link')}
                </Link>
                .
              </p>
            </section>

            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.terms.disputes.title')}
              </h2>
              <p>
                {t('legal.terms.disputes.prefix')}{' '}
                <a href="https://www.coi.cz" className="text-primary-500 underline hover:text-primary-600">
                  www.coi.cz
                </a>
                .
              </p>
            </section>

            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.terms.final.title')}
              </h2>
              <TranslatedList i18nKey="legal.terms.final.items" />
            </section>

            <p className="border-t border-neutral-200 pt-4 text-xs text-neutral-500 dark:border-dark-border">
              {t('legal.terms.footer')}
            </p>
          </div>
        </Card>
      </div>
    </div>
  )
}
