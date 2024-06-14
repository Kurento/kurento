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

#include <config.h>

#include <gst/gst.h>
#include "UriEndpointImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>
#if HAS_STD_REGEX_REPLACE
#include <regex>
using std::regex;
using std::regex_replace;
#else
#include <boost/regex.hpp>
using boost::regex;
using boost::regex_replace;
#endif
#include "kmsuriendpoint.h"
#include <SignalHandler.hpp>

#define GST_CAT_DEFAULT kurento_uri_endpoint_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoUriEndpointImpl"

#define DEFAULT_PATH "defaultPath"
#define DEFAULT_PATH_VALUE "file:///var/lib/kurento"

namespace kurento
{

/*
Example URI:

    rtsp://us:er:p@ssword@example.com/test.mp4?user=token/s3/request&uri=umsp://example.com:1234/

Both username and password fields should be provided in URL-encoded form:

    rtsp://us%3Aer:p%40ssword@example.com/test.mp4?user=token/s3/request&uri=umsp://example.com:1234/

GStreamer's `rtspsrc` uses the function `gst_rtsp_url_parse()` to break the string at the points
where `@` and `:` delimit username and password fields, then performs URL-decoding on those fields.

See: https://gitlab.freedesktop.org/gstreamer/gst-plugins-base/-/blob/1.16/gst-libs/gst/rtsp/gstrtspurl.c#L101
*/

void UriEndpointImpl::checkUri ()
{
  regex re ("%2F");
  this->uri = (regex_replace (uri, re, "/") );
  this->absolute_uri = this->uri;

  //Check if uri is an absolute or relative path.
  if (! (gst_uri_is_valid (this->absolute_uri.c_str () ) ) ) {
    std::string path;
    getConfigValue <std::string, UriEndpoint> (&path, DEFAULT_PATH,
        DEFAULT_PATH_VALUE);

    this->absolute_uri = path + "/" + this->uri;
  }
}

void
UriEndpointImpl::postConstructor ()
{
  EndpointImpl::postConstructor ();

  stateChangedHandlerId = register_signal_handler (G_OBJECT (element),
                          "state-changed",
                          std::function <void (GstElement *, guint) > (std::bind (
                                &UriEndpointImpl::stateChanged, this,
                                std::placeholders::_2) ),
                          std::dynamic_pointer_cast<UriEndpointImpl> (shared_from_this() ) );
}

static std::shared_ptr<UriEndpointState>
wrap_c_state (KmsUriEndpointState state)
{
  UriEndpointState::type type;

  switch (state) {
  case KMS_URI_ENDPOINT_STATE_STOP:
    type = UriEndpointState::type::STOP;
    break;

  case KMS_URI_ENDPOINT_STATE_START:
    type = UriEndpointState::type::START;
    break;

  case KMS_URI_ENDPOINT_STATE_PAUSE:
    type = UriEndpointState::type::PAUSE;
    break;

  default:
    GST_ERROR ("Invalid state value %d", state);
    type = UriEndpointState::type::STOP;
  }

  std::shared_ptr<UriEndpointState> uriState (new UriEndpointState (type) );

  return uriState;
}

UriEndpointImpl::UriEndpointImpl (const boost::property_tree::ptree &config,
                                  std::shared_ptr< MediaObjectImpl > parent,
                                  const std::string &factoryName, const std::string &uri) :
  EndpointImpl (config, parent, factoryName)
{
  KmsUriEndpointState uriState;

  this->uri = uri;
  checkUri();
  g_object_set (G_OBJECT (getGstreamerElement() ), "uri",
                this->absolute_uri.c_str(),
                NULL);

  g_object_get (G_OBJECT (getGstreamerElement() ), "state",
                &uriState, NULL);

  state = wrap_c_state (uriState);
}

UriEndpointImpl::~UriEndpointImpl ()
{
  if (stateChangedHandlerId > 0) {
    unregister_signal_handler (element, stateChangedHandlerId);
  }
}

void UriEndpointImpl::pause ()
{
  GError *error = nullptr;

  if (!kms_uri_endpoint_set_state (KMS_URI_ENDPOINT (getGstreamerElement() ),
                                   KMS_URI_ENDPOINT_STATE_PAUSE, &error) ) {
    GST_ERROR_OBJECT (getGstreamerElement(), "Error: %s", error->message);
    g_error_free (error);
  }
}

void UriEndpointImpl::stop ()
{
  GError *error = nullptr;

  if (!kms_uri_endpoint_set_state (KMS_URI_ENDPOINT (getGstreamerElement() ),
                                   KMS_URI_ENDPOINT_STATE_STOP, &error) ) {
    GST_ERROR_OBJECT (getGstreamerElement(), "Error: %s", error->message);
    g_error_free (error);
  }
}

void
UriEndpointImpl::start ()
{
  GError *error = nullptr;

  if (!kms_uri_endpoint_set_state (KMS_URI_ENDPOINT (getGstreamerElement() ),
                                   KMS_URI_ENDPOINT_STATE_START, &error) ) {
    GST_ERROR_OBJECT (getGstreamerElement(), "Error: %s", error->message);
    g_error_free (error);
  }
}

std::string
UriEndpointImpl::getUri ()
{
  return uri;
}

std::shared_ptr<UriEndpointState>
UriEndpointImpl::getState ()
{
  return state;
}

void
UriEndpointImpl::stateChanged (guint new_state)
{
  state = wrap_c_state ( (KmsUriEndpointState) new_state);

  try {
    UriEndpointStateChanged event (shared_from_this (),
        UriEndpointStateChanged::getName (), state);
    sigcSignalEmit(signalUriEndpointStateChanged, event);
  } catch (const std::bad_weak_ptr &e) {
    // shared_from_this()
    GST_ERROR ("BUG creating %s: %s",
        UriEndpointStateChanged::getName ().c_str (), e.what ());
  }
}

UriEndpointImpl::StaticConstructor UriEndpointImpl::staticConstructor;

UriEndpointImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
