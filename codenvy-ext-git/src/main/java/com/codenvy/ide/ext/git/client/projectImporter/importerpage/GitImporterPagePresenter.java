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

import com.codenvy.api.project.shared.dto.ProjectImporterDescriptor;
import com.codenvy.ide.api.projectimporter.ImporterPagePresenter;
import com.codenvy.ide.api.projecttype.wizard.ImportProjectWizard;
import com.codenvy.ide.api.projecttype.wizard.ProjectWizard;
import com.codenvy.ide.api.wizard.Wizard;
import com.codenvy.ide.api.wizard.WizardContext;
import com.codenvy.ide.ext.git.client.GitLocalizationConstant;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

import javax.annotation.Nonnull;

/**
 * @author Roman Nikitenko
 */
public class GitImporterPagePresenter implements ImporterPagePresenter, GitImporterPageView.ActionDelegate {

    private static final RegExp NAME_PATTERN    = RegExp.compile("^[A-Za-z0-9_\\-\\.]*$");
    // An alternative scp-like syntax: [user@]host.xz:path/to/repo.git/
    private static final RegExp SCP_LIKE_SYNTAX = RegExp.compile("([A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-:]+)+:");
    // the transport protocol
    private static final RegExp PROTOCOL        = RegExp.compile("((http|https|git|ssh|ftp|ftps)://)");
    // the address of the remote server between // and /
    private static final RegExp HOST1           = RegExp.compile("//([A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-:]+)+/");
    // the address of the remote server between @ and : or /
    private static final RegExp HOST2           = RegExp.compile("@([A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-:]+)+[:/]");
    // the repository name
    private static final RegExp REPO_NAME       = RegExp.compile("/[A-Za-z0-9_.\\-]+$");
    // start with white space
    private static final RegExp WHITE_SPACE     = RegExp.compile("^\\s");

    private GitLocalizationConstant locale;
    private GitImporterPageView     view;
    private WizardContext           wizardContext;
    private Wizard.UpdateDelegate   updateDelegate;

    @Inject
    public GitImporterPagePresenter(GitImporterPageView view,
                                    GitLocalizationConstant locale) {
        this.view = view;
        this.view.setDelegate(this);
        this.locale = locale;
    }

    @Nonnull
    @Override
    public String getId() {
        return "git";
    }

    @Override
    public void disableInputs() {
        view.setInputsEnableState(false);
    }

    @Override
    public void enableInputs() {
        view.setInputsEnableState(true);
    }

    @Override
    public void setContext(@Nonnull WizardContext wizardContext) {
        this.wizardContext = wizardContext;
    }

    @Override
    public void setProjectWizardDelegate(@Nonnull Wizard.UpdateDelegate updateDelegate) {
        this.updateDelegate = updateDelegate;
    }

    @Override
    public void clear() {
        view.reset();
    }

    @Override
    public void projectNameChanged(@Nonnull String name) {
        if (name.isEmpty()) {
            wizardContext.removeData(ProjectWizard.PROJECT_NAME);
        } else if (NAME_PATTERN.test(name)) {
            wizardContext.putData(ProjectWizard.PROJECT_NAME, name);
            view.hideNameError();
        } else {
            wizardContext.removeData(ProjectWizard.PROJECT_NAME);
            view.showNameError();
        }
        updateDelegate.updateControls();
    }

    @Override
    public void projectUrlChanged(@Nonnull String url) {
        if (!isGitUrlCorrect(url)) {
            wizardContext.removeData(ImportProjectWizard.PROJECT_URL);
        } else {
            wizardContext.putData(ImportProjectWizard.PROJECT_URL, url);
            String projectName = view.getProjectName();
            if (projectName.isEmpty()) {
                projectName = parseUri(url);
                view.setProjectName(projectName);
                projectNameChanged(projectName);
            }
        }
        updateDelegate.updateControls();
    }

    @Override
    public void projectDescriptionChanged(@Nonnull String projectDescriptionValue) {
        wizardContext.putData(ProjectWizard.PROJECT_DESCRIPTION, projectDescriptionValue);
    }

    @Override
    public void projectVisibilityChanged(boolean aPublic) {
        wizardContext.putData(ProjectWizard.PROJECT_VISIBILITY, aPublic);
    }

    @Override
    public void go(@Nonnull AcceptsOneWidget container) {
        clear();
        ProjectImporterDescriptor projectImporter = wizardContext.getData(ImportProjectWizard.PROJECT_IMPORTER);
        if (projectImporter != null) {
            view.setImporterDescription(projectImporter.getDescription());
        }

        view.setInputsEnableState(true);
        container.setWidget(view);
        view.focusInUrlInput();
    }

    /** Gets project name from uri. */
    private String parseUri(@Nonnull String uri) {
        int indexFinishProjectName = uri.lastIndexOf(".");
        int indexStartProjectName = uri.lastIndexOf("/") != -1 ? uri.lastIndexOf("/") + 1 : (uri.lastIndexOf(":") + 1);

        if (indexStartProjectName != 0 && indexStartProjectName < indexFinishProjectName) {
            return uri.substring(indexStartProjectName, indexFinishProjectName);
        }
        if (indexStartProjectName != 0) {
            return uri.substring(indexStartProjectName);
        }
        return "";
    }

    /**
     * Validate url
     *
     * @param url
     *         url for validate
     * @return <code>true</code> if url is correct
     */
    private boolean isGitUrlCorrect(@Nonnull String url) {
        if (WHITE_SPACE.test(url)) {
            view.showUrlError(locale.importProjectMessageStartWithWhiteSpace());
            return false;
        }
        if (SCP_LIKE_SYNTAX.test(url)) {
            view.hideUrlError();
            return true;
        }
        if (!PROTOCOL.test(url)) {
            view.showUrlError(locale.importProjectMessageProtocolIncorrect());
            return false;
        }
        if (!(HOST1.test(url) || HOST2.test(url))) {
            view.showUrlError(locale.importProjectMessageHostIncorrect());
            return false;
        }
        if (!(REPO_NAME.test(url))) {
            view.showUrlError(locale.importProjectMessageNameRepoIncorrect());
            return false;
        }
        view.hideUrlError();
        return true;
    }

}
