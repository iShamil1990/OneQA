package io.oneqa.workspace;

import io.oneqa.workspace.OrganizationAccessService.Permission;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations/{organizationId}/workspaces")
public class WorkspaceApiController {
    private final WorkspaceService workspaces;
    private final OrganizationAccessService access;

    public WorkspaceApiController(WorkspaceService workspaces, OrganizationAccessService access) {
        this.workspaces = workspaces;
        this.access = access;
    }

    @GetMapping
    public List<WorkspaceService.WorkspaceSummary> list(
        @PathVariable UUID organizationId, @AuthenticationPrincipal Jwt jwt) {
        access.requirePermission(organizationId, authenticatedUserId(jwt), Permission.VIEW_WORKSPACES);
        return workspaces.listWorkspaces(organizationId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedWorkspace create(@PathVariable UUID organizationId,
                                   @AuthenticationPrincipal Jwt jwt,
                                   @RequestBody CreateWorkspaceRequest request) {
        access.requirePermission(organizationId, authenticatedUserId(jwt), Permission.MANAGE_WORKSPACES);
        return new CreatedWorkspace(workspaces.createWorkspace(organizationId, request.name()));
    }

    private static UUID authenticatedUserId(Jwt jwt) {
        if (jwt == null) throw new SecurityException("Authentication required");
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new SecurityException("Token subject must be a user UUID", ex);
        }
    }

    public record CreateWorkspaceRequest(String name) {}
    public record CreatedWorkspace(UUID id) {}
}
