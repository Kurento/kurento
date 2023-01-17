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

#include <string.h>

#include "kmssdpconnectionext.h"
#include "kmsisdpmediaextension.h"
#include "kms-sdp-agent-marshal.h"

#define OBJECT_NAME "srcspecattrext"

GST_DEBUG_CATEGORY_STATIC (kms_connection_ext_debug_category);
#define GST_CAT_DEFAULT kms_connection_ext_debug_category

#define parent_class kms_connection_ext_parent_class

#define KMS_RESERVED_CONNECTION_SIZE 10

static void kms_i_sdp_media_extension_init (KmsISdpMediaExtensionInterface *
    iface);

G_DEFINE_TYPE_WITH_CODE (KmsConnectionExt, kms_connection_ext,
    G_TYPE_OBJECT,
    G_IMPLEMENT_INTERFACE (KMS_TYPE_I_SDP_MEDIA_EXTENSION,
        kms_i_sdp_media_extension_init)
    GST_DEBUG_CATEGORY_INIT (kms_connection_ext_debug_category, OBJECT_NAME,
        0, "debug category for sdp sdes_ext"));

enum
{
  SIGNAL_ON_ANSWER_IPS,
  SIGNAL_ON_OFFER_IPS,
  SIGNAL_ON_ANSWERED_IPS,

  LAST_SIGNAL
};

static guint obj_signals[LAST_SIGNAL] = { 0 };

static void
kms_connection_ext_class_init (KmsConnectionExtClass * klass)
{
  obj_signals[SIGNAL_ON_ANSWER_IPS] =
      g_signal_new ("on-answer-ips",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsConnectionExtClass, on_answer_ips),
      NULL, NULL, __kms_sdp_agent_marshal_VOID__BOXED_BOXED,
      G_TYPE_NONE, 2, G_TYPE_ARRAY, G_TYPE_ARRAY);

  obj_signals[SIGNAL_ON_OFFER_IPS] =
      g_signal_new ("on-offer-ips",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsConnectionExtClass, on_offer_ips),
      NULL, NULL, g_cclosure_marshal_VOID__BOXED, G_TYPE_NONE, 1, G_TYPE_ARRAY);

  obj_signals[SIGNAL_ON_ANSWERED_IPS] =
      g_signal_new ("on-answered-ips",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsConnectionExtClass, on_answered_ips),
      NULL, NULL, g_cclosure_marshal_VOID__BOXED, G_TYPE_NONE, 1, G_TYPE_ARRAY);
}

static void
kms_connection_ext_init (KmsConnectionExt * self)
{
  /* Nothing to do */
}

static void
kms_connection_attr_ext_add_ips (KmsISdpMediaExtension * ext,
    GstSDPMedia * media, GArray * ips)
{
  gint i;

  for (i = 0; i < ips->len; i++) {
    gchar *nettype, *addrtype, *address;
    guint ttl, addr_number;
    const GstStructure *addr;
    GValue *val;

    val = &g_array_index (ips, GValue, i);

    if (!GST_VALUE_HOLDS_STRUCTURE (val)) {
      GST_WARNING_OBJECT (ext, "Inavalid address provided");
      continue;
    }

    addr = gst_value_get_structure (val);
    nettype = addrtype = address = NULL;

    if (gst_structure_get (addr, "nettype", G_TYPE_STRING, &nettype,
            "addrtype", G_TYPE_STRING, &addrtype, "address", G_TYPE_STRING,
            &address, "ttl", G_TYPE_UINT, &ttl, "addrnumber", G_TYPE_UINT,
            &addr_number, NULL)) {
      gst_sdp_media_add_connection (media, nettype, addrtype, address, ttl,
          addr_number);
    }

    g_free (nettype);
    g_free (addrtype);
    g_free (address);
  }
}

static void
add_address_to_list (GArray * conns, const GstSDPConnection * conn)
{
  GValue val = G_VALUE_INIT;
  GstStructure *addr;

  addr = gst_structure_new ("sdp-connection", "nettype", G_TYPE_STRING,
      conn->nettype, "addrtype", G_TYPE_STRING, conn->addrtype, "address",
      G_TYPE_STRING, conn->address, "ttl", G_TYPE_UINT, conn->ttl, "addrnumber",
      G_TYPE_UINT, conn->addr_number, NULL);

  g_value_init (&val, GST_TYPE_STRUCTURE);
  gst_value_set_structure (&val, addr);
  gst_structure_free (addr);

  g_array_append_val (conns, val);
}

static void
kms_connection_ext_get_connection_attrs (KmsISdpMediaExtension * ext,
    const GstSDPMedia * media, GArray * conns)
{
  guint i, len;

  len = gst_sdp_media_connections_len (media);

  for (i = 0; i < len; i++) {
    const GstSDPConnection *conn;

    conn = gst_sdp_media_get_connection (media, i);

    add_address_to_list (conns, conn);
  }
}

static gboolean
kms_connection_ext_add_offer_attributes (KmsISdpMediaExtension * ext,
    GstSDPMedia * offer, GError ** error)
{
  GArray *ips;

  ips = g_array_sized_new (FALSE, FALSE, sizeof (GValue),
      KMS_RESERVED_CONNECTION_SIZE);
  g_array_set_clear_func (ips, (GDestroyNotify) g_value_unset);

  g_signal_emit (G_OBJECT (ext), obj_signals[SIGNAL_ON_OFFER_IPS], 0, ips);

  kms_connection_attr_ext_add_ips (ext, offer, ips);
  g_array_unref (ips);

  return TRUE;
}

static gboolean
kms_connection_ext_add_answer_attributes (KmsISdpMediaExtension * ext,
    const GstSDPMedia * offer, GstSDPMedia * answer, GError ** error)
{
  GArray *ips_offered, *ips_answered;

  ips_offered = g_array_sized_new (FALSE, FALSE, sizeof (GValue),
      KMS_RESERVED_CONNECTION_SIZE);
  ips_answered = g_array_sized_new (FALSE, FALSE, sizeof (GValue),
      KMS_RESERVED_CONNECTION_SIZE);

  g_array_set_clear_func (ips_offered, (GDestroyNotify) g_value_unset);
  g_array_set_clear_func (ips_answered, (GDestroyNotify) g_value_unset);

  kms_connection_ext_get_connection_attrs (ext, offer, ips_offered);

  g_signal_emit (G_OBJECT (ext), obj_signals[SIGNAL_ON_ANSWER_IPS], 0,
      ips_offered, ips_answered);

  kms_connection_attr_ext_add_ips (ext, answer, ips_answered);

  g_array_unref (ips_offered);
  g_array_unref (ips_answered);

  return TRUE;
}

static gboolean
kms_connection_ext_can_insert_attribute (KmsISdpMediaExtension * ext,
    const GstSDPMedia * offer, const GstSDPAttribute * attr,
    GstSDPMedia * answer, const GstSDPMessage * msg)
{
  /* No special management of attributes are required. We leave other   */
  /* plugin to decide if attributes are going to be added to the answer */

  return FALSE;
}

static gboolean
kms_connection_ext_process_answer_attributes (KmsISdpMediaExtension * ext,
    const GstSDPMedia * answer, GError ** error)
{
  GArray *ips;

  ips = g_array_sized_new (FALSE, FALSE, sizeof (GValue),
      KMS_RESERVED_CONNECTION_SIZE);
  g_array_set_clear_func (ips, (GDestroyNotify) g_value_unset);

  kms_connection_ext_get_connection_attrs (ext, answer, ips);

  g_signal_emit (G_OBJECT (ext), obj_signals[SIGNAL_ON_ANSWERED_IPS], 0, ips);

  g_array_unref (ips);

  return TRUE;
}

static void
kms_i_sdp_media_extension_init (KmsISdpMediaExtensionInterface * iface)
{
  iface->add_offer_attributes = kms_connection_ext_add_offer_attributes;
  iface->add_answer_attributes = kms_connection_ext_add_answer_attributes;
  iface->can_insert_attribute = kms_connection_ext_can_insert_attribute;
  iface->process_answer_attributes =
      kms_connection_ext_process_answer_attributes;
}

KmsConnectionExt *
kms_connection_ext_new ()
{
  gpointer obj;

  obj = g_object_new (KMS_TYPE_CONNECTION_EXT, NULL);

  return KMS_CONNECTION_EXT (obj);
}
