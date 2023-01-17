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
#include <gst/gst.h>
#include "MediaPipelineImpl.hpp"
#include "MediaObjectImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>
#include <UUIDGenerator.hpp>
#include <MediaSet.hpp>

#define GST_CAT_DEFAULT kurento_media_object_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoMediaObjectImpl"

namespace kurento
{

MediaObjectImpl::MediaObjectImpl (const boost::property_tree::ptree &config) :
  MediaObjectImpl (config, std::shared_ptr<MediaObject> () )
{
}

MediaObjectImpl::MediaObjectImpl (const boost::property_tree::ptree &config,
                                  std::shared_ptr< MediaObject > parent) : config (config)
{
  this->parent = parent;

  creationTime = time(nullptr);
  initialId = createId();
  this->sendTagsInEvents = false;
}

std::shared_ptr<MediaPipeline>
MediaObjectImpl::getMediaPipeline ()
{
  if (parent) {
    return std::dynamic_pointer_cast<MediaObjectImpl> (parent)->getMediaPipeline();
  } else {
    return std::dynamic_pointer_cast<MediaPipeline> (shared_from_this() );
  }
}

std::string
MediaObjectImpl::createId()
{
  std::string uuid = generateUUID();

  if (parent) {
    std::shared_ptr<MediaObjectImpl> parent;

    parent = std::dynamic_pointer_cast<MediaObjectImpl> (
        MediaObjectImpl::getParent() );
    return parent->getId() + "/" +
           uuid;
  } else {
    return uuid;
  }
}

std::string
MediaObjectImpl::getName()
{
  std::unique_lock<std::recursive_mutex> lck (mutex);

  if (name.empty () ) {
    name = getId ();
  }

  return name;
}

std::string
MediaObjectImpl::getId()
{
  std::unique_lock<std::recursive_mutex> lck (mutex);

  if (id.empty () ) {
    id = this->initialId + "_" + this->getModule() + "." + this->getType ();
  }

  return id;
}

void
MediaObjectImpl::setName (const std::string &name)
{
  std::unique_lock<std::recursive_mutex> lck (mutex);

  this->name = name;
}

std::vector<std::shared_ptr<MediaObject>> MediaObjectImpl::getChildren ()
{
  std::vector<std::shared_ptr<MediaObject>> children;

  for (auto it : MediaSet::getMediaSet ()->getChildren (std::dynamic_pointer_cast
       <MediaObjectImpl> (shared_from_this() ) ) ) {
    children.push_back (it);
  }

  return children;
}

bool MediaObjectImpl::getSendTagsInEvents ()
{
  return this->sendTagsInEvents;
}

void MediaObjectImpl::setSendTagsInEvents (bool sendTagsInEvents)
{
  this->sendTagsInEvents = sendTagsInEvents;
}

void MediaObjectImpl::addTag (const std::string &key, const std::string &value)
{
  tagsMap [key] = value;
  GST_DEBUG ("Tag added");
}

void MediaObjectImpl::removeTag (const std::string &key)
{
  auto it = tagsMap.find (key);

  if (it != tagsMap.end() ) {
    tagsMap.erase (it);
    GST_DEBUG ("Tag deleted");
    return;
  }

  GST_DEBUG ("Tag not found");
}

std::string MediaObjectImpl::getTag (const std::string &key)
{
  auto it = tagsMap.find (key);

  if (it != tagsMap.end() ) {
    return it->second;
  }

  throw KurentoException (MEDIA_OBJECT_TAG_KEY_NOT_FOUND,
                          "Tag key not found");
}

void MediaObjectImpl::postConstructor()
{
}

std::vector<std::shared_ptr<Tag>> MediaObjectImpl::getTags ()
{
  std::vector<std::shared_ptr<Tag>> ret;

  for (auto it : tagsMap ) {
    std::shared_ptr <Tag> tag (new Tag (it.first, it.second) );
    ret.push_back (tag);
  }

  return ret;
}

int  MediaObjectImpl::getCreationTime ()
{
  return (int) creationTime;
}

MediaObjectImpl::StaticConstructor MediaObjectImpl::staticConstructor;

MediaObjectImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
