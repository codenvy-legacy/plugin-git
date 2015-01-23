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
package com.codenvy.ide.ext.git.server.nativegit;

import com.codenvy.api.core.UnauthorizedException;
import com.codenvy.api.core.util.LineConsumerFactory;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.ide.ext.git.server.Config;
import com.codenvy.ide.ext.git.server.DiffPage;
import com.codenvy.ide.ext.git.server.GitConnection;
import com.codenvy.ide.ext.git.server.GitException;
import com.codenvy.ide.ext.git.server.LogPage;
import com.codenvy.ide.ext.git.server.commons.Util;
import com.codenvy.ide.ext.git.server.nativegit.commands.AddCommand;
import com.codenvy.ide.ext.git.server.nativegit.commands.BranchCheckoutCommand;
import com.codenvy.ide.ext.git.server.nativegit.commands.BranchCreateCommand;
import com.codenvy.ide.ext.git.server.nativegit.commands.BranchDeleteCommand;
import com.codenvy.ide.ext.git.server.nativegit.commands.BranchListCommand;
import com.codenvy.ide.ext.git.server.nativegit.commands.CloneCommand;
import com.codenvy.ide.ext.git.server.nativegit.commands.CommitCommand;
import com.codenvy.ide.ext.git.server.nativegit.commands.EmptyGitCommand;
import com.codenvy.ide.ext.git.server.nativegit.commands.FetchCommand;
import com.codenvy.ide.ext.git.server.nativegit.commands.GitCommand;
import com.codenvy.ide.ext.git.server.nativegit.commands.InitCommand;
import com.codenvy.ide.ext.git.server.nativegit.commands.LogCommand;
import com.codenvy.ide.ext.git.server.nativegit.commands.LsRemoteCommand;
import com.codenvy.ide.ext.git.server.nativegit.commands.PullCommand;
import com.codenvy.ide.ext.git.server.nativegit.commands.PushCommand;
import com.codenvy.ide.ext.git.server.nativegit.commands.RemoteListCommand;
import com.codenvy.ide.ext.git.server.nativegit.commands.RemoteUpdateCommand;
import com.codenvy.ide.ext.git.shared.AddRequest;
import com.codenvy.ide.ext.git.shared.Branch;
import com.codenvy.ide.ext.git.shared.BranchCheckoutRequest;
import com.codenvy.ide.ext.git.shared.BranchCreateRequest;
import com.codenvy.ide.ext.git.shared.BranchDeleteRequest;
import com.codenvy.ide.ext.git.shared.BranchListRequest;
import com.codenvy.ide.ext.git.shared.CloneRequest;
import com.codenvy.ide.ext.git.shared.CommitRequest;
import com.codenvy.ide.ext.git.shared.DiffRequest;
import com.codenvy.ide.ext.git.shared.FetchRequest;
import com.codenvy.ide.ext.git.shared.GitUser;
import com.codenvy.ide.ext.git.shared.InitRequest;
import com.codenvy.ide.ext.git.shared.LogRequest;
import com.codenvy.ide.ext.git.shared.LsRemoteRequest;
import com.codenvy.ide.ext.git.shared.MergeRequest;
import com.codenvy.ide.ext.git.shared.MergeResult;
import com.codenvy.ide.ext.git.shared.MoveRequest;
import com.codenvy.ide.ext.git.shared.PullRequest;
import com.codenvy.ide.ext.git.shared.PushRequest;
import com.codenvy.ide.ext.git.shared.Remote;
import com.codenvy.ide.ext.git.shared.RemoteAddRequest;
import com.codenvy.ide.ext.git.shared.RemoteListRequest;
import com.codenvy.ide.ext.git.shared.RemoteReference;
import com.codenvy.ide.ext.git.shared.RemoteUpdateRequest;
import com.codenvy.ide.ext.git.shared.ResetRequest;
import com.codenvy.ide.ext.git.shared.Revision;
import com.codenvy.ide.ext.git.shared.RmRequest;
import com.codenvy.ide.ext.git.shared.Status;
import com.codenvy.ide.ext.git.shared.Tag;
import com.codenvy.ide.ext.git.shared.TagCreateRequest;
import com.codenvy.ide.ext.git.shared.TagDeleteRequest;
import com.codenvy.ide.ext.git.shared.TagListRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Native implementation of GitConnection
 *
 * @author Eugene Voevodin
 */
public class NativeGitConnection implements GitConnection {

    private static final Logger LOG = LoggerFactory.getLogger(NativeGitConnection.class);

    private final NativeGit         nativeGit;
    private final CredentialsLoader credentialsLoader;
    private final GitAskPassScript  gitAskPassScript;
    private final GitUser           user;
    private final SshKeysManager    keysManager;

    private static final Pattern authErrorPattern =
            Pattern.compile(
                    ".*fatal: could not read (Username|Password) for '.*': No such device or address.*|" +
                    ".*fatal: could not read (Username|Password) for '.*': Input/output error.*|" +
                    ".*fatal: Authentication failed for '.*'.*|.*fatal: Could not read from remote repository\\.\\n\\nPlease make sure " +
                    "you have the correct access rights\\nand the repository exists\\.\\n.*",
                    Pattern.MULTILINE);

    /**
     * @param repository
     *         directory where commands will be invoked
     * @param user
     *         git user
     * @param keysManager
     *         manager for ssh keys. If it is null default ssh will be used;
     * @param credentialsLoader
     *         loader for credentials
     * @throws GitException
     *         when some error occurs
     */
    public NativeGitConnection(File repository, GitUser user, SshKeysManager keysManager, CredentialsLoader credentialsLoader)
            throws GitException {
        this(new NativeGit(repository), user, keysManager, credentialsLoader, new GitAskPassScript());
    }

    /**
     * @param nativeGit
     *         native git client
     * @param user
     *         git user
     * @param keysManager
     *         manager for ssh keys. If it is null default ssh will be used;
     * @param credentialsLoader
     *         loader for credentials
     * @throws GitException
     *         when some error occurs
     */
    public NativeGitConnection(NativeGit nativeGit,
                               GitUser user,
                               SshKeysManager keysManager,
                               CredentialsLoader credentialsLoader,
                               GitAskPassScript gitAskPassScript
                              )
            throws GitException {
        this.user = user;
        this.keysManager = keysManager;
        this.credentialsLoader = credentialsLoader;
        this.nativeGit = nativeGit;
        this.gitAskPassScript = gitAskPassScript;
    }

    @Override
    public File getWorkingDir() {
        return nativeGit.getRepository();
    }

    @Override
    public void add(AddRequest request) throws GitException {
        AddCommand command = nativeGit.createAddCommand();
        command.setFilePattern(request.getFilepattern() == null ?
                               AddRequest.DEFAULT_PATTERN :
                               request.getFilepattern());
        command.setUpdate(request.isUpdate());
        command.execute();
    }

    @Override
    public void branchCheckout(BranchCheckoutRequest request) throws GitException {
        BranchCheckoutCommand command = nativeGit.createBranchCheckoutCommand();
        /*
         * IF branch name is origin/HEAD then *(no branch).
         * Create new means that remote branch was selected,
         * so git checkout -t remote/branchName will create
         * branchName tracked to remote/branchName
         */
        if (request.isCreateNew()) {
            try {
                if (!(getBranchRef(request.getName()).startsWith("refs/remotes/") && request.getName().endsWith("/HEAD"))) {
                    command.setRemote(true);
                }
            } catch (GitException ignored) {
                command.setCreateNew(true);
                command.setStartPoint(request.getStartPoint());
            }
        }
        command.setBranchName(request.getName()).execute();
    }

    @Override
    public Branch branchCreate(BranchCreateRequest request) throws GitException {
        BranchCreateCommand branchCreateCommand = nativeGit.createBranchCreateCommand();
        branchCreateCommand.setBranchName(request.getName())
                           .setStartPoint(request.getStartPoint())
                           .execute();
        return DtoFactory.getInstance().createDto(Branch.class).withName(getBranchRef(request.getName())).withActive(false)
                         .withDisplayName(request.getName()).withRemote(false);
    }

    @Override
    public void branchDelete(BranchDeleteRequest request) throws GitException, UnauthorizedException {
        String branchName = getBranchRef(request.getName());
        String remoteName = null;
        BranchDeleteCommand branchDeleteCommand = null;

        if (branchName.startsWith("refs/remotes/")) {
            remoteName = parseRemoteName(branchName);

            String remoteUri;
            try {
                remoteUri = nativeGit.createRemoteListCommand()
                                     .setRemoteName(remoteName)
                                     .execute()
                                     .get(0)
                                     .getUrl();
            } catch (GitException ignored) {
                remoteUri = remoteName;
            }
            if (Util.isSSH(remoteUri)) {
                branchDeleteCommand = nativeGit.createBranchDeleteCommand(keysManager.writeKeyFile(remoteUri).getAbsolutePath());
            }
        }
        branchName = parseBranchName(branchName);

        if (branchDeleteCommand == null) {
            branchDeleteCommand = nativeGit.createBranchDeleteCommand();
        }

        branchDeleteCommand.setBranchName(branchName);
        branchDeleteCommand.setRemote(remoteName);
        branchDeleteCommand.setDeleteFullyMerged(request.isForce());

        executeWithCredentials(branchDeleteCommand, remoteName);
    }

    @Override
    public void branchRename(String oldName, String newName) throws GitException {
        nativeGit.createBranchRenameCommand().setNames(oldName, newName).execute();
    }

    @Override
    public List<Branch> branchList(BranchListRequest request) throws GitException {
        String listMode = request.getListMode();
        if (listMode != null
            && !(listMode.equals(BranchListRequest.LIST_ALL) || listMode.equals(BranchListRequest.LIST_REMOTE))) {
            throw new IllegalArgumentException("Unsupported list mode '" + listMode + "'. Must be either 'a' or 'r'. ");
        }
        List<Branch> branches;
        BranchListCommand branchListCommand = nativeGit.createBranchListCommand();
        if (request.getListMode() == null) {
            branches = branchListCommand.execute();
        } else if (request.getListMode().equals(BranchListRequest.LIST_ALL)) {
            branches = branchListCommand.execute();
            branches.addAll(branchListCommand.setShowRemotes(true).execute());
        } else {
            branches = branchListCommand.setShowRemotes(true).execute();
        }
        return branches;
    }

    @Override
    public void clone(CloneRequest request) throws URISyntaxException, UnauthorizedException, GitException {
        CloneCommand clone;
        final String remoteUri = request.getRemoteUri();
        if (Util.isSSH(remoteUri)) {
            clone = nativeGit.createCloneCommand(keysManager.writeKeyFile(remoteUri).getAbsolutePath());
        } else {
            clone = nativeGit.createCloneCommand();
        }
        clone.setUri(remoteUri);
        clone.setRemoteName(request.getRemoteName());
        if (clone.getTimeout() > 0) {
            clone.setTimeout(request.getTimeout());
        }
        executeWithCredentials(clone, remoteUri);
        UserCredential credentials = credentialsLoader.getUserCredential(remoteUri);
        if (credentials != null) {
            getConfig().set("codenvy.credentialsProvider", credentials.getProviderId());
        }
        nativeGit.createRemoteUpdateCommand()
                 .setRemoteName(request.getRemoteName() == null ? "origin" : request.getRemoteName())
                 .setNewUrl(remoteUri)
                 .execute();
    }

    @Override
    public Revision commit(CommitRequest request) throws GitException {
        CommitCommand command = nativeGit.createCommitCommand();
        GitUser committer = getLocalCommitter();
        command.setCommitter(committer);

        try {
            // overrider author from .gitconfig. We may set it in previous versions.
            // We need to override it since committer can differ from the person who clone or init repository.
            getConfig().get("user.name");
            command.setAuthor(committer);
        } catch (GitException e) {
            //ignore property not found.
        }


        command.setAll(request.isAll());
        command.setAmend(request.isAmend());
        command.setMessage(request.getMessage());

        try {
            command.execute();
            LogCommand log = nativeGit.createLogCommand();
            Revision rev = log.execute().get(0);
            rev.setBranch(getCurrentBranch());
            return rev;
        } catch (Exception e) {
            Revision revision = DtoFactory.getInstance().createDto(Revision.class);
            revision.setMessage(e.getMessage());
            revision.setFake(true);
            return revision;
        }

    }

    @Override
    public DiffPage diff(DiffRequest request) throws GitException {
        return new NativeGitDiffPage(request, nativeGit);
    }

    @Override
    public void fetch(FetchRequest request) throws GitException, UnauthorizedException {
        FetchCommand fetchCommand;
        String remoteUri;
        try {
            remoteUri = nativeGit.createRemoteListCommand()
                                 .setRemoteName(request.getRemote())
                                 .execute()
                                 .get(0)
                                 .getUrl();
        } catch (GitException ignored) {
            remoteUri = request.getRemote();
        }
        if (Util.isSSH(remoteUri)) {
            fetchCommand = nativeGit.createFetchCommand(keysManager.writeKeyFile(remoteUri).getAbsolutePath());
        } else {
            fetchCommand = nativeGit.createFetchCommand();
        }
        fetchCommand.setRemote(request.getRemote())
                    .setPrune(request.isRemoveDeletedRefs())
                    .setRefSpec(request.getRefSpec())
                    .setTimeout(request.getTimeout());
        executeWithCredentials(fetchCommand, remoteUri);
    }

    @Override
    public void init(InitRequest request) throws GitException {
        InitCommand initCommand = nativeGit.createInitCommand();
        initCommand.setBare(request.isBare());
        initCommand.execute();
        //make initial commit.
        if (!request.isBare() && request.isInitCommit()) {
            try {
                nativeGit.createAddCommand()
                         .setFilePattern(new ArrayList<>(Arrays.asList(".")))
                         .execute();
                nativeGit.createCommitCommand()
                         .setCommitter(getUser())
                         .setMessage("init")
                         .execute();
            } catch (GitException ignored) {
                //if nothing to commit
            }
        }
    }

    @Override
    public LogPage log(LogRequest request) throws GitException {
        return new LogPage(nativeGit.createLogCommand().execute());
    }

    @Override
    public List<RemoteReference> lsRemote(LsRemoteRequest request) throws GitException, UnauthorizedException {
        LsRemoteCommand command = nativeGit.createLsRemoteCommand().setRemoteUrl(request.getRemoteUrl());
        if (request.isUseAuthorization()) {
            executeWithCredentials(command, request.getRemoteUrl());
        } else {
            try {
                command.setAskPassScriptPath(gitAskPassScript.build(UserCredential.EMPTY_CREDENTIALS).toString());
                if (!nativeGit.getRepository().exists()) {
                    nativeGit.getRepository().mkdirs();
                }

                command.execute();
            } finally {
                gitAskPassScript.remove();
            }
        }
        return command.getRemoteReferences();
    }

    @Override
    public MergeResult merge(MergeRequest request) throws GitException {
        if (getBranchRef(request.getCommit()) == null) {
            throw new GitException("Invalid reference to commit for merge " + request.getCommit());
        }
        return nativeGit.createMergeCommand().setCommit(request.getCommit()).setCommitter(getLocalCommitter()).execute();
    }

    @Override
    public void mv(MoveRequest request) throws GitException {
        nativeGit.createMoveCommand()
                 .setSource(request.getSource())
                 .setTarget(request.getTarget())
                 .execute();
    }

    @Override
    public void pull(PullRequest request) throws GitException, UnauthorizedException {
        PullCommand pullCommand;
        String remoteUri;
        try {
            remoteUri = nativeGit.createRemoteListCommand()
                                 .setRemoteName(request.getRemote())
                                 .execute()
                                 .get(0)
                                 .getUrl();
        } catch (GitException ignored) {
            remoteUri = request.getRemote();
        }
        if (Util.isSSH(remoteUri)) {
            pullCommand = nativeGit.createPullCommand(keysManager.writeKeyFile(remoteUri).getAbsolutePath());
        } else {
            pullCommand = nativeGit.createPullCommand();
        }
        pullCommand.setRemote(remoteUri);
        pullCommand.setRefSpec(request.getRefSpec())
                   .setAuthor(getLocalCommitter())
                   .setTimeout(request.getTimeout());

        executeWithCredentials(pullCommand, remoteUri);
        if (pullCommand.getText().toLowerCase().contains("already up-to-date")) {
            throw new AlreadyUpToDateException("Already up-to-date");
        }
    }

    @Override
    public void push(PushRequest request) throws GitException, UnauthorizedException {
        PushCommand pushCommand;
        String remoteUri;
        try {
            remoteUri = nativeGit.createRemoteListCommand()
                                 .setRemoteName(request.getRemote())
                                 .execute()
                                 .get(0)
                                 .getUrl();
        } catch (GitException ignored) {
            remoteUri = request.getRemote();
        }
        if (Util.isSSH(remoteUri)) {
            pushCommand = nativeGit.createPushCommand(keysManager.writeKeyFile(remoteUri).getAbsolutePath());
        } else {
            pushCommand = nativeGit.createPushCommand();
        }

        pushCommand.setRemote(request.getRemote())
                   .setForce(request.isForce())
                   .setRefSpec(request.getRefSpec())
                   .setTimeout(request.getTimeout());
        executeWithCredentials(pushCommand, remoteUri);
        if (pushCommand.getText().toLowerCase().contains("everything up-to-date")) {
            throw new AlreadyUpToDateException("Everything up-to-date");
        }
    }

    @Override
    public void remoteAdd(RemoteAddRequest request) throws GitException {
        nativeGit.createRemoteAddCommand()
                 .setName(request.getName())
                 .setUrl(request.getUrl())
                 .setBranches(request.getBranches())
                 .execute();
    }

    @Override
    public void remoteDelete(String name) throws GitException {
        nativeGit.createRemoteDeleteCommand().setName(name).execute();
    }

    @Override
    public List<Remote> remoteList(RemoteListRequest request) throws GitException {
        RemoteListCommand remoteListCommand = nativeGit.createRemoteListCommand();
        return remoteListCommand.setRemoteName(request.getRemote()).execute();
    }

    @Override
    public void remoteUpdate(RemoteUpdateRequest request) throws GitException {
        nativeGit.createRemoteUpdateCommand()
                 .setRemoteName(request.getName())
                 .setAddUrl(request.getAddUrl())
                 .setBranchesToAdd(request.getBranches())
                 .setAddBranches(request.isAddBranches())
                 .setAddPushUrl(request.getAddPushUrl())
                 .setRemovePushUrl(request.getRemovePushUrl())
                 .setRemoveUrl(request.getRemoveUrl())
                 .execute();
    }

    @Override
    public void reset(ResetRequest request) throws GitException {
        nativeGit.createResetCommand()
                 .setMode(request.getType().getValue())
                 .setCommit(request.getCommit())
                 .setFilePattern(request.getFilePattern())
                 .execute();
    }

    @Override
    public void rm(RmRequest request) throws GitException {
        nativeGit.createRemoveCommand()
                 .setCached(request.isCached())
                 .setListOfItems(request.getItems())
                 .setRecursively(request.isRecursively())
                 .execute();
    }

    @Override
    public Status status(boolean shortFormat) throws GitException {
        return new NativeGitStatusImpl(getCurrentBranch(), nativeGit, shortFormat);
    }

    @Override
    public Tag tagCreate(TagCreateRequest request) throws GitException {
        return nativeGit.createTagCreateCommand().setName(request.getName())
                        .setCommit(request.getCommit())
                        .setMessage(request.getMessage())
                        .setForce(request.isForce())
                        .execute();
    }

    @Override
    public void tagDelete(TagDeleteRequest request) throws GitException {
        nativeGit.createTagDeleteCommand().setName(request.getName()).execute();
    }

    @Override
    public List<Tag> tagList(TagListRequest request) throws GitException {
        return nativeGit.createTagListCommand().setPattern(request.getPattern()).execute();
    }

    @Override
    public GitUser getUser() {
        return user;
    }

    @Override
    public List<GitUser> getCommiters() throws GitException {
        List<GitUser> users = new LinkedList<>();
        List<Revision> revList = nativeGit.createLogCommand().execute();
        for (Revision rev : revList) {
            users.add(rev.getCommitter());
        }
        return users;
    }

    @Override
    public Config getConfig() throws GitException {
        return nativeGit.createConfig();
    }

    @Override
    public void close() {
        //do not need to do anything
    }

    /** @return NativeGit for this connection */
    public NativeGit getNativeGit() {
        return nativeGit;
    }

    /**
     * Gets current branch name.
     *
     * @return name of current branch or <code>null</code> if current branch not exists
     * @throws GitException
     *         if any error occurs
     */
    public String getCurrentBranch() throws GitException {
        BranchListCommand command = nativeGit.createBranchListCommand();
        command.execute();
        String branchName = null;
        for (String outLine : command.getLines()) {
            if (outLine.indexOf('*') != -1) {
                branchName = outLine.substring(2);
            }
        }
        return branchName;
    }

    /**
     * Executes git command with credentials.
     *
     * @param command
     *         command that will be executed
     * @param url
     *         given URL
     * @throws GitException
     *         when it is not possible to store credentials or
     *         authentication failed or command execution failed
     */
    public void executeWithCredentials(GitCommand command, String url) throws GitException, UnauthorizedException {
        try {
            // execute without any credentials
            command.execute();
        } catch (GitException e) {
            if (!isOperationNeedAuth(e.getMessage())) {
                throw e;
            }
            try {
                prepareCredentials(command, url);
                restoreGitRepoDir();
                command.execute();
            } catch (GitException inner) {
                //if not authorized again make runtime exception
                if (isOperationNeedAuth(inner.getMessage())) {
                    throw new UnauthorizedException("Not authorized");
                } else {
                    throw inner;
                }
            } finally {
                gitAskPassScript.remove();
            }
        } finally {
            if (Util.isSSH(url)) {
                keysManager.removeKey(url);
            }
        }
    }

    /** Prepares credentials for git command */
    private void prepareCredentials(GitCommand<?> command, String url) throws GitException {
        UserCredential credentials = credentialsLoader.getUserCredential(url);
        if (credentials == null) {
            credentials = UserCredential.EMPTY_CREDENTIALS;
        }
        command.setAskPassScriptPath(gitAskPassScript.build(credentials).toString());
    }

    /** Restores associated with {@link #nativeGit} repository directory */
    private void restoreGitRepoDir() throws GitException {
        final File repo = nativeGit.getRepository();
        if (!repo.exists() && !repo.mkdirs()) {
            LOG.error("Could not restore git repository directory " + repo);
        }
    }

    /** Check if error message from git output corresponding authenticate issue. */
    private boolean isOperationNeedAuth(String errorMessage) {
        return authErrorPattern.matcher(errorMessage).find();
    }

    /**
     * Gets branch ref by branch name.
     *
     * @param branchName
     *         existing git branch name
     * @return ref to the branch
     * @throws GitException
     *         when it is not possible to get branchName ref
     */
    private String getBranchRef(String branchName) throws GitException {
        EmptyGitCommand command = nativeGit.createEmptyGitCommand();
        command.setNextParameter("show-ref").setNextParameter(branchName).execute();
        final String output = command.getText();
        if (output.length() > 0) {
            return output.split(" ")[1];
        } else {
            return null;
        }
    }

    private String parseBranchName(String name) {
        int branchNameIndex = 0;
        if (name.startsWith("refs/remotes/")) {
            branchNameIndex = name.indexOf("/", "refs/remotes/".length()) + 1;
        } else if (name.startsWith("refs/heads/")) {
            branchNameIndex = name.indexOf("/", "refs/heads".length()) + 1;
        }
        return name.substring(branchNameIndex);
    }

    private String parseRemoteName(String branchRef) {
        int remoteStartIndex = "refs/remotes/".length();
        int remoteEndIndex = branchRef.indexOf("/", remoteStartIndex);
        return branchRef.substring(remoteStartIndex, remoteEndIndex);
    }

    private GitUser getLocalCommitter() {
        GitUser committer = user;
        try {
            String credentialsProvider = getConfig().get("codenvy.credentialsProvider");
            GitUser providerUser = credentialsLoader.getUser(credentialsProvider);
            if (providerUser != null) {
                committer = providerUser;
            }
        } catch (GitException e) {
            //ignore property not found.
        }
        return committer;
    }

    @Override
    public void setOutputLineConsumerFactory(LineConsumerFactory gitOutputPublisherFactory) {
        nativeGit.setOutputLineConsumerFactory(gitOutputPublisherFactory);
    }
}
