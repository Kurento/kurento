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

#include "sdp_utils.h"
#include "kmssdpagent.h"
#include "kmssdpmediahandler.h"

#define OBJECT_NAME "sdpmediahandler"

GST_DEBUG_CATEGORY_STATIC (kms_sdp_media_handler_debug_category);
#define GST_CAT_DEFAULT kms_sdp_media_handler_debug_category

#define parent_class kms_sdp_media_handler_parent_class

G_DEFINE_TYPE_WITH_CODE (KmsSdpMediaHandler, kms_sdp_media_handler,
    G_TYPE_OBJECT,
    GST_DEBUG_CATEGORY_INIT (kms_sdp_media_handler_debug_category, OBJECT_NAME,
        0, "debug category for sdp media_handler"));

#define DEFAULT_ADDR_TYPE "IP4"

typedef gboolean (*KmsSdpAcceptAttributeFunc) (const GstSDPMedia * offer,
    const GstSDPAttribute * attr, GstSDPMedia * media,
    const GstSDPMessage * msg);

static gboolean
default_accept_attribute (const GstSDPMedia * offer,
    const GstSDPAttribute * attr, GstSDPMedia * media,
    const GstSDPMessage * msg)
{
  return TRUE;
}

static gboolean
accept_fmtp_attribute (const GstSDPMedia * offer,
    const GstSDPAttribute * attr, GstSDPMedia * media,
    const GstSDPMessage * msg)
{
  guint i, len;
  gchar **fmtp;
  gboolean ret = FALSE;

  fmtp = g_strsplit (attr->value, " ", 0);

  /* Check that answer supports this format */
  len = gst_sdp_media_formats_len (media);

  for (i = 0; i < len; i++) {
    const gchar *fmt;

    fmt = gst_sdp_media_get_format (media, i);
    if (g_strcmp0 (fmt, fmtp[0] /* format */ ) == 0) {
      ret = TRUE;
      break;
    }
  }

  g_strfreev (fmtp);

  return ret;
}

typedef struct _KmsSdpSupportedAttrType
{
  const gchar *name;
  KmsSdpAcceptAttributeFunc accept;
} KmsSdpSupportedAttrType;

/* Supported media session attributes */
static KmsSdpSupportedAttrType attributes[] = {
  {"framerate", default_accept_attribute},
  {"fmtp", accept_fmtp_attribute},
  {"lang", default_accept_attribute},
  {"maxptime", default_accept_attribute},
  {"mid", default_accept_attribute},
  {"ptime", default_accept_attribute},
  {"quality", default_accept_attribute},
  {"setup", default_accept_attribute},
  {"direction", default_accept_attribute}
};

/* Object properties */
enum
{
  PROP_0,
  PROP_ID,
  PROP_INDEX,
  PROP_PROTO,
  PROP_ADDR,
  PROP_ADDR_TYPE,
  N_PROPERTIES
};

#define KMS_SDP_MEDIA_HANDLER_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                   \
    (obj),                                        \
    KMS_TYPE_SDP_MEDIA_HANDLER,                   \
    KmsSdpMediaHandlerPrivate                     \
  )                                               \
)

struct _KmsSdpMediaHandlerPrivate
{
  gchar *proto;
  gchar *addr;
  gchar *addr_type;
  GArray *bwtypes;
  GSList *extensions;
  gint id;
  KmsSdpAgent *parent;
};

static void
kms_sdp_media_handler_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  KmsSdpMediaHandler *self = KMS_SDP_MEDIA_HANDLER (object);

  switch (prop_id) {
    case PROP_ID:
      g_value_set_int (value, self->priv->id);
      break;
    case PROP_INDEX:{
      gint ret = -1;

      if (self->priv->id >= 0 && self->priv->parent != NULL) {
        ret = kms_sdp_agent_get_handler_index (self->priv->parent,
            self->priv->id);
      }

      g_value_set_int (value, ret);
      break;
    }
    case PROP_PROTO:
      g_value_set_string (value, self->priv->proto);
      break;
    case PROP_ADDR:
      g_value_set_string (value, self->priv->addr);
      break;
    case PROP_ADDR_TYPE:
      g_value_set_string (value, self->priv->addr_type);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_sdp_media_handler_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsSdpMediaHandler *self = KMS_SDP_MEDIA_HANDLER (object);

  switch (prop_id) {
    case PROP_PROTO:
      g_free (self->priv->proto);
      self->priv->proto = g_value_dup_string (value);
      break;
    case PROP_ADDR:
      g_free (self->priv->addr);
      self->priv->addr = g_value_dup_string (value);
      break;
    case PROP_ADDR_TYPE:
      g_free (self->priv->addr_type);
      self->priv->addr_type = g_value_dup_string (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_sdp_media_handler_finalize (GObject * object)
{
  KmsSdpMediaHandler *self = KMS_SDP_MEDIA_HANDLER (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_free (self->priv->proto);
  g_free (self->priv->addr);
  g_free (self->priv->addr_type);

  g_array_free (self->priv->bwtypes, TRUE);
  g_slist_free_full (self->priv->extensions, g_object_unref);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static GstSDPMedia *
kms_sdp_media_handler_create_offer_impl (KmsSdpMediaHandler * handler,
    const gchar * media, const GstSDPMedia * offer, GError ** error)
{
  g_set_error_literal (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
      "Not implemented");

  return NULL;
}

static GstSDPMedia *
kms_sdp_media_handler_create_answer_impl (KmsSdpMediaHandler * handler,
    const GstSDPMessage * msg, const GstSDPMedia * offer, GError ** error)
{
  g_set_error_literal (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
      "Not implemented");

  return NULL;
}

static gboolean
kms_sdp_media_handler_process_answer_impl (KmsSdpMediaHandler * handler,
    const GstSDPMedia * answer, GError ** error)
{
  GSList *l;

  for (l = handler->priv->extensions; l != NULL; l = g_slist_next (l)) {
    KmsISdpMediaExtension *ext = KMS_I_SDP_MEDIA_EXTENSION (l->data);

    if (!kms_i_sdp_media_extension_process_answer_attributes (ext, answer,
            error)) {
      return FALSE;
    }
  }

  return TRUE;
}

static void
kms_sdp_media_handler_add_bandwidth_impl (KmsSdpMediaHandler * handler,
    const gchar * bwtype, guint bandwidth)
{
  GstSDPBandwidth bw;

  gst_sdp_bandwidth_set (&bw, bwtype, bandwidth);
  g_array_append_val (handler->priv->bwtypes, bw);
}

static gboolean
kms_sdp_media_handler_manage_protocol_impl (KmsSdpMediaHandler * handler,
    const gchar * protocol)
{
  /* Just check protocol by default */
  return g_strcmp0 (handler->priv->proto, protocol) == 0;
}

static gboolean
kms_sdp_media_handler_add_media_extension_impl (KmsSdpMediaHandler * handler,
    KmsISdpMediaExtension * ext)
{
  handler->priv->extensions = g_slist_append (handler->priv->extensions, ext);

  return TRUE;
}

static gboolean
is_direction_attr_present (const GstSDPMedia * media)
{
  guint i, len;

  len = gst_sdp_media_attributes_len (media);

  for (i = 0; i < len; i++) {
    const GstSDPAttribute *attr;

    attr = gst_sdp_media_get_attribute (media, i);
    if (sdp_utils_attribute_is_direction (attr, NULL)) {
      return TRUE;
    }
  }

  return FALSE;
}

static gboolean
kms_sdp_media_handler_can_insert_attribute_impl (KmsSdpMediaHandler * handler,
    const GstSDPMedia * offer, const GstSDPAttribute * attr,
    GstSDPMedia * media, const GstSDPMessage * msg)
{
  guint i, len;
  GSList *l;

  if (sdp_utils_is_attribute_in_media (media, attr)) {
    return FALSE;
  }

  if (sdp_utils_attribute_is_direction (attr, NULL)) {
    return !is_direction_attr_present (media);
  }

  len = G_N_ELEMENTS (attributes);

  for (i = 0; i < len; i++) {
    if (g_strcmp0 (attr->key, attributes[i].name) == 0) {
      return attributes[i].accept (offer, attr, media, msg);
    }
  }

  for (l = handler->priv->extensions; l != NULL; l = g_slist_next (l)) {
    KmsISdpMediaExtension *ext = KMS_I_SDP_MEDIA_EXTENSION (l->data);

    if (kms_i_sdp_media_extension_can_insert_attribute (ext, offer, attr, media,
            msg)) {
      return TRUE;
    }
  }

  return FALSE;
}

static gboolean
kms_sdp_media_handler_intersect_sdp_medias_impl (KmsSdpMediaHandler * handler,
    const GstSDPMedia * offer, GstSDPMedia * answer, const GstSDPMessage * msg,
    GError ** error)
{
  g_set_error_literal (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
      "Not implemented");

  return FALSE;
}

static gboolean
kms_sdp_media_handler_init_offer_impl (KmsSdpMediaHandler * handler,
    const gchar * media, GstSDPMedia * offer, const GstSDPMedia * prev_offer,
    GError ** error)
{
  g_set_error_literal (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
      "Media offert initialization is not implemented");

  return FALSE;
}

static gboolean
kms_sdp_media_handler_add_offer_attributes_impl (KmsSdpMediaHandler * handler,
    GstSDPMedia * offer, const GstSDPMedia * prev_offer, GError ** error)
{
  GError *err = NULL;
  GSList *l;
  gint i;

  /* Add bandwidth attributes */
  for (i = 0; i < handler->priv->bwtypes->len; i++) {
    GstSDPBandwidth *bw;

    bw = &g_array_index (handler->priv->bwtypes, GstSDPBandwidth, i);
    gst_sdp_media_add_bandwidth (offer, bw->bwtype, bw->bandwidth);
  }

  for (l = handler->priv->extensions; l != NULL; l = g_slist_next (l)) {
    KmsISdpMediaExtension *ext = KMS_I_SDP_MEDIA_EXTENSION (l->data);

    if (!kms_i_sdp_media_extension_add_offer_attributes (ext, offer, &err)) {
      GST_ERROR_OBJECT (ext, "%s", err->message);
      g_clear_error (&err);
    }
  }

  return TRUE;
}

static gboolean
kms_sdp_media_handler_init_answer_impl (KmsSdpMediaHandler * handler,
    const GstSDPMedia * offer, GstSDPMedia * answer, GError ** error)
{
  g_set_error_literal (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
      "Media answer initialization is not implemented");

  return FALSE;
}

static gboolean
kms_sdp_media_handler_add_answer_attributes_impl (KmsSdpMediaHandler * handler,
    const GstSDPMedia * offer, GstSDPMedia * answer, GError ** error)
{
  GError *err = NULL;
  GSList *l;
  gint i;

  /* Add bandwidth attributes */
  for (i = 0; i < handler->priv->bwtypes->len; i++) {
    GstSDPBandwidth *bw;

    bw = &g_array_index (handler->priv->bwtypes, GstSDPBandwidth, i);
    gst_sdp_media_add_bandwidth (answer, bw->bwtype, bw->bandwidth);
  }

  for (l = handler->priv->extensions; l != NULL; l = g_slist_next (l)) {
    KmsISdpMediaExtension *ext = KMS_I_SDP_MEDIA_EXTENSION (l->data);

    if (!kms_i_sdp_media_extension_add_answer_attributes (ext, offer, answer,
            &err)) {
      GST_ERROR_OBJECT (ext, "%s", err->message);
      g_clear_error (&err);
    }
  }

  return TRUE;
}

static gboolean
kms_sdp_media_handler_set_id_impl (KmsSdpMediaHandler * handler, guint id,
    GError ** error)
{
  if (handler->priv->id >= 0) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Media handler already has an assigned id = %u", handler->priv->id);
    return FALSE;
  }

  handler->priv->id = id;

  return TRUE;
}

static void
kms_sdp_media_handler_class_init (KmsSdpMediaHandlerClass * klass)
{
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->get_property = kms_sdp_media_handler_get_property;
  gobject_class->set_property = kms_sdp_media_handler_set_property;
  gobject_class->finalize = kms_sdp_media_handler_finalize;

  g_object_class_install_property (gobject_class, PROP_PROTO,
      g_param_spec_string ("proto", "Protocol",
          "Media protocol", NULL,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT_ONLY | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_ADDR,
      g_param_spec_string ("addr", "Address", "Address", NULL,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_ADDR_TYPE,
      g_param_spec_string ("addr-type",
          "Address type", "Address type either IP4 or IP6", DEFAULT_ADDR_TYPE,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_ID,
      g_param_spec_int ("id", "Identifier",
          "Identifier assigned by an SDP agent", -1, G_MAXINT, -1,
          G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_INDEX,
      g_param_spec_int ("index", "SDP Index",
          "Index which this handler has in the SDP message", -1, G_MAXINT, -1,
          G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  klass->set_id = kms_sdp_media_handler_set_id_impl;
  klass->create_offer = kms_sdp_media_handler_create_offer_impl;
  klass->create_answer = kms_sdp_media_handler_create_answer_impl;
  klass->process_answer = kms_sdp_media_handler_process_answer_impl;
  klass->add_bandwidth = kms_sdp_media_handler_add_bandwidth_impl;
  klass->manage_protocol = kms_sdp_media_handler_manage_protocol_impl;
  klass->add_media_extension = kms_sdp_media_handler_add_media_extension_impl;

  klass->can_insert_attribute = kms_sdp_media_handler_can_insert_attribute_impl;
  klass->intersect_sdp_medias = kms_sdp_media_handler_intersect_sdp_medias_impl;

  klass->init_offer = kms_sdp_media_handler_init_offer_impl;
  klass->add_offer_attributes = kms_sdp_media_handler_add_offer_attributes_impl;

  klass->init_answer = kms_sdp_media_handler_init_answer_impl;
  klass->add_answer_attributes =
      kms_sdp_media_handler_add_answer_attributes_impl;

  g_type_class_add_private (klass, sizeof (KmsSdpMediaHandlerPrivate));
}

static void
kms_sdp_media_handler_init (KmsSdpMediaHandler * self)
{
  self->priv = KMS_SDP_MEDIA_HANDLER_GET_PRIVATE (self);
  self->priv->bwtypes = g_array_new (FALSE, TRUE, sizeof (GstSDPBandwidth));
  g_array_set_clear_func (self->priv->bwtypes,
      (GDestroyNotify) gst_sdp_bandwidth_clear);
  self->priv->id = -1;
  self->priv->parent = NULL;
}

GstSDPMedia *
kms_sdp_media_handler_create_offer (KmsSdpMediaHandler * handler,
    const gchar * media, const GstSDPMedia * prev_offer, GError ** error)
{
  g_return_val_if_fail (KMS_IS_SDP_MEDIA_HANDLER (handler), NULL);

  return KMS_SDP_MEDIA_HANDLER_GET_CLASS (handler)->create_offer (handler,
      media, prev_offer, error);
}

GstSDPMedia *
kms_sdp_media_handler_create_answer (KmsSdpMediaHandler * handler,
    const GstSDPMessage * msg, const GstSDPMedia * offer, GError ** error)
{
  g_return_val_if_fail (KMS_IS_SDP_MEDIA_HANDLER (handler), NULL);

  return KMS_SDP_MEDIA_HANDLER_GET_CLASS (handler)->create_answer (handler,
      msg, offer, error);
}

gboolean
kms_sdp_media_handler_process_answer (KmsSdpMediaHandler * handler,
    const GstSDPMedia * answer, GError ** error)
{
  g_return_val_if_fail (KMS_IS_SDP_MEDIA_HANDLER (handler), FALSE);

  return KMS_SDP_MEDIA_HANDLER_GET_CLASS (handler)->process_answer (handler,
      answer, error);
}

void
kms_sdp_media_handler_add_bandwidth (KmsSdpMediaHandler * handler,
    const gchar * bwtype, guint bandwidth)
{
  g_return_if_fail (KMS_IS_SDP_MEDIA_HANDLER (handler));

  return KMS_SDP_MEDIA_HANDLER_GET_CLASS (handler)->add_bandwidth (handler,
      bwtype, bandwidth);
}

gboolean
kms_sdp_media_handler_manage_protocol (KmsSdpMediaHandler * handler,
    const gchar * protocol)
{
  g_return_val_if_fail (KMS_IS_SDP_MEDIA_HANDLER (handler), FALSE);

  return KMS_SDP_MEDIA_HANDLER_GET_CLASS (handler)->manage_protocol (handler,
      protocol);
}

gboolean
kms_sdp_media_handler_add_media_extension (KmsSdpMediaHandler * handler,
    KmsISdpMediaExtension * ext)
{
  g_return_val_if_fail (KMS_IS_SDP_MEDIA_HANDLER (handler), FALSE);
  g_return_val_if_fail (KMS_IS_I_SDP_MEDIA_EXTENSION (ext), FALSE);

  return
      KMS_SDP_MEDIA_HANDLER_GET_CLASS (handler)->add_media_extension (handler,
      ext);
}

gboolean
kms_sdp_media_handler_set_id (KmsSdpMediaHandler * handler, guint id,
    GError ** error)
{
  g_return_val_if_fail (KMS_IS_SDP_MEDIA_HANDLER (handler), FALSE);

  return KMS_SDP_MEDIA_HANDLER_GET_CLASS (handler)->set_id (handler, id, error);
}

gboolean
kms_sdp_media_handler_set_parent (KmsSdpMediaHandler * handler,
    KmsSdpAgent * parent, GError ** error)
{
  g_return_val_if_fail (KMS_IS_SDP_MEDIA_HANDLER (handler), FALSE);
  g_return_val_if_fail (KMS_IS_SDP_AGENT (parent), FALSE);

  if (handler->priv->parent != NULL) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Handler already has a parent");
    return FALSE;
  }

  handler->priv->parent = parent;

  return TRUE;
}

void
kms_sdp_media_handler_remove_parent (KmsSdpMediaHandler * handler)
{
  g_return_if_fail (KMS_IS_SDP_MEDIA_HANDLER (handler));

  handler->priv->parent = NULL;
}
