package io.oneqa.workspace;

import static org.junit.jupiter.api.Assertions.*;
import io.oneqa.bootstrap.OneQaApplication;
import io.oneqa.workspace.OrganizationAccessService.Permission;
import io.oneqa.workspace.OrganizationAccessService.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = OneQaApplication.class)
class OrganizationAccessServiceIntegrationTest {
    @Autowired OrganizationAccessService access;
    @Autowired WorkspaceService workspaces;

    @Test
    void rolesAndMembershipsAreScopedToOrganization() {
        UUID firstOrg = workspaces.createOrganization("RBAC A " + UUID.randomUUID());
        UUID secondOrg = workspaces.createOrganization("RBAC B " + UUID.randomUUID());
        UUID owner = access.createUser(UUID.randomUUID() + "@example.test");
        UUID member = access.createUser(UUID.randomUUID() + "@example.test");
        access.addMember(firstOrg, owner, Role.OWNER);
        access.addMember(firstOrg, member, Role.MEMBER);

        assertTrue(access.hasPermission(firstOrg, owner, Permission.MANAGE_MEMBERS));
        assertTrue(access.hasPermission(firstOrg, owner, Permission.MANAGE_WORKSPACES));
        assertTrue(access.hasPermission(firstOrg, member, Permission.VIEW_WORKSPACES));
        assertFalse(access.hasPermission(firstOrg, member, Permission.MANAGE_WORKSPACES));
        assertFalse(access.hasPermission(firstOrg, member, Permission.MANAGE_MEMBERS));
        assertFalse(access.hasPermission(secondOrg, owner, Permission.VIEW_WORKSPACES));
        assertThrows(SecurityException.class,
            () -> access.requirePermission(secondOrg, owner, Permission.VIEW_WORKSPACES));
    }

    @Test
    void adminCanManageWorkspacesButNotMemberships() {
        UUID org = workspaces.createOrganization("RBAC Admin " + UUID.randomUUID());
        UUID admin = access.createUser(UUID.randomUUID() + "@example.test");
        access.addMember(org, admin, Role.ADMIN);
        assertTrue(access.hasPermission(org, admin, Permission.MANAGE_WORKSPACES));
        assertFalse(access.hasPermission(org, admin, Permission.MANAGE_MEMBERS));
        assertFalse(access.hasPermission(org, null, Permission.VIEW_WORKSPACES));
    }
}
