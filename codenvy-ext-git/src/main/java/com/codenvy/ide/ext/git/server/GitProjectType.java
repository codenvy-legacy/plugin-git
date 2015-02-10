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
package com.codenvy.ide.ext.git.server;

import com.codenvy.api.project.server.type.ProjectType;

/**
 * @author Vitaly Parfonov
 */
public class GitProjectType extends ProjectType {

    public GitProjectType(IsGitRepositoryValueProviderFactory isGitRepositoryValueProviderFactory) {
        super("git", "git", false, true);
        addVariableDefinition(IsGitRepositoryValueProviderFactory.NAME, "Is this git repo or not?", false,
                              isGitRepositoryValueProviderFactory);
    }
}
