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

import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.project.server.FolderEntry;
import com.codenvy.api.project.server.InvalidValueException;
import com.codenvy.api.project.server.ValueProvider;
import com.codenvy.api.project.server.ValueProviderFactory;
import com.codenvy.api.project.server.ValueStorageException;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

/**
 * @author Roman Nikitenko
 */
@Singleton
public class GitValueProviderFactory implements ValueProviderFactory {


    @Override
    public ValueProvider newInstance(final FolderEntry project) {
        return new ValueProvider() {
            @Override
            public List<String> getValues(String attributeName) throws ValueStorageException {
                try {
                    final FolderEntry git = (FolderEntry)project.getChild(".git");
                    if (git != null) {
                        return Arrays.asList("git");
                    } else {
                        throw new ValueStorageException(String.format("Folder .git not found in %s", project.getPath()));
                    }
                } catch (ForbiddenException | ServerException e) {
                    throw new ValueStorageException(e.getMessage());
                }
            }

            @Override
            public void setValues(String attributeName, List<String> value) throws InvalidValueException {
                throw new InvalidValueException(
                        String.format("It is not possible to set value for attribute %s on project %s .git project values are read only",
                                      attributeName, project.getPath()));
            }
        };
    }
}
