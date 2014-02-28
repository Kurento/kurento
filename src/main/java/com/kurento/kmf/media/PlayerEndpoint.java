package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;
import com.kurento.tool.rom.server.FactoryMethod;
import java.util.List;
import com.kurento.kmf.media.events.*;

public interface PlayerEndpoint extends UriEndpoint {

    void play();
    void play(Continuation<Void> cont);

    ListenerRegistration addEndOfStreamListener(MediaEventListener<EndOfStreamEvent> listener);
    void addEndOfStreamListener(MediaEventListener<EndOfStreamEvent> listener, Continuation<ListenerRegistration> cont);

	
	


    public interface Factory {

        public Builder create(@Param("mediaPipeline") MediaPipeline mediaPipeline, @Param("uri") String uri);
    }

    public interface Builder extends AbstractBuilder<PlayerEndpoint> {

        public Builder useEncodedMedia();
        public Builder withGarbagePeriod(int garbagePeriod);
    }
}
