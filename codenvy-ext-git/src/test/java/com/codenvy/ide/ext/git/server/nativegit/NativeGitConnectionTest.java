/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
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
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.codenvy.api.core.UnauthorizedException;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.ide.ext.git.server.GitException;
import com.codenvy.ide.ext.git.server.nativegit.commands.*;
import com.codenvy.ide.ext.git.shared.*;
import com.google.common.collect.ImmutableList;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

@Listeners(MockitoTestNGListener.class)
public class NativeGitConnectionTest {

    @Mock
    GitUser             user;
    @Mock
    GitUser             wso2User;
    @Mock
    SshKeysManager      keysManager;
    @Mock
    CredentialsLoader   credentialsLoader;
    @Mock
    NativeGit           nativeGit;
    @Mock
    EmptyGitCommand     emptyGitCommand;
    @Mock
    CommitCommand       commitCommand;
    @Mock
    BranchDeleteCommand branchDeleteCommand;
    @Mock
    ConfigImpl          config;
    @Mock
    LogCommand          logCommand;
    @Mock
    Revision            revision;
    @Mock
    BranchListCommand   branchListCommand;
    @Mock
    RemoteListCommand   remoteListCommand;
    @Mock
    Remote              remote;

    NativeGitConnection connection;

    @BeforeMethod
    public void setUp() throws Exception {

        connection = new NativeGitConnection(nativeGit, user, keysManager, credentialsLoader, new GitAskPassScript());

        when(nativeGit.createEmptyGitCommand()).thenReturn(emptyGitCommand);
        when(nativeGit.createCommitCommand()).thenReturn(commitCommand);
        when(nativeGit.createBranchDeleteCommand()).thenReturn(branchDeleteCommand);
        when(nativeGit.createLogCommand()).thenReturn(logCommand);
        when(logCommand.execute()).thenReturn(ImmutableList.of(revision));
        when(nativeGit.createConfig()).thenReturn(config);
        when(nativeGit.createBranchListCommand()).thenReturn(branchListCommand);
        when(branchListCommand.getLines()).thenReturn(ImmutableList.of("* master"));
        when(nativeGit.createRemoteListCommand()).thenReturn(remoteListCommand);
        when(remoteListCommand.execute()).thenReturn(ImmutableList.of(remote));

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

    @Test
    public void shouldDeleteLocalBranch()
            throws GitException, UnauthorizedException, InterruptedException, IOException {
        BranchDeleteRequest branchDeleteRequest = DtoFactory.getInstance().createDto(BranchDeleteRequest.class)
                                                            .withName("refs/heads/localBranch").withForce(true);
        when(emptyGitCommand.setNextParameter(anyString())).thenReturn(emptyGitCommand);
        when(emptyGitCommand.getText()).thenReturn("f5d9ef24292f7e432b2b13762e112c380323f869 refs/heads/localBranch");

        //delete branch
        connection.branchDelete(branchDeleteRequest);

        verify(branchDeleteCommand).setBranchName(eq("localBranch"));
        verify(branchDeleteCommand).setDeleteFullyMerged(eq(true));
        verify(branchDeleteCommand).execute();
    }

    @Test
    public void shouldDeleteRemoteBranch()
            throws GitException, UnauthorizedException, InterruptedException, IOException {
        final String REMOTE_URI = "git@github.com:gitaccount/repository.git";
        File keyfile = mock(File.class);
        BranchDeleteRequest branchDeleteRequest = DtoFactory.getInstance().createDto(BranchDeleteRequest.class)
                                                            .withName("refs/remotes/origin/remoteBranch").withForce(true);

        when(emptyGitCommand.setNextParameter(anyString())).thenReturn(emptyGitCommand);
        when(emptyGitCommand.getText())
                .thenReturn("f5d9ef24292f7e432b2b13762e112c380323f869 refs/remotes/origin/remoteBranch");
        when(remoteListCommand.setRemoteName("origin")).thenReturn(remoteListCommand);
        when(remoteListCommand.execute().get(0).getUrl())
                .thenReturn(REMOTE_URI);
        when(keysManager.writeKeyFile(REMOTE_URI)).thenReturn(keyfile);
        when(keyfile.getAbsolutePath()).thenReturn("keyfile");
        when(nativeGit.createBranchDeleteCommand(eq("keyfile"))).thenReturn(branchDeleteCommand);

        //delete branch
        connection.branchDelete(branchDeleteRequest);

        verify(branchDeleteCommand).setBranchName(eq("remoteBranch"));
        verify(branchDeleteCommand).setRemote(eq("origin"));
        verify(branchDeleteCommand).setDeleteFullyMerged(eq(true));
        verify(branchDeleteCommand).execute();
    }
}