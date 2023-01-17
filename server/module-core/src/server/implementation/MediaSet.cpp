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

#include "MediaSet.hpp"

#include <gst/gst.h>
#include <KurentoException.hpp>
#include <MediaPipelineImpl.hpp>
#include <ServerManagerImpl.hpp>

#include <functional>

/* This is included to avoid problems with slots and lamdas */
#include <memory>
#include <type_traits>
#include <sigc++/sigc++.h>

#define GST_CAT_DEFAULT kurento_media_set
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoMediaSet"

namespace kurento
{

static const std::chrono::seconds COLLECTOR_INTERVAL_DEFAULT =
  std::chrono::seconds (
    240);

std::chrono::seconds MediaSet::collectorInterval = COLLECTOR_INTERVAL_DEFAULT;

void
MediaSet::setCollectorInterval (std::chrono::seconds interval)
{
  collectorInterval = interval;
}

std::chrono::seconds
MediaSet::getCollectorInterval()
{
  return collectorInterval;
}


static std::shared_ptr<MediaSet> mediaSet;
static std::recursive_mutex mutex;

std::shared_ptr<MediaSet>
MediaSet::getMediaSet()
{
  std::unique_lock <std::recursive_mutex> lock (mutex);

  if (!mediaSet) {
    mediaSet = std::shared_ptr<MediaSet> (new MediaSet() );
  }

  return mediaSet;
}

void
MediaSet::deleteMediaSet()
{
  std::condition_variable_any cv;
  std::unique_lock <std::recursive_mutex> lock (mutex);

  if (!mediaSet) {
    return;
  }

  auto pipes = mediaSet->getPipelines();

  if (!pipes.empty() ) {
    bool empty = false;

    auto sig = mediaSet->signalEmpty.connect ([&cv, &empty] () {
      std::unique_lock <std::recursive_mutex> lock (mutex);
      empty = true;
      cv.notify_all();
    });

    GST_INFO ("Destroying %zd pipelines that are already alive", pipes.size() );

    for (auto it : pipes) {
      mediaSet->release (it);
    }

    pipes.clear();

    while (!empty) {
      cv.wait (lock);
    }

    sig->disconnect();
  }

  GST_INFO ("Destroying mediaSet");

  mediaSet.reset();
}

void MediaSet::doGarbageCollection ()
{
  std::unique_lock <std::recursive_mutex> lock (recMutex);
  auto sessions = sessionInUse;

  lock.unlock();

  GST_DEBUG ("Running garbage collector");

  for (auto it : sessions) {
    if (it.second) {
      lock.lock();
      sessionInUse[it.first] = false;
      lock.unlock();
    } else {
      GST_WARNING ("Removing inactive session: %s", it.first.c_str() );
      unrefSession (it.first);
    }
  }
}

MediaSet::MediaSet () : workers {}
{
  terminated = false;

  thread = std::thread ( [&] () {
    std::unique_lock <std::recursive_mutex> lock (recMutex);

    while (!terminated && waitCond.wait_for (lock,
           collectorInterval) == std::cv_status::timeout) {

      if (terminated) {
        return;
      }

      try {
        doGarbageCollection();
      } catch (...) {
        GST_ERROR ("Error during garbage collection");
      }
    }

  });
}

MediaSet::~MediaSet ()
{
  std::unique_lock <std::recursive_mutex> lock (recMutex);

  if (objectsMap.size() > 1) {
    GST_WARNING ("Still %zu object/s alive", objectsMap.size());
  }

  terminated = true;

  serverManager.reset();
  waitCond.notify_all();

  lock.unlock();

  if (std::this_thread::get_id() != thread.get_id() ) {
    try {
      thread.join();
    } catch (std::system_error &e) {
      GST_ERROR ("Error while joining the thread: %s", e.what() );
    }
  }

  try {
    if (thread.joinable() ) {
      thread.detach();
    }
  } catch (std::system_error &e) {
    GST_ERROR ("Error while detaching the thread: %s", e.what() );
  }
}

void
MediaSet::post (std::function<void (void) > f)
{
  std::unique_lock <std::recursive_mutex> lock (recMutex);

  if (!terminated) {
    workers.post (f);
  } else {
    lock.unlock();
    f();
  }
}

void
MediaSet::setServerManager (std::shared_ptr <ServerManagerImpl> serverManager)
{
  std::unique_lock <std::recursive_mutex> lock (recMutex);

  if (this->serverManager) {
    GST_WARNING ("ServerManager can only set once, ignoring");
  } else {
    this->serverManager = serverManager;
  }
}

std::shared_ptr<MediaObjectImpl>
MediaSet::ref (MediaObjectImpl *mediaObjectPtr)
{
  std::unique_lock <std::recursive_mutex> lock (recMutex);
  std::shared_ptr<MediaObjectImpl> mediaObject;

  if (mediaObjectPtr == nullptr) {
    throw KurentoException (MEDIA_OBJECT_NOT_FOUND, "Invalid object");
  }

  mediaObject =  std::shared_ptr<MediaObjectImpl> (mediaObjectPtr, [this] (
  MediaObjectImpl * obj) {
    // this will always exist because destructor is waiting for its threads
    this->releasePointer (obj);
  });

  objectsMap[mediaObject->getId()] = std::weak_ptr<MediaObjectImpl> (mediaObject);

  if (mediaObject->getParent() ) {
    std::shared_ptr<MediaObjectImpl> parent = std::dynamic_pointer_cast
        <MediaObjectImpl> (mediaObject->getParent() );

    childrenMap[parent->getId()][mediaObject->getId()] = mediaObject;
  }

  auto parent = mediaObject->getParent();

  if (parent) {
    for (auto session : reverseSessionMap[parent->getId()]) {
      ref (session, mediaObject);
    }
  }

  mediaObject->postConstructor ();

  if (this->serverManager) {
    lock.unlock ();
    serverManager->signalObjectCreated (ObjectCreated (this->serverManager,
                                        std::dynamic_pointer_cast<MediaObject> (mediaObject) ) );
  }

  return mediaObject;
}

void
MediaSet::ref (const std::string &sessionId,
               std::shared_ptr<MediaObjectImpl> mediaObject)
{
  std::unique_lock <std::recursive_mutex> lock (recMutex);

  if (objectsMap.find (mediaObject->getId() ) == objectsMap.end() ) {
    throw KurentoException (MEDIA_OBJECT_NOT_FOUND,
                            "Cannot register media object, it was not created by MediaSet");
  }

  keepAliveSession (sessionId, true);

  if (mediaObject->getParent() ) {
    ref (sessionId,
         std::dynamic_pointer_cast<MediaObjectImpl> (mediaObject->getParent() ) );
  }

  sessionMap[sessionId][mediaObject->getId()] = mediaObject;
  reverseSessionMap[mediaObject->getId()].insert (sessionId);
}

void
MediaSet::ref (const std::string &sessionId,
               const std::string &mediaObjectRef)
{
  std::shared_ptr <MediaObjectImpl> object = getMediaObject (mediaObjectRef);

  ref (sessionId, object);
}

void
MediaSet::keepAliveSession (const std::string &sessionId)
{
  keepAliveSession (sessionId, false);
}

void
MediaSet::keepAliveSession (const std::string &sessionId, bool create)
{
  std::unique_lock <std::recursive_mutex> lock (recMutex);

  auto it = sessionInUse.find (sessionId);

  if (it == sessionInUse.end() ) {
    if (create) {
      sessionInUse[sessionId] = true;
    } else {
      throw KurentoException (INVALID_SESSION, "Invalid session");
    }
  } else {
    it->second = true;
  }
}

void
MediaSet::releaseSession (const std::string &sessionId)
{
  std::unique_lock <std::recursive_mutex> lock (recMutex);

  auto it = sessionMap.find (sessionId);

  if (it != sessionMap.end() ) {
    auto objects = it->second;

    for (auto it2 : objects) {
      release (it2.second);
    }
  }

  sessionMap.erase (sessionId);
  sessionInUse.erase (sessionId);
  eventHandler.erase (sessionId);
  lock.unlock ();

}

void
MediaSet::unrefSession (const std::string &sessionId)
{
  std::unique_lock <std::recursive_mutex> lock (recMutex);

  auto it = sessionMap.find (sessionId);

  if (it != sessionMap.end() ) {
    auto objects = it->second;

    for (auto it2 : objects) {
      unref (sessionId, it2.second);
    }
  }

  sessionMap.erase (sessionId);
  sessionInUse.erase (sessionId);
  eventHandler.erase (sessionId);

  lock.unlock();
}

static void
call_release (std::shared_ptr<MediaObjectImpl> mediaObject)
{
  if (mediaObject) {
    mediaObject->release();
  }
}

bool
MediaSet::isServerManager (std::shared_ptr< MediaObjectImpl > mediaObject)
{
  if (mediaObject && serverManager) {
    return mediaObject.get() == serverManager.get();
  } else {
    return false;
  }
}

void
MediaSet::unref (const std::string &sessionId,
                 std::shared_ptr< MediaObjectImpl > mediaObject)
{
  std::unique_lock <std::recursive_mutex> lock (recMutex);
  bool released = false;

  if (!mediaObject) {
    return;
  }

  auto it = sessionMap.find (sessionId);

  if (it != sessionMap.end() ) {
    auto it2 = it->second.find (mediaObject->getId() );

    if (it2 != it->second.end() ) {
      it->second.erase (it2);
    }
  }

  auto childrenIt = childrenMap.find (mediaObject->getId() );

  if (childrenIt != childrenMap.end() ) {
    auto childMap = childrenIt->second;

    for (auto child : childMap) {
      unref (sessionId, child.second);
    }
  }

  auto it3 = reverseSessionMap.find (mediaObject->getId() );

  if (it3 != reverseSessionMap.end() ) {
    it3->second.erase (sessionId);

    if (it3->second.empty() ) {
      released = true;
    }
  } else {
    released = true;
  }

  if (released && !isServerManager (mediaObject) ) {
    std::shared_ptr<MediaObjectImpl> parent;
    parent = std::dynamic_pointer_cast<MediaObjectImpl> (mediaObject->getParent() );

    if (parent) {
      childrenMap[parent->getId()].erase (mediaObject->getId() );
    }

    childrenMap.erase (mediaObject->getId() );
    reverseSessionMap.erase (mediaObject->getId() );
  }

  auto eventIt = eventHandler.find (sessionId);

  if (eventIt != eventHandler.end() ) {
    eventIt->second.erase (mediaObject->getId() );
  }

  if (released) {
    post (std::bind (call_release, mediaObject) );
  }

  lock.unlock();
}

void
MediaSet::unref (const std::string &sessionId,
                 const std::string &mediaObjectRef)
{
  std::shared_ptr <MediaObjectImpl> object;

  try {
    object = getMediaObject (mediaObjectRef);
  } catch (KurentoException &e) {
    return;
  }

  unref (sessionId, object);
}

static void
async_delete (MediaObjectImpl *mediaObject, std::string id)
{
  GST_DEBUG ("Destroying %s -> %s", mediaObject->getType().c_str(), id.c_str() );
  /* Deletion of mediaObject weak reference */
  delete mediaObject;
}

void MediaSet::releasePointer (MediaObjectImpl *mediaObject)
{
  std::unique_lock <std::recursive_mutex> lock (recMutex);
  std::string id = mediaObject->getId();

  objectsMap.erase (id );

  post (std::bind (async_delete, mediaObject, id) );

  if (this->serverManager && !terminated) {
    serverManager->signalObjectDestroyed (ObjectDestroyed (this->serverManager,
                                          id) );
  }

  lock.unlock();

  checkEmpty();
}

void MediaSet::release (std::shared_ptr< MediaObjectImpl > mediaObject)
{
  std::unique_lock <std::recursive_mutex> lock (recMutex);

  auto it = reverseSessionMap.find (mediaObject->getId() );

  if (it == reverseSessionMap.end() ) {
    /* Already released */
    return;
  }

  auto sessions = it->second;

  for (auto it2 : sessions) {
    unref (it2, mediaObject);
  }

  lock.unlock();
}

void MediaSet::release (const std::string &mediaObjectRef)
{
  try {
    std::shared_ptr< MediaObjectImpl > obj = getMediaObject (mediaObjectRef);

    release (obj);
  } catch (...) {
    /* Do not raise exception if it is already released*/
  }
}

static const std::string NEW_REF = "newref:";

std::shared_ptr< MediaObjectImpl >
MediaSet::getMediaObject (const std::string &mediaObjectRef)
{
  if (mediaObjectRef.size() > NEW_REF.size()
      && mediaObjectRef.substr (0, NEW_REF.size() ) == NEW_REF) {
    throw KurentoException (MEDIA_OBJECT_NOT_FOUND_TRANSACTION_NO_COMMIT,
                            "Object '" + mediaObjectRef +
                            "' not found. Possibly using a transactional " +
                            "object without committing the transaction.");
  }

  std::shared_ptr <MediaObjectImpl> objectLocked;
  std::unique_lock <std::recursive_mutex> lock (recMutex);

  auto it = objectsMap.find (mediaObjectRef);

  if (it == objectsMap.end() ) {
    throw KurentoException (MEDIA_OBJECT_NOT_FOUND,
                            "Object '" + mediaObjectRef + "' not found");
  }

  try {
    objectLocked = it->second.lock();
  } catch (...) {
    throw KurentoException (MEDIA_OBJECT_NOT_FOUND,
                            "Object '" + mediaObjectRef + "' not found");
  }

  if (!objectLocked) {
    throw KurentoException (MEDIA_OBJECT_NOT_FOUND,
                            "Object '" + mediaObjectRef + "' not found");
  }

  auto it2 = reverseSessionMap.find (objectLocked->getId() );

  if (it2 == reverseSessionMap.end() || it2->second.empty() ) {
    if (serverManager && mediaObjectRef == serverManager->getId() ) {
      return serverManager;
    }

    throw KurentoException (MEDIA_OBJECT_NOT_FOUND,
                            "Object '" + mediaObjectRef + "' not found");
  }

  return objectLocked;
}

std::shared_ptr< MediaObjectImpl >
MediaSet::getMediaObject (const std::string &sessionId,
                          const std::string &mediaObjectRef)
{
//   std::unique_lock <std::recursive_mutex> lock (recMutex);
  std::shared_ptr< MediaObjectImpl > obj = getMediaObject (mediaObjectRef);

  ref (sessionId, obj);
  return obj;
}

void
MediaSet::addEventHandler (const std::string &sessionId,
                           const std::string &objectId,
                           const std::string &subscriptionId,
                           std::shared_ptr<EventHandler> handler)
{
  std::unique_lock <std::recursive_mutex> lock (recMutex);

  eventHandler[sessionId][objectId][subscriptionId] = handler;
}

void
MediaSet::removeEventHandler (const std::string &sessionId,
                              const std::string &objectId,
                              const std::string &handlerId)
{
  std::unique_lock <std::recursive_mutex> lock (recMutex);
  auto it = eventHandler.find (sessionId);

  if (it != eventHandler.end() ) {
    auto it2 = eventHandler[sessionId].find (objectId);

    if (it2 != eventHandler[sessionId].end() ) {
      it2->second.erase (handlerId);
    }
  }
}

void
MediaSet::checkEmpty()
{
  std::unique_lock <std::recursive_mutex> lock (recMutex);

  if ( empty() ) {
    signalEmptyLocked.emit();
    lock.unlock();
    signalEmpty.emit();
  }
}

bool
MediaSet::empty()
{
  std::unique_lock <std::recursive_mutex> lock (recMutex);

  if (serverManager) {
    return objectsMap.size () == 1;
  } else {
    return objectsMap.empty();
  }
}

std::vector<std::string>
MediaSet::getSessions ()
{
  std::unique_lock <std::recursive_mutex> lock (recMutex);
  std::vector<std::string> ret (sessionMap.size () );

  for (auto it : sessionMap) {
    ret.push_back (it.first);
  }

  return ret;
}

std::list<std::shared_ptr<MediaObjectImpl>>
    MediaSet::getPipelines (const std::string &sessionId)
{
  std::unique_lock <std::recursive_mutex> lock (recMutex);
  std::list<std::shared_ptr<MediaObjectImpl>> ret;

  auto copy = objectsMap;

  for (auto it : copy) {
    try {
      auto obj = getMediaObject (sessionId, it.first);

      if (std::dynamic_pointer_cast <MediaPipelineImpl> (obj) ) {
        ret.push_back (obj);
      }
    } catch (KurentoException &e) {
    }
  }

  return ret;
}

std::list<std::shared_ptr<MediaObjectImpl>>
    MediaSet::getChildren (std::shared_ptr<MediaObjectImpl> obj)
{
  std::unique_lock <std::recursive_mutex> lock (recMutex);
  std::list<std::shared_ptr<MediaObjectImpl>> ret;

  try {
    for (auto it : childrenMap.at (obj->getId() ) ) {
      ret.push_back (it.second);
    }
  } catch (std::out_of_range &) {
    GST_DEBUG ("Object %s has not children", obj->getId().c_str() );
  }

  return ret;
}

MediaSet::StaticConstructor MediaSet::staticConstructor;

MediaSet::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} // kurento
