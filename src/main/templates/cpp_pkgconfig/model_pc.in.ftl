${module.code.implementation.lib?replace("^lib", "", "r")}.pc.in
prefix=@prefix@
exec_prefix=@exec_prefix@
libdir=@libdir@
includedir=@includedir@

Name: ${module.code.implementation.lib?replace("^lib", "", "r")}
Description: Kurento ${module.name} Module
Version: ${module.version}
URL:<#if module.code.repoAddress??> ${module.code.repoAddress}</#if>
Requires:<#list module.imports as import> ${import.module.code.implementation.lib?replace("^lib", "", "r")} </#list> @requires@
Requires.private:
Libs: -L<#noparse>${libdir}</#noparse> -l${module.code.implementation.lib?replace("^lib", "", "r")}impl
Libs.private:
<#noparse>
CFlags: -I${includedir}
</#noparse>
