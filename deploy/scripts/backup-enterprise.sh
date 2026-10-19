#!/usr/bin/env bash
set -euo pipefail
if [[ $# -ne 1 ]]; then
  echo "Usage: $0 /secure/backup-directory" >&2
  exit 2
fi
dest="$1"
mkdir -p -- "$dest"
chmod 700 "$dest"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
target="$dest/oneqa-$stamp.dump"
if [[ -e "$target" ]]; then
  echo "Backup already exists: $target" >&2
  exit 1
fi
tmp="$(mktemp "$dest/.oneqa-$stamp.XXXXXX")"
trap 'rm -f -- "$tmp"' EXIT
chmod 600 "$tmp"
docker compose --env-file deploy/.env -f deploy/compose.enterprise.yml exec -T postgres \
  pg_dump -U oneqa -d oneqa --format=custom --no-owner --no-acl > "$tmp"
test -s "$tmp" || { echo "Empty backup" >&2; exit 1; }
mv -- "$tmp" "$target"
trap - EXIT
echo "Backup created: $target"
