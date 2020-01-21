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
#include <gst/gst.h>
#include "HttpEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>
#include "HttpServer/HttpEndPointServer.hpp"
#include <condition_variable>

#define GST_CAT_DEFAULT kurento_http_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoHttpEndpointImpl"

static const std::string HTTP_SERVICE_ADDRESS = "serverAddress";
static const std::string HTTP_SERVICE_PORT = "serverPort";
static const std::string HTTP_SERVICE_ANNOUNCED_ADDRESS = "announcedAddress";

namespace kurento
{

static void
action_requested_adaptor_function (KmsHttpEPServer *server, const gchar *uri,
                                   KmsHttpEndPointAction action, gpointer data)
{
  auto handler =
    reinterpret_cast < std::function < void (const gchar *uri,
        KmsHttpEndPointAction action) > * >
    (data);

  (*handler) (uri, action);
}

static std::string
getUriFromUrl (std::string url)
{
  std::string uri;
  gboolean host_read = FALSE;

  /* skip first 7 characters in the url regarding the protocol "http://" */
  if (url.size() < 7) {
    return "";
  }

  for ( guint i = 7; i < url.size(); i++) {
    gchar c = url.at (i);

    if (!host_read) {
      if (c == '/') {
        /* URL has no port */
        uri = url.substr (i, std::string::npos);
        break;
      } else if (c == ':') {
        /* skip port number */
        host_read = TRUE;
        continue;
      } else {
        continue;
      }
    }

    if (c != '/') {
      continue;
    }

    uri = url.substr (i, std::string::npos);
    break;
  }

  return uri;
}

static void
session_terminated_adaptor_function (KmsHttpEPServer *server, const gchar *uri,
                                     gpointer data)
{
  auto handler = reinterpret_cast<std::function<void (const gchar *uri) >*>
                 (data);

  (*handler) (uri);
}

static void
register_end_point_adaptor_function (KmsHttpEPServer *self, const gchar *uri,
                                     GstElement *e, GError *err, gpointer data)
{
  auto handler =
    reinterpret_cast<std::function<void (const gchar *uri, GError *err) >*>
    (data);

  (*handler) (uri, err);
}

static void
unregister_end_point_adaptor_function (KmsHttpEPServer *self, GError *err,
                                       gpointer data)
{
  auto handler = reinterpret_cast<std::function<void (GError *err) >*> (data);
  (*handler) (err);
}

void
HttpEndpointImpl::unregister_end_point ()
{
  std::string uri = getUriFromUrl (url);
  std::condition_variable cond;
  std::mutex mutex;
  bool finish = FALSE;

  if (!urlSet) {
    return;
  }

  std::function<void(GError * err)> aux = [&](GError *err) {
    if (err != nullptr) {
      GST_ERROR ("Could not unregister uri %s: %s", uri.c_str(), err->message);
    }

    url = "";
    urlSet = false;

    std::unique_lock<std::mutex> lock (mutex);

    finish = TRUE;
    cond.notify_all ();
  };

  server->unregisterEndPoint(uri, unregister_end_point_adaptor_function, &aux,
                             nullptr);

  std::unique_lock<std::mutex> lock (mutex);

  while (!finish) {
    cond.wait (lock);
  }
}

void
HttpEndpointImpl::register_end_point ()
{
  std::condition_variable cond;
  std::mutex mutex;
  bool done = FALSE;

  std::function <void (const gchar *, GError *err) > aux = [&] (const gchar * uri,
  GError * err) {
    std::string addr;
    guint port;
    gchar *url_tmp;

    if (err != nullptr) {
      GST_ERROR ("Can not register end point: %s", err->message);
      goto do_signal;
    }

    actionRequestedHandlerId =
      server->connectSignal ("action-requested",
                             G_CALLBACK (action_requested_adaptor_function),
                             &actionRequestedLambda);
    urlRemovedHandlerId =
      server->connectSignal ("url-removed",
                             G_CALLBACK (session_terminated_adaptor_function),
                             &sessionTerminatedLambda);
    urlExpiredHandlerId =
      server->connectSignal ("url-expired",
                             G_CALLBACK (session_terminated_adaptor_function),
                             &sessionTerminatedLambda);

    addr = server->getAnnouncedAddress();
    port = server->getPort();

    url_tmp = g_strdup_printf ("http://%s:%d%s", addr.c_str (), port, uri);
    url = std::string (url_tmp);
    g_free (url_tmp);
    urlSet = true;

do_signal:

    std::unique_lock<std::mutex> lock (mutex);

    done = TRUE;
    cond.notify_all ();

  };

  server->registerEndPoint(element, disconnectionTimeout,
                           register_end_point_adaptor_function, &aux, nullptr);

  std::unique_lock<std::mutex> lock (mutex);

  while (!done) {
    cond.wait (lock);
  }
}

bool
HttpEndpointImpl::is_registered()
{
  return urlSet;
}

HttpEndpointImpl::HttpEndpointImpl (const boost::property_tree::ptree &conf,
                                    std::shared_ptr< MediaObjectImpl > parent,
                                    int disconnectionTimeout,
                                    const std::string &factoryName) : SessionEndpointImpl (conf, parent,
                                          factoryName)
{
  this->disconnectionTimeout = disconnectionTimeout;
  actionRequestedLambda = [&] (const gchar * uri,
  KmsHttpEndPointAction action) {
    std::string uriStr = uri;

    GST_DEBUG ("Action requested URI %s", uriStr.c_str() );

    if (url.size() <= uriStr.size() ) {
      return;
    }

    /* Remove the initial "http://host:port" to compare the uri */
    std::string substr = url.substr (url.size() - uriStr.size(),
                                     std::string::npos);

    if (substr.compare (uriStr) != 0) {
      return;
    }

    /* Send event */
    if (!g_atomic_int_compare_and_exchange (& (sessionStarted), 0, 1) ) {
      return;
    }

    if (action == KMS_HTTP_END_POINT_ACTION_UNDEFINED) {
      std::string errorMessage = "Invalid or unexpected request received";

      try {
        Error event (shared_from_this (), "Invalid URI", 0, "INVALID_URI");
        GST_ERROR ("%s", errorMessage.c_str ());
        sigcSignalEmit(signalError, event);
      } catch (const std::bad_weak_ptr &e) {
        // shared_from_this()
        GST_ERROR ("BUG creating %s: %s", Error::getName ().c_str (),
            e.what ());
      }
    } else {
      try {
        MediaSessionStarted event (shared_from_this (),
            MediaSessionStarted::getName ());
        sigcSignalEmit(signalMediaSessionStarted, event);
      } catch (const std::bad_weak_ptr &e) {
        // shared_from_this()
        GST_ERROR ("BUG creating %s: %s",
            MediaSessionStarted::getName ().c_str (), e.what ());
      }
    }
  };

  sessionTerminatedLambda = [&] (const gchar * uri) {
    std::string uriStr = uri;

    if (url.size() <= uriStr.size() ) {
      return;
    }

    /* Remove the initial "http://host:port" to compare the uri */
    std::string substr = url.substr (url.size() - uriStr.size(),
                                     std::string::npos);

    if (substr.compare (uriStr) != 0) {
      return;
    }

    GST_DEBUG ("Session terminated URI %s", uriStr.c_str() );

    if (actionRequestedHandlerId > 0) {
      server->disconnectSignal (
        actionRequestedHandlerId);
      actionRequestedHandlerId = 0;
    }

    if (urlExpiredHandlerId > 0) {
      server->disconnectSignal (
        urlExpiredHandlerId);
      urlExpiredHandlerId = 0;
    }

    if (urlRemovedHandlerId > 0) {
      server->disconnectSignal (
        urlRemovedHandlerId);
      urlRemovedHandlerId = 0;
    }

    unregister_end_point ();

    if (!g_atomic_int_compare_and_exchange (& (sessionStarted), 1, 0) ) {
      return;
    }

    try {
      MediaSessionTerminated event (shared_from_this (),
          MediaSessionTerminated::getName());
      sigcSignalEmit(signalMediaSessionTerminated, event);
    } catch (const std::bad_weak_ptr &e) {
      // shared_from_this()
      GST_ERROR ("BUG creating %s: %s",
          MediaSessionTerminated::getName ().c_str (), e.what ());
    }
  };

  uint httpServicePort = 0;
  getConfigValue <uint, HttpEndpoint> (&httpServicePort, HTTP_SERVICE_PORT,
      HttpEndPointServer::DEFAULT_PORT);

  std::string httpServiceAddress;
  getConfigValue <std::string, HttpEndpoint> (&httpServiceAddress,
      HTTP_SERVICE_ADDRESS, std::string());

  std::string httpServiceAnnouncedAddress;
  getConfigValue <std::string, HttpEndpoint> (&httpServiceAnnouncedAddress,
      HTTP_SERVICE_ANNOUNCED_ADDRESS, std::string());

  server = HttpEndPointServer::getHttpEndPointServer (httpServicePort,
      httpServiceAddress, httpServiceAnnouncedAddress);

  if (server == nullptr) {
    throw KurentoException (HTTP_END_POINT_REGISTRATION_ERROR ,
                            "HttpServer is not created");
  }
}

HttpEndpointImpl::~HttpEndpointImpl()
{
  if (actionRequestedHandlerId > 0) {
    server->disconnectSignal (
      actionRequestedHandlerId);
  }

  if (urlExpiredHandlerId > 0) {
    server->disconnectSignal (
      urlExpiredHandlerId);
  }

  if (urlRemovedHandlerId > 0) {
    server->disconnectSignal (
      urlRemovedHandlerId);
  }

  unregister_end_point ();
}

std::string HttpEndpointImpl::getUrl ()
{
  return url;
}

HttpEndpointImpl::StaticConstructor HttpEndpointImpl::staticConstructor;

HttpEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
