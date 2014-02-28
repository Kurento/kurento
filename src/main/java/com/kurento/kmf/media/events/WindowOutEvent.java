package com.kurento.kmf.media.events;

import com.kurento.tool.rom.server.Param;
import com.kurento.kmf.media.*;

public class WindowOutEvent extends MediaEvent {

    private String windowId;

    public WindowOutEvent(@Param("source") MediaObject source, @Param("type") String type, @Param("windowId") String windowId){
        super(source, type);
        this.windowId = windowId;
    }

    public String getWindowId(){
    	return windowId;
    }

    public void setWindowId(String windowId){
    	this.windowId = windowId;
    }

}
