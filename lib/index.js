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
var url = require('url');

var Promise = require('es6-promise').Promise;

var async = require('async');
var extend = require('extend');
var inherits = require('inherits');
var reconnect = require('reconnect-ws');

var checkType = require('checktype');

var RpcBuilder = require('kurento-jsonrpc');
var JsonRPC = RpcBuilder.packers.JsonRPC;

var promiseCallback = require('promisecallback');

var createPromise = require('./createPromise');
var MediaObjectCreator = require('./MediaObjectCreator');
var register = require('./register');
var TransactionsManager = require('./TransactionsManager');

var TransactionNotCommitedException = TransactionsManager.TransactionNotCommitedException;
var transactionOperation = TransactionsManager.transactionOperation;

// Export KurentoClient

module.exports = KurentoClient;
KurentoClient.KurentoClient = KurentoClient;

KurentoClient.checkType = checkType;
KurentoClient.MediaObjectCreator = MediaObjectCreator;
KurentoClient.register = register;
KurentoClient.TransactionsManager = TransactionsManager;

var MediaObject = require('kurento-client-core').abstracts.MediaObject;

const BASE_TIMEOUT = 20000;

/*
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/findIndex#Polyfill
 */
if (!Array.prototype.findIndex) {
  Array.prototype.findIndex = function (predicate) {
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
function serializeParams(params) {
  for (var key in params) {
    var param = params[key];
    if (param instanceof MediaObject) {
      var id = param.id;

      if (id !== undefined) params[key] = id;
    }
  };

  return params;
};

function serializeOperation(operation, index) {
  var params = operation.params;

  switch (operation.method) {
  case 'create':
    params.constructorParams = serializeParams(params.constructorParams);
    break;

  default:
    params = serializeParams(params);
    params.operationParams = serializeParams(params.operationParams);
  };

  operation.jsonrpc = "2.0";

  operation.id = index;
};

function deferred(mediaObject, params, prevRpc, callback) {
  var promises = [];

  if (mediaObject != undefined)
    promises.push(mediaObject);

  for (var key in params) {
    var param = params[key];
    if (param !== undefined)
      promises.push(param);
  };

  if (prevRpc != undefined)
    promises.push(prevRpc);

  return promiseCallback(Promise.all(promises), callback);
};

function noop(error) {
  if (error) console.trace(error);
};

function id2object(error, result, operation, id, callback) {
  if (error) return callback(error);

  var operations = [
    'getConnectedSinks',
    'getMediaPipeline',
    'getMediaSinks',
    'getMediaSrcs',
    'getParent'
  ]

  if (operations.indexOf(operation) != -1) {
    var sessionId = result.sessionId;

    return this.getMediaobjectById(id, function (error, result) {
      if (error) return callback(error);

      var result = {
        sessionId: sessionId,
        value: result
      };

      callback(null, result);
    });
  };

  callback(null, result)
};

/**
 * Creates a connection with the Kurento Media Server
 *
 * @class
 *
 * @param {external:String} ws_uri - Address of the Kurento Media Server
 * @param {Object} [options]
 *   @property failAfter - Don't try to reconnect after several tries
 *     @default 5
 *   @property enableTransactions - Enable transactions functionality
 *     @default true
 *   @property access_token - Set access token for the WebSocket connection
 *   @property max_retries - Number of tries to send the requests
 *     @default 0
 *   @property request_timeout - Timeout between requests retries
 *     @default 20000
 *   @property response_timeout - Timeout while a response is being stored
 *     @default 20000
 *   @property duplicates_timeout - Timeout to ignore duplicated responses
 *     @default 20000
 * @param {KurentoClientApi~constructorCallback} [callback]
 */
function KurentoClient(ws_uri, options, callback) {
  if (!(this instanceof KurentoClient))
    return new KurentoClient(ws_uri, options, callback);

  var self = this;

  EventEmitter.call(this);

  // Promises to check previous RPC calls
  var prevRpc = Promise.resolve(); // request has been send
  var prevRpc_result = Promise.resolve(); // response has been received

  // Fix optional parameters
  if (options instanceof Function) {
    callback = options;
    options = undefined;
  };

  options = options || {};

  var failAfter = options.failAfter
  if (failAfter == undefined) failAfter = 5

  options.enableTransactions = options.enableTransactions || true

  options.request_timeout = options.request_timeout || BASE_TIMEOUT;
  options.response_timeout = options.response_timeout || BASE_TIMEOUT;
  options.duplicates_timeout = options.duplicates_timeout || BASE_TIMEOUT;

  var objects = {};

  function onNotification(message) {
    var method = message.method;
    var params = message.params.value;

    var id = params.object;

    var object = objects[id];
    if (!object)
      return console.warn("Unknown object id '" + id + "'", message);

    switch (method) {
    case 'onEvent':
      object.emit(params.type, params.data);
      break;

      //      case 'onError':
      //        object.emit('error', params.error);
      //      break;

    default:
      console.warn("Unknown message type '" + method + "'");
    };
  };

  //
  // JsonRPC
  //

  if (typeof ws_uri == 'string') {
    var access_token = options.access_token;
    if (access_token != undefined) {
      ws_uri = url.parse(ws_uri, true);
      ws_uri.query.access_token = access_token;
      ws_uri = url.format(ws_uri);

      delete options.access_token;
    };
  }

  var rpc = new RpcBuilder(JsonRPC, options, function (request) {
    if (request instanceof RpcBuilder.RpcNotification) {
      // Message is an unexpected request, notify error
      if (request.duplicated != undefined)
        return console.warning('Unexpected request:', request);

      // Message is a notification, process it
      return onNotification(request);
    };

    // Invalid message, notify error
    console.error('Invalid request instance', request);
  });

  function connect(callback) {
    //
    // Reconnect websockets
    //

    var closed = false;
    var re = reconnect({
        failAfter: failAfter
      }, function (ws_stream) {
        if (closed)
          ws_stream.writable = false;

        rpc.transport = ws_stream;
      })
      .connect(ws_uri);

    Object.defineProperty(this, '_re', {
      configurable: true,
      get: function () {
        return re
      }
    })

    this.close = function () {
      closed = true;

      prevRpc_result.then(re.disconnect.bind(re));
    };

    re.on('fail', this.emit.bind(this, 'disconnect'));

    //
    // Promise interface ("thenable")
    //

    this.then = function (onFulfilled, onRejected) {
      return new Promise(function (resolve, reject) {
        function success() {
          re.removeListener('fail', failure);

          var result;

          if (onFulfilled)
            try {
              result = onFulfilled.call(self, self);
            } catch (exception) {
              if (!onRejected)
                console.trace('Uncaugh exception', exception)

              return reject(exception);
            }

          resolve(result);
        };

        function failure() {
          re.removeListener('connection', success);

          var result = new Error('Connection error');

          if (onRejected)
            try {
              result = onRejected.call(self, result);
            } catch (exception) {
              return reject(exception);
            } else
              console.trace('Uncaugh exception', result)

          reject(result);
        };

        if (re.connected)
          success()
        else if (!re.reconnect)
          failure()
        else {
          re.once('connection', success);
          re.once('fail', failure);
        }
      });
    };

    this.catch = this.then.bind(this, null);

    if (callback)
      this.then(callback.bind(undefined, null), callback);
  };
  connect.call(self, callback);

  // Select what transactions mechanism to use
  var encodeTransaction = options.enableTransactions ? commitTransactional :
    commitSerial;

  // Transactional API

  var transactionsManager = new TransactionsManager(this,
    function (operations, callback) {
      var params = {
        object: self,
        operations: operations
      };

      encodeTransaction(params, callback)
    });

  this.beginTransaction = transactionsManager.beginTransaction.bind(
    transactionsManager);
  this.endTransaction = transactionsManager.endTransaction.bind(
    transactionsManager);
  this.transaction = transactionsManager.transaction.bind(transactionsManager);

  // Encode commands

  function encode(method, params, callback) {
    self.then(function () {
        // [ToDo] Use stacktrace of caller, not from response
        rpc.encode(method, params, function (error, result) {
          if (error)
            error = extend(new Error(error.message || error), error);

          callback(error, result);
        });
      },
      function () {
        connect.call(self, function (error) {
          if (error) return callback(error);

          encode(method, params, callback);
        });
      })
  }

  function encodeCreate(transaction, params, callback) {
    if (transaction)
      return transactionOperation.call(transaction, 'create', params,
        callback);

    if (transactionsManager.length)
      return transactionOperation.call(transactionsManager, 'create',
        params, callback);

    callback = callback || noop;

    function callback2(error, result) {
      var mediaObject = params.object;

      if (error) {
        mediaObject.emit('_id', error);
        return callback(error);
      }

      var id = result.value;

      callback(null, registerObject(mediaObject, id));
    }

    deferred(null, params.constructorParams, null, function (error) {
      if (error) return callback(error);

      params.constructorParams = serializeParams(params.constructorParams);

      encode('create', params, callback2);
    });
  };

  /**
   * Request a generic functionality to be procesed by the server
   */
  function encodeRpc(transaction, method, params, callback) {
    if (transaction)
      return transactionOperation.call(transaction, method, params,
        callback);

    var object = params.object;
    if (object && object.transactions && object.transactions.length) {
      var error = new TransactionNotCommitedException();
      error.method = method;
      error.params = params;

      return setTimeout(callback, 0, error)
    };

    for (var key in params.operationParams) {
      var object = params.operationParams[key];

      if (object && object.transactions && object.transactions.length) {
        var error = new TransactionNotCommitedException();
        error.method = method;
        error.params = params;

        return setTimeout(callback, 0, error)
      };
    }

    if (transactionsManager.length)
      return transactionOperation.call(transactionsManager, method, params,
        callback);

    var promise = new Promise(function (resolve, reject) {
      function callback2(error, result) {
        var operation = params.operation;
        var id = result ? result.value : undefined;

        id2object.call(self, error, result, operation, id, function (
          error, result) {
          if (error) return reject(error);

          resolve(result);
        });
      };

      prevRpc = deferred(params.object, params.operationParams, prevRpc,
        function (error) {
          if (error) return reject(error);

          params = serializeParams(params);
          params.operationParams = serializeParams(params.operationParams);

          encode(method, params, callback2);
        })
    });

    prevRpc_result = promiseCallback(promise, callback);

    if (method == 'release') prevRpc = prevRpc_result;
  }

  // Commit mechanisms

  function commitTransactional(params, callback) {
    if (transactionsManager.length)
      return transactionOperation.call(transactionsManager, 'transaction',
        params, callback);

    var operations = params.operations;

    for (var i = 0, operation; operation = operations[i]; i++) {
      var object = operation.params.object;
      if (object instanceof MediaObject && object.id === null) {
        var error = new Error('MediaObject not found in server');
        error.code = 40101;
        error.object = object;

        // Notify error to all the operations in the transaction
        operations.forEach(function (operation) {
          if (operation.method == 'create')
            operation.params.object.emit('_id', error);

          var callback = operation.callback;
          if (callback instanceof Function)
            callback(error);
        });

        return callback(error);
      }
    }

    var promises = [];

    function checkId(operation, param) {
      if (param instanceof MediaObject && param.id === undefined) {
        var index = operations.findIndex(function (element) {
          return operation != element && element.params.object ===
            param;
        });

        // MediaObject dependency is created in this transaction,
        // set a new reference ID
        if (index >= 0)
          return 'newref:' + index;

        // MediaObject dependency is created outside this transaction,
        // wait until it's ready
        promises.push(param);
      }

      return param
    }

    // Fix references to uninitialized MediaObjects
    operations.forEach(function (operation) {
      var params = operation.params;

      switch (operation.method) {
      case 'create':
        var constructorParams = params.constructorParams;
        for (var key in constructorParams)
          constructorParams[key] = checkId(operation, constructorParams[
            key]);
        break;

      default:
        params.object = checkId(operation, params.object);

        var operationParams = params.operationParams;
        for (var key in operationParams)
          operationParams[key] = checkId(operation, operationParams[key]);
      };
    });

    function callback2(error, transaction_result) {
      if (error) return callback(error);

      operations.forEach(function (operation, index) {
        var callback = operation.callback || noop;

        var operation_response = transaction_result.value[index];
        if (operation_response == undefined)
          return callback(new Error(
            'Command not executed in the server'));

        var error = operation_response.error;
        var result = operation_response.result;

        var id;
        if (result) id = result.value;

        switch (operation.method) {
        case 'create':
          var mediaObject = operation.params.object;

          if (error) {
            mediaObject.emit('_id', error);
            return callback(error)
          }

          callback(null, registerObject(mediaObject, id));
          break;

        default:
          id2object.call(self, error, result, operation, id, callback);
        }
      })

      callback(null, transaction_result);
    };

    Promise.all(promises).then(function () {
        operations.forEach(serializeOperation)

        encode('transaction', params, callback2);
      },
      callback);
  }

  function commitSerial(params, callback) {
    if (transactionsManager.length)
      return transactionOperation.call(transactionsManager, 'transaction',
        params, callback);

    var operations = params.operations;

    async.each(operations, function (operation) {
        switch (operation.method) {
        case 'create':
          encodeCreate(undefined, operation.params, operation.callback);
          break;

        case 'transaction':
          commitSerial(operation.params.operations, operation.callback);
          break;

        default:
          encodeRpc(undefined, operation.method, operation.params,
            operation.callback);
        }
      },
      callback)
  }

  function registerObject(mediaObject, id) {
    var object = objects[id];
    if (object) return object;

    mediaObject.emit('_id', null, id);

    objects[id] = mediaObject;

    /**
     * Remove the object from cache
     */
    mediaObject.once('release', function () {
      delete objects[id];
    });

    return mediaObject;
  }

  // Creation of objects

  var mediaObjectCreator = new MediaObjectCreator(undefined, encodeCreate,
    encodeRpc, encodeTransaction);

  function describe(id, callback) {
    var mediaObject = objects[id];
    if (mediaObject) return callback(null, mediaObject);

    var params = {
      object: id
    };

    function callback2(error, result) {
      if (error) return callback(error);

      var mediaObject = mediaObjectCreator.createInmediate(result);

      return callback(null, registerObject(mediaObject, id));
    }

    encode('describe', params, callback2);
  };

  /**
   * Get a MediaObject from its ID
   *
   * @param {(external:String|external:string[])} id - ID of the MediaElement
   * @callback {getMediaobjectByIdCallback} callback
   *
   * @return {external:Promise}
   */
  this.getMediaobjectById = function (id, callback) {
    return createPromise(id, describe, callback)
  };
  /**
   * @callback KurentoClientApi~getMediaobjectByIdCallback
   * @param {external:Error} error
   * @param {(module:core/abstract~MediaElement|module:core/abstract~MediaElement[])} result
   *  The requested MediaElement
   */

  /**
   * Create a new instance of a MediaObject
   *
   * @param {external:String} type - Type of the element
   * @param {external:string[]} [params]
   * @callback {createMediaPipelineCallback} callback
   *
   * @return {module:KurentoClientApi~KurentoClient} The Kurento client itself
   */
  this.create = mediaObjectCreator.create.bind(mediaObjectCreator);
  /**
   * @callback KurentoClientApi~createCallback
   * @param {external:Error} error
   * @param {module:core/abstract~MediaElement} result
   *  The created MediaElement
   */
};
inherits(KurentoClient, EventEmitter);
/**
 * @callback KurentoClientApi~constructorCallback
 * @param {external:Error} error
 * @param {module:KurentoClientApi~KurentoClient} client
 *  The created KurentoClient
 */

var checkMediaElement = checkType.bind(null, 'MediaElement', 'media');

/**
 * Connect the source of a media to the sink of the next one
 *
 * @param {...module:core/abstract~MediaObject} media - A media to be connected
 * @callback {module:KurentoClientApi~connectCallback} [callback]
 *
 * @return {external:Promise}
 *
 * @throws {SyntaxError}
 */
KurentoClient.prototype.connect = function (media, callback) {
  // Fix lenght-variable arguments
  media = Array.prototype.slice.call(arguments, 0);
  callback = (typeof media[media.length - 1] == 'function') ? media.pop() :
    undefined;

  // Check if we have enought media components
  if (media.length < 2)
    throw new SyntaxError("Need at least two media elements to connect");

  // Check MediaElements are of the correct type
  media.forEach(checkMediaElement);

  // Generate promise
  var promise = new Promise(function (resolve, reject) {
    function callback(error, result) {
      if (error) return reject(error);

      resolve(result);
    };

    // Connect the media elements
    var src = media[0];

    async.each(media.slice(1), function (sink, callback) {
      src.connect(sink, callback);
      src = sink;
    }, callback);
  });

  return promiseCallback(promise, callback);
};
/**
 * @callback KurentoClientApi~connectCallback
 * @param {external:Error} error
 */

/**
 * Get a reference to the current Kurento Media Server we are connected
 *
 * @callback {module:KurentoClientApi~getServerManagerCallback} callback
 *
 * @return {external:Promise}
 */
KurentoClient.prototype.getServerManager = function (callback) {
  return this.getMediaobjectById('manager_ServerManager', callback)
};
/**
 * @callback KurentoClientApi~getServerManagerCallback
 * @param {external:Error} error
 * @param {module:core/abstract~ServerManager} server
 *  Info of the MediaServer instance
 */

// Register Kurento basic elements

register(require('kurento-client-core'))
register(require('kurento-client-elements'))
register(require('kurento-client-filters'))
