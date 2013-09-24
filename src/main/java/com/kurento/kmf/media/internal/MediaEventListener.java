package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.events.MediaEvent;

public abstract class MediaEventListener<T extends MediaEvent> {

	private String callbackToken;

	public abstract void onEvent(T event);

	@SuppressWarnings("unchecked")
	public void internalOnEvent(MediaEvent event) {
		// TODO try to replace this internal
		onEvent((T) event);
	}

	public String getCallbackToken() {
		return callbackToken;
	}

	public void setCallbackToken(String callbackToken) {
		this.callbackToken = callbackToken;
	}
}
