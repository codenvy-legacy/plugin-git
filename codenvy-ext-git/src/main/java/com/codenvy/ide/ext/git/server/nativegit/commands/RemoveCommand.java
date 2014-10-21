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

import java.io.File;

/**
 * Remove files
 *
 * @author Eugene Voevodin
 */
public class RemoveCommand extends GitCommand<Void> {

    private String  item;
    private boolean cached;
    private boolean recursively;

    public RemoveCommand(File repository) {
        super(repository);
    }

    /** @see com.codenvy.ide.ext.git.server.nativegit.commands.GitCommand#execute() */
    @Override
    public Void execute() throws GitException {
        if (item == null) {
            throw new GitException("Nothing to remove.");
        }
        reset();
        commandLine.add("rm");
        commandLine.add(item);

        if (cached) {
            commandLine.add("--cached");
        }

        if (recursively) {
            commandLine.add("-r");
        }

        start();
        return null;
    }

    /**
     * @param item
     *         item to remove
     * @return RemoveCommand with established item
     */
    public RemoveCommand setItem(String item) {
        this.item = item;
        return this;
    }

    public RemoveCommand setCached(boolean cached) {
        this.cached = cached;
        return this;
    }

    public RemoveCommand setRecursively(boolean isRecursively) {
        this.recursively = isRecursively;
        return this;
    }
}
