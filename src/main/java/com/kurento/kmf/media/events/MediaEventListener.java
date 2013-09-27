package com.kurento.kmf.media.events;

public interface MediaEventListener<T extends MediaEvent> {
	public void onEvent(T event);
}
