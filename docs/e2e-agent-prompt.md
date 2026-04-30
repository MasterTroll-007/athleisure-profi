# E2E Agent Implementation Prompt

Budes implementovat automatizovane E2E testy pro projekt Athleisure-Domi v `C:\PROJECTS\athleisure-domi`.

Cilem neni manualni testovani. Cilem je pridat do repozitare spustitelnou Playwright E2E test suite, deterministicky seed/reset testovacich dat a dokumentaci, jak testy spustit.

Nejdrive si precti aktualni frontend routy, backend controllery, DTO a service vrstvu. Implementuj podle existujicich patternu v repozitari, ne podle domyslenych struktur.

## Kontext projektu

- Backend: Spring Boot Kotlin, lokalne `http://localhost:8080`
- Frontend: React/Vite, lokalne `http://localhost:3000`
- Lokální Docker stack se spousti pouze pres `docker-compose.local.yml`
- Nikdy nepouzivej produkcni `docker-compose.yml` pro lokalni E2E
- Lokalni DB je PostgreSQL na `localhost:5433`, database `fitness`
- Testovaci heslo pro seedovane ucty je `Test1234`
- Existujici lokalni seed je v `seed.local.sql`
- Rezervacni flow pouziva primarne `slots`, ne stare `availability_blocks`
- Pred implementaci rezervacnich testu zkontroluj aktualni `ReservationDTOs.kt`, `ReservationController.kt` a `ReservationService.kt`
- Pro vytvoreni rezervace pouzij aktualni backend request shape podle `CreateReservationRequest`
- Pokud se request pole pro slot jmenuje `slotId`, pouzivej vyhradne `slotId`
- Pokud ve frontendu nebo starych testech najdes historicke `blockId`, povazuj to za technicky dluh nebo bug a sjednot E2E testy na aktualni API kontrakt

## Hlavni cil

Implementuj automatizovane E2E testy, ktere overi:

- autentizaci a role
- admin spravu slotu
- admin sablony slotu
- aplikaci sablony
- odemceni tydne
- klientskou rezervaci
- ochranu proti double-bookingu
- odecitani a refund kreditu
- admin vytvoreni a zruseni rezervace
- registraci klienta pod trenerem

Testy musi kombinovat:

- UI kroky pro hlavni uzivatelsky flow
- API assertions pro overeni skutecneho backend stavu

## P0: Deterministicky E2E reset a seed

Pridej deterministicky E2E seed/reset mechanismus.

Pozadavky:

- Pridej SQL nebo skript pro E2E data, napriklad `test-data/e2e-reset.sql` nebo podobne umisteni podle konvenci projektu
- Reset musi byt bezpecny pro lokalni vyvoj a jasne oznaceny jako E2E/local only
- Reset musi odstranit pouze E2E data nebo data v testovacim rozsahu tak, aby nerozbil zakladni lokalni seed mimo testy
- Reset musi pripravit konzistentni stav pro kazdy beh testu
- Reset musi byt idempotentni

E2E seed musi obsahovat:

- admin/trener `admin@test.com`
- klienti:
  - `test1@test.com`
  - `test2@test.com`
  - `test3@test.com`
- vsichni maji heslo `Test1234`
- vsichni klienti maji:
  - `email_verified = true`
  - `role = client`
  - `trainer_id = id(admin@test.com)`
  - minimalne 10 kreditu
- admin ma:
  - `role = admin`
  - `email_verified = true`
  - stabilni `invite_code`
  - `adjacent_booking_required = false`, aby rezervace nebyly blokovane pravidlem navaznosti
- alespon jeden pricing item pro admina, napriklad `E2E Jeden trenink`, za 1 kredit
- alespon jedno training location, pokud UI/API s misty pracuje
- sloty pro cely budouci tyden pondeli-nedele
- sloty patri adminovi `admin@test.com`
- sloty jsou dostatecne v budoucnosti, aby nebyly povazovane za minule
- cast slotu je `UNLOCKED`
- cast slotu muze byt `LOCKED` nebo `BLOCKED`, pokud to pomuze testovat viditelnost
- dve sablony slotu pro admina:
  - `E2E Morning Template`
  - `E2E Weekend Template`

## P0: Jeden prikaz pro cisty lokalni start

Pridej nebo zdokumentuj jeden prikaz/script pro cisty E2E beh.

Priklady prijatelneho reseni:

- `npm run test:e2e`
- `npm run test:e2e:docker`
- PowerShell nebo shell skript v repu

Musi byt jasne:

- jak zvednout cisty Docker stack pres `docker-compose.local.yml`
- jak aplikovat E2E reset/seed
- jak spustit Playwright testy
- jak spustit debug/UI mod

Nesmí se používat produkční compose file.

## P0: Playwright E2E suite

Pridej Playwright do frontend casti projektu.

Preferovana struktura:

```text
frontend/e2e/
frontend/e2e/specs/
frontend/e2e/fixtures/
frontend/playwright.config.ts
```

Pridej npm skripty do `frontend/package.json`, napriklad:

- `test:e2e`
- `test:e2e:ui`
- `test:e2e:debug`

Playwright testy musi byt opakovatelne bez rucniho zasahu.

## P1: Test IDs

Dopln `data-testid` na klicove UI prvky, pokud aktualni UI nema stabilni selektory.

Minimalne pro:

- login email input
- login password input
- login submit button
- admin calendar/slots page
- apply template action
- unlock week action
- reservation slot button/card
- reservation confirm button
- credits balance
- logout button
- registration form fields
- registration submit button

Zachovej existujici vzhled a chovani UI. Pridani `data-testid` nesmi menit UX.

## API acceptance kriteria

API pouzivej jako zdroj pravdy pro PASS/FAIL.

Base URLs:

- Backend: `http://localhost:8080/api`
- Frontend: `http://localhost:3000`

Autentizace:

```http
POST /api/auth/login
```

Admin login:

```json
{
  "email": "admin@test.com",
  "password": "Test1234",
  "rememberMe": false
}
```

PASS:

- HTTP 200
- response obsahuje `accessToken`
- `user.email = admin@test.com`
- `user.role = admin`

Klient login:

```json
{
  "email": "test1@test.com",
  "password": "Test1234",
  "rememberMe": false
}
```

PASS:

- HTTP 200
- response obsahuje `accessToken`
- `user.email = test1@test.com`
- `user.role = client`
- `GET /api/auth/me` vraci stejneho uzivatele

Bezpecnost:

- `GET /api/admin/slots?start=<WEEK_START>&end=<WEEK_END>` bez tokenu vraci 401
- stejny request s klientskym tokenem vraci 403
- stejny request s admin tokenem vraci 200

## E2E scenare

### 1. Auth a role

Test:

- admin login pres UI
- klient login pres UI
- spatny login podle rozumne moznosti

API acceptance:

- `POST /api/auth/login` pro admina vraci 200
- `user.role = admin`
- `POST /api/auth/login` pro klienta vraci 200
- `user.role = client`
- `GET /api/auth/me` vraci spravneho uzivatele
- admin endpoint bez tokenu vraci 401
- admin endpoint s klientskym tokenem vraci 403

### 2. Admin vidi sloty a sablony

Test:

- prihlas admina
- otevri admin kalendar nebo spravu slotu
- over, ze sloty pro E2E tyden existuji
- over, ze existuji dve E2E sablony

API acceptance:

- `GET /api/admin/slots?start=<weekStart>&end=<weekEnd>` vraci 200
- response obsahuje sloty pro pondeli-nedeli
- sloty maji `id`, `date`, `startTime`, `endTime`, `status`
- `GET /api/admin/templates` obsahuje `E2E Morning Template` a `E2E Weekend Template`

### 3. Aplikace sablony a odemceni tydne

Test:

- admin aplikuje jednu sablonu na dalsi budouci tyden
- over, ze sloty vzniknou
- over, ze locked sloty nejsou klientovi dostupne
- admin odemkne tyden
- over, ze klient sloty vidi

API acceptance:

- `POST /api/admin/slots/apply-template` vraci 200
- response ma `createdSlots > 0`
- nove sloty jsou nejdriv `locked`
- klient pred odemcenim nevidi locked sloty jako dostupne
- `POST /api/admin/slots/unlock-week` vraci 200
- `unlockedCount > 0`
- `GET /api/reservations/available?start=<start>&end=<end>` jako klient vraci dostupne sloty

### 4. Klient vytvori rezervaci

Test:

- prihlas `test1@test.com`
- otevri rezervace
- vyber dostupny slot
- potvrd rezervaci
- over uspech v UI

API acceptance:

- pred rezervaci zavolej `GET /api/credits/balance`
- vytvoreni rezervace pouziva aktualni backend request shape podle `CreateReservationRequest`
- payload musi obsahovat spravny identifikator slotu podle aktualniho DTO, napriklad `slotId`, pokud tak aktualni API vypada
- response je 201
- `status = confirmed`
- rezervace je navazana na spravny `slot.id`
- `creditsUsed` odpovida pricing itemu
- `GET /api/reservations` obsahuje novou rezervaci
- `GET /api/reservations/upcoming` obsahuje novou rezervaci
- `GET /api/credits/balance` je snizeny presne o `creditsUsed`
- `GET /api/credits/transactions` obsahuje zapornou reservation transakci
- `GET /api/admin/slots?...` ukazuje slot jako `reserved`, pokud byla naplnena kapacita slotu

### 5. Double booking ochrana

Test:

- `test1@test.com` rezervuje capacity=1 slot
- `test2@test.com` se pokusi rezervovat stejny slot

API acceptance:

- druhy `POST /api/reservations` nevrati 201
- ocekavej 400 nebo jinou chybovou odpoved
- kreditni zustatek `test2@test.com` se nezmeni
- na stejny slot nevznikne druha confirmed rezervace

### 6. Zruseni rezervace klientem a refund

Test:

- klient zrusi vlastni rezervaci
- UI ukaze zruseny stav nebo rezervace zmizi z upcoming podle aktualniho chovani aplikace

API acceptance:

- `GET /api/reservations/{id}/refund-preview` vraci 200
- `DELETE /api/reservations/{id}` vraci 200
- rezervace ma `status = cancelled`
- kredity jsou vracene podle preview
- `GET /api/credits/transactions` obsahuje refund transakci
- slot se vrati do `unlocked`, pokud kapacita neni naplnena

### 7. Admin vytvori a zrusi rezervaci klientovi

Test:

- admin vytvori rezervaci pro `test3@test.com`
- admin ji zrusi s refundem

API acceptance:

- `POST /api/admin/reservations` vraci 201
- request pouziva aktualni backend request shape pro admin rezervaci podle `AdminCreateReservationRequest`
- reservation je `confirmed`
- pokud `deductCredits = true`, klientovi se odectou kredity
- `DELETE /api/admin/reservations/{id}?refundCredits=true` vraci 200
- reservation je `cancelled`
- kredity jsou vracene

### 8. Registrace pod trenera

Test:

- admin ma invite code
- novy klient se registruje pres registracni flow pod trenerem
- aktivaci proved pres SQL nebo test helper
- novy klient se prihlasi a vytvori rezervaci

API acceptance:

- `GET /api/admin/settings` obsahuje `inviteCode`
- pokud chybi, zavolej `POST /api/admin/settings/regenerate-code`
- `GET /api/auth/trainer/{inviteCode}` vraci 200
- `POST /api/auth/register` vraci 201
- login pred aktivaci selze kvuli neoverenemu emailu
- SQL aktivace:

```sql
UPDATE users
SET email_verified = true, credits = 10
WHERE email = '<new-e2e-user-email>';
```

PASS po aktivaci:

- login po aktivaci vraci 200
- novy uzivatel ma `role = client`
- `trainerId = id(admin@test.com)`
- vidi sloty trenera
- umi vytvorit rezervaci

## P1/P2: Artefakty a reporting

Nakonfiguruj Playwright tak, aby pri selhani ukladal:

- screenshot
- trace
- video, pokud to neni prilis narocne

Vystupy ukladej do standardnich Playwright output slozek, idealne mimo tracked source.

## P1: CI

Pokud je v repu GitHub Actions, pridej samostatny E2E job.

Job ma:

- zvednout lokalni Docker stack pres `docker-compose.local.yml`
- pockat na backend healthcheck
- spustit E2E seed/reset
- spustit Playwright testy
- uploadnout Playwright artefakty pri failu

Pokud CI neni pripravene nebo by vyzadovalo vetsi zasah, vytvor alespon dokumentovany navrh v README/E2E dokumentaci a oznac to jako follow-up.

## P1: Stripe nebo platebni lokalni test rezim

Nepokladej platebni flow za blocker hlavni E2E sady.

Pokud je projekt uz pripraveny na lokalni simulaci plateb:

- pridej jednoduchy E2E test nakupu kreditu pres dostupnou simulaci/API

Pokud neni:

- zdokumentuj, co chybi pro lokalni test rezim plateb
- neblokuj tim P0 testy rezervaci

## P1: Mail sink

Nepokladej email flow za blocker hlavni E2E sady.

Pokud je lokalni mail sink uz pripraveny:

- pridej test registrace nebo resetu hesla pres zachyceny email

Pokud neni:

- zdokumentuj navrh pro Mailpit nebo MailHog
- neblokuj tim P0 testy rezervaci

## Dokumentace

Pridej kratkou dokumentaci, napriklad:

- `docs/e2e.md`
- nebo sekci do README

Dokumentace musi obsahovat:

- jak spustit cisty lokalni stack
- jak spustit E2E testy
- jak spustit debug mod
- jak resetovat E2E data
- jake ucty se pouzivaji
- kde najit screenshot/trace/video artefakty

## Validace pred dokoncenim

Na konci spust:

- frontend lint, pokud existuje
- Playwright testy
- backend testy jen pokud zmeny zasahuji backend logiku

## Vystup prace

Na konci dodej:

- seznam zmenenych souboru
- jake testy byly pridany
- presne prikazy, ktere byly spusteny
- vysledky testu
- co pripadne nebylo mozne dokoncit a proc

## Dulezite hranice

- Neopravuj nesouvisejici chyby
- Nemen produkcni `docker-compose.yml`
- Nepouzivej produkcni data ani produkcni sluzby
- Nepouzivej krehke selektory, pokud lze pridat `data-testid`
- E2E testy musi byt opakovane spustitelne bez rucniho zasahu
- Testovaci data musi byt deterministicka
- Pokud narazis na bug v aplikaci, muzes ho opravit pouze pokud primo blokuje implementaci E2E scenare; jinak ho zdokumentuj jako nalez
