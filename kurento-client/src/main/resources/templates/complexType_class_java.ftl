${packageToFolder(module.code.api.java.packageName)}/${complexType.name}.java
<#include "macros.ftm" >
/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package ${module.code.api.java.packageName};

<#if complexType.typeFormat == "REGISTER">
<@comment complexType.doc!"" />
public class ${complexType.name} <#if complexType.extends??>extends ${complexType.extends.name}</#if> {

   <#list complexType.properties as property>
    <@comment property.doc />
    private ${getJavaObjectType(property.type,false)} ${property.name};
   </#list>

    <@comment "Create a ${complexType.name}" />
    public ${complexType.name}(<#rt>
     <#assign first=true>
     <#lt><#list complexType.parentProperties as property><#rt>
    	<#if !property.optional>
    		<#lt><#if first><#assign first=false><#else>, </#if>@org.kurento.client.internal.server.Param("${property.name}") ${getJavaObjectType(property.type,false)} ${property.name}<#rt>
    	</#if>
    </#list>
    <#lt><#list complexType.properties as property>
    	<#if !property.optional>
    		<#lt><#if first><#assign first=false><#else>, </#if>@org.kurento.client.internal.server.Param("${property.name}") ${getJavaObjectType(property.type,false)} ${property.name}<#rt>
    	</#if>
    <#lt></#list>) {

	super(<#list complexType.parentProperties as property>
		<#if !property.optional>
			<#lt>${property.name}<#if property_has_next>, </#if><#rt>
		</#if>
	<#lt></#list>);

     <#list complexType.properties as property>
        <#if !property.optional>
        this.${property.name} = ${property.name};
        </#if>
     </#list>
    }

    <#list complexType.properties as property>
    <@comment "get " + property.doc />
    public ${getJavaObjectType(property.type,false)} get${property.name?cap_first}(){
    	return ${property.name};
    }

    <@comment "set " + property.doc />
    public void set${property.name?cap_first}(${getJavaObjectType(property.type,false)} ${property.name}){
    	this.${property.name} = ${property.name};
    }

    </#list>
}

<#else>
<@comment complexType.doc />
public enum ${complexType.name} {<#rt>
   <#lt><#list complexType.values as value>${value}<#if value_has_next>, </#if></#list>}
</#if>
