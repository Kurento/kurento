<#assign node_name=module.code.api.js.nodeName>
<#if node_name != "kurento-client-core"
  && node_name != "kurento-client-elements"
  && node_name != "kurento-client-filters">
.gitignore
doc/
node_modules/
</#if>
