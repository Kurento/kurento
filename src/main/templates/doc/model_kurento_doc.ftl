Kurento_API_Reference.txt

Media Objects
-------------

Media objects represent all objects that live in Kurento Server and can be 
controlled with Kurento Clients (or any client using Kurento Protocol).

<#list module.remoteClasses as remoteClass>

${remoteClass.name}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    - **Description:** ${remoteClass.doc?replace("\n", " ")} 
    <#if remoteClass.extends??>
    - **Extends:** ${remoteClass.extends.name}    
    </#if>
    <#if !remoteClass.abstract && remoteClass.constructor??>
    - **Constructor Params:**
        <#list remoteClass.constructor.params as param>
         - **${param.type.name}<#if param.type.list>[]</#if> ${param.name}<#if param.optional>?</#if>**: ${param.doc?replace("\n", " ")}
        </#list>
    </#if>
    <#if remoteClass.properties?has_content >
    - **Properties:**
        <#list remoteClass.properties as property>
            - **${property.type.name}<#if property.type.list>[]</#if> ${property.name}:** ${property.doc?replace("\n", " ")}
        </#list>
    </#if>
    <#if remoteClass.methods?has_content >
    - **Methods:**
        <#list remoteClass.methods as method>
            - **<#if method.return??>${method.return.type.name}<#if method.return.type.list>[]</#if> </#if>${method.name}<#rt>
                <#lt>(<#list method.params as param><#rt>
                <#lt>${param.type.name}<#if param.type.list>[]</#if> ${param.name}<#if param_has_next>, </#if><#rt><#lt></#list>):** ${method.doc?replace("\n", " ")}
                </#list>
    </#if>            
    <#if remoteClass.events?has_content >
    - **Events:** <#list remoteClass.events as event>${event.name}, </#list>
    </#if>
    
----------
              
</#list>

Events
------

Events are sent from Kurento Server to apps using Kurento Clients when an event
occurs in Kurento Server. They carry information about the event. All its 
information is serialized to be accessible to clients.

<#list module.events as event>

${event.name}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    - **Description:** ${event.doc?replace("\n", " ")} 
    <#if event.extends??>
    - **Extends:** ${event.extends.name}    
    </#if>
    <#if event.properties?has_content >
    - **Properties:**
        <#list event.properties as property>
            - **${property.type.name}<#if property.type.list>[]</#if> ${property.name}**: ${property.doc?replace("\n", " ")}
        </#list>
    </#if>
    
----------
                 
</#list>

Types
-----

Types are used as parameters in constructors and methods. They are also used in
media object properties or event properties. There are two kinds of types: 
registers (with properties) and enums (with values). Types are serialized when
exchanged between client and server.

<#list module.complexTypes as type>

${type.name}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    - **Description:** <#if type.doc??>${type.doc?replace("\n", " ")}<#else> FIXME: Add doc here</#if>
    <#if type.extends??>
    - **Extends:** ${type.extends.name}    
    </#if>

<#if type.typeFormat == "REGISTER">
    
    <#if type.properties?has_content >
    - **Methods:**
        <#list type.properties as property>
            - **${property.type.name}<#if property.type.list>[]</#if> ${property.name}**: ${property.doc?replace("\n", " ")}
        </#list>
    </#if>
        
<#else>

    - **Enum constants:** <#list type.values as value>${value}, </#list>
                     
</#if>

----------

</#list>