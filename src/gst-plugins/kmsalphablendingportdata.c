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

#include "kmsalphablendingportdata.h"

GST_DEBUG_CATEGORY_STATIC (kms_alpha_blending_port_data_debug);
#define GST_CAT_DEFAULT kms_alpha_blending_port_data_debug

GType _kms_alpha_blending_port_data_type = 0;

GST_DEFINE_MINI_OBJECT_TYPE (KmsAlphaBlendingPortData,
    kms_alpha_blending_port_data);

static void
_priv_kms_alpha_blending_port_data_initialize (void)
{
  _kms_alpha_blending_port_data_type = kms_alpha_blending_port_data_get_type ();

  GST_DEBUG_CATEGORY_INIT (kms_alpha_blending_port_data_debug,
      "alphablendingportdata", 0, "alpha blending port data");
}

static void
_kms_alpha_blending_port_data_free (KmsAlphaBlendingPortData * data)
{
  g_return_if_fail (data != NULL);

  GST_DEBUG ("free");

  g_slice_free (KmsAlphaBlendingPortData, data);
}

KmsAlphaBlendingPortData *
kms_alpha_blending_port_data_new (KmsAlphaBlending * mixer, gint id)
{
  KmsAlphaBlendingPortData *data;

  data = g_slice_new0 (KmsAlphaBlendingPortData);

  gst_mini_object_init (GST_MINI_OBJECT_CAST (data), 0,
      _kms_alpha_blending_port_data_type, NULL, NULL,
      (GstMiniObjectFreeFunction) _kms_alpha_blending_port_data_free);

  data->mixer = mixer;
  data->id = id;

  return data;
}

static void _priv_kms_alpha_blending_port_data_initialize (void)
    __attribute__ ((constructor));
