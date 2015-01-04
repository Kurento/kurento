/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

#include <functional>
#include "JsonRpcClient.hpp"
#include "JsonRpcConstants.hpp"

namespace kurento
{
namespace JsonRpc
{

Client::Client (std::shared_ptr<Transport> transport) : transport (transport)
{
  id = 0;

  transport->registerMessageHandler (std::bind (&Client::onMessageReceived, this,
                                     std::placeholders::_1) );
}

Client::Client (std::shared_ptr<Transport> transport,
                std::shared_ptr<Handler> eventHandler) : Client (transport)
{
  this->eventHandler = eventHandler;
}

void
Client::sendRequest (const std::string &method, Json::Value &params,
                     Continuation cont)
{
  Json::Value request;
  Json::FastWriter writer;
  std::string reqId = std::to_string (id++);

  request [JSON_RPC_ID] = reqId;
  request [JSON_RPC_PROTO] = JSON_RPC_PROTO_VERSION;
  request [JSON_RPC_METHOD] = method;

  if (params != Json::Value::null) {
    request [JSON_RPC_PARAMS] = params;
  }

  responseHandlers [reqId] = cont;

  transport->sendMessage (writer.write (request) );
}

void
Client::sendNotification (const std::string &method, Json::Value &params)
{
  Json::Value request;
  Json::FastWriter writer;

  request [JSON_RPC_PROTO] = JSON_RPC_PROTO_VERSION;
  request [JSON_RPC_METHOD] = method;

  if (params != Json::Value::null) {
    request [JSON_RPC_PARAMS] = params;
  }

  transport->sendMessage (writer.write (request) );
}

bool
check_protocol (const Json::Value &message)
{
  Json::Value proto;

  if (!message.isMember (JSON_RPC_PROTO) ) {
    // Invalid message
    return false;
  }

  proto = message[JSON_RPC_PROTO];

  if (!proto.isConvertibleTo (Json::ValueType::stringValue) ||
      proto.asString () != JSON_RPC_PROTO_VERSION) {
    // Invalid protocol version
    return false;
  }

  return true;
}

void
Client::onMessageReceived (const std::string &msg)
{
  Json::Reader reader;
  Json::Value message;

  reader.parse (msg, message);

  if (!check_protocol (message) ) {
    return;
  }

  if (message.isMember (JSON_RPC_RESULT) || message.isMember (JSON_RPC_ERROR) ) {
    Json::Value idValue = message[JSON_RPC_ID];
    std::string id;

    // Message is response
    if (!idValue.isString () ) {
      return;
    }

    id = message[JSON_RPC_ID].asString();

    try {
      Continuation cont = responseHandlers.at (id);

      if (message.isMember (JSON_RPC_RESULT) ) {
        cont (message[JSON_RPC_RESULT], false);
      } else {
        cont (message[JSON_RPC_ERROR], true);
      }

      responseHandlers.erase (id);
    } catch (std::out_of_range) {

    }
  } else {
    // Message is request
    std::string response;

    if (eventHandler) {
      eventHandler->process (msg, response);

      if (!response.empty () ) {
        transport->sendMessage (response);
      }
    }
  }
}

} /* JsonRpc */
} /* kurento */
