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
 * @author Vladyslav Zhukovskii
 */
@Singleton
public class GitProjectImporter implements ProjectImporter {

    private final NativeGitConnectionFactory nativeGitConnectionFactory;
    private final LocalPathResolver          localPathResolver;
    private final ProjectManager             projectManager;

    private static final String DEFAULT_REMOTE = "origin";

    @Inject
    public GitProjectImporter(NativeGitConnectionFactory nativeGitConnectionFactory, LocalPathResolver localPathResolver,
                              ProjectManager projectManager) {
        this.nativeGitConnectionFactory = nativeGitConnectionFactory;
        this.localPathResolver = localPathResolver;
        this.projectManager = projectManager;
    }

    @Override
    public String getId() {
        return "git";
    }


    @Override
    public String getDescription() {
        return "Add possibility to import project from GIT repository";
    }

    @Override
    public void importSources(FolderEntry baseFolder, String location) throws IOException, ApiException {
        try {
            if (!baseFolder.isFolder()) {
                throw new IOException("Project cannot be imported into \"" + baseFolder.getName() + "\". It is not a folder.");
            }

            String fullPathToClonedProject = localPathResolver.resolve(baseFolder.getVirtualFile());
            GitConnection gitConnection = nativeGitConnectionFactory.getConnection(fullPathToClonedProject);

            if (!isFolderEmpty(baseFolder)) {
                gitConnection = gitConnection.init(DtoFactory.getInstance().createDto(InitRequest.class)
                                                             .withWorkingDir(fullPathToClonedProject)
                                                             .withInitCommit(false)
                                                             .withBare(false));
                gitConnection.remoteAdd(DtoFactory.getInstance().createDto(RemoteAddRequest.class)
                                                  .withName(DEFAULT_REMOTE)
                                                  .withUrl(location));
                gitConnection.fetch(DtoFactory.getInstance().createDto(FetchRequest.class)
                                              .withRemote(DEFAULT_REMOTE)
                                              .withRefSpec(Collections.singletonList("refs/heads/master:refs/remotes/origin/master")));
                gitConnection.branchCheckout(DtoFactory.getInstance().createDto(BranchCheckoutRequest.class)
                                                       .withName("master"));
            } else {
                gitConnection.clone(DtoFactory.getInstance().createDto(CloneRequest.class)
                                              .withWorkingDir(fullPathToClonedProject)
                                              .withRemoteName(DEFAULT_REMOTE)
                                              .withRemoteUri(location));
            }

            if (!baseFolder.isProjectFolder()) {
                String propertyFileContent = "{\"type\":\"" + Constants.NAMELESS_ID + "\"}";
                FolderEntry projectMetaFolder = baseFolder.createFolder(".codenvy");
                projectMetaFolder.createFile("project", propertyFileContent.getBytes(), MediaType.APPLICATION_JSON_TYPE.getType());
            }

        } catch (NotAuthorizedException e) {

            throw new UnauthorizedException("User is not authorize to call this action. " +
                                            "Try go to main menu Window->Preference->SSH Key and generate new keys pair");
        } catch (VirtualFileSystemException | GitException | URISyntaxException e) {
            throw new IOException("Selected project cannot be imported.", e);
        }
    }


    private boolean isFolderEmpty(FolderEntry folder) {
        return folder.getChildren().size() == 0;
    }


}
