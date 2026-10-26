package io.oneqa.execution;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.oneqa.bootstrap.OneQaApplication;
import io.oneqa.project.ProjectService;
import io.oneqa.workspace.OrganizationAccessService;
import io.oneqa.workspace.OrganizationAccessService.Role;
import io.oneqa.workspace.WorkspaceService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = OneQaApplication.class)
@AutoConfigureMockMvc
class ExecutionLifecycleIntegrationTest {
    @Autowired ExecutionService runs;
    @Autowired WorkspaceService workspaces;
    @Autowired ProjectService projects;
    @Autowired OrganizationAccessService access;
    @Autowired MockMvc mvc;

    @Test
    void lifecycleIsStrictAndTenantScoped() {
        UUID organization = workspaces.createOrganization("Runs " + UUID.randomUUID());
        UUID workspace = workspaces.createWorkspace(organization, "Main");
        UUID project = projects.create(organization, workspace, "Tests", null);
        UUID run = runs.queue(organization, workspace, project);
        assertEquals(ExecutionService.Status.QUEUED, runs.list(organization, workspace, project).get(0).status());
        assertFalse(runs.transition(organization, workspace, project, run, ExecutionService.Status.PASSED));
        assertTrue(runs.transition(organization, workspace, project, run, ExecutionService.Status.RUNNING));
        assertFalse(runs.transition(UUID.randomUUID(), workspace, project, run, ExecutionService.Status.FAILED));
        assertTrue(runs.transition(organization, workspace, project, run, ExecutionService.Status.PASSED));
        assertFalse(runs.transition(organization, workspace, project, run, ExecutionService.Status.FAILED));
        var result = runs.list(organization, workspace, project).get(0);
        assertEquals(ExecutionService.Status.PASSED, result.status());
        assertNotNull(result.startedAt());
        assertNotNull(result.finishedAt());
        assertThrows(IllegalArgumentException.class,
            () -> runs.queue(UUID.randomUUID(), workspace, project));
    }

    @Test
    void apiEnforcesMembership() throws Exception {
        UUID organization = workspaces.createOrganization("Run API " + UUID.randomUUID());
        UUID workspace = workspaces.createWorkspace(organization, "Main");
        UUID project = projects.create(organization, workspace, "Tests", null);
        UUID member = access.createUser(UUID.randomUUID() + "@example.test");
        UUID admin = access.createUser(UUID.randomUUID() + "@example.test");
        access.addMember(organization, member, Role.MEMBER);
        access.addMember(organization, admin, Role.ADMIN);
        String url = "/api/organizations/{org}/workspaces/{ws}/projects/{project}/runs";
        mvc.perform(get(url, organization, workspace, project)).andExpect(status().isUnauthorized());
        mvc.perform(post(url, organization, workspace, project)
            .with(jwt().jwt(t -> t.subject(member.toString()))))
            .andExpect(status().isForbidden());
        mvc.perform(post(url, organization, workspace, project)
            .with(jwt().jwt(t -> t.subject(admin.toString()))))
            .andExpect(status().isCreated());
        mvc.perform(get(url, organization, workspace, project)
            .with(jwt().jwt(t -> t.subject(member.toString()))))
            .andExpect(status().isOk());
        mvc.perform(get(url, UUID.randomUUID(), workspace, project)
            .with(jwt().jwt(t -> t.subject(member.toString()))))
            .andExpect(status().isForbidden());
    }
}
