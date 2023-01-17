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

#include <stdlib.h>

#include "sdp_utils.h"
#include "kmssdpagent.h"
#include "kmssdpredundantext.h"
#include "kmsisdpmediaextension.h"
#include "kms-sdp-agent-marshal.h"

#define OBJECT_NAME "sdpredundantext"

GST_DEBUG_CATEGORY_STATIC (kms_sdp_redundant_ext_debug_category);
#define GST_CAT_DEFAULT kms_sdp_redundant_ext_debug_category

#define parent_class kms_sdp_redundant_ext_parent_class

static void kms_i_sdp_media_extension_init (KmsISdpMediaExtensionInterface *
    iface);

G_DEFINE_TYPE_WITH_CODE (KmsSdpRedundantExt, kms_sdp_redundant_ext,
    G_TYPE_OBJECT,
    G_IMPLEMENT_INTERFACE (KMS_TYPE_I_SDP_MEDIA_EXTENSION,
        kms_i_sdp_media_extension_init)
    GST_DEBUG_CATEGORY_INIT (kms_sdp_redundant_ext_debug_category, OBJECT_NAME,
        0, "debug category for sdp redundant_ext"));

#define KMS_REDUNDANT "red"

enum
{
  SIGNAL_ON_OFFERED_REDUNDANCY,

  LAST_SIGNAL
};

static guint obj_signals[LAST_SIGNAL] = { 0 };

static gboolean
kms_sdp_redundant_ext_add_redundant_attrs (KmsISdpMediaExtension * ext,
    GstSDPMedia * media, gint pt, gint clock_rate, GError ** error)
{
  gboolean ret;
  gchar *fmt;
  guint len;

  len = gst_sdp_media_formats_len (media);

  if (len == 0) {
    GST_WARNING_OBJECT (ext, "No medias");
    return TRUE;
  }

  if (sdp_utils_is_pt_in_fmts (media, pt)) {
    g_set_error (error, KMS_SDP_AGENT_ERROR,
        SDP_AGENT_INVALID_PARAMETER,
        "Payload type (%i) already assigned in the SDP media", pt);
    return FALSE;
  }

  fmt = g_strdup_printf ("%i", pt);

  if (sdp_utils_get_attr_map_value (media, "rtpmap", fmt) != NULL) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR,
        SDP_AGENT_UNEXPECTED_ERROR,
        "rtpmap attribute for ulpfec already added");
    g_free (fmt);
    return FALSE;
  }

  ret = gst_sdp_media_add_format (media, fmt) == GST_SDP_OK;
  g_free (fmt);

  if (!ret) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR,
        SDP_AGENT_UNEXPECTED_ERROR, "Can not add red payload");
    return FALSE;
  }

  return ret;
}

static gboolean
kms_sdp_redundant_ext_add_offer_attributes (KmsISdpMediaExtension * ext,
    GstSDPMedia * offer, GError ** error)
{
  /* Do not add anything to the offer */
  return TRUE;
}

static gboolean
kms_sdp_redundant_ext_add_answer_attributes (KmsISdpMediaExtension * ext,
    const GstSDPMedia * offer, GstSDPMedia * answer, GError ** error)
{
  gboolean managed = FALSE;
  gint pt, clock_rate;

  if (!sdp_utils_get_data_from_rtpmap_codec (offer, KMS_REDUNDANT, &pt,
          &clock_rate)) {
    return TRUE;
  }

  g_signal_emit (G_OBJECT (ext), obj_signals[SIGNAL_ON_OFFERED_REDUNDANCY], 0,
      pt, clock_rate, &managed);

  if (!managed) {
    /* Do not add atributes to the answer */
    return TRUE;
  }

  return kms_sdp_redundant_ext_add_redundant_attrs (ext, answer, pt, clock_rate,
      error);
}

static gboolean
kms_sdp_redundant_ext_can_insert_attribute (KmsISdpMediaExtension * ext,
    const GstSDPMedia * offer, const GstSDPAttribute * attr,
    GstSDPMedia * answer, const GstSDPMessage * msg)
{
  return FALSE;
}

static gboolean
kms_sdp_redundant_ext_process_answer_attributes (KmsISdpMediaExtension * ext,
    const GstSDPMedia * answer, GError ** error)
{
  /* So far, ulpfec is only supported on reception. */
  /* This callback is only called as response to a previous offer */

  return TRUE;
}

static void
kms_sdp_redundant_ext_class_init (KmsSdpRedundantExtClass * klass)
{
  obj_signals[SIGNAL_ON_OFFERED_REDUNDANCY] =
      g_signal_new ("on-offered-redundancy",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsSdpRedundantExtClass, on_offered_redundancy),
      g_signal_accumulator_true_handled, NULL,
      __kms_sdp_agent_marshal_BOOLEAN__UINT_UINT, G_TYPE_BOOLEAN, 2,
      G_TYPE_UINT, G_TYPE_UINT);
}

static void
kms_sdp_redundant_ext_init (KmsSdpRedundantExt * self)
{
  /* Nothing to do */
}

static void
kms_i_sdp_media_extension_init (KmsISdpMediaExtensionInterface * iface)
{
  iface->add_offer_attributes = kms_sdp_redundant_ext_add_offer_attributes;
  iface->add_answer_attributes = kms_sdp_redundant_ext_add_answer_attributes;
  iface->can_insert_attribute = kms_sdp_redundant_ext_can_insert_attribute;
  iface->process_answer_attributes =
      kms_sdp_redundant_ext_process_answer_attributes;
}

KmsSdpRedundantExt *
kms_sdp_redundant_ext_new ()
{
  gpointer obj;

  obj = g_object_new (KMS_TYPE_SDP_REDUNDANT_EXT, NULL);

  return KMS_SDP_REDUNDANT_EXT (obj);
}
