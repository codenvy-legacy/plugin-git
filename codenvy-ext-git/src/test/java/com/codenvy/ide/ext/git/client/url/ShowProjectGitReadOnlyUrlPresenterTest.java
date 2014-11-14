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
package com.codenvy.ide.ext.git.client.url;

import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.ide.api.notification.Notification;
import com.codenvy.ide.collections.Array;
import com.codenvy.ide.collections.Collections;
import com.codenvy.ide.ext.git.client.BaseTest;
import com.codenvy.ide.ext.git.shared.Remote;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.googlecode.gwt.test.utils.GwtReflectionUtils;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Testing {@link ShowProjectGitReadOnlyUrlPresenter} functionality.
 *
 * @author Andrey Plotnikov
 * @author Oleksii Orel
 */
public class ShowProjectGitReadOnlyUrlPresenterTest extends BaseTest {
    @Mock
    private ShowProjectGitReadOnlyUrlView      view;
    private ShowProjectGitReadOnlyUrlPresenter presenter;

    @Override
    public void disarm() {
        super.disarm();

        presenter = new ShowProjectGitReadOnlyUrlPresenter(view, service, appContext, constant,
                                                           notificationManager, dtoUnmarshallerFactory);
    }

    @Test
    public void testShowDialogWhenGetGitUrlRequestIsSuccessful() throws Exception {
        final Array<Remote> remotes = Collections.createArray();
        remotes.add(mock(Remote.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<String> callback = (AsyncRequestCallback<String>)arguments[1];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, LOCALE_URI);
                return callback;
            }
        }).when(service).getGitReadOnlyUrl((ProjectDescriptor)anyObject(), (AsyncRequestCallback<String>)anyObject());
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

        presenter.showDialog();

        verify(appContext).getCurrentProject();
        verify(service).remoteList(eq(rootProjectDescriptor), anyString(), eq(true), (AsyncRequestCallback<Array<Remote>>)anyObject());
        verify(view).setRemotes((Array<Remote>)anyObject());
        verify(service).getGitReadOnlyUrl(eq(rootProjectDescriptor), (AsyncRequestCallback<String>)anyObject());
        verify(view).setLocaleUrl(eq(LOCALE_URI));
    }

    @Test
    public void testShowDialogWhenGetGitUrlRequestIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<String> callback = (AsyncRequestCallback<String>)arguments[1];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).getGitReadOnlyUrl((ProjectDescriptor)anyObject(), (AsyncRequestCallback<String>)anyObject());

        presenter.showDialog();

        verify(appContext).getCurrentProject();
        verify(service).getGitReadOnlyUrl(eq(rootProjectDescriptor), (AsyncRequestCallback<String>)anyObject());
        verify(notificationManager).showNotification((Notification)anyObject());
        verify(constant).initFailed();
    }

    @Test
    public void testOnCloseClicked() throws Exception {
        presenter.onCloseClicked();

        verify(view).close();
    }
}