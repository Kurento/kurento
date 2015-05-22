/*
 * (C) Copyright 2013-2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

/**
 * Media API for the Kurento Web SDK
 *
 * @module KurentoClient
 *
 * @copyright 2013-2015 Kurento (http://kurento.org/)
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

var disguise = require('./disguise')
var createPromise = require('./createPromise');
var MediaObjectCreator = require('./MediaObjectCreator');
var TransactionsManager = require('./TransactionsManager');

var TransactionNotCommitedException = TransactionsManager.TransactionNotCommitedException;
var transactionOperation = TransactionsManager.transactionOperation;

var MediaObject = require('kurento-client-core').abstracts.MediaObject;

const MEDIA_OBJECT_TYPE_NOT_FOUND = 40100
const MEDIA_OBJECT_NOT_FOUND = 40101
const MEDIA_OBJECT_METHOD_NOT_FOUND = 40105

const BASE_TIMEOUT = 20000;

function findIndex(list, predicate) {
  for (var i = 0, item; item = list[i]; i++)
    if (predicate(item)) return i;

  return -1;
};

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

  Object.defineProperty(this, 'sessionId', {
    configurable: true
  })
  this.on('disconnect', function () {
    Object.defineProperty(this, 'sessionId', {
      get: function () {
        throw new SyntaxError('Client has been disconnected')
      }
    })

    for (var id in objects)
      objects[id].emit('release')
  })

  // Encode commands

  function encode(method, params, callback) {
    var stack = (new Error).stack

    params.sessionId = self.sessionId

    self.then(function () {
        var requestTimestamp = Date.now()

        rpc.encode(method, params, function (error, result) {
          if (error) {
            var responseTimestamp = Date.now()

            var constructor = Error
            switch (error.code) {
            case MEDIA_OBJECT_TYPE_NOT_FOUND:
              constructor = TypeError
              break

            case MEDIA_OBJECT_NOT_FOUND:
              constructor = ReferenceError
              break

            case MEDIA_OBJECT_METHOD_NOT_FOUND:
              constructor = SyntaxError
              break
            }

            error = extend(new constructor(error.message || error),
              error);

            Object.defineProperty(error, 'requestTimestamp', {
              value: requestTimestamp
            })
            Object.defineProperty(error, 'responseTimestamp', {
              value: responseTimestamp
            })
            Object.defineProperty(error, 'stack', {
              value: [error.toString()].concat(
                error.stack.split('\n')[1],
                stack.split('\n').slice(2)
              ).join('\n')
            })
          } else if (self.sessionId !== result.sessionId)
            Object.defineProperty(self, 'sessionId', {
              configurable: true,
              value: result.sessionId
            })

          callback(error, result);
        });
      },
      callback)
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
        if (error) return reject(error);

        resolve(result);
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
        var error = new ReferenceError('MediaObject not found in server');
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
        var index = findIndex(operations, function (element) {
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
          if (error) return callback(error);

          callback(null, result);
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

  /**
   * Get a MediaObject from its ID
   *
   * @param {(external:String|external:string[])} id - ID of the MediaElement
   * @callback {getMediaobjectByIdCallback} callback
   *
   * @return {external:Promise}
   */
  this.getMediaobjectById = function (id, callback) {
    return disguise(createPromise(id, describe, callback), this)
  };
  /**
   * @callback KurentoClientApi~getMediaobjectByIdCallback
   * @param {external:Error} error
   * @param {(module:core/abstract~MediaElement|module:core/abstract~MediaElement[])} result
   *  The requested MediaElement
   */

  var mediaObjectCreator = new MediaObjectCreator(this, encodeCreate,
    encodeRpc, encodeTransaction, this.getMediaobjectById.bind(this));

  function describe(id, callback) {
    if (id == undefined)
      return callback(new TypeError("'id' can't be null or undefined"))

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

  Object.defineProperty(this, '_resetCache', {
    value: function () {
      objects = {}
    }
  })

  /**
   * Create a new instance of a MediaObject
   *
   * @param {external:String} type - Type of the element
   * @param {external:string[]} [params]
   * @callback {createMediaPipelineCallback} callback
   *
   * @return {(module:core~MediaObject|module:core~MediaObject[])}
   */
  this.create = mediaObjectCreator.create.bind(mediaObjectCreator);
  /**
   * @callback KurentoClientApi~createCallback
   * @param {external:Error} error
   * @param {module:core/abstract~MediaElement} result
   *  The created MediaElement
   */

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
      var promise = new Promise(function (resolve, reject) {
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

      return disguise(promise, this)
    };

    this.catch = this.then.bind(this, null);

    // Check for available modules in the Kurento Media Server

    var promise = this.getServerManager()
      .then(function (serverManager) {
        return serverManager.getInfo()
      })
      .then(function (info) {
        var serverModules = info.modules.map(function (module) {
          return module.name
        })

        var notInstalled = KurentoClient.register.modules.filter(
          function (module) {
            return serverModules.indexOf(module) < 0
          })

        var length = notInstalled.length
        if (length) {
          if (length === 1)
            var message = "Module '" + notInstalled[0] +
              "' is not installed in the Kurento Media Server"
          else
            var message = "Modules '" + notInstalled.slice(0, -1).join(
                "', '") + "' and '" + notInstalled[length - 1] +
              "' are not installed in the Kurento Media Server"

          var error = new SyntaxError(message)
          error.modules = notInstalled

          return Promise.reject(error)
        }

        return Promise.resolve(self)
      })

    if (callback)
      promise.then(callback.bind(undefined, null), callback);
  };
  connect.call(self, callback);
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
  if (!(media instanceof Array)) {
    media = Array.prototype.slice.call(arguments, 0);
    callback = (typeof media[media.length - 1] === 'function') ? media.pop() :
      undefined;
  }

  // Check if we have enought media components
  if (media.length < 2)
    throw new SyntaxError("Need at least two media elements to connect");

  // Check MediaElements are of the correct type
  media.forEach(checkMediaElement);

  // Connect the media elements
  var src = media[0];
  var sink = media[media.length - 1]

  // Generate promise
  var promise = new Promise(function (resolve, reject) {
    function callback(error, result) {
      if (error) return reject(error);

      resolve(result);
    };

    async.each(media.slice(1), function (sink, callback) {
      src = src.connect(sink, callback);
    }, callback);
  });

  return disguise(promiseCallback(promise, callback), sink)
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

//
// Helper function to return a singleton client for a particular ws_uri
//
var singletons = {};

KurentoClient.getSingleton = function (ws_uri, callback) {
  var client = singletons[ws_uri]
  if (!client)
    client = KurentoClient(ws_uri, function (error, client) {
      if (error) return callback(error);

      singletons[ws_uri] = client
      client.on('disconnect', function () {
        delete singletons[ws_uri]
      })
    });

  return disguise(promiseCallback(client, callback), client)
}

// Export KurentoClient

module.exports = KurentoClient;
