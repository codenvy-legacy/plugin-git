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
package com.codenvy.ide.ext.github.server.inject;

import com.codenvy.api.project.server.ProjectImporter;
import com.codenvy.ide.ext.git.server.nativegit.CredentialsProvider;
import com.codenvy.ide.ext.git.server.nativegit.SshKeyUploader;
import com.codenvy.ide.ext.github.server.GitHub;
import com.codenvy.ide.ext.github.server.GitHubKeyUploader;
import com.codenvy.ide.ext.github.server.GitHubProjectImporter;
import com.codenvy.ide.ext.github.server.oauth.GitHubOAuthCredentialProvider;
import com.codenvy.ide.ext.github.server.rest.GitHubExceptionMapper;
import com.codenvy.ide.ext.github.server.rest.GitHubService;
import com.codenvy.inject.DynaModule;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * The module that contains configuration of the server side part of the GitHub extension.
 *
 * @author Andrey Plotnikov
 */
@DynaModule
public class GitHubModule extends AbstractModule {

    /** {@inheritDoc} */
    @Override
    protected void configure() {
        bind(GitHub.class);

        Multibinder<ProjectImporter> projectImporterMultibinder = Multibinder.newSetBinder(binder(), ProjectImporter.class);
        projectImporterMultibinder.addBinding().to(GitHubProjectImporter.class);

        Multibinder.newSetBinder(binder(), CredentialsProvider.class).addBinding().to(GitHubOAuthCredentialProvider.class);
        Multibinder.newSetBinder(binder(), SshKeyUploader.class).addBinding().to(GitHubKeyUploader.class);

        bind(GitHubService.class);
        bind(GitHubExceptionMapper.class);
    }
}
