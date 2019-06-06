/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include <commons/kmsutils.h>
#include <commons/kms-core-enumtypes.h>

#include "kmsbasemediamuxer.h"

#define OBJECT_NAME "basemediamuxer"

#define parent_class kms_base_media_muxer_parent_class

GST_DEBUG_CATEGORY_STATIC (kms_base_media_muxer_debug_category);
#define GST_CAT_DEFAULT kms_base_media_muxer_debug_category

G_DEFINE_TYPE_WITH_CODE (KmsBaseMediaMuxer, kms_base_media_muxer, G_TYPE_OBJECT,
    GST_DEBUG_CATEGORY_INIT (kms_base_media_muxer_debug_category, OBJECT_NAME,
        0, "debug category for muxing pipeline object"));

#define HTTP_PROTO "http"
#define HTTPS_PROTO "https"

#define MEGA_BYTES(n) ((n) * 1000000)

enum
{
  PROP_0,
  PROP_URI,
  PROP_PROFILE,
  N_PROPERTIES
};

#define KMA_BASE_MEDIA_MUXER_DEFAULT_URI NULL
#define KMA_BASE_MEDIA_MUXER_DEFAULT_RECORDING_PROFILE KMS_RECORDING_PROFILE_WEBM

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

enum
{
  /* signals */
  SIGNAL_ON_SINK_ADDED,
  LAST_SIGNAL
};

static guint obj_signals[LAST_SIGNAL] = { 0 };

static void
kms_base_media_muxer_finalize (GObject * object)
{
  KmsBaseMediaMuxer *self = KMS_BASE_MEDIA_MUXER (object);

  GST_DEBUG_OBJECT (self, "finalize");

  gst_element_set_state (KMS_BASE_MEDIA_MUXER_GET_PIPELINE (self),
      GST_STATE_NULL);
  g_clear_object (&KMS_BASE_MEDIA_MUXER_GET_PIPELINE (self));
  g_rec_mutex_clear (&self->mutex);
  g_free (self->uri);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

GstStateChangeReturn
kms_base_media_muxer_set_state_impl (KmsBaseMediaMuxer * obj, GstState state)
{
  g_return_val_if_fail (obj != NULL, GST_STATE_CHANGE_FAILURE);

  return gst_element_set_state (KMS_BASE_MEDIA_MUXER_GET_PIPELINE (obj), state);
}

GstState
kms_base_media_muxer_get_state_impl (KmsBaseMediaMuxer * obj)
{
  return GST_STATE (KMS_BASE_MEDIA_MUXER_GET_PIPELINE (obj));
}

GstClock *
kms_base_media_muxer_get_clock_impl (KmsBaseMediaMuxer * obj)
{
  g_return_val_if_fail (obj != NULL, NULL);

  return GST_ELEMENT (KMS_BASE_MEDIA_MUXER_GET_PIPELINE (obj))->clock;
}

GstBus *
kms_base_media_muxer_get_bus_impl (KmsBaseMediaMuxer * obj)
{
  g_return_val_if_fail (obj != NULL, NULL);

  return
      gst_pipeline_get_bus (GST_PIPELINE (KMS_BASE_MEDIA_MUXER_GET_PIPELINE
          (obj)));
}

void
kms_base_media_muxer_dot_file_impl (KmsBaseMediaMuxer * obj)
{
  g_return_if_fail (obj != NULL);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (KMS_BASE_MEDIA_MUXER_GET_PIPELINE
          (obj)), GST_DEBUG_GRAPH_SHOW_ALL,
      GST_ELEMENT_NAME (KMS_BASE_MEDIA_MUXER_GET_PIPELINE (obj)));
}

static GstElement *
kms_base_media_muxer_get_sink_fallback (KmsBaseMediaMuxer * self,
    const gchar * uri)
{
  GstElement *sink = NULL;
  gchar *prot;

  prot = gst_uri_get_protocol (uri);

  if ((g_strcmp0 (prot, HTTP_PROTO) == 0)
      || (g_strcmp0 (prot, HTTPS_PROTO) == 0)) {

    if (kms_is_valid_uri (uri)) {
      /* We use souphttpclientsink */
      sink = gst_element_factory_make ("curlhttpsink", NULL);
      if (sink != NULL) {
        g_object_set (sink, "blocksize", MEGA_BYTES (1), "qos", FALSE,
            "async", FALSE, NULL);
      }
      else {
        GST_ERROR_OBJECT (self, "CURL HTTP plugin not available: curlhttpsink");
      }
    } else {
      GST_ERROR_OBJECT (self, "URL not valid");
    }
  }

  g_free (prot);

  /* Add more if required */
  return sink;
}

static GstElement *
kms_base_media_muxer_get_sink (KmsBaseMediaMuxer * self, const gchar * uri)
{
  GObjectClass *sink_class;
  GstElement *sink = NULL;
  GParamSpec *pspec;
  GError *err = NULL;

  if (uri == NULL) {
    goto no_uri;
  }

  if (!gst_uri_is_valid (uri)) {
    goto invalid_uri;
  }

  sink = gst_element_make_from_uri (GST_URI_SINK, uri, NULL, &err);

  if (sink == NULL) {
    /* Some elements have no URI handling capabilities though they can */
    /* handle them. We try to find such element before failing to attend */
    /* this request */
    sink = kms_base_media_muxer_get_sink_fallback (self, uri);
    if (sink == NULL)
      goto no_sink;
    g_clear_error (&err);
  }

  /* Try to configure the sink element */
  sink_class = G_OBJECT_GET_CLASS (sink);

  pspec = g_object_class_find_property (sink_class, "location");
  if (pspec != NULL && G_PARAM_SPEC_VALUE_TYPE (pspec) == G_TYPE_STRING) {
    if (g_strcmp0 (GST_OBJECT_NAME (gst_element_get_factory (sink)),
            "filesink") == 0) {
      /* Work around for filesink elements */
      gchar *location = gst_uri_get_location (uri);

      GST_DEBUG_OBJECT (sink, "filesink location=%s", location);
      g_object_set (sink, "location", location, NULL);
      g_free (location);
    } else {
      GST_DEBUG_OBJECT (sink, "configuring location=%s", uri);
      g_object_set (sink, "location", uri, NULL);
    }
  }

  goto end;

no_uri:
  {
    GST_ERROR_OBJECT (self, "No URI specified to record to.");
    goto end;
  }
invalid_uri:
  {
    GST_ERROR_OBJECT (self, "Invalid URI \"%s\".", uri);
    g_clear_error (&err);
    goto end;
  }
no_sink:
  {
    /* whoops, could not create the sink element, dig a little deeper to
     * figure out what might be wrong. */
    if (err != NULL && err->code == GST_URI_ERROR_UNSUPPORTED_PROTOCOL) {
      gchar *prot;

      prot = gst_uri_get_protocol (uri);
      if (prot == NULL)
        goto invalid_uri;

      GST_ERROR_OBJECT (self, "No URI handler implemented for \"%s\".", prot);

      g_free (prot);
    } else {
      GST_ERROR_OBJECT (self, "%s", (err) ? err->message :
          "URI was not accepted by any element");
    }

    g_clear_error (&err);
    goto end;
  }
end:
  return sink;
}

static GstElement *
kms_base_media_muxer_create_sink_impl (KmsBaseMediaMuxer * self,
    const gchar * uri)
{
  GstElement *sink;

  sink = kms_base_media_muxer_get_sink (self, uri);

  if (sink == NULL) {
    GST_ERROR_OBJECT (self, "No available sink for uri %s", uri);
    sink = gst_element_factory_make ("fakesink", NULL);
  }

  return sink;
}

static void
kms_base_media_muxer_emit_on_sink_added_impl (KmsBaseMediaMuxer * obj,
    GstElement * sink)
{
  g_signal_emit (obj, obj_signals[SIGNAL_ON_SINK_ADDED], 0, sink);
}

static void
kms_base_media_muxer_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsBaseMediaMuxer *self = KMS_BASE_MEDIA_MUXER (object);

  KMS_BASE_MEDIA_MUXER_LOCK (self);

  switch (property_id) {
    case PROP_URI:
      g_free (self->uri);
      self->uri = g_value_dup_string (value);
      break;
    case PROP_PROFILE:
      self->profile = g_value_get_enum (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }

  KMS_BASE_MEDIA_MUXER_UNLOCK (self);
}

static void
kms_base_media_muxer_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsBaseMediaMuxer *self = KMS_BASE_MEDIA_MUXER (object);

  KMS_BASE_MEDIA_MUXER_LOCK (self);

  switch (property_id) {
    case PROP_URI:
      g_value_set_string (value, self->uri);
      break;
    case PROP_PROFILE:
      g_value_set_enum (value, self->profile);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }

  KMS_BASE_MEDIA_MUXER_UNLOCK (self);
}

static void
kms_base_media_muxer_class_init (KmsBaseMediaMuxerClass * klass)
{
  GObjectClass *objclass = G_OBJECT_CLASS (klass);

  objclass->finalize = kms_base_media_muxer_finalize;
  objclass->set_property = kms_base_media_muxer_set_property;
  objclass->get_property = kms_base_media_muxer_get_property;

  klass->create_sink = kms_base_media_muxer_create_sink_impl;
  klass->emit_on_sink_added = kms_base_media_muxer_emit_on_sink_added_impl;

  klass->set_state = kms_base_media_muxer_set_state_impl;
  klass->get_state = kms_base_media_muxer_get_state_impl;
  klass->get_clock = kms_base_media_muxer_get_clock_impl;
  klass->get_bus = kms_base_media_muxer_get_bus_impl;
  klass->dot_file = kms_base_media_muxer_dot_file_impl;

  obj_properties[PROP_URI] = g_param_spec_string (KMS_BASE_MEDIA_MUXER_URI,
      "uri where the file is located", "Set uri",
      KMA_BASE_MEDIA_MUXER_DEFAULT_URI, G_PARAM_READWRITE);

  obj_properties[PROP_PROFILE] =
      g_param_spec_enum (KMS_BASE_MEDIA_MUXER_PROFILE, "Recording profile",
      "The profile used to encapsulate media", KMS_TYPE_RECORDING_PROFILE,
      KMA_BASE_MEDIA_MUXER_DEFAULT_RECORDING_PROFILE,
      (G_PARAM_CONSTRUCT_ONLY | G_PARAM_READWRITE));

  g_object_class_install_properties (objclass, N_PROPERTIES, obj_properties);

  obj_signals[SIGNAL_ON_SINK_ADDED] =
      g_signal_new ("on-sink-added",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsBaseMediaMuxerClass, on_sink_added), NULL, NULL,
      g_cclosure_marshal_VOID__OBJECT, G_TYPE_NONE, 1, GST_TYPE_ELEMENT);
}

static void
kms_base_media_muxer_init (KmsBaseMediaMuxer * self)
{
  g_rec_mutex_init (&self->mutex);
  KMS_BASE_MEDIA_MUXER_GET_PIPELINE (self) = gst_pipeline_new (NULL);
}

GstStateChangeReturn
kms_base_media_muxer_set_state (KmsBaseMediaMuxer * obj, GstState state)
{
  g_return_val_if_fail (KMS_IS_BASE_MEDIA_MUXER (obj),
      GST_STATE_CHANGE_FAILURE);

  return KMS_BASE_MEDIA_MUXER_GET_CLASS (obj)->set_state (obj, state);
}

GstState
kms_base_media_muxer_get_state (KmsBaseMediaMuxer * obj)
{
  g_return_val_if_fail (KMS_IS_BASE_MEDIA_MUXER (obj), GST_STATE_VOID_PENDING);

  return KMS_BASE_MEDIA_MUXER_GET_CLASS (obj)->get_state (obj);
}

GstClock *
kms_base_media_muxer_get_clock (KmsBaseMediaMuxer * obj)
{
  g_return_val_if_fail (KMS_IS_BASE_MEDIA_MUXER (obj), NULL);

  return KMS_BASE_MEDIA_MUXER_GET_CLASS (obj)->get_clock (obj);
}

GstBus *
kms_base_media_muxer_get_bus (KmsBaseMediaMuxer * obj)
{
  g_return_val_if_fail (KMS_IS_BASE_MEDIA_MUXER (obj), NULL);

  return KMS_BASE_MEDIA_MUXER_GET_CLASS (obj)->get_bus (obj);
}

void
kms_base_media_muxer_dot_file (KmsBaseMediaMuxer * obj)
{
  g_return_if_fail (KMS_IS_BASE_MEDIA_MUXER (obj));

  KMS_BASE_MEDIA_MUXER_GET_CLASS (obj)->dot_file (obj);
}

GstElement *
kms_base_media_muxer_add_src (KmsBaseMediaMuxer * obj, KmsMediaType type,
    const gchar * id)
{
  g_return_val_if_fail (KMS_IS_BASE_MEDIA_MUXER (obj), NULL);

  return KMS_BASE_MEDIA_MUXER_GET_CLASS (obj)->add_src (obj, type, id);
}

gboolean
kms_base_media_muxer_remove_src (KmsBaseMediaMuxer * obj, const gchar * id)
{
  g_return_val_if_fail (KMS_IS_BASE_MEDIA_MUXER (obj), FALSE);

  return KMS_BASE_MEDIA_MUXER_GET_CLASS (obj)->remove_src (obj, id);
}
