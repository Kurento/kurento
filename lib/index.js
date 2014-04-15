/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

var inherits = require('inherits');

var packers = require('./packers');
var Mapper = require('./Mapper');


const BASE_TIMEOUT = 5000;


/**
 * Representation of a RPC notification
 *
 * @class
 *
 * @constructor
 *
 * @param {String} method -method of the notification
 * @param params - parameters of the notification
 * @param [from] - source of the notification
 */
function RpcNotification(method, params, from)
{
  Object.defineProperty(this, 'method', {value: method, enumerable: true});
  Object.defineProperty(this, 'params', {value: params, enumerable: true});
  Object.defineProperty(this, 'from',   {value: from,   enumerable: true});
};


/**
 * @class
 *
 * @constructor
 */
function RpcBuilder(packer)
{
  if(!packer)
    throw new SyntaxError('Packer is not defined');

  if(!packer.pack || !packer.unpack)
    throw new SyntaxError('Packer is invalid');

  var requestID = 0;

  var requests  = new Mapper();
  var responses = new Mapper();


  /**
   * Store the response to prevent to process duplicate request later
   */
  function storeResponse(message, id, dest)
  {
    var response =
    {
      message: message,
      /** Timeout to auto-clean old responses */
      timeout: setTimeout(function()
      {
        responses.remove(id, dest);
      },
      BASE_TIMEOUT)
    };

    responses.set(response, id, dest);
  };


  /**
   * Representation of a RPC request
   *
   * @class
   * @extends RpcNotification
   *
   * @constructor
   *
   * @param {String} method -method of the notification
   * @param params - parameters of the notification
   * @param {Integer} id - identifier of the request
   * @param [from] - source of the notification
   */
  function RpcRequest(method, params, id, from)
  {
    RpcNotification.call(this, method, params, from);

    var response = responses.get(id, from);

    /**
     * @constant {Boolean} duplicated
     */
    Object.defineProperty(this, 'duplicated',
    {
      value: Boolean(response)
    });

    /**
     * Generate a response to this request
     *
     * @param {Error} [error]
     * @param {*} [result]
     *
     * @returns {string}
     */
    this.reply = function(error, result)
    {
      var message;

      // Duplicated request, remove old response timeout
      if(response)
        clearTimeout(response.timeout);

      // Duplicate & not-overriden request, re-send old response
      if(error == undefined && result === undefined)
        message = response.message;

      // New request or overriden one, create new response with provided data
      else
        message = packer.pack(
        {
          error:  error,
          result: result
        }, id);

      // Store the response to prevent to process a duplicated request later
      storeResponse(message, id, from);

      // Return the stored response so it can be directly send back
      return message;
    }
  };
  inherits(RpcRequest, RpcNotification);


  /**
   * Generates and encode a JsonRPC 2.0 message
   *
   * @param {String} method -method of the notification
   * @param params - parameters of the notification
   * @param [dest] - destination of the notification
   * @param [callback] - function called when a response to this request is
   *   received. If not defined, a notification will be send instead
   *
   * @returns {string} A raw JsonRPC 2.0 request or notification string
   */
  this.encode = function(method, params, dest, callback)
  {
    // Fix optional parameters
    if(params instanceof Function)
    {
      if(dest != undefined)
        throw new SyntaxError("There can't be parameters after callback");

      callback = params;
      dest     = undefined;
      params   = undefined;
    }

    else if(dest instanceof Function)
    {
      if(callback != undefined)
        throw new SyntaxError("There can't be parameters after callback");

      callback = dest;
      dest     = undefined;
    };

    // Encode message
    var message =
    {
      method: method,
      params: params
    };

    if(callback)
    {
      var id = requestID++;

      message = packer.pack(message, id);

      function dispatchCallback(error, result)
      {
        message.cancel();

        callback(error, result);
      };

      var request =
      {
        message:  message,
        callback: dispatchCallback,
        timeout:  setTimeout(function()
        {
          var error = new Error('Timed Out');
              error.request = message;

          dispatchCallback(error)
        },
        BASE_TIMEOUT)
      };

      // Hack to allow to add methods to the encoded string
      message = new String(message);

      /**
       * Allow to cancel a request and don't wait for a response
       */
      message.cancel = function()
      {
        clearTimeout(request.timeout);

        requests.remove(id, dest);
      };

      requests.set(request, id, dest);
    }
    else
    {
      message = packer.pack(message);
      message.cancel = function(){};
    }

    // Return the packed message
    return message;
  };

  /**
   * Decode and process a JsonRPC 2.0 message
   *
   * @param {string} message - string with the content of the JsonRPC 2.0 message
   *
   * @returns {RpcNotification|RpcRequest|undefined} - the representation of the
   *   notification or the request. If a response was processed, it will return
   *   `undefined` to notify that it was processed
   *
   * @throws {TypeError} - Message is not defined
   */
  this.decode = function(message)
  {
    if(!message)
      throw new TypeError("Message is not defined");

    message = packer.unpack(message);

    var id     = message.id;
    var method = message.method;
    var params = message.params;
    var from   = message.from;

    // Notification
    if(id == undefined)
      return new RpcNotification(method, params, from);

    // Request
    if(method)
      return new RpcRequest(method, params, id, from);

    // Response
    var request = requests.pop(id, from);
    if(request == undefined)
      throw new TypeError("No callback was defined for this message");

    // Process response
    request.callback(message.error, message.result);
  };
};


RpcBuilder.RpcNotification = RpcNotification;


module.exports = RpcBuilder;

RpcBuilder.packers = packers;
