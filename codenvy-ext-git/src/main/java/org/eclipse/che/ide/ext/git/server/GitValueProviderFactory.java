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
package org.eclipse.che.ide.ext.git.server;

import com.google.inject.Inject;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.project.server.FolderEntry;
import org.eclipse.che.api.project.server.InvalidValueException;
import org.eclipse.che.api.project.server.ValueProvider;
import org.eclipse.che.api.project.server.ValueProviderFactory;
import org.eclipse.che.api.project.server.ValueStorageException;
import org.eclipse.che.api.vfs.server.MountPoint;
import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.api.vfs.server.VirtualFileSystem;
import org.eclipse.che.api.vfs.server.VirtualFileSystemRegistry;
import org.eclipse.che.api.vfs.shared.PropertyFilter;
import org.eclipse.che.api.vfs.shared.dto.Item;
import org.eclipse.che.ide.ext.git.shared.Status;
import org.eclipse.che.vfs.impl.fs.LocalPathResolver;
import org.eclipse.che.vfs.impl.fs.VirtualFileImpl;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

import static org.eclipse.che.ide.ext.git.shared.StatusFormat.LONG;

/**
 * @author Roman Nikitenko
 */
@Singleton
public class GitValueProviderFactory implements ValueProviderFactory {

    @Inject
    private GitConnectionFactory      gitConnectionFactory;
    @Inject
    private VirtualFileSystemRegistry vfsRegistry;
    @Inject
    private LocalPathResolver         localPathResolver;

    @Override
    public ValueProvider newInstance(final FolderEntry folder) {
        return new ValueProvider() {
            @Override
            public List<String> getValues(String attributeName) throws ValueStorageException {
                try {
                    Status status = getStatus(folder.getPath(), folder.getWorkspace());
                    String branch = status.getBranchName();
                    if (branch == null || branch.isEmpty()) {
                        throw new ValueStorageException(String.format("Not a git repository (or current branch not found in %s", folder.getPath()));
                    } else {
                        return Arrays.asList("git");
                    }
                } catch (ApiException e) {
                    throw new ValueStorageException(e.getMessage());
                }
            }

            @Override
            public void setValues(String attributeName, List<String> value) throws InvalidValueException {
                throw new InvalidValueException(
                        String.format("It is not possible to set value for attribute %s on project %s .git project values are read only",
                                      attributeName, folder.getPath()));
            }
        };
    }

    private Status getStatus(String projectPath, String wsId) throws ApiException {
        GitConnection gitConnection = gitConnectionFactory.getConnection(resolveLocalPathByPath(projectPath, wsId));
        try {
            return gitConnection.status(LONG);
        } finally {
            gitConnection.close();
        }
    }

    private String resolveLocalPathByPath(String folderPath, String wsId) throws ApiException {
        VirtualFileSystem vfs = vfsRegistry.getProvider(wsId).newInstance(null);
        Item gitProject = vfs.getItemByPath(folderPath, null, false, PropertyFilter.ALL_FILTER);
        final MountPoint mountPoint = vfs.getMountPoint();
        final VirtualFile virtualFile = mountPoint.getVirtualFile(gitProject.getPath());
        return localPathResolver.resolve((VirtualFileImpl)virtualFile);
    }
}
