/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.media;

import org.kurento.media.events.MediaEventListener;
import org.kurento.media.events.MediaSessionStartedEvent;
import org.kurento.media.events.MediaSessionTerminatedEvent;
import org.kurento.tool.rom.RemoteClass;

/**
 * 
 * Session based endpoint. A session is considered to be started when the media
 * exchange starts. On the other hand, sessions terminate when a timeout,
 * defined by the developer, takes place after the connection is lost.
 * 
 **/
@RemoteClass
public interface SessionEndpoint extends Endpoint {

	/**
	 * Add a {@link MediaEventListener} for event
	 * {@link MediaSessionTerminatedEvent}. Synchronous call.
	 * 
	 * @param listener
	 *            Listener to be called on MediaSessionTerminatedEvent
	 * @return ListenerRegistration for the given Listener
	 * 
	 **/
	ListenerRegistration addMediaSessionTerminatedListener(
			MediaEventListener<MediaSessionTerminatedEvent> listener);

	/**
	 * Add a {@link MediaEventListener} for event
	 * {@link MediaSessionTerminatedEvent}. Asynchronous call. Calls
	 * Continuation&lt;ListenerRegistration&gt; when it has been added.
	 * 
	 * @param listener
	 *            Listener to be called on MediaSessionTerminatedEvent
	 * @param cont
	 *            Continuation to be called when the listener is registered
	 * 
	 **/
	void addMediaSessionTerminatedListener(
			MediaEventListener<MediaSessionTerminatedEvent> listener,
			Continuation<ListenerRegistration> cont);

	/**
	 * Add a {@link MediaEventListener} for event
	 * {@link MediaSessionStartedEvent}. Synchronous call.
	 * 
	 * @param listener
	 *            Listener to be called on MediaSessionStartedEvent
	 * @return ListenerRegistration for the given Listener
	 * 
	 **/
	ListenerRegistration addMediaSessionStartedListener(
			MediaEventListener<MediaSessionStartedEvent> listener);

	/**
	 * Add a {@link MediaEventListener} for event
	 * {@link MediaSessionStartedEvent}. Asynchronous call. Calls
	 * Continuation&lt;ListenerRegistration&gt; when it has been added.
	 * 
	 * @param listener
	 *            Listener to be called on MediaSessionStartedEvent
	 * @param cont
	 *            Continuation to be called when the listener is registered
	 * 
	 **/
	void addMediaSessionStartedListener(
			MediaEventListener<MediaSessionStartedEvent> listener,
			Continuation<ListenerRegistration> cont);

}
