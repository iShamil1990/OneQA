# API Security — PR-0007

- Stateless OAuth2 JWT resource server; all endpoints require authentication except
  GET /api/system/ping and GET /actuator/health.
- Configure `ONEQA_SECURITY_JWK_SET_URI` with the HTTPS JWK Set URL of the trusted
  identity provider. The default deliberately points to an unavailable local endpoint:
  authentication fails closed until configured.
- JWT `sub` must be a UUID corresponding to `app_users.id`; provisioning / identity
  synchronization is a separate concern. Token signature and expiry are validated by
  Spring Security. Configure `ONEQA_SECURITY_ISSUER` and `ONEQA_SECURITY_AUDIENCE` to match
  the trusted identity provider and the OneQA API resource identifier. The JWT
  validator enforces issuer, audience, expiry, and standard temporal claims.
  Never use the default UNCONFIGURED values in production. Use an HTTPS JWK
  endpoint in production; an internal HTTPS endpoint is supported offline.
- GET /api/organizations/{organizationId}/workspaces: any organization member.
- POST /api/organizations/{organizationId}/workspaces: OWNER or ADMIN.
- A JWT is not sufficient to authorize access: membership and role are checked
  against PostgreSQL on each request.
- Other service methods remain internal and must not be exposed directly.
- This PR does not provide login, registration, invitations, or token issuance.
