package com.kurento.kmf.media.events;

import com.kurento.tool.rom.server.Param;
import com.kurento.kmf.media.*;

public class ErrorEvent implements Event {

    private MediaObject object;
    private String description;
    private int errorCode;
    private String type;

    public ErrorEvent(@Param("object") MediaObject object, @Param("description") String description, @Param("errorCode") int errorCode, @Param("type") String type){
        super();
        this.object = object;
        this.description = description;
        this.errorCode = errorCode;
        this.type = type;
    }

    public MediaObject getObject(){
    	return object;
    }

    public void setObject(MediaObject object){
    	this.object = object;
    }

    public String getDescription(){
    	return description;
    }

    public void setDescription(String description){
    	this.description = description;
    }

    public int getErrorCode(){
    	return errorCode;
    }

    public void setErrorCode(int errorCode){
    	this.errorCode = errorCode;
    }

    public String getType(){
    	return type;
    }

    public void setType(String type){
    	this.type = type;
    }

}
