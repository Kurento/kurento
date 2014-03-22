${config.subfolder}/${remoteClass.name}.java
<#include "macros.ftm" >
/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package ${config.packageName};

import com.kurento.tool.rom.server.Param;
import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.FactoryMethod;
import java.util.List;
import ${config.packageName}.events.*;

<@comment remoteClass.doc />
@RemoteClass
public interface ${remoteClass.name} <#if remoteClass.extends??>extends ${remoteClass.extends.name}</#if> {

   <#list remoteClass.methods as method>

	<@comment method.doc method.params method.return />
	${getJavaObjectType(method.return,false)} ${method.name}(<#rt>
		<#lt><#list method.params as param>@Param("${param.name}") ${getJavaObjectType(param.type,false)} ${param.name}<#if param_has_next>, </#if></#list>);

	<#assign doc>
Asynchronous version of ${method.name}:
{@link Continuation#onSuccess} is called when the action is
done. If an error occurs, {@link Continuation#onError} is called.

@see ${remoteClass.name}#${method.name}
    </#assign>
    <@comment doc method.params />
    void ${method.name}(<#rt>
		<#lt><#list method.params as param>@Param("${param.name}") ${getJavaObjectType(param.type,false)} ${param.name}, </#list>Continuation<${getJavaObjectType(method.return)}> cont);

    </#list>
	<#list remoteClass.events as event>
    /**
     * Add a {@link MediaEventListener} for event {@link ${event.name}Event}. Synchronous call.
     *
     * @param  listener Listener to be called on ${event.name}Event
     * @return ListenerRegistration for the given Listener
     *
     **/
    ListenerRegistration add${event.name}Listener(MediaEventListener<${event.name}Event> listener);
    /**
     * Add a {@link MediaEventListener} for event {@link ${event.name}Event}. Asynchronous call.
     * Calls Continuation&lt;ListenerRegistration&gt; when it has been added.
     *
     * @param listener Listener to be called on ${event.name}Event
     * @param cont     Continuation to be called when the listener is registered
     *
     **/
    void add${event.name}Listener(MediaEventListener<${event.name}Event> listener, Continuation<ListenerRegistration> cont);
    </#list>

	<#if !remoteClass.extends??>
    /**
     *
     * Explicitly release a media object form memory. All of its children
     * will also be released.
     *
     **/
    void release();
    /**
     *
     * Explicitly release a media object form memory. All of its children
     * will also be released. Asynchronous call.
     *
     * @param continuation {@link #onSuccess(void)} will be called when the actions complete.
     *                     {@link #onError} will be called if there is an exception.
     *
     **/
	void release(Continuation<Void> continuation);
    </#if>

    <#--Factory methods for other elements -->
    <#list model.remoteClasses as otherRemoteClass>
    <#if isFirstConstructorParam(remoteClass, otherRemoteClass) && !otherRemoteClass.abstract>
    /**
     * Get a {@link ${otherRemoteClass.name}}.{@link Builder} for this ${remoteClass.name}
     *
    **/
    @FactoryMethod("${otherRemoteClass.constructors[0].params[0].name}")
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

    <#if !remoteClass.abstract>
    /**
     *
     * Factory for building {@link ${remoteClass.name}}
     *
     **/
    public interface Factory {
        <#assign doc="Creates a Builder for ${remoteClass.name}" param=[{"name":"mediaPipeline","type":"MediaPipeline"}] />
        <@comment doc param />
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
        <#if param.type.name != "boolean" >
            <#assign par=[param] />
            <@comment  "Sets a value for ${param.name} in Builder for ${remoteClass.name}." par />
            public Builder with${param.name?cap_first}(${getJavaObjectType(param.type,false)} ${param.name});
        <#else>
            <@comment  param.doc />
            public Builder ${param.name}();
        </#if>
       </#list>
    }
	</#if>
}
