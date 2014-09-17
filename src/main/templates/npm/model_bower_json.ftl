<#assign node_name=module.code.api.js.nodeName>
bower.json
{
  "name": "${node_name}",
  "description": "${module.code.api.js.npmDescription}",
  "main": "dist/${node_name}.js",
  "ignore": [
    "doc/",
    "lib/",
    "package.json"
  ],
  "keywords": [
    "Kurento"
  ]
}
