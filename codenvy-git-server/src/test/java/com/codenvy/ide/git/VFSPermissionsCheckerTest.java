/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.ide.git;

import com.codenvy.api.vfs.shared.dto.Principal;
import com.codenvy.api.vfs.shared.dto.VirtualFileSystemInfo;
import com.codenvy.api.workspace.server.dao.Member;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.vfs.impl.fs.AccessControlList;
import com.codenvy.vfs.impl.fs.AccessControlListSerializer;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/** @author <a href="mailto:evoevodin@codenvy.com">Eugene Voevodin</a> */
public class VFSPermissionsCheckerTest {

    final static String WORKSPACE = "workspace";
    final File projectDirectory;
    final VFSPermissionsChecker userPermissionsChecker = new VFSPermissionsChecker();

    VFSPermissionsCheckerTest() throws URISyntaxException {
        File workspace =
                new File(new File(Thread.currentThread().getContextClassLoader().getResource(".").toURI()).getParentFile(), WORKSPACE);
        projectDirectory = new File(workspace, "testProject");
        projectDirectory.mkdirs();
    }

    /* If project doesn't have permissions to any principal, empty user should have access */
    @Test
    public void testEmptyUserAndProjectWithoutPermissions() throws IOException, ServletException {
        assertTrue(userPermissionsChecker.isAccessAllowed("", null, projectDirectory));
    }

    /* If project has ALL permissions to any principal, empty user should have access */
    @Test
    public void testEmptyUserAndProjectWithUserAllPermissions() throws IOException, ServletException {
        //given
        Principal principal = DtoFactory.getInstance().createDto(Principal.class).withName(VirtualFileSystemInfo.ANY_PRINCIPAL).withType(Principal.Type.USER);
        createProjectACL(getPermissionsMap(principal, VirtualFileSystemInfo.BasicPermissions.ALL.value()));
        //then
        assertTrue(userPermissionsChecker.isAccessAllowed("", null, projectDirectory));
    }

    /* If project has ALL permissions to "workspace/developer" group, user with role "workspace/developer" should have access */
    @Test
    public void testProjectWithWorkspaceDeveloperGroupAllPermissionsAndUserWithDeveloperRole()
            throws  IOException, ServletException {
        //given
        Principal userPrincipal = DtoFactory.getInstance().createDto(Principal.class).withName("workspace/developer").withType(Principal.Type.GROUP);
        createProjectACL(getPermissionsMap(userPrincipal, VirtualFileSystemInfo.BasicPermissions.ALL.value()));
        //then
        assertTrue(userPermissionsChecker.isAccessAllowed("user", new Member().withRoles(Arrays.asList(
                                                                  "workspace/developer")),
                                                          projectDirectory
                                                         ));
        assertFalse(userPermissionsChecker
                            .isAccessAllowed("user", new Member().withRoles(Arrays.asList(
                                                     "workspace/president")),
                                             projectDirectory
                                            ));
    }

    /* If project has ALL permissions to specific user, only this user should have access  */
    @Test
    public void testProjectWithAllPermissionsToSpecificUserAndUserWithCorrectCredentials() throws IOException, ServletException {
        //given
        Principal userPrincipal = DtoFactory.getInstance().createDto(Principal.class).withName("user").withType(
                Principal.Type.USER);
        createProjectACL(getPermissionsMap(userPrincipal, VirtualFileSystemInfo.BasicPermissions.ALL.value()));
        //then
        assertTrue(userPermissionsChecker.isAccessAllowed("user", new Member().withRoles(Arrays.asList(
                                                                  "workspace/developer")),
                                                          projectDirectory
                                                         ));
        assertFalse(userPermissionsChecker.isAccessAllowed("ChuckNorris",  new Member().withRoles(Arrays.asList(
                                                                   "workspace/developer")),
                                                           projectDirectory
                                                          ));
    }

    @Test
    public void shouldReadPermissionsFromWorkspaceAclIfProjectAclDoesNotExists() throws Exception {
        //given
        Principal userPrincipal = DtoFactory.getInstance().createDto(Principal.class).withName("user").withType(Principal.Type.USER);
        createWorkspaceACL(getPermissionsMap(userPrincipal, VirtualFileSystemInfo.BasicPermissions.ALL.value()));
        //then
        assertTrue(userPermissionsChecker.isAccessAllowed("user", new Member().withRoles(Arrays.asList(
                                                                  "workspace/developer")),
                                                          projectDirectory
                                                         ));
        assertFalse(userPermissionsChecker.isAccessAllowed("ChuckNorris",  new Member().withRoles(Arrays.asList(
                                                                   "workspace/developer")),
                                                           projectDirectory
                                                          ));
    }

    /* delete project acl file after test */
    @AfterMethod
    public void deleteACLs() {
        File projectACL = new File(projectDirectory.getParentFile(),
                                   ".vfs".concat(File.separator).concat("acl").concat(File.separator).concat(
                                           projectDirectory.getName().concat("_acl")));
        if (projectACL.exists()) {
            projectACL.delete();
        }

        File workspaceACL = new File(projectDirectory.getParentFile(),
                                     ".vfs".concat(File.separator).concat("acl").concat(File.separator).concat("_acl"));

        if (workspaceACL.exists()) {
            workspaceACL.delete();
        }
    }




    /**
     * Used with AccessControlList
     *
     * @param principal
     *         principal that will be written to acl file
     * @param permissions
     *         permissions that will be written to acl file with given principal
     * @return permissions map
     */
    private Map<Principal, Set<String>> getPermissionsMap(Principal principal, String... permissions) {
        Set<String> setOfGivenPermissions = new HashSet<>();
        setOfGivenPermissions.addAll(Arrays.asList(permissions));
        HashMap<Principal, Set<String>> resultPermissions = new HashMap<>();
        resultPermissions.put(principal, setOfGivenPermissions);
        return resultPermissions;
    }

    /**
     * Create project aclFile and write given permissions to it
     *
     * @param permissionsMap
     *         map with permissions
     * @throws java.io.IOException
     *         when it is not possible to write permissions
     */
    private void createProjectACL(Map<Principal, Set<String>> permissionsMap)
            throws IOException {
        File aclDir = new File(projectDirectory.getParentFile(), ".vfs".concat(File.separator).concat("acl"));
        if (!aclDir.exists()) {
            aclDir.mkdirs();
        }
        File aclFile = new File(aclDir, projectDirectory.getName().concat("_acl"));
        AccessControlListSerializer serializer = new AccessControlListSerializer();
        AccessControlList acl = new AccessControlList(permissionsMap);
        serializer.write(new DataOutputStream(new FileOutputStream(aclFile)), acl);
    }

    /**
     * Create workspace aclFile and write given permissions to it
     *
     * @param permissionsMap
     *         map with permissions
     * @throws java.io.IOException
     *         when it is not possible to write permissions
     */
    private void createWorkspaceACL(Map<Principal, Set<String>> permissionsMap)
            throws IOException {
        File aclDir = new File(projectDirectory.getParentFile(), ".vfs".concat(File.separator).concat("acl"));
        if (!aclDir.exists()) {
            aclDir.mkdirs();
        }
        File aclFile = new File(aclDir, "_acl");
        AccessControlListSerializer serializer = new AccessControlListSerializer();
        AccessControlList acl = new AccessControlList(permissionsMap);
        serializer.write(new DataOutputStream(new FileOutputStream(aclFile)), acl);
    }
}
