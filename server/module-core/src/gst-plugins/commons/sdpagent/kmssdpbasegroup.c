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

#include "kmssdpbasegroup.h"

#define OBJECT_NAME "sdpbasegroup"

GST_DEBUG_CATEGORY_STATIC (kms_sdp_base_group_debug_category);
#define GST_CAT_DEFAULT kms_sdp_base_group_debug_category

#define parent_class kms_sdp_base_group_parent_class
#define KMS_BASE_GROUP_DEFAULT_ID (-1)

static void kms_i_sdp_session_extension_init (KmsISdpSessionExtensionInterface *
    iface);

G_DEFINE_TYPE_WITH_CODE (KmsSdpBaseGroup, kms_sdp_base_group,
    G_TYPE_OBJECT,
    G_IMPLEMENT_INTERFACE (KMS_TYPE_I_SDP_SESSION_EXTENSION,
        kms_i_sdp_session_extension_init)
    GST_DEBUG_CATEGORY_INIT (kms_sdp_base_group_debug_category, OBJECT_NAME,
        0, "debug category for sdp base_group"));

#define KMS_SDP_BASE_GROUP_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                \
    (obj),                                     \
    KMS_TYPE_SDP_BASE_GROUP,                   \
    KmsSdpBaseGroupPrivate                     \
  )                                            \
)

struct _KmsSdpBaseGroupPrivate
{
  gint id;
  gchar *semantics;
  gboolean pre_proc;
  GSList *handlers;
};

/* Object properties */
enum
{
  PROP_0,
  PROP_ID,
  PROP_SEMANTICS,
  PROP_PRE_PROC,
  N_PROPERTIES
};

static void
kms_sdp_base_group_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  KmsSdpBaseGroup *self = KMS_SDP_BASE_GROUP (object);

  switch (prop_id) {
    case PROP_ID:
      g_value_set_int (value, self->priv->id);
      break;
    case PROP_SEMANTICS:
      g_value_set_string (value, self->priv->semantics);
      break;
    case PROP_PRE_PROC:
      g_value_set_boolean (value, self->priv->pre_proc);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_sdp_base_group_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsSdpBaseGroup *self = KMS_SDP_BASE_GROUP (object);

  switch (prop_id) {
    case PROP_ID:
      self->priv->id = g_value_get_int (value);
      break;
    case PROP_SEMANTICS:
      g_free (self->priv->semantics);
      self->priv->semantics = g_value_dup_string (value);
      break;
    case PROP_PRE_PROC:
      self->priv->pre_proc = g_value_get_boolean (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_sdp_base_group_finalize (GObject * object)
{
  KmsSdpBaseGroup *self = KMS_SDP_BASE_GROUP (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_slist_free_full (self->priv->handlers,
      (GDestroyNotify) kms_sdp_agent_common_unref_sdp_handler);

  g_free (self->priv->semantics);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

typedef struct _SdpGroupStrVal
{
  gchar *str;
  GstSDPMessage *msg;
  KmsSdpBaseGroup *group;
  gchar **filter;
} SdpGroupStrVal;

static void
append_enabled_medias (KmsSdpHandler * handler, SdpGroupStrVal * val)
{
  const GstSDPMedia *media;
  gboolean removed = TRUE;
  gint i, index, id;
  const gchar *mid;
  gchar *tmp;

  g_object_get (handler->handler, "index", &index, "id", &id, NULL);

  if (index < 0) {
    /* Agent not negotiated */
    return;
  }

  if (index >= gst_sdp_message_medias_len (val->msg)) {
    GST_ERROR_OBJECT (val->group,
        "Cannot append media to handler (%u): Invalid media index %u in SDP",
        id, index);
    return;
  }

  media = gst_sdp_message_get_media (val->msg, index);

  if (media == NULL) {
    GST_ERROR_OBJECT (val->group, "No media got in SDP");
    return;
  }

  mid = gst_sdp_media_get_attribute_val (media, "mid");

  if (mid == NULL) {
    GST_WARNING_OBJECT (val->group,
        "Skipping from group: No 'mid' attribute for media index %u", index);
    return;
  }

  if (gst_sdp_media_get_port (media) == 0) {
    GST_WARNING_OBJECT (val->group,
        "Skipping from group: Media 'mid:%s' has port 0 (media rejected)", mid);
    return;
  }

  if (val->filter == NULL) {
    goto append_media;
  }

  for (i = 1; val->filter[i] != NULL; i++) {
    if (g_strcmp0 (mid, val->filter[i]) == 0) {
      removed = FALSE;
      break;
    }
  }

  if (removed) {
    GST_WARNING ("Media 'mid:%s' removed from group '%s'", mid,
        val->group->priv->semantics);
    return;
  }

append_media:

  tmp = val->str;
  val->str = g_strdup_printf ("%s %s", tmp, mid);

  g_free (tmp);
}

static gboolean
kms_sdp_base_group_add_offer_attributes_impl (KmsSdpBaseGroup * self,
    GstSDPMessage * offer, GError ** error)
{
  SdpGroupStrVal val;
  gboolean pre_proc;

  g_object_get (self, "pre-media-processing", &pre_proc, NULL);

  if (pre_proc) {
    /* Let chlidren classes manage this case */
    return TRUE;
  }

  val.msg = offer;
  val.group = self;
  val.str = g_strdup (self->priv->semantics);
  val.filter = NULL;

  /* Add all handlers that are not disabled to this group */
  g_slist_foreach (self->priv->handlers, (GFunc) append_enabled_medias, &val);

  gst_sdp_message_add_attribute (offer, "group", val.str);

  g_free (val.str);

  return TRUE;
}

static const gchar *
kms_sdp_base_group_is_offered (KmsSdpBaseGroup * self,
    const GstSDPMessage * offer)
{
  const gchar *group = NULL;
  gboolean found = FALSE;
  guint i;

  for (i = 0; !found; i++) {
    gchar **tokens;

    group = gst_sdp_message_get_attribute_val_n (offer, "group", i);

    if (group == NULL) {
      return NULL;
    }

    tokens = g_strsplit (group, " ", 0);
    found = g_strcmp0 (self->priv->semantics, tokens[0]) == 0;
    g_strfreev (tokens);
  }

  return group;
}

static gboolean
kms_sdp_base_group_add_answer_attributes_impl (KmsSdpBaseGroup * self,
    const GstSDPMessage * offer, GstSDPMessage * answer, GError ** error)
{
  SdpGroupStrVal val;
  gboolean pre_proc;
  const gchar *group;

  g_object_get (self, "pre-media-processing", &pre_proc, NULL);

  if (pre_proc) {
    /* Let chlidren classes manage this case */
    return TRUE;
  }

  group = kms_sdp_base_group_is_offered (self, offer);

  if (group == NULL) {
    /* Do not add group attribute */
    return TRUE;
  }

  val.msg = answer;
  val.group = self;
  val.str = g_strdup (self->priv->semantics);
  val.filter = g_strsplit (group, " ", 0);

  /* Add all handlers that are not disabled to this group */
  g_slist_foreach (self->priv->handlers, (GFunc) append_enabled_medias, &val);

  gst_sdp_message_add_attribute (answer, "group", val.str);

  g_free (val.str);
  g_strfreev (val.filter);

  return TRUE;
}

static gboolean
kms_sdp_base_group_can_insert_attribute_impl (KmsSdpBaseGroup * self,
    const GstSDPMessage * offer, const GstSDPAttribute * attr,
    GstSDPMessage * answer)
{
  /* Nothing to add at this level */
  return FALSE;
}

static gboolean
kms_sdp_base_group_add_media_handler_impl (KmsSdpBaseGroup * grp,
    KmsSdpHandler * handler, GError ** error)
{
  if (g_slist_find (grp->priv->handlers, handler) != NULL) {
    /* Already added */
    return TRUE;
  }

  grp->priv->handlers = g_slist_append (grp->priv->handlers,
      kms_sdp_agent_common_ref_sdp_handler (handler));

  /* TODO: Add this group to handler->groups in new API */

  return TRUE;
}

static gboolean
kms_sdp_base_group_remove_media_handler_impl (KmsSdpBaseGroup * grp,
    KmsSdpHandler * handler, GError ** error)
{
  if (g_slist_find (grp->priv->handlers, handler) == NULL) {
    return TRUE;
  }

  grp->priv->handlers = g_slist_remove (grp->priv->handlers, handler);
  kms_sdp_agent_common_unref_sdp_handler (handler);

  /* TODO: Remove this group from handler->groups in new API */

  return TRUE;
}

static gboolean
kms_sdp_base_group_contains_handler_impl (KmsSdpBaseGroup * grp,
    KmsSdpHandler * handler)
{
  return g_slist_find (grp->priv->handlers, handler) != NULL;
}

static void
kms_sdp_base_group_class_init (KmsSdpBaseGroupClass * klass)
{
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->get_property = kms_sdp_base_group_get_property;
  gobject_class->set_property = kms_sdp_base_group_set_property;
  gobject_class->finalize = kms_sdp_base_group_finalize;

  g_object_class_install_property (gobject_class, PROP_ID,
      g_param_spec_int ("id", "Identifier",
          "Group identifier", KMS_BASE_GROUP_DEFAULT_ID, G_MAXINT,
          KMS_BASE_GROUP_DEFAULT_ID,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_SEMANTICS,
      g_param_spec_string ("semantics", "Semantics",
          "Semantics of this group", NULL,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_override_property (gobject_class, PROP_PRE_PROC,
      "pre-media-processing");

  klass->add_offer_attributes = kms_sdp_base_group_add_offer_attributes_impl;
  klass->add_answer_attributes = kms_sdp_base_group_add_answer_attributes_impl;
  klass->contains_handler = kms_sdp_base_group_contains_handler_impl;
  klass->can_insert_attribute = kms_sdp_base_group_can_insert_attribute_impl;

  klass->add_media_handler = kms_sdp_base_group_add_media_handler_impl;
  klass->remove_media_handler = kms_sdp_base_group_remove_media_handler_impl;

  g_type_class_add_private (klass, sizeof (KmsSdpBaseGroupPrivate));
}

static void
kms_sdp_base_group_init (KmsSdpBaseGroup * self)
{
  self->priv = KMS_SDP_BASE_GROUP_GET_PRIVATE (self);
  self->priv->id = -1;
}

static gboolean
kms_sdp_base_group_add_offer_attributes (KmsISdpSessionExtension * ext,
    GstSDPMessage * offer, GError ** error)
{
  KmsSdpBaseGroup *self = KMS_SDP_BASE_GROUP (ext);

  return KMS_SDP_BASE_GROUP_GET_CLASS (self)->add_offer_attributes (self, offer,
      error);
}

static gboolean
kms_sdp_base_group_add_answer_attributes (KmsISdpSessionExtension * ext,
    const GstSDPMessage * offer, GstSDPMessage * answer, GError ** error)
{
  KmsSdpBaseGroup *self = KMS_SDP_BASE_GROUP (ext);

  return KMS_SDP_BASE_GROUP_GET_CLASS (self)->add_answer_attributes (self,
      offer, answer, error);
}

static gboolean
kms_sdp_base_group_can_insert_attribute (KmsISdpSessionExtension * ext,
    const GstSDPMessage * offer, const GstSDPAttribute * attr,
    GstSDPMessage * answer)
{
  KmsSdpBaseGroup *self = KMS_SDP_BASE_GROUP (ext);

  return KMS_SDP_BASE_GROUP_GET_CLASS (self)->can_insert_attribute (self, offer,
      attr, answer);
}

void
kms_i_sdp_session_extension_init (KmsISdpSessionExtensionInterface * iface)
{
  iface->add_offer_attributes = kms_sdp_base_group_add_offer_attributes;
  iface->add_answer_attributes = kms_sdp_base_group_add_answer_attributes;
  iface->can_insert_attribute = kms_sdp_base_group_can_insert_attribute;
}

gboolean
kms_sdp_base_group_add_media_handler (KmsSdpBaseGroup * grp,
    KmsSdpHandler * handler, GError ** error)
{
  g_return_val_if_fail (KMS_IS_SDP_BASE_GROUP (grp), FALSE);

  return KMS_SDP_BASE_GROUP_GET_CLASS (grp)->add_media_handler (grp, handler,
      error);
}

gboolean
kms_sdp_base_group_remove_media_handler (KmsSdpBaseGroup * grp,
    KmsSdpHandler * handler, GError ** error)
{
  g_return_val_if_fail (KMS_IS_SDP_BASE_GROUP (grp), FALSE);

  return KMS_SDP_BASE_GROUP_GET_CLASS (grp)->remove_media_handler (grp, handler,
      error);
}

gboolean
kms_sdp_base_group_contains_handler (KmsSdpBaseGroup * grp,
    KmsSdpHandler * handler)
{
  g_return_val_if_fail (KMS_IS_SDP_BASE_GROUP (grp), FALSE);

  return KMS_SDP_BASE_GROUP_GET_CLASS (grp)->contains_handler (grp, handler);
}
