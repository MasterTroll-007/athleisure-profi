import { Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { Card } from '@/components/ui'
import ThemeToggle from '@/components/layout/ThemeToggle'
import LanguageSwitch from '@/components/layout/LanguageSwitch'

const provider = {
  name: '[DOPLNIT JMÉNO A PŘÍJMENÍ TRENÉRKY PÁNKOVÉ]',
  businessId: '[DOPLNIT IČO]',
  taxId: '[DOPLNIT DIČ, POKUD EXISTUJE]',
  address: '[DOPLNIT SÍDLO / KONTAKTNÍ ADRESU]',
  email: '[DOPLNIT KONTAKTNÍ E-MAIL]',
  phone: '[DOPLNIT TELEFON, POKUD MÁ BÝT UVEDEN]',
}

function ProviderDetails() {
  return (
    <dl className="grid gap-2 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-950 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-100">
      <div className="font-semibold">Před ostrým spuštěním doplnit identifikační údaje poskytovatele.</div>
      <div><dt className="inline font-medium">Poskytovatel: </dt><dd className="inline">{provider.name}</dd></div>
      <div><dt className="inline font-medium">IČO: </dt><dd className="inline">{provider.businessId}</dd></div>
      <div><dt className="inline font-medium">DIČ: </dt><dd className="inline">{provider.taxId}</dd></div>
      <div><dt className="inline font-medium">Sídlo / adresa: </dt><dd className="inline">{provider.address}</dd></div>
      <div><dt className="inline font-medium">E-mail: </dt><dd className="inline">{provider.email}</dd></div>
      <div><dt className="inline font-medium">Telefon: </dt><dd className="inline">{provider.phone}</dd></div>
    </dl>
  )
}

export default function TermsOfService() {
  return (
    <div className="app-stage min-h-screen">
      <div className="flex items-center justify-end p-4">
        <div className="flex items-center gap-2">
          <LanguageSwitch />
          <ThemeToggle />
        </div>
      </div>

      <div className="max-w-2xl mx-auto px-4 pb-24">
        <Link
          to="/login"
          className="inline-flex items-center gap-1 text-sm text-primary-500 hover:text-primary-600 mb-4"
        >
          <ArrowLeft size={16} />
          Zpět
        </Link>

        <Card variant="bordered" padding="lg">
          <h1 className="text-2xl font-heading font-bold text-neutral-900 dark:text-white mb-6">
            Obchodní podmínky
          </h1>

          <div className="space-y-6 text-neutral-700 dark:text-neutral-300 text-sm leading-relaxed">
            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">1. Poskytovatel služby</h2>
              <p>
                Tyto obchodní podmínky upravují vztah mezi poskytovatelem osobních tréninků a klientem
                při používání rezervačního systému na doméně rezervace-pankova.online.
              </p>
              <ProviderDetails />
              <p className="mt-3">
                Technickou správu rezervačního systému může zajišťovat pověřený dodavatel IT služeb.
                Tento dodavatel není poskytovatelem tréninků ani smluvní stranou klienta, pokud není
                v konkrétním případě uvedeno jinak.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">2. Registrace a uživatelský účet</h2>
              <ul className="list-disc pl-5 space-y-1">
                <li>Registrace je možná pouze prostřednictvím pozvánky od trenéra.</li>
                <li>Klient je povinen uvést pravdivé a aktuální údaje.</li>
                <li>Klient odpovídá za zabezpečení svého účtu a nesmí přihlašovací údaje předávat dalším osobám.</li>
                <li>Účet je možné smazat v nastavení profilu nebo na základě žádosti zaslané poskytovateli.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">3. Kreditový systém a platby</h2>
              <ul className="list-disc pl-5 space-y-1">
                <li>Klient nakupuje kreditové balíčky, které lze čerpat na rezervace tréninků.</li>
                <li>Cena balíčku je uvedena před odesláním platby.</li>
                <li>Platby jsou zpracovávány prostřednictvím platební brány Stripe.</li>
                <li>Kredity nemají omezenou platnost, pokud není u konkrétní nabídky uvedeno jinak.</li>
                <li>Po úspěšné platbě jsou kredity připsány na uživatelský účet automaticky po potvrzení platební bránou.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">4. Rezervace tréninku</h2>
              <ul className="list-disc pl-5 space-y-1">
                <li>Rezervace je závazná po jejím potvrzení v rezervačním systému.</li>
                <li>Za rezervaci se klientovi odečte počet kreditů odpovídající typu tréninku.</li>
                <li>Klient je povinen dostavit se na trénink včas a informovat trenéra o zdravotních omezeních.</li>
                <li>Poskytovatel může trénink zrušit nebo přesunout z vážných provozních, zdravotních nebo organizačních důvodů.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">5. Storno a vrácení kreditů</h2>
              <ul className="list-disc pl-5 space-y-1">
                <li>[DOPLNIT PŘESNÁ STORNO PRAVIDLA: například 24+ hodin předem = 100 % kreditů zpět.]</li>
                <li>[DOPLNIT PRAVIDLO PRO POZDNÍ ZRUŠENÍ: například méně než 24 hodin = bez vrácení kreditů.]</li>
                <li>Pokud trénink zruší poskytovatel, klientovi se vrací příslušné kredity v plné výši, pokud se strany nedohodnou na náhradním termínu.</li>
                <li>Vrácení peněz za zakoupené kreditové balíčky se řeší individuálně v souladu s těmito podmínkami a platnými právními předpisy.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">6. Odstoupení od smlouvy</h2>
              <p>
                [DOPLNIT FINÁLNÍ TEXT PO PRÁVNÍ KONTROLE.] U online nákupu kreditů a rezervací služeb
                vázaných na konkrétní termín je potřeba přesně vymezit, kdy může spotřebitel od smlouvy
                odstoupit a kdy se uplatní zákonná výjimka.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">7. Reklamace</h2>
              <ul className="list-disc pl-5 space-y-1">
                <li>Reklamace nebo dotazy ke kreditům, platbě či rezervaci klient zasílá na kontakt poskytovatele uvedený v těchto podmínkách.</li>
                <li>[DOPLNIT LHŮTU PRO VYŘÍZENÍ REKLAMACE, například bez zbytečného odkladu, nejpozději do 30 dnů.]</li>
                <li>Při technické chybě platby nebo nepřipsání kreditů klient uvede e-mail účtu, datum platby a dostupné identifikační údaje platby.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">8. Odpovědnost a zdravotní stav</h2>
              <ul className="list-disc pl-5 space-y-1">
                <li>Klient cvičí s ohledem na svůj zdravotní stav a je povinen informovat trenéra o omezeních, zraněních nebo zdravotních rizicích.</li>
                <li>Poskytovatel nenahrazuje lékařskou péči ani zdravotní diagnostiku.</li>
                <li>Klient bere na vědomí, že fyzická aktivita může být spojena s přiměřeným rizikem zátěže nebo zranění.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">9. Ochrana osobních údajů</h2>
              <p>
                Zpracování osobních údajů se řídí{' '}
                <Link to="/privacy" className="text-primary-500 hover:text-primary-600 underline">
                  Zásadami ochrany osobních údajů
                </Link>.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">10. Mimosoudní řešení sporů</h2>
              <p>
                K mimosoudnímu řešení spotřebitelských sporů je příslušná Česká obchodní inspekce,
                Štěpánská 567/15, 120 00 Praha 2, web:{' '}
                <a href="https://www.coi.cz" className="text-primary-500 hover:text-primary-600 underline">
                  www.coi.cz
                </a>.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">11. Závěrečná ustanovení</h2>
              <ul className="list-disc pl-5 space-y-1">
                <li>Vztahy neupravené těmito podmínkami se řídí právním řádem České republiky.</li>
                <li>Poskytovatel může podmínky změnit, zejména při změně služeb, cen, platebních metod nebo právních předpisů.</li>
                <li>O podstatných změnách bude klient informován přiměřeným způsobem, například e-mailem nebo oznámením v aplikaci.</li>
              </ul>
            </section>

            <p className="text-xs text-neutral-500 pt-4 border-t border-neutral-200 dark:border-dark-border">
              Draft připraven pro produkční release: duben 2026. Před ostrým spuštěním doplnit identifikační údaje a nechat zkontrolovat finální znění.
            </p>
          </div>
        </Card>
      </div>
    </div>
  )
}
