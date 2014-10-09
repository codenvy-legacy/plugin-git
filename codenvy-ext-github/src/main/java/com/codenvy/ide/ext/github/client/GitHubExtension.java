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
package com.codenvy.ide.ext.github.client;

import com.codenvy.ide.api.action.ActionManager;
import com.codenvy.ide.api.action.DefaultActionGroup;
import com.codenvy.ide.api.action.IdeActions;
import com.codenvy.ide.api.extension.Extension;
import com.codenvy.ide.ext.ssh.client.SshKeyService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Extension adds GitHub support to the IDE Application.
 *
 * @author Andrey Plotnikov
 */
@Singleton
@Extension(title = "GitHub", version = "3.0.0")
public class GitHubExtension {

    public static final String GITHUB_HOST = "github.com";

    @Inject
    public GitHubExtension(ActionManager actionManager,
                           SshKeyService sshKeyService,
                           GitHubSshKeyProvider gitHubSshKeyProvider) {
        sshKeyService.registerSshKeyProvider(GITHUB_HOST, gitHubSshKeyProvider);

        DefaultActionGroup importProjectGroup = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_IMPORT_PROJECT);
    }
}