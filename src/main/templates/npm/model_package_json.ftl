package.json
{
  "name": "${model.code.api.js["node.name"]}",
  "version": "${model.code.api.js["npm.version"]}",
  "description": "${model.code.api.js["npm.description"]}",
  "main": "lib/index.js",
  "author": "",
  "license": "LGPL",
  "dependencies": {
    "inherits": "^2.0.1"<#if model.imports[0]??>,</#if>
<#list model.imports as import>
    "${import.model.code.api.js["node.name"]}":"${import.model.code.api.js["npm.version"]}"<#if import_has_next>,</#if>
</#list>
  },
  "peerDependencies": {
    "kws-media-api": "^${model.kurentoVersion}"
  }
}
