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

#include "HttpEndPointServer.hpp"

#define GST_CAT_DEFAULT HttpEndPointServer_
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "HttpEndPointServer"

namespace kurento
{

std::shared_ptr<HttpEndPointServer> HttpEndPointServer::instance = nullptr;
std::recursive_mutex HttpEndPointServer::mutex;

uint HttpEndPointServer::port;
std::string HttpEndPointServer::interface;
std::string HttpEndPointServer::announcedAddr;

static void
check_port (int port)
{
  if (port <= 0 || port > G_MAXUSHORT) {
    throw std::runtime_error ("Port value not valid");
  }
}

std::shared_ptr<HttpEndPointServer>
HttpEndPointServer::getHttpEndPointServer (const uint port,
    const std::string &iface, const std::string &addr)
{
  std::unique_lock <std::recursive_mutex> lock (mutex);
  uint finalPort = port;


  if (instance) {
    return instance;
  }

  if (finalPort == 0) {
    GST_INFO ("HttpService will start on any available port");
  } else {
    try {
      check_port (finalPort);
    } catch (std::exception &ex) {
      GST_WARNING ("Setting default port %d to http end point server",
                   DEFAULT_PORT);
      finalPort = DEFAULT_PORT;
    }
  }

  HttpEndPointServer::port = finalPort;
  HttpEndPointServer::interface = iface;
  HttpEndPointServer::announcedAddr = addr;

  instance = std::shared_ptr<HttpEndPointServer> (new HttpEndPointServer () );
  instance->start();

  return instance;
}

HttpEndPointServer::HttpEndPointServer ()
{
  server =
      kms_http_ep_server_new(KMS_HTTP_EP_SERVER_PORT, HttpEndPointServer::port,
                             KMS_HTTP_EP_SERVER_INTERFACE,
                             (HttpEndPointServer::interface.empty())
                                 ? nullptr
                                 : HttpEndPointServer::interface.c_str(),
                             KMS_HTTP_EP_SERVER_ANNOUNCED_IP,
                             (HttpEndPointServer::announcedAddr.empty())
                                 ? nullptr
                                 : HttpEndPointServer::announcedAddr.c_str(),
                             NULL);

  logHandler = [&](GError *err) {
    if (err != nullptr) {
      GST_ERROR ("%s", err->message);
    }
  };
}

HttpEndPointServer::~HttpEndPointServer()
{
  g_object_unref (G_OBJECT (server) );
}

static void
http_server_handler_cb (KmsHttpEPServer *self, GError *err, gpointer data)
{
  auto handler =
    reinterpret_cast < std::function < void (GError *err) > * > (data);

  (*handler) (err);
}

void
HttpEndPointServer::start ()
{
  kms_http_ep_server_start(server, http_server_handler_cb, &logHandler,
                           nullptr);
}

void
HttpEndPointServer::stop ()
{
  kms_http_ep_server_stop(server, http_server_handler_cb, &logHandler, nullptr);
}

void
HttpEndPointServer::registerEndPoint (GstElement *endpoint, guint timeout,
                                      KmsHttpEPRegisterCallback cb, gpointer user_data, GDestroyNotify notify)
{
  kms_http_ep_server_register_end_point (server, endpoint, timeout, cb, user_data,
                                         notify);
}

void
HttpEndPointServer::unregisterEndPoint (std::string uri,
                                        KmsHttpEPServerNotifyCallback cb, gpointer user_data, GDestroyNotify notify)
{
  kms_http_ep_server_unregister_end_point (server, uri.c_str(), cb, user_data,
      notify);
}

gulong
HttpEndPointServer::connectSignal (std::string name, GCallback c_handler,
                                   gpointer user_data)
{
  return g_signal_connect (server, name.c_str(), c_handler, user_data);
}

void
HttpEndPointServer::disconnectSignal (gulong id)
{
  g_signal_handler_disconnect (server, id);
}

uint
HttpEndPointServer::getPort ()
{
  guint port;

  g_object_get (G_OBJECT (server), KMS_HTTP_EP_SERVER_PORT, &port, NULL);

  return port;
}

std::string
HttpEndPointServer::getInterface()
{
  std::string iface;
  gchar *iface_c;

  g_object_get (G_OBJECT (server), KMS_HTTP_EP_SERVER_INTERFACE, &iface_c, NULL);
  iface = iface_c;
  g_free (iface_c);

  return iface;
}

std::string
HttpEndPointServer::getAnnouncedAddress()
{
  std::string addr;
  gchar *addr_c;

  g_object_get (G_OBJECT (server), KMS_HTTP_EP_SERVER_ANNOUNCED_IP, &addr_c,
                NULL);
  addr = addr_c;
  g_free (addr_c);

  return addr;
}

HttpEndPointServer::StaticConstructor HttpEndPointServer::staticConstructor;

HttpEndPointServer::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
