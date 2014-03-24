package com.kurento.kmf.media;

import com.kurento.kmf.media.events.ErrorEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.tool.rom.RemoteClass;

@RemoteClass
public interface MediaObject {

	MediaPipeline getMediaPipeline();

	void getMediaPipeline(Continuation<MediaPipeline> cont);

	MediaObject getParent();

	void getParent(Continuation<MediaObject> cont);

	ListenerRegistration addErrorListener(
			MediaEventListener<ErrorEvent> listener);

	void addErrorListener(MediaEventListener<ErrorEvent> listener,
			Continuation<ListenerRegistration> cont);

	/**
	 * 
	 * Explicitly release a media object form memory. All of its children will
	 * also be released.
	 * 
	 **/
	void release();

	/**
	 * 
	 * Explicitly release a media object form memory. All of its children will
	 * also be released. Asynchronous call.
	 * 
	 * @param continuation
	 *            {@link #onSuccess(void)} will be called when the actions
	 *            complete. {@link #onError} will be called if there is an
	 *            exception.
	 * 
	 **/
	void release(Continuation<Void> continuation);

}
