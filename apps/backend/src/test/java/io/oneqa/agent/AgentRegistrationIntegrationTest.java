package io.oneqa.agent;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.oneqa.bootstrap.OneQaApplication;
import io.oneqa.workspace.OrganizationAccessService;
import io.oneqa.workspace.OrganizationAccessService.Role;
import io.oneqa.workspace.WorkspaceService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = OneQaApplication.class)
@AutoConfigureMockMvc
class AgentRegistrationIntegrationTest {
    @Autowired AgentService agents;
    @Autowired WorkspaceService workspaces;
    @Autowired OrganizationAccessService access;
    @Autowired MockMvc mvc;

    @Test
    void credentialsHeartbeatRevocationAndTenantIsolation() throws Exception {
        UUID organization = workspaces.createOrganization("Agents " + UUID.randomUUID());
        UUID other = workspaces.createOrganization("Other agents " + UUID.randomUUID());
        AgentService.Registration registration = agents.register(organization, "runner-1", List.of("vanessa", "windows"));
        assertEquals(43, registration.token().length());
        assertEquals(2, agents.list(organization).get(0).capabilities().size());
        assertTrue(agents.list(other).isEmpty());
        String url = "/api/agents/{agentId}/heartbeat";
        mvc.perform(post(url, registration.id())).andExpect(status().isUnauthorized());
        mvc.perform(post(url, registration.id()).header("X-OneQA-Agent-Token", "incorrect"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post(url, registration.id()).header("X-OneQA-Agent-Token", registration.token()))
            .andExpect(status().isNoContent());
        assertNotNull(agents.list(organization).get(0).lastSeenAt());
        assertFalse(agents.revoke(other, registration.id()));
        assertTrue(agents.revoke(organization, registration.id()));
        mvc.perform(post(url, registration.id()).header("X-OneQA-Agent-Token", registration.token()))
            .andExpect(status().isUnauthorized());
        assertFalse(agents.revoke(organization, registration.id()));
    }

    @Test
    void registrationRequiresAdminAndTokenIsNotListed() throws Exception {
        UUID organization = workspaces.createOrganization("Agent RBAC " + UUID.randomUUID());
        UUID member = access.createUser(UUID.randomUUID() + "@example.test");
        UUID admin = access.createUser(UUID.randomUUID() + "@example.test");
        access.addMember(organization, member, Role.MEMBER);
        access.addMember(organization, admin, Role.ADMIN);
        String url = "/api/organizations/{organizationId}/agents";
        String payload = "{\"name\":\"win-runner\",\"capabilities\":[\"vanessa\",\"1c\"]}";
        mvc.perform(post(url, organization).contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isUnauthorized());
        mvc.perform(post(url, organization).with(jwt().jwt(t -> t.subject(member.toString())))
            .contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isForbidden());
        mvc.perform(post(url, organization).with(jwt().jwt(t -> t.subject(admin.toString())))
            .contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.token").isString());
        mvc.perform(get(url, organization).with(jwt().jwt(t -> t.subject(member.toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].token").doesNotExist());
        mvc.perform(get(url, UUID.randomUUID()).with(jwt().jwt(t -> t.subject(member.toString()))))
            .andExpect(status().isForbidden());
    }
}
