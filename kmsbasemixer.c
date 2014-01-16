/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

#include "kmsbasemixer.h"

#define PLUGIN_NAME "basemixer"

GST_DEBUG_CATEGORY_STATIC (kms_base_mixer_debug_category);
#define GST_CAT_DEFAULT kms_base_mixer_debug_category

#define KMS_BASE_MIXER_GET_PRIVATE(obj) (       \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_BASE_MIXER,                        \
    KmsBaseMixerPrivate                         \
  )                                             \
)
struct _KmsBaseMixerPrivate
{
  GHashTable *ports;
};

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsBaseMixer, kms_base_mixer,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_base_mixer_debug_category, PLUGIN_NAME,
        0, "debug category for basemixer element"));

static void
kms_base_mixer_dispose (GObject * object)
{
  G_OBJECT_CLASS (kms_base_mixer_parent_class)->dispose (object);
}

static void
kms_base_mixer_finalize (GObject * object)
{
  G_OBJECT_CLASS (kms_base_mixer_parent_class)->finalize (object);
}

static void
kms_base_mixer_class_init (KmsBaseMixerClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "BaseMixer", "Generic", "Kurento plugin for mixer connection",
      "Jose Antonio Santos Cadenas <santoscadenas@gmail.com>");

  gobject_class->dispose = kms_base_mixer_dispose;
  gobject_class->finalize = kms_base_mixer_finalize;

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsBaseMixerPrivate));
}

static void
kms_base_mixer_init (KmsBaseMixer * self)
{
  self->priv = KMS_BASE_MIXER_GET_PRIVATE (self);
}
