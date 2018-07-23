<#assign api_js=module.code.api.js>
<#assign node_name=api_js.nodeName>
<#if api_js.npmGit??>
  <#assign bowerGit=api_js.npmGit>
</#if>
package.json
{
  "name": "${node_name}",
  "version": "${module.version}",
  "description": "${api_js.npmDescription}",
<#if bowerGit??>
  "repository": {
    "type": "git",
    "url": "https://github.com/${bowerGit}.git"
  },
</#if>
<#if node_name != "kurento-client-core"
  && node_name != "kurento-client-elements"
  && node_name != "kurento-client-filters">
  "scripts": {
    "prepublish": "grunt"
  },
  "dependencies": {
    "es6-promise": "^2.0.1",
    "inherits": "^2.0.1",
    "promisecallback": "^0.0.3"
  },
  "devDependencies": {
    "bower": "~1.4.1",
    "grunt": "~0.4.5",
    "grunt-browserify": "^3.6.0",
    "grunt-cli": "~0.1.13",
    "grunt-contrib-clean": "~0.6.0",
    "grunt-jsdoc": "^0.6.3",
    "grunt-npm2bower-sync": "^0.9.1",
  <#if bowerGit??>
    "grunt-shell": "^1.1.2",
  </#if>
    "minifyify": "^6.4.0"
  },
</#if>
  "keywords": [
    "Kurento",
    "module",
    "plugin"
  ],
  "main": "lib/index.js"
}
