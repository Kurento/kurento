${packageToFolder(module.code.api.java.packageName)}/${event.name}Event.java
<#include "macros.ftm" >
/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package ${module.code.api.java.packageName};

import org.kurento.client.internal.server.Param;
import java.util.List;

<#list module.allImports as import>
import ${import.module.code.api.java.packageName}.*;
</#list>


<@comment "${event.doc}" />
public class ${event.name}Event <#if event.extends??>extends ${event.extends.name}Event<#else>implements Event</#if> {

<#list event.properties as property>
	<#lt><@comment property.doc />
	private ${property.type.name} ${property.name};
</#list>

		<@comment event.doc event.parentProperties + event.properties />
	public ${event.name}Event(<#rt>
     <#assign first=true>
     <#lt><#list event.parentProperties as property><#if first><#assign first=false><#else>, </#if>@Param("${property.name}") ${getJavaObjectType(property.type,false)} ${property.name}</#list><#rt>
     <#lt><#list event.properties as property><#if first><#assign first=false><#else>, </#if>@Param("${property.name}") ${getJavaObjectType(property.type,false)} ${property.name}</#list>) {
		super(<#list event.parentProperties as property>${property.name}<#if property_has_next>, </#if></#list>);
     <#list event.properties as property>
		this.${property.name} = ${property.name};
     </#list>
	}

	<#list event.properties as property>
	<#assign par=[] />
	<#lt><@comment "Getter for the ${property.name} property" par property />
	public ${getJavaObjectType(property.type,false)} get${property.name?cap_first}() {
		return ${property.name};
	}

    <#assign par=[property] />
    <#lt><@comment "Setter for the ${property.name} property" par />
	public void set${property.name?cap_first}(${getJavaObjectType(property.type,false)} ${property.name}) {
		this.${property.name} = ${property.name};
	}

    </#list>
}
