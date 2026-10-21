package io.oneqa.execution;

import io.oneqa.workspace.OrganizationAccessService;
import io.oneqa.workspace.OrganizationAccessService.Permission;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations/{organizationId}/workspaces/{workspaceId}/projects/{projectId}/runs")
public class ExecutionApiController {
    private final ExecutionService executions;
    private final OrganizationAccessService access;

    public ExecutionApiController(ExecutionService executions, OrganizationAccessService access) {
        this.executions = executions;
        this.access = access;
    }

    @GetMapping
    public List<ExecutionService.RunSummary> list(@PathVariable UUID organizationId,
            @PathVariable UUID workspaceId, @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        access.requirePermission(organizationId, userId(jwt), Permission.VIEW_WORKSPACES);
        return executions.list(organizationId, workspaceId, projectId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedRun queue(@PathVariable UUID organizationId, @PathVariable UUID workspaceId,
            @PathVariable UUID projectId, @AuthenticationPrincipal Jwt jwt) {
        access.requirePermission(organizationId, userId(jwt), Permission.MANAGE_WORKSPACES);
        return new CreatedRun(executions.queue(organizationId, workspaceId, projectId));
    }

    @PostMapping("/{runId}/transitions")
    public TransitionResult transition(@PathVariable UUID organizationId,
            @PathVariable UUID workspaceId, @PathVariable UUID projectId,
            @PathVariable UUID runId, @RequestBody TransitionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        access.requirePermission(organizationId, userId(jwt), Permission.MANAGE_WORKSPACES);
        return new TransitionResult(executions.transition(
            organizationId, workspaceId, projectId, runId, request.status()));
    }

    private static UUID userId(Jwt jwt) {
        if (jwt == null) throw new SecurityException("Authentication required");
        try { return UUID.fromString(jwt.getSubject()); }
        catch (IllegalArgumentException ex) {
            throw new SecurityException("Token subject must be a user UUID", ex);
        }
    }

    public record CreatedRun(UUID id) {}
    public record TransitionRequest(ExecutionService.Status status) {}
    public record TransitionResult(boolean applied) {}
}
