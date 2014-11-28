<#assign api_js=module.code.api.js>
<#assign node_name=api_js.nodeName>
package.json
{
  "name": "${node_name}",
  "version": "${module.version}",
  "description": "${module.code.api.js.npmDescription}",
  "main": "lib/index.js",
  "keywords": [
    "Kurento"
  ],
<#if api_js.npmGit??>
  "repository": {
    "type": "git",
    "url": "${module.code.api.js.npmGit}"
  },
</#if>
  "scripts": {
    "prepublish": "node scripts/prepublish"
  },
<#if node_name != "kurento-client-core"
  && node_name != "kurento-client-elements"
  && node_name != "kurento-client-filters">
  "peerDependencies": {
    "kurento-client": "${module.kurentoVersion}"
  },
</#if>
  "dependencies": {
    "es6-promise": "^2.0.0",
    "inherits": "^2.0.1",
    "promisecallback": "^0.0.2"
  },
  "devDependencies": {
<#list module.imports as import>
  <#assign api_js=import.module.code.api.js>
    "${api_js.nodeName}": "${api_js.npmVersion}",
</#list>
<#if node_name != "kurento-client-core"
  && node_name != "kurento-client-elements"
  && node_name != "kurento-client-filters">
    "bower": "~1.3.12",
    "grunt": "~0.4.5",
    "grunt-browserify": "^3.1.0",
    "grunt-cli": "~0.1.13",
    "grunt-jsdoc": "~0.5.7",
  <#if api_js.npmGit??>
    "grunt-npm2bower-sync": "^0.4.0",
  </#if>
    "kurento-client": "${module.kurentoVersion}",
    "minifyify": "^4.4.0",
</#if>
    "grunt-contrib-clean": "~0.6.0",
    "grunt-path-check": "^0.9.3",
    "grunt-shell": "^1.1.1"
  }
}
