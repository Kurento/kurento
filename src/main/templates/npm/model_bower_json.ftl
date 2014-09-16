<#assign node_name=module.code.api.js.nodeName>
bower.json
{
  "name": "${node_name}",
  "description": "${module.code.api.js.npmDescription}",
  "homepage": "http://www.kurento.com",
  "main": "dist/${node_name}.js",
  "authors": [
    "Kurento <info@kurento.com> (http://kurento.org)"
  ],
  "license": "LGPL",
  "repository": {
    "type": "git",
    "url": "Kurento/${node_name}-js"
  },
  "keywords": [
    "API",
    "Kurento",
    "WebRTC"
  ],
  "ignore": [
    "doc/",
    "lib/",
    "package.json"
  ]
}
