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
 * @module KurentoClient
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var EventEmitter = require('events').EventEmitter;
var extend       = require('extend');
var inherits     = require('inherits');
var url          = require('url');

var Promise = require('es6-promise').Promise;

var async     = require('async');
var reconnect = require('reconnect-ws');

var RpcBuilder = require('kurento-jsonrpc');
var JsonRPC    = RpcBuilder.packers.JsonRPC;

var checkType   = require('checktype');
var checkParams = checkType.checkParams;

var promiseCallback = require('promisecallback');

var register = require('./register');


/**
 * Serialize objects using their id
 */
function serializeParams(params)
{
  for(var key in params)
  {
    var param = params[key];
    if(param instanceof register.abstracts.MediaObject)
      params[key] = param.id;
  };

  return params;
};

/**
 * Get the constructor for a type
 *
 * If the type is not registered, use generic {MediaObject}
 */
function getConstructor(type)
{
  return register.classes[type]
      || register.abstracts[type]
      || register.abstracts.MediaObject;
};

function createConstructor(item)
{
  var constructor = getConstructor(item.type);

  if(constructor.create)
  {
    item = constructor.create(item.params);

    // Apply inheritance
    var prototype = constructor.prototype;
    inherits(constructor, getConstructor(item.type));
    extend(constructor.prototype, prototype);
  };

  constructor.item = item;

  return constructor;
}

function createPromise(data, func, callback)
{
  var promise = new Promise(function(resolve, reject)
  {
    function callback2(error, result)
    {
      if(error) return reject(error);

      resolve(result);
    };

    if(data instanceof Array)
      async.map(data, func, callback2);
    else
      func(data, callback2);
  });

  return promiseCallback(promise, callback);
};


/**
 * Creates a connection with the Kurento Media Server
 *
 * @class
 *
 * @param {external:String} ws_uri - Address of the Kurento Media Server
 */
function KurentoClient(ws_uri, options, callback)
{
  if(!(this instanceof KurentoClient))
    return new KurentoClient(ws_uri, options, callback);

  var self = this;

  EventEmitter.call(this);


  // Fix optional parameters
  if(options instanceof Function)
  {
    callback = options;
    options  = undefined;
  };

  options = options || {};

  var failAfter = options.failAfter
  if(failAfter == undefined) failAfter = 5


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

  if(typeof ws_uri == 'string')
  {
    var access_token = options.access_token;
    if(access_token != undefined)
    {
      ws_uri = url.parse(ws_uri, true);
      ws_uri.query.access_token = access_token;
      ws_uri = url.format(ws_uri);

      delete options.access_token;
    };
  }

  var rpc = new RpcBuilder(JsonRPC, function(request)
  {
    if(request instanceof RpcBuilder.RpcNotification)
    {
      // Message is an unexpected request, notify error
      if(request.duplicated != undefined)
        return console.warning('Unexpected request:', request);

      // Message is a notification, process it
      return onNotification(request);
    };

    // Invalid message, notify error
    console.error('Invalid request instance', request);
  });


  // Reconnect websockets

  var re = reconnect({failAfter: failAfter}, function(ws_stream)
  {
    rpc.transport = ws_stream;
  })
  .connect(ws_uri);

  this.close = re.disconnect.bind(re);

  re.on('fail', this.emit.bind(this, 'disconnect'));


  // Promise interface ("thenable")

  this.then = function(onFulfilled, onRejected)
  {
    return new Promise(function(resolve, reject)
    {
      function removeListeners()
      {
        re.removeListener('connection', success);
        re.removeListener('fail',       failure);
      }

      function success()
      {
        removeListeners()

        var result;

        if(onFulfilled)
          try
          {
            result = onFulfilled(self);
          }
          catch(exception)
          {
            if(!onRejected)
              console.trace('Uncaugh exception', exception)

            return reject(exception);
          }

        resolve(result);
      };
      function failure()
      {
        removeListeners()

        var result = new Error('Connection error');

        if(onRejected)
          try
          {
            result = onRejected(result);
          }
          catch(exception)
          {
            return reject(exception);
          }
        else
          console.trace('Uncaugh exception', result)

        reject(result);
      };

      if(re.connected)
        success()
      else if(!re.reconnect)
        failure()
      else
      {
        re.on('connection', success);
        re.on('fail',       failure);
      }
    });
  };

  this.catch = this.then.bind(this, null);

  if(callback)
    this.then(callback.bind(undefined, null), callback);


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

      // Serialize objects using their id
      params.operationParams = serializeParams(params.operationParams);

      rpc.encode(method, params, function(error, result)
      {
        if(error) return callback(error);

        var operation = params.operation;

        if(operation == 'getConnectedSinks'
        || operation == 'getMediaSinks'
        || operation == 'getMediaSrcs')
        {
          var sessionId = result.sessionId;

          return self.getMediaobjectById(result.value, function(error, result)
          {
            var result =
            {
              sessionId: sessionId,
              value: result
            };

            callback(error, result);
          });
        };

        callback(null, result);
      });
    });

    if(mediaObject instanceof register.abstracts.Hub
    || mediaObject instanceof register.classes.MediaPipeline)
      mediaObject.on('_create', self.create.bind(self));

    objects[id] = mediaObject;

    return mediaObject;
  };

  /**
   * Request to the server to create a new MediaElement
   */
  function createMediaObject(item, callback)
  {
    var constructor = createConstructor(item);

    item = constructor.item;
    delete constructor.item;

    item.constructorParams = checkParams(item.params,
                                      constructor.constructorParams, item.type);
    delete item.params;

    // Serialize objects using their id
    item.constructorParams = serializeParams(item.constructorParams);

    rpc.encode('create', item, function(error, result)
    {
      if(error) return callback(error);

      var id = result.value;

      callback(null, objects[id] || createObject(constructor, id));
    });
  };

  function describe(id, callback)
  {
    var mediaObject = objects[id];
    if(mediaObject) return callback(null, mediaObject);

    rpc.encode('describe', {object: id}, function(error, result)
    {
      if(error) return callback(error);

      var constructor = createConstructor(result);
      delete constructor.item;

      return callback(null, createObject(constructor, id));
    });
  };


  this.getMediaobjectById = function(id, callback)
  {
    return createPromise(id, describe, callback)
  };


  /**
   * Create a new instance of a MediaObject
   *
   * @param {external:String} type - Type of the element
   * @param {external:string[]} [params]
   * @callback {createMediaPipelineCallback} callback
   *
   * @return {module:KurentoClientApi~MediaPipeline} The pipeline itself
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

    if(!(type instanceof Array))
      type = {params: params || {}, type: type};

    return createPromise(type, createMediaObject, callback)
  };
};
inherits(KurentoClient, EventEmitter);


var checkMediaElement = checkType.bind(null, 'MediaElement', 'media');

/**
 * Connect the source of a media to the sink of the next one
 *
 * @param {...MediaObject} media - A media to be connected
 * @callback {createMediaObjectCallback} [callback]
 *
 * @return {module:KurentoClientApi~MediaPipeline} The pipeline itself
 *
 * @throws {SyntaxError}
 */
KurentoClient.prototype.connect = function(media, callback)
{
  // Fix lenght-variable arguments
  media = Array.prototype.slice.call(arguments, 0);
  callback = (typeof media[media.length - 1] == 'function')
           ? media.pop() : undefined;

  // Check if we have enought media components
  if(media.length < 2)
    throw new SyntaxError("Need at least two media elements to connect");

  // Check MediaElements are of the correct type
  media.forEach(checkMediaElement);

  // Generate promise
  var promise = new Promise(function(resolve, reject)
  {
    function callback(error, result)
    {
      if(error) return reject(error);

      resolve(result);
    };

    // Connect the media elements
    var src = media[0];

    async.each(media.slice(1), function(sink, callback)
    {
      src.connect(sink, callback);
      src = sink;
    }, callback);
  });

  return promiseCallback(promise, callback);
};


// Export KurentoClient

module.exports = KurentoClient;
KurentoClient.KurentoClient = KurentoClient;

KurentoClient.register = register;


// Register Kurento basic elements

register(require('kurento-client-core'))
register(require('kurento-client-elements'))
register(require('kurento-client-filters'))
