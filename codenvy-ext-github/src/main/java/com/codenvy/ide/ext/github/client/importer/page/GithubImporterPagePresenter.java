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
package com.codenvy.ide.ext.github.client.importer.page;

import com.codenvy.api.project.shared.dto.ProjectImporterDescriptor;
import com.codenvy.api.user.gwt.client.UserServiceClient;
import com.codenvy.api.user.shared.dto.UserDescriptor;
import com.codenvy.ide.api.notification.Notification;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.api.projectimporter.ImporterPagePresenter;
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
import com.codenvy.ide.ext.github.client.marshaller.AllRepositoriesUnmarshaller;
import com.codenvy.ide.ext.github.shared.GitHubRepository;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.codenvy.ide.ui.dialogs.ConfirmCallback;
import com.codenvy.ide.ui.dialogs.DialogFactory;
import com.codenvy.ide.util.Config;
import com.codenvy.security.oauth.OAuthCallback;
import com.codenvy.security.oauth.OAuthStatus;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;

import java.util.Comparator;

import static com.codenvy.ide.api.notification.Notification.Type.ERROR;

/**
 * @author Roman Nikitenko
 */
public class GithubImporterPagePresenter implements ImporterPagePresenter, GithubImporterPageView.ActionDelegate, OAuthCallback {

    private static final RegExp NAME_PATTERN    = RegExp.compile("^[A-Za-z0-9_\\-\\.]*$");
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
    private       DialogFactory                      dialogFactory;
    private       EventBus                           eventBus;
    private       StringMap<Array<GitHubRepository>> repositories;
    private       ProjectData                        selectedRepository;
    private       GitHubLocalizationConstant         locale;
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
                                       DialogFactory dialogFactory,
                                       EventBus eventBus,
                                       GitHubLocalizationConstant locale) {
        this.view = view;
        this.baseUrl = baseUrl;
        this.notificationManager = notificationManager;
        this.userServiceClient = userServiceClient;
        this.gitHubClientService = gitHubClientService;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.dtoFactory = dtoFactory;
        this.dialogFactory = dialogFactory;
        this.eventBus = eventBus;
        this.view.setDelegate(this);
        this.locale = locale;
    }

    @Nonnull
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
    public void setContext(@Nonnull WizardContext wizardContext) {
        this.wizardContext = wizardContext;
    }

    @Override
    public void setProjectWizardDelegate(@Nonnull Wizard.UpdateDelegate updateDelegate) {
        this.updateDelegate = updateDelegate;
    }

    @Override
    public void clear() {
        view.reset();
    }

    @Override
    public void projectNameChanged(@Nonnull String name) {
        if (name.isEmpty()) {
            wizardContext.removeData(ProjectWizard.PROJECT_NAME);
        } else {
            name = replaceSpaceToHyphen(name);
            if (NAME_PATTERN.test(name)) {
                wizardContext.putData(ProjectWizard.PROJECT_NAME, name);
                view.hideNameError();
            } else {
                wizardContext.removeData(ProjectWizard.PROJECT_NAME);
                view.showNameError();
            }
        }
        updateDelegate.updateControls();
    }

    private String replaceSpaceToHyphen(String projectName) {
        if (projectName.contains(" ")) {
            projectName  = projectName.replace(" ", "-");
            view.setProjectName(projectName);
        }
        return projectName;
    }

    @Override
    public void projectUrlChanged(@Nonnull String url) {
        if (!isGitUrlCorrect(url)) {
            wizardContext.removeData(ImportProjectWizard.PROJECT_URL);
        } else {
            wizardContext.putData(ImportProjectWizard.PROJECT_URL, url);
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
    public void projectDescriptionChanged(@Nonnull String projectDescriptionValue) {
        wizardContext.putData(ProjectWizard.PROJECT_DESCRIPTION, projectDescriptionValue);
    }

    @Override
    public void projectVisibilityChanged(boolean aPublic) {
        wizardContext.putData(ProjectWizard.PROJECT_VISIBILITY, aPublic);
    }

    @Override
    public void go(@Nonnull AcceptsOneWidget container) {
        clear();
        ProjectImporterDescriptor projectImporter = wizardContext.getData(ImportProjectWizard.PROJECT_IMPORTER);
        if (projectImporter != null) {
            view.setImporterDescription(projectImporter.getDescription());
        }

        view.setInputsEnableState(true);
        container.setWidget(view);
        getUserRepos(false);
        view.focusInUrlInput();
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
                                dialogFactory.createConfirmDialog("GiHub", "Codenvy requests authorization through OAuth2 protocol",
                                                                  new ConfirmCallback() {
                                                                      @Override
                                                                      public void accepted() {
                                                                          showAuthWindow();
                                                                      }
                                                                  }, null).show();
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

    private void showAuthWindow() {
        String authUrl = baseUrl
                         + "/oauth/authenticate?oauth_provider=github"
                         + "&scope=user,repo,write:public_key&userId=" + userDescriptor.getId()
                         + "&redirect_after_login="
                         + Window.Location.getProtocol() + "//"
                         + Window.Location.getHost() + "/ws/"
                         + Config.getWorkspaceName();
        view.showAuthWindow(authUrl, this);
    }

    @Override
    public void onRepositorySelected(@Nonnull ProjectData repository) {
        selectedRepository = repository;
        view.setProjectName(selectedRepository.getName());
        view.setProjectUrl(selectedRepository.getRepositoryUrl());
        view.setProjectDescription(selectedRepository.getDescription());
        updateDelegate.updateControls();
    }

    @Override
    public void onAccountChanged() {
        refreshProjectList();
    }

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
        if (repositories.containsKey(accountName)) {
            Array<GitHubRepository> repo = repositories.get(accountName);

            for (GitHubRepository repository : repo.asIterable()) {
                ProjectData projectData =
                        new ProjectData(repository.getName(), repository.getDescription(), null, null, repository.getSshUrl(),
                                        repository.getGitUrl());
                projectsData.add(projectData);
            }

            projectsData.sort(new Comparator<ProjectData>() {
                @Override
                public int compare(ProjectData o1, ProjectData o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            view.setRepositories(projectsData);
            view.reset();
            view.showGithubPanel();
            selectedRepository = null;
        }
    }

    /** Shown the state that the request is processing. */
    private void showProcessing(boolean inProgress) {
        view.setLoaderVisibility(inProgress);
        view.setInputsEnableState(!inProgress);
    }

    /** Gets project name from uri. */
    private String parseUri(@Nonnull String uri) {
        int indexFinishProjectName = uri.lastIndexOf(".");
        int indexStartProjectName = uri.lastIndexOf("/") != -1 ? uri.lastIndexOf("/") + 1 : (uri.lastIndexOf(":") + 1);

        if (indexStartProjectName != 0 && indexStartProjectName < indexFinishProjectName) {
            return uri.substring(indexStartProjectName, indexFinishProjectName);
        }
        if (indexStartProjectName != 0) {
            return uri.substring(indexStartProjectName);
        }
        return "";
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
            view.showUrlError(locale.importProjectMessageStartWithWhiteSpace());
            return false;
        }
        if (SCP_LIKE_SYNTAX.test(url)) {
            view.hideUrlError();
            return true;
        }
        if (!PROTOCOL.test(url)) {
            view.showUrlError(locale.importProjectMessageProtocolIncorrect());
            return false;
        }
        if (!(HOST1.test(url) || HOST2.test(url))) {
            view.showUrlError(locale.importProjectMessageHostIncorrect());
            return false;
        }
        if (!(REPO_NAME.test(url))) {
            view.showUrlError(locale.importProjectMessageNameRepoIncorrect());
            return false;
        }
        view.hideUrlError();
        return true;
    }

}
