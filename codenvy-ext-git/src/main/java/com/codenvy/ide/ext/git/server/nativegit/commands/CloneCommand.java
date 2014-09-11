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

import java.io.File;

import com.codenvy.ide.ext.git.server.GitException;

/**
 * This command used for cloning repositories.
 *
 * @author Eugene Voevodin
 */
public class CloneCommand extends GitCommand<Void> {

    private String uri;
    private String remoteName;

    public CloneCommand(File place) {
        super(place);
    }

    /** @see com.codenvy.ide.ext.git.server.nativegit.commands.GitCommand#execute() */
    @Override
    public Void execute() throws GitException {
        reset();
        commandLine.add("clone");
        if (remoteName != null) {
            commandLine.add("--origin", remoteName);
        } //else default origin name
        commandLine.add(uri, getRepository().getAbsolutePath());
        // Progress not shown if not a terminal. Activating progress output. See git clone man page.
        commandLine.add("--progress");
        start();
        return null;
    }

    /**
     * @param uri
     *         link to repository that will be cloned
     * @return CloneCommand with established uri
     */
    public CloneCommand setUri(String uri) {
        this.uri = uri;
        return this;
    }

    /**
     * @param remoteName
     *         name of remote, if it is null than default "origin" name will be used
     * @return CloneCommand with established remoteName
     */
    public CloneCommand setRemoteName(String remoteName) {
        this.remoteName = remoteName;
        return this;
    }
}
