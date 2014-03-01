package com.kurento.kmf.media;

import com.kurento.kmf.media.events.ErrorEvent;
import com.kurento.kmf.media.events.MediaEventListener;

public interface MediaObject {

	MediaPipeline getMediaPipeline();

	void getMediaPipeline(Continuation<MediaPipeline> cont);

	MediaObject getParent();

	void getParent(Continuation<MediaObject> cont);

	ListenerRegistration addErrorListener(
			MediaEventListener<ErrorEvent> listener);

	void addErrorListener(MediaEventListener<ErrorEvent> listener,
			Continuation<ListenerRegistration> cont);

	void release();

	void release(Continuation<Void> continuation);
}
