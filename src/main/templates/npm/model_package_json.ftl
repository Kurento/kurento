package.json
{
  "name": "${model.code.api.js["node.name"]}",
  "version": "${model.code.api.js["npm.version"]}",
  "description": "${model.code.api.js["npm.description"]}",
  "main": "lib/index.js",
  "scripts": {
    "prepublish": "scripts/prepublish.sh",
    "test": "echo \"Error: no test specified\" && exit 1"
  },
  "author": "",
  "license": "LGPL",
  "dependencies": {
    "inherits": "^2.0.1",
<#list model.imports as import>
    "${import.model.code.api.js["node.name"]}":"${import.model.code.api.js["npm.version"]}",
</#list>
  },
  "peerDependencies": {
    "kws-media-api": "^${model.kurentoVersion}"
  }
}
