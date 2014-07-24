package.json
{
  "name": "${module.code.api.js["node.name"]}",
  "version": "${module.code.api.js["npm.version"]}",
  "description": "${module.code.api.js["npm.description"]}",
  "main": "lib/index.js",
  "author": "",
  "license": "LGPL",
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
