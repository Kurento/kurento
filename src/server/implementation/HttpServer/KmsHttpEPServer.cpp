/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

#include <ctime>
#include <libsoup/soup.h>
#include <uuid/uuid.h>
#include <cstring>
#include <gio/gio.h>
#include <nice/interfaces.h>
#include <commons/kmsloop.h>

#include "KmsHttpEPServer.h"
#include "KmsHttpPost.h"
#include "http-enumtypes.h"
#include "http-marshal.h"

#define OBJECT_NAME "HttpEPServer"

/* 36-byte string (plus tailing '\0') */
#define UUID_STR_SIZE 37

#define COOKIE_NAME "HttpEPCookie"

#define KEY_HTTP_EP_SERVER "kms-http-ep-server"
G_DEFINE_QUARK (KEY_HTTP_EP_SERVER, key_http_ep_server)

#define KEY_GOT_DATA_HANDLER_ID "kms-got-data-handler-id"
G_DEFINE_QUARK (KEY_GOT_DATA_HANDLER_ID, key_got_data_handler_id)

#define KEY_FINISHED_HANDLER_ID "kms-finished-handler-id"
G_DEFINE_QUARK (KEY_FINISHED_HANDLER_ID, key_finished_handler_id)

#define KEY_TIMEOUT_ID "kms-timeout-id"
G_DEFINE_QUARK (KEY_TIMEOUT_ID, key_timeout_id)

#define KEY_FINISHED "kms-finished"
G_DEFINE_QUARK (KEY_FINISHED, key_finished)

#define KEY_MESSAGE "kms-message"
G_DEFINE_QUARK (KEY_MESSAGE, key_message)

#define KEY_COOKIE "kms-cookie"
G_DEFINE_QUARK (KEY_COOKIE, key_cookie)

#define KEY_PARAM_POST_CONTROLLER "kms-post-controller"
G_DEFINE_QUARK (KEY_PARAM_POST_CONTROLLER, key_param_post_controller)

#define KEY_PARAM_TIMEOUT "kms-param-timeout"
G_DEFINE_QUARK (KEY_PARAM_TIMEOUT, key_param_timeout)

#define GST_CAT_DEFAULT kms_http_ep_server_debug_category
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define RESOLV_TIMEOUT 5000 /* 5 seconds */

#define KMS_HTTP_EP_SERVER_GET_PRIVATE(obj) (G_TYPE_INSTANCE_GET_PRIVATE ((obj), KMS_TYPE_HTTP_EP_SERVER, KmsHttpEPServerPrivate))
struct _KmsHttpEPServerPrivate {
  GHashTable *handlers;
  SoupServer *server;
  gchar *announced_addr;
  gchar *got_addr;
  gchar *iface;
  gint port;
  GRand *rand;
  KmsLoop *loop;
};

static GType http_t = G_TYPE_INVALID;

#define KMS_IS_EXPECTED_TYPE(obj, objtype) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),(objtype)))

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsHttpEPServer, kms_http_ep_server,
                         G_TYPE_OBJECT,
                         GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, OBJECT_NAME,
                             0, "debug category for " OBJECT_NAME " element") )

/* properties */
enum {
  PROP_0,

  PROP_KMS_HTTP_EP_SERVER_PORT,
  PROP_KMS_HTTP_EP_SERVER_INTERFACE,
  PROP_KMS_HTTP_EP_SERVER_ANNOUNCED_ADDRESS,

  N_PROPERTIES
};

#define KMS_HTTP_EP_SERVER_DEFAULT_PORT 0
#define KMS_HTTP_EP_SERVER_DEFAULT_INTERFACE NULL
#define KMS_HTTP_EP_SERVER_DEFAULT_ANNOUNCED_ADDRESS \
  KMS_HTTP_EP_SERVER_DEFAULT_INTERFACE

static GParamSpec *obj_properties[N_PROPERTIES] = {
    nullptr,
};

/* signals */
enum {
  ACTION_REQUESTED,
  URL_REMOVED,
  URL_EXPIRED,
  LAST_SIGNAL
};

static guint obj_signals[LAST_SIGNAL] = { 0 };

struct tmp_data {
  KmsHttpEPServerNotifyCallback cb;
  GDestroyNotify notify;
  gpointer data;
  KmsHttpEPServer *server;
  guint id;
};

struct tmp_register_data {
  KmsHttpEPRegisterCallback function;
  gpointer data;
  GDestroyNotify notify;
  GstElement *endpoint;
  guint timeout;
  KmsHttpEPServer *server;
};

struct tmp_unregister_data {
  KmsHttpEPServerNotifyCallback cb;
  GDestroyNotify notify;
  gpointer data;
  gchar *uri;
  KmsHttpEPServer *server;
};

struct sample_data {
  GstElement *httpep;
  GstSample *sample;
};

static gchar *
get_address ()
{
  gchar *addressStr = nullptr;
  GList *ips, *l;
  gboolean done = FALSE;

  ips = nice_interfaces_get_local_ips (FALSE);

  for (l = ips; l != nullptr && !done; l = l->next) {
    GInetAddress *addr;

    addr = g_inet_address_new_from_string ( (const gchar *) l->data);

    if (addr == nullptr) {
      GST_WARNING ("Can not parse address %s", (const gchar *) l->data);
      continue;
    }

    if (G_IS_INET_ADDRESS (addr) ) {
      switch (g_inet_address_get_family (addr) ) {
      case G_SOCKET_FAMILY_INVALID:
      case G_SOCKET_FAMILY_UNIX:
        /* Ignore this addresses */
        break;

      case G_SOCKET_FAMILY_IPV6:
        /* Ignore this addresses */
        break;

      case G_SOCKET_FAMILY_IPV4:
        addressStr = g_strdup ( (const gchar *) l->data);
        done = TRUE;
        break;
      }
    }

    if (G_IS_OBJECT (addr) ) {
      g_object_unref (addr);
    }
  }

  g_list_free_full (ips, g_free);

  if (addressStr == nullptr) {
    addressStr = g_strdup ("0.0.0.0");
  }

  return addressStr;
}

static void
kms_http_ep_server_remove_timeout (KmsHttpEPServer *self, GstElement *httpep)
{
  guint *timeout_id;

  /* Remove timeout if there is any */
  timeout_id = (guint *) g_object_get_qdata (G_OBJECT (httpep),
               key_timeout_id_quark () );

  if (timeout_id == nullptr) {
    return;
  }

  GST_DEBUG ("Remove timeout %d", *timeout_id);
  kms_loop_remove (self->priv->loop, *timeout_id);
  g_object_set_qdata_full(G_OBJECT(httpep), key_timeout_id_quark(), nullptr,
                          nullptr);
}

static GstElement *
kms_http_ep_server_get_ep_from_msg (KmsHttpEPServer *self, SoupMessage *msg)
{
  SoupURI *suri = soup_message_get_uri (msg);
  const char *uri = soup_uri_get_path (suri);

  if (uri == nullptr || self->priv->handlers == nullptr) {
    return nullptr;
  }

  return (GstElement *) g_hash_table_lookup (self->priv->handlers, uri);
}

static gboolean
emit_expiration_signal_cb (gpointer user_data)
{
  SoupMessage *msg = (SoupMessage *) user_data;
  KmsHttpEPServer *serv = (KmsHttpEPServer *) g_object_get_qdata (G_OBJECT (msg),
                          key_http_ep_server_quark () );
  SoupURI *uri = soup_message_get_uri (msg);
  const char *path = soup_uri_get_path (uri);
  GstElement *httpep;

  GST_DEBUG ("Cookie expired for %s", path);
  g_signal_emit (G_OBJECT (serv), obj_signals[URL_EXPIRED], 0, path);

  httpep = (GstElement *) g_hash_table_lookup (serv->priv->handlers, path);

  if (httpep != nullptr) {
    kms_http_ep_server_remove_timeout (serv, httpep);
  }

  return G_SOURCE_REMOVE;
}

static void
destroy_guint (guint *id)
{
  g_slice_free (guint, id);
}

static void
emit_expiration_signal (SoupMessage *msg, GstElement *httpep)
{
  KmsHttpEPServer *serv;
  double t_timeout;
  SoupDate *now;
  guint *timeout, *id;

  /* Set a timeout if no more connection are done over this httpendpoint */
  /* and the cookie expires */
  now = soup_date_new_from_now (0);
  timeout = (guint *) g_object_get_qdata (G_OBJECT (httpep),
                                          key_param_timeout_quark () );

  t_timeout = difftime (soup_date_to_time_t (now) + *timeout,
                        soup_date_to_time_t (now) );

  serv = (KmsHttpEPServer *) g_object_get_qdata (G_OBJECT (msg),
         key_http_ep_server_quark () );
  id = g_slice_new (guint);
  *id = kms_loop_timeout_add_full (serv->priv->loop,
                                   G_PRIORITY_DEFAULT, t_timeout * 1000,
                                   emit_expiration_signal_cb,
                                   g_object_ref (G_OBJECT (msg) ),
                                   g_object_unref);
  g_object_set_qdata_full (G_OBJECT (httpep), key_timeout_id_quark (), id,
                           (GDestroyNotify) destroy_guint);
  soup_date_free (now);
}

static void
destroy_ulong (gulong *handlerid)
{
  g_slice_free (gulong, handlerid);
}

static void
got_post_data_cb (KmsHttpPost *post_obj, SoupBuffer *buffer, gpointer data)
{
  GstElement *httpep = GST_ELEMENT (data);
  GstFlowReturn ret;
  GstBuffer *new_buffer;
  GstMemory *memory;
  GstMapInfo info{};

  new_buffer = gst_buffer_new ();
  memory = gst_allocator_alloc(nullptr, buffer->length, nullptr);
  gst_buffer_append_memory (new_buffer, memory);

  gst_buffer_map (new_buffer, &info, GST_MAP_WRITE);
  memcpy (info.data, buffer->data, info.size);
  gst_buffer_unmap (new_buffer, &info);

  g_signal_emit_by_name (httpep, "push-buffer", new_buffer, &ret);

  if (ret != GST_FLOW_OK) {
    /* something wrong */
    GST_ERROR ("Could not send buffer to httpep %s. Ret code %d",
               GST_ELEMENT_NAME (httpep), ret);
  }

  gst_buffer_unref (new_buffer);
}

static void
finished_post_cb (KmsHttpPost *post_obj, gpointer data)
{
  GstElement *httpep = GST_ELEMENT (data);
  GstFlowReturn ret;
  gpointer param;

  GST_DEBUG ("POST finished");

  g_signal_emit_by_name (httpep, "end-of-stream", &ret);

  if (ret != GST_FLOW_OK) {
    // something wrong
    GST_ERROR ("Could not send EOS to %s. Ret code %d",
               GST_ELEMENT_NAME (httpep), ret);
  }

  param = g_object_steal_qdata (G_OBJECT (httpep), key_message_quark () );

  if (SOUP_IS_MESSAGE (param) ) {
    emit_expiration_signal (SOUP_MESSAGE (param), httpep);
    g_object_unref (G_OBJECT (param) );
  }
}

static void
install_http_post_signals (GstElement *httpep)
{
  KmsHttpPost *post_obj;
  gulong *handlerid;

  post_obj = (KmsHttpPost *) g_object_get_qdata (G_OBJECT (httpep),
             key_param_post_controller_quark () );

  if (post_obj == nullptr) {
    return;
  }

  handlerid = (gulong *) g_object_get_qdata (G_OBJECT (httpep),
              key_got_data_handler_id_quark () );

  if (handlerid == nullptr) {
    handlerid = g_slice_new (gulong);
    *handlerid = g_signal_connect (post_obj, "got-data",
                                   G_CALLBACK (got_post_data_cb), httpep);
    GST_DEBUG ("Installing got-data signal with id %lu from %p ",
               *handlerid, (gpointer) httpep);
    g_object_set_qdata_full (G_OBJECT (httpep), key_got_data_handler_id_quark (),
                             handlerid, (GDestroyNotify) destroy_ulong);
  }

  handlerid = (gulong *) g_object_get_qdata (G_OBJECT (httpep),
              key_finished_handler_id_quark () );

  if (handlerid == nullptr) {
    handlerid = g_slice_new (gulong);
    *handlerid = g_signal_connect (post_obj, "finished",
                                   G_CALLBACK (finished_post_cb), httpep);
    GST_DEBUG ("Installing finished signal with id %lu from %p ",
               *handlerid, (gpointer) httpep);
    g_object_set_qdata_full (G_OBJECT (httpep), key_finished_handler_id_quark (),
                             handlerid, (GDestroyNotify) destroy_ulong);
  }
}

static void
uninstall_http_post_signals (GstElement *httpep)
{
  KmsHttpPost *post_obj;
  gulong *handlerid;

  post_obj = (KmsHttpPost *) g_object_get_qdata (G_OBJECT (httpep),
             key_param_post_controller_quark () );

  if (post_obj == nullptr) {
    return;
  }

  handlerid = (gulong *) g_object_get_qdata (G_OBJECT (httpep),
              key_got_data_handler_id_quark () );

  if (handlerid != nullptr) {
    GST_DEBUG ("Disconnecting got-data signal with id %lu from %p ",
               *handlerid, (gpointer) httpep);
    g_signal_handler_disconnect (post_obj, *handlerid);
    g_object_set_qdata_full(G_OBJECT(httpep), key_got_data_handler_id_quark(),
                            nullptr, nullptr);
  }

  handlerid = (gulong *) g_object_get_qdata (G_OBJECT (httpep),
              key_finished_handler_id_quark () );

  if (handlerid != nullptr) {
    GST_DEBUG ("Disconnecting finished signal with id %lu from %p ",
               *handlerid, (gpointer) httpep);
    g_signal_handler_disconnect (post_obj, *handlerid);
    g_object_set_qdata_full(G_OBJECT(httpep), key_finished_handler_id_quark(),
                            nullptr, nullptr);
  }
}

static void
add_access_control_headers (SoupMessage *msg)
{
  soup_message_headers_append (msg->response_headers, "Allow", "POST");

  /* We allow access from all domains. This is generally not appropriate */
  /* TODO: Provide a configuration file containing all allowed domains */
  soup_message_headers_append (msg->response_headers,
                               "Access-Control-Allow-Origin", "*");

  /* Next header is required by chrome to work */
  soup_message_headers_append (msg->response_headers,
                               "Access-Control-Allow-Headers", "Content-Type");
}

static void
kms_http_ep_server_post_handler (KmsHttpEPServer *self, SoupMessage *msg,
                                 GstElement *httpep)
{
  KmsHttpPost *post_obj;

  post_obj = (KmsHttpPost *) g_object_get_qdata (G_OBJECT (httpep),
             key_param_post_controller_quark () );

  if (post_obj == nullptr) {
    post_obj = kms_http_post_new ();
    g_object_set_qdata_full (G_OBJECT (httpep), key_param_post_controller_quark (),
                             post_obj, g_object_unref);
  }

  install_http_post_signals (httpep);
  g_object_set (G_OBJECT (post_obj), "soup-message", msg, NULL);
}

static void
emit_removed_url_signal (KmsHttpEPServer *self, gchar *uri)
{
  GST_DEBUG ("Emit signal for uri %s", uri);
  g_signal_emit (G_OBJECT (self), obj_signals[URL_REMOVED], 0, uri);
}

static void
kms_http_ep_server_clean_http_end_point (KmsHttpEPServer *self,
    GstElement *httpep)
{
  uninstall_http_post_signals (httpep);

  kms_http_ep_server_remove_timeout (self, httpep);

  /* Cancel current transtacion */
  g_object_set_qdata_full(G_OBJECT(httpep), key_message_quark(), nullptr,
                          nullptr);
}

static void
remove_http_end_point_cb (gpointer key, gpointer value, gpointer user_data)
{
  KmsHttpEPServer *self = KMS_HTTP_EP_SERVER (user_data);
  GstElement *httpep = GST_ELEMENT (value);

  kms_http_ep_server_clean_http_end_point (self, httpep);

  /* Emit removed url signal for each key */
  emit_removed_url_signal (self, (gchar *) key);
}

static void
kms_http_ep_server_remove_handlers (KmsHttpEPServer *self)
{
  g_hash_table_foreach (self->priv->handlers, remove_http_end_point_cb, self);

  /* Remove handlers */
  g_hash_table_remove_all (self->priv->handlers);
}

static void
destroy_tmp_unregister_data (struct tmp_unregister_data *tdata)
{
  if (tdata->server != nullptr) {
    g_object_unref (tdata->server);
  }

  if (tdata->notify != nullptr) {
    tdata->notify (tdata->data);
  }

  g_free (tdata->uri);

  g_slice_free (struct tmp_unregister_data, tdata);
}

static void
destroy_tmp_register_data (struct tmp_register_data *tdata)
{
  if (tdata->notify != nullptr) {
    tdata->notify (tdata->data);
  }

  if (tdata->endpoint != nullptr) {
    gst_object_unref (tdata->endpoint);
  }

  if (tdata->server != nullptr) {
    g_object_unref (tdata->server);
  }

  g_slice_free (struct tmp_register_data, tdata);
}

static void
destroy_tmp_data (struct tmp_data *tdata)
{
  if (tdata->id > 0) {
    /* Remove timeout */
    kms_loop_remove (tdata->server->priv->loop, tdata->id);
  }

  if (tdata->server != nullptr) {
    g_object_unref (tdata->server);
  }

  if (tdata->notify != nullptr) {
    tdata->notify (tdata->data);
  }

  g_slice_free (struct tmp_data, tdata);
}

static gboolean
stop_http_ep_server_cb (struct tmp_data *tdata)
{
  GError *gerr = nullptr;

  if (tdata->server->priv->server == nullptr) {
    g_set_error (&gerr, KMS_HTTP_EP_SERVER_ERROR,
                 HTTPEPSERVER_UNEXPECTED_ERROR,
                 "Server is not started");
    goto end;
  }

  kms_http_ep_server_remove_handlers (tdata->server);

  /* Stops processing for server */
  soup_server_quit (tdata->server->priv->server);

end:

  if (tdata->cb != nullptr) {
    tdata->cb (tdata->server, gerr, tdata->data);
  }

  if (gerr != nullptr) {
    g_error_free (gerr);
  }

  return G_SOURCE_REMOVE;
}

static void
kms_http_ep_server_stop_impl (KmsHttpEPServer *self,
                              KmsHttpEPServerNotifyCallback stop_cb,
                              gpointer user_data, GDestroyNotify notify)
{
  struct tmp_data *tdata;

  tdata = g_slice_new0 (struct tmp_data);
  tdata->cb = stop_cb;
  tdata->notify = notify;
  tdata->data = user_data;
  tdata->server = KMS_HTTP_EP_SERVER ( g_object_ref (self) );

  kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_HIGH_IDLE,
                          (GSourceFunc) stop_http_ep_server_cb, tdata,
                          (GDestroyNotify) destroy_tmp_data);
}

static void
destroy_pending_message (SoupMessage *msg)
{
  KmsHttpEPServer *serv = KMS_HTTP_EP_SERVER (g_object_get_qdata (G_OBJECT (msg),
                          key_http_ep_server_quark () ) );
  GstElement *httpep = kms_http_ep_server_get_ep_from_msg (serv, msg);

  GST_DEBUG ("Destroy pending message %" GST_PTR_FORMAT, (gpointer) msg);

  if (msg->method == SOUP_METHOD_GET) {
    gulong *handlerid;

    if (httpep != nullptr) {
      /* Drop internal media flowing in the piepline */
      g_object_set (G_OBJECT (httpep), "start", FALSE, NULL);
    }

    /* Do not call to finished callback */
    handlerid = (gulong *) g_object_get_qdata (G_OBJECT (msg),
                key_finished_handler_id_quark () );
    g_signal_handler_disconnect (G_OBJECT (msg), *handlerid);

    soup_server_unpause_message (serv->priv->server, msg);
    soup_message_body_complete (msg->response_body);

  } else if (msg->method == SOUP_METHOD_POST) {
    KmsHttpPost *post_obj = nullptr;

    if (httpep != nullptr)
      post_obj = (KmsHttpPost *) g_object_get_qdata (G_OBJECT (httpep),
                 key_param_post_controller_quark () );

    if (post_obj != nullptr) {
      g_object_set (G_OBJECT (post_obj), "soup-message", NULL, NULL);
    }
  }

  /* Force to remove http server reference */
  g_object_set_qdata_full(G_OBJECT(msg), key_http_ep_server_quark(), nullptr,
                          nullptr);

  /* Remove internal msg reference */
  g_object_unref (G_OBJECT (msg) );
}

static gboolean
kms_http_ep_server_register_handler (KmsHttpEPServer *self, gchar *uri,
                                     GstElement *endpoint)
{
  GstElement *element;

  element = (GstElement *) g_hash_table_lookup (self->priv->handlers, uri);

  if (element != nullptr) {
    GST_ERROR ("URI %s is already registered for element %s.", uri,
               GST_ELEMENT_NAME (element) );
    return FALSE;
  }

  g_hash_table_insert (self->priv->handlers, uri, g_object_ref (endpoint) );

  return TRUE;
}

static const gchar *
kms_http_ep_server_get_announced_addr (KmsHttpEPServer *self)
{
  if (self->priv->announced_addr) {
    return self->priv->announced_addr;
  } else if (self->priv->iface) {
    return self->priv->iface;
  } else {
    if (!self->priv->got_addr) {
      self->priv->got_addr = get_address();
    }

    return self->priv->got_addr;
  }
}

static void
kms_http_ep_server_set_cookie (KmsHttpEPServer *self, GstElement *httpep,
                               SoupMessage *msg, const char *path)
{
  gchar *id_str, *header;
  SoupCookie *cookie;
  gint64 id;

  /* No cookie has been set for this httpep */
  id = g_rand_double_range (self->priv->rand, G_MININT64, G_MAXINT64);
  id_str = g_strdup_printf ("%" G_GINT64_FORMAT, id);
  cookie = soup_cookie_new (COOKIE_NAME, id_str,
                            kms_http_ep_server_get_announced_addr (self), path,
                            -1);
  g_free (id_str);

  header = soup_cookie_to_set_cookie_header (cookie);
  soup_message_headers_append (msg->response_headers, "Set-Cookie", header);
  g_free (header);

  g_object_set_qdata_full (G_OBJECT (httpep), key_cookie_quark (), cookie,
                           (GDestroyNotify) soup_cookie_free);
}

static gboolean
kms_http_ep_server_check_cookie (SoupCookie *cookie, SoupMessage *msg)
{
  GSList *cookies, *e;
  gboolean ret = FALSE;

  /* Check cookie */
  cookies = soup_cookies_from_request (msg);

  if (cookies == nullptr) {
    GST_WARNING ("No cookie present in request");
    return FALSE;
  }

  for (e = cookies; e != nullptr; e = e->next) {
    SoupCookie *c = (SoupCookie *) e->data;

    if (g_strcmp0 (soup_cookie_get_name (cookie),
                   soup_cookie_get_name (c) ) != 0) {
      continue;
    }

    if (g_strcmp0 (soup_cookie_get_value (cookie),
                   soup_cookie_get_value (c) ) == 0) {
      ret = TRUE;
      break;
    }
  }

  soup_cookies_free (cookies);
  return ret;
}

static gboolean
kms_http_ep_server_manage_cookie_session (KmsHttpEPServer *self,
    GstElement *httpep, SoupMessage *msg, const char *path)
{
  SoupCookie *cookie;
  gchar *method = nullptr;

  g_object_get (G_OBJECT (msg), "method", &method, NULL);

  if (g_strcmp0 (method, SOUP_METHOD_OPTIONS) == 0) {
    g_free (method);
    return TRUE;
  }

  g_free (method);

  cookie = (SoupCookie *) g_object_get_qdata (G_OBJECT (httpep),
           key_cookie_quark () );

  if (cookie != nullptr) {
    return kms_http_ep_server_check_cookie (cookie, msg);
  }

  kms_http_ep_server_set_cookie (self, httpep, msg, path);

  return TRUE;
}

static void
kms_http_ep_server_options_handler (KmsHttpEPServer *self, SoupMessage *msg,
                                    GstElement *httpep)
{
  soup_message_set_status (msg, SOUP_STATUS_OK);

  add_access_control_headers (msg);
}

static void
got_headers_handler (SoupMessage *msg, gpointer data)
{
  KmsHttpEndPointAction action = KMS_HTTP_END_POINT_ACTION_UNDEFINED;
  KmsHttpEPServer *self = KMS_HTTP_EP_SERVER (data);
  SoupURI *uri = soup_message_get_uri (msg);
  const char *path = soup_uri_get_path (uri);
  GstElement *httpep;

  httpep = (GstElement *) g_hash_table_lookup (self->priv->handlers, path);

  if (httpep == nullptr) {
    /* URI is not registered */
    soup_message_set_status_full (msg, SOUP_STATUS_NOT_FOUND,
                                  "Http end point not found");
    return;
  }

  if (!kms_http_ep_server_manage_cookie_session (self, httpep, msg, path) ) {
    GST_WARNING ("Request declined because of a cookie error");
    soup_message_set_status_full (msg, SOUP_STATUS_BAD_REQUEST,
                                  "Invalid cookie");
    return;
  }

  kms_http_ep_server_remove_timeout (self, httpep);

  /* Bind message life cicle to this httpendpoint */
  g_object_set_qdata_full (G_OBJECT (httpep), key_message_quark (),
                           g_object_ref (G_OBJECT (msg) ),
                           (GDestroyNotify) destroy_pending_message);

  /* Common parameters used for both, get and post operations */
  g_object_set_qdata_full (G_OBJECT (msg), key_http_ep_server_quark (),
                           g_object_ref (self), g_object_unref);

  if (msg->method == SOUP_METHOD_POST) {
    kms_http_ep_server_post_handler (self, msg, httpep);
    action = KMS_HTTP_END_POINT_ACTION_POST;
  } else if (msg->method == SOUP_METHOD_OPTIONS) {
    kms_http_ep_server_options_handler (self, msg, httpep);
    return;
  } else {
    GST_WARNING ("HTTP operation %s is not allowed", msg->method);
    soup_message_set_status_full (msg, SOUP_STATUS_METHOD_NOT_ALLOWED,
                                  "Not allowed");
    return;
  }

  g_signal_emit (G_OBJECT (self), obj_signals[ACTION_REQUESTED], 0, path,
                 action);
}

static void
request_started_handler (SoupServer *server, SoupMessage *msg,
                         SoupClientContext *client, gpointer data)
{
  g_signal_connect (msg, "got-headers", G_CALLBACK (got_headers_handler), data);
}

static void
kms_http_ep_server_create_server (KmsHttpEPServer *self, SoupAddress *addr)
{
  SoupSocket *listener;
  GMainContext *ctx;

  g_object_get (self->priv->loop, "context", &ctx, NULL);
  self->priv->server = soup_server_new (SOUP_SERVER_PORT, self->priv->port,
                                        SOUP_SERVER_INTERFACE, addr,
                                        SOUP_SERVER_ASYNC_CONTEXT, ctx, NULL);
  g_main_context_unref (ctx);

  /* Connect server signals handlers */
  g_signal_connect (self->priv->server, "request-started",
                    G_CALLBACK (request_started_handler), self);

  soup_server_run_async (self->priv->server);

  listener = soup_server_get_listener (self->priv->server);

  if (!soup_socket_is_connected (listener) ) {
    GST_ERROR ("Server socket is not connected");
    return;
  }

  addr = soup_socket_get_local_address (listener);

  if (self->priv->iface == nullptr) {
    /* Update the recently id adrress */
    self->priv->iface = g_strdup (soup_address_get_physical (addr) );
    /* TODO: Emit property change signal */
  }

  if (self->priv->port == 0) {
    /* Update the recently id adrress */
    self->priv->port = soup_address_get_port (addr);
    /* TODO: Emit property change signal */
  }

  GST_DEBUG ("Http end point server running in %s:%d", self->priv->iface,
             self->priv->port );
}

static void
soup_address_callback (SoupAddress *addr, guint status, gpointer user_data)
{
  struct tmp_data *tdata = (struct tmp_data *) user_data;
  GError *gerr = nullptr;

  switch (status) {
  case SOUP_STATUS_OK:
    GST_DEBUG ("Domain name resolved");
    kms_http_ep_server_create_server (tdata->server, addr);
    break;

  case SOUP_STATUS_CANCELLED:
    g_set_error (&gerr, KMS_HTTP_EP_SERVER_ERROR,
                 HTTPEPSERVER_RESOLVE_CANCELED_ERROR,
                 "Domain name resolution canceled");
    tdata->id = 0;
    break;

  case SOUP_STATUS_CANT_RESOLVE:
    g_set_error (&gerr, KMS_HTTP_EP_SERVER_ERROR,
                 HTTPEPSERVER_CANT_RESOLVE_ERROR,
                 "Domain name can not be resolved");
    break;

  default:
    g_set_error (&gerr, KMS_HTTP_EP_SERVER_ERROR,
                 HTTPEPSERVER_UNEXPECTED_ERROR,
                 "Domain name can not be resolved");
    break;
  }

  if (tdata->cb != nullptr) {
    tdata->cb (tdata->server, gerr, tdata->data);
  }

  g_clear_error (&gerr);
  destroy_tmp_data (tdata);
}

static gboolean
cancel_resolution (GCancellable *cancel)
{
  GST_WARNING ("Name resolution timed out.");

  g_cancellable_cancel (cancel);

  return G_SOURCE_REMOVE;
}

static void
kms_http_ep_server_start_impl (KmsHttpEPServer *self,
                               KmsHttpEPServerNotifyCallback start_cb,
                               gpointer user_data, GDestroyNotify notify)
{
  struct tmp_data *tdata;
  SoupAddress *addr = nullptr;
  GCancellable *cancel;

  if (self->priv->server != nullptr) {
    GST_WARNING ("Server is already running");
    return;
  }

  if (self->priv->iface == nullptr) {
    kms_http_ep_server_create_server(self, nullptr);
    start_cb(self, nullptr, user_data);
    return;
  }

  cancel = g_cancellable_new ();

  tdata = g_slice_new (struct tmp_data);
  tdata->cb = start_cb;
  tdata->notify = notify;
  tdata->data = user_data;
  tdata->server = KMS_HTTP_EP_SERVER ( g_object_ref (self) );
  tdata->id = kms_loop_timeout_add_full (self->priv->loop,
                                         G_PRIORITY_DEFAULT_IDLE, RESOLV_TIMEOUT, (GSourceFunc) cancel_resolution,
                                         cancel, g_object_unref);

  addr = soup_address_new (self->priv->iface, self->priv->port);

  soup_address_resolve_async(addr, nullptr, cancel,
                             (SoupAddressCallback)soup_address_callback, tdata);
}

static void
add_guint_param (GstElement *httpep, GQuark quark, guint val)
{
  guint *param;

  param = g_slice_new (guint);
  *param = val;
  g_object_set_qdata_full (G_OBJECT (httpep), quark, param,
                           (GDestroyNotify) destroy_guint);
}

static gboolean
register_end_point_cb (struct tmp_register_data *tdata)
{
  GError *gerr = nullptr;
  gchar *uuid_str;
  gchar *uri;
  uuid_t uuid;

  uuid_str = (gchar *) g_malloc (UUID_STR_SIZE);
  uuid_generate (uuid);
  uuid_unparse (uuid, uuid_str);

  /* Create URI from uuid string and add it to list of handlers */
  uri = g_strdup_printf ("/%s", uuid_str);
  g_free (uuid_str);

  if (!kms_http_ep_server_register_handler (tdata->server, uri,
      tdata->endpoint) ) {
    g_free (uri);
    uri = nullptr;
    g_set_error (&gerr, KMS_HTTP_EP_SERVER_ERROR,
                 HTTPEPSERVER_UNEXPECTED_ERROR,
                 "Could not register httpendpoint");
  } else {
    add_guint_param (tdata->endpoint, key_param_timeout_quark (), tdata->timeout);
  }

  if (tdata->function != nullptr) {
    tdata->function (tdata->server, uri, tdata->endpoint, gerr, tdata->data);
  }

  g_clear_error (&gerr);

  return G_SOURCE_REMOVE;
}

static void
kms_http_ep_server_register_end_point_impl (KmsHttpEPServer *self,
    GstElement *endpoint, guint timeout, KmsHttpEPRegisterCallback cb,
    gpointer user_data, GDestroyNotify notify)
{
  struct tmp_register_data *tdata;
  GError *gerr = nullptr;

  /* Check whether this is really an httpendpoint element */
  if (http_t == G_TYPE_INVALID) {
    GstElementFactory *http_f;

    http_f = gst_element_factory_find ("httpendpoint");

    if (http_f == nullptr) {
      g_set_error (&gerr, KMS_HTTP_EP_SERVER_ERROR,
                   HTTPEPSERVER_UNEXPECTED_ERROR,
                   "No httpendpoint factory found");
      goto error;
    }

    http_t = gst_element_factory_get_element_type (http_f);
    g_object_unref (http_f);
  }

  if (!KMS_IS_EXPECTED_TYPE (endpoint, http_t) ) {
    g_set_error (&gerr, KMS_HTTP_EP_SERVER_ERROR,
                 HTTPEPSERVER_UNEXPECTED_ERROR,
                 "Element is not an httpendpoint");
    goto error;
  }

  tdata = g_slice_new (struct tmp_register_data);
  tdata->endpoint = GST_ELEMENT ( gst_object_ref (endpoint) );
  tdata->timeout = timeout;
  tdata->function = cb;
  tdata->data = user_data;
  tdata->notify = notify;
  tdata->server = KMS_HTTP_EP_SERVER ( g_object_ref (self) );

  if (KMS_LOOP_IS_CURRENT_THREAD (self->priv->loop) ) {
    register_end_point_cb (tdata);
    destroy_tmp_register_data (tdata);
  } else
    kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_HIGH_IDLE,
                            (GSourceFunc) register_end_point_cb, tdata,
                            (GDestroyNotify) destroy_tmp_register_data);

  return;

error:

  if (cb != nullptr) {
    cb(self, nullptr, endpoint, gerr, user_data);
  }

  if (notify != nullptr) {
    notify (user_data);
  }

  g_clear_error (&gerr);
}

static gboolean
unregister_end_point_cb (struct tmp_unregister_data *tdata)
{
  GstElement *httpep;
  GError *gerr = nullptr;

  GST_DEBUG ("Unregister uri: %s", tdata->uri);

  if (tdata->server->priv->handlers == nullptr) {
    g_set_error (&gerr, KMS_HTTP_EP_SERVER_ERROR,
                 HTTPEPSERVER_UNEXPECTED_ERROR,
                 "handlers list is NULL");
    goto error;
  }

  if (!g_hash_table_contains (tdata->server->priv->handlers, tdata->uri) ) {
    g_set_error (&gerr, KMS_HTTP_EP_SERVER_ERROR,
                 HTTPEPSERVER_UNEXPECTED_ERROR,
                 "uri not registered");
    goto error;
  }

  httpep = (GstElement *) g_hash_table_lookup (tdata->server->priv->handlers,
           tdata->uri);

  if (httpep != nullptr) {
    kms_http_ep_server_clean_http_end_point (tdata->server, httpep);
  }

  g_hash_table_remove (tdata->server->priv->handlers, tdata->uri);

  if (tdata->cb != nullptr) {
    tdata->cb (tdata->server, gerr, tdata->data);
  }

  emit_removed_url_signal (tdata->server, tdata->uri);

  return G_SOURCE_REMOVE;

error:

  if (tdata->cb != nullptr) {
    tdata->cb (tdata->server, gerr, tdata->data);
  }

  g_clear_error (&gerr);

  return G_SOURCE_REMOVE;
}

static void
kms_http_ep_server_unregister_end_point_impl (KmsHttpEPServer *self,
    const gchar *uri, KmsHttpEPServerNotifyCallback cb, gpointer user_data,
    GDestroyNotify notify)
{
  struct tmp_unregister_data *tdata;

  tdata = g_slice_new (struct tmp_unregister_data);
  tdata->cb = cb;
  tdata->data = user_data;
  tdata->notify = notify;
  tdata->uri = g_strdup (uri);
  tdata->server = KMS_HTTP_EP_SERVER ( g_object_ref (self) );

  if (KMS_LOOP_IS_CURRENT_THREAD (self->priv->loop) ) {
    unregister_end_point_cb (tdata);
    destroy_tmp_unregister_data (tdata);
  } else {
    kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_HIGH_IDLE,
                            (GSourceFunc) unregister_end_point_cb, tdata,
                            (GDestroyNotify) destroy_tmp_unregister_data);
  }
}

static void
kms_http_ep_server_dispose (GObject *obj)
{
  KmsHttpEPServer *self = KMS_HTTP_EP_SERVER (obj);

  GST_DEBUG_OBJECT (self, "dispose");

  kms_http_ep_server_remove_handlers (self);

  /* Chain up to the parent class */
  G_OBJECT_CLASS (kms_http_ep_server_parent_class)->dispose (obj);
}

static void
kms_http_ep_server_finalize (GObject *obj)
{
  KmsHttpEPServer *self = KMS_HTTP_EP_SERVER (obj);

  GST_DEBUG_OBJECT (self, "finalize");

  g_free (self->priv->iface);

  g_free (self->priv->announced_addr);
  g_free (self->priv->got_addr);

  if (self->priv->loop) {
    g_clear_object (&self->priv->loop);
  }

  if (self->priv->handlers != nullptr) {
    g_hash_table_unref (self->priv->handlers);
    self->priv->handlers = nullptr;
  }

  if (self->priv->server != nullptr) {
    // soup_server_disconnect (self->priv->server);  g_clear_object -> dispose -> Already does soup_server_disconnect
    g_clear_object (&self->priv->server);
  }

  if (self->priv->rand != nullptr) {
    g_rand_free (self->priv->rand);
    self->priv->rand = nullptr;
  }

  /* Chain up to the parent class */
  G_OBJECT_CLASS (kms_http_ep_server_parent_class)->finalize (obj);
}

static void
kms_http_ep_server_set_property (GObject *obj, guint prop_id,
                                 const GValue *value, GParamSpec *pspec)
{
  KmsHttpEPServer *self = KMS_HTTP_EP_SERVER (obj);

  switch (prop_id) {
  case PROP_KMS_HTTP_EP_SERVER_PORT:
    self->priv->port = g_value_get_int (value);
    break;

  case PROP_KMS_HTTP_EP_SERVER_INTERFACE:

    if (self->priv->iface != nullptr) {
      g_free (self->priv->iface);
    }

    self->priv->iface = g_value_dup_string (value);
    break;

  case PROP_KMS_HTTP_EP_SERVER_ANNOUNCED_ADDRESS: {
    gchar *val = g_value_dup_string (value);

    if (self->priv->announced_addr != nullptr) {
      g_free (self->priv->announced_addr);
    }

    self->priv->announced_addr = val;

    break;
  }

  default:
    /* We don't have any other property... */
    G_OBJECT_WARN_INVALID_PROPERTY_ID (obj, prop_id, pspec);
    break;
  }
}

static void
kms_http_ep_server_get_property (GObject *obj, guint prop_id, GValue *value,
                                 GParamSpec *pspec)
{
  KmsHttpEPServer *self = KMS_HTTP_EP_SERVER (obj);

  switch (prop_id) {
  case PROP_KMS_HTTP_EP_SERVER_PORT:
    g_value_set_int (value, self->priv->port);
    break;

  case PROP_KMS_HTTP_EP_SERVER_INTERFACE:
    g_value_set_string (value, self->priv->iface);
    break;

  case PROP_KMS_HTTP_EP_SERVER_ANNOUNCED_ADDRESS:
    g_value_set_string (value, kms_http_ep_server_get_announced_addr (self) );
    break;

  default:
    /* We don't have any other property... */
    G_OBJECT_WARN_INVALID_PROPERTY_ID (obj, prop_id, pspec);
    break;
  }
}

static void
kms_http_ep_server_class_init (KmsHttpEPServerClass *klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gobject_class->set_property = kms_http_ep_server_set_property;
  gobject_class->get_property = kms_http_ep_server_get_property;
  gobject_class->dispose = kms_http_ep_server_dispose;
  gobject_class->finalize = kms_http_ep_server_finalize;

  /* Set public virtual methods */
  klass->start = kms_http_ep_server_start_impl;
  klass->stop = kms_http_ep_server_stop_impl;
  klass->register_end_point = kms_http_ep_server_register_end_point_impl;
  klass->unregister_end_point = kms_http_ep_server_unregister_end_point_impl;

  obj_properties[PROP_KMS_HTTP_EP_SERVER_PORT] =
    g_param_spec_int (KMS_HTTP_EP_SERVER_PORT,
                      "port number",
                      "The TCP port to listen on",
                      0,
                      G_MAXUSHORT,
                      KMS_HTTP_EP_SERVER_DEFAULT_PORT,
                      (GParamFlags) (G_PARAM_CONSTRUCT_ONLY | G_PARAM_READWRITE) );

  obj_properties[PROP_KMS_HTTP_EP_SERVER_INTERFACE] =
    g_param_spec_string (KMS_HTTP_EP_SERVER_INTERFACE,
                         "IP address",
                         "IP address of the network interface to run the server on",
                         KMS_HTTP_EP_SERVER_DEFAULT_INTERFACE,
                         (GParamFlags) (G_PARAM_CONSTRUCT_ONLY | G_PARAM_READWRITE) );

  obj_properties[PROP_KMS_HTTP_EP_SERVER_ANNOUNCED_ADDRESS] =
    g_param_spec_string (KMS_HTTP_EP_SERVER_ANNOUNCED_IP,
                         "Announced IP address",
                         "IP address that will be used to compose URLs",
                         KMS_HTTP_EP_SERVER_DEFAULT_INTERFACE,
                         (GParamFlags) (G_PARAM_CONSTRUCT_ONLY | G_PARAM_READWRITE) );

  g_object_class_install_properties (gobject_class,
                                     N_PROPERTIES,
                                     obj_properties);

  obj_signals[ACTION_REQUESTED] = g_signal_new(
      "action-requested", G_TYPE_FROM_CLASS(klass), G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET(KmsHttpEPServerClass, action_requested), nullptr, nullptr,
      http_marshal_VOID__STRING_ENUM, G_TYPE_NONE, 2, G_TYPE_STRING,
      KMS_TYPE_HTTP_END_POINT_ACTION);

  obj_signals[URL_REMOVED] = g_signal_new(
      "url-removed", G_TYPE_FROM_CLASS(klass), G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET(KmsHttpEPServerClass, url_removed), nullptr, nullptr,
      g_cclosure_marshal_VOID__STRING, G_TYPE_NONE, 1, G_TYPE_STRING);

  obj_signals[URL_EXPIRED] = g_signal_new(
      "url-expired", G_TYPE_FROM_CLASS(klass), G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET(KmsHttpEPServerClass, url_expired), nullptr, nullptr,
      g_cclosure_marshal_VOID__STRING, G_TYPE_NONE, 1, G_TYPE_STRING);

  /* Registers a private structure for an instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsHttpEPServerPrivate) );
}

static gboolean
equal_str_key (gconstpointer a, gconstpointer b)
{
  const char *str1 = (const char *) a;
  const char *str2 = (const char *) b;

  return (g_strcmp0 (str1, str2) == 0);
}

static void
kms_http_ep_server_init (KmsHttpEPServer *self)
{
  self->priv = KMS_HTTP_EP_SERVER_GET_PRIVATE (self);

  /* Set default values */
  self->priv->server = nullptr;
  self->priv->port = KMS_HTTP_EP_SERVER_DEFAULT_PORT;
  self->priv->iface = KMS_HTTP_EP_SERVER_DEFAULT_INTERFACE;
  self->priv->announced_addr = KMS_HTTP_EP_SERVER_DEFAULT_ANNOUNCED_ADDRESS;
  self->priv->got_addr = nullptr;
  self->priv->handlers = g_hash_table_new_full (g_str_hash, equal_str_key,
                         g_free, g_object_unref);

  self->priv->rand = g_rand_new();
  self->priv->loop = kms_loop_new ();
}

/* Virtual public methods */
KmsHttpEPServer *
kms_http_ep_server_new (const char *optname1, ...)
{
  KmsHttpEPServer *self;

  va_list ap;

  va_start (ap, optname1);
  self = KMS_HTTP_EP_SERVER (g_object_new_valist (KMS_TYPE_HTTP_EP_SERVER,
                             optname1, ap) );
  va_end (ap);

  return KMS_HTTP_EP_SERVER (self);
}

void
kms_http_ep_server_start (KmsHttpEPServer *self,
                          KmsHttpEPServerNotifyCallback start_cb,
                          gpointer user_data, GDestroyNotify notify)
{
  g_return_if_fail (KMS_IS_HTTP_EP_SERVER (self) );

  KMS_HTTP_EP_SERVER_GET_CLASS (self)->start (self, start_cb, user_data, notify);
}

void
kms_http_ep_server_stop (KmsHttpEPServer *self,
                         KmsHttpEPServerNotifyCallback stop_cb,
                         gpointer user_data, GDestroyNotify notify)
{
  g_return_if_fail (KMS_IS_HTTP_EP_SERVER (self) );

  KMS_HTTP_EP_SERVER_GET_CLASS (self)->stop (self, stop_cb, user_data, notify);
}

void
kms_http_ep_server_register_end_point (KmsHttpEPServer *self,
                                       GstElement *endpoint, guint timeout,
                                       KmsHttpEPRegisterCallback cb,
                                       gpointer user_data,
                                       GDestroyNotify notify)
{
  g_return_if_fail (KMS_IS_HTTP_EP_SERVER (self) );

  return KMS_HTTP_EP_SERVER_GET_CLASS (self)->register_end_point (self,
         endpoint, timeout, cb, user_data, notify);
}

void
kms_http_ep_server_unregister_end_point (KmsHttpEPServer *self,
    const gchar *uri, KmsHttpEPServerNotifyCallback cb, gpointer user_data,
    GDestroyNotify notify)
{
  g_return_if_fail (KMS_IS_HTTP_EP_SERVER (self) );

  return KMS_HTTP_EP_SERVER_GET_CLASS (self)->unregister_end_point (self, uri,
         cb, user_data, notify);
}
