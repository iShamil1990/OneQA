#!/usr/bin/env bash
set -euo pipefail
# Destructive test intended ONLY for disposable CI environments.
[[ "${ONEQA_DESTRUCTIVE_TEST:-}" == "YES" ]] || {
  echo "Set ONEQA_DESTRUCTIVE_TEST=YES for disposable CI only" >&2
  exit 2
}
dir="$(mktemp -d)"
trap 'rm -rf -- "$dir"' EXIT
compose=(docker compose --env-file deploy/.env -f deploy/compose.enterprise.yml)
"${compose[@]}" exec -T postgres psql -v ON_ERROR_STOP=1 -U oneqa -d oneqa \
  -c "CREATE TABLE recovery_probe (value text NOT NULL); INSERT INTO recovery_probe VALUES ('verified');"
bash deploy/scripts/backup-enterprise.sh "$dir"
backup="$(find "$dir" -maxdepth 1 -name '*.dump' -print -quit)"
[[ -n "$backup" ]]
"${compose[@]}" exec -T postgres psql -v ON_ERROR_STOP=1 -U oneqa -d oneqa \
  -c "DROP TABLE recovery_probe;"
bash deploy/scripts/restore-enterprise.sh "$backup" --confirm-destroy-data
result="$("${compose[@]}" exec -T postgres psql -At -v ON_ERROR_STOP=1 -U oneqa -d oneqa \
  -c "SELECT value FROM recovery_probe;")"
[[ "$result" == "verified" ]] || { echo "Restored data does not match" >&2; exit 1; }
echo "Backup/restore data integrity verified"
