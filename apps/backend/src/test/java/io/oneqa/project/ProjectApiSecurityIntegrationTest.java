package io.oneqa.project;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.oneqa.bootstrap.OneQaApplication;
import io.oneqa.workspace.OrganizationAccessService;
import io.oneqa.workspace.OrganizationAccessService.Role;
import io.oneqa.workspace.WorkspaceService;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = OneQaApplication.class)
@AutoConfigureMockMvc
class ProjectApiSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired WorkspaceService workspaces;
    @Autowired OrganizationAccessService access;
    @Autowired ProjectService projects;

    @Test
    void enforcesMembershipAndRoles() throws Exception {
        UUID org = workspaces.createOrganization("Projects org " + UUID.randomUUID());
        UUID other = workspaces.createOrganization("Projects other " + UUID.randomUUID());
        UUID ws = workspaces.createWorkspace(org, "Projects workspace");
        UUID member = access.createUser(UUID.randomUUID() + "@example.test");
        UUID admin = access.createUser(UUID.randomUUID() + "@example.test");
        access.addMember(org, member, Role.MEMBER);
        access.addMember(org, admin, Role.ADMIN);
        String url = "/api/organizations/{org}/workspaces/{ws}/projects";

        mvc.perform(get(url, org, ws)).andExpect(status().isUnauthorized());
        mvc.perform(get(url, other, ws)
            .with(jwt().jwt(t -> t.subject(member.toString()))))
            .andExpect(status().isForbidden());
        mvc.perform(post(url, org, ws)
            .with(jwt().jwt(t -> t.subject(member.toString())))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Not allowed\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post(url, org, ws)
            .with(jwt().jwt(t -> t.subject(admin.toString())))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Allowed\",\"description\":\"Project test\"}"))
            .andExpect(status().isCreated());
        mvc.perform(get(url, org, ws)
            .with(jwt().jwt(t -> t.subject(member.toString()))))
            .andExpect(status().isOk());
    }

    @Test
    void cannotCreateProjectInAnotherOrganizationsWorkspace() {
        UUID org = workspaces.createOrganization("Projects A " + UUID.randomUUID());
        UUID other = workspaces.createOrganization("Projects B " + UUID.randomUUID());
        UUID ws = workspaces.createWorkspace(other, "Other workspace");
        assertThrows(IllegalArgumentException.class,
            () -> projects.create(org, ws, "Escape", null));
        assertTrue(projects.list(org, ws).isEmpty());
    }
}
