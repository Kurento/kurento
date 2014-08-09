<#assign node_name=module.code.api.js["node.name"]>
package.json
{
  "name": "${node_name}",
  "version": "${module.code.api.js["npm.version"]}",
  "description": "${module.code.api.js["npm.description"]}",
  "homepage": "http://www.kurento.com",
  "main": "lib/index.js",
  "author": "Kurento <info@kurento.com> (http://kurento.org)",
  "license": "LGPL",
  "repository": {
    "type": "git",
    "url": "Kurento/${node_name}-js"
  },
  "bugs": {
    "url": "Kurento/${node_name}-js/issues",
    "email": "info@kurento.com"
  },
  "keywords": [
    "API",
    "Kurento",
    "KWS",
    "SDK",
    "web",
    "WebRTC"
  ],
  "dependencies": {
    "inherits": "^2.0.1"<#if module.imports?has_content>,
  <#list module.imports as import>
    <#assign package=import.module.code.api.js>
    "${package["node.name"]}": "^${package["npm.version"]}"<#if import_has_next>,</#if>
  </#list>
</#if>
  }<#if node_name != "kurento-client-core"
     && node_name != "kurento-client-elements"
     && node_name != "kurento-client-filters">,
  "peerDependencies": {
    "kurento-client": "^${module.kurentoVersion}"
  }
</#if>
}
