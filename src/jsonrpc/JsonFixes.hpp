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

#ifndef JSONFIXES_HPP
#define JSONFIXES_HPP

#include <string>
#include <json/json.h>

namespace kurento
{

namespace JsonFixes
{

// This is a workaround for a bug in jsoncpp that can't get numeric values as string
std::string getString (const Json::Value &value);

} /* JsonFixes */

} /* kurento */

#endif // JSONFIXES_HPP
