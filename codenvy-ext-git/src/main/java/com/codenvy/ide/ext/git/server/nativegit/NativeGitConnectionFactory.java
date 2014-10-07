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

import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.util.LineConsumerFactory;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.lang.Strings;
import com.codenvy.commons.user.User;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.ide.ext.git.server.GitConnection;
import com.codenvy.ide.ext.git.server.GitConnectionFactory;
import com.codenvy.ide.ext.git.server.GitException;
import com.codenvy.ide.ext.git.shared.GitUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.Map;

/**
 * Native implementation for GitConnectionFactory
 *
 * @author Eugene Voevodin
 */
@Singleton
public class NativeGitConnectionFactory extends GitConnectionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(NativeGitConnectionFactory.class);

    private final SshKeysManager    keysManager;
    private final CredentialsLoader credentialsLoader;
    private final UserProfileDao    userProfileDao;

    @Inject
    public NativeGitConnectionFactory(SshKeysManager keysManager, CredentialsLoader credentialsLoader, UserProfileDao userProfileDao) {
        this.keysManager = keysManager;
        this.credentialsLoader = credentialsLoader;
        this.userProfileDao = userProfileDao;
    }

    @Override
    public GitConnection getConnection(File workDir, GitUser user, LineConsumerFactory outputPublisherFactory) throws GitException {
        final GitConnection gitConnection = new NativeGitConnection(workDir, user, keysManager, credentialsLoader);
        gitConnection.setOutputLineConsumerFactory(outputPublisherFactory);
        return gitConnection;
    }

    @Override
    public GitConnection getConnection(File workDir, LineConsumerFactory outputPublisherFactory) throws GitException {
        return getConnection(workDir, getGitUser(), outputPublisherFactory);
    }

    private GitUser getGitUser() {
        final User user = EnvironmentContext.getCurrent().getUser();
        Map<String, String> profileAttributes = null;
        try {
            profileAttributes = userProfileDao.getById(user.getId()).getAttributes();
        } catch (NotFoundException | ServerException e) {
            LOG.warn("Failed to obtain user information.", e);
        }
        final GitUser gitUser = DtoFactory.getInstance().createDto(GitUser.class);
        if (profileAttributes == null) {
            return gitUser.withName(user.getName());
        }
        final String firstName = profileAttributes.get("firstName");
        final String lastName = profileAttributes.get("lastName");
        final String email = profileAttributes.get("email");
        if (firstName != null || lastName != null) {
            gitUser.withName(Strings.join(" ", Strings.nullToEmpty(firstName), Strings.nullToEmpty(lastName)));
        } else {
            gitUser.withName(user.getName());
        }
        gitUser.withEmail(email != null ? email : user.getName());

        return gitUser;
    }
}
