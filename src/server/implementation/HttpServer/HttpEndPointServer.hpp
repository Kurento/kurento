/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

#ifndef __HTTP_END_POINT_SERVER_HPP__
#define __HTTP_END_POINT_SERVER_HPP__

#include <gst/gst.h>
#include <string>
#include <glibmm.h>
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

  static const uint DEFAULT_PORT = 9091;

private:
  static std::shared_ptr<HttpEndPointServer> instance;
  static Glib::Threads::RecMutex mutex;
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
