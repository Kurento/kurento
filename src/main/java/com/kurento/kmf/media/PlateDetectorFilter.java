package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;
import com.kurento.tool.rom.server.FactoryMethod;
import java.util.List;
import com.kurento.kmf.media.events.*;

public interface PlateDetectorFilter extends Filter {

    ListenerRegistration addPlateDetectedListener(MediaEventListener<PlateDetectedEvent> listener);
    void addPlateDetectedListener(MediaEventListener<PlateDetectedEvent> listener, Continuation<ListenerRegistration> cont);

	
	


    public interface Factory {

        public Builder create(@Param("mediaPipeline") MediaPipeline mediaPipeline);
    }

    public interface Builder extends AbstractBuilder<PlateDetectorFilter> {

        public Builder withGarbagePeriod(int garbagePeriod);
    }
}
