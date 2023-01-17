/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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
#ifndef __MEDIA_OBJECT_IMPL_HPP__
#define __MEDIA_OBJECT_IMPL_HPP__

#include "MediaObject.hpp"
#include <EventHandler.hpp>
#include <boost/property_tree/ptree.hpp>
#include <boost/property_tree/json_parser.hpp>
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <mutex>
#include <map>
#include "Tag.hpp"
#include <gst/gst.h>

namespace kurento
{

class MediaPipelineImpl;
class MediaObjectImpl;
class MediaSet;

void Serialize (std::shared_ptr<MediaObjectImpl> &object,
                JsonSerializer &serializer);

class MediaObjectImpl : public virtual MediaObject
{

public:

  MediaObjectImpl (const boost::property_tree::ptree &config);

  MediaObjectImpl (const boost::property_tree::ptree &config,
                   std::shared_ptr <MediaObject> parent);

  virtual ~MediaObjectImpl () {}

  void addTag (const std::string &key, const std::string &value);
  void removeTag (const std::string &key);
  std::string getTag (const std::string &key);
  std::vector<std::shared_ptr<Tag>> getTags ();

  virtual std::shared_ptr<MediaPipeline> getMediaPipeline ();

  virtual std::shared_ptr<MediaObject> getParent ()
  {
    return parent;
  }

  virtual std::vector<std::shared_ptr<MediaObject>> getChildren ();

  virtual std::string getId ();

  virtual std::string getName ();
  virtual void setName (const std::string &name);

  virtual bool getSendTagsInEvents ();
  virtual void setSendTagsInEvents (bool sendTagsInEvents);

  virtual int getCreationTime ();

  virtual void release ()
  {

  }

  /* Next methods are automatically implemented by code generator */
  virtual bool connect (const std::string &eventType,
                        std::shared_ptr<EventHandler> handler);

  sigc::signal<void, Error> signalError;

  virtual void invoke (std::shared_ptr<MediaObjectImpl> obj,
                       const std::string &methodName, const Json::Value &params,
                       Json::Value &response);

  virtual void Serialize (JsonSerializer &serializer);

  template <class T>
  static bool getConfigValue (T *value, const std::string &key,
                              const boost::property_tree::ptree &config)
  {
    boost::property_tree::ptree array;
    try {
      auto child = config.get_child (key);
      array.push_back (std::make_pair ("val", child));
    } catch (boost::property_tree::ptree_bad_path &) {
      // This case is expected, the requested key doesn't exist in config
      GST_DEBUG ("Key '%s' not found in config", key.c_str ());
      return false;
    }

    std::stringstream ss;
    boost::property_tree::write_json (ss, array, false);
    GST_DEBUG ("Key '%s' found in config, value: '%s'", key.c_str (),
        ss.str ().c_str ());

    Json::Reader reader;
    Json::Value val;
    reader.parse (ss.str (), val);

    kurento::JsonSerializer serializer (false);
    serializer.JsonValue = val;

    try {
      serializer.Serialize ("val", *value);
    } catch (const std::exception &e) {
      GST_ERROR (
          "Error deserializing '%s' from config: %s", key.c_str (), e.what ());
      return false;
    }

    return true;
  }

protected:

  template <class T>
  bool getConfigValue (T *value, const std::string &key)
  {
    return getConfigValue <T> (value, key, config);
  }

  template <class T, class C>
  bool getConfigValue (T *value, const std::string &key)
  {
    return getConfigValue <T> (value, "modules."
        + dynamic_cast <C *> (this)->getModule() + "."
        + dynamic_cast <C *> (this)->getType() + "." + key, config);
  }

  template <class T>
  bool getConfigValue (T *value, const std::string &key, const T &defaultValue)
  {
    if (!getConfigValue <T> (value, key, config)) {
      *value = defaultValue;
      return false;
    }
    return true;
  }

  template <class T, class C>
  bool getConfigValue (T *value, const std::string &key, const T &defaultValue)
  {
    if (!getConfigValue <T> (value, "modules."
        + dynamic_cast <C *> (this)->getModule() + "."
        + dynamic_cast <C *> (this)->getType() + "." + key, config)) {
      *value = defaultValue;
      return false;
    }
    return true;
  }

  template <class T>
  void sigcSignalEmit (const sigc::signal<void, T> &sigcSignal,
      const RaiseBase &event)
  {
    std::unique_lock<std::recursive_mutex> sigcLock (sigcMutex);
    try {
      sigcSignal.emit (dynamic_cast <const T&> (event));
    } catch (const std::bad_cast &e) {
      // dynamic_cast()
      GST_ERROR ("BUG emitting signal: %s", e.what ());
    }
  }

  std::recursive_mutex sigcMutex;

  /*
   * This method is intented to perform initialization actions that require
   * a call to shared_from_this ()
   */
  virtual void postConstructor ();

  const boost::property_tree::ptree &config;

private:

  std::string initialId;
  std::string id;
  std::string name;
  std::recursive_mutex mutex;
  std::shared_ptr<MediaObject> parent;
  int64_t creationTime;

  std::string createId();

  std::map<std::string, std::string> tagsMap;
  bool sendTagsInEvents;

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;

  friend MediaSet;
};

} /* kurento */

#endif /*  __MEDIA_OBJECT_IMPL_HPP__ */
