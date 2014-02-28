package com.kurento.kmf.media.events;

import com.kurento.tool.rom.server.Param;
import com.kurento.kmf.media.*;

public class MediaSessionStartedEvent extends MediaEvent {


    public MediaSessionStartedEvent(@Param("source") MediaObject source, @Param("type") String type){
        super(source, type);
    }

}
