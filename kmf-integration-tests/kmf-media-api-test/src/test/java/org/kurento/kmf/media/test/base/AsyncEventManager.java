package org.kurento.kmf.media.test.base;

import org.kurento.kmf.media.events.MediaEvent;
import org.kurento.kmf.media.events.MediaEventListener;

public class AsyncEventManager<T extends MediaEvent> extends AsyncManager<T> {

	public AsyncEventManager(String message) {
		super(message);
	}

	public MediaEventListener<T> getMediaEventListener() {

		return new MediaEventListener<T>() {
			@Override
			public void onEvent(T event) {
				addResult(event);
			}
		};
	}
}
