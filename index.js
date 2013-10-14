function RpcBuilder()
{
  var requestID = 0;

  var requests  = HandshakeConnector.requests;
  var responses = HandshakeConnector.responses;


  function RpcNotification(method, params)
  {
    Object.defineProperty(this, 'method', {value: method});
    Object.defineProperty(this, 'params', {value: params});
  };

  function RpcRequest(method, params, id)
  {
    RpcNotification.call(this, method, params);

    /**
     *
     *
     * @param {Error} error
     * @param {*} value
     *
     * @returns {string}
     */
    this.reply = function(error, value)
    {
      var message =
      {
        jsonrpc: "2.0",
        id: id
      };

      if(error)
        message.error = error;
      else
        message.value = value;

      return JSON.stringify(message);
    };
  };
  RpcRequest.prototype.__proto__   = RpcNotification.prototype;
  RpcRequest.prototype.constructor = RpcRequest;

  function RpcResponse(value)
  {
    Object.defineProperty(this, 'value', {value: value});
  };


  // JsonRPC 2.0

  /**
   *
   *
   * @returns {string} A raw JsonRPC 2.0 request string
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
      jsonrpc: "2.0",
      method: method
    };

    if(params)
      message.params = params;

    if(callback)
    {
      var id = requestID++;
      message.id = id;

      requests[id] =
      {
        message: message,
        callback: callback
      };
    };

    return JSON.stringify(message);
  };

  /**
   *
   *
   * @param {string} message - JSON message
   *
   * @returns {RpcNotification|RpcRequest|RpcResponse|Error}
   */
  this.decodeJSON = function(message)
  {
    message = JSON.parse(message);

    var version = message.jsonrpc;
    if(version != "2.0")
    {
      console.error("Invalid JsonRPC version: "+version);
      console.error(message);
      return;
    };

    var id = message.id;
    var method = message.method;

    if(id)
    {
      // Request
      if(method)
        return new RpcRequest(method, message.params, id);

      // Response
      var result = message.result;
      if(result)
      {
        var request = requests[id];
        delete requests[id];

        if(request)
        {
          request.callback(result);

          return new RpcResponse(result);
        };

        throw new TypeError("No callback was defined for this message");
      };

      // Error
      var error = message.error;
      if(error)
        return error;
    };

    // Notification
    if(method)
      return new RpcNotification(method, params);

    throw new TypeError("Invalid message type");
  };


  // XML-RPC

  this.encodeXML = function(method, params, callback)
  {
    throw new TypeError("Not yet implemented");
  };

  this.decodeXML = function(message)
  {
    throw new TypeError("Not yet implemented");
  };
};


module.exports = RpcBuilder;