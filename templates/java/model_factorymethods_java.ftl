com/kurento/kmf/media/MediaServerFactory.java
package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;

public abstract class MediaServerFactory {

<#list model.remoteClasses as remoteClass>
<#if !remoteClass.abstract>
    public abstract ${remoteClass.name}.Builder create${remoteClass.name}(<#rt>
        <#assign first=true>
        <#lt><#list remoteClass.constructors[0].params as param>
        <#if !param.optional>
            <#lt><#if first><#assign first=false><#else>, </#if><#rt>
            <#lt>@Param("${param.name}") ${getJavaObjectType(param.type,false)} ${param.name}<#rt>
        </#if>
        </#list>
        <#lt>);
    </#if>
    </#list>
}
