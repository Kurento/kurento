package com.kurento.kmf.media.internal;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.media.events.KmsEvent;
import com.kurento.kmf.media.objects.MediaObject;

public class MediaServerCallbackHandler {

	private static final Logger log = LoggerFactory
			.getLogger(MediaServerCallbackHandler.class);

	private final ConcurrentHashMap<Long, Set<MediaEventListener<? extends KmsEvent>>> listenerMap;

	MediaServerCallbackHandler() {
		this.listenerMap = new ConcurrentHashMap<Long, Set<MediaEventListener<? extends KmsEvent>>>();
	}

	public void onEvent(KmsEvent kmsEvent) {
		Set<MediaEventListener<? extends KmsEvent>> listeners = this.listenerMap
				.get(kmsEvent.getObjectRef().getId());
		fireEvent(listeners, kmsEvent);
	}

	public void onError(KmsError kmsError) {
		// TODO Call the appropriate handler
	}

	public <T extends KmsEvent> MediaEventListener<T> addListener(
			MediaObject mediaObject, MediaEventListener<T> listener) {

		Long id = mediaObject.getObjectRef().getId();
		Set<MediaEventListener<? extends KmsEvent>> listeners = this.listenerMap
				.get(id);
		// TODO Sequence of calls may not be atomic here
		if (listeners == null) {
			listeners = new CopyOnWriteArraySet<MediaEventListener<? extends KmsEvent>>();
			this.listenerMap.put(id, listeners);
		}
		listeners.add(listener);
		return listener;
	}

	public <T extends KmsEvent> boolean removeListener(MediaObject mediaObject,
			MediaEventListener<T> listener) {
		boolean removed = false;
		Set<MediaEventListener<? extends KmsEvent>> listeners = this.listenerMap
				.get(mediaObject.getObjectRef().getId());
		// TODO Sequence of calls may not be atomic here
		if (listeners != null) {
			removed = listeners.remove(listener);
			if (listeners.isEmpty()) {
				this.listenerMap.remove(mediaObject.getObjectRef().getId());
			}
		}
		return removed;
	}

	public boolean removeAllListeners(MediaObject mediaObject) {
		return this.listenerMap.remove(mediaObject.getObjectRef().getId()) != null;
	}

	private void fireEvent(
			Set<MediaEventListener<? extends KmsEvent>> listeners,
			KmsEvent event) {
		if (listeners == null) {
			return;
		}

		for (MediaEventListener<? extends KmsEvent> listener : listeners) {
			try {
				listener.internalOnEvent(event);
			} catch (Throwable t) {
				log.error(t.getMessage(), t);
			}
		}
	}

}
