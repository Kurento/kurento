package org.kurento.client.test.util;

import org.kurento.client.events.MediaEvent;
import org.kurento.client.events.MediaEventListener;

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
