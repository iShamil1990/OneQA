# Tenant isolation (initial foundation)

An organization is the tenant boundary. A workspace belongs to exactly one organization.
The initial service API requires an explicit organization ID for workspace queries, and
the database enforces organization ownership via foreign keys and uniqueness constraints.

**Security limitation:** This is a data-model foundation, **not authentication or authorization**.
The organization ID is not yet derived from an authenticated identity. Do not expose
these service methods through public endpoints until access control and membership checks
are implemented. A caller-supplied organization ID alone is not a security boundary.

Next: users, organization membership, roles, Spring Security, and tenant-scoped
authorization tests. All tenant-owned future tables must include an organization ID,
with composite constraints where cross-tenant references could otherwise occur.
