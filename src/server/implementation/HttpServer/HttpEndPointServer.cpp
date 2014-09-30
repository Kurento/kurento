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

using namespace Glib::Threads;

namespace kurento
{

static const uint DEFAULT_PORT = 9091;

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
HttpEndPointServer::getHttpEndPointServer (const uint port,
    const std::string &iface, const std::string &addr)
{
  RecMutex::Lock lock (mutex);
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

std::shared_ptr<HttpEndPointServer>
HttpEndPointServer::getHttpEndPointServer (const boost::property_tree::ptree
    &config)
{
  int port = DEFAULT_PORT;
  std::string iface;
  std::string address;

  try {
    port = config.get<int> ("modules.kurento.HttpEndpoint.port");
  } catch (boost::property_tree::ptree_error &e) {
  }

  try {
    address = config.get<int> ("modules.kurento.HttpEndpoint.announcedAddress");
  } catch (boost::property_tree::ptree_error &e) {
  }

  try {
    iface = config.get<int> ("modules.kurento.HttpEndpoint.serverAddress");
  } catch (boost::property_tree::ptree_error &e) {
  }

  return getHttpEndPointServer (port, iface, address);
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

  logHandler = [&] (GError * err) {
    if (err != NULL) {
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
  kms_http_ep_server_start (server, http_server_handler_cb, &logHandler, NULL);
}

void
HttpEndPointServer::stop ()
{
  kms_http_ep_server_stop (server, http_server_handler_cb, &logHandler , NULL);
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
