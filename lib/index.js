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


// Export KurentoClient

module.exports = KurentoClient;
KurentoClient.KurentoClient = KurentoClient;

KurentoClient.register = register;


var MediaObject = require('kurento-client-core').abstracts.MediaObject;


/*
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/findIndex#Polyfill
 */
if (!Array.prototype.findIndex) {
  Array.prototype.findIndex = function(predicate) {
    if (this == null) {
      throw new TypeError('Array.prototype.find called on null or undefined');
    }
    if (typeof predicate !== 'function') {
      throw new TypeError('predicate must be a function');
    }
    var list = Object(this);
    var length = list.length >>> 0;
    var thisArg = arguments[1];
    var value;

    for (var i = 0; i < length; i++) {
      value = list[i];
      if (predicate.call(thisArg, value, i, list)) {
        return i;
      }
    }
    return -1;
  };
}


/**
 * Serialize objects using their id
 */
function serializeParams(params)
{
  for(var key in params)
  {
    var param = params[key];
    if(param instanceof MediaObject)
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
  return MediaObject;
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

function deferred(mediaObject, params, prevRpc, callback)
{
  var promises = [];

  if(mediaObject != undefined)
    promises.push(mediaObject);

  for(var key in params)
  {
    var param = params[key];
    if(param !== undefined)
      promises.push(param);
  };

  if(prevRpc != undefined)
    promises.push(prevRpc);

  return promiseCallback(Promise.all(promises), callback);
};

function noop(error)
{
  if(error) console.trace(error);
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

  options.enableTransactions = options.enableTransactions || true


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


  // Transactional API

  var transactions = [];

  function transactionOperation(method, params, callback)
  {
    var message =
    {
      method: method,
      params: params,
      callback: callback
    }

    transactions[0].push(message);
  }

  this.beginTransaction = function()
  {
    transactions.unshift([]);
  };

  this.endTransaction = function(callback)
  {
    var operations = transactions.shift();

    var promise = new Promise(function(resolve, reject)
    {
      function callback(error, result)
      {
        if(error) return reject(error);

        resolve(result)
      }

      var params =
      {
        object: self,
        operations: operations
      }

      commit(params, callback);
    })

    return promiseCallback(promise, callback)
  };

  this.transaction = function(func, callback)
  {
    this.beginTransaction();
    func.call(this);
    return this.endTransaction(callback);
  };


  function id2object(error, result, operation, id, callback)
  {
    if(error) return callback(error);

    if(operation == 'getConnectedSinks'
    || operation == 'getMediaPipeline'
    || operation == 'getMediaSinks'
    || operation == 'getMediaSrcs'
    || operation == 'getParent')
    {
      var sessionId = result.sessionId;

      return self.getMediaobjectById(id, function(error, result)
      {
        if(error) return callback(error);

        var result =
        {
          sessionId: sessionId,
          value: result
        };

        callback(null, result);
      });
    };

    callback(null, result)
  };


  // Encode commands

  function encode(method, params, callback)
  {
    if(transactions.length)
      transactionOperation(method, params, callback);
    else
      self.then(function()
      {
        // [ToDo] Use stacktrace of caller, not from response
        rpc.encode(method, params, function(error, result)
        {
          if(error)
            error = extend(new Error(error.message || error), error);

          callback(error, result);
        });
      },
      callback)
  }

  function encodeCreate(params, callback)
  {
    callback = callback || noop;

    function callback2(error, result)
    {
      var mediaObject = params.object;

      if(error)
      {
        mediaObject.emit('_id', error);
        return callback(error);
      }

      var id = result.value;

      callback(null, registerObject(mediaObject, id));
    }

    deferred(null, params.constructorParams, null, function(error)
    {
      if(error) return callback(error);

      params.constructorParams = serializeParams(params.constructorParams);

      encode('create', params, callback2);
    });
  };

  var prevRpc = Promise.resolve();

  /**
   * Request a generic functionality to be procesed by the server
   */
  function encodeRpc(method, params, callback)
  {
    if(!params.object) params.object = this;

    prevRpc = new Promise(function(resolve, reject)
    {
      function callback2(error, result)
      {
        var operation = params.operation;
        var id = result.value;

        id2object(error, result, operation, id, function(error, result)
        {
          if(error) return reject(error);

          resolve(result);
        });
      };

      deferred(params.object, params.operationParams, prevRpc, function(error)
      {
        if(error) return reject(error);

        // Serialize object using their id
        params.object = params.object.id;
        params.operationParams = serializeParams(params.operationParams);

        encode(method, params, callback2);
      })
    });

    promiseCallback(prevRpc, callback);
  }


  // Commit mechanisms

  function commitTransactional(params, callback)
  {
    var operations = params.operations;

    var promises = [];
//    var promises = [prevRpc];

    function checkId(operation, param)
    {
      if(param instanceof MediaObject && param.id === undefined)
      {
        var index = operations.findIndex(function(element)
        {
          return operation != element && element.params.object === param;
        });

        // MediaObject dependency is created in this transaction,
        // set a new reference ID
        if(index >= 0)
          return 'newref:'+index;

        // MediaObject dependency is created outside this transaction,
        // wait until it's ready
        promises.push(param);
      }

      return param
    }

    // Fix references to uninitialized MediaObjects
    operations.forEach(function(operation)
    {
      var params = operation.params;

      switch(operation.method)
      {
        case 'create':
          var constructorParams = params.constructorParams;
          for(var key in constructorParams)
            constructorParams[key] = checkId(operation, constructorParams[key]);
        break;

//        case 'transaction':
//          commitTransactional(params.operations, operation.callback);
//        break;

        default:
          params.object = checkId(operation, params.object);

          var operationParams = params.operationParams;
          for(var key in operationParams)
            operationParams[key] = checkId(operation, operationParams[key]);
      };
    });

    function callback2(error, transaction_result)
    {
      if(error) return callback(error);

      operations.forEach(function(operation, index)
      {
        var callback = operation.callback || noop;

        var operation_response = transaction_result.value[index];
        if(operation_response == undefined)
          return callback(new Error('Command not executed in the server'));

        var error  = operation_response.error;
        var result = operation_response.result;

        var id;
        if(result) id = result.value;

        switch(operation.method)
        {
          case 'create':
            var mediaObject = operation.params.object;

            if(error)
            {
              mediaObject.emit('_id', error);
              return callback(error)
            }

            callback(null, registerObject(mediaObject, id));
          break;

//          case 'transaction':
//          break;

          default:
            id2object(error, result, operation, id, callback);
        }
      })

      callback(null, transaction_result);
    };

    Promise.all(promises).then(function()
//    prevRpc = Promise.all(promises).then(function()
    {
      operations.forEach(function(operation, index)
      {
        var params = operation.params;

        switch(operation.method)
        {
          case 'create':
            var constructorParams = params.constructorParams;
            if(constructorParams)
              params.constructorParams = serializeParams(constructorParams);
          break;

//          case 'transaction':
//          break;

          default:
            var id = params.object.id;
            if(id !== undefined)
              params.object = id;

            var operationParams = params.operationParams;
            if(operationParams)
              params.operationParams = serializeParams(operationParams);
        };

        operation.jsonrpc = "2.0";

        if(operation.callback)
          operation.id = index;
      })

      encode('transaction', params, callback2);
    },
    callback);
  }

  function commitSerial(params, callback)
  {
    var operations = params.operations;

    async.forEach(operations, function(operation)
    {
      switch(operation.method)
      {
        case 'create':
          encodeCreate(operation.params, operation.callback);
        break;

        case 'transaction':
          commitSerial(operation.params.operations, operation.callback);
        break;

        default:
          encodeRpc(operation.method, operation.params, operation.callback);
      }
    },
    callback)
  }


  // Select what transactions mechanism to use
  var commit = options.enableTransactions ? commitTransactional : commitSerial;

  function createObject(constructor)
  {
    var mediaObject = new constructor()

    mediaObject.on('_rpc', encodeRpc);

    if(mediaObject instanceof register.classes.MediaPipeline)
      mediaObject.on('_transaction', commit);

    return mediaObject;
  };

  function registerObject(mediaObject, id)
  {
    var object = objects[id];
    if(object) return object;

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

    var mediaObject = createObject(constructor)

    Object.defineProperty(item, 'object', {value: mediaObject});

    encodeCreate(item, callback);

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


// Register Kurento basic elements

register(require('kurento-client-core'))
register(require('kurento-client-elements'))
register(require('kurento-client-filters'))
