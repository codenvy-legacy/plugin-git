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
import com.codenvy.ide.api.projectimporter.basepage.ImporterBasePageView;
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
public class GithubImporterPagePresenter implements ImporterPagePresenter, GithubImporterPageView.ActionDelegate, ImporterBasePageView.ActionDelegate,
                                                    OAuthCallback {

    private static final RegExp NAME_PATTERN = RegExp.compile("^[A-Za-z0-9_-]*$");
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
    private       WizardContext                      wizardContext;
    private       Wizard.UpdateDelegate              updateDelegate;
    private       String                             baseUrl;
    private       UserDescriptor                     userDescriptor;

    @Inject
    public GithubImporterPagePresenter(GithubImporterPageView view,
                                       @Named("restContext") String baseUrl,
                                       NotificationManager notificationManager,
                                       UserServiceClient userServiceClient,
                                       GitHubClientService gitHubClientService,
                                       DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                       DtoFactory dtoFactory,
                                       EventBus eventBus,
                                       GitLocalizationConstant locale) {
        this.view = view;
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

    @Override
    public String getId() {
        return "github";
    }

    @Override
    public void disableInputs() {
        view.setInputsEnableState(false);
    }

    @Override
    public void enableInputs() {
        view.setInputsEnableState(true);
    }

    @Override
    public void setContext(WizardContext wizardContext) {
        this.wizardContext = wizardContext;
    }

    @Override
    public void setProjectWizardDelegate(Wizard.UpdateDelegate updateDelegate) {
        this.updateDelegate = updateDelegate;
    }

    @Override
    public void clear() {
        view.reset();
    }

    @Override
    public void projectNameChanged(String name) {
        if (name == null || name.isEmpty()) {
            wizardContext.removeData(ProjectWizard.PROJECT_NAME);
        } else if (NAME_PATTERN.test(name)) {
            wizardContext.putData(ProjectWizard.PROJECT_NAME, name);
            view.hideNameError();
        } else {
            wizardContext.removeData(ProjectWizard.PROJECT_NAME);
            view.showNameError();
        }
        updateDelegate.updateControls();
    }

    @Override
    public void projectUrlChanged(String url) {
        if (!isGitUrlCorrect(url)) {
            wizardContext.removeData(ImportProjectWizard.PROJECT_URL);
        } else {
            wizardContext.putData(ImportProjectWizard.PROJECT_URL, url);
            view.hideUrlError();

            String projectName = view.getProjectName();
            if (projectName.isEmpty()) {
                projectName = parseUri(url);
                view.setProjectName(projectName);
                projectNameChanged(projectName);
            }
        }
        updateDelegate.updateControls();
    }

    @Override
    public void projectDescriptionChanged(String projectDescriptionValue) {
        wizardContext.putData(ProjectWizard.PROJECT_DESCRIPTION, projectDescriptionValue);
    }

    @Override
    public void projectVisibilityChanged(Boolean aPublic) {
        wizardContext.putData(ProjectWizard.PROJECT_VISIBILITY, aPublic);
    }

    @Override
    public void onEnterClicked() {

    }

    @Override
    public void go(AcceptsOneWidget container) {
        clear();
        ProjectImporterDescriptor projectImporter = wizardContext.getData(ImportProjectWizard.PROJECT_IMPORTER);
        view.setImporterDescription(projectImporter.getDescription());
        view.setInputsEnableState(true);
        container.setWidget(view.asWidget());
        view.focusInUrlInput();
        getUserRepos(false);
    }

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

    @Override
    public void onRepositorySelected(@Nonnull ProjectData repository) {
        selectedRepository = repository;
        view.setProjectName(selectedRepository.getName());
        view.setProjectUrl(selectedRepository.getRepositoryUrl());
        updateDelegate.updateControls();
    }

    @Override
    public void onAccountChanged() {
        refreshProjectList();
    }

    @Override
    public void onAuthenticated(OAuthStatus authStatus) {
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
        view.setProjectName("");
        view.setProjectUrl("");
        view.hideUrlError();
        view.hideNameError();
        selectedRepository = null;
    }

    /**
     * Shown the state that the request is processing.
     *
     * @param inProgress
     */
    private void showProcessing(boolean inProgress) {
        view.setLoaderVisibility(inProgress);
    }

    /** Gets project name from uri. */
    private String parseUri(String uri) {
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

    private boolean isGitUrlCorrect(String url) {
        // An alternative scp-like syntax: [user@]host.xz:path/to/repo.git/
        RegExp scpLikeSyntax = RegExp.compile("([A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-:]+)+:");

        // the transport protocol
        RegExp protocol = RegExp.compile("((http|https|git|ssh|ftp|ftps)://)");

        // the address of the remote server between // and /
        RegExp host1 = RegExp.compile("//([A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-:]+)+/");

        // the address of the remote server between @ and : or /
        RegExp host2 = RegExp.compile("@([A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-:]+)+[:/]");

        // the repository name
        RegExp repoName = RegExp.compile("/[A-Za-z0-9_.\\-]+$");

        // start with white space
        RegExp whiteSpace = RegExp.compile("^\\s");

        if (whiteSpace.test(url)) {
            view.showUrlError(locale.importProjectMessageStartWithWhiteSpace());
            return false;
        }

        if (scpLikeSyntax.test(url) && repoName.test(url)) {
            return true;
        } else if (scpLikeSyntax.test(url) && !repoName.test(url)) {
            view.showUrlError(locale.importProjectMessageNameRepoIncorrect());
            return false;
        }

        if (!protocol.test(url)) {
            view.showUrlError(locale.importProjectMessageProtocolIncorrect());
            return false;
        }
        if (!(host1.test(url) || host2.test(url))) {
            view.showUrlError(locale.importProjectMessageHostIncorrect());
            return false;
        }
        if (!(repoName.test(url))) {
            view.showUrlError(locale.importProjectMessageNameRepoIncorrect());
            return false;
        }
        view.hideUrlError();
        return true;
    }

}
