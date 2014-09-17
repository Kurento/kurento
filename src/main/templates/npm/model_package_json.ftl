<#assign node_name=module.code.api.js.nodeName>
package.json
{
  "name": "${node_name}",
  "version": "${module.version}",
  "description": "${module.code.api.js.npmDescription}",
  "main": "lib/index.js",
  "keywords": [
    "Kurento"
  ],
<#if module.code.api.js.npmGit??>
  "repository": {
    "type": "git",
    "url": "${module.code.api.js.npmGit}"
  },
</#if>
  "dependencies": {
    "checktype": "^0.0.4",
    "inherits": "^2.0.1"
  }<#if node_name != "kurento-client-core"
     && node_name != "kurento-client-elements"
     && node_name != "kurento-client-filters">,
  "peerDependencies": {
    "kurento-client": "${module.kurentoVersion}"
  }
<#else>

</#if>
}
