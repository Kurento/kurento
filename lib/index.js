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
  var result = register.classes[type] || register.abstracts[type];
  if(result) return result;

  console.warn("Unknown type '"+type+"', using MediaObject instead");
  return register.abstracts.MediaObject;
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
      function success()
      {
        re.removeListener('fail', failure);

        var result;

        if(onFulfilled)
          try
          {
            result = onFulfilled.call(self, self);
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
        re.removeListener('connection', success);

        var result = new Error('Connection error');

        if(onRejected)
          try
          {
            result = onRejected.call(self, result);
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
        re.once('connection', success);
        re.once('fail',       failure);
      }
    });
  };

  this.catch = this.then.bind(this, null);

  if(callback)
    this.then(callback.bind(undefined, null), callback);


  function encode(method, params, callback)
  {
    self.then(function()
    {
      rpc.encode(method, params, function(error, result)
      {
        if(error)
          error = extend(new Error(error.message || error), error);

        callback(error, result);
      });
    },
    callback)
  }


  function encodeCreate(mediaObject, params, callback)
  {
    callback = callback || function(){};

    function callback2(error, result)
    {
      if(error)
      {
        mediaObject.emit('_id', error);
        return callback(error);
      }

      var id = result.value;

      callback(null, registerObject(mediaObject, id));
    }

    var promises = [];
    for(var key in params.constructorParams)
    {
      var param = params.constructorParams[key];
      if(param !== undefined)
        promises.push(param);
    };

    Promise.all(promises).then(function()
    {
      params.constructorParams = serializeParams(params.constructorParams);

      encode('create', params, callback2);
    },
    callback);
  };

  var encodeTransaction = encode.bind(undefined,'transaction');

  function commit(operations, callback)
  {
    operations = operations.map(function(operation)
    {
      var mediaObject = operation.mediaObject;
      var params      = operation.params;

      params.object = mediaObject.id === undefined
                    ? mediaObject
                    : mediaObject.id;

      // Serialize objects using their id
      params.constructorParams = serializeParams(params.constructorParams);

      operation.promises = [params.object];
      for(var key in params.constructorParams)
      {
        var param = params.constructorParams[key];
        if(param instanceof register.abstracts.MediaObject)
          operation.promises.push(param.id === undefined
                                ? param
                                : param.id);
      };

      return operation;
    });

    var params =
    {
      operations: operations
    }

    encodeTransaction(params, function(error, result)
    {
      if(error) return callback(error);

      console.log('transaction result:',result)

      result.value.forEach(registerObject);

      operations.forEach(function(operation, index)
      {
        var callback = operation.callback;
        if(callback)
        {
          var item = resul.value[index] || {};

          Promise.all(operation.promises).then(function()
          {
            operation.callback(item.error, item.result);
          },
          callback)
        }
      })

      callback(null, result);
    });
  }


  function createObject(constructor)
  {
    var mediaObject = new constructor()

    if(mediaObject instanceof register.classes.MediaPipeline)
      mediaObject.on('_commit', commit);

    return mediaObject;
  };

  function registerObject(mediaObject, id)
  {
    var object = objects[id];
    if(object) return object;

    if(EventEmitter.listenerCount(mediaObject, '_rpc'))
      mediaObject.removeAllListeners('_rpc');

    /**
     * Request a generic functionality to be procesed by the server
     */
    mediaObject.on('_rpc', function(method, params, callback)
    {
      if(!params.object) params.object = this;

      var promises = [params.object];
      for(var key in params.operationParams)
      {
        var param = params.operationParams[key];
        if(param !== undefined)
          promises.push(param);
      };

      Promise.all(promises).then(function()
      {
        // Serialize object using their id
        params.object = params.object.id;
        params.operationParams = serializeParams(params.operationParams);

        encode(method, params, function(error, result)
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
      },
      callback)
    });


    if(EventEmitter.listenerCount(mediaObject, '_create'))
      mediaObject.removeAllListeners('_create');

    if(mediaObject instanceof register.abstracts.Hub
    || mediaObject instanceof register.classes.MediaPipeline)
      mediaObject.on('_create', encodeCreate);


    mediaObject.emit('_id', null, id);

    objects[id] = mediaObject;

    /**
     * Request to release the object on the server and remove it from cache
     */
    mediaObject.once('release', function()
    {
      delete objects[id];
    });

    return mediaObject;
  }


  /**
   * Request to the server to create a new MediaElement
   */
  function createMediaObject(item, callback)
  {
    var constructor = createConstructor(item);

    item = constructor.item;
    delete constructor.item;

    item.constructorParams = checkParams(item.params,
                                         constructor.constructorParams,
                                         item.type);
    delete item.params;

    // Serialize objects using their id
    item.constructorParams = serializeParams(item.constructorParams);

    var mediaObject = createObject(constructor)

    encodeCreate(mediaObject, item, callback);

    return mediaObject
  };

  function describe(id, callback)
  {
    var mediaObject = objects[id];
    if(mediaObject) return callback(null, mediaObject);

    encode('describe', {object: id}, function(error, result)
    {
      if(error) return callback(error);

      var constructor = createConstructor(result);
      delete constructor.item;

      var mediaObject = createObject(constructor);

      return callback(null, registerObject(mediaObject, id));
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

    params = params || {};

    if(type instanceof Array)
      return createPromise(type, createMediaObject, callback)

    type = {params: params, type: type};

    return createMediaObject(type, callback)
  };
};
inherits(KurentoClient, EventEmitter);


var checkMediaElement = checkType.bind(null, 'MediaElement', 'media');

/**
 * Connect the source of a media to the sink of the next one
 *
 * @param {...MediaObject} media - A media to be connected
 * @callback {connectCallback} [callback]
 *
 * @return {Promise}
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
