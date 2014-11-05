/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
