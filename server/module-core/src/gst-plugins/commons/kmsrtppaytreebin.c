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
#  include <config.h>
#endif

#include "kmsrtppaytreebin.h"
#include "kmsutils.h"

#define GST_DEFAULT_NAME "rtppaytreebin"
#define GST_CAT_DEFAULT kms_rtp_pay_tree_bin_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_rtp_pay_tree_bin_parent_class parent_class
G_DEFINE_TYPE (KmsRtpPayTreeBin, kms_rtp_pay_tree_bin, KMS_TYPE_TREE_BIN);

#define PICTURE_ID_15_BIT 2

static GstElement *
create_payloader_for_caps (const GstCaps * caps)
{
  GList *payloader_list, *filtered_list, *l;
  GstElementFactory *payloader_factory = NULL;
  GstElement *payloader = NULL;

  payloader_list =
      gst_element_factory_list_get_elements (GST_ELEMENT_FACTORY_TYPE_PAYLOADER,
      GST_RANK_NONE);
  filtered_list =
      gst_element_factory_list_filter (payloader_list, caps, GST_PAD_SRC,
      FALSE);

  for (l = filtered_list; l != NULL && payloader_factory == NULL; l = l->next) {
    payloader_factory = GST_ELEMENT_FACTORY (l->data);
    if (gst_element_factory_get_num_pad_templates (payloader_factory) != 2)
      payloader_factory = NULL;
  }

  if (payloader_factory != NULL) {
    payloader = gst_element_factory_create (payloader_factory, NULL);
  }

  if (payloader) {
    GParamSpec *pspec;

    pspec =
        g_object_class_find_property (G_OBJECT_GET_CLASS (payloader),
        "config-interval");
    if (pspec != NULL && G_PARAM_SPEC_VALUE_TYPE (pspec) == G_TYPE_INT) {
      g_object_set (payloader, "config-interval", 1, NULL);
    }

    pspec =
        g_object_class_find_property (G_OBJECT_GET_CLASS (payloader),
        "picture-id-mode");
    if (pspec != NULL && G_TYPE_IS_ENUM (G_PARAM_SPEC_VALUE_TYPE (pspec))) {
      /* Set picture id so that remote peer can determine continuity if */
      /* there are lost FEC packets and if it has to NACK them */
      g_object_set (payloader, "picture-id-mode", PICTURE_ID_15_BIT, NULL);
    }
  }

  gst_plugin_feature_list_free (filtered_list);
  gst_plugin_feature_list_free (payloader_list);

  return payloader;
}

static gboolean
kms_rtp_pay_tree_bin_configure (KmsRtpPayTreeBin * self, const GstCaps * caps)
{
  KmsTreeBin *tree_bin = KMS_TREE_BIN (self);
  GstElement *pay, *output_tee;
  GstPad *pad;

  pay = create_payloader_for_caps (caps);
  if (pay == NULL) {
    GST_WARNING_OBJECT (self, "Cannot find payloader for caps %" GST_PTR_FORMAT,
        caps);
    return FALSE;
  }
  GST_DEBUG_OBJECT (self, "Payloader found: %" GST_PTR_FORMAT, pay);

  pad = gst_element_get_static_pad (pay, "sink");
  kms_utils_drop_until_keyframe (pad, TRUE);
  gst_object_unref (pad);

  gst_bin_add (GST_BIN (self), pay);
  gst_element_sync_state_with_parent (pay);

  kms_tree_bin_set_input_element (tree_bin, pay);
  output_tee = kms_tree_bin_get_output_tee (tree_bin);
  gst_element_link (pay, output_tee);

  return TRUE;
}

KmsRtpPayTreeBin *
kms_rtp_pay_tree_bin_new (const GstCaps * caps)
{
  GObject *dec;

  dec = g_object_new (KMS_TYPE_RTP_PAY_TREE_BIN, NULL);
  if (!kms_rtp_pay_tree_bin_configure (KMS_RTP_PAY_TREE_BIN (dec), caps)) {
    g_object_unref (dec);
    return NULL;
  }

  return KMS_RTP_PAY_TREE_BIN (dec);
}

static void
kms_rtp_pay_tree_bin_init (KmsRtpPayTreeBin * self)
{
  /* Nothing to do */
}

static void
kms_rtp_pay_tree_bin_class_init (KmsRtpPayTreeBinClass * klass)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  gst_element_class_set_details_simple (gstelement_class,
      "RtpPayTreeBin",
      "Generic",
      "Bin to payload and distribute media.",
      "Jose Antonio Santos <santoscadenas@gmail.com>");

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);
}
