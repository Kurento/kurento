function RpcBuilder()
{
  var requestID = 0;

  var requests  = {};
  var responses = {};


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
    if(id)
    {
      result.id = id;

      if(message.error)
        result.error = message.error;
      else if(message.value)
        result.value = message.value;
    };

    return JSON.stringify(result);
  };

  function unpack(message)
  {
    if(typeof message == 'string')
      message = JSON.parse(message);

    function throwException(text)
    {
      var error = new TypeError(text);
          error.data = message;

      throw error;
    };

    var version = message.jsonrpc;
    if(version != "2.0")
      throwException("Invalid JsonRPC version: "+version);

    return message;
  };


  function RpcNotification(method, params)
  {
    Object.defineProperty(this, 'method', {value: method});
    Object.defineProperty(this, 'params', {value: params});
  };

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
    this.response = function(error, value)
    {
      if(previousResponse)
        return previousResponse;

      var message = pack(
      {
        id:    id,
        error: error,
        value: value
      });

      responses[id] = message;

      return message;
    };
  };
  RpcRequest.prototype.__proto__   = RpcNotification.prototype;
  RpcRequest.prototype.constructor = RpcRequest;


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
      method: method,
      params: params
    };

    if(callback)
    {
      var id = requestID++;
      message.id = id;

      requests[id] = callback;
    };

    var message = pack(message);

    return message;
  };

  /**
   *
   *
   * @param {string} message - JSON message
   *
   * @returns {RpcNotification|RpcRequest|null}
   *
   * @throws {TypeError}
   */
  this.decodeJSON = function(message)
  {
    if(!message)
      throwException("No message is defined");

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

        if(!result ^ !error)
        {
          delete requests[id];

          request(error, result);
          return;
        };

        // Invalid response message
        if(result && error)
          throwException("Both result and error are defined");
        throwException("No result or error is defined");
      };

      // Request not found for this response
      throwException("No callback was defined for this message");
    };

    // Notification
    if(method)
      return new RpcNotification(method, params);

    throwException("Invalid message type");
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