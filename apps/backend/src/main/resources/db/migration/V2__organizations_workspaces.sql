CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT organizations_name_nonblank CHECK (length(trim(name)) > 0)
);

CREATE TABLE workspaces (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT workspaces_name_nonblank CHECK (length(trim(name)) > 0),
    CONSTRAINT workspaces_organization_name_unique UNIQUE (organization_id, name),
    CONSTRAINT workspaces_org_id_unique UNIQUE (organization_id, id)
);

CREATE INDEX idx_workspaces_organization_id ON workspaces(organization_id);
