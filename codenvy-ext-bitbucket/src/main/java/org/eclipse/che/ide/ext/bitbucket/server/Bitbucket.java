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
package org.eclipse.che.ide.ext.bitbucket.server;

import org.eclipse.che.api.auth.oauth.OAuthAuthorizationHeaderProvider;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.json.JsonHelper;
import org.eclipse.che.commons.json.JsonParseException;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.commons.ParsingResponseException;
import org.eclipse.che.ide.ext.bitbucket.shared.BitbucketPullRequest;
import org.eclipse.che.ide.ext.bitbucket.shared.BitbucketPullRequests;
import org.eclipse.che.ide.ext.bitbucket.shared.BitbucketPullRequestsPage;
import org.eclipse.che.ide.ext.bitbucket.shared.BitbucketRepositories;
import org.eclipse.che.ide.ext.bitbucket.shared.BitbucketRepositoriesPage;
import org.eclipse.che.ide.ext.bitbucket.shared.BitbucketRepository;
import org.eclipse.che.ide.ext.bitbucket.shared.BitbucketRepositoryFork;
import org.eclipse.che.ide.ext.bitbucket.shared.BitbucketUser;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.commons.json.JsonHelper.toJson;
import static org.eclipse.che.commons.json.JsonNameConventions.CAMEL_UNDERSCORE;
import static org.eclipse.che.ide.MimeType.APPLICATION_FORM_URLENCODED;
import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.ext.bitbucket.shared.Preconditions.checkArgument;
import static org.eclipse.che.ide.ext.bitbucket.shared.StringHelper.isNullOrEmpty;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.AUTHORIZATION;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENT_TYPE;
import static org.eclipse.che.ide.rest.HTTPMethod.GET;
import static org.eclipse.che.ide.rest.HTTPMethod.POST;
import static org.eclipse.che.ide.rest.HTTPStatus.CREATED;
import static org.eclipse.che.ide.rest.HTTPStatus.OK;
import static java.net.URLDecoder.decode;
import static java.net.URLEncoder.encode;

/**
 * Contains methods for retrieving data from BITBUCKET and processing it before sending to client side.
 *
 * @author Kevin Pollet
 */
public class Bitbucket {
    private static final String BITBUCKET_API_URL     = "https://api.bitbucket.org";
    private static final String BITBUCKET_2_0_API_URL = BITBUCKET_API_URL + "/2.0";
    private static final String BITBUCKET_1_0_API_URL = BITBUCKET_API_URL + "/1.0";

    private final OAuthAuthorizationHeaderProvider authorizationHeaderProvider;

    @Inject
    public Bitbucket(@Nonnull final OAuthAuthorizationHeaderProvider authorizationHeaderProvider) {
        this.authorizationHeaderProvider = authorizationHeaderProvider;
    }

    /**
     * Get authorized user information.
     *
     * @return the {@link org.eclipse.che.ide.ext.bitbucket.shared.BitbucketUser}.
     * @throws java.io.IOException
     *         if any i/o errors occurs.
     * @throws BitbucketException
     *         if Bitbucket server return unexpected or error status for request.
     * @throws ParsingResponseException
     *         if any error occurs when parse.
     */
    public BitbucketUser getUser() throws IOException, BitbucketException, ParsingResponseException {
        final String response = getJson(BITBUCKET_2_0_API_URL + "/user", OK);
        return parseJsonResponse(response, BitbucketUser.class);
    }

    /**
     * Get Bitbucket repository information.
     *
     * @param owner
     *         the repository owner, cannot be {@code null}.
     * @param repositorySlug
     *         the repository name, cannot be {@code null}.
     * @return the {@link org.eclipse.che.ide.ext.bitbucket.shared.BitbucketRepository}.
     * @throws java.io.IOException
     *         if any i/o errors occurs.
     * @throws BitbucketException
     *         if Bitbucket server return unexpected or error status for request.
     * @throws ParsingResponseException
     *         if any error occurs when parse.
     * @throws java.lang.IllegalArgumentException
     *         if one parameter is not valid.
     */
    public BitbucketRepository getRepository(@Nonnull final String owner, @Nonnull final String repositorySlug)
            throws IOException, BitbucketException, ParsingResponseException, IllegalArgumentException {
        checkArgument(!isNullOrEmpty(owner), "owner");
        checkArgument(!isNullOrEmpty(repositorySlug), "repositorySlug");

        final String response = getJson(BITBUCKET_2_0_API_URL + "/repositories/" + owner + "/" + repositorySlug, OK);
        return parseJsonResponse(response, BitbucketRepository.class);
    }

    /**
     * Get Bitbucket repository forks.
     *
     * @param owner
     *         the repository owner, cannot be {@code null}.
     * @param repositorySlug
     *         the repository name, cannot be {@code null}.
     * @return the fork {@link org.eclipse.che.ide.ext.bitbucket.shared.BitbucketRepositories}.
     * @throws java.io.IOException
     *         if any i/o errors occurs.
     * @throws BitbucketException
     *         if Bitbucket server return unexpected or error status for request.
     * @throws ParsingResponseException
     *         if any error occurs when parse.
     * @throws java.lang.IllegalArgumentException
     *         if one parameter is not valid.
     */
    public BitbucketRepositories getRepositoryForks(@Nonnull final String owner, @Nonnull final String repositorySlug)
            throws IOException, BitbucketException, ParsingResponseException, IllegalArgumentException {
        checkArgument(!isNullOrEmpty(owner), "owner");
        checkArgument(!isNullOrEmpty(repositorySlug), "repositorySlug");

        final List<BitbucketRepository> repositories = new ArrayList<>();
        BitbucketRepositoriesPage repositoryPage = DtoFactory.getInstance().createDto(BitbucketRepositoriesPage.class);

        do {

            final String nextPageUrl = repositoryPage.getNext();
            final String url =
                    nextPageUrl == null ? BITBUCKET_2_0_API_URL + "/repositories/" + owner + "/" + repositorySlug + "/forks" : nextPageUrl;
            repositoryPage = getBitbucketPage(url, BitbucketRepositoriesPage.class);
            repositories.addAll(repositoryPage.getValues());

        } while (repositoryPage.getNext() != null);

        return DtoFactory.getInstance().createDto(BitbucketRepositories.class).withRepositories(repositories);
    }

    /**
     * Fork a Bitbucket repository.
     *
     * @param owner
     *         the repository owner, cannot be {@code null}.
     * @param repositorySlug
     *         the repository name, cannot be {@code null}.
     * @param forkName
     *         the fork name, cannot be {@code null}.
     * @param isForkPrivate
     *         if the fork must be private.
     * @return the fork {@link org.eclipse.che.ide.ext.bitbucket.shared.BitbucketRepositoryFork}.
     * @throws IOException
     *         if any i/o errors occurs.
     * @throws BitbucketException
     *         if Bitbucket server return unexpected or error status for request.
     * @throws ParsingResponseException
     *         if any error occurs when parse.
     * @throws java.lang.IllegalArgumentException
     *         if one parameter is not valid.
     */
    public BitbucketRepositoryFork forkRepository(@Nonnull final String owner,
                                                  @Nonnull final String repositorySlug,
                                                  @Nonnull final String forkName,
                                                  final boolean isForkPrivate)
            throws IOException, BitbucketException, ParsingResponseException, IllegalArgumentException {
        checkArgument(!isNullOrEmpty(owner), "owner");
        checkArgument(!isNullOrEmpty(repositorySlug), "repositorySlug");
        checkArgument(!isNullOrEmpty(forkName), "forkName");

        final String url = BITBUCKET_1_0_API_URL + "/repositories/" + owner + "/" + repositorySlug + "/fork";
        final String data = "name=" + encode(forkName, "UTF-8") + "&is_private=" + isForkPrivate;
        final String response = doRequest(POST, url, OK, APPLICATION_FORM_URLENCODED, data);
        return parseJsonResponse(response, BitbucketRepositoryFork.class);
    }


    /**
     * Get Bitbucket repository pull requests.
     *
     * @param owner
     *         the repositories owner, cannot be {@code null}.
     * @param repositorySlug
     *         the repository name, cannot be {@code null}.
     * @return the {@link org.eclipse.che.ide.ext.bitbucket.shared.BitbucketPullRequests}.
     * @throws IOException
     *         if any i/o errors occurs.
     * @throws BitbucketException
     *         if Bitbucket server return unexpected or error status for request.
     * @throws ParsingResponseException
     *         if any error occurs when parse.
     * @throws IllegalArgumentException
     *         if one parameter is not valid.
     */
    public BitbucketPullRequests getRepositoryPullRequests(@Nonnull final String owner, @Nonnull final String repositorySlug)
            throws IOException, BitbucketException, ParsingResponseException, IllegalArgumentException {
        checkArgument(!isNullOrEmpty(owner), "owner");
        checkArgument(!isNullOrEmpty(repositorySlug), "repositorySlug");

        final List<BitbucketPullRequest> pullRequests = new ArrayList<>();
        BitbucketPullRequestsPage pullRequestsPage = DtoFactory.getInstance().createDto(BitbucketPullRequestsPage.class);

        do {

            final String nextPageUrl = pullRequestsPage.getNext();
            final String url =
                    nextPageUrl == null ? BITBUCKET_2_0_API_URL + "/repositories/" + owner + "/" + repositorySlug + "/pullrequests"
                                        : nextPageUrl;

            pullRequestsPage = getBitbucketPage(url, BitbucketPullRequestsPage.class);
            pullRequests.addAll(pullRequestsPage.getValues());

        } while (pullRequestsPage.getNext() != null);

        return DtoFactory.getInstance().createDto(BitbucketPullRequests.class).withPullRequests(pullRequests);

    }

    /**
     * Open the given {@link org.eclipse.che.ide.ext.bitbucket.shared.BitbucketPullRequest}.
     *
     * @param owner
     *         the repository owner, cannot be {@code null}.
     * @param repositorySlug
     *         the repository name, cannot be {@code null}.
     * @param pullRequest
     *         the {@link org.eclipse.che.ide.ext.bitbucket.shared.BitbucketPullRequest} to open, cannot be {@code null}.
     * @return the opened {@link org.eclipse.che.ide.ext.bitbucket.shared.BitbucketPullRequest}.
     */
    public BitbucketPullRequest openPullRequest(@Nonnull final String owner,
                                                @Nonnull final String repositorySlug,
                                                @Nonnull final BitbucketPullRequest pullRequest)
            throws ParsingResponseException, IOException, BitbucketException {
        checkArgument(!isNullOrEmpty(owner), "owner");
        checkArgument(!isNullOrEmpty(repositorySlug), "repositorySlug");
        checkArgument(pullRequest != null, "pullRequest");

        final String url = BITBUCKET_2_0_API_URL + "/repositories/" + owner + "/" + repositorySlug + "/pullrequests";
        final String response = postJson(url, CREATED, toJson(pullRequest, CAMEL_UNDERSCORE));
        return parseJsonResponse(response, BitbucketPullRequest.class);
    }

    private <T> T getBitbucketPage(final String url,
                                   final Class<T> pageClass) throws IOException, BitbucketException, ParsingResponseException {
        final String response = getJson(url, OK);
        return parseJsonResponse(response, pageClass);
    }

    private String getJson(final String url, final int success) throws IOException, BitbucketException {
        return doRequest(GET, url, success, null, null);
    }

    private String postJson(final String url, final int success, final String data) throws IOException, BitbucketException {
        return doRequest(POST, url, success, APPLICATION_JSON, data);
    }

    private String doRequest(final String requestMethod,
                             final String requestUrl,
                             final int success,
                             final String contentType,
                             final String data) throws IOException, BitbucketException {
        HttpURLConnection http = null;

        try {

            http = (HttpURLConnection)new URL(requestUrl).openConnection();
            http.setInstanceFollowRedirects(false);
            http.setRequestMethod(requestMethod);

            final Map<String, String> requestParameters = new HashMap<>();
            if (data != null && APPLICATION_FORM_URLENCODED.equals(contentType)) {
                final String[] parameters = data.split("&");

                for (final String oneParameter : parameters) {
                    final String[] oneParameterKeyAndValue = oneParameter.split("=");
                    if (oneParameterKeyAndValue.length == 2) {
                        requestParameters.put(oneParameterKeyAndValue[0], decode(oneParameterKeyAndValue[1], "UTF-8"));
                    }
                }
            }

            final String authorizationHeaderValue = authorizationHeaderProvider.getAuthorizationHeader("bitbucket", getUserId(),
                                                                                                       requestMethod, requestUrl,
                                                                                                       requestParameters);
            http.setRequestProperty(AUTHORIZATION, authorizationHeaderValue);
            http.setRequestProperty(ACCEPT, APPLICATION_JSON);

            if (data != null && !data.isEmpty()) {
                http.setRequestProperty(CONTENT_TYPE, contentType);
                http.setDoOutput(true);

                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(http.getOutputStream()))) {
                    writer.write(data);
                }
            }

            if (http.getResponseCode() != success) {
                throw fault(http);
            }

            String result;
            try (InputStream input = http.getInputStream()) {
                result = readBody(input, http.getContentLength());
            }

            return result;

        } finally {
            if (http != null) {
                http.disconnect();
            }
        }
    }

    private <O> O parseJsonResponse(final String json, final Class<O> clazz) throws ParsingResponseException {
        try {

            return JsonHelper.fromJson(json, clazz, null, CAMEL_UNDERSCORE);

        } catch (JsonParseException e) {
            throw new ParsingResponseException(e);
        }
    }

    private BitbucketException fault(final HttpURLConnection http) throws IOException {
        final int responseCode = http.getResponseCode();

        try (final InputStream stream = (responseCode >= 400 ? http.getErrorStream() : http.getInputStream())) {

            String body = null;
            if (stream != null) {
                final int length = http.getContentLength();
                body = readBody(stream, length);
            }

            return new BitbucketException(responseCode, body, http.getContentType());
        }
    }

    private String readBody(final InputStream input, final int contentLength) throws IOException {
        String body = null;
        if (contentLength > 0) {
            byte[] b = new byte[contentLength];
            int off = 0;
            int i;
            while ((i = input.read(b, off, contentLength - off)) > 0) {
                off += i;
            }
            body = new String(b);
        } else if (contentLength < 0) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int point;
            while ((point = input.read(buf)) != -1) {
                bout.write(buf, 0, point);
            }
            body = bout.toString();
        }
        return body;
    }

    private String getUserId() {
        return EnvironmentContext.getCurrent().getUser().getId();
    }
}
