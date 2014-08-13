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
package com.codenvy.ide.ext.git.client.branch;

import com.codenvy.api.project.gwt.client.ProjectServiceClient;
import com.codenvy.api.project.shared.dto.ItemReference;
import com.codenvy.ide.api.app.AppContext;
import com.codenvy.ide.api.app.CurrentProject;
import com.codenvy.ide.api.editor.EditorAgent;
import com.codenvy.ide.api.editor.EditorInitException;
import com.codenvy.ide.api.editor.EditorInput;
import com.codenvy.ide.api.editor.EditorPartPresenter;
import com.codenvy.ide.api.event.FileEvent;
import com.codenvy.ide.api.notification.Notification;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.collections.Array;
import com.codenvy.ide.ext.git.client.GitLocalizationConstant;
import com.codenvy.ide.ext.git.client.GitServiceClient;
import com.codenvy.ide.ext.git.shared.Branch;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.codenvy.ide.util.loging.Log;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static com.codenvy.ide.api.notification.Notification.Type.ERROR;
import static com.codenvy.ide.ext.git.shared.BranchListRequest.LIST_ALL;

/**
 * Presenter for displaying and work with branches.
 *
 * @author <a href="mailto:zhulevaanna@gmail.com">Ann Zhuleva</a>
 */
@Singleton
public class BranchPresenter implements BranchView.ActionDelegate {
    private BranchView view;
    private ProjectServiceClient projectServiceClient;
    private       EventBus                eventBus;
    private       CurrentProject          project;
    private       GitServiceClient        service;
    private       GitLocalizationConstant constant;
    private       EditorAgent             editorAgent;
    private       Branch                  selectedBranch;
    private       AppContext              appContext;
    private       NotificationManager     notificationManager;
    private final DtoUnmarshallerFactory  dtoUnmarshallerFactory;

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
    public BranchPresenter(BranchView view,
                           EventBus eventBus,
                           EditorAgent editorAgent,
                           GitServiceClient service,
                           GitLocalizationConstant constant,
                           AppContext appContext,
                           NotificationManager notificationManager,
                           DtoUnmarshallerFactory dtoUnmarshallerFactory,
                           ProjectServiceClient projectServiceClient) {
        this.view = view;
        this.projectServiceClient = projectServiceClient;
        this.view.setDelegate(this);
        this.eventBus = eventBus;
        this.editorAgent = editorAgent;
        this.service = service;
        this.constant = constant;
        this.appContext = appContext;
        this.notificationManager = notificationManager;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
    }

    /** Show dialog. */
    public void showDialog() {
        project = appContext.getCurrentProject();
        view.setEnableCheckoutButton(false);
        view.setEnableDeleteButton(false);
        view.setEnableRenameButton(false);
        getBranches();
        view.showDialog();
    }

    /** {@inheritDoc} */
    @Override
    public void onCloseClicked() {
        view.close();
    }

    /** {@inheritDoc} */
    @Override
    public void onRenameClicked() {
        final String currentBranchName = selectedBranch.getDisplayName();
        String name = Window.prompt(constant.branchTypeNew(), currentBranchName);
        if (!name.isEmpty()) {
            service.branchRename(project.getProjectDescription(), currentBranchName, name, new AsyncRequestCallback<String>() {
                @Override
                protected void onSuccess(String result) {
                    getBranches();
                }

                @Override
                protected void onFailure(Throwable exception) {
                    String errorMessage =
                            (exception.getMessage() != null) ? exception.getMessage()
                                                             : constant.branchRenameFailed();
                    Notification notification = new Notification(errorMessage, ERROR);
                    notificationManager.showNotification(notification);
                }
            });
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onDeleteClicked() {
        final String name = selectedBranch.getName();

        service.branchDelete(project.getProjectDescription(), name, true, new AsyncRequestCallback<String>() {
            @Override
            protected void onSuccess(String result) {
                getBranches();
            }

            @Override
            protected void onFailure(Throwable exception) {
                String errorMessage = (exception.getMessage() != null) ? exception.getMessage() : constant.branchDeleteFailed();
                Notification notification = new Notification(errorMessage, ERROR);
                notificationManager.showNotification(notification);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onCheckoutClicked() {
        final List<EditorPartPresenter> openedEditors = new ArrayList<>();
        for (EditorPartPresenter partPresenter : editorAgent.getOpenedEditors().getValues().asIterable()) {
            openedEditors.add(partPresenter);
        }

        String name = selectedBranch.getDisplayName();
        String startingPoint = null;
        boolean remote = selectedBranch.isRemote();
        if (remote) {
            startingPoint = selectedBranch.getDisplayName();
        }
        if (name == null) {
            return;
        }

        service.branchCheckout(project.getProjectDescription(), name, startingPoint, remote, new AsyncRequestCallback<String>() {
            @Override
            protected void onSuccess(String result) {
                getBranches();
                refreshProject(openedEditors);
            }

            @Override
            protected void onFailure(Throwable exception) {
                final String errorMessage = (exception.getMessage() != null) ? exception.getMessage()
                                                                             : constant.branchCheckoutFailed();
                Notification notification = new Notification(errorMessage, ERROR);
                notificationManager.showNotification(notification);
            }
        });
    }

    /**
     * Refresh project.
     *
     * @param openedEditors
     *         editors that corresponds to open files
     */
    private void refreshProject(final List<EditorPartPresenter> openedEditors) {
        for (EditorPartPresenter partPresenter : openedEditors) {
            final ItemReference file = partPresenter.getEditorInput().getFile();
            refreshFile(file, partPresenter);
        }
    }

    /**
     * Refresh file.
     *
     * @param file
     *         file to refresh
     * @param partPresenter
     *         editor that corresponds to the <code>file</code>.
     */
    private void refreshFile(final ItemReference file, final EditorPartPresenter partPresenter) {
        projectServiceClient.getFileContent(file.getPath(), new AsyncRequestCallback<String>() {
            @Override
            protected void onSuccess(String result) {
                updateOpenedFile(partPresenter);
            }

            @Override
            protected void onFailure(Throwable throwable) {
                eventBus.fireEvent(new FileEvent(file, FileEvent.FileOperation.CLOSE));
            }
        });
    }

    /**
     * Update content of the file.
     *
     * @param partPresenter
     *         editor that corresponds to the <code>file</code>.
     */
    private void updateOpenedFile(EditorPartPresenter partPresenter) {
        try {
            EditorInput editorInput = partPresenter.getEditorInput();
            partPresenter.init(editorInput);
        } catch (EditorInitException event) {
            Log.error(BranchPresenter.class, "can not initializes the editor with the given input " + event);
        }
    }

    /** Get the list of branches. */
    private void getBranches() {
        service.branchList(project.getProjectDescription(), LIST_ALL,
                           new AsyncRequestCallback<Array<Branch>>(dtoUnmarshallerFactory.newArrayUnmarshaller(Branch.class)) {
                               @Override
                               protected void onSuccess(Array<Branch> result) {
                                   view.setBranches(result);
                               }

                               @Override
                               protected void onFailure(Throwable exception) {
                                   final String errorMessage =
                                           (exception.getMessage() != null) ? exception.getMessage() : constant.branchesListFailed();
                                   Notification notification = new Notification(errorMessage, ERROR);
                                   notificationManager.showNotification(notification);
                               }
                           }
                          );
    }

    /** {@inheritDoc} */
    @Override
    public void onCreateClicked() {
        String name = Window.prompt(constant.branchTypeNew(), "");
        if (!name.isEmpty()) {

            service.branchCreate(project.getProjectDescription(), name, null,
                                 new AsyncRequestCallback<Branch>(dtoUnmarshallerFactory.newUnmarshaller(Branch.class)) {
                                     @Override
                                     protected void onSuccess(Branch result) {
                                         getBranches();
                                     }

                                     @Override
                                     protected void onFailure(Throwable exception) {
                                         final String errorMessage = (exception.getMessage() != null) ? exception.getMessage()
                                                                                                      : constant.branchCreateFailed();
                                         Notification notification = new Notification(errorMessage, ERROR);
                                         notificationManager.showNotification(notification);
                                     }
                                 }
                                );
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onBranchSelected(@NotNull Branch branch) {
        selectedBranch = branch;
        boolean enabled = !selectedBranch.isActive();
        view.setEnableCheckoutButton(enabled);
        view.setEnableDeleteButton(true);
        view.setEnableRenameButton(true);
    }
}