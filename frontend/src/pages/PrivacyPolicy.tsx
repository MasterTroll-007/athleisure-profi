import AuthLayout from '@/components/layout/AuthLayout'
import { Card } from '@/components/ui'
import { Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'

export default function PrivacyPolicy() {
  return (
    <AuthLayout>
      <Card variant="bordered" className="w-full max-w-2xl" padding="lg">
        <Link
          to="/login"
          className="inline-flex items-center gap-1 text-sm text-primary-500 hover:text-primary-600 mb-4"
        >
          <ArrowLeft size={16} />
          Zpět
        </Link>

        <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white mb-6">
          Zásady ochrany osobních údajů
        </h1>

        <div className="space-y-6 text-sm text-neutral-700 dark:text-neutral-300">
          <section>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
              1. Správce údajů
            </h2>
            <p>
              Správcem osobních údajů je provozovatel služby DomiFit (domi-fit.online).
              Kontakt: info@domi-fit.online
            </p>
          </section>

          <section>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
              2. Shromažďované údaje
            </h2>
            <ul className="list-disc list-inside space-y-1">
              <li>E-mailová adresa</li>
              <li>Jméno a příjmení</li>
              <li>Telefonní číslo</li>
              <li>Historie rezervací a tréninků</li>
              <li>Tělesné míry (pokud jsou zadány)</li>
              <li>Platební transakce (bez údajů o kartě)</li>
            </ul>
          </section>

          <section>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
              3. Účel zpracování
            </h2>
            <ul className="list-disc list-inside space-y-1">
              <li>Poskytování služby rezervace tréninků</li>
              <li>Správa kreditového systému a plateb</li>
              <li>Komunikace s klientem (připomínky, oznámení)</li>
              <li>Vedení tréninkového deníku a měření</li>
            </ul>
          </section>

          <section>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
              4. Právní základ
            </h2>
            <p>
              Zpracování je nezbytné pro plnění smlouvy o poskytování služeb (čl. 6 odst. 1 písm. b) GDPR).
            </p>
          </section>

          <section>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
              5. Doba uchování
            </h2>
            <ul className="list-disc list-inside space-y-1">
              <li>Po dobu aktivního účtu</li>
              <li>7 let po smazání účtu (zákonné požadavky)</li>
              <li>Po smazání účtu jsou data anonymizována</li>
            </ul>
          </section>

          <section>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
              6. Vaše práva
            </h2>
            <ul className="list-disc list-inside space-y-1">
              <li>Právo na přístup k osobním údajům</li>
              <li>Právo na export dat (v profilu)</li>
              <li>Právo na opravu nepřesných údajů</li>
              <li>Právo na výmaz účtu (v profilu)</li>
              <li>Právo podat stížnost u ÚOOÚ</li>
            </ul>
          </section>

          <section>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
              7. Třetí strany
            </h2>
            <ul className="list-disc list-inside space-y-1">
              <li>Stripe — zpracování plateb</li>
              <li>SMTP služba (Gmail) — odesílání e-mailů</li>
            </ul>
            <p className="mt-1">
              Údaje nejsou předávány mimo EU/EHP bez odpovídajících záruk.
            </p>
          </section>

          <section>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
              8. Kontakt
            </h2>
            <p>
              V případě dotazů ohledně ochrany osobních údajů nás kontaktujte na info@domi-fit.online.
            </p>
          </section>
        </div>

        <p className="mt-6 text-xs text-neutral-500 dark:text-neutral-500">
          Poslední aktualizace: březen 2026
        </p>
      </Card>
    </AuthLayout>
  )
}
