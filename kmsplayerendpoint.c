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
#include "kmsutils.h"
#include "kmselement.h"
#include "kmsagnosticcaps.h"
#include "kmsplayerendpoint.h"

#define PLUGIN_NAME "playerendpoint"
#define AUDIO_APPSRC "audio_appsrc"
#define VIDEO_APPSRC "video_appsrc"
#define URIDECODEBIN "uridecodebin"

#define APPSRC_DATA "appsrc_data"
#define APPSINK_DATA "appsink_data"
#define BASE_TIME_DATA "base_time_data"

GST_DEBUG_CATEGORY_STATIC (kms_player_end_point_debug_category);
#define GST_CAT_DEFAULT kms_player_end_point_debug_category

#define KMS_PLAYER_END_POINT_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_PLAYER_END_POINT,                  \
    KmsPlayerEndPointPrivate                    \
  )                                             \
)

#define KMS_PLAYER_END_POINT_GET_LOCK(obj) (               \
  &KMS_PLAYER_END_POINT (obj)->priv->tdata.thread_mutex    \
)

#define KMS_PLAYER_END_POINT_GET_COND(obj) (               \
  &KMS_PLAYER_END_POINT (obj)->priv->tdata.thread_cond     \
)

#define KMS_PLAYER_END_POINT_LOCK(obj) (                   \
  g_mutex_lock (KMS_PLAYER_END_POINT_GET_LOCK (obj))       \
)

#define KMS_PLAYER_END_POINT_UNLOCK(obj) (                 \
  g_mutex_unlock (KMS_PLAYER_END_POINT_GET_LOCK (obj))     \
)

#define KMS_PLAYER_END_POINT_WAIT(obj) (                   \
  g_cond_wait (KMS_PLAYER_END_POINT_GET_COND (obj),        \
    KMS_PLAYER_END_POINT_GET_LOCK (obj))                   \
)

#define KMS_PLAYER_END_POINT_SIGNAL(obj) (                 \
  g_cond_signal (KMS_PLAYER_END_POINT_GET_COND (obj))      \
)

typedef void (*KmsActionFunc) (gpointer user_data);

struct thread_data
{
  GQueue *actions;
  gboolean finish_thread;
  GThread *thread;
  GCond thread_cond;
  GMutex thread_mutex;
};

struct thread_cb_data
{
  KmsActionFunc function;
  gpointer data;
  GDestroyNotify notify;
};

struct _KmsPlayerEndPointPrivate
{
  GstElement *pipeline;
  GstElement *uridecodebin;
  struct thread_data tdata;
};

enum
{
  SIGNAL_EOS,
  SIGNAL_INVALID_URI,
  SIGNAL_INVALID_MEDIA,
  LAST_SIGNAL
};

static guint kms_player_end_point_signals[LAST_SIGNAL] = { 0 };

/* pad templates */

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsPlayerEndPoint, kms_player_end_point,
    KMS_TYPE_URI_END_POINT,
    GST_DEBUG_CATEGORY_INIT (kms_player_end_point_debug_category, PLUGIN_NAME,
        0, "debug category for playerendpoint element"));

static void
destroy_thread_cb_data (gpointer data)
{
  struct thread_cb_data *th_data = data;

  if (th_data->notify)
    th_data->notify (th_data->data);

  g_slice_free (struct thread_cb_data, th_data);
}

static void
kms_player_end_point_add_action (KmsPlayerEndPoint * self,
    KmsActionFunc function, gpointer data, GDestroyNotify notify)
{
  struct thread_cb_data *th_data;

  th_data = g_slice_new0 (struct thread_cb_data);

  th_data->data = data;
  th_data->function = function;
  th_data->notify = notify;

  KMS_PLAYER_END_POINT_LOCK (self);

  g_queue_push_tail (self->priv->tdata.actions, th_data);

  KMS_PLAYER_END_POINT_SIGNAL (self);
  KMS_PLAYER_END_POINT_UNLOCK (self);
}

static void
kms_player_end_point_dispose (GObject * object)
{
  KmsPlayerEndPoint *self = KMS_PLAYER_END_POINT (object);

  if (self->priv->pipeline != NULL) {
    GstBus *bus;

    bus = gst_pipeline_get_bus (GST_PIPELINE (self->priv->pipeline));
    gst_bus_set_sync_handler (bus, NULL, NULL, NULL);
    g_object_unref (bus);

    gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
    gst_object_unref (GST_OBJECT (self->priv->pipeline));
    self->priv->pipeline = NULL;
  }

  /* clean up as possible. May be called multiple times */

  G_OBJECT_CLASS (kms_player_end_point_parent_class)->dispose (object);
}

static void
kms_player_end_point_finalize (GObject * object)
{
  KmsPlayerEndPoint *self = KMS_PLAYER_END_POINT (object);

  /* clean up object here */
  KMS_PLAYER_END_POINT_LOCK (self);
  self->priv->tdata.finish_thread = TRUE;
  KMS_PLAYER_END_POINT_SIGNAL (self);
  KMS_PLAYER_END_POINT_UNLOCK (self);

  g_thread_join (self->priv->tdata.thread);
  g_thread_unref (self->priv->tdata.thread);

  g_cond_clear (&self->priv->tdata.thread_cond);
  g_mutex_clear (&self->priv->tdata.thread_mutex);

  g_queue_free_full (self->priv->tdata.actions, destroy_thread_cb_data);

  G_OBJECT_CLASS (kms_player_end_point_parent_class)->finalize (object);
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

static void
pad_added (GstElement * element, GstPad * pad, KmsPlayerEndPoint * self)
{
  GST_DEBUG ("Pad added");
  GstElement *appsrc, *agnosticbin, *appsink;
  GstPad *sinkpad;
  GstCaps *audio_caps, *video_caps;
  GstCaps *src_caps;

  /* Create and link appsrc--agnosticbin with proper caps */
  audio_caps = gst_caps_from_string (KMS_AGNOSTIC_AUDIO_CAPS);
  video_caps = gst_caps_from_string (KMS_AGNOSTIC_VIDEO_CAPS);
  src_caps = gst_pad_query_caps (pad, NULL);
  GST_TRACE ("caps are %" GST_PTR_FORMAT, src_caps);

  if (gst_caps_can_intersect (audio_caps, src_caps))
    agnosticbin = kms_element_get_audio_agnosticbin (KMS_ELEMENT (self));
  else if (gst_caps_can_intersect (video_caps, src_caps))
    agnosticbin = kms_element_get_video_agnosticbin (KMS_ELEMENT (self));
  else {
    GST_ELEMENT_ERROR (self, CORE, CAPS, ("No agnostic caps provided"),
        GST_ERROR_SYSTEM);
    goto end;
  }

  /* Create appsrc element and link to agnosticbin */
  appsrc = gst_element_factory_make ("appsrc", NULL);
  g_object_set (G_OBJECT (appsrc), "is-live", FALSE, "do-timestamp", FALSE,
      "min-latency", G_GUINT64_CONSTANT (0),
      "max-latency", G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME,
      "caps", src_caps, NULL);

  gst_bin_add (GST_BIN (self), appsrc);
  gst_element_sync_state_with_parent (appsrc);
  if (!gst_element_link (appsrc, agnosticbin)) {
    GST_ERROR ("Could not link %s to element %s", GST_ELEMENT_NAME (appsrc),
        GST_ELEMENT_NAME (agnosticbin));
  }

  /* Create appsink and link to pad */
  appsink = gst_element_factory_make ("appsink", NULL);
  g_object_set (appsink, "sync", TRUE, "enable-last-sample",
      FALSE, "emit-signals", TRUE, NULL);
  gst_bin_add (GST_BIN (self->priv->pipeline), appsink);
  gst_element_sync_state_with_parent (appsink);

  sinkpad = gst_element_get_static_pad (appsink, "sink");
  gst_pad_link (pad, sinkpad);
  GST_DEBUG_OBJECT (self, "Linked %s---%s", GST_ELEMENT_NAME (element),
      GST_ELEMENT_NAME (appsink));
  g_object_unref (sinkpad);

  g_object_set_data (G_OBJECT (pad), APPSRC_DATA, appsrc);
  g_object_set_data (G_OBJECT (pad), APPSINK_DATA, appsink);

  /* Connect new-sample signal to callback */
  g_signal_connect (appsink, "new-sample", G_CALLBACK (new_sample_cb), appsrc);
  g_signal_connect (appsink, "eos", G_CALLBACK (eos_cb), appsrc);

end:
  if (src_caps != NULL)
    gst_caps_unref (src_caps);

  if (audio_caps != NULL)
    gst_caps_unref (audio_caps);

  if (video_caps != NULL)
    gst_caps_unref (video_caps);
}

static void
pad_removed (GstElement * element, GstPad * pad, KmsPlayerEndPoint * self)
{
  GST_DEBUG ("Pad removed");
  GstElement *appsink, *appsrc;

  if (GST_PAD_IS_SINK (pad))
    return;

  appsink = g_object_steal_data (G_OBJECT (pad), APPSINK_DATA);
  appsrc = g_object_steal_data (G_OBJECT (pad), APPSRC_DATA);

  if (appsrc != NULL) {
    GST_INFO ("Removing %" GST_PTR_FORMAT " from its parent", appsrc);
    if (GST_OBJECT_PARENT (appsrc) != NULL) {
      g_object_ref (appsrc);
      gst_bin_remove (GST_BIN (GST_OBJECT_PARENT (appsrc)), appsrc);
      gst_element_set_state (appsrc, GST_STATE_NULL);
      g_object_unref (appsrc);
    }
  }

  if (appsink == NULL) {
    GST_ERROR ("No appsink was found associated with %" GST_PTR_FORMAT, pad);
    return;
  }
  if (!gst_element_set_locked_state (appsink, TRUE))
    GST_ERROR ("Could not block element %s", GST_ELEMENT_NAME (appsink));

  GST_DEBUG ("Removing appsink %s from %s", GST_ELEMENT_NAME (appsink),
      GST_ELEMENT_NAME (self->priv->pipeline));

  gst_element_set_state (appsink, GST_STATE_NULL);
  gst_bin_remove (GST_BIN (self->priv->pipeline), appsink);
}

static void
kms_player_end_point_stopped (KmsUriEndPoint * obj)
{
  KmsPlayerEndPoint *self = KMS_PLAYER_END_POINT (obj);

  /* Set internal pipeline to NULL */
  gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);

  KMS_URI_END_POINT_GET_CLASS (self)->change_state (KMS_URI_END_POINT (self),
      KMS_URI_END_POINT_STATE_STOP);
}

static void
kms_player_end_point_started (KmsUriEndPoint * obj)
{
  KmsPlayerEndPoint *self = KMS_PLAYER_END_POINT (obj);

  /* Set uri property in uridecodebin */
  g_object_set (G_OBJECT (self->priv->uridecodebin), "uri",
      KMS_URI_END_POINT (self)->uri, NULL);

  /* Set internal pipeline to playing */
  gst_element_set_state (self->priv->pipeline, GST_STATE_PLAYING);

  KMS_URI_END_POINT_GET_CLASS (self)->change_state (KMS_URI_END_POINT (self),
      KMS_URI_END_POINT_STATE_START);
}

static void
kms_player_end_point_paused (KmsUriEndPoint * obj)
{
  KmsPlayerEndPoint *self = KMS_PLAYER_END_POINT (obj);

  /* Set internal pipeline to paused */
  gst_element_set_state (self->priv->pipeline, GST_STATE_PAUSED);

  KMS_URI_END_POINT_GET_CLASS (self)->change_state (KMS_URI_END_POINT (self),
      KMS_URI_END_POINT_STATE_PAUSE);
}

static GstPadProbeReturn
catch_eos (GstPad * pad, GstPadProbeInfo * info, gpointer data)
{
  GstEvent *event = GST_PAD_PROBE_INFO_EVENT (info);

  if (GST_EVENT_TYPE (event) == GST_EVENT_EOS) {
    GST_WARNING_OBJECT (pad, "Dropping flush EOS event");

    if (!gst_pad_push_event (pad,
            gst_event_new_custom (GST_EVENT_CUSTOM_DOWNSTREAM,
                gst_structure_new_empty
                (KMS_PLAYERENDPOINT_CUSTOM_EVENT_NAME))))
      GST_ERROR (KMS_PLAYERENDPOINT_CUSTOM_EVENT_NAME
          " event could not be sent");

    return GST_PAD_PROBE_DROP;
  }

  return GST_PAD_PROBE_OK;
}

static GstPad *
kms_player_end_point_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps)
{
  GstPad *ret_pad =
      GST_ELEMENT_CLASS (kms_player_end_point_parent_class)->request_new_pad
      (element, templ, name, caps);

  gst_pad_add_probe (ret_pad, GST_PAD_PROBE_TYPE_EVENT_BOTH, catch_eos, element,
      NULL);

  return ret_pad;
}

static void
kms_player_end_point_class_init (KmsPlayerEndPointClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);
  KmsUriEndPointClass *urienpoint_class = KMS_URI_END_POINT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "PlayerEndPoint", "Sink/Generic", "Kurento plugin player end point",
      "Joaquin Mengual García <kini.mengual@gmail.com>");

  gobject_class->dispose = kms_player_end_point_dispose;
  gobject_class->finalize = kms_player_end_point_finalize;

  urienpoint_class->stopped = kms_player_end_point_stopped;
  urienpoint_class->started = kms_player_end_point_started;
  urienpoint_class->paused = kms_player_end_point_paused;

  gstelement_class->request_new_pad =
      GST_DEBUG_FUNCPTR (kms_player_end_point_request_new_pad);

  kms_player_end_point_signals[SIGNAL_EOS] =
      g_signal_new ("eos",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsPlayerEndPointClass, eos_signal), NULL, NULL,
      g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

  kms_player_end_point_signals[SIGNAL_INVALID_URI] =
      g_signal_new ("invalid-uri",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsPlayerEndPointClass, invalid_uri_signal), NULL, NULL,
      g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

  kms_player_end_point_signals[SIGNAL_INVALID_MEDIA] =
      g_signal_new ("invalid-media",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsPlayerEndPointClass, invalid_media_signal), NULL,
      NULL, g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsPlayerEndPointPrivate));
}

static void
kms_player_end_point_emit_EOS_signal (gpointer data)
{
  KmsPlayerEndPoint *self = KMS_PLAYER_END_POINT (data);

  GST_DEBUG ("Emit EOS Signal");
  g_signal_emit (G_OBJECT (self), kms_player_end_point_signals[SIGNAL_EOS], 0);
}

static void
kms_player_end_point_emit_invalid_uri_signal (gpointer data)
{
  KmsPlayerEndPoint *self = KMS_PLAYER_END_POINT (data);

  GST_DEBUG ("Emit invalid uri signal");
  g_signal_emit (G_OBJECT (self),
      kms_player_end_point_signals[SIGNAL_INVALID_URI], 0);
}

static void
kms_player_end_point_emit_invalid_media_signal (gpointer data)
{
  KmsPlayerEndPoint *self = KMS_PLAYER_END_POINT (data);

  GST_DEBUG ("Emit invalid media signal");
  g_signal_emit (G_OBJECT (self),
      kms_player_end_point_signals[SIGNAL_INVALID_MEDIA], 0);
}

static void
kms_player_end_point_post_media_error (gpointer data)
{
  KmsPlayerEndPoint *self = KMS_PLAYER_END_POINT (data);

  GST_ELEMENT_ERROR (self, STREAM, FORMAT, ("Wrong video format"), (NULL));
}

static GstBusSyncReply
bus_sync_signal_handler (GstBus * bus, GstMessage * msg, gpointer data)
{
  KmsPlayerEndPoint *self = KMS_PLAYER_END_POINT (data);

  if (GST_MESSAGE_TYPE (msg) == GST_MESSAGE_EOS) {
    kms_player_end_point_add_action (self, kms_player_end_point_emit_EOS_signal,
        self, NULL);
  } else if (GST_MESSAGE_TYPE (msg) == GST_MESSAGE_ERROR) {

    if (g_str_has_prefix (GST_OBJECT_NAME (msg->src), "decodebin")) {
      kms_player_end_point_add_action (self,
          kms_player_end_point_emit_invalid_media_signal, self, NULL);
    } else if (g_strcmp0 (GST_OBJECT_NAME (msg->src), "source") == 0) {
      kms_player_end_point_add_action (self,
          kms_player_end_point_emit_invalid_uri_signal, self, NULL);
    } else {
      kms_player_end_point_add_action (self,
          kms_player_end_point_post_media_error, self, NULL);
    }
  }
  return GST_BUS_PASS;
}

static gpointer
kms_player_end_point_thread (gpointer data)
{
  KmsPlayerEndPoint *self = KMS_PLAYER_END_POINT (data);
  struct thread_cb_data *th_data;

  /* Main thread loop */
  while (TRUE) {
    KMS_PLAYER_END_POINT_LOCK (self);
    while (!self->priv->tdata.finish_thread &&
        g_queue_is_empty (self->priv->tdata.actions)) {
      GST_DEBUG_OBJECT (self, "Waiting for elements to remove");
      KMS_PLAYER_END_POINT_WAIT (self);
      GST_DEBUG_OBJECT (self, "Waked up");
    }

    if (self->priv->tdata.finish_thread) {
      KMS_PLAYER_END_POINT_UNLOCK (self);
      break;
    }

    th_data = g_queue_pop_head (self->priv->tdata.actions);

    KMS_PLAYER_END_POINT_UNLOCK (self);

    if (th_data->function)
      th_data->function (th_data->data);

    destroy_thread_cb_data (th_data);
  }

  GST_DEBUG_OBJECT (self, "Thread finished");
  return NULL;
}

static void
kms_player_end_point_init (KmsPlayerEndPoint * self)
{
  GstBus *bus;
  GstCaps *deco_caps;

  self->priv = KMS_PLAYER_END_POINT_GET_PRIVATE (self);

  self->priv->pipeline = gst_pipeline_new ("pipeline");
  self->priv->uridecodebin =
      gst_element_factory_make ("uridecodebin", URIDECODEBIN);

  deco_caps = gst_caps_from_string (KMS_AGNOSTIC_CAPS_CAPS);
  g_object_set (G_OBJECT (self->priv->uridecodebin), "caps", deco_caps, NULL);
  gst_caps_unref (deco_caps);

  gst_bin_add (GST_BIN (self->priv->pipeline), self->priv->uridecodebin);

  bus = gst_pipeline_get_bus (GST_PIPELINE (self->priv->pipeline));
  gst_bus_set_sync_handler (bus, bus_sync_signal_handler, self, NULL);
  g_object_unref (bus);

  /* Connect to signals */
  g_signal_connect (self->priv->uridecodebin, "pad-added",
      G_CALLBACK (pad_added), self);
  g_signal_connect (self->priv->uridecodebin, "pad-removed",
      G_CALLBACK (pad_removed), self);

  self->priv->tdata.actions = g_queue_new ();
  g_cond_init (&self->priv->tdata.thread_cond);
  self->priv->tdata.finish_thread = FALSE;
  self->priv->tdata.thread =
      g_thread_new (GST_OBJECT_NAME (self), kms_player_end_point_thread, self);
}

gboolean
kms_player_end_point_plugin_init (GstPlugin * plugin)
{

  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_PLAYER_END_POINT);
}
