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

  if(message.method)
  {
    result.method = message.method;

    if(message.params)
      result.params = message.params;
  };

  if(id != undefined)
  {
    result.id = id;

    if(message.error)
      result.error = message.error;
    else if(message.value)
      result.value = message.value;
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


exports.pack   = pack;
exports.unpack = unpack;
