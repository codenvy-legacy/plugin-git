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

import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.UnauthorizedException;
import com.codenvy.api.project.server.AbstractVirtualFileEntry;
import com.codenvy.api.project.server.FolderEntry;
import com.codenvy.api.project.server.ProjectImporter;
import com.codenvy.api.project.server.ProjectManager;
import com.codenvy.api.vfs.server.exceptions.VirtualFileSystemException;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.ide.Constants;
import com.codenvy.ide.ext.git.server.nativegit.NativeGitConnectionFactory;
import com.codenvy.ide.ext.git.shared.BranchCheckoutRequest;
import com.codenvy.ide.ext.git.shared.CloneRequest;
import com.codenvy.ide.ext.git.shared.FetchRequest;
import com.codenvy.ide.ext.git.shared.InitRequest;
import com.codenvy.ide.ext.git.shared.RemoteAddRequest;
import com.codenvy.vfs.impl.fs.LocalPathResolver;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

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
    public void importSources(FolderEntry baseFolder, String location) throws IOException, ApiException {
        super.importSources(baseFolder, location);

        //cleanup git
        AbstractVirtualFileEntry gitFolder = baseFolder.getChild(".git");
        if (gitFolder != null)
            gitFolder.remove();
        AbstractVirtualFileEntry gitIgnore = baseFolder.getChild(".gitignore");
        if (gitIgnore != null)
            gitIgnore.remove();
    }




}
