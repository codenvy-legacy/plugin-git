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
package org.eclipse.che.ide.ext.git.client.merge;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.Collections;
import org.eclipse.che.ide.commons.exception.ExceptionThrownEvent;
import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.ext.git.client.GitServiceClient;
import org.eclipse.che.ide.ext.git.shared.Branch;
import org.eclipse.che.ide.ext.git.shared.MergeResult;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;
import static org.eclipse.che.ide.api.notification.Notification.Type.INFO;
import static org.eclipse.che.ide.ext.git.client.merge.Reference.RefType.LOCAL_BRANCH;
import static org.eclipse.che.ide.ext.git.client.merge.Reference.RefType.REMOTE_BRANCH;
import static org.eclipse.che.ide.ext.git.shared.BranchListRequest.LIST_LOCAL;
import static org.eclipse.che.ide.ext.git.shared.BranchListRequest.LIST_REMOTE;
import static org.eclipse.che.ide.ext.git.shared.MergeResult.MergeStatus.ALREADY_UP_TO_DATE;

/**
 * Presenter to perform merge reference with current HEAD commit.
 *
 * @author Ann Zhuleva
 */
@Singleton
public class MergePresenter implements MergeView.ActionDelegate {
    public static final String LOCAL_BRANCHES_TITLE  = "Local Branches";
    public static final String REMOTE_BRANCHES_TITLE = "Remote Branches";
    private final DtoUnmarshallerFactory  dtoUnmarshallerFactory;
    private       MergeView               view;
    private       GitServiceClient        service;
    private       EventBus                eventBus;
    private       GitLocalizationConstant constant;
    private       EditorAgent             editorAgent;
    private       AppContext              appContext;
    private       Reference               selectedReference;
    private       NotificationManager     notificationManager;

    /**
     * Create presenter.
     *
     * @param view
     * @param service
     * @param appContext
     * @param eventBus
     * @param constant
     * @param notificationManager
     */
    @Inject
    public MergePresenter(MergeView view,
                          EventBus eventBus,
                          EditorAgent editorAgent,
                          GitServiceClient service,
                          GitLocalizationConstant constant,
                          AppContext appContext,
                          NotificationManager notificationManager,
                          DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        this.view = view;
        this.view.setDelegate(this);
        this.service = service;
        this.eventBus = eventBus;
        this.constant = constant;
        this.editorAgent = editorAgent;
        this.appContext = appContext;
        this.notificationManager = notificationManager;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
    }

    /** Show dialog. */
    public void showDialog() {
        ProjectDescriptor project = appContext.getCurrentProject().getRootProject();
        selectedReference = null;
        view.setEnableMergeButton(false);

        service.branchList(project, LIST_LOCAL,
                           new AsyncRequestCallback<Array<Branch>>(dtoUnmarshallerFactory.newArrayUnmarshaller(Branch.class)) {
                               @Override
                               protected void onSuccess(Array<Branch> result) {
                                   if (result.isEmpty()) {
                                       return;
                                   }

                                   Array<Reference> references = Collections.createArray();
                                   for (int i = 0; i < result.size(); i++) {
                                       Branch branch = result.get(i);
                                       if (!branch.isActive()) {
                                           Reference reference = new Reference(branch.getName(), branch.getDisplayName(), LOCAL_BRANCH);
                                           references.add(reference);
                                       }
                                   }
                                   view.setLocalBranches(references);
                               }

                               @Override
                               protected void onFailure(Throwable exception) {
                                   eventBus.fireEvent(new ExceptionThrownEvent(exception));
                                   Notification notification = new Notification(exception.getMessage(), ERROR);
                                   notificationManager.showNotification(notification);
                               }
                           });

        service.branchList(project, LIST_REMOTE,
                           new AsyncRequestCallback<Array<Branch>>(dtoUnmarshallerFactory.newArrayUnmarshaller(Branch.class)) {
                               @Override
                               protected void onSuccess(Array<Branch> result) {
                                   if (result.isEmpty()) {
                                       return;
                                   }

                                   Array<Reference> references = Collections.createArray();
                                   for (int i = 0; i < result.size(); i++) {
                                       Branch branch = result.get(i);
                                       if (!branch.isActive()) {
                                           Reference reference =
                                                   new Reference(branch.getName(), branch.getDisplayName(), REMOTE_BRANCH);
                                           references.add(reference);
                                       }
                                   }
                                   view.setRemoteBranches(references);
                               }

                               @Override
                               protected void onFailure(Throwable exception) {
                                   eventBus.fireEvent(new ExceptionThrownEvent(exception));
                                   Notification notification = new Notification(exception.getMessage(), ERROR);
                                   notificationManager.showNotification(notification);
                               }
                           });

        view.showDialog();
    }

    /** {@inheritDoc} */
    @Override
    public void onCancelClicked() {
        view.close();
    }

    /** {@inheritDoc} */
    @Override
    public void onMergeClicked() {
        view.close();

        final List<EditorPartPresenter> openedEditors = new ArrayList<>();
        for (EditorPartPresenter partPresenter : editorAgent.getOpenedEditors().getValues().asIterable()) {
            openedEditors.add(partPresenter);
        }
        service.merge(appContext.getCurrentProject().getRootProject(), selectedReference.getDisplayName(),
                      new AsyncRequestCallback<MergeResult>(dtoUnmarshallerFactory.newUnmarshaller(MergeResult.class)) {
                          @Override
                          protected void onSuccess(final MergeResult result) {
                              Notification notification = new Notification(formMergeMessage(result), INFO);
                              notificationManager.showNotification(notification);
                              refreshProject(openedEditors);
                          }

                          @Override
                          protected void onFailure(Throwable exception) {
                              Notification notification = new Notification(exception.getMessage(), ERROR);
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
        eventBus.fireEvent(new RefreshProjectTreeEvent());
        for (EditorPartPresenter partPresenter : openedEditors) {
            final VirtualFile file = partPresenter.getEditorInput().getFile();
            eventBus.fireEvent(new FileEvent(file, FileEvent.FileOperation.CLOSE));
        }
    }

    /**
     * Form the result message of the merge operation.
     *
     * @param mergeResult
     *         result of merge operation
     * @return {@link String} merge result message
     */
    @Nonnull
    private String formMergeMessage(@Nonnull MergeResult mergeResult) {
        if (mergeResult.getMergeStatus().equals(ALREADY_UP_TO_DATE)) {
            return mergeResult.getMergeStatus().getValue();
        }

        StringBuilder conflictMessage = new StringBuilder();
        List<String> conflicts = mergeResult.getConflicts();
        if (conflicts != null && conflicts.size() > 0) {
            for (String conflict : conflicts) {
                conflictMessage.append("- ").append(conflict);
            }
        }
        StringBuilder commitsMessage = new StringBuilder();
        List<String> commits = mergeResult.getMergedCommits();
        if (commits != null && commits.size() > 0) {
            for (String commit : commits) {
                commitsMessage.append("- ").append(commit);
            }
        }

        String message = "<b>" + mergeResult.getMergeStatus().getValue() + "</b>";
        String conflictText = conflictMessage.toString();
        message += (!conflictText.isEmpty()) ? constant.mergedConflicts(conflictText) : "";


        String commitText = commitsMessage.toString();
        message += (!commitText.isEmpty()) ? " " +constant.mergedCommits(commitText) : "";
        message += (mergeResult.getNewHead() != null) ? " " + constant.mergedNewHead(mergeResult.getNewHead()) : "";
        return message;
    }

    /** {@inheritDoc} */
    @Override
    public void onReferenceSelected(@Nonnull Reference reference) {
        selectedReference = reference;
        String displayName = selectedReference.getDisplayName();
        boolean isEnabled = !displayName.equals(LOCAL_BRANCHES_TITLE) && !displayName.equals(REMOTE_BRANCHES_TITLE);
        view.setEnableMergeButton(isEnabled);
    }
}