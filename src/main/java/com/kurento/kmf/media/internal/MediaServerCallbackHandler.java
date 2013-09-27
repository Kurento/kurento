package com.kurento.kmf.media.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.kurento.kmf.media.ListenerRegistration;
import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.events.MediaError;
import com.kurento.kmf.media.events.MediaEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.internal.AbstractMediaEventListener;

public class MediaServerCallbackHandler {

	private final Map<Long, Map<ListenerRegistration, MediaEventListener<? extends MediaEvent>>> listenerMap;

	public MediaServerCallbackHandler() {
		// TODO change to listener registration
		this.listenerMap = new ConcurrentHashMap<Long, Map<ListenerRegistration, MediaEventListener<? extends MediaEvent>>>();
	}

	public void onEvent(ListenerRegistration registration, Long id,
			MediaEvent kmsEvent) {
		Map<ListenerRegistration, MediaEventListener<? extends MediaEvent>> listeners = this.listenerMap
				.get(id);
		fireEvent(registration, listeners, kmsEvent);
	}

	public void onError(String callbackToken, MediaError kmsError) {
		// TODO Call the appropriate handler
	}

	public <T extends MediaEvent> MediaEventListener<T> addListener(
			MediaObject mediaObject, ListenerRegistration registration,
			MediaEventListener<T> listener) {

		Long id = ((AbstractMediaObject) mediaObject).getObjectRef().getId();
		Map<ListenerRegistration, MediaEventListener<? extends MediaEvent>> listeners = this.listenerMap
				.get(id);
		synchronized (this) {
			if (listeners == null) {
				listeners = new HashMap<ListenerRegistration, MediaEventListener<? extends MediaEvent>>();
				this.listenerMap.put(id, listeners);
			}
		}
		listeners.put(registration, listener);
		return listener;
	}

	public <T extends MediaEvent> boolean removeListener(
			MediaObject mediaObject, ListenerRegistration listenerRegistration) {
		MediaEventListener<? extends MediaEvent> removed = null;
		Map<ListenerRegistration, MediaEventListener<? extends MediaEvent>> listeners = this.listenerMap
				.get(((AbstractMediaObject) mediaObject).getObjectRef().getId());
		synchronized (this) {
			if (listeners != null) {
				removed = listeners.remove(listenerRegistration
						.getRegistrationId());
				if (listeners.isEmpty()) {
					this.listenerMap.remove(((AbstractMediaObject) mediaObject)
							.getObjectRef().getId());
				}
			}
		}
		return (removed != null);
	}

	public boolean removeAllListeners(MediaObject mediaObject) {
		return this.listenerMap.remove(((AbstractMediaObject) mediaObject)
				.getObjectRef().getId()) != null;
	}

	private void fireEvent(
			ListenerRegistration registration,
			Map<ListenerRegistration, MediaEventListener<? extends MediaEvent>> listeners,
			MediaEvent event) {
		if (listeners != null) {
			MediaEventListener<? extends MediaEvent> listener = listeners
					.get(registration);
			((AbstractMediaEventListener<? extends MediaEvent>) listener)
					.internalOnEvent(event);
		}
	}

}
