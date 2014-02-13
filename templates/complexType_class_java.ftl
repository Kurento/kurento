com/kurento/kmf/media/${complexType.name}.java
package com.kurento.kmf.media;

<#if complexType.typeFormat == "REGISTER">
public class ${complexType.name} {

   <#list complexType.properties as property>
    private ${property.type.name} ${property.name};
   </#list>  
     
    public ${complexType.name}(<#rt>  
     <#lt><#list complexType.properties as property>${property.type.name} ${property.name}<#if property_has_next>, </#if></#list>){
     <#list complexType.properties as property>
        this.${property.name} = ${property.name};
     </#list>
    }    
     	
    <#list complexType.properties as property>
    public ${property.type.name} get${property.name?cap_first}(){
    	return ${property.name};
    }
        
    public void set${property.name?cap_first}(${property.type.name} ${property.name}){
    	this.${property.name} = ${property.name};
    }
    
    </#list>
}

<#else>
public enum ${complexType.name} {<#rt>
   <#lt><#list complexType.values as value>${value}<#if value_has_next>, </#if></#list>}
</#if>
