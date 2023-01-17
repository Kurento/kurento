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

#include "kmssdpbundlegroup.h"

#define OBJECT_NAME "sdpbundlegroup"

GST_DEBUG_CATEGORY_STATIC (kms_sdp_bundle_group_debug_category);
#define GST_CAT_DEFAULT kms_sdp_bundle_group_debug_category

#define parent_class kms_sdp_bundle_group_parent_class

G_DEFINE_TYPE_WITH_CODE (KmsSdpBundleGroup, kms_sdp_bundle_group,
    KMS_TYPE_SDP_BASE_GROUP,
    GST_DEBUG_CATEGORY_INIT (kms_sdp_bundle_group_debug_category, OBJECT_NAME,
        0, "debug category for sdp bundle_group"));

#define KMS_SDP_BUNDLE_GROUP_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                  \
    (obj),                                       \
    KMS_TYPE_SDP_BUNDLE_GROUP,                   \
    KmsSdpBundleGroupPrivate                     \
  )                                              \
)

#define KMS_SDP_BUNDLE_GROUP_SEMANTICS "BUNDLE"

struct _KmsSdpBundleGroupPrivate
{
  gchar *semantics;
};

static void
kms_sdp_bundle_group_finalize (GObject * object)
{
  KmsSdpBundleGroup *self = KMS_SDP_BUNDLE_GROUP (object);

  GST_DEBUG_OBJECT (self, "finalize");

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static gboolean
kms_sdp_bundle_group_add_media_handler (KmsSdpBaseGroup * grp,
    KmsSdpHandler * handler, GError ** error)
{
  /* Allow to add this media to this group only if this is not in other group */
  if (g_slist_length (handler->groups) > 0) {
    return FALSE;
  }

  return KMS_SDP_BASE_GROUP_CLASS (parent_class)->add_media_handler (grp,
      handler, error);
}

static gboolean
kms_sdp_bundle_group_add_offer_attributes_impl (KmsSdpBaseGroup * self,
    GstSDPMessage * offer, GError ** error)
{
  /* TODO: Check if there is any other bundle group and if there */
  /* are the same handlers in it */

  return KMS_SDP_BASE_GROUP_CLASS (parent_class)->add_offer_attributes (self,
      offer, error);
}

static void
kms_sdp_bundle_group_class_init (KmsSdpBundleGroupClass * klass)
{
  KmsSdpBaseGroupClass *base_group_class;
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_sdp_bundle_group_finalize;

  base_group_class = KMS_SDP_BASE_GROUP_CLASS (klass);
  base_group_class->add_offer_attributes =
      kms_sdp_bundle_group_add_offer_attributes_impl;
  base_group_class->add_media_handler = kms_sdp_bundle_group_add_media_handler;

  g_type_class_add_private (klass, sizeof (KmsSdpBundleGroupPrivate));
}

static void
kms_sdp_bundle_group_init (KmsSdpBundleGroup * self)
{
  self->priv = KMS_SDP_BUNDLE_GROUP_GET_PRIVATE (self);

  g_object_set (self, "pre-media-processing", FALSE, NULL);

  g_object_set (self, "semantics", KMS_SDP_BUNDLE_GROUP_SEMANTICS, NULL);
}

KmsSdpBundleGroup *
kms_sdp_bundle_group_new ()
{
  KmsSdpBundleGroup *obj;

  obj = KMS_SDP_BUNDLE_GROUP (g_object_new (KMS_TYPE_SDP_BUNDLE_GROUP,
          "semantics", KMS_SDP_BUNDLE_GROUP_SEMANTICS, NULL));

  return obj;
}
