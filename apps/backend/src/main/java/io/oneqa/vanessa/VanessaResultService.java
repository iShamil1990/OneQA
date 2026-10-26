package io.oneqa.vanessa;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VanessaResultService {
    private final JdbcTemplate jdbc;
    public VanessaResultService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public enum Status { PASSED, FAILED, SKIPPED }
    public enum ArtifactKind { LOG, SCREENSHOT, ERROR, OTHER }
    public record Step(int index, String text, Status status, Long durationMs, String errorMessage) {}
    public record Artifact(ArtifactKind kind, String reference) {}
    public record Scenario(String externalId, String featureName, String scenarioName, Status status,
                           String errorMessage, List<Step> steps, List<Artifact> artifacts) {}
    public record ScenarioSummary(UUID id, String externalId, String featureName, String scenarioName,
                                  Status status, String errorMessage) {}

    @Transactional
    public UUID ingest(UUID agentId, String token, UUID runId, Scenario payload) {
        if (agentId == null || token == null || token.isBlank() || runId == null)
            throw new SecurityException("Agent credentials required");
        if (payload == null || payload.status() == null)
            throw new IllegalArgumentException("Scenario and status required");
        checkText(payload.externalId(), 200, "externalId");
        checkText(payload.featureName(), 300, "featureName");
        checkText(payload.scenarioName(), 300, "scenarioName");
        checkOptional(payload.errorMessage(), 4000, "errorMessage");
        if (payload.steps() == null || payload.steps().size() > 1000 ||
            payload.artifacts() == null || payload.artifacts().size() > 50)
            throw new IllegalArgumentException("Invalid steps or artifacts");
        int authorized = jdbc.queryForObject("""
            SELECT count(*) FROM agents a JOIN execution_runs r ON r.organization_id = a.organization_id
            WHERE a.id = ? AND a.token_hash = encode(sha256(convert_to(?, 'UTF8')), 'hex')
              AND a.revoked_at IS NULL AND r.id = ? AND r.status = 'RUNNING'
            """, Integer.class, agentId, token, runId);
        if (authorized != 1) throw new SecurityException("Agent not authorized for running execution");
        UUID scenarioId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO vanessa_scenarios(id, run_id, agent_id, external_id, feature_name,
                                          scenario_name, status, error_message)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, scenarioId, runId, agentId, payload.externalId(), payload.featureName(),
            payload.scenarioName(), payload.status().name(), payload.errorMessage());
        for (Step step : payload.steps()) {
            if (step == null || step.index() < 0 || step.status() == null ||
                step.durationMs() != null && step.durationMs() < 0)
                throw new IllegalArgumentException("Invalid step");
            checkText(step.text(), 2000, "step text");
            checkOptional(step.errorMessage(), 4000, "step error");
            jdbc.update("""
                INSERT INTO vanessa_steps(id, scenario_id, step_index, step_text, status, duration_ms, error_message)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), scenarioId, step.index(), step.text(), step.status().name(),
                step.durationMs(), step.errorMessage());
        }
        for (Artifact artifact : payload.artifacts()) {
            if (artifact == null || artifact.kind() == null)
                throw new IllegalArgumentException("Invalid artifact");
            checkText(artifact.reference(), 2000, "artifact reference");
            jdbc.update("""
                INSERT INTO vanessa_artifacts(id, scenario_id, kind, reference)
                VALUES (?, ?, ?, ?)
                """, UUID.randomUUID(), scenarioId, artifact.kind().name(), artifact.reference());
        }
        return scenarioId;
    }

    @Transactional(readOnly = true)
    public List<ScenarioSummary> list(UUID organizationId, UUID workspaceId, UUID projectId, UUID runId) {
        return jdbc.query("""
            SELECT s.id, s.external_id, s.feature_name, s.scenario_name, s.status, s.error_message
            FROM vanessa_scenarios s JOIN execution_runs r ON r.id = s.run_id
            WHERE r.id = ? AND r.organization_id = ? AND r.workspace_id = ? AND r.project_id = ?
            ORDER BY s.reported_at, s.id
            """, (rs, row) -> new ScenarioSummary(rs.getObject("id", UUID.class),
                rs.getString("external_id"), rs.getString("feature_name"),
                rs.getString("scenario_name"), Status.valueOf(rs.getString("status")),
                rs.getString("error_message")), runId, organizationId, workspaceId, projectId);
    }

    private static void checkText(String value, int limit, String name) {
        if (value == null || value.isBlank() || value.length() > limit)
            throw new IllegalArgumentException("Invalid " + name);
    }
    private static void checkOptional(String value, int limit, String name) {
        if (value != null && value.length() > limit)
            throw new IllegalArgumentException("Invalid " + name);
    }
}
