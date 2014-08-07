package org.kurento.client.test.util;

import org.kurento.client.MediaEvent;
import org.kurento.client.EventListener;

public class AsyncEventManager<T extends MediaEvent> extends AsyncManager<T> {

	public AsyncEventManager(String message) {
		super(message);
	}

	public EventListener<T> getMediaEventListener() {

		return new EventListener<T>() {
			@Override
			public void onEvent(T event) {
				addResult(event);
			}
		};
	}
}
