package org.kurento.room.client.event;

public interface Listener<T extends Event> {

	public void onEvent(T event);

}
