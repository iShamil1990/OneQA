ALTER TABLE execution_runs ADD COLUMN agent_id UUID REFERENCES agents(id);
CREATE INDEX idx_execution_runs_dispatch ON execution_runs(organization_id, created_at, id)
    WHERE status = 'QUEUED';
CREATE UNIQUE INDEX idx_execution_runs_active_agent ON execution_runs(agent_id)
    WHERE status = 'RUNNING' AND agent_id IS NOT NULL;
