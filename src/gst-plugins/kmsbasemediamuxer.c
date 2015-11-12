/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
#include "kmsbasemediamuxer.h"

#define OBJECT_NAME "basemediamuxer"

#define parent_class kms_base_media_muxer_parent_class

GST_DEBUG_CATEGORY_STATIC (kms_base_media_muxer_debug_category);
#define GST_CAT_DEFAULT kms_base_media_muxer_debug_category

G_DEFINE_TYPE_WITH_CODE (KmsBaseMediaMuxer, kms_base_media_muxer, G_TYPE_OBJECT,
    GST_DEBUG_CATEGORY_INIT (kms_base_media_muxer_debug_category, OBJECT_NAME,
        0, "debug category for muxing pipeline object"));

static void
kms_base_media_muxer_finalize (GObject * object)
{
  KmsBaseMediaMuxer *self = KMS_BASE_MEDIA_MUXER (object);

  GST_DEBUG_OBJECT (self, "finalize");

  gst_element_set_state (KMS_BASE_MEDIA_MUXER_PIPELINE (self), GST_STATE_NULL);
  g_clear_object (&KMS_BASE_MEDIA_MUXER_PIPELINE (self));
  g_rec_mutex_clear (&self->mutex);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

GstStateChangeReturn
kms_base_media_muxer_set_state_impl (KmsBaseMediaMuxer * obj, GstState state)
{
  g_return_val_if_fail (obj != NULL, GST_STATE_CHANGE_FAILURE);

  return gst_element_set_state (KMS_BASE_MEDIA_MUXER_PIPELINE (obj), state);
}

GstState
kms_base_media_muxer_get_state_impl (KmsBaseMediaMuxer * obj)
{
  return GST_STATE (KMS_BASE_MEDIA_MUXER_PIPELINE (obj));
}

GstClock *
kms_base_media_muxer_get_clock_impl (KmsBaseMediaMuxer * obj)
{
  g_return_val_if_fail (obj != NULL, GST_CLOCK_TIME_NONE);

  return GST_ELEMENT (KMS_BASE_MEDIA_MUXER_PIPELINE (obj))->clock;
}

GstBus *
kms_base_media_muxer_get_bus_impl (KmsBaseMediaMuxer * obj)
{
  g_return_val_if_fail (obj != NULL, NULL);

  return
      gst_pipeline_get_bus (GST_PIPELINE (KMS_BASE_MEDIA_MUXER_PIPELINE (obj)));
}

void
kms_base_media_muxer_dot_file_impl (KmsBaseMediaMuxer * obj)
{
  g_return_if_fail (obj != NULL);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (KMS_BASE_MEDIA_MUXER_PIPELINE
          (obj)), GST_DEBUG_GRAPH_SHOW_ALL,
      GST_ELEMENT_NAME (KMS_BASE_MEDIA_MUXER_PIPELINE (obj)));
}

static void
kms_base_media_muxer_class_init (KmsBaseMediaMuxerClass * klass)
{
  GObjectClass *objclass = G_OBJECT_CLASS (klass);

  objclass->finalize = kms_base_media_muxer_finalize;

  klass->set_state = kms_base_media_muxer_set_state_impl;
  klass->get_state = kms_base_media_muxer_get_state_impl;
  klass->get_clock = kms_base_media_muxer_get_clock_impl;
  klass->get_bus = kms_base_media_muxer_get_bus_impl;
  klass->dot_file = kms_base_media_muxer_dot_file_impl;
}

static void
kms_base_media_muxer_init (KmsBaseMediaMuxer * self)
{
  g_rec_mutex_init (&self->mutex);
  KMS_BASE_MEDIA_MUXER_PIPELINE (self) = gst_pipeline_new (NULL);
}

GstStateChangeReturn
kms_base_media_muxer_set_state (KmsBaseMediaMuxer * obj, GstState state)
{
  g_return_val_if_fail (KMS_IS_BASE_MEDIA_MUXER (obj),
      GST_STATE_CHANGE_FAILURE);

  return KMS_BASE_MEDIA_MUXER_GET_CLASS (obj)->set_state (obj, state);
}

GstState
kms_base_media_muxer_get_state (KmsBaseMediaMuxer * obj)
{
  g_return_val_if_fail (KMS_IS_BASE_MEDIA_MUXER (obj), GST_STATE_VOID_PENDING);

  return KMS_BASE_MEDIA_MUXER_GET_CLASS (obj)->get_state (obj);
}

GstClock *
kms_base_media_muxer_get_clock (KmsBaseMediaMuxer * obj)
{
  g_return_val_if_fail (KMS_IS_BASE_MEDIA_MUXER (obj), NULL);

  return KMS_BASE_MEDIA_MUXER_GET_CLASS (obj)->get_clock (obj);
}

GstBus *
kms_base_media_muxer_get_bus (KmsBaseMediaMuxer * obj)
{
  g_return_val_if_fail (KMS_IS_BASE_MEDIA_MUXER (obj), NULL);

  return KMS_BASE_MEDIA_MUXER_GET_CLASS (obj)->get_bus (obj);
}

void
kms_base_media_muxer_dot_file (KmsBaseMediaMuxer * obj)
{
  g_return_if_fail (KMS_IS_BASE_MEDIA_MUXER (obj));

  KMS_BASE_MEDIA_MUXER_GET_CLASS (obj)->dot_file (obj);
}
