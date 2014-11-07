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
import com.codenvy.ide.api.projectimporter.basepage.ImporterBasePageListener;
import com.codenvy.ide.api.projectimporter.basepage.ImporterBasePagePresenter;
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
public class GitImporterPagePresenter implements ImporterPagePresenter, ImporterBasePageListener {

    private static final RegExp NAME_PATTERN    = RegExp.compile("^[A-Za-z0-9_-]*$");
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

    private GitLocalizationConstant   locale;
    private ImporterBasePagePresenter basePagePresenter;
    private GitImporterPageView       view;
    private WizardContext             wizardContext;
    private Wizard.UpdateDelegate     updateDelegate;

    @Inject
    public GitImporterPagePresenter(GitImporterPageView view,
                                    GitLocalizationConstant locale,
                                    ImporterBasePagePresenter basePagePresenter) {
        this.view = view;
        this.locale = locale;
        this.basePagePresenter = basePagePresenter;
        this.basePagePresenter.go(view.getBasePagePanel());
        this.basePagePresenter.setListener(this);
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public String getId() {
        return "git";
    }

    /** {@inheritDoc} */
    @Override
    public void disableInputs() {
        basePagePresenter.setInputsEnableState(false);
    }

    /** {@inheritDoc} */
    @Override
    public void enableInputs() {
        basePagePresenter.setInputsEnableState(true);
    }

    /** {@inheritDoc} */
    @Override
    public void setContext(@Nonnull WizardContext wizardContext) {
        this.wizardContext = wizardContext;
    }

    /** {@inheritDoc} */
    @Override
    public void setProjectWizardDelegate(@Nonnull Wizard.UpdateDelegate updateDelegate) {
        this.updateDelegate = updateDelegate;
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        basePagePresenter.reset();
    }

    /** {@inheritDoc} */
    @Override
    public void projectNameChanged(@Nonnull String name) {
        if (name.isEmpty()) {
            wizardContext.removeData(ProjectWizard.PROJECT_NAME);
        } else if (NAME_PATTERN.test(name)) {
            wizardContext.putData(ProjectWizard.PROJECT_NAME, name);
            basePagePresenter.hideNameError();
        } else {
            wizardContext.removeData(ProjectWizard.PROJECT_NAME);
            basePagePresenter.showNameError();
        }
        updateDelegate.updateControls();
    }

    /** {@inheritDoc} */
    @Override
    public void projectUrlChanged(@Nonnull String url) {
        if (!isGitUrlCorrect(url)) {
            wizardContext.removeData(ImportProjectWizard.PROJECT_URL);
        } else {
            wizardContext.putData(ImportProjectWizard.PROJECT_URL, url);
            basePagePresenter.hideUrlError();

            String projectName = basePagePresenter.getProjectName();
            if (projectName.isEmpty()) {
                projectName = parseUri(url);
                basePagePresenter.setProjectName(projectName);
                projectNameChanged(projectName);
            }
        }
        updateDelegate.updateControls();
    }

    /** {@inheritDoc} */
    @Override
    public void projectDescriptionChanged(@Nonnull String projectDescriptionValue) {
        wizardContext.putData(ProjectWizard.PROJECT_DESCRIPTION, projectDescriptionValue);
    }

    /** {@inheritDoc} */
    @Override
    public void projectVisibilityChanged(boolean aPublic) {
        wizardContext.putData(ProjectWizard.PROJECT_VISIBILITY, aPublic);
    }

    /** {@inheritDoc} */
    @Override
    public void go(@Nonnull AcceptsOneWidget container) {
        clear();
        ProjectImporterDescriptor projectImporter = wizardContext.getData(ImportProjectWizard.PROJECT_IMPORTER);
        if(projectImporter != null) {
            basePagePresenter.setImporterDescription(projectImporter.getDescription());
        }

        basePagePresenter.setInputsEnableState(true);
        container.setWidget(view.asWidget());
        basePagePresenter.focusInUrlInput();
    }

    /** Gets project name from uri. */
    private String parseUri(@Nonnull String uri) {
        String result;
        int indexStartProjectName = uri.lastIndexOf("/") + 1;
        int indexFinishProjectName = uri.indexOf(".", indexStartProjectName);
        if (indexStartProjectName != 0 && indexFinishProjectName != (-1)) {
            result = uri.substring(indexStartProjectName, indexFinishProjectName);
        } else if (indexStartProjectName != 0) {
            result = uri.substring(indexStartProjectName);
        } else {
            result = "";
        }
        return result;
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
            basePagePresenter.showUrlError(locale.importProjectMessageStartWithWhiteSpace());
            return false;
        }

        if (SCP_LIKE_SYNTAX.test(url) && REPO_NAME.test(url)) {
            return true;
        } else if (SCP_LIKE_SYNTAX.test(url) && !REPO_NAME.test(url)) {
            basePagePresenter.showUrlError(locale.importProjectMessageNameRepoIncorrect());
            return false;
        }

        if (!PROTOCOL.test(url)) {
            basePagePresenter.showUrlError(locale.importProjectMessageProtocolIncorrect());
            return false;
        }
        if (!(HOST1.test(url) || HOST2.test(url))) {
            basePagePresenter.showUrlError(locale.importProjectMessageHostIncorrect());
            return false;
        }
        if (!(REPO_NAME.test(url))) {
            basePagePresenter.showUrlError(locale.importProjectMessageNameRepoIncorrect());
            return false;
        }
        basePagePresenter.hideUrlError();
        return true;
    }

}
