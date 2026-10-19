# ADR: OneQA Cloud and OneQA Enterprise deployment modes

Status: Accepted (2026-10-19)

## Decision
OneQA is one product with a shared Java backend, API contracts, database schema,
authorization policies, test orchestration, and frontend. We support two deployment
topologies, without forking application business logic:

- **Cloud (SaaS):** centrally operated multi-organization service.
- **Enterprise (On-Premise):** customer-managed deployment, including air-gapped networks.

## Non-negotiable constraints
1. Core features, migrations, and automated tests must run in both topologies.
2. Enterprise operation must not depend on a public internet connection, external
   cloud authentication, telemetry, license verification, or hosted build services.
3. Authentication must use a configurable trusted identity provider. Cloud may use
   managed OIDC; Enterprise must support a locally reachable OIDC provider and
   optionally an AD/LDAP bridge. The current JWT resource server does **not**
   implement identity-provider provisioning or token issuance.
4. Store tenant data and credentials within the selected deployment. Enforce
   organization-scoped authorization regardless of deployment mode.
5. Agents running 1C / Vanessa Automation must support both locally hosted and
   cloud control planes, with outbound-only connectivity where practical.
6. License validation and updates must have an offline-capable Enterprise path.
7. External integrations and telemetry must be optional, explicit, and disabled
   by default for air-gapped installations.

## Implementation checkpoints
- Define deployment profiles and explicit configuration validation.
- Add integration tests for Cloud and offline Enterprise configurations.
- Add reproducible Docker Compose and installation / backup / restore procedures.
- Test identity-provider outage, offline startup, database migrations, and
  backward-compatible agent / API versions.
- Document secrets management, HTTPS, issuer/audience JWT checks, and upgrade paths.

No feature may claim full dual-deployment support until these checks exist and pass.
