com/kurento/kmf/media/MediaServerFactory.java
package com.kurento.kmf.media;

public abstract class MediaServerFactory {

    <#list model.remoteClasses as remoteClass>
    <#if !remoteClass.abstract>
    public ${remoteClass.name}Builder new${remoteClass.name}(<#rt>
    <#lt><#list remoteClass.constructors[0].params as param>
    	<#if !param.optional>
    		<#lt>${getJavaObjectType(param.type,false)} ${param.name}<#rt>
    		<#lt><#if param_has_next>, </#if><#rt></#if></#list>); 
        </#if>
    </#list>
}