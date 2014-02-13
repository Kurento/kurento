com/kurento/kmf/media/${remoteClass.name}.java
package com.kurento.kmf.media;

import com.kurento.kmf.media.Continuation;

public interface ${remoteClass.name} <#if extends??>extends ${remoteClass.extends.name}</#if> {

   <#list remoteClass.methods as method>
    ${getJavaObjectType(method.return,false)} ${method.name}(<#rt>
     	<#lt><#list method.params as param>${param.type.name} ${param.name}<#if param_has_next>, </#if></#list>);     	
    void ${method.name}(<#rt>
     	<#lt><#list method.params as param>${param.type.name} ${param.name}, </#list>Continuation<${getJavaObjectType(method.return)}> cont);
   
    </#list> 
	<#list remoteClass.events as event>
    ListenerRegistration add${event.name}Listener(MediaEventListener<${event.name}> listener);
    void add${event.name}Listener(MediaEventListener<${event.name}Event> listener, Continuation<ListenerRegistration> cont);
			
   </#list>
   <#if !remoteClass.abstract>
    public interface ${remoteClass.name}Builder extends MediaObjectBuilder<${remoteClass.name}Builder, ${remoteClass.name}> {

       <#list remoteClass.constructors[0].params as param>		
         <#if param.optional>
         ${remoteClass.name}Builder <#rt>         
          <#if param.type.name != "boolean">
          <#lt>with${param.name?cap_first}(${getJavaObjectType(param.type,false)} ${param.name});
          <#lt><#else>${param.name}();
          </#if>
         </#if> 
       </#list>
    }
   </#if>
}
