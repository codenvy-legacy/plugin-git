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
package org.eclipse.che.ide.ext.git.client.inject;

import org.eclipse.che.ide.api.extension.ExtensionGinModule;
import org.eclipse.che.ide.api.project.wizard.ImportWizardRegistrar;
import org.eclipse.che.ide.ext.git.client.GitOutputPartView;
import org.eclipse.che.ide.ext.git.client.GitOutputPartViewImpl;
import org.eclipse.che.ide.ext.git.client.GitServiceClient;
import org.eclipse.che.ide.ext.git.client.GitServiceClientImpl;
import org.eclipse.che.ide.ext.git.client.add.AddToIndexView;
import org.eclipse.che.ide.ext.git.client.add.AddToIndexViewImpl;
import org.eclipse.che.ide.ext.git.client.branch.BranchView;
import org.eclipse.che.ide.ext.git.client.branch.BranchViewImpl;
import org.eclipse.che.ide.ext.git.client.commit.CommitView;
import org.eclipse.che.ide.ext.git.client.commit.CommitViewImpl;
import org.eclipse.che.ide.ext.git.client.fetch.FetchView;
import org.eclipse.che.ide.ext.git.client.fetch.FetchViewImpl;
import org.eclipse.che.ide.ext.git.client.history.HistoryView;
import org.eclipse.che.ide.ext.git.client.history.HistoryViewImpl;
import org.eclipse.che.ide.ext.git.client.importer.GitImportWizardRegistrar;
import org.eclipse.che.ide.ext.git.client.merge.MergeView;
import org.eclipse.che.ide.ext.git.client.merge.MergeViewImpl;
import org.eclipse.che.ide.ext.git.client.pull.PullView;
import org.eclipse.che.ide.ext.git.client.pull.PullViewImpl;
import org.eclipse.che.ide.ext.git.client.push.PushToRemoteView;
import org.eclipse.che.ide.ext.git.client.push.PushToRemoteViewImpl;
import org.eclipse.che.ide.ext.git.client.remote.RemoteView;
import org.eclipse.che.ide.ext.git.client.remote.RemoteViewImpl;
import org.eclipse.che.ide.ext.git.client.remote.add.AddRemoteRepositoryView;
import org.eclipse.che.ide.ext.git.client.remote.add.AddRemoteRepositoryViewImpl;
import org.eclipse.che.ide.ext.git.client.remove.RemoveFromIndexView;
import org.eclipse.che.ide.ext.git.client.remove.RemoveFromIndexViewImpl;
import org.eclipse.che.ide.ext.git.client.reset.commit.ResetToCommitView;
import org.eclipse.che.ide.ext.git.client.reset.commit.ResetToCommitViewImpl;
import org.eclipse.che.ide.ext.git.client.reset.files.ResetFilesView;
import org.eclipse.che.ide.ext.git.client.reset.files.ResetFilesViewImpl;
import org.eclipse.che.ide.ext.git.client.url.ShowProjectGitReadOnlyUrlView;
import org.eclipse.che.ide.ext.git.client.url.ShowProjectGitReadOnlyUrlViewImpl;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.multibindings.GinMultibinder;
import com.google.inject.Singleton;

/** @author <a href="mailto:aplotnikov@codenvy.com">Andrey Plotnikov</a> */
@ExtensionGinModule
public class GitGinModule extends AbstractGinModule {
    /** {@inheritDoc} */
    @Override
    protected void configure() {
        bind(GitServiceClient.class).to(GitServiceClientImpl.class).in(Singleton.class);

        GinMultibinder.newSetBinder(binder(), ImportWizardRegistrar.class).addBinding().to(GitImportWizardRegistrar.class);

        bind(AddToIndexView.class).to(AddToIndexViewImpl.class).in(Singleton.class);
        bind(ResetToCommitView.class).to(ResetToCommitViewImpl.class).in(Singleton.class);
        bind(RemoveFromIndexView.class).to(RemoveFromIndexViewImpl.class).in(Singleton.class);
        bind(CommitView.class).to(CommitViewImpl.class).in(Singleton.class);
        bind(BranchView.class).to(BranchViewImpl.class).in(Singleton.class);
        bind(MergeView.class).to(MergeViewImpl.class).in(Singleton.class);
        bind(ResetFilesView.class).to(ResetFilesViewImpl.class).in(Singleton.class);
        bind(ShowProjectGitReadOnlyUrlView.class).to(ShowProjectGitReadOnlyUrlViewImpl.class).in(Singleton.class);
        bind(RemoteView.class).to(RemoteViewImpl.class).in(Singleton.class);
        bind(AddRemoteRepositoryView.class).to(AddRemoteRepositoryViewImpl.class).in(Singleton.class);
        bind(PushToRemoteView.class).to(PushToRemoteViewImpl.class).in(Singleton.class);
        bind(FetchView.class).to(FetchViewImpl.class).in(Singleton.class);
        bind(PullView.class).to(PullViewImpl.class).in(Singleton.class);
        bind(HistoryView.class).to(HistoryViewImpl.class).in(Singleton.class);
        bind(GitOutputPartView.class).to(GitOutputPartViewImpl.class).in(Singleton.class);
    }
}