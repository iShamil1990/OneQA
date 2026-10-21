package io.oneqa.project;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
    private final JdbcTemplate jdbc;

    public ProjectService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public UUID create(UUID organizationId, UUID workspaceId, String name, String description) {
        String normalized = requireName(name);
        if (description != null && description.length() > 2000)
            throw new IllegalArgumentException("description is too long");
        UUID id = UUID.randomUUID();
        int inserted = jdbc.update("""
            INSERT INTO projects (id, organization_id, workspace_id, name, description)
            SELECT ?, w.organization_id, w.id, ?, ?
            FROM workspaces w WHERE w.id = ? AND w.organization_id = ?
            """, id, normalized, description, workspaceId, organizationId);
        if (inserted != 1) throw new IllegalArgumentException("Workspace not found in organization");
        return id;
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> list(UUID organizationId, UUID workspaceId) {
        return jdbc.query("""
            SELECT p.id, p.workspace_id, p.name, p.description
            FROM projects p
            WHERE p.organization_id = ? AND p.workspace_id = ?
            ORDER BY p.name, p.id
            """, (rs, row) -> new ProjectSummary(
                rs.getObject("id", UUID.class),
                rs.getObject("workspace_id", UUID.class),
                rs.getString("name"),
                rs.getString("description")), organizationId, workspaceId);
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        String result = name.trim();
        if (result.length() > 200) throw new IllegalArgumentException("name is too long");
        return result;
    }

    public record ProjectSummary(UUID id, UUID workspaceId, String name, String description) {}
}
