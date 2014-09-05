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
import com.codenvy.api.core.util.LineConsumer;
import com.codenvy.api.project.server.FolderEntry;
import com.codenvy.api.project.server.ProjectManager;
import com.codenvy.api.project.server.VirtualFileEntry;
import com.codenvy.ide.ext.git.server.nativegit.NativeGitConnectionFactory;
import com.codenvy.vfs.impl.fs.LocalPathResolver;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.util.Map;

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
        return "Adds possibility to import project from GIT repository. Removes .git folder after cloning. Useful for creation project from templates";
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters)
            throws IOException, ForbiddenException, ServerException, UnauthorizedException, ConflictException {
        super.importSources(baseFolder, location, parameters);

        //cleanup git
        cleanupGit(baseFolder);
    }

    @Override
    public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters, LineConsumer consumer)
            throws ForbiddenException, ConflictException, UnauthorizedException, IOException, ServerException {
        super.importSources(baseFolder, location, parameters, LineConsumer.DEV_NULL);
        //cleanup git
        cleanupGit(baseFolder);
    }


    private void cleanupGit(FolderEntry baseFolder) throws ForbiddenException, ServerException {
        VirtualFileEntry gitFolder = baseFolder.getChild(".git");
        if (gitFolder != null) {
            gitFolder.remove();
        }
        VirtualFileEntry gitIgnore = baseFolder.getChild(".gitignore");
        if (gitIgnore != null) {
            gitIgnore.remove();
        }
    }
}
