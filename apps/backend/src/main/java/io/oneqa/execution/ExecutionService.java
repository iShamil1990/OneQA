package io.oneqa.execution;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecutionService {
    private final JdbcTemplate jdbc;
    public ExecutionService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public enum Status { QUEUED, RUNNING, PASSED, FAILED, CANCELLED }

    @Transactional
    public UUID queue(UUID organizationId, UUID workspaceId, UUID projectId) {
        UUID id = UUID.randomUUID();
        int count = jdbc.update("""
            INSERT INTO execution_runs(id, organization_id, workspace_id, project_id)
            SELECT ?, p.organization_id, p.workspace_id, p.id FROM projects p
            WHERE p.id = ? AND p.organization_id = ? AND p.workspace_id = ?
            """, id, projectId, organizationId, workspaceId);
        if (count != 1) throw new IllegalArgumentException("Project not found in workspace");
        return id;
    }

    @Transactional(readOnly = true)
    public List<RunSummary> list(UUID organizationId, UUID workspaceId, UUID projectId) {
        return jdbc.query("""
            SELECT id, project_id, status, created_at, started_at, finished_at
            FROM execution_runs
            WHERE organization_id = ? AND workspace_id = ? AND project_id = ?
            ORDER BY created_at DESC, id DESC
            """, (rs, row) -> new RunSummary(
                rs.getObject("id", UUID.class), rs.getObject("project_id", UUID.class),
                Status.valueOf(rs.getString("status")),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("started_at", OffsetDateTime.class),
                rs.getObject("finished_at", OffsetDateTime.class)),
            organizationId, workspaceId, projectId);
    }

    @Transactional
    public boolean transition(UUID organizationId, UUID workspaceId, UUID projectId,
                              UUID runId, Status target) {
        if (target == null || target == Status.QUEUED)
            throw new IllegalArgumentException("Invalid target status");
        String previous = switch (target) {
            case RUNNING -> "QUEUED";
            case PASSED, FAILED -> "RUNNING";
            case CANCELLED -> "QUEUED";
            default -> throw new IllegalArgumentException("Invalid target status");
        };
        int updated = jdbc.update("""
            UPDATE execution_runs SET status = ?,
                started_at = CASE WHEN ? = 'RUNNING' THEN now() ELSE started_at END,
                finished_at = CASE WHEN ? IN ('PASSED','FAILED','CANCELLED') THEN now() ELSE finished_at END
            WHERE id = ? AND organization_id = ? AND workspace_id = ? AND project_id = ?
              AND status = ?
            """, target.name(), target.name(), target.name(), runId,
            organizationId, workspaceId, projectId, previous);
        return updated == 1;
    }

    public record RunSummary(UUID id, UUID projectId, Status status, OffsetDateTime createdAt,
                             OffsetDateTime startedAt, OffsetDateTime finishedAt) {}
}
