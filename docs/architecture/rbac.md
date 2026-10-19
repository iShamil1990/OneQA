# Organization RBAC (PR-0006)

Initial roles: OWNER, ADMIN, MEMBER.

| Permission | OWNER | ADMIN | MEMBER |
| --- | --- | --- | --- |
| VIEW_WORKSPACES | Yes | Yes | Yes |
| MANAGE_WORKSPACES | Yes | Yes | No |
| MANAGE_MEMBERS | Yes | No | No |

Membership is keyed by (organization_id, user_id). A user can belong to multiple
organizations with different roles. Missing membership denies all permissions.

## Security status

This PR introduces persistence and a policy-checking service, **not end-to-end
authentication or enforced authorization**. No identity provider, sessions, tokens,
password handling, or authenticated HTTP endpoints are implemented. Existing
WorkspaceService methods are not yet guarded; do not expose them to untrusted
callers. A later PR must bind identity to trusted authentication, enforce permission
checks at every entry point, and prevent role escalation (including first OWNER
bootstrap and role changes).
