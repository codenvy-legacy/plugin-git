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
package com.codenvy.ide.ext.git.client;

import com.codenvy.api.project.gwt.client.ProjectServiceClient;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.ide.api.app.AppContext;
import com.codenvy.ide.api.app.CurrentProject;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.api.parts.ConsolePart;
import com.codenvy.ide.api.selection.SelectionAgent;
import com.codenvy.ide.dto.DtoFactory;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.google.web.bindery.event.shared.EventBus;
import com.googlecode.gwt.test.GwtModule;
import com.googlecode.gwt.test.GwtTestWithMockito;

import org.junit.Before;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

/**
 * Base test for git extension.
 *
 * @author <a href="mailto:aplotnikov@codenvy.com">Andrey Plotnikov</a>
 */
@GwtModule("com.codenvy.ide.ext.git.Git")
public abstract class BaseTest extends GwtTestWithMockito {
    public static final String  PROJECT_PATH    = "/test";
    public static final String  VFS_ID          = "vfsid";
    public static final boolean SELECTED_ITEM   = true;
    public static final boolean UNSELECTED_ITEM = false;
    public static final boolean ENABLE_BUTTON   = true;
    public static final boolean DISABLE_BUTTON  = false;
    public static final boolean ENABLE_FIELD    = true;
    public static final boolean DISABLE_FIELD   = false;
    public static final boolean ACTIVE_BRANCH   = true;
    public static final String  EMPTY_TEXT      = "";
    public static final String  PROJECT_NAME    = "test";
    public static final String  REMOTE_NAME     = "codenvy";
    public static final String  REMOTE_URI      = "git@github.com:codenvy/test.git";
    public static final String  REPOSITORY_NAME = "origin";
    public static final String  LOCAL_BRANCH    = "localBranch";
    public static final String  REMOTE_BRANCH   = "remoteBranch";
    @Mock
    protected CurrentProject          currentProject;
    @Mock
    protected ProjectDescriptor       projectDescriptor;
    @Mock
    protected AppContext              appContext;
    @Mock
    protected GitServiceClient        service;
    @Mock
    protected GitLocalizationConstant constant;
    @Mock
    protected ConsolePart             console;
    @Mock
    protected GitResources            resources;
    @Mock
    protected EventBus                eventBus;
    @Mock
    protected SelectionAgent          selectionAgent;
    @Mock
    protected NotificationManager     notificationManager;
    @Mock
    protected DtoFactory              dtoFactory;
    @Mock
    protected DtoUnmarshallerFactory  dtoUnmarshallerFactory;
    @Mock
    protected ProjectServiceClient    projectServiceClient;

    @Before
    public void disarm() {
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getProjectDescription()).thenReturn(projectDescriptor);
//        when(currentProject.getName()).thenReturn(PROJECT_NAME);
    }
}
