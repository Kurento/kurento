package com.kurento.kmf.media.events.internal;

import com.kurento.kmf.media.events.MediaEvent;
import com.kurento.kmf.media.events.MediaEventListener;

public abstract class AbstractMediaEventListener<T extends MediaEvent>
		implements MediaEventListener<T> {

	@Override
	public abstract void onEvent(T event);

	@SuppressWarnings("unchecked")
	public void internalOnEvent(MediaEvent event) {
		// TODO try to replace this internal
		// TODO throw in different thread, in order not to block due to user
		// implementation
		onEvent((T) event);
	}
}
