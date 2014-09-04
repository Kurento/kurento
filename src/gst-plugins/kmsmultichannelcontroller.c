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

#include "kmsmultichannelcontroller.h"
#include "kmsmccp.h"

GST_DEBUG_CATEGORY_STATIC (kms_multi_channel_controller_debug);
#define GST_CAT_DEFAULT kms_multi_channel_controller_debug

GType _kms_multi_channel_controller_type = 0;

struct _KmsMultiChannelController
{
  GstMiniObject obj;

  gchar *host;
  guint16 port;
};

GST_DEFINE_MINI_OBJECT_TYPE (KmsMultiChannelController,
    kms_multi_channel_controller);

static void
_priv_kms_multi_channel_controller_initialize (void)
{
  _kms_multi_channel_controller_type = kms_multi_channel_controller_get_type ();

  GST_DEBUG_CATEGORY_INIT (kms_multi_channel_controller_debug,
      "multichannelcontroller", 0, "multi-channel controller protocol");
}

static void
_kms_multi_channel_controller_free (KmsMultiChannelController * mcc)
{
  g_return_if_fail (mcc != NULL);

  GST_DEBUG ("free");

  if (mcc->host != NULL)
    g_free (mcc->host);

  g_slice_free1 (sizeof (KmsMultiChannelController), mcc);
}

KmsMultiChannelController *
kms_multi_channel_controller_new ()
{
  KmsMultiChannelController *mcc;

  mcc = g_slice_new0 (KmsMultiChannelController);

  gst_mini_object_init (GST_MINI_OBJECT_CAST (mcc), 0,
      _kms_multi_channel_controller_type, NULL, NULL,
      (GstMiniObjectFreeFunction) _kms_multi_channel_controller_free);

  return KMS_MULTI_CHANNEL_CONTROLLER (mcc);
}

static void _priv_kms_multi_channel_controller_initialize (void)
    __attribute__ ((constructor));
