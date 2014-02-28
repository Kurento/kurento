package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;
import com.kurento.tool.rom.server.FactoryMethod;
import java.util.List;
import com.kurento.kmf.media.events.*;

public interface GStreamerFilter extends Filter {


	
	


    public interface Factory {

        public Builder create(@Param("mediaPipeline") MediaPipeline mediaPipeline, @Param("command") String command);
    }

    public interface Builder extends AbstractBuilder<GStreamerFilter> {

        public Builder withGarbagePeriod(int garbagePeriod);
    }
}
