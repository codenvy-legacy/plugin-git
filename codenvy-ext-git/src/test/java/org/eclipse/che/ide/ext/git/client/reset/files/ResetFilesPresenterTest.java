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
package org.eclipse.che.ide.ext.git.client.reset.files;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.ext.git.client.BaseTest;
import org.eclipse.che.ide.ext.git.shared.IndexFile;
import org.eclipse.che.ide.ext.git.shared.ResetRequest;
import org.eclipse.che.ide.ext.git.shared.Status;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.message.MessageDialog;
import com.googlecode.gwt.test.utils.GwtReflectionUtils;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link ResetFilesPresenter} functionality.
 *
 * @author <a href="mailto:aplotnikov@codenvy.com">Andrey Plotnikov</a>
 */
public class ResetFilesPresenterTest extends BaseTest {
    @Mock
    private ResetFilesView      view;
    private ResetFilesPresenter presenter;

    @Override
    public void disarm() {
        super.disarm();
        presenter = new ResetFilesPresenter(view, service, appContext, constant, notificationManager, dtoFactory, dtoUnmarshallerFactory,
                                            dialogFactory);
        when(dtoFactory.createDto(IndexFile.class)).thenReturn(mock(IndexFile.class));
    }

    @Test
    public void testShowDialogWhenStatusRequestIsSuccessful() throws Exception {
        final Status status = mock(Status.class);
        List<String> changes = new ArrayList<String>();
        changes.add("Change");
        when(status.getAdded()).thenReturn(changes);
        when(status.getChanged()).thenReturn(changes);
        when(status.getRemoved()).thenReturn(changes);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Status> callback = (AsyncRequestCallback<Status>)arguments[1];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, status);
                return callback;
            }
        }).when(service).status((ProjectDescriptor)anyObject(), (AsyncRequestCallback<Status>)anyObject());

        presenter.showDialog();

        verify(appContext).getCurrentProject();
        verify(service).status(eq(rootProjectDescriptor), (AsyncRequestCallback<Status>)anyObject());
        verify(view).setIndexedFiles((Array<IndexFile>)anyObject());
        verify(view).showDialog();
    }

    @Test
    public void testShowDialogWhenStatusRequestIsSuccessfulButIndexIsEmpty() throws Exception {
        MessageDialog messageDialog = mock(MessageDialog.class);
        when(constant.messagesWarningTitle()).thenReturn("Warning");
        when(constant.indexIsEmpty()).thenReturn("Index is Empty");
        when(dialogFactory.createMessageDialog(anyString(), anyString(), (ConfirmCallback)anyObject()))
                          .thenReturn(messageDialog);
        final Status status = mock(Status.class);
        List<String> changes = new ArrayList<>();
        when(status.getAdded()).thenReturn(changes);
        when(status.getChanged()).thenReturn(changes);
        when(status.getRemoved()).thenReturn(changes);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Status> callback = (AsyncRequestCallback<Status>)arguments[1];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, status);
                return callback;
            }
        }).when(service).status((ProjectDescriptor)anyObject(), (AsyncRequestCallback<Status>)anyObject());

        presenter.showDialog();

        verify(appContext).getCurrentProject();
        verify(service).status(eq(rootProjectDescriptor), (AsyncRequestCallback<Status>)anyObject());
        verify(dialogFactory).createMessageDialog(eq("Warning"), eq("Index is Empty"),
                                                 (ConfirmCallback)anyObject());
        verify(view, never()).setIndexedFiles((Array<IndexFile>)anyObject());
        verify(view, never()).showDialog();
    }

    @Test
    public void testShowDialogWhenStatusRequestIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Status> callback = (AsyncRequestCallback<Status>)arguments[1];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).status((ProjectDescriptor)anyObject(), (AsyncRequestCallback<Status>)anyObject());

        presenter.showDialog();

        verify(appContext).getCurrentProject();
        verify(service).status(eq(rootProjectDescriptor), (AsyncRequestCallback<Status>)anyObject());
        verify(notificationManager).showNotification((Notification)anyObject());
        verify(constant).statusFailed();
    }

    @Test
    public void testOnResetClickedWhenNothingToReset() throws Exception {
        MessageDialog messageDialog = mock(MessageDialog.class);
        final Status status = mock(Status.class);
        IndexFile indexFile = mock(IndexFile.class);
        when(dtoFactory.createDto(IndexFile.class)).thenReturn(indexFile);
        when(constant.messagesWarningTitle()).thenReturn("Warning");
        when(constant.indexIsEmpty()).thenReturn("Index is Empty");
        when(dialogFactory.createMessageDialog(constant.messagesWarningTitle(), constant.indexIsEmpty(), null)).thenReturn(messageDialog);
        when(indexFile.isIndexed()).thenReturn(true);
        List<String> changes = new ArrayList<String>();
        changes.add("Change");
        when(status.getAdded()).thenReturn(changes);
        when(status.getChanged()).thenReturn(changes);
        when(status.getRemoved()).thenReturn(changes);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Status> callback = (AsyncRequestCallback<Status>)arguments[1];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, status);
                return callback;
            }
        }).when(service).status((ProjectDescriptor)anyObject(), (AsyncRequestCallback<Status>)anyObject());

        presenter.showDialog();
        presenter.onResetClicked();

        verify(view).close();
        verify(service, never()).reset(eq(projectDescriptor), anyString(), (ResetRequest.ResetType)anyObject(), (List<String>)anyObject(),
                                       (AsyncRequestCallback<Void>)anyObject());
        verify(notificationManager).showNotification((Notification)anyObject());
        verify(constant).nothingToReset();
    }

    @Test
    public void testOnResetClickedWhenResetRequestIsSuccessful() throws Exception {
        final Status status = mock(Status.class);
        List<String> changes = new ArrayList<String>();
        changes.add("Change");
        when(status.getAdded()).thenReturn(changes);
        when(status.getChanged()).thenReturn(changes);
        when(status.getRemoved()).thenReturn(changes);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Status> callback = (AsyncRequestCallback<Status>)arguments[1];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, status);
                return callback;
            }
        }).when(service).status((ProjectDescriptor)anyObject(), (AsyncRequestCallback<Status>)anyObject());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[4];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, (Void)null);
                return callback;
            }
        }).when(service).reset((ProjectDescriptor)anyObject(), anyString(), (ResetRequest.ResetType)anyObject(), (List<String>)anyObject(),
                               (AsyncRequestCallback<Void>)anyObject());

        presenter.showDialog();
        presenter.onResetClicked();

        verify(view).close();
        verify(service).reset(eq(rootProjectDescriptor), anyString(), (ResetRequest.ResetType)anyObject(), (List<String>)anyObject(),
                              (AsyncRequestCallback<Void>)anyObject());
        verify(notificationManager).showNotification((Notification)anyObject());
        verify(constant).resetFilesSuccessfully();
    }

    @Test
    public void testOnResetClickedWhenResetRequestIsFailed() throws Exception {
        final Status status = mock(Status.class);
        List<String> changes = new ArrayList<String>();
        changes.add("Change");
        when(status.getAdded()).thenReturn(changes);
        when(status.getChanged()).thenReturn(changes);
        when(status.getRemoved()).thenReturn(changes);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Status> callback = (AsyncRequestCallback<Status>)arguments[1];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, status);
                return callback;
            }
        }).when(service).status((ProjectDescriptor)anyObject(), (AsyncRequestCallback<Status>)anyObject());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[4];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).reset((ProjectDescriptor)anyObject(), anyString(), (ResetRequest.ResetType)anyObject(), (List<String>)anyObject(),
                               (AsyncRequestCallback<Void>)anyObject());

        presenter.showDialog();
        presenter.onResetClicked();

        verify(service).reset(eq(rootProjectDescriptor), anyString(), (ResetRequest.ResetType)anyObject(), (List<String>)anyObject(),
                              (AsyncRequestCallback<Void>)anyObject());
        verify(constant).resetFilesFailed();
        verify(notificationManager).showNotification((Notification)anyObject());
    }

    @Test
    public void testOnCancelClicked() throws Exception {
        presenter.onCancelClicked();

        verify(view).close();
    }
}