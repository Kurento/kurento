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
#include "MediaPipeline.hpp"
#include "FilterType.hpp"
#include <GStreamerFilterImplFactory.hpp>
#include "GStreamerFilterImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>
#include <commons/kms-core-enumtypes.h>
#include <algorithm>

#define GST_CAT_DEFAULT kurento_gstreamer_filter_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoGStreamerFilterImpl"

namespace kurento
{

GStreamerFilterImpl::GStreamerFilterImpl (const boost::property_tree::ptree
    &conf, std::shared_ptr<MediaPipeline>
    mediaPipeline, const std::string &command,
    std::shared_ptr<FilterType> filterType) : FilterImpl (conf,
          std::dynamic_pointer_cast<MediaObjectImpl> ( mediaPipeline) )
{
  GstElement *filter, *filter_check;
  GError *error = NULL;

  this->cmd = command;

  GST_DEBUG ("Command %s", command.c_str() );

  switch (filterType->getValue() ) {
  case FilterType::VIDEO:
    g_object_set (element, "type", 2, NULL);
    break;

  case FilterType::AUDIO:
    g_object_set (element, "type", 1, NULL);
    break;

  case FilterType::AUTODETECT:
    g_object_set (element, "type", 0, NULL);
    break;

  default:
    break;
  }

  filter = gst_parse_launch (command.c_str(), &error);

  if (filter == NULL || error != NULL) {
    std::string error_str = "GStreamer filter cannot be created";

    if (filter) {
      g_object_unref (filter);
    }

    if (error != NULL) {
      if (error->message != NULL) {
        error_str += ": " + std::string (error->message);
      }

      g_error_free (error);
    }

    throw KurentoException (MARSHALL_ERROR, error_str);
  } else if (GST_IS_BIN (filter) ) {
    g_object_unref (filter);

    throw KurentoException (MARSHALL_ERROR,
                            "Given command is not valid, just one element can be created");
  }

  g_object_set (element, "filter", filter, NULL);

  g_object_get (element, "filter", &filter_check, NULL);

  if (filter_check != filter) {
    g_object_unref (filter);
    g_object_unref (filter_check);

    throw KurentoException (MARSHALL_ERROR,
                            "Given command is not valid, pad templates does not match");
  }

  g_object_unref (filter);
  g_object_unref (filter_check);
}

MediaObjectImpl *
GStreamerFilterImplFactory::createObject (const boost::property_tree::ptree
    &conf, std::shared_ptr<MediaPipeline>
    mediaPipeline, const std::string &command,
    std::shared_ptr<FilterType> filterType) const
{
  return new GStreamerFilterImpl (conf, mediaPipeline, command, filterType);
}

std::string GStreamerFilterImpl::getCommand ()
{
  return this->cmd;
}

GStreamerFilterImpl::StaticConstructor GStreamerFilterImpl::staticConstructor;

GStreamerFilterImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
