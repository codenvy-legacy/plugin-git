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
package com.codenvy.ide.ext.git.client.init;

import com.codenvy.api.project.gwt.client.ProjectServiceClient;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.ide.api.app.AppContext;
import com.codenvy.ide.api.app.CurrentProject;
import com.codenvy.ide.api.notification.Notification;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.ext.git.client.GitLocalizationConstant;
import com.codenvy.ide.ext.git.client.GitServiceClient;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.codenvy.ide.rest.Unmarshallable;
import com.codenvy.ide.util.loging.Log;
import com.codenvy.ide.websocket.WebSocketException;
import com.codenvy.ide.websocket.rest.RequestCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.validation.constraints.NotNull;

import static com.codenvy.ide.api.notification.Notification.Type.ERROR;
import static com.codenvy.ide.api.notification.Notification.Type.INFO;

/**
 * Presenter for Git command Init Repository.
 *
 * @author <a href="mailto:zhulevaanna@gmail.com">Ann Zhuleva</a>
 */
@Singleton
public class InitRepositoryPresenter implements InitRepositoryView.ActionDelegate {
    private InitRepositoryView      view;
    private ProjectServiceClient    projectServiceClient;
    private DtoUnmarshallerFactory  dtoUnmarshallerFactory;
    private GitServiceClient        service;
    private AppContext              appContext;
    private GitLocalizationConstant constant;
    private NotificationManager     notificationManager;

    /**
     * Create presenter.
     *
     * @param view
     * @param service
     * @param appContext
     * @param constant
     * @param notificationManager
     */
    @Inject
    public InitRepositoryPresenter(InitRepositoryView view,
                                   GitServiceClient service,
                                   AppContext appContext,
                                   GitLocalizationConstant constant,
                                   NotificationManager notificationManager,
                                   ProjectServiceClient projectServiceClient,
                                   DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        this.view = view;
        this.projectServiceClient = projectServiceClient;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.view.setDelegate(this);
        this.service = service;
        this.appContext = appContext;
        this.constant = constant;
        this.notificationManager = notificationManager;
    }

    /** Show dialog. */
    public void showDialog() {
        view.setWorkDir(appContext.getCurrentProject().getRootProject().getPath());
        view.setEnableOkButton(true);
        view.showDialog();
    }

    /** {@inheritDoc} */
    @Override
    public void onOkClicked() {
        final CurrentProject currentProject = appContext.getCurrentProject();
        view.close();
        try {
            service.init(currentProject.getRootProject(), false, new RequestCallback<Void>() {
                @Override
                protected void onSuccess(Void result) {
                    Notification notification = new Notification(constant.initSuccess(), INFO);
                    notificationManager.showNotification(notification);

                    // update 'vcs.provider.name' attribute value
                    Unmarshallable<ProjectDescriptor> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ProjectDescriptor.class);
                    projectServiceClient.getProject(currentProject.getRootProject().getPath(),
                                                    new AsyncRequestCallback<ProjectDescriptor>(unmarshaller) {
                                                        @Override
                                                        protected void onSuccess(ProjectDescriptor projectDescriptor) {
                                                            currentProject.setRootProject(projectDescriptor);
                                                        }

                                                        @Override
                                                        protected void onFailure(Throwable throwable) {
                                                            Log.error(InitRepositoryPresenter.class, throwable);
                                                        }
                                                    });
                }

                @Override
                protected void onFailure(Throwable exception) {
                    handleError(exception);
                }
            });
        } catch (WebSocketException e) {
            handleError(e);
        }
    }

    /**
     * Handler some action whether some exception happened.
     *
     * @param e
     *         exception what happened
     */
    private void handleError(@NotNull Throwable e) {
        String errorMessage = (e.getMessage() != null && !e.getMessage().isEmpty()) ? e.getMessage() : constant.initFailed();
        Notification notification = new Notification(errorMessage, ERROR);
        notificationManager.showNotification(notification);
    }

    /** {@inheritDoc} */
    @Override
    public void onCancelClicked() {
        view.close();
    }

    /** {@inheritDoc} */
    @Override
    public void onValueChanged() {
        String workDir = view.getWorkDir();
        view.setEnableOkButton(!workDir.isEmpty());
    }
}