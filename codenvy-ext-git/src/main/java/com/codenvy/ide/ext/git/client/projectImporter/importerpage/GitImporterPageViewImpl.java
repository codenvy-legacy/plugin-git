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
package com.codenvy.ide.ext.git.client.projectImporter.importerpage;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Inject;

/**
 * @author Roman Nikitenko
 */
public class GitImporterPageViewImpl extends Composite implements GitImporterPageView {

    interface GitImporterPageViewImplUiBinder extends UiBinder<DockLayoutPanel, GitImporterPageViewImpl> {
    }

    @UiField
    SimplePanel            basePagePanel;

    @Inject
    public GitImporterPageViewImpl(GitImporterPageViewImplUiBinder uiBinder) {
        initWidget(uiBinder.createAndBindUi(this));
    }

    /** {@inheritDoc} */
    @Override
    public AcceptsOneWidget getBasePagePanel() {
        return basePagePanel;
    }
}
