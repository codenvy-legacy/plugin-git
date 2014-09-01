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
import com.codenvy.api.project.server.ProjectImporter;
import com.codenvy.api.project.server.ProjectManager;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.ide.ext.git.server.nativegit.NativeGitConnectionFactory;
import com.codenvy.ide.ext.git.shared.BranchCheckoutRequest;
import com.codenvy.ide.ext.git.shared.CloneRequest;
import com.codenvy.ide.ext.git.shared.FetchRequest;
import com.codenvy.ide.ext.git.shared.InitRequest;
import com.codenvy.ide.ext.git.shared.RemoteAddRequest;
import com.codenvy.vfs.impl.fs.LocalPathResolver;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getDescription() {
        return "Add possibility to import project from GIT repository";
    }


    @Override
    public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters)
            throws ForbiddenException, ConflictException, UnauthorizedException, IOException, ServerException {
        try {
            if (!baseFolder.isFolder()) {
                throw new IOException("Project cannot be imported into \"" + baseFolder.getName() + "\". It is not a folder.");
            }

            String fullPathToClonedProject =
                    localPathResolver.resolve((com.codenvy.vfs.impl.fs.VirtualFileImpl)baseFolder.getVirtualFile());
            GitConnection gitConnection = nativeGitConnectionFactory.getConnection(fullPathToClonedProject);
            String branch = null;
            if (parameters != null) {
                branch = parameters.get("vcsbranch");
            }
            final DtoFactory dtoFactory = DtoFactory.getInstance();
            if (!isFolderEmpty(baseFolder)) {
                gitConnection = gitConnection.init(dtoFactory.createDto(InitRequest.class)
                                                             .withWorkingDir(fullPathToClonedProject)
                                                             .withInitCommit(false)
                                                             .withBare(false));
                gitConnection.remoteAdd(dtoFactory.createDto(RemoteAddRequest.class)
                                                  .withName(DEFAULT_REMOTE)
                                                  .withUrl(location));
                List<String> refSpec;
                if (branch == null || "master".equals(branch)) {
                    refSpec = Collections.singletonList("refs/heads/master:refs/remotes/origin/master");
                } else {
                    refSpec = Collections.singletonList(String.format("refs/heads/%1$s:refs/remotes/origin/%1$s", branch));
                }
                gitConnection.fetch(dtoFactory.createDto(FetchRequest.class).withRemote(DEFAULT_REMOTE).withRefSpec(refSpec));
                if (branch == null || "master".equals(branch)) {
                    gitConnection.branchCheckout(dtoFactory.createDto(BranchCheckoutRequest.class).withName("master"));
                } else {
                    gitConnection.branchCheckout(dtoFactory.createDto(BranchCheckoutRequest.class).withName(branch));
                }
            } else {
                gitConnection.clone(dtoFactory.createDto(CloneRequest.class)
                                              .withWorkingDir(fullPathToClonedProject)
                                              .withRemoteName(DEFAULT_REMOTE)
                                              .withRemoteUri(location));
                if (branch != null) {
                    gitConnection.branchCheckout(dtoFactory.createDto(BranchCheckoutRequest.class).withName(branch));
                }
            }

//            if (!baseFolder.isProjectFolder()) {
//                String propertyFileContent = "{\"type\":\"" + com.codenvy.api.project.shared.Constants.BLANK_ID + "\"}";
//                FolderEntry projectMetaFolder = baseFolder.createFolder(".codenvy");
//                projectMetaFolder.createFile("project.json", propertyFileContent.getBytes(), MediaType.APPLICATION_JSON_TYPE.getType());
//            }

        } catch (UnauthorizedException e) {
            throw new UnauthorizedException(
                    "You are not authorized to perform the remote import.  Codenvy may need accurate keys to the external system. " +
                    "You can create a new key pair in Window->Preferences->SSH Keys.");
        } catch (URISyntaxException e) {
            throw new ServerException(
                    "Your project cannot be imported.  The issue is either from git configuration, a malformed URL, or file system corruption.  " +
                    "Please contact support for assistance.", e);
        }
    }

    private boolean isFolderEmpty(FolderEntry folder) throws ServerException {
        return folder.getChildren().size() == 0;
    }
}
