/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.kmf.media.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.media.MediaObject;
import com.kurento.kmf.media.events.MediaError;
import com.kurento.kmf.media.events.MediaErrorListener;
import com.kurento.kmf.media.events.MediaEvent;
import com.kurento.kmf.media.events.MediaEventListener;

public class MediaServerCallbackHandler {

	private static final Logger log = LoggerFactory
			.getLogger(MediaServerCallbackHandler.class);

	private final Map<Long, Map<EventListenerRegistration, MediaEventListener<? extends MediaEvent>>> listenerMap;
	private final Map<Long, Map<ErrorListenerRegistration, MediaErrorListener>> errorListenerMap;

	public MediaServerCallbackHandler() {
		// TODO change to listener registration
		this.listenerMap = new ConcurrentHashMap<Long, Map<EventListenerRegistration, MediaEventListener<? extends MediaEvent>>>();
		this.errorListenerMap = new ConcurrentHashMap<Long, Map<ErrorListenerRegistration, MediaErrorListener>>();
	}

	public void onEvent(EventListenerRegistration registration, Long id,
			MediaEvent KmsMediaEvent) {
		Map<EventListenerRegistration, MediaEventListener<? extends MediaEvent>> listeners = this.listenerMap
				.get(id);
		try {
			fireEvent(registration, listeners, KmsMediaEvent);
		} catch (Throwable t) {
			log.error(
					"Exception invoking event listener "
							+ registration.getRegistrationId()
							+ " for event type" + KmsMediaEvent.getType(), t);
		}
	}

	public void onError(ErrorListenerRegistration registration, Long id,
			MediaError error) {
		Map<ErrorListenerRegistration, MediaErrorListener> listeners = this.errorListenerMap
				.get(id);
		try {
			fireError(registration, listeners, error);
		} catch (Throwable t) {
			log.error(
					"Exception invoking error listener "
							+ registration.getRegistrationId(), t);
		}
	}

	/**
	 * Adds a {@link MediaErrorListener} to a {@link MediaObject}, that will
	 * handle all errors produced by this object.
	 * </br><strong>NOTE</strong></br> Already registered listeners can be
	 * replaced by invoking this method with a different listener
	 * 
	 * @param mediaObject
	 *            The media object to add the listener to
	 * @param registration
	 *            The object that will be used to identify the listener
	 *            registration. This object will be used to unregister the event
	 *            also
	 * @param listener
	 *            The listener to handle the error events
	 * @return The error listener stored in the map
	 */
	public MediaErrorListener addErrorListener(MediaObject mediaObject,
			ErrorListenerRegistration registration, MediaErrorListener listener) {

		Long id = ((AbstractMediaObject) mediaObject).getObjectRef().getId();
		Map<ErrorListenerRegistration, MediaErrorListener> listeners = this.errorListenerMap
				.get(id);
		synchronized (this) {
			if (listeners == null) {
				listeners = new HashMap<ErrorListenerRegistration, MediaErrorListener>();
				this.errorListenerMap.put(id, listeners);
			}
		}
		listeners.put(registration, listener);
		return listener;
	}

	/**
	 * Removes a listener that handles errors for a given {@link MediaObject}
	 * 
	 * @param mediaObj
	 * @param registration
	 */
	public void removeErrorListener(MediaObject mediaObj,
			ErrorListenerRegistration registration) {
		errorListenerMap.remove(((AbstractMediaObject) mediaObj).getId());
	}

	public <T extends MediaEvent> MediaEventListener<T> addListener(
			MediaObject mediaObject, EventListenerRegistration registration,
			MediaEventListener<T> listener) {

		Long id = ((AbstractMediaObject) mediaObject).getObjectRef().getId();
		Map<EventListenerRegistration, MediaEventListener<? extends MediaEvent>> listeners = this.listenerMap
				.get(id);
		synchronized (this) {
			if (listeners == null) {
				listeners = new HashMap<EventListenerRegistration, MediaEventListener<? extends MediaEvent>>();
				this.listenerMap.put(id, listeners);
			}
		}
		listeners.put(registration, listener);
		return listener;
	}

	public <T extends MediaEvent> boolean removeListener(
			MediaObject mediaObject,
			EventListenerRegistration listenerRegistration) {
		MediaEventListener<? extends MediaEvent> removed = null;
		Map<EventListenerRegistration, MediaEventListener<? extends MediaEvent>> listeners = this.listenerMap
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

	public boolean removeAllErrorListeners(MediaObject mediaObject) {
		return this.errorListenerMap.remove(((AbstractMediaObject) mediaObject)
				.getObjectRef().getId()) != null;
	}

	@SuppressWarnings("unchecked")
	private void fireEvent(
			EventListenerRegistration registration,
			Map<EventListenerRegistration, MediaEventListener<? extends MediaEvent>> listeners,
			MediaEvent event) {
		if (listeners != null) {
			@SuppressWarnings("rawtypes")
			MediaEventListener listener = listeners.get(registration);
			// TODO Unchecked exception suppressed. Maybe a more safe check
			// should be done here.
			listener.onEvent(event);
		}
	}

	private void fireError(ErrorListenerRegistration registration,
			Map<ErrorListenerRegistration, MediaErrorListener> listeners,
			MediaError error) {
		if (listeners != null) {
			MediaErrorListener listener = listeners.get(registration);
			listener.onError(error);
		}
	}

}
