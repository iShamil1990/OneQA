#!/usr/bin/env bash
set -euo pipefail
# Destructive test intended ONLY for disposable CI environments.
[[ "${ONEQA_DESTRUCTIVE_TEST:-}" == "YES" ]] || {
  echo "Set ONEQA_DESTRUCTIVE_TEST=YES for disposable CI only" >&2
  exit 2
}
dir="$(mktemp -d)"
trap 'rm -rf -- "$dir"' EXIT
bash deploy/scripts/backup-enterprise.sh "$dir"
backup="$(find "$dir" -maxdepth 1 -name '*.dump' -print -quit)"
[[ -n "$backup" ]]
bash deploy/scripts/restore-enterprise.sh "$backup" --confirm-destroy-data
