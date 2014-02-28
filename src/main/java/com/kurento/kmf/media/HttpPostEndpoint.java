package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;
import com.kurento.tool.rom.server.FactoryMethod;
import java.util.List;
import com.kurento.kmf.media.events.*;

public interface HttpPostEndpoint extends HttpEndpoint {

    ListenerRegistration addEndOfStreamListener(MediaEventListener<EndOfStreamEvent> listener);
    void addEndOfStreamListener(MediaEventListener<EndOfStreamEvent> listener, Continuation<ListenerRegistration> cont);

	
	


    public interface Factory {

        public Builder create(@Param("mediaPipeline") MediaPipeline mediaPipeline);
    }

    public interface Builder extends AbstractBuilder<HttpPostEndpoint> {

        public Builder withDisconnectionTimeout(int disconnectionTimeout);
        public Builder useEncodedMedia();
        public Builder withGarbagePeriod(int garbagePeriod);
    }
}
