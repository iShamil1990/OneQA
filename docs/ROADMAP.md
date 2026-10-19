# OneQA Development Roadmap

Last reconciled: 2026-10-19. Source of truth for completed work: merged GitHub
pull requests and passing CI on `main`. A PR that is open is **not complete**.

## Product direction

OneQA is a test management and execution platform initially focused on 1C.
The product must support **Cloud (SaaS)** and **Enterprise (on-premises)** from
one shared codebase. Enterprise deployments must ultimately support offline
and air-gapped customer networks, without a mandatory external identity
provider or public-cloud dependency.

## Completed — merged to main

| Delivery | Actual scope | Status |
| --- | --- | --- |
| PR-0001 | Repository bootstrap | Completed earlier |
| PR-0002 | Spring Boot / Modulith bootstrap | Completed earlier |
| PR-0003 | Restore backend CI and local security defaults | Merged |
| PR-0004 | PostgreSQL and Flyway integration | Merged |
| PR-0005 | Organization and workspace tenancy foundation | Merged |
| PR-0006 | Memberships and role-based access policy | Merged |
| PR-0007 | JWT resource server and authorized workspace API | Merged |
| PR-0008 | Cloud/Enterprise Compose, Docker image and startup smoke test | Merged |

Passing CI is evidence for the checks performed, **not** proof of production
readiness or complete offline support.

## In progress

| Delivery | Scope | Gate |
| --- | --- | --- |
| PR-0009 | Optional local OIDC/Keycloak, application DB backup and verified restore | Draft; CI and review required |

## Proposed next deliveries (subject to refinement)

| Order | Scope | Acceptance focus |
| --- | --- | --- |
| PR-0010 | Roadmap alignment (this documentation change) | README links to this canonical roadmap |
| PR-0011 | Enterprise recovery and security hardening | Keycloak DB backup, TLS/reverse proxy guidance, secret handling, restore rehearsal |
| PR-0012 | Reproducible offline Enterprise packaging | Pinned image versions/digests, export/import bundle, offline startup verification without external downloads |
| PR-0013 | Projects domain and REST API | Organization-scoped projects, authorization, migrations, tests |
| PR-0014 | Execution model and state transitions | Runs, statuses, lifecycle, tenant isolation, API tests |
| PR-0015 | Agent protocol and registration | Secure agent identity, heartbeats, capabilities, transport contract |
| PR-0016 | Vanessa Automation adapter | Ingest feature/scenario/step outcomes, logs, screenshots, structured errors |
| PR-0017 | Execution orchestration | Dispatch, retries, cancellation, artifact handling and auditing |
| PR-0018 | Frontend MVP | Project/run views, execution history, role-aware UI |
| PR-0019 | First end-to-end 1C/Vanessa test run | Real agent executes a scenario and OneQA shows the result and artifacts |

Numbers are **planning identifiers**, not guaranteed PR numbering or delivery
dates. Scope may be split into smaller PRs as needed.

## Cross-cutting quality gates

1. CI must pass on the exact head commit before merge, and on `main` after merge.
2. Cover authentication, authorization and tenant isolation with negative tests.
3. Validate both Cloud and Enterprise configuration paths for relevant changes.
4. Never require public internet connectivity at runtime for Enterprise offline mode.
5. Test backup **and actual restore**, not only successful archive creation.
6. Keep secrets out of Git, Docker images and application logs.
7. Record explicitly what has **not** been tested (for example, full air-gap,
   production HA, real 1C clients, or external identity integration).
8. Do not merge failing or unreviewed changes.

## Deferred / not yet implemented

Real 1C execution, Jenkins/GitLab adapters, Vanessa integration, frontend MVP,
full offline installation, multi-node HA, SSO federation, production TLS,
complete disaster recovery and observability are not completed by PR-0008.

## Original plan reconciliation

The original README assigned PR-0004 to authentication, PR-0005 to workspace,
PR-0006 to projects, PR-0007 to agent, PR-0008 to executions, PR-0009 to
Vanessa and PR-0010 to an end-to-end run. Those numbers no longer reflect the
actual implementation. This document supersedes that list while retaining the
same long-term product goals.
