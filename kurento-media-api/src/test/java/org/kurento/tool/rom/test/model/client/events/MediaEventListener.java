package org.kurento.tool.rom.test.model.client.events;

public interface MediaEventListener<T extends Event> {

	void onEvent(T event);
}
