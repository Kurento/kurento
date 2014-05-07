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


function unifyResponseMethods(responseMethods)
{
  if(!responseMethods) return {};

  for(var key in responseMethods)
  {
    var value = responseMethods[key];

    if(typeof value == 'string')
      responseMethods[key] =
      {
        response: value
      }
  };

  return responseMethods;
};


/**
 * Representation of a RPC notification
 *
 * @class
 *
 * @constructor
 *
 * @param {String} method -method of the notification
 * @param params - parameters of the notification
 */
function RpcNotification(method, params)
{
  Object.defineProperty(this, 'method', {value: method, enumerable: true});
  Object.defineProperty(this, 'params', {value: params, enumerable: true});
};


/**
 * @class
 *
 * @constructor
 */
function RpcBuilder(packer, options)
{
  if(!packer)
    throw new SyntaxError('Packer is not defined');

  if(!packer.pack || !packer.unpack)
    throw new SyntaxError('Packer is invalid');

  var responseMethods = unifyResponseMethods(packer.responseMethods);


  options = options || {};

  const request_timeout  = options.request_timeout  || BASE_TIMEOUT;
  const response_timeout = options.response_timeout || BASE_TIMEOUT;


  var self = this;

  var requestID = 0;

  var requests  = new Mapper();
  var responses = new Mapper();

  var message2Key = {};


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
      response_timeout)
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
    RpcNotification.call(this, method, params);

    var response = responses.get(id, from);

    /**
     * @constant {Boolean} duplicated
     */
    Object.defineProperty(this, 'duplicated',
    {
      value: Boolean(response)
    });

    var responseMethod = responseMethods[method];

    this.pack = function()
    {
      return packer.pack(this, id);
    }

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
      // Duplicated request, remove old response timeout
      if(response)
        clearTimeout(response.timeout);

      if(from != undefined)
      {
        if(error)
          error.dest = from;

        if(result)
          result.dest = from;
      };

      var message;

      // Protocol indicates that responses has own request methods
      if(responseMethod)
      {
        if(responseMethod.error == undefined && error)
          message = packer.pack(
          {
            error: error
          }, id);

        else
        {
          responseMethod = error
                         ? responseMethod.error
                         : responseMethod.response;

          message = packer.pack(
          {
            method: responseMethod,
            params: error || result
          }, id);
        }
      }

      // New request or overriden one, create new response with provided data
      else if(error || result)
        message = packer.pack(
        {
          error:  error,
          result: result
        }, id);

      // Duplicate & not-overriden request, re-send old response
      else if(response)
        message = response.message;

      // New empty reply, response null value
      else
        message = packer.pack({result: null}, id);

      // Store the response to prevent to process a duplicated request later
      storeResponse(message, id, from);

      // Return the stored response so it can be directly send back
      return message;
    }
  };
  inherits(RpcRequest, RpcNotification);


  /**
   * Allow to cancel a request and don't wait for a response
   */
  this.cancel = function(message)
  {
    var key = message2Key[message];
    if(!key) return;

    delete message2Key[message];

    var request = requests.pop(key.id, key.dest);
    if(!request) return;

    clearTimeout(request.timeout);
  };


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
        self.cancel(message);

        callback(error, result);
      };

      var request =
      {
        message:  message,
        callback: dispatchCallback,
        responseMethods: responseMethods[method] || {},
        timeout:  setTimeout(function()
        {
          var error = new Error('Request has timed out');
              error.request = request.message;

          dispatchCallback(error)
        },
        request_timeout)
      };

      message2Key[message] = {id: id, dest: dest};

      requests.set(request, id, dest);
    }
    else
      message = packer.pack(message);

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
    var params = message.params || {};

    var from = params.from;

    // Notification
    if(id == undefined)
      return new RpcNotification(method, params);

    // Request, or response with own method
    if(method)
    {
      // Check for response with own method
      var request = requests.get(id, from);
      if(request)
      {
        var responseMethods = request.responseMethods;

        if(method == responseMethods.error)
        {
          request.callback(params);

          return requests.remove(id, from);
        }

        if(method == responseMethods.response)
        {
          request.callback(null, params);

          return requests.remove(id, from);
        }
      }

      // Request
      message.pack = function()
      {
        return packer.pack(message, id);
      };

      return new RpcRequest(method, params, id, from);
    }

    // Response
    var request = requests.get(id, from);
    if(request == undefined)
      return console.warn("No callback was defined for this message", message);

    // Process response
    request.callback(message.error, message.result);
  };
};


RpcBuilder.RpcNotification = RpcNotification;


module.exports = RpcBuilder;

RpcBuilder.packers = packers;
