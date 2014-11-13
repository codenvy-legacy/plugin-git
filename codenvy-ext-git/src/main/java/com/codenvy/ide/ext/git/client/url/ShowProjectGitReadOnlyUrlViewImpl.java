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
package com.codenvy.ide.ext.git.client.url;

import com.codenvy.ide.collections.Array;
import com.codenvy.ide.ext.git.client.GitLocalizationConstant;
import com.codenvy.ide.ext.git.client.GitResources;
import com.codenvy.ide.ext.git.shared.Remote;
import com.codenvy.ide.ui.window.Window;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.vectomatic.dom.svg.ui.SVGImage;

import javax.annotation.Nonnull;

/**
 * The implementation of {@link ShowProjectGitReadOnlyUrlView}.
 *
 * @author <a href="mailto:aplotnikov@codenvy.com">Andrey Plotnikov</a>
 */
@Singleton
public class ShowProjectGitReadOnlyUrlViewImpl extends Window implements ShowProjectGitReadOnlyUrlView {
    interface ShowProjectGitReadOnlyUrlViewImplUiBinder extends UiBinder<Widget, ShowProjectGitReadOnlyUrlViewImpl> {
    }

    private static ShowProjectGitReadOnlyUrlViewImplUiBinder ourUiBinder = GWT.create(ShowProjectGitReadOnlyUrlViewImplUiBinder.class);

    @UiField
    FlowPanel localPanel;
    @UiField
    TextBox   localUrl;
    @UiField
    FlowPanel remotePanel;

    Button btnClose;
    @UiField(provided = true)
    final   GitResources            res;
    @UiField(provided = true)
    final   GitLocalizationConstant locale;
    private ActionDelegate          delegate;

    /**
     * Create view.
     *
     * @param resources
     * @param locale
     */
    @Inject
    protected ShowProjectGitReadOnlyUrlViewImpl(GitResources resources, GitLocalizationConstant locale) {
        this.res = resources;
        this.locale = locale;
        this.ensureDebugId("projectReadOnlyGitUrl-window");

        Widget widget = ourUiBinder.createAndBindUi(this);

        this.setTitle(locale.projectReadOnlyGitUrlWindowTitle());
        this.setWidget(widget);

        btnClose = createButton(locale.buttonClose(), "", new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                delegate.onCloseClicked();
            }
        });
        btnClose.ensureDebugId("projectReadOnlyGitUrl-btnClose");
        getFooter().add(btnClose);

        appendCopyToClipboardButton(localUrl);
    }

    /** {@inheritDoc} */
    @Override
    public void setLocaleUrl(@Nonnull String url) {
        localUrl.setText(url);
    }

    /** {@inheritDoc} */
    @Override
    public void setRemotes(Array<Remote> remotes) {
        remotePanel.clear();
        if (remotes != null) {
            remotePanel.add(new Label(
                    remotes.size() > 1 ? locale.projectReadOnlyGitRemoteUrlsTitle() : locale.projectReadOnlyGitRemoteUrlTitle()));
            for (Remote remote : remotes.asIterable()) {
                if (remote != null && remote.getUrl() != null) {
                    Document.get().createTextInputElement();
                    TextBox remoteUrl = new TextBox();
                    remoteUrl.setText(remote.getUrl());
                    remotePanel.add(remoteUrl);
                    appendCopyToClipboardButton(remoteUrl);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        this.hide();
    }

    /** {@inheritDoc} */
    @Override
    public void showDialog() {
        this.show();
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    protected void onClose() {
    }

    /**
     * Append copy to clipboard button for the Widget.
     *
     * @param textBoxWidget
     */
    private void appendCopyToClipboardButton(@Nonnull Widget textBoxWidget) {
        Element copyToClipboardButton = createCopyToClipboardButton(textBoxWidget.getElement(),
                                                                    new SVGImage(res.clipboard()).getElement(),
                                                                    res.gitCSS().zeroClipboardButton(),
                                                                    locale.zeroClipboardButtonReadyCopyPrompt(),
                                                                    locale.zeroClipboardButtonAfterCopyPrompt(),
                                                                    locale.zeroClipboardButtonCopyErrorPrompt(),
                                                                    locale.zeroClipboardButtonReadySelectPrompt());
        textBoxWidget.getParent().getElement().appendChild(copyToClipboardButton);
    }

    /**
     * Create copy to clipboard button.
     *
     * @param textBox
     * @param image
     * @param className
     * @param readyCopyPrompt
     * @param afterCopyPrompt
     * @param copyErrorPrompt
     * @param readySelectPrompt
     */
    private native Element createCopyToClipboardButton(Element textBox, Element image, String className, String readyCopyPrompt,
                                                       String afterCopyPrompt, String copyErrorPrompt, String readySelectPrompt) /*-{
        var button = document.createElement('div');
        var tooltip = document.createElement('span');
        button.setAttribute('class', className);
        button.appendChild(image);
        button.appendChild(tooltip);
        if (typeof $wnd.ZeroClipboard !== 'undefined') {
            button.onmouseout = function () {
                button.setAttribute('class', className);
            }
            var client = new $wnd.ZeroClipboard(button);
            client.on('ready', function (event) {
                tooltip.innerHTML = readyCopyPrompt;
                client.on('copy', function (event) {
                    event.clipboardData.setData('text/plain', textBox.value);
                });
                client.on('aftercopy', function (event) {
                    tooltip.innerHTML = afterCopyPrompt;
                    client.unclip();
                    setTimeout(function () {
                        client.clip(button);
                        tooltip.innerHTML = readyCopyPrompt;
                    }, 3000);
                });
            });
            client.on('error', function (event) {
                console.log('ZeroClipboard error of type "' + event.name + '": ' + event.message);
                tooltip.innerHTML = copyErrorPrompt;
                ZeroClipboard.destroy();
                setTimeout(function () {
                    tooltip.innerHTML = readyCopyPrompt;
                }, 5000);
            });
        }
        else {
            tooltip.innerHTML = readySelectPrompt;
            button.onclick = function () {
                textBox.select();
            };
        }
        return button;
    }-*/;
}