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
package com.codenvy.ide.ext.git.client.add;

import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.ide.api.projecttree.generic.FileNode;
import com.codenvy.ide.api.projecttree.generic.FolderNode;
import com.codenvy.ide.api.projecttree.generic.ProjectNode;
import com.codenvy.ide.api.selection.Selection;
import com.codenvy.ide.api.selection.SelectionAgent;
import com.codenvy.ide.ext.git.client.BaseTest;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.websocket.WebSocketException;
import com.codenvy.ide.websocket.rest.RequestCallback;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.googlecode.gwt.test.utils.GwtReflectionUtils;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.lang.reflect.Method;
import java.util.List;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link AddToIndexPresenter} functionality.
 *
 * @author Andrey Plotnikov
 */
public class AddToIndexPresenterTest extends BaseTest {
    public static final boolean  NEED_UPDATING = true;
    public static final SafeHtml SAFE_HTML     = mock(SafeHtml.class);
    public static final String   MESSAGE       = "message";
    public static final String   STATUS_TEXT   = "Changes not staged for commit";

    @Captor
    private ArgumentCaptor<RequestCallback<Void>>        requestCallbackAddToIndexCaptor;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<String>> asyncRequestCallbackStatusCaptor;

    @Mock
    private AddToIndexView      view;
    @Mock
    private SelectionAgent      selectionAgent;
    private AddToIndexPresenter presenter;

    @Override
    public void disarm() {
        super.disarm();
        presenter = new AddToIndexPresenter(view, service, constant, appContext, selectionAgent, notificationManager);
    }

    @Test
    public void testDialogWillNotBeShownWhenStatusRequestIsFailed() throws Exception {
        presenter.showDialog();

        verify(service).statusText(eq(rootProjectDescriptor), eq(false), asyncRequestCallbackStatusCaptor.capture());
        AsyncRequestCallback<String> callback = asyncRequestCallbackStatusCaptor.getValue();

        //noinspection NonJREEmulationClassesInClientCode
        Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
        onFailure.invoke(callback, mock(Throwable.class));

        verify(notificationManager).showError(anyString());
        verify(view, never()).showDialog();
        verify(constant).statusFailed();
    }

    @Test
    public void testDialogWillNotBeShownWhenNothingAddToIndex() throws Exception {
        presenter.showDialog();

        verify(service).statusText(eq(rootProjectDescriptor), eq(false), asyncRequestCallbackStatusCaptor.capture());
        AsyncRequestCallback<String> callback = asyncRequestCallbackStatusCaptor.getValue();

        //noinspection NonJREEmulationClassesInClientCode
        Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
        onSuccess.invoke(callback, "working directory clean");

        verify(notificationManager).showInfo(anyString());
        verify(view, never()).showDialog();
        verify(constant).nothingAddToIndex();
    }

    @Test
    public void testShowDialogWhenRootFolderIsSelected() throws Exception {
        Selection selection = mock(Selection.class);
        ProjectNode project = mock(ProjectNode.class);
        when(project.getPath()).thenReturn(PROJECT_PATH);
        when(selection.getFirstElement()).thenReturn(project);
        when(selectionAgent.getSelection()).thenReturn(selection);
        when(constant.addToIndexAllChanges()).thenReturn(MESSAGE);

        presenter.showDialog();

        verify(service).statusText(eq(rootProjectDescriptor), eq(false), asyncRequestCallbackStatusCaptor.capture());
        AsyncRequestCallback<String> callback = asyncRequestCallbackStatusCaptor.getValue();

        //noinspection NonJREEmulationClassesInClientCode
        Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
        onSuccess.invoke(callback, STATUS_TEXT);

        verify(appContext).getCurrentProject();
        verify(constant).addToIndexAllChanges();
        verify(view).setMessage(eq(MESSAGE));
        verify(view).setUpdated(anyBoolean());
        verify(view).showDialog();
    }

    @Test
    public void testShowDialogWhenSomeFolderIsSelected() throws Exception {
        String folderPath = PROJECT_PATH + PROJECT_NAME;
        Selection selection = mock(Selection.class);
        FolderNode folder = mock(FolderNode.class);
        when(folder.getPath()).thenReturn(folderPath);
        when(selection.getFirstElement()).thenReturn(folder);
        when(selectionAgent.getSelection()).thenReturn(selection);
        when(constant.addToIndexFolder(anyString())).thenReturn(SAFE_HTML);

        presenter.showDialog();

        verify(service).statusText(eq(rootProjectDescriptor), eq(false), asyncRequestCallbackStatusCaptor.capture());
        AsyncRequestCallback<String> callback = asyncRequestCallbackStatusCaptor.getValue();

        //noinspection NonJREEmulationClassesInClientCode
        Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
        onSuccess.invoke(callback, STATUS_TEXT);

        verify(appContext).getCurrentProject();
        verify(constant).addToIndexFolder(eq(PROJECT_NAME));
        verify(view).setMessage(eq(MESSAGE));
        verify(view).setUpdated(anyBoolean());
        verify(view).showDialog();
    }

    @Test
    public void testShowDialogWhenSomeFileIsSelected() throws Exception {
        String filePath = PROJECT_PATH + PROJECT_NAME;
        Selection selection = mock(Selection.class);
        FileNode file = mock(FileNode.class);
        when(file.getPath()).thenReturn(filePath);
        when(selection.getFirstElement()).thenReturn(file);
        when(selectionAgent.getSelection()).thenReturn(selection);
        when(constant.addToIndexFile(anyString())).thenReturn(SAFE_HTML);
        when(SAFE_HTML.asString()).thenReturn(MESSAGE);

        presenter.showDialog();

        verify(service).statusText(eq(rootProjectDescriptor), eq(false), asyncRequestCallbackStatusCaptor.capture());
        AsyncRequestCallback<String> callback = asyncRequestCallbackStatusCaptor.getValue();

        //noinspection NonJREEmulationClassesInClientCode
        Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
        onSuccess.invoke(callback, STATUS_TEXT);

        verify(appContext).getCurrentProject();
        verify(constant).addToIndexFile(eq(PROJECT_NAME));
        verify(view).setMessage(eq(MESSAGE));
        verify(view).setUpdated(anyBoolean());
        verify(view).showDialog();
    }

    @Test
    public void testOnAddClickedWhenAddWSRequestIsSuccessful() throws Exception {
        when(view.isUpdated()).thenReturn(NEED_UPDATING);
        when(constant.addSuccess()).thenReturn(MESSAGE);

        presenter.showDialog();
        presenter.onAddClicked();

        verify(service)
                .add(eq(rootProjectDescriptor), eq(NEED_UPDATING), (List<String>)anyObject(), requestCallbackAddToIndexCaptor.capture());
        RequestCallback<Void> callback = requestCallbackAddToIndexCaptor.getValue();

        //noinspection NonJREEmulationClassesInClientCode
        Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
        onSuccess.invoke(callback, (Void)null);

        verify(view).isUpdated();
        verify(view).close();
        verify(service).add(eq(rootProjectDescriptor), eq(NEED_UPDATING), (List<String>)anyObject(),
                            (RequestCallback<Void>)anyObject());
        verify(notificationManager).showInfo(anyString());
        verify(constant).addSuccess();
    }

    @Test
    public void testOnAddClickedWhenAddWSRequestIsFailed() throws Exception {
        when(view.isUpdated()).thenReturn(NEED_UPDATING);

        presenter.showDialog();
        presenter.onAddClicked();

        verify(service)
                .add(eq(rootProjectDescriptor), eq(NEED_UPDATING), (List<String>)anyObject(), requestCallbackAddToIndexCaptor.capture());
        RequestCallback<Void> callback = requestCallbackAddToIndexCaptor.getValue();

        //noinspection NonJREEmulationClassesInClientCode
        Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
        onFailure.invoke(callback, mock(Throwable.class));

        verify(view).isUpdated();
        verify(view).close();
        verify(notificationManager).showError(anyString());
        verify(constant).addFailed();
    }

    @Test
    public void testOnAddClickedWhenAddRequestIsFailed() throws Exception {
        doThrow(WebSocketException.class).when(service)
                                         .add((ProjectDescriptor)anyObject(), anyBoolean(), (List<String>)anyObject(),
                                              (RequestCallback<Void>)anyObject());
        when(view.isUpdated()).thenReturn(NEED_UPDATING);

        presenter.showDialog();
        presenter.onAddClicked();

        verify(view).isUpdated();
        verify(service)
                .add(eq(rootProjectDescriptor), eq(NEED_UPDATING), (List<String>)anyObject(),
                     (RequestCallback<Void>)anyObject());
        verify(view).close();
        verify(notificationManager).showError(anyString());
        verify(constant).addFailed();
    }

    @Test
    public void testOnCancelClicked() throws Exception {
        presenter.onCancelClicked();

        verify(view).close();
    }
}