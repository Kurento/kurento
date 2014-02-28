package com.kurento.kmf.media.events;

public interface MediaEventListener<T extends Event> {

	void onEvent(T event);
}
