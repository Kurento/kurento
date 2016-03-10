/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

#define BOOST_TEST_STATIC_LINK
#define BOOST_TEST_PROTECTED_VIRTUAL

#include <boost/test/included/unit_test.hpp>
#include <MediaPipelineImpl.hpp>
#include <objects/RecorderEndpointImpl.hpp>
#include <mutex>
#include <condition_variable>
#include <ModuleManager.hpp>
#include <MediaSet.hpp>

#include <cstdio>
#include <iostream>

using namespace kurento;
using namespace boost::unit_test;

boost::property_tree::ptree config;
std::string mediaPipelineId;
ModuleManager moduleManager;

#define EXPECTED_LEN 0.2

struct GF {
  GF();
  ~GF();
};

BOOST_GLOBAL_FIXTURE (GF)

GF::GF()
{
  boost::property_tree::ptree ac, audioCodecs, vc, videoCodecs;
  gst_init (NULL, NULL);

  moduleManager.loadModulesFromDirectories ("../../src/server");

  mediaPipelineId = moduleManager.getFactory ("MediaPipeline")->createObject (
                      config, "",
                      Json::Value() )->getId();
}

GF::~GF()
{
  MediaSet::deleteMediaSet();
}

std::string exec (const char *cmd)
{
  std::shared_ptr<FILE> pipe (popen (cmd, "r"), pclose);

  if (!pipe) {
    return "ERROR";
  }

  char buffer[128];
  std::string result = "";

  while (!feof (pipe.get() ) ) {
    if (fgets (buffer, 128, pipe.get() ) != NULL) {
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
  std::string tmp_file = std::tmpnam (nullptr);

  constructorParams ["mediaPipeline"] = mediaPipelineId;
  constructorParams ["uri"] = "file://" + tmp_file;

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

static std::shared_ptr <MediaElementImpl>
createTestSrc (void)
{
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

static void
recorder_state_changes ()
{
  std::atomic<int> recording_changes (0);
  std::atomic<int> pause_changes (0);
  std::atomic<int> stop_changes (0);

  std::shared_ptr <RecorderEndpointImpl> recorder = createRecorderEndpoint ();
  std::shared_ptr <MediaElementImpl> src = createTestSrc();

  recorder->signalRecording.connect ([&] (Recording event) {
    recording_changes++;
  });

  recorder->signalPaused.connect ([&] (Paused event) {
    pause_changes++;
  });

  recorder->signalStopped.connect ([&] (Stopped event) {
    stop_changes++;
  });

  src->connect (recorder);
  recorder->pause();
  recorder->stop();
  recorder->stop();
  recorder->pause();
  recorder->record();
  recorder->pause();
  recorder->record();
  recorder->stop();
  recorder->pause();
  recorder->record();
  recorder->pause();
  recorder->stopAndWait();

  recorder->record();
  recorder->pause();
  recorder->record();
  recorder->record();
  // Wait for record event
  g_usleep (100000);

  recorder->pause();
  g_usleep (100000);
  recorder->record();
  recorder->record();

  // Wait for record event
  g_usleep (100000);
  recorder->stopAndWait();

  releaseTestSrc (src);

  std::string uri = recorder->getUri();

  releaseRecorderEndpoint (recorder);

  std::string command = "ffprobe -i " + uri +
                        " -show_format -v quiet | sed -n 's/duration=//p'";

  std::string duration = exec (command.c_str() );
  std::cout << duration << std::endl;

  float dur = atof (duration.c_str() );
  BOOST_WARN_GE (dur, EXPECTED_LEN * 0.8);
  BOOST_WARN_LE (dur, EXPECTED_LEN * 1.2);

  command = "ffprobe -i " + uri +
            " -show_streams -v quiet | sed -n 's/codec_name=//p'";
  std::string codecs = exec (command.c_str() );

  BOOST_REQUIRE (codecs.find ("vp8") != std::string::npos);
  BOOST_REQUIRE (codecs.find ("opus") != std::string::npos);

  std::cout << "recording_changes: " << recording_changes << std::endl;
  std::cout << "stop_changes: " << stop_changes << std::endl;
  std::cout << "pause_changes: " << pause_changes << std::endl;

  BOOST_CHECK_GE (recording_changes, 2);
  BOOST_CHECK_GE (stop_changes, 1);
  BOOST_CHECK_GE (pause_changes, 2);

  BOOST_CHECK_LE (recording_changes, 6);
  BOOST_CHECK_LE (stop_changes, 5);
  BOOST_CHECK_LE (pause_changes, 7);

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
