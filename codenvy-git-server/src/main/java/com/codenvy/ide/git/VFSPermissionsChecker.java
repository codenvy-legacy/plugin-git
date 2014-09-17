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


import java.io.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * Used with @link VFSPermissionsFilter to check user permissions
 *
 * @author <a href="mailto:evoevodin@codenvy.com">Eugene Voevodin</a>
 */
public class VFSPermissionsChecker {

    /**
     * Check user permissions to project using project acls.
     * If user has READ or ALL permissions he has access.
     * Permission check chain: any user , specific user, user groups.
     *
     * @param user
     *         username
     * @param userMember
     *         roles specific to user
     * @param projectDirectory
     *         directory where project is situated
     * @return <code>true</code> if user has READ or ALL permissions, <code>false</code> if he doesn't
     * @throws java.io.IOException
     *         when it's not possible to get ACL
     */
    public boolean isAccessAllowed(String user, Member userMember, File projectDirectory) throws IOException {
        String projectName = projectDirectory.getName();
        //acl folder by path ../projectDirectory/.vfs/acl
        File aclDirectory = Paths.get(projectDirectory.getParentFile().getAbsolutePath(), ".vfs", "acl").toFile();
        // project acl is projectName_acl
        File aclFile = new File(aclDirectory, projectName + "_acl");
        if (!aclFile.exists()) {
            // if there is no acl for project find it for workspace
            aclFile = new File(aclDirectory, "_acl");
            if (!aclFile.exists()) {
                return true;
            }
        }
        AccessControlList acl = new AccessControlListSerializer().read(new DataInputStream(new FileInputStream(aclFile)));
        Set<String> resultPermissions = new HashSet<>();
        Principal principal = DtoFactory.getInstance().createDto(Principal.class).withName(VirtualFileSystemInfo.ANY_PRINCIPAL).withType(Principal.Type.USER);
        //get permissions to any principal
        if (acl.getPermissions(principal) != null) {
            resultPermissions = acl.getPermissions(principal);
        }
        if (!user.isEmpty()) {
            //get permissions to specific user
            principal.setName(user);
            if (acl.getPermissions(principal) != null) {
                resultPermissions.addAll(acl.getPermissions(principal));
            }
            //get permissions to userGroup
            principal.setType(Principal.Type.GROUP);
            if (userMember != null) {
                for (String role : userMember.getRoles()) {
                    principal.setName(role);
                    if (acl.getPermissions(principal) != null) {
                        resultPermissions.addAll(acl.getPermissions(principal));
                    }
                }
            }
        }
        return resultPermissions.contains(VirtualFileSystemInfo.BasicPermissions.READ.value()) ||
               resultPermissions.contains(VirtualFileSystemInfo.BasicPermissions.ALL.value());
    }
}
