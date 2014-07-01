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

import com.codenvy.api.auth.oauth.OAuthTokenProvider;
import com.codenvy.api.auth.shared.dto.OAuthToken;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.ide.ext.git.server.GitException;
import com.codenvy.ide.ext.git.server.nativegit.CredentialItem;
import com.codenvy.ide.ext.git.server.nativegit.CredentialsProvider;
import com.codenvy.ide.security.oauth.server.OAuthAuthenticationException;
import com.codenvy.ide.security.oauth.shared.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 *  Credentials provider for ProjectLocker
 *
 *  @author Max Shaposhnik
 */
public class ProjectLockerOAuthCredentialProvider implements CredentialsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectLockerOAuthCredentialProvider.class);

    private static String OAUTH_PROVIDER_NAME = "projectlocker";
    public final Pattern PROJECTLOCKER_2_URL_PATTERN;

    private final OAuthTokenProvider tokenProvider;
    private final String             userUri;

    @Inject
    public ProjectLockerOAuthCredentialProvider(OAuthTokenProvider tokenProvider,
                                                @Named("oauth.projectlocker.useruri") String userUri,
                                                @Named("oauth.projectlocker.git.pattern") String gitPattern) {
        this.tokenProvider = tokenProvider;
        this.userUri = userUri;
        this.PROJECTLOCKER_2_URL_PATTERN = Pattern.compile(gitPattern);
    }

    @Override
    public boolean get(String url, CredentialItem... items) throws GitException {
        if (!PROJECTLOCKER_2_URL_PATTERN.matcher(url).matches()) {
            return false;
        }
        OAuthToken token;
        try {
            token = tokenProvider.getToken(OAUTH_PROVIDER_NAME, EnvironmentContext.getCurrent().getUser().getId());
        } catch (IOException e) {
            LOG.error("Can't get token", e);
            return false;
        }
        if (token != null) {
            for (CredentialItem item : items) {
                if (item instanceof CredentialItem.Password) {
                    ((CredentialItem.Password)item).setValue("x");
                    continue;
                }
                if (item instanceof CredentialItem.Username) {
                    ((CredentialItem.Username)item).setValue(token.getToken());
                }
            }
        } else {
            LOG.error("Token is null");
            return false;
        }
        return true;
    }

    @Override
    public boolean getUser(String url, CredentialItem... items) throws GitException {
        if (!PROJECTLOCKER_2_URL_PATTERN.matcher(url).matches()) {
            return false;
        }

        OAuthToken token;
        try {
            token = tokenProvider.getToken(OAUTH_PROVIDER_NAME, EnvironmentContext.getCurrent().getUser().getId());
        } catch (IOException e) {
            return false;
        }

        if (token == null) {
            return false;
        }

        User user;
        try {
            user = getUser(token);
        } catch (OAuthAuthenticationException e) {
            throw new GitException(e);
        }

        if (user != null) {
            for (CredentialItem item : items) {
                if (item instanceof CredentialItem.AuthenticatedUserName) {
                    ((CredentialItem.AuthenticatedUserName)item).setValue(user.getName());
                    continue;
                }
                if (item instanceof CredentialItem.AuthenticatedUserEmail) {
                    ((CredentialItem.AuthenticatedUserEmail)item).setValue(user.getEmail());
                }
            }
        } else {
            return false;
        }

        return true;
    }

    private User getUser(OAuthToken accessToken) throws OAuthAuthenticationException {
        // Not implemented
        return null;
    }
}
