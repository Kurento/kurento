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
#include <mutex>
#include <condition_variable>
#include <ModuleManager.hpp>
#include <MediaSet.hpp>
#include <cstdio> // popen()
#include <cstdlib> // system()

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

test_suite *
init_unit_test_suite ( int , char *[] )
{
  test_suite *test = BOOST_TEST_SUITE ( "RecorderEndpoint" );

  test->add (BOOST_TEST_CASE ( &recorder_state_changes ), 0, /* timeout */ 15);

  return test;
}
