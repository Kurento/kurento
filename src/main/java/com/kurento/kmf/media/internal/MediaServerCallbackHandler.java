package com.kurento.kmf.media.internal;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.media.events.MediaEvent;
import com.kurento.kmf.media.objects.MediaObject;

public class MediaServerCallbackHandler {

	private static final Logger log = LoggerFactory
			.getLogger(MediaServerCallbackHandler.class);

	private final ConcurrentHashMap<Long, Set<MediaEventListener<? extends MediaEvent>>> listenerMap;

	MediaServerCallbackHandler() {
		this.listenerMap = new ConcurrentHashMap<Long, Set<MediaEventListener<? extends MediaEvent>>>();
	}

	public void onEvent(MediaEvent kmsEvent, Long id) {
		Set<MediaEventListener<? extends MediaEvent>> listeners = this.listenerMap
				.get(id);
		fireEvent(listeners, kmsEvent);
	}

	public void onError(MediaError kmsError) {
		// TODO Call the appropriate handler
	}

	public <T extends MediaEvent> MediaEventListener<T> addListener(
			MediaObject mediaObject, MediaEventListener<T> listener) {

		Long id = mediaObject.getObjectRef().getId();
		Set<MediaEventListener<? extends MediaEvent>> listeners = this.listenerMap
				.get(id);
		// TODO Sequence of calls may not be atomic here
		if (listeners == null) {
			listeners = new CopyOnWriteArraySet<MediaEventListener<? extends MediaEvent>>();
			this.listenerMap.put(id, listeners);
		}
		listeners.add(listener);
		return listener;
	}

	public <T extends MediaEvent> boolean removeListener(
			MediaObject mediaObject, MediaEventListener<T> listener) {
		boolean removed = false;
		Set<MediaEventListener<? extends MediaEvent>> listeners = this.listenerMap
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
			Set<MediaEventListener<? extends MediaEvent>> listeners,
			MediaEvent event) {
		if (listeners == null) {
			return;
		}

		for (MediaEventListener<? extends MediaEvent> listener : listeners) {
			try {
				listener.internalOnEvent(event);
			} catch (Throwable t) {
				log.error(t.getMessage(), t);
			}
		}
	}

}
