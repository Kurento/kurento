<#include "macros.ftm" >
<#assign namespace>
  <#switch getJsNamespace(remoteClass)>
    <#case "Filter">filters<#break>
    <#case "Endpoint">endpoints<#break>
    <#case "Hub">hubs<#break>
  </#switch>
</#assign>
<#if remoteClass.extends??>
  <#assign extends_name>
    <#switch remoteClass.name>
      <#case "Hub">
      <#case "MediaElement">
      <#case "MediaPipeline"><#lt>MediaContainer<#break>
      <#default>${remoteClass.extends.name}<#break>
    </#switch>
  </#assign>
</#if>
<#assign filename>
  <#switch remoteClass.name>
    <#case "MediaElement"><#break>
    <#case "MediaObject"><#break>
    <#case "MediaPad"><#break>
    <#case "MediaPipeline"><#break>
    <#case "MediaSink"><#break>
    <#case "MediaSource"><#break>
    <#default><#lt><#if namespace != "">${namespace}/</#if>${remoteClass.name}.js<#rt><#break>
  </#switch>
</#assign>
${filename}
/*
 * (C) Copyright 2013-2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

var extend   = require('extend');
var inherits = require('inherits');
<#if remoteClass.methods?has_content>

var checkType = require('<#if namespace != "">..<#else>.</#if>/checkType');
</#if>


/**
 * Media API for the Kurento Web SDK
 *
 * @module kwsMediaApi<#if namespace != "">/${namespace}</#if>
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */
<#if remoteClass.extends??>

var ${extends_name} = require('<#if getJsNamespace(remoteClass) == getJsNamespace(remoteClass.extends.type)>.<#else>..</#if>/${extends_name}');
</#if>


/**
<#if remoteClass.doc??>
 * ${remoteClass.doc}
</#if>
 *
<#if remoteClass.abstract>
 * @abstract
</#if>
 * @class   module:kwsMediaApi<#if namespace != "">/${namespace}</#if>~${remoteClass.name}
<#if remoteClass.extends??>
 * @extends module:kwsMediaApi~${extends_name}
</#if>
 */

/**
<#if remoteClass.constructors[0]??>
 * ${remoteClass.constructors[0].doc}
</#if>
 *
 * @constructor
 *
 * @param {string} id
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} pipeline
 * @param {module:kwsMediaApi<#if namespace != "">/${namespace}</#if>~${remoteClass.name}.constructorParams} params
 */
function ${remoteClass.name}(id, parent, pipeline, params)
{
<#if remoteClass.extends??>
  ${extends_name}.call(this, id, parent, pipeline, params);
</#if>
};
<#if remoteClass.extends??>
inherits(${remoteClass.name}, ${extends_name});
</#if>

<#if remoteClass.methods?has_content>
  <#list remoteClass.methods?sort_by("name") as method>

    <#assign methodParams_name=[]>
    <#list method.params as param>
      <#assign methodParams_name=methodParams_name+[param.name]>
    </#list>
/**
    <#if method.doc??>
 * ${method.doc}
    </#if>
    <#list method.params as param>
 *
 * @param {${param.type.name}} ${param.name}
      <#if param.doc??>
 *  ${param.doc}
      </#if>
    </#list>
 *
 * @param {module:kwsMediaApi<#if namespace != "">/${namespace}</#if>~${remoteClass.name}.${method.name}Callback} [callback]
 *
 * @return {module:kwsMediaApi<#if namespace != "">/${namespace}</#if>~${remoteClass.name}}
 *  The own media object
 */
${remoteClass.name}.prototype.${method.name} = function(<@join sequence=(methodParams_name + ["callback"]) separator=", "/>){
    <#if method.params?has_content>
      <#list method.params as param>
  checkType('${param.type.name}', '${param.name}', ${param.name}, {<#if param.type.isList()>isList: true,</#if><#if !param.optional>required: true</#if>});
      </#list>

  var params = {
      <#list methodParams_name as name>
    ${name}: ${name},
      </#list>
  };

    </#if>
  this.invoke('${method.name}'<#if method.params?has_content>, params</#if>, callback);

  return this;
};
/**
 * @callback ${remoteClass.name}~${method.name}Callback
 * @param {Error} error
    <#if method.return??>
 * @param {${method.return.type.name}} result
 *  ${method.return.doc}
    </#if>
 */
  </#list>
</#if>

/**
 * @type   module:kwsMediaApi<#if namespace != "">/${namespace}</#if>~${remoteClass.name}.constructorParams
<#if remoteClass.extends??>
 * @extend module:kwsMediaApi~${extends_name}.constructorParams
</#if>
<#if remoteClass.constructors[0]??>
  <#list remoteClass.constructors[0].params?sort_by("name") as param>
 *
 * @property {${param.type.name}} <#if param.optional>[${param.name}]<#else>${param.name}</#if>
 *  ${param.doc}
  </#list>
</#if>
 */
${remoteClass.name}.constructorParams = {<#list (remoteClass.constructors[0].params?sort_by("name"))![] as param>
  ${param.name}: {
    type: '${param.type.name}',
<#if param.type.isList()>
    isList: true,
</#if>
<#if !param.optional>
    required: true
</#if>
  },
</#list>};
<#if remoteClass.extends??>
extend(${remoteClass.name}.constructorParams, ${extends_name}.constructorParams);
</#if>

/**
 * @type   module:kwsMediaApi<#if namespace != "">/${namespace}</#if>~${remoteClass.name}.events
<#if remoteClass.extends??>
 * @extend module:kwsMediaApi~${extends_name}.events
</#if>
 */
<#assign remoteClassEvents_name=[]>
<#list remoteClass.events?sort_by("name") as event>
  <#assign remoteClassEvents_name=remoteClassEvents_name+["'"+event.name+"'"]>
</#list>
${remoteClass.name}.events = [<@join sequence=remoteClassEvents_name separator=", "/>];
<#if remoteClass.extends??>
${remoteClass.name}.events.concat(${extends_name}.events);
</#if>
<#if !remoteClass.abstract>


/**
 *
 *
 * @param {module:kwsMediaApi<#if namespace != "">/${namespace}</#if>~${remoteClass.name}.constructorParams} params
 */
${remoteClass.name}.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('${remoteClass.name}', params, callback);
};
</#if>


module.exports = ${remoteClass.name};
