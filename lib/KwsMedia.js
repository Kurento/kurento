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
var extend       = require('extend');
var inherits     = require('inherits');
var url          = require('url');

var Promise = require('es6-promise').Promise;

var async     = require('async');
var reconnect = require('reconnect-ws');

var RpcBuilder = require('kws-rpc-builder');
var JsonRPC    = RpcBuilder.packers.JsonRPC;

var checkType   = require('checktype');
var checkParams = checkType.checkParams;

var promiseCallback = require('promisecallback');


// Remote classes

var abstracts  = {};
var registered = {};

var registered = {};


/**
 * Serialize objects using their id
 */
function serializeParams(params)
{
  for(var key in params)
  {
    var param = params[key];
    if(param instanceof abstracts.MediaObject)
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
  return registered[type] || abstracts[type] || abstracts.MediaObject;
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
function KwsMedia(ws_uri, options, callback)
{
  if(!(this instanceof KwsMedia))
    return new KwsMedia(ws_uri, options, callback);

  var self = this;

  EventEmitter.call(this);


  // Fix optional parameters
  if(options instanceof Function)
  {
    callback = options;
    options  = undefined;
  };

  options = options || {};


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

  var re = reconnect(function(ws_stream)
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
      function success()
      {
        var result;

        if(onFulfilled)
          try
          {
            result = onFulfilled(self);
          }
          catch(exception)
          {
            return reject(exception);
          }

        resolve(result);
      };
      function failure()
      {
        var result;

        if(onRejected)
          try
          {
            result = onRejected(new Error('Connection error'));
          }
          catch(exception)
          {
            return reject(exception);
          }

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

    if(mediaObject instanceof abstracts.Hub
    || mediaObject instanceof registered.MediaPipeline)
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

    if(!(type instanceof Array))
      type = {params: params || {}, type: type};

    return createPromise(type, createMediaObject, callback)
  };
};
inherits(KwsMedia, EventEmitter);


var checkMediaElement = checkType.bind(null, 'MediaElement', 'media');

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


function registerAbstracts(classes)
{
  for(var name in classes)
  {
    var constructor = classes[name]

    // Register constructor checker
    var check = constructor.check;
    if(check) checkType[name] = check;

    // Register constructor
    abstracts[name] = constructor;
  }
}

function registerClass(name, constructor)
{
  // Register constructor checker
  var check = constructor.check;
  if(check) checkType[name] = check;

  // Register constructor
  registered[name] = constructor;
}

function registerComplexTypes(complexTypes)
{
  for(var name in complexTypes)
    checkType[name] = complexTypes[name];
}


KwsMedia.register = function(name, constructor)
{
  // Adjust parameters
  if(typeof name != 'string')
  {
    type = constructor
    constructor = name
    name = undefined
  }

  // Registering a function
  if(constructor instanceof Function)
  {
    // Registration name
    if(!name)
      name = constructor.name

    if(name == undefined)
      throw new Error("Can't register an anonymous module");

    registerClass(name, constructor)
  }

  // Registering a plugin
  else
    for(key in constructor)
      switch(key)
      {
        case 'abstracts':
          registerAbstracts(constructor[key])
        break

        case 'complexTypes':
          registerComplexTypes(constructor[key])
        break

        default:
          registerClass(key, constructor[key])
      }
};


KwsMedia.require = function(name)
{
  var module = require(name)

  if(module instanceof Function)
    KwsMedia.register(module)
  else
    for(var name in module)
      KwsMedia.register(name, module[name])
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


KwsMedia.register(require('kws-media-core'))


module.exports = KwsMedia;
