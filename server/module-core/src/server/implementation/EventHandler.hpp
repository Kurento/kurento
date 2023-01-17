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

#ifndef __EVENT_HANDLER_HPP__
#define __EVENT_HANDLER_HPP__

#include <memory>
#include <sigc++/sigc++.h>
#include <string>
#include <json/json.h>
#include <functional>

namespace kurento
{

class MediaObjectImpl;

class EventHandler : public std::enable_shared_from_this<EventHandler>
{
public:
  EventHandler (std::shared_ptr <MediaObjectImpl> object);

  virtual ~EventHandler();

  virtual void sendEvent (Json::Value &value) = 0;
  void sendEventAsync  (std::function <void () > cb);

  void setConnection (sigc::connection conn)
  {
    this->conn = conn;
  }

private:
  std::weak_ptr<MediaObjectImpl> object;
  sigc::connection conn;
};

} /* kurento */

#endif /* __EVENT_HANDLER_HPP__ */
