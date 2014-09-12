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
package com.codenvy.ide.ext.git.client.reset.commit;

import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.ide.api.editor.EditorAgent;
import com.codenvy.ide.api.editor.EditorInput;
import com.codenvy.ide.api.editor.EditorPartPresenter;
import com.codenvy.ide.api.event.FileEvent;
import com.codenvy.ide.api.notification.Notification;
import com.codenvy.ide.api.projecttree.generic.FileNode;
import com.codenvy.ide.collections.Collections;
import com.codenvy.ide.collections.StringMap;
import com.codenvy.ide.ext.git.client.BaseTest;
import com.codenvy.ide.ext.git.shared.DiffRequest;
import com.codenvy.ide.ext.git.shared.LogResponse;
import com.codenvy.ide.ext.git.shared.ResetRequest;
import com.codenvy.ide.ext.git.shared.Revision;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.googlecode.gwt.test.utils.GwtReflectionUtils;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;
import java.util.ArrayList;

import static com.codenvy.ide.ext.git.shared.ResetRequest.ResetType.HARD;
import static com.codenvy.ide.ext.git.shared.ResetRequest.ResetType.MERGE;
import static com.codenvy.ide.ext.git.shared.ResetRequest.ResetType.MIXED;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link ResetToCommitPresenter} functionality.
 *
 * @author <a href="mailto:aplotnikov@codenvy.com">Andrey Plotnikov</a>
 */
public class ResetToCommitPresenterTest extends BaseTest {
    public static final boolean IS_TEXT_FORMATTED  = true;
    public static final boolean IS_MIXED           = true;
    public static final String  FILE_PATH          = "/src/testClass.java";
    public static final String  DIFF_WITH_NEW_FILE = FILE_PATH + " new file mode " + FILE_PATH;
    public static final String  DIFF_FILE_CHANGED  = FILE_PATH + " " + FILE_PATH;

    @Mock
    private ResetToCommitView      view;
    @Mock
    private FileNode               file;
    @Mock
    private EditorInput            editorInput;
    @Mock
    private EditorAgent            editorAgent;
    @Mock
    private EditorPartPresenter    partPresenter;
    @Mock
    private Revision               selectedRevision;
    @InjectMocks
    private ResetToCommitPresenter presenter;

    @Override
    public void disarm() {
        super.disarm();

        presenter = new ResetToCommitPresenter(view, service, constant, eventBus, editorAgent, appContext, notificationManager,
                                               dtoUnmarshallerFactory);

        StringMap<EditorPartPresenter> partPresenterMap = Collections.createStringMap();
        partPresenterMap.put("partPresenter", partPresenter);

        when(view.isMixMode()).thenReturn(IS_MIXED);
        when(selectedRevision.getId()).thenReturn(PROJECT_PATH);
        when(editorAgent.getOpenedEditors()).thenReturn(partPresenterMap);
        when(partPresenter.getEditorInput()).thenReturn(editorInput);
        when(editorInput.getFile()).thenReturn(file);
        when(file.getPath()).thenReturn(FILE_PATH);
    }

    @Test
    public void testShowDialogWhenLogRequestIsSuccessful() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<String> callback = (AsyncRequestCallback<String>)arguments[2];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, mock(LogResponse.class));
                return callback;

            }
        }).when(service).log((ProjectDescriptor)anyObject(), anyBoolean(), (AsyncRequestCallback<LogResponse>)anyObject());

        presenter.showDialog();

        verify(appContext).getCurrentProject();
        verify(service).log(eq(rootProjectDescriptor), eq(!IS_TEXT_FORMATTED), (AsyncRequestCallback<LogResponse>)anyObject());
        verify(view).setRevisions((ArrayList<Revision>)anyObject());
        verify(view).setMixMode(eq(IS_MIXED));
        verify(view).setEnableResetButton(eq(DISABLE_BUTTON));
        verify(view).showDialog();
    }

    @Test
    public void testShowDialogWhenLogRequestIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<String> callback = (AsyncRequestCallback<String>)arguments[2];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;

            }
        }).when(service).log((ProjectDescriptor)anyObject(), anyBoolean(), (AsyncRequestCallback<LogResponse>)anyObject());

        presenter.showDialog();

        verify(appContext).getCurrentProject();
        verify(service).log(eq(rootProjectDescriptor), eq(!IS_TEXT_FORMATTED), (AsyncRequestCallback<LogResponse>)anyObject());
        verify(constant).logFailed();
        verify(notificationManager).showNotification((Notification)anyObject());
    }

//    @Test
//    public void testOnResetClickedWhenResetTypeNotEqualsHardOrMergeAndDiffAndResetRequestsIsSuccessful() throws Exception {
//        when(view.isSoftMode()).thenReturn(true);
//        doAnswer(new Answer() {
//            @Override
//            public Object answer(InvocationOnMock invocation) throws Throwable {
//                Object[] arguments = invocation.getArguments();
//                AsyncRequestCallback<String> callback = (AsyncRequestCallback<String>)arguments[7];
//                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
//                onSuccess.invoke(callback, DIFF_WITH_NEW_FILE);
//                return callback;
//            }
//        }).when(service).diff(eq(rootProjectDescriptor), anyList(), eq(DiffRequest.DiffType.RAW), eq(true), eq(0), eq(PROJECT_PATH), eq(false),
//                              (AsyncRequestCallback<String>)anyObject());
//
//        doAnswer(new Answer() {
//            @Override
//            public Object answer(InvocationOnMock invocation) throws Throwable {
//                Object[] arguments = invocation.getArguments();
//                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[3];
//                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
//                onSuccess.invoke(callback, (Void)null);
//                return callback;
//            }
//        }).when(service)
//          .reset((ProjectDescriptor)anyObject(), anyString(), (ResetRequest.ResetType)anyObject(), (AsyncRequestCallback<Void>)anyObject());
//
//        presenter.onRevisionSelected(selectedRevision);
//        presenter.onResetClicked();
//
//        verify(view).close();
//        verify(selectedRevision, times(2)).getId();
//        verify(appContext, times(2)).getCurrentProject();
//        verify(service).diff(eq(rootProjectDescriptor), anyList(), eq(DiffRequest.DiffType.RAW), eq(true), eq(0), eq(PROJECT_PATH), eq(false),
//                             (AsyncRequestCallback<String>)anyObject());
//        verify(service).reset((ProjectDescriptor)anyObject(), eq(PROJECT_PATH), eq(MIXED), (AsyncRequestCallback<Void>)anyObject());
//        verify(notificationManager).showNotification((Notification)anyObject());
//    }

    @Test
    public void testOnResetClickedWhenResetTypeEqualsHardOrMergeAndFileIsNotExistInCommitToResetAndDiffAndResetRequestsIsSuccessful()
            throws Exception {
        when(view.isMixMode()).thenReturn(false);
        when(view.isHardMode()).thenReturn(true);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<String> callback = (AsyncRequestCallback<String>)arguments[7];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, DIFF_WITH_NEW_FILE);
                return callback;
            }
        }).when(service).diff(eq(rootProjectDescriptor), anyList(), eq(DiffRequest.DiffType.RAW), eq(true), eq(0), eq(PROJECT_PATH), eq(false),
                              (AsyncRequestCallback<String>)anyObject());

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[3];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, (Void)null);
                return callback;
            }
        }).when(service)
          .reset((ProjectDescriptor)anyObject(), anyString(), (ResetRequest.ResetType)anyObject(), (AsyncRequestCallback<Void>)anyObject());

        presenter.onRevisionSelected(selectedRevision);
        presenter.onResetClicked();

        verify(view).close();
        verify(selectedRevision, times(2)).getId();
        verify(appContext, times(3)).getCurrentProject();
        verify(service).diff(eq(rootProjectDescriptor), anyList(), eq(DiffRequest.DiffType.RAW), eq(true), eq(0), eq(PROJECT_PATH), eq(false),
                             (AsyncRequestCallback<String>)anyObject());
        verify(service).reset((ProjectDescriptor)anyObject(), eq(PROJECT_PATH), eq(HARD), (AsyncRequestCallback<Void>)anyObject());
        verify(eventBus, times(2)).fireEvent((FileEvent)anyObject());
        verify(notificationManager).showNotification((Notification)anyObject());
    }

//    @Test
//    public void testOnResetClickedWhenResetTypeEqualsHardOrMergeAndWhenFileIsChangedInCommitToResetAndDiffAndResetRequestsIsSuccessful()
//            throws Exception {
//        when(view.isMixMode()).thenReturn(false);
////        when(view.isMergeMode()).thenReturn(true);
//
//        doAnswer(new Answer() {
//            @Override
//            public Object answer(InvocationOnMock invocation) throws Throwable {
//                Object[] arguments = invocation.getArguments();
//                AsyncRequestCallback<String> callback = (AsyncRequestCallback<String>)arguments[7];
//                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
//                onSuccess.invoke(callback, DIFF_FILE_CHANGED);
//                return callback;
//            }
//        }).when(service).diff(eq(rootProjectDescriptor), anyList(), eq(DiffRequest.DiffType.RAW), eq(true), eq(0), eq(PROJECT_PATH), eq(false),
//                              (AsyncRequestCallback<String>)anyObject());
//
//        doAnswer(new Answer() {
//            @Override
//            public Object answer(InvocationOnMock invocation) throws Throwable {
//                Object[] arguments = invocation.getArguments();
//                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[3];
//                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
//                onSuccess.invoke(callback, (Void)null);
//                return callback;
//            }
//        }).when(service)
//          .reset((ProjectDescriptor)anyObject(), anyString(), (ResetRequest.ResetType)anyObject(), (AsyncRequestCallback<Void>)anyObject());
//
//        presenter.onRevisionSelected(selectedRevision);
//        presenter.onResetClicked();
//
//        verify(view).close();
//        verify(selectedRevision, times(2)).getId();
//        verify(appContext, times(3)).getCurrentProject();
//        verify(service).diff(eq(rootProjectDescriptor), anyList(), eq(DiffRequest.DiffType.RAW), eq(true), eq(0), eq(PROJECT_PATH), eq(false),
//                             (AsyncRequestCallback<String>)anyObject());
//        verify(service).reset((ProjectDescriptor)anyObject(), eq(PROJECT_PATH), eq(MERGE), (AsyncRequestCallback<Void>)anyObject());
//        verify(partPresenter).init(eq(editorInput));
//        verify(notificationManager).showNotification((Notification)anyObject());
//    }

    @Test
    public void testOnResetClickedWhenDiffRequestIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<String> callback = (AsyncRequestCallback<String>)arguments[7];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).diff(eq(projectDescriptor), anyList(), eq(DiffRequest.DiffType.RAW), eq(true), eq(0), eq(PROJECT_PATH), eq(false),
                              (AsyncRequestCallback<String>)anyObject());

        presenter.onRevisionSelected(selectedRevision);
        presenter.onResetClicked();

        verify(view).close();
        verify(selectedRevision).getId();
        verify(appContext).getCurrentProject();
        verify(service).diff(eq(rootProjectDescriptor), anyList(), eq(DiffRequest.DiffType.RAW), eq(true), eq(0), eq(PROJECT_PATH), eq(false),
                             (AsyncRequestCallback<String>)anyObject());
        verify(service, never()).reset((ProjectDescriptor)anyObject(), anyString(), (ResetRequest.ResetType)anyObject(),
                                       (AsyncRequestCallback<Void>)anyObject());
    }

    @Test
    public void testOnResetClickedWhenDiffRequestIsSuccessfulButResetRequestIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<String> callback = (AsyncRequestCallback<String>)arguments[7];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, DIFF_FILE_CHANGED);
                return callback;
            }
        }).when(service).diff(eq(rootProjectDescriptor), anyList(), eq(DiffRequest.DiffType.RAW), eq(true), eq(0), eq(PROJECT_PATH), eq(false),
                              (AsyncRequestCallback<String>)anyObject());

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[3];
                Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
                onFailure.invoke(callback, mock(Throwable.class));
                return callback;
            }
        }).when(service).reset((ProjectDescriptor)anyObject(), anyString(), (ResetRequest.ResetType)anyObject(),
                               (AsyncRequestCallback<Void>)anyObject());

        presenter.onRevisionSelected(selectedRevision);
        presenter.onResetClicked();

        verify(view).close();
        verify(selectedRevision, times(2)).getId();
        verify(appContext, times(2)).getCurrentProject();
        verify(service).diff(eq(rootProjectDescriptor), anyList(), eq(DiffRequest.DiffType.RAW), eq(true), eq(0), eq(PROJECT_PATH), eq(false),
                             (AsyncRequestCallback<String>)anyObject());
        verify(service).reset((ProjectDescriptor)anyObject(), eq(PROJECT_PATH), eq(MIXED), (AsyncRequestCallback<Void>)anyObject());
        verify(notificationManager).showNotification((Notification)anyObject());
    }

    @Test
    public void testOnCancelClicked() throws Exception {
        presenter.onCancelClicked();

        verify(view).close();
    }

    @Test
    public void testOnRevisionSelected() throws Exception {
        presenter.onRevisionSelected(selectedRevision);

        verify(view).setEnableResetButton(eq(ENABLE_BUTTON));
    }
}