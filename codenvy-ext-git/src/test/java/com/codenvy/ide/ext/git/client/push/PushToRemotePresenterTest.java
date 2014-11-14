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
package com.codenvy.ide.ext.git.client.push;

import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.ide.collections.Array;
import com.codenvy.ide.collections.Collections;
import com.codenvy.ide.ext.git.client.BaseTest;
import com.codenvy.ide.ext.git.client.utils.BranchUtil;
import com.codenvy.ide.ext.git.shared.Branch;
import com.codenvy.ide.ext.git.shared.Remote;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.googlecode.gwt.test.utils.GwtReflectionUtils;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link PushToRemotePresenter} functionality.
 *
 * @author Andrey Plotnikov
 * @author Sergii Leschenko
 */
public class PushToRemotePresenterTest extends BaseTest {
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<String>>              asyncRequestCallbackStringCaptor;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Array<Remote>>>       asyncRequestCallbackArrayRemoteCaptor;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Array<Branch>>>       asyncRequestCallbackArrayBranchCaptor;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Map<String, String>>> asyncRequestCallbackMapCaptor;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Branch>>              asyncRequestCallbackBranchCaptor;
    @Captor
    private ArgumentCaptor<AsyncCallback<Array<Branch>>>              asyncCallbackArrayBranchCaptor;

    @Mock
    private PushToRemoteView view;
    @Mock
    private Branch           localBranch;
    @Mock
    private Branch           remoteBranch;
    @Mock
    private BranchUtil       branchUtil;

    @InjectMocks
    private PushToRemotePresenter presenter;

    public static final boolean SHOW_ALL_INFORMATION = true;
    public static final boolean DISABLE_CHECK        = false;

    public void disarm() {
        super.disarm();

        when(view.getRepository()).thenReturn(REPOSITORY_NAME);
        when(view.getLocalBranch()).thenReturn(LOCAL_BRANCH);
        when(view.getRemoteBranch()).thenReturn(REMOTE_BRANCH);

        when(localBranch.getName()).thenReturn("refs/heads/" + LOCAL_BRANCH);
        when(localBranch.getDisplayName()).thenReturn(LOCAL_BRANCH);
        when(localBranch.isActive()).thenReturn(true);
        when(localBranch.isRemote()).thenReturn(false);

        when(remoteBranch.getName()).thenReturn("refs/remotes/" + REPOSITORY_NAME + "/" + REMOTE_BRANCH);
        when(remoteBranch.getDisplayName()).thenReturn(REMOTE_BRANCH);
        when(remoteBranch.isActive()).thenReturn(true);
        when(remoteBranch.isRemote()).thenReturn(false);
    }

    @Test
    public void testShowListOfLocalBranches() throws Exception {
        final Array<Branch> localBranches = Collections.createArray();
        localBranches.add(localBranch);

        presenter.updateLocalBranches();

        verify(service).branchList((ProjectDescriptor)anyObject(), (String)eq(null), asyncRequestCallbackArrayBranchCaptor.capture());
        AsyncRequestCallback<Array<Branch>> value = asyncRequestCallbackArrayBranchCaptor.getValue();
        Method onSuccessRemotes = GwtReflectionUtils.getMethod(value.getClass(), "onSuccess");
        onSuccessRemotes.invoke(value, localBranches);

        verify(branchUtil).getLocalBranchesToDisplay(eq(localBranches));
        verify(view).setLocalBranches((Array<String>)anyObject());
        verify(view).selectLocalBranch(localBranch.getDisplayName());
    }

    public void testShowDialogWhenBranchListRequestIsSuccessful() throws Exception {
        final Array<Remote> remotes = Collections.createArray();
        remotes.add(mock(Remote.class));
        final Array<Branch> branches = Collections.createArray();
        branches.add(localBranch);

        final Array<String> names_branches = Collections.createArray();
        names_branches.add(LOCAL_BRANCH);

        when(branchUtil.getLocalBranchesToDisplay((Array<Branch>)anyObject())).thenReturn(names_branches);

        presenter.showDialog();

        verify(service).remoteList((ProjectDescriptor)anyObject(), anyString(), anyBoolean(),
                                   asyncRequestCallbackArrayRemoteCaptor.capture());

        AsyncRequestCallback<Array<Remote>> remotesCallback = asyncRequestCallbackArrayRemoteCaptor.getValue();
        //noinspection NonJREEmulationClassesInClientCode
        Method onSuccessRemotes = GwtReflectionUtils.getMethod(remotesCallback.getClass(), "onSuccess");
        onSuccessRemotes.invoke(remotesCallback, remotes);

        verify(service).branchList((ProjectDescriptor)anyObject(), anyString(), asyncRequestCallbackArrayBranchCaptor.capture());
        AsyncRequestCallback<Array<Branch>> branchesCallback = asyncRequestCallbackArrayBranchCaptor.getValue();
        //noinspection NonJREEmulationClassesInClientCode
        Method onSuccessBranches = GwtReflectionUtils.getMethod(branchesCallback.getClass(), "onSuccess");
        onSuccessBranches.invoke(branchesCallback, branches);

        verify(service, times(2)).branchList((ProjectDescriptor)anyObject(), anyString(), asyncRequestCallbackArrayBranchCaptor.capture());
        branchesCallback = asyncRequestCallbackArrayBranchCaptor.getValue();
        //noinspection NonJREEmulationClassesInClientCode
        onSuccessBranches = GwtReflectionUtils.getMethod(branchesCallback.getClass(), "onSuccess");
        onSuccessBranches.invoke(branchesCallback, branches);

        verify(service)
                .config((ProjectDescriptor)anyObject(), anyListOf(String.class), anyBoolean(), asyncRequestCallbackMapCaptor.capture());
        AsyncRequestCallback<Map<String, String>> value = asyncRequestCallbackMapCaptor.getValue();
        Method config = GwtReflectionUtils.getMethod(value.getClass(), "onSuccess");
        config.invoke(value, new HashMap<>());

        verify(appContext).getCurrentProject();
        verify(service).remoteList(eq(rootProjectDescriptor), anyString(), eq(SHOW_ALL_INFORMATION),
                                   (AsyncRequestCallback<Array<Remote>>)anyObject());
        verify(view).setEnablePushButton(eq(ENABLE_BUTTON));
        verify(view).setRepositories((Array<Remote>)anyObject());
        verify(view).showDialog();
        verify(view).setRemoteBranches((Array<String>)anyObject());
        verify(view).setLocalBranches((Array<String>)anyObject());
    }
/*
    @Test
    public void testSelectActiveBranch() throws Exception {
        final Array<Remote> remotes = Collections.createArray();
        remotes.add(mock(Remote.class));
        final Array<Branch> branches = Collections.createArray();
        branches.add(localBranch);
        when(localBranch.isActive()).thenReturn(ACTIVE_BRANCH);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Array<Remote>> callback = (AsyncRequestCallback<Array<Remote>>)arguments[3];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, remotes);
                return callback;
            }
        }).when(service)
          .remoteList((ProjectDescriptor)anyObject(), anyString(), anyBoolean(), (AsyncRequestCallback<Array<Remote>>)anyObject());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<String> callback = (AsyncRequestCallback<String>)arguments[2];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, branches);
                return callback;
            }
        }).doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<String> callback = (AsyncRequestCallback<String>)arguments[2];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, branches);
                return callback;
            }
        }).when(service).branchList((ProjectDescriptor)anyObject(), anyString(), (AsyncRequestCallback<Array<Branch>>)anyObject());

        presenter.showDialog();

        verify(appContext).getCurrentProject();
        verify(service).remoteList(eq(rootProjectDescriptor), anyString(), eq(SHOW_ALL_INFORMATION),
                                   (AsyncRequestCallback<Array<Remote>>)anyObject());
        verify(service, times(2)).branchList(eq(rootProjectDescriptor), anyString(), (AsyncRequestCallback<Array<Branch>>)anyObject());
        verify(view).setEnablePushButton(eq(ENABLE_BUTTON));
        verify(view).setRepositories((Array<Remote>)anyObject());
        verify(view).showDialog();
        verify(view).setRemoteBranches((Array<String>)anyObject());
        verify(view).setLocalBranches((Array<String>)anyObject());
        verify(view).selectLocalBranch(anyString());

        presenter.onLocalBranchChanged();
        verify(view, times(2)).selectRemoteBranch(anyString());
    }

    @Test
    public void testShowDialogWhenBranchListRequestIsFailed() throws Exception {
        final Array<Remote> remotes = Collections.createArray();
        remotes.add(mock(Remote.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Array<Remote>> callback = (AsyncRequestCallback<Array<Remote>>)arguments[3];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, remotes);
                return callback;
            }
        }).when(service).remoteList((ProjectDescriptor)anyObject(), anyString(), anyBoolean(),
                                    (AsyncRequestCallback<Array<Remote>>)anyObject());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Array<Branch>> callback = (AsyncRequestCallback<Array<Branch>>)arguments[2];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Array<Branch>> callback = (AsyncRequestCallback<Array<Branch>>)arguments[2];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).branchList((ProjectDescriptor)anyObject(), anyString(), (AsyncRequestCallback<Array<Branch>>)anyObject());

        presenter.showDialog();

        verify(appContext).getCurrentProject();
        verify(service).remoteList(eq(rootProjectDescriptor), anyString(), eq(SHOW_ALL_INFORMATION),
                                   (AsyncRequestCallback<Array<Remote>>)anyObject());
        verify(constant).branchesListFailed();
        verify(notificationManager).showNotification((Notification)anyObject());
        verify(view).setEnablePushButton(eq(DISABLE_BUTTON));
    }

    @Test
    public void testShowDialogWhenRemoteListRequestIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Array<Remote>> callback = (AsyncRequestCallback<Array<Remote>>)arguments[3];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).remoteList((ProjectDescriptor)anyObject(), anyString(), anyBoolean(),
                                    (AsyncRequestCallback<Array<Remote>>)anyObject());

        presenter.showDialog();

        verify(appContext).getCurrentProject();
        verify(service).remoteList(eq(rootProjectDescriptor), anyString(), eq(SHOW_ALL_INFORMATION),
                                   (AsyncRequestCallback<Array<Remote>>)anyObject());
        verify(constant).remoteListFailed();
        verify(view).setEnablePushButton(eq(DISABLE_BUTTON));
    }

    @Test
    public void testOnPushClickedWhenPushWSRequestIsSuccessful() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[4];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, (Void)null);
                return callback;
            }
        }).when(service).push((ProjectDescriptor)anyObject(), (List<String>)anyObject(), anyString(), anyBoolean(),
                              (AsyncRequestCallback<Void>)anyObject());

        presenter.showDialog();
        presenter.onPushClicked();

        verify(service).push(eq(rootProjectDescriptor), (List<String>)anyObject(), eq(REPOSITORY_NAME), eq(DISABLE_CHECK),
                             (AsyncRequestCallback<Void>)anyObject());
        verify(view).close();
        verify(notificationManager).showNotification((Notification)anyObject());
        verify(constant).pushSuccess(eq(REPOSITORY_NAME));
    }

    @Test
    public void testOnPushClickedWhenPushWSRequestIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[4];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).push((ProjectDescriptor)anyObject(), (List<String>)anyObject(), anyString(), anyBoolean(),
                              (AsyncRequestCallback<Void>)anyObject());

        presenter.showDialog();
        presenter.onPushClicked();

        verify(service).push(eq(rootProjectDescriptor), (List<String>)anyObject(), eq(REPOSITORY_NAME), eq(DISABLE_CHECK),
                             (AsyncRequestCallback<Void>)anyObject());
        verify(view).close();
        verify(constant).pushFail();
        verify(notificationManager).showNotification((Notification)anyObject());
    }

    @Test
    public void testReloadRemoteBranchesWhenChangesRepository() {
        //TODO
    }

    @Test
    public void testAutoSelectRemoteBranchToUpstreamWhenItExist() {
        //TODO
    }

    @Test
    public void testOnCancelClicked() throws Exception {
        presenter.onCancelClicked();

        verify(view).close();
    }*/
}
