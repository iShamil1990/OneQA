package io.oneqa.workspace;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.oneqa.bootstrap.OneQaApplication;
import io.oneqa.workspace.OrganizationAccessService.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = OneQaApplication.class)
@AutoConfigureMockMvc
class WorkspaceApiSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired WorkspaceService workspaces;
    @Autowired OrganizationAccessService access;

    @Test
    void deniesAnonymousAndCrossOrganizationRequests() throws Exception {
        UUID org = workspaces.createOrganization("Security A " + UUID.randomUUID());
        UUID other = workspaces.createOrganization("Security B " + UUID.randomUUID());
        UUID user = access.createUser(UUID.randomUUID() + "@example.test");
        access.addMember(org, user, Role.MEMBER);

        mvc.perform(get("/api/organizations/{id}/workspaces", org))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/organizations/{id}/workspaces", org)
            .with(jwt().jwt(token -> token.subject(user.toString()))))
            .andExpect(status().isOk());
        mvc.perform(get("/api/organizations/{id}/workspaces", other)
            .with(jwt().jwt(token -> token.subject(user.toString()))))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/organizations/{id}/workspaces", org)
            .with(jwt().jwt(token -> token.subject(user.toString())))
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Denied\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminToCreateWorkspace() throws Exception {
        UUID org = workspaces.createOrganization("Security Admin " + UUID.randomUUID());
        UUID user = access.createUser(UUID.randomUUID() + "@example.test");
        access.addMember(org, user, Role.ADMIN);
        mvc.perform(post("/api/organizations/{id}/workspaces", org)
            .with(jwt().jwt(token -> token.subject(user.toString())))
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Allowed\"}"))
            .andExpect(status().isCreated());
    }
}
