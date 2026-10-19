package io.oneqa.workspace;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WorkspaceServiceIntegrationTest {
    @Autowired WorkspaceService service;

    @Test
    void isolatesWorkspaceQueriesByOrganization() {
        UUID firstOrg = service.createOrganization("First org " + UUID.randomUUID());
        UUID secondOrg = service.createOrganization("Second org " + UUID.randomUUID());
        UUID firstWorkspace = service.createWorkspace(firstOrg, "QA");
        UUID secondWorkspace = service.createWorkspace(secondOrg, "QA");

        var first = service.listWorkspaces(firstOrg);
        var second = service.listWorkspaces(secondOrg);
        assertEquals(1, first.size());
        assertEquals(1, second.size());
        assertEquals(firstWorkspace, first.getFirst().id());
        assertEquals(secondWorkspace, second.getFirst().id());
        assertEquals(firstOrg, first.getFirst().organizationId());
        assertEquals(secondOrg, second.getFirst().organizationId());
    }

    @Test
    void rejectsInvalidNames() {
        assertThrows(IllegalArgumentException.class, () -> service.createOrganization(" "));
        assertThrows(IllegalArgumentException.class, () -> service.createWorkspace(UUID.randomUUID(), ""));
    }
}
