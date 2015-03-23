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
package org.eclipse.che.ide.ext.github.client.authenticator;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.eclipse.che.ide.ext.github.client.GitHubLocalizationConstant;
import org.eclipse.che.ide.ui.dialogs.CancelCallback;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;

/**
 * @author Roman Nikitenko
 */
public class GitHubAuthenticatorViewImpl implements GitHubAuthenticatorView{

    private DialogFactory                         dialogFactory;
    private GitHubLocalizationConstant            locale;
    private ActionDelegate delegate;


    private CheckBox isGenerateKeys;
    private DockLayoutPanel contentPanel;

    @Inject
    public GitHubAuthenticatorViewImpl(DialogFactory dialogFactory,
                                       GitHubLocalizationConstant locale) {
        this.dialogFactory = dialogFactory;
        this.locale = locale;

        isGenerateKeys = new CheckBox(locale.authGenerateKeyLabel());
        isGenerateKeys.setValue(true);

        contentPanel = new DockLayoutPanel(Style.Unit.PX);
        contentPanel.addNorth(new InlineHTML(locale.authMessageAuthRequest()), 20);
        contentPanel.addNorth(isGenerateKeys, 20);
    }

    @Override
    public void showDialog() {
        isGenerateKeys.setValue(true);
        dialogFactory.createConfirmDialog(locale.authTitle(),
                                          contentPanel,
                                          getConfirmCallback(),
                                          getCancelCallback()).show();
    }

    @Override
    public boolean isGenerateKeysSelected() {
        return isGenerateKeys.getValue();
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public Widget asWidget() {
        return contentPanel;
    }

    private ConfirmCallback getConfirmCallback() {
        return new ConfirmCallback() {
            @Override
            public void accepted() {
                delegate.onAccepted();
            }
        };
    }

    private CancelCallback getCancelCallback() {
        return new CancelCallback() {
            @Override
            public void cancelled() {
                delegate.onCancelled();
            }
        };
    }
}
