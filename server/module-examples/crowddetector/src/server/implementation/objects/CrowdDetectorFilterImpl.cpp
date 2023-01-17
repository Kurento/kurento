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
#include "RegionOfInterest.hpp"
#include "RelativePoint.hpp"
#include "RegionOfInterestConfig.hpp"
#include <CrowdDetectorFilterImplFactory.hpp>
#include "CrowdDetectorFilterImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include "SignalHandler.hpp"

#define GST_CAT_DEFAULT kurento_crowd_detector_filter_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoCrowdDetectorFilterImpl"

#define ROIS_PARAM "rois"
#define PROCESSING_WIDTH "processing-width"

namespace kurento
{
namespace module
{
namespace crowddetector
{

static GstStructure *
get_structure_from_roi (std::shared_ptr<RegionOfInterest> roi)
{
  GstStructure *roiStructure, *configRoiSt;
  std::shared_ptr<RegionOfInterestConfig> config;
  int pointCount = 0;

  roiStructure = gst_structure_new_empty (roi->getId().c_str() );

  if (roiStructure == NULL) {
    throw KurentoException (MEDIA_OBJECT_ILLEGAL_PARAM_ERROR,
                            "Invalid roi name");
  }

  for (std::shared_ptr<RelativePoint> point : roi->getPoints() ) {
    GstStructure *pointSt;
    std::string name = "point" + std::to_string (pointCount ++);

    pointSt = gst_structure_new (name.c_str(),
                                 "x", G_TYPE_FLOAT, point->getX(),
                                 "y", G_TYPE_FLOAT, point->getY(),
                                 NULL);

    gst_structure_set (roiStructure,
                       name.c_str(), GST_TYPE_STRUCTURE, pointSt,
                       NULL);

    gst_structure_free (pointSt);
  }

  config = roi->getRegionOfInterestConfig();
  configRoiSt = gst_structure_new ("config",
                                   "id", G_TYPE_STRING, roi->getId().c_str(),
                                   "occupancy_level_min", G_TYPE_INT, config->getOccupancyLevelMin(),
                                   "occupancy_level_med", G_TYPE_INT, config->getOccupancyLevelMed(),
                                   "occupancy_level_max", G_TYPE_INT, config->getOccupancyLevelMax(),
                                   "occupancy_num_frames_to_event", G_TYPE_INT,
                                   config->getOccupancyNumFramesToEvent(),
                                   "fluidity_level_min", G_TYPE_INT, config->getFluidityLevelMin(),
                                   "fluidity_level_med", G_TYPE_INT, config->getFluidityLevelMed(),
                                   "fluidity_level_max", G_TYPE_INT, config->getFluidityLevelMax(),
                                   "fluidity_num_frames_to_event", G_TYPE_INT,
                                   config->getFluidityNumFramesToEvent(),
                                   "send_optical_flow_event", G_TYPE_BOOLEAN, config->getSendOpticalFlowEvent(),
                                   "optical_flow_num_frames_to_event", G_TYPE_INT,
                                   config->getOpticalFlowNumFramesToEvent(),
                                   "optical_flow_num_frames_to_reset", G_TYPE_INT,
                                   config->getOpticalFlowNumFramesToReset(),
                                   "optical_flow_angle_offset", G_TYPE_INT, config->getOpticalFlowAngleOffset(),
                                   NULL);
  gst_structure_set (roiStructure,
                     "config", GST_TYPE_STRUCTURE, configRoiSt,
                     NULL);

  gst_structure_free (configRoiSt);

  return roiStructure;
}

void CrowdDetectorFilterImpl::busMessage (GstMessage *message)
{
  const GstStructure *st;
  gchar *roiID;
  const gchar *type;
  std::string roiIDStr, typeStr;

  if (GST_MESSAGE_SRC (message) != GST_OBJECT (crowdDetector) ||
      GST_MESSAGE_TYPE (message) != GST_MESSAGE_ELEMENT) {
    return;
  }

  st = gst_message_get_structure (message);
  type = gst_structure_get_name (st);

  if (!gst_structure_get (st, "roi", G_TYPE_STRING , &roiID, NULL) ) {
    GST_WARNING ("The message does not contain the roi ID");
    return;
  }

  roiIDStr = roiID;
  typeStr = type;

  g_free (roiID);

  if (typeStr == "fluidity-event") {

    double fluidity_percentage;
    int fluidity_level;

    if (! (gst_structure_get (st, "fluidity_percentage", G_TYPE_DOUBLE,
                              &fluidity_percentage, NULL) ) ) {
      GST_WARNING ("The message does not contain the fluidity percentage");
      return;
    }

    if (! (gst_structure_get (st, "fluidity_level", G_TYPE_INT,
                              &fluidity_level, NULL) ) ) {
      GST_WARNING ("The message does not contain the fluidity level");
      return;
    }

    try {
      CrowdDetectorFluidity event (shared_from_this(),
                                   CrowdDetectorFluidity::getName(),
                                   fluidity_percentage, fluidity_level,
                                   roiIDStr);
      signalCrowdDetectorFluidity (event);
    } catch (std::bad_weak_ptr &e) {
    }
  } else if (typeStr == "occupancy-event") {

    double occupancy_percentage;
    int occupancy_level;

    if (! (gst_structure_get (st, "occupancy_percentage", G_TYPE_DOUBLE,
                              &occupancy_percentage, NULL) ) ) {
      GST_WARNING ("The message does not contain the occupancy percentage");
      return;
    }

    if (! (gst_structure_get (st, "occupancy_level", G_TYPE_INT,
                              &occupancy_level, NULL) ) ) {
      GST_WARNING ("The message does not contain the occupancy level");
      return;
    }

    try {
      CrowdDetectorOccupancy event (shared_from_this(),
                                    CrowdDetectorOccupancy::getName(),
                                    occupancy_percentage, occupancy_level,
                                    roiIDStr);
      signalCrowdDetectorOccupancy (event);
    } catch (std::bad_weak_ptr &e) {
    }
  } else if (typeStr == "direction-event") {

    double direction_angle;

    if (! (gst_structure_get (st, "direction_angle", G_TYPE_DOUBLE,
                              &direction_angle, NULL) ) ) {
      GST_WARNING ("The message does not contain the direction angle");
      return;
    }

    try {
      CrowdDetectorDirection event ( shared_from_this(),
                                     CrowdDetectorDirection::getName(),
                                     direction_angle, roiIDStr);
      signalCrowdDetectorDirection (event);
    } catch (std::bad_weak_ptr &e) {
    }
  } else {
    GST_WARNING ("The message does not have the correct name");
  }
}

void CrowdDetectorFilterImpl::postConstructor ()
{
  GstBus *bus;
  std::shared_ptr<MediaPipelineImpl> pipe;

  FilterImpl::postConstructor ();

  pipe = std::dynamic_pointer_cast<MediaPipelineImpl> (getMediaPipeline() );

  bus = gst_pipeline_get_bus (GST_PIPELINE (pipe->getPipeline() ) );

  bus_handler_id = register_signal_handler (G_OBJECT (bus),
                   "message",
                   std::function <void (GstElement *, GstMessage *) >
                   (std::bind (&CrowdDetectorFilterImpl::busMessage, this,
                               std::placeholders::_2) ),
                   std::dynamic_pointer_cast<CrowdDetectorFilterImpl>
                   (shared_from_this() ) );

  g_object_unref (bus);
}

CrowdDetectorFilterImpl::CrowdDetectorFilterImpl (const
    boost::property_tree::ptree &config,
    std::shared_ptr<MediaPipeline> mediaPipeline,
    const std::vector<std::shared_ptr<RegionOfInterest>> &rois)  : FilterImpl (
        config, std::dynamic_pointer_cast<MediaPipelineImpl>
        (mediaPipeline) )
{
  GstStructure *roisStructure;

  g_object_set (element, "filter-factory", "crowddetector", NULL);

  g_object_get (G_OBJECT (element), "filter", &crowdDetector, NULL);

  if (crowdDetector == NULL) {
    throw KurentoException (MEDIA_OBJECT_NOT_AVAILABLE,
                            "Media Object not available");
  }

  roisStructure = gst_structure_new_empty  ("Rois");

  for (auto roi : rois) {
    GstStructure *roiStructureAux = get_structure_from_roi (roi);

    gst_structure_set (roisStructure,
                       roi->getId().c_str(), GST_TYPE_STRUCTURE,
                       roiStructureAux,
                       NULL);

    gst_structure_free (roiStructureAux);
  }

  g_object_set (G_OBJECT (crowdDetector), ROIS_PARAM, roisStructure, NULL);
  gst_structure_free (roisStructure);

  bus_handler_id = 0;
  // There is no need to reference crowddetector because its life cycle is the same as the filter life cycle
  g_object_unref (crowdDetector);
}

CrowdDetectorFilterImpl::~CrowdDetectorFilterImpl()
{
  std::shared_ptr<MediaPipelineImpl> pipe;

  if (bus_handler_id > 0) {
    pipe = std::dynamic_pointer_cast<MediaPipelineImpl> (getMediaPipeline() );
    GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipe->getPipeline() ) );
    unregister_signal_handler (bus, bus_handler_id);
    g_object_unref (bus);
  }
}

int CrowdDetectorFilterImpl::getProcessingWidth ()
{
  int ret;

  g_object_get (G_OBJECT (crowdDetector), PROCESSING_WIDTH, &ret, NULL);

  return ret;
}

void CrowdDetectorFilterImpl::setProcessingWidth (int processingWidth)
{
  g_object_set (G_OBJECT (crowdDetector), PROCESSING_WIDTH, processingWidth,
                NULL);
}

MediaObjectImpl *
CrowdDetectorFilterImplFactory::createObject (const boost::property_tree::ptree
    &config, std::shared_ptr<MediaPipeline> mediaPipeline,
    const std::vector<std::shared_ptr<RegionOfInterest>> &rois) const
{
  return new CrowdDetectorFilterImpl (config, mediaPipeline, rois);
}

CrowdDetectorFilterImpl::StaticConstructor
CrowdDetectorFilterImpl::staticConstructor;

CrowdDetectorFilterImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* crowddetector */
} /* module */
} /* kurento */
