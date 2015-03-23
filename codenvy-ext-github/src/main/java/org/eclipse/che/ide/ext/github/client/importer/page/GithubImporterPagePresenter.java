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
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.wizard.AbstractWizardPage;
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
import org.eclipse.che.ide.ext.github.client.marshaller.AllRepositoriesUnmarshaller;
import org.eclipse.che.ide.ext.github.shared.GitHubRepository;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.util.NameUtils;
import org.eclipse.che.security.oauth.OAuthStatus;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;
import java.util.Comparator;

/**
 * @author Roman Nikitenko
 */
public class GithubImporterPagePresenter extends AbstractWizardPage<ImportProject> implements GithubImporterPageView.ActionDelegate {

    private static final String PUBLIC_VISIBILITY  = "public";
    private static final String PRIVATE_VISIBILITY = "private";

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

    private final DtoFactory                         dtoFactory;
    private       NotificationManager                notificationManager;
    private       GitHubClientService                gitHubClientService;
    private       EventBus                           eventBus;
    private       StringMap<Array<GitHubRepository>> repositories;
    private       GitHubLocalizationConstant         locale;
    private       GithubImporterPageView             view;
    private       GitHubAuthenticator                gitHubAuthenticator;

    @Inject
    public GithubImporterPagePresenter(GithubImporterPageView view,
                                       GitHubAuthenticator gitHubAuthenticator,
                                       NotificationManager notificationManager,
                                       GitHubClientService gitHubClientService,
                                       DtoFactory dtoFactory,
                                       EventBus eventBus,
                                       GitHubLocalizationConstant locale) {
        this.view = view;
        this.gitHubAuthenticator = gitHubAuthenticator;
        this.notificationManager = notificationManager;
        this.gitHubClientService = gitHubClientService;
        this.dtoFactory = dtoFactory;
        this.eventBus = eventBus;
        this.view.setDelegate(this);
        this.locale = locale;
    }

    @Override
    public boolean isCompleted() {
        return isGitUrlCorrect(dataObject.getSource().getProject().getLocation());
    }

    @Override
    public void projectNameChanged(@Nonnull String name) {
        dataObject.getProject().setName(name);
        updateDelegate.updateControls();

        validateProjectName();
    }

    private void validateProjectName() {
        if (NameUtils.checkProjectName(view.getProjectName())) {
            view.hideNameError();
        } else {
            view.showNameError();
        }
    }

    @Override
    public void projectUrlChanged(@Nonnull String url) {
        dataObject.getSource().getProject().setLocation(url);
        isGitUrlCorrect(url);

        String projectName = view.getProjectName();
        if (projectName.isEmpty()) {
            projectName = extractProjectNameFromUri(url);

            dataObject.getProject().setName(projectName);
            view.setProjectName(projectName);
            validateProjectName();
        }

        updateDelegate.updateControls();
    }

    @Override
    public void projectDescriptionChanged(@Nonnull String projectDescription) {
        dataObject.getProject().setDescription(projectDescription);
        updateDelegate.updateControls();
    }

    @Override
    public void projectVisibilityChanged(boolean visible) {
        dataObject.getProject().setVisibility(visible ? PUBLIC_VISIBILITY : PRIVATE_VISIBILITY);
        updateDelegate.updateControls();
    }

    @Override
    public void go(@Nonnull AcceptsOneWidget container) {
        container.setWidget(view);
        updateView();

        view.setInputsEnableState(true);
        view.focusInUrlInput();
    }

    /** Updates view from data-object. */
    private void updateView() {
        final NewProject project = dataObject.getProject();

        view.setProjectName(project.getName());
        view.setProjectDescription(project.getDescription());
        view.setVisibility(PUBLIC_VISIBILITY.equals(project.getVisibility()));
        view.setProjectUrl(dataObject.getSource().getProject().getLocation());
    }

    @Override
    public void onLoadRepoClicked() {
        getUserRepos();
    }

    /** Get the list of all authorized user's repositories. */
    private void getUserRepos() {
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
                        if (exception instanceof UnauthorizedException) {
                            authorize();
                        } else {
                            eventBus.fireEvent(new ExceptionThrownEvent(exception));
                            notificationManager.showError(exception.getMessage());
                        }
                    }
                });
    }

    private void authorize() {
        showProcessing(true);
        gitHubAuthenticator.authorize(new AsyncCallback<OAuthStatus>() {
            @Override
            public void onFailure(Throwable caught) {
                showProcessing(false);
            }

            @Override
            public void onSuccess(OAuthStatus result) {
                showProcessing(false);
                getUserRepos();
            }
        });
    }

    @Override
    public void onRepositorySelected(@Nonnull ProjectData repository) {
        dataObject.getProject().setName(repository.getName());
        dataObject.getProject().setDescription(repository.getDescription());
        dataObject.getSource().getProject().setLocation(repository.getRepositoryUrl());

        updateView();

        updateDelegate.updateControls();
    }

    @Override
    public void onAccountChanged() {
        refreshProjectList();
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
        }
    }

    /** Shown the state that the request is processing. */
    private void showProcessing(boolean inProgress) {
        view.setLoaderVisibility(inProgress);
        view.setInputsEnableState(!inProgress);
    }

    /** Gets project name from uri. */
    private String extractProjectNameFromUri(@Nonnull String uri) {
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
