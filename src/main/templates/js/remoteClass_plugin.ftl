<#include "macros.ftm" >
<#assign namespace=model.name>
<#assign extends_name>
  <#if remoteClass.name=="MediaObject">
    <#lt>_MediaObject<#rt>
  <#elseif remoteClass.extends??>
    <#lt>${remoteClass.extends.name}<#rt>
  </#if>
</#assign>
<#if remoteClass.abstract>abstracts/</#if>${remoteClass.name}
/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

var inherits = require('inherits');
<#include "sugarSyntax1.ftm" >
<#if remoteClass.methods?has_content>

var checkType = require('kws-media-runtime').checkType;
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
  <#assign import_name>
    <#list model.imports as import>
      <#list import.model.remoteClasses as remoteClass>
        <#if remoteClass.name == extends_name>
          <#lt>${import.name}<#rt>
        </#if>
      </#list>
    </#list>
  </#assign>

  <#if import_name == ''>
    <#assign import_name=namespace>
var ${extends_name} = require('./<#if (remoteClass.abstract?? && remoteClass.abstract) == (remoteClass.extends.abstract?? && remoteClass.extends.abstract)>abstracts/</#if>${extends_name}');
  <#else>
var ${extends_name} = require('${import_name}').${extends_name};
  </#if>
<#elseif remoteClass.name=="MediaObject">

var ${extends_name} = require('kws-media-runtime').${extends_name};
</#if>


/**
<#if remoteClass.doc??>
 * ${remoteClass.doc}
 *
</#if>
<#if remoteClass.abstract>
 * @abstract
</#if>
 * @class   module:kwsMediaApi<#if namespace != "">/${namespace}</#if>~${remoteClass.name}
<#if remoteClass.extends??>
 * @extends module:${import_name}~${extends_name}
</#if>
 */

/**
<#if remoteClass.constructor??>
 * ${remoteClass.constructor.doc}
 *
</#if>
 * @constructor
 *
 * @param {string} id
 */
function ${remoteClass.name}(id)
{
<#if extends_name??>
  ${extends_name}.call(this, id);
</#if>
};
<#if extends_name??>
inherits(${remoteClass.name}, ${extends_name});
</#if>
<#if remoteClass.properties?has_content>

  <#list remoteClass.properties?sort_by("name") as property>
    <#if property.name != "id">

/**
      <#if property.doc??>
 * ${property.doc}
      </#if>
 *
 * @param {module:kwsMediaApi<#if namespace != "">/${namespace}</#if>~${remoteClass.name}.${property.name}Callback} [callback]
 *
 * @return {module:kwsMediaApi<#if namespace != "">/${namespace}</#if>~${remoteClass.name}}
 *  The own media object
 */
      <#assign getPropertyName="get${property.name?cap_first}">
${remoteClass.name}.prototype.${getPropertyName} = function(callback){
  return this.invoke('${getPropertyName}', callback);
};
/**
 * @callback ${remoteClass.name}~${getPropertyName}Callback
 * @param {Error} error
 * @param {${property.type.name}} result
 */
    </#if>
  </#list>
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
 * @param {${param.type.name}}<#if param.type.isList()>[]</#if> <#if param.optional>[${param.name}]<#else>${param.name}</#if>
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
        <#if param.optional>
  callback = arguments[arguments.length-1] instanceof Function
           ? Array.prototype.pop.call(arguments)
           : undefined;

//  eval(['<@join sequence=methodParams_name separator="', '"/>'][arguments.length]+'=undefined')
  if(callback)
    switch(arguments.length){
          <#list method.params as param>
            <#if param.optional>
      case ${param_index}: ${param.name} = undefined; break;
            </#if>
          </#list>
    }

          <#break>
        </#if>
      </#list>
      <#list method.params as param>
  checkType('${param.type.name}', '${param.name}', ${param.name}<#if param.type.isList() || !param.optional>, {<#if param.type.isList()>isList: true,</#if><#if !param.optional>required: true</#if>}</#if>);
      </#list>

  var params = {
      <#list methodParams_name as name>
    ${name}: ${name},
      </#list>
  };

    </#if>
  return this.invoke('${method.name}'<#if method.params?has_content>, params</#if>, callback);
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

<#include "sugarSyntax2.ftm" >

/**
 * @type module:kwsMediaApi<#if namespace != "">/${namespace}</#if>~${remoteClass.name}.constructorParams
<#if remoteClass.constructor??>
  <#list remoteClass.constructor.params?sort_by("name") as param>
 *
 * @property {${param.type.name}} <#if param.optional>[${param.name}]<#else>${param.name}</#if>
 *  ${param.doc}
  </#list>
</#if>
 */
${remoteClass.name}.constructorParams = {<#list (remoteClass.constructor.params?sort_by("name"))![] as param>
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

/**
 * @type <#if remoteClass.extends??>  </#if>module:kwsMediaApi<#if namespace != "">/${namespace}</#if>~${remoteClass.name}.events
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


module.exports = ${remoteClass.name};


${remoteClass.name}.check = function(key, value)
{
  if(!(value instanceof ${remoteClass.name}))
    throw SyntaxError(key+' param should be a ${remoteClass.name}, not '+typeof value);
};
