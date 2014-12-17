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
#include <commons/kmsutils.h>
#include <commons/kmselement.h>
#include <commons/kmsagnosticcaps.h>
#include "kmsplayerendpoint.h"
#include <commons/kmsloop.h>

#define PLUGIN_NAME "playerendpoint"
#define AUDIO_APPSRC "audio_appsrc"
#define VIDEO_APPSRC "video_appsrc"
#define URIDECODEBIN "uridecodebin"

#define APPSRC_DATA "appsrc_data"
#define APPSINK_DATA "appsink_data"
#define BASE_TIME_DATA "base_time_data"

GST_DEBUG_CATEGORY_STATIC (kms_player_endpoint_debug_category);
#define GST_CAT_DEFAULT kms_player_endpoint_debug_category

#define KMS_PLAYER_ENDPOINT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_PLAYER_ENDPOINT,                   \
    KmsPlayerEndpointPrivate                    \
  )                                             \
)

#define BASE_TIME_LOCK(obj) (                                           \
  g_mutex_lock (&KMS_PLAYER_ENDPOINT(obj)->priv->base_time_lock)        \
)

#define BASE_TIME_UNLOCK(obj) (                                         \
  g_mutex_unlock (&KMS_PLAYER_ENDPOINT(obj)->priv->base_time_lock)      \
)

typedef void (*KmsActionFunc) (gpointer user_data);

struct _KmsPlayerEndpointPrivate
{
  GstElement *pipeline;
  GstElement *uridecodebin;
  KmsLoop *loop;
  gboolean use_encoded_media;
  GMutex base_time_lock;
};

enum
{
  PROP_0,
  PROP_USE_ENCODED_MEDIA,
  N_PROPERTIES
};

enum
{
  SIGNAL_EOS,
  SIGNAL_INVALID_URI,
  SIGNAL_INVALID_MEDIA,
  LAST_SIGNAL
};

static guint kms_player_endpoint_signals[LAST_SIGNAL] = { 0 };

/* pad templates */

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsPlayerEndpoint, kms_player_endpoint,
    KMS_TYPE_URI_ENDPOINT,
    GST_DEBUG_CATEGORY_INIT (kms_player_endpoint_debug_category, PLUGIN_NAME,
        0, "debug category for playerendpoint element"));

static void
kms_player_endpoint_set_caps (KmsPlayerEndpoint * self)
{
  GstCaps *deco_caps;

  deco_caps = gst_caps_from_string (KMS_AGNOSTIC_CAPS_CAPS);
  g_object_set (G_OBJECT (self->priv->uridecodebin), "caps", deco_caps, NULL);
  gst_caps_unref (deco_caps);
}

void
kms_player_endpoint_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsPlayerEndpoint *playerendpoint = KMS_PLAYER_ENDPOINT (object);

  switch (property_id) {
    case PROP_USE_ENCODED_MEDIA:{
      playerendpoint->priv->use_encoded_media = g_value_get_boolean (value);
      if (playerendpoint->priv->use_encoded_media) {
        kms_player_endpoint_set_caps (playerendpoint);
      }
      break;
    }
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

void
kms_player_endpoint_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsPlayerEndpoint *playerendpoint = KMS_PLAYER_ENDPOINT (object);

  switch (property_id) {
    case PROP_USE_ENCODED_MEDIA:
      g_value_set_boolean (value, playerendpoint->priv->use_encoded_media);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_player_endpoint_dispose (GObject * object)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (object);

  g_clear_object (&self->priv->loop);

  if (self->priv->pipeline != NULL) {
    GstBus *bus;

    bus = gst_pipeline_get_bus (GST_PIPELINE (self->priv->pipeline));
    gst_bus_set_sync_handler (bus, NULL, NULL, NULL);
    g_object_unref (bus);

    gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
    gst_object_unref (GST_OBJECT (self->priv->pipeline));
    self->priv->pipeline = NULL;
  }

  g_mutex_clear (&self->priv->base_time_lock);

  /* clean up as possible. May be called multiple times */

  G_OBJECT_CLASS (kms_player_endpoint_parent_class)->dispose (object);
}

static void
release_gst_clock (gpointer data)
{
  g_slice_free (GstClockTime, data);
}

static GstFlowReturn
new_sample_cb (GstElement * appsink, gpointer user_data)
{
  GstElement *appsrc = GST_ELEMENT (user_data);
  GstFlowReturn ret;
  GstSample *sample;
  GstBuffer *buffer;
  GstClockTime *base_time;
  GstPad *src, *sink;

  g_signal_emit_by_name (appsink, "pull-sample", &sample);

  if (sample == NULL)
    return GST_FLOW_OK;

  buffer = gst_sample_get_buffer (sample);

  if (buffer == NULL) {
    ret = GST_FLOW_OK;
    goto end;
  }

  gst_buffer_ref (buffer);

  buffer = gst_buffer_make_writable (buffer);

  BASE_TIME_LOCK (GST_OBJECT_PARENT (appsrc));

  base_time =
      g_object_get_data (G_OBJECT (GST_OBJECT_PARENT (appsrc)), BASE_TIME_DATA);

  if (base_time == NULL) {
    GstClock *clock;

    clock = gst_element_get_clock (appsrc);
    base_time = g_slice_new0 (GstClockTime);

    g_object_set_data_full (G_OBJECT (GST_OBJECT_PARENT (appsrc)),
        BASE_TIME_DATA, base_time, release_gst_clock);
    *base_time =
        gst_clock_get_time (clock) - gst_element_get_base_time (appsrc);
    g_object_unref (clock);
    GST_DEBUG ("Setting base time to: %" G_GUINT64_FORMAT, *base_time);
  }

  if (GST_BUFFER_PTS_IS_VALID (buffer))
    buffer->pts += *base_time;
  if (GST_BUFFER_DTS_IS_VALID (buffer))
    buffer->dts += *base_time;

  BASE_TIME_UNLOCK (GST_OBJECT_PARENT (appsrc));

  src = gst_element_get_static_pad (appsrc, "src");
  sink = gst_pad_get_peer (src);

  if (sink != NULL) {
    if (GST_OBJECT_FLAG_IS_SET (sink, GST_PAD_FLAG_EOS)) {
      GST_INFO_OBJECT (sink, "Sending flush events");
      gst_pad_send_event (sink, gst_event_new_flush_start ());
      gst_pad_send_event (sink, gst_event_new_flush_stop (FALSE));
    }
    g_object_unref (sink);
  }

  g_object_unref (src);

  // TODO: Do something to fix a possible previous EOS event
  g_signal_emit_by_name (appsrc, "push-buffer", buffer, &ret);

  gst_buffer_unref (buffer);

  if (ret != GST_FLOW_OK) {
    /* something wrong */
    GST_ERROR ("Could not send buffer to appsrc %s. Cause: %s",
        GST_ELEMENT_NAME (appsrc), gst_flow_get_name (ret));
  }

end:
  if (sample != NULL)
    gst_sample_unref (sample);

  return ret;
}

static void
eos_cb (GstElement * appsink, gpointer user_data)
{
  GstElement *appsrc = GST_ELEMENT (user_data);
  GstPad *srcpad;

  GST_DEBUG_OBJECT (appsrc, "Sending eos event to main pipeline");

  srcpad = gst_element_get_static_pad (appsrc, "src");
  if (srcpad == NULL) {
    GST_ERROR ("Can not get source pad from %s", GST_ELEMENT_NAME (appsrc));
    return;
  }

  if (!gst_pad_push_event (srcpad, gst_event_new_eos ()))
    GST_ERROR ("EOS event could not be sent");

  g_object_unref (srcpad);
}

static GstPadProbeReturn
main_pipeline_probe (GstPad * pad, GstPadProbeInfo * info, gpointer element)
{
  GstQuery *query = GST_PAD_PROBE_INFO_QUERY (info);
  GstElement *appsink = GST_ELEMENT (element);

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_CAPS:
    case GST_QUERY_ACCEPT_CAPS:
      break;
    default:
      return GST_PAD_PROBE_OK;
  }

  query = gst_query_make_writable (query);
  gst_element_query (appsink, query);
  GST_PAD_PROBE_INFO_DATA (info) = query;

  return GST_PAD_PROBE_OK;
}

static GstElement *
kms_player_end_point_add_appsrc (KmsPlayerEndpoint * self,
    GstElement * agnosticbin, GstElement * appsink)
{
  GstElement *appsrc = NULL;
  GstPad *srcpad;

  /* Create appsrc element and link to agnosticbin */
  appsrc = gst_element_factory_make ("appsrc", NULL);
  g_object_set (G_OBJECT (appsrc), "is-live", TRUE, "do-timestamp", FALSE,
      "min-latency", G_GUINT64_CONSTANT (0), "max-latency",
      G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME, NULL);

  srcpad = gst_element_get_static_pad (appsrc, "src");
  gst_pad_add_probe (srcpad, GST_PAD_PROBE_TYPE_QUERY_UPSTREAM,
      main_pipeline_probe, appsink, NULL);
  g_object_unref (srcpad);

  gst_bin_add (GST_BIN (self), appsrc);
  if (!gst_element_link (appsrc, agnosticbin)) {
    GST_ERROR ("Could not link %s to element %s", GST_ELEMENT_NAME (appsrc),
        GST_ELEMENT_NAME (agnosticbin));
  }

  gst_element_sync_state_with_parent (appsrc);

  return appsrc;
}

static GstPadProbeReturn
set_appsrc_caps (GstPad * pad, GstPadProbeInfo * info, gpointer element)
{
  GstEvent *event = GST_PAD_PROBE_INFO_EVENT (info);
  GstElement *appsrc = GST_ELEMENT (element);
  GstCaps *caps;

  if (GST_EVENT_TYPE (event) != GST_EVENT_CAPS) {
    return GST_PAD_PROBE_OK;
  }

  gst_event_parse_caps (event, &caps);
  if (caps == NULL) {
    GST_ERROR_OBJECT (pad, "Invalid caps received");
    return GST_PAD_PROBE_OK;
  }

  GST_DEBUG_OBJECT (appsrc, "Setting caps %" GST_PTR_FORMAT, caps);

  g_object_set (G_OBJECT (appsrc), "caps", caps, NULL);

  return GST_PAD_PROBE_OK;
}

static GstElement *
kms_player_end_point_get_agnostic_for_pad (KmsPlayerEndpoint * self,
    GstPad * pad)
{
  GstCaps *caps, *audio_caps = NULL, *video_caps = NULL;
  GstElement *agnosticbin = NULL;

  caps = gst_pad_query_caps (pad, NULL);

  if (caps == NULL) {
    return NULL;
  }

  audio_caps = gst_caps_from_string (KMS_AGNOSTIC_AUDIO_CAPS);
  video_caps = gst_caps_from_string (KMS_AGNOSTIC_VIDEO_CAPS);

  if (gst_caps_can_intersect (audio_caps, caps)) {
    agnosticbin = kms_element_get_audio_agnosticbin (KMS_ELEMENT (self));
  } else if (gst_caps_can_intersect (video_caps, caps)) {
    agnosticbin = kms_element_get_video_agnosticbin (KMS_ELEMENT (self));
  }

  gst_caps_unref (caps);
  gst_caps_unref (audio_caps);
  gst_caps_unref (video_caps);

  return agnosticbin;
}

static GstPadProbeReturn
negotiate_appsrc_caps (GstPad * pad, GstPadProbeInfo * info, gpointer element)
{
  GstQuery *query = GST_PAD_PROBE_INFO_QUERY (info);
  GstElement *appsrc = GST_ELEMENT (element);
  GstPad *srcpad;

  switch (GST_QUERY_TYPE (query)) {
    case GST_QUERY_CAPS:
    case GST_QUERY_ACCEPT_CAPS:
      break;
    default:
      return GST_PAD_PROBE_OK;
  }

  query = gst_query_make_writable (query);
  srcpad = gst_element_get_static_pad (appsrc, "src");
  /* Send query to the agnosticbin */
  gst_pad_peer_query (srcpad, query);
  g_object_unref (srcpad);
  GST_PAD_PROBE_INFO_DATA (info) = query;

  return GST_PAD_PROBE_OK;
}

static GstPadProbeReturn
internal_pipeline_probe (GstPad * pad, GstPadProbeInfo * info, gpointer element)
{
  if (GST_PAD_PROBE_INFO_TYPE (info) & GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM) {
    return set_appsrc_caps (pad, info, element);
  } else if (GST_PAD_PROBE_INFO_TYPE (info) &
      GST_PAD_PROBE_TYPE_QUERY_DOWNSTREAM) {
    return negotiate_appsrc_caps (pad, info, element);
  } else {
    GST_WARNING_OBJECT (pad, "Probe does nothing");
    return GST_PAD_PROBE_OK;
  }
}

static void
pad_added (GstElement * element, GstPad * pad, KmsPlayerEndpoint * self)
{
  GstElement *appsink, *appsrc;
  GstElement *agnosticbin;
  GstPad *sinkpad;

  GST_DEBUG_OBJECT (pad, "Pad added");

  agnosticbin = kms_player_end_point_get_agnostic_for_pad (self, pad);

  if (agnosticbin != NULL) {
    /* Create appsink */
    appsink = gst_element_factory_make ("appsink", NULL);
    appsrc = kms_player_end_point_add_appsrc (self, agnosticbin, appsink);

    g_object_set (appsink, "enable-last-sample", FALSE, "emit-signals", TRUE,
        "qos", TRUE, "max-buffers", 1, NULL);

    /* Connect new-sample signal to callback */
    g_signal_connect (appsink, "new-sample", G_CALLBACK (new_sample_cb),
        appsrc);
    g_signal_connect (appsink, "eos", G_CALLBACK (eos_cb), appsrc);

    g_object_set_data (G_OBJECT (pad), APPSINK_DATA, appsink);
    g_object_set_data (G_OBJECT (pad), APPSRC_DATA, appsrc);
  } else {
    GST_WARNING_OBJECT (self, "No supported pad: %" GST_PTR_FORMAT
        ". Connecting it to a fakesink", pad);
    appsink = gst_element_factory_make ("fakesink", NULL);
  }

  g_object_set (appsink, "sync", TRUE, "async", FALSE, NULL);

  sinkpad = gst_element_get_static_pad (appsink, "sink");

  if (agnosticbin != NULL) {
    gst_pad_add_probe (sinkpad,
        (GST_PAD_PROBE_TYPE_QUERY_DOWNSTREAM |
            GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM), internal_pipeline_probe,
        appsrc, NULL);
  }

  gst_bin_add (GST_BIN (self->priv->pipeline), appsink);
  gst_pad_link (pad, sinkpad);

  g_object_unref (sinkpad);

  gst_element_sync_state_with_parent (appsink);
}

static void
kms_remove_element_from_bin (GstBin * bin, GstElement * element)
{
  GST_DEBUG ("Removing %" GST_PTR_FORMAT " from %" GST_PTR_FORMAT, element,
      bin);

  if (!gst_element_set_locked_state (element, TRUE)) {
    GST_ERROR ("Could not block element %" GST_PTR_FORMAT, element);
  }

  gst_element_set_state (element, GST_STATE_NULL);
  gst_bin_remove (bin, element);
}

static void
pad_removed (GstElement * element, GstPad * pad, KmsPlayerEndpoint * self)
{
  GstElement *appsink, *appsrc;

  GST_DEBUG_OBJECT (pad, "Pad removed");

  if (GST_PAD_IS_SINK (pad))
    return;

  GST_DEBUG ("pad %" GST_PTR_FORMAT " removed", pad);

  appsink = g_object_steal_data (G_OBJECT (pad), APPSINK_DATA);
  appsrc = g_object_steal_data (G_OBJECT (pad), APPSRC_DATA);

  if (appsink != NULL) {
    kms_remove_element_from_bin (GST_BIN (self->priv->pipeline), appsink);
  }

  if (appsrc != NULL) {
    kms_remove_element_from_bin (GST_BIN (self), appsrc);
  }
}

static void
kms_player_endpoint_stopped (KmsUriEndpoint * obj)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (obj);

  /* Set internal pipeline to NULL */
  gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
  BASE_TIME_LOCK (self);
  g_object_set_data (G_OBJECT (self), BASE_TIME_DATA, NULL);
  BASE_TIME_UNLOCK (self);

  KMS_URI_ENDPOINT_GET_CLASS (self)->change_state (KMS_URI_ENDPOINT (self),
      KMS_URI_ENDPOINT_STATE_STOP);
}

static void
kms_player_endpoint_started (KmsUriEndpoint * obj)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (obj);

  /* Set uri property in uridecodebin */
  g_object_set (G_OBJECT (self->priv->uridecodebin), "uri",
      KMS_URI_ENDPOINT (self)->uri, NULL);

  /* Set internal pipeline to playing */
  gst_element_set_state (self->priv->pipeline, GST_STATE_PLAYING);

  KMS_URI_ENDPOINT_GET_CLASS (self)->change_state (KMS_URI_ENDPOINT (self),
      KMS_URI_ENDPOINT_STATE_START);
}

static void
kms_player_endpoint_paused (KmsUriEndpoint * obj)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (obj);

  /* Set internal pipeline to paused */
  gst_element_set_state (self->priv->pipeline, GST_STATE_PAUSED);

  KMS_URI_ENDPOINT_GET_CLASS (self)->change_state (KMS_URI_ENDPOINT (self),
      KMS_URI_ENDPOINT_STATE_PAUSE);
}

static void
kms_player_endpoint_class_init (KmsPlayerEndpointClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  KmsUriEndpointClass *urienpoint_class = KMS_URI_ENDPOINT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "PlayerEndpoint", "Sink/Generic", "Kurento plugin player end point",
      "Joaquin Mengual García <kini.mengual@gmail.com>");

  gobject_class->dispose = kms_player_endpoint_dispose;
  gobject_class->set_property = kms_player_endpoint_set_property;
  gobject_class->get_property = kms_player_endpoint_get_property;

  urienpoint_class->stopped = kms_player_endpoint_stopped;
  urienpoint_class->started = kms_player_endpoint_started;
  urienpoint_class->paused = kms_player_endpoint_paused;

  g_object_class_install_property (gobject_class, PROP_USE_ENCODED_MEDIA,
      g_param_spec_boolean ("use-encoded-media", "use encoded media",
          "The element uses encoded media instead of raw media. This mode "
          "could have an unexpected behaviour if key frames are lost",
          FALSE, G_PARAM_READWRITE | GST_PARAM_MUTABLE_READY));

  kms_player_endpoint_signals[SIGNAL_EOS] =
      g_signal_new ("eos",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsPlayerEndpointClass, eos_signal), NULL, NULL,
      g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

  kms_player_endpoint_signals[SIGNAL_INVALID_URI] =
      g_signal_new ("invalid-uri",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsPlayerEndpointClass, invalid_uri_signal), NULL, NULL,
      g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

  kms_player_endpoint_signals[SIGNAL_INVALID_MEDIA] =
      g_signal_new ("invalid-media",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsPlayerEndpointClass, invalid_media_signal), NULL,
      NULL, g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsPlayerEndpointPrivate));
}

static gboolean
kms_player_endpoint_emit_EOS_signal (gpointer data)
{
  GST_DEBUG ("Emit EOS Signal");
  kms_player_endpoint_stopped (KMS_URI_ENDPOINT (data));
  g_signal_emit (G_OBJECT (data), kms_player_endpoint_signals[SIGNAL_EOS], 0);

  return G_SOURCE_REMOVE;
}

static gboolean
kms_player_endpoint_emit_invalid_uri_signal (gpointer data)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (data);

  GST_DEBUG ("Emit invalid uri signal");
  g_signal_emit (G_OBJECT (self),
      kms_player_endpoint_signals[SIGNAL_INVALID_URI], 0);

  return G_SOURCE_REMOVE;
}

static gboolean
kms_player_endpoint_emit_invalid_media_signal (gpointer data)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (data);

  GST_DEBUG ("Emit invalid media signal");
  g_signal_emit (G_OBJECT (self),
      kms_player_endpoint_signals[SIGNAL_INVALID_MEDIA], 0);

  return G_SOURCE_REMOVE;
}

static gboolean
kms_player_endpoint_post_media_error (gpointer data)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (data);

  GST_ELEMENT_ERROR (self, STREAM, FORMAT, ("Wrong video format"), (NULL));

  return G_SOURCE_REMOVE;
}

static GstBusSyncReply
bus_sync_signal_handler (GstBus * bus, GstMessage * msg, gpointer data)
{
  KmsPlayerEndpoint *self = KMS_PLAYER_ENDPOINT (data);

  if (GST_MESSAGE_TYPE (msg) == GST_MESSAGE_EOS) {
    kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_HIGH_IDLE,
        kms_player_endpoint_emit_EOS_signal, g_object_ref (self),
        g_object_unref);
  } else if (GST_MESSAGE_TYPE (msg) == GST_MESSAGE_ERROR) {

    if (g_str_has_prefix (GST_OBJECT_NAME (msg->src), "decodebin")) {
      kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_HIGH_IDLE,
          kms_player_endpoint_emit_invalid_media_signal, g_object_ref (self),
          g_object_unref);
    } else if (g_strcmp0 (GST_OBJECT_NAME (msg->src), "source") == 0) {
      kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_HIGH_IDLE,
          kms_player_endpoint_emit_invalid_uri_signal, g_object_ref (self),
          g_object_unref);
    } else {
      kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_HIGH_IDLE,
          kms_player_endpoint_post_media_error, g_object_ref (self),
          g_object_unref);
    }
  }
  return GST_BUS_PASS;
}

static void
kms_player_endpoint_init (KmsPlayerEndpoint * self)
{
  GstBus *bus;

  self->priv = KMS_PLAYER_ENDPOINT_GET_PRIVATE (self);

  g_mutex_init (&self->priv->base_time_lock);

  g_object_set (self, "do-synchronization", TRUE, NULL);

  self->priv->loop = kms_loop_new ();
  self->priv->pipeline = gst_pipeline_new ("pipeline");
  self->priv->uridecodebin =
      gst_element_factory_make ("uridecodebin", URIDECODEBIN);

  g_object_set (self->priv->pipeline, "async-handling", TRUE, NULL);
  gst_bin_add (GST_BIN (self->priv->pipeline), self->priv->uridecodebin);

  bus = gst_pipeline_get_bus (GST_PIPELINE (self->priv->pipeline));
  gst_bus_set_sync_handler (bus, bus_sync_signal_handler, self, NULL);
  g_object_unref (bus);

  /* Connect to signals */
  g_signal_connect (self->priv->uridecodebin, "pad-added",
      G_CALLBACK (pad_added), self);
  g_signal_connect (self->priv->uridecodebin, "pad-removed",
      G_CALLBACK (pad_removed), self);
}

gboolean
kms_player_endpoint_plugin_init (GstPlugin * plugin)
{

  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_PLAYER_ENDPOINT);
}
