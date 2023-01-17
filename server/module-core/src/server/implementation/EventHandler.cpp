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

#include "EventHandler.hpp"
#include "WorkerPool.hpp"

namespace kurento
{

static void
post_task (std::function <void () > cb)
{
  // Use a single thread pool for all EventHandlers
  static kurento::WorkerPool workers {};

  workers.post (cb);
}

EventHandler::EventHandler (std::shared_ptr <MediaObjectImpl> object) :
  object (object)
{
}

EventHandler::~EventHandler()
{
  try {
    std::shared_ptr <MediaObjectImpl> obj = object.lock();

    if (obj) {
      conn.disconnect();
    }
  } catch (...) {
  }
}

void
EventHandler::sendEventAsync  (std::function <void () > cb)
{
  post_task (cb);
}

} /* kurento */
