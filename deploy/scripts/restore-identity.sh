#!/usr/bin/env bash
set -euo pipefail
[[ $# -eq 2 && "$2" == "--confirm-destroy-data" ]] || {
  echo "Usage: $0 /path/keycloak.dump --confirm-destroy-data" >&2
  exit 2
}
[[ -f deploy/.env ]] || { echo "Missing deploy/.env" >&2; exit 1; }
[[ -f "$1" && -s "$1" ]] || { echo "Backup missing or empty" >&2; exit 1; }
compose=(docker compose --env-file deploy/.env -f deploy/compose.enterprise.yml -f deploy/compose.identity.yml)
echo "WARNING: replacing Keycloak data. Existing sessions and identity state may be lost."
"${compose[@]}" stop identity
"${compose[@]}" exec -T identity-db pg_restore -U keycloak -d keycloak \
  --clean --if-exists --no-owner --no-acl --single-transaction < "$1"
echo "Keycloak restore completed. Review realm/client configuration before restarting identity."
