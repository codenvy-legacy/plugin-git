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
package com.codenvy.ide.ext.git.client.projectimporter.importerpage;

import com.codenvy.api.project.shared.dto.ProjectImporterDescriptor;
import com.codenvy.ide.api.projecttype.wizard.ImportProjectWizard;
import com.codenvy.ide.api.projecttype.wizard.ProjectWizard;
import com.codenvy.ide.api.wizard.Wizard;
import com.codenvy.ide.api.wizard.WizardContext;
import com.codenvy.ide.ext.git.client.GitLocalizationConstant;
import com.codenvy.ide.ext.git.client.projectImporter.importerpage.GitImporterPagePresenter;
import com.codenvy.ide.ext.git.client.projectImporter.importerpage.GitImporterPageView;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link GitImporterPagePresenter} functionality.
 *
 * @author Roman Nikitenko
 */
@RunWith(MockitoJUnitRunner.class)
public class GitImporterPagePresenterTest {
    private WizardContext            wizardContext;
    private Wizard.UpdateDelegate    updateDelegate;
    @Mock
    private GitImporterPageView      view;
    @Mock
    private GitLocalizationConstant  locale;
    @InjectMocks
    private GitImporterPagePresenter presenter;

    @Before
    public void setUp() {
        wizardContext = mock(WizardContext.class);
        updateDelegate = mock(Wizard.UpdateDelegate.class);
        presenter.setContext(wizardContext);
        presenter.setProjectWizardDelegate(updateDelegate);
    }

    @Test
    public void testGo() {
        String importerDescription = "description";
        AcceptsOneWidget container = mock(AcceptsOneWidget.class);
        ProjectImporterDescriptor projectImporter = mock(ProjectImporterDescriptor.class);
        when(wizardContext.getData(ImportProjectWizard.PROJECT_IMPORTER)).thenReturn(projectImporter);
        when(projectImporter.getDescription()).thenReturn(importerDescription);

        presenter.go(container);

        verify(view).reset();
        verify(wizardContext).getData(eq(ImportProjectWizard.PROJECT_IMPORTER));
        verify(view).setImporterDescription(eq(importerDescription));
        verify(view).setInputsEnableState(eq(true));
        verify(container).setWidget(eq(view));
        verify(view).focusInUrlInput();
    }

    @Test
    public void projectUrlStartWithWhiteSpaceEnteredTest() {
        String incorrectUrl = " https://github.com/codenvy/ide.git";

        presenter.projectUrlChanged(incorrectUrl);

        verify(view).showUrlError(eq(locale.importProjectMessageStartWithWhiteSpace()));
        verify(wizardContext).removeData(eq(ImportProjectWizard.PROJECT_URL));
        verify(wizardContext, never()).putData(eq(ImportProjectWizard.PROJECT_URL), anyString());
        verify(view, never()).setProjectName(anyString());
        verify(updateDelegate).updateControls();
    }

    @Test
    public void testUrlMatchScpLikeSyntax() {
        // test for url with an alternative scp-like syntax: [user@]host.xz:path/to/repo.git/
        String correctUrl = "host.xz:path/to/repo.git";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testUrlWithoutUsername() {
        String correctUrl = "git@hostname.com:projectName.git";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testSshUriWithHostBetweenDoubleSlashAndSlash() {
        //Check for type uri which start with ssh:// and has host between // and /
        String correctUrl = "ssh://host.com/some/path";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testSshUriWithHostBetweenDoubleSlashAndColon() {
        //Check for type uri with host between // and :
        String correctUrl = "ssh://host.com:port/some/path";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testGitUriWithHostBetweenDoubleSlashAndSlash() {
        //Check for type uri which start with git:// and has host between // and /
        String correctUrl = "git://host.com/user/repo";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testSshUriWithHostBetweenAtAndColon() {
        //Check for type uri with host between @ and :
        String correctUrl = "user@host.com:login/repo";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void testSshUriWithHostBetweenAtAndSlash() {
        //Check for type uri with host between @ and /
        String correctUrl = "ssh://user@host.com/some/path";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verifyInvocationsForCorrectUrl(correctUrl);
    }

    @Test
    public void projectUrlWithIncorrectProtocolEnteredTest() {
        String correctUrl = "htps://github.com/codenvy/ide.git";
        when(view.getProjectName()).thenReturn("");

        presenter.projectUrlChanged(correctUrl);

        verify(view).showUrlError(eq(locale.importProjectMessageProtocolIncorrect()));
        verify(wizardContext).removeData(eq(ImportProjectWizard.PROJECT_URL));
        verify(wizardContext, never()).putData(eq(ImportProjectWizard.PROJECT_URL), anyString());
        verify(view, never()).setProjectName(anyString());
        verify(updateDelegate).updateControls();
    }

    @Test
    public void correctProjectNameEnteredTest() {
        String correctName = "angularjs";

        presenter.projectNameChanged(correctName);

        verify(wizardContext).putData(eq(ProjectWizard.PROJECT_NAME), eq(correctName));
        verify(view).hideNameError();
        verify(view, never()).showNameError();
        verify(updateDelegate).updateControls();
    }

    @Test
    public void correctProjectNameWithPointEnteredTest() {
        String correctName = "Test.project..ForCodenvy";

        presenter.projectNameChanged(correctName);

        verify(wizardContext).putData(eq(ProjectWizard.PROJECT_NAME), eq(correctName));
        verify(view).hideNameError();
        verify(view, never()).showNameError();
        verify(updateDelegate).updateControls();
    }

    @Test
    public void emptyProjectNameEnteredTest() {
        String emptyName = "";

        presenter.projectNameChanged(emptyName);

        verify(wizardContext, never()).putData(eq(ProjectWizard.PROJECT_NAME), anyString());
        verify(wizardContext).removeData(eq(ProjectWizard.PROJECT_NAME));
        verify(updateDelegate).updateControls();
    }

    @Test
    public void incorrectProjectNameEnteredTest() {
        String incorrectName = "angularjs+";

        presenter.projectNameChanged(incorrectName);

        verify(wizardContext, never()).putData(eq(ProjectWizard.PROJECT_NAME), anyString());
        verify(wizardContext).removeData(eq(ProjectWizard.PROJECT_NAME));
        verify(view).showNameError();
        verify(updateDelegate).updateControls();
    }

    @Test
    public void projectDescriptionChangedTest() {
        String description = "description";
        presenter.projectDescriptionChanged(description);

        verify(wizardContext).putData(eq(ProjectWizard.PROJECT_DESCRIPTION), eq(description));
    }

    @Test
    public void projectVisibilityChangedTest() {
        presenter.projectVisibilityChanged(true);

        verify(wizardContext).putData(eq(ProjectWizard.PROJECT_VISIBILITY), eq(true));
    }

    private void verifyInvocationsForCorrectUrl(String correctUrl) {
        verify(view, never()).showUrlError(anyString());
        verify(wizardContext).putData(eq(ImportProjectWizard.PROJECT_URL), eq(correctUrl));
        verify(view).hideUrlError();
        verify(view).setProjectName(anyString());
        verify(updateDelegate, times(2)).updateControls();
    }

}
