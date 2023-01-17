/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

#ifndef __JSONRPC_HANDLER_HPP__
#define __JSONRPC_HANDLER_HPP__

#include <string>
#include <functional>
#include <memory>
#include <map>

#include <json/json.h>
#include "JsonRpcException.hpp"

namespace kurento
{
namespace JsonRpc
{

class Handler
{
public:

  Handler() {};
  virtual ~Handler() {};

  typedef std::function<void (const Json::Value &, Json::Value &) >
  Method;

  void addMethod (const std::string &name, Method method);
  bool process (const std::string &msg, std::string &_responseMsg);
  bool process (const Json::Value &msg, Json::Value &_response);
  void setPreProcess (std::function < bool (const Json::Value &, Json::Value &) >
                      func);
  void setPostProcess (std::function < void (const Json::Value &, Json::Value &) >
                       func);
private:

  std::map<std::string, Method> methods;
  bool checkProtocol (const Json::Value &root, Json::Value &error);

  std::function < bool (const Json::Value &, Json::Value &) > preproc;
  std::function < void (const Json::Value &, Json::Value &) > postproc;
};

} /* JsonRpc */
} /* kurento  */

#endif /* __JSONRPC_HANDLER_HPP__ */
