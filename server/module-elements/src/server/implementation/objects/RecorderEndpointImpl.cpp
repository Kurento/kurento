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
#include "GapsFixMethod.hpp"
#include "RecorderVideoCodec.hpp"
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

#define PARAM_GAPS_FIX "gapsFix"
#define PROP_GAPS_FIX "gaps-fix"

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

static KmsRecordingProfile
getRecordingProfile (MediaProfileSpecType::type type)
{
  switch (type) {
  case MediaProfileSpecType::WEBM:
    return KMS_RECORDING_PROFILE_WEBM;
  case MediaProfileSpecType::MP4:
    return KMS_RECORDING_PROFILE_MP4;
  case MediaProfileSpecType::MKV:
    return KMS_RECORDING_PROFILE_MKV;
  case MediaProfileSpecType::WEBM_VIDEO_ONLY:
    return KMS_RECORDING_PROFILE_WEBM_VIDEO_ONLY;
  case MediaProfileSpecType::WEBM_AUDIO_ONLY:
    return KMS_RECORDING_PROFILE_WEBM_AUDIO_ONLY;
  case MediaProfileSpecType::MKV_VIDEO_ONLY:
    return KMS_RECORDING_PROFILE_MKV_VIDEO_ONLY;
  case MediaProfileSpecType::MKV_AUDIO_ONLY:
    return KMS_RECORDING_PROFILE_MKV_AUDIO_ONLY;
  case MediaProfileSpecType::MP4_VIDEO_ONLY:
    return KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY;
  case MediaProfileSpecType::MP4_AUDIO_ONLY:
    return KMS_RECORDING_PROFILE_MP4_AUDIO_ONLY;
  case MediaProfileSpecType::JPEG_VIDEO_ONLY:
    return KMS_RECORDING_PROFILE_JPEG_VIDEO_ONLY;
  case MediaProfileSpecType::KURENTO_SPLIT_RECORDER:
    return KMS_RECORDING_PROFILE_KSR;
  case MediaProfileSpecType::FLV:
    return KMS_RECORDING_PROFILE_FLV;
  }

  return KMS_RECORDING_PROFILE_NONE;
}

/* Maps the API enum to the internal one; DEFAULT maps to NONE, which means
 * "default codec of the container". */
static KmsRecordingVideoCodec
getRecordingVideoCodec (RecorderVideoCodec::type type, const char **name)
{
  switch (type) {
  case RecorderVideoCodec::DEFAULT:
    *name = "DEFAULT";
    return KMS_RECORDING_VIDEO_CODEC_NONE;
  case RecorderVideoCodec::H264:
    *name = "H264";
    return KMS_RECORDING_VIDEO_CODEC_H264;
  case RecorderVideoCodec::H265:
    *name = "H265";
    return KMS_RECORDING_VIDEO_CODEC_H265;
  case RecorderVideoCodec::VP8:
    *name = "VP8";
    return KMS_RECORDING_VIDEO_CODEC_VP8;
  case RecorderVideoCodec::VP9:
    *name = "VP9";
    return KMS_RECORDING_VIDEO_CODEC_VP9;
  case RecorderVideoCodec::AV1:
    *name = "AV1";
    return KMS_RECORDING_VIDEO_CODEC_AV1;
  }

  throw KurentoException (MEDIA_OBJECT_ILLEGAL_PARAM_ERROR,
                          "Unknown video codec");
}

/* Checks the container x codec combination. It must be called before creating
 * the element, so that no GStreamer element is created for a rejected request. */
static void
checkVideoCodec (std::shared_ptr<MediaProfileSpecType> mediaProfile,
                 std::shared_ptr<RecorderVideoCodec> videoCodec)
{
  const char *codecName;
  KmsRecordingVideoCodec codec;

  if (!videoCodec) {
    return;
  }

  codec = getRecordingVideoCodec (videoCodec->getValue (), &codecName);

  if (!kms_recording_profile_supports_video_codec (
          getRecordingProfile (mediaProfile->getValue () ), codec) ) {
    throw KurentoException (MEDIA_OBJECT_ILLEGAL_PARAM_ERROR,
                            std::string ("Video codec ") + codecName +
                            " is not supported by media profile " +
                            mediaProfile->getString () +
                            " (see the RecorderEndpoint documentation for the"
                            " valid container and codec combinations)");
  }
}

RecorderEndpointImpl::RecorderEndpointImpl (const boost::property_tree::ptree
    &conf,
    std::shared_ptr<MediaPipeline> mediaPipeline, const std::string &uri,
    std::shared_ptr<MediaProfileSpecType> mediaProfile,
    bool stopOnEndOfStream,
    std::shared_ptr<RecorderVideoCodec> videoCodec) : UriEndpointImpl (conf,
          std::dynamic_pointer_cast<MediaObjectImpl> (mediaPipeline), FACTORY_NAME, uri)
{
  g_object_set (G_OBJECT (getGstreamerElement() ), "accept-eos",
                stopOnEndOfStream, NULL);

  if (videoCodec && videoCodec->getValue () != RecorderVideoCodec::DEFAULT) {
    const char *codecName;
    KmsRecordingVideoCodec codec =
      getRecordingVideoCodec (videoCodec->getValue (), &codecName);

    /* IMPORTANT: "video-codec" must be set BEFORE "profile"; once the profile
     * is set, the element ignores any later change of the video codec. */
    g_object_set (G_OBJECT (element), "video-codec", codec, NULL);
    GST_INFO ("Set video codec: %s", codecName);
  }

  if (mediaProfile->getValue () == MediaProfileSpecType::KURENTO_SPLIT_RECORDER
      && !RecorderEndpointImpl::support_ksr) {
    throw KurentoException (MEDIA_OBJECT_ILLEGAL_PARAM_ERROR,
                            "Kurento Split Recorder not supported");
  }

  g_object_set (G_OBJECT (element), "profile",
                getRecordingProfile (mediaProfile->getValue () ), NULL);
  GST_INFO ("Set %s profile", mediaProfile->getString ().c_str () );

  GapsFixMethod gapsFix;
  if (getConfigValue<GapsFixMethod, RecorderEndpoint> (
          &gapsFix, PARAM_GAPS_FIX)) {
    GST_INFO ("Set RecorderEndpoint gaps fix mode: %s",
        gapsFix.getString ().c_str ());
    g_object_set (getGstreamerElement (),
        PROP_GAPS_FIX, gapsFix.getValue (), NULL);
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
    GST_ERROR_OBJECT (element, "Recorder state change to %d timed out after %d seconds",
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

void
RecorderEndpointImpl::collectEndpointStats (std::map
    <std::string, std::shared_ptr<Stats>>
    &statsReport, std::string id, const GstStructure *stats,
    int64_t timestampMillis)
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
                  std::make_shared <StatsType> (StatsType::endpoint),
                  timestampMillis, inputStats, e2eStats);

  statsReport[id] = endpointStats;
}

void
RecorderEndpointImpl::fillStatsReport (std::map
                                       <std::string, std::shared_ptr<Stats>>
                                       &report, const GstStructure *stats,
                                       int64_t timestampMillis)
{
  const GstStructure *e_stats;

  e_stats = kms_utils_get_structure_by_name (stats, KMS_MEDIA_ELEMENT_FIELD);

  if (e_stats != nullptr) {
    collectEndpointStats (report, getId (), e_stats, timestampMillis);
  }

  UriEndpointImpl::fillStatsReport (report, stats, timestampMillis);
}

MediaObjectImpl *
RecorderEndpointImplFactory::createObject (const boost::property_tree::ptree
    &conf, std::shared_ptr<MediaPipeline>
    mediaPipeline, const std::string &uri,
    std::shared_ptr<MediaProfileSpecType> mediaProfile,
    bool stopOnEndOfStream,
    std::shared_ptr<RecorderVideoCodec> videoCodec) const
{
  checkVideoCodec (mediaProfile, videoCodec);

  return new RecorderEndpointImpl (conf, mediaPipeline, uri, mediaProfile,
                                   stopOnEndOfStream, videoCodec);
}

RecorderEndpointImpl::StaticConstructor RecorderEndpointImpl::staticConstructor;

RecorderEndpointImpl::StaticConstructor::StaticConstructor()
{
  RecorderEndpointImpl::support_ksr = check_support_for_ksr();

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
