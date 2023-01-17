/*
 * Copyright 2022 Kurento
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <config.h>
#include <gst/gst.h>

#include "gstgstreamerexample.h"
#include "config.h"

static gboolean
plugin_init (GstPlugin *plugin)
{
  if (!gst_gstreamer_example_plugin_init (plugin)) {
    GST_WARNING_OBJECT (plugin, "GStreamer plugin failed registering");
    return FALSE;
  }

  return TRUE;
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    gstreamerexample,
    "Filter documentation",
    plugin_init,
    VERSION,
    GST_LICENSE_UNKNOWN,
    "GStreamerExample",
    "https://www.example.com/")
