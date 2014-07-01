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
package com.codenvy.ide.ext.git.client.action;

import com.codenvy.ide.api.resources.ResourceProvider;
import com.codenvy.ide.api.resources.model.Project;
import com.codenvy.ide.api.ui.action.Action;
import com.google.gwt.resources.client.ImageResource;

import org.vectomatic.dom.svg.ui.SVGResource;

import java.util.List;

/**
 * @author Roman Nikitenko
 */
public abstract class GitAction extends Action {

    protected final ResourceProvider resourceProvider;

    public GitAction(String text,
                     String description,
                     ImageResource icon,
                     SVGResource svgIcon,
                     ResourceProvider resourceProvider) {
        super(text, description, icon, svgIcon);
        this.resourceProvider = resourceProvider;
    }

    protected boolean isGitRepository() {
        boolean isGitRepository = false;

        if (getActiveProject() != null && getActiveProject().getAttributes().containsKey("vcs.provider.name")) {
            List<String>listVcsProvider = getActiveProject().getAttributes().get("vcs.provider.name");

            if ((! listVcsProvider.isEmpty()) && listVcsProvider.contains("git")) {
                isGitRepository = true;
            }
        }
        return isGitRepository;
    }

    protected Project getActiveProject() {
        return resourceProvider.getActiveProject();
    }
}
