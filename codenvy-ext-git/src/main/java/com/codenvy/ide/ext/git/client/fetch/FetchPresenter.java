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
package com.codenvy.ide.ext.git.client.fetch;

import com.codenvy.ide.api.app.AppContext;
import com.codenvy.ide.api.app.CurrentProject;
import com.codenvy.ide.api.notification.Notification;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.collections.Array;
import com.codenvy.ide.ext.git.client.GitLocalizationConstant;
import com.codenvy.ide.ext.git.client.GitServiceClient;
import com.codenvy.ide.ext.git.client.BranchSearcher;
import com.codenvy.ide.ext.git.shared.Branch;
import com.codenvy.ide.ext.git.shared.Remote;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.codenvy.ide.ui.dialogs.DialogFactory;
import com.codenvy.ide.websocket.WebSocketException;
import com.codenvy.ide.websocket.rest.RequestCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.codenvy.ide.api.notification.Notification.Type.ERROR;
import static com.codenvy.ide.api.notification.Notification.Type.INFO;
import static com.codenvy.ide.ext.git.shared.BranchListRequest.LIST_LOCAL;
import static com.codenvy.ide.ext.git.shared.BranchListRequest.LIST_REMOTE;

/**
 * Presenter for fetching changes from remote repository.
 *
 * @author Ann Zhuleva
 */
@Singleton
public class FetchPresenter implements FetchView.ActionDelegate {
    private final DtoUnmarshallerFactory  dtoUnmarshallerFactory;
    private final NotificationManager     notificationManager;
    private final BranchSearcher          branchSearcher;
    private       FetchView               view;
    private       GitServiceClient        service;
    private       AppContext              appContext;
    private       GitLocalizationConstant constant;
    private       CurrentProject          project;

    @Inject
    public FetchPresenter(FetchView view,
                          GitServiceClient service,
                          AppContext appContext,
                          GitLocalizationConstant constant,
                          NotificationManager notificationManager,
                          DtoUnmarshallerFactory dtoUnmarshallerFactory,
                          BranchSearcher branchSearcher) {
        this.view = view;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.branchSearcher = branchSearcher;
        this.view.setDelegate(this);
        this.service = service;
        this.appContext = appContext;
        this.constant = constant;
        this.notificationManager = notificationManager;
    }

    /** Show dialog. */
    public void showDialog() {
        project = appContext.getCurrentProject();
        view.setRemoveDeleteRefs(false);
        view.setFetchAllBranches(true);
        getRemotes();
    }

    /**
     * Get the list of remote repositories for local one. If remote repositories are found, then get the list of branches (remote and
     * local).
     */
    private void getRemotes() {
        service.remoteList(project.getRootProject(), null, true,
                           new AsyncRequestCallback<Array<Remote>>(dtoUnmarshallerFactory.newArrayUnmarshaller(Remote.class)) {
                               @Override
                               protected void onSuccess(Array<Remote> result) {
                                   view.setRepositories(result);
                                   getBranches(LIST_REMOTE);
                                   view.setEnableFetchButton(!result.isEmpty());
                                   view.showDialog();
                               }

                               @Override
                               protected void onFailure(Throwable exception) {
                                   String errorMessage =
                                           exception.getMessage() != null ? exception.getMessage() : constant.remoteListFailed();
                                   handleError(errorMessage);
                                   view.setEnableFetchButton(false);
                               }
                           });
    }

    private void handleError(@Nonnull String errorMessage) {
        Notification notification = new Notification(errorMessage, ERROR);
        notificationManager.showNotification(notification);
    }

    /**
     * Get the list of branches.
     *
     * @param remoteMode
     *         is a remote mode
     */
    private void getBranches(@Nonnull final String remoteMode) {
        service.branchList(project.getRootProject(), remoteMode,
                           new AsyncRequestCallback<Array<Branch>>(dtoUnmarshallerFactory.newArrayUnmarshaller(Branch.class)) {
                               @Override
                               protected void onSuccess(Array<Branch> result) {
                                   if (LIST_REMOTE.equals(remoteMode)) {
                                       view.setRemoteBranches(branchSearcher.getRemoteBranchesToDisplay(view.getRepositoryName(), result));
                                       getBranches(LIST_LOCAL);
                                   } else {
                                       view.setLocalBranches(branchSearcher.getLocalBranchesToDisplay(result));
                                       for (Branch branch : result.asIterable()) {
                                           if (branch.isActive()) {
                                               view.selectRemoteBranch(branch.getDisplayName());
                                               break;
                                           }
                                       }
                                   }
                               }

                               @Override
                               protected void onFailure(Throwable exception) {
                                   String errorMessage =
                                           exception.getMessage() != null ? exception.getMessage() : constant.branchesListFailed();
                                   Notification notification = new Notification(errorMessage, ERROR);
                                   notificationManager.showNotification(notification);
                                   view.setEnableFetchButton(false);
                               }
                           });
    }

    /** {@inheritDoc} */
    @Override
    public void onFetchClicked() {
        final String remoteUrl = view.getRepositoryUrl();
        String remoteName = view.getRepositoryName();
        boolean removeDeletedRefs = view.isRemoveDeletedRefs();

        try {
            service.fetch(project.getRootProject(), remoteName, getRefs(), removeDeletedRefs,
                          new RequestCallback<String>() {
                              @Override
                              protected void onSuccess(String result) {
                                  Notification notification = new Notification(constant.fetchSuccess(remoteUrl), INFO);
                                  notificationManager.showNotification(notification);
                              }

                              @Override
                              protected void onFailure(Throwable exception) {
                                  handleError(exception, remoteUrl);
                              }
                          }
                         );
        } catch (WebSocketException e) {
            handleError(e, remoteUrl);
        }
        view.close();
    }

    /** @return list of refs to fetch */
    @Nonnull
    private List<String> getRefs() {
        if (view.isFetchAllBranches()) {
            return new ArrayList<>();
        }

        String localBranch = view.getLocalBranch();
        String remoteBranch = view.getRemoteBranch();
        String remoteName = view.getRepositoryName();
        String refs = localBranch.isEmpty() ? remoteBranch
                                            : "refs/heads/" + localBranch + ":" + "refs/remotes/" + remoteName + "/" + remoteBranch;
        return new ArrayList<>(Arrays.asList(refs));
    }

    /**
     * Handler some action whether some exception happened.
     *
     * @param t
     *         exception what happened
     */
    private void handleError(@Nonnull Throwable t, @Nonnull String remoteUrl) {
        String errorMessage = (t.getMessage() != null) ? t.getMessage() : constant.fetchFail(remoteUrl);
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
        boolean isFetchAll = view.isFetchAllBranches();
        view.setEnableLocalBranchField(!isFetchAll);
        view.setEnableRemoteBranchField(!isFetchAll);
    }

    /** {@inheritDoc} */
    @Override
    public void onRemoteBranchChanged() {
        view.selectLocalBranch(view.getRemoteBranch());
    }
}