# OneQA Enterprise offline container bundle (PR-0012)

This bundle provides **container images and Compose configuration** for a
single-host Enterprise installation with optional local Keycloak. It does not
install Docker, provide TLS certificates, configure a Keycloak realm, or
include a licensed production support agreement.

## Prepare on a connected build machine

Install Docker Engine with Compose v2, Bash, tar and sha256sum. From the
repository root, run:

```sh
bash deploy/scripts/build-offline-bundle.sh /secure/output/oneqa-offline.tar.gz
```

The script builds the backend image and downloads PostgreSQL 17 and
Keycloak 26.2 images. These tags are **not immutable digests**; capture,
scan, approve and pin image digests for production release management.
The outer .sha256 file must be distributed through a trusted channel.
No credentials are included in the archive.

## Import inside the offline network

Verify the bundle checksum from the trusted channel before extracting:

```sh
sha256sum -c oneqa-offline.tar.gz.sha256
mkdir -p oneqa-offline
tar -xzf oneqa-offline.tar.gz -C oneqa-offline
cd oneqa-offline
bash import-offline-bundle.sh
cp env.example .env
chmod 600 .env
```

Edit .env to replace all example secrets, Keycloak HTTPS hostname and
OneQA JWT issuer/JWKS/audience. A reachable local TLS reverse proxy and
correctly configured Keycloak realm/client are prerequisites for login.

Run with **no image pulling** (both files are required):

```sh
docker compose --env-file .env -f compose.enterprise.yml -f compose.identity.yml -f compose.offline.yml up -d --no-build --pull never
```

For Enterprise without the bundled Keycloak service, use only the Enterprise
file plus a backend-only overlay; the supplied offline overlay references
identity services and is intended for the full local identity stack.

## Limits and verification

- Docker Engine and Compose must already be installed on the target host.
- The backend container must reach the configured issuer/JWKS over the
  customer network; localhost-only identity HTTP requires a properly
  configured HTTPS proxy and DNS.
- Application, Keycloak and TLS secrets must be provisioned separately.
- Backup/restore scripts in this archive assume repository-relative
  `deploy/.env` paths and therefore must be adapted or run from the
  repository layout. They are included as reference utilities.
- Image import does not itself prove offline runtime behavior.
- CI should verify container startup with preloaded images, no builds,
  and no pulls; network isolation and end-to-end authentication still
  require dedicated validation.
