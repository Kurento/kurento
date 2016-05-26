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
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include <commons/kmsutils.h>
#include <commons/kmsloop.h>

#include "kmshttpendpoint.h"
#include "kms-elements-enumtypes.h"

#define PLUGIN_NAME "httpendpoint"

#define BASE_TIME_DATA "base_time_data"

#define GST_CAT_DEFAULT kms_http_endpoint_debug_category
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

/* Object properties */
enum
{
  PROP_0,
  PROP_METHOD,
  PROP_START,
  N_PROPERTIES
};

#define DEFAULT_HTTP_ENDPOINT_START FALSE

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

/* Object signals */
enum
{
  /* signals */
  SIGNAL_EOS,
  LAST_SIGNAL
};

static guint http_ep_signals[LAST_SIGNAL] = { 0 };

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsHttpEndpoint, kms_http_endpoint,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME,
        0, "debug category for httpendpoint element"));

static void
kms_http_endpoint_dispose (GObject * object)
{
  KmsHttpEndpoint *self = KMS_HTTP_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "dispose");

  if (self->pipeline == NULL)
    goto end;

  gst_element_set_state (self->pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (self->pipeline));
  self->pipeline = NULL;

  /* clean up as possible. May be called multiple times */
end:
  G_OBJECT_CLASS (kms_http_endpoint_parent_class)->dispose (object);
}

static void
kms_http_endpoint_finalize (GObject * object)
{
  KmsHttpEndpoint *self = KMS_HTTP_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "finalize");

  /* clean up object here */
  g_mutex_clear (&self->base_time_lock);

  G_OBJECT_CLASS (kms_http_endpoint_parent_class)->finalize (object);
}

static void
kms_http_endpoint_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsHttpEndpoint *self = KMS_HTTP_ENDPOINT (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_START:{
      gboolean start;

      start = g_value_get_boolean (value);

      if (self->start != g_value_get_boolean (value)) {
        KMS_HTTP_ENDPOINT_GET_CLASS (self)->start (self, start);
      }
      break;
    }
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static void
kms_http_endpoint_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsHttpEndpoint *self = KMS_HTTP_ENDPOINT (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_METHOD:
      g_value_set_enum (value, g_atomic_int_get (&self->method));
      break;
    case PROP_START:
      g_value_set_boolean (value, self->start);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static void
kms_http_endpoint_start (KmsHttpEndpoint * self, gboolean start)
{
  GST_WARNING_OBJECT (self, "Not implemented method");
}

static void
kms_http_endpoint_class_init (KmsHttpEndpointClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "HttpEndpoint", "Generic", "Kurento http end point plugin",
      "Santiago Carot-Nemesio <sancane.kurento@gmail.com>");

  gobject_class->set_property = kms_http_endpoint_set_property;
  gobject_class->get_property = kms_http_endpoint_get_property;
  gobject_class->dispose = kms_http_endpoint_dispose;
  gobject_class->finalize = kms_http_endpoint_finalize;

  klass->start = GST_DEBUG_FUNCPTR (kms_http_endpoint_start);

  /* Install properties */
  obj_properties[PROP_METHOD] = g_param_spec_enum ("http-method",
      "Http method",
      "Http method used in requests",
      KMS_TYPE_HTTP_ENDPOINT_METHOD,
      KMS_HTTP_ENDPOINT_METHOD_UNDEFINED, G_PARAM_READABLE);

  obj_properties[PROP_START] = g_param_spec_boolean ("start",
      "start media stream",
      "start media stream", DEFAULT_HTTP_ENDPOINT_START, G_PARAM_READWRITE);

  g_object_class_install_properties (gobject_class,
      N_PROPERTIES, obj_properties);

  /* set signals */
  http_ep_signals[SIGNAL_EOS] =
      g_signal_new ("eos",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsHttpEndpointClass, eos_signal), NULL, NULL,
      g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);
}

static void
kms_http_endpoint_init (KmsHttpEndpoint * self)
{
  g_mutex_init (&self->base_time_lock);

  g_atomic_int_set (&self->method, KMS_HTTP_ENDPOINT_METHOD_UNDEFINED);
  self->pipeline = NULL;
  self->start = FALSE;
}

gboolean
kms_http_endpoint_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_HTTP_ENDPOINT);
}
