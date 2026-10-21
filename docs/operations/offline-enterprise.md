# OneQA Enterprise offline container bundle (PR-0012)

This bundle packages container images and Compose configuration for a
single-host Enterprise installation with local Keycloak. Docker Engine,
Compose v2, TLS reverse proxy, DNS, certificates and Keycloak realm/client
configuration must be provided separately.

## Build on a connected machine

From the repository root:

```sh
bash deploy/scripts/build-offline-bundle.sh /secure/output/oneqa-offline.tar.gz
```

This builds the backend and pulls PostgreSQL 17 and Keycloak 26.2.
These version tags are **not immutable image digests**. Pin, scan and
approve image digests before a production release. Distribute the outer
SHA-256 checksum through a trusted independent channel.

## Import and run in the offline network

```sh
sha256sum -c oneqa-offline.tar.gz.sha256
mkdir -p oneqa-offline
tar -xzf oneqa-offline.tar.gz -C oneqa-offline
cd oneqa-offline
bash import-offline-bundle.sh
cp deploy/.env.example deploy/.env
chmod 600 deploy/.env
```

Replace all example secrets and configure local issuer/JWKS, audience,
Keycloak HTTPS hostname, and trusted TLS reverse proxy. Start without
build or image pull:

```sh
docker compose --env-file deploy/.env \
  -f deploy/compose.enterprise.yml \
  -f deploy/compose.identity.yml \
  -f deploy/compose.offline.yml \
  up -d --no-build --pull never
```

The archive preserves repository-relative `deploy/` paths so backup and
restore scripts can be invoked from the extracted bundle root.

## Security and limitations

- Preinstall Docker Engine/Compose on the target host.
- No secrets, realm configuration, certificates, or Docker installer are
  included. Never place production secrets in the archive.
- The backend must reach local OIDC issuer/JWKS; Keycloak requires a
  customer-managed HTTPS proxy and correct DNS.
- `docker image load` does not establish offline runtime readiness.
  CI must exercise no-build/no-pull startup. Full network-isolated
  end-to-end authentication is a separate acceptance test.
- The application and identity PostgreSQL databases must both be backed up.
- Bundle integrity via SHA-256 does not prove publisher authenticity;
  use a trusted distribution and signing process.
