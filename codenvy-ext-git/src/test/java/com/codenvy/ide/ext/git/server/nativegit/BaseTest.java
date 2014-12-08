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
package com.codenvy.ide.ext.git.server.nativegit;

import com.codenvy.dto.server.DtoFactory;
import com.codenvy.ide.ext.git.server.GitConnection;
import com.codenvy.ide.ext.git.server.GitConnectionFactory;
import com.codenvy.ide.ext.git.server.GitException;
import com.codenvy.ide.ext.git.server.nativegit.commands.EmptyGitCommand;
import com.codenvy.ide.ext.git.shared.GitUser;


import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.codenvy.api.core.util.LineConsumerFactory.NULL;
import static com.codenvy.commons.lang.IoUtil.deleteRecursive;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.write;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Eugene Voevodin
 */
public abstract class BaseTest {
    protected final String CONTENT     = "git repository content\n";
    protected final String DEFAULT_URI = "user@host.com:login/repo";

    private GitConnection connection;
    private Path          target;
    private final DtoFactory dto = DtoFactory.getInstance();

    @BeforeMethod
    public void initRepository() throws Exception {
        final File repository = getTarget().resolve("repository").toFile();
        if (!repository.exists()) {
            assertTrue(repository.mkdir());
        }
        init(repository);
        //setup connection
        final GitConnectionFactory factory = new NativeGitConnectionFactory(null, null, null);
        connection = factory.getConnection(repository,
                                           newDTO(GitUser.class).withName("test_name")
                                                                .withEmail("test@email"),
                                           NULL);
    }

    @AfterMethod
    public void removeRepository() throws IOException {
        deleteRecursive(connection.getWorkingDir());
    }

    protected Path getTarget() throws URISyntaxException {
        if (target == null) {
            final URL targetParent = Thread.currentThread().getContextClassLoader().getResource(".");
            assertNotNull(targetParent);
            target = Paths.get(targetParent.toURI()).getParent();
        }
        return target;
    }

    protected Path getRepository() {
        return getConnection().getWorkingDir().toPath();
    }

    protected void addFile(String name, String content) throws IOException {
        addFile(getRepository(), name, content);
    }

    protected void deleteFile(String name) throws IOException {
        delete(getRepository().resolve(name));
    }

    protected void addFile(Path parent, String name, String content) throws IOException {
        if (!exists(parent)) {
            createDirectories(parent);
        }
        write(parent.resolve(name), content.getBytes());
    }

    protected GitConnection getConnection() {
        return connection;
    }

    protected <T> T newDTO(Class<T> dtoInterface) {
        return DtoFactory.getInstance().createDto(dtoInterface);
    }

    private void init(File repository) throws GitException {
        final NativeGit git = new NativeGit(repository);
        git.createInitCommand().execute();
    }

    protected int getCountOfCommitsInCurrentBranch(File repo) throws GitException {
        EmptyGitCommand emptyGitCommand = new EmptyGitCommand(repo);
        emptyGitCommand.setNextParameter("rev-list")
                       .setNextParameter("HEAD")
                       .setNextParameter("--count")
                       .execute();
        return Integer.parseInt(emptyGitCommand.getText());
    }

}
