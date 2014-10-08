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

import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.User;
import com.codenvy.api.workspace.server.dao.Member;
import com.codenvy.api.workspace.server.dao.MemberDao;
import com.codenvy.api.workspace.server.dao.Workspace;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.dto.server.DtoFactory;

import org.apache.commons.codec.binary.Base64;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.Arrays;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test different situations of user access to projects with different permissions.
 * Test related to @link com.codenvy.ide.git.VFSPermissionsFilter class.
 *
 * @author <a href="mailto:evoevodin@codenvy.com">Eugene Voevodin</a>
 */

@Listeners(MockitoTestNGListener.class)
public class VFSPermissionsFilterTest {

    final static String               USER      = "username";
    final static String               EMAIL     =  "email@email.com";
    final static String               PASSWORD  = "password";
    final static String               WORKSPACE = "workspace";
    @InjectMocks
    final static VFSPermissionsFilter filter    = new VFSPermissionsFilter();
    final File projectDirectory;
    @Mock
    HttpServletResponse   response;
    @Mock
    HttpServletRequest    request;
    @Mock
    UserDao               userDao;
    @Mock
    MemberDao             memberDao;
    @Mock
    WorkspaceDao          workspaceDao;
    @Mock
    FilterChain           filterChain;
    @Mock
    VFSPermissionsChecker vfsPermissionsChecker;

    /**
     * Basic setups need for tests: create workspace, create project directory, set system com.codenvy.vfs.rootdir
     * property
     */
    VFSPermissionsFilterTest()
            throws URISyntaxException, FileNotFoundException, NoSuchFieldException,
                   IllegalAccessException {
        File workspace =
                new File(new File(Thread.currentThread().getContextClassLoader().getResource(".").toURI())
                                 .getParentFile(), WORKSPACE);
        System.setProperty("com.codenvy.vfs.rootdir", workspace.getParentFile().getAbsolutePath());
        projectDirectory = new File(workspace, "testProject");
        projectDirectory.mkdirs();
    }

    @BeforeMethod
    public void before() throws Exception {
        System.setProperty("organization.application.server.url", "orgPath");
        filter.init(null);
        //set up UserManager mock
        Field filterUserManager = filter.getClass().getDeclaredField("userDao");
        filterUserManager.setAccessible(true);
        filterUserManager.set(filter, userDao);
        //set up permission checker mock
        Field filterUserPermissionsChecker = filter.getClass().getDeclaredField("vfsPermissionsChecker");
        filterUserPermissionsChecker.setAccessible(true);
        filterUserPermissionsChecker.set(filter, vfsPermissionsChecker);

        when((request).getRequestURL())
                .thenReturn(new StringBuffer("http://host.com/git/").append(WORKSPACE).append("/testProject"));
    }

    @Test
    public void shouldSkipFurtherIfProjectHasPermissionsForAllAndUserIsEmpty() throws IOException, ServletException {
        //given
        when(vfsPermissionsChecker.isAccessAllowed("", null, projectDirectory)).thenReturn(true);
        //when
        filter.doFilter(request, response, filterChain);
        //then should skip further request
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void shouldRespondUnauthorizedIfProjectHasPermissionsToSpecificUserAndUserIsEmpty()
            throws IOException, ServletException {
        //given
        when(vfsPermissionsChecker.isAccessAllowed("", null, projectDirectory)).thenReturn(false);
        //when
        filter.doFilter(request, response, filterChain);
        //then
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), eq("You are not authorized to perform this " +
                                                                           "action."));
    }


    @Test
    public void shouldSkipFurtherIfProjectHasWorkspaceDeveloperGroupAllPermissionsAndUserHasDeveloperRole()
            throws IOException, ServletException, ApiException {
        //given
        User user = new User();
        user.setId(USER);
        user.setEmail(EMAIL);
        Workspace workspace = new Workspace().withId(WORKSPACE);
        Member member = new Member().withUserId(USER).withWorkspaceId(WORKSPACE)
                                  .withRoles(Arrays.asList("workspace/developer"));

        when(userDao.getByAlias(USER)).thenReturn(user);
        when(workspaceDao.getByName(anyString())).thenReturn(workspace);
        when(memberDao.getWorkspaceMembers(WORKSPACE)).thenReturn(Arrays.asList(member));
        when(vfsPermissionsChecker.isAccessAllowed(EMAIL, member, projectDirectory)).thenReturn(true);
        when(request.getHeader("authorization"))
                .thenReturn("BASIC " + (Base64.encodeBase64String((USER + ":" + PASSWORD).getBytes())));
        when(userDao.authenticate(eq(USER), anyString())).thenReturn(true);
        //when
        filter.doFilter(request, response, filterChain);
        //then should skip further request
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void shouldRespondUnauthorizedIfProjectHasHasPermissionsToSpecificUserAndUserDoesNotExist()
            throws IOException, ServletException, ApiException {
        //given
        //when(userDao.getUserByAlias(eq(USER))).thenThrow(new UserExistenceException());
        doThrow(new NotFoundException(USER)).when(userDao).getByAlias(eq("OTHERUSER"));
        when(vfsPermissionsChecker.isAccessAllowed("", null, projectDirectory)).thenReturn(false);
        when(request.getHeader("authorization")).thenReturn(
                "BASIC " + (Base64.encodeBase64String(("OTHERUSER" + ":" + PASSWORD).getBytes())));
        //when
        filter.doFilter(request, response, filterChain);
        //then
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), eq("You are not authorized to perform this " +
                                                                           "action."));
    }

    @Test
    public void shouldSkipFurtherIfProjectHasWorkspaceDeveloperGroupAllPermissionsAndUserDoesNotExist()
            throws IOException, ServletException, ApiException {
        //given
        //when(userDao.getUserByAlias(eq(USER))).thenThrow(new UserExistenceException());
        doThrow(new NotFoundException("OTHERUSER")).when(userDao).getByAlias(eq("OTHERUSER"));
        when(vfsPermissionsChecker.isAccessAllowed("", null, projectDirectory)).thenReturn(true);
        when(request.getHeader("authorization")).thenReturn(
                "BASIC " + (Base64.encodeBase64String(("OTHERUSER" + ":" + PASSWORD).getBytes())));
        //when
        filter.doFilter(request, response, filterChain);
        //then
        verify(filterChain).doFilter(request, response);
    }
}
