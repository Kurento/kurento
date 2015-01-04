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

  std::atomic<long> id;

  std::map <std::string, Continuation> responseHandlers;
  std::shared_ptr <Transport> transport;
  std::shared_ptr <Handler> eventHandler;
};

} /* JsonRpc */
} /* kurento  */

#endif /* __JSONRPC_CLIENT_HPP__ */
