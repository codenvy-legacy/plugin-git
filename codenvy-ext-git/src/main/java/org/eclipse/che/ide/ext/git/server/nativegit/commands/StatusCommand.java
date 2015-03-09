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
package org.eclipse.che.ide.ext.git.server.nativegit.commands;

import org.eclipse.che.ide.ext.git.server.GitException;

import java.io.File;
import java.util.List;

/**
 * Show repository status
 *
 * @author Eugene Voevodin
 */
public class StatusCommand extends GitCommand<List<String>> {

    private boolean isShort;

    public StatusCommand(File repository) {
        super(repository);
    }

    /** @see GitCommand#execute() */
    @Override
    public List<String> execute() throws GitException {
        reset();
        commandLine.add("status");
        if (isShort) {
            commandLine.add("--short");
        }
        start();
        return getLines();
    }

    /**
     * @param aShort
     *         short status format
     * @return StatusCommand withe established short parameter
     */
    public StatusCommand setShort(boolean aShort) {
        isShort = aShort;
        return this;
    }
}
