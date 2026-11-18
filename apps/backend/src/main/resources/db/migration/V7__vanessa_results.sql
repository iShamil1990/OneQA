CREATE TABLE vanessa_scenarios (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES execution_runs(id) ON DELETE CASCADE,
    agent_id UUID NOT NULL REFERENCES agents(id),
    external_id VARCHAR(200) NOT NULL,
    feature_name VARCHAR(300) NOT NULL,
    scenario_name VARCHAR(300) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PASSED','FAILED','SKIPPED')),
    error_message VARCHAR(4000),
    reported_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT vanessa_scenario_unique UNIQUE (run_id, external_id)
);
CREATE INDEX idx_vanessa_scenarios_run ON vanessa_scenarios(run_id);
CREATE TABLE vanessa_steps (
    id UUID PRIMARY KEY,
    scenario_id UUID NOT NULL REFERENCES vanessa_scenarios(id) ON DELETE CASCADE,
    step_index INTEGER NOT NULL CHECK (step_index >= 0),
    step_text VARCHAR(2000) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PASSED','FAILED','SKIPPED')),
    duration_ms BIGINT CHECK (duration_ms >= 0),
    error_message VARCHAR(4000),
    CONSTRAINT vanessa_step_unique UNIQUE (scenario_id, step_index)
);
CREATE TABLE vanessa_artifacts (
    id UUID PRIMARY KEY,
    scenario_id UUID NOT NULL REFERENCES vanessa_scenarios(id) ON DELETE CASCADE,
    kind VARCHAR(32) NOT NULL CHECK (kind IN ('LOG','SCREENSHOT','ERROR','OTHER')),
    reference VARCHAR(2000) NOT NULL
);
