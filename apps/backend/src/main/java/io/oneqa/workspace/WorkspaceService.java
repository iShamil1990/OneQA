package io.oneqa.workspace;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceService {
    private final JdbcTemplate jdbc;

    public WorkspaceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public UUID createOrganization(String name) {
        String normalized = requireName(name);
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO organizations (id, name) VALUES (?, ?)", id, normalized);
        return id;
    }

    @Transactional
    public UUID createWorkspace(UUID organizationId, String name) {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        String normalized = requireName(name);
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO workspaces (id, organization_id, name) VALUES (?, ?, ?)",
            id, organizationId, normalized);
        return id;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceSummary> listWorkspaces(UUID organizationId) {
        if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
        return jdbc.query(
            "SELECT id, organization_id, name FROM workspaces WHERE organization_id = ? ORDER BY name, id",
            (rs, rowNum) -> new WorkspaceSummary(
                rs.getObject("id", UUID.class),
                rs.getObject("organization_id", UUID.class),
                rs.getString("name")),
            organizationId);
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        String normalized = name.trim();
        if (normalized.length() > 200) throw new IllegalArgumentException("name is too long");
        return normalized;
    }

    public record WorkspaceSummary(UUID id, UUID organizationId, String name) {}
}
