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
package com.codenvy.ide.ext.git.server.nativegit.commands;

import com.codenvy.ide.ext.git.server.GitException;
import com.codenvy.ide.ext.git.shared.GitUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Commit changes
 *
 * @author Eugene Voevodin
 * @author Artem Zatsarynnyy
 */
public class CommitCommand extends GitCommand<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(CommitCommand.class);

    private String  message;
    private GitUser author;
    private boolean amend;
    private boolean all;

    public CommitCommand(File place) {
        super(place);
    }

    /** @see com.codenvy.ide.ext.git.server.nativegit.commands.GitCommand#execute() */
    @Override
    public Void execute() throws GitException {
        if (message == null) {
            throw new GitException("Message wasn't set.");
        }
        reset();
        commandLine.add("commit");
        if (amend) {
            commandLine.add("--amend");
        }
        if (all) {
            commandLine.add("-a");
        }
        Path commitMsgFile = null;
        if (!message.contains("\n")) {
            commandLine.add("-m", message);
        } else {
            try {
                commitMsgFile = Files.createTempFile("git-commit-message-", null);
                Files.write(commitMsgFile, message.getBytes());
                commandLine.add("-F", commitMsgFile.toString());
            } catch (IOException e) {
                // allow to commit but message will be in 'one-line' format
                commandLine.add("-m", message);
            }
        }
        if (author != null) {
            commandLine.add(String.format("--author=%s \\<%s>", author.getName(), author.getEmail()));
        }
        start();
        if (commitMsgFile != null) {
            try {
                Files.deleteIfExists(commitMsgFile);
            } catch (IOException e) {
                LOG.error("Can not delete temporary file with commit message", e);
            }
        }
        return null;
    }

    /**
     * @param message
     *         commit message
     * @return CommitCommand object
     */
    public CommitCommand setMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * @param all
     *         if <code>true</code> all files will be added to index
     * @return CommitCommand with established all parameter
     */
    public CommitCommand setAll(boolean all) {
        this.all = all;
        return this;
    }

    /**
     * @param amend
     *         change previous commit
     * @return CommitCommand established amend parameter
     */
    public CommitCommand setAmend(boolean amend) {
        this.amend = amend;
        return this;
    }

    /**
     * @param author
     *         author of commit
     * @return CommitCommand with established author
     */
    public CommitCommand setAuthor(GitUser author) {
        this.author = author;
        return this;
    }
}
