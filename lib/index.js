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

var packer = require('./packers').JsonRPC;
var Mapper = require('./Mapper');


//
// RPC message classes
//

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


//
// RPC-Builder
//

const BASE_TIMEOUT = 5000;


/**
 * @class
 *
 * @constructor
 */
function RpcBuilder()
{
  var requestID = 0;

  var requests  = new Mapper();
  var responses = new Mapper();


  /**
   * Store the response to prevent to process duplicate request later
   */
  function storeResponse(message, id, dest)
  {
    Object.defineProperty(message, 'stored', {value: true});

    var response =
    {
      message: message,
      id: id,
      /** Timeout to clean old responses */
      timeout: setTimeout(function()
      {
        clearTimeout(response.timeout);
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
   */
  function RpcRequest(method, params, id, from)
  {
    RpcNotification.call(this, method, params);

    var response = responses.get(id);

    Object.defineProperty(this, 'duplicated',
    {
      value: Boolean(response)
    });

    /**
     * Generate a response to this message
     *
     * @param {Error} error
     * @param {*} value
     *
     * @returns {string}
     */
    this.response = function(error, value)
    {
      if(response)
        return response;

      var message = packer.pack(
      {
        error: error,
        value: value
      }, id);

      responses.set(message, id);

      return message;
    };
  };
  inherits(RpcRequest, RpcNotification);


  /**
   * Generates and encode a JsonRPC 2.0 message
   *
   * @param {String} method -method of the notification
   * @param params - parameters of the notification
   * @param [callback] - function called when a response to this request is
   *   received. If not defined, a notification will be send instead
   *
   * @returns {string} A raw JsonRPC 2.0 request or notification string
   */
  this.encode = function(method, params, callback)
  {
    if(params instanceof Function)
    {
      if(callback != undefined)
        throw new SyntaxError("There can't be parameters after callback");

      callback = params;
      params = undefined;
    };

    var message =
    {
      method: method,
      params: params
    };

    var id;
    if(callback)
    {
      id = requestID++;

      requests.set(callback, id);
    };

    return packer.pack(message, id);
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

    storeResponse(message, id);

    // Notification
    if(id == undefined)
      return new RpcNotification(method, params);

    // Request
    if(method)
      return new RpcRequest(method, params, id);

    // Response
    var request = requests.pop(id);
    if(request == undefined)
      throw new TypeError("No callback was defined for this message");

    // Process response
    request(message.error, message.result);
  };
};


RpcBuilder.RpcNotification = RpcNotification;


module.exports = RpcBuilder;
