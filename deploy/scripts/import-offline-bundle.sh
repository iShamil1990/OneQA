#!/usr/bin/env bash
set -euo pipefail
# Run from the extracted bundle directory.
[[ -f images.tar && -f SHA256SUMS ]] || {
  echo "Run from extracted offline bundle directory" >&2
  exit 2
}
sha256sum -c SHA256SUMS
docker image load -i images.tar
for name in oneqa-backend:offline oneqa-postgres:17 oneqa-keycloak:26.2; do
  docker image inspect "$name" >/dev/null
done
echo "Images imported. Configure .env and run the offline Compose overlay."
