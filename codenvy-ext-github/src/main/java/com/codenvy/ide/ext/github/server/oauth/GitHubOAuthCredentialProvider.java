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
package com.codenvy.ide.ext.github.server.oauth;

import com.codenvy.api.auth.oauth.OAuthTokenProvider;
import com.codenvy.api.auth.shared.dto.OAuthToken;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.json.JsonHelper;
import com.codenvy.commons.json.JsonParseException;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.ide.ext.git.server.GitException;
import com.codenvy.ide.ext.git.server.nativegit.UserCredential;
import com.codenvy.ide.ext.git.server.nativegit.CredentialsProvider;
import com.codenvy.ide.ext.git.shared.GitUser;
import com.codenvy.ide.security.oauth.server.OAuthAuthenticationException;
import com.codenvy.ide.security.oauth.shared.User;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Vladyslav Zhukovskii
 */
@Singleton
public class GitHubOAuthCredentialProvider implements CredentialsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubOAuthCredentialProvider.class);

    private static String OAUTH_PROVIDER_NAME = "github";
    private OAuthTokenProvider tokenProvider;

    @Inject
    public GitHubOAuthCredentialProvider(OAuthTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public UserCredential getUserCredential() throws GitException {
        //        OAuthToken token;
        try {
            OAuthToken token = tokenProvider.getToken(OAUTH_PROVIDER_NAME, EnvironmentContext.getCurrent().getUser().getId());
            if (token != null) {
                return new UserCredential(token.getToken(), token.getToken(), OAUTH_PROVIDER_NAME);
            }
        } catch (IOException e) {
            LOG.warn(e.getLocalizedMessage());
        }
        return null;
    }

    @Override
    public GitUser getUser() throws GitException {
        OAuthToken token;
        try {
            token = tokenProvider.getToken(OAUTH_PROVIDER_NAME, EnvironmentContext.getCurrent().getUser().getId());
            if (token != null) {
                User user = getUser(token);
                return DtoFactory.getInstance().createDto(GitUser.class)
                                 .withEmail(user.getEmail())
                                 .withName(user.getName());
            }

        } catch (IOException e) {
        } catch (OAuthAuthenticationException e) {
            throw new GitException(e);
        }
        return null;
    }

    @Override
    public String getId() {
        return OAUTH_PROVIDER_NAME;
    }

    @Override
    public boolean canProvideCredentials(String url) {
        return url.contains("github.com");
    }

    private User getUser(OAuthToken accessToken) throws OAuthAuthenticationException {
        GitHubUser user = getJson("https://api.github.com/user?access_token=" + accessToken.getToken(), GitHubUser.class);

        GithubEmail[] result =
                getJson2("https://api.github.com/user/emails?access_token=" + accessToken.getToken(), GithubEmail[].class, null);

        GithubEmail verifiedEmail = null;
        for (GithubEmail email : result) {
            if (email.isPrimary() && email.isVerified()) {
                verifiedEmail = email;
                break;
            }
        }
        if (verifiedEmail == null || verifiedEmail.getEmail() == null || verifiedEmail.getEmail().isEmpty()) {
            throw new OAuthAuthenticationException(
                    "Sorry, we failed to find any verified emails associated with your GitHub account." +
                    " Please, verify at least one email in your GitHub account and try to connect with GitHub again.");

        }
        user.setEmail(verifiedEmail.getEmail());
        final String email = user.getEmail();
        try {
            new InternetAddress(email).validate();
        } catch (AddressException e) {
            throw new OAuthAuthenticationException(e.getMessage());
        }
        return user;
    }

    private <O> O getJson(String getUserUrl, Class<O> userClass) throws OAuthAuthenticationException {
        HttpURLConnection urlConnection = null;
        InputStream urlInputStream = null;

        try {
            urlConnection = (HttpURLConnection)new URL(getUserUrl).openConnection();
            urlInputStream = urlConnection.getInputStream();
            return JsonHelper.fromJson(urlInputStream, userClass, null);
        } catch (JsonParseException | IOException e) {
            throw new OAuthAuthenticationException(e.getMessage(), e);
        } finally {
            if (urlInputStream != null) {
                try {
                    urlInputStream.close();
                } catch (IOException ignored) {
                }
            }

            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    protected <O> O getJson2(String getUserUrl, Class<O> userClass, Type type) throws OAuthAuthenticationException {
        HttpURLConnection urlConnection = null;
        InputStream urlInputStream = null;

        try {
            urlConnection = (HttpURLConnection)new URL(getUserUrl).openConnection();
            urlConnection.setRequestProperty("Accept", "application/vnd.github.v3.html+json");
            urlInputStream = urlConnection.getInputStream();
            return JsonHelper.fromJson(urlInputStream, userClass, type);
        } catch (JsonParseException | IOException e) {
            throw new OAuthAuthenticationException(e.getMessage(), e);
        } finally {
            if (urlInputStream != null) {
                try {
                    urlInputStream.close();
                } catch (IOException ignored) {
                }
            }

            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    public static class GithubEmail {
        private boolean primary;
        private boolean verified;
        private String  email;

        public boolean isPrimary() {
            return primary;
        }

        public void setPrimary(boolean primary) {
            this.primary = primary;
        }

        public boolean isVerified() {
            return verified;
        }

        public void setVerified(boolean verified) {
            this.verified = verified;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
