import { Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { Card } from '@/components/ui'
import ThemeToggle from '@/components/layout/ThemeToggle'
import LanguageSwitch from '@/components/layout/LanguageSwitch'

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
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">1. Správce údajů</h2>
              <p>Provozovatel rezervačního systému na doméně rezervace-pankova.online je správcem vašich osobních údajů ve smyslu nařízení GDPR.</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">2. Shromažďované údaje</h2>
              <ul className="list-disc pl-5 space-y-1">
                <li>E-mailová adresa (povinné — pro přihlášení a komunikaci)</li>
                <li>Jméno a příjmení (volitelné — pro identifikaci)</li>
                <li>Telefonní číslo (volitelné — pro kontakt)</li>
                <li>Historie rezervací a tréninků</li>
                <li>Kreditové transakce a platební historie</li>
                <li>Tělesná měření a tréninkové záznamy (pokud je zadáte)</li>
                <li>Hodnocení a zpětná vazba</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">3. Účel zpracování</h2>
              <ul className="list-disc pl-5 space-y-1">
                <li>Poskytování rezervačního systému a správy tréninků</li>
                <li>Zpracování plateb za kreditové balíčky</li>
                <li>Zasílání připomínek a oznámení</li>
                <li>Sledování pokroku v tréninku</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">4. Právní základ</h2>
              <p>Zpracování je nezbytné pro plnění smlouvy (čl. 6 odst. 1 písm. b) GDPR) — poskytování fitness rezervačních služeb.</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">5. Doba uchovávání</h2>
              <ul className="list-disc pl-5 space-y-1">
                <li>Aktivní účet — po dobu trvání smluvního vztahu</li>
                <li>Po smazání účtu — data jsou anonymizována, anonymizované záznamy uchovány 7 let (daňové povinnosti)</li>
                <li>Ověřovací tokeny — 24 hodin</li>
                <li>Tokeny pro reset hesla — 30 minut</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">6. Vaše práva</h2>
              <ul className="list-disc pl-5 space-y-1">
                <li><strong>Právo na přístup</strong> — můžete exportovat svá data v JSON formátu (Profil → Data a soukromí)</li>
                <li><strong>Právo na výmaz</strong> — můžete smazat svůj účet (Profil → Data a soukromí)</li>
                <li><strong>Právo na opravu</strong> — můžete upravit své osobní údaje v profilu</li>
                <li><strong>Právo na přenositelnost</strong> — export dat ve strojově čitelném formátu</li>
                <li><strong>Právo odvolat souhlas</strong> — kdykoli můžete zrušit svůj účet</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">7. Třetí strany</h2>
              <ul className="list-disc pl-5 space-y-1">
                <li><strong>Stripe</strong> — zpracování plateb (e-mail, částka). Stripe je certifikovaný dle GDPR.</li>
                <li><strong>E-mailová služba (SMTP)</strong> — zasílání ověřovacích e-mailů a připomínek (e-mail, jméno).</li>
              </ul>
              <p className="mt-2">Žádná analytická ani sledovací data nejsou shromažďována.</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">8. Zabezpečení</h2>
              <ul className="list-disc pl-5 space-y-1">
                <li>Hesla hashována algoritmem BCrypt</li>
                <li>Veškerá komunikace šifrována (HTTPS/TLS)</li>
                <li>Přístup omezen dle rolí (klient vidí jen svá data)</li>
                <li>Audit log všech citlivých operací</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">9. Cookies</h2>
              <p>Aplikace používá pouze technicky nezbytné cookies pro autentizaci (HttpOnly, Secure, SameSite=Strict). Žádné sledovací cookies nejsou použity.</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">10. Kontakt</h2>
              <p>V případě dotazů ohledně ochrany osobních údajů nás kontaktujte prostřednictvím svého trenéra nebo na e-mailové adrese uvedené v aplikaci.</p>
            </section>

            <p className="text-xs text-neutral-500 pt-4 border-t border-neutral-200 dark:border-dark-border">
              Poslední aktualizace: březen 2026
            </p>
          </div>
        </Card>
      </div>
    </div>
  )
}
