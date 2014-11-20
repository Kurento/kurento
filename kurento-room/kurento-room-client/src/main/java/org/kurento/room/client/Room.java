package org.kurento.room.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.kurento.room.client.event.Event;
import org.kurento.room.client.event.Listener;
import org.kurento.room.client.event.StreamAddedEvent;
import org.kurento.room.client.event.StreamRemovedEvent;
import org.kurento.room.client.internal.RoomClientService;
import org.kurento.room.client.internal.RoomListener;
import org.kurento.room.client.internal.StreamInfo;
import org.kurento.room.client.internal.StreamListener;

public class Room {

	private String token;
	private RoomClientService service;
	private Map<String, Stream> localStreams = new HashMap<>();
	private ConcurrentMap<String, Stream> remoteStreams = new ConcurrentHashMap<>();
	private String id;
	private ListenerList listeners = new ListenerList();

	Room(RoomClientService service, String id, String token) {
		this.service = service;
		this.token = token;
		this.id = id;
	}

	static Room create(RoomClientService service, String id) {
		return new Room(service, id, null);
	}

	static Room get(RoomClientService service, String token) {
		return new Room(service, null, token);
	}

	public void connect() {

		RoomListener roomListener = new RoomListener() {
			@Override
			public void streamAddedEvent(StreamInfo streamInfo) {
				Room.this.streamAddedEvent(streamInfo);
			}

			@Override
			public void streamRemovedEvent(String streamId) {
				Room.this.streamRemovedEvent(streamId);
			}
		};

		RoomInfo roomInfo = null;
		if (id != null) {
			roomInfo = service.createRoom(id, roomListener);
		} else {
			roomInfo = service.getRoom(token, roomListener);
		}

		this.token = roomInfo.getToken();
		this.id = roomInfo.getId();

		for (StreamInfo streamInfo : roomInfo.getRemoteStreams()) {
			streamAddedEvent(streamInfo);
		}
	}

	private void streamRemovedEvent(String streamId) {
		Stream stream = remoteStreams.remove(streamId);
		fireStreamRemoved(stream);
	}

	private void streamAddedEvent(StreamInfo streamInfo) {
		Stream stream = new Stream(this, streamInfo);
		remoteStreams.put(stream.getId(), stream);
		fireStreamAdded(stream);
	}

	private void fireStreamRemoved(Stream stream) {
		listeners.fireEvent(new StreamRemovedEvent(stream));
	}

	private void fireStreamAdded(Stream stream) {
		listeners.fireEvent(new StreamAddedEvent(stream));
	}

	public <E extends Event> void addEventListener(Class<E> eventClass,
			Listener<E> listener) {
		listeners.addListener(eventClass, listener);
	}

	public String getId() {
		return id;
	}

	public Collection<Stream> getLocalStreams() {
		return Collections.unmodifiableCollection(localStreams.values());
	}

	public Collection<Stream> getRemoteStreams() {
		return Collections.unmodifiableCollection(remoteStreams.values());
	}

	public void publish(Stream localStream) {
		String sdpResponse = service.publish(token,
				localStream.createLocalStreamInfo());
		localStream.setSdpResponse(sdpResponse);
	}

	public void unpublish(Stream localStream) {
		service.unpublish(token, localStream.getId());
	}

	// Returns the recording Id
	public String startRecording(Stream localStream) {
		return service.startRecording(token, localStream.getId());
	}

	public void stopRecording(String recordingId) {
		service.stopRecording(token, recordingId);
	}

	public Collection<Stream> getStreamsByAttribute(String name, Object value) {
		List<Stream> streams = new ArrayList<>();
		for (Stream stream : remoteStreams.values()) {
			if (Objects.equals(stream.getAttributes().get(name), value)) {
				streams.add(stream);
			}
		}

		for (Stream stream : localStreams.values()) {
			if (Objects.equals(stream.getAttributes().get(name), value)) {
				streams.add(stream);
			}
		}
		return streams;
	}

	public void disconnect() {
		service.removeRoomListener(token);
	}

	// Stream methods -------------------------------

	void sendData(Stream stream, String data) {
		service.sendData(stream.getId(), data);
	}

	void subscribe(final Stream stream) {
		service.setStreamListener(token, stream.getId(), new StreamListener() {
			@Override
			public void dataEvent(String message) {
				stream.dataEvent(message);
			}

			@Override
			public void attributeUpdateEvent(Map<String, Object> atts) {
				stream.attributeUpdateEvent(atts);
			}
		});
	}

	void unsubscribe(Stream stream) {
		service.removeStreamListener(token, stream.getId());
	}

}
