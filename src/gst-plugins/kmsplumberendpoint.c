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

#include <gst/gst.h>
#include "commons/kmsutils.h"
#include "kmsplumberendpoint.h"
#include "kmsmultichannelcontroller.h"

#define parent_class kms_plumber_endpoint_parent_class

#define SCTP_DEFAULT_ADDR "localhost"
#define SCTP_DEFAULT_LOCAL_PORT 0
#define SCTP_DEFAULT_REMOTE_PORT 9999

#define PLUGIN_NAME "plumberendpoint"
#define GST_CAT_DEFAULT kms_plumber_endpoint_debug_category
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define KMS_PLUMBER_ENDPOINT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (               \
    (obj),                                    \
    KMS_TYPE_PLUMBER_ENDPOINT,                \
    KmsPlumberEndpointPrivate                 \
  )                                           \
)

struct _KmsPlumberEndpointPrivate
{
  KmsMultiChannelController *mcc;

  gchar *local_addr;
  gchar *remote_addr;

  guint16 local_port;
  guint16 remote_port;

  /* SCTP server elements */
  GstElement *audiosrc;
  GstElement *videosrc;

  /* SCTP client elements */
  GstElement *audiosink;
  GstElement *videosink;
};

enum
{
  PROP_0,
  PROP_LOCAL_ADDR,
  PROP_LOCAL_PORT,
  PROP_REMOTE_ADDR,
  PROP_REMOTE_PORT,
  N_PROPERTIES
};

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsPlumberEndpoint, kms_plumber_endpoint,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_plumber_endpoint_debug_category, PLUGIN_NAME,
        0, "debug category for plumberendpoint element"));

static void
kms_plumber_endpoint_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsPlumberEndpoint *plumberendpoint = KMS_PLUMBER_ENDPOINT (object);

  switch (property_id) {
    case PROP_LOCAL_ADDR:
      if (!g_value_get_string (value)) {
        GST_WARNING ("local-address property cannot be NULL");
        break;
      }

      g_free (plumberendpoint->priv->local_addr);
      plumberendpoint->priv->local_addr = g_value_dup_string (value);
      break;
    case PROP_LOCAL_PORT:
      plumberendpoint->priv->local_port = g_value_get_int (value);
      break;
    case PROP_REMOTE_ADDR:
      if (!g_value_get_string (value)) {
        GST_WARNING ("remote-address property cannot be NULL");
        break;
      }

      g_free (plumberendpoint->priv->remote_addr);
      plumberendpoint->priv->remote_addr = g_value_dup_string (value);
      break;
    case PROP_REMOTE_PORT:
      plumberendpoint->priv->remote_port = g_value_get_int (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_plumber_endpoint_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsPlumberEndpoint *plumberendpoint = KMS_PLUMBER_ENDPOINT (object);

  switch (property_id) {
    case PROP_LOCAL_ADDR:
      g_value_set_string (value, plumberendpoint->priv->local_addr);
      break;
    case PROP_LOCAL_PORT:
      g_value_set_int (value, plumberendpoint->priv->local_port);
      break;
    case PROP_REMOTE_ADDR:
      g_value_set_string (value, plumberendpoint->priv->remote_addr);
      break;
    case PROP_REMOTE_PORT:
      g_value_set_int (value, plumberendpoint->priv->remote_port);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_plumber_endpoint_finalize (GObject * object)
{
  KmsPlumberEndpoint *plumberendpoint = KMS_PLUMBER_ENDPOINT (object);

  g_free (plumberendpoint->priv->local_addr);
  g_free (plumberendpoint->priv->remote_addr);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static void
kms_plumber_endpoint_link_valve (KmsPlumberEndpoint * self, GstElement * valve,
    GstElement ** sctpsink, gchar * remote_addr, guint16 remote_port)
{
  if (*sctpsink != NULL) {
    GST_ERROR_OBJECT (self, "Stream is already connected");
    return;
  }

  *sctpsink = gst_element_factory_make ("sctpclientsink", NULL);
  g_object_set (G_OBJECT (*sctpsink), "host", remote_addr, "port", remote_port,
      NULL);

  gst_bin_add (GST_BIN (self), *sctpsink);
  gst_element_sync_state_with_parent (*sctpsink);

  if (!gst_element_link (valve, *sctpsink)) {
    GST_ERROR_OBJECT (self, "Could not link %s to element %s",
        GST_ELEMENT_NAME (valve), GST_ELEMENT_NAME (*sctpsink));
  } else {
    /* Open valve so that buffers and events can pass throug it */
    kms_utils_set_valve_drop (valve, FALSE);
  }
}

static void
kms_plumber_endpoint_audio_valve_added (KmsElement * self, GstElement * valve)
{
  KmsPlumberEndpoint *plumber = KMS_PLUMBER_ENDPOINT (self);

  kms_plumber_endpoint_link_valve (plumber, valve, &plumber->priv->audiosink,
      plumber->priv->remote_addr, plumber->priv->remote_port);
}

static void
kms_plumber_endpoint_audio_valve_removed (KmsElement * self, GstElement * valve)
{
  GST_INFO ("TODO: Implement this");
}

static void
kms_plumber_endpoint_video_valve_added (KmsElement * self, GstElement * valve)
{
  KmsPlumberEndpoint *plumber = KMS_PLUMBER_ENDPOINT (self);

  kms_plumber_endpoint_link_valve (plumber, valve, &plumber->priv->videosink,
      plumber->priv->remote_addr, plumber->priv->remote_port + 1);
}

static void
kms_plumber_endpoint_video_valve_removed (KmsElement * self, GstElement * valve)
{
  GST_INFO ("TODO: Implement this");
}

static GstStateChangeReturn
kms_plumber_endpoint_change_state (GstElement * element,
    GstStateChange transition)
{
  GstStateChangeReturn ret;
  KmsPlumberEndpoint *self = KMS_PLUMBER_ENDPOINT (element);

  switch (transition) {
    case GST_STATE_CHANGE_NULL_TO_READY:
      self->priv->mcc =
          kms_multi_channel_controller_new (self->priv->local_addr,
          self->priv->local_port);
      kms_multi_channel_controller_start (self->priv->mcc);

      /* Try to connect control link if remote address is provided */
      if (self->priv->remote_addr != NULL) {
        GError *err = NULL;

        if (!kms_multi_channel_controller_connect (self->priv->mcc,
                self->priv->remote_addr, self->priv->remote_port, &err)) {
          GST_ERROR_OBJECT (self, "%s", err->message);
          g_error_free (err);
        }
      }
      break;
    default:
      break;
  }

  ret = GST_ELEMENT_CLASS (parent_class)->change_state (element, transition);

  switch (transition) {
    case GST_STATE_CHANGE_READY_TO_NULL:{
      kms_multi_channel_controller_stop (self->priv->mcc);
      kms_multi_channel_controller_unref (self->priv->mcc);
      break;
    }
    default:
      break;
  }

  return ret;
}

static void
kms_plumber_endpoint_class_init (KmsPlumberEndpointClass * klass)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  KmsElementClass *kms_element_class = KMS_ELEMENT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "PlumberEndpoint", "SCTP/Generic", "Kurento plugin plumber end point",
      "Santiago Carot-Nemesio <sancane at gmail dot com>");

  gobject_class->finalize = kms_plumber_endpoint_finalize;
  gobject_class->set_property = kms_plumber_endpoint_set_property;
  gobject_class->get_property = kms_plumber_endpoint_get_property;

  g_object_class_install_property (gobject_class, PROP_LOCAL_ADDR,
      g_param_spec_string ("local-address", "Local Address",
          "The local address to bind the socket to",
          SCTP_DEFAULT_ADDR,
          G_PARAM_READWRITE | GST_PARAM_MUTABLE_READY |
          G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_REMOTE_ADDR,
      g_param_spec_string ("remote-address", "Remote Address",
          "The remote address to connect the socket to",
          NULL, G_PARAM_READWRITE | GST_PARAM_MUTABLE_READY |
          G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_LOCAL_PORT,
      g_param_spec_int ("local-port", "Local-port",
          "The port to listen to (0=random available port)", 0, G_MAXUINT16,
          SCTP_DEFAULT_LOCAL_PORT,
          G_PARAM_READWRITE | GST_PARAM_MUTABLE_READY |
          G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_REMOTE_PORT,
      g_param_spec_int ("remote-port", "Remote port",
          "The port to send the packets to", 0, G_MAXUINT16,
          0, G_PARAM_READWRITE | GST_PARAM_MUTABLE_READY |
          G_PARAM_STATIC_STRINGS));

  kms_element_class->audio_valve_added =
      GST_DEBUG_FUNCPTR (kms_plumber_endpoint_audio_valve_added);
  kms_element_class->video_valve_added =
      GST_DEBUG_FUNCPTR (kms_plumber_endpoint_video_valve_added);
  kms_element_class->audio_valve_removed =
      GST_DEBUG_FUNCPTR (kms_plumber_endpoint_audio_valve_removed);
  kms_element_class->video_valve_removed =
      GST_DEBUG_FUNCPTR (kms_plumber_endpoint_video_valve_removed);

  gstelement_class->change_state =
      GST_DEBUG_FUNCPTR (kms_plumber_endpoint_change_state);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsPlumberEndpointPrivate));
}

static void
kms_plumber_endpoint_init (KmsPlumberEndpoint * self)
{
  self->priv = KMS_PLUMBER_ENDPOINT_GET_PRIVATE (self);
}

gboolean
kms_plumber_endpoint_plugin_init (GstPlugin * plugin)
{

  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_PLUMBER_ENDPOINT);
}
