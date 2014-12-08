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

import com.codenvy.api.core.ConflictException;
import com.codenvy.api.core.ForbiddenException;
import com.codenvy.api.core.NotFoundException;
import com.codenvy.api.core.ServerException;
import com.codenvy.api.core.UnauthorizedException;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.user.server.dao.Profile;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.ide.ext.git.server.GitException;
import com.codenvy.ide.ext.git.server.nativegit.CredentialsProvider;
import com.codenvy.ide.ext.git.server.nativegit.UserCredential;
import com.codenvy.ide.ext.git.shared.GitUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Credentials provider for Codenvy
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class CodenvyAccessTokenCredentialProvider implements CredentialsProvider {
    private static final Logger LOG = LoggerFactory.getLogger(CodenvyAccessTokenCredentialProvider.class);

    private final String codenvyHost;
    private final String apiEndpoint;

    @Inject
    public CodenvyAccessTokenCredentialProvider(@Named("api.endpoint") String apiEndPoint) throws URISyntaxException {
        this.apiEndpoint = apiEndPoint;
        this.codenvyHost = new URI(apiEndPoint).getHost();
    }

    @Override
    public UserCredential getUserCredential() throws GitException {
        String token = EnvironmentContext.getCurrent().getUser().getToken();
        if (token != null) {
            return new UserCredential(token, "x-codenvy", "codenvy");
        }
        return null;
    }

    @Override
    public GitUser getUser() throws GitException {
        try {
            Link link = DtoFactory.getInstance().createDto(Link.class).withMethod("GET")
                                  .withHref(UriBuilder.fromUri(apiEndpoint).path("profile").build().toString());
            final Profile profile = HttpJsonHelper.request(Profile.class, link);


            String name = profile.getAttributes().get("firstName");
            String lastName = profile.getAttributes().get("lastName");

            if (null != lastName) {
                name = null != name ? name + " " + lastName : lastName;
            }
            return DtoFactory.getInstance().createDto(GitUser.class)
                             .withEmail(profile.getAttributes().get("email"))
                             .withName(name);

        } catch (IOException | ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            LOG.warn(e.getLocalizedMessage());
            // throw new GitException(e);
        }
        return null;
    }

    @Override
    public String getId() {
        return "codenvy";
    }

    @Override
    public boolean canProvideCredentials(String url) {
        return url.contains(codenvyHost);
    }

}

