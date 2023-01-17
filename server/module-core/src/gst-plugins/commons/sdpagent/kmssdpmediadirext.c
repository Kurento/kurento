/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

#include "kmsisdpmediaextension.h"
#include "kmssdpmediadirext.h"
#include "kms-sdp-agent-marshal.h"
#include "kmssdpagent.h"

#define OBJECT_NAME "sdpmediadirext"

GST_DEBUG_CATEGORY_STATIC (kms_sdp_media_direction_ext_debug_category);
#define GST_CAT_DEFAULT kms_sdp_media_direction_ext_debug_category

#define parent_class kms_sdp_media_direction_ext_parent_class

static void kms_i_sdp_media_extension_init (KmsISdpMediaExtensionInterface *
    iface);

G_DEFINE_TYPE_WITH_CODE (KmsSdpMediaDirectionExt, kms_sdp_media_direction_ext,
    G_TYPE_OBJECT,
    G_IMPLEMENT_INTERFACE (KMS_TYPE_I_SDP_MEDIA_EXTENSION,
        kms_i_sdp_media_extension_init)
    GST_DEBUG_CATEGORY_INIT (kms_sdp_media_direction_ext_debug_category,
        OBJECT_NAME, 0, "debug category for sdp media direction_ext"));

enum
{
  SIGNAL_ON_OFFER_MEDIA_DIRECTION,
  SIGNAL_ON_ANSWER_MEDIA_DIRECTION,
  SIGNAL_ON_ANSWERED_MEDIA_DIRECTION,
  LAST_SIGNAL
};

static guint obj_signals[LAST_SIGNAL] = { 0 };

static const gchar *
kms_sdp_media_direction_to_string (GstSDPDirection dir)
{
  switch (dir) {
    case GST_SDP_DIRECTION_SENDONLY:
      return SENDONLY_STR;
    case GST_SDP_DIRECTION_RECVONLY:
      return RECVONLY_STR;
    case GST_SDP_DIRECTION_SENDRECV:
      return SENDRECV_STR;
    case GST_SDP_DIRECTION_INACTIVE:
      return INACTIVE_STR;
    default:
      return NULL;
  }
}

static gboolean
kms_sdp_media_direction_ext_add_offer_attributes (KmsISdpMediaExtension * ext,
    GstSDPMedia * offer, GError ** error)
{
  GstSDPDirection dir;
  gboolean ret;

  g_signal_emit (G_OBJECT (ext), obj_signals[SIGNAL_ON_OFFER_MEDIA_DIRECTION],
      0, &dir);

  ret = sdp_utils_media_config_set_direction (offer, dir);

  if (!ret) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_PARAMETER,
        "Unexpected error setting media direction '%s'",
        kms_sdp_media_direction_to_string (dir));
  }

  return ret;
}

static gboolean
kms_sdp_media_direction_ext_add_answer_attributes (KmsISdpMediaExtension * ext,
    const GstSDPMedia * offer, GstSDPMedia * answer, GError ** error)
{
  GstSDPDirection offer_dir, answer_dir;
  gboolean ret;

  offer_dir = sdp_utils_media_config_get_direction (offer);

  g_signal_emit (G_OBJECT (ext), obj_signals[SIGNAL_ON_ANSWER_MEDIA_DIRECTION],
      0, offer_dir, &answer_dir);

  ret = sdp_utils_media_config_set_direction (answer, answer_dir);

  if (!ret) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_PARAMETER,
        "Unexpected error setting media direction '%s'",
        kms_sdp_media_direction_to_string (answer_dir));
  }

  return ret;
}

static gboolean
kms_sdp_media_direction_ext_can_insert_attribute (KmsISdpMediaExtension * ext,
    const GstSDPMedia * offer, const GstSDPAttribute * attr,
    GstSDPMedia * answer, const GstSDPMessage * msg)
{
  GST_DEBUG_OBJECT (ext, "Insertion of %s:%s ?", attr->key, attr->value);

  return FALSE;
}

static gboolean
kms_sdp_media_direction_ext_process_answer_attributes (KmsISdpMediaExtension *
    ext, const GstSDPMedia * answer, GError ** error)
{
  GstSDPDirection dir;

  dir = sdp_utils_media_config_get_direction (answer);

  g_signal_emit (G_OBJECT (ext),
      obj_signals[SIGNAL_ON_ANSWERED_MEDIA_DIRECTION], 0, dir);

  return TRUE;
}

static void
kms_sdp_media_direction_ext_class_init (KmsSdpMediaDirectionExtClass * klass)
{
  obj_signals[SIGNAL_ON_OFFER_MEDIA_DIRECTION] =
      g_signal_new ("on-offer-media-direction",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsSdpMediaDirectionExtClass, on_offer_media_direction),
      g_signal_accumulator_first_wins, NULL,
      __kms_sdp_agent_marshal_UINT__VOID, G_TYPE_UINT, 0);

  obj_signals[SIGNAL_ON_ANSWER_MEDIA_DIRECTION] =
      g_signal_new ("on-answer-media-direction",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsSdpMediaDirectionExtClass, on_answer_media_direction),
      g_signal_accumulator_first_wins, NULL,
      __kms_sdp_agent_marshal_UINT__UINT, G_TYPE_UINT, 1, G_TYPE_UINT);

  obj_signals[SIGNAL_ON_ANSWERED_MEDIA_DIRECTION] =
      g_signal_new ("on-answered-media-direction",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsSdpMediaDirectionExtClass,
          on_answered_media_direction), NULL, NULL,
      g_cclosure_marshal_VOID__UINT, G_TYPE_NONE, 1, G_TYPE_UINT);
}

static void
kms_sdp_media_direction_ext_init (KmsSdpMediaDirectionExt * self)
{
}

static void
kms_i_sdp_media_extension_init (KmsISdpMediaExtensionInterface * iface)
{
  iface->add_offer_attributes =
      kms_sdp_media_direction_ext_add_offer_attributes;
  iface->add_answer_attributes =
      kms_sdp_media_direction_ext_add_answer_attributes;
  iface->can_insert_attribute =
      kms_sdp_media_direction_ext_can_insert_attribute;
  iface->process_answer_attributes =
      kms_sdp_media_direction_ext_process_answer_attributes;
}

KmsSdpMediaDirectionExt *
kms_sdp_media_direction_ext_new ()
{
  gpointer obj;

  obj = g_object_new (KMS_TYPE_SDP_MEDIA_DIRECTION_EXT, NULL);

  return KMS_SDP_MEDIA_DIRECTION_EXT (obj);
}
