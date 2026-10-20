#!/usr/bin/env bash
set -euo pipefail
[[ $# -eq 1 ]] || { echo "Usage: $0 /secure/backup-directory" >&2; exit 2; }
[[ -f deploy/.env ]] || { echo "Missing deploy/.env" >&2; exit 1; }
dest="$1"
mkdir -p -- "$dest"
chmod 700 "$dest"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
target="$dest/oneqa-keycloak-$stamp.dump"
[[ ! -e "$target" ]] || { echo "Backup already exists" >&2; exit 1; }
tmp="$(mktemp "$dest/.keycloak-$stamp.XXXXXX")"
trap 'rm -f -- "$tmp"' EXIT
chmod 600 "$tmp"
docker compose --env-file deploy/.env -f deploy/compose.enterprise.yml -f deploy/compose.identity.yml \
  exec -T identity-db pg_dump -U keycloak -d keycloak --format=custom --no-owner --no-acl > "$tmp"
[[ -s "$tmp" ]] || { echo "Empty backup" >&2; exit 1; }
mv -- "$tmp" "$target"
trap - EXIT
echo "Keycloak backup created: $target"
