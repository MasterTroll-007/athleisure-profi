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

function ControllerDetails() {
  const { t } = useTranslation()

  return (
    <dl className="grid gap-2 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-950 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-100">
      <div className="font-semibold">{t('legal.privacy.details.notice')}</div>
      <DetailRow label={t('legal.privacy.details.nameLabel')} value={t('legal.privacy.details.nameValue')} />
      <DetailRow
        label={t('legal.privacy.details.businessIdLabel')}
        value={t('legal.privacy.details.businessIdValue')}
      />
      <DetailRow label={t('legal.privacy.details.addressLabel')} value={t('legal.privacy.details.addressValue')} />
      <DetailRow label={t('legal.privacy.details.emailLabel')} value={t('legal.privacy.details.emailValue')} />
    </dl>
  )
}

export default function PrivacyPolicy() {
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
            {t('legal.privacy.title')}
          </h1>

          <div className="space-y-6 text-sm leading-relaxed text-neutral-700 dark:text-neutral-300">
            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.privacy.controller.title')}
              </h2>
              <p>{t('legal.privacy.controller.intro')}</p>
              <ControllerDetails />
              <p className="mt-3">{t('legal.privacy.controller.processor')}</p>
            </section>

            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.privacy.data.title')}
              </h2>
              <TranslatedList i18nKey="legal.privacy.data.items" />
            </section>

            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.privacy.purposes.title')}
              </h2>
              <TranslatedList i18nKey="legal.privacy.purposes.items" />
            </section>

            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.privacy.recipients.title')}
              </h2>
              <TranslatedList i18nKey="legal.privacy.recipients.items" />
              <p className="mt-2">{t('legal.privacy.recipients.noTracking')}</p>
            </section>

            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.privacy.retention.title')}
              </h2>
              <TranslatedList i18nKey="legal.privacy.retention.items" />
            </section>

            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.privacy.rights.title')}
              </h2>
              <TranslatedList i18nKey="legal.privacy.rights.items" />
              <p className="mt-2">{t('legal.privacy.rights.profileHint')}</p>
            </section>

            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.privacy.cookies.title')}
              </h2>
              <p>{t('legal.privacy.cookies.body')}</p>
            </section>

            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.privacy.security.title')}
              </h2>
              <TranslatedList i18nKey="legal.privacy.security.items" />
            </section>

            <section>
              <h2 className="mb-2 text-lg font-semibold text-neutral-900 dark:text-white">
                {t('legal.privacy.contact.title')}
              </h2>
              <p>{t('legal.privacy.contact.body')}</p>
            </section>

            <p className="border-t border-neutral-200 pt-4 text-xs text-neutral-500 dark:border-dark-border">
              {t('legal.privacy.footer')}
            </p>
          </div>
        </Card>
      </div>
    </div>
  )
}
