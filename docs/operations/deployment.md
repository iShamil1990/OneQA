# OneQA deployment (PR-0008 foundation)

The backend and API are identical for Cloud and Enterprise. The Compose files
are single-host deployment foundations, not production-ready HA/TLS/backup solutions.

## Enterprise
1. Copy `deploy/.env.example` to `deploy/.env`; replace placeholders.
2. Configure a reachable local OIDC provider and its HTTPS JWKS URL.
3. Run `docker compose --env-file deploy/.env -f deploy/compose.enterprise.yml up --build -d`.
4. Use an HTTPS reverse proxy; port 8080 binds to localhost.

## Cloud
Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ONEQA_SECURITY_JWK_SET_URI`,
`ONEQA_SECURITY_ISSUER`, and `ONEQA_SECURITY_AUDIENCE`.
Run `docker compose -f deploy/compose.cloud.yml up --build -d`.

## Security and limitations
- Never expose PostgreSQL publicly. Keep secrets out of Git and images.
- Enterprise must use an identity provider hosted within the customer network.
- Offline installations must pre-load container images and Maven dependencies:
  Docker builds currently require a dependency cache or internet access.
- Offline licensing, installation bundles, backups, TLS termination, monitoring,
  image pinning, and full air-gapped end-to-end testing remain future work.
- `ONEQA_DEPLOYMENT_MODE` is informational until validation is implemented.
