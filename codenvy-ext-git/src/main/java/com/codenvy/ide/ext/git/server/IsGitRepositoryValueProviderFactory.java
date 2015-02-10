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
import com.codenvy.api.project.server.Project;
import com.codenvy.api.project.server.ValueProviderFactory;
import com.codenvy.api.project.server.ValueProvider;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Roman Nikitenko
 */
public class IsGitRepositoryValueProviderFactory implements ValueProviderFactory {
    @Override
    public ValueProvider newInstance(final FolderEntry project) {
        return new ValueProvider() {
            @Override
            public List<String> getValues(String attributeName) {
                final List<String> list = new LinkedList<>();
                try {
                    final FolderEntry git = (FolderEntry)project.getChild(".git");
                    if (git != null) {
                        list.add("git");
                    }
                } catch (ForbiddenException | ServerException ignored) {
                }
                return list;
            }

            @Override
            public void setValues(String attributeName, List<String> value) {
                //noting todo
            }
        };
    }
}
