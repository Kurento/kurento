<#assign node_name=module.code.api.js.nodeName>
.npmignore
dist/
doc/
node_modules/
bower.json
<#if node_name != "kurento-client-core"
  && node_name != "kurento-client-elements"
  && node_name != "kurento-client-filters">
Gruntfile.js
</#if>
