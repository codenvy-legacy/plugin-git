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
package com.codenvy.ide.ext.git.server;

import com.codenvy.api.project.server.ProjectImporter;
import com.codenvy.api.project.server.ValueProviderFactory;
import com.codenvy.ide.ext.git.server.commons.GitRepositoryPrivacyChecker;
import com.codenvy.ide.ext.git.server.nativegit.NativeGitConnectionFactory;
import com.codenvy.ide.ext.git.server.rest.BranchListWriter;
import com.codenvy.ide.ext.git.server.rest.CommitMessageWriter;
import com.codenvy.ide.ext.git.server.rest.GitService;
import com.codenvy.ide.ext.git.server.rest.MergeResultWriter;
import com.codenvy.ide.ext.git.server.rest.RemoteListWriter;
import com.codenvy.ide.ext.git.server.rest.StatusPageWriter;
import com.codenvy.ide.ext.git.server.rest.TagListWriter;
import com.codenvy.inject.DynaModule;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * The module that contains configuration of the server side part of the Git extension.
 *
 * @author andrew00x
 */
@DynaModule
public class GitModule extends AbstractModule {

    /** {@inheritDoc} */
    @Override
    protected void configure() {
        Multibinder<ProjectImporter> projectImporterMultibinder = Multibinder.newSetBinder(binder(), ProjectImporter.class);
        projectImporterMultibinder.addBinding().to(GitProjectImporter.class);
        bind(GitConfigurationChecker.class).asEagerSingleton();
        bind(GitRepositoryPrivacyChecker.class);

        Multibinder<ValueProviderFactory> multiBinder = Multibinder.newSetBinder(binder(), ValueProviderFactory.class);
        multiBinder.addBinding().to(IsGitRepositoryValueProviderFactory.class);

        bind(GitService.class);
        bind(BranchListWriter.class);
        bind(CommitMessageWriter.class);
        bind(MergeResultWriter.class);
        bind(RemoteListWriter.class);
        bind(StatusPageWriter.class);
        bind(TagListWriter.class);

        bind(GitConnectionFactory.class).to(NativeGitConnectionFactory.class);
    }
}