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
package com.codenvy.ide.ext.git.client.add;

import com.codenvy.ide.api.app.AppContext;
import com.codenvy.ide.api.app.CurrentProject;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.api.projecttree.generic.FolderNode;
import com.codenvy.ide.api.projecttree.generic.StorableNode;
import com.codenvy.ide.api.selection.Selection;
import com.codenvy.ide.api.selection.SelectionAgent;
import com.codenvy.ide.ext.git.client.GitLocalizationConstant;
import com.codenvy.ide.ext.git.client.GitServiceClient;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.StringUnmarshaller;
import com.codenvy.ide.websocket.WebSocketException;
import com.codenvy.ide.websocket.rest.RequestCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Presenter for add changes to Git index.
 *
 * @author <a href="mailto:zhulevaanna@gmail.com">Ann Zhuleva</a>
 */
@Singleton
public class AddToIndexPresenter implements AddToIndexView.ActionDelegate {
    private AddToIndexView          view;
    private GitServiceClient        service;
    private GitLocalizationConstant constant;
    private AppContext              appContext;
    private CurrentProject          project;
    private SelectionAgent          selectionAgent;
    private NotificationManager     notificationManager;

    /**
     * Create presenter
     *
     * @param view
     * @param service
     * @param constant
     * @param appContext
     * @param selectionAgent
     * @param notificationManager
     */
    @Inject
    public AddToIndexPresenter(AddToIndexView view,
                               GitServiceClient service,
                               GitLocalizationConstant constant,
                               AppContext appContext,
                               SelectionAgent selectionAgent,
                               NotificationManager notificationManager) {
        this.view = view;
        this.view.setDelegate(this);
        this.service = service;
        this.constant = constant;
        this.appContext = appContext;
        this.selectionAgent = selectionAgent;
        this.notificationManager = notificationManager;
    }

    /** Show dialog. */
    public void showDialog() {
        project = appContext.getCurrentProject();
        if (project == null) {
            return;
        }
        service.statusText(project.getRootProject(), false,
                           new AsyncRequestCallback<String>(new StringUnmarshaller()) {
                               @Override
                               protected void onSuccess(String result) {
                                   if (haveChanges(result)) {
                                       final String workDir = project.getRootProject().getPath();
                                       view.setMessage(formMessage(workDir));
                                       view.setUpdated(false);
                                       view.showDialog();
                                   } else {
                                       notificationManager.showInfo(constant.nothingAddToIndex());
                                   }
                               }

                               @Override
                               protected void onFailure(Throwable exception) {
                                   String errorMessage = exception.getMessage() != null ? exception.getMessage() : constant.statusFailed();
                                   notificationManager.showError(errorMessage);
                               }
                           });
    }

    /**
     * Form the message to display for adding to index, telling the user what is gonna to be added.
     *
     * @return {@link String} message to display
     */
    @Nonnull
    private String formMessage(@Nonnull String workDir) {
        Selection<StorableNode> selection = (Selection<StorableNode>)selectionAgent.getSelection();

        String path;
        if (selection == null || selection.getFirstElement() == null) {
            path = project.getRootProject().getPath();
        } else {
            path = selection.getFirstElement().getPath();
        }

        String pattern = path.replaceFirst(workDir, "");
        pattern = (pattern.startsWith("/")) ? pattern.replaceFirst("/", "") : pattern;

        // Root of the working tree:
        if (pattern.length() == 0 || "/".equals(pattern)) {
            return constant.addToIndexAllChanges();
        }

        // Do not display file name longer 50 characters
        if (pattern.length() > 50) {
            pattern = pattern.substring(0, 50) + "...";
        }

        if (selection.getFirstElement() instanceof FolderNode) {
            return constant.addToIndexFolder(pattern).asString();
        } else {
            return constant.addToIndexFile(pattern).asString();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onAddClicked() {
        boolean update = view.isUpdated();

        try {
            service.add(project.getRootProject(), update, getFilePatterns(), new RequestCallback<Void>() {
                @Override
                protected void onSuccess(Void result) {
                    notificationManager.showInfo(constant.addSuccess());
                }

                @Override
                protected void onFailure(Throwable exception) {
                    handleError(exception);
                }
            });
        } catch (WebSocketException e) {
            handleError(e);
        }
        view.close();
    }

    /**
     * Returns pattern of the files to be added.
     *
     * @return pattern of the files to be added
     */
    @Nonnull
    private List<String> getFilePatterns() {
        String projectPath = project.getRootProject().getPath();

        Selection<StorableNode> selection = (Selection<StorableNode>)selectionAgent.getSelection();
        String path;
        if (selection == null || selection.getFirstElement() == null) {
            path = project.getRootProject().getPath();
        } else {
            path = selection.getFirstElement().getPath();
        }

        String pattern = path.replaceFirst(projectPath, "");
        pattern = (pattern.startsWith("/")) ? pattern.replaceFirst("/", "") : pattern;

        return (pattern.length() == 0 || "/".equals(pattern)) ? new ArrayList<>(Arrays.asList("."))
                                                              : new ArrayList<>(Arrays.asList(pattern));
    }

    /**
     * Handler some action whether some exception happened.
     *
     * @param e
     *         exception what happened
     */
    private void handleError(@Nonnull Throwable e) {
        String errorMessage = (e.getMessage() != null && !e.getMessage().isEmpty()) ? e.getMessage() : constant.addFailed();
        notificationManager.showError(errorMessage);
    }

    /** {@inheritDoc} */
    @Override
    public void onCancelClicked() {
        view.close();
    }

    /**
     * Returns <code>true</code> if the working tree has changes for add to index.
     *
     * @param statusText
     *         the working tree status
     * @return <code>true</code> if the working tree has changes for add to index, <code>false</code> otherwise
     */
    private boolean haveChanges(String statusText) {
        if (statusText.contains("Changes not staged for commit")) {
            return true;
        }
        if (statusText.contains("Untracked files")) {
            return true;
        }
        if (statusText.contains("unmerged")) {
            return true;
        }
        return false;
    }
}