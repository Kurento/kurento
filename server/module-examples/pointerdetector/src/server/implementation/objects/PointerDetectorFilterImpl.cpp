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
#include "MediaPipelineImpl.hpp"
#include "WindowParam.hpp"
#include "PointerDetectorWindowMediaParam.hpp"
#include <PointerDetectorFilterImplFactory.hpp>
#include "PointerDetectorFilterImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>
#include "SignalHandler.hpp"

#define GST_CAT_DEFAULT kurento_pointer_detector_filter_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoPointerDetectorFilterImpl"

#define WINDOWS_LAYOUT "windows-layout"
#define CALIBRATION_AREA "calibration-area"
#define CALIBRATE_COLOR "calibrate-color"

namespace kurento
{
namespace module
{
namespace pointerdetector
{
static GstStructure *
get_structure_from_window (std::shared_ptr<PointerDetectorWindowMediaParam>
                           window)
{
  GstStructure *buttonsLayoutAux;

  buttonsLayoutAux = gst_structure_new (
                       window->getId().c_str(),
                       "upRightCornerX", G_TYPE_INT, window->getUpperRightX(),
                       "upRightCornerY", G_TYPE_INT, window->getUpperRightY(),
                       "width", G_TYPE_INT, window->getWidth(),
                       "height", G_TYPE_INT, window->getHeight(),
                       "id", G_TYPE_STRING, window->getId().c_str(),
                       NULL);

  if (window->isSetImage() ) {
    gst_structure_set (buttonsLayoutAux, "inactive_uri",
                       G_TYPE_STRING, window->getImage().c_str(), NULL);
  }

  if (window->isSetImageTransparency() ) {
    gst_structure_set (buttonsLayoutAux, "transparency",
                       G_TYPE_DOUBLE, double (window->getImageTransparency() ), NULL);
  }

  if (window->isSetActiveImage() ) {
    gst_structure_set (buttonsLayoutAux, "active_uri",
                       G_TYPE_STRING, window->getActiveImage().c_str(), NULL);
  }

  return buttonsLayoutAux;
}

void PointerDetectorFilterImpl::busMessage (GstMessage *message)
{
  const GstStructure *st;
  gchar *windowID;
  const gchar *type;
  std::string windowIDStr, typeStr;

  if (GST_MESSAGE_SRC (message) != GST_OBJECT (pointerDetector) ||
      GST_MESSAGE_TYPE (message) != GST_MESSAGE_ELEMENT) {
    return;
  }

  st = gst_message_get_structure (message);
  type = gst_structure_get_name (st);

  if ( (g_strcmp0 (type, "window-out") != 0) &&
       (g_strcmp0 (type, "window-in") != 0) ) {
    GST_WARNING ("The message does not have the correct name");
    return;
  }

  if (!gst_structure_get (st, "window", G_TYPE_STRING , &windowID, NULL) ) {
    GST_WARNING ("The message does not contain the window ID");
    return;
  }

  windowIDStr = windowID;
  typeStr = type;

  g_free (windowID);

  if (typeStr == "window-in") {
    try {
      WindowIn event (shared_from_this(), WindowIn::getName(), windowIDStr );

      signalWindowIn (event);
    } catch (std::bad_weak_ptr &e) {
    }
  } else if (typeStr == "window-out") {
    try {
      WindowOut event (shared_from_this(), WindowOut::getName(), windowIDStr );

      signalWindowOut (event);
    } catch (std::bad_weak_ptr &e) {
    }
  }
}

void PointerDetectorFilterImpl::postConstructor ()
{
  GstBus *bus;
  std::shared_ptr<MediaPipelineImpl> pipe;

  FilterImpl::postConstructor ();

  pipe = std::dynamic_pointer_cast<MediaPipelineImpl> (getMediaPipeline() );

  bus = gst_pipeline_get_bus (GST_PIPELINE (pipe->getPipeline() ) );

  bus_handler_id = register_signal_handler (G_OBJECT (bus),
                   "message",
                   std::function <void (GstElement *, GstMessage *) >
                   (std::bind (&PointerDetectorFilterImpl::busMessage, this,
                               std::placeholders::_2) ),
                   std::dynamic_pointer_cast<PointerDetectorFilterImpl>
                   (shared_from_this() ) );

  g_object_unref (bus);
}

PointerDetectorFilterImpl::PointerDetectorFilterImpl (const
    boost::property_tree::ptree &config,
    std::shared_ptr<MediaPipeline> mediaPipeline,
    std::shared_ptr<WindowParam> calibrationRegion,
    const std::vector<std::shared_ptr<PointerDetectorWindowMediaParam>> &windows)  :
  FilterImpl (config, std::dynamic_pointer_cast<MediaPipelineImpl>
              (mediaPipeline) )
{
  GstStructure *calibrationArea;
  GstStructure *buttonsLayout;

  g_object_set (element, "filter-factory", "pointerdetector", NULL);

  g_object_get (G_OBJECT (element), "filter", &pointerDetector, NULL);

  if (pointerDetector == NULL) {
    throw KurentoException (MEDIA_OBJECT_NOT_AVAILABLE,
                            "Media Object not available");
  }

  calibrationArea = gst_structure_new (
                      "calibration_area",
                      "x", G_TYPE_INT, calibrationRegion->getTopRightCornerX(),
                      "y", G_TYPE_INT, calibrationRegion->getTopRightCornerY(),
                      "width", G_TYPE_INT, calibrationRegion->getWidth(),
                      "height", G_TYPE_INT, calibrationRegion->getHeight(),
                      NULL);
  g_object_set (G_OBJECT (pointerDetector), CALIBRATION_AREA,
                calibrationArea, NULL);
  gst_structure_free (calibrationArea);

  buttonsLayout = gst_structure_new_empty  ("windowsLayout");

  for (auto window : windows) {
    GstStructure *buttonsLayoutAux = get_structure_from_window (window);

    gst_structure_set (buttonsLayout,
                       window->getId().c_str(), GST_TYPE_STRUCTURE,
                       buttonsLayoutAux,
                       NULL);

    gst_structure_free (buttonsLayoutAux);
  }

  g_object_set (G_OBJECT (pointerDetector), WINDOWS_LAYOUT, buttonsLayout,
                NULL);
  gst_structure_free (buttonsLayout);

  bus_handler_id = 0;
  // There is no need to reference pointerdetector because its life cycle is the same as the filter life cycle
  g_object_unref (pointerDetector);
}

PointerDetectorFilterImpl::~PointerDetectorFilterImpl()
{
  std::shared_ptr<MediaPipelineImpl> pipe;

  if (bus_handler_id > 0) {
    pipe = std::dynamic_pointer_cast<MediaPipelineImpl> (getMediaPipeline() );
    GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipe->getPipeline() ) );
    unregister_signal_handler (bus, bus_handler_id);
    g_object_unref (bus);
  }
}


void PointerDetectorFilterImpl::addWindow (
  std::shared_ptr<PointerDetectorWindowMediaParam> window)
{
  GstStructure *buttonsLayout, *buttonsLayoutAux;

  buttonsLayoutAux = get_structure_from_window (window);

  /* The function obtains the actual window list */
  g_object_get (G_OBJECT (pointerDetector), WINDOWS_LAYOUT, &buttonsLayout,
                NULL);
  gst_structure_set (buttonsLayout,
                     window->getId().c_str(), GST_TYPE_STRUCTURE,
                     buttonsLayoutAux, NULL);

  g_object_set (G_OBJECT (pointerDetector), WINDOWS_LAYOUT, buttonsLayout, NULL);

  gst_structure_free (buttonsLayout);
  gst_structure_free (buttonsLayoutAux);
}

void PointerDetectorFilterImpl::clearWindows ()
{
  GstStructure *buttonsLayout;

  buttonsLayout = gst_structure_new_empty  ("buttonsLayout");
  g_object_set (G_OBJECT (pointerDetector), WINDOWS_LAYOUT, buttonsLayout,
                NULL);
  gst_structure_free (buttonsLayout);
}

void PointerDetectorFilterImpl::trackColorFromCalibrationRegion ()
{
  g_signal_emit_by_name (pointerDetector, CALIBRATE_COLOR, NULL);
}

void PointerDetectorFilterImpl::removeWindow (const std::string &windowId)
{
  GstStructure *buttonsLayout;
  gint len;

  /* The function obtains the actual window list */
  g_object_get (G_OBJECT (pointerDetector), WINDOWS_LAYOUT, &buttonsLayout,
                NULL);
  len = gst_structure_n_fields (buttonsLayout);

  if (len == 0) {
    GST_WARNING ("There are no windows in the layout");
    return;
  }

  for (int i = 0; i < len; i++) {
    const gchar *name;
    name = gst_structure_nth_field_name (buttonsLayout, i);

    if (windowId == name) {
      /* this window will be removed */
      gst_structure_remove_field (buttonsLayout, name);
    }
  }

  /* Set the buttons layout list without the window with id = id */
  g_object_set (G_OBJECT (pointerDetector), WINDOWS_LAYOUT, buttonsLayout, NULL);

  gst_structure_free (buttonsLayout);
}

MediaObjectImpl *
PointerDetectorFilterImplFactory::createObject (const
    boost::property_tree::ptree &config,
    std::shared_ptr<MediaPipeline> mediaPipeline,
    std::shared_ptr<WindowParam> calibrationRegion,
    const std::vector<std::shared_ptr<PointerDetectorWindowMediaParam>> &windows)
const
{
  return new PointerDetectorFilterImpl (config, mediaPipeline, calibrationRegion,
                                        windows);
}

PointerDetectorFilterImpl::StaticConstructor
PointerDetectorFilterImpl::staticConstructor;

PointerDetectorFilterImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* pointerdetector */
} /* module */
} /* kurento */
