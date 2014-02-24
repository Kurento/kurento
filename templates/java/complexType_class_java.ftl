${config.subfolder}/${complexType.name}.java
package ${config.packageName};

import com.kurento.tool.rom.server.Param;

<#if complexType.typeFormat == "REGISTER">
public class ${complexType.name} {

   <#list complexType.properties as property>
    private ${getJavaObjectType(property.type,false)} ${property.name};
   </#list>

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
    public ${getJavaObjectType(property.type,false)} get${property.name?cap_first}(){
    	return ${property.name};
    }

    public void set${property.name?cap_first}(${getJavaObjectType(property.type,false)} ${property.name}){
    	this.${property.name} = ${property.name};
    }

    </#list>
}

<#else>
public enum ${complexType.name} {<#rt>
   <#lt><#list complexType.values as value>${value}<#if value_has_next>, </#if></#list>}
</#if>
