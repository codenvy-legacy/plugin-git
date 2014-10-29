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

import com.codenvy.api.analytics.logger.AnalyticsEventLogger;
import com.codenvy.ide.api.action.ActionEvent;
import com.codenvy.ide.api.app.AppContext;
import com.codenvy.ide.api.selection.SelectionAgent;
import com.codenvy.ide.ext.git.client.GitLocalizationConstant;
import com.codenvy.ide.ext.git.client.GitResources;
import com.codenvy.ide.ext.git.client.init.InitRepositoryPresenter;
import com.codenvy.ide.ui.dialogs.ConfirmCallback;
import com.codenvy.ide.ui.dialogs.DialogFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/** @author Andrey Plotnikov */
@Singleton
public class InitRepositoryAction extends GitAction {
    private final InitRepositoryPresenter presenter;
    private final AnalyticsEventLogger    eventLogger;
    private final DialogFactory           dialogFactory;
    private GitLocalizationConstant constant;

    @Inject
    public InitRepositoryAction(InitRepositoryPresenter presenter,
                                GitResources resources,
                                GitLocalizationConstant constant,
                                AppContext appContext,
                                AnalyticsEventLogger eventLogger,
                                SelectionAgent selectionAgent,
                                DialogFactory dialogFactory) {
        super(constant.initControlTitle(), constant.initControlPrompt(), resources.initRepo(), appContext, selectionAgent);
        this.presenter = presenter;
        this.eventLogger = eventLogger;
        this.constant = constant;
        this.dialogFactory = dialogFactory;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        dialogFactory.createConfirmDialog(constant.createTitle(),
                                          constant.messagesInitRepoQuestion(appContext.getCurrentProject().getRootProject().getName()),
                                          new ConfirmCallback() {
                                              @Override
                                              public void accepted() {
                                                  presenter.initRepository();
                                              }
                                          }, null).show();
    }
}
