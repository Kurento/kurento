${packageToFolder(module.code.api.java.packageName)}/${remoteClass.name}.java
<#include "macros.ftm" >
/**
 * This file is generated with Kurento-maven-plugin.
 * Please don't edit.
 */
package ${module.code.api.java.packageName};

<#if module.code.api.java.packageName != "org.kurento.client">
import org.kurento.client.*;
</#if>

<@comment remoteClass.doc />
@org.kurento.client.internal.RemoteClass
public interface ${remoteClass.name} extends <#if remoteClass.extends??>${remoteClass.extends.name}<#else>KurentoObject</#if> {

   <#list remoteClass.properties as property>
     ${getJavaObjectType(property.type,false)} get${property.name?cap_first}();

     void get${property.name?cap_first}(Continuation<${getJavaObjectType(property.type,true)}> cont);

     TFuture<${getJavaObjectType(property.type,true)}> get${property.name?cap_first}(Transaction tx);

     <#if !property.readOnly && !property.final>
     void set${property.name?cap_first}(@org.kurento.client.internal.server.Param("${property.name}") ${getJavaObjectType(property.type,false)} ${property.name});

     void set${property.name?cap_first}(@org.kurento.client.internal.server.Param("${property.name}") ${getJavaObjectType(property.type,false)} ${property.name}, Continuation<Void> cont);

     void set${property.name?cap_first}(@org.kurento.client.internal.server.Param("${property.name}") ${getJavaObjectType(property.type,false)} ${property.name}, Transaction tx);
     </#if>
   </#list>

   <#list remoteClass.methods as method>

  <@comment method.doc method.params method.return />
  ${getJavaObjectType(method.return,false)} ${method.name}(<#rt>
    <#lt><#list method.params as param>@org.kurento.client.internal.server.Param("${param.name}") ${getJavaObjectType(param.type,false)} ${param.name}<#if param_has_next>, </#if></#list>);

  <#assign doc>
Asynchronous version of ${method.name}:
{@link Continuation#onSuccess} is called when the action is
done. If an error occurs, {@link Continuation#onError} is called.

@see ${remoteClass.name}#${method.name}
    </#assign>
    <@comment doc method.params />
    void ${method.name}(<#rt>
    <#lt><#list method.params as param>@org.kurento.client.internal.server.Param("${param.name}") ${getJavaObjectType(param.type,false)} ${param.name}, </#list>Continuation<${getJavaObjectType(method.return)}> cont);

    <@comment method.doc method.params method.return />
    <#assign type = getJavaObjectType(method.return,true)>
    <#if type == "Void">void<#else>TFuture<${type}></#if> ${method.name}(Transaction tx<#rt>
    <#lt><#list method.params as param>, @org.kurento.client.internal.server.Param("${param.name}") ${getJavaObjectType(param.type,false)} ${param.name}</#list>);

    </#list>
  <#list remoteClass.events as event>
    /**
     * Add a {@link EventListener} for event {@link ${event.name}Event}. Synchronous call.
     *
     * @param  listener Listener to be called on ${event.name}Event
     * @return ListenerSubscription for the given Listener
     *
     **/
    @org.kurento.client.internal.server.EventSubscription(${event.name}Event.class)
    ListenerSubscription add${event.name}Listener(EventListener<${event.name}Event> listener);
    /**
     * Add a {@link EventListener} for event {@link ${event.name}Event}. Asynchronous call.
     * Calls Continuation&lt;ListenerSubscription&gt; when it has been added.
     *
     * @param listener Listener to be called on ${event.name}Event
     * @param cont     Continuation to be called when the listener is registered
     *
     **/
    @org.kurento.client.internal.server.EventSubscription(${event.name}Event.class)
    void add${event.name}Listener(EventListener<${event.name}Event> listener, Continuation<ListenerSubscription> cont);
    </#list>

    <#if remoteClass.name == "MediaPipeline">
    Transaction beginTransaction();
    </#if>


    <#if !remoteClass.abstract && remoteClass.name != "MediaPipeline">

    public class Builder extends AbstractBuilder<${remoteClass.name}> {

    <#assign doc="Creates a Builder for ${remoteClass.name}" />
    <@comment doc param />
    public Builder(<#rt>
          <#assign first=true>
          <#lt><#list remoteClass.constructor.params as param>
          <#if !param.optional>
              <#lt><#if first><#assign first=false><#else>, </#if><#rt>
              <#lt>${getJavaObjectType(param.type,false)} ${param.name}<#rt>
          </#if>
          </#list>
          <#lt>){

      super(${remoteClass.name}.class,${remoteClass.constructor.params[0].name});

          <#list remoteClass.constructor.params as param>
          <#if !param.optional>
      props.add("${param.name}",${param.name});
          </#if>
          </#list>
    }

        <#list remoteClass.constructor.params as param>
        <#if param.optional>
        <#if param.type.name != "boolean" >
    <#assign par=[param] />
    <@comment  "Sets a value for ${param.name} in Builder for ${remoteClass.name}." par />
    public Builder with${param.name?cap_first}(${getJavaObjectType(param.type,false)} ${param.name}){
      props.add("${param.name}",${param.name});
      return this;
    }
        <#else>
            <@comment  param.doc />
    public Builder ${param.name}(){
      props.add("${param.name}",Boolean.TRUE);
      return this;
    }
    </#if>
        </#if>
       </#list>
    }
  </#if>


}