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

import com.codenvy.ide.api.app.AppContext;
import com.codenvy.ide.api.app.CurrentProject;
import com.codenvy.ide.api.editor.EditorAgent;
import com.codenvy.ide.api.editor.EditorPartPresenter;
import com.codenvy.ide.api.event.FileEvent;
import com.codenvy.ide.api.event.RefreshProjectTreeEvent;
import com.codenvy.ide.api.notification.Notification;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.api.parts.PartStackType;
import com.codenvy.ide.api.parts.WorkspaceAgent;
import com.codenvy.ide.api.projecttree.generic.FileNode;
import com.codenvy.ide.collections.Array;
import com.codenvy.ide.ext.git.client.GitLocalizationConstant;
import com.codenvy.ide.ext.git.client.GitOutputPartPresenter;
import com.codenvy.ide.ext.git.client.GitServiceClient;
import com.codenvy.ide.ext.git.shared.Branch;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.codenvy.ide.ui.dialogs.DialogFactory;
import com.codenvy.ide.ui.dialogs.InputCallback;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static com.codenvy.ide.api.notification.Notification.Type.ERROR;
import static com.codenvy.ide.ext.git.shared.BranchListRequest.LIST_ALL;

/**
 * Presenter for displaying and work with branches.
 *
 * @author Ann Zhuleva
 */
@Singleton
public class BranchPresenter implements BranchView.ActionDelegate {
    private DtoUnmarshallerFactory  dtoUnmarshallerFactory;
    private BranchView              view;
    private GitOutputPartPresenter  gitConsole;
    private WorkspaceAgent          workspaceAgent;
    private DialogFactory           dialogFactory;
    private EventBus                eventBus;
    private CurrentProject          project;
    private GitServiceClient        service;
    private GitLocalizationConstant constant;
    private EditorAgent             editorAgent;
    private Branch                  selectedBranch;
    private AppContext              appContext;
    private NotificationManager     notificationManager;

    /** Create presenter. */
    @Inject
    public BranchPresenter(BranchView view,
                           EventBus eventBus,
                           EditorAgent editorAgent,
                           GitServiceClient service,
                           GitLocalizationConstant constant,
                           AppContext appContext,
                           NotificationManager notificationManager,
                           DtoUnmarshallerFactory dtoUnmarshallerFactory,
                           GitOutputPartPresenter gitConsole,
                           WorkspaceAgent workspaceAgent,
                           DialogFactory dialogFactory) {
        this.view = view;
        this.gitConsole = gitConsole;
        this.workspaceAgent = workspaceAgent;
        this.dialogFactory = dialogFactory;
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
        dialogFactory.createInputDialog(constant.branchTitleRename(), constant.branchTypeRename(), currentBranchName, new InputCallback() {
            @Override
            public void accepted(String value) {
                service.branchRename(project.getRootProject(), currentBranchName, value, new AsyncRequestCallback<String>() {
                    @Override
                    protected void onSuccess(String result) {
                        getBranches();
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        String errorMessage = (exception.getMessage() != null) ? exception.getMessage() : constant.branchRenameFailed();
                        Notification notification = new Notification(errorMessage, ERROR);
                        notificationManager.showNotification(notification);
                    }
                });
            }
        }, null).show();
    }

    /** {@inheritDoc} */
    @Override
    public void onDeleteClicked() {
        final String name = selectedBranch.getName();

        service.branchDelete(project.getRootProject(), name, true, new AsyncRequestCallback<String>() {
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

        service.branchCheckout(project.getRootProject(), name, startingPoint, remote, new AsyncRequestCallback<String>() {
            @Override
            protected void onSuccess(String result) {
                getBranches();
                refreshProject(openedEditors);
            }

            @Override
            protected void onFailure(Throwable exception) {
                printGitMessage(exception.getMessage());
            }
        });
    }

    private void printGitMessage(String messageText) {
        if (messageText == null || messageText.isEmpty()) {
            return;
        }
        JSONObject jsonObject = JSONParser.parseStrict(messageText).isObject();
        if (jsonObject == null) {
            return;
        }
        String message = "";
        if (jsonObject.containsKey("message")) {
            message = jsonObject.get("message").isString().stringValue();
        }

        workspaceAgent.openPart(gitConsole, PartStackType.INFORMATION);

        gitConsole.print("");
        String[] lines = message.split("\n");
        for (String line : lines) {
            gitConsole.printError(line);
        }
    }

    /**
     * Refresh project.
     *
     * @param openedEditors
     *         editors that corresponds to open files
     */
    private void refreshProject(final List<EditorPartPresenter> openedEditors) {
        eventBus.fireEvent(new RefreshProjectTreeEvent());
        for (EditorPartPresenter partPresenter : openedEditors) {
            final FileNode file = partPresenter.getEditorInput().getFile();
            eventBus.fireEvent(new FileEvent(file, FileEvent.FileOperation.CLOSE));
        }
    }

    /** Get the list of branches. */
    private void getBranches() {
        service.branchList(project.getRootProject(), LIST_ALL,
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
        dialogFactory.createInputDialog(constant.branchCreateNew(), constant.branchTypeNew(), "", new InputCallback() {
            @Override
            public void accepted(String value) {
                if (!value.isEmpty()) {
                    service.branchCreate(project.getRootProject(), value, null,
                                         new AsyncRequestCallback<Branch>(dtoUnmarshallerFactory.newUnmarshaller(Branch.class)) {
                                             @Override
                                             protected void onSuccess(Branch result) {
                                                 getBranches();
                                             }

                                             @Override
                                             protected void onFailure(Throwable exception) {
                                                 final String errorMessage = (exception.getMessage() != null)
                                                                             ? exception.getMessage()
                                                                             : constant.branchCreateFailed();
                                                 Notification notification = new Notification(errorMessage, ERROR);
                                                 notificationManager.showNotification(notification);
                                             }
                                         }
                                        );
                }

            }
        }, null).show();
    }

    /** {@inheritDoc} */
    @Override
    public void onBranchSelected(@Nonnull Branch branch) {
        selectedBranch = branch;
        boolean enabled = !selectedBranch.isActive();
        view.setEnableCheckoutButton(enabled);
        view.setEnableDeleteButton(true);
        view.setEnableRenameButton(true);
    }
}