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

#define DEFAULT_RECORDING_PROFILE KMS_RECORDING_PROFILE_WEBM
#define DEFAULT_HAS_DATA_VALUE FALSE

#define AUDIO_APPSINK "audio_appsink"
#define VIDEO_APPSINK "video_appsink"

#define KEY_DESTINATION_PAD_NAME "kms-pad-key-destination-pad-name"
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

struct _KmsConfControllerPrivate
{
  KmsElement *element;
  GstElement *encodebin;
  GstPipeline *pipeline;
  GstElement *sink;
  KmsRecordingProfile profile;
  ControllerState state;
  gboolean has_data;
};

/* Object properties */
enum
{
  PROP_0,
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

static void
kms_conf_controller_set_sink (KmsConfController * self, GstElement * sink)
{
  if (self->priv->pipeline == NULL) {
    GST_ERROR_OBJECT (self, "Not internal pipeline provided");
    return;
  }

  gst_bin_add (GST_BIN (self->priv->pipeline), sink);
  gst_element_sync_state_with_parent (sink);

  GST_DEBUG ("Added sink %s", GST_ELEMENT_NAME (sink));

  if (!gst_element_link (self->priv->encodebin, sink)) {
    GST_ERROR ("Could not link %s to %s",
        GST_ELEMENT_NAME (self->priv->encodebin), GST_ELEMENT_NAME (sink));
  }

  self->priv->sink = gst_object_ref (sink);
}

static void
kms_conf_controller_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsConfController *self = KMS_CONF_CONTROLLER (object);

  switch (property_id) {
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
    case CONFIGURING:
    case WAIT_PENDING:
      /* TODO */
      break;
  }
}

static void
kms_conf_controller_dispose (GObject * obj)
{
  KmsConfController *self = KMS_CONF_CONTROLLER (obj);

  GST_DEBUG_OBJECT (self, "Dispose");

  g_clear_object (&self->priv->sink);
  g_clear_object (&self->priv->pipeline);

  G_OBJECT_CLASS (kms_conf_controller_parent_class)->dispose (obj);
}

static void
kms_conf_controller_finalize (GObject * obj)
{
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

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsConfControllerPrivate));
}

static void
kms_conf_controller_init (KmsConfController * self)
{
  self->priv = KMS_CONF_CONTROLLER_GET_PRIVATE (self);
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
