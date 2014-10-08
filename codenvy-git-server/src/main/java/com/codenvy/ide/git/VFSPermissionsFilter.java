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
import com.codenvy.api.user.server.dao.User;
import com.codenvy.api.workspace.server.dao.Member;
import com.codenvy.api.workspace.server.dao.MemberDao;
import com.codenvy.commons.json.JsonHelper;
import com.codenvy.commons.json.JsonParseException;
import com.codenvy.commons.lang.ExpirableCache;
import com.codenvy.commons.lang.IoUtil;

import org.apache.commons.codec.binary.Base64;
import org.everrest.core.impl.provider.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
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
@Singleton
public class VFSPermissionsFilter implements Filter {

    @Inject
    private UserDao userDao;

    @Inject
    private MemberDao memberDao;

    @Inject
    @Named("api.endpoint")
    String apiEndPoint;

    private VFSPermissionsChecker           vfsPermissionsChecker;
    private ExpirableCache<String, Boolean> credentialsCache;

    private static final Logger LOG = LoggerFactory.getLogger(VFSPermissionsFilter.class);

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
                Boolean authenticated = false;
                if (!userName.isEmpty()) {
                    try {
                        if (password.equals("x-codenvy")) { // internal SSO login
                            user = getUserBySSO(userName);
                            if (user != null)
                                authenticated = true;
                        } else {
                            user = userDao.getByAlias(userName);
                        }
                        Member userMember = null;
                        for (Member member : memberDao.getWorkspaceMembers(projectDirectory.getParentFile().getName())) {
                            if (member.getUserId().equals(user.getId()))
                                userMember = member;
                        }

                        if (!authenticated) {
                            if (credentialsCache.get((userName + password)) == null) {
                                authenticated = userDao.authenticate(userName, password);
                                credentialsCache.put(userName + password, authenticated);
                            }
                        }

                        if (!authenticated ||
                            !vfsPermissionsChecker.isAccessAllowed(user.getEmail(), userMember, projectDirectory)) {
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
                        ((HttpServletResponse)response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "You are not authorized to perform this action.");
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


    private User getUserBySSO(String token) {
        try {
            HttpURLConnection conn = (HttpURLConnection)new URL(
                    apiEndPoint + "/internal/sso/server" + "/" + token + "?" + "clienturl=" +
                    URLEncoder.encode(apiEndPoint, "UTF-8")
            ).openConnection();

            try {
                conn.setRequestMethod("GET");
                conn.setDoOutput(true);

                final int responseCode = conn.getResponseCode();
                if (responseCode == 400) {
                    return null;
                } else if (responseCode != 200) {
                    throw new IOException(
                            "Error response with status " + responseCode + " for sso client  " + token + ". Message " +
                            IoUtil.readAndCloseQuietly(conn.getErrorStream())
                    );
                }

                try (InputStream in = conn.getInputStream()) {
                    JsonValue value = JsonHelper.parseJson(in);
                    User result = new User();
                    result.setId(value.getElement("id").getStringValue());
                    result.setEmail(value.getElement("name").getStringValue());
                    return result;
                }

            } finally {
                conn.disconnect();
            }
        } catch (IOException | JsonParseException e) {
            LOG.warn(e.getLocalizedMessage());
        }
        return null;
    }
}