package com.kurento.kmf.media.events;

import com.kurento.tool.rom.server.Param;
import com.kurento.kmf.media.*;

public class EndOfStreamEvent extends MediaEvent {


    public EndOfStreamEvent(@Param("source") MediaObject source, @Param("type") String type){
        super(source, type);
    }

}
