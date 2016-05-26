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

#include "JsonRpcUtils.hpp"
#include "JsonRpcException.hpp"

namespace kurento
{
namespace JsonRpc
{

static void
checkParameter (const Json::Value &params, const std::string &name)
{
  if (!params.isMember (name) ) {
    JsonRpc::CallException e (JsonRpc::ErrorCode::SERVER_ERROR_INIT,
                              "'" + name + "' parameter is required");
    throw e;
  }
}

void
getValue (const Json::Value &params, const std::string &name,
          std::string &_return)
{
  checkParameter (params, name);

  if (!params[name].isConvertibleTo (Json::stringValue) ) {
    JsonRpc::CallException e (JsonRpc::ErrorCode::SERVER_ERROR_INIT,
                              "'" + name + "' parameter should be a string");
    throw e;
  }

  _return = params[name].asString();
}

void
getValue (const Json::Value &params, const std::string &name,
          int &_return)
{
  checkParameter (params, name);

  if (!params[name].isConvertibleTo (Json::intValue) ) {
    JsonRpc::CallException e (JsonRpc::ErrorCode::SERVER_ERROR_INIT,
                              "'" + name + "' parameter should be an integer");
    throw e;
  }

  _return = params[name].asInt ();
}

void
getValue (const Json::Value &params, const std::string &name,
          bool &_return)
{
  checkParameter (params, name);

  if (!params[name].isConvertibleTo (Json::booleanValue) ) {
    JsonRpc::CallException e (JsonRpc::ErrorCode::SERVER_ERROR_INIT,
                              "'" + name + "' parameter should be a boolean");
    throw e;
  }

  _return = params[name].asBool ();
}

void
getValue (const Json::Value &params, const std::string &name,
          Json::Value &_return)
{
  checkParameter (params, name);

  if (!params[name].isObject () ) {
    JsonRpc::CallException e (JsonRpc::ErrorCode::SERVER_ERROR_INIT,
                              "'" + name + "' parameter should be an object");
    throw e;
  }

  _return = params[name];
}

void
getArray (const Json::Value &params, const std::string &name,
          Json::Value &_return)
{
  checkParameter (params, name);

  if (!params[name].isArray () ) {
    JsonRpc::CallException e (JsonRpc::ErrorCode::SERVER_ERROR_INIT,
                              "'" + name + "' parameter should be an array");
    throw e;
  }

  _return = params[name];
}

} /* JsonRpc */
} /* kurento */
