CREATE TABLE execution_runs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    CONSTRAINT execution_runs_status_check CHECK (status IN ('QUEUED', 'RUNNING', 'PASSED', 'FAILED', 'CANCELLED')),
    CONSTRAINT execution_runs_finished_check CHECK (
        (status IN ('QUEUED','RUNNING') AND finished_at IS NULL)
        OR (status IN ('PASSED','FAILED','CANCELLED') AND finished_at IS NOT NULL)
    ),
    CONSTRAINT execution_runs_start_check CHECK (started_at IS NULL OR started_at >= created_at),
    CONSTRAINT execution_runs_finish_order_check CHECK (finished_at IS NULL OR finished_at >= created_at)
);
CREATE INDEX idx_execution_runs_project_created ON execution_runs(project_id, created_at DESC);
CREATE INDEX idx_execution_runs_tenant ON execution_runs(organization_id, workspace_id);
