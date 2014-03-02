package com.kurento.kmf.media;

import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.MediaSessionStartedEvent;
import com.kurento.kmf.media.events.MediaSessionTerminatedEvent;
import com.kurento.tool.rom.RemoteClass;

@RemoteClass
public interface SessionEndpoint extends Endpoint {

	ListenerRegistration addMediaSessionTerminatedListener(
			MediaEventListener<MediaSessionTerminatedEvent> listener);

	void addMediaSessionTerminatedListener(
			MediaEventListener<MediaSessionTerminatedEvent> listener,
			Continuation<ListenerRegistration> cont);

	ListenerRegistration addMediaSessionStartedListener(
			MediaEventListener<MediaSessionStartedEvent> listener);

	void addMediaSessionStartedListener(
			MediaEventListener<MediaSessionStartedEvent> listener,
			Continuation<ListenerRegistration> cont);

}
