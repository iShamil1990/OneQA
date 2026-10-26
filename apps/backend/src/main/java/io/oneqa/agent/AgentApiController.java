package io.oneqa.agent;

import io.oneqa.workspace.OrganizationAccessService;
import io.oneqa.workspace.OrganizationAccessService.Permission;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class AgentApiController {
    private final AgentService agents;
    private final OrganizationAccessService access;

    public AgentApiController(AgentService agents, OrganizationAccessService access) {
        this.agents = agents;
        this.access = access;
    }

    @PostMapping("/api/organizations/{organizationId}/agents")
    @ResponseStatus(HttpStatus.CREATED)
    public AgentService.Registration register(@PathVariable UUID organizationId,
            @RequestBody RegisterAgentRequest request, @AuthenticationPrincipal Jwt jwt) {
        access.requirePermission(organizationId, userId(jwt), Permission.MANAGE_WORKSPACES);
        return agents.register(organizationId, request.name(), request.capabilities());
    }

    @GetMapping("/api/organizations/{organizationId}/agents")
    public List<AgentService.AgentView> list(@PathVariable UUID organizationId,
            @AuthenticationPrincipal Jwt jwt) {
        access.requirePermission(organizationId, userId(jwt), Permission.VIEW_WORKSPACES);
        return agents.list(organizationId);
    }

    @DeleteMapping("/api/organizations/{organizationId}/agents/{agentId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID organizationId,
            @PathVariable UUID agentId, @AuthenticationPrincipal Jwt jwt) {
        access.requirePermission(organizationId, userId(jwt), Permission.MANAGE_WORKSPACES);
        return agents.revoke(organizationId, agentId)
            ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/api/agents/{agentId}/heartbeat")
    public ResponseEntity<Void> heartbeat(@PathVariable UUID agentId,
            @RequestHeader(value = "X-OneQA-Agent-Token", required = false) String token) {
        return agents.heartbeat(agentId, token)
            ? ResponseEntity.noContent().build() : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    private static UUID userId(Jwt jwt) {
        if (jwt == null) throw new SecurityException("Authentication required");
        try { return UUID.fromString(jwt.getSubject()); }
        catch (IllegalArgumentException ex) {
            throw new SecurityException("Token subject must be a user UUID", ex);
        }
    }

    public record RegisterAgentRequest(String name, List<String> capabilities) {}
}
