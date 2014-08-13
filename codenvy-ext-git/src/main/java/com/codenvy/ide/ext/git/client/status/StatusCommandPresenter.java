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
package com.codenvy.ide.ext.git.client.status;

import com.codenvy.ide.api.app.AppContext;
import com.codenvy.ide.api.app.CurrentProject;
import com.codenvy.ide.api.event.ProjectActionEvent;
import com.codenvy.ide.api.event.ProjectActionHandler;
import com.codenvy.ide.api.notification.Notification;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.api.parts.PartStackType;
import com.codenvy.ide.api.parts.WorkspaceAgent;
import com.codenvy.ide.ext.git.client.GitLocalizationConstant;
import com.codenvy.ide.ext.git.client.GitOutputPartPresenter;
import com.codenvy.ide.ext.git.client.GitServiceClient;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.StringUnmarshaller;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import static com.codenvy.ide.api.notification.Notification.Type.ERROR;

/**
 * Handler to process actions with displaying the status of the Git work tree.
 *
 * @author <a href="mailto:zhulevaanna@gmail.com">Ann Zhuleva</a>
 */
@Singleton
public class StatusCommandPresenter {

    private boolean isViewClosed = true;

    private WorkspaceAgent          workspaceAgent;
    private GitServiceClient        service;
    private AppContext              appContext;
    private GitLocalizationConstant constant;
    private GitOutputPartPresenter  console;
    private NotificationManager     notificationManager;

    /**
     * Create presenter.
     *
     * @param service
     * @param appContext
     * @param console
     * @param constant
     * @param notificationManager
     */
    @Inject
    public StatusCommandPresenter(final WorkspaceAgent workspaceAgent,
                                  GitServiceClient service,
                                  EventBus eventBus,
                                  AppContext appContext,
                                  final GitOutputPartPresenter console,
                                  GitLocalizationConstant constant,
                                  NotificationManager notificationManager) {
        this.workspaceAgent = workspaceAgent;
        this.service = service;
        this.appContext = appContext;
        this.console = console;
        this.constant = constant;
        this.notificationManager = notificationManager;

        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {
            @Override
            public void onProjectOpened(ProjectActionEvent event) {

            }

            @Override
            public void onProjectClosed(ProjectActionEvent event) {
                isViewClosed = true;
                console.clear();
                workspaceAgent.hidePart(console);
            }
        });
    }

    /** Show status. */
    public void showStatus() {
        CurrentProject project = appContext.getCurrentProject();
        if (project == null) {
            return;
        }

        service.statusText(project.getProjectDescription(), false,
                           new AsyncRequestCallback<String>(new StringUnmarshaller()) {
                               @Override
                               protected void onSuccess(String result) {
                                   printGitStatus(result);
                               }

                               @Override
                               protected void onFailure(Throwable exception) {
                                   String errorMessage = exception.getMessage() != null ? exception.getMessage() : constant.statusFailed();
                                   Notification notification = new Notification(errorMessage, ERROR);
                                   notificationManager.showNotification(notification);
                               }
                           });
    }

    /**
     * Print colored Git status to Output
     *
     * @param statusText text to be printed
     */
    private void printGitStatus(String statusText) {
        if (isViewClosed) {
            workspaceAgent.openPart(console, PartStackType.INFORMATION);
            isViewClosed = false;
        }

        console.print("");
        String []lines = statusText.split("\n");
        for (String line : lines) {

            if (line.startsWith("\tmodified:") || line.startsWith("#\tmodified:")) {
                console.printError(line);
                continue;
            }

            if (line.startsWith("\t") || line.startsWith("#\t")) {
                console.printInfo(line);
                continue;
            }

            console.print(line);
        }
    }

}
