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

import com.codenvy.api.auth.oauth.OAuthTokenProvider;
import com.codenvy.api.auth.shared.dto.OAuthToken;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.commons.json.JsonHelper;
import com.codenvy.commons.json.JsonParseException;
import com.codenvy.commons.lang.IoUtil;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.ide.ext.git.server.GitException;
import com.codenvy.ide.ext.git.shared.GitUser;

import org.everrest.core.impl.provider.json.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Used to store credentials when given url is WSO2.
 *
 * @author Eugene Voevodin
 */
@Singleton
public class WSO2OAuthCredentialsProvider implements CredentialsProvider {
    private static String OAUTH_PROVIDER_NAME = "wso2";
    public final Pattern WSO_2_URL_PATTERN;

    private static final Logger LOG   = LoggerFactory.getLogger(WSO2OAuthCredentialsProvider.class);
    private static final String SCOPE = "openid";
    private final OAuthTokenProvider tokenProvider;
    private final String             userUri;

    @Inject
    public WSO2OAuthCredentialsProvider(OAuthTokenProvider tokenProvider, @Named("oauth.wso2.useruri") String userUri,
                                        @Named("oauth.wso2.git.pattern") String gitPattern) {
        this.tokenProvider = tokenProvider;
        this.userUri = userUri;
        this.WSO_2_URL_PATTERN = Pattern.compile(gitPattern);
    }


    @Override
    public UserCredential getUserCredential() throws GitException {
        OAuthToken token;
        try {
            token = tokenProvider.getToken(OAUTH_PROVIDER_NAME, EnvironmentContext.getCurrent().getUser().getId());
            if (token != null) {
                return new UserCredential(token.getToken(), "x-oauth-basic", OAUTH_PROVIDER_NAME);
            } else {
                LOG.error("Token is null");

            }
        } catch (IOException e) {
            LOG.error("Can't get token", e);
            return null;
        }
        return null;
    }

    @Override
    public GitUser getUser() throws GitException {
        OAuthToken token;
        try {
            token = tokenProvider.getToken(OAUTH_PROVIDER_NAME, EnvironmentContext.getCurrent().getUser().getId());
            if (token != null) {

                URL getUserUrL;
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + token.getToken());
                getUserUrL = new URL(String.format("%s?schema=%s", userUri, SCOPE));
                JsonValue userValue = doRequest(getUserUrL, params);
                if (userValue != null) {

                    return DtoFactory.getInstance().createDto(GitUser.class)
                                     .withName(userValue.getElement("http://wso2.org/claims/fullname").getStringValue())
                                     .withEmail(userValue.getElement("http://wso2.org/claims/emailaddress").getStringValue());
                }

            }

        } catch (JsonParseException | IOException e) {
            LOG.warn(e.getLocalizedMessage());
            LOG.debug(e.getLocalizedMessage(), e);
            return null;
        }


        return null;
    }


    private JsonValue doRequest(URL tokenInfoUrl, Map<String, String> params) throws IOException, JsonParseException {
        HttpURLConnection http = null;
        try {
            http = (HttpURLConnection)tokenInfoUrl.openConnection();
            http.setRequestMethod("GET");
            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    http.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            int responseCode = http.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                LOG.warn("Can not receive wso2 token by path: {}. Response status: {}. Error message: {}",
                         tokenInfoUrl.toString(), responseCode, IoUtil.readStream(http.getErrorStream()));
                return null;
            }

            JsonValue result;
            try (InputStream input = http.getInputStream()) {
                result = JsonHelper.parseJson(input);
            }
            return result;
        } finally {
            if (http != null) {
                http.disconnect();
            }
        }
    }

    @Override
    public String getId() {
        return OAUTH_PROVIDER_NAME;
    }

    @Override
    public boolean canProvideCredentials(String url) {
        return WSO_2_URL_PATTERN.matcher(url).matches();
    }

}
