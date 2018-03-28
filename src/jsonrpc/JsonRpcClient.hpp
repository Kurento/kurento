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

#ifndef __JSONRPC_CLIENT_HPP__
#define __JSONRPC_CLIENT_HPP__

#include <string>
#include <functional>
#include <atomic>
#include <memory>

#include <json/json.h>

#include "JsonRpcHandler.hpp"

namespace kurento
{
namespace JsonRpc
{

typedef std::function<void (const std::string &) > MessageHandler;

class Transport
{
public:
  Transport () {}
  virtual ~Transport() {}

  void registerMessageHandler (MessageHandler handler)
  {
    this->handler = handler;
  }

  virtual void sendMessage (const std::string &data) = 0;

protected:
  void messageReceived (const std::string &data)
  {
    if (handler) {
      handler (data);
    }
  }

private:
  MessageHandler handler;
};

class Client
{
public:

  Client (std::shared_ptr<Transport> transport);
  Client (std::shared_ptr<Transport> transport,
          std::shared_ptr<Handler> eventHandler);
  virtual ~Client() {}

  typedef std::function<void (const Json::Value &result, bool isError) >
  Continuation;

  void sendRequest (const std::string &method, Json::Value &params,
                    Continuation cont);
  void sendNotification (const std::string &method,
                         Json::Value &params);

private:
  void onMessageReceived (const std::string &msg);

  std::atomic<long> id{};

  std::map <std::string, Continuation> responseHandlers;
  std::shared_ptr <Transport> transport;
  std::shared_ptr <Handler> eventHandler;
};

} /* JsonRpc */
} /* kurento  */

#endif /* __JSONRPC_CLIENT_HPP__ */
