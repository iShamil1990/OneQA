package io.oneqa.workspace;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationAccessService {
    private final JdbcTemplate jdbc;

    public OrganizationAccessService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public enum Role { OWNER, ADMIN, MEMBER }
    public enum Permission { VIEW_WORKSPACES, MANAGE_WORKSPACES, MANAGE_MEMBERS }

    @Transactional
    public UUID createUser(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email is required");
        String normalized = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.length() > 320) throw new IllegalArgumentException("email is too long");
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO app_users (id, email) VALUES (?, ?)", id, normalized);
        return id;
    }

    @Transactional
    public void addMember(UUID organizationId, UUID userId, Role role) {
        if (organizationId == null || userId == null || role == null)
            throw new IllegalArgumentException("organization, user and role are required");
        jdbc.update("INSERT INTO organization_memberships (organization_id, user_id, role) VALUES (?, ?, ?)",
            organizationId, userId, role.name());
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(UUID organizationId, UUID userId, Permission permission) {
        if (organizationId == null || userId == null || permission == null) return false;
        return jdbc.query(
            "SELECT role FROM organization_memberships WHERE organization_id = ? AND user_id = ?",
            rs -> {
                if (!rs.next()) return false;
                Role role = Role.valueOf(rs.getString("role"));
                return switch (permission) {
                    case VIEW_WORKSPACES -> true;
                    case MANAGE_WORKSPACES -> role == Role.OWNER || role == Role.ADMIN;
                    case MANAGE_MEMBERS -> role == Role.OWNER;
                };
            }, organizationId, userId);
    }

    public void requirePermission(UUID organizationId, UUID userId, Permission permission) {
        if (!hasPermission(organizationId, userId, permission))
            throw new SecurityException("Access denied");
    }
}
