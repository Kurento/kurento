package org.kurento.client.internal.test.model.client.events;

public interface MediaEventListener<T extends Event> {

	void onEvent(T event);
}
