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
import com.codenvy.ide.ext.github.shared.GitHubRepository;
import com.codenvy.ide.ext.github.shared.GitHubRepositoryList;
import com.codenvy.ide.ext.github.shared.GitHubUser;
import com.codenvy.ide.rest.AsyncRequestCallback;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Client service for Samples.
 *
 * @author Oksana Vereshchaka
 */
public interface GitHubClientService {
    /**
     * Get the list of available public and private repositories of the authorized user.
     *
     * @param callback
     *         the callback client has to implement
     */
    public abstract void getRepositoriesList(@Nonnull AsyncRequestCallback<GitHubRepositoryList> callback);

    /**
     * Get the list of forks for given repository
     *
     * @param user
     * @param repository
     * @param callback
     */
    public abstract void getForks(@Nonnull String user, @Nonnull String repository,
                                  @Nonnull AsyncRequestCallback<GitHubRepositoryList> callback);

    /**
     * Get the list of forks for given repository
     *
     * @param user
     * @param repository
     * @param callback
     */
    public abstract void fork(@Nonnull String user, @Nonnull String repository,
                                  @Nonnull AsyncRequestCallback<GitHubRepository> callback);

    /**
     * Get the list of available public repositories from GitHub user.
     *
     * @param userName
     *         Name of GitHub User
     * @param callback
     *         the callback client has to implement
     */
    public abstract void getRepositoriesByUser(@Nonnull String userName, @Nonnull AsyncRequestCallback<GitHubRepositoryList> callback);

    /**
     * Get the page with GitHub repositories.
     *
     * @param pageLocation
     *         page location
     * @param callback
     */
    public abstract void getPage(@Nonnull String pageLocation, @Nonnull AsyncRequestCallback<GitHubRepositoryList> callback);

    /**
     * Get the list of available repositories by GitHub organization.
     *
     * @param organization
     *         Name of GitHub organization
     * @param callback
     *         the callback client has to implement
     */
    public abstract void getRepositoriesByOrganization(@Nonnull String organization,
                                                       @Nonnull AsyncRequestCallback<GitHubRepositoryList> callback);

    /**
     * Get the list of available public repositories from GitHub account.
     *
     * @param account
     *         Name of GitHub Account
     * @param callback
     *         the callback client has to implement
     */
    public abstract void getRepositoriesByAccount(@Nonnull String account, @Nonnull AsyncRequestCallback<GitHubRepositoryList> callback);

    /**
     * Get list of collaborators of GitHub repository. For detail see GitHub REST API http://developer.github.com/v3/repos/collaborators/.
     *
     * @param user
     * @param repository
     * @param callback
     */
    public abstract void getCollaborators(@Nonnull String user, @Nonnull String repository,
                                          @Nonnull AsyncRequestCallback<Collaborators> callback);

    /**
     * Get the GitHub oAuth token for the pointed user.
     *
     * @param user
     *         user's id
     * @param callback
     */
    public abstract void getUserToken(@Nonnull String user, @Nonnull AsyncRequestCallback<String> callback);

    /**
     * Get the map of available public and private repositories of the authorized user and organizations he exists in.
     *
     * @param callback
     *         the callback client has to implement
     */
    public abstract void getAllRepositories(@Nonnull AsyncRequestCallback<StringMap<Array<GitHubRepository>>> callback);

    /**
     * Get the list of the organizations, where authorized user is a member.
     *
     * @param callback
     */
    public abstract void getOrganizations(@Nonnull AsyncRequestCallback<List<String>> callback);

    /**
     * Get authorized user information.
     *
     * @param callback
     */
    public abstract void getUserInfo(@Nonnull AsyncRequestCallback<GitHubUser> callback);

    /**
     * Generate and upload new public key if not exist on github.com.
     *
     * @param callback
     */
    public abstract void updatePublicKey(@Nonnull AsyncRequestCallback<Void> callback);
}