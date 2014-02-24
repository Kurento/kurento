${config.subfolder}/${remoteClass.name}.java
package ${config.packageName};

import com.kurento.tool.rom.server.Param;
import java.util.List;
import ${config.packageName}.events.*;

public interface ${remoteClass.name} <#if remoteClass.extends??>extends ${remoteClass.extends.name}</#if> {

   <#list remoteClass.methods as method>
    ${getJavaObjectType(method.return,false)} ${method.name}(<#rt>
       <#lt><#list method.params as param>${param.type.name} ${param.name}<#if param_has_next>, </#if></#list>);
    void ${method.name}(<#rt>
       <#lt><#list method.params as param>${param.type.name} ${param.name}, </#list>Continuation<${getJavaObjectType(method.return)}> cont);

    </#list>
	<#list remoteClass.events as event>
    ListenerRegistration add${event.name}Listener(MediaEventListener<${event.name}Event> listener);
    void add${event.name}Listener(MediaEventListener<${event.name}Event> listener, Continuation<ListenerRegistration> cont);
    </#list>

	<#if !remoteClass.extends??>void release();</#if>
	<#if !remoteClass.extends??>void release(Continuation<Void> continuation);</#if>
	<#if !remoteClass.abstract>

    <#--Factory methods for other elements -->
    <#list model.remoteClasses as otherRemoteClass>
    <#if isFirstConstructorParam(remoteClass, otherRemoteClass)>
    public abstract ${otherRemoteClass.name}.Builder new${otherRemoteClass.name}(<#rt>
        <#assign num=0>
        <#lt><#list otherRemoteClass.constructors[0].params as param>
        <#if !param.optional>
            <#if (num>0)>
               <#lt><#if (num>1)>, </#if><#rt>
               <#lt>@Param("${param.name}") ${getJavaObjectType(param.type,false)} ${param.name}<#rt>
            </#if>
            <#assign num=num+1>
        </#if>
        </#list>
        <#lt>);
    </#if>
    </#list>

    public interface Factory {

        public Builder create(<#rt>
        <#assign first=true>
        <#lt><#list remoteClass.constructors[0].params as param>
        <#if !param.optional>
            <#lt><#if first><#assign first=false><#else>, </#if><#rt>
            <#lt>@Param("${param.name}") ${getJavaObjectType(param.type,false)} ${param.name}<#rt>
        </#if>
        </#list>
        <#lt>);
    }

    public interface Builder extends AbstractBuilder<${remoteClass.name}> {

        <#list remoteClass.constructors[0].params as param>
        <#if param.optional>
        public Builder <#rt>
        <#if param.type.name != "boolean">
          <#lt>with${param.name?cap_first}(${getJavaObjectType(param.type,false)} ${param.name});
          <#lt><#else>${param.name}();
          </#if>
         </#if>
       </#list>
    }
	</#if>
}
