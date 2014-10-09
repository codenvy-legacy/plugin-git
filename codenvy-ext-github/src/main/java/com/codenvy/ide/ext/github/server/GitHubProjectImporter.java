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
package com.codenvy.ide.ext.github.server;

import com.codenvy.ide.ext.git.server.GitConnectionFactory;
import com.codenvy.ide.ext.git.server.GitProjectImporter;
import com.codenvy.vfs.impl.fs.LocalPathResolver;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Roman Nikitenko
 */
@Singleton
public class GitHubProjectImporter extends GitProjectImporter{

    @Inject
    public GitHubProjectImporter(GitConnectionFactory gitConnectionFactory,
                                 LocalPathResolver localPathResolver) {
        super(gitConnectionFactory, localPathResolver);
    }

    @Override
    public String getId() {
        return "github";
    }

    @Override
    public String getDescription() {
        return "Import project from github.";
    }
}
