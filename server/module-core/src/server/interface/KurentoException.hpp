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

#ifndef __KURENTO_EXCEPTION_HPP__
#define __KURENTO_EXCEPTION_HPP__

#include <exception>
#include <string>

/* Error codes */
#define ERROR_MIN 40000
#define ERROR_MAX 49999

/* GENERIC MEDIA ERRORS */
#define MEDIA_ERROR_MIN 40000
#define MEDIA_ERROR_MAX 40099
// #define MEDIA_ERROR 40000
#define MARSHALL_ERROR 40001
// #define UNMARSHALL_ERROR 40002
#define UNEXPECTED_ERROR 40003
#define CONNECT_ERROR 40004
#define UNSUPPORTED_MEDIA_TYPE 40005
#define NOT_IMPLEMENTED 40006
#define INVALID_SESSION 40007
#define MALFORMED_TRANSACTION 40008
#define NOT_ENOUGH_RESOURCES 40009

/* MediaObject ERRORS */
#define MEDIA_OBJECT_ERROR_MIN 40100
#define MEDIA_OBJECT_ERROR_MAX 40199
#define MEDIA_OBJECT_TYPE_NOT_FOUND 40100
#define MEDIA_OBJECT_NOT_FOUND 40101
// #define MEDIA_OBJECT_CAST_ERROR 40102
// #define MEDIA_OBJECT_HAS_NOT_PARENT 40103
#define MEDIA_OBJECT_CONSTRUCTOR_NOT_FOUND 40104
#define MEDIA_OBJECT_METHOD_NOT_FOUND 40105
#define MEDIA_OBJECT_EVENT_NOT_SUPPORTED 40106
#define MEDIA_OBJECT_ILLEGAL_PARAM_ERROR 40107
#define MEDIA_OBJECT_NOT_AVAILABLE 40108
#define MEDIA_OBJECT_NOT_FOUND_TRANSACTION_NO_COMMIT 40109
#define MEDIA_OBJECT_TAG_KEY_NOT_FOUND 40110
#define MEDIA_OBJECT_OPERATION_NOT_SUPPORTED 40111

/* SDP ERRORS */
#define SDP_ERROR_MIN 40200
#define SDP_ERROR_MAX 40299
#define SDP_CREATE_ERROR 40200
#define SDP_PARSE_ERROR 40201
#define SDP_END_POINT_NO_LOCAL_SDP_ERROR 40202
#define SDP_END_POINT_NO_REMOTE_SDP_ERROR 40203
#define SDP_END_POINT_GENERATE_OFFER_ERROR 40204
#define SDP_END_POINT_PROCESS_OFFER_ERROR 40205
#define SDP_END_POINT_PROCESS_ANSWER_ERROR 40206
#define SDP_CONFIGURATION_ERROR 40207
#define SDP_END_POINT_ALREADY_NEGOTIATED 40208
#define SDP_END_POINT_NOT_OFFER_GENERATED 40209
#define SDP_END_POINT_ANSWER_ALREADY_PROCCESED 40210
#define SDP_END_POINT_CANNOT_CREATE_SESSON 40211

/* HTTP ERRORS */
#define HTTP_ERROR_MIN 40300
#define HTTP_ERROR_MAX 40399
#define HTTP_END_POINT_REGISTRATION_ERROR 40300

/* ICE ERRORS */
#define ICE_ERROR_MIN 40400
#define ICE_ERROR_MAX 40499
#define ICE_GATHER_CANDIDATES_ERROR 40400
#define ICE_ADD_CANDIDATE_ERROR 40401

/* SERVER MANAGER ERRORS */
#define SERVER_MANAGER_ERROR_MIN 40500
#define SERVER_MANAGER_ERROR_MAX 40599
#define SERVER_MANAGER_ERROR_KMD_NOT_FOUND 40500

/* URI ERRORS */
#define URI_ERROR_MIN 40600
#define URI_ERROR_MAX 40699
#define URI_PATH_FILE_NOT_FOUND 40600

/* PLAYER ERRORS */
#define PLAYER_ERROR_MIN 40700
#define PLAYER_ERROR_MAX 40799
#define PLAYER_SEEK_FAIL 40700

/* Custom ERRORS */
/* Reserved codes for custom modules */
#define CUSTOM_ERROR_MIN 49000
#define CUSTOM_ERROR_MAX 49999

namespace kurento
{

class KurentoException: public virtual std::exception
{
public:
  KurentoException (int code, const std::string &message) : message (message),
    code (code) {};
  virtual ~KurentoException() {};

  virtual const char *what() const noexcept
  {
    return message.c_str();
  };

  const std::string &getMessage() const
  {
    return message;
  };

  int getCode() const
  {
    return code;
  }

  std::string getType()
  {
    switch (code) {
    /* Error codes */
    /* GENERIC MEDIA ERRORS */

//    case MEDIA_ERROR:
//      return "MEDIA_ERROR";
    case MARSHALL_ERROR:
      return "MARSHALL_ERROR";

//    case UNMARSHALL_ERROR:
//      return "UNMARSHALL_ERROR";
    case UNEXPECTED_ERROR:
      return "UNEXPECTED_ERROR";

    case CONNECT_ERROR:
      return "CONNECT_ERROR";

    case UNSUPPORTED_MEDIA_TYPE:
      return "UNSUPPORTED_MEDIA_TYPE";

    case NOT_IMPLEMENTED:
      return "NOT_IMPLEMENTED";

    case INVALID_SESSION:
      return "INVALID_SESSION";

    case MALFORMED_TRANSACTION:
      return "MALFORMED_TRANSACTION";

    case NOT_ENOUGH_RESOURCES:
      return "NOT_ENOUGH_RESOURCES";

    /* MediaObject ERRORS */
    case MEDIA_OBJECT_TYPE_NOT_FOUND:
      return "MEDIA_OBJECT_TYPE_NOT_FOUND";

    case MEDIA_OBJECT_NOT_FOUND:
      return "MEDIA_OBJECT_NOT_FOUND";

//    case MEDIA_OBJECT_CAST_ERROR:
//      return "MEDIA_OBJECT_CAST_ERROR";
//    case MEDIA_OBJECT_HAS_NOT_PARENT:
//      return "MEDIA_OBJECT_HAS_NOT_PARENT";
    case MEDIA_OBJECT_CONSTRUCTOR_NOT_FOUND:
      return "MEDIA_OBJECT_CONSTRUCTOR_NOT_FOUND";

    case MEDIA_OBJECT_METHOD_NOT_FOUND:
      return "MEDIA_OBJECT_METHOD_NOT_FOUND";

    case MEDIA_OBJECT_EVENT_NOT_SUPPORTED:
      return "MEDIA_OBJECT_EVENT_NOT_SUPPORTED";

    case MEDIA_OBJECT_ILLEGAL_PARAM_ERROR:
      return "MEDIA_OBJECT_ILLEGAL_PARAM_ERROR";

    case MEDIA_OBJECT_NOT_AVAILABLE:
      return "MEDIA_OBJECT_NOT_AVAILABLE";

    case MEDIA_OBJECT_NOT_FOUND_TRANSACTION_NO_COMMIT:
      return "MEDIA_OBJECT_NOT_FOUND_TRANSACTION_NO_COMMIT";

    case MEDIA_OBJECT_TAG_KEY_NOT_FOUND:
      return "MEDIA_OBJECT_TAG_KEY_NOT_FOUND";

    case MEDIA_OBJECT_OPERATION_NOT_SUPPORTED:
      return "MEDIA_OBJECT_OPERATION_NOT_SUPPORTED";

    /* SDP ERRORS */
    case SDP_CREATE_ERROR:
      return "SDP_CREATE_ERROR";

    case SDP_PARSE_ERROR:
      return "SDP_PARSE_ERROR";

    case SDP_END_POINT_NO_LOCAL_SDP_ERROR:
      return "SDP_END_POINT_NO_LOCAL_SDP_ERROR";

    case SDP_END_POINT_NO_REMOTE_SDP_ERROR:
      return "SDP_END_POINT_NO_REMOTE_SDP_ERROR";

    case SDP_END_POINT_GENERATE_OFFER_ERROR:
      return "SDP_END_POINT_GENERATE_OFFER_ERROR";

    case SDP_END_POINT_PROCESS_OFFER_ERROR:
      return "SDP_END_POINT_PROCESS_OFFER_ERROR";

    case SDP_END_POINT_PROCESS_ANSWER_ERROR:
      return "SDP_END_POINT_PROCESS_ANSWER_ERROR";

    case SDP_CONFIGURATION_ERROR:
      return "SDP_CONFIGURATION_ERROR";

    case SDP_END_POINT_ALREADY_NEGOTIATED:
      return "SDP_END_POINT_ALREADY_NEGOTIATED";

    case SDP_END_POINT_NOT_OFFER_GENERATED:
      return "SDP_END_POINT_NOT_OFFER_GENERATED";

    case SDP_END_POINT_ANSWER_ALREADY_PROCCESED:
      return "SDP_END_POINT_ANSWER_ALREADY_PROCCESED";

    case SDP_END_POINT_CANNOT_CREATE_SESSON:
      return "SDP_END_POINT_CANNOT_CREATE_SESSON";

    /* HTTP ERRORS */
    case HTTP_END_POINT_REGISTRATION_ERROR:
      return "HTTP_END_POINT_REGISTRATION_ERROR";

    /* ICE ERRORS */
    case ICE_GATHER_CANDIDATES_ERROR:
      return "ICE_GATHER_CANDIDATES_ERROR";

    case ICE_ADD_CANDIDATE_ERROR:
      return "ICE_ADD_CANDIDATE_ERROR";

    /* URI ERRORS */
    case URI_PATH_FILE_NOT_FOUND:
      return "URI_PATH_FILE_NOT_FOUND";

    /*PLAYER ERRORS*/
    case PLAYER_SEEK_FAIL:
      return "PLAYER_SEEK_FAIL";

    default:
      return "UNDEFINED";
    }
  }

protected:
  std::string message;
  int code;
};

} /* kurento */

#endif /* __KURENTO_EXCEPTION_HPP__ */
