package org.kurento.room.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.kurento.room.client.event.Event;
import org.kurento.room.client.event.Listener;
import org.kurento.room.client.internal.LocalStreamInfo;
import org.kurento.room.client.internal.StreamInfo;

public class Stream {

	public enum StreamType {
		LOCAL, REMOTE
	}

	private static class ListenerEntry {
		public Class<? extends Event> eventClass;
		public Listener<? extends Event> listener;

		public ListenerEntry(Class<? extends Event> eventClass,
				Listener<? extends Event> listener) {
			super();
			this.eventClass = eventClass;
			this.listener = listener;
		}
	}

	private Room room;

	private String id;
	private StreamType type;
	private Map<String, Object> attributes;

	private CopyOnWriteArrayList<ListenerEntry> listeners;

	private String sdpResponse;

	private String sdpOffer;

	public Stream(Map<String, Object> attributes) {
		this.attributes = attributes;
		this.id = UUID.randomUUID().toString();
		this.type = StreamType.LOCAL;
	}

	Stream(Room room, StreamInfo streamInfo) {
		this.room = room;
		this.attributes = streamInfo.getAttributes();
		this.id = streamInfo.getId();
		this.type = StreamType.REMOTE;
	}

	public String getId() {
		return id;
	}

	public StreamType getType() {
		return type;
	}

	void setRoom(Room room) {
		this.room = room;
	}

	public Room getRoom() {
		return room;
	}

	public void close() {
		this.room.unpublish(this);
	}

	public void sendData(String data) {
		room.sendData(this, data);
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public <E extends Event> void addEventListener(Class<E> eventClass,
			Listener<E> listener) {
		listeners.add(new ListenerEntry(eventClass, listener));
	}

	@SuppressWarnings("unchecked")
	void fireEvent(Event e) {
		for (ListenerEntry entry : listeners) {
			if (entry.eventClass == e.getClass()) {
				((Listener<Event>) entry.listener).onEvent(e);
			}
		}
	}

	public void subscribe(String sdpOffer) {
		room.subscribe(this);
	}

	public void unsubscribe() {
		room.unsubscribe(this);
	}

	public void setSdpResponse(String sdpResponse) {
		this.sdpResponse = sdpResponse;
	}

	public String getSdpResponse() {
		return sdpResponse;
	}

	public void setSdpOffer(String sdpOffer) {
		this.sdpOffer = sdpOffer;
	}

	public LocalStreamInfo createLocalStreamInfo() {
		return new LocalStreamInfo(attributes, sdpOffer);
	}

	void dataEvent(String message) {
		// TODO Auto-generated method stub

	}

	public void attributeUpdateEvent(Map<String, Object> atts) {
		// TODO Auto-generated method stub

	}
}
