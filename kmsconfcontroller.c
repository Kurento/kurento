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
struct _KmsConfControllerPrivate
{
  KmsElement *element;
  GstElement *encodebin;
  GstPipeline *pipeline;
  GstElement *sink;
  KmsRecordingProfile profile;
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
kms_conf_controller_link_valve_impl (KmsConfController * self,
    GstElement * valve, const gchar * sinkname,
    const gchar * srcname, const gchar * destpadname)
{
  /*TODO: */
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
