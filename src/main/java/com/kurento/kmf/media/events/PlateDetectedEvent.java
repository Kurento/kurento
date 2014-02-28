package com.kurento.kmf.media.events;

import com.kurento.tool.rom.server.Param;
import com.kurento.kmf.media.*;

public class PlateDetectedEvent extends MediaEvent {

    private String plate;

    public PlateDetectedEvent(@Param("source") MediaObject source, @Param("type") String type, @Param("plate") String plate){
        super(source, type);
        this.plate = plate;
    }

    public String getPlate(){
    	return plate;
    }

    public void setPlate(String plate){
    	this.plate = plate;
    }

}
