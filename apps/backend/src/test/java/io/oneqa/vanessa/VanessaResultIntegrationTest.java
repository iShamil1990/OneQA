package io.oneqa.vanessa;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.oneqa.agent.AgentService;
import io.oneqa.bootstrap.OneQaApplication;
import io.oneqa.execution.ExecutionService;
import io.oneqa.project.ProjectService;
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
class VanessaResultIntegrationTest {
    @Autowired AgentService agents;
    @Autowired ExecutionService runs;
    @Autowired VanessaResultService results;
    @Autowired WorkspaceService workspaces;
    @Autowired ProjectService projects;
    @Autowired OrganizationAccessService access;
    @Autowired MockMvc mvc;

    @Test
    void ingestRequiresAgentTokenMatchingTenantAndRunningRun() throws Exception {
        UUID orgId = workspaces.createOrganization("VA " + UUID.randomUUID());
        UUID other = workspaces.createOrganization("Other VA " + UUID.randomUUID());
        UUID workspace = workspaces.createWorkspace(orgId, "Main");
        UUID project = projects.create(orgId, workspace, "Tests", null);
        UUID run = runs.queue(orgId, workspace, project);
        AgentService.Registration agent = agents.register(orgId, "runner-" + UUID.randomUUID(), List.of("vanessa"));
        AgentService.Registration outsider = agents.register(other, "outsider-" + UUID.randomUUID(), List.of("vanessa"));
        String url = "/api/agents/{agentId}/runs/{runId}/vanessa/scenarios";
        String body = """
            {"externalId":"scenario-1","featureName":"Feature","scenarioName":"Test",
             "status":"PASSED","steps":[{"index":0,"text":"Given something","status":"PASSED","durationMs":100}],
             "artifacts":[{"kind":"LOG","reference":"logs/run-1.txt"}]}
            """;
        mvc.perform(post(url, agent.id(), run).header("X-OneQA-Agent-Token", agent.token())
            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        assertTrue(runs.transition(orgId, workspace, project, run, ExecutionService.Status.RUNNING));
        mvc.perform(post(url, agent.id(), run).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
        mvc.perform(post(url, outsider.id(), run).header("X-OneQA-Agent-Token", outsider.token())
            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post(url, agent.id(), run).header("X-OneQA-Agent-Token", agent.token())
            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        assertEquals(1, results.list(orgId, workspace, project, run).size());
        assertTrue(results.list(other, workspace, project, run).isEmpty());
        assertTrue(runs.transition(orgId, workspace, project, run, ExecutionService.Status.PASSED));
        mvc.perform(post(url, agent.id(), run).header("X-OneQA-Agent-Token", agent.token())
            .contentType(MediaType.APPLICATION_JSON).content(body.replace("scenario-1","scenario-2")))
            .andExpect(status().isForbidden());
    }

    @Test
    void readRequiresMembership() throws Exception {
        UUID orgId = workspaces.createOrganization("VA view " + UUID.randomUUID());
        UUID workspace = workspaces.createWorkspace(orgId, "Main");
        UUID project = projects.create(orgId, workspace, "Tests", null);
        UUID run = runs.queue(orgId, workspace, project);
        UUID member = access.createUser(UUID.randomUUID() + "@example.test");
        access.addMember(orgId, member, Role.MEMBER);
        String url = "/api/organizations/{org}/workspaces/{ws}/projects/{project}/runs/{run}/vanessa/scenarios";
        mvc.perform(get(url, orgId, workspace, project, run)).andExpect(status().isUnauthorized());
        mvc.perform(get(url, orgId, workspace, project, run)
            .with(jwt().jwt(t -> t.subject(member.toString())))).andExpect(status().isOk());
        mvc.perform(get(url, UUID.randomUUID(), workspace, project, run)
            .with(jwt().jwt(t -> t.subject(member.toString())))).andExpect(status().isForbidden());
    }
}
