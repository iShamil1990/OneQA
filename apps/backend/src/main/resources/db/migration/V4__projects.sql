CREATE TABLE projects (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT projects_name_nonblank CHECK (length(trim(name)) > 0),
    CONSTRAINT projects_workspace_fk FOREIGN KEY (organization_id, workspace_id)
        REFERENCES workspaces(organization_id, id) ON DELETE CASCADE,
    CONSTRAINT projects_workspace_name_unique UNIQUE (workspace_id, name)
);
CREATE INDEX idx_projects_workspace_id ON projects(workspace_id);
CREATE INDEX idx_projects_organization_id ON projects(organization_id);
