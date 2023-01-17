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

#ifndef __FACTORY_HPP__
#define __FACTORY_HPP__

#include <memory>
#include <json/json.h>
#include <boost/property_tree/ptree.hpp>

namespace kurento
{

class MediaObjectImpl;

class Factory
{
public:
  Factory() {};
  virtual ~Factory() {};

  std::shared_ptr<MediaObjectImpl> createObject (const boost::property_tree::ptree
      &conf, const std::string &session, const Json::Value &params) const;

  static std::shared_ptr<MediaObjectImpl> getObject (const std::string &id);

  virtual std::string getName() const = 0;

protected:
  virtual MediaObjectImpl *createObjectPointer (
      const boost::property_tree::ptree &conf,
      const Json::Value &params) const = 0;
};

} /* kurento */

#endif /* __FACTORY_HPP__ */
