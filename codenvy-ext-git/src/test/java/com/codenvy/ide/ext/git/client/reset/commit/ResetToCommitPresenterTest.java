import com.codenvy.ide.api.event.RefreshProjectTreeEvent;
import com.google.web.bindery.event.shared.Event;
        presenter = new ResetToCommitPresenter(view,
                                               service,
                                               constant,
                                               eventBus,
                                               editorAgent,
                                               appContext,
                                               notificationManager,
    public void testOnResetClickedWhenFileIsNotExistInCommitToReset()
        // Only in the cases of <code>ResetRequest.ResetType.HARD</code>  or <code>ResetRequest.ResetType.MERGE</code>
        // must change the workdir
        verify(selectedRevision).getId();
        verify(appContext).getCurrentProject();
        verify(eventBus, times(2)).fireEvent((RefreshProjectTreeEvent)anyObject());
    public void testOnResetClickedWhenFileIsChangedInCommitToReset()
            throws Exception {
        // Only in the cases of <code>ResetRequest.ResetType.HARD</code>  or <code>ResetRequest.ResetType.MERGE</code>
        // must change the workdir
        when(view.isMixMode()).thenReturn(false);
        when(view.isHardMode()).thenReturn(true);

                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[3];
                Method onSuccess = GwtReflectionUtils.getMethod(callback.getClass(), "onSuccess");
                onSuccess.invoke(callback, (Void)null);
        }).when(service)
          .reset((ProjectDescriptor)anyObject(), anyString(), (ResetRequest.ResetType)anyObject(), (AsyncRequestCallback<Void>)anyObject());
        verify(service).reset((ProjectDescriptor)anyObject(), eq(PROJECT_PATH), eq(HARD), (AsyncRequestCallback<Void>)anyObject());
        verify(eventBus, times(2)).fireEvent((FileEvent)anyObject());
        verify(partPresenter).getEditorInput();
        verify(notificationManager).showNotification((Notification)anyObject());
    public void testOnResetClickedWhenResetRequestIsFailed() throws Exception {
        verify(selectedRevision).getId();
        verify(appContext).getCurrentProject();
        verify(eventBus, never()).fireEvent((Event<?>)anyObject());