import AuthLayout from '@/components/layout/AuthLayout'
import { Card } from '@/components/ui'
import { Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'

export default function TermsOfService() {
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
          Obchodní podmínky
        </h1>

        <div className="space-y-6 text-sm text-neutral-700 dark:text-neutral-300">
          <section>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
              1. Úvodní ustanovení
            </h2>
            <p>
              Tyto obchodní podmínky upravují vztah mezi provozovatelem služby DomiFit (domi-fit.online)
              a uživatelem při využívání služby online rezervace osobních tréninků.
            </p>
          </section>

          <section>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
              2. Registrace a účet
            </h2>
            <ul className="list-disc list-inside space-y-1">
              <li>Registrace je možná pouze prostřednictvím pozvánky od trenéra</li>
              <li>Uživatel je povinen uvést pravdivé údaje</li>
              <li>Uživatel je odpovědný za zabezpečení svého účtu</li>
              <li>Účet je možné kdykoli smazat v nastavení profilu</li>
            </ul>
          </section>

          <section>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
              3. Kreditový systém
            </h2>
            <ul className="list-disc list-inside space-y-1">
              <li>Tréninky se rezervují pomocí kreditů (tréninků)</li>
              <li>Kredity se nakupují prostřednictvím online platební brány</li>
              <li>Zakoupené kredity jsou nevratné, pokud není dohodnuto jinak</li>
              <li>Kredity nemají omezenou platnost</li>
            </ul>
          </section>

          <section>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
              4. Rezervace a storno
            </h2>
            <ul className="list-disc list-inside space-y-1">
              <li>Rezervace je závazná po potvrzení v systému</li>
              <li>Storno podmínky nastavuje trenér (typicky 24 hodin předem pro plný refund)</li>
              <li>Při pozdním zrušení může být kredit stržen částečně nebo úplně</li>
              <li>Trenér si vyhrazuje právo zrušit trénink z vážných důvodů</li>
            </ul>
          </section>

          <section>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
              5. Odpovědnost
            </h2>
            <ul className="list-disc list-inside space-y-1">
              <li>Uživatel cvičí na vlastní odpovědnost</li>
              <li>Provozovatel neodpovídá za zdravotní komplikace vzniklé při tréninku</li>
              <li>Uživatel je povinen informovat trenéra o zdravotních omezeních</li>
            </ul>
          </section>

          <section>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
              6. Ochrana osobních údajů
            </h2>
            <p>
              Zpracování osobních údajů se řídí{' '}
              <Link to="/privacy" className="text-primary-500 hover:text-primary-600 underline">
                Zásadami ochrany osobních údajů
              </Link>.
            </p>
          </section>

          <section>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">
              7. Závěrečná ustanovení
            </h2>
            <ul className="list-disc list-inside space-y-1">
              <li>Provozovatel si vyhrazuje právo na změnu podmínek</li>
              <li>O změnách bude uživatel informován e-mailem</li>
              <li>Vztahy se řídí právním řádem České republiky</li>
            </ul>
          </section>
        </div>

        <p className="mt-6 text-xs text-neutral-500 dark:text-neutral-500">
          Poslední aktualizace: březen 2026
        </p>
      </Card>
    </AuthLayout>
  )
}
