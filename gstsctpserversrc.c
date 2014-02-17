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
#  include <config.h>
#endif

#include <gio/gio.h>
#include "gstsctp.h"
#include "gstsctpserversrc.h"

#define PLUGIN_NAME "sctpserversrc"

GST_DEBUG_CATEGORY_STATIC (gst_sctp_server_src_debug_category);
#define GST_CAT_DEFAULT gst_sctp_server_src_debug_category

G_DEFINE_TYPE_WITH_CODE (GstSCTPServerSrc, gst_sctp_server_src,
    GST_TYPE_PUSH_SRC,
    GST_DEBUG_CATEGORY_INIT (gst_sctp_server_src_debug_category, PLUGIN_NAME,
        0, "debug category for sctp server source"));

#define GST_SCTP_SERVER_SRC_GET_PRIVATE(obj) \
  (G_TYPE_INSTANCE_GET_PRIVATE ((obj), GST_TYPE_SCTP_SERVER_SRC, GstSCTPServerSrcPrivate))

struct _GstSCTPServerSrcPrivate
{
  /* socket */
  GSocket *server_socket;
  GSocket *client_socket;
  GCancellable *cancellable;

  /* server information */
  int current_port;             /* currently bound-to port, or 0 *//* ATOMIC */
  int server_port;              /* port property */
  gchar *host;
};

enum
{
  PROP_0,
  PROP_HOST,
  PROP_PORT,
  PROP_CURRENT_PORT
};

static GstStaticPadTemplate srctemplate = GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);

static void
gst_sctp_server_src_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstSCTPServerSrc *self;

  g_return_if_fail (GST_IS_SCTP_SERVER_SRC (object));
  self = GST_SCTP_SERVER_SRC (object);

  switch (prop_id) {
    case PROP_HOST:
      if (!g_value_get_string (value)) {
        GST_WARNING ("host property cannot be NULL");
        break;
      }
      g_free (self->priv->host);
      self->priv->host = g_strdup (g_value_get_string (value));
      break;
    case PROP_PORT:
      self->priv->server_port = g_value_get_int (value);
      break;

    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_sctp_server_src_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstSCTPServerSrc *self;

  g_return_if_fail (GST_IS_SCTP_SERVER_SRC (object));
  self = GST_SCTP_SERVER_SRC (object);

  switch (prop_id) {
    case PROP_HOST:
      g_value_set_string (value, self->priv->host);
      break;
    case PROP_PORT:
      g_value_set_int (value, self->priv->server_port);
      break;
    case PROP_CURRENT_PORT:
      g_value_set_int (value, g_atomic_int_get (&self->priv->current_port));
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_sctp_server_src_finalize (GObject * gobject)
{
  GstSCTPServerSrc *self = GST_SCTP_SERVER_SRC (gobject);

  g_free (self->priv->host);
}

static void
gst_sctp_server_src_class_init (GstSCTPServerSrcClass * klass)
{
  GstElementClass *gstelement_class;
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->set_property = gst_sctp_server_src_set_property;
  gobject_class->get_property = gst_sctp_server_src_get_property;
  gobject_class->finalize = gst_sctp_server_src_finalize;

  g_object_class_install_property (gobject_class, PROP_HOST,
      g_param_spec_string ("bind-address", "Bind Address",
          "The address to bind the socket to",
          SCTP_DEFAULT_HOST,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_PORT,
      g_param_spec_int ("port", "Port",
          "The port to listen to (0=random available port)",
          0, G_MAXUINT16, SCTP_DEFAULT_PORT,
          G_PARAM_READWRITE | G_PARAM_CONSTRUCT | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_CURRENT_PORT,
      g_param_spec_int ("current-port", "current-port",
          "The port number the socket is currently bound to", 0,
          G_MAXUINT16, 0, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  gstelement_class = GST_ELEMENT_CLASS (klass);

  gst_element_class_set_static_metadata (gstelement_class,
      "SCTP server source", "Source/Network",
      "Receive data as a server over the network via SCTP",
      "Santiago Carot-Nemesio <sancane at gmail dot com>");

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&srctemplate));

  g_type_class_add_private (klass, sizeof (GstSCTPServerSrcPrivate));
}

static void
gst_sctp_server_src_init (GstSCTPServerSrc * self)
{
  self->priv = GST_SCTP_SERVER_SRC_GET_PRIVATE (self);
}

gboolean
gst_sctp_server_src_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      GST_TYPE_SCTP_SERVER_SRC);
}
