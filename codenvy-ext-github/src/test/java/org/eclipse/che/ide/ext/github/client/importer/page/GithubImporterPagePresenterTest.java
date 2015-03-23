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
package org.eclipse.che.ide.ext.github.client.importer.page;

import org.eclipse.che.api.project.shared.dto.ImportProject;
import org.eclipse.che.api.project.shared.dto.ImportSourceDescriptor;
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.api.project.shared.dto.ProjectImporterDescriptor;
import org.eclipse.che.api.project.shared.dto.Source;
import org.eclipse.che.api.user.gwt.client.UserServiceClient;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.wizard.Wizard;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.Collections;
import org.eclipse.che.ide.collections.StringMap;
import org.eclipse.che.ide.commons.exception.ExceptionThrownEvent;
import org.eclipse.che.ide.commons.exception.UnauthorizedException;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.github.client.GitHubClientService;
import org.eclipse.che.ide.ext.github.client.GitHubLocalizationConstant;
import org.eclipse.che.ide.ext.github.client.authenticator.GitHubAuthenticator;
import org.eclipse.che.ide.ext.github.client.load.ProjectData;
import org.eclipse.che.ide.ext.github.shared.GitHubRepository;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.security.oauth.OAuthStatus;
import org.eclipse.che.test.GwtReflectionUtils;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.EventBus;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.util.Iterator;
import java.util.Map;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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
@RunWith(GwtMockitoTestRunner.class)
public class GithubImporterPagePresenterTest {

    @Captor
    private ArgumentCaptor<AsyncCallback<OAuthStatus>> asyncCallbackCaptor;

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<StringMap<Array<GitHubRepository>>>> asyncRequestCallbackRepoListCaptor;

    @Mock
    private Wizard.UpdateDelegate       updateDelegate;
    @Mock
    private DtoFactory                  dtoFactory;
    @Mock
    private GithubImporterPageView      view;
    @Mock
    private UserServiceClient           userServiceClient;
    @Mock
    private GitHubClientService         gitHubClientService;
    @Mock
    private DtoUnmarshallerFactory      dtoUnmarshallerFactory;
    @Mock
    private NotificationManager         notificationManager;
    @Mock
    private EventBus                    eventBus;
    @Mock
    private GitHubLocalizationConstant  locale;
    @Mock
    private ImportProject               dataObject;
    @Mock
    private ImportSourceDescriptor      importSourceDescriptor;
    @Mock
    private NewProject                  newProject;
    @Mock
    private Map<String, String>         parameters;
    @Mock
    private GitHubAuthenticator         gitHubAuthenticator;
    @InjectMocks
    private GithubImporterPagePresenter presenter;

    @Before
    public void setUp() {
        Source source = mock(Source.class);
        when(importSourceDescriptor.getParameters()).thenReturn(parameters);
        when(source.getProject()).thenReturn(importSourceDescriptor);
        when(dataObject.getSource()).thenReturn(source);
        when(dataObject.getProject()).thenReturn(newProject);

        presenter.setUpdateDelegate(updateDelegate);
        presenter.init(dataObject);
    }

    @Test
    public void delegateShouldBeSet() throws Exception {
        verify(view).setDelegate(presenter);
    }

    @Test
    public void testGo() throws Exception {
        String importerDescription = "description";
        AcceptsOneWidget container = mock(AcceptsOneWidget.class);
        ProjectImporterDescriptor projectImporter = mock(ProjectImporterDescriptor.class);
        //when(wizardContext.getData(ImportProjectWizard.PROJECT_IMPORTER)).thenReturn(projectImporter);
        when(projectImporter.getDescription()).thenReturn(importerDescription);

        presenter.go(container);

        verify(view).setInputsEnableState(eq(true));
        verify(container).setWidget(eq(view));
        verify(view).focusInUrlInput();
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

        verify(gitHubClientService).getAllRepositories(asyncRequestCallbackRepoListCaptor.capture());
        AsyncRequestCallback<StringMap<Array<GitHubRepository>>> asyncRequestCallback = asyncRequestCallbackRepoListCaptor.getValue();
        GwtReflectionUtils.callOnSuccess(asyncRequestCallback, repositories);

        verify(notificationManager, never()).showError(anyString());
        verify(view).setLoaderVisibility(eq(true));
        verify(view).setInputsEnableState(eq(false));
        verify(view).setLoaderVisibility(eq(false));
        verify(view).setInputsEnableState(eq(true));
        verify(gitHubClientService).getAllRepositories(Matchers.<AsyncRequestCallback<StringMap<Array<GitHubRepository>>>>anyObject());
        verify(view).setAccountNames((Array<String>)anyObject());
        verify(view, times(2)).showGithubPanel();
        verify(view).setRepositories(Matchers.<Array<ProjectData>>anyObject());
        verify(view).reset();
    }

    @Test
    public void onLoadRepoClickedWhenGetUserReposIsFailed() throws Exception {
        final Throwable exception = mock(Throwable.class);
        when(exception.getMessage()).thenReturn("");

        presenter.onLoadRepoClicked();

        verify(gitHubClientService).getAllRepositories(asyncRequestCallbackRepoListCaptor.capture());
        AsyncRequestCallback<StringMap<Array<GitHubRepository>>> asyncRequestCallback = asyncRequestCallbackRepoListCaptor.getValue();
        GwtReflectionUtils.callOnFailure(asyncRequestCallback,exception);

        verify(notificationManager).showError(anyString());
        verify(eventBus).fireEvent(Matchers.<ExceptionThrownEvent>anyObject());
        verify(view).setLoaderVisibility(eq(true));
        verify(view).setInputsEnableState(eq(false));
        verify(view).setLoaderVisibility(eq(false));
        verify(view).setInputsEnableState(eq(true));
        verify(gitHubClientService).getAllRepositories(Matchers.<AsyncRequestCallback<StringMap<Array<GitHubRepository>>>>anyObject());
        verify(view, never()).setAccountNames((Array<String>)anyObject());
        verify(view, never()).showGithubPanel();
        verify(view, never()).setRepositories(Matchers.<Array<ProjectData>>anyObject());
    }

    @Test
    public void onRepositorySelectedTest() {
        ProjectData projectData = new ProjectData("name", "description", "type", Collections.<String>createArray(), "repoUrl", "readOnlyUrl");

        presenter.onRepositorySelected(projectData);

        verify(newProject).setName(eq("name"));
        verify(newProject).setDescription(eq("description"));
        verify(importSourceDescriptor).setLocation(eq("repoUrl"));
        verify(view).setProjectName(anyString());
        verify(view).setProjectDescription(anyString());
        verify(view).setProjectUrl(anyString());
        verify(updateDelegate).updateControls();
    }

    @Test
    public void projectUrlStartWithWhiteSpaceEnteredTest() {
        String incorrectUrl = " https://github.com/codenvy/ide.git";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(incorrectUrl);

        verify(view).showUrlError(eq(locale.importProjectMessageStartWithWhiteSpace()));
        verify(importSourceDescriptor).setLocation(eq(incorrectUrl));
        verify(view).setProjectName(anyString());
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
        verify(importSourceDescriptor).setLocation(eq(correctUrl));
        verify(view).setProjectName(anyString());
        verify(updateDelegate).updateControls();
    }

    @Test
    public void correctProjectNameEnteredTest() {
        String correctName = "angularjs";
        when(view.getProjectName()).thenReturn(correctName);

        presenter.projectNameChanged(correctName);

        verify(newProject).setName(eq(correctName));
        verify(view).hideNameError();
        verify(view, never()).showNameError();
        verify(updateDelegate).updateControls();
    }

    @Test
    public void correctProjectNameWithPointEnteredTest() {
        String correctName = "Test.project..ForCodenvy";
        when(view.getProjectName()).thenReturn(correctName);

        presenter.projectNameChanged(correctName);

        verify(newProject).setName(eq(correctName));
        verify(view).hideNameError();
        verify(view, never()).showNameError();
        verify(updateDelegate).updateControls();
    }

    @Test
    public void emptyProjectNameEnteredTest() {
        String emptyName = "";
        when(view.getProjectName()).thenReturn(emptyName);

        presenter.projectNameChanged(emptyName);

        verify(newProject).setName(eq(emptyName));
        verify(updateDelegate).updateControls();
    }

    @Test
    public void incorrectProjectNameEnteredTest() {
        String incorrectName = "angularjs+";
        when(view.getProjectName()).thenReturn(incorrectName);

        presenter.projectNameChanged(incorrectName);

        verify(newProject).setName(eq(incorrectName));
        verify(view).showNameError();
        verify(updateDelegate).updateControls();
    }

    @Test
    public void projectDescriptionChangedTest() {
        String description = "description";
        presenter.projectDescriptionChanged(description);

        verify(newProject).setDescription(eq(description));
    }

    @Test
    public void projectVisibilityChangedTest() {
        presenter.projectVisibilityChanged(true);

        verify(newProject).setVisibility(eq("public"));
    }

    @Test
    public void onLoadRepoClickedWhenAuthorizeIsFailed() throws Exception {
        final Throwable exception = mock(UnauthorizedException.class);

        presenter.onLoadRepoClicked();

        verify(gitHubClientService).getAllRepositories(asyncRequestCallbackRepoListCaptor.capture());
        AsyncRequestCallback<StringMap<Array<GitHubRepository>>> asyncRequestCallback = asyncRequestCallbackRepoListCaptor.getValue();
        GwtReflectionUtils.callOnFailure(asyncRequestCallback, exception);

        verify(gitHubAuthenticator).authorize(asyncCallbackCaptor.capture());
        AsyncCallback<OAuthStatus> asyncCallback= asyncCallbackCaptor.getValue();
        asyncCallback.onFailure(exception);

        verify(view, times(2)).setLoaderVisibility(eq(true));
        verify(view, times(2)).setInputsEnableState(eq(false));
        verify(view, times(2)).setInputsEnableState(eq(true));
        verify(gitHubClientService).getAllRepositories(Matchers.<AsyncRequestCallback<StringMap<Array<GitHubRepository>>>>anyObject());
        verify(view, never()).setAccountNames((Array<String>)anyObject());
        verify(view, never()).showGithubPanel();
        verify(view, never()).setRepositories(Matchers.<Array<ProjectData>>anyObject());
    }

    @Test
    public void onLoadRepoClickedWhenAuthorizeIsSuccessful() throws Exception {
        final Throwable exception = mock(UnauthorizedException.class);

        presenter.onLoadRepoClicked();

        verify(gitHubClientService).getAllRepositories(asyncRequestCallbackRepoListCaptor.capture());
        AsyncRequestCallback<StringMap<Array<GitHubRepository>>> asyncRequestCallback = asyncRequestCallbackRepoListCaptor.getValue();
        GwtReflectionUtils.callOnFailure(asyncRequestCallback, exception);

        verify(gitHubAuthenticator).authorize(asyncCallbackCaptor.capture());
        AsyncCallback<OAuthStatus> asyncCallback= asyncCallbackCaptor.getValue();
        asyncCallback.onSuccess(null);

        verify(view, times(3)).setLoaderVisibility(eq(true));
        verify(view, times(3)).setInputsEnableState(eq(false));
        verify(view, times(2)).setInputsEnableState(eq(true));
        verify(gitHubClientService, times(2)).getAllRepositories(Matchers.<AsyncRequestCallback<StringMap<Array<GitHubRepository>>>>anyObject());
    }

    private void verifyInvocationsForCorrectUrl(String correctUrl) {
        verify(view, never()).showUrlError(anyString());
        verify(importSourceDescriptor).setLocation(eq(correctUrl));
        verify(view).hideUrlError();
        verify(view).setProjectName(anyString());
        verify(updateDelegate).updateControls();
    }

}
