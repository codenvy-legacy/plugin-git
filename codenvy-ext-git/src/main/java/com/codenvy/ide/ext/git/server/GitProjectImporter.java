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
import com.codenvy.api.project.server.ProjectImporter;
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

    @Inject
    public GitProjectImporter(NativeGitConnectionFactory nativeGitConnectionFactory, LocalPathResolver localPathResolver) {
        this.nativeGitConnectionFactory = nativeGitConnectionFactory;
        this.localPathResolver = localPathResolver;
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
        importSources(baseFolder, location, parameters, LineConsumer.DEV_NULL);
    }

    @Override
    public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters, LineConsumer consumer)
            throws ForbiddenException, ConflictException, UnauthorizedException, IOException, ServerException {
        try {
            final String fullPath = localPathResolver.resolve((com.codenvy.vfs.impl.fs.VirtualFileImpl)baseFolder.getVirtualFile());
            // For factory: checkout particular commit after clone
            String commitId = null;
            // For factory: github pull request feature
            String remoteOriginFetch = null;
            String branch = null;
            if (parameters != null) {
                commitId = parameters.get("vcsCommitId");
                branch = parameters.get("branch");
                remoteOriginFetch = parameters.get("remoteOriginFetch");
            }
            final DtoFactory dtoFactory = DtoFactory.getInstance();
            final GitConnection git = nativeGitConnectionFactory.getConnection(fullPath, consumer);
            try {
                if (baseFolder.getChildren().size() == 0) {
                    cloneRepository(git, fullPath, "origin", location, dtoFactory);
                    if (commitId != null) {
                        checkoutCommit(git, commitId, dtoFactory);
                    } else if (remoteOriginFetch != null) {
                        git.getConfig().add("remote.origin.fetch", remoteOriginFetch);
                        fetch(git, "origin", dtoFactory);
                        if (branch != null) {
                            checkoutBranch(git, branch, dtoFactory);
                        }
                    } else if (branch != null) {
                        checkoutBranch(git, branch, dtoFactory);
                    }
                } else {
                    initRepository(git, fullPath, dtoFactory);
                    addRemote(git, "origin", location, dtoFactory);
                    if (commitId != null) {
                        fetchBranch(git, "origin", branch == null ? "master" : branch, dtoFactory);
                        checkoutCommit(git, commitId, dtoFactory);
                    } else if (remoteOriginFetch != null) {
                        git.getConfig().add("remote.origin.fetch", remoteOriginFetch);
                        fetch(git, "origin", dtoFactory);
                        if (branch != null) {
                            checkoutBranch(git, branch, dtoFactory);
                        }
                    } else {
                        fetchBranch(git, "origin", branch == null ? "master" : branch, dtoFactory);
                        checkoutBranch(git, branch == null ? "master" : branch, dtoFactory);
                    }
                }
            } finally {
                git.close();
            }
        } catch (UnauthorizedException e) {
            throw new UnauthorizedException(
                    "You are not authorized to perform the remote import.  Codenvy may need accurate keys to the external system. You can create a new key pair in Window->Preferences->SSH Keys.");
        } catch (URISyntaxException e) {
            throw new ServerException(
                    "Your project cannot be imported.  The issue is either from git configuration, a malformed URL, or file system corruption. Please contact support for assistance.",
                    e);
        }
    }

    private void cloneRepository(GitConnection git, String path, String remoteName, String url, DtoFactory dtoFactory)
            throws ServerException, UnauthorizedException, URISyntaxException {
        final CloneRequest request =
                dtoFactory.createDto(CloneRequest.class).withWorkingDir(path).withRemoteName(remoteName).withRemoteUri(url);
        git.clone(request);
    }

    private void initRepository(GitConnection git, String path, DtoFactory dtoFactory) throws GitException {
        final InitRequest request = dtoFactory.createDto(InitRequest.class).withWorkingDir(path).withInitCommit(false).withBare(false);
        git.init(request);
    }

    private void addRemote(GitConnection git, String name, String url, DtoFactory dtoFactory) throws GitException {
        final RemoteAddRequest request = dtoFactory.createDto(RemoteAddRequest.class).withName(name).withUrl(url);
        git.remoteAdd(request);
    }

    private void fetch(GitConnection git, String remote, DtoFactory dtoFactory) throws UnauthorizedException, GitException {
        final FetchRequest request = dtoFactory.createDto(FetchRequest.class).withRemote(remote);
        git.fetch(request);
    }

    private void fetchBranch(GitConnection gitConnection, String remote, String branch, DtoFactory dtoFactory)
            throws UnauthorizedException, GitException {
        final List<String> refSpecs = Collections.singletonList(String.format("refs/heads/%1$s:refs/remotes/origin/%1$s", branch));
        fetchRefSpecs(gitConnection, remote, refSpecs, dtoFactory);
    }

    private void fetchRefSpecs(GitConnection git, String remote, List<String> refSpecs, DtoFactory dtoFactory)
            throws UnauthorizedException, GitException {
        final FetchRequest request = dtoFactory.createDto(FetchRequest.class).withRemote(remote).withRefSpec(refSpecs);
        git.fetch(request);
    }

    private void checkoutCommit(GitConnection git, String commit, DtoFactory dtoFactory) throws GitException {
        final BranchCheckoutRequest request = dtoFactory.createDto(BranchCheckoutRequest.class).withName("temp").withCreateNew(true)
                                                        .withStartPoint(commit);
        git.branchCheckout(request);
    }

    private void checkoutBranch(GitConnection git, String branch, DtoFactory dtoFactory) throws GitException {
        final BranchCheckoutRequest request = dtoFactory.createDto(BranchCheckoutRequest.class).withName(branch);
        git.branchCheckout(request);
    }
}
