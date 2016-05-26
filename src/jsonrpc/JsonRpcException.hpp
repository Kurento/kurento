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

#ifndef __JSONRPC_EXCEPTION_HPP__
#define __JSONRPC_EXCEPTION_HPP__

#include <json/json.h>

namespace kurento
{
namespace JsonRpc
{

namespace ErrorCode
{
static const int PARSE_ERROR = -32700;
static const int INVALID_REQUEST = -32600;
static const int METHOD_NOT_FOUND = -32601;
static const int INVALID_PARAMS = -32602;
static const int INTERNAL_ERROR = -32603;
static const int SERVER_ERROR_INIT = -32000;
static const int SERVER_ERROR_END = -32099;
}

class CallException
{

public:
  CallException (int code, const std::string &message,
                 const Json::Value &data = Json::Value::null)
  {
    this->code = code;
    this->message = message;
    this->data = data;

    if (data.isNull () ) {
      std::string errorType;

      if (code == ErrorCode::PARSE_ERROR) {
        errorType = "PARSE_ERROR";
      } else if (code == ErrorCode::INVALID_REQUEST) {
        errorType = "INVALID_REQUEST";
      } else if (code == ErrorCode::METHOD_NOT_FOUND) {
        errorType = "METHOD_NOT_FOUND";
      } else if (code == ErrorCode::INVALID_PARAMS) {
        errorType = "INVALID_PARAMS";
      } else if (code == ErrorCode::INTERNAL_ERROR) {
        errorType = "INTERNAL_ERROR";
      }

      if (!errorType.empty() ) {
        this->data["type"] = errorType;
      }
    }
  }

  int getCode()
  {
    return code;
  }

  std::string getMessage()
  {
    return message;
  }

  Json::Value getData()
  {
    return data;
  }

private:
  int code;
  std::string message;
  Json::Value data;
};

} /* JsonRpc */
} /* kurento */

#endif /* __JSONRPC_EXCEPTION_H__ */
