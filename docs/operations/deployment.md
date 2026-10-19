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

## Local identity provider (optional Enterprise overlay)
Add `KEYCLOAK_DB_PASSWORD`, `KEYCLOAK_ADMIN_USERNAME`,
`KEYCLOAK_ADMIN_PASSWORD`, and `KEYCLOAK_HOSTNAME` (an HTTPS URL reachable
by browsers and the backend) to `deploy/.env`. Then run:

```sh
docker compose --env-file deploy/.env -f deploy/compose.enterprise.yml -f deploy/compose.identity.yml up -d
```

Place an HTTPS reverse proxy in front of Keycloak (localhost port 8081).
In Keycloak, **manually** create a realm, users, a client for OneQA and a
JWT audience mapper for `oneqa-api`; set the Enterprise JWKS URI, issuer and
audience to match the configured realm. Keycloak bootstrap admin is for initial
administration only; rotate and secure credentials. No preconfigured realm,
identity federation, or automatic account provisioning is supplied yet.

## PostgreSQL backup and restore (Enterprise)
From the repository root, with `deploy/.env` configured:

```sh
bash deploy/scripts/backup-enterprise.sh /secure/oneqa-backups
bash deploy/scripts/restore-enterprise.sh /secure/oneqa-backups/oneqa-TIMESTAMP.dump --confirm-destroy-data
```

Backup writes a PostgreSQL custom-format archive to a mode-0600 file in a
mode-0700 directory, using a temporary file and atomic rename. Store backups
encrypted off-host and restrict access. Restore is **destructive**, stops
the backend, and leaves it stopped for manual verification. Stop any other
database writers before restoring. Test recovery regularly; the CI test uses
a disposable PostgreSQL database and verifies a restored sentinel record.

These scripts back up **only the OneQA application PostgreSQL database**.
Keycloak's separate PostgreSQL database, object storage, configuration and
secrets require independent backup plans. Do not treat this as complete
disaster recovery or a fully offline installation.
