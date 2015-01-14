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
package com.codenvy.ide.ext.github.client;

import com.codenvy.ide.collections.Array;
import com.codenvy.ide.collections.StringMap;
import com.codenvy.ide.ext.github.shared.Collaborators;
import com.codenvy.ide.ext.github.shared.GitHubIssueComment;
import com.codenvy.ide.ext.github.shared.GitHubIssueCommentInput;
import com.codenvy.ide.ext.github.shared.GitHubPullRequest;
import com.codenvy.ide.ext.github.shared.GitHubPullRequestCreationInput;
import com.codenvy.ide.ext.github.shared.GitHubPullRequestList;
import com.codenvy.ide.ext.github.shared.GitHubRepository;
import com.codenvy.ide.ext.github.shared.GitHubRepositoryList;
import com.codenvy.ide.ext.github.shared.GitHubUser;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.AsyncRequestFactory;
import com.codenvy.ide.rest.AsyncRequestLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Implementation for {@link GitHubClientService}.
 *
 * @author Oksana Vereshchaka
 * @author Stéphane Daviet
 */
@Singleton
public class GitHubClientServiceImpl implements GitHubClientService {
    private static final String       LIST           = "/list";
    private static final String       LIST_ACCOUNT   = "/list/account";
    private static final String       LIST_ORG       = "/list/org";
    private static final String       LIST_USER      = "/list/user";
    private static final String       LIST_ALL       = "/list/available";
    private static final String       COLLABORATORS  = "/collaborators";
    private static final String       ORGANIZATIONS  = "/orgs";
    private static final String       PAGE           = "/page";
    private static final String       TOKEN          = "/token";
    private static final String       USER           = "/user";
    private static final String       SSH_GEN        = "/ssh/generate";
    private static final String       FORKS          = "/forks";
    private static final String       CREATE_FORK    = "/createfork";
    private static final String       PULL_REQUEST   = "/pullrequest";
    private static final String       PULL_REQUESTS  = "/pullrequests";
    private static final String       ISSUE_COMMENTS = "/issuecomments";
    /** REST service context. */
    private final String              baseUrl;
    /** Loader to be displayed. */
    private final AsyncRequestLoader  loader;
    private final AsyncRequestFactory asyncRequestFactory;

    @Inject
    protected GitHubClientServiceImpl(@Named("restContext") String baseUrl,
                                      AsyncRequestLoader loader,
                                      AsyncRequestFactory asyncRequestFactory) {
        this.baseUrl = baseUrl + "/github";
        this.loader = loader;
        this.asyncRequestFactory = asyncRequestFactory;
    }

    /** {@inheritDoc} */
    @Override
    public void getRepositoriesList(AsyncRequestCallback<GitHubRepositoryList> callback) {
        String url = baseUrl + LIST;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getForks(@Nonnull String user, @Nonnull String repository,
                                  @Nonnull AsyncRequestCallback<GitHubRepositoryList> callback) {
        String url = baseUrl + FORKS + "/" + user + "/" + repository;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void fork(@Nonnull String user, @Nonnull String repository, @Nonnull AsyncRequestCallback<GitHubRepository> callback) {
        String url = baseUrl + CREATE_FORK + "/" + user + "/" + repository;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    @Override
    public void commentIssue(String user, String repository, String issue, GitHubIssueCommentInput input, AsyncRequestCallback<GitHubIssueComment> callback) {
        String url = baseUrl + ISSUE_COMMENTS + "/" + user + "/" + repository + "/" + issue;
        asyncRequestFactory.createPostRequest(url, input).loader(loader).send(callback);
    }

    @Override
    public void getPullRequests(String owner, String repository, AsyncRequestCallback<GitHubPullRequestList> callback) {
        String url = baseUrl + PULL_REQUESTS + "/" + owner + "/" + repository;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void createPullRequest(@Nonnull String user, @Nonnull String repository, @Nonnull GitHubPullRequestCreationInput input,
                                  @Nonnull AsyncRequestCallback<GitHubPullRequest> callback) {
        String url = baseUrl + PULL_REQUEST + "/" + user + "/" + repository;
        asyncRequestFactory.createPostRequest(url, input).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getRepositoriesByUser(String userName, AsyncRequestCallback<GitHubRepositoryList> callback) {
        String params = (userName != null) ? "?username=" + userName : "";
        String url = baseUrl + LIST_USER;
        asyncRequestFactory.createGetRequest(url + params).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getAllRepositories(AsyncRequestCallback<StringMap<Array<GitHubRepository>>> callback) {
        String url = baseUrl + LIST_ALL;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getCollaborators(String user, String repository, AsyncRequestCallback<Collaborators> callback) {
        String url = baseUrl + COLLABORATORS + "/" + user + "/" + repository;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getUserToken(@Nonnull String user, @Nonnull AsyncRequestCallback<String> callback) {
        String url = baseUrl + TOKEN + "/" + user;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getOrganizations(AsyncRequestCallback<List<String>> callback) {
        String url = baseUrl + ORGANIZATIONS;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getUserInfo(AsyncRequestCallback<GitHubUser> callback) {
        String url = baseUrl + USER;
        asyncRequestFactory.createGetRequest(url).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getRepositoriesByOrganization(String organization, AsyncRequestCallback<GitHubRepositoryList> callback) {
        String params = (organization != null) ? "?organization=" + organization : "";
        String url = baseUrl + LIST_ORG;
        asyncRequestFactory.createGetRequest(url + params).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getRepositoriesByAccount(String account, AsyncRequestCallback<GitHubRepositoryList> callback) {
        String params = (account != null) ? "?account=" + account : "";
        String url = baseUrl + LIST_ACCOUNT;
        asyncRequestFactory.createGetRequest(url + params).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getPage(String pageLocation, AsyncRequestCallback<GitHubRepositoryList> callback) {
        String params = (pageLocation != null) ? "?url=" + pageLocation : "";
        String url = baseUrl + PAGE;
        asyncRequestFactory.createGetRequest(url + params).loader(loader).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void updatePublicKey(@Nonnull AsyncRequestCallback<Void> callback) {
        String url = baseUrl + SSH_GEN;
        asyncRequestFactory.createPostRequest(url, null).loader(loader).send(callback);
    }
}