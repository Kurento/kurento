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

#define BOOST_TEST_STATIC_LINK
#define BOOST_TEST_PROTECTED_VIRTUAL
#define BOOST_TEST_IGNORE_NON_ZERO_CHILD_CODE // Don't fail if child process fails

#include <boost/test/included/unit_test.hpp>
#include <MediaPipelineImpl.hpp>
#include <objects/RecorderEndpointImpl.hpp>
#include <objects/PlayerEndpointImpl.hpp>
#include <KurentoException.hpp>
#include <MediaType.hpp>
#include <MediaTranscodingState.hpp>
#include <mutex>
#include <condition_variable>
#include <ModuleManager.hpp>
#include <MediaSet.hpp>
#include <cstdio> // popen()
#include <cstdlib> // system()
#include <ftw.h>
#include <iostream>
#include <unistd.h>

#define DIR_TEMPLATE "/tmp/recoder_test_XXXXXX"

using namespace kurento;
using namespace boost::unit_test;

boost::property_tree::ptree config;
std::string mediaPipelineId;
ModuleManager moduleManager;

#define EXPECTED_LEN 0.2
static const int TIMEOUT = 5; /* seconds */

struct GF {
  GF();
  ~GF();
};

BOOST_GLOBAL_FIXTURE (GF);

GF::GF()
{
  boost::property_tree::ptree ac, audioCodecs, vc, videoCodecs;
  gst_init(nullptr, nullptr);

  moduleManager.loadModulesFromDirectories ("../../src/server:../../..");

  mediaPipelineId = moduleManager.getFactory ("MediaPipeline")->createObject (
                      config, "",
                      Json::Value() )->getId();
}

GF::~GF()
{
  MediaSet::deleteMediaSet();
}

std::string exec (const std::string &str)
{
  std::shared_ptr<FILE> pipe (::popen (str.c_str(), "r"), pclose);

  if (!pipe) {
    return "ERROR";
  }

  char buffer[128];
  std::string result = "";

  while (!feof (pipe.get() ) ) {
    if (fgets(buffer, 128, pipe.get()) != nullptr) {
      result += buffer;
    }
  }

  return result;
}

static std::shared_ptr <RecorderEndpointImpl>
createRecorderEndpoint ()
{
  std::shared_ptr <kurento::MediaObjectImpl> recorderEndpoint;
  Json::Value constructorParams;
  gchar tmp_file_template[] = DIR_TEMPLATE;
  gchar *tmp_file = mkdtemp (tmp_file_template);

  constructorParams ["mediaPipeline"] = mediaPipelineId;
  constructorParams ["uri"] = "file://" + std::string (tmp_file) +
                              "/recording.webm";

  recorderEndpoint = moduleManager.getFactory ("RecorderEndpoint")->createObject (
                       config, "",
                       constructorParams );

  return std::dynamic_pointer_cast <RecorderEndpointImpl> (recorderEndpoint);
}

static void
releaseRecorderEndpoint (std::shared_ptr<RecorderEndpointImpl> &ep)
{
  std::string id = ep->getId();

  ep.reset();
  MediaSet::getMediaSet ()->release (id);
}

static std::shared_ptr<MediaElementImpl> createTestSrc() {
  std::shared_ptr <MediaElementImpl> src = std::dynamic_pointer_cast
      <MediaElementImpl> (MediaSet::getMediaSet()->ref (new  MediaElementImpl (
                            boost::property_tree::ptree(),
                            MediaSet::getMediaSet()->getMediaObject (mediaPipelineId),
                            "dummysrc") ) );

  g_object_set (src->getGstreamerElement(), "audio", TRUE, "video", TRUE, NULL);

  return std::dynamic_pointer_cast <MediaElementImpl> (src);
}

static void
releaseTestSrc (std::shared_ptr<MediaElementImpl> &ep)
{
  std::string id = ep->getId();

  ep.reset();
  MediaSet::getMediaSet ()->release (id);
}

std::condition_variable cv;
std::mutex mtx;
std::atomic<bool> transited (false);

static void
set_state (std::shared_ptr <RecorderEndpointImpl> recorder,
           std::shared_ptr<UriEndpointState> next)
{
  std::shared_ptr<UriEndpointState> current = recorder->getState ();
  std::unique_lock<std::mutex> lck (mtx);

  BOOST_TEST_MESSAGE ("Setting recorder to state: " << next->getString() );

  if (current->getValue () == next->getValue () ) {
    BOOST_TEST_MESSAGE ("Recorder is already in state: " <<  current->getString() );
    return;
  }

  switch (next->getValue() ) {
  case UriEndpointState::STOP:
    recorder->stop();
    break;

  case UriEndpointState::PAUSE:
    recorder->pause();
    break;

  case UriEndpointState::START:
    recorder->record();
    break;
  }

  /* Lets wait for asynchronous change of state */
  if (!cv.wait_for (lck, std::chrono::seconds (TIMEOUT), [] () {
  return transited.load();
  }) ) {
    BOOST_ERROR ("Timeout changing to state " << next->getString() );
  }

  transited.store (false);
}

static void
recorder_state_changes ()
{
  std::atomic<int> recording_changes (0);
  std::atomic<int> pause_changes (0);
  std::atomic<int> stop_changes (0);
  std::atomic<int> start_changes (0);

  std::shared_ptr <RecorderEndpointImpl> recorder = createRecorderEndpoint ();
  std::shared_ptr <MediaElementImpl> src = createTestSrc();

  std::shared_ptr<UriEndpointState> start (new UriEndpointState (
        UriEndpointState::START) );
  std::shared_ptr<UriEndpointState> stop (new UriEndpointState (
      UriEndpointState::STOP) );
  std::shared_ptr<UriEndpointState> pause (new UriEndpointState (
        UriEndpointState::PAUSE) );

  recorder->signalUriEndpointStateChanged.connect ([&] (UriEndpointStateChanged
  event) {
    BOOST_TEST_MESSAGE ("Recorder transited to state: " <<
                        event.getState()->getString() );

    switch (event.getState()->getValue() ) {
    case UriEndpointState::STOP:
      stop_changes++;
      break;

    case UriEndpointState::PAUSE:
      pause_changes++;
      break;

    case UriEndpointState::START:
      start_changes++;
      break;
    }

    transited.store (true);
    cv.notify_one();
  });

  recorder->signalRecording.connect ([&] (Recording event) {
    recording_changes++;
  });

  src->connect (recorder);

  set_state (recorder, pause);
  set_state (recorder, pause); /* No transition done */
  set_state (recorder, start);
  set_state (recorder, pause);
  set_state (recorder, start);
  set_state (recorder, pause);
  set_state (recorder, pause);
  set_state (recorder, start);
  set_state (recorder, pause);

  set_state (recorder, start);
  set_state (recorder, pause);
  set_state (recorder, start);
  set_state (recorder, start); /* No transition done */

  set_state (recorder, pause);
  set_state (recorder, start);
  set_state (recorder, start); /* No transition done */

  g_usleep (100000);

  set_state (recorder, pause);
  set_state (recorder, start);
  set_state (recorder, start); /* No transition done */

  g_usleep (100000);

  recorder->stopAndWait();

  releaseTestSrc (src);

  std::string uri = recorder->getUri();
  std::cout << "Recorder URI: " << uri << std::endl;

  releaseRecorderEndpoint (recorder);

  std::string cmd = "ffprobe";
  std::string arg = " -version";
  int rc = ::system ( (cmd + arg).c_str() );

  if (rc != 0) {
    cmd = "avprobe";
    rc = ::system ( (cmd + arg).c_str() );

    if (rc != 0) {
      BOOST_ERROR ("ffprobe or avprobe is not installed");
    }
  }

  arg = " -show_format " + uri + " 2>/dev/null | sed -n 's/duration=//p'";

  std::cout << "Exec: " << cmd + arg << std::endl;
  std::string duration = exec (cmd + arg);

  float dur = atof (duration.c_str() );
  BOOST_WARN_GE (dur, EXPECTED_LEN * 0.8);
  BOOST_WARN_LE (dur, EXPECTED_LEN * 1.2);

  arg = " -show_streams " + uri + " 2>/dev/null | sed -n 's/codec_name=//p'";

  std::cout << "Exec: " << cmd + arg << std::endl;
  std::string codecs = exec (cmd + arg);

  BOOST_REQUIRE (codecs.find ("vp8") != std::string::npos);
  BOOST_REQUIRE (codecs.find ("opus") != std::string::npos);

  std::cout << "recording_changes: " << recording_changes << std::endl;
  std::cout << "start_changes: " << start_changes << std::endl;
  std::cout << "stop_changes: " << stop_changes << std::endl;
  std::cout << "pause_changes: " << pause_changes << std::endl;

  BOOST_CHECK_EQUAL (recording_changes, 7);
  BOOST_CHECK_EQUAL (stop_changes, 1);
  BOOST_CHECK_EQUAL (pause_changes, 7);

  BOOST_CHECK_EQUAL (recording_changes, start_changes);

  uri = uri.substr (sizeof ("file://") - 1);

  if (remove (uri.c_str() ) != 0) {
    BOOST_ERROR ("Error deleting tmp file");
  }
}

/* Temporary directory used by the video codec tests; removed by
 * removeCodecTestDir() at the end of each test case. */
static std::string codecTestDir;

static const std::string &
getCodecTestDir ()
{
  if (codecTestDir.empty () ) {
    gchar tmp_file_template[] = DIR_TEMPLATE;
    gchar *tmp_dir = mkdtemp (tmp_file_template);

    BOOST_REQUIRE_MESSAGE (tmp_dir != nullptr, "Cannot create a temporary dir");
    codecTestDir = tmp_dir;
  }

  return codecTestDir;
}

static int
removeEntry (const char *path, const struct stat *, int, struct FTW *)
{
  return ::remove (path);
}

static void
removeCodecTestDir ()
{
  if (!codecTestDir.empty () ) {
    ::nftw (codecTestDir.c_str (), removeEntry, 16, FTW_DEPTH | FTW_PHYS);
    codecTestDir.clear ();
  }
}

/* Creates a RecorderEndpoint with the given profile and (optional) video codec;
 * an empty videoCodec means "do not send the parameter". */
static std::shared_ptr <RecorderEndpointImpl>
createRecorderEndpointWithCodec (const std::string &profile,
                                 const std::string &videoCodec, const std::string &extension)
{
  Json::Value constructorParams;

  constructorParams ["mediaPipeline"] = mediaPipelineId;
  constructorParams ["uri"] = "file://" + getCodecTestDir () +
                              "/recording." + extension;
  constructorParams ["mediaProfile"] = profile;

  if (!videoCodec.empty () ) {
    constructorParams ["videoCodec"] = videoCodec;
  }

  return std::dynamic_pointer_cast <RecorderEndpointImpl> (
           moduleManager.getFactory ("RecorderEndpoint")->createObject (
             config, "", constructorParams) );
}

static void
check_codec_accepted (const std::string &profile, const std::string &videoCodec,
                      const std::string &extension)
{
  BOOST_TEST_MESSAGE ("Valid: " << profile << " + " << videoCodec);

  std::shared_ptr <RecorderEndpointImpl> recorder;

  BOOST_CHECK_NO_THROW (recorder = createRecorderEndpointWithCodec (profile,
                                   videoCodec, extension) );
  BOOST_CHECK (recorder != nullptr);

  if (recorder) {
    releaseRecorderEndpoint (recorder);
  }
}

static void
check_codec_rejected (const std::string &profile, const std::string &videoCodec,
                      const std::string &extension)
{
  BOOST_TEST_MESSAGE ("Invalid: " << profile << " + " << videoCodec);

  bool thrown = false;

  try {
    std::shared_ptr <RecorderEndpointImpl> recorder =
      createRecorderEndpointWithCodec (profile, videoCodec, extension);
  } catch (const KurentoException &e) {
    thrown = true;
    BOOST_CHECK_EQUAL (e.getCode (), MEDIA_OBJECT_ILLEGAL_PARAM_ERROR);
    BOOST_CHECK (std::string (e.what () ).find (videoCodec) != std::string::npos);
    BOOST_TEST_MESSAGE ("  error: " << e.what () );
  }

  BOOST_CHECK_MESSAGE (thrown, "Expected KurentoException for " << profile << " + "
                       << videoCodec);
}

/* Counts GStreamer/GLib CRITICAL and WARNING messages while it is installed */
static std::atomic<int> criticalCount (0);

static void
countCriticals (const gchar *log_domain, GLogLevelFlags log_level,
                const gchar *message, gpointer user_data)
{
  if (log_level & G_LOG_LEVEL_CRITICAL) {
    criticalCount++;
  }

  g_log_default_handler (log_domain, log_level, message, nullptr);
}

static void
video_codec_matrix ()
{
  criticalCount = 0;
  GLogFunc previous_handler = g_log_set_default_handler (countCriticals, nullptr);

  /* No videoCodec: the default of each container, as before */
  check_codec_accepted ("WEBM", "", "webm");
  check_codec_accepted ("MP4", "", "mp4");
  check_codec_accepted ("MKV", "", "mkv");

  /* DEFAULT is the same as not sending the parameter, valid for any profile */
  check_codec_accepted ("MP4", "DEFAULT", "mp4");
  check_codec_accepted ("WEBM", "DEFAULT", "webm");
  check_codec_accepted ("FLV", "DEFAULT", "flv");
  check_codec_accepted ("JPEG_VIDEO_ONLY", "DEFAULT", "jpg");

  /* Valid combinations */
  check_codec_accepted ("MP4", "H264", "mp4");
  check_codec_accepted ("MP4", "H265", "mp4");
  check_codec_accepted ("MP4", "VP9", "mp4");
  check_codec_accepted ("MP4", "AV1", "mp4");
  check_codec_accepted ("MP4_VIDEO_ONLY", "H265", "mp4");
  check_codec_accepted ("MKV", "H264", "mkv");
  check_codec_accepted ("MKV", "H265", "mkv");
  check_codec_accepted ("MKV", "VP8", "mkv");
  check_codec_accepted ("MKV", "VP9", "mkv");
  check_codec_accepted ("MKV", "AV1", "mkv");
  check_codec_accepted ("MKV_VIDEO_ONLY", "H265", "mkv");
  check_codec_accepted ("WEBM", "VP8", "webm");
  check_codec_accepted ("WEBM", "VP9", "webm");
  check_codec_accepted ("WEBM", "AV1", "webm");
  check_codec_accepted ("WEBM_VIDEO_ONLY", "VP9", "webm");

  /* Audio-only profiles ignore the codec */
  check_codec_accepted ("MP4_AUDIO_ONLY", "H265", "mp4");
  check_codec_accepted ("MKV_AUDIO_ONLY", "H265", "mkv");
  check_codec_accepted ("WEBM_AUDIO_ONLY", "H265", "webm");

  /* Invalid combinations */
  check_codec_rejected ("WEBM", "H264", "webm");
  check_codec_rejected ("WEBM", "H265", "webm");
  check_codec_rejected ("WEBM_VIDEO_ONLY", "H265", "webm");
  check_codec_rejected ("MP4", "VP8", "mp4");
  check_codec_rejected ("MP4_VIDEO_ONLY", "VP8", "mp4");
  check_codec_rejected ("FLV", "H264", "flv");
  check_codec_rejected ("FLV", "H265", "flv");
  check_codec_rejected ("JPEG_VIDEO_ONLY", "H264", "jpg");

  /* A rejected request must not create any GStreamer element, so there must
   * be no CRITICAL messages (e.g. unbalanced bus signal watches) */
  BOOST_CHECK_EQUAL (criticalCount.load (), 0);
  g_log_set_default_handler (previous_handler, nullptr);

  removeCodecTestDir ();
}

static void
video_codec_unknown_value ()
{
  /* A value outside of the enum is rejected when deserializing the parameter */
  BOOST_CHECK_THROW (createRecorderEndpointWithCodec ("MP4", "H266", "mp4"),
                     KurentoException);
  removeCodecTestDir ();
}

static bool
programAvailable (const std::string &program)
{
  return ::system ((program + " -version > /dev/null 2>&1").c_str () ) == 0;
}

static std::string
probe (const std::string &uri, const std::string &args)
{
  return exec ("ffprobe -v error " + args + " " + uri + " 2>/dev/null");
}

/* Generates a short H265 MP4 file with a test pattern, inside this process.
 * Returns false if a needed GStreamer element is not available. */
static bool
generateH265Source (const std::string &file)
{
  static const char *elements[] = {"videotestsrc", "x265enc", "h265parse",
                                   "mp4mux", "filesink"};

  for (const char *name : elements) {
    GstElementFactory *factory = gst_element_factory_find (name);

    if (factory == nullptr) {
      BOOST_WARN_MESSAGE (false, "GStreamer element '" << name << "' is not "
                          "available: cannot generate the H265 source");
      std::cerr << "SKIPPED: GStreamer element '" << name
                << "' is not available: cannot generate the H265 source"
                << std::endl;
      return false;
    }

    gst_object_unref (factory);
  }

  std::string description = "videotestsrc num-buffers=75 "
                            "! video/x-raw,width=320,height=240,framerate=25/1 "
                            /* No B-frames: PlayerEndpoint with useEncodedMedia drops
                             * buffers with non-increasing PTS; B-frames are covered by
                             * other tests */
                            "! x265enc option-string=bframes=0 "
                            "! h265parse ! mp4mux ! filesink location=" + file;
  GError *err = nullptr;
  GstElement *pipeline = gst_parse_launch (description.c_str (), &err);

  if (pipeline == nullptr) {
    BOOST_ERROR ("Cannot create the source pipeline: " << err->message);
    g_clear_error (&err);
    return false;
  }

  GstBus *bus = gst_element_get_bus (pipeline);
  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  GstMessage *msg = gst_bus_timed_pop_filtered (bus, 30 * GST_SECOND,
                    (GstMessageType) (GST_MESSAGE_EOS | GST_MESSAGE_ERROR) );
  bool ok = msg != nullptr && GST_MESSAGE_TYPE (msg) == GST_MESSAGE_EOS;

  BOOST_CHECK_MESSAGE (ok, "Generating the H265 source file failed or timed out");

  if (msg != nullptr) {
    gst_message_unref (msg);
  }

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (bus);
  gst_object_unref (pipeline);

  return ok;
}

/* Records an H265 file played by a PlayerEndpoint (encoded media as-is) into
 * the given container, with videoCodec=H265, and checks that the result is
 * H265 and that no transcoding took place. */
static void
record_h265_from_player (const std::string &profile,
                         const std::string &extension, const std::string &expected_tag)
{
  std::atomic<int> transcoding_events (0);

  BOOST_TEST_MESSAGE ("Recording H265 from a PlayerEndpoint into " << profile);

  if (!programAvailable ("ffprobe") ) {
    BOOST_WARN_MESSAGE (false, "SKIPPED: ffprobe is not available");
    std::cerr << "SKIPPED: ffprobe is not available" << std::endl;
    return;
  }

  std::string src_file = getCodecTestDir () + "/source_h265.mp4";

  if (!generateH265Source (src_file) ) {
    removeCodecTestDir ();
    return;
  }

  std::shared_ptr <RecorderEndpointImpl> recorder =
    createRecorderEndpointWithCodec (profile, "H265", extension);

  Json::Value playerParams;
  playerParams ["mediaPipeline"] = mediaPipelineId;
  playerParams ["uri"] = "file://" + src_file;
  playerParams ["useEncodedMedia"] = true;

  std::shared_ptr <PlayerEndpointImpl> player =
    std::dynamic_pointer_cast <PlayerEndpointImpl> (
      moduleManager.getFactory ("PlayerEndpoint")->createObject (config, "",
          playerParams) );

  auto on_transcoding = [&] (MediaTranscodingStateChanged event) {
    BOOST_TEST_MESSAGE ("Transcoding state: " << event.getState ()->getString ()
                        << " (" << event.getBinName () << ")");

    if (event.getState ()->getValue () == MediaTranscodingState::TRANSCODING) {
      transcoding_events++;
    }
  };

  sigc::connection conn_player =
    player->signalMediaTranscodingStateChanged.connect (on_transcoding);
  sigc::connection conn_recorder =
    recorder->signalMediaTranscodingStateChanged.connect (on_transcoding);

  std::shared_ptr<MediaType> video (new MediaType (MediaType::VIDEO) );

  player->connect (recorder, video);
  recorder->record ();
  player->play ();

  g_usleep (4 * G_USEC_PER_SEC);

  recorder->stopAndWait ();

  std::string uri = recorder->getUri ();

  player->stop ();

  /* The lambda captures local variables: disconnect before leaving */
  conn_player.disconnect ();
  conn_recorder.disconnect ();

  std::string player_id = player->getId ();

  player.reset ();
  MediaSet::getMediaSet ()->release (player_id);
  releaseRecorderEndpoint (recorder);

  std::string codec = probe (uri, "-select_streams v:0 -show_entries "
                             "stream=codec_name,codec_tag_string -of csv=p=0");

  BOOST_TEST_MESSAGE ("ffprobe codec,tag: " << codec);
  BOOST_CHECK (codec.find ("hevc") != std::string::npos);

  if (!expected_tag.empty () ) {
    BOOST_CHECK (codec.find (expected_tag) != std::string::npos);
  }

  float dur = atof (probe (uri, "-show_entries format=duration -of "
                           "default=noprint_wrappers=1:nokey=1").c_str () );
  BOOST_CHECK_GE (dur, 2.0);

  BOOST_CHECK_EQUAL (transcoding_events.load (), 0);

  removeCodecTestDir ();
}

static void
record_h265_mp4_from_player ()
{
  record_h265_from_player ("MP4_VIDEO_ONLY", "mp4", "hvc1");
}

static void
record_h265_mkv_from_player ()
{
  record_h265_from_player ("MKV_VIDEO_ONLY", "mkv", "");
}

test_suite *
init_unit_test_suite ( int , char *[] )
{
  test_suite *test = BOOST_TEST_SUITE ( "RecorderEndpoint" );

  test->add (BOOST_TEST_CASE ( &recorder_state_changes ), 0, /* timeout */ 15);
  test->add (BOOST_TEST_CASE ( &video_codec_matrix ), 0, /* timeout */ 30);
  test->add (BOOST_TEST_CASE ( &video_codec_unknown_value ), 0, /* timeout */ 10);
  test->add (BOOST_TEST_CASE ( &record_h265_mp4_from_player ), 0, /* timeout */ 30);
  test->add (BOOST_TEST_CASE ( &record_h265_mkv_from_player ), 0, /* timeout */ 30);

  return test;
}
