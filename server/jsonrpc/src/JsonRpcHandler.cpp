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

#include "JsonRpcHandler.hpp"
#include "JsonRpcConstants.hpp"

#define JSON_RPC_ERROR_INVALID_REQUEST "Invalid JSON-RPC request."

namespace kurento
{
namespace JsonRpc
{

using namespace ErrorCode;

void
Handler::addMethod (const std::string &name, Method method)
{
  methods[name] = method;
}

bool
Handler::checkProtocol (const Json::Value &msg, Json::Value &error)
{
  Json::Value err;

  if (!msg.isObject() || !msg.isMember (JSON_RPC_PROTO) ||
      msg[JSON_RPC_PROTO] != JSON_RPC_PROTO_VERSION) {
    error[JSON_RPC_ID] = Json::Value::null;
    error[JSON_RPC_PROTO] = JSON_RPC_PROTO_VERSION;

    err[JSON_RPC_ERROR_CODE] = INVALID_REQUEST;
    err[JSON_RPC_ERROR_MESSAGE] = JSON_RPC_ERROR_INVALID_REQUEST;
    error[JSON_RPC_ERROR] = err;
    return false;
  }

  if (msg.isMember (JSON_RPC_ID) && (msg[JSON_RPC_ID].isArray()
                                     || msg[JSON_RPC_ID].isObject() ) ) {
    error[JSON_RPC_ID] = Json::Value::null;
    error[JSON_RPC_PROTO] = JSON_RPC_PROTO_VERSION;

    err[JSON_RPC_ERROR_CODE] = INVALID_REQUEST;
    err[JSON_RPC_ERROR_MESSAGE] = JSON_RPC_ERROR_INVALID_REQUEST;
    error[JSON_RPC_ERROR] = err;
    return false;
  }

  if (!msg.isMember (JSON_RPC_METHOD) || !msg[JSON_RPC_METHOD].isString() ) {
    error[JSON_RPC_ID] = Json::Value::null;
    error[JSON_RPC_PROTO] = JSON_RPC_PROTO_VERSION;

    err[JSON_RPC_ERROR_CODE] = INVALID_REQUEST;
    err[JSON_RPC_ERROR_MESSAGE] = JSON_RPC_ERROR_INVALID_REQUEST;
    error[JSON_RPC_ERROR] = err;
    return false;
  }

  return true;
}

bool
Handler::process (const Json::Value &msg, Json::Value &_response)
{
  Json::Value error;
  std::string methodName;

  if (!checkProtocol (msg, error) ) {
    _response = error;
    return false;
  }

  if (msg.isArray() ) {
    Json::Value::ArrayIndex i = 0;
    Json::Value::ArrayIndex j = 0;

    for (i = 0 ; i < msg.size() ; i++) {
      Json::Value ret;

      process (msg[i], ret);

      if (ret != Json::Value::null) {
        _response[j] = ret;
        j++;
      }
    }

    return true;
  }

  _response[JSON_RPC_ID] = msg.isMember (JSON_RPC_ID) ? msg[JSON_RPC_ID] :
                           Json::Value::null;
  _response[JSON_RPC_PROTO] = JSON_RPC_PROTO_VERSION;

  methodName = msg[JSON_RPC_METHOD].asString();

  if (methodName != "" && methods.find (methodName) != methods.end() ) {
    Method &method = methods[methodName];
    Json::Value response;

    try {

      /* Execute pre-process function if any */
      if (preproc && !preproc (msg, _response) ) {
        return !_response.isMember (JSON_RPC_ERROR);
      }

      method (msg[JSON_RPC_PARAMS], response);

      if (!msg.isMember (JSON_RPC_ID) || msg[JSON_RPC_ID] == Json::Value::null) {
        if (response != Json::Value::null) {
          throw JsonRpc::CallException (ErrorCode::SERVER_ERROR_INIT,
                                        "Ignoring response because of a notification request",
                                        response);
        }

        _response = Json::Value::null;
      } else {
        _response[JSON_RPC_RESULT] = response;
      }

      /* Execute post-process */
      if (postproc) {
        postproc (msg, _response);
      }

      return true;
    } catch (CallException &e) {
      Json::Value error;
      Json::Value data;

      error[JSON_RPC_ERROR_CODE] = e.getCode();
      error[JSON_RPC_ERROR_MESSAGE] = e.getMessage();

      data = e.getData();

      if (data != Json::Value::null) {
        error[JSON_RPC_ERROR_DATA] = data;
      }

      _response[JSON_RPC_ERROR] = error;
    } catch (std::string &e) {
      Json::Value error;

      error[JSON_RPC_ERROR_CODE] = INTERNAL_ERROR;
      error[JSON_RPC_ERROR_MESSAGE] =
        std::string ("Unexpected error while processing method: ") + e;

      _response[JSON_RPC_ERROR] = error;
    } catch (std::exception &e) {
      Json::Value error;

      error[JSON_RPC_ERROR_CODE] = INTERNAL_ERROR;
      error[JSON_RPC_ERROR_MESSAGE] =
        std::string ("Unexpected error while processing method: ") + e.what();

      _response[JSON_RPC_ERROR] = error;
    } catch (...) {
      Json::Value error;

      error[JSON_RPC_ERROR_CODE] = INTERNAL_ERROR;
      error[JSON_RPC_ERROR_MESSAGE] = "Unexpected error while processing method";

      _response[JSON_RPC_ERROR] = error;
    }

    /* Execute post-process */
    if (postproc) {
      postproc (msg, _response);
    }

    return false;
  }

  error[JSON_RPC_ERROR_CODE] = METHOD_NOT_FOUND;
  error[JSON_RPC_ERROR_MESSAGE] = "Method not found.";
  _response[JSON_RPC_ERROR] = error;

  /* Execute post-process */
  if (postproc) {
    postproc (msg, _response);
  }

  return false;
}

bool
Handler::process (const std::string &msg, std::string &_responseMsg)
{
  Json::Value response;
  Json::Value request;
  Json::Value error;
  bool parse = false;
  Json::Reader reader;
  bool ret;

  Json::StreamWriterBuilder writerFactory;
  writerFactory["indentation"] = "";

  parse = reader.parse (msg, request);

  if (!parse) {
    response[JSON_RPC_ID] = Json::Value::null;
    response[JSON_RPC_PROTO] = JSON_RPC_PROTO_VERSION;

    error[JSON_RPC_ERROR_CODE] = PARSE_ERROR;
    error[JSON_RPC_ERROR_MESSAGE] = "Parse error.";
    response[JSON_RPC_ERROR] = error;
    _responseMsg = Json::writeString (writerFactory, response);
    return false;
  }

  ret = process (request, response);

  if (response != Json::Value::null) {
    _responseMsg = Json::writeString (writerFactory, response);
  }

  return ret;
}

void
Handler::setPreProcess (std::function
                        < bool (const Json::Value &, Json::Value &) > func)
{
  preproc = func;
}

void
Handler::setPostProcess (std::function
                         < void (const Json::Value &, Json::Value &) > func)
{
  postproc = func;
}

} /* JsonRpc */
} /* kurento */
