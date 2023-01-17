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
#include "kmselement.h"
#include "kms-core-enumtypes.h"
#include "kmsuriendpoint.h"

#define PLUGIN_NAME "uriendpoint"

GST_DEBUG_CATEGORY_STATIC (kms_uri_endpoint_debug_category);
#define GST_CAT_DEFAULT kms_uri_endpoint_debug_category

#define KMS_URI_ENDPOINT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (              \
    (obj),                                   \
    KMS_TYPE_URI_ENDPOINT,                   \
    KmsUriEndpointPrivate                    \
  )                                          \
)
struct _KmsUriEndpointPrivate
{
  KmsUriEndpointState state;
};

enum
{
  STATE_CHANGED,
  LAST_SIGNAL
};

static guint kms_uri_endpoint_signals[LAST_SIGNAL] = { 0 };

enum
{
  PROP_0,
  PROP_URI,
  PROP_STATE,
  N_PROPERTIES
};

#define DEFAULT_URI_ENDPOINT_STATE KMS_URI_ENDPOINT_STATE_STOP

#define CALL_IF_DEFINED(obj, method, err) ({                            \
  gboolean _ret;                                                        \
  if ((method) != NULL) {                                               \
    _ret = method(obj, err);                                            \
  } else {                                                              \
    g_set_error_literal (err, KMS_URI_ENDPOINT_ERROR,                   \
      KMS_URI_ENDPOINT_UNEXPECTED_ERROR, "Undefined method " #method);  \
    _ret = FALSE;                                                       \
  }                                                                     \
  _ret;                                                                 \
})

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsUriEndpoint, kms_uri_endpoint,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_uri_endpoint_debug_category, PLUGIN_NAME,
        0, "debug category for uriendpoint element"));

static const gchar *str_uristate[] = {
  "stop",
  "start",
  "pause"
};

const gchar *
kms_uriendpoint_state_to_string (KmsUriEndpointState state)
{
  if (state < G_N_ELEMENTS (str_uristate))
    return str_uristate[state];

  GST_ERROR ("Invalid uri endpoint state: %u", state);

  return NULL;
}

static void
kms_uri_endpoint_change_state_impl (KmsUriEndpoint * self,
    KmsUriEndpointState state)
{
  if (self->priv->state == state)
    return;

  GST_DEBUG_OBJECT (self, "State changed from %s to %s",
      kms_uriendpoint_state_to_string (self->priv->state),
      kms_uriendpoint_state_to_string (state));

  self->priv->state = state;
  g_signal_emit (G_OBJECT (self), kms_uri_endpoint_signals[STATE_CHANGED], 0,
      state);
}

static gboolean
kms_uri_endpoint_change_state (KmsUriEndpoint * self, KmsUriEndpointState next,
    GError ** err)
{
  if (self->priv->state == next) {
    g_set_error (err, KMS_URI_ENDPOINT_ERROR,
        KMS_URI_ENDPOINT_INVALID_TRANSITION, "Already in state %s",
        kms_uriendpoint_state_to_string (next));

    return FALSE;
  }

  switch (next) {
    case KMS_URI_ENDPOINT_STATE_STOP:
      return CALL_IF_DEFINED (self, KMS_URI_ENDPOINT_GET_CLASS (self)->stopped,
          err);
    case KMS_URI_ENDPOINT_STATE_START:
      return CALL_IF_DEFINED (self, KMS_URI_ENDPOINT_GET_CLASS (self)->started,
          err);
    case KMS_URI_ENDPOINT_STATE_PAUSE:
      return CALL_IF_DEFINED (self, KMS_URI_ENDPOINT_GET_CLASS (self)->paused,
          err);
  }

  g_set_error (err, KMS_URI_ENDPOINT_ERROR,
      KMS_URI_ENDPOINT_UNEXPECTED_ERROR, "Invalid state %u", next);

  return FALSE;
}

gboolean
kms_uri_endpoint_set_state (KmsUriEndpoint * self, KmsUriEndpointState next,
    GError ** err)
{
  gboolean ret;

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  ret = kms_uri_endpoint_change_state (self, next, err);
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));

  return ret;
}

KmsUriEndpointState
kms_uri_endpoint_get_state (KmsUriEndpoint * self)
{
  return self->priv->state;
}

static void
kms_uri_endpoint_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsUriEndpoint *self = KMS_URI_ENDPOINT (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_URI:
      if (self->uri != NULL)
        g_free (self->uri);

      self->uri = g_value_dup_string (value);
      break;
    case PROP_STATE:
      kms_uri_endpoint_change_state (self, g_value_get_enum (value), NULL);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static void
kms_uri_endpoint_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsUriEndpoint *self = KMS_URI_ENDPOINT (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_URI:
      g_value_set_string (value, self->uri);
      break;
    case PROP_STATE:
      g_value_set_enum (value, self->priv->state);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static void
kms_uri_endpoint_dispose (GObject * object)
{
  KmsUriEndpoint *self = KMS_URI_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "dispose");

  G_OBJECT_CLASS (kms_uri_endpoint_parent_class)->dispose (object);
}

static void
kms_uri_endpoint_finalize (GObject * object)
{
  KmsUriEndpoint *self = KMS_URI_ENDPOINT (object);

  GST_DEBUG_OBJECT (self, "finalize");

  if (self->uri) {
    g_free (self->uri);
    self->uri = NULL;
  }

  G_OBJECT_CLASS (kms_uri_endpoint_parent_class)->finalize (object);
}

static void
kms_uri_endpoint_class_init (KmsUriEndpointClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "UriEndpoint", "Generic", "Kurento plugin uri end point",
      "Santiago Carot-Nemesio <sancane.kurento@gmail.com>");

  gobject_class->set_property = kms_uri_endpoint_set_property;
  gobject_class->get_property = kms_uri_endpoint_get_property;
  gobject_class->dispose = kms_uri_endpoint_dispose;
  gobject_class->finalize = kms_uri_endpoint_finalize;

  /* protected methods */
  klass->change_state = kms_uri_endpoint_change_state_impl;

  /* pure virtual methods: mandates implementation in children. */
  klass->paused = NULL;
  klass->started = NULL;
  klass->stopped = NULL;

  obj_properties[PROP_URI] = g_param_spec_string ("uri",
      "uri where the file is located", "Set uri", NULL /* default value */ ,
      G_PARAM_READWRITE);

  obj_properties[PROP_STATE] = g_param_spec_enum ("state",
      "Uri end point state",
      "state of the uri end point element",
      KMS_TYPE_URI_ENDPOINT_STATE,
      DEFAULT_URI_ENDPOINT_STATE, G_PARAM_READWRITE);

  g_object_class_install_properties (gobject_class,
      N_PROPERTIES, obj_properties);

  kms_uri_endpoint_signals[STATE_CHANGED] =
      g_signal_new ("state-changed",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsUriEndpointClass, state_changed), NULL, NULL,
      g_cclosure_marshal_VOID__ENUM, G_TYPE_NONE, 1,
      KMS_TYPE_URI_ENDPOINT_STATE);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsUriEndpointPrivate));
}

static void
kms_uri_endpoint_init (KmsUriEndpoint * self)
{
  self->priv = KMS_URI_ENDPOINT_GET_PRIVATE (self);
  self->uri = NULL;
}

gboolean
kms_uri_endpoint_plugin_init (GstPlugin * plugin)
{

  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_URI_ENDPOINT);
}
