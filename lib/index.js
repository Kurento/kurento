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
 *
 *
 * @module
 *
 * @copyright 2013 Kurento (http://kurento.org/)
 * @license LGPL
 */

var EventEmitter = require('events').EventEmitter;

var RpcBuilder = require('rpc-builder');
var WebSocket = require('ws');
//var XMLHttpRequest = require("xmlhttprequest");

var MediaPipeline = require('./MediaPipeline');


/**
 * Creates a connection with the Kurento Media Server
 *
 * @class
 *
 * @param {string} uri - Address of the Kurento Media Server
 */
function KwsMedia(uri, options, onconnect)
{
  if(!(this instanceof KwsMedia))
    return new KwsMedia(uri, options, onconnect);

  var self = this;

  EventEmitter.call(this);


  // Fix optional parameters
  if(options instanceof Function)
  {
    onconnect = options;
    options = null;
  };

  options = options || {};


  // Register connect event callback
  if(onconnect)
    this.on('connect', onconnect);


  var objects = {};


  function objectId(objectRef)
  {
    return objectRef.id +":"+ objectRef.token;
  };


  function onNotification(message)
  {
    var method = message.method;
    var params = message.params;

    var object_id = objectId(params.object);

    var object = objects[object_id];
    if(!object)
    {
      console.warning("Unknown object id "+object_id);
      console.warning(message);
      return;
    };

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
    };
  };


  //
  // JsonRPC
  //

  var rpc = new RpcBuilder();

  var ws = (typeof uri == 'string') ? new WebSocket(uri, null, options) : uri;

  ws.addEventListener('open', function()
  {
    self.emit('connect', self);
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
    };
  });

//  var ws = new XMLHttpRequest();
//  ws.open('POST', uri);
//  ws.onload = function(event)
//  {
//    var message = rpc.decodeJSON(event.data);
//
//    switch(typeof message)
//    {
//      case 'RpcRequest':
//        console.warning("Unexpected request");
//        console.warning(message);
//      break;
//
//      case 'RpcNotification':
//        onNotification(message);
//      break;
//    };
//  };
 
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
      params.object =
      {
        id:    mediaObject.id,
        token: mediaObject.token
      };

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
    };

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
};
KwsMedia.prototype.__proto__   = EventEmitter.prototype;
KwsMedia.prototype.constructor = KwsMedia;


module.exports = KwsMedia;