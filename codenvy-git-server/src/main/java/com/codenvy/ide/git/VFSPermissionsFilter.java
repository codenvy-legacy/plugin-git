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

import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.api.workspace.server.dao.Member;
import com.codenvy.api.workspace.server.dao.MemberDao;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.commons.lang.ExpirableCache;

import org.apache.commons.codec.binary.Base64;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * If user doesn't have permissions to repository, filter will deny request with 403.
 * Filter searches for .../workspace/.vfs/acl/ProjectName_acl file that contains permissions
 * if it doesn't exist request will be accepted, else if permissions is not READ or ALL request will
 * be denied with 403 FORBIDDEN.
 *
 * @author <a href="mailto:evoevodin@codenvy.com">Eugene Voevodin</a>
 */
public class VFSPermissionsFilter implements Filter {

    @Inject
    private UserDao userDao;

    @Inject
    private MemberDao memberDao;

    @Inject
    private WorkspaceDao workspaceDao;

    private VFSPermissionsChecker           vfsPermissionsChecker;
    private ExpirableCache<String, Boolean> credentialsCache;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        vfsPermissionsChecker = new VFSPermissionsChecker();
        credentialsCache = new ExpirableCache<>(TimeUnit.MINUTES.toMillis(5), 20);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)request;
        String fsRootPath = System.getProperty("com.codenvy.vfs.rootdir");
        int tokenPlace;
        String lastTokenBeforePath = "/git/";
        if ((tokenPlace = req.getRequestURL().indexOf(lastTokenBeforePath)) != -1) {
            //get path to project
            String url = req.getRequestURL().substring(tokenPlace + lastTokenBeforePath.length());
            url = url.replaceFirst("/info/refs", "");
            url = url.replaceFirst("/git-upload-pack", "");
            //adaptation to fs
            url = url.replaceAll("/", File.separator);
            //search for dotVFS directory
            File projectDirectory = Paths.get(fsRootPath, url).toFile();
            String auth;
            String userName = "";
            String password = "";
            if ((auth = req.getHeader("authorization")) != null) {
                //get encoded password phrase
                String userAndPasswordEncoded = auth.substring(6);
                // decode Base64 user:password
                String userAndPasswordDecoded = new String(Base64.decodeBase64(userAndPasswordEncoded));
                //get username and password separator ':'
                int betweenUserAndPassword = userAndPasswordDecoded.indexOf(':');
                //get username - it is before first ':'
                userName = userAndPasswordDecoded.substring(0, betweenUserAndPassword);
                //get password - it is after first ':'
                password = userAndPasswordDecoded.substring(betweenUserAndPassword + 1);
            }

            // Check if user authenticated and hasn't permissions to project, then send response code 403
            try {
                User user = null;
                if (!userName.isEmpty()) {
                    try {
                        user = userDao.getByAlias(userName);
                        Member userMember = null;
                        for (Member member : memberDao.getWorkspaceMembers(
                                workspaceDao.getByName(projectDirectory.getParentFile().getName()).getId())) {
                            if (member.getUserId().equals(user.getId()))
                                userMember = member;
                        }

                        Boolean authenticated = credentialsCache.get((userName + password));
                        if (authenticated == null) {
                            authenticated = userDao.authenticate(userName, password);
                            credentialsCache.put(userName + password, authenticated);
                        }

                        if (!authenticated ||
                            !vfsPermissionsChecker.isAccessAllowed(userName, userMember, projectDirectory)) {
                            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN);
                            return;
                        }
                    } catch (NotFoundException ignore) {
                        //ignore, let user be anonymous
                    }
                }

                if (userName.isEmpty() || user == null) {
                    // if user wasn't required check project permissions to any user,
                    // if it is not READ or ALL send response code 401 and header with BASIC type of authentication

                    if (!vfsPermissionsChecker.isAccessAllowed("", null, projectDirectory)) {
                        ((HttpServletResponse)response).addHeader("Cache-Control", "private");
                        ((HttpServletResponse)response).addHeader("WWW-Authenticate", "Basic");
                        ((HttpServletResponse)response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                }
            } catch (ServerException e) {
                throw new ServletException(e.getMessage(), e);
            }
        }
        chain.doFilter(req, response);
    }

    @Override
    public void destroy() {
    }
}