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
#include <gst/pbutils/encoding-profile.h>

#include <commons/kms-core-marshal.h>
#include "kmshttpendpoint.h"
#include <commons/kmsagnosticcaps.h>
#include "kms-elements-enumtypes.h"
#include <commons/kms-core-enumtypes.h>
#include "kmsconfcontroller.h"
#include "commons/kmsutils.h"
#include <commons/kmsloop.h>

#define PLUGIN_NAME "httpendpoint"

#define AUDIO_APPSINK "audio_appsink"
#define AUDIO_APPSRC "audio_appsrc"
#define VIDEO_APPSINK "video_appsink"
#define VIDEO_APPSRC "video_appsrc"

#define APPSRC_DATA "appsrc_data"
#define APPSINK_DATA "appsink_data"
#define BASE_TIME_DATA "base_time_data"

#define GET_PIPELINE "get-pipeline"
#define POST_PIPELINE "post-pipeline"

#define GST_CAT_DEFAULT kms_http_endpoint_debug_category
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define KMS_HTTP_ENDPOINT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (               \
    (obj),                                    \
    KMS_TYPE_HTTP_ENDPOINT,                   \
    KmsHttpEndpointPrivate                    \
  )                                           \
)
typedef void (*KmsActionFunc) (gpointer user_data);

typedef struct _GetData GetData;
typedef struct _PostData PostData;

struct remove_data
{
  KmsHttpEndpoint *httpep;
  GstElement *element;
};

struct _PostData
{
  GstElement *appsrc;
};

struct _GetData
{
  GstElement *appsink;
  KmsConfController *controller;
};

struct _KmsHttpEndpointPrivate
{
  gboolean use_encoded_media;
  KmsLoop *loop;
  union
  {
    GetData *get;
    PostData *post;
  };
};

/* Object properties */
enum
{
  PROP_0,
  PROP_METHOD,
  PROP_START,
  N_PROPERTIES
};

#define DEFAULT_HTTP_ENDPOINT_START FALSE
#define DEFAULT_HTTP_ENDPOINT_LIVE TRUE

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

struct config_valve
{
  GstElement *valve;
  const gchar *sinkname;
  const gchar *srcname;
  const gchar *destpadname;
};

struct cb_data
{
  KmsHttpEndpoint *self;
  gboolean start;
};

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

static void kms_change_internal_pipeline_state (KmsHttpEndpoint *, gboolean);

static void
kms_http_endpoint_dispose (GObject * object)
{
  KmsHttpEndpoint *self = KMS_HTTP_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "dispose");

  g_clear_object (&self->priv->loop);

  if (self->pipeline == NULL)
    return;

  gst_element_set_state (self->pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (self->pipeline));
  self->pipeline = NULL;

  /* clean up as possible. May be called multiple times */

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
kms_change_internal_pipeline_state (KmsHttpEndpoint * self, gboolean start)
{
  GstElement *audio_v, *video_v;

  if (self->pipeline == NULL) {
    GST_WARNING ("Element %s is not initialized", GST_ELEMENT_NAME (self));
    self->start = start;
    return;
  }

  audio_v = kms_element_get_audio_valve (KMS_ELEMENT (self));
  if (audio_v != NULL)
    kms_utils_set_valve_drop (audio_v, !start);

  video_v = kms_element_get_video_valve (KMS_ELEMENT (self));
  if (video_v != NULL)
    kms_utils_set_valve_drop (video_v, !start);

  if (start) {
    /* Set pipeline to PLAYING */
    GST_DEBUG ("Setting pipeline to PLAYING");
    if (gst_element_set_state (self->pipeline, GST_STATE_PLAYING) ==
        GST_STATE_CHANGE_ASYNC)
      GST_DEBUG ("Change to PLAYING will be asynchronous");
  } else {
    /* Set pipeline to READY */
    GST_DEBUG ("Setting pipeline to READY.");
    if (gst_element_set_state (self->pipeline, GST_STATE_READY) ==
        GST_STATE_CHANGE_ASYNC)
      GST_DEBUG ("Change to READY will be asynchronous");

    // Reset base time data
    BASE_TIME_LOCK (self);
    g_object_set_data_full (G_OBJECT (self), BASE_TIME_DATA, NULL, NULL);
    BASE_TIME_UNLOCK (self);
  }

  self->start = start;
}

static void
kms_http_endpoint_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsHttpEndpoint *self = KMS_HTTP_ENDPOINT (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_START:{

      if (self->start != g_value_get_boolean (value)) {
//        kms_change_internal_pipeline_state (self, g_value_get_boolean (value));
        KMS_HTTP_ENDPOINT_GET_CLASS (self)->start (self,
            g_value_get_boolean (value));
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

  if (FALSE) {
    kms_change_internal_pipeline_state (self, start);
  }
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

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsHttpEndpointPrivate));
}

static void
kms_http_endpoint_init (KmsHttpEndpoint * self)
{
  self->priv = KMS_HTTP_ENDPOINT_GET_PRIVATE (self);

  g_mutex_init (&self->base_time_lock);

  g_object_set (self, "do-synchronization", TRUE, NULL);

  self->priv->loop = kms_loop_new ();
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
