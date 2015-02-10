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
package org.eclipse.che.ide.ext.git.client.add;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.tree.generic.FolderNode;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.ext.git.client.GitServiceClient;
import org.eclipse.che.ide.ext.git.shared.Status;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.RequestCallback;
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
    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;

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
                               AppContext appContext,
                               DtoUnmarshallerFactory dtoUnmarshallerFactory,
                               GitLocalizationConstant constant,
                               GitServiceClient service,
                               NotificationManager notificationManager,
                               SelectionAgent selectionAgent) {
        this.view = view;
        this.view.setDelegate(this);
        this.service = service;
        this.constant = constant;
        this.appContext = appContext;
        this.selectionAgent = selectionAgent;
        this.notificationManager = notificationManager;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
    }

    /** Show dialog. */
    public void showDialog() {
        project = appContext.getCurrentProject();
        if (project == null) {
            return;
        }
        final Unmarshallable<Status> unmarshall = this.dtoUnmarshallerFactory.newUnmarshaller(Status.class);
        service.status(project.getRootProject(),
                           new AsyncRequestCallback<Status>(unmarshall) {
                               @Override
                               protected void onSuccess(final Status result) {
                                   if (!result.isClean()) {
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