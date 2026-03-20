#!/bin/bash
# First-time SSL certificate setup for domi-fit.online
# Run this ONCE on the server before starting with HTTPS
#
# Usage: ./init-ssl.sh [email]
# Example: ./init-ssl.sh admin@domi-fit.online

set -e

DOMAIN="domi-fit.online"
EMAIL="${1:-admin@domi-fit.online}"

echo "==> Obtaining SSL certificate for $DOMAIN"

# Start only nginx with HTTP (for ACME challenge)
docker compose up -d nginx

# Request certificate
docker compose run --rm certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email "$EMAIL" \
  --agree-tos \
  --no-eff-email \
  -d "$DOMAIN" \
  -d "www.$DOMAIN"

# Restart everything with HTTPS
docker compose down
docker compose up -d

echo "==> SSL certificate obtained and services started!"
echo "==> Certificate will auto-renew via certbot container."
