#!/usr/bin/env bash
set -euo pipefail
[[ $# -eq 1 ]] || { echo "Usage: $0 /absolute/path/oneqa-offline-bundle.tar.gz" >&2; exit 2; }
output="$1"
[[ ! -e "$output" ]] || { echo "Refusing to overwrite $output" >&2; exit 1; }
command -v docker >/dev/null
command -v sha256sum >/dev/null
tmp="$(mktemp -d)"
trap 'rm -rf -- "$tmp"' EXIT
docker build --pull -t oneqa-backend:offline apps/backend
docker pull postgres:17
docker pull quay.io/keycloak/keycloak:26.2
docker tag postgres:17 oneqa-postgres:17
docker tag quay.io/keycloak/keycloak:26.2 oneqa-keycloak:26.2
docker image save -o "$tmp/images.tar" \
  oneqa-backend:offline oneqa-postgres:17 oneqa-keycloak:26.2
cp deploy/compose.enterprise.yml deploy/compose.identity.yml deploy/compose.offline.yml "$tmp/"
cp deploy/.env.example "$tmp/env.example"
mkdir -p "$tmp/scripts"
cp deploy/scripts/backup-enterprise.sh deploy/scripts/restore-enterprise.sh \
  deploy/scripts/backup-identity.sh deploy/scripts/restore-identity.sh "$tmp/scripts/"
cp deploy/scripts/import-offline-bundle.sh "$tmp/import-offline-bundle.sh"
cp docs/operations/offline-enterprise.md "$tmp/README.md"
(cd "$tmp" && sha256sum images.tar compose.enterprise.yml compose.identity.yml compose.offline.yml env.example scripts/* import-offline-bundle.sh README.md > SHA256SUMS)
mkdir -p "$(dirname "$output")"
tar -C "$tmp" -czf "$output" .
sha256sum "$output" > "$output.sha256"
echo "Offline bundle: $output"
