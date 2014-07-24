${module.code.implementation.lib?replace("lib", "")}.pc.in
prefix=@prefix@
exec_prefix=@exec_prefix@
libdir=@libdir@
includedir=@includedir@

Name: gstmarshal
Description: Kurento ${module.name} Module
Version: ${module.version}
URL:<#if module.code.repoAddress??> ${module.code.repoAddress}</#if>
Requires:<#if !module.imports[0]?? > gstreamer-1.0 >= 1.3.3 gstreamer-sdp-1.0 >= 1.3.3 libjsonrpc >= 0.0.6 sigc++-2.0 >= 2.0.10 glibmm-2.4 >= 2.37<#else><#rt>
<#list module.imports as import>${import.module.code.implementation.lib?replace("lib", "")} </#list><#rt>
</#if> @requires@
Requires.private:
Libs: -L<#noparse>${libdir}</#noparse> -l${module.code.implementation.lib?replace("lib", "")}impl
Libs.private:
<#noparse>
CFlags: -I${includedir}
</#noparse>
