<#assign node_name=module.code.api.js.nodeName>
package.json
{
  "name": "${node_name}",
  "version": "${module.version}",
  "description": "${module.code.api.js.npmDescription}",
  "homepage": "http://www.kurento.com",
  "main": "lib/index.js",
  "author": "Kurento <info@kurento.com> (http://kurento.org)",
  "license": "LGPL",
<#if module.code.api.js.npmGit??>
  "repository": {
    "type": "git",
    "url": "${module.code.api.js.npmGit}"
  },
</#if>
  "bugs": {
    "email": "kurento@googlegroups.com"
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
