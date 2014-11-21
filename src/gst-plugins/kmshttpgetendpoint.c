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

#include "kmshttpgetendpoint.h"
#include "kmsconfcontroller.h"

#define PLUGIN_NAME "httpgetendpoint"

GST_DEBUG_CATEGORY_STATIC (kms_http_get_endpoint_debug_category);
#define GST_CAT_DEFAULT kms_http_get_endpoint_debug_category

#define KMS_HTTP_GET_ENDPOINT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                   \
    (obj),                                        \
    KMS_TYPE_HTTP_GET_ENDPOINT,                   \
    KmsHttpGetEndpointPrivate                     \
  )                                               \
)

struct _KmsHttpGetEndpointPrivate
{
  GstElement *appsink;
  KmsConfController *controller;
};

G_DEFINE_TYPE_WITH_CODE (KmsHttpGetEndpoint, kms_http_get_endpoint,
    KMS_TYPE_HTTP_ENDPOINT,
    GST_DEBUG_CATEGORY_INIT (kms_http_get_endpoint_debug_category, PLUGIN_NAME,
        0, "debug category for http get endpoint plugin"));

static void
kms_http_get_endpoint_class_init (KmsHttpGetEndpointClass * klass)
{
  g_type_class_add_private (klass, sizeof (KmsHttpGetEndpointPrivate));
}

static void
kms_http_get_endpoint_init (KmsHttpGetEndpoint * self)
{
  self->priv = KMS_HTTP_GET_ENDPOINT_GET_PRIVATE (self);
}

gboolean
kms_http_get_endpoint_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_HTTP_GET_ENDPOINT);
}
