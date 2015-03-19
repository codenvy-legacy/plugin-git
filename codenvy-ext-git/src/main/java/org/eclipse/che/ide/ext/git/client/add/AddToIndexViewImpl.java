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
package org.eclipse.che.ide.ext.git.client.add;

import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.ide.ext.git.client.GitResources;
import org.eclipse.che.ide.ui.window.Window;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;
import javax.annotation.Nonnull;
/**
 * The implementation of {@link AddToIndexView}.
 *
 * @author <a href="mailto:aplotnikov@codenvy.com">Andrey Plotnikov</a>
 */
@Singleton
public class AddToIndexViewImpl extends Window implements AddToIndexView {
    interface AddToIndexViewImplUiBinder extends UiBinder<Widget, AddToIndexViewImpl> {
    }

    private static AddToIndexViewImplUiBinder ourUiBinder = GWT.create(AddToIndexViewImplUiBinder.class);

    @UiField
    HTML message;
    @UiField
    TextArea items;
    @UiField
    CheckBox update;
    Button   btnAdd;
    Button   btnCancel;
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
    protected AddToIndexViewImpl(GitResources resources, GitLocalizationConstant locale) {
        this.res = resources;
        this.locale = locale;

        ensureDebugId("git-addToIndex-window");
        setTitle(locale.addToIndexTitle());
        setWidget(ourUiBinder.createAndBindUi(this));

        btnAdd = createButton(locale.buttonAdd(), "git-addToIndex-btnAdd", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onAddClicked();
            }
        });
        btnAdd.addStyleName(Window.resources.centerPanelCss().blueButton());
        getFooter().add(btnAdd);

        btnCancel = createButton(locale.buttonCancel(), "git-addToIndex-btnCancel", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onCancelClicked();
            }
        });
        getFooter().add(btnCancel);
    }

    /** {@inheritDoc} */
    @Override
    public void setMessage(@Nonnull String message, @Nonnull List<String> items) {
        this.message.setHTML(message);
        if (items == null || items.isEmpty()) {
            this.items.setVisible(false);
            this.items.setText("");
        } else {
            this.items.setVisible(true);
            final StringBuilder sb = new StringBuilder();
            String toAppend = "";
            for (String item : items) {
                sb.append(toAppend);
                toAppend = "\n";
                sb.append(item);
            }
            this.items.setText(sb.toString());
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUpdated() {
        return update.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public void setUpdated(boolean isUpdated) {
        update.setValue(isUpdated);
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
}