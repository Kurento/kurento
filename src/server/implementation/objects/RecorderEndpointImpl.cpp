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
#include "MediaType.hpp"
#include "MediaPipeline.hpp"
#include "MediaProfileSpecType.hpp"
#include <RecorderEndpointImplFactory.hpp>
#include "RecorderEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>
#include <commons/kmsrecordingprofile.h>

#include "StatsType.hpp"
#include "EndpointStats.hpp"
#include <commons/kmsutils.h>
#include <commons/kmsstats.h>

#include <SignalHandler.hpp>
#include <functional>

#define GST_CAT_DEFAULT kurento_recorder_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoRecorderEndpointImpl"

#define FACTORY_NAME "recorderendpoint"

#define TIMEOUT 4 /* seconds */

namespace kurento
{

typedef enum {
  KMS_URI_END_POINT_STATE_STOP,
  KMS_URI_END_POINT_STATE_START,
  KMS_URI_END_POINT_STATE_PAUSE
} KmsUriEndPointState;

bool RecorderEndpointImpl::support_ksr;

static bool
check_support_for_ksr ()
{
  GstPlugin *plugin = nullptr;
  bool supported;

  plugin = gst_plugin_load_by_name ("kmsrecorder");

  supported = plugin != nullptr;

  g_clear_object (&plugin);

  return supported;
}

RecorderEndpointImpl::RecorderEndpointImpl (const boost::property_tree::ptree
    &conf,
    std::shared_ptr<MediaPipeline> mediaPipeline, const std::string &uri,
    std::shared_ptr<MediaProfileSpecType> mediaProfile,
    bool stopOnEndOfStream) : UriEndpointImpl (conf,
          std::dynamic_pointer_cast<MediaObjectImpl> (mediaPipeline), FACTORY_NAME, uri)
{
  g_object_set (G_OBJECT (getGstreamerElement() ), "accept-eos",
                stopOnEndOfStream, NULL);

  switch (mediaProfile->getValue() ) {
  case MediaProfileSpecType::WEBM:
    g_object_set ( G_OBJECT (element), "profile", KMS_RECORDING_PROFILE_WEBM, NULL);
    GST_INFO ("Set WEBM profile");
    break;

  case MediaProfileSpecType::MP4:
    g_object_set ( G_OBJECT (element), "profile", KMS_RECORDING_PROFILE_MP4, NULL);
    GST_INFO ("Set MP4 profile");
    break;

  case MediaProfileSpecType::MKV:
    g_object_set ( G_OBJECT (element), "profile", KMS_RECORDING_PROFILE_MKV, NULL);
    GST_INFO ("Set MKV profile");
    break;

  case MediaProfileSpecType::WEBM_VIDEO_ONLY:
    g_object_set ( G_OBJECT (element), "profile",
                   KMS_RECORDING_PROFILE_WEBM_VIDEO_ONLY, NULL);
    GST_INFO ("Set WEBM VIDEO ONLY profile");
    break;

  case MediaProfileSpecType::WEBM_AUDIO_ONLY:
    g_object_set ( G_OBJECT (element), "profile",
                   KMS_RECORDING_PROFILE_WEBM_AUDIO_ONLY, NULL);
    GST_INFO ("Set WEBM AUDIO ONLY profile");
    break;

  case MediaProfileSpecType::MKV_VIDEO_ONLY:
    g_object_set ( G_OBJECT (element), "profile",
                   KMS_RECORDING_PROFILE_MKV_VIDEO_ONLY, NULL);
    GST_INFO ("Set MKV VIDEO ONLY profile");
    break;

  case MediaProfileSpecType::MKV_AUDIO_ONLY:
    g_object_set ( G_OBJECT (element), "profile",
                   KMS_RECORDING_PROFILE_MKV_AUDIO_ONLY, NULL);
    GST_INFO ("Set MKV AUDIO ONLY profile");
    break;

  case MediaProfileSpecType::MP4_VIDEO_ONLY:
    g_object_set ( G_OBJECT (element), "profile",
                   KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY, NULL);
    GST_INFO ("Set MP4 VIDEO ONLY profile");
    break;

  case MediaProfileSpecType::MP4_AUDIO_ONLY:
    g_object_set ( G_OBJECT (element), "profile",
                   KMS_RECORDING_PROFILE_MP4_AUDIO_ONLY, NULL);
    GST_INFO ("Set MP4 AUDIO ONLY profile");
    break;

  case MediaProfileSpecType::JPEG_VIDEO_ONLY:
    g_object_set ( G_OBJECT (element), "profile",
                   KMS_RECORDING_PROFILE_JPEG_VIDEO_ONLY, NULL);
    GST_INFO ("Set JPEG profile");
    break;

  case MediaProfileSpecType::KURENTO_SPLIT_RECORDER:
    if (!RecorderEndpointImpl::support_ksr) {
      throw KurentoException (MEDIA_OBJECT_ILLEGAL_PARAM_ERROR,
                              "Kurento Split Recorder not supported");
    }

    g_object_set ( G_OBJECT (element), "profile", KMS_RECORDING_PROFILE_KSR, NULL);
    GST_INFO ("Set KSR profile");
    break;
  }
}

void RecorderEndpointImpl::postConstructor()
{
  UriEndpointImpl::postConstructor();

  handlerOnStateChanged = register_signal_handler (G_OBJECT (element),
                          "state-changed",
                          std::function <void (GstElement *, gint) >
                          (std::bind (&RecorderEndpointImpl::onStateChanged, this,
                                      std::placeholders::_2) ),
                          std::dynamic_pointer_cast<RecorderEndpointImpl>
                          (shared_from_this() ) );
}

void
RecorderEndpointImpl::onStateChanged (gint newState)
{
  switch (newState) {
  case KMS_URI_END_POINT_STATE_STOP: {
    GST_DEBUG_OBJECT (element, "State changed to Stopped");
    try {
      Stopped event (shared_from_this (), Stopped::getName ());
      sigcSignalEmit(signalStopped, event);
    } catch (const std::bad_weak_ptr &e) {
      // shared_from_this()
      GST_ERROR ("BUG creating %s: %s", Stopped::getName ().c_str (),
          e.what ());
    }
    break;
  }

  case KMS_URI_END_POINT_STATE_START: {
    GST_DEBUG_OBJECT (element, "State changed to Recording");
    try {
      Recording event (shared_from_this(), Recording::getName () );
      sigcSignalEmit(signalRecording, event);
    } catch (const std::bad_weak_ptr &e) {
      // shared_from_this()
      GST_ERROR ("BUG creating %s: %s", Recording::getName ().c_str (),
          e.what ());
    }
    break;
  }

  case KMS_URI_END_POINT_STATE_PAUSE: {
    GST_DEBUG_OBJECT (element, "State changed to Paused");
    try {
      Paused event (shared_from_this(), Paused::getName () );
      sigcSignalEmit(signalPaused, event);
    } catch (const std::bad_weak_ptr &e) {
      // shared_from_this()
      GST_ERROR ("BUG creating %s: %s", Paused::getName ().c_str (),
          e.what ());
    }
    break;
  }
  }

  std::unique_lock<std::mutex> lck (mtx);

  GST_TRACE_OBJECT (element, "State changed to %d", newState);

  state = newState;
  cv.notify_one();
}

void RecorderEndpointImpl::waitForStateChange (gint expectedState)
{
  std::unique_lock<std::mutex> lck (mtx);

  if (!cv.wait_for (lck, std::chrono::seconds (TIMEOUT), [&] {return expectedState == state;}) ) {
    GST_ERROR_OBJECT (element, "STATE did not changed to %d in %d seconds",
                      expectedState, TIMEOUT);
  }
}

void
RecorderEndpointImpl::release ()
{
  gint state = -1;

  g_object_get (getGstreamerElement(), "state", &state, NULL);

  if (state == 0 /* stop */) {
    goto end;
  }

  stopAndWait();

end:
  UriEndpointImpl::release();
}

RecorderEndpointImpl::~RecorderEndpointImpl()
{
  gint state = -1;

  if (handlerOnStateChanged > 0) {
    unregister_signal_handler (element, handlerOnStateChanged);
  }

  g_object_get (getGstreamerElement(), "state", &state, NULL);

  if (state != 0 /* stop */) {
    GST_ERROR ("Recorder should be stopped when reaching this point");
  }
}

void RecorderEndpointImpl::record ()
{
  start();
}

void RecorderEndpointImpl::stopAndWait ()
{
  stop();
  waitForStateChange (KMS_URI_END_POINT_STATE_STOP);
}

static void
setDeprecatedProperties (std::shared_ptr<EndpointStats> eStats)
{
  std::vector<std::shared_ptr<MediaLatencyStat>> inStats =
        eStats->getE2ELatency();

  for (auto &inStat : inStats) {
    if (inStat->getName() == "sink_audio_default") {
      eStats->setAudioE2ELatency(inStat->getAvg());
    } else if (inStat->getName() == "sink_video_default") {
      eStats->setVideoE2ELatency(inStat->getAvg());
    }
  }
}

void
RecorderEndpointImpl::collectEndpointStats (std::map
    <std::string, std::shared_ptr<Stats>>
    &statsReport, std::string id, const GstStructure *stats,
    double timestamp, int64_t timestampMillis)
{
  std::shared_ptr<Stats> endpointStats;
  GstStructure *e2e_stats;

  std::vector<std::shared_ptr<MediaLatencyStat>> inputStats;
  std::vector<std::shared_ptr<MediaLatencyStat>> e2eStats;

  if (gst_structure_get (stats, "e2e-latencies", GST_TYPE_STRUCTURE,
                         &e2e_stats, NULL) ) {
    collectLatencyStats (e2eStats, e2e_stats);
    gst_structure_free (e2e_stats);
  }

  endpointStats = std::make_shared <EndpointStats> (id,
                  std::make_shared <StatsType> (StatsType::endpoint), timestamp,
                  timestampMillis, 0.0, 0.0, inputStats, 0.0, 0.0, e2eStats);

  setDeprecatedProperties (std::dynamic_pointer_cast <EndpointStats>
                           (endpointStats) );

  statsReport[id] = endpointStats;
}

void
RecorderEndpointImpl::fillStatsReport (std::map
                                       <std::string, std::shared_ptr<Stats>>
                                       &report, const GstStructure *stats,
                                       double timestamp, int64_t timestampMillis)
{
  const GstStructure *e_stats;

  e_stats = kms_utils_get_structure_by_name (stats, KMS_MEDIA_ELEMENT_FIELD);

  if (e_stats != nullptr) {
    collectEndpointStats (report, getId (), e_stats, timestamp, timestampMillis);
  }

  UriEndpointImpl::fillStatsReport (report, stats, timestamp, timestampMillis);
}

MediaObjectImpl *
RecorderEndpointImplFactory::createObject (const boost::property_tree::ptree
    &conf, std::shared_ptr<MediaPipeline>
    mediaPipeline, const std::string &uri,
    std::shared_ptr<MediaProfileSpecType> mediaProfile,
    bool stopOnEndOfStream) const
{
  return new RecorderEndpointImpl (conf, mediaPipeline, uri, mediaProfile,
                                   stopOnEndOfStream);
}

RecorderEndpointImpl::StaticConstructor RecorderEndpointImpl::staticConstructor;

RecorderEndpointImpl::StaticConstructor::StaticConstructor()
{
  RecorderEndpointImpl::support_ksr = check_support_for_ksr();

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
