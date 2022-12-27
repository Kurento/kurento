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

#include "JsonFixes.hpp"

namespace kurento
{

namespace JsonFixes
{

std::string getString (const Json::Value &value)
{
  switch (value.type () ) {
  case Json::ValueType::nullValue:
  case Json::ValueType::stringValue:
  case Json::ValueType::booleanValue:
    return value.asString();

  case Json::ValueType::intValue:
    return std::to_string (value.asInt () );

  case Json::ValueType::uintValue:
    return std::to_string (value.asUInt () );

  case Json::ValueType::realValue:
    return std::to_string (value.asDouble () );

  case Json::ValueType::arrayValue:
  case Json::ValueType::objectValue:
  default:
    return value.asString();
  }
}

} /* JsonFixes */

} /* kurento  */
