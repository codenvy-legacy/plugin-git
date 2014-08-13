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

import com.codenvy.ide.api.action.Action;
import com.codenvy.ide.api.app.AppContext;
import com.codenvy.ide.api.app.CurrentProject;
import com.google.gwt.resources.client.ImageResource;

import org.vectomatic.dom.svg.ui.SVGResource;

import java.util.List;

/**
 * @author Roman Nikitenko
 */
public abstract class GitAction extends Action {

    protected final AppContext appContext;

    public GitAction(String text,
                     String description,
                     ImageResource icon,
                     SVGResource svgIcon,
                     AppContext appContext) {
        super(text, description, icon, svgIcon);
        this.appContext = appContext;
    }

    protected boolean isGitRepository() {
        boolean isGitRepository = false;

        if (getActiveProject() != null) {
            List<String> listVcsProvider = getActiveProject().getAttributeValues("vcs.provider.name");

            if (listVcsProvider != null && (!listVcsProvider.isEmpty()) && listVcsProvider.contains("git")) {
                isGitRepository = true;
            }
        }
        return isGitRepository;
    }

    protected CurrentProject getActiveProject() {
        return appContext.getCurrentProject();
    }
}
