#!/bin/sh
set -eu

BACKUP_DIR="${BACKUP_DIR:-/backups}"
BACKUP_INTERVAL_SECONDS="${BACKUP_INTERVAL_SECONDS:-21600}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"
DATABASE_NAME="${PGDATABASE:-fitness}"
BACKUP_OFFSITE_RCLONE_REMOTE="${BACKUP_OFFSITE_RCLONE_REMOTE:-}"

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
    upload_offsite "$target"
  else
    rm -f "$tmp_target"
    echo "Backup failed" >&2
    return 1
  fi

  echo "Removing backups older than $BACKUP_RETENTION_DAYS days"
  find "$BACKUP_DIR" -type f -name "*.dump" -mtime +"$BACKUP_RETENTION_DAYS" -print -delete
}

upload_offsite() {
  backup_file="$1"
  if [ -z "$BACKUP_OFFSITE_RCLONE_REMOTE" ]; then
    return 0
  fi
  if ! command -v rclone >/dev/null 2>&1; then
    echo "Off-site backup remote is configured, but rclone is not installed in this image" >&2
    return 1
  fi

  echo "Uploading PostgreSQL backup off-site: $BACKUP_OFFSITE_RCLONE_REMOTE"
  rclone copy "$backup_file" "$BACKUP_OFFSITE_RCLONE_REMOTE" --checksum
}

if [ "${1:-}" = "once" ]; then
  backup_once
  exit $?
fi

while true; do
  backup_once || true
  sleep "$BACKUP_INTERVAL_SECONDS"
done
