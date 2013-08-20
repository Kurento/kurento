package com.kurento.kmf.media;

public abstract class MediaEventListener<T extends KmsEvent> {
	public abstract void onEvent(T event);

	@SuppressWarnings("unchecked")
	void internalOnEvent(KmsEvent event) {
		onEvent((T) event);
	}
}
