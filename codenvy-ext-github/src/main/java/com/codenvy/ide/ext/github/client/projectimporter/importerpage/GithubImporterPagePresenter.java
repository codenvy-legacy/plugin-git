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
package com.codenvy.ide.ext.github.client.projectimporter.importerpage;

import com.codenvy.api.project.shared.dto.ProjectImporterDescriptor;
import com.codenvy.api.user.gwt.client.UserServiceClient;
import com.codenvy.api.user.shared.dto.UserDescriptor;
import com.codenvy.ide.api.notification.Notification;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.api.projectimporter.ImporterPagePresenter;
import com.codenvy.ide.api.projectimporter.basepage.ImporterBasePageListener;
import com.codenvy.ide.api.projectimporter.basepage.ImporterBasePagePresenter;
import com.codenvy.ide.api.projecttype.wizard.ImportProjectWizard;
import com.codenvy.ide.api.projecttype.wizard.ProjectWizard;
import com.codenvy.ide.api.wizard.Wizard;
import com.codenvy.ide.api.wizard.WizardContext;
import com.codenvy.ide.collections.Array;
import com.codenvy.ide.collections.Collections;
import com.codenvy.ide.collections.StringMap;
import com.codenvy.ide.commons.exception.ExceptionThrownEvent;
import com.codenvy.ide.dto.DtoFactory;
import com.codenvy.ide.ext.git.client.GitLocalizationConstant;
import com.codenvy.ide.ext.github.client.GitHubClientService;
import com.codenvy.ide.ext.github.client.load.ProjectData;
import com.codenvy.ide.ext.github.client.marshaller.AllRepositoriesUnmarshaller;
import com.codenvy.ide.ext.github.shared.GitHubRepository;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.codenvy.ide.security.oauth.JsOAuthWindow;
import com.codenvy.ide.security.oauth.OAuthCallback;
import com.codenvy.ide.security.oauth.OAuthStatus;
import com.codenvy.ide.util.Config;
import com.codenvy.ide.util.loging.Log;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;

import static com.codenvy.ide.api.notification.Notification.Type.*;

/**
 * @author Roman Nikitenko
 */
public class GithubImporterPagePresenter implements ImporterPagePresenter, GithubImporterPageView.ActionDelegate, ImporterBasePageListener,
                                                    OAuthCallback {

    private static final RegExp NAME_PATTERN    = RegExp.compile("^[A-Za-z0-9_-]*$");
    // An alternative scp-like syntax: [user@]host.xz:path/to/repo.git/
    private static final RegExp SCP_LIKE_SYNTAX = RegExp.compile("([A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-:]+)+:");
    // the transport protocol
    private static final RegExp PROTOCOL        = RegExp.compile("((http|https|git|ssh|ftp|ftps)://)");
    // the address of the remote server between // and /
    private static final RegExp HOST1           = RegExp.compile("//([A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-:]+)+/");
    // the address of the remote server between @ and : or /
    private static final RegExp HOST2           = RegExp.compile("@([A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-:]+)+[:/]");
    // the repository name
    private static final RegExp REPO_NAME       = RegExp.compile("/[A-Za-z0-9_.\\-]+$");
    // start with white space
    private static final RegExp WHITE_SPACE     = RegExp.compile("^\\s");

    private       NotificationManager                notificationManager;
    private final UserServiceClient                  userServiceClient;
    private       GitHubClientService                gitHubClientService;
    private final DtoUnmarshallerFactory             dtoUnmarshallerFactory;
    private final DtoFactory                         dtoFactory;
    private       EventBus                           eventBus;
    private       StringMap<Array<GitHubRepository>> repositories;
    private       ProjectData                        selectedRepository;
    private       GitLocalizationConstant            locale;
    private       GithubImporterPageView             view;
    private       ImporterBasePagePresenter          basePagePresenter;
    private       WizardContext                      wizardContext;
    private       Wizard.UpdateDelegate              updateDelegate;
    private       String                             baseUrl;
    private       UserDescriptor                     userDescriptor;

    @Inject
    public GithubImporterPagePresenter(GithubImporterPageView view,
                                       ImporterBasePagePresenter basePagePresenter,
                                       @Named("restContext") String baseUrl,
                                       NotificationManager notificationManager,
                                       UserServiceClient userServiceClient,
                                       GitHubClientService gitHubClientService,
                                       DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                       DtoFactory dtoFactory,
                                       EventBus eventBus,
                                       GitLocalizationConstant locale) {
        this.view = view;
        this.basePagePresenter = basePagePresenter;
        this.basePagePresenter.go(view.getBasePagePanel());
        this.basePagePresenter.setListener(this);
        this.baseUrl = baseUrl;
        this.notificationManager = notificationManager;
        this.userServiceClient = userServiceClient;
        this.gitHubClientService = gitHubClientService;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.dtoFactory = dtoFactory;
        this.eventBus = eventBus;
        this.view.setDelegate(this);
        this.locale = locale;
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public String getId() {
        return "github";
    }

    /** {@inheritDoc} */
    @Override
    public void disableInputs() {
        basePagePresenter.setInputsEnableState(false);
    }

    /** {@inheritDoc} */
    @Override
    public void enableInputs() {
        basePagePresenter.setInputsEnableState(true);
    }

    /** {@inheritDoc} */
    @Override
    public void setContext(@Nonnull WizardContext wizardContext) {
        this.wizardContext = wizardContext;
    }

    /** {@inheritDoc} */
    @Override
    public void setProjectWizardDelegate(@Nonnull Wizard.UpdateDelegate updateDelegate) {
        this.updateDelegate = updateDelegate;
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        view.reset();
        basePagePresenter.reset();
    }

    /** {@inheritDoc} */
    @Override
    public void projectNameChanged(@Nonnull String name) {
        if (name.isEmpty()) {
            wizardContext.removeData(ProjectWizard.PROJECT_NAME);
        } else if (NAME_PATTERN.test(name)) {
            wizardContext.putData(ProjectWizard.PROJECT_NAME, name);
            basePagePresenter.hideNameError();
        } else {
            wizardContext.removeData(ProjectWizard.PROJECT_NAME);
            basePagePresenter.showNameError();
        }
        updateDelegate.updateControls();
    }

    /** {@inheritDoc} */
    @Override
    public void projectUrlChanged(@Nonnull String url) {
        if (!isGitUrlCorrect(url)) {
            wizardContext.removeData(ImportProjectWizard.PROJECT_URL);
        } else {
            wizardContext.putData(ImportProjectWizard.PROJECT_URL, url);
            basePagePresenter.hideUrlError();

            String projectName = basePagePresenter.getProjectName();
            if (projectName.isEmpty()) {
                projectName = parseUri(url);
                basePagePresenter.setProjectName(projectName);
                projectNameChanged(projectName);
            }
        }
        updateDelegate.updateControls();
    }

    /** {@inheritDoc} */
    @Override
    public void projectDescriptionChanged(@Nonnull String projectDescriptionValue) {
        wizardContext.putData(ProjectWizard.PROJECT_DESCRIPTION, projectDescriptionValue);
    }

    /** {@inheritDoc} */
    @Override
    public void projectVisibilityChanged(boolean aPublic) {
        wizardContext.putData(ProjectWizard.PROJECT_VISIBILITY, aPublic);
    }

    /** {@inheritDoc} */
    @Override
    public void go(@Nonnull AcceptsOneWidget container) {
        clear();
        ProjectImporterDescriptor projectImporter = wizardContext.getData(ImportProjectWizard.PROJECT_IMPORTER);
        if (projectImporter != null) {
            basePagePresenter.setImporterDescription(projectImporter.getDescription());
        }

        basePagePresenter.setInputsEnableState(true);
        container.setWidget(view.asWidget());
        getUserRepos(false);
        basePagePresenter.focusInUrlInput();
    }

    /** {@inheritDoc} */
    @Override
    public void onLoadRepoClicked() {
        clear();
        showProcessing(true);
        userServiceClient.getCurrentUser(
                new AsyncRequestCallback<UserDescriptor>(dtoUnmarshallerFactory.newUnmarshaller(UserDescriptor.class)) {
                    @Override
                    protected void onSuccess(UserDescriptor user) {
                        userDescriptor = user;
                        getUserRepos(true);
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        showProcessing(false);
                        notificationManager.showNotification(new Notification(exception.getMessage(), Notification.Type.ERROR));
                        Log.error(getClass(), "Can't get user", exception);
                    }
                }
                                        );

    }

    /** Get the list of all authorized user's repositories. */
    private void getUserRepos(final boolean isUserAction) {
        showProcessing(true);
        gitHubClientService.getAllRepositories(
                new AsyncRequestCallback<StringMap<Array<GitHubRepository>>>(new AllRepositoriesUnmarshaller(dtoFactory)) {
                    @Override
                    protected void onSuccess(StringMap<Array<GitHubRepository>> result) {
                        showProcessing(false);
                        onListLoaded(result);
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        showProcessing(false);
                        if (isUserAction) {
                            if (exception.getMessage().contains("Bad credentials")) {
                                showPopUp();
                            } else {
                                eventBus.fireEvent(new ExceptionThrownEvent(exception));
                                Notification notification = new Notification(exception.getMessage(), ERROR);
                                notificationManager.showNotification(notification);
                            }
                        }
                    }
                }
                                              );
    }

    private void showPopUp() {
        String authUrl = baseUrl + "/oauth/authenticate?oauth_provider=github"
                         + "&scope=user,repo,write:public_key&userId=" + userDescriptor.getId() + "&redirect_after_login=" +
                         Window.Location.getProtocol() + "//" + Window.Location.getHost() + "/ws/" + Config.getWorkspaceName();
        JsOAuthWindow authWindow = new JsOAuthWindow(authUrl, "error.url", 500, 980, this);
        authWindow.loginWithOAuth();
    }

    /** {@inheritDoc} */
    @Override
    public void onRepositorySelected(@Nonnull ProjectData repository) {
        selectedRepository = repository;
        basePagePresenter.setProjectName(selectedRepository.getName());
        basePagePresenter.setProjectUrl(selectedRepository.getRepositoryUrl());
        updateDelegate.updateControls();
    }

    /** {@inheritDoc} */
    @Override
    public void onAccountChanged() {
        refreshProjectList();
    }

    /** {@inheritDoc} */
    @Override
    public void onAuthenticated(@Nonnull OAuthStatus authStatus) {
        getUserRepos(false);
    }

    /**
     * Perform actions when the list of repositories was loaded.
     *
     * @param repositories
     *         loaded list of repositories
     */
    private void onListLoaded(@Nonnull StringMap<Array<GitHubRepository>> repositories) {
        this.repositories = repositories;
        view.setAccountNames(repositories.getKeys());
        refreshProjectList();
        view.showGithubPanel();
    }

    /** Refresh project list on view. */
    private void refreshProjectList() {
        Array<ProjectData> projectsData = Collections.createArray();

        String accountName = view.getAccountName();
        Array<GitHubRepository> repo = repositories.get(accountName);

        for (GitHubRepository repository : repo.asIterable()) {
            ProjectData projectData = new ProjectData(repository.getName(), repository.getDescription(), null, null, repository.getSshUrl(),
                                                      repository.getGitUrl());
            projectsData.add(projectData);
        }

        view.setRepositories(projectsData);
        basePagePresenter.reset();
        selectedRepository = null;
    }

    /**
     * Shown the state that the request is processing.
     *
     * @param inProgress
     */
    private void showProcessing(boolean inProgress) {
        view.setLoaderVisibility(inProgress);
        basePagePresenter.setInputsEnableState(!inProgress);
    }

    /** Gets project name from uri. */
    private String parseUri(@Nonnull String uri) {
        String result;
        int indexStartProjectName = uri.lastIndexOf("/") + 1;
        int indexFinishProjectName = uri.indexOf(".", indexStartProjectName);
        if (indexStartProjectName != 0 && indexFinishProjectName != (-1)) {
            result = uri.substring(indexStartProjectName, indexFinishProjectName);
        } else if (indexStartProjectName != 0) {
            result = uri.substring(indexStartProjectName);
        } else {
            result = "";
        }
        return result;
    }

    /**
     * Validate url
     *
     * @param url
     *         url for validate
     * @return <code>true</code> if url is correct
     */
    private boolean isGitUrlCorrect(@Nonnull String url) {
        if (WHITE_SPACE.test(url)) {
            basePagePresenter.showUrlError(locale.importProjectMessageStartWithWhiteSpace());
            return false;
        }

        if (SCP_LIKE_SYNTAX.test(url) && REPO_NAME.test(url)) {
            return true;
        } else if (SCP_LIKE_SYNTAX.test(url) && !REPO_NAME.test(url)) {
            basePagePresenter.showUrlError(locale.importProjectMessageNameRepoIncorrect());
            return false;
        }

        if (!PROTOCOL.test(url)) {
            basePagePresenter.showUrlError(locale.importProjectMessageProtocolIncorrect());
            return false;
        }
        if (!(HOST1.test(url) || HOST2.test(url))) {
            basePagePresenter.showUrlError(locale.importProjectMessageHostIncorrect());
            return false;
        }
        if (!(REPO_NAME.test(url))) {
            basePagePresenter.showUrlError(locale.importProjectMessageNameRepoIncorrect());
            return false;
        }
        basePagePresenter.hideUrlError();
        return true;
    }

}
