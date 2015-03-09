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
package org.eclipse.che.ide.ext.github.server.inject;

import org.eclipse.che.api.project.server.ProjectImporter;
import org.eclipse.che.ide.ext.git.server.nativegit.SshKeyUploader;
import org.eclipse.che.ide.ext.github.server.GitHub;
import org.eclipse.che.ide.ext.github.server.GitHubKeyUploader;
import org.eclipse.che.ide.ext.github.server.GitHubProjectImporter;
import org.eclipse.che.ide.ext.github.server.rest.GitHubExceptionMapper;
import org.eclipse.che.ide.ext.github.server.rest.GitHubService;
import org.eclipse.che.inject.DynaModule;
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

        Multibinder.newSetBinder(binder(), SshKeyUploader.class).addBinding().to(GitHubKeyUploader.class);

        bind(GitHubService.class);
        bind(GitHubExceptionMapper.class);
    }
}
