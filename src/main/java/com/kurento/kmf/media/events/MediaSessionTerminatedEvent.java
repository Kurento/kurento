package com.kurento.kmf.media.events;

import com.kurento.tool.rom.server.Param;
import com.kurento.kmf.media.*;

public class MediaSessionTerminatedEvent extends MediaEvent {


    public MediaSessionTerminatedEvent(@Param("source") MediaObject source, @Param("type") String type){
        super(source, type);
    }

}
