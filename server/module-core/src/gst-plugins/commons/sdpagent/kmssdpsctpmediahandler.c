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
#include "sdp_utils.h"
#include "kmssdpsctpmediahandler.h"

#define OBJECT_NAME "sdpsctpmediahandler"

GST_DEBUG_CATEGORY_STATIC (kms_sdp_sctp_media_handler_debug_category);
#define GST_CAT_DEFAULT kms_sdp_sctp_media_handler_debug_category

#define parent_class kms_sdp_sctp_media_handler_parent_class

G_DEFINE_TYPE_WITH_CODE (KmsSdpSctpMediaHandler, kms_sdp_sctp_media_handler,
    KMS_TYPE_SDP_MEDIA_HANDLER,
    GST_DEBUG_CATEGORY_INIT (kms_sdp_sctp_media_handler_debug_category,
        OBJECT_NAME, 0, "debug category for sdp sctp media_handler"));

#define SDP_MEDIA_DTLS_SCTP_PROTO "DTLS/SCTP"
#define SDP_MEDIA_UDP_PROTO_INFO "UDP/"
#define SDP_MEDIA_UDP_DTLS_SCTP_PROTO SDP_MEDIA_UDP_PROTO_INFO SDP_MEDIA_DTLS_SCTP_PROTO

#define SDP_MEDIA_SCTP_PORT "5000"      // The default SCTP stream identifier

// RFC 8841: SDP Offer/Answer for SCTP over DTLS.
// https://www.rfc-editor.org/rfc/rfc8841
//
// SDP Media Description:
//
//     m=application 12345 UDP/DTLS/SCTP webrtc-datachannel
//     a=sctp-port:5000
//     a=max-message-size:100000
//
// * The "m=" line protocol can be "UDP/DTLS/SCTP" or "TCP/DTLS/SCTP". Its port
//   value indicates the underlying UDP or TCP port.
// * "a=sctp-port": Indicates the SCTP port.
//   0 = Close or reject an SCTP association.
//   Required.
// * "a=max-message-size": Indicates the maximum SCTP user message
//   size (in bytes) that the endpoint is willing to receive.
//   0 = messages can be of any size.
//   Optional. Default: 64K.

static GObject *
kms_sdp_sctp_media_handler_constructor (GType gtype, guint n_properties,
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
      g_value_set_string (property->value, SDP_MEDIA_DTLS_SCTP_PROTO);
    }
  }

  object =
      G_OBJECT_CLASS (parent_class)->constructor (gtype, n_properties,
      properties);

  return object;
}

static GstSDPMedia *
kms_sdp_sctp_media_handler_create_offer (KmsSdpMediaHandler * handler,
    const gchar * media, const GstSDPMedia * prev_offer, GError ** error)
{
  GstSDPMedia *m = NULL;

  if (gst_sdp_media_new (&m) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Cannot create '%s' media", media);
    goto error;
  }

  /* Create m-line */
  if (!KMS_SDP_MEDIA_HANDLER_GET_CLASS (handler)->init_offer (handler, media, m,
          prev_offer, error)) {
    goto error;
  }

  /* Add attributes to m-line */
  if (!KMS_SDP_MEDIA_HANDLER_GET_CLASS (handler)->add_offer_attributes (handler,
          m, prev_offer, error)) {
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
format_supported (const GstSDPMedia * media, const gchar * fmt)
{
  const gchar *val;

  // "m=" line format must be as specified.
  if (g_strcmp0 (fmt, SDP_MEDIA_SCTP_FMT) != 0) {
    return FALSE;
  }

  // "a=sctp-port" is mandatory.
  val = gst_sdp_media_get_attribute_val (media, SDP_MEDIA_SCTP_PORT_ATTR);
  if (val == NULL) {
    return FALSE;
  }

  return TRUE;
}

static gboolean
add_supported_sctp_attrs (const GstSDPMedia * offer, GstSDPMedia * answer,
    GError ** error)
{
  const gchar *fmt = gst_sdp_media_get_format (answer, 0);

  if (fmt == NULL || !format_supported (offer, fmt)) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Cannot process SCTP: Invalid media section in SDP offer");
    return FALSE;
  }

  const gchar *attr = SDP_MEDIA_SCTP_PORT_ATTR;
  const gchar *val_str = gst_sdp_media_get_attribute_val (offer, attr);

  if (gst_sdp_media_add_attribute (answer, attr, val_str) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Cannot add attribute 'a=%s:%s' from SDP offer", attr, val_str);
    return FALSE;
  }

  return TRUE;
}

static gboolean
kms_sdp_sctp_media_handler_can_insert_attribute (KmsSdpMediaHandler *
    handler, const GstSDPMedia * offer, const GstSDPAttribute * attr,
    GstSDPMedia * answer, const GstSDPMessage * msg)
{
  if (g_strcmp0 (attr->key, SDP_MEDIA_SCTPMAP_ATTR) == 0
      || g_strcmp0 (attr->key, SDP_MEDIA_SCTP_PORT_ATTR) == 0) {
    /* ignore */
    return FALSE;
  }

  if (sdp_utils_attribute_is_direction (attr, NULL)) {
    /* SDP direction attributes MUST be discarded if present. */
    /* [draft-ietf-mmusic-sctp-sdp] 9.2                       */
    return FALSE;
  }

  if (!KMS_SDP_MEDIA_HANDLER_CLASS (parent_class)->can_insert_attribute
      (handler, offer, attr, answer, msg)) {
    return FALSE;
  }

  return TRUE;
}

static GstSDPMedia *
kms_sdp_sctp_media_handler_create_answer (KmsSdpMediaHandler * handler,
    const GstSDPMessage * msg, const GstSDPMedia * offer, GError ** error)
{
  GstSDPMedia *m = NULL;

  if (gst_sdp_media_new (&m) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Cannot create '%s' media answer", gst_sdp_media_get_media (offer));
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

  if (!KMS_SDP_MEDIA_HANDLER_GET_CLASS (handler)->intersect_sdp_medias (handler,
          offer, m, msg, error)) {
    goto error;
  }

  return m;

error:

  if (m != NULL) {
    gst_sdp_media_free (m);
  }

  return NULL;
}

gboolean
kms_sdp_sctp_media_handler_manage_protocol (const gchar * protocol)
{
  GRegex *regex;
  gboolean ret;

  /* Support both DTLS/SCTP and UDP/DTLS/SCTP */

  regex =
      g_regex_new ("(" SDP_MEDIA_UDP_PROTO_INFO ")?"
      SDP_MEDIA_DTLS_SCTP_PROTO, 0, 0, NULL);
  ret = g_regex_match (regex, protocol, G_REGEX_MATCH_ANCHORED, NULL);
  g_regex_unref (regex);

  return ret;
}

static gboolean
kms_sdp_sctp_media_handler_manage_protocol_impl (KmsSdpMediaHandler * handler,
    const gchar * protocol)
{
  return kms_sdp_sctp_media_handler_manage_protocol (protocol);
}

struct intersect_data
{
  KmsSdpMediaHandler *handler;
  const GstSDPMedia *offer;
  GstSDPMedia *answer;
  const GstSDPMessage *msg;
};

static gboolean
instersect_sctp_media_attr (const GstSDPAttribute * attr, gpointer user_data)
{
  struct intersect_data *data = (struct intersect_data *) user_data;

  if (!KMS_SDP_MEDIA_HANDLER_GET_CLASS (data->
          handler)->can_insert_attribute (data->handler, data->offer, attr,
          data->answer, data->msg)) {
    return FALSE;
  }

  if (gst_sdp_media_add_attribute (data->answer, attr->key,
          attr->value) != GST_SDP_OK) {
    GST_WARNING ("Cannot add attribute '%s'", attr->key);
    return FALSE;
  }

  return TRUE;
}

static gboolean
kms_sdp_sctp_media_handler_intersect_sdp_medias (KmsSdpMediaHandler *
    handler, const GstSDPMedia * offer, GstSDPMedia * answer,
    const GstSDPMessage * msg, GError ** error)
{
  struct intersect_data data = {
    .handler = handler,
    .offer = offer,
    .answer = answer,
    .msg = msg
  };

  if (!sdp_utils_intersect_media_attributes (offer,
          instersect_sctp_media_attr, &data)) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR,
        SDP_AGENT_UNEXPECTED_ERROR, "Cannot intersect media attributes");
    return FALSE;
  }

  return TRUE;
}

static gboolean
kms_sdp_sctp_media_handler_init_offer (KmsSdpMediaHandler * handler,
    const gchar * media, GstSDPMedia * offer, const GstSDPMedia * prev_offer,
    GError ** error)
{
  gboolean ret = TRUE;

  if (g_strcmp0 (media, "application") != 0) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_MEDIA,
        "Unsupported '%s' media", media);
    ret = FALSE;
    goto end;
  }

  if (gst_sdp_media_set_media (offer, media) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Cannot set '%s' media", media);
    ret = FALSE;
    goto end;
  }

  if (gst_sdp_media_set_proto (offer, SDP_MEDIA_UDP_DTLS_SCTP_PROTO)
      != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Cannot set '%s' protocol", SDP_MEDIA_UDP_DTLS_SCTP_PROTO);
    ret = FALSE;
    goto end;
  }

  if (gst_sdp_media_set_port_info (offer, 1, 1) != GST_SDP_OK) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Cannot set port");
    ret = FALSE;
    goto end;
  }

  if (gst_sdp_media_add_format (offer, SDP_MEDIA_SCTP_FMT) != GST_SDP_OK) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Cannot set format");
    ret = FALSE;
    goto end;
  }

  if (gst_sdp_media_add_attribute (offer, "setup", "actpass") != GST_SDP_OK) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Cannot set attribute 'setup:actpass'");
    ret = FALSE;
    goto end;
  }

end:
  return ret;
}

static gboolean
kms_sdp_sctp_media_handler_add_offer_attributes (KmsSdpMediaHandler * handler,
    GstSDPMedia * offer, const GstSDPMedia * prev_offer, GError ** error)
{
  if (gst_sdp_media_add_attribute (offer, SDP_MEDIA_SCTP_PORT_ATTR,
          SDP_MEDIA_SCTP_PORT) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Cannot set attribute 'a=%s:%s'", SDP_MEDIA_SCTP_PORT_ATTR,
        SDP_MEDIA_SCTP_PORT);
    return FALSE;
  }

  /* Chain up */
  return
      KMS_SDP_MEDIA_HANDLER_CLASS (parent_class)->add_offer_attributes (handler,
      offer, prev_offer, error);
}

static gboolean
kms_sdp_sctp_media_handler_init_answer (KmsSdpMediaHandler * handler,
    const GstSDPMedia * offer, GstSDPMedia * answer, GError ** error)
{
  const gchar *proto;

  if (g_strcmp0 (gst_sdp_media_get_media (offer), "application") != 0) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_MEDIA,
        "Unsupported '%s' media", gst_sdp_media_get_media (offer));
    return FALSE;
  }

  proto = gst_sdp_media_get_proto (offer);

  if (!kms_sdp_media_handler_manage_protocol (handler, proto)) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_PROTOCOL,
        "Unexpected media protocol '%s'", gst_sdp_media_get_proto (offer));
    return FALSE;
  }

  if (gst_sdp_media_set_media (answer,
          gst_sdp_media_get_media (offer)) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_PARAMETER,
        "Cannot set '%s' media ttribute", gst_sdp_media_get_media (offer));
    return FALSE;
  }

  if (gst_sdp_media_set_proto (answer, proto) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_INVALID_PARAMETER,
        "Cannot set proto '%s' attribute", proto);
    return FALSE;
  }

  if (gst_sdp_media_set_port_info (answer, 1, 1) != GST_SDP_OK) {
    g_set_error_literal (error, KMS_SDP_AGENT_ERROR,
        SDP_AGENT_INVALID_PARAMETER, "Cannot set port attribute");
    return FALSE;
  }

  return TRUE;
}

static gboolean
kms_sdp_sctp_media_handler_add_answer_attributes_impl (KmsSdpMediaHandler *
    handler, const GstSDPMedia * offer, GstSDPMedia * answer, GError ** error)
{
  if (!KMS_SDP_MEDIA_HANDLER_CLASS (parent_class)
           ->add_answer_attributes (handler, offer, answer, error)) {
    return FALSE;
  }

  const gchar *fmt;

  fmt = gst_sdp_media_get_format (offer, 0);

  if (fmt == NULL || !format_supported (offer, fmt)) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Cannot process SCTP: Invalid SDP offer");
    return FALSE;
  }

  if (gst_sdp_media_add_format (answer, fmt) != GST_SDP_OK) {
    g_set_error (error, KMS_SDP_AGENT_ERROR, SDP_AGENT_UNEXPECTED_ERROR,
        "Cannot add format '%s' from SDP offer", fmt);
    return FALSE;
  }

  if (!add_supported_sctp_attrs (offer, answer, error)) {
    return FALSE;
  }

  return TRUE;
}

static void
kms_sdp_sctp_media_handler_class_init (KmsSdpSctpMediaHandlerClass * klass)
{
  GObjectClass *gobject_class;
  KmsSdpMediaHandlerClass *handler_class;

  gobject_class = G_OBJECT_CLASS (klass);

  gobject_class->constructor = kms_sdp_sctp_media_handler_constructor;

  handler_class = KMS_SDP_MEDIA_HANDLER_CLASS (klass);

  handler_class->create_offer = kms_sdp_sctp_media_handler_create_offer;
  handler_class->create_answer = kms_sdp_sctp_media_handler_create_answer;

  handler_class->manage_protocol =
      kms_sdp_sctp_media_handler_manage_protocol_impl;

  handler_class->can_insert_attribute =
      kms_sdp_sctp_media_handler_can_insert_attribute;
  handler_class->intersect_sdp_medias =
      kms_sdp_sctp_media_handler_intersect_sdp_medias;

  handler_class->init_offer = kms_sdp_sctp_media_handler_init_offer;
  handler_class->add_offer_attributes =
      kms_sdp_sctp_media_handler_add_offer_attributes;

  handler_class->init_answer = kms_sdp_sctp_media_handler_init_answer;
  handler_class->add_answer_attributes =
      kms_sdp_sctp_media_handler_add_answer_attributes_impl;
}

static void
kms_sdp_sctp_media_handler_init (KmsSdpSctpMediaHandler * self)
{
  /* Nothing to do here */
}

KmsSdpSctpMediaHandler *
kms_sdp_sctp_media_handler_new ()
{
  KmsSdpSctpMediaHandler *handler;

  handler =
      KMS_SDP_SCTP_MEDIA_HANDLER (g_object_new (KMS_TYPE_SDP_SCTP_MEDIA_HANDLER,
          NULL));

  return handler;
}
