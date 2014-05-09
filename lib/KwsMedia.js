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

/**
 * Media API for the Kurento Web SDK
 *
 * @module KwsMedia
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var EventEmitter = require('events').EventEmitter;
var inherits     = require('inherits');
var url          = require('url');

var async = require('async');
var WebSocket = require('ws');

var RpcBuilder = require('kws-rpc-builder');
var JsonRPC    = RpcBuilder.packers.JsonRPC;

var checkType   = require('./checkType');
var checkParams = checkType.checkParams;

var core      = require('./core');
var endpoints = require('./endpoints');
var filters   = require('./filters');
var hubs      = require('./hubs');

var Hub          = require('./core/Hub');
var MediaElement = require('./core/MediaElement');

var HubPort       = core.HubPort;
var MediaPipeline = core.MediaPipeline;

var noop = require('./utils').noop;


/**
 * Creates a connection with the Kurento Media Server
 *
 * @class
 *
 * @param {external:String} uri - Address of the Kurento Media Server
 */
function KwsMedia(uri, options, onconnect, onerror)
{
  if(!(this instanceof KwsMedia))
    return new KwsMedia(uri, options, onconnect, onerror);

  var self = this;

  EventEmitter.call(this);


  // Fix optional parameters
  if(options instanceof Function)
  {
    onerror   = onconnect;
    onconnect = options;
    options   = undefined;
  };

  options = options || {};


  // Register connect and connection error event callback
  if(onconnect)
    this.on('connect', onconnect);
  if(onerror)
    this.on('error', onerror);


  var objects = {};


  function onNotification(message)
  {
    var method = message.method;
    var params = message.params.value;

    var id = params.object;

    var object = objects[id];
    if(!object)
      return console.warn("Unknown object id '"+id+"'", message);

    switch(method)
    {
      case 'onEvent':
        object.emit(params.type, params.data);
      break;

//      case 'onError':
//        object.emit('error', params.error);
//      break;

      default:
        console.warn("Unknown message type '"+method+"'");
    };
  };


  //
  // JsonRPC
  //

  var rpc = new RpcBuilder(JsonRPC);

  var ws;
  if(typeof uri == 'string')
  {
    var access_token = options.access_token;
    if(access_token != undefined)
    {
      uri = url.parse(uri, true);
      uri.query.access_token = access_token;
      uri = url.format(uri);

      delete options.access_token;
    };

    ws = new WebSocket(uri, null, options);
  }

  // URI is the WebSocket itself
  else
    ws = uri;

  ws.addEventListener('open', function()
  {
    self.emit('connect', self);

    // We connected successfully, remove connection error callback
    if(onerror)
      self.removeListener('error', onerror);
  });

  ws.addEventListener('error', function(error)
  {
    self.emit('error', error);
//    self.emit('error', new Error('Connection error'));
  });

  ws.addEventListener('close', function(event)
  {
    self.emit('disconnect', event);
  });

  ws.addEventListener('message', function(event)
  {
    var message = rpc.decode(event.data);

    // Response was processed, do nothing
    if(message == undefined) return;

    if(message instanceof RpcBuilder.RpcNotification)
    {
      // Message is an unexpected request, notify error
      if(message.duplicated != undefined)
        return console.warning("Unexpected request", message);

      // Message is a notification, process it
      return onNotification(message);
    }

    // Invalid message, notify error
    console.error('Invalid message instance', message);
  });


  this.close = function()
  {
    ws.close();
  };


  function createObject(constructor, id, params)
  {
    var mediaObject = new constructor(id, params);

    /**
     * Request to release the object on the server and remove it from cache
     */
    mediaObject.on('release', function()
    {
      delete objects[id];
    });

    /**
     * Request a generic functionality to be procesed by the server
     */
    mediaObject.on('_rpc', function(method, params, callback)
    {
      params.object = id;

      ws.send(rpc.encode(method, params, callback));
    });

    if(mediaObject instanceof Hub
    || mediaObject instanceof MediaPipeline)
      mediaObject.on('_create', function(type, params, callback)
      {
        self.create(type, params, callback);
      });

    objects[id] = mediaObject;

    return mediaObject;
  };

  /**
   * Request to the server to create a new MediaElement
   */
  function createMediaObject(item, callback)
  {
    var type = item.type;

    // If element type is not registered, use generic MediaObject
    var constructor = core[type] || endpoints[type] || filters[type]
                   || hubs[type] || MediaObject;

    item.constructorParams = checkParams(item.params, 
                                         constructor.constructorParams, type);
    delete item.params;

    if(type == 'HubPort')
    {
      var hub = item.constructorParams.hub;
      item.constructorParams.hub = hub.id;
    };

    var mediaPipeline = item.constructorParams.mediaPipeline;
    if(mediaPipeline)
      item.constructorParams.mediaPipeline = mediaPipeline.id;

    ws.send(rpc.encode('create', item, function(error, result)
    {
      if(error) return callback(error);

      var id     = result.value;
      var params = item.params;

      var mediaObject = objects[id];
      if(mediaObject) return callback(null, mediaObject);

      callback(null, createObject(constructor, id));
    }));
  };

  function describe(id, callback)
  {
    var mediaObject = objects[id];
    if(mediaObject) return callback(null, mediaObject);

    ws.send(rpc.encode('describe', {id: id}, function(error, result)
    {
      if(error) return callback(error);

      var type = result.type;

      // If element type is not registered, use generic MediaObject
      var constructor = core[type] || endpoints[type] || filters[type]
                     || hubs[type] || MediaObject;

      return callback(null, createObject(constructor, id));
    }));
  };


  this.getMediaobjectById = function(id, callback)
  {
    callback = callback || noop;

    if(id instanceof Array)
      async.map(id, describe, callback);
    else
      describe(id, callback);

    return this;
  };


  /**
   * Create a new instance of a MediaObject
   *
   * @param {external:String} type - Type of the element
   * @param {external:string[]} [params]
   * @callback {createMediaPipelineCallback} callback
   *
   * @return {module:kwsMediaApi~MediaPipeline} The pipeline itself
   */
  this.create = function(type, params, callback)
  {
    // Fix optional parameters
    if(params instanceof Function)
    {
      if(callback)
        throw new SyntaxError("Nothing can be defined after the callback");

      callback = params;
      params   = undefined;
    };

    callback = callback || noop;
    params   = params   || {};

    if(type instanceof Array)
      async.map(type, createMediaObject, callback);
    else
      createMediaObject({params: params, type: type}, callback);

    return this;
  };
};
inherits(KwsMedia, EventEmitter);


/**
 * Connect the source of a media to the sink of the next one
 *
 * @param {...MediaObject} media - A media to be connected
 * @callback {createMediaObjectCallback} [callback]
 *
 * @return {module:kwsMediaApi~MediaPipeline} The pipeline itself
 *
 * @throws {SyntaxError}
 */
KwsMedia.prototype.connect = function(media, callback)
{
  // Fix lenght-variable arguments
  media = Array.prototype.slice.call(arguments, 0);
  callback = (typeof media[media.length - 1] == 'function') ? media.pop() : noop;

  // Check if we have enought media components
  if(media.length < 2)
    throw new SyntaxError("Need at least two media elements to connect");

  // Check MediaElements are of the correct type
  media.forEach(function(element)
  {
    checkType('MediaElement', 'media', element);
  });

  // Connect the media elements
  var src = media[0];

  async.each(media.slice(1), function(sink, callback)
  {
    src.connect(sink, callback);
    src = sink;
  }, callback);

  // Allow method chaining
  return this;
};


/**
 * The built in number object.
 * @external Number
 * @see {@link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Number Number}
 */

/**
 * The built in string object.
 * @external String
 * @see {@link https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/String String}
 */


/**
 * Node.js EventEmitter object.
 * @external EventEmitter
 * @see {@link http://nodejs.org/api/events.html#events_class_events_eventemitter EventEmitter}
 */


module.exports = KwsMedia;
