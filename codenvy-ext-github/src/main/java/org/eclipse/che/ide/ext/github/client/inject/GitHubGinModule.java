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
package org.eclipse.che.ide.ext.github.client.inject;

import org.eclipse.che.ide.api.extension.ExtensionGinModule;
import org.eclipse.che.ide.api.project.wizard.ImportWizardRegistrar;
import org.eclipse.che.ide.ext.github.client.GitHubClientService;
import org.eclipse.che.ide.ext.github.client.authenticator.GitHubAuthenticator;
import org.eclipse.che.ide.ext.github.client.authenticator.GitHubAuthenticatorImpl;
import org.eclipse.che.ide.ext.github.client.importer.GitHubImportWizardRegistrar;
import org.eclipse.che.ide.ext.github.client.GitHubClientServiceImpl;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.multibindings.GinMultibinder;
import com.google.inject.Singleton;

/** @author Andrey Plotnikov */
@ExtensionGinModule
public class GitHubGinModule extends AbstractGinModule {
    /** {@inheritDoc} */
    @Override
    protected void configure() {
        bind(GitHubClientService.class).to(GitHubClientServiceImpl.class).in(Singleton.class);
        bind(GitHubAuthenticator.class).to(GitHubAuthenticatorImpl.class).in(Singleton.class);

        GinMultibinder.newSetBinder(binder(), ImportWizardRegistrar.class).addBinding().to(GitHubImportWizardRegistrar.class);
    }
}