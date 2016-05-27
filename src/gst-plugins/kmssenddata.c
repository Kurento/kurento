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
#include "kmssenddata.h"

#include <glib/gstdio.h>
#include <commons/kmsagnosticcaps.h>
#include <string.h>

GST_DEBUG_CATEGORY_STATIC (kms_send_data_debug_category);
#define GST_CAT_DEFAULT kms_send_data_debug_category
#define PLUGIN_NAME "kmssenddata"

#define KMS_SEND_DATA_GET_PRIVATE(obj) (                    \
    G_TYPE_INSTANCE_GET_PRIVATE (                           \
        (obj),                                              \
        KMS_TYPE_SEND_DATA,                                 \
        KmsSendDataPrivate                                  \
    )                                                       \
)

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsSendData, kms_send_data,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_send_data_debug_category,
        PLUGIN_NAME, 0, "debug category for kms_send_data element"));

enum
{
  SIGNAL_CONFIGURE_BUS_WATCHER,
  LAST_SIGNAL
};

static guint kms_send_data_signals[LAST_SIGNAL] = { 0 };

struct _KmsSendDataPrivate
{
  GstElement *zbar;
  GstElement *appsrc;
  GstBus *bus;
  guint handler_id;
};

static void
kms_send_data_connect_video (KmsSendData * self, GstElement * agnosticbin)
{
  GstPad *zbar_sink = gst_element_get_static_pad (self->priv->zbar, "sink");

  kms_element_connect_sink_target (KMS_ELEMENT (self), zbar_sink,
      KMS_ELEMENT_PAD_TYPE_VIDEO);
  gst_element_link (self->priv->zbar, agnosticbin);

  g_object_unref (zbar_sink);
}

static void
kms_send_data_connect_audio (KmsSendData * self, GstElement * agnosticbin)
{
  GstPad *target = gst_element_get_static_pad (agnosticbin, "sink");

  kms_element_connect_sink_target (KMS_ELEMENT (self), target,
      KMS_ELEMENT_PAD_TYPE_AUDIO);
  g_object_unref (target);
}

static void
kms_send_data_connect_data (KmsSendData * self, GstElement * tee)
{
  GstCaps *caps;

  caps = gst_caps_from_string (KMS_AGNOSTIC_DATA_CAPS);
  self->priv->appsrc = gst_element_factory_make ("appsrc", NULL);
  gst_bin_add (GST_BIN (self), self->priv->appsrc);
  g_object_set (G_OBJECT (self->priv->appsrc), "is-live", TRUE,
      "caps", caps, "emit-signals", TRUE, "stream-type", 0,
      "format", GST_FORMAT_TIME, NULL);
  gst_caps_unref (caps);

  gst_element_link (self->priv->appsrc, tee);
}

static void
kms_send_data_new_code (KmsSendData * self, guint64 ts, gchar * type,
    gchar * symbol)
{
  GstClockTime running_time, base_time, now;
  GstClock *clock;
  GstBuffer *buffer;
  gchar *buffer_data;
  GstFlowReturn ret;

  if ((clock = GST_ELEMENT_CLOCK (self->priv->appsrc)) == NULL) {
    GST_ERROR_OBJECT (GST_ELEMENT (self), "no clock, we can't sync");
    return;
  }

  buffer_data =
      g_strdup_printf ("Code detected in time %lu, type %s, symbol %s", ts,
      type, symbol);

  buffer = gst_buffer_new_wrapped (buffer_data, strlen (buffer_data));

  base_time = GST_ELEMENT_CAST (self->priv->appsrc)->base_time;

  now = gst_clock_get_time (clock);
  running_time = now - base_time;

  /* Live sources always timestamp their buffers with the running_time of the */
  /* pipeline. This is needed to be able to match the timestamps of different */
  /* live sources in order to synchronize them. */
  GST_BUFFER_PTS (buffer) = running_time;

  g_signal_emit_by_name (self->priv->appsrc, "push-buffer", buffer, &ret);

  if (ret != GST_FLOW_OK) {
    /* something wrong */
    GST_WARNING ("Could not send buffer");
  }

  gst_buffer_unref (buffer);
}

static void
code_received_cb (GstBus * bus, GstMessage * message, gpointer data)
{
  KmsSendData *self = KMS_SEND_DATA (data);

  if (GST_MESSAGE_SRC (message) == GST_OBJECT (self->priv->zbar) &&
      GST_MESSAGE_TYPE (message) == GST_MESSAGE_ELEMENT) {
    const GstStructure *st;
    guint64 ts;
    gchar *type, *symbol;

    st = gst_message_get_structure (message);

    if (g_strcmp0 (gst_structure_get_name (st), "barcode") != 0) {
      return;
    }

    if (!gst_structure_get (st, "timestamp", G_TYPE_UINT64, &ts,
            "type", G_TYPE_STRING, &type, "symbol",
            G_TYPE_STRING, &symbol, NULL)) {
      return;
    }

    kms_send_data_new_code (self, ts, type, symbol);

    g_free (type);
    g_free (symbol);
  }

  return;
}

static void
kms_send_data_add_bus_watcher (KmsSendData * self)
{
  GstElement *parent;

  parent = GST_ELEMENT (gst_element_get_parent (GST_ELEMENT (self)));

  self->priv->bus = gst_pipeline_get_bus (GST_PIPELINE (GST_BIN (parent)));

  self->priv->handler_id =
      g_signal_connect (G_OBJECT (self->priv->bus), "message",
      G_CALLBACK (code_received_cb), self);
  gst_object_unref (parent);
}

static void
kms_send_data_finalize (GObject * object)
{
  KmsSendData *self = KMS_SEND_DATA (object);

  g_signal_handler_disconnect (G_OBJECT (self->priv->bus),
      self->priv->handler_id);

  gst_object_unref (self->priv->bus);
}

static void
kms_send_data_init (KmsSendData * self)
{
  self->priv = KMS_SEND_DATA_GET_PRIVATE (self);

  self->priv->zbar = gst_element_factory_make ("zbar", NULL);

  gst_bin_add (GST_BIN (self), self->priv->zbar);

  g_object_set (G_OBJECT (self->priv->zbar), "qos", FALSE, NULL);

  kms_send_data_connect_video (self,
      kms_element_get_video_agnosticbin (KMS_ELEMENT (self)));
  kms_send_data_connect_audio (self,
      kms_element_get_audio_agnosticbin (KMS_ELEMENT (self)));
  kms_send_data_connect_data (self,
      kms_element_get_data_tee (KMS_ELEMENT (self)));

  gst_element_sync_state_with_parent (self->priv->zbar);
}

static void
kms_send_data_class_init (KmsSendDataClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gobject_class->finalize = kms_send_data_finalize;

  klass->configure_bus_watcher = kms_send_data_add_bus_watcher;

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "KmsSendData", "Generic",
      "Look for qr codes and sends data through data channels",
      "David Fernández López <d.fernandezlop@gmail.com>");

  kms_send_data_signals[SIGNAL_CONFIGURE_BUS_WATCHER] =
      g_signal_new ("configure-bus-watcher",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsSendDataClass, configure_bus_watcher), NULL, NULL,
      g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

  g_type_class_add_private (klass, sizeof (KmsSendDataPrivate));
}

gboolean
kms_send_data_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_SEND_DATA);
}
