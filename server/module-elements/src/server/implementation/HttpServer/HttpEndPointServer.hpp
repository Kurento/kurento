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

#ifndef __HTTP_END_POINT_SERVER_HPP__
#define __HTTP_END_POINT_SERVER_HPP__

#include <gst/gst.h>
#include <functional>
#include <string>
#include <mutex>
#include <memory>

#include "KmsHttpEPServer.h"

namespace kurento
{

class HttpEndPointServer
{
public:
  static std::shared_ptr<HttpEndPointServer> getHttpEndPointServer (
    const uint port, const std::string &iface, const std::string &addr);
  void start ();
  void stop ();
  void registerEndPoint (GstElement *endpoint, guint timeout,
                         KmsHttpEPRegisterCallback cb, gpointer user_data, GDestroyNotify notify);
  void unregisterEndPoint (std::string uri, KmsHttpEPServerNotifyCallback cb,
                           gpointer user_data, GDestroyNotify notify);
  gulong connectSignal (std::string name, GCallback c_handler,
                        gpointer user_data);
  void disconnectSignal (gulong id);
  uint getPort ();
  std::string getInterface();
  std::string getAnnouncedAddress();

  ~HttpEndPointServer ();

  enum { DEFAULT_PORT = 9091 };

private:
  static std::shared_ptr<HttpEndPointServer> instance;
  static std::recursive_mutex mutex;
  static uint port;
  static std::string interface;
  static std::string announcedAddr;

  HttpEndPointServer ();
  KmsHttpEPServer *server;

  std::function <void (GError *err) > logHandler;

  class StaticConstructor
  {
  public:
    StaticConstructor();
  };

  static StaticConstructor staticConstructor;
};

} /* kurento */

#endif /* __HTTP_SERVICE_HPP__ */
