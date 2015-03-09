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
package org.eclipse.che.ide.ext.git.client.init;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.ext.git.client.BaseTest;
import org.eclipse.che.ide.ext.git.client.GitRepositoryInitializer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.googlecode.gwt.test.utils.GwtReflectionUtils;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Testing {@link InitRepositoryPresenter} functionality.
 *
 * @author Andrey Plotnikov
 * @author Roman Nikitenko
 */
public class InitRepositoryPresenterTest extends BaseTest {
    @Mock
    private GitRepositoryInitializer gitRepositoryInitializer;

    private InitRepositoryPresenter presenter;

    @Override
    public void disarm() {
        super.disarm();

        presenter = new InitRepositoryPresenter(appContext,
                                                constant,
                                                notificationManager,
                                                gitRepositoryInitializer);
    }

    @Test
    public void testOnOkClickedInitWSRequestAndGetProjectIsSuccessful() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncCallback<Void> callback = (AsyncCallback<Void>)arguments[1];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, (Void)null);
                return callback;
            }
        }).when(gitRepositoryInitializer).initGitRepository((ProjectDescriptor)anyObject(), (AsyncCallback<Void>)anyObject());

        presenter.initRepository();

        verify(gitRepositoryInitializer).initGitRepository(eq(rootProjectDescriptor), (AsyncCallback<Void>)anyObject());
        verify(constant).initSuccess();
        verify(notificationManager).showNotification((Notification)anyObject());
    }

    @Test
    public void testOnOkClickedInitWSRequestIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncCallback<String> callback = (AsyncCallback<String>)arguments[1];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(gitRepositoryInitializer).initGitRepository((ProjectDescriptor)anyObject(), (AsyncCallback<Void>)anyObject());

        presenter.initRepository();

        verify(gitRepositoryInitializer).initGitRepository(eq(rootProjectDescriptor), (AsyncCallback<Void>)anyObject());
        verify(constant).initFailed();
        verify(notificationManager).showNotification((Notification)anyObject());
    }
}
