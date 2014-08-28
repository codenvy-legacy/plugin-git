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
package com.codenvy.ide.ext.github.client.load;

import com.codenvy.api.core.rest.shared.dto.ServiceError;
import com.codenvy.api.project.gwt.client.ProjectServiceClient;
import com.codenvy.api.project.shared.dto.ImportSourceDescriptor;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.ide.api.ResourceNameValidator;
import com.codenvy.ide.api.event.OpenProjectEvent;
import com.codenvy.ide.api.notification.Notification;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.api.projecttype.ProjectTypeDescriptorRegistry;
import com.codenvy.ide.api.projecttype.wizard.ProjectWizard;
import com.codenvy.ide.api.wizard.WizardContext;
import com.codenvy.ide.collections.Array;
import com.codenvy.ide.collections.Collections;
import com.codenvy.ide.collections.StringMap;
import com.codenvy.ide.commons.exception.ExceptionThrownEvent;
import com.codenvy.ide.commons.exception.UnauthorizedException;
import com.codenvy.ide.dto.DtoFactory;
import com.codenvy.ide.ext.git.client.GitLocalizationConstant;
import com.codenvy.ide.ext.git.client.GitServiceClient;
import com.codenvy.ide.ext.github.client.GitHubClientService;
import com.codenvy.ide.ext.github.client.GitHubSshKeyProvider;
import com.codenvy.ide.ext.github.client.marshaller.AllRepositoriesUnmarshaller;
import com.codenvy.ide.ext.github.shared.GitHubRepository;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.codenvy.ide.util.loging.Log;
import com.codenvy.ide.wizard.project.NewProjectWizardPresenter;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import javax.validation.constraints.NotNull;

import static com.codenvy.ide.api.notification.Notification.Status.PROGRESS;
import static com.codenvy.ide.api.notification.Notification.Type.ERROR;
import static com.codenvy.ide.api.notification.Notification.Type.INFO;

/**
 * Presenter for importing user's GitHub project to IDE.
 *
 * @author <a href="oksana.vereshchaka@gmail.com">Oksana Vereshchaka</a>
 */
@Singleton
public class ImportPresenter implements ImportView.ActionDelegate {
    private final DtoFactory             dtoFactory;
    private       DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private final ProjectServiceClient projectServiceClient;
    private NewProjectWizardPresenter          wizardPresenter;
    private ImportView                         view;
    private GitHubClientService                service;
    private EventBus                           eventBus;
    private StringMap<Array<GitHubRepository>> repositories;
    private ProjectData                        selectedRepository;
    private GitLocalizationConstant            gitConstant;
    private NotificationManager                notificationManager;
    private Notification                       notification;
    private GitHubSshKeyProvider               gitHubSshKeyProvider;

    /** Create presenter. */
    @Inject
    public ImportPresenter(ImportView view,
                           GitHubClientService service,
                           EventBus eventBus,
                           GitLocalizationConstant gitConstant,
                           NotificationManager notificationManager,
                           GitHubSshKeyProvider gitHubSshKeyProvider,
                           DtoFactory dtoFactory,
                           DtoUnmarshallerFactory dtoUnmarshallerFactory,
                           ProjectServiceClient projectServiceClient,
                           NewProjectWizardPresenter wizardPresenter) {
        this.view = view;
        this.dtoFactory = dtoFactory;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.projectServiceClient = projectServiceClient;
        this.wizardPresenter = wizardPresenter;
        this.view.setDelegate(this);
        this.service = service;
        this.eventBus = eventBus;
        this.gitConstant = gitConstant;
        this.notificationManager = notificationManager;
        this.gitHubSshKeyProvider = gitHubSshKeyProvider;
    }

    /** Show dialog. */
    public void showDialog(User user) {
        AsyncCallback<Void> callback = new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                getUserRepos();
            }

            @Override
            public void onFailure(Throwable exception) {
                notificationManager.showNotification(new Notification(exception.getMessage(), Notification.Type.ERROR));
                Log.error(ImportPresenter.class, "Can't generate ssh key", exception);
            }
        };
        gitHubSshKeyProvider.generateKey(user.getId(), callback);
    }

    /** Get the list of all authorized user's repositories. */
    private void getUserRepos() {
        service.getAllRepositories(
                new AsyncRequestCallback<StringMap<Array<GitHubRepository>>>(new AllRepositoriesUnmarshaller(dtoFactory)) {
                    @Override
                    protected void onSuccess(StringMap<Array<GitHubRepository>> result) {
                        onListLoaded(result);
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        if (exception.getMessage().contains("Bad credentials")) {
                            Window.alert("Looks like a problem with your SSH key.  Delete a GitHub key at Window > Preferences > " +
                                         "SSH Keys, and try importing your GitHub projects again.");
                        } else {
                            eventBus.fireEvent(new ExceptionThrownEvent(exception));
                            Notification notification = new Notification(exception.getMessage(), ERROR);
                            notificationManager.showNotification(notification);
                        }
                    }
                });
    }

    /**
     * Perform actions when the list of repositories was loaded.
     *
     * @param repositories
     *         loaded list of repositories
     */
    private void onListLoaded(@NotNull StringMap<Array<GitHubRepository>> repositories) {
        this.repositories = repositories;

        view.setAccountNames(repositories.getKeys());
        view.setEnableFinishButton(false);

        refreshProjectList();

        view.showDialog();
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
        selectedRepository = null;
    }

    /** Return token for user. */
    @Override
    public void onFinishClicked() {
        final String projectName = view.getProjectName();
        boolean hasProjectNameIncorrectSymbol = !ResourceNameValidator.isProjectNameValid(projectName) || projectName.isEmpty();
        if (selectedRepository != null) {
            if (hasProjectNameIncorrectSymbol) {
                Window.alert(gitConstant.noIncorrectProjectNameMessage());
            } else {
                String remoteUri = selectedRepository.getRepositoryUrl();
                if (!remoteUri.endsWith(".git")) {
                    remoteUri += ".git";
                }

                notification = new Notification(gitConstant.cloneStarted(projectName, remoteUri), PROGRESS);
                notificationManager.showNotification(notification);
                final String finalRemoteUri = remoteUri;


                doImport(finalRemoteUri);
//
            }
        }
    }


    /** {@inheritDoc} */
    public void doImport(final String url) {
//        String url = view.getUri();
        String importer = "git";
        final String projectName = view.getProjectName();
        view.close();
        ImportSourceDescriptor importSourceDescriptor =
                dtoFactory.createDto(ImportSourceDescriptor.class).withType(importer).withLocation(url);
        projectServiceClient.importProject(projectName, importSourceDescriptor, new AsyncRequestCallback<ProjectDescriptor>(dtoUnmarshallerFactory.newUnmarshaller(ProjectDescriptor.class)) {
            @Override
            protected void onSuccess(ProjectDescriptor result) {
                eventBus.fireEvent(new OpenProjectEvent(result.getName()));
                Notification notification = new Notification(gitConstant.cloneSuccess(url), INFO);
                notificationManager.showNotification(notification);
                WizardContext context = new WizardContext();
                context.putData(ProjectWizard.PROJECT, result);
                wizardPresenter.show(context);
            }

            @Override
            protected void onFailure(Throwable exception) {
                if (exception instanceof UnauthorizedException) {
                    ServiceError serverError =
                            dtoFactory.createDtoFromJson(((UnauthorizedException)exception).getResponse().getText(), ServiceError.class);
                    Notification notification = new Notification(serverError.getMessage(), ERROR);
                    notificationManager.showNotification(notification);
                } else {
                    Log.error(ImportPresenter.class, "can not import project: " + exception);
                    Notification notification = new Notification(exception.getMessage(), ERROR);
                    notificationManager.showNotification(notification);
                }
                deleteFolder(projectName);
            }
        });
    }

    private void deleteFolder(String name) {
        projectServiceClient.delete(name, new AsyncRequestCallback<Void>() {
            @Override
            protected void onSuccess(Void result) {
            }

            @Override
            protected void onFailure(Throwable exception) {
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onCancelClicked() {
        view.close();
    }

    /** {@inheritDoc} */
    @Override
    public void onRepositorySelected(@NotNull ProjectData repository) {
        selectedRepository = repository;
        view.setProjectName(selectedRepository.getName());
        view.setEnableFinishButton(true);
    }

    /** {@inheritDoc} */
    @Override
    public void onAccountChanged() {
        refreshProjectList();
    }


}