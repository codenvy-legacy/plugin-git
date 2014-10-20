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
package com.codenvy.ide.ext.git.client.init;

import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.ide.api.notification.Notification;
import com.codenvy.ide.ext.git.client.BaseTest;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.websocket.rest.RequestCallback;
import com.googlecode.gwt.test.utils.GwtReflectionUtils;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Testing {@link InitRepositoryPresenter} functionality.
 *
 * @author Andrey Plotnikov
 * @author Roman Nikitenko
 */
public class InitRepositoryPresenterTest extends BaseTest {
    public static final boolean BARE = false;
    @Mock
    private InitRepositoryPresenter presenter;

    @Override
    public void disarm() {
        super.disarm();

        presenter = new InitRepositoryPresenter(service,
                                                appContext,
                                                constant,
                                                notificationManager,
                                                projectServiceClient,
                                                dtoUnmarshallerFactory);
    }

    @Test
    public void testOnOkClickedInitWSRequestAndGetProjectIsSuccessful() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                RequestCallback<Void> callback = (RequestCallback<Void>)arguments[2];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, (Void)null);
                return callback;
            }
        }).when(service).init((ProjectDescriptor)anyObject(), anyBoolean(), (RequestCallback<Void>)anyObject());

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<ProjectDescriptor> callback = (AsyncRequestCallback<ProjectDescriptor>)arguments[1];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, projectDescriptor);
                return callback;
            }
        }).when(projectServiceClient).getProject(anyString(), (AsyncRequestCallback<ProjectDescriptor>)anyObject());

        presenter.initRepository();

        verify(service).init(eq(rootProjectDescriptor), eq(BARE), (RequestCallback<Void>)anyObject());
        verify(projectServiceClient).getProject(anyString(), (AsyncRequestCallback<ProjectDescriptor>)anyObject());
        verify(constant).initSuccess();
        verify(notificationManager).showNotification((Notification)anyObject());
        verify(currentProject).setRootProject(eq(projectDescriptor));
    }

    @Test
    public void testOnOkClickedInitWSRequestIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                RequestCallback<String> callback = (RequestCallback<String>)arguments[2];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).init((ProjectDescriptor)anyObject(), anyBoolean(), (RequestCallback<Void>)anyObject());

        presenter.initRepository();

        verify(service).init(eq(rootProjectDescriptor), eq(BARE), (RequestCallback<Void>)anyObject());
        verify(notificationManager).showNotification((Notification)anyObject());
        verify(constant).initFailed();
    }

    @Test
    public void testOnOkClickedInitWSRequestIsSuccessfulButGetProjectIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                RequestCallback<Void> callback = (RequestCallback<Void>)arguments[2];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, (Void)null);
                return callback;
            }
        }).when(service).init((ProjectDescriptor)anyObject(), anyBoolean(), (RequestCallback<Void>)anyObject());

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<ProjectDescriptor> callback = (AsyncRequestCallback<ProjectDescriptor>)arguments[1];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(projectServiceClient).getProject(anyString(), (AsyncRequestCallback<ProjectDescriptor>)anyObject());

        presenter.initRepository();

        verify(service).init(eq(rootProjectDescriptor), eq(BARE), (RequestCallback<Void>)anyObject());
        verify(projectServiceClient).getProject(anyString(), (AsyncRequestCallback<ProjectDescriptor>)anyObject());
        verify(constant).initSuccess();
        verify(notificationManager, times(2)).showNotification((Notification)anyObject());
        verify(currentProject, never()).setRootProject(eq(projectDescriptor));
    }
}