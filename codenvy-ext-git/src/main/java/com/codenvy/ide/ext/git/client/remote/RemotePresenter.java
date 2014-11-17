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
package com.codenvy.ide.ext.git.client.remote;

import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.ide.api.app.AppContext;
import com.codenvy.ide.api.notification.Notification;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.collections.Array;
import com.codenvy.ide.ext.git.client.GitLocalizationConstant;
import com.codenvy.ide.ext.git.client.GitServiceClient;
import com.codenvy.ide.ext.git.client.remote.add.AddRemoteRepositoryPresenter;
import com.codenvy.ide.ext.git.shared.Remote;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.codenvy.ide.ui.dialogs.DialogFactory;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.annotation.Nonnull;

import static com.codenvy.ide.api.notification.Notification.Type.ERROR;

/**
 * Presenter for working with remote repository list (view, add and delete).
 *
 * @author <a href="mailto:zhulevaanna@gmail.com">Ann Zhuleva</a>
 */
@Singleton
public class RemotePresenter implements RemoteView.ActionDelegate {
    private final DtoUnmarshallerFactory       dtoUnmarshallerFactory;
    private       DialogFactory                dialogFactory;
    private       RemoteView                   view;
    private       GitServiceClient             service;
    private       AppContext                   appContext;
    private       GitLocalizationConstant      constant;
    private       AddRemoteRepositoryPresenter addRemoteRepositoryPresenter;
    private       NotificationManager          notificationManager;
    private       Remote                       selectedRemote;
    private       ProjectDescriptor            project;

    /**
     * Create presenter.
     *
     * @param view
     * @param service
     * @param appContext
     * @param constant
     * @param addRemoteRepositoryPresenter
     * @param notificationManager
     */
    @Inject
    public RemotePresenter(RemoteView view, GitServiceClient service, AppContext appContext, GitLocalizationConstant constant,
                           AddRemoteRepositoryPresenter addRemoteRepositoryPresenter, NotificationManager notificationManager,
                           DtoUnmarshallerFactory dtoUnmarshallerFactory, DialogFactory dialogFactory) {
        this.view = view;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.dialogFactory = dialogFactory;
        this.view.setDelegate(this);
        this.service = service;
        this.appContext = appContext;
        this.constant = constant;
        this.addRemoteRepositoryPresenter = addRemoteRepositoryPresenter;
        this.notificationManager = notificationManager;
    }

    /** Show dialog. */
    public void showDialog() {
        project = appContext.getCurrentProject().getRootProject();
        getRemotes();
    }

    /**
     * Get the list of remote repositories for local one. If remote repositories are found,
     * then get the list of branches (remote and local).
     */
    private void getRemotes() {
        service.remoteList(project, null, true,
                           new AsyncRequestCallback<Array<Remote>>(dtoUnmarshallerFactory.newArrayUnmarshaller(Remote.class)) {
                               @Override
                               protected void onSuccess(Array<Remote> result) {
                                   view.setEnableDeleteButton(selectedRemote != null);
                                   view.setRemotes(result);
                                   if (!view.isShown()) {
                                       view.showDialog();
                                   }
                               }

                               @Override
                               protected void onFailure(Throwable exception) {
                                   String errorMessage =
                                           exception.getMessage() != null ? exception.getMessage() : constant.remoteListFailed();
                                   dialogFactory.createMessageDialog("", errorMessage, null).show();
                               }
                           }
                          );
    }

    /** {@inheritDoc} */
    @Override
    public void onCloseClicked() {
        view.close();
    }

    /** {@inheritDoc} */
    @Override
    public void onAddClicked() {
        addRemoteRepositoryPresenter.showDialog(new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                getRemotes();
            }

            @Override
            public void onFailure(Throwable caught) {
                String errorMessage = caught.getMessage() != null ? caught.getMessage() : constant.remoteAddFailed();
                Notification notification = new Notification(errorMessage, ERROR);
                notificationManager.showNotification(notification);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onDeleteClicked() {
        if (selectedRemote == null) {
            dialogFactory.createMessageDialog("", constant.selectRemoteRepositoryFail(), null).show();
            return;
        }

        final String name = selectedRemote.getName();
        service.remoteDelete(project, name, new AsyncRequestCallback<String>() {
            @Override
            protected void onSuccess(String result) {
                getRemotes();
            }

            @Override
            protected void onFailure(Throwable exception) {
                String errorMessage = exception.getMessage() != null ? exception.getMessage() : constant.remoteDeleteFailed();
                Notification notification = new Notification(errorMessage, ERROR);
                notificationManager.showNotification(notification);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onRemoteSelected(@Nonnull Remote remote) {
        selectedRemote = remote;
        view.setEnableDeleteButton(selectedRemote != null);
    }
}