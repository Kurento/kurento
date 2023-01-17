<#if module.name != "elements" && module.name != "filters"><#rt>
<#if module.name == "core"><#rt>
${packageToFolder("org.kurento.module")}/KurentoModuleInfo.java
<#else>
${packageToFolder("org.kurento.module")}/${module.name?cap_first}ModuleInfo.java
</#if>
/**
 * This file is generated with Kurento-maven-plugin.
 * Please don't edit.
 */
package org.kurento.module;

<#if module.name == "core"><#rt>
public class KurentoModuleInfo {
<#else>
public class ${module.name?cap_first}ModuleInfo {
</#if>
	public static String getPackageName () {

		return "${module.code.api.java.packageName}";
	}
}
</#if>