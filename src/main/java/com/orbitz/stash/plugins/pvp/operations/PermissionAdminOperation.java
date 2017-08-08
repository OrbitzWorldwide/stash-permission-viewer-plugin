package com.orbitz.stash.plugins.pvp.operations;

import java.util.*;
import com.atlassian.soy.renderer.SoyException;
import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.user.*;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.permission.PermittedGroup;
import com.atlassian.bitbucket.permission.PermittedUser;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.bitbucket.util.PageRequestImpl;
import com.atlassian.bitbucket.util.UncheckedOperation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.permission.PermissionAdminService;
import com.atlassian.bitbucket.user.UserService;
import com.atlassian.soy.renderer.SoyTemplateRenderer;


/**
 * Secure operation that retrieves all of the groups and users that have permissions applied to the repository
 */
public class PermissionAdminOperation implements UncheckedOperation<Map<Permission, List<String>>> {

    private PermissionAdminService permissionAdminService;
    private UserService userService;
    private Project project;
    private Repository repository;

    public PermissionAdminOperation(
        PermissionAdminService permissionAdminService,
        UserService userService,
        Project project
    ) {
        this.permissionAdminService = permissionAdminService;
        this.userService = userService;
        this.project = project;
        this.repository = null;
    }

    public PermissionAdminOperation(PermissionAdminService permissionAdminService, UserService userService, Project project, Repository repository) {
        this.permissionAdminService = permissionAdminService;
        this.userService = userService;
        this.project = project;
        this.repository = repository;
    }

    /**
     * Perform the secure operations to retrieve all groups and users that have permissions applied to the repository
     * @return
     */
    public Map<Permission, List<String>> perform() {

        Map<Permission, List<String>> identityMap = new HashMap<Permission, List<String>>();


        PageRequest pageRequest = new PageRequestImpl(0, 1000);

        // Project level permissions
        Page<PermittedUser> permittedUserProjectPage =
                permissionAdminService.findUsersWithProjectPermission(project, null, pageRequest);
        Page<PermittedGroup> permittedGroupProjectPage =
                permissionAdminService.findGroupsWithProjectPermission(project, null, pageRequest);

        identityMap.put(Permission.PROJECT_ADMIN, new java.util.ArrayList<String>());
        identityMap.put(Permission.PROJECT_WRITE, new java.util.ArrayList<String>());
        identityMap.put(Permission.PROJECT_READ, new java.util.ArrayList<String>());
        identityMap.put(Permission.REPO_ADMIN, new java.util.ArrayList<String>());
        identityMap.put(Permission.REPO_WRITE, new java.util.ArrayList<String>());
        identityMap.put(Permission.REPO_READ, new java.util.ArrayList<String>());

        for (PermittedGroup pg : permittedGroupProjectPage.getValues()) {
            identityMap.get(pg.getPermission()).add(buildGroupString(pg, pageRequest));
        }
        for (PermittedUser pu : permittedUserProjectPage.getValues()) {
            identityMap.get(pu.getPermission()).add(pu.getUser().getDisplayName());
        }


        // Repository level permissions
        if (repository != null) {

            Page<PermittedUser> permittedUserRepositoryPage =
                    permissionAdminService.findUsersWithRepositoryPermission(repository, null, pageRequest);
            Page<PermittedGroup> permittedGroupRepositoryPage =
                    permissionAdminService.findGroupsWithRepositoryPermission(repository, null, pageRequest);

            for (PermittedGroup pg : permittedGroupRepositoryPage.getValues()) {
                identityMap.get(pg.getPermission()).add(buildGroupString(pg, pageRequest));
            }

            for (PermittedUser pu : permittedUserRepositoryPage.getValues()) {
                identityMap.get(pu.getPermission()).add(pu.getUser().getDisplayName());
            }
        }

        return identityMap;
    }

    /**
     * Create a display string consisting of the group name and members
     * @param permittedGroup
     * @return display string
     */
    private String buildGroupString(PermittedGroup permittedGroup, PageRequest pageRequest) {

        StringBuilder builder = new StringBuilder();

        builder.append(permittedGroup.getGroup());
        Page<? extends ApplicationUser> applicationUserPage =  userService.findUsersByGroup(permittedGroup.getGroup(), pageRequest);


        ArrayList<String> userList = new ArrayList<String>();

        for (ApplicationUser applicationUser : applicationUserPage.getValues()) {
            userList.add(applicationUser.getDisplayName());
        }
        builder.append(Arrays.toString(userList.toArray()));


        return builder.toString();

    }


}
