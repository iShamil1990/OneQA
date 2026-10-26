# Agent registration protocol (PR-0015)

This is the initial management/control-plane contract for OneQA agents. It is **not** the execution dispatch protocol.

## Management (user JWT)

- `POST /api/organizations/{organizationId}/agents` (OWNER/ADMIN): JSON `{"name":"win-01","capabilities":["vanessa","windows"]}`. Returns `201` with `{"id":"UUID","token":"secret"}`. The token is returned **only once**.
- `GET /api/organizations/{organizationId}/agents` (any organization member): lists agents, capabilities, timestamps and revocation state; **never** returns tokens.
- `DELETE /api/organizations/{organizationId}/agents/{agentId}` (OWNER/ADMIN): revokes credentials. Returns `204` or `404`.

## Agent heartbeat (agent credential)

- `POST /api/agents/{agentId}/heartbeat` with `X-OneQA-Agent-Token: <registration token>`.
- Returns `204` and updates `last_seen_at` if credentials are valid and not revoked; otherwise `401`.
- The endpoint is exempt from user-JWT authentication **only** to allow its own per-agent credential check.
- Use HTTPS; do not log the registration token or include it in URLs. Store it securely on the agent host.
- Only SHA-256 hashes of 256-bit random agent tokens are stored in the database. This is appropriate for high-entropy random secrets, not user passwords.
- For offline Enterprise deployments, credentials and heartbeats remain local to the OneQA instance.

## Not yet implemented

Agent-to-run assignment, capabilities negotiation/versioning, rotation, request signing, mTLS, rate limiting, expiration, distributed leases, heartbeat liveness alerts, and execution results. These belong to subsequent PRs. A production deployment should enforce TLS and apply ingress rate limits.
