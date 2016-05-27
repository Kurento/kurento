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
#include <PlateDetectorFilterImplFactory.hpp>
#include "PlateDetectorFilterImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include "SignalHandler.hpp"

#define GST_CAT_DEFAULT kurento_plate_detector_filter_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoPlateDetectorFilterImpl"

#define PLATE_WIDTH_PERCENTAGE "plate-width-percentage"

namespace kurento
{
namespace module
{
namespace platedetector
{

void PlateDetectorFilterImpl::busMessage (GstMessage *message)
{
  const GstStructure *st;
  gchar *plateNumber;
  const gchar *type;
  std::string typeStr, plateNumberStr;

  if (GST_MESSAGE_SRC (message) != GST_OBJECT (plateDetector) ||
      GST_MESSAGE_TYPE (message) != GST_MESSAGE_ELEMENT) {
    return;
  }

  st = gst_message_get_structure (message);
  type = gst_structure_get_name (st);

  if (g_strcmp0 (type, "plate-detected") != 0) {
    GST_WARNING ("The message does not have the correct type");
    return;
  }

  if (! (gst_structure_get (st, "plate", G_TYPE_STRING , &plateNumber,
                            NULL) ) ) {
    GST_WARNING ("The message does not contain the plate number");
    return;
  }

  plateNumberStr = plateNumber;
  typeStr = type;

  try {
    PlateDetected event (shared_from_this(), typeStr, plateNumberStr);
    signalPlateDetected (event);
  } catch (std::bad_weak_ptr &e) {
  }

  g_free (plateNumber);
}

void PlateDetectorFilterImpl::postConstructor ()
{
  GstBus *bus;
  std::shared_ptr<MediaPipelineImpl> pipe;

  FilterImpl::postConstructor ();

  pipe = std::dynamic_pointer_cast<MediaPipelineImpl> (getMediaPipeline() );

  bus = gst_pipeline_get_bus (GST_PIPELINE (pipe->getPipeline() ) );

  bus_handler_id = register_signal_handler (G_OBJECT (bus),
                   "message",
                   std::function <void (GstElement *, GstMessage *) >
                   (std::bind (&PlateDetectorFilterImpl::busMessage, this,
                               std::placeholders::_2) ),
                   std::dynamic_pointer_cast<PlateDetectorFilterImpl>
                   (shared_from_this() ) );

  g_object_unref (bus);
}

PlateDetectorFilterImpl::PlateDetectorFilterImpl (const
    boost::property_tree::ptree &config,
    std::shared_ptr<MediaPipeline> mediaPipeline)  : FilterImpl (config,
          std::dynamic_pointer_cast<MediaPipelineImpl>
          (mediaPipeline) )
{
  g_object_set (element, "filter-factory", "platedetector", NULL);

  g_object_get (G_OBJECT (element), "filter", &plateDetector, NULL);

  bus_handler_id = 0;

  // There is no need to reference platedetector because its life cycle is the same as the filter life cycle
  g_object_unref (plateDetector);
}

PlateDetectorFilterImpl::~PlateDetectorFilterImpl()
{
  std::shared_ptr<MediaPipelineImpl> pipe;

  if (bus_handler_id > 0) {
    pipe = std::dynamic_pointer_cast<MediaPipelineImpl> (getMediaPipeline() );
    GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipe->getPipeline() ) );
    unregister_signal_handler (bus, bus_handler_id);
    g_object_unref (bus);
  }
}

void PlateDetectorFilterImpl::setPlateWidthPercentage (float
    plateWidthPercentage)
{
  g_object_set (G_OBJECT (plateDetector), PLATE_WIDTH_PERCENTAGE,
                plateWidthPercentage,
                NULL);
}

MediaObjectImpl *
PlateDetectorFilterImplFactory::createObject (const boost::property_tree::ptree
    &config, std::shared_ptr<MediaPipeline> mediaPipeline) const
{
  return new PlateDetectorFilterImpl (config, mediaPipeline);
}

PlateDetectorFilterImpl::StaticConstructor
PlateDetectorFilterImpl::staticConstructor;

PlateDetectorFilterImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* platedetector */
} /* module */
} /* kurento */
