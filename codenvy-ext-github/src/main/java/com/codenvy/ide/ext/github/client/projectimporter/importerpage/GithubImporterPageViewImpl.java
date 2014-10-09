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
import com.codenvy.ide.api.projectimporter.basepage.ImporterBasePageView;
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
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
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

    private ImporterBasePageView importerBasePageView;
    private ActionDelegate       delegate;

    @UiField
    FlowPanel              basePagePanel;
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
                                      ImporterBasePageView importerBasePageView,
                                      GithubImporterPageViewImplUiBinder uiBinder) {
        this.resources = resources;
        this.locale = locale;
        this.importerBasePageView = importerBasePageView;
        createRepositoriesTable(ideResources);
        initWidget(uiBinder.createAndBindUi(this));
        closeGithubPanel();

        basePagePanel.add(importerBasePageView);
        loadRepo.addStyleName(ideResources.Css().buttonLoader());
        loadRepo.sinkEvents(Event.ONCLICK);
        loadRepo.addHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                delegate.onLoadRepoClicked();

            }
        }, ClickEvent.getType());
    }

    /** Creates table what contains list of available repositories.
     * @param ideResources*/
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

    @Override
    public void reset() {
        importerBasePageView.reset();
        githubPanel.removeFromParent();
    }

    @Override
    public void showNameError() {
        importerBasePageView.showNameError();
    }

    @Override
    public void hideNameError() {
        importerBasePageView.hideNameError();
    }

    @Override
    public void showUrlError(String message) {
        importerBasePageView.showUrlError(message);
    }

    @Override
    public void hideUrlError() {
        importerBasePageView.hideUrlError();
    }

    @Override
    public void setImporterDescription(String text) {
        importerBasePageView.setImporterDescription(text);
    }

    @Override
    public String getProjectName() {
        return importerBasePageView.getProjectName();
    }

    @Override
    public void setProjectName(String projectName) {
        importerBasePageView.setProjectName(projectName);
    }

    @Override
    public void focusInUrlInput() {
        importerBasePageView.focusInUrlInput();
    }

    @Override
    public void setInputsEnableState(boolean isEnabled) {
        importerBasePageView.setInputsEnableState(isEnabled);
    }

    @Override
    public void setDelegate(ImporterBasePageView.ActionDelegate delegate) {

    }

    @Override
    public Widget asWidget() {
        return this;
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        importerBasePageView.setDelegate(delegate);
        this.delegate = delegate;
    }

    @Override
    public void setRepositories(@Nonnull Array<ProjectData> repositories) {
        // Wraps Array in java.util.List
        List<ProjectData> list = new ArrayList<ProjectData>();
        for (int i = 0; i < repositories.size(); i++) {
            list.add(repositories.get(i));
        }
        this.repositories.setRowData(list);
    }

    @Override
    public void setProjectUrl(String url) {
        importerBasePageView.setProjectUrl(url);
    }

    @Nonnull
    @Override
    public String getAccountName() {
        int index = accountName.getSelectedIndex();
        return index != -1 ? accountName.getItemText(index) : "";
    }

    @Override
    public void setAccountNames(@Nonnull Array<String> names) {
        this.accountName.clear();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            this.accountName.addItem(name);
        }
    }

    @Override
    public void closeGithubPanel() {
        githubPanel.removeFromParent();
    }

    @Override
    public void showGithubPanel() {
        bottomPanel.add(githubPanel);
    }

    @Override
    public void setLoaderVisibility(boolean isVisible) {
        if (isVisible) {
            importerBasePageView.setInputsEnableState(false);
            loadRepo.setHTML("<i></i>");
            loadRepo.setEnabled(false);
        } else {
            importerBasePageView.setInputsEnableState(true);
            loadRepo.setText("Load Repo");
            loadRepo.setEnabled(true);
        }
    }

    @UiHandler("accountName")
    public void onAccountChange(ChangeEvent event) {
        delegate.onAccountChanged();
    }
}
