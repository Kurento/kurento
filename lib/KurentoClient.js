/*
 * (C) Copyright 2013-2015 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var EventEmitter = require('events').EventEmitter;
var url = require('url');

var async = require('async');
var extend = require('extend');
var inherits = require('inherits');
var reconnect = require('reconnect-ws');

var checkType = require('./checkType');

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
const INVALID_SESSION = 40007

const BASE_TIMEOUT = 20000;

var PING_INTERVAL = 5000;
var HEARTBEAT = 60000;
var pingNextNum = 0;
var enabledPings = true;
var pingPongStarted = false;
var pingInterval;
var notReconnectIfNumLessThan = -1;

/**
 * @function module:kurentoClient.KurentoClient~findIndex
 *
 * @param {external:Array} list
 * @param {external:Function} predicate
 *
 * @return {external:Integer}
 */
function findIndex(list, predicate) {
  for (var i = 0, item; item = list[i]; i++)
    if (predicate(item)) return i;

  return -1;
};

/**
 * Serialize objects using their id
 *
 * @function module:kurentoClient.KurentoClient~serializeParams
 *
 * @param {external:Object} params
 *
 * @return {external:Object}
 */
function serializeParams(params) {
  for (var key in params) {
    var param = params[key];
    if (param instanceof MediaObject || (param && (params.object !== undefined ||
        params.hub !== undefined || params.sink !== undefined))) {
      if (param && param.id != null) {
        params[key] = param.id;
      }
    }
  };

  return params;
};

/**
 * @function module:kurentoClient.KurentoClient~serializeOperation
 *
 * @param {external:Object} operation
 * @param {external:Integer} index
 */
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

/**
 * @function module:kurentoClient.KurentoClient~deferred
 *
 * @param {module:core/abstracts.MediaObject} mediaObject
 * @param {external:Object} params
 * @param {external:Promise} prevRpc
 * @param {external:Function} callback
 *
 * @return {external:Promise}
 */
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

/**
 * @function module:kurentoClient.KurentoClient~noop
 *
 * @param error
 * @param result
 *
 * @return result
 */
function noop(error, result) {
  if (error) console.trace(error);

  return result
};

/**
 * @typedef {Object} module:kurentoClient.KurentoClient~KurentoClientDict
 *   @property {external:Number} [failAfter=Infinity]
 *    Fail after N reconnection attempts
 *   @property {external:Boolean} [enableTransactions=true]
 *    Enable transactions functionality
 *   @property {external:Boolean} [strict=true]
 *    Throw an error when creating an object of unknown type
 *   @property {external:String} [access_token]
 *    Set access token for the WebSocket connection
 *   @property {external:Number} [max_retries=0]
 *    Number of tries to send the requests
 *   @property {external:Number} [request_timeout=20000]
 *    Timeout between requests retries
 *   @property {external:Number} [response_timeout=20000]
 *    Timeout while a response is being stored
 *   @property {external:Number} [duplicates_timeout=20000]
 *    Timeout to ignore duplicated responses
 */

/**
 * Creates a connection with the Kurento Media Server
 *
 * @class module:kurentoClient.KurentoClient
 *
 * @param {external:String} ws_uri - Address of the Kurento Media Server
 * @param {module:kurentoClient.KurentoClient~KurentoClientDict} [options]
 * @param {module:kurentoClient.KurentoClient~constructorCallback} [callback]
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
  if (failAfter == undefined) failAfter = Infinity

  if (options.enableTransactions === undefined) options.enableTransactions =
    true
  if (options.strict === undefined) options.strict = true

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
    onDisconnected();
    Object.defineProperty(this, 'sessionId', {
      configurable: false,
      get: function () {
        throw new SyntaxError('Client has been disconnected')
      }
    })

    for (var id in objects)
      objects[id].emit('release')
  })

  // Emit events

  function onReconnected(sameSession) {
    self.emit('reconnected', sameSession);
  }

  function onDisconnected() {
    self.emit('disconnected');
  }

  function onConnectionFailed() {
    self.emit('connectionFailed');
  }

  function onConnected() {
    self.emit('connected');
  }

  // Encode commands

  function send(request) {
    var method = request.method
    var params = request.params
    var callback = request.callback
    var stack = request.stack

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

        error = extend(new constructor(error.message || error), error);

        Object.defineProperties(error, {
          'requestTimestamp': {
            value: requestTimestamp
          },
          'responseTimestamp': {
            value: responseTimestamp
          },
          'stack': {
            value: [error.toString()].concat(
              error.stack.split('\n')[1],
              error.stack.split('\n').slice(2)
            ).join('\n')
          }
        })
      } else if ((self.sessionId !== result.sessionId) && (result.value !==
          'pong'))
        Object.defineProperty(self, 'sessionId', {
          configurable: true,
          value: result.sessionId
        })

      callback(error, result);
    });
  }

  function operationResponse(operation, index) {
    var callback = operation.callback || noop;

    var operation_response = this.value[index];
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
      callback(error, result);
    }
  }

  function sendImplicitTransaction(operations) {
    function callback(error, result) {
      if (error) return console.error('Implicit transaction failed')

      operations.forEach(operationResponse, result)
    }

    operations.forEach(serializeOperation)

    var request = {
      method: 'transaction',
      params: {
        operations: operations
      },
      callback: callback
    }
    send(request)
  }

  var queueEncode = []

  function sendQueueEncode() {
    var request = queueEncode.shift()

    // We have several pending requests, create an "implicit" transaction
    if (queueEncode.length) {
      // Send (implicit) transactions from previous iteration
      while (request && request.method === 'transaction') {
        send(request)
        request = queueEncode.shift()
      }

      // Encode and queue transactions from current iteration to exec on next one
      var operations = []

      while (request) {
        if (request.method === 'transaction') {
          if (operations.length) {
            sendImplicitTransaction(operations)
            operations = []
          }

          send(request)
        } else
          operations.push(request)

        request = queueEncode.shift()
      }

      // Encode and queue remaining operations for next iteration
      if (operations.length) sendImplicitTransaction(operations)
    }

    // We have only one pending request, send it directly
    else
      send(request)
  }

  function encode(method, params, callback) {
    var stack = (new Error).stack

    params.sessionId = self.sessionId

    self.then(function () {
        if (options.useImplicitTransactions && !queueEncode.length)
          async.setImmediate(sendQueueEncode)

        var request = {
          method: method,
          params: params,
          callback: callback
        }
        Object.defineProperty(request, 'stack', {
          value: stack
        })

        if (options.useImplicitTransactions)
          queueEncode.push(request)
        else
          send(request)
      },
      callback)
  }

  function encodeCreate(transaction, params, callback) {
    if (transaction)
      return transactionOperation.call(transaction, 'create', params, callback)

    if (transactionsManager.length)
      return transactionOperation.call(transactionsManager, 'create',
        params, callback);

    callback = callback || noop;

    function callback2(error, result) {
      var mediaObject = params.object;

      // Implicit transaction has already register the MediaObject
      if (mediaObject === result) return callback(null, mediaObject);

      if (error) {
        mediaObject.emit('_id', error);
        return callback(error);
      }

      var id = result.value;

      callback(null, registerObject(mediaObject, id));
    }

    return deferred(null, params.constructorParams, null, function (error) {
        if (error) throw error;

        params.constructorParams = serializeParams(params.constructorParams);

        return encode('create', params, callback2);
      })
      .catch(callback)
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
            if (error) throw error

            params = serializeParams(params);
            params.operationParams = serializeParams(params.operationParams);

            return encode(method, params, callback2);
          })
        .catch(reject)
    });

    prevRpc_result = promiseCallback(promise, callback);

    if (method == 'release') prevRpc = prevRpc_result;
  }

  // Commit mechanisms

  /**
   * @function module:kurentoClient.KurentoClient~commitTransactional
   *
   * @param {external:Object} params
   * @param {external:Function} callback
   */
  function commitTransactional(params, callback) {
    if (transactionsManager.length)
      return transactionOperation.call(transactionsManager, 'transaction',
        params, callback);

    callback = callback || noop;

    var operations = params.operations;

    var promises = [];

    function checkId(operation, param) {
      if (param instanceof MediaObject && param.id === undefined) {
        var index = findIndex(operations, function (element) {
          return operation != element && element.params.object === param;
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
          constructorParams[key] = checkId(operation, constructorParams[key]);
        break;

      default:
        params.object = checkId(operation, params.object);

        var operationParams = params.operationParams;
        for (var key in operationParams)
          operationParams[key] = checkId(operation, operationParams[key]);
      };
    });

    function callback2(error, result) {
      if (error) return callback(error);

      operations.forEach(operationResponse, result)

      callback(null, result);
    };

    Promise.all(promises).then(function () {
        operations.forEach(serializeOperation)

        encode('transaction', params, callback2);
      },
      callback);
  }

  /**
   * @function module:kurentoClient.KurentoClient~commitSerial
   *
   * @param {external:Object} params
   * @param {external:Function} callback
   */
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

  /**
   * @function module:kurentoClient.KurentoClient~registerObject
   *
   * @param {module:core/abstracts.MediaObject} mediaObject
   * @param {external:string} id
   */
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
   * @function module:kurentoClient.KurentoClient#getMediaobjectById
   *
   * @param {(external:String|external:string[])} id - ID of the MediaElement
   * @param {module:kurentoClient.KurentoClient~getMediaobjectByIdCallback} callback
   *
   * @return {external:Promise}
   */
  this.getMediaobjectById = function (id, callback) {
    return disguise(createPromise(id, describe, callback), this)
  };
  /**
   * @callback module:kurentoClient.KurentoClient~getMediaobjectByIdCallback
   * @param {external:Error} error
   * @param {(module:core/abstracts.MediaElement|module:core/abstracts.MediaElement[])} result
   *  The requested MediaElement
   */

  var mediaObjectCreator = new MediaObjectCreator(this, encodeCreate,
    encodeRpc, encodeTransaction, this.getMediaobjectById.bind(this),
    options.strict);

  /**
   * @function module:kurentoClient.KurentoClient~describe
   *
   * @param {external:string} id
   * @param {external:Function} callback
   */
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

  /**
   * @function module:kurentoClient.KurentoClient#_resetCache
   * @private
   */
  Object.defineProperty(this, '_resetCache', {
    value: function () {
      objects = {}
    }
  })

  /**
   * Create a new instance of a MediaObject
   *
   * @function module:kurentoClient.KurentoClient#create
   *
   * @param {external:String} type - Type of the element
   * @param {external:string[]} [params]
   * @param {module:kurentoClient.KurentoClient~createCallback} callback
   *
   * @return {(module:core/abstracts.MediaObject|module:core/abstracts.MediaObject[])}
   */
  this.create = mediaObjectCreator.create.bind(mediaObjectCreator);
  /**
   * @callback module:kurentoClient.KurentoClient~createCallback
   * @param {external:Error} error
   * @param {module:core/abstracts.MediaElement} result
   *  The created MediaElement
   */

  function connect(callback) {
    callback = (callback || noop).bind(this)

    //
    // Ping
    //
    function enablePing() {
      enabledPings = true;
      if (!pingPongStarted) {
        pingPongStarted = true;
        pingInterval = setInterval(sendPing, HEARTBEAT);
        sendPing();
      }
    }

    function updateNotReconnectIfLessThan() {
      notReconnectIfNumLessThan = pingNextNum;
      console.log("notReconnectIfNumLessThan = " + notReconnectIfNumLessThan);
    }

    function sendPing() {
      if (enabledPings) {
        var params = null;

        if (pingNextNum == 0 || pingNextNum == notReconnectIfNumLessThan) {
          params = {
            interval: PING_INTERVAL
          };
        }

        pingNextNum++;

        var request = {
          method: 'ping',
          params: params,
          callback: (function (pingNum) {
            return function (error, result) {
              if (error) {
                if (pingNum > notReconnectIfNumLessThan) {
                  enabledPings = false;
                  updateNotReconnectIfLessThan();
                  console.log("Server did not respond to ping message " +
                    pingNum + ".");
                  clearInterval(pingInterval);
                  pingPongStarted = false;
                }
              }
            }
          }(pingNextNum))
        }
        send(request);
      } else {
        console.log("Trying to send ping, but ping is not enabled");
      }
    }

    //
    // Reconnect websockets
    //

    var closed = false;
    var reconnected = false;
    var re = reconnect({
        // all options are optional
        // initialDelay: 1e3,
        // maxDelay: 30e3,
        // type: 'fibonacci',      // available: fibonacci, exponential
        // randomisationFactor: 0,
        // immediate: false
        failAfter: failAfter
      }, function (ws_stream) {
        if (closed)
          ws_stream.writable = false;

        rpc.transport = ws_stream;
        enablePing();
        if (reconnected) {
          var params = {
            sessionId: self.sessionId
          };
          var request = {
            method: 'connect',
            params: params,
            callback: function (error, response) {
              if (error) {
                if (error.code === INVALID_SESSION) {
                  console.log("Invalid Session")
                  objects = {}
                  onReconnected(false);
                }
              } else {
                onReconnected(true);
              }
            }
          }
          send(request);
        } else {
          onConnected();
        }
      })
      .connect(ws_uri);

    Object.defineProperty(this, '_re', {
      get: function () {
        return re
      }
    })

    /**
     * @function module:kurentoClient.KurentoClient#close
     */
    this.close = function () {
      closed = true;

      prevRpc_result.then(re.disconnect.bind(re));
    };

    re.on('fail', this.emit.bind(this, 'disconnect'));

    re.on('reconnect', function (n, delay) {
      console.log('reconnect to server', n, delay, self.sessionId);
      if (pingInterval != undefined) {
        clearInterval(pingInterval);
        pingPongStarted = false;
      }

      reconnected = true;
    })

    //
    // Promise interface ("thenable")
    //

    /**
     * @function module:kurentoClient.KurentoClient#then
     *
     * @param {external:Function} onFulfilled
     * @param {external:Function} [onRejected]
     *
     * @return {external:Promise}
     */
    this.then = function (onFulfilled, onRejected) {
      if (re.connected)
        var promise = Promise.resolve(disguise.unthenable(this))
      else if (!re.reconnect)
        var promise = Promise.reject(new Error('Connection error'))
      else {
        var self = this

        var promise = new Promise(function (resolve, reject) {
          function success() {
            re.removeListener('fail', failure);

            resolve(disguise.unthenable(self));
          };

          function failure() {
            re.removeListener('connection', success);

            reject(new Error('Connection error'));
          };

          re.once('connection', success);
          re.once('fail', failure);
        });

      }

      promise = promise.then(onFulfilled ? onFulfilled.bind(this) :
        function (result) {
          return Promise.resolve(result)
        },
        onRejected ? onRejected.bind(this) :
        function (error) {
          return Promise.reject(error)
        });

      return disguise(promise, this)
    };

    /**
     * @function module:kurentoClient.KurentoClient#catch
     *
     * @param {external:Function} [onRejected]
     *
     * @return {external:Promise}
     */
    this.catch = this.then.bind(this, null);

    // Check for available modules in the Kurento Media Server

    var thenable = this
    if (options.strict)
      thenable = this.getServerManager()
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
            var message = "Modules '" + notInstalled.slice(0, -1).join("', '") +
              "' and '" + notInstalled[length - 1] +
              "' are not installed in the Kurento Media Server"

          var error = new SyntaxError(message)
          error.modules = notInstalled

          return Promise.reject(error)
        }

        return Promise.resolve(self)
      })

    promiseCallback(thenable, callback);
  };
  connect.call(self, callback);
};
inherits(KurentoClient, EventEmitter);
/**
 * @callback module:kurentoClient.KurentoClient~constructorCallback
 * @param {external:Error} error
 * @param {module:kurentoClient.KurentoClient} client
 *  The created KurentoClient
 */

/**
 * Connect the source of a media to the sink of the next one
 *
 * @function module:kurentoClient.KurentoClient#connect
 *
 * @param {...module:core/abstracts.MediaObject} media - A media to be connected
 * @param {module:kurentoClient.KurentoClient~connectCallback} [callback]
 *
 * @return {external:Promise}
 *
 * @throws {SyntaxError}
 */
KurentoClient.prototype.connect = function (media, callback) {
  if (!(media instanceof Array)) {
    media = Array.prototype.slice.call(arguments, 0);
    callback = (typeof media[media.length - 1] === 'function') ? media.pop() :
      undefined;
  }

  callback = (callback || noop).bind(this)

  // Check if we have enought media components
  if (media.length < 2)
    throw new SyntaxError("Need at least two media elements to connect");

  return media[0].connect(media.slice(1), callback)
};
/**
 * @callback module:kurentoClient.KurentoClient~connectCallback
 * @param {external:Error} error
 */

/**
 * Get a reference to the current Kurento Media Server we are connected
 *
 * @function module:kurentoClient.KurentoClient#getServerManager
 *
 * @param {module:kurentoClient.KurentoClient~getServerManagerCallback} callback
 *
 * @return {external:Promise}
 */
KurentoClient.prototype.getServerManager = function (callback) {
  return this.getMediaobjectById('manager_ServerManager', callback)
};
/**
 * @callback module:kurentoClient.KurentoClient~getServerManagerCallback
 * @param {external:Error} error
 * @param {module:core/abstracts.ServerManager} server
 *  Info of the MediaServer instance
 */

//
// Helper function to return a singleton client for a particular ws_uri
//
var singletons = {};

/**
 * Creates a unique connection with the Kurento Media Server
 *
 * @function module:kurentoClient.KurentoClient.getSingleton
 * @see module:kurentoClient.KurentoClient
 *
 * @param {external:String} ws_uri - Address of the Kurento Media Server
 * @param {module:kurentoClient.KurentoClient~KurentoClientDict} [options]
 * @param {module:kurentoClient.KurentoClient~constructorCallback} [callback]
 *
 * @return {external:Promise}
 */
KurentoClient.getSingleton = function (ws_uri, options, callback) {
  var client = singletons[ws_uri]
  if (!client) {
    // Fix optional parameters
    if (options instanceof Function) {
      callback = options;
      options = undefined;
    };

    client = KurentoClient(ws_uri, options, function (error, client) {
      if (error) return callback(error);

      singletons[ws_uri] = client
      client.on('disconnect', function () {
        delete singletons[ws_uri]
      })
    });
  }

  return disguise(promiseCallback(client, callback), client)
}

/**
 * Get a complexType across the qualified name
 *
 * @function module:kurentoClient.KurentoClient#getComplexType
 *
 * @param {external:String} complexType - ComplexType's name
 *
 * @return {module:core/complexType}
 */
KurentoClient.getComplexType = function (complexType) {
  return KurentoClient.register.complexTypes[complexType]
};

// Export KurentoClient

module.exports = KurentoClient;
