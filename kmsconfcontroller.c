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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>

#include "kms-marshal.h"
#include "kmsconfcontroller.h"
#include "kmselement.h"
#include "kmsrecordingprofile.h"
#include "kms-enumtypes.h"
#include "kmsloop.h"

#define KMS_TEMP_TEMPLATE "/tmp/kurento-XXXXXX"

#define DEFAULT_RECORDING_PROFILE KMS_RECORDING_PROFILE_WEBM
#define DEFAULT_HAS_DATA_VALUE FALSE

#define AUDIO_APPSINK "audio_appsink"
#define VIDEO_APPSINK "video_appsink"

#define KEY_DESTINATION_PAD_NAME "kms-pad-key-destination-pad-name"
#define KEY_USE_DVR "kms-use_dvr"
#define KEY_PAD_PROBE_ID "kms-pad-key-probe-id"
#define KEY_APP_SINK "kms-key_app_sink"

#define NAME "confcontroller"

GST_DEBUG_CATEGORY_STATIC (kms_conf_controller_debug_category);
#define GST_CAT_DEFAULT kms_conf_controller_debug_category

G_DEFINE_TYPE_WITH_CODE (KmsConfController, kms_conf_controller,
    G_TYPE_OBJECT,
    GST_DEBUG_CATEGORY_INIT (kms_conf_controller_debug_category, NAME,
        0, "debug category for configuration controller"));

#define KMS_CONF_CONTROLLER_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (     \
    (obj),                          \
    KMS_TYPE_CONF_CONTROLLER,       \
    KmsConfControllerPrivate        \
  )                                 \
)

typedef enum
{
  UNCONFIGURED,
  CONFIGURING,
  WAIT_PENDING,
  CONFIGURED
} ControllerState;

struct config_data
{
  guint padblocked;
  guint pendingpadsblocked;
  GSList *blockedpads;
  GSList *pendingvalves;
};

struct _KmsConfControllerPrivate
{
  KmsLoop *loop;
  KmsElement *element;
  GstElement *encodebin;
  GstElement *queue;
  GstPipeline *pipeline;
  GstElement *sink;
  KmsRecordingProfile profile;
  ControllerState state;
  gboolean has_data;
  gboolean use_dvr;
  GSList *pads;
  struct config_data *confdata;
};

/* Object properties */
enum
{
  PROP_0,
  PROP_DVR,
  PROP_ELEMENT,
  PROP_HAS_DATA,
  PROP_PIPELINE,
  PROP_PROFILE,
  PROP_SINK,
  N_PROPERTIES
};

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

/* signals */
enum
{
  MATCHED_ELEMENTS,
  SINK_REQUIRED,
  SINK_UNREQUIRED,
  LAST_SIGNAL
};

static guint obj_signals[LAST_SIGNAL] = { 0 };

struct config_valve
{
  GstElement *valve;
  const gchar *sinkname;
  const gchar *srcname;
  const gchar *destpadname;
};

static void
destroy_configuration_data (gpointer data)
{
  g_slice_free (struct config_valve, data);
}

static void
destroy_gboolean (gpointer data)
{
  g_slice_free (gboolean, data);
}

static void
destroy_ulong (gpointer data)
{
  g_slice_free (gulong, data);
}

static struct config_valve *
create_configuration_data (GstElement * valve, const gchar * sinkname,
    const gchar * srcname, const gchar * destpadname)
{
  struct config_valve *conf;

  conf = g_slice_new0 (struct config_valve);

  conf->valve = valve;
  conf->sinkname = sinkname;
  conf->srcname = srcname;
  conf->destpadname = destpadname;

  return conf;
}

static GstPadProbeReturn
fake_seek_support (GstPad * pad, GstPadProbeInfo * info, gpointer data)
{
  GstEvent *event = gst_pad_probe_info_get_event (info);

  if (GST_EVENT_TYPE (event) == GST_EVENT_SEEK) {
    GST_INFO ("Seek event received, dropping: %" GST_PTR_FORMAT, event);
    return GST_PAD_PROBE_DROP;
  }

  return GST_PAD_PROBE_OK;
}

static gboolean
fake_query_func (GstPad * pad, GstObject * parent, GstQuery * query)
{
  if (GST_QUERY_TYPE (query) == GST_QUERY_SEEKING) {
    gst_query_set_seeking (query, GST_FORMAT_BYTES, TRUE,
        G_GUINT64_CONSTANT (0), GST_CLOCK_TIME_NONE);

    return TRUE;
  } else if (GST_QUERY_TYPE (query) | GST_QUERY_TYPE_UPSTREAM) {
    return gst_pad_peer_query (pad, query);
  } else {
    return FALSE;
  }
}

static void
kms_conf_controller_set_sink (KmsConfController * self, GstElement * sink)
{
  GstPad *pad;

  if (self->priv->pipeline == NULL) {
    GST_ERROR_OBJECT (self, "Not internal pipeline provided");
    return;
  }

  self->priv->queue = gst_element_factory_make ("queue2", NULL);
  g_object_set (self->priv->queue, "temp-template", KMS_TEMP_TEMPLATE,
      "max-size-buffers", 0, "max-size-bytes", 0, "max-size-time",
      G_GUINT64_CONSTANT (0), NULL);
  gst_bin_add (GST_BIN (self->priv->pipeline), self->priv->queue);
  gst_element_sync_state_with_parent (self->priv->queue);

  GST_DEBUG ("Added queue %s", GST_ELEMENT_NAME (self->priv->queue));

  gst_bin_add (GST_BIN (self->priv->pipeline), sink);
  gst_element_sync_state_with_parent (sink);

  GST_DEBUG ("Added sink %s", GST_ELEMENT_NAME (sink));

  if (!gst_element_link_many (self->priv->encodebin, self->priv->queue, sink,
          NULL)) {
    GST_ERROR ("Could not link elements: %s, %s, %s",
        GST_ELEMENT_NAME (self->priv->encodebin),
        GST_ELEMENT_NAME (self->priv->queue), GST_ELEMENT_NAME (sink));
    return;
  }

  self->priv->sink = gst_object_ref (sink);

  if (self->priv->profile != KMS_RECORDING_PROFILE_MP4)
    return;

  /* As mp4mux does not work unless the sink supports seeks, as it */
  /* is configured for fragment output it won't really need to seek */
  pad = gst_element_get_static_pad (sink, "sink");

  gst_pad_add_probe (pad, GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM,
      fake_seek_support, NULL, NULL);

  pad->queryfunc = fake_query_func;

  g_object_unref (pad);
}

static gboolean
kms_query_duration (GstPad * pad, GstObject * parent, GstQuery * query)
{
  gboolean *use_dvr;

  use_dvr = g_object_get_data (G_OBJECT (pad), KEY_USE_DVR);

  if (!(*use_dvr) || GST_QUERY_TYPE (query) != GST_QUERY_DURATION)
    return gst_pad_query_default (pad, parent, query);

  GST_LOG ("Using live-DVR. Setting maximum duration");

  gst_query_set_duration (query, GST_FORMAT_TIME, G_MAXINT64);
  return TRUE;
}

static void
kms_configure_DVR (KmsConfController * self, GstElement * appsrc)
{
  GstPad *srcpad;
  gboolean *use_dvr;

  srcpad = gst_element_get_static_pad (appsrc, "src");
  use_dvr = g_slice_new (gboolean);
  *use_dvr = self->priv->use_dvr;

  g_object_set_data_full (G_OBJECT (srcpad), KEY_USE_DVR, use_dvr,
      destroy_gboolean);

  self->priv->pads = g_slist_prepend (self->priv->pads, srcpad);

  gst_pad_set_query_function (srcpad, kms_query_duration);
  gst_object_unref (srcpad);
}

static void
set_DVR (gpointer data, gpointer user_data)
{
  KmsConfController *self = KMS_CONF_CONTROLLER (user_data);
  gboolean *use_dvr;

  use_dvr = g_object_get_data (G_OBJECT (data), KEY_USE_DVR);
  *use_dvr = self->priv->use_dvr;
}

static void
kms_conf_controller_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsConfController *self = KMS_CONF_CONTROLLER (object);

  switch (property_id) {
    case PROP_DVR:
      self->priv->use_dvr = g_value_get_boolean (value);
      g_slist_foreach (self->priv->pads, set_DVR, self);
      break;
    case PROP_ELEMENT:{
      KmsElement *element = g_value_get_object (value);

      if (!KMS_IS_ELEMENT (element)) {
        GST_ERROR_OBJECT (self, "Element %" GST_PTR_FORMAT
            " is not a kmselement", self->priv->element);
        return;
      }

      self->priv->element = element;
      break;
    }
    case PROP_HAS_DATA:
      self->priv->has_data = g_value_get_boolean (value);
      break;
    case PROP_PIPELINE:{
      GstElement *element = g_value_get_object (value);

      if (!GST_IS_PIPELINE (element)) {
        GST_ERROR_OBJECT (self, "Element %" GST_PTR_FORMAT
            " is not a GstPipeline", self->priv->element);
        return;
      }

      self->priv->pipeline = gst_object_ref (g_value_get_object (value));
      break;
    }
    case PROP_PROFILE:
      self->priv->profile = g_value_get_enum (value);
      break;
    case PROP_SINK:{
      GstElement *element = g_value_get_object (value);

      if (!GST_IS_ELEMENT (element)) {
        GST_ERROR_OBJECT (self, "Element %" GST_PTR_FORMAT
            " is not a GstElement", self->priv->element);
        return;
      }

      if (self->priv->sink != NULL) {
        GST_ERROR_OBJECT (self, "Sink %" GST_PTR_FORMAT
            " is already configured", self->priv->sink);
        return;
      }

      kms_conf_controller_set_sink (self, element);
      break;
    }
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_conf_controller_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsConfController *self = KMS_CONF_CONTROLLER (object);

  switch (property_id) {
    case PROP_DVR:
      g_value_set_boolean (value, self->priv->use_dvr);
      break;
    case PROP_HAS_DATA:
      g_value_set_boolean (value, self->priv->has_data);
      break;
    case PROP_PROFILE:
      g_value_set_enum (value, self->priv->profile);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_conf_controller_add_appsink (KmsConfController * self,
    struct config_valve *conf)
{
  GstElement *appsink;

  GST_DEBUG ("Adding appsink %s", conf->sinkname);

  appsink = gst_element_factory_make ("appsink", conf->sinkname);

  g_object_set (appsink, "emit-signals", TRUE, NULL);
  g_object_set (appsink, "async", FALSE, NULL);
  g_object_set (appsink, "sync", FALSE, NULL);
  g_object_set (appsink, "qos", TRUE, NULL);

  gst_bin_add (GST_BIN (self->priv->element), appsink);
  gst_element_sync_state_with_parent (appsink);
}

static void
kms_conf_controller_connect_valve_to_appsink (KmsConfController * self,
    struct config_valve *conf)
{
  GstElement *appsink;

  appsink = gst_bin_get_by_name (GST_BIN (self->priv->element), conf->sinkname);
  if (appsink == NULL) {
    GST_ERROR ("No appsink %s found", conf->sinkname);
    return;
  }

  GST_DEBUG ("Connecting %s to %s", GST_ELEMENT_NAME (conf->valve),
      GST_ELEMENT_NAME (appsink));

  if (!gst_element_link (conf->valve, appsink)) {
    GST_ERROR ("Could not link %s to %s", GST_ELEMENT_NAME (conf->valve),
        GST_ELEMENT_NAME (appsink));
  }

  g_object_unref (appsink);
}

static void
kms_conf_controller_connect_appsink_to_appsrc (KmsConfController * self,
    struct config_valve *conf)
{
  GstElement *appsink, *appsrc;

  appsink = gst_bin_get_by_name (GST_BIN (self->priv->element), conf->sinkname);
  if (appsink == NULL) {
    GST_ERROR ("No appsink %s found", conf->sinkname);
    return;
  }

  appsrc = gst_element_factory_make ("appsrc", conf->srcname);
  g_object_set_data (G_OBJECT (appsrc), KEY_DESTINATION_PAD_NAME,
      (gpointer) conf->destpadname);

  g_object_set (G_OBJECT (appsrc), "is-live", TRUE, "do-timestamp", FALSE,
      "min-latency", G_GUINT64_CONSTANT (0), "max-latency",
      G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME, NULL);

  gst_bin_add (GST_BIN (self->priv->pipeline), appsrc);
  gst_element_sync_state_with_parent (appsrc);

  g_signal_emit (G_OBJECT (self), obj_signals[MATCHED_ELEMENTS], 0, appsink,
      appsrc);

  GST_DEBUG ("Connected %s to %s", GST_ELEMENT_NAME (appsink),
      GST_ELEMENT_NAME (appsrc));

  g_object_set_data_full (G_OBJECT (appsrc), KEY_APP_SINK,
      g_object_ref (appsink), g_object_unref);

  g_object_unref (appsink);
}

static void
kms_conf_controller_connect_appsrc_to_encodebin (KmsConfController * self,
    struct config_valve *conf)
{
  GstElement *appsrc;

  appsrc = gst_bin_get_by_name (GST_BIN (self->priv->pipeline), conf->srcname);
  if (appsrc == NULL) {
    GST_ERROR ("No appsrc %s found", conf->srcname);
    return;
  }

  kms_configure_DVR (self, appsrc);

  GST_DEBUG ("Connecting %s to %s (%s)", GST_ELEMENT_NAME (appsrc),
      GST_ELEMENT_NAME (self->priv->encodebin), conf->destpadname);

  if (!gst_element_link_pads (appsrc, "src", self->priv->encodebin,
          conf->destpadname)) {
    GST_DEBUG ("Connecting %s to %s (%s)", GST_ELEMENT_NAME (appsrc),
        GST_ELEMENT_NAME (self->priv->encodebin), conf->destpadname);
  }

  g_object_unref (appsrc);
}

static void
kms_conf_controller_set_profile_to_encodebin (KmsConfController * self)
{
  gboolean has_audio, has_video;
  GstEncodingContainerProfile *cprof;
  const GList *profiles, *l;

  has_video = kms_element_get_video_valve (KMS_ELEMENT (self->priv->element))
      != NULL;
  has_audio = kms_element_get_audio_valve (KMS_ELEMENT (self->priv->element))
      != NULL;

  cprof =
      kms_recording_profile_create_profile (self->priv->profile, has_audio,
      has_video);

  profiles = gst_encoding_container_profile_get_profiles (cprof);

  for (l = profiles; l != NULL; l = l->next) {
    GstEncodingProfile *prof = l->data;
    GstCaps *caps;
    const gchar *appsink_name;
    GstElement *appsink;

    if (GST_IS_ENCODING_AUDIO_PROFILE (prof))
      appsink_name = AUDIO_APPSINK;
    else if (GST_IS_ENCODING_VIDEO_PROFILE (prof))
      appsink_name = VIDEO_APPSINK;
    else
      continue;

    appsink = gst_bin_get_by_name (GST_BIN (self->priv->element), appsink_name);

    if (appsink == NULL)
      continue;

    caps = gst_encoding_profile_get_input_caps (prof);

    g_object_set (G_OBJECT (appsink), "caps", caps, NULL);

    g_object_unref (appsink);

    gst_caps_unref (caps);
  }

  g_object_set (G_OBJECT (self->priv->encodebin), "profile", cprof,
      "audio-jitter-tolerance", 100 * GST_MSECOND,
      "avoid-reencoding", TRUE, NULL);
  gst_encoding_profile_unref (cprof);

  if (self->priv->use_dvr)
    return;

  if (self->priv->profile == KMS_RECORDING_PROFILE_MP4) {
    GstElement *mux =
        gst_bin_get_by_name (GST_BIN (self->priv->encodebin), "muxer");

    g_object_set (G_OBJECT (mux), "fragment-duration", 2000, "streamable", TRUE,
        NULL);

    g_object_unref (mux);
  } else if (self->priv->profile == KMS_RECORDING_PROFILE_WEBM) {
    GstElement *mux =
        gst_bin_get_by_name (GST_BIN (self->priv->encodebin), "muxer");

    g_object_set (G_OBJECT (mux), "streamable", TRUE, NULL);

    g_object_unref (mux);
  }
}

static void
kms_conf_controller_free_config_data (KmsConfController * self)
{
  if (self->priv->confdata == NULL)
    return;

  g_slist_free (self->priv->confdata->blockedpads);
  g_slist_free_full (self->priv->confdata->pendingvalves,
      destroy_configuration_data);

  g_slice_free (struct config_data, self->priv->confdata);

  self->priv->confdata = NULL;
}

static void
kms_conf_controller_init_config_data (KmsConfController * self)
{
  if (self->priv->confdata != NULL) {
    GST_WARNING ("Configuration data is not empty.");
    kms_conf_controller_free_config_data (self);
  }

  self->priv->confdata = g_slice_new0 (struct config_data);
}

static void
add_pending_appsinks (gpointer data, gpointer user_data)
{
  KmsConfController *self = KMS_CONF_CONTROLLER (user_data);
  struct config_valve *config = data;

  kms_conf_controller_add_appsink (self, config);
}

static void
connect_pending_valves_to_appsinks (gpointer data, gpointer user_data)
{
  KmsConfController *self = KMS_CONF_CONTROLLER (user_data);
  struct config_valve *config = data;

  kms_conf_controller_connect_valve_to_appsink (self, config);
}

static void
connect_pending_appsinks_to_appsrcs (gpointer data, gpointer user_data)
{
  KmsConfController *self = KMS_CONF_CONTROLLER (user_data);
  struct config_valve *config = data;

  kms_conf_controller_connect_appsink_to_appsrc (self, config);
}

static void
connect_pending_appsrcs_to_encodebin (gpointer data, gpointer user_data)
{
  KmsConfController *self = KMS_CONF_CONTROLLER (user_data);
  struct config_valve *config = data;

  kms_conf_controller_connect_appsrc_to_encodebin (self, config);
}

static void
kms_conf_controller_reconnect_pads (KmsConfController * self, GSList * pads)
{
  GSList *e;

  for (e = pads; e != NULL; e = e->next) {
    GstPad *srcpad = e->data;
    GstElement *appsrc = gst_pad_get_parent_element (srcpad);
    gchar *destpad = g_object_get_data (G_OBJECT (appsrc),
        KEY_DESTINATION_PAD_NAME);

    GST_DEBUG ("Relinking pad %" GST_PTR_FORMAT " to %s", srcpad,
        GST_ELEMENT_NAME (self->priv->encodebin));

    if (!gst_element_link_pads (appsrc, "src", self->priv->encodebin, destpad)) {
      GST_ERROR ("Could not link srcpad %" GST_PTR_FORMAT " to %s", srcpad,
          GST_ELEMENT_NAME (self->priv->encodebin));
    }

    gst_object_unref (appsrc);
  }
}

static void
kms_conf_controller_unblock_pads (KmsConfController * self, GSList * pads)
{
  GSList *e;

  for (e = pads; e != NULL; e = e->next) {
    GstStructure *s;
    GstEvent *force_key_unit_event;
    GstPad *srcpad = e->data;
    GstElement *appsrc = GST_ELEMENT (GST_OBJECT_PARENT (srcpad));
    GstElement *appsink = g_object_get_data (G_OBJECT (appsrc), KEY_APP_SINK);
    GstPad *sinkpad = gst_element_get_static_pad (appsink, "sink");
    gulong *probe_id = g_object_get_data (G_OBJECT (srcpad), KEY_PAD_PROBE_ID);

    if (probe_id != NULL) {
      GST_DEBUG ("Remove probe in pad %" GST_PTR_FORMAT, srcpad);
      gst_pad_remove_probe (srcpad, *probe_id);
      g_object_set_data_full (G_OBJECT (srcpad), KEY_PAD_PROBE_ID, NULL, NULL);
    }

    /* Request key frame */
    s = gst_structure_new ("GstForceKeyUnit", "all-headers", G_TYPE_BOOLEAN,
        TRUE, NULL);
    force_key_unit_event = gst_event_new_custom (GST_EVENT_CUSTOM_UPSTREAM, s);
    GST_DEBUG_OBJECT (sinkpad, "Request key frame.");
    gst_pad_push_event (sinkpad, force_key_unit_event);
    g_object_unref (sinkpad);
  }
}

static void
unlock_pending_valves (gpointer data, gpointer user_data)
{
  struct config_valve *config = data;
  gulong *probe_id;
  GstPad *srcpad;

  srcpad = gst_element_get_static_pad (config->valve, "src");
  probe_id = g_object_get_data (G_OBJECT (srcpad), KEY_PAD_PROBE_ID);

  if (probe_id != NULL) {
    GST_DEBUG ("Remove probe in pad %" GST_PTR_FORMAT, srcpad);
    gst_pad_remove_probe (srcpad, *probe_id);
    g_object_set_data_full (G_OBJECT (srcpad), KEY_PAD_PROBE_ID, NULL, NULL);
  }

  g_object_unref (srcpad);
}

static void
kms_conf_controller_reconfigure_pipeline (KmsConfController * self)
{
  gst_element_unlink_many (self->priv->encodebin, self->priv->queue,
      self->priv->sink, NULL);

  /* Remove old encodebin and sink elements */
  gst_element_set_locked_state (self->priv->queue, TRUE);
  gst_element_set_state (self->priv->queue, GST_STATE_NULL);
  gst_bin_remove (GST_BIN (self->priv->pipeline), self->priv->queue);

  gst_element_set_locked_state (self->priv->sink, TRUE);
  gst_element_set_state (self->priv->sink, GST_STATE_NULL);
  gst_bin_remove (GST_BIN (self->priv->pipeline), self->priv->sink);
  g_clear_object (&self->priv->sink);

  gst_element_set_locked_state (self->priv->encodebin, TRUE);
  gst_element_set_state (self->priv->encodebin, GST_STATE_NULL);
  gst_bin_remove (GST_BIN (self->priv->pipeline), self->priv->encodebin);

  GST_DEBUG ("Adding New encodebin");
  /* Add the new encodebin to the pipeline */
  self->priv->encodebin = gst_element_factory_make ("encodebin", NULL);
  g_slist_foreach (self->priv->confdata->pendingvalves,
      add_pending_appsinks, self);
  kms_conf_controller_set_profile_to_encodebin (self);
  g_slist_foreach (self->priv->confdata->pendingvalves,
      connect_pending_valves_to_appsinks, self);
  g_slist_foreach (self->priv->confdata->pendingvalves,
      connect_pending_appsinks_to_appsrcs, self);
  gst_bin_add (GST_BIN (self->priv->pipeline), self->priv->encodebin);

  /* Add new sink linked to the new encodebin */
  g_signal_emit (G_OBJECT (self), obj_signals[SINK_REQUIRED], 0);
  gst_element_sync_state_with_parent (self->priv->encodebin);

  /* Reconnect sources pads */
  kms_conf_controller_reconnect_pads (self, self->priv->confdata->blockedpads);

  /* Reconnect pending pads */
  g_slist_foreach (self->priv->confdata->pendingvalves,
      connect_pending_appsrcs_to_encodebin, self);

  /* remove probes to unlock pads */
  kms_conf_controller_unblock_pads (self, self->priv->confdata->blockedpads);
  g_slist_foreach (self->priv->confdata->pendingvalves,
      unlock_pending_valves, self);

  kms_conf_controller_free_config_data (self);
}

static gboolean
kms_conf_controller_do_reconfiguration (gpointer user_data)
{
  KmsConfController *self = KMS_CONF_CONTROLLER (user_data);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self->priv->element));

  kms_conf_controller_reconfigure_pipeline (self);
  self->priv->state = CONFIGURED;
  self->priv->has_data = FALSE;

  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self->priv->element));

  return G_SOURCE_REMOVE;
}

static GstPadProbeReturn
event_probe_cb (GstPad * pad, GstPadProbeInfo * info, gpointer user_data)
{
  KmsConfController *self = KMS_CONF_CONTROLLER (user_data);

  /*drop buffer during reconfiguration stage */
  if (GST_PAD_PROBE_INFO_TYPE (info) & (GST_PAD_PROBE_TYPE_BUFFER |
          GST_PAD_PROBE_TYPE_BUFFER_LIST))
    return GST_PAD_PROBE_DROP;

  if (GST_EVENT_TYPE (GST_PAD_PROBE_INFO_DATA (info)) != GST_EVENT_EOS)
    return GST_PAD_PROBE_OK;

  /* remove the probe first */
  gst_pad_remove_probe (pad, GST_PAD_PROBE_INFO_ID (info));

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self->priv->element));

  /* Old encodebin has been flushed out. It's time to remove it */
  GST_DEBUG ("Element %s flushed out",
      GST_ELEMENT_NAME (self->priv->encodebin));

  if (self->priv->confdata->pendingpadsblocked ==
      g_slist_length (self->priv->confdata->pendingvalves)) {
    GST_DEBUG ("No pad in blocking state");
    /* No more pending valves in blocking state */
    /* so we can remove probes to unlock pads */
    kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_HIGH_IDLE,
        kms_conf_controller_do_reconfiguration, g_object_ref (self),
        g_object_unref);
  } else {
    GST_DEBUG ("Waiting for pads to block");
    self->priv->state = WAIT_PENDING;
  }

  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self->priv->element));

  /* Do not pass the EOS event downstream */
  return GST_PAD_PROBE_DROP;
}

static void
send_eos_to_sink_pads (GstElement * element)
{
  GstIterator *it;
  GValue val = G_VALUE_INIT;
  gboolean done = FALSE;

  it = gst_element_iterate_sink_pads (element);
  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstPad *sinkpad;

        sinkpad = g_value_get_object (&val);
        GST_DEBUG ("Sending event to %" GST_PTR_FORMAT, sinkpad);

        if (!gst_pad_send_event (sinkpad, gst_event_new_eos ()))
          GST_WARNING ("EOS event could not be sent");

        g_value_reset (&val);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR ("Error iterating over %s's sink pads",
            GST_ELEMENT_NAME (element));
      case GST_ITERATOR_DONE:
        g_value_unset (&val);
        done = TRUE;
        break;
    }
  } while (!done);

  gst_iterator_free (it);
}

static GstPadProbeReturn
pad_probe_cb (GstPad * srcpad, GstPadProbeInfo * info, gpointer user_data)
{
  KmsConfController *self = KMS_CONF_CONTROLLER (user_data);
  GstPad *sinkpad;

  GST_DEBUG ("Pad blocked %" GST_PTR_FORMAT, srcpad);
  sinkpad = gst_pad_get_peer (srcpad);

  if (sinkpad == NULL) {
    GST_ERROR ("TODO: This situation should not happen");
    return GST_PAD_PROBE_DROP;
  }

  gst_pad_unlink (srcpad, sinkpad);
  g_object_unref (sinkpad);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self->priv->element));

  self->priv->confdata->blockedpads =
      g_slist_prepend (self->priv->confdata->blockedpads, srcpad);
  if (g_slist_length (self->priv->confdata->blockedpads) ==
      self->priv->confdata->padblocked) {
    GstPad *pad, *peer;
    gulong *probe_id;

    GST_DEBUG ("Encodebin source pads blocked");

    /* Tell objects to reset their settings over this object */
    g_signal_emit (G_OBJECT (self), obj_signals[SINK_UNREQUIRED], 0,
        self->priv->sink);

    /* install new probe for EOS */
    pad = gst_element_get_static_pad (self->priv->queue, "src");
    peer = gst_pad_get_peer (pad);

    probe_id = g_object_get_data (G_OBJECT (peer), KEY_PAD_PROBE_ID);
    if (probe_id != NULL) {
      gst_pad_remove_probe (peer, *probe_id);
      g_object_set_data_full (G_OBJECT (peer), KEY_PAD_PROBE_ID, NULL, NULL);
    }

    gst_pad_add_probe (peer, GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM,
        event_probe_cb, self, NULL);
    g_object_unref (pad);
    g_object_unref (peer);

    /* Flush out encodebin data by sending an EOS in all its sinkpads */
    send_eos_to_sink_pads (self->priv->encodebin);
  }

  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self->priv->element));

  return GST_PAD_PROBE_OK;
}

static void
kms_conf_controller_remove_encodebin (KmsConfController * self)
{
  GstIterator *it;
  GValue val = G_VALUE_INIT;
  gboolean done = FALSE;

  GST_DEBUG ("Blocking encodebin %" GST_PTR_FORMAT, self->priv->encodebin);
  self->priv->confdata->padblocked = 0;

  it = gst_element_iterate_sink_pads (self->priv->encodebin);
  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstPad *sinkpad, *srcpad;

        sinkpad = g_value_get_object (&val);
        srcpad = gst_pad_get_peer (sinkpad);

        if (srcpad != NULL) {
          gulong *probe_id;

          probe_id = g_slice_new0 (gulong);
          *probe_id = gst_pad_add_probe (srcpad,
              GST_PAD_PROBE_TYPE_BLOCK_DOWNSTREAM, pad_probe_cb, self, NULL);
          g_object_set_data_full (G_OBJECT (srcpad), KEY_PAD_PROBE_ID, probe_id,
              destroy_ulong);
          self->priv->confdata->padblocked++;
          g_object_unref (srcpad);
        }

        g_value_reset (&val);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR ("Error iterating over %s's sink pads",
            GST_ELEMENT_NAME (self->priv->encodebin));
      case GST_ITERATOR_DONE:
        g_value_unset (&val);
        done = TRUE;
        break;
    }
  } while (!done);

  gst_iterator_free (it);
}

static void
kms_conf_controller_add_appsrc_pads (KmsConfController * self)
{
  GstIterator *it;
  GValue val = G_VALUE_INIT;
  gboolean done = FALSE;

  it = gst_element_iterate_sink_pads (self->priv->encodebin);
  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstPad *sinkpad, *srcpad;

        sinkpad = g_value_get_object (&val);
        srcpad = gst_pad_get_peer (sinkpad);

        if (srcpad != NULL) {
          self->priv->confdata->blockedpads =
              g_slist_prepend (self->priv->confdata->blockedpads, srcpad);
          g_object_unref (srcpad);
        }

        g_value_reset (&val);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR ("Error iterating over %s's sink pads",
            GST_ELEMENT_NAME (self->priv->encodebin));
      case GST_ITERATOR_DONE:
        g_value_unset (&val);
        done = TRUE;
        break;
    }
  } while (!done);

  gst_iterator_free (it);
}

static gint
compare_configuration_data (gconstpointer a, gconstpointer b)
{
  const GstElement *valve = GST_ELEMENT (b);
  const struct config_valve *conf = a;

  return (conf->valve == valve) ? 0 : -1;
}

static struct config_valve *
kms_conf_controller_get_configuration_from_valve (KmsConfController * self,
    GstElement * valve)
{
  GSList *l;

  l = g_slist_find_custom (self->priv->confdata->pendingvalves, valve,
      compare_configuration_data);

  return l->data;
}

static GstPadProbeReturn
pad_probe_blocked_cb (GstPad * srcpad, GstPadProbeInfo * info,
    gpointer user_data)
{
  KmsConfController *self = KMS_CONF_CONTROLLER (user_data);
  struct config_valve *conf;
  GstElement *valve;

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self->priv->element));

  GST_DEBUG ("Blocked pending pad %" GST_PTR_FORMAT, srcpad);

  self->priv->confdata->pendingpadsblocked++;

  if (self->priv->state != WAIT_PENDING ||
      self->priv->confdata->pendingpadsblocked !=
      g_slist_length (self->priv->confdata->pendingvalves))
    goto end;

  GST_DEBUG ("Reconfiguring internal pipeline");

  valve = gst_pad_get_parent_element (srcpad);
  conf = kms_conf_controller_get_configuration_from_valve (self, valve);

  if (conf == NULL) {
    GST_ERROR ("No configuration found for valve %s",
        GST_ELEMENT_NAME (conf->valve));
    goto end;
  }

  kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_HIGH_IDLE,
      kms_conf_controller_do_reconfiguration, g_object_ref (self),
      g_object_unref);

end:
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self->priv->element));

  return GST_PAD_PROBE_OK;
}

static void
kms_conf_controller_block_valve (KmsConfController * self,
    struct config_valve *conf)
{
  gulong *probe_id;
  GstPad *srcpad;

  GST_DEBUG ("Blocking valve %s", GST_ELEMENT_NAME (conf->valve));
  srcpad = gst_element_get_static_pad (conf->valve, "src");

  probe_id = g_slice_new0 (gulong);
  *probe_id = gst_pad_add_probe (srcpad, GST_PAD_PROBE_TYPE_BLOCK_DOWNSTREAM,
      pad_probe_blocked_cb, self, NULL);
  g_object_set_data_full (G_OBJECT (srcpad), KEY_PAD_PROBE_ID, probe_id,
      destroy_ulong);

  self->priv->confdata->pendingvalves =
      g_slist_prepend (self->priv->confdata->pendingvalves, conf);
  g_object_unref (srcpad);
}

static void
kms_conf_controller_link_valve_impl (KmsConfController * self,
    GstElement * valve, const gchar * sinkname,
    const gchar * srcname, const gchar * destpadname)
{
  struct config_valve *config;

  config = create_configuration_data (valve, sinkname, srcname, destpadname);

  GST_DEBUG_OBJECT (self, "Connecting %s", GST_ELEMENT_NAME (valve));

  switch (self->priv->state) {
    case UNCONFIGURED:
      self->priv->encodebin = gst_element_factory_make ("encodebin", NULL);
      kms_conf_controller_add_appsink (self, config);
      kms_conf_controller_set_profile_to_encodebin (self);
      kms_conf_controller_connect_valve_to_appsink (self, config);
      kms_conf_controller_connect_appsink_to_appsrc (self, config);

      gst_bin_add (GST_BIN (self->priv->pipeline), self->priv->encodebin);

      /* Launch get_sink singal so as to get the sink element that */
      /* is going to be linked to the encodebin */
      g_signal_emit (G_OBJECT (self), obj_signals[SINK_REQUIRED], 0);

      gst_element_sync_state_with_parent (self->priv->encodebin);
      kms_conf_controller_connect_appsrc_to_encodebin (self, config);
      destroy_configuration_data (config);
      self->priv->state = CONFIGURED;
      self->priv->has_data = FALSE;
      break;
    case CONFIGURED:
      kms_conf_controller_init_config_data (self);

      if (self->priv->has_data
          && (GST_STATE (self->priv->encodebin) >= GST_STATE_PAUSED
              || GST_STATE_PENDING (self->priv->encodebin) >= GST_STATE_PAUSED
              || GST_STATE_TARGET (self->priv->encodebin) >=
              GST_STATE_PAUSED)) {
        kms_conf_controller_remove_encodebin (self);
        self->priv->state = CONFIGURING;
      } else {
        kms_conf_controller_add_appsrc_pads (self);
        self->priv->confdata->pendingvalves =
            g_slist_prepend (self->priv->confdata->pendingvalves, config);
        kms_conf_controller_reconfigure_pipeline (self);
        break;
      }
    case CONFIGURING:
    case WAIT_PENDING:
      kms_conf_controller_block_valve (self, config);
      break;
  }
}

static void
kms_conf_controller_dispose (GObject * obj)
{
  KmsConfController *self = KMS_CONF_CONTROLLER (obj);

  g_clear_object (&self->priv->loop);
  g_clear_object (&self->priv->sink);
  g_clear_object (&self->priv->pipeline);

  G_OBJECT_CLASS (kms_conf_controller_parent_class)->dispose (obj);
}

static void
kms_conf_controller_finalize (GObject * obj)
{
  KmsConfController *self = KMS_CONF_CONTROLLER (obj);

  kms_conf_controller_free_config_data (self);
  g_slist_free (self->priv->pads);

  G_OBJECT_CLASS (kms_conf_controller_parent_class)->finalize (obj);
}

static void
kms_conf_controller_class_init (KmsConfControllerClass * klass)
{
  GObjectClass *objclass = G_OBJECT_CLASS (klass);

  objclass->set_property = kms_conf_controller_set_property;
  objclass->get_property = kms_conf_controller_get_property;
  objclass->dispose = kms_conf_controller_dispose;
  objclass->finalize = kms_conf_controller_finalize;

  /* Set public virtual methods */
  klass->link_valve = kms_conf_controller_link_valve_impl;

  /* Install properties */
  obj_properties[PROP_DVR] = g_param_spec_boolean ("live-DVR",
      "Live digital video recorder", "Enables or disbles DVR", FALSE,
      G_PARAM_READWRITE);

  obj_properties[PROP_ELEMENT] = g_param_spec_object ("kmselement",
      "Kurento element",
      "Kurento element", KMS_TYPE_ELEMENT,
      (G_PARAM_CONSTRUCT_ONLY | G_PARAM_WRITABLE));

  obj_properties[PROP_HAS_DATA] = g_param_spec_boolean ("has_data",
      "Has data flag",
      "Flag to indicate if any data has been received",
      DEFAULT_HAS_DATA_VALUE, G_PARAM_READWRITE);

  obj_properties[PROP_PIPELINE] = g_param_spec_object ("pipeline",
      "Internal pipeline",
      "Internal pipeline", GST_TYPE_PIPELINE,
      (G_PARAM_CONSTRUCT_ONLY | G_PARAM_WRITABLE));

  obj_properties[PROP_PROFILE] = g_param_spec_enum ("profile",
      "Recording profile",
      "The profile used for encapsulating the media",
      GST_TYPE_RECORDING_PROFILE, DEFAULT_RECORDING_PROFILE, G_PARAM_READWRITE);

  obj_properties[PROP_SINK] = g_param_spec_object ("sink",
      "Sink element", "Sink element", GST_TYPE_ELEMENT, G_PARAM_WRITABLE);

  g_object_class_install_properties (objclass, N_PROPERTIES, obj_properties);

  obj_signals[MATCHED_ELEMENTS] =
      g_signal_new ("matched-elements",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsConfControllerClass, matched_elements),
      NULL, NULL, __kms_marshal_VOID__OBJECT_OBJECT, G_TYPE_NONE, 2,
      GST_TYPE_ELEMENT, GST_TYPE_ELEMENT);

  obj_signals[SINK_REQUIRED] =
      g_signal_new ("sink-required",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsConfControllerClass, sink_required),
      NULL, NULL, g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

  obj_signals[SINK_UNREQUIRED] =
      g_signal_new ("sink-unrequired",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsConfControllerClass, sink_unrequired),
      NULL, NULL, g_cclosure_marshal_VOID__OBJECT, G_TYPE_NONE, 1,
      GST_TYPE_ELEMENT);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsConfControllerPrivate));
}

static void
kms_conf_controller_init (KmsConfController * self)
{
  self->priv = KMS_CONF_CONTROLLER_GET_PRIVATE (self);
  self->priv->loop = kms_loop_new ();
}

KmsConfController *
kms_conf_controller_new (const char *optname1, ...)
{
  KmsConfController *obj;

  va_list ap;

  va_start (ap, optname1);
  obj = KMS_CONF_CONTROLLER (g_object_new_valist (KMS_TYPE_CONF_CONTROLLER,
          optname1, ap));
  va_end (ap);

  return obj;
}

void
kms_conf_controller_link_valve (KmsConfController * self,
    GstElement * valve, const gchar * sinkname,
    const gchar * srcname, const gchar * destpadname)
{
  g_return_if_fail (KMS_IS_CONF_CONTROLLER (self));

  KMS_CONF_CONTROLLER_GET_CLASS (self)->link_valve (self, valve, sinkname,
      srcname, destpadname);
}
