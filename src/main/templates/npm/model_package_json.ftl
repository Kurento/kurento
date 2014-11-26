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
    "inherits": "^2.0.1"
  },
<#if node_name != "kurento-client-core"
  && node_name != "kurento-client-elements"
  && node_name != "kurento-client-filters">
  "peerDependencies": {
    "kurento-client": "${module.kurentoVersion}"
  },
</#if>
  "devDependencies": {
    "bower": "~1.3.12",
    "grunt": "~0.4.5",
    "grunt-browserify": "^3.1.0",
    "grunt-cli": "~0.1.13",
    "grunt-contrib-clean": "~0.6.0",
    "grunt-jsdoc": "~0.5.7",
    "grunt-npm2bower-sync": "^0.4.0",
    "grunt-shell": "^1.1.1",
    "minifyify": "^4.4.0"
  }
}
