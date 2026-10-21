package io.oneqa.project;

import io.oneqa.workspace.OrganizationAccessService;
import io.oneqa.workspace.OrganizationAccessService.Permission;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations/{organizationId}/workspaces/{workspaceId}/projects")
public class ProjectApiController {
    private final ProjectService projects;
    private final OrganizationAccessService access;

    public ProjectApiController(ProjectService projects, OrganizationAccessService access) {
        this.projects = projects;
        this.access = access;
    }

    @GetMapping
    public List<ProjectService.ProjectSummary> list(
        @PathVariable UUID organizationId, @PathVariable UUID workspaceId,
        @AuthenticationPrincipal Jwt jwt) {
        access.requirePermission(organizationId, userId(jwt), Permission.VIEW_WORKSPACES);
        return projects.list(organizationId, workspaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedProject create(
        @PathVariable UUID organizationId, @PathVariable UUID workspaceId,
        @AuthenticationPrincipal Jwt jwt, @RequestBody CreateProjectRequest request) {
        access.requirePermission(organizationId, userId(jwt), Permission.MANAGE_WORKSPACES);
        return new CreatedProject(projects.create(
            organizationId, workspaceId, request.name(), request.description()));
    }

    private static UUID userId(Jwt jwt) {
        if (jwt == null) throw new SecurityException("Authentication required");
        try { return UUID.fromString(jwt.getSubject()); }
        catch (IllegalArgumentException ex) {
            throw new SecurityException("Token subject must be a user UUID", ex);
        }
    }

    public record CreateProjectRequest(String name, String description) {}
    public record CreatedProject(UUID id) {}
}
