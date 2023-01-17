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

#ifndef __FACTORY_REGISTRAR_HPP__
#define __FACTORY_REGISTRAR_HPP__

#include <list>
#include <memory>
#include <Factory.hpp>

namespace kurento
{

class FactoryRegistrar
{
public:
  FactoryRegistrar (std::map<std::string, std::shared_ptr<Factory>> &factories) :
    factories (factories) {};
  ~FactoryRegistrar() {};

  const std::map<std::string, std::shared_ptr<Factory>> &getFactories () const
  {
    return factories;
  }

private:
  std::map<std::string, std::shared_ptr<Factory>> factories;
};

} /* kurento */

typedef const kurento::FactoryRegistrar * (*RegistrarFactoryFunc) ();

#endif /*  __FACTORY_REGISTRAR_HPP__ */
