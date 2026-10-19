#!/usr/bin/env bash
set -euo pipefail
if [[ $# -ne 2 || "$2" != "--confirm-destroy-data" ]]; then
  echo "Usage: $0 /absolute/path/backup.dump --confirm-destroy-data" >&2
  exit 2
fi
backup="$1"
[[ -f "$backup" && -s "$backup" ]] || { echo "Backup missing or empty" >&2; exit 1; }
command -v docker >/dev/null
echo "WARNING: existing OneQA database objects will be replaced."
echo "Stop backend and any other database writers before restoring."
docker compose --env-file deploy/.env -f deploy/compose.enterprise.yml stop backend
docker compose --env-file deploy/.env -f deploy/compose.enterprise.yml exec -T postgres \
  pg_restore -U oneqa -d oneqa --clean --if-exists --no-owner --no-acl --single-transaction < "$backup"
echo "Restore completed. Start backend manually after verifying the database."
