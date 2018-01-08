/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
  std::string reqId = std::to_string (id++);

  request [JSON_RPC_ID] = reqId;
  request [JSON_RPC_PROTO] = JSON_RPC_PROTO_VERSION;
  request [JSON_RPC_METHOD] = method;

  if (params != Json::Value::null) {
    request [JSON_RPC_PARAMS] = params;
  }

  responseHandlers [reqId] = cont;

  Json::StreamWriterBuilder writerFactory;
  writerFactory["indentation"] = "";
  transport->sendMessage (Json::writeString (writerFactory, request) );
}

void
Client::sendNotification (const std::string &method, Json::Value &params)
{
  Json::Value request;

  request [JSON_RPC_PROTO] = JSON_RPC_PROTO_VERSION;
  request [JSON_RPC_METHOD] = method;

  if (params != Json::Value::null) {
    request [JSON_RPC_PARAMS] = params;
  }

  Json::StreamWriterBuilder writerFactory;
  writerFactory["indentation"] = "";
  transport->sendMessage (Json::writeString (writerFactory, request) );
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
