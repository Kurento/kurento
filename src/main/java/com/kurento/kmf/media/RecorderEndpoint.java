package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;
import com.kurento.tool.rom.server.FactoryMethod;
import java.util.List;
import com.kurento.kmf.media.events.*;

public interface RecorderEndpoint extends UriEndpoint {

    void record();
    void record(Continuation<Void> cont);


	
	


    public interface Factory {

        public Builder create(@Param("mediaPipeline") MediaPipeline mediaPipeline, @Param("uri") String uri);
    }

    public interface Builder extends AbstractBuilder<RecorderEndpoint> {

        public Builder withMediaProfile(MediaProfileSpecType mediaProfile);
        public Builder stopOnEndOfStream();
        public Builder withGarbagePeriod(int garbagePeriod);
    }
}
