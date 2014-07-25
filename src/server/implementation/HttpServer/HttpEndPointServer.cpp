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

#include "HttpEndPointServer.hpp"

#define GST_CAT_DEFAULT HttpEndPointServer_
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "HttpEndPointServer"

#define HTTP_SERVICE_GROUP "HttpEPServer"
#define HTTP_SERVICE_ADDRESS "serverAddress"
#define HTTP_SERVICE_PORT "serverPort"
#define HTTP_SERVICE_ANNOUNCED_ADDRESS "announcedAddress"

#define DEFAULT_PORT 9091

using namespace Glib::Threads;

namespace kurento
{

std::shared_ptr<HttpEndPointServer> HttpEndPointServer::instance = 0;
RecMutex HttpEndPointServer::mutex;

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
HttpEndPointServer::getHttpEndPointServer()
{
  RecMutex::Lock lock (mutex);

  if (!instance) {
    GST_DEBUG ("b");
    throw std::runtime_error ("HttpServer not created");
  }
  return instance;
}

std::shared_ptr<HttpEndPointServer>
HttpEndPointServer::getHttpEndPointServer (uint port, std::string iface, std::string addr)
{
  RecMutex::Lock lock (mutex);

  if (instance) {
    return instance;
  }

  if (port == 0){
    GST_INFO ("HttpService will start on any available port");
  } else {
    try {
      check_port(port);
    } catch (std::exception &ex) {
      GST_WARNING ("Setting default port %d to http end point server",
                  DEFAULT_PORT);
      port = DEFAULT_PORT;
    }
  }

  HttpEndPointServer::port = port;
  HttpEndPointServer::interface = iface;
  HttpEndPointServer::announcedAddr = addr;

  instance = std::shared_ptr<HttpEndPointServer> (new HttpEndPointServer () );
  instance->start();

  return instance;
}

HttpEndPointServer::HttpEndPointServer ()
{
  server = kms_http_ep_server_new (
             KMS_HTTP_EP_SERVER_PORT, HttpEndPointServer::port,
             KMS_HTTP_EP_SERVER_INTERFACE,
             (HttpEndPointServer::interface.empty() ) ? NULL :
             HttpEndPointServer::interface.c_str (),
             KMS_HTTP_EP_SERVER_ANNOUNCED_IP,
             (HttpEndPointServer::announcedAddr.empty() ) ? NULL :
             HttpEndPointServer::announcedAddr.c_str (),
             NULL);
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
  std::function <void (GError *err) > startHandler = [&] (GError * err) {
    bool error = (err != NULL);

    if (error) {
      GST_ERROR ("Service could not start. (%s)", err->message);
    } else {
      GST_INFO ("Service successfully started");
    }
  };

  kms_http_ep_server_start (server, http_server_handler_cb, &startHandler, NULL);
}

void
HttpEndPointServer::stop ()
{
  std::function <void (GError *err) > stopHandler = [&] (GError * err) {
    bool error = (err != NULL);

    if (error) {
      GST_ERROR ("Error stopping server. (%s)", err->message);
    } else {
      GST_INFO ("Service stopped");
    }
  };

  kms_http_ep_server_stop (server, http_server_handler_cb, &stopHandler , NULL);
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