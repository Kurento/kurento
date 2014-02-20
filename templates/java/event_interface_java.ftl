com/kurento/kmf/media/events/${event.name}Event.java
package com.kurento.kmf.media.events;

import com.kurento.kmf.media.*;

public interface ${event.name}Event extends <#if event.extends??>${event.extends.name}Event<#else>Event</#if> {

    <#list event.properties as property>
    public ${property.type.name} get${property.name?cap_first}();
    
    </#list>
}
