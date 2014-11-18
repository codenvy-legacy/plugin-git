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
package com.codenvy.ide.ext.git.client.commit;

import com.codenvy.ide.api.app.AppContext;
import com.codenvy.ide.api.notification.Notification;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.ext.git.client.GitLocalizationConstant;
import com.codenvy.ide.ext.git.client.GitServiceClient;
import com.codenvy.ide.ext.git.client.DateTimeFormatter;
import com.codenvy.ide.ext.git.shared.Revision;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.annotation.Nonnull;

import static com.codenvy.ide.api.notification.Notification.Type.ERROR;
import static com.codenvy.ide.api.notification.Notification.Type.INFO;

/**
 * Presenter for commit changes on git.
 *
 * @author Ann Zhuleva
 */
@Singleton
public class CommitPresenter implements CommitView.ActionDelegate {
    private final DtoUnmarshallerFactory  dtoUnmarshallerFactory;
    private final AppContext              appContext;
    private final CommitView              view;
    private final GitServiceClient        service;
    private final GitLocalizationConstant constant;
    private final NotificationManager     notificationManager;
    private final DateTimeFormatter       dateTimeFormatter;

    @Inject
    public CommitPresenter(CommitView view,
                           GitServiceClient service,
                           GitLocalizationConstant constant,
                           NotificationManager notificationManager,
                           DtoUnmarshallerFactory dtoUnmarshallerFactory,
                           AppContext appContext,
                           DateTimeFormatter dateTimeFormatter) {
        this.view = view;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.appContext = appContext;
        this.dateTimeFormatter = dateTimeFormatter;
        this.view.setDelegate(this);
        this.service = service;
        this.constant = constant;
        this.notificationManager = notificationManager;
    }

    /** Show dialog. */
    public void showDialog() {
        view.setAmend(false);
        view.setAllFilesInclude(false);
        view.focusInMessageField();
        view.setEnableCommitButton(!view.getMessage().isEmpty());
        view.showDialog();
    }

    /** {@inheritDoc} */
    @Override
    public void onCommitClicked() {
        String message = view.getMessage();
        boolean all = view.isAllFilesInclued();
        boolean amend = view.isAmend();

        service.commit(appContext.getCurrentProject().getRootProject(), message, all, amend,
                       new AsyncRequestCallback<Revision>(dtoUnmarshallerFactory.newUnmarshaller(Revision.class)) {
                           @Override
                           protected void onSuccess(Revision result) {
                               if (!result.isFake()) {
                                   onCommitSuccess(result);
                               } else {
                                   Notification notification = new Notification(result.getMessage(), ERROR);
                                   notificationManager.showNotification(notification);
                               }
                           }

                           @Override
                           protected void onFailure(Throwable exception) {
                               handleError(exception);
                           }
                       }
                      );
        view.close();
    }

    /**
     * Performs action when commit is successfully completed.
     *
     * @param revision
     *         a {@link Revision}
     */
    private void onCommitSuccess(@Nonnull final Revision revision) {
        String date = dateTimeFormatter.getFormattedDate(revision.getCommitTime());
        String message = constant.commitMessage(revision.getId(), date);

        if ((revision.getCommitter() != null && revision.getCommitter().getName() != null &&
             !revision.getCommitter().getName().isEmpty())) {
            message += " " + constant.commitUser(revision.getCommitter().getName());
        }

        Notification notification = new Notification(message, INFO);
        notificationManager.showNotification(notification);
        view.setMessage("");
    }

    /**
     * Handler some action whether some exception happened.
     *
     * @param e
     *         exception what happened
     */
    private void handleError(@Nonnull Throwable e) {
        String errorMessage = (e.getMessage() != null && !e.getMessage().isEmpty()) ? e.getMessage() : constant.commitFailed();
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
        String message = view.getMessage();
        view.setEnableCommitButton(!message.isEmpty());
    }
}