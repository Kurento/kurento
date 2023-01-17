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

#include "Factory.hpp"
#include "MediaSet.hpp"

namespace kurento
{

std::shared_ptr<MediaObjectImpl>
Factory::getObject (const std::string &id)
{
  return MediaSet::getMediaSet()->getMediaObject (id);
}

std::shared_ptr< MediaObjectImpl >
Factory::createObject (const boost::property_tree::ptree &conf,
                       const std::string &session, const Json::Value &params) const
{
  std::shared_ptr< MediaObjectImpl > object;
  object = MediaSet::getMediaSet()->ref (dynamic_cast <MediaObjectImpl *>
                                         (createObjectPointer (conf, params) ) );

  MediaSet::getMediaSet()->ref (session, object);

  return object;
}

} /* kurento */
