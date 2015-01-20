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

#include "kmsrtpbaseconnection.h"
#include <gio/gio.h>
#include <gst/gst.h>

#define GST_CAT_DEFAULT kmsrtpbaseconnection
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define GST_DEFAULT_NAME "kmsrtpbaseconnection"

G_DEFINE_TYPE (KmsRtpBaseConnection, kms_rtp_base_connection, G_TYPE_OBJECT);

static guint
kms_rtp_base_connection_get_rtp_port_default (KmsRtpBaseConnection * self)
{
  KmsRtpBaseConnectionClass *klass =
      KMS_RTP_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->get_rtp_port == kms_rtp_base_connection_get_rtp_port_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'get_rtp_port'", G_OBJECT_CLASS_NAME (klass));
  }

  return 0;
}

static guint
kms_rtp_base_connection_get_rtcp_port_default (KmsRtpBaseConnection * self)
{
  KmsRtpBaseConnectionClass *klass =
      KMS_RTP_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->get_rtcp_port == kms_rtp_base_connection_get_rtcp_port_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'get_rtcp_port'", G_OBJECT_CLASS_NAME (klass));
  }

  return 0;
}

static void
kms_rtp_base_connection_set_remote_info_default (KmsRtpBaseConnection * self,
    const gchar * host, gint rtp_port, gint rtcp_port)
{
  KmsRtpBaseConnectionClass *klass =
      KMS_RTP_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  if (klass->set_remote_info == kms_rtp_base_connection_set_remote_info_default) {
    GST_WARNING_OBJECT (self,
        "%s does not reimplement 'set_remote_info'",
        G_OBJECT_CLASS_NAME (klass));
  }
}

static void
kms_rtp_base_connection_init (KmsRtpBaseConnection * self)
{
  /* Nothing to do */
}

static void
kms_rtp_base_connection_class_init (KmsRtpBaseConnectionClass * klass)
{
  klass->get_rtp_port = kms_rtp_base_connection_get_rtp_port_default;
  klass->get_rtcp_port = kms_rtp_base_connection_get_rtcp_port_default;
  klass->set_remote_info = kms_rtp_base_connection_set_remote_info_default;

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);
}

guint
kms_rtp_base_connection_get_rtp_port (KmsRtpBaseConnection * self)
{
  KmsRtpBaseConnectionClass *klass =
      KMS_RTP_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->get_rtp_port (self);
}

guint
kms_rtp_base_connection_get_rtcp_port (KmsRtpBaseConnection * self)
{
  KmsRtpBaseConnectionClass *klass =
      KMS_RTP_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  return klass->get_rtcp_port (self);
}

void
kms_rtp_base_connection_set_remote_info (KmsRtpBaseConnection * self,
    const gchar * host, gint rtp_port, gint rtcp_port)
{
  KmsRtpBaseConnectionClass *klass =
      KMS_RTP_BASE_CONNECTION_CLASS (G_OBJECT_GET_CLASS (self));

  klass->set_remote_info (self, host, rtp_port, rtcp_port);
}
