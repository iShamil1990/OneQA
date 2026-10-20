# Enterprise security and recovery checklist

## Scope

This is a single-host operational baseline, **not** production security
certification or proof of air-gapped operation. Keycloak is an optional local
identity provider; its realm, clients, roles, audience and federation require
explicit administrator configuration.

## Before production

1. Provide a customer-controlled HTTPS reverse proxy with valid internal
   certificates for both OneQA and Keycloak. Do not expose backend HTTP (8080)
   or Keycloak HTTP (8081) directly to untrusted networks.
2. Set a stable `KEYCLOAK_HOSTNAME` HTTPS origin; configure
   `ONEQA_SECURITY_ISSUER` and `ONEQA_SECURITY_JWK_SET_URI` for the same realm.
   Verify issuer, audience, token expiration and permissions.
3. Replace bootstrap credentials with long, independent secrets. Store
   `deploy/.env` outside Git with file permissions 0600; rotate credentials.
   For stronger isolation, migrate to orchestrator secrets or a vault:
   environment variables remain visible to Docker administrators.
4. Restrict Docker daemon access, network egress, host filesystem permissions
   and access to backups. Pin and scan container images before deployment.
5. Configure monitoring, retention, encrypted off-host backups and alerting.
   Record recovery-point/recovery-time targets and practice restoration.
6. Back up **both** the application and Keycloak PostgreSQL databases.
   Also preserve TLS keys, realm configuration, deployment configuration,
   and any external artifact/object storage independently.
7. Stop active writers before restoring; application and identity backups
   taken at different times may be inconsistent. Restore both into an
   isolated rehearsal environment before disaster recovery.
8. Verify availability without public internet only after images, certificates,
   dependencies and all required services have been staged locally.

## Keycloak database backup

From repository root, with `deploy/.env` configured and identity-db running:

```sh
bash deploy/scripts/backup-identity.sh /secure/oneqa-backups
```

This produces a custom-format PostgreSQL dump. The backup file is mode 0600
and its directory mode 0700. Encrypt and copy it off-host.

## Keycloak database restore

**Destructive**. Stop clients and all identity database writers first:

```sh
bash deploy/scripts/restore-identity.sh /secure/oneqa-backups/oneqa-keycloak-TIMESTAMP.dump --confirm-destroy-data
```

The script stops the identity service, restores the database transactionally,
and **does not restart identity**. Verify realm/client settings, then start it
manually. It does not restore files, external LDAP/AD or certificates.

The CI test restores a deliberately removed sentinel table in a disposable
database. This verifies dump/restore mechanics, **not** full Keycloak
application-level recovery, production-scale data or coordinated point-in-time
recovery across the two databases.
