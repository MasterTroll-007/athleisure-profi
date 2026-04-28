# Deployment Guide

## Prerekvizity na serveru

- Docker + Docker Compose
- Doména `rezervace-pankova.online` nasměrovaná na IP serveru
- Otevřené porty 80 a 443

## 1. GitHub Secrets

V repozitáři nastav secrets: **Settings > Secrets and variables > Actions > New repository secret**

| Secret | Popis |
|---|---|
| `DOCKERHUB_USERNAME` | Docker Hub username (`jveselka`) |
| `DOCKERHUB_TOKEN` | Docker Hub access token (vytvoříš na https://hub.docker.com/settings/security) |
| `SERVER_HOST` | IP adresa nebo hostname serveru |
| `SERVER_USER` | SSH uživatel na serveru (např. `root` nebo `deploy`) |
| `SERVER_SSH_KEY` | Obsah privátního SSH klíče pro přístup na server |

## 2. Prvotní setup serveru

```bash
# Připoj se na server
ssh user@server-ip

# Vytvoř adresář
mkdir -p ~/fitness-app
cd ~/fitness-app

# Nahraj soubory (nebo se naklonují přes GitHub Actions automaticky)
# docker-compose.yml, init.sql, nginx/nginx.conf, init-ssl.sh

# Vytvoř .env soubor s produkčními hodnotami
cat > .env << 'EOF'
DB_PASSWORD=silne-heslo-pro-databazi
JWT_SECRET=min-32-znaku-nahodny-retezec-pro-jwt
SMTP_USER=tvuj@gmail.com
SMTP_PASSWORD=app-password-z-google
APP_BASE_URL=https://rezervace-pankova.online
STRIPE_SECRET_KEY=sk_live_...
STRIPE_PUBLISHABLE_KEY=pk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
EOF

# Získej SSL certifikát (jednorázově)
chmod +x init-ssl.sh
./init-ssl.sh tvuj@email.com
```

## 3. Automatický deploy

Po nastavení secrets se deploy spouští automaticky při každém push do `main`:

1. GitHub Actions buildne Docker images (backend + frontend)
2. Pushne je na Docker Hub jako `jveselka/fitness-backend:latest` a `jveselka/fitness-frontend:latest`
3. Přes SSH na serveru stáhne nové images a restartuje kontejnery

## 4. Manuální deploy (bez CI/CD)

```bash
ssh user@server-ip
cd ~/fitness-app
docker compose pull
docker compose up -d --remove-orphans
docker image prune -f
```

## 5. Správa

```bash
# Logy
docker compose logs -f              # všechny služby
docker compose logs -f backend      # jen backend
docker compose logs -f nginx        # jen nginx

# Restart
docker compose restart backend
docker compose restart nginx

# Status
docker compose ps

# Databáze
docker compose exec db psql -U fitness -d fitness

# Záloha databáze
docker compose exec db pg_dump -U fitness fitness > backup_$(date +%Y%m%d).sql

# Obnova ze zálohy
cat backup.sql | docker compose exec -T db psql -U fitness -d fitness
```

## Architektura

```
Internet
  │
  ├── :80  ──→ Nginx (redirect → HTTPS)
  └── :443 ──→ Nginx (SSL termination)
                 ├── /api/* ──→ Backend (:8080) ──→ PostgreSQL (:5432)
                 └── /*     ──→ Frontend (:80)

Volumes:
  postgres_data  – data databáze
  uploads_data   – avatary, PDF plány, preview obrázky
  certbot_certs  – SSL certifikáty (auto-renew)
```

## SSL certifikát

Certifikát se obnovuje automaticky přes certbot kontejner (každých 12h kontrola). Platnost certifikátu je 90 dní.

Ruční obnova:
```bash
docker compose run --rm certbot renew
docker compose restart nginx
```
