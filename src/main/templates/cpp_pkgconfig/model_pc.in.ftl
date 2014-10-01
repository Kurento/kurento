${module.code.implementation.lib?replace("lib", "")}.pc.in
prefix=@prefix@
exec_prefix=@exec_prefix@
libdir=@libdir@
includedir=@includedir@

Name: ${module.code.implementation.lib?replace("lib", "")}
Description: Kurento ${module.name} Module
Version: ${module.version}
URL:<#if module.code.repoAddress??> ${module.code.repoAddress}</#if>
Requires:<#list module.imports as import> ${import.module.code.implementation.lib?replace("lib", "")} </#list> @requires@
Requires.private:
Libs: -L<#noparse>${libdir}</#noparse> -l${module.code.implementation.lib?replace("lib", "")}impl
Libs.private:
<#noparse>
CFlags: -I${includedir}
</#noparse>
