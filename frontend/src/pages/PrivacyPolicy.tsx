import { Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { Card } from '@/components/ui'
import ThemeToggle from '@/components/layout/ThemeToggle'
import LanguageSwitch from '@/components/layout/LanguageSwitch'

const controller = {
  name: '[DOPLNIT JMÉNO A PŘÍJMENÍ TRENÉRKY PÁNKOVÉ]',
  businessId: '[DOPLNIT IČO]',
  address: '[DOPLNIT SÍDLO / KONTAKTNÍ ADRESU]',
  email: '[DOPLNIT KONTAKTNÍ E-MAIL PRO GDPR]',
}

function ControllerDetails() {
  return (
    <dl className="grid gap-2 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-950 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-100">
      <div className="font-semibold">Před ostrým spuštěním doplnit identifikační údaje správce.</div>
      <div><dt className="inline font-medium">Správce: </dt><dd className="inline">{controller.name}</dd></div>
      <div><dt className="inline font-medium">IČO: </dt><dd className="inline">{controller.businessId}</dd></div>
      <div><dt className="inline font-medium">Sídlo / adresa: </dt><dd className="inline">{controller.address}</dd></div>
      <div><dt className="inline font-medium">Kontakt pro osobní údaje: </dt><dd className="inline">{controller.email}</dd></div>
    </dl>
  )
}

export default function PrivacyPolicy() {
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
            Zásady ochrany osobních údajů
          </h1>

          <div className="space-y-6 text-neutral-700 dark:text-neutral-300 text-sm leading-relaxed">
            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">1. Správce osobních údajů</h2>
              <p>
                Správcem osobních údajů klientů je poskytovatel osobních tréninků, který provozuje
                rezervační systém na doméně rezervace-pankova.online.
              </p>
              <ControllerDetails />
              <p className="mt-3">
                Technickou správu systému může zajišťovat pověřený dodavatel IT služeb jako zpracovatel.
                Zpracovatel přistupuje k osobním údajům pouze v rozsahu nezbytném pro provoz, údržbu,
                zabezpečení a podporu systému.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">2. Jaké údaje zpracováváme</h2>
              <ul className="list-disc pl-5 space-y-1">
                <li>identifikační a kontaktní údaje: e-mail, jméno, příjmení, telefon,</li>
                <li>údaje o účtu: přihlašovací údaje v zabezpečené podobě, role, stav ověření e-mailu,</li>
                <li>rezervační údaje: historie rezervací, zrušení, docházka, typ tréninku, místo a čas tréninku,</li>
                <li>platební a kreditové údaje: kreditové transakce, balíčky, stav platby, identifikátory platební brány,</li>
                <li>tréninkové údaje: poznámky k tréninku, tréninkový deník, cviky, série, opakování, váhy a související záznamy, pokud jsou v systému zadány,</li>
                <li>komunikační údaje: systémová oznámení, připomínky, ověřovací a resetovací e-maily,</li>
                <li>technické a bezpečnostní údaje: časy přihlášení, IP adresa v serverových logách, chybové a bezpečnostní záznamy.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">3. Účely a právní základy</h2>
              <ul className="list-disc pl-5 space-y-1">
                <li>vedení účtu, rezervací a kreditů: plnění smlouvy,</li>
                <li>zpracování plateb a účetních záznamů: plnění smlouvy a zákonné povinnosti,</li>
                <li>zasílání transakčních e-mailů: plnění smlouvy a oprávněný zájem na informování klienta,</li>
                <li>zabezpečení systému, prevence zneužití a řešení incidentů: oprávněný zájem,</li>
                <li>tréninkový deník a související poznámky: plnění služby osobního tréninku; u citlivých zdravotních informací je potřeba doplnit finální právní posouzení.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">4. Příjemci a zpracovatelé</h2>
              <ul className="list-disc pl-5 space-y-1">
                <li><strong>Stripe</strong> - zpracování online plateb a platebních událostí.</li>
                <li><strong>SMTP/e-mailová služba</strong> - doručování ověřovacích e-mailů, resetu hesla a systémových zpráv.</li>
                <li><strong>Hosting a technická správa</strong> - provoz serveru, databáze, záloh, monitoringu a údržby aplikace.</li>
                <li><strong>Orgány veřejné moci</strong> - pouze pokud to ukládá právní předpis.</li>
              </ul>
              <p className="mt-2">Aplikace nepoužívá analytické ani marketingové sledování, pokud není výslovně uvedeno jinak.</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">5. Doba uchování</h2>
              <ul className="list-disc pl-5 space-y-1">
                <li>účet a rezervační data: po dobu trvání uživatelského účtu a následně po dobu nezbytnou pro ochranu práv poskytovatele,</li>
                <li>platební a účetní záznamy: po dobu vyžadovanou účetními a daňovými předpisy, typicky až 10 let podle povahy záznamu,</li>
                <li>tréninkové poznámky a deník: po dobu aktivní spolupráce, případně do smazání účtu nebo žádosti o výmaz, pokud nebrání jiný právní důvod,</li>
                <li>ověřovací tokeny: zpravidla 24 hodin,</li>
                <li>tokeny pro reset hesla: zpravidla 30 minut,</li>
                <li>serverové a bezpečnostní logy: po omezenou dobu nezbytnou pro bezpečnost a provoz systému.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">6. Práva klienta</h2>
              <ul className="list-disc pl-5 space-y-1">
                <li>právo na přístup k osobním údajům,</li>
                <li>právo na opravu nepřesných údajů,</li>
                <li>právo na výmaz, pokud jsou splněny zákonné podmínky,</li>
                <li>právo na omezení zpracování,</li>
                <li>právo na přenositelnost údajů,</li>
                <li>právo vznést námitku proti zpracování založenému na oprávněném zájmu,</li>
                <li>právo podat stížnost u Úřadu pro ochranu osobních údajů.</li>
              </ul>
              <p className="mt-2">
                V aplikaci lze využít export dat a žádost o smazání účtu v sekci Profil - Data a soukromí.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">7. Cookies a lokální úložiště</h2>
              <p>
                Aplikace používá technicky nezbytné mechanismy pro přihlášení, zabezpečení a uložení základních
                nastavení aplikace, například jazyk, vzhled nebo potvrzení cookie informace. Nepoužívá marketingové
                ani analytické cookies.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">8. Zabezpečení</h2>
              <ul className="list-disc pl-5 space-y-1">
                <li>hesla jsou ukládána pouze v hashované podobě,</li>
                <li>komunikace s aplikací probíhá přes HTTPS,</li>
                <li>přístup k údajům je omezen podle role uživatele,</li>
                <li>databáze je zálohována a přístup k produkčnímu prostředí je omezen na pověřené osoby.</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">9. Kontakt</h2>
              <p>
                Žádosti a dotazy k ochraně osobních údajů posílejte na kontakt správce uvedený v článku 1.
                [DOPLNIT FINÁLNÍ E-MAIL PRO GDPR A PODPORU.]
              </p>
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
