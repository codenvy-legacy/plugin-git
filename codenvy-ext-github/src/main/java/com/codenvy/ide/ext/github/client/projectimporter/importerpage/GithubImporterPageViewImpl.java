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
package com.codenvy.ide.ext.github.client.projectimporter.importerpage;

import com.codenvy.ide.Resources;
import com.codenvy.ide.collections.Array;
import com.codenvy.ide.ext.github.client.GitHubLocalizationConstant;
import com.codenvy.ide.ext.github.client.GitHubResources;
import com.codenvy.ide.ext.github.client.load.ProjectData;
import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman Nikitenko
 */
public class GithubImporterPageViewImpl extends Composite implements GithubImporterPageView {

    interface GithubImporterPageViewImplUiBinder extends UiBinder<DockLayoutPanel, GithubImporterPageViewImpl> {
    }

    private ActionDelegate delegate;

    @UiField
    SimplePanel            basePagePanel;
    @UiField
    FlowPanel              bottomPanel;
    @UiField
    DockLayoutPanel        githubPanel;
    @UiField
    Button                 loadRepo;
    @UiField
    ListBox                accountName;
    @UiField(provided = true)
    CellTable<ProjectData> repositories;
    @UiField(provided = true)
    final GitHubResources            resources;
    @UiField(provided = true)
    final GitHubLocalizationConstant locale;

    @Inject
    public GithubImporterPageViewImpl(GitHubResources resources,
                                      GitHubLocalizationConstant locale,
                                      Resources ideResources,
                                      GithubImporterPageViewImplUiBinder uiBinder) {
        this.resources = resources;
        this.locale = locale;
        createRepositoriesTable(ideResources);
        initWidget(uiBinder.createAndBindUi(this));
        closeGithubPanel();

        loadRepo.addStyleName(ideResources.Css().buttonLoader());
        loadRepo.sinkEvents(Event.ONCLICK);
        loadRepo.addHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                delegate.onLoadRepoClicked();

            }
        }, ClickEvent.getType());
    }

    /**
     * Creates table what contains list of available repositories.
     *
     * @param ideResources
     */
    private void createRepositoriesTable(Resources ideResources) {
        repositories = new CellTable<ProjectData>(15, ideResources);

        Column<ProjectData, ImageResource> iconColumn = new Column<ProjectData, ImageResource>(new ImageResourceCell()) {
            @Override
            public ImageResource getValue(ProjectData item) {
                return resources.project();
            }
        };

        Column<ProjectData, SafeHtml> repositoryColumn = new Column<ProjectData, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(final ProjectData item) {
                return new SafeHtml() {
                    public String asString() {
                        return item.getName();
                    }
                };
            }
        };

        Column<ProjectData, SafeHtml> descriptionColumn = new Column<ProjectData, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(final ProjectData item) {
                return new SafeHtml() {
                    public String asString() {
                        return "<span>" + item.getDescription() + "</span>";
                    }
                };
            }
        };

        repositories.addColumn(iconColumn, SafeHtmlUtils.fromSafeConstant("<br/>"));
        repositories.setColumnWidth(iconColumn, 28, Style.Unit.PX);

        repositories.addColumn(repositoryColumn, locale.samplesListRepositoryColumn());
        repositories.addColumn(descriptionColumn, locale.samplesListDescriptionColumn());

        // don't show loading indicator
        repositories.setLoadingIndicator(null);

        final SingleSelectionModel<ProjectData> selectionModel = new SingleSelectionModel<ProjectData>();
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                ProjectData selectedObject = selectionModel.getSelectedObject();
                delegate.onRepositorySelected(selectedObject);
            }
        });
        repositories.setSelectionModel(selectionModel);
    }

    /** {@inheritDoc} */
    @Override
    public void reset() {
        githubPanel.removeFromParent();
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    public void setRepositories(@Nonnull Array<ProjectData> repositories) {
        // Wraps Array in java.util.List
        List<ProjectData> list = new ArrayList<ProjectData>();
        for (int i = 0; i < repositories.size(); i++) {
            list.add(repositories.get(i));
        }
        this.repositories.setRowData(list);
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public String getAccountName() {
        int index = accountName.getSelectedIndex();
        return index != -1 ? accountName.getItemText(index) : "";
    }

    /** {@inheritDoc} */
    @Override
    public void setAccountNames(@Nonnull Array<String> names) {
        this.accountName.clear();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            this.accountName.addItem(name);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void closeGithubPanel() {
        githubPanel.removeFromParent();
    }

    /** {@inheritDoc} */
    @Override
    public void showGithubPanel() {
        bottomPanel.add(githubPanel);
    }

    /** {@inheritDoc} */
    @Override
    public void setLoaderVisibility(boolean isVisible) {
        if (isVisible) {
            loadRepo.setHTML("<i></i>");
            loadRepo.setEnabled(false);
        } else {
            loadRepo.setText("Load Repo");
            loadRepo.setEnabled(true);
        }
    }

    /** {@inheritDoc} */
    @Override
    public AcceptsOneWidget getBasePagePanel() {
        return basePagePanel;
    }

    @UiHandler("accountName")
    public void onAccountChange(ChangeEvent event) {
        delegate.onAccountChanged();
    }
}
