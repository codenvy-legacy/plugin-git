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
package com.codenvy.ide.ext.github.client.importer.page;

import com.codenvy.api.project.shared.dto.ProjectImporterDescriptor;
import com.codenvy.api.user.gwt.client.UserServiceClient;
import com.codenvy.api.user.shared.dto.UserDescriptor;
import com.codenvy.ide.api.notification.Notification;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.api.projecttype.wizard.ImportProjectWizard;
import com.codenvy.ide.api.projecttype.wizard.ProjectWizard;
import com.codenvy.ide.api.wizard.Wizard;
import com.codenvy.ide.api.wizard.WizardContext;
import com.codenvy.ide.collections.Array;
import com.codenvy.ide.collections.Collections;
import com.codenvy.ide.collections.StringMap;
import com.codenvy.ide.commons.exception.ExceptionThrownEvent;
import com.codenvy.ide.dto.DtoFactory;
import com.codenvy.ide.ext.github.client.GitHubClientService;
import com.codenvy.ide.ext.github.client.GitHubLocalizationConstant;
import com.codenvy.ide.ext.github.client.load.ProjectData;
import com.codenvy.ide.ext.github.shared.GitHubRepository;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.codenvy.ide.security.oauth.OAuthCallback;
import com.codenvy.ide.ui.dialogs.CancelCallback;
import com.codenvy.ide.ui.dialogs.ConfirmCallback;
import com.codenvy.ide.ui.dialogs.DialogFactory;
import com.codenvy.ide.ui.dialogs.confirm.ConfirmDialog;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.event.shared.EventBus;
import com.googlecode.gwt.test.GwtModule;
import com.googlecode.gwt.test.GwtTestWithMockito;
import com.googlecode.gwt.test.utils.GwtReflectionUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.lang.Iterable;
import java.lang.reflect.Method;
import java.util.Iterator;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link GithubImporterPagePresenter} functionality.
 *
 * @author Roman Nikitenko
 */
@GwtModule("com.codenvy.ide.ext.github.GitHub")
public class GithubImporterPagePresenterTest extends GwtTestWithMockito {

    private WizardContext         wizardContext;
    private Wizard.UpdateDelegate updateDelegate;

    @Captor
    private ArgumentCaptor<ConfirmCallback> confirmCallbackCaptor;

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<UserDescriptor>> asyncRequestCallbackUserDescriptorCaptor;

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<StringMap<Array<GitHubRepository>>>> asyncRequestCallbackRepoListCaptor;

    @Mock
    private DtoFactory                  dtoFactory;
    @Mock
    private UserDescriptor              userDescriptor;
    @Mock
    private GithubImporterPageView      view;
    @Mock
    private UserServiceClient           userServiceClient;
    @Mock
    private GitHubClientService         gitHubClientService;
    @Mock
    private DtoUnmarshallerFactory      dtoUnmarshallerFactory;
    @Mock
    private DialogFactory               dialogFactory;
    @Mock
    private NotificationManager         notificationManager;
    @Mock
    private EventBus                    eventBus;
    @Mock
    private GitHubLocalizationConstant  locale;
    @InjectMocks
    private GithubImporterPagePresenter presenter;

    @Before
    public void setUp() {
        wizardContext = mock(WizardContext.class);
        updateDelegate = mock(Wizard.UpdateDelegate.class);
        presenter.setContext(wizardContext);
        presenter.setProjectWizardDelegate(updateDelegate);

    }

    @Test
    public void delegateShouldBeSet() throws Exception {
        verify(view).setDelegate(presenter);
    }

    @Test
    public void userServiceShouldBeCalled() throws Exception {
        presenter.onLoadRepoClicked();
        InOrder inOrder = inOrder(view, userServiceClient);

        inOrder.verify(view).reset();
        inOrder.verify(view).setLoaderVisibility(true);
        inOrder.verify(userServiceClient).getCurrentUser(asyncRequestCallbackUserDescriptorCaptor.capture());
    }

    @Test
    public void userServiceAsyncCallbackIsSuccess() throws Exception {
        presenter.onLoadRepoClicked();
        verify(userServiceClient).getCurrentUser(asyncRequestCallbackUserDescriptorCaptor.capture());
        AsyncRequestCallback<UserDescriptor> callback = asyncRequestCallbackUserDescriptorCaptor.getValue();

        //noinspection NonJREEmulationClassesInClientCode
        Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
        onSuccess.invoke(callback, userDescriptor);

        verify(view, times(2)).setLoaderVisibility(true);
        verify(gitHubClientService).getAllRepositories(asyncRequestCallbackRepoListCaptor.capture());

        AsyncRequestCallback<StringMap<Array<GitHubRepository>>> callback2 = asyncRequestCallbackRepoListCaptor.getValue();

        StringMap<Array<GitHubRepository>> map = Collections.createStringMap();
        Array<GitHubRepository> list = Collections.createArray();
        list.add(mock(GitHubRepository.class));
        map.put(anyString(), list);

        //noinspection NonJREEmulationClassesInClientCode
        Method onSuccess2 = GwtReflectionUtils.getMethod(callback2.getClass(), "onSuccess");
        onSuccess2.invoke(callback2, map);
        verify(view).setLoaderVisibility(false);
    }

    @Test
    public void testGoWhenGetUserReposIsSuccessful() throws Exception {
        String importerDescription = "description";
        AcceptsOneWidget container = mock(AcceptsOneWidget.class);
        ProjectImporterDescriptor projectImporter = mock(ProjectImporterDescriptor.class);
        when(wizardContext.getData(ImportProjectWizard.PROJECT_IMPORTER)).thenReturn(projectImporter);
        when(projectImporter.getDescription()).thenReturn(importerDescription);

        final StringMap repositories = mock(StringMap.class);
        Array repo = mock(Array.class);
        Iterable iterable = mock(Iterable.class);
        Iterator iterator = mock(Iterator.class);
        when(repositories.get(anyString())).thenReturn(repo);
        when(repo.asIterable()).thenReturn(iterable);
        when(iterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(false);
        when(repositories.getKeys()).thenReturn(repo);
        when(repositories.containsKey(anyString())).thenReturn(true);

        presenter.go(container);

        verify(gitHubClientService).getAllRepositories(asyncRequestCallbackRepoListCaptor.capture());
        AsyncRequestCallback<StringMap<Array<GitHubRepository>>> asyncRequestCallback = asyncRequestCallbackRepoListCaptor.getValue();
        Method onSuccess = GwtReflectionUtils.getMethod(asyncRequestCallback.getClass(), "onSuccess");
        onSuccess.invoke(asyncRequestCallback, repositories);

        verify(view, times(2)).reset();
        verify(wizardContext).getData(eq(ImportProjectWizard.PROJECT_IMPORTER));
        verify(view).setImporterDescription(eq(importerDescription));
        verify(view, times(2)).setInputsEnableState(eq(true));
        verify(container).setWidget(eq(view));
        verify(view).setLoaderVisibility(eq(true));
        verify(view).setInputsEnableState(eq(false));
        verify(view).setAccountNames((Array<String>)anyObject());
        verify(view, times(2)).showGithubPanel();
        verify(view).getAccountName();
        verify(view).setRepositories((Array<ProjectData>)anyObject());
        verify(view).focusInUrlInput();
    }

    @Test
    public void testGoWhenGetUserReposIsFailed() throws Exception {
        String importerDescription = "description";
        AcceptsOneWidget container = mock(AcceptsOneWidget.class);
        ProjectImporterDescriptor projectImporter = mock(ProjectImporterDescriptor.class);
        when(wizardContext.getData(ImportProjectWizard.PROJECT_IMPORTER)).thenReturn(projectImporter);
        when(projectImporter.getDescription()).thenReturn(importerDescription);

        presenter.go(container);

        verify(gitHubClientService).getAllRepositories(asyncRequestCallbackRepoListCaptor.capture());
        AsyncRequestCallback<StringMap<Array<GitHubRepository>>> asyncRequestCallback = asyncRequestCallbackRepoListCaptor.getValue();
        Method onFailure = GwtReflectionUtils.getMethod(asyncRequestCallback.getClass(), "onFailure");
        onFailure.invoke(asyncRequestCallback, mock(Throwable.class));

        verify(view).reset();
        verify(wizardContext).getData(eq(ImportProjectWizard.PROJECT_IMPORTER));
        verify(view).setImporterDescription(eq(importerDescription));
        verify(view, times(2)).setInputsEnableState(eq(true));
        verify(container).setWidget(eq(view));
        verify(view).setLoaderVisibility(eq(true));
        verify(view).setInputsEnableState(eq(false));
        verify(view, never()).setAccountNames((Array<String>)anyObject());
        verify(view, never()).showGithubPanel();
        verify(view, never()).setRepositories((Array<ProjectData>)anyObject());
        verify(view).focusInUrlInput();
    }

    @Test
    public void onLoadRepoClickedWhenGetCurrentUserIsSuccessful() throws Exception {
        presenter.onLoadRepoClicked();

        verify(userServiceClient).getCurrentUser(asyncRequestCallbackUserDescriptorCaptor.capture());
        AsyncRequestCallback<UserDescriptor> callback = asyncRequestCallbackUserDescriptorCaptor.getValue();
        Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
        onSuccess.invoke(callback, userDescriptor);

        verify(userServiceClient).getCurrentUser(Matchers.<AsyncRequestCallback<UserDescriptor>>anyObject());
        verify(view, times(2)).setLoaderVisibility(eq(true));
        verify(view, times(2)).setInputsEnableState(eq(false));
        verify(gitHubClientService).getAllRepositories(Matchers.<AsyncRequestCallback<StringMap<Array<GitHubRepository>>>>anyObject());
        verify(notificationManager, never()).showNotification((Notification)anyObject());
    }

    @Test
    public void onLoadRepoClickedWhenGetCurrentUserIsFailed() throws Exception {
        presenter.onLoadRepoClicked();

        verify(userServiceClient).getCurrentUser(asyncRequestCallbackUserDescriptorCaptor.capture());
        AsyncRequestCallback<UserDescriptor> callback = asyncRequestCallbackUserDescriptorCaptor.getValue();
        Method onFailure = GwtReflectionUtils.getMethod(callback.getClass(), "onFailure");
        onFailure.invoke(callback, mock(Throwable.class));

        verify(userServiceClient).getCurrentUser(Matchers.<AsyncRequestCallback<UserDescriptor>>anyObject());
        verify(view).setLoaderVisibility(eq(true));
        verify(view).setInputsEnableState(eq(false));
        verify(gitHubClientService, never())
                .getAllRepositories(Matchers.<AsyncRequestCallback<StringMap<Array<GitHubRepository>>>>anyObject());
        verify(notificationManager).showNotification((Notification)anyObject());
    }

    @Test
    public void onLoadRepoClickedWhenGetUserReposIsSuccessful() throws Exception {
        final StringMap repositories = mock(StringMap.class);
        Array repo = mock(Array.class);
        Iterable iterable = mock(Iterable.class);
        Iterator iterator = mock(Iterator.class);
        when(repositories.get(anyString())).thenReturn(repo);
        when(repo.asIterable()).thenReturn(iterable);
        when(iterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(false);
        when(repositories.getKeys()).thenReturn(repo);
        when(view.getAccountName()).thenReturn("AccountName");
        when(repositories.containsKey(anyString())).thenReturn(true);

        presenter.onLoadRepoClicked();

        verify(userServiceClient).getCurrentUser(asyncRequestCallbackUserDescriptorCaptor.capture());
        AsyncRequestCallback<UserDescriptor> callback = asyncRequestCallbackUserDescriptorCaptor.getValue();
        Method onSuccessUser = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
        onSuccessUser.invoke(callback, userDescriptor);

        verify(gitHubClientService).getAllRepositories(asyncRequestCallbackRepoListCaptor.capture());
        AsyncRequestCallback<StringMap<Array<GitHubRepository>>> asyncRequestCallback = asyncRequestCallbackRepoListCaptor.getValue();
        Method onSuccessRepo = GwtReflectionUtils.getMethod(asyncRequestCallback.getClass(), "onSuccess");
        onSuccessRepo.invoke(asyncRequestCallback, repositories);

        verify(notificationManager, never()).showNotification((Notification)anyObject());
        verify(userServiceClient).getCurrentUser(Matchers.<AsyncRequestCallback<UserDescriptor>>anyObject());
        verify(view, times(2)).setLoaderVisibility(eq(true));
        verify(view, times(2)).setInputsEnableState(eq(false));
        verify(gitHubClientService).getAllRepositories(Matchers.<AsyncRequestCallback<StringMap<Array<GitHubRepository>>>>anyObject());
        verify(view).setAccountNames((Array<String>)anyObject());
        verify(view, times(2)).showGithubPanel();
        verify(view).setRepositories(Matchers.<Array<ProjectData>>anyObject());
    }

    @Test
    public void onLoadRepoClickedWhenGetUserReposIsFailed() throws Exception {
        final Throwable exception = mock(Throwable.class);
        when(exception.getMessage()).thenReturn("");

        presenter.onLoadRepoClicked();

        verify(userServiceClient).getCurrentUser(asyncRequestCallbackUserDescriptorCaptor.capture());
        AsyncRequestCallback<UserDescriptor> callback = asyncRequestCallbackUserDescriptorCaptor.getValue();
        Method onSuccessUser = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
        onSuccessUser.invoke(callback, userDescriptor);

        verify(gitHubClientService).getAllRepositories(asyncRequestCallbackRepoListCaptor.capture());
        AsyncRequestCallback<StringMap<Array<GitHubRepository>>> asyncRequestCallback = asyncRequestCallbackRepoListCaptor.getValue();
        Method onFailure = GwtReflectionUtils.getMethod(asyncRequestCallback.getClass(), "onFailure");
        onFailure.invoke(asyncRequestCallback, exception);

        verify(notificationManager).showNotification((Notification)anyObject());
        verify(eventBus).fireEvent(Matchers.<ExceptionThrownEvent>anyObject());
        verify(userServiceClient).getCurrentUser(Matchers.<AsyncRequestCallback<UserDescriptor>>anyObject());
        verify(view, times(2)).setLoaderVisibility(eq(true));
        verify(view, times(2)).setInputsEnableState(eq(false));
        verify(gitHubClientService).getAllRepositories(Matchers.<AsyncRequestCallback<StringMap<Array<GitHubRepository>>>>anyObject());
        verify(view, never()).setAccountNames((Array<String>)anyObject());
        verify(view, never()).showGithubPanel();
        verify(view, never()).setRepositories(Matchers.<Array<ProjectData>>anyObject());
    }

    @Test
    public void onLoadRepoClickedWhenShouldShowAuthWindow() throws Exception {
        final Throwable exception = mock(Throwable.class);
        ConfirmDialog confirmDialog = mock(ConfirmDialog.class);
        when(dialogFactory.createConfirmDialog(anyString(), anyString(), (ConfirmCallback)anyObject(), (CancelCallback)anyObject()))
                .thenReturn(confirmDialog);
        when(exception.getMessage()).thenReturn("Bad credentials");

        presenter.onLoadRepoClicked();

        verify(userServiceClient).getCurrentUser(asyncRequestCallbackUserDescriptorCaptor.capture());
        AsyncRequestCallback<UserDescriptor> callback = asyncRequestCallbackUserDescriptorCaptor.getValue();
        Method onSuccessUser = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
        onSuccessUser.invoke(callback, userDescriptor);

        verify(gitHubClientService).getAllRepositories(asyncRequestCallbackRepoListCaptor.capture());
        AsyncRequestCallback<StringMap<Array<GitHubRepository>>> asyncRequestCallback = asyncRequestCallbackRepoListCaptor.getValue();
        Method onFailure = GwtReflectionUtils.getMethod(asyncRequestCallback.getClass(), "onFailure");
        onFailure.invoke(asyncRequestCallback, exception);

        verify(dialogFactory).createConfirmDialog(anyString(), anyString(), confirmCallbackCaptor.capture(), (CancelCallback)anyObject());
        ConfirmCallback confirmCallback = confirmCallbackCaptor.getValue();
        confirmCallback.accepted();

        verify(view).showAuthWindow(anyString(), (OAuthCallback)anyObject());
        verify(notificationManager, never()).showNotification((Notification)anyObject());
        verify(eventBus, never()).fireEvent(Matchers.<ExceptionThrownEvent>anyObject());
        verify(userServiceClient).getCurrentUser(Matchers.<AsyncRequestCallback<UserDescriptor>>anyObject());
        verify(view, times(2)).setLoaderVisibility(eq(true));
        verify(view, times(2)).setInputsEnableState(eq(false));
        verify(gitHubClientService).getAllRepositories(Matchers.<AsyncRequestCallback<StringMap<Array<GitHubRepository>>>>anyObject());
        verify(view, never()).setAccountNames((Array<String>)anyObject());
        verify(view, never()).showGithubPanel();
        verify(view, never()).setRepositories(Matchers.<Array<ProjectData>>anyObject());
    }

    @Test
    public void onRepositorySelectedTest() {
        ProjectData projectData =
                new ProjectData("name", "description", "type", Collections.<String>createArray(), "repoUrl", "readOnlyUrl");

        presenter.onRepositorySelected(projectData);

        verify(view).setProjectName(eq("name"));
        verify(view).setProjectUrl(eq("repoUrl"));
        verify(updateDelegate).updateControls();
    }

    @Test
    public void projectUrlStartWithWhiteSpaceEnteredTest() {
        String incorrectUrl = " https://github.com/codenvy/ide.git";

        presenter.projectUrlChanged(incorrectUrl);

        verify(view).showUrlError(eq(locale.importProjectMessageStartWithWhiteSpace()));
        verify(wizardContext).removeData(eq(ImportProjectWizard.PROJECT_URL));
        verify(wizardContext, never()).putData(eq(ImportProjectWizard.PROJECT_URL), anyString());
        verify(view, never()).setProjectName(anyString());
        verify(updateDelegate).updateControls();
    }

    @Test
    public void testUrlMatchScpLikeSyntax() {
        // test for url with an alternative scp-like syntax: [user@]host.xz:path/to/repo.git/
        String correctUrl = "host.xz:path/to/repo.git";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testUrlWithoutUsername() {
        String correctUrl = "git@hostname.com:projectName.git";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testSshUriWithHostBetweenDoubleSlashAndSlash() {
        //Check for type uri which start with ssh:// and has host between // and /
        String correctUrl = "ssh://host.com/some/path";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testSshUriWithHostBetweenDoubleSlashAndColon() {
        //Check for type uri with host between // and :
        String correctUrl = "ssh://host.com:port/some/path";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testGitUriWithHostBetweenDoubleSlashAndSlash() {
        //Check for type uri which start with git:// and has host between // and /
        String correctUrl = "git://host.com/user/repo";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testSshUriWithHostBetweenAtAndColon() {
        //Check for type uri with host between @ and :
        String correctUrl = "user@host.com:login/repo";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testSshUriWithHostBetweenAtAndSlash() {
        //Check for type uri with host between @ and /
        String correctUrl = "ssh://user@host.com/some/path";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void projectUrlWithIncorrectProtocolEnteredTest() {
        String correctUrl = "htps://github.com/codenvy/ide.git";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verify(view).showUrlError(eq(locale.importProjectMessageProtocolIncorrect()));
        verify(wizardContext).removeData(eq(ImportProjectWizard.PROJECT_URL));
        verify(wizardContext, never()).putData(eq(ImportProjectWizard.PROJECT_URL), anyString());
        verify(view, never()).setProjectName(anyString());
        verify(updateDelegate).updateControls();
    }

    @Test
    public void correctProjectNameEnteredTest() {
        String correctName = "angularjs";

        presenter.projectNameChanged(correctName);

        verify(wizardContext).putData(eq(ProjectWizard.PROJECT_NAME), eq(correctName));
        verify(view).hideNameError();
        verify(view, never()).showNameError();
        verify(updateDelegate).updateControls();
    }

    @Test
    public void correctProjectNameWithPointEnteredTest() {
        String correctName = "Test.project..ForCodenvy";

        presenter.projectNameChanged(correctName);

        verify(wizardContext).putData(eq(ProjectWizard.PROJECT_NAME), eq(correctName));
        verify(view).hideNameError();
        verify(view, never()).showNameError();
        verify(updateDelegate).updateControls();
    }

    @Test
    public void emptyProjectNameEnteredTest() {
        String emptyName = "";

        presenter.projectNameChanged(emptyName);

        verify(wizardContext, never()).putData(eq(ProjectWizard.PROJECT_NAME), anyString());
        verify(wizardContext).removeData(eq(ProjectWizard.PROJECT_NAME));
        verify(updateDelegate).updateControls();
    }

    @Test
    public void replaceSpaceToHyphenTest() {
        String namesWithSpace = "Test project For  Codenvy";
        String fixedName = "Test-project-For--Codenvy";
        presenter.projectNameChanged(namesWithSpace);

        verify(wizardContext).putData(eq(ProjectWizard.PROJECT_NAME), eq(fixedName));
        verify(view).hideNameError();
        verify(updateDelegate).updateControls();
    }

    @Test
    public void incorrectProjectNameEnteredTest() {
        String incorrectName = "angularjs+";

        presenter.projectNameChanged(incorrectName);

        verify(wizardContext, never()).putData(eq(ProjectWizard.PROJECT_NAME), anyString());
        verify(wizardContext).removeData(eq(ProjectWizard.PROJECT_NAME));
        verify(view).showNameError();
        verify(updateDelegate).updateControls();
    }

    @Test
    public void projectDescriptionChangedTest() {
        String description = "description";
        presenter.projectDescriptionChanged(description);

        verify(wizardContext).putData(eq(ProjectWizard.PROJECT_DESCRIPTION), eq(description));
    }

    @Test
    public void projectVisibilityChangedTest() {
        presenter.projectVisibilityChanged(true);

        verify(wizardContext).putData(eq(ProjectWizard.PROJECT_VISIBILITY), eq(true));
    }

    private void verifyInvocationsForCorrectUrl(String correctUrl) {
        verify(view, never()).showUrlError(anyString());
        verify(wizardContext).putData(eq(ImportProjectWizard.PROJECT_URL), eq(correctUrl));
        verify(view).hideUrlError();
        verify(view).setProjectName(anyString());
        verify(updateDelegate, times(2)).updateControls();
    }

}
