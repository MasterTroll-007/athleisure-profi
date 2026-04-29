#!/bin/sh
set -eu

BACKUP_DIR="${BACKUP_DIR:-/backups}"
BACKUP_INTERVAL_SECONDS="${BACKUP_INTERVAL_SECONDS:-21600}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"
DATABASE_NAME="${PGDATABASE:-fitness}"

mkdir -p "$BACKUP_DIR"

backup_once() {
  timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
  target="$BACKUP_DIR/${DATABASE_NAME}-${timestamp}.dump"
  tmp_target="${target}.tmp"

  echo "Creating PostgreSQL backup: $target"
  if pg_dump --format=custom --no-owner --no-acl --file="$tmp_target"; then
    mv "$tmp_target" "$target"
    chmod 0600 "$target" 2>/dev/null || true
    echo "Backup created: $target"
  else
    rm -f "$tmp_target"
    echo "Backup failed" >&2
    return 1
  fi

  echo "Removing backups older than $BACKUP_RETENTION_DAYS days"
  find "$BACKUP_DIR" -type f -name "*.dump" -mtime +"$BACKUP_RETENTION_DAYS" -print -delete
}

if [ "${1:-}" = "once" ]; then
  backup_once
  exit $?
fi

while true; do
  backup_once || true
  sleep "$BACKUP_INTERVAL_SECONDS"
done
