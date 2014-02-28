package com.kurento.tool.rom.test.model.client.events;

import com.kurento.tool.rom.test.model.client.*;

public class SampleEvent extends BaseEvent {

    private String prop1;
     
    public SampleEvent(String prop2, String prop1){
        super(prop2);
        this.prop1 = prop1;
    }    
     	
    public String getProp1(){
    	return prop1;
    }
        
    public void setProp1(String prop1){
    	this.prop1 = prop1;
    }
    
}
