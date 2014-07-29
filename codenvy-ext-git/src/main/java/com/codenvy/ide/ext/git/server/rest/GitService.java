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
package com.codenvy.ide.ext.git.server.rest;

import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.UnauthorizedException;
import com.codenvy.api.vfs.server.MountPoint;
import com.codenvy.api.vfs.server.VirtualFile;
import com.codenvy.api.vfs.server.VirtualFileSystem;
import com.codenvy.api.vfs.server.VirtualFileSystemRegistry;
import com.codenvy.api.vfs.shared.PropertyFilter;
import com.codenvy.api.vfs.shared.dto.Item;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.ide.ext.git.server.GitConnection;
import com.codenvy.ide.ext.git.server.GitConnectionFactory;
import com.codenvy.ide.ext.git.server.GitException;
import com.codenvy.ide.ext.git.server.InfoPage;
import com.codenvy.ide.ext.git.server.LogPage;
import com.codenvy.ide.ext.git.shared.AddRequest;
import com.codenvy.ide.ext.git.shared.Branch;
import com.codenvy.ide.ext.git.shared.BranchCheckoutRequest;
import com.codenvy.ide.ext.git.shared.BranchCreateRequest;
import com.codenvy.ide.ext.git.shared.BranchDeleteRequest;
import com.codenvy.ide.ext.git.shared.BranchListRequest;
import com.codenvy.ide.ext.git.shared.CloneRequest;
import com.codenvy.ide.ext.git.shared.CommitRequest;
import com.codenvy.ide.ext.git.shared.Commiters;
import com.codenvy.ide.ext.git.shared.DiffRequest;
import com.codenvy.ide.ext.git.shared.FetchRequest;
import com.codenvy.ide.ext.git.shared.InitRequest;
import com.codenvy.ide.ext.git.shared.LogRequest;
import com.codenvy.ide.ext.git.shared.MergeRequest;
import com.codenvy.ide.ext.git.shared.MergeResult;
import com.codenvy.ide.ext.git.shared.MoveRequest;
import com.codenvy.ide.ext.git.shared.PullRequest;
import com.codenvy.ide.ext.git.shared.PushRequest;
import com.codenvy.ide.ext.git.shared.Remote;
import com.codenvy.ide.ext.git.shared.RemoteAddRequest;
import com.codenvy.ide.ext.git.shared.RemoteListRequest;
import com.codenvy.ide.ext.git.shared.RemoteUpdateRequest;
import com.codenvy.ide.ext.git.shared.RepoInfo;
import com.codenvy.ide.ext.git.shared.ResetRequest;
import com.codenvy.ide.ext.git.shared.Revision;
import com.codenvy.ide.ext.git.shared.RmRequest;
import com.codenvy.ide.ext.git.shared.Status;
import com.codenvy.ide.ext.git.shared.Tag;
import com.codenvy.ide.ext.git.shared.TagCreateRequest;
import com.codenvy.ide.ext.git.shared.TagDeleteRequest;
import com.codenvy.ide.ext.git.shared.TagListRequest;
import com.codenvy.vfs.impl.fs.GitUrlResolver;
import com.codenvy.vfs.impl.fs.LocalPathResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/** @author andrew00x */
@Path("git/{ws-id}")
public class GitService {
    private static final Logger LOG = LoggerFactory.getLogger(GitService.class);
    @Inject
    private LocalPathResolver         localPathResolver;
    @Inject
    private GitUrlResolver            gitUrlResolver;
    @Inject
    private VirtualFileSystemRegistry vfsRegistry;
    @Inject
    private GitConnectionFactory      gitConnectionFactory;

    @PathParam("ws-id")
    private String vfsId;
    @QueryParam("projectPath")
    private String projectPath;

    @Path("add")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void add(AddRequest request) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            gitConnection.add(request);
        } finally {
            gitConnection.close();
        }
    }

    @Path("branch-checkout")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void branchCheckout(BranchCheckoutRequest request) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            gitConnection.branchCheckout(request);
        } finally {
            gitConnection.close();
        }
    }

    @Path("branch-create")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Branch branchCreate(BranchCreateRequest request) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            return gitConnection.branchCreate(request);
        } finally {
            gitConnection.close();
        }
    }

    @Path("branch-delete")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void branchDelete(BranchDeleteRequest request) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            gitConnection.branchDelete(request);
        } finally {
            gitConnection.close();
        }
    }

    @Path("branch-rename")
    @POST
    public void branchRename(@QueryParam("oldName") String oldName,
                             @QueryParam("newName") String newName) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            gitConnection.branchRename(oldName, newName);
        } finally {
            gitConnection.close();
        }
    }

    @Path("branch-list")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public GenericEntity<List<Branch>> branchList(BranchListRequest request) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            return new GenericEntity<List<Branch>>(gitConnection.branchList(request)) {
            };
        } finally {
            gitConnection.close();
        }
    }

    @Path("clone")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RepoInfo clone(final CloneRequest request)
            throws URISyntaxException, UnauthorizedException, NotFoundException, ForbiddenException, ServerException {
        long start = System.currentTimeMillis();
        // On-the-fly resolving of repository's working directory.
        request.setWorkingDir(resolveLocalPathByPath(request.getWorkingDir()));
        LOG.info("Repository clone from '" + request.getRemoteUri() + "' to '" + request.getWorkingDir() + "' started");
        GitConnection gitConnection = getGitConnection();
        try {
            gitConnection.clone(request);
            return DtoFactory.getInstance().createDto(RepoInfo.class).withRemoteUri(request.getRemoteUri());
        } finally {
            long end = System.currentTimeMillis();
            long seconds = (end - start) / 1000;
            LOG.info("Repository clone from '" + request.getRemoteUri() + "' to '" + request.getWorkingDir()
                     + "' finished. Process took " + seconds + " seconds (" + seconds / 60 + " minutes)");
            gitConnection.close();
        }
    }

    @Path("commit")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Revision commit(CommitRequest request) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        Revision revision = gitConnection.commit(request);
        try {
            if (revision.isFake()) {
                Status status = status(false);

                try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    ((InfoPage)status).writeTo(bos);
                    revision.setMessage(new String(bos.toByteArray()));
                } catch (IOException e) {
                    LOG.error("Cant write to revision", e);
                    throw new GitException("Cant execute status");
                }
            }
        } finally {
            gitConnection.close();
        }
        return revision;
    }

    @Path("diff")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public InfoPage diff(DiffRequest request) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            return gitConnection.diff(request);
        } finally {
            gitConnection.close();
        }
    }

    @Path("fetch")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void fetch(FetchRequest request) throws NotFoundException, ForbiddenException, UnauthorizedException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            gitConnection.fetch(request);
        } finally {
            gitConnection.close();
        }
    }

    @Path("init")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void init(final InitRequest request) throws NotFoundException, ForbiddenException, ServerException {
        request.setWorkingDir(resolveLocalPathByPath(projectPath));
        GitConnection gitConnection = getGitConnection();
        try {
            gitConnection.init(request);
        } finally {
            gitConnection.close();
        }
    }

    @Path("log")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public LogPage log(LogRequest request) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            return gitConnection.log(request);
        } finally {
            gitConnection.close();
        }
    }

    @Path("merge")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public MergeResult merge(MergeRequest request) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            return gitConnection.merge(request);
        } finally {
            gitConnection.close();
        }
    }

    @Path("mv")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void mv(MoveRequest request) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            gitConnection.mv(request);
        } finally {
            gitConnection.close();
        }
    }

    @Path("pull")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void pull(PullRequest request) throws NotFoundException, ForbiddenException, UnauthorizedException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            gitConnection.pull(request);
        } finally {
            gitConnection.close();
        }
    }

    @Path("push")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void push(PushRequest request) throws NotFoundException, ForbiddenException, UnauthorizedException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            gitConnection.push(request);
        } finally {
            gitConnection.close();
        }
    }

    @Path("remote-add")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void remoteAdd(RemoteAddRequest request) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            gitConnection.remoteAdd(request);
        } finally {
            gitConnection.close();
        }
    }

    @Path("remote-delete/{name}")
    @POST
    public void remoteDelete(@PathParam("name") String name) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            gitConnection.remoteDelete(name);
        } finally {
            gitConnection.close();
        }
    }

    @Path("remote-list")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public GenericEntity<List<Remote>> remoteList(RemoteListRequest request) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            return new GenericEntity<List<Remote>>(gitConnection.remoteList(request)) {
            };
        } finally {
            gitConnection.close();
        }
    }

    @Path("remote-update")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void remoteUpdate(RemoteUpdateRequest request) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            gitConnection.remoteUpdate(request);
        } finally {
            gitConnection.close();
        }
    }

    @Path("reset")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void reset(ResetRequest request) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            gitConnection.reset(request);
        } finally {
            gitConnection.close();
        }
    }

    @Path("rm")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void rm(RmRequest request) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            gitConnection.rm(request);
        } finally {
            gitConnection.close();
        }
    }

    @Path("status")
    @POST
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public Status status(@QueryParam("short") boolean shortFormat) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            return gitConnection.status(shortFormat);
        } finally {
            gitConnection.close();
        }
    }

    @Path("tag-create")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Tag tagCreate(TagCreateRequest request) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            return gitConnection.tagCreate(request);
        } finally {
            gitConnection.close();
        }
    }

    @Path("tag-delete")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void tagDelete(TagDeleteRequest request) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            gitConnection.tagDelete(request);
        } finally {
            gitConnection.close();
        }
    }

    @Path("tag-list")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public GenericEntity<List<Tag>> tagList(TagListRequest request) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            return new GenericEntity<List<Tag>>(gitConnection.tagList(request)) {
            };
        } finally {
            gitConnection.close();
        }
    }

    @Path("read-only-url")
    @GET
    public String readOnlyGitUrl(@Context UriInfo uriInfo) throws NotFoundException, ForbiddenException, ServerException {
        final VirtualFile virtualFile = vfsRegistry.getProvider(vfsId).getMountPoint(true).getVirtualFile(projectPath);
        return gitUrlResolver.resolve(uriInfo.getBaseUri(), (com.codenvy.vfs.impl.fs.VirtualFileImpl)virtualFile);
    }

    @GET
    @Path("commiters")
    public Commiters getCommiters(@Context UriInfo uriInfo) throws NotFoundException, ForbiddenException, ServerException {
        GitConnection gitConnection = getGitConnection();
        try {
            return DtoFactory.getInstance().createDto(Commiters.class).withCommiters(gitConnection.getCommiters());
        } finally {
            gitConnection.close();
        }
    }

    @GET
    @Path("delete-repository")
    public void deleteRepository(@Context UriInfo uriInfo) throws NotFoundException, ForbiddenException, ServerException {
        final VirtualFileSystem vfs = vfsRegistry.getProvider(vfsId).newInstance(null);
        final Item project = getGitProjectByPath(vfs, projectPath);
        final String path2gitFolder = project.getPath() + "/.git";
        final Item gitItem = vfs.getItemByPath(path2gitFolder, null, false, PropertyFilter.NONE_FILTER);
        vfs.delete(gitItem.getId(), null);
    }

    // TODO: this is temporary method
    private Item getGitProjectByPath(VirtualFileSystem vfs, String projectPath)
            throws NotFoundException, ForbiddenException, ServerException {
        final Item project = vfs.getItemByPath(projectPath, null, false, PropertyFilter.ALL_FILTER);
//        Item parent = vfs.getItem(project.getParentId(), false, PropertyFilter.ALL_FILTER);
//        if (parent.getItemType().equals(ItemType.PROJECT)) // MultiModule project
//            return parent;
        return project;
    }

    private Item getGitProject(VirtualFileSystem vfs, String projectId) throws NotFoundException, ForbiddenException, ServerException {
        final Item project = vfs.getItem(projectId, false, PropertyFilter.ALL_FILTER);
//        Item parent = vfs.getItem(project.getParentId(), false, PropertyFilter.ALL_FILTER);
//        if (parent.getItemType().equals(ItemType.PROJECT)) // MultiModule project
//            return parent;
        return project;
    }


    // TODO: this is temporary method
    protected String resolveLocalPathByPath(String folderPath) throws NotFoundException, ForbiddenException, ServerException {
        VirtualFileSystem vfs = vfsRegistry.getProvider(vfsId).newInstance(null);
        Item gitProject = getGitProjectByPath(vfs, folderPath);
        final MountPoint mountPoint = vfs.getMountPoint();
        final VirtualFile virtualFile = mountPoint.getVirtualFile(gitProject.getPath());
        return localPathResolver.resolve((com.codenvy.vfs.impl.fs.VirtualFileImpl)virtualFile);
    }

    protected GitConnection getGitConnection() throws NotFoundException, ForbiddenException, ServerException {
        return gitConnectionFactory.getConnection(resolveLocalPathByPath(projectPath));
    }
}
