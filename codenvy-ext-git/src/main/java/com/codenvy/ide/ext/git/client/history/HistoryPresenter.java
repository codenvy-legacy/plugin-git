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
package com.codenvy.ide.ext.git.client.history;

import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.ide.api.app.AppContext;
import com.codenvy.ide.api.event.ProjectActionEvent;
import com.codenvy.ide.api.event.ProjectActionHandler;
import com.codenvy.ide.api.notification.Notification;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.api.parts.PartPresenter;
import com.codenvy.ide.api.parts.PartStackType;
import com.codenvy.ide.api.parts.WorkspaceAgent;
import com.codenvy.ide.api.parts.base.BasePresenter;
import com.codenvy.ide.api.projecttree.generic.StorableNode;
import com.codenvy.ide.api.selection.Selection;
import com.codenvy.ide.api.selection.SelectionAgent;
import com.codenvy.ide.collections.Array;
import com.codenvy.ide.collections.Collections;
import com.codenvy.ide.ext.git.client.GitLocalizationConstant;
import com.codenvy.ide.ext.git.client.GitResources;
import com.codenvy.ide.ext.git.client.GitServiceClient;
import com.codenvy.ide.ext.git.client.DateTimeFormatter;
import com.codenvy.ide.ext.git.shared.LogResponse;
import com.codenvy.ide.ext.git.shared.Revision;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.codenvy.ide.rest.StringUnmarshaller;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.vectomatic.dom.svg.ui.SVGResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.codenvy.ide.api.notification.Notification.Type.ERROR;
import static com.codenvy.ide.ext.git.shared.DiffRequest.DiffType.RAW;

/**
 * Presenter for showing git history.
 * This presenter must implements ActivePartChangedHandler and PropertyListener to be able to get the changes for the selected resource
 * and change a dedicated resource with the history-window open
 *
 * @author Ann Zhuleva
 */
@Singleton
public class HistoryPresenter extends BasePresenter implements HistoryView.ActionDelegate {
    private final DtoUnmarshallerFactory  dtoUnmarshallerFactory;
    private final HistoryView             view;
    private final GitServiceClient        service;
    private final GitLocalizationConstant constant;
    private final GitResources            resources;
    private final AppContext              appContext;
    private final WorkspaceAgent          workspaceAgent;
    private final DateTimeFormatter       dateTimeFormatter;
    /** If <code>true</code> then show all changes in project, if <code>false</code> then show changes of the selected resource. */
    private       boolean                 showChangesInProject;
    private       DiffWith                diffType;
    private boolean isViewClosed = true;
    private Array<Revision>     revisions;
    private SelectionAgent      selectionAgent;
    private Revision            selectedRevision;
    private NotificationManager notificationManager;

    @Inject
    public HistoryPresenter(final HistoryView view,
                            EventBus eventBus,
                            GitResources resources,
                            GitServiceClient service,
                            final WorkspaceAgent workspaceAgent,
                            GitLocalizationConstant constant,
                            AppContext appContext,
                            NotificationManager notificationManager,
                            DtoUnmarshallerFactory dtoUnmarshallerFactory,
                            DateTimeFormatter dateTimeFormatter,
                            SelectionAgent selectionAgent) {
        this.view = view;
        this.dateTimeFormatter = dateTimeFormatter;
        this.view.setDelegate(this);
        this.view.setTitle(constant.historyTitle());
        this.resources = resources;
        this.service = service;
        this.workspaceAgent = workspaceAgent;
        this.constant = constant;
        this.appContext = appContext;
        this.notificationManager = notificationManager;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.selectionAgent = selectionAgent;
        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {
            @Override
            public void onProjectOpened(ProjectActionEvent event) {

            }

            @Override
            public void onProjectClosed(ProjectActionEvent event) {
                isViewClosed = true;
                workspaceAgent.hidePart(HistoryPresenter.this);
                view.clear();
            }
        });
    }

    /** Show dialog. */
    public void showDialog() {
        ProjectDescriptor project = appContext.getCurrentProject().getRootProject();
        getCommitsLog(project);
        selectedRevision = null;

        view.selectProjectChangesButton(true);
        view.selectResourceChangesButton(false);
        showChangesInProject = true;
        view.selectDiffWithPrevVersionButton(true);
        diffType = DiffWith.DIFF_WITH_PREV_VERSION;

        displayCommitA(null);
        displayCommitB(null);
        view.setDiffContext("");
        view.setCompareType(constant.historyNothingToDisplay());

        if (isViewClosed) {
            workspaceAgent.openPart(this, PartStackType.TOOLING);
            isViewClosed = false;
        }

        PartPresenter activePart = partStack.getActivePart();
        if (activePart == null || !activePart.equals(this)) {
            partStack.setActivePart(this);
        }
    }

    /** Get the log of the commits. If successfully received, then display in revision grid, otherwise - show error in output panel. */
    private void getCommitsLog(@Nonnull ProjectDescriptor project) {
        service.log(project, false,
                    new AsyncRequestCallback<LogResponse>(dtoUnmarshallerFactory.newUnmarshaller(LogResponse.class)) {
                        @Override
                        protected void onSuccess(LogResponse result) {
                            revisions = Collections.createArray(result.getCommits());
                            view.setRevisions(revisions);
                        }

                        @Override
                        protected void onFailure(Throwable exception) {
                            nothingToDisplay(null);
                            String errorMessage = exception.getMessage() != null ? exception.getMessage() : constant.logFailed();
                            Notification notification = new Notification(errorMessage, ERROR);
                            notificationManager.showNotification(notification);
                        }
                    });
    }

    /**
     * Clear the comparance result, when there is nothing to compare.
     *
     * @param revision
     */
    private void nothingToDisplay(@Nullable Revision revision) {
        displayCommitA(revision);
        displayCommitB(null);
        view.setCompareType(constant.historyNothingToDisplay());
        view.setDiffContext("");
    }

    /**
     * Display information about commit A.
     *
     * @param revision
     *         revision what need to display
     */
    private void displayCommitA(@Nullable Revision revision) {
        if (revision == null) {
            view.setCommitADate("");
            view.setCommitARevision("");
        } else {
            view.setCommitADate(dateTimeFormatter.getFormattedDate(revision.getCommitTime()));
            view.setCommitARevision(revision.getId());
        }
    }

    /**
     * Display information about commit B.
     *
     * @param revision
     *         revision what need to display
     */
    private void displayCommitB(@Nullable Revision revision) {
        boolean isEmpty = revision == null;
        if (isEmpty) {
            view.setCommitBDate("");
            view.setCommitBRevision("");
        } else {
            view.setCommitBDate(dateTimeFormatter.getFormattedDate(revision.getCommitTime()));
            view.setCommitBRevision(revision.getId());
        }
        view.setCommitBPanelVisible(!isEmpty);
    }

    /** {@inheritDoc} */
    @Override
    public void onRefreshClicked() {
        ProjectDescriptor project = appContext.getCurrentProject().getRootProject();
        getCommitsLog(project);
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectChangesClicked() {
        if (showChangesInProject) {
            return;
        }
        showChangesInProject = true;
        view.selectProjectChangesButton(true);
        view.selectResourceChangesButton(false);
        update();
    }

    /** {@inheritDoc} */
    @Override
    public void onResourceChangesClicked() {
        if (!showChangesInProject) {
            return;
        }
        showChangesInProject = false;
        view.selectProjectChangesButton(false);
        view.selectResourceChangesButton(true);
        update();
    }

    /** {@inheritDoc} */
    @Override
    public void onDiffWithIndexClicked() {
        if (DiffWith.DIFF_WITH_INDEX.equals(diffType)) {
            return;
        }
        diffType = DiffWith.DIFF_WITH_INDEX;
        view.selectDiffWithIndexButton(true);
        view.selectDiffWithPrevVersionButton(false);
        view.selectDiffWithWorkingTreeButton(false);
        update();
    }

    /** {@inheritDoc} */
    @Override
    public void onDiffWithWorkTreeClicked() {
        if (DiffWith.DIFF_WITH_WORK_TREE.equals(diffType)) {
            return;
        }
        diffType = DiffWith.DIFF_WITH_WORK_TREE;
        view.selectDiffWithIndexButton(false);
        view.selectDiffWithPrevVersionButton(false);
        view.selectDiffWithWorkingTreeButton(true);
        update();
    }

    /** {@inheritDoc} */
    @Override
    public void onDiffWithPrevCommitClicked() {
        if (DiffWith.DIFF_WITH_PREV_VERSION.equals(diffType)) {
            return;
        }
        diffType = DiffWith.DIFF_WITH_PREV_VERSION;
        view.selectDiffWithIndexButton(false);
        view.selectDiffWithPrevVersionButton(true);
        view.selectDiffWithWorkingTreeButton(false);
        update();
    }

    /** {@inheritDoc} */
    @Override
    public void onRevisionSelected(@Nonnull Revision revision) {
        selectedRevision = revision;
        update();
    }

    /** Update content. */
    private void update() {
        getDiff();
        ProjectDescriptor project = appContext.getCurrentProject().getRootProject();
        getCommitsLog(project);
    }

    /** Get the changes between revisions. On success - display diff in text format, otherwise - show the error message in output panel. */
    private void getDiff() {
        String pattern = "";
        ProjectDescriptor project = appContext.getCurrentProject().getRootProject();
        if (!showChangesInProject && project != null) {
            String path;

            Selection<StorableNode> selection = (Selection<StorableNode>)selectionAgent.getSelection();

            if (selection == null || selection.getFirstElement() == null) {
                path = project.getPath();
            } else {
                path = selection.getFirstElement().getPath();
            }

            pattern = path.replaceFirst(project.getPath(), "");
            pattern = (pattern.startsWith("/")) ? pattern.replaceFirst("/", "") : pattern;
        }

        if (DiffWith.DIFF_WITH_INDEX.equals(diffType) || DiffWith.DIFF_WITH_WORK_TREE.equals(diffType)) {
            boolean isCached = DiffWith.DIFF_WITH_INDEX.equals(diffType);
            doDiffWithNotCommitted((pattern.length() > 0) ? new ArrayList<>(Arrays.asList(pattern)) : new ArrayList<String>(),
                                   selectedRevision, isCached);
        } else {
            doDiffWithPrevVersion((pattern.length() > 0) ? new ArrayList<>(Arrays.asList(pattern)) : new ArrayList<String>(),
                                  selectedRevision);
        }
    }

    /**
     * Perform diff between pointed revision and index or working tree.
     *
     * @param filePatterns
     *         patterns for which to show diff
     * @param revision
     *         revision to compare with
     * @param isCached
     *         if <code>true</code> compare with index, else - with working tree
     */
    private void doDiffWithNotCommitted(@Nonnull List<String> filePatterns, @Nullable final Revision revision, final boolean isCached) {
        if (revision == null) {
            return;
        }

        ProjectDescriptor project = appContext.getCurrentProject().getRootProject();
        service.diff(project, filePatterns, RAW, false, 0, revision.getId(), isCached,
                     new AsyncRequestCallback<String>(new StringUnmarshaller()) {
                         @Override
                         protected void onSuccess(String result) {
                             view.setDiffContext(result);
                             String text = isCached ? constant.historyDiffIndexState() : constant.historyDiffTreeState();
                             displayCommitA(revision);
                             view.setCompareType(text);
                         }

                         @Override
                         protected void onFailure(Throwable exception) {
                             nothingToDisplay(revision);
                             String errorMessage = exception.getMessage() != null ? exception.getMessage() : constant.diffFailed();
                             Notification notification = new Notification(errorMessage, ERROR);
                             notificationManager.showNotification(notification);
                         }
                     });
    }

    /**
     * Perform diff between selected commit and previous one.
     *
     * @param filePatterns
     *         patterns for which to show diff
     * @param revisionB
     *         selected commit
     */
    private void doDiffWithPrevVersion(@Nonnull List<String> filePatterns, @Nullable final Revision revisionB) {
        if (revisionB == null) {
            return;
        }

        int index = revisions.indexOf(revisionB);
        if (index + 1 < revisions.size()) {
            final Revision revisionA = revisions.get(index + 1);
            ProjectDescriptor project = appContext.getCurrentProject().getRootProject();
            service.diff(project, filePatterns, RAW, false, 0, revisionA.getId(),
                         revisionB.getId(),
                         new AsyncRequestCallback<String>(new StringUnmarshaller()) {
                             @Override
                             protected void onSuccess(String result) {
                                 view.setDiffContext(result);
                                 view.setCompareType("");
                                 displayCommitA(revisionA);
                                 displayCommitB(revisionB);
                             }

                             @Override
                             protected void onFailure(Throwable exception) {
                                 nothingToDisplay(revisionB);
                                 String errorMessage = exception.getMessage() != null ? exception.getMessage() : constant.diffFailed();
                                 Notification notification = new Notification(errorMessage, ERROR);
                                 notificationManager.showNotification(notification);
                             }
                         });
        } else {
            nothingToDisplay(revisionB);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getTitle() {
        return constant.historyTitle();
    }

    /** {@inheritDoc} */
    @Override
    public ImageResource getTitleImage() {
        return resources.history();
    }

    /** {@inheritDoc} */
    @Override
    public SVGResource getTitleSVGImage() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getTitleToolTip() {
        return constant.historyTitle();
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
    }

    /** {@inheritDoc} */
    @Override
    public int getSize() {
        return 450;
    }

    protected enum DiffWith {
        DIFF_WITH_INDEX,
        DIFF_WITH_WORK_TREE,
        DIFF_WITH_PREV_VERSION
    }
}
