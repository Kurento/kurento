/**
 * JsonRPC 2.0 packer
 */

/**
 * Pack a JsonRPC 2.0 message
 *
 * @param {Object} message - object to be packaged. It requires to have all the
 *   fields needed by the JsonRPC 2.0 message that it's going to be generated
 *
 * @return {String} - the stringified JsonRPC 2.0 message
 */
function pack(message, id)
{
  var result =
  {
    jsonrpc: "2.0"
  };

  // Request
  if(message.method)
  {
    result.method = message.method;

    if(message.params)
      result.params = message.params;

    // Request is a notification
    if(id != undefined)
      result.id = id;
  }

  // Response
  else if(id != undefined)
  {
    if(message.error != undefined)
    {
      if(message.value != undefined)
        throw new TypeError("Both result and error are defined");

      result.error = message.error;
    }
    else if(message.value != undefined)
      result.value = message.value;
    else
      throw new TypeError("No result or error is defined");

    result.id = id;
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

  // Check if it's a valid message

  var version = message.jsonrpc;
  if(version != "2.0")
    throw new TypeError("Invalid JsonRPC version: "+version);

  if(message.method == undefined)
  {
    if(message.id == undefined)
      throw new TypeError("Invalid message: "+JSON.stringify(message));

    var result_defined = message.result !== undefined;
    var error_defined  = message.error  !== undefined;

    // Check only result or error is defined, not both or none
    if(result_defined && error_defined)
      throw new TypeError("Both result and error are defined: "
                         +JSON.stringify(message));

    if(!result_defined && !error_defined)
      throw new TypeError("No result or error is defined: "
                         +JSON.stringify(message));
  }

  // Return unpacked message
  return message;
};


exports.pack   = pack;
exports.unpack = unpack;
