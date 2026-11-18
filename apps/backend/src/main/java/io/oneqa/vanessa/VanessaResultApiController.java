package io.oneqa.vanessa;

import io.oneqa.workspace.OrganizationAccessService;
import io.oneqa.workspace.OrganizationAccessService.Permission;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class VanessaResultApiController {
    private final VanessaResultService results;
    private final OrganizationAccessService access;
    public VanessaResultApiController(VanessaResultService results, OrganizationAccessService access) {
        this.results = results;
        this.access = access;
    }

    @PostMapping("/api/agents/{agentId}/runs/{runId}/vanessa/scenarios")
    @ResponseStatus(HttpStatus.CREATED)
    public Ingested ingest(@PathVariable UUID agentId, @PathVariable UUID runId,
            @RequestHeader(value = "X-OneQA-Agent-Token", required = false) String token,
            @RequestBody VanessaResultService.Scenario scenario) {
        return new Ingested(results.ingest(agentId, token, runId, scenario));
    }

    @GetMapping("/api/organizations/{organizationId}/workspaces/{workspaceId}/projects/{projectId}/runs/{runId}/vanessa/scenarios")
    public List<VanessaResultService.ScenarioSummary> list(
            @PathVariable UUID organizationId, @PathVariable UUID workspaceId,
            @PathVariable UUID projectId, @PathVariable UUID runId,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) throw new SecurityException("Authentication required");
        UUID user;
        try { user = UUID.fromString(jwt.getSubject()); }
        catch (IllegalArgumentException ex) { throw new SecurityException("Invalid user subject", ex); }
        access.requirePermission(organizationId, user, Permission.VIEW_WORKSPACES);
        return results.list(organizationId, workspaceId, projectId, runId);
    }
    public record Ingested(UUID id) {}
}
