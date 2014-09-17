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

import com.codenvy.api.core.ApiException;
import com.codenvy.api.core.rest.HttpJsonHelper;
import com.codenvy.api.core.rest.shared.dto.Link;
import com.codenvy.api.user.server.dao.Profile;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.ide.ext.git.server.GitException;
import com.codenvy.ide.ext.git.server.nativegit.CredentialItem;
import com.codenvy.ide.ext.git.server.nativegit.CredentialsProvider;
import com.codenvy.ide.security.oauth.shared.User;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Credentials provider for ProjectLocker
 *
 * @author Alexander Garagatyi
 */
public class CodenvyAccessTokenCredentialProvider implements CredentialsProvider {
    private final String codenvyHost;
    private final String apiEndpoint;

    @Inject
    public CodenvyAccessTokenCredentialProvider(@Named("api.endpoint") String apiEndPoint) throws URISyntaxException {
        this.apiEndpoint = apiEndPoint;
        this.codenvyHost = new URI(apiEndPoint).getHost();
    }

    @Override
    public boolean get(String url, CredentialItem... items) throws GitException {
        if (!url.contains(codenvyHost)) {
            return false;
        }
        String token = EnvironmentContext.getCurrent().getUser().getToken();
        if (token != null) {
            for (CredentialItem item : items) {
                if (item instanceof CredentialItem.Password) {
                    ((CredentialItem.Password)item).setValue("x-codenvy");
                    continue;
                }
                if (item instanceof CredentialItem.Username) {
                    ((CredentialItem.Username)item).setValue(token);
                }
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean getUser(String url, CredentialItem... items) throws GitException {
        if (!url.contains(codenvyHost)) {
            return false;
        }

        String token = EnvironmentContext.getCurrent().getUser().getToken();
        if (token == null) {
            return false;
        }

        User user;
        try {
            user = getUser();
        } catch (Exception e) {
            throw new GitException(e);
        }

        if (user != null) {
            for (CredentialItem item : items) {
                if (item instanceof CredentialItem.AuthenticatedUserName) {
                    ((CredentialItem.AuthenticatedUserName)item).setValue(user.getName());
                } else if (item instanceof CredentialItem.AuthenticatedUserEmail) {
                    ((CredentialItem.AuthenticatedUserEmail)item).setValue(user.getEmail());
                }
            }
        } else {
            return false;
        }

        return true;
    }

    private User getUser() throws ApiException, IOException {
        Link link = DtoFactory.getInstance().createDto(Link.class).withMethod("GET")
                              .withHref(UriBuilder.fromUri(apiEndpoint).path("profile").build().toString());
        final Profile profile = HttpJsonHelper.request(Profile.class, link);
        return new User() {
            @Override
            public String getId() {
                return null;
            }

            @Override
            public void setId(String s) {
            }

            @Override
            public String getName() {
                String firstName = profile.getAttributes().get("firstName");
                String lastName = profile.getAttributes().get("lastName");
                if (null != lastName) {
                    return null != firstName ? firstName + " " + lastName : lastName;
                }
                return null;
            }

            @Override
            public void setName(String s) {
            }

            @Override
            public String getEmail() {
                String email = profile.getAttributes().get("email");
                return null != email ? email : null;
            }

            @Override
            public void setEmail(String s) {
            }
        };
    }
}
