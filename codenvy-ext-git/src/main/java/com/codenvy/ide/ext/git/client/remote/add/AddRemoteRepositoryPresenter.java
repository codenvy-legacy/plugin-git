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
package com.codenvy.ide.ext.git.client.remote.add;

import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.ide.api.app.AppContext;
import com.codenvy.ide.ext.git.client.GitServiceClient;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * Presenter for adding remote repository.
 *
 * @author <a href="mailto:zhulevaanna@gmail.com">Ann Zhuleva</a>
 */
@Singleton
public class AddRemoteRepositoryPresenter implements AddRemoteRepositoryView.ActionDelegate {
    private AddRemoteRepositoryView view;
    private GitServiceClient        service;
    private AppContext appContext;
    private AsyncCallback<Void>     callback;

    /**
     * Create presenter.
     *
     * @param view
     * @param service
     * @param appContext
     */
    @Inject
    public AddRemoteRepositoryPresenter(AddRemoteRepositoryView view, GitServiceClient service, AppContext appContext) {
        this.view = view;
        this.view.setDelegate(this);
        this.service = service;
        this.appContext = appContext;
    }

    /** Show dialog. */
    public void showDialog(@Nonnull AsyncCallback<Void> callback) {
        this.callback = callback;
        view.setUrl("");
        view.setName("");
        view.setEnableOkButton(false);
        view.showDialog();
    }

    /** {@inheritDoc} */
    @Override
    public void onOkClicked() {
        String name = view.getName();
        String url = view.getUrl();
        final ProjectDescriptor project = appContext.getCurrentProject().getRootProject();

        service.remoteAdd(project, name, url, new AsyncRequestCallback<String>() {
            @Override
            protected void onSuccess(String result) {
                callback.onSuccess(null);
                view.close();
            }

            @Override
            protected void onFailure(Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onCancelClicked() {
        view.close();
    }

    /** {@inheritDoc} */
    @Override
    public void onValueChanged() {
        String name = view.getName();
        String url = view.getUrl();
        boolean isEnabled = !name.isEmpty() && !url.isEmpty();
        view.setEnableOkButton(isEnabled);
    }
}