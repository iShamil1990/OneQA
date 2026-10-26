CREATE TABLE agents (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    capabilities TEXT NOT NULL DEFAULT '',
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT agents_name_nonblank CHECK (length(trim(name)) > 0),
    CONSTRAINT agents_name_unique UNIQUE (organization_id, name)
);
CREATE INDEX idx_agents_organization ON agents(organization_id);
