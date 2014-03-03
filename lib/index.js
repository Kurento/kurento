/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

var RpcBuilder = require('kws-rpc-builder');
var WebSocket = require('ws');
var MediaPipeline = require('./MediaPipeline');


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
  }

  options = options || {};

  // Register connect and connection error event callback
  if(onconnect)
    this.on('connect', onconnect);
  if(onerror)
    this.on('error', onerror);


  var objects = {};


  function objectId(objectRef)
  {
    return objectRef.id +":"+ objectRef.token;
  }


  function onNotification(message)
  {
    var method = message.method;
    var params = message.params;

    var object_id = objectId(params.source);

    var object = objects[object_id];
    if(!object)
    {
      console.warning("Unknown object id '"+object_id+"'", message);
      return;
    }

    switch(method)
    {
      case 'onEvent':
        object.emit(params.type, params.data);
      break;

      case 'onError':
        object.emit('error', params.error);
      break;

      default:
        console.warn("Unknown message type '"+method+"'");
    }
  }


  //
  // JsonRPC
  //

  var rpc = new RpcBuilder();

  var ws = (typeof uri == 'string') ? new WebSocket(uri, null, options) : uri;

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
  });

  ws.addEventListener('close', function()
  {
    self.emit('disconnect');
  });

  ws.addEventListener('message', function(event)
  {
    var message = rpc.decodeJSON(event.data);
    
    switch(typeof message)
    {
      case 'RpcRequest':
        console.warning("Unexpected request");
        console.warning(message);
      break;

      case 'RpcNotification':
        onNotification(message);
      break;
    }
  });
 
  //
  // Register created objects
  //

  /**
   * A new MediaObject has been created
   */
  this.on('mediaObject', function(mediaObject)
  {
    var object_id = objectId(mediaObject);

    objects[object_id] = mediaObject;


    /**
     * Request to release the object on the server and remove it from cache
     */
    mediaObject.on('release', function()
    {
      delete objects[object_id];
    });

    /**
     * Request a generic functionality to be procesed by the server
     */
    mediaObject.on('_rpc', function(method, params, callback)
    {
      ws.send(rpc.encodeJSON(method, params, callback));
    });
  });


  /**
   *
   *
   * @param {Object} [params] -
   * @callback {createMediaObjectCallback} [callback]
   *
   * @return {KwsMedia} The own pipeline
   */
  this.createMediaPipeline = function(params, callback)
  {
    // Fix optional parameters
    if(params instanceof Function)
    {
      if(callback)
        throw new SyntaxError("Nothing can be defined after the callback");

      callback = params;
      params = null;
    }

    callback = callback || function(){};

    if(params)
      params =
      {
        params: MediaPipeline.checkParams(params)
      };

    // Do the request
    ws.send(rpc.encodeJSON('createMediaPipeline', params, function(error, result)
    {
      if(error) return callback(error);

      var mediaPipeline = new MediaPipeline(result, self);

      // Exec successful callback if it's defined
      callback(null, mediaPipeline);
    }));

    return this;
  };
}
KwsMedia.prototype.__proto__   = EventEmitter.prototype;
KwsMedia.prototype.constructor = KwsMedia;

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
