package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;
import com.kurento.tool.rom.server.FactoryMethod;
import java.util.List;
import com.kurento.kmf.media.events.*;

public interface SessionEndpoint extends Endpoint {

    ListenerRegistration addMediaSessionTerminatedListener(MediaEventListener<MediaSessionTerminatedEvent> listener);
    void addMediaSessionTerminatedListener(MediaEventListener<MediaSessionTerminatedEvent> listener, Continuation<ListenerRegistration> cont);
    ListenerRegistration addMediaSessionStartedListener(MediaEventListener<MediaSessionStartedEvent> listener);
    void addMediaSessionStartedListener(MediaEventListener<MediaSessionStartedEvent> listener, Continuation<ListenerRegistration> cont);

	
	
}
