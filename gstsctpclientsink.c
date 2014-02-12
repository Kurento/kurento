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

#include "gstsctpclientsink.h"

#define PLUGIN_NAME "sctpclientsink"

#define SCTP_DEFAULT_HOST "localhost"
#define SCTP_DEFAULT_PORT 8000

GST_DEBUG_CATEGORY_STATIC (gst_sctp_client_sink_debug_category);
#define GST_CAT_DEFAULT gst_sctp_client_sink_debug_category

G_DEFINE_TYPE_WITH_CODE (GstSCTPClientSink, gst_sctp_client_sink,
    GST_TYPE_BIN,
    GST_DEBUG_CATEGORY_INIT (gst_sctp_client_sink_debug_category, PLUGIN_NAME,
        0, "debug category for element"));

#define GST_SCTP_CLIENT_SINK_GET_PRIVATE(obj) \
  (G_TYPE_INSTANCE_GET_PRIVATE ((obj), GST_TYPE_SCTP_CLIENT_SINK, GstSCTPClientSinkPrivate))

struct _GstSCTPClientSinkPrivate
{
  gint sockfd;
  guint16 num_ostreams;
  guint16 max_istreams;

  /* server information */
  gint port;
  gchar *host;
};

enum
{
  PROP_0,
  PROP_HOST,
  PROP_PORT,
  PROP_NUM_OSTREAMS,
  PROP_MAX_INSTREAMS
};

static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE ("sink_%u",
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS_ANY);

static gchar *
get_stream_id_from_padname (const gchar * name)
{
  GMatchInfo *match_info = NULL;
  GRegex *regex;
  gchar *id = NULL;

  if (name == NULL)
    return NULL;

  regex = g_regex_new ("^sink_(?<id>\\d+)", 0, 0, NULL);
  g_regex_match (regex, name, 0, &match_info);

  if (g_match_info_matches (match_info))
    id = g_match_info_fetch_named (match_info, "id");

  g_match_info_free (match_info);
  g_regex_unref (regex);

  return id;
}

static void
gst_sctp_client_sink_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstSCTPClientSink *stcpclientsink;

  g_return_if_fail (GST_IS_SCTP_CLIENT_SINK (object));
  stcpclientsink = GST_SCTP_CLIENT_SINK (object);

  switch (prop_id) {
    case PROP_HOST:
      if (g_value_get_string (value) == NULL) {
        GST_WARNING ("host property cannot be NULL");
        break;
      }
      g_free (stcpclientsink->priv->host);
      stcpclientsink->priv->host = g_strdup (g_value_get_string (value));
      break;
    case PROP_PORT:
      stcpclientsink->priv->port = g_value_get_int (value);
      break;
    case PROP_NUM_OSTREAMS:
      stcpclientsink->priv->num_ostreams = g_value_get_int (value);
      break;
    case PROP_MAX_INSTREAMS:
      stcpclientsink->priv->max_istreams = g_value_get_int (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_sctp_client_sink_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstSCTPClientSink *stcpclientsink;

  g_return_if_fail (GST_IS_SCTP_CLIENT_SINK (object));
  stcpclientsink = GST_SCTP_CLIENT_SINK (object);

  switch (prop_id) {
    case PROP_HOST:
      g_value_set_string (value, stcpclientsink->priv->host);
      break;
    case PROP_PORT:
      g_value_set_int (value, stcpclientsink->priv->port);
      break;
    case PROP_NUM_OSTREAMS:
      g_value_set_int (value, stcpclientsink->priv->num_ostreams);
      break;
    case PROP_MAX_INSTREAMS:
      g_value_set_int (value, stcpclientsink->priv->max_istreams);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_sctp_client_sink_finalize (GObject * gobject)
{
  GstSCTPClientSink *self = GST_SCTP_CLIENT_SINK (gobject);

  g_free (self->priv->host);
  self->priv->host = NULL;

  G_OBJECT_CLASS (gst_sctp_client_sink_parent_class)->finalize (gobject);
}

static GstPad *
gst_sctp_client_sink_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps)
{
  GstPad *sinkpad, *ghostpad;
  GstElement *sctpbasesink;
  gchar *padname;
  gchar *pad_id;
  gint64 val;
  guint16 id;

  pad_id = get_stream_id_from_padname (name);
  if (pad_id == NULL) {
    GST_WARNING
        ("Link of elements without using pad names is not yet supported");
    return NULL;
  }

  val = g_ascii_strtoll (pad_id, NULL, 10);
  g_free (pad_id);

  if (val > G_MAXUINT32) {
    GST_ERROR ("SCTP stream id %" G_GINT64_FORMAT " is not valid", val);
    return NULL;
  }

  id = val;

  sctpbasesink = gst_element_factory_make ("sctpbasesink", NULL);
  sinkpad = gst_element_get_static_pad (sctpbasesink, "sink");
  if (sinkpad == NULL) {
    GST_ERROR_OBJECT (sctpbasesink, "Can not get sink pad");
    gst_object_unref (sctpbasesink);
    return NULL;
  }

  g_object_set (sctpbasesink, "stream-id", id, NULL);

  gst_bin_add (GST_BIN (element), sctpbasesink);
  gst_element_sync_state_with_parent (sctpbasesink);

  padname = g_strdup_printf ("sink_%u", id);
  ghostpad = gst_ghost_pad_new_from_template (padname, sinkpad, templ);

  g_object_unref (sinkpad);
  g_free (padname);

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (ghostpad, TRUE);

  gst_element_add_pad (element, ghostpad);

  return ghostpad;
}

static void
gst_sctp_client_sink_release_pad (GstElement * element, GstPad * pad)
{
  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, FALSE);

  gst_element_remove_pad (element, pad);
}

static void
gst_sctp_client_sink_class_init (GstSCTPClientSinkClass * klass)
{
  GstElementClass *gstelement_class;
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->set_property = gst_sctp_client_sink_set_property;
  gobject_class->get_property = gst_sctp_client_sink_get_property;
  gobject_class->finalize = gst_sctp_client_sink_finalize;

  g_object_class_install_property (gobject_class, PROP_HOST,
      g_param_spec_string ("host", "Host", "The host/IP to send the packets to",
          SCTP_DEFAULT_HOST, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_PORT,
      g_param_spec_int ("port", "Port", "The port to send the packets to",
          0, G_MAXUINT16, SCTP_DEFAULT_PORT,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_NUM_OSTREAMS,
      g_param_spec_int ("num-ostreams", "Output streams",
          "This is the number of streams that the application wishes to be "
          "able to send to", 0, G_MAXUINT16, 1,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  g_object_class_install_property (gobject_class, PROP_MAX_INSTREAMS,
      g_param_spec_int ("max-instreams", "Inputput streams",
          "This value represents the maximum number of inbound streams the "
          "application is prepared to support", 0, G_MAXUINT16, 0,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  gstelement_class = GST_ELEMENT_CLASS (klass);

  gst_element_class_set_static_metadata (gstelement_class,
      "SCTP client sink", "Sink/Network",
      "Send data as a client over the network via SCTP",
      "Santiago Carot-Nemesio <sancane at gmail dot com>");

  gstelement_class->request_new_pad =
      GST_DEBUG_FUNCPTR (gst_sctp_client_sink_request_new_pad);
  gstelement_class->release_pad =
      GST_DEBUG_FUNCPTR (gst_sctp_client_sink_release_pad);
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&sink_factory));

  g_type_class_add_private (klass, sizeof (GstSCTPClientSinkPrivate));
}

static void
gst_sctp_client_sink_init (GstSCTPClientSink * self)
{
  self->priv = GST_SCTP_CLIENT_SINK_GET_PRIVATE (self);
  self->priv->sockfd = -1;
  self->priv->host = g_strdup (SCTP_DEFAULT_HOST);
  self->priv->port = SCTP_DEFAULT_PORT;

  g_object_set (G_OBJECT (self), "async-handling", TRUE, NULL);
}

gboolean
gst_sctp_client_sink_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      GST_TYPE_SCTP_CLIENT_SINK);
}
