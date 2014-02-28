package com.kurento.kmf.media.events;

import com.kurento.tool.rom.server.Param;
import com.kurento.kmf.media.*;

public class CodeFoundEvent extends MediaEvent {

    private String codeType;
    private String value;

    public CodeFoundEvent(@Param("source") MediaObject source, @Param("type") String type, @Param("codeType") String codeType, @Param("value") String value){
        super(source, type);
        this.codeType = codeType;
        this.value = value;
    }

    public String getCodeType(){
    	return codeType;
    }

    public void setCodeType(String codeType){
    	this.codeType = codeType;
    }

    public String getValue(){
    	return value;
    }

    public void setValue(String value){
    	this.value = value;
    }

}
