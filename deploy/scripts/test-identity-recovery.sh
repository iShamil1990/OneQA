#!/usr/bin/env bash
set -euo pipefail
[[ "${ONEQA_DESTRUCTIVE_TEST:-}" == "YES" ]] || {
  echo "Disposable CI only: set ONEQA_DESTRUCTIVE_TEST=YES" >&2
  exit 2
}
dir="$(mktemp -d)"
trap 'rm -rf -- "$dir"' EXIT
compose=(docker compose --env-file deploy/.env -f deploy/compose.enterprise.yml -f deploy/compose.identity.yml)
"${compose[@]}" exec -T identity-db psql -v ON_ERROR_STOP=1 -U keycloak -d keycloak \
  -c "CREATE TABLE recovery_probe (value text NOT NULL); INSERT INTO recovery_probe VALUES ('keycloak-restored');"
bash deploy/scripts/backup-identity.sh "$dir"
backup="$(find "$dir" -maxdepth 1 -name '*.dump' -print -quit)"
[[ -n "$backup" ]]
"${compose[@]}" exec -T identity-db psql -v ON_ERROR_STOP=1 -U keycloak -d keycloak \
  -c "DROP TABLE recovery_probe;"
bash deploy/scripts/restore-identity.sh "$backup" --confirm-destroy-data
value="$("${compose[@]}" exec -T identity-db psql -At -v ON_ERROR_STOP=1 -U keycloak -d keycloak \
  -c "SELECT value FROM recovery_probe;")"
[[ "$value" == "keycloak-restored" ]] || { echo "Recovery integrity mismatch" >&2; exit 1; }
echo "Keycloak database recovery verified"
