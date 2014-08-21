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
package com.codenvy.ide.ext.git.server;

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.UnauthorizedException;
import com.codenvy.api.project.server.FolderEntry;
import com.codenvy.api.project.server.ProjectManager;
import com.codenvy.api.project.server.VirtualFileEntry;
import com.codenvy.ide.ext.git.server.nativegit.NativeGitConnectionFactory;
import com.codenvy.vfs.impl.fs.LocalPathResolver;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;

/**
 * @author Vitalii Parfonov
 */
@Singleton
public class GitProjectImporterGitCleanUp extends GitProjectImporter {

    @Inject
    public GitProjectImporterGitCleanUp(NativeGitConnectionFactory nativeGitConnectionFactory, LocalPathResolver localPathResolver,
                                        ProjectManager projectManager) {
        super(nativeGitConnectionFactory, localPathResolver, projectManager);

    }

    @Override
    public String getId() {
        return "git-less";
    }


    @Override
    public String getDescription() {
        return "Add possibility to import project from GIT repository. And remove .git folder after cloning. Useful for creation project from templates";
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public void importSources(FolderEntry baseFolder, String location)
            throws IOException, ForbiddenException, ServerException, UnauthorizedException, ConflictException {
        super.importSources(baseFolder, location);

        //cleanup git
        VirtualFileEntry gitFolder = baseFolder.getChild(".git");
        if (gitFolder != null)
            gitFolder.remove();
        VirtualFileEntry gitIgnore = baseFolder.getChild(".gitignore");
        if (gitIgnore != null)
            gitIgnore.remove();
    }
}
