com/kurento/kmf/media/events/internal/${event.name}EventImpl.java
package com.kurento.kmf.media.events.internal;

import com.kurento.kmf.media.events.*;
import com.kurento.kmf.media.*;

public class ${event.name}EventImpl <#if event.extends??>extends ${event.extends.name}EventImpl</#if> implements ${event.name}Event {

   <#list event.properties as property>
    private ${property.type.name} ${property.name};
   </#list>

    public ${event.name}EventImpl(<#rt>
     <#assign first=true>
     <#lt><#list event.parentProperties as property><#if first><#assign first=false><#else>, </#if>${property.type.name} ${property.name}</#list><#rt>
     <#lt><#list event.properties as property><#if first><#assign first=false><#else>, </#if>${property.type.name} ${property.name}</#list>){
        super(<#list event.parentProperties as property>${property.name}<#if property_has_next>, </#if></#list>);
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
