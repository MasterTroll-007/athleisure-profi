# Rezervacni system Pankova

Webova aplikace pro fitness rezervace trenerky a jejich klientu. System resi spravu volnych terminu, klientskych rezervaci, kreditovych balicku, plateb pres Stripe, audit operaci a provozni monitoring.

## Tech stack

| Cast | Technologie |
|---|---|
| Backend | Spring Boot 3.2, Kotlin, PostgreSQL, JWT |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS, React Query, Zustand |
| E2E | Playwright |
| Infrastruktura | Docker Compose, Nginx, Certbot, GitHub Actions |
| Platby | Stripe Checkout + webhooky |

## Lokalni spusteni

Pro lokalni vyvoj pouzivej `docker-compose.local.yml`, protoze buildi aktualni zdrojaky z disku.

```bash
docker compose -f docker-compose.local.yml up --build
```

Lokani URL:

| Sluzba | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api |
| PostgreSQL | localhost:5433 |

Vychozi lokalni hodnoty patri do `.env` nebo `.env.local`. Tajne hodnoty nikdy necommitovat.

## Vyvojove prikazy

Backend:

```bash
cd backend
./gradlew build
./gradlew test
./gradlew bootRun
```

Frontend:

```bash
cd frontend
npm install
npm run lint
npm run build
npm run test
npm run test:e2e
```

Docker:

```bash
docker compose -f docker-compose.local.yml up -d --build
docker compose -f docker-compose.local.yml logs -f
docker compose -f docker-compose.local.yml down -v
```

## Produkcni deploy

Produkce bezi z `main` vetve pres GitHub Actions workflow `Deploy`.

Deploy pipeline:

1. Build backend a frontend Docker images.
2. Push images na Docker Hub jako `latest` a jako tag s SHA commitu.
3. Spusti Trivy security scan.
4. Pres SSH zkopiruje `docker-compose.yml`, `nginx/`, `deploy/` a `init.sql` na server.
5. Na serveru vytvori pre-deploy DB backup.
6. Aplikuje `deploy/schema-migrations.sql`.
7. Stahne nove images a restartuje stack.

Produkce:

| Cast | Hodnota |
|---|---|
| URL | https://rezervace-pankova.online |
| Compose file | `docker-compose.yml` |
| App directory na serveru | `~/fitness-app` |
| DB backups | `~/fitness-app/backups/postgres` |

## Nutne secrets

GitHub Actions secrets:

| Secret | Ucel |
|---|---|
| `DOCKERHUB_USERNAME` | Docker Hub login |
| `DOCKERHUB_TOKEN` | Docker Hub token |
| `SERVER_HOST` | Produkcni server |
| `SERVER_USER` | SSH uzivatel |
| `SERVER_SSH_KEY` | SSH private key pro deploy |
| `STRIPE_SECRET_KEY` | Stripe secret key |
| `STRIPE_PUBLISHABLE_KEY` | Stripe publishable key |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret |

Server `.env` musi obsahovat minimalne:

| Promenna | Poznamka |
|---|---|
| `DB_PASSWORD` | Heslo PostgreSQL |
| `JWT_SECRET` | Silny nahodny secret pro JWT |
| `MONITOR_PASSWORD` | Heslo pro monitor endpointy |
| `SMTP_USER` / `SMTP_PASSWORD` | Odesilani emailu |
| `APP_BASE_URL` | `https://rezervace-pankova.online` |

## Zalohy databaze

Produkce obsahuje kontejner `db-backup`.

| Nastaveni | Hodnota |
|---|---|
| Interval | kazdych 6 hodin |
| Retence | 7 dni |
| Format | PostgreSQL custom dump (`.dump`) |
| Umisteni | `~/fitness-app/backups/postgres` |

Rucni pre-deploy backup dela i deploy workflow pred migracemi.

Restore priklad:

```bash
cd ~/fitness-app
docker compose stop backend frontend nginx
docker compose exec -T db pg_restore -U fitness -d fitness --clean --if-exists --no-owner --no-acl < backups/postgres/backup-file.dump
docker compose up -d
```

## Overeni po deployi

```bash
docker compose ps
docker compose logs --tail=200 backend
curl -fsS https://rezervace-pankova.online/api/health
```

V GitHub Actions musi byt pro posledni commit uspesne:

| Workflow | Ocekavani |
|---|---|
| CI | success |
| Deploy | success |

## Poznamky k provozu

- Stripe webhook na produkci musi posilat udalost `checkout.session.completed`.
- Testovaci Stripe klice patri pouze do lokalniho prostredi.
- Produkcni Stripe klice patri pouze do GitHub Secrets/server env.
- `tmp-screenshots/` a lokalni logy jsou vyvojove artefakty a necommituji se.
