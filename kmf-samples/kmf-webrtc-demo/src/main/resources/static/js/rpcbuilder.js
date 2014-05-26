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

//
// JsonRPC 2.0 pack & unpack
//

/**
 * Pack a JsonRPC 2.0 message
 *
 * @param {Object} message - object to be packaged. It requires to have all the
 *   fields needed by the JsonRPC 2.0 message that it's going to be generated
 *
 * @return {String} - the stringified JsonRPC 2.0 message
 */
function pack(message)
{
  var result =
  {
    jsonrpc: "2.0"
  };

  if(message.method)
  {
    result.method = message.method;

    if(message.params)
      result.params = message.params;
  };

  var id = message.id;
  if(id != undefined)
  {
    result.id = id;

    if(message.error)
      result.error = message.error;
    else if(message.result)
      result.result = message.result;
  };

  return JSON.stringify(result);
};

/**
 * Unpack a JsonRPC 2.0 message
 *
 * @param {String} message - string with the content of the JsonRPC 2.0 message
 *
 * @throws {TypeError} - Invalid JsonRPC version
 *
 * @return {Object} - object filled with the JsonRPC 2.0 message content
 */
function unpack(message)
{
  if(typeof message == 'string')
    message = JSON.parse(message);

  var version = message.jsonrpc;
  if(version != "2.0")
    throw new TypeError("Invalid JsonRPC version: "+version);

  return message;
};


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

/**
 * @class
 *
 * @constructor
 */
function RpcBuilder()
{
  var requestID = 0;

  var requests  = {};
  var responses = {};


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
  function RpcRequest(method, params, id)
  {
    RpcNotification.call(this, method, params);

    var previousResponse = responses[id];

    Object.defineProperty(this, 'duplicated',
    {
      value: Boolean(previousResponse)
    });

    /**
     * Generate a response to this message
     *
     * @param {Error} error
     * @param {*} value
     *
     * @returns {string}
     */
    this.response = function(error, result)
    {
      if(previousResponse)
        return previousResponse;

      var message = pack(
      {
        id:    id,
        error: error,
        result: result
      });

      responses[id] = message;

      return message;
    };
  };
  RpcRequest.prototype.__proto__   = RpcNotification.prototype;
  RpcRequest.prototype.constructor = RpcRequest;

  RpcBuilder.RpcRequest = RpcRequest;

  //
  // JsonRPC 2.0
  //

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
  this.encodeJSON = function(method, params, callback)
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

    if(callback)
    {
      var id = requestID++;
      message.id = id;

      requests[id] = callback;
    };

    return pack(message);
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
  this.decodeJSON = function(message)
  {
    if(!message)
      throw new TypeError("Message is not defined");

    message = unpack(message);

    var id     = message.id;
    var method = message.method;
    var params = message.params;

    if(id != undefined)
    {
      // Request
      if(method)
        return new RpcRequest(method, params, id);

      // Response
      var request = requests[id];
      if(request)
      {
        var result = message.result;
        var error  = message.error;

        var result_undefined = result === undefined;
        var error_undefined  = error  === undefined;

        // Process request if only result or error is defined, not both or none
        if(result_undefined ^ error_undefined)
        {
          delete requests[id];

          request(error, result);
          return;
        };

        // Invalid response message
        if(result_undefined && error_undefined)
          throw new TypeError("No result or error is defined");
        throw new TypeError("Both result and error are defined");
      };

      // Request not found for this response
      throw new TypeError("No callback was defined for this message");
    };

    // Notification
    if(method)
      return new RpcNotification(method, params);

    throw new TypeError("Invalid message");
  };


  //
  // XML-RPC
  //

  /**
   * Generates and encode a XML-RPC message
   *
   * @param {String} method -method of the notification
   * @param params - parameters of the notification
   * @param [callback] - function called when a response to this request is
   *   received. If not defined, a notification will be send instead
   *
   * @returns {string} A raw JsonRPC 2.0 request or notification string
   */
  this.encodeXML = function(method, params, callback)
  {
    throw new TypeError("Not yet implemented");
  };

  /**
   * Decode and process a XML-RPC message
   *
   * @param {string} message - string with the content of the JsonRPC 2.0 message
   *
   * @returns {RpcNotification|RpcRequest|undefined} - the representation of the
   *   notification or the request. If a response was processed, it will return
   *   `undefined` to notify that it was processed
   *
   * @throws {TypeError}
   */
  this.decodeXML = function(message)
  {
    throw new TypeError("Not yet implemented");
  };
};


RpcBuilder.RpcNotification = RpcNotification;