${config.subfolder}/${complexType.name}.java
<#include "macros.ftm" >
/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package ${config.packageName};

import com.kurento.tool.rom.server.Param;
import java.util.List;

<#if complexType.typeFormat == "REGISTER">
<@comment complexType.doc />
public class ${complexType.name} {

   <#list complexType.properties as property>
    <@comment property.doc />
    private ${getJavaObjectType(property.type,false)} ${property.name};
   </#list>

    <@comment "Create a ${complexType.name}" />
    public ${complexType.name}(<#rt>
     <#assign num=0>
     <#list complexType.properties as property>
     <#if !property.optional>
        <#lt><#if (num>0)>, </#if><#rt>
        <#lt>@Param("${property.name}") ${getJavaObjectType(property.type,false)} ${property.name}<#rt>
        <#assign num=num+1>
     </#if>
     <#lt></#list>){
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
