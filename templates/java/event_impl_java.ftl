${config.subfolder}/events/${event.name}Event.java
package ${config.packageName}.events;

import ${config.packageName}.*;

public class ${event.name}Event <#if event.extends??>extends ${event.extends.name}Event<#else>implements Event</#if> {

   <#list event.properties as property>
    private ${property.type.name} ${property.name};
   </#list>

    public ${event.name}Event(<#rt>
     <#assign first=true>
     <#lt><#list event.parentProperties as property><#if first><#assign first=false><#else>, </#if>${getJavaObjectType(property.type,false)} ${property.name}</#list><#rt>
     <#lt><#list event.properties as property><#if first><#assign first=false><#else>, </#if>${getJavaObjectType(property.type,false)} ${property.name}</#list>){
        super(<#list event.parentProperties as property>${property.name}<#if property_has_next>, </#if></#list>);
     <#list event.properties as property>
        this.${property.name} = ${property.name};
     </#list>
    }

    <#list event.properties as property>
    public ${getJavaObjectType(property.type,false)} get${property.name?cap_first}(){
    	return ${property.name};
    }

    public void set${property.name?cap_first}(${getJavaObjectType(property.type,false)} ${property.name}){
    	this.${property.name} = ${property.name};
    }

    </#list>
}
