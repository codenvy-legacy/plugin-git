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
package com.codenvy.ide.ext.git.server.nativegit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

import com.codenvy.commons.lang.IoUtil;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.ide.ext.git.server.GitException;
import com.codenvy.ide.ext.git.server.nativegit.commands.BranchListCommand;
import com.codenvy.ide.ext.git.server.nativegit.commands.CommitCommand;
import com.codenvy.ide.ext.git.server.nativegit.commands.LogCommand;
import com.codenvy.ide.ext.git.shared.CommitRequest;
import com.codenvy.ide.ext.git.shared.GitUser;
import com.codenvy.ide.ext.git.shared.Revision;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.lang.reflect.Field;

@Listeners(MockitoTestNGListener.class)
public class NativeGitConnectionTest {

    @Mock
    GitUser           user;
    @Mock
    GitUser           wso2User;
    @Mock
    SshKeysManager    keysManager;
    @Mock
    CredentialsLoader credentialsLoader;
    @Mock
    NativeGit         nativeGit;
    @Mock
    CommitCommand     commitCommand;
    @Mock
    ConfigImpl        config;
    @Mock
    LogCommand        logCommand;
    @Mock
    Revision          revision;
    @Mock
    BranchListCommand branchListCommand;

    NativeGitConnection connection;

    @BeforeMethod
    public void setUp() throws Exception {

        connection = new NativeGitConnection(new File("/tmp"), user, keysManager, credentialsLoader);
        Field field = NativeGitConnection.class.getDeclaredField("nativeGit");
        field.setAccessible(true);
        field.set(connection, nativeGit);
        when(nativeGit.createCommitCommand()).thenReturn(commitCommand);
        when(nativeGit.createLogCommand()).thenReturn(logCommand);
        when(logCommand.execute()).thenReturn(ImmutableList.of(revision));
        when(nativeGit.createConfig()).thenReturn(config);
        when(nativeGit.createBranchListCommand()).thenReturn(branchListCommand);
        when(branchListCommand.getLines()).thenReturn(ImmutableList.of("* master"));

    }

    @Test
    public void shouldCommit() throws Exception {
        //given
        CommitRequest commitRequest =
                DtoFactory.getInstance().createDto(CommitRequest.class).withMessage("Message 1").withAmend(false).withAll(true);
        when(config.get(eq("codenvy.credentialsProvider"))).thenThrow(GitException.class); //emulate no property configured
        //when
        Revision actualRevision = connection.commit(commitRequest);
        //then
        ArgumentCaptor<GitUser> captor = ArgumentCaptor.forClass(GitUser.class);
        verify(commitCommand).setCommitter(captor.capture());
        verify(commitCommand).execute();
        verify(commitCommand).setAll(eq(true));
        verify(commitCommand).setCommitter(user);
        verify(commitCommand).setAmend(eq(false));
        verify(commitCommand).setMessage(eq("Message 1"));
        assertEquals(captor.getValue(), user);
        assertEquals(actualRevision, revision);
        verify(actualRevision).setBranch(eq("master"));
    }

    @Test
    public void shouldOverrideAuthorIfUserSetInConfig() throws GitException {
        //given
        CommitRequest commitRequest =
                DtoFactory.getInstance().createDto(CommitRequest.class).withMessage("Message 1").withAmend(false).withAll(true);
        when(config.get(eq("codenvy.credentialsProvider"))).thenThrow(GitException.class); //emulate no property configured
        when(config.get(eq("user.name"))).thenReturn("Inders Gogh Rasmusen");
        //when
        connection.commit(commitRequest);
        //then
        verify(commitCommand).setAuthor(user);
    }

    @Test
    public void shouldNotOverrideAuthorIfUserNotSetInConfig() throws GitException {
        //given
        CommitRequest commitRequest =
                DtoFactory.getInstance().createDto(CommitRequest.class).withMessage("Message 1").withAmend(false).withAll(true);
        when(config.get(eq("codenvy.credentialsProvider"))).thenThrow(GitException.class); //emulate no property configured
        when(config.get(eq("user.name"))).thenThrow(GitException.class);
        //when
        connection.commit(commitRequest);
        //then
        verify(commitCommand, never()).setAuthor(any(GitUser.class));
    }

    @Test
    public void shouldSetCommitterFromCredentialLoader() throws GitException {
        //given
        CommitRequest commitRequest =
                DtoFactory.getInstance().createDto(CommitRequest.class).withMessage("Message 1").withAmend(false).withAll(true);
        when(config.get(eq("codenvy.credentialsProvider"))).thenReturn("wso2");
        when(credentialsLoader.getUser(eq("wso2"))).thenReturn(wso2User);

        //when
        connection.commit(commitRequest);
        //then
        verify(commitCommand).setCommitter(eq(wso2User));
        verify(commitCommand).setAuthor(eq(wso2User));

    }


}