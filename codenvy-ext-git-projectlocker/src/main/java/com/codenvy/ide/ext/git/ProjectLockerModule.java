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
package com.codenvy.ide.ext.git;

import com.codenvy.api.project.server.ProjectImporter;
import com.codenvy.ide.ext.git.server.GitConfigurationChecker;
import com.codenvy.ide.ext.git.server.GitProjectImporter;
import com.codenvy.ide.ext.git.server.commons.GitRepositoryPrivacyChecker;
import com.codenvy.ide.ext.git.server.nativegit.CredentialsProvider;
import com.codenvy.ide.ext.git.server.nativegit.WSO2OAuthCredentialsProvider;
import com.codenvy.ide.ext.git.server.rest.GitExceptionMapper;
import com.codenvy.inject.DynaModule;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * The module that contains configuration of the server side part of the Git extension.
 *
 * @author andrew00x
 */
@DynaModule
public class ProjectLockerModule extends AbstractModule {

    /** {@inheritDoc} */
    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), CredentialsProvider.class).addBinding().to(ProjectLockerOAuthCredentialProvider.class);
    }
}