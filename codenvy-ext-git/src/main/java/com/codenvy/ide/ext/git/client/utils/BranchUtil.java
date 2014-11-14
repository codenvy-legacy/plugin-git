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
package com.codenvy.ide.ext.git.client.utils;

import com.codenvy.ide.collections.Array;
import com.codenvy.ide.collections.Collections;
import com.codenvy.ide.ext.git.shared.Branch;

import javax.annotation.Nonnull;

/**
 * @author Serii Leschenko
 */
public class BranchUtil {
    /**
     * Set values of remote branches: filter remote branches due to selected remote repository.
     *
     * @param remoteName
     *         remote name
     * @param remoteBranches
     *         remote branches
     */
    @Nonnull
    public Array<String> getRemoteBranchesToDisplay(@Nonnull String remoteName, @Nonnull Array<Branch> remoteBranches) {
        return getRemoteBranchesToDisplay(new BranchFilterByRemote(remoteName), remoteBranches);
    }

    /**
     * Set values of remote branches: filter remote branches due to selected remote repository.
     */
    @Nonnull
    public Array<String> getRemoteBranchesToDisplay(BranchFilterByRemote filterByRemote, @Nonnull Array<Branch> remoteBranches) {
        Array<String> branches = Collections.createArray();

        if (remoteBranches.isEmpty()) {
            branches.add("master");
            return branches;
        }


        for (int i = 0; i < remoteBranches.size(); i++) {
            Branch branch = remoteBranches.get(i);
            if (filterByRemote.isLinkedTo(branch)) {
                branches.add(filterByRemote.getBranchNameWithoutRefs(branch));
            }
        }

        if (branches.isEmpty()) {
            branches.add("master");
        }
        return branches;
    }

    /**
     * Set values of local branches.
     *
     * @param localBranches
     *         local branches
     */
    @Nonnull
    public Array<String> getLocalBranchesToDisplay(@Nonnull Array<Branch> localBranches) {
        Array<String> branches = Collections.createArray();

        if (localBranches.isEmpty()) {
            branches.add("master");
            return branches;
        }

        for (Branch branch : localBranches.asIterable()) {
            branches.add(branch.getDisplayName());
        }

        return branches;
    }
}
