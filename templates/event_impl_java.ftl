com/kurento/kmf/media/events/internal/${event.name}.java
package com.kurento.kmf.media.events.internal;

public class ${event.name} extends AbstractMediaEvent {

   <#list event.properties as property>
    private ${property.type.name} ${property.name};
   </#list>  
     
    public ${event.name}(<#rt>  
     <#lt><#list event.properties as property>${property.type.name} ${property.name}<#if property_has_next>, </#if></#list>){
     <#list event.properties as property>
        this.${property.name} = ${property.name};
     </#list>
    }    
     	
    <#list event.properties as property>
    public ${property.type.name} get${property.name?cap_first}(){
    	return ${property.name};
    }
        
    public void set${property.name?cap_first}(${property.type.name} ${property.name}){
    	this.${property.name} = ${property.name};
    }
    
    </#list>
}
