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

import com.codenvy.ide.api.notification.Notification;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.api.resources.ResourceProvider;
import com.codenvy.ide.ext.git.client.GitServiceClient;
import com.codenvy.ide.ext.git.client.GitLocalizationConstant;
import com.codenvy.ide.api.resources.model.Project;
import com.codenvy.ide.websocket.WebSocketException;
import com.codenvy.ide.websocket.rest.RequestCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.codenvy.ide.api.notification.Notification.Type.ERROR;
import static com.codenvy.ide.api.notification.Notification.Type.INFO;

/**
 * Presenter for Git command Init Repository.
 *
 * @author <a href="mailto:zhulevaanna@gmail.com">Ann Zhuleva</a>
 * @version $Id: Mar 24, 2011 9:07:58 AM anya $
 */
@Singleton
public class InitRepositoryPresenter implements InitRepositoryView.ActionDelegate {
    private InitRepositoryView      view;
    private GitServiceClient        service;
    private Project                 project;
    private ResourceProvider        resourceProvider;
    private GitLocalizationConstant constant;
    private NotificationManager     notificationManager;

    /**
     * Create presenter.
     *
     * @param view
     * @param service
     * @param resourceProvider
     * @param constant
     * @param notificationManager
     */
    @Inject
    public InitRepositoryPresenter(InitRepositoryView view,
                                   GitServiceClient service,
                                   ResourceProvider resourceProvider,
                                   GitLocalizationConstant constant,
                                   NotificationManager notificationManager) {
        this.view = view;
        this.view.setDelegate(this);
        this.service = service;
        this.resourceProvider = resourceProvider;
        this.constant = constant;
        this.notificationManager = notificationManager;
    }

    /** Show dialog. */
    public void showDialog() {
        project = resourceProvider.getActiveProject();

        view.setWorkDir(project.getPath());
        view.setEnableOkButton(true);
        view.showDialog();
    }

    /** {@inheritDoc} */
    @Override
    public void onOkClicked() {
        String projectPath = project.getPath();
        String projectName = project.getName();
        view.close();

        try {
            service.init(projectPath, projectName, false, new RequestCallback<Void>() {
                @Override
                protected void onSuccess(Void result) {
                    List<String> vcsProvider = new ArrayList<>();
                    vcsProvider.add("git");
                    project.getAttributes().put("vcs.provider.name", vcsProvider);

                    Notification notification = new Notification(constant.initSuccess(), INFO);
                    notificationManager.showNotification(notification);
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