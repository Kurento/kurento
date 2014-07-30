<#assign node_name=module.code.api.js["node.name"]>
bower.json
{
  "name": "${node_name}",
  "description": "${module.code.api.js["npm.description"]}",
  "main": "dist/${node_name}.js",
  "license": "LGPL",
  "ignore": [
    "doc/",
    "lib/",
    "package.json"
  ],
  "keywords": [
    "API",
    "Kurento",
    "KWS",
    "SDK",
    "web",
    "WebRTC"
  ],
  "authors": [
    "Kurento <info@kurento.com> (http://kurento.org)"
  ],
  "homepage": "http://www.kurento.com",
  "repository": {
    "type": "git",
    "url": "https://github.com/Kurento/${node_name}.git"
  }
}
