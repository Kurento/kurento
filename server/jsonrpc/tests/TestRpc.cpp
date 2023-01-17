/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

#define BOOST_TEST_DYN_LINK
#define BOOST_TEST_MODULE MediaElement
#include <boost/test/unit_test.hpp>
#include <jsonrpc/JsonRpcHandler.hpp>
#include <jsonrpc/JsonRpcClient.hpp>
#include <functional>
#include <condition_variable>

static const std::string MESSAGE = "message";

using namespace kurento;

class TestTransport : public kurento::JsonRpc::Transport
{
public:
  TestTransport (JsonRpc::Handler &handler) : handler (handler)
  {

  }

  ~TestTransport() override = default;

  void sendMessage(const std::string &data) override {
    std::string response;

    handler.process (data, response);

    messageReceived (response);
  }

private:
  JsonRpc::Handler &handler;
};

class TestEchoTransport : public kurento::JsonRpc::Transport
{
public:
  TestEchoTransport() = default;

  ~TestEchoTransport() override = default;

  void sendMessage(const std::string &data) override { messageReceived(data); }
};

void
echo (const Json::Value &params, Json::Value &response)
{
  response[MESSAGE] = params[MESSAGE];
}

BOOST_AUTO_TEST_CASE (rpc_echo)
{
  JsonRpc::Handler handler;
  std::shared_ptr<TestTransport> transport (new TestTransport (handler) );
  JsonRpc::Client client (transport);
  Json::Value params;
  std::mutex mutex;
  std::condition_variable cond;
  std::unique_lock <std::mutex> lock (mutex, std::defer_lock);
  bool answered = false;

  handler.addMethod ("echo", std::bind (echo, std::placeholders::_1,
                                        std::placeholders::_2) );

  params[MESSAGE] = "123";

  client.sendRequest ("echo", params, [&lock, &params, &answered,
  &cond] (const Json::Value & result, bool isError) {
    BOOST_CHECK (params[MESSAGE] == result[MESSAGE]);
    lock.lock();
    answered = true;
    cond.notify_all ();
    lock.unlock();
  });

  lock.lock();

  if (!answered) {
    BOOST_CHECK (cond.wait_for (lock,
                                std::chrono::seconds (2) ) == std::cv_status::no_timeout);
  }
}

BOOST_AUTO_TEST_CASE (rpc_get_event)
{
  std::shared_ptr<JsonRpc::Handler> handler (new JsonRpc::Handler () );
  std::shared_ptr<TestEchoTransport> transport (new TestEchoTransport () );
  JsonRpc::Client client (transport, handler);
  Json::Value params;
  std::mutex mutex;
  std::condition_variable cond;
  std::unique_lock <std::mutex> lock (mutex, std::defer_lock);
  bool answered = false;

  handler->addMethod ("event", [&lock, &answered,
  &cond] (const Json::Value & params, Json::Value & response) {
    lock.lock();
    answered = true;
    cond.notify_all ();
    lock.unlock();
  });

  client.sendNotification ("event", params);

  lock.lock();

  if (!answered) {
    BOOST_CHECK (cond.wait_for (lock,
                                std::chrono::seconds (2) ) == std::cv_status::no_timeout);
  }
}
