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
package com.codenvy.ide.ext.github.client.projectimporter;

import com.codenvy.api.project.gwt.client.ProjectServiceClient;
import com.codenvy.ide.ext.git.client.projectImporter.GitProjectImporter;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.google.inject.Inject;

import javax.inject.Singleton;

/**
 * Provide possibility for importing source from github.
 *
 * @author Roman Nikitenko
 */
@Singleton
public class GithubProjectImporter extends GitProjectImporter {

    @Inject
    public GithubProjectImporter(ProjectServiceClient projectService,
                                 DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        super(projectService, dtoUnmarshallerFactory);
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return "github";
    }

}
