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
    "url": "https://github.com/Kurento/${node_name}.git"
  },
  "bugs": {
    "url": "https://github.com/Kurento/${node_name}/issues",
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
    "inherits": "^2.0.1"<#if module.imports[0]??>,</#if>
<#list module.imports as import>
    "${import.module.code.api.js["node.name"]}":"${import.module.code.api.js["npm.version"]}"<#if import_has_next>,</#if>
</#list>
  },
  "peerDependencies": {
    "kws-media-api": "^${module.kurentoVersion}"
  }
}
