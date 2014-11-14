/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include "commons/kmsutils.h"
#include "kmsplumberendpoint.h"
#include "kmsmultichannelcontroller.h"
#include "kms-elements-marshal.h"

#define parent_class kms_plumber_endpoint_parent_class

#define PLUMBER_DEFAULT_ADDR "localhost"
#define PLUMBER_DEFAULT_PORT 0

#define KMS_WAIT_TIMEOUT 5

#define PLUGIN_NAME "plumberendpoint"
#define GST_CAT_DEFAULT kms_plumber_endpoint_debug_category
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define KMS_PLUMBER_ENDPOINT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (               \
    (obj),                                    \
    KMS_TYPE_PLUMBER_ENDPOINT,                \
    KmsPlumberEndpointPrivate                 \
  )                                           \
)

struct _KmsPlumberEndpointPrivate
{
  KmsMultiChannelController *mcc;

  gchar *local_addr;
  guint16 local_port;

  /* SCTP server elements */
  GstElement *audiosrc;
  GstElement *videosrc;

  /* SCTP client elements */
  GstElement *audiosink;
  GstElement *videosink;
};

typedef struct _SyncCurrentPortData
{
  GCond cond;
  GMutex mutex;
  gboolean done;
  gint port;
} SyncCurrentPortData;

enum
{
  PROP_0,
  PROP_LOCAL_ADDR,
  PROP_LOCAL_PORT,
  PROP_BOUND_PORT,
  N_PROPERTIES
};

enum
{
  /* actions */
  ACTION_ACCEPT,
  ACTION_CONNECT,
  LAST_SIGNAL
};

static guint plumberEndPoint_signals[LAST_SIGNAL] = { 0 };

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsPlumberEndpoint, kms_plumber_endpoint,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_plumber_endpoint_debug_category, PLUGIN_NAME,
        0, "debug category for plumberendpoint element"));

static void
kms_plumber_endpoint_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsPlumberEndpoint *plumberendpoint = KMS_PLUMBER_ENDPOINT (object);

  switch (property_id) {
    case PROP_LOCAL_ADDR:
      if (!g_value_get_string (value)) {
        GST_WARNING_OBJECT (plumberendpoint,
            "local-address property cannot be NULL");
        break;
      }

      g_free (plumberendpoint->priv->local_addr);
      plumberendpoint->priv->local_addr = g_value_dup_string (value);
      break;
    case PROP_LOCAL_PORT:
      plumberendpoint->priv->local_port = g_value_get_int (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_plumber_endpoint_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsPlumberEndpoint *plumberendpoint = KMS_PLUMBER_ENDPOINT (object);

  switch (property_id) {
    case PROP_LOCAL_ADDR:
      g_value_set_string (value, plumberendpoint->priv->local_addr);
      break;
    case PROP_LOCAL_PORT:
      g_value_set_int (value, plumberendpoint->priv->local_port);
      break;
    case PROP_BOUND_PORT:{
      gint port;

      if (plumberendpoint->priv->mcc == NULL) {
        port = -1;
      } else {
        port =
            kms_multi_channel_controller_get_bound_port (plumberendpoint->priv->
            mcc);
      }

      g_value_set_int (value, port);
      break;
    }
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_plumber_endpoint_finalize (GObject * object)
{
  KmsPlumberEndpoint *plumberendpoint = KMS_PLUMBER_ENDPOINT (object);

  g_free (plumberendpoint->priv->local_addr);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static void
sctp_server_notify_current_port (GObject * object, GParamSpec * pspec,
    SyncCurrentPortData * syncdata)
{
  gint port;

  g_object_get (G_OBJECT (object), "current-port", &port, NULL);

  /* Wake up cause we got bound port */

  g_mutex_lock (&syncdata->mutex);
  syncdata->done = TRUE;
  syncdata->port = port;
  g_cond_signal (&syncdata->cond);
  g_mutex_unlock (&syncdata->mutex);
}

static int
kms_plumber_endpoint_create_sctp_src (StreamType type, guint16 chanid,
    KmsPlumberEndpoint * self)
{
  SyncCurrentPortData syncdata;
  GstElement *agnosticbin;
  GstElement **element;
  gint port = -1;
  gulong sig_id;

  switch (type) {
    case STREAM_TYPE_AUDIO:
      if (self->priv->audiosrc != NULL) {
        GST_WARNING_OBJECT (self, "Audio src is already created");
        return -1;
      }
      agnosticbin = kms_element_get_audio_agnosticbin (KMS_ELEMENT (self));
      self->priv->audiosrc = gst_element_factory_make ("sctpserversrc", NULL);
      element = &self->priv->audiosrc;
      break;
    case STREAM_TYPE_VIDEO:
      if (self->priv->videosrc != NULL) {
        GST_WARNING_OBJECT (self, "Video src is already created");
        return -1;
      }
      agnosticbin = kms_element_get_video_agnosticbin (KMS_ELEMENT (self));
      self->priv->videosrc = gst_element_factory_make ("sctpserversrc", NULL);
      element = &self->priv->videosrc;
      break;
    default:
      GST_WARNING_OBJECT (self, "Invalid stream type requested");
      return -1;
  }

  g_object_set (G_OBJECT (*element), "bind-address", self->priv->local_addr,
      NULL);

  g_cond_init (&syncdata.cond);
  g_mutex_init (&syncdata.mutex);
  syncdata.done = FALSE;
  syncdata.port = -1;

  sig_id = g_signal_connect (G_OBJECT (*element), "notify::current-port",
      (GCallback) sctp_server_notify_current_port, &syncdata);

  gst_bin_add (GST_BIN (self), *element);
  gst_element_sync_state_with_parent (*element);

  if (!gst_element_link (*element, agnosticbin)) {
    GST_ERROR_OBJECT (self, "Could not link %s to element %s",
        GST_ELEMENT_NAME (*element), GST_ELEMENT_NAME (agnosticbin));
    gst_element_set_state (*element, GST_STATE_NULL);
    gst_bin_remove (GST_BIN (self), *element);
    *element = NULL;

    goto end;
  }

  {
    gint64 end_time;

    /* wait for the signal emission to get the bound port */
    g_mutex_lock (&syncdata.mutex);
    end_time = g_get_monotonic_time () + KMS_WAIT_TIMEOUT * G_TIME_SPAN_SECOND;
    while (!syncdata.done) {
      if (!g_cond_wait_until (&syncdata.cond, &syncdata.mutex, end_time)) {
        GST_ERROR_OBJECT (self,
            "Time out expired while waiting for current-port signal");
      }
    }
    g_mutex_unlock (&syncdata.mutex);
  }

  port = syncdata.port;

end:
  g_signal_handler_disconnect (G_OBJECT (*element), sig_id);

  g_cond_clear (&syncdata.cond);
  g_mutex_clear (&syncdata.mutex);

  return port;
}

static gboolean
kms_plumber_endpoint_create_mcc (KmsPlumberEndpoint * self)
{
  KMS_ELEMENT_LOCK (self);

  if (self->priv->mcc != NULL) {
    KMS_ELEMENT_UNLOCK (self);
    return TRUE;
  }

  if (self->priv->local_addr == NULL) {
    GST_WARNING ("Property local address can not be NULL");
    KMS_ELEMENT_UNLOCK (self);
    return FALSE;
  }

  GST_DEBUG_OBJECT (self, "Creating multi-channel control link");

  self->priv->mcc = kms_multi_channel_controller_new (self->priv->local_addr,
      self->priv->local_port);

  kms_multi_channel_controller_set_create_stream_callback (self->priv->mcc,
      (KmsCreateStreamFunction) kms_plumber_endpoint_create_sctp_src, self,
      NULL);

  if (!kms_multi_channel_controller_start (self->priv->mcc)) {
    return FALSE;
  }

  KMS_ELEMENT_UNLOCK (self);

  return TRUE;
}

static gboolean
kms_plumber_endpoint_connect_mcc (KmsPlumberEndpoint * self, gchar * host,
    guint port)
{
  GError *err = NULL;
  gboolean ret;

  KMS_ELEMENT_LOCK (self);

  if (host == NULL) {
    KMS_ELEMENT_UNLOCK (self);
    return FALSE;
  }

  GST_DEBUG_OBJECT (self, "Connecting remote control link to %s:%d", host,
      port);

  if (!kms_multi_channel_controller_connect (self->priv->mcc, host, port, &err)) {
    GST_DEBUG_OBJECT (self, "%s", err->message);
    g_error_free (err);
    ret = FALSE;
  } else {
    ret = TRUE;
  }

  KMS_ELEMENT_UNLOCK (self);

  return ret;
}

static void
kms_plumber_endpoint_link_valve (KmsPlumberEndpoint * self, GstElement * valve,
    GstElement ** sctpsink, StreamType type)
{
  GError *err = NULL;
  gint port;
  gchar *addr;

  if (self->priv->mcc == NULL) {
    GST_WARNING_OBJECT (self, "Control channel is not connected");
    return;
  }

  port = kms_multi_channel_controller_create_media_stream (self->priv->mcc,
      type, 0, &err);

  if (port < 0) {
    GST_ERROR_OBJECT (self, "%s", err->message);
    g_error_free (err);
    return;
  }

  addr = kms_multi_channel_controller_get_remote_address (self->priv->mcc);
  if (addr == NULL) {
    GST_ERROR_OBJECT (self, "Could not get remote address");
    return;
  }

  *sctpsink = gst_element_factory_make ("sctpclientsink", NULL);
  g_object_set (G_OBJECT (*sctpsink), "host", addr, "port", port, NULL);
  g_free (addr);

  gst_bin_add (GST_BIN (self), *sctpsink);
  gst_element_sync_state_with_parent (*sctpsink);

  if (!gst_element_link (valve, *sctpsink)) {
    GST_ERROR_OBJECT (self, "Could not link %s to element %s",
        GST_ELEMENT_NAME (valve), GST_ELEMENT_NAME (*sctpsink));
  } else {
    /* Open valve so that buffers and events can pass throug it */
    kms_utils_set_valve_drop (valve, FALSE);
  }
}

static void
kms_plumber_endpoint_audio_valve_added (KmsElement * self, GstElement * valve)
{
  KmsPlumberEndpoint *plumber = KMS_PLUMBER_ENDPOINT (self);

  kms_plumber_endpoint_link_valve (plumber, valve, &plumber->priv->audiosink,
      STREAM_TYPE_AUDIO);
}

static void
kms_plumber_endpoint_audio_valve_removed (KmsElement * self, GstElement * valve)
{
  /* TODO: Implement this */
}

static void
kms_plumber_endpoint_video_valve_added (KmsElement * self, GstElement * valve)
{
  KmsPlumberEndpoint *plumber = KMS_PLUMBER_ENDPOINT (self);

  kms_plumber_endpoint_link_valve (plumber, valve, &plumber->priv->videosink,
      STREAM_TYPE_VIDEO);
}

static void
kms_plumber_endpoint_video_valve_removed (KmsElement * self, GstElement * valve)
{
  /* TODO: Implement this */
}

static gboolean
kms_plumber_endpoint_accept (KmsPlumberEndpoint * self)
{
  GST_DEBUG_OBJECT (self, "Accept multi channel control link.");

  return kms_plumber_endpoint_create_mcc (self);
}

static gboolean
kms_plumber_endpoint_connect (KmsPlumberEndpoint * self, gchar * host,
    guint port)
{
  GST_DEBUG_OBJECT (self, "Connect multi channel control link. %s:%d", host,
      port);

  KMS_ELEMENT_LOCK (self);

  if (self->priv->mcc == NULL) {
    KMS_ELEMENT_UNLOCK (self);
    return FALSE;
  }

  KMS_ELEMENT_UNLOCK (self);

  return kms_plumber_endpoint_connect_mcc (self, host, port);
}

static GstCaps *
kms_plumber_endpoint_allowed_caps (KmsElement * self, KmsElementPadType type)
{
  GstElement *valve;
  GstPad *srcpad;
  GstCaps *caps;

  switch (type) {
    case KMS_ELEMENT_PAD_TYPE_VIDEO:
      valve = kms_element_get_video_valve (self);
      break;
    case KMS_ELEMENT_PAD_TYPE_AUDIO:
      valve = kms_element_get_audio_valve (self);
      break;
    default:
      return NULL;
  }

  srcpad = gst_element_get_static_pad (valve, "src");
  caps = gst_pad_get_allowed_caps (srcpad);
  gst_object_unref (srcpad);

  return caps;
}

static gboolean
kms_plumber_endpoint_query_caps (KmsElement * self, GstPad * pad,
    GstQuery * query)
{
  GstCaps *allowed = NULL, *caps = NULL;
  GstCaps *filter, *result = NULL, *tcaps;

  gst_query_parse_caps (query, &filter);

  switch (kms_element_get_pad_type (self, pad)) {
    case KMS_ELEMENT_PAD_TYPE_VIDEO:
      allowed =
          kms_plumber_endpoint_allowed_caps (self, KMS_ELEMENT_PAD_TYPE_VIDEO);
      g_object_get (self, "video-caps", &caps, NULL);
      break;
    case KMS_ELEMENT_PAD_TYPE_AUDIO:{
      allowed =
          kms_plumber_endpoint_allowed_caps (self, KMS_ELEMENT_PAD_TYPE_AUDIO);
      g_object_get (self, "audio-caps", &caps, NULL);
      break;
    }
    default:
      GST_DEBUG ("unknown pad");
      return FALSE;
  }

  /* make sure we only return results that intersect our padtemplate */
  tcaps = gst_pad_get_pad_template_caps (pad);
  if (tcaps != NULL) {
    result = gst_caps_intersect (allowed, tcaps);
    gst_caps_unref (tcaps);
  }

  if (caps != NULL) {
    /* Filter against our caps */
    GstCaps *aux;

    aux = gst_caps_intersect (caps, result);
    gst_caps_unref (result);
    result = aux;
  }

  /* filter against the query filter when needed */
  if (filter != NULL) {
    GstCaps *aux;

    aux = gst_caps_intersect (result, filter);
    gst_caps_unref (result);
    result = aux;
  }

  gst_query_set_caps_result (query, result);
  gst_caps_unref (result);

  if (allowed != NULL)
    gst_caps_unref (allowed);

  if (caps != NULL)
    gst_caps_unref (caps);

  return TRUE;
}

static gboolean
kms_plumber_endpoint_query_accept_caps (KmsElement * self, GstPad * pad,
    GstQuery * query)
{
  GstCaps *caps, *accept;
  GstElement *valve;
  gboolean ret = TRUE;;

  switch (kms_element_get_pad_type (self, pad)) {
    case KMS_ELEMENT_PAD_TYPE_VIDEO:
      valve = kms_element_get_video_valve (self);
      g_object_get (self, "video-caps", &caps, NULL);
      break;
    case KMS_ELEMENT_PAD_TYPE_AUDIO:{
      valve = kms_element_get_audio_valve (self);
      g_object_get (self, "audio-caps", &caps, NULL);
      break;
    }
    default:
      GST_DEBUG ("unknown pad");
      return FALSE;
  }

  if (caps == NULL) {
    return KMS_ELEMENT_CLASS (parent_class)->sink_query (self, pad, query);
  }

  gst_query_parse_accept_caps (query, &accept);

  ret = gst_caps_can_intersect (accept, caps);

  if (ret) {
    GstPad *srcpad;

    srcpad = gst_element_get_static_pad (valve, "src");
    ret = gst_pad_peer_query_accept_caps (srcpad, caps);
    gst_object_unref (srcpad);
  }

  gst_caps_unref (caps);

  gst_query_set_accept_caps_result (query, ret);

  return TRUE;
}

static gboolean
kms_plumber_endpoint_sink_query (KmsElement * self, GstPad * pad,
    GstQuery * query)
{
  gboolean ret;

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_CAPS:
      ret = kms_plumber_endpoint_query_caps (self, pad, query);
      break;
    case GST_QUERY_ACCEPT_CAPS:
      ret = kms_plumber_endpoint_query_accept_caps (self, pad, query);
      break;
    default:
      ret = KMS_ELEMENT_CLASS (parent_class)->sink_query (self, pad, query);
  }

  return ret;
}

static void
kms_plumber_endpoint_class_init (KmsPlumberEndpointClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  KmsElementClass *kms_element_class = KMS_ELEMENT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "PlumberEndpoint", "SCTP/Generic", "Kurento plugin plumber end point",
      "Santiago Carot-Nemesio <sancane at gmail dot com>");

  gobject_class->finalize = kms_plumber_endpoint_finalize;
  gobject_class->set_property = kms_plumber_endpoint_set_property;
  gobject_class->get_property = kms_plumber_endpoint_get_property;

  g_object_class_install_property (gobject_class, PROP_LOCAL_ADDR,
      g_param_spec_string ("local-address", "Local Address",
          "The local address to bind the multi-channel controller socket",
          PLUMBER_DEFAULT_ADDR,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_LOCAL_PORT,
      g_param_spec_int ("local-port", "Local-port",
          "The port to listen to (0=random available port)", 0, G_MAXUINT16,
          PLUMBER_DEFAULT_PORT,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_BOUND_PORT,
      g_param_spec_int ("bound-port", "Bound port",
          "The port where this endpoint is attached (helpful when port 0 is used)",
          0, G_MAXUINT16, 0, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  /* set actions */
  plumberEndPoint_signals[ACTION_ACCEPT] =
      g_signal_new ("accept", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsPlumberEndpointClass, accept),
      NULL, NULL, __kms_elements_marshal_BOOLEAN__VOID, G_TYPE_BOOLEAN,
      0, G_TYPE_NONE);

  plumberEndPoint_signals[ACTION_CONNECT] =
      g_signal_new ("connect", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsPlumberEndpointClass, connect),
      NULL, NULL, __kms_elements_marshal_BOOLEAN__STRING_UINT, G_TYPE_BOOLEAN,
      2, G_TYPE_STRING, G_TYPE_UINT);

  klass->accept = kms_plumber_endpoint_accept;
  klass->connect = kms_plumber_endpoint_connect;

  kms_element_class->audio_valve_added =
      GST_DEBUG_FUNCPTR (kms_plumber_endpoint_audio_valve_added);
  kms_element_class->video_valve_added =
      GST_DEBUG_FUNCPTR (kms_plumber_endpoint_video_valve_added);
  kms_element_class->audio_valve_removed =
      GST_DEBUG_FUNCPTR (kms_plumber_endpoint_audio_valve_removed);
  kms_element_class->video_valve_removed =
      GST_DEBUG_FUNCPTR (kms_plumber_endpoint_video_valve_removed);
  kms_element_class->sink_query =
      GST_DEBUG_FUNCPTR (kms_plumber_endpoint_sink_query);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsPlumberEndpointPrivate));
}

static void
kms_plumber_endpoint_init (KmsPlumberEndpoint * self)
{
  self->priv = KMS_PLUMBER_ENDPOINT_GET_PRIVATE (self);

  g_object_set (self, "do-synchronization", TRUE, NULL);
}

gboolean
kms_plumber_endpoint_plugin_init (GstPlugin * plugin)
{

  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_PLUMBER_ENDPOINT);
}
