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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Presenter for add changes to Git index.
 *
 * @author <a href="mailto:zhulevaanna@gmail.com">Ann Zhuleva</a>
 */
@Singleton
public class AddToIndexPresenter implements AddToIndexView.ActionDelegate {

    private static final String ROOT_FOLDER = ".";

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
                                   addSelection();
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

    private void addSelection() {
        if (isSelectionEmpty() || isSelectionSingle()) {
            final String path = getSingleFilePattern();
            this.view.setMessage(formatMessage(path), null);
        } else {
            final List<String> paths = getMultipleFilePatterns();
            this.view.setMessage(constant.addToIndexMultiple(), paths);
        }
        this.view.setUpdated(false);
        this.view.showDialog();
    }

    /**
     * Form the message to display for adding to index, telling the user what is gonna to be added.
     *
     * @return {@link String} message to display
     */
    @Nonnull
    private String formatMessage(@Nonnull final String path) {
        String pattern = path;

        // Root of the working tree:
        if (ROOT_FOLDER.equals(pattern)) {
            return constant.addToIndexAllChanges();
        }

        // Do not display file name longer 50 characters
        if (pattern.length() > 50) {
            pattern = pattern.substring(0, 50) + "...";
        }

        if (getExplorerSelection().getHeadElement() instanceof FolderNode) {
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
            service.add(project.getRootProject(), update, getMultipleFilePatterns(), new RequestCallback<Void>() {
                @Override
                protected void onSuccess(final Void result) {
                    notificationManager.showInfo(constant.addSuccess());
                }

                @Override
                protected void onFailure(final Throwable exception) {
                    handleError(exception);
                }
            });
        } catch (final WebSocketException e) {
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
    private List<String> getMultipleFilePatterns() {

        final Selection<StorableNode> selection = getExplorerSelection();

        if (selection == null || selection.isEmpty()) {
            return Collections.singletonList(ROOT_FOLDER);
        } else {
            final Set<String> paths = new HashSet<>();
            final Set<String> directories = new HashSet<>();

            for (final StorableNode node : selection.getAllElements()) {
                final String normalized = normalizePath(node.getPath());
                if (ROOT_FOLDER.equals(normalized)) {
                    return Collections.singletonList(ROOT_FOLDER);
                }
                if (!node.isLeaf()) {
                    directories.add(normalized);
                } else {
                    paths.add(normalized);
                }
            }
            // filter out 'duplicates'
            final List<String> result = new ArrayList<>();
            for (final String path : paths) {
                boolean found = false;
                for (final String directory : directories) {
                    if (path.startsWith(directory)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    result.add(path);
                }
            }
            // add directories to result
            result.addAll(directories);
            return result;
        }
    }

    private boolean isSelectionSingle() {
        final Selection<StorableNode> selection = getExplorerSelection();
        return (selection != null && selection.isSingleSelection());
    }

    private boolean isSelectionEmpty() {
        final Selection<StorableNode> selection = getExplorerSelection();
        return (selection == null || selection.isEmpty());
    }

    private Selection<StorableNode> getExplorerSelection() {
        final Selection<StorableNode> selection = (Selection<StorableNode>)selectionAgent.getSelection();
        if (selection == null || selection.isEmpty() || selection.getHeadElement() instanceof StorableNode) {
            return selection;
        } else {
            return null;
        }
    }

    private String getSingleFilePattern() {
        final Selection<StorableNode> selection = getExplorerSelection();
        final String path;
        if (selection == null || selection.isEmpty()) {
            return ROOT_FOLDER;
        } else {
            path = selection.getHeadElement().getPath();
        }

        return normalizePath(path);
    }

    private String normalizePath(final String path) {
        final String projectPath = project.getRootProject().getPath();

        String pattern = path;
        if (path.startsWith(projectPath)) {
            pattern = pattern.replaceFirst(projectPath, "");
        }
        // no GWT emulation for File
        if (pattern.startsWith("/")) {
            pattern = pattern.replaceFirst("/", "");
        }

        if (pattern.length() == 0 || "/".equals(pattern)) {
            pattern = ROOT_FOLDER;
        }
        return pattern;
    }

    /**
     * Handler some action whether some exception happened.
     *
     * @param e
     *         exception that happened
     */
    private void handleError(@Nonnull final Throwable e) {
        String errorMessage = (e.getMessage() != null && !e.getMessage().isEmpty()) ? e.getMessage() : constant.addFailed();
        notificationManager.showError(errorMessage);
    }

    /** {@inheritDoc} */
    @Override
    public void onCancelClicked() {
        view.close();
    }
}