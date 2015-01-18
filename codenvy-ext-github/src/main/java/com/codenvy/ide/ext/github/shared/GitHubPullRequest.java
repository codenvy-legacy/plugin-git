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
package com.codenvy.ide.ext.github.shared;

import com.codenvy.dto.shared.DTO;

@DTO
public interface GitHubPullRequest {
    /**
     * Get pull request id.
     *
     * @return {@link String} id
     */
    String getId();

    void setId(String id);

    GitHubPullRequest withId(String id);

    /**
     * Get pull request URL.
     *
     * @return {@link String} url
     */
    String getUrl();

    void setUrl(String url);

    GitHubPullRequest withUrl(String url);

    /**
     * Get pull request html URL.
     *
     * @return {@link String} html_url
     */
    String getHtmlUrl();

    void setHtmlUrl(String htmlUrl);

    GitHubPullRequest withHtmlUrl(String htmlUrl);

    /**
     * Get pull request number.
     *
     * @return {@link String} number
     */
    String getNumber();

    void setNumber(String number);

    GitHubPullRequest withNumber(String number);

    /**
     * Get pull request state.
     *
     * @return {@link String} state
     */
    String getState();

    void setState(String state);

    GitHubPullRequest withState(String state);

    /**
     * Get pull request head.
     *
     * @return {@link GitHubPullRequestHead} head
     */
    GitHubPullRequestHead getHead();

    void setHead(GitHubPullRequestHead head);

    GitHubPullRequest withHead(GitHubPullRequestHead head);
}
