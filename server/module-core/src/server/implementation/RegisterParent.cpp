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

#ifndef __REGISTER_PARENT_CPP__
#define __REGISTER_PARENT_CPP__

#include "RegisterParent.hpp"

namespace kurento
{

std::map <std::string, std::function<RegisterParent* (void) >>
    RegisterParent::complexTypesRegistered;

void
RegisterParent::registerType (std::string type,
                              std::function<RegisterParent* (void) > func)
{
  complexTypesRegistered[type] = func;
}

RegisterParent *
RegisterParent::createRegister (std::string type)
{
  return complexTypesRegistered [type] ();
}

} /* kurento */

#endif /*  __REGISTER_PARENT_CPP__ */
