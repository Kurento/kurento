com/kurento/kmf/media/events/${event.name}.java
package com.kurento.kmf.media.events;

public interface ${event.name} extends <#if extends??>${event.extends.name}<#else>Event</#if> {

    <#list event.properties as property>
    public ${property.type.name} get${property.name?cap_first}();
    
    </#list>
}
