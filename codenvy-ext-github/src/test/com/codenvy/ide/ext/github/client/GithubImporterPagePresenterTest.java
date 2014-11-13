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
package com.codenvy.ide.ext.github.client;

import com.codenvy.api.user.gwt.client.UserServiceClient;
import com.codenvy.api.user.shared.dto.UserDescriptor;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.collections.Array;
import com.codenvy.ide.collections.Collections;
import com.codenvy.ide.collections.StringMap;
import com.codenvy.ide.dto.DtoFactory;
import com.codenvy.ide.ext.git.client.GitLocalizationConstant;
import com.codenvy.ide.ext.github.client.projectimporter.importerpage.GithubImporterPagePresenter;
import com.codenvy.ide.ext.github.client.projectimporter.importerpage.GithubImporterPageView;
import com.codenvy.ide.ext.github.shared.GitHubRepository;
import com.codenvy.ide.ext.github.shared.GitHubRepositoryList;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.codenvy.ide.ui.dialogs.DialogFactory;
import com.google.web.bindery.event.shared.EventBus;
import com.googlecode.gwt.test.utils.GwtReflectionUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Method;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Vitaly Parfonov
 */
@RunWith(MockitoJUnitRunner.class)
public class GithubImporterPagePresenterTest {

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<UserDescriptor>> asyncRequestCallbackUserDescriptorCaptor;

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<StringMap<Array<GitHubRepository>>>> asyncRequestCallbackRepoListCaptor;

    @Mock
    private NotificationManager     notificationManager;
    @Mock
    private UserServiceClient       userServiceClient;
    @Mock
    private GitHubClientService     gitHubClientService;
    @Mock
    private DtoUnmarshallerFactory  dtoUnmarshallerFactory;
    @Mock
    private DtoFactory              dtoFactory;
    @Mock
    private DialogFactory           dialogFactory;
    @Mock
    private EventBus                eventBus;
    @Mock
    private GitLocalizationConstant locale;
    @Mock
    private GithubImporterPageView  view;
    @Mock
    private UserDescriptor          userDescriptor;


    @InjectMocks
    private GithubImporterPagePresenter presenter;

    @Test
    public void delegateShouldBeSet() throws Exception {
        verify(view).setDelegate(presenter);
    }


    @Test
    public void userServiceShouldBeCalled() throws Exception {
        presenter.onLoadRepoClicked();
        InOrder inOrder = inOrder(view, userServiceClient);

        inOrder.verify(view).reset();
        inOrder.verify(view).setLoaderVisibility(true);
        inOrder.verify(userServiceClient).getCurrentUser(asyncRequestCallbackUserDescriptorCaptor.capture());
    }

    @Test
    public void userServiceAsyncCallbackIsSuccess() throws Exception {
        presenter.onLoadRepoClicked();
        verify(userServiceClient).getCurrentUser(asyncRequestCallbackUserDescriptorCaptor.capture());
        AsyncRequestCallback<UserDescriptor> callback = asyncRequestCallbackUserDescriptorCaptor.getValue();

        //noinspection NonJREEmulationClassesInClientCode
        Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
        onSuccess.invoke(callback, userDescriptor);

        verify(view, times(2)).setLoaderVisibility(true);
        verify(gitHubClientService).getAllRepositories(asyncRequestCallbackRepoListCaptor.capture());

        AsyncRequestCallback<StringMap<Array<GitHubRepository>>> callback2 = asyncRequestCallbackRepoListCaptor.getValue();

        StringMap<Array<GitHubRepository>> map = Collections.createStringMap();
        Array<GitHubRepository> list = Collections.createArray();
        list.add(mock(GitHubRepository.class));
        map.put(anyString(), list);

        //noinspection NonJREEmulationClassesInClientCode
        Method onSuccess2 = GwtReflectionUtils.getMethod(callback2.getClass(), "onSuccess");
        onSuccess2.invoke(callback2, map);
        verify(view).setLoaderVisibility(false);
    }

}
