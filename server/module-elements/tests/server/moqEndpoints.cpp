/*
 * (C) Copyright 2026 Kurento (http://kurento.org/)
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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#define BOOST_TEST_STATIC_LINK
#define BOOST_TEST_PROTECTED_VIRTUAL

#include <boost/test/included/unit_test.hpp>
#include <MediaPipelineImpl.hpp>
#include <MediaElementImpl.hpp>
#include <objects/MoqPublisherEndpointImpl.hpp>
#include <objects/MoqSubscriberEndpointImpl.hpp>
#include <MediaFlowInStateChanged.hpp>
#include <MediaFlowState.hpp>
#include <MediaType.hpp>
#include <ModuleManager.hpp>
#include <MediaSet.hpp>

#include <gio/gio.h>
#include <glib/gstdio.h>

#include <condition_variable>
#include <chrono>
#include <cstring>
#include <cstdlib>
#include <cerrno>
#include <csignal>
#include <mutex>
#include <spawn.h>
#include <string>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <netinet/in.h>
#include <unistd.h>

using namespace kurento;
using namespace boost::unit_test;

extern char **environ;

boost::property_tree::ptree config;
std::string mediaPipelineId;
ModuleManager moduleManager;

static const auto MEDIA_TIMEOUT = std::chrono::seconds(45);
static const auto RELAY_START_TIMEOUT = std::chrono::seconds(10);

class LocalMoqRelay
{
public:
  LocalMoqRelay() : pid(-1), directory(nullptr), port(0)
  {
  }

  ~LocalMoqRelay()
  {
    stop();
    if (directory != nullptr) {
      const std::string configPath = std::string(directory) + "/relay.toml";
      g_remove(configPath.c_str());
      g_rmdir(directory);
      g_free(directory);
    }
  }

  bool start(std::string &url, std::string &error)
  {
    gchar *executable = g_find_program_in_path("moq-relay");
    if (executable == nullptr) {
      error = "Could not find moq-relay in PATH; install the MoQ relay or set "
          "KURENTO_MOQ_TEST_RELAY_URL to use an existing relay";
      return false;
    }

    GError *directoryError = nullptr;
    directory = g_dir_make_tmp("kurento-moq-relay-XXXXXX", &directoryError);
    if (directory == nullptr) {
      error = directoryError->message;
      g_clear_error(&directoryError);
      g_free(executable);
      return false;
    }

    port = findAvailablePort();
    if (port == 0) {
      error = "Could not find an available loopback port for moq-relay";
      g_free(executable);
      return false;
    }

    const std::string configPath = std::string(directory) + "/relay.toml";
    const std::string relayConfig =
        "drain_timeout = \"0s\"\n"
        "\n"
        "[listen]\n"
        "bind = \"127.0.0.1:" + std::to_string(port) + "\"\n"
        "tls.generate = [\"localhost\"]\n"
        "\n"
        "[web.http]\n"
        "listen = \"127.0.0.1:" + std::to_string(port) + "\"\n"
        "\n"
        "[auth]\n"
        "public = \"anon/**\"\n";
    GError *configError = nullptr;
    if (!g_file_set_contents(configPath.c_str(), relayConfig.c_str(),
            relayConfig.size(), &configError)) {
      error = configError->message;
      g_clear_error(&configError);
      g_free(executable);
      return false;
    }

    char *arguments[] = {executable, const_cast<char *>(configPath.c_str()),
        nullptr};
    const int spawnError = posix_spawn(&pid, executable, nullptr, nullptr,
        arguments, environ);
    g_free(executable);
    if (spawnError != 0) {
      pid = -1;
      error = std::string("Could not start moq-relay: ") +
          g_strerror(spawnError);
      return false;
    }

    const auto deadline = std::chrono::steady_clock::now() +
        RELAY_START_TIMEOUT;
    while (std::chrono::steady_clock::now() < deadline) {
      int status = 0;
      const pid_t waited = waitpid(pid, &status, WNOHANG);
      if (waited == pid) {
        pid = -1;
        error = "moq-relay exited before becoming ready";
        return false;
      }

      GSocketClient *client = g_socket_client_new();
      g_socket_client_set_timeout(client, 1);
      GError *connectError = nullptr;
      GSocketConnection *connection = g_socket_client_connect_to_host(
          client, "127.0.0.1", port, nullptr, &connectError);
      g_clear_error(&connectError);
      g_object_unref(client);
      if (connection != nullptr) {
        GSocket *socket = g_socket_connection_get_socket(connection);
        g_socket_set_timeout(socket, 1);
        const std::string request =
            "GET /certificate.sha256 HTTP/1.1\r\n"
            "Host: localhost\r\n"
            "Connection: close\r\n\r\n";
        gsize requestBytesWritten = 0;
        GError *requestError = nullptr;
        const gboolean requestSent = g_output_stream_write_all(
            g_io_stream_get_output_stream(G_IO_STREAM(connection)),
            request.c_str(), request.size(), &requestBytesWritten, nullptr,
            &requestError);
        char response[128] = {};
        const gssize responseLength = requestSent ?
            g_input_stream_read(
                g_io_stream_get_input_stream(G_IO_STREAM(connection)),
                response, sizeof(response) - 1, nullptr, &requestError) : -1;
        g_clear_error(&requestError);
        g_object_unref(connection);
        if (responseLength > 0 &&
            (std::strncmp(response, "HTTP/1.1 200", 12) == 0 ||
            std::strncmp(response, "HTTP/1.0 200", 12) == 0)) {
          url = "http://localhost:" + std::to_string(port) + "/anon";
          return true;
        }
      }
      g_usleep(100000);
    }

    error = "moq-relay did not become ready within 10 seconds";
    return false;
  }

private:
  static guint16 findAvailablePort()
  {
    const int tcpSocket = socket(AF_INET, SOCK_STREAM, 0);
    const int udpSocket = socket(AF_INET, SOCK_DGRAM, 0);
    if (tcpSocket < 0 || udpSocket < 0) {
      if (tcpSocket >= 0) {
        close(tcpSocket);
      }
      if (udpSocket >= 0) {
        close(udpSocket);
      }
      return 0;
    }

    sockaddr_in address = {};
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    address.sin_port = 0;
    if (bind(tcpSocket, reinterpret_cast<sockaddr *>(&address),
            sizeof(address)) != 0) {
      close(tcpSocket);
      close(udpSocket);
      return 0;
    }

    socklen_t addressLength = sizeof(address);
    if (getsockname(tcpSocket, reinterpret_cast<sockaddr *>(&address),
            &addressLength) != 0 ||
        bind(udpSocket, reinterpret_cast<sockaddr *>(&address),
            sizeof(address)) != 0) {
      close(tcpSocket);
      close(udpSocket);
      return 0;
    }

    const guint16 availablePort = ntohs(address.sin_port);
    close(tcpSocket);
    close(udpSocket);
    return availablePort;
  }

  void stop()
  {
    if (pid <= 0) {
      return;
    }

    kill(pid, SIGTERM);
    for (int attempt = 0; attempt < 20; ++attempt) {
      const pid_t waited = waitpid(pid, nullptr, WNOHANG);
      if (waited == pid || (waited < 0 && errno == ECHILD)) {
        pid = -1;
        return;
      }
      g_usleep(100000);
    }

    kill(pid, SIGKILL);
    while (waitpid(pid, nullptr, 0) < 0 && errno == EINTR) {
    }
    pid = -1;
  }

  pid_t pid;
  gchar *directory;
  guint16 port;
};

struct GF {
  GF();
  ~GF();
};

BOOST_GLOBAL_FIXTURE(GF);

GF::GF()
{
  gst_init(nullptr, nullptr);
  moduleManager.loadModulesFromDirectories("../../src/server:../../..");

  mediaPipelineId = moduleManager.getFactory("MediaPipeline")->createObject(
      config, "", Json::Value())->getId();
}

GF::~GF()
{
  MediaSet::deleteMediaSet();
}

static std::shared_ptr<MediaElementImpl>
createTestElement(const char *factoryName)
{
  std::shared_ptr<MediaElementImpl> element =
      std::dynamic_pointer_cast<MediaElementImpl>(
          MediaSet::getMediaSet()->ref(new MediaElementImpl(
              boost::property_tree::ptree(),
              MediaSet::getMediaSet()->getMediaObject(mediaPipelineId),
              factoryName)));

  g_object_set(element->getGstreamerElement(),
      "audio", TRUE, "video", TRUE, NULL);
  return element;
}

static std::shared_ptr<MoqPublisherEndpointImpl>
createPublisher(const std::string &url, const std::string &broadcast)
{
  Json::Value params;
  params["mediaPipeline"] = mediaPipelineId;
  params["url"] = url;
  params["broadcast"] = broadcast;

  return std::dynamic_pointer_cast<MoqPublisherEndpointImpl>(
      moduleManager.getFactory("MoqPublisherEndpoint")->createObject(
          config, "", params));
}

static std::shared_ptr<MoqSubscriberEndpointImpl>
createSubscriber(const std::string &url, const std::string &broadcast)
{
  Json::Value params;
  params["mediaPipeline"] = mediaPipelineId;
  params["url"] = url;
  params["broadcast"] = broadcast;

  return std::dynamic_pointer_cast<MoqSubscriberEndpointImpl>(
      moduleManager.getFactory("MoqSubscriberEndpoint")->createObject(
          config, "", params));
}

static void
releaseElement(std::shared_ptr<MediaElementImpl> &element)
{
  const std::string id = element->getId();
  element.reset();
  MediaSet::getMediaSet()->release(id);
}

template <typename Endpoint>
static void
releaseEndpoint(std::shared_ptr<Endpoint> &endpoint)
{
  const std::string id = endpoint->getId();
  endpoint.reset();
  MediaSet::getMediaSet()->release(id);
}

static void
moq_audio_and_video_round_trip()
{
  LocalMoqRelay localRelay;
  std::string relayUrl;
  const char *configuredRelayUrl =
      std::getenv("KURENTO_MOQ_TEST_RELAY_URL");
  if (configuredRelayUrl != nullptr && configuredRelayUrl[0] != '\0') {
    relayUrl = configuredRelayUrl;
  } else {
    std::string relayError;
    BOOST_REQUIRE_MESSAGE(localRelay.start(relayUrl, relayError), relayError);
  }

  const std::string broadcast = "kurento-moq-test-" +
      std::to_string(g_get_monotonic_time());
  std::shared_ptr<MediaElementImpl> source = createTestElement("dummysrc");
  std::shared_ptr<MediaElementImpl> sink = createTestElement("dummysink");
  std::shared_ptr<MoqPublisherEndpointImpl> publisher =
      createPublisher(relayUrl, broadcast);
  std::shared_ptr<MoqSubscriberEndpointImpl> subscriber =
      createSubscriber(relayUrl, broadcast);

  std::mutex mutex;
  std::condition_variable condition;
  bool audioFlowing = false;
  bool videoFlowing = false;

  sigc::connection flowConnection =
      sink->signalMediaFlowInStateChanged.connect(
          [&](MediaFlowInStateChanged event) {
            const bool flowing =
                event.getState()->getValue() == MediaFlowState::FLOWING;

            std::lock_guard<std::mutex> lock(mutex);
            if (event.getMediaType()->getValue() == MediaType::AUDIO) {
              audioFlowing = flowing;
            } else if (event.getMediaType()->getValue() == MediaType::VIDEO) {
              videoFlowing = flowing;
            }
            condition.notify_one();
          });

  source->connect(publisher);
  subscriber->connect(sink);
  subscriber->subscribe();
  publisher->publish();

  std::unique_lock<std::mutex> lock(mutex);
  const bool receivedAudioAndVideo = condition.wait_for(
      lock, MEDIA_TIMEOUT, [&]() { return audioFlowing && videoFlowing; });
  lock.unlock();

  flowConnection.disconnect();
  publisher->unpublish();
  subscriber->unsubscribe();

  BOOST_CHECK_MESSAGE(receivedAudioAndVideo,
      "The MoQ subscriber did not receive flowing audio and video before "
      "the timeout");

  releaseElement(source);
  releaseElement(sink);
  releaseEndpoint(publisher);
  releaseEndpoint(subscriber);
}

test_suite *
init_unit_test_suite(int, char *[])
{
  test_suite *test = BOOST_TEST_SUITE("MoqEndpoints");
  test->add(BOOST_TEST_CASE(&moq_audio_and_video_round_trip), 0, 50);
  return test;
}
