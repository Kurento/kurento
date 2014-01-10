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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include "kmsconfcontroller.h"

#define NAME "confcontroller"

GST_DEBUG_CATEGORY_STATIC (kms_conf_controller_debug_category);
#define GST_CAT_DEFAULT kms_conf_controller_debug_category

G_DEFINE_TYPE_WITH_CODE (KmsConfController, kms_conf_controller,
    G_TYPE_OBJECT,
    GST_DEBUG_CATEGORY_INIT (kms_conf_controller_debug_category, NAME,
        0, "debug category for configuration controller"));

#define KMS_CONF_CONTROLLER_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (     \
    (obj),                          \
    KMS_TYPE_CONF_CONTROLLER,       \
    KmsConfControllerPrivate        \
  )                                 \
)
struct _KmsConfControllerPrivate
{
  GstElement *encodebin;
};

static void
kms_conf_controller_dispose (GObject * obj)
{
  G_OBJECT_CLASS (kms_conf_controller_parent_class)->dispose (obj);
}

static void
kms_conf_controller_finalize (GObject * obj)
{
  G_OBJECT_CLASS (kms_conf_controller_parent_class)->finalize (obj);
}

static void
kms_conf_controller_class_init (KmsConfControllerClass * klass)
{
  GObjectClass *objclass = G_OBJECT_CLASS (klass);

  objclass->dispose = kms_conf_controller_dispose;
  objclass->finalize = kms_conf_controller_finalize;

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsConfControllerPrivate));
}

static void
kms_conf_controller_init (KmsConfController * self)
{
  self->priv = KMS_CONF_CONTROLLER_GET_PRIVATE (self);
}
