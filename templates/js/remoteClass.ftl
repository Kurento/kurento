<#include "macros.ftm" >
<#assign namespace>
  <#switch getJsNamespace(remoteClass)>
    <#case "Filter">filters<#break>
    <#case "Endpoint">endpoints<#break>
    <#case "Hub">hubs<#break>
    <#default>core<#break>
  </#switch>
</#assign>
<#assign filename>
  <#lt><#if namespace != "">${namespace}/</#if>${remoteClass.name}.js<#rt>
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

var ${remoteClass.extends.name} = require('<#if getJsNamespace(remoteClass) == getJsNamespace(remoteClass.extends.type)>.<#else>../core</#if>/${remoteClass.extends.name}');
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
 * @extends module:kwsMediaApi~${remoteClass.extends.name}
</#if>
 */

/**
<#if remoteClass.constructors[0]??>
 * ${remoteClass.constructors[0].doc}
 *
</#if>
 * @constructor
 *
 * @param {string} id
 */
function ${remoteClass.name}(id)
{
<#if remoteClass.extends??>
  ${remoteClass.extends.name}.call(this, id);
</#if>
};
<#if remoteClass.extends??>
inherits(${remoteClass.name}, ${remoteClass.extends.name});
</#if>
<#if remoteClass.methods?has_content>

  <#assign mediaElement_processedDuplicates=false>
  <#list remoteClass.methods?sort_by("name") as method>

    <#if remoteClass.name == "MediaElement" && (method.name == "connect" || method.name == "getMediaSinks" || method.name == "getMediaSrcs")>
      <#if !mediaElement_processedDuplicates>
/**
 * perform :rom:meth:`connect(sink,mediaType)` if there is exactly one sink for the given type, and their mediaDescriptions are the same
 *
 * @param {MediaElement} sink
 *  the target :rom:cls:`MediaElement`  from which :rom:cls:`MediaSink` will be obtained
 *
 * @param {MediaPad.MediaType} [mediaType]
 *  the :rom:enum:`MediaType` of the pads that will be connected
 *
 * @param {external:String} [mediaDescription]
 *  A textual description of the media source. Currently not used, aimed mainly for :rom:attr:`MediaType.DATA` sources
 *
 * @param {module:kwsMediaApi/core~MediaElement.connectCallback} [callback]
 *
 * @return {module:kwsMediaApi/core~MediaElement}
 *  The own media object
 */
MediaElement.prototype.connect = function(sink, mediaType, mediaDescription, callback){
  // Fix optional parameters
  if(mediaType instanceof Function)
  {
    if(mediaDescription)
      throw new SyntaxError("Nothing can be defined after the callback");

    callback = mediaType;
    mediaDescription = undefined;
    mediaType = undefined;
  }

  else if(mediaDescription instanceof Function)
  {
    if(callback)
      throw new SyntaxError("Nothing can be defined after the callback");

    callback = mediaDescription;
    mediaDescription = undefined;
  };

  if(!mediaType && mediaDescription)
    throw new SyntaxError("'mediaType' is undefined while 'mediaDescription' is not");

  checkType('MediaElement', 'sink', sink, {required: true});
  checkType('MediaType', 'mediaType', mediaType);
  checkType('String', 'mediaDescription', mediaDescription);

  var params = {
    sink: sink.id,
    mediaType: mediaType,
    mediaDescription: mediaDescription,
  };

  this.invoke('connect', params, callback);

  return this;
};
/**
 * @callback MediaElement~connectCallback
 * @param {Error} error
 */

/**
 * A list of sinks of the given :rom:ref:`MediaType`. The list will be empty if no sinks are found.
 *
 * @param {MediaPad.MediaType} [mediaType]
 *  One of :rom:attr:`MediaType.AUDIO`, :rom:attr:`MediaType.VIDEO` or :rom:attr:`MediaType.DATA`
 *
 * @param {external:String} [description]
 *  A textual description of the media source. Currently not used, aimed mainly for :rom:attr:`MediaType.DATA` sources
 *
 * @param {module:kwsMediaApi/core~MediaElement.getMediaSinksCallback} [callback]
 *
 * @return {module:kwsMediaApi/core~MediaElement}
 *  The own media object
 */
MediaElement.prototype.getMediaSinks = function(mediaType, description, callback){
  // Fix optional parameters
  if(mediaType instanceof Function)
  {
    if(description)
      throw new SyntaxError("Nothing can be defined after the callback");

    callback = mediaType;
    description = undefined;
    mediaType = undefined;
  }

  else if(description instanceof Function)
  {
    if(callback)
      throw new SyntaxError("Nothing can be defined after the callback");

    callback = description;
    mediaDescription = undefined;
  };

  if(!mediaType && description)
    throw new SyntaxError("'mediaType' is undefined while 'description' is not");

  checkType('MediaType', 'mediaType', mediaType);
  checkType('String', 'description', description);

  var params = {
    mediaType: mediaType,
    description: description,
  };

  this.invoke('getMediaSinks', params, callback);

  return this;
};
/**
 * @callback MediaElement~getMediaSinksCallback
 * @param {Error} error
 * @param {MediaSink} result
 *  A list of sinks. The list will be empty if no sinks are found.
 */

/**
 * Get the media sources of the given type and description
 *
 * @param {MediaPad.MediaType} [mediaType]
 *  One of :rom:attr:`MediaType.AUDIO`, :rom:attr:`MediaType.VIDEO` or :rom:attr:`MediaType.DATA`
 *
 * @param {external:string} [description]
 *  A textual description of the media source. Currently not used, aimed mainly for :rom:attr:`MediaType.DATA` sources
 *
 * @param {module:kwsMediaApi/core~MediaElement.getMediaSrcsCallback} [callback]
 *
 * @return {module:kwsMediaApi/core~MediaElement}
 *  The own media object
 */
MediaElement.prototype.getMediaSrcs = function(mediaType, description, callback){
  // Fix optional parameters
  if(mediaType instanceof Function)
  {
    if(description)
      throw new SyntaxError("Nothing can be defined after the callback");

    callback = mediaType;
    description = undefined;
    mediaType = undefined;
  }

  else if(description instanceof Function)
  {
    if(callback)
      throw new SyntaxError("Nothing can be defined after the callback");

    callback = description;
    mediaDescription = undefined;
  };

  if(!mediaType && description)
    throw new SyntaxError("'mediaType' is undefined while 'description' is not");

  checkType('MediaType', 'mediaType', mediaType);
  checkType('String', 'description', description);

  var params = {
    mediaType: mediaType,
    description: description,
  };

  this.invoke('getMediaSrcs', params, callback);

  return this;
};
/**
 * @callback MediaElement~getMediaSrcsCallback
 * @param {Error} error
 * @param {MediaSource} result
 *  A list of sources. The list will be empty if no sources are found.
 */
      </#if>
      <#assign mediaElement_processedDuplicates=true>
      <#break>
    </#if>
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

<#if remoteClass.name == "Hub">

/**
 * Create a new instance of a {module:kwsMediaApi/core~HubPort} attached to this {module:kwsMediaApi/core~Hub}
 *
 * @callback {createHubCallback} callback
 *
 * @return {module:kwsMediaApi/core~Hub} The hub itself
 */
Hub.prototype.createHubPort = function(callback)
{
  this.emit('_create', 'HubPort', {hub: this.id}, callback);

  return this;
};
/**
 * @callback module:kwsMediaApi/core~Hub~createHubCallback
 * @param {Error} error
 * @param {module:kwsMediaApi/core~HubPort} result
 *  The created HubPort
 */

</#if>
<#if remoteClass.name == "MediaPipeline">

/**
 * Create a new instance of a {module:kwsMediaApi/core~MediaObject} attached to this {module:kwsMediaApi/core~MediaPipeline}
 *
 * @param {external:string} type - Type of the {module:kwsMediaApi/core~MediaObject}
 * @param {external:string[]} [params]
 * @callback {module:kwsMediaApi/core~MediaPipeline~createCallback} callback
 *
 * @return {module:kwsMediaApi/core~MediaPipeline} The pipeline itself
 */
MediaPipeline.prototype.create = function(type, params, callback){
  // Fix optional parameters
  if(params instanceof Function){
    if(callback)
      throw new SyntaxError("Nothing can be defined after the callback");

    callback = params;
    params = {};
  };

  params.mediaPipeline = this;

  this.emit('_create', type, params, callback);

  return this;
};
/**
 * @callback module:kwsMediaApi/core~MediaPipeline~createCallback
 * @param {Error} error
 * @param {module:kwsMediaApi/core~MediaElement} result
 *  The created MediaElement
 */

</#if>
<#if remoteClass.name == "MediaSource">

/**
 * Disconnect this source pad from the specified sink pad
 *
 * @public
 *
 * @param {...module:kwsMediaApi/core~MediaSink} sink - Sink to be disconnected
 * @callback {module:kwsMediaApi/core~MediaSource~disconnectCallback} callback
 *
 * @return {module:kwsMediaApi/core~MediaSource} The own {module:kwsMediaApi/core~MediaSource}
 */
MediaSource.prototype.disconnect = function(sink, callback)
{
  checkType('MediaSink', 'sink', sink, {required: true});

  var params =
  {
    sink: sink
  };

  this.invoke('disconnect', params, callback);

  return this;
};
/**
 * @callback module:kwsMediaApi/core~MediaSource~disconnectCallback
 * @param {Error} error
 */

</#if>

/**
 * @type module:kwsMediaApi<#if namespace != "">/${namespace}</#if>~${remoteClass.name}.constructorParams
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

/**
 * @type <#if remoteClass.extends??>  </#if>module:kwsMediaApi<#if namespace != "">/${namespace}</#if>~${remoteClass.name}.events
<#if remoteClass.extends??>
 * @extend module:kwsMediaApi~${remoteClass.extends.name}.events
</#if>
 */
<#assign remoteClassEvents_name=[]>
<#list remoteClass.events?sort_by("name") as event>
  <#assign remoteClassEvents_name=remoteClassEvents_name+["'"+event.name+"'"]>
</#list>
${remoteClass.name}.events = [<@join sequence=remoteClassEvents_name separator=", "/>];
<#if remoteClass.extends??>
${remoteClass.name}.events.concat(${remoteClass.extends.name}.events);
</#if>


module.exports = ${remoteClass.name};
<#if !remoteClass.abstract>


${remoteClass.name}.check = function(key, value)
{
  if(!(value instanceof ${remoteClass.name}))
    throw SyntaxError(key+' param should be a ${remoteClass.name}, not '+typeof value);
};
</#if>
<#if remoteClass.name == "MediaElement">


MediaElement.check = function(key, value)
{
  if(!(value instanceof MediaElement))
    throw SyntaxError(key+' param should be a MediaElement, not '+typeof value);
};
</#if>
