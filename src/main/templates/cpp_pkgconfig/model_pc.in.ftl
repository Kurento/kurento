${model.code.implementation.lib?replace("lib", "")}.pc.in
prefix=@prefix@
exec_prefix=@exec_prefix@
libdir=@libdir@
includedir=@includedir@

Name: gstmarshal
Description: Kurento ${model.name} Module
Version: ${model.version}
URL:<#if model.code.repoAddress??> ${model.code.repoAddress}</#if>
Requires:<#if !model.imports[0]?? > gstreamer-1.0 >= 1.3.3 libjsonrpc >= 0.0.6 sigc++2.0 >= 2.0.10 glibmm-2.4 >= 2.37<#else></#if> @requires@
Requires.private:
Libs: -L<#noparse>${libdir}</#noparse> -l${model.code.implementation.lib?replace("lib", "")}impl
Libs.private:
<#noparse>
CFlags: -I${includedir}
</#noparse>
