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
#include <sys/stat.h>
#include <sys/wait.h>
#include <cerrno>
#include <csignal>
#include <cstring>
#include <unistd.h>
#include <vector>

extern char **environ;

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
  /* VP9 in MP4 is not supported (see record_vp9_mp4_is_rejected) */
  check_codec_rejected ("MP4", "VP9", "mp4");
  check_codec_rejected ("MP4_VIDEO_ONLY", "VP9", "mp4");
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

static void
skip (const std::string &reason)
{
  BOOST_WARN_MESSAGE (false, "SKIPPED: " << reason);
  std::cerr << "SKIPPED: " << reason << std::endl;
}

static bool
elementAvailable (const std::string &name)
{
  GstElementFactory *factory = gst_element_factory_find (name.c_str () );

  if (factory == nullptr) {
    return false;
  }

  gst_object_unref (factory);
  return true;
}

/* Returns the first available element of a list of alternatives, or "" */
static std::string
firstAvailable (const std::vector<std::string> &names)
{
  for (const std::string &name : names) {
    if (elementAvailable (name) ) {
      return name;
    }
  }

  return "";
}

/* Video codecs that the passthrough/transcoding tests can generate a source
 * for. */
struct SourceCodec {
  const char *name;           /* videoCodec parameter value */
  const char *ffprobe_name;   /* codec_name reported by ffprobe */
  const char *src_muxer;      /* container of the generated source */
  const char *src_extension;
  const char *parser;         /* needed by the source and by the recorder */
  /* Encoders, with their options, alternatives for the same codec */
  std::vector<std::pair<std::string, std::string>> encoders;
  const char *decoder;        /* needed to decode the source (raw input) */
};

static const SourceCodec H265_SOURCE = {"H265", "hevc", "mp4mux", "mp4",
                                        "h265parse",
  /* No B-frames: PlayerEndpoint with useEncodedMedia drops buffers with
   * non-increasing PTS; B-frames are covered by other tests */
  {{"x265enc", "option-string=bframes=0"}}, "avdec_h265"
};
static const SourceCodec VP9_SOURCE = {"VP9", "vp9", "matroskamux", "mkv",
                                       "vp9parse",
  {{"vp9enc", "deadline=1 cpu-used=8"}}, "vp9dec"
};
static const SourceCodec AV1_SOURCE = {"AV1", "av1", "matroskamux", "mkv",
                                       "av1parse",
  {{"av1enc", "cpu-used=8 usage-profile=realtime"}, {"svtav1enc", "preset=10"}},
  "av1dec"
};

/* Generates a short encoded file with a test pattern, inside this process.
 * Returns false (with a visible SKIPPED message) if a needed GStreamer
 * element is not available. */
static bool
generateSource (const SourceCodec &codec, const std::string &file)
{
  std::string encoder, options;

  for (const auto &candidate : codec.encoders) {
    if (elementAvailable (candidate.first) ) {
      encoder = candidate.first;
      options = candidate.second;
      break;
    }
  }

  if (encoder.empty () ) {
    skip (std::string ("no GStreamer encoder for ") + codec.name +
          " is available: cannot generate the source");
    return false;
  }

  for (const char *name : {"videotestsrc", codec.parser, codec.src_muxer,
                           "filesink"}) {
    if (!elementAvailable (name) ) {
      skip (std::string ("GStreamer element '") + name + "' is not "
            "available: cannot generate the " + codec.name + " source");
      return false;
    }
  }

  std::string description = "videotestsrc num-buffers=75 "
                            "! video/x-raw,format=I420,width=320,height=240,framerate=25/1 "
                            "! " + encoder + " " + options + " "
                            "! " + codec.parser + " ! " + codec.src_muxer +
                            " ! filesink location=" + file;
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

  BOOST_CHECK_MESSAGE (ok, "Generating the " << codec.name <<
                       " source file failed or timed out: " << description);

  if (msg != nullptr) {
    gst_message_unref (msg);
  }

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (bus);
  gst_object_unref (pipeline);

  return ok;
}

/* Elements that the recorder needs to store `codec` in `profile`, and the
 * decoder when the source has to be decoded first. Returns false (with a
 * visible SKIPPED message) if one is missing. */
static bool
recorderElementsAvailable (const SourceCodec &codec,
                           const std::string &extension, bool decode_source)
{
  std::vector<std::string> required = {codec.parser};

  if (decode_source) {
    required.push_back (codec.decoder);
  }

  if (extension == "mp4") {
    required.push_back ("mp4mux");
  } else if (extension == "mkv") {
    required.push_back ("matroskamux");
  }

  for (const std::string &name : required) {
    if (!elementAvailable (name) ) {
      skip ("GStreamer element '" + name + "' is not available: cannot "
            "record " + codec.name + " into " + extension);
      return false;
    }
  }

  if (extension == "webm" && firstAvailable ({"webmmux", "matroskamux"}).empty () ) {
    skip ("no WebM muxer available: cannot record " +
          std::string (codec.name) + " into webm");
    return false;
  }

  bool has_encoder = false;

  for (const auto &candidate : codec.encoders) {
    has_encoder = has_encoder || elementAvailable (candidate.first);
  }

  if (!has_encoder) {
    skip (std::string ("no GStreamer encoder for ") + codec.name +
          " is available");
    return false;
  }

  return true;
}

/* Records a file played by a PlayerEndpoint into the given container, with
 * videoCodec=<codec>, and checks that the result has that codec and that the
 * recording is not empty.
 *  - encoded_media: the player sends the encoded video as-is
 *    (useEncodedMedia, passthrough: no TRANSCODING event is expected);
 *  - otherwise the player decodes it, and the recorder has to encode the raw
 *    video (a TRANSCODING event is expected). */
static void
record_from_player (const SourceCodec &codec, const std::string &profile,
                    const std::string &extension, const std::string &expected_tag,
                    bool encoded_media)
{
  std::atomic<int> transcoding_events (0);
  std::atomic<int> not_transcoding_events (0);

  BOOST_TEST_MESSAGE ("Recording " << codec.name << " from a PlayerEndpoint "
                      << (encoded_media ? "(encoded media, passthrough)" :
                          "(raw media, transcoding)") << " into " << profile);

  if (!programAvailable ("ffprobe") ) {
    skip ("ffprobe is not available");
    return;
  }

  if (!recorderElementsAvailable (codec, extension, !encoded_media) ) {
    return;
  }

  std::string src_file = getCodecTestDir () + "/source." + codec.src_extension;

  if (!generateSource (codec, src_file) ) {
    removeCodecTestDir ();
    return;
  }

  std::shared_ptr <RecorderEndpointImpl> recorder =
    createRecorderEndpointWithCodec (profile, codec.name, extension);

  Json::Value playerParams;
  playerParams ["mediaPipeline"] = mediaPipelineId;
  playerParams ["uri"] = "file://" + src_file;
  playerParams ["useEncodedMedia"] = encoded_media;

  std::shared_ptr <PlayerEndpointImpl> player =
    std::dynamic_pointer_cast <PlayerEndpointImpl> (
      moduleManager.getFactory ("PlayerEndpoint")->createObject (config, "",
          playerParams) );

  auto on_transcoding = [&] (MediaTranscodingStateChanged event) {
    BOOST_TEST_MESSAGE ("Transcoding state: " << event.getState ()->getString ()
                        << " (" << event.getBinName () << ")");

    if (event.getState ()->getValue () == MediaTranscodingState::TRANSCODING) {
      transcoding_events++;
    } else {
      not_transcoding_events++;
    }
  };

  sigc::connection conn_player =
    player->signalMediaTranscodingStateChanged.connect (on_transcoding);
  sigc::connection conn_recorder =
    recorder->signalMediaTranscodingStateChanged.connect (on_transcoding);

  std::shared_ptr<MediaType> video (new MediaType (MediaType::VIDEO) );

  std::mutex eos_mutex;
  std::condition_variable eos_cv;
  bool eos = false;

  sigc::connection conn_eos =
    player->signalEndOfStream.connect ([&] (EndOfStream) {
    std::unique_lock<std::mutex> lock (eos_mutex);
    eos = true;
    eos_cv.notify_all ();
  });

  player->connect (recorder, video);
  recorder->record ();
  player->play ();

  /* Record until the whole file has been played (it is not safe to assume a
   * fixed time: the machine may be loaded) */
  {
    std::unique_lock<std::mutex> lock (eos_mutex);
    BOOST_CHECK_MESSAGE (eos_cv.wait_for (lock, std::chrono::seconds (25),
    [&] { return eos; }), "The player did not reach the end of the stream");
  }

  g_usleep (500000);

  recorder->stopAndWait ();

  std::string uri = recorder->getUri ();

  player->stop ();

  /* The lambda captures local variables: disconnect before leaving */
  conn_player.disconnect ();
  conn_recorder.disconnect ();
  conn_eos.disconnect ();

  std::string player_id = player->getId ();

  player.reset ();
  MediaSet::getMediaSet ()->release (player_id);
  releaseRecorderEndpoint (recorder);

  std::string recorded = uri.substr (std::string ("file://").size () );
  struct stat st;

  BOOST_REQUIRE_MESSAGE (::stat (recorded.c_str (), &st) == 0 && st.st_size > 0,
                         "The recording is missing or empty: " << recorded);

  std::string info = probe (uri, "-select_streams v:0 -show_entries "
                            "stream=codec_name,codec_tag_string,pix_fmt -of csv=p=0");

  BOOST_TEST_MESSAGE ("ffprobe codec,tag: " << info);
  BOOST_CHECK_MESSAGE (info.find (codec.ffprobe_name) == 0,
                       "Expected codec " << codec.ffprobe_name << ", got: " << info);

  if (!expected_tag.empty () ) {
    BOOST_CHECK_MESSAGE (info.find (expected_tag) != std::string::npos,
                         "Expected tag " << expected_tag << ", got: " << info);
  }

  float dur = atof (probe (uri, "-show_entries format=duration -of "
                           "default=noprint_wrappers=1:nokey=1").c_str () );
  BOOST_CHECK_GE (dur, 2.0);

  if (encoded_media) {
    BOOST_CHECK_EQUAL (transcoding_events.load (), 0);
    /* Not only "no TRANSCODING": the state must have been reported */
    BOOST_CHECK_GT (not_transcoding_events.load (), 0);
  } else {
    BOOST_CHECK_GT (transcoding_events.load (), 0);
  }

  removeCodecTestDir ();
}

static void
record_h265_mp4_from_player ()
{
  record_from_player (H265_SOURCE, "MP4_VIDEO_ONLY", "mp4", "hvc1", true);
}

static void
record_h265_mkv_from_player ()
{
  record_from_player (H265_SOURCE, "MKV_VIDEO_ONLY", "mkv", "", true);
}

/* VP9 and AV1: every container of the support matrix, with encoded media
 * (passthrough) and with raw media (the recorder encodes). VP9 is not
 * supported in MP4. */
#define CODEC_CASES(NAME, SRC, EXT, PROFILE, TAG) \
  static void record_##NAME##_##EXT##_passthrough () \
  { record_from_player (SRC, PROFILE, #EXT, TAG, true); } \
  static void record_##NAME##_##EXT##_raw () \
  { record_from_player (SRC, PROFILE, #EXT, TAG, false); }

CODEC_CASES (vp9, VP9_SOURCE, mkv, "MKV_VIDEO_ONLY", "")
CODEC_CASES (vp9, VP9_SOURCE, webm, "WEBM_VIDEO_ONLY", "")
CODEC_CASES (av1, AV1_SOURCE, mp4, "MP4_VIDEO_ONLY", "av01")
CODEC_CASES (av1, AV1_SOURCE, mkv, "MKV_VIDEO_ONLY", "")
CODEC_CASES (av1, AV1_SOURCE, webm, "WEBM_VIDEO_ONLY", "")

/* VP9 in MP4 must be rejected when creating the RecorderEndpoint, so that the
 * pipeline never gets to mp4mux with VP9 (see record_vp9_mp4_known_issue). */
static void
record_vp9_mp4_is_rejected ()
{
  for (const char *profile : {"MP4", "MP4_VIDEO_ONLY"}) {
    check_codec_rejected (profile, "VP9", "mp4");
  }

  /* The other video codecs of the container keep working */
  check_codec_accepted ("MP4", "AV1", "mp4");
  removeCodecTestDir ();
}

/* Reproduces, with plain GStreamer, the problem that makes VP9 in MP4 not
 * supported: with GStreamer 1.24, mp4mux crashes (SIGSEGV) with the caps that
 * vp9parse produces from a VP9 stream without "chroma-site"; the output of
 * vp9enc, which is not parsed, is not accepted by mp4mux either. This test
 * fails while the problem exists, so it only runs if KMS_TEST_KNOWN_ISSUES is
 * set. It belongs to the follow-up work that will re-enable VP9 in MP4 once the
 * GStreamer version is upgraded: if it passes, check the support matrix. */
static void
record_vp9_mp4_known_issue ()
{
  if (::getenv ("KMS_TEST_KNOWN_ISSUES") == nullptr) {
    skip ("known issue reproduction (VP9 in mp4mux); set "
          "KMS_TEST_KNOWN_ISSUES=1 to run it");
    return;
  }

  std::string src_file = getCodecTestDir () + "/source.mkv";
  std::string out_file = getCodecTestDir () + "/out.mp4";

  if (!generateSource (VP9_SOURCE, src_file) ) {
    removeCodecTestDir ();
    return;
  }

  /* Run the pipeline in a new process (this same binary, running only the
   * helper test case below): the crash must not take this process down, and
   * forking a process that already has GStreamer threads is not safe. The
   * variables that tell the helper what to do exist only in the child. */
  std::vector<std::string> env_strings;

  for (char **e = environ; *e != nullptr; e++) {
    env_strings.push_back (*e);
  }

  env_strings.push_back ("KMS_TEST_MP4MUX_CHILD=1");
  env_strings.push_back ("KMS_TEST_MP4MUX_SRC=" + src_file);
  env_strings.push_back ("KMS_TEST_MP4MUX_OUT=" + out_file);

  std::vector<char *> envp;

  for (std::string &e : env_strings) {
    envp.push_back (const_cast<char *> (e.c_str () ) );
  }

  envp.push_back (nullptr);

  pid_t pid = ::fork ();

  if (pid == 0) {
    /* Do not leave an orphan if this process is killed by a timeout */
    ::alarm (90);
    /* Let the SIGSEGV kill the child, instead of being caught by Boost */
    ::execle ("/proc/self/exe", "test_recorder",
              "--run_test=RecorderEndpoint/vp9_mp4_known_issue_helper",
              "--catch_system_errors=no", (char *) nullptr, envp.data () );
    ::_exit (3);
  }

  BOOST_REQUIRE (pid > 0);

  int status = 0;
  pid_t waited;

  do {
    waited = ::waitpid (pid, &status, 0);
  } while (waited < 0 && errno == EINTR);

  if (waited != pid) {
    int wait_errno = errno;

    ::kill (pid, SIGKILL);
    ::waitpid (pid, &status, 0);
    BOOST_ERROR ("waitpid failed: " << ::strerror (wait_errno) );
    removeCodecTestDir ();
    return;
  }

  BOOST_CHECK_MESSAGE (!WIFSIGNALED (status), "vp9parse ! mp4mux crashed with "
                       "signal " << (WIFSIGNALED (status) ? WTERMSIG (status) : 0) );
  BOOST_CHECK_MESSAGE (WIFEXITED (status) && WEXITSTATUS (status) == 0,
                       "vp9parse ! mp4mux did not finish correctly");

  removeCodecTestDir ();
}

/* Helper of record_vp9_mp4_known_issue: runs "vp9parse ! mp4mux" and reports
 * the result with the exit status. It is only registered in the child process
 * started by that test (KMS_TEST_MP4MUX_CHILD), never in a regular run. */
static void
vp9_mp4_known_issue_helper ()
{
  const char *src_file = ::getenv ("KMS_TEST_MP4MUX_SRC");
  const char *out_file = ::getenv ("KMS_TEST_MP4MUX_OUT");

  BOOST_REQUIRE (src_file != nullptr && out_file != nullptr);

  std::string description = std::string ("filesrc location=") + src_file +
                            " ! matroskademux ! vp9parse ! mp4mux "
                            "! filesink location=" + out_file;
  GError *err = nullptr;
  GstElement *pipeline = gst_parse_launch (description.c_str (), &err);

  if (pipeline == nullptr) {
    ::_exit (2);
  }

  GstBus *bus = gst_element_get_bus (pipeline);
  gst_element_set_state (pipeline, GST_STATE_PLAYING);
  GstMessage *msg = gst_bus_timed_pop_filtered (bus, 20 * GST_SECOND,
                    (GstMessageType) (GST_MESSAGE_EOS | GST_MESSAGE_ERROR) );
  ::_exit (msg != nullptr && GST_MESSAGE_TYPE (msg) == GST_MESSAGE_EOS ? 0 : 1);
}

test_suite *
init_unit_test_suite ( int , char *[] )
{
  test_suite *test = BOOST_TEST_SUITE ( "RecorderEndpoint" );

  test->add (BOOST_TEST_CASE ( &recorder_state_changes ), 0, /* timeout */ 15);
  test->add (BOOST_TEST_CASE ( &video_codec_matrix ), 0, /* timeout */ 30);
  test->add (BOOST_TEST_CASE ( &video_codec_unknown_value ), 0, /* timeout */ 10);
  test->add (BOOST_TEST_CASE ( &record_h265_mp4_from_player ), 0, /* timeout */ 40);
  test->add (BOOST_TEST_CASE ( &record_h265_mkv_from_player ), 0, /* timeout */ 40);

#define ADD_CASE(NAME, EXT) \
  test->add (BOOST_TEST_CASE ( &record_##NAME##_##EXT##_passthrough ), 0, 40); \
  test->add (BOOST_TEST_CASE ( &record_##NAME##_##EXT##_raw ), 0, 40)

  ADD_CASE (vp9, mkv);
  ADD_CASE (vp9, webm);
  ADD_CASE (av1, mp4);
  ADD_CASE (av1, mkv);
  ADD_CASE (av1, webm);
  test->add (BOOST_TEST_CASE ( &record_vp9_mp4_is_rejected ), 0, 10);
  test->add (BOOST_TEST_CASE ( &record_vp9_mp4_known_issue ), 0, 60);

  if (::getenv ("KMS_TEST_MP4MUX_CHILD") != nullptr) {
    test->add (BOOST_TEST_CASE ( &vp9_mp4_known_issue_helper ), 0, 40);
  }


  return test;
}
