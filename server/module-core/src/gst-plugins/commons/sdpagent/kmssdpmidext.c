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

#include <string.h>

#include "kmsisdpmediaextension.h"
#include "kmssdpmidext.h"
#include "kmssdpagent.h"
#include "kms-sdp-agent-marshal.h"

#define OBJECT_NAME "sdpmidext"

GST_DEBUG_CATEGORY_STATIC (kms_sdp_mid_ext_debug_category);
#define GST_CAT_DEFAULT kms_sdp_mid_ext_debug_category

#define parent_class kms_sdp_mid_ext_parent_class

static void kms_i_sdp_media_extension_init (KmsISdpMediaExtensionInterface *
    iface);

G_DEFINE_TYPE_WITH_CODE (KmsSdpMidExt, kms_sdp_mid_ext,
    G_TYPE_OBJECT,
    G_IMPLEMENT_INTERFACE (KMS_TYPE_I_SDP_MEDIA_EXTENSION,
        kms_i_sdp_media_extension_init)
    GST_DEBUG_CATEGORY_INIT (kms_sdp_mid_ext_debug_category, OBJECT_NAME,
        0, "debug category for sdp mid_ext"));

#define KMS_SDP_MID_EXT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (             \
    (obj),                                  \
    KMS_TYPE_SDP_MID_EXT,                   \
    KmsSdpMidExtPrivate                     \
  )                                         \
)

#define MID_ATTR "mid"

struct _KmsSdpMidExtPrivate
{
  gchar *mid;
};

/* Object properties */
enum
{
  PROP_0,
  PROP_MID,
  N_PROPERTIES
};

enum
{
  SIGNAL_ON_OFFER_MID,
  SIGNAL_ON_ANSWER_MID,

  LAST_SIGNAL
};

static guint obj_signals[LAST_SIGNAL] = { 0 };

static gboolean
kms_sdp_mid_ext_add_offer_attributes (KmsISdpMediaExtension * ext,
    GstSDPMedia * offer, GError ** error)
{
  gchar *mid;

  mid = (gchar *) gst_sdp_media_get_attribute_val (offer, MID_ATTR);
  if (mid != NULL) {
    GST_WARNING_OBJECT (ext, "Mid '%s' already added to the offer", mid);
    return TRUE;
  }

  g_signal_emit (G_OBJECT (ext), obj_signals[SIGNAL_ON_OFFER_MID], 0, &mid);

  gst_sdp_media_add_attribute (offer, MID_ATTR, mid);
  g_free (mid);

  return TRUE;
}

static gboolean
kms_sdp_mid_ext_add_answer_attributes (KmsISdpMediaExtension * ext,
    const GstSDPMedia * offer, GstSDPMedia * answer, GError ** error)
{
  const gchar *mid;
  gchar *str;
  gboolean ret = FALSE;

  mid = gst_sdp_media_get_attribute_val (answer, MID_ATTR);
  if (mid != NULL) {
    /* do not add more mid attributes */
    GST_DEBUG_OBJECT (ext, "Mid has already set in answer");
    return TRUE;
  }

  mid = gst_sdp_media_get_attribute_val (offer, MID_ATTR);
  if (mid == NULL) {
    GST_WARNING_OBJECT (ext, "Remote agent does not support groups");
    return TRUE;
  }

  str = g_strdup (mid);
  g_signal_emit (G_OBJECT (ext), obj_signals[SIGNAL_ON_ANSWER_MID],
      0, mid, &ret);
  g_free (str);

  if (!ret) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Media stream identifier error");

    return FALSE;
  }

  gst_sdp_media_add_attribute (answer, MID_ATTR, mid);

  return TRUE;
}

static gboolean
kms_sdp_mid_ext_can_insert_attribute (KmsISdpMediaExtension * ext,
    const GstSDPMedia * offer, const GstSDPAttribute * attr,
    GstSDPMedia * answer, const GstSDPMessage * msg)
{
  GST_DEBUG_OBJECT (ext, "an insert %s:%s ?", attr->key, attr->value);

  return FALSE;
}

static void
kms_sdp_mid_ext_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  KmsSdpMidExt *self = KMS_SDP_MID_EXT (object);

  switch (prop_id) {
    case PROP_MID:
      g_value_set_string (value, self->priv->mid);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_sdp_mid_ext_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsSdpMidExt *self = KMS_SDP_MID_EXT (object);

  switch (prop_id) {
    case PROP_MID:
      g_free (self->priv->mid);
      self->priv->mid = g_value_dup_string (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_sdp_mid_ext_finalize (GObject * object)
{
  KmsSdpMidExt *self = KMS_SDP_MID_EXT (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_free (self->priv->mid);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static void
kms_sdp_mid_ext_class_init (KmsSdpMidExtClass * klass)
{
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->get_property = kms_sdp_mid_ext_get_property;
  gobject_class->set_property = kms_sdp_mid_ext_set_property;
  gobject_class->finalize = kms_sdp_mid_ext_finalize;

  g_object_class_install_property (gobject_class, PROP_MID,
      g_param_spec_string ("mid", "Mid", "Media stream identification", NULL,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

  obj_signals[SIGNAL_ON_OFFER_MID] =
      g_signal_new ("on-offer-mid",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsSdpMidExtClass, on_offer_mid),
      NULL, NULL, __kms_sdp_agent_marshal_STRING__VOID, G_TYPE_STRING, 0);

  obj_signals[SIGNAL_ON_ANSWER_MID] =
      g_signal_new ("on-answer-mid",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsSdpMidExtClass, on_answer_mid),
      NULL, NULL, __kms_sdp_agent_marshal_BOOLEAN__STRING, G_TYPE_BOOLEAN, 1,
      G_TYPE_STRING);

  g_type_class_add_private (klass, sizeof (KmsSdpMidExtPrivate));
}

static void
kms_sdp_mid_ext_init (KmsSdpMidExt * self)
{
  self->priv = KMS_SDP_MID_EXT_GET_PRIVATE (self);
  /* Nothing to do */
}

static void
kms_i_sdp_media_extension_init (KmsISdpMediaExtensionInterface * iface)
{
  iface->add_offer_attributes = kms_sdp_mid_ext_add_offer_attributes;
  iface->add_answer_attributes = kms_sdp_mid_ext_add_answer_attributes;
  iface->can_insert_attribute = kms_sdp_mid_ext_can_insert_attribute;
}

KmsSdpMidExt *
kms_sdp_mid_ext_new ()
{
  gpointer obj;

  obj = g_object_new (KMS_TYPE_SDP_MID_EXT, NULL);

  return KMS_SDP_MID_EXT (obj);
}
