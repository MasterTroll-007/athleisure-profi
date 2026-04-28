# E2E testy

Playwright E2E sada testuje lokální Docker stack. Nepoužívá produkční compose, produkční databázi ani produkční platební služby.

## Čistý lokální běh

Z kořene repozitáře:

```powershell
docker compose -f docker-compose.local.yml up -d --build
cd frontend
npm run test:e2e
```

Playwright `global-setup` počká na `http://localhost:8080/api/monitor/health`, počká na `http://localhost:3000/login` a aplikuje `test-data/e2e-reset.sql`. Každý test potom reset aplikuje znovu, aby scénáře nebyly závislé na pořadí.

## Debug režim

```powershell
cd frontend
npm run test:e2e:ui
npm run test:e2e:debug
```

## Ruční reset E2E dat

```powershell
Get-Content -Raw test-data/e2e-reset.sql | docker compose -f docker-compose.local.yml exec -T db psql -U fitness -d fitness -v ON_ERROR_STOP=1
```

Reset je určený jen pro lokální E2E. Připraví `admin@test.com`, `test1@test.com`, `test2@test.com`, `test3@test.com`, pricing item, místo tréninku, budoucí sloty a dvě šablony.

Heslo pro všechny seedované účty je `Test1234`.

## Artefakty

Při selhání Playwright ukládá screenshot, trace a video do standardních složek:

- `frontend/test-results/`
- `frontend/playwright-report/`

Tyto složky nejsou trackované v gitu.

## Platební a e-mail flow

Hlavní E2E sada neblokuje na Stripe ani e-mailu. Registrace je testovaná přes UI a aktivace účtu se provádí SQL helperem. Pro plný e-mail test by bylo vhodné přidat Mailpit nebo MailHog do `docker-compose.local.yml`.
