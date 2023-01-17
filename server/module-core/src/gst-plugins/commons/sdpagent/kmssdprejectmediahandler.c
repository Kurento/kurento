/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

#include "kmssdpagent.h"
#include "kmssdprejectmediahandler.h"

#define OBJECT_NAME "sdprejectmediahandler"

GST_DEBUG_CATEGORY_STATIC (kms_sdp_reject_media_handler_debug_category);
#define GST_CAT_DEFAULT kms_sdp_reject_media_handler_debug_category

#define parent_class kms_sdp_reject_media_handler_parent_class

G_DEFINE_TYPE_WITH_CODE (KmsSdpRejectMediaHandler, kms_sdp_reject_media_handler,
    KMS_TYPE_SDP_MEDIA_HANDLER,
    GST_DEBUG_CATEGORY_INIT (kms_sdp_reject_media_handler_debug_category,
        OBJECT_NAME, 0, "debug category for sdp reject media_handler"));

#define DEFAULT_SDP_MEDIA_REJECT_RTCP_MUX TRUE
#define DEFAULT_SDP_MEDIA_REJECT_RTCP_ENTRY TRUE

#define DEFAULT_RTCP_ENTRY_PORT 9

#define KMS_SDP_REJECT_MEDIA_HANDLER_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                       \
    (obj),                                            \
    KMS_TYPE_SDP_REJECT_MEDIA_HANDLER,                   \
    KmsSdpRejectMediaHandlerPrivate                      \
  )                                                   \
)

struct _KmsSdpRejectMediaHandlerPrivate
{
  gboolean rtcp_mux;
};

static GstSDPMedia *
kms_sdp_reject_media_handler_create_offer (KmsSdpMediaHandler * handler,
    const gchar * media, const GstSDPMedia * offer, GError ** error)
{
  /* Reject handler can not provide offer, it is mainly used for rejecting */
  /* media offers activating all media extensions availables just in case  */
  /* additional attributes were required. */

  g_set_error_literal (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_PARAMETER,
      "Media handler can not create offers");

  return NULL;
}

GstSDPMedia *
kms_sdp_reject_media_handler_create_answer (KmsSdpMediaHandler * handler,
    const GstSDPMessage * msg, const GstSDPMedia * offer, GError ** error)
{
  GstSDPMedia *m = NULL;

  if (gst_sdp_media_new (&m) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Can not create '%s' media answer", gst_sdp_media_get_media (offer));
    goto error;
  }

  /* Create m-line */
  if (!KMS_SDP_MEDIA_HANDLER_GET_CLASS (handler)->init_answer (handler, offer,
          m, error)) {
    goto error;
  }

  /* Add attributes to m-line */
  if (!KMS_SDP_MEDIA_HANDLER_GET_CLASS (handler)->add_answer_attributes
      (handler, offer, m, error)) {
    goto error;
  }

  return m;

error:

  if (m != NULL) {
    gst_sdp_media_free (m);
  }

  return NULL;
}

static gboolean
kms_sdp_reject_media_handler_init_answer (KmsSdpMediaHandler * handler,
    const GstSDPMedia * offer, GstSDPMedia * answer, GError ** error)
{
  guint i, len;

  GST_WARNING_OBJECT (handler, "Rejecting media: '%s'",
      gst_sdp_media_get_media (offer));

  if (gst_sdp_media_set_media (answer,
          gst_sdp_media_get_media (offer)) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_PARAMETER,
        "Error setting '%s' on rejected media", gst_sdp_media_get_media (offer));
    return FALSE;
  }

  if (gst_sdp_media_set_proto (answer,
          gst_sdp_media_get_proto (offer)) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_PARAMETER,
        "Error setting proto '%s' on rejected media", gst_sdp_media_get_proto (offer));
    return FALSE;
  }

  if (gst_sdp_media_set_port_info (answer, 0, 1) != GST_SDP_OK) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR,
        SDP_AGENT_INVALID_PARAMETER, "Error setting port on rejected media");
    return FALSE;
  }

  len = gst_sdp_media_formats_len (offer);
  for (i = 0; i < len; i++) {
    const gchar *format;

    format = gst_sdp_media_get_format (offer, i);
    gst_sdp_media_insert_format (answer, i, format);
  }

  return TRUE;
}

static GObject *
kms_sdp_reject_media_handler_constructor (GType gtype, guint n_properties,
    GObjectConstructParam * properties)
{
  GObjectConstructParam *property;
  gchar const *name;
  GObject *object;
  guint i;

  for (i = 0, property = properties; i < n_properties; ++i, ++property) {
    name = g_param_spec_get_name (property->pspec);
    if (g_strcmp0 (name, "proto") == 0) {
      /* change G_PARAM_CONSTRUCT_ONLY value */
      g_value_set_string (property->value, "");
    }
  }

  object =
      G_OBJECT_CLASS (parent_class)->constructor (gtype, n_properties,
      properties);

  return object;
}

static void
kms_sdp_reject_media_handler_class_init (KmsSdpRejectMediaHandlerClass * klass)
{
  GObjectClass *gobject_class;
  KmsSdpMediaHandlerClass *handler_class;

  gobject_class = G_OBJECT_CLASS (klass);

  gobject_class->constructor = kms_sdp_reject_media_handler_constructor;

  handler_class = KMS_SDP_MEDIA_HANDLER_CLASS (klass);

  handler_class->create_offer = kms_sdp_reject_media_handler_create_offer;
  handler_class->create_answer = kms_sdp_reject_media_handler_create_answer;
  handler_class->init_answer = kms_sdp_reject_media_handler_init_answer;
}

static void
kms_sdp_reject_media_handler_init (KmsSdpRejectMediaHandler * self)
{
  self->priv = KMS_SDP_REJECT_MEDIA_HANDLER_GET_PRIVATE (self);
}

KmsSdpRejectMediaHandler *
kms_sdp_reject_media_handler_new ()
{
  KmsSdpRejectMediaHandler *handler;

  handler =
      KMS_SDP_REJECT_MEDIA_HANDLER (g_object_new
      (KMS_TYPE_SDP_REJECT_MEDIA_HANDLER, NULL));

  return handler;
}
