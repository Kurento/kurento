/*
 * GStreamer
 *
 *  Copyright 2013 Collabora Ltd
 *   @author: Olivier Crete <olivier.crete@collabora.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 *
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gstdtlssrtpenc.h"

#include "gstdtls-enumtypes.h"

#include "ext/gio/kmsgtlsconnection.h"

GST_DEBUG_CATEGORY_STATIC (dtls_srtp_enc_debug);
#define GST_CAT_DEFAULT (dtls_srtp_enc_debug)

G_DEFINE_TYPE (GstDtlsSrtpEnc, gst_dtls_srtp_enc, GST_TYPE_BIN);

#ifdef GSTREAMER_1_3_FOUND
#define RTCP_SINK_TEMPLATE "rtcp_sink_%u"
#define RTP_SINK_TEMPLATE "rtp_sink_%u"
#else
#define RTCP_SINK_TEMPLATE "rtcp_sink_%d"
#define RTP_SINK_TEMPLATE "rtp_sink_%d"
#endif

enum
{
  PROP_CHANNEL_ID = 1,
  PROP_IS_CLIENT,
  PROP_TLS_CONNECTION,
  PROP_CERTIFICATE_PEM_FILE,
  PROP_CLIENT_VALIDATION_FLAGS,
  PROP_SRTP_PROFILES
};

#define DEFAULT_SRTP_PROFILES GST_DTLS_SRTP_PROFILE_AES128_CM_HMAC_SHA1_80

static GstStaticPadTemplate gst_dtls_srtp_enc_rtp_sink_template =
GST_STATIC_PAD_TEMPLATE ("rtp_sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("application/x-rtp"));

static GstStaticPadTemplate gst_dtls_srtp_enc_rtcp_sink_template =
GST_STATIC_PAD_TEMPLATE ("rtcp_sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("application/x-rtcp"));

static GstStaticPadTemplate gst_dtls_srtp_enc_src_template =
    GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("application/x-dtls;application/x-srtp;"
        "application/x-srtcp"));

static void gst_dtls_srtp_enc_set_property (GObject * object,
    guint prop_id, const GValue * value, GParamSpec * pspec);
static void gst_dtls_srtp_enc_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec);

static GstStateChangeReturn gst_dtls_srtp_enc_change_state (GstElement *
    element, GstStateChange transition);

static void
gst_dtls_srtp_enc_class_init (GstDtlsSrtpEncClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  GST_DEBUG_CATEGORY_INIT (dtls_srtp_enc_debug, "dtlssrtpenc", 0,
      "DTLS-SRTP encrypter");

  gobject_class->set_property = gst_dtls_srtp_enc_set_property;
  gobject_class->get_property = gst_dtls_srtp_enc_get_property;

  gstelement_class->change_state = gst_dtls_srtp_enc_change_state;

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&gst_dtls_srtp_enc_rtp_sink_template));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&gst_dtls_srtp_enc_rtcp_sink_template));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&gst_dtls_srtp_enc_src_template));

  gst_element_class_set_static_metadata (gstelement_class,
      "DTLS-SRTP encrypter",
      "Enc/Network",
      "Demultiplexes DTLS and RTP/RTCP/SRTP/SRTCP packets",
      "Olivier Crete <olivier.crete@collabora.com>");

  g_object_class_install_property (gobject_class, PROP_CHANNEL_ID,
      g_param_spec_string ("channel-id",
          "Channel ID",
          "ID of the TLS Channel, used to find matching decoder",
          "", G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_IS_CLIENT,
      g_param_spec_boolean ("is-client",
          "Is Client",
          "TRUE for a client, FALSE for a server",
          FALSE, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_TLS_CONNECTION,
      g_param_spec_object ("tls-connection",
          "TLS Connection",
          "TLS Connection object, only available in READY or higher",
          G_TYPE_TLS_CONNECTION, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_CERTIFICATE_PEM_FILE,
      g_param_spec_string ("certificate-pem-file",
          "Certificate PEM File",
          "PEM File name containing the certificate and private key",
          "",
          G_PARAM_READWRITE | GST_PARAM_MUTABLE_READY |
          G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_CLIENT_VALIDATION_FLAGS,
      g_param_spec_flags ("client-validation-flags",
          "Client Validation Flags",
          "Verifications to perform on the server's certificate if we are"
          " a client",
          G_TYPE_TLS_CERTIFICATE_FLAGS, G_TLS_CERTIFICATE_VALIDATE_ALL,
          G_PARAM_READWRITE | GST_PARAM_MUTABLE_READY |
          G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_SRTP_PROFILES,
      g_param_spec_flags ("srtp-profiles",
          "Acceptable SRTP profiles",
          "Verifications to perform on the server's certificate if we are"
          " a client",
          GST_TYPE_DTLS_SRTP_PROFILE, DEFAULT_SRTP_PROFILES,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
}

static void
gst_dtls_srtp_enc_init (GstDtlsSrtpEnc * self)
{
  GstPadTemplate *tmpl;
  GstPad *srcpad;

  self->profiles = DEFAULT_SRTP_PROFILES;

  self->out_funnel = gst_element_factory_make ("funnel", NULL);
  if (!self->out_funnel) {
    GST_ERROR_OBJECT (self, "Could not create required elements, "
        "missing core elements");
    return;
  }
  gst_bin_add (GST_BIN (self), self->out_funnel);

  self->dtls_enc = gst_element_factory_make ("dtlsenc", NULL);
  gst_bin_add (GST_BIN (self), self->dtls_enc);

  gst_element_link_pads (self->dtls_enc, "src", self->out_funnel, "sink_1");

  srcpad = gst_element_get_static_pad (self->out_funnel, "src");
  tmpl = gst_static_pad_template_get (&gst_dtls_srtp_enc_src_template);
  self->srcpad = gst_ghost_pad_new_from_template ("src", srcpad, tmpl);
  g_object_unref (tmpl);
  gst_object_unref (srcpad);
  gst_element_add_pad (GST_ELEMENT (self), self->srcpad);

  tmpl = gst_static_pad_template_get (&gst_dtls_srtp_enc_rtp_sink_template);
  self->rtp_sinkpad =
      gst_ghost_pad_new_no_target_from_template ("rtp_sink", tmpl);
  g_object_unref (tmpl);
  gst_element_add_pad (GST_ELEMENT (self), self->rtp_sinkpad);

  tmpl = gst_static_pad_template_get (&gst_dtls_srtp_enc_rtcp_sink_template);
  self->rtcp_sinkpad =
      gst_ghost_pad_new_no_target_from_template ("rtcp_sink", tmpl);
  g_object_unref (tmpl);
  gst_element_add_pad (GST_ELEMENT (self), self->rtcp_sinkpad);
}

static void
gst_dtls_srtp_enc_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstDtlsSrtpEnc *self = GST_DTLS_SRTP_ENC (object);

  switch (prop_id) {
    case PROP_CHANNEL_ID:
      g_object_set_property (G_OBJECT (self->dtls_enc), "channel-id", value);
      break;
    case PROP_IS_CLIENT:
      g_object_set_property (G_OBJECT (self->dtls_enc), "is-client", value);
      break;
    case PROP_CERTIFICATE_PEM_FILE:
      g_object_set_property (G_OBJECT (self->dtls_enc),
          "certificate-pem-file", value);
      break;
    case PROP_CLIENT_VALIDATION_FLAGS:
      g_object_set_property (G_OBJECT (self->dtls_enc),
          "client-validation-flags", value);
      break;
    case PROP_SRTP_PROFILES:
      self->profiles = g_value_get_flags (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }

}

static void
gst_dtls_srtp_enc_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstDtlsSrtpEnc *self = GST_DTLS_SRTP_ENC (object);

  switch (prop_id) {
    case PROP_CHANNEL_ID:
      g_object_get_property (G_OBJECT (self->dtls_enc), "channel-id", value);
      break;
    case PROP_IS_CLIENT:
      g_object_get_property (G_OBJECT (self->dtls_enc), "is-client", value);
      break;
    case PROP_CERTIFICATE_PEM_FILE:
      g_object_get_property (G_OBJECT (self->dtls_enc),
          "certificate-pem-file", value);
      break;
    case PROP_TLS_CONNECTION:
      g_object_get_property (G_OBJECT (self->dtls_enc),
          "tls-connection", value);
      break;
    case PROP_CLIENT_VALIDATION_FLAGS:
      g_object_get_property (G_OBJECT (self->dtls_enc),
          "client-validation-flags", value);
      break;
    case PROP_SRTP_PROFILES:
      g_value_set_flags (value, self->profiles);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
clear_pad_blocks (GstDtlsSrtpEnc * self)
{
  if (self->rtp_probe_id)
    gst_pad_remove_probe (self->rtp_sinkpad, self->rtp_probe_id);
  self->rtp_probe_id = 0;
  if (self->rtcp_probe_id)
    gst_pad_remove_probe (self->rtcp_sinkpad, self->rtcp_probe_id);
  self->rtcp_probe_id = 0;
}

static void
release_funnel_pad (const GValue * item, gpointer user_data)
{
  GstElement *funnel = user_data;
  GstPad *pad = g_value_get_object (item);

  gst_element_release_request_pad (funnel, pad);
}

static void
tls_status_changed (GTlsConnection * connection, GParamSpec * param,
    GstDtlsSrtpEnc * self)
{
  GTlsStatus status;
  GTlsSrtpProfile profile;
  GByteArray *key = NULL;
  GByteArray *salt = NULL;
  gboolean is_client;

  g_object_get (connection, "status", &status, NULL);
  if (status != G_TLS_STATUS_CONNECTED && status != G_TLS_STATUS_REHANDSHAKING)
    return;

  g_object_get (self->dtls_enc, "is-client", &is_client, NULL);

  if (is_client)
    profile = g_tls_connection_get_selected_srtp_profile (connection,
        NULL, NULL, &key, &salt);
  else
    profile = g_tls_connection_get_selected_srtp_profile (connection,
        &key, &salt, NULL, NULL);

  if (profile == G_TLS_SRTP_PROFILE_NONE) {
    GstPad *pad;

    gst_buffer_replace (&self->key_and_salt, NULL);
    self->srtp_profile = G_TLS_SRTP_PROFILE_NONE;

    clear_pad_blocks (self);

    if (!self->in_funnel) {
      self->in_funnel = gst_element_factory_make ("funnel", NULL);
      gst_bin_add (GST_BIN (self), self->in_funnel);
      gst_element_sync_state_with_parent (self->in_funnel);

      gst_element_link_pads (self->in_funnel, "src", self->dtls_enc, "sink");
    }

    pad = gst_element_request_pad (self->in_funnel,
        gst_element_class_get_pad_template (GST_ELEMENT_GET_CLASS
            (self->in_funnel), "sink_%u"), NULL, NULL);
    gst_ghost_pad_set_target (GST_GHOST_PAD (self->rtp_sinkpad), pad);
    gst_object_unref (pad);

    pad = gst_element_request_pad (self->in_funnel,
        gst_element_class_get_pad_template (GST_ELEMENT_GET_CLASS
            (self->in_funnel), "sink_%u"), NULL, NULL);
    gst_ghost_pad_set_target (GST_GHOST_PAD (self->rtcp_sinkpad), pad);
    gst_object_unref (pad);
  } else {
    GstBuffer *key_and_salt;
    gboolean add_enc = FALSE;

    g_byte_array_append (key, salt->data, salt->len);
    g_byte_array_unref (salt);

    if (profile == self->srtp_profile &&
        self->key_and_salt != NULL &&
        gst_buffer_memcmp (self->key_and_salt, 0, key->data, key->len)) {
      /* Nothing changed, ignore this */
      g_byte_array_free (key, TRUE);
      return;
    }

    key_and_salt = gst_buffer_new_wrapped (key->data, key->len);
    g_byte_array_free (key, FALSE);

    if (!self->srtp_enc) {
      self->srtp_enc = gst_element_factory_make ("srtpenc", NULL);
      add_enc = TRUE;
    }

    switch (profile) {
      case G_TLS_SRTP_PROFILE_AES128_CM_HMAC_SHA1_80:
        g_object_set (self->srtp_enc, "rtp-cipher", 1, "rtcp-cipher", 1,
            "rtp-auth", 2, "rtcp-auth", 2, "key", key_and_salt, NULL);
        break;
      case G_TLS_SRTP_PROFILE_AES128_CM_HMAC_SHA1_32:
        g_object_set (self->srtp_enc, "rtp-cipher", 1, "rtcp-cipher", 1,
            "rtp-auth", 1, "rtcp-auth", 1, "key", key_and_salt, NULL);
        break;
      case G_TLS_SRTP_PROFILE_NULL_HMAC_SHA1_80:
        g_object_set (self->srtp_enc, "rtp-cipher", 0, "rtcp-cipher", 0,
            "rtp-auth", 1, "rtcp-auth", 1, "key", key_and_salt, NULL);
        break;
      case G_TLS_SRTP_PROFILE_NULL_HMAC_SHA1_32:
        g_object_set (self->srtp_enc, "rtp-cipher", 0, "rtcp-cipher", 0,
            "rtp-auth", 1, "rtcp-auth", 1, "key", key_and_salt, NULL);
        break;
      default:
        g_assert_not_reached ();
    }

    GST_OBJECT_LOCK (self);
    gst_buffer_replace (&self->key_and_salt, key_and_salt);
    self->srtp_profile = profile;
    GST_OBJECT_UNLOCK (self);
    gst_buffer_unref (key_and_salt);

    if (add_enc) {
      gst_bin_add (GST_BIN (self), self->srtp_enc);
      self->srtpenc_rtpsink = gst_element_request_pad (self->srtp_enc,
          gst_element_class_get_pad_template (GST_ELEMENT_GET_CLASS
              (self->srtp_enc), RTP_SINK_TEMPLATE), "rtp_sink_1", NULL);
      self->srtpenc_rtcpsink =
          gst_element_request_pad (self->srtp_enc,
          gst_element_class_get_pad_template (GST_ELEMENT_GET_CLASS
              (self->srtp_enc), RTCP_SINK_TEMPLATE), "rtcp_sink_1", NULL);

      /* Release the extra ref */
      gst_object_unref (self->srtpenc_rtpsink);
      gst_object_unref (self->srtpenc_rtcpsink);

      gst_element_link_pads (self->srtp_enc, "rtp_src_1",
          self->out_funnel, NULL);
      gst_element_link_pads (self->srtp_enc, "rtcp_src_1",
          self->out_funnel, NULL);

      gst_element_sync_state_with_parent (self->srtp_enc);
    }

    gst_ghost_pad_set_target (GST_GHOST_PAD (self->rtp_sinkpad),
        self->srtpenc_rtpsink);
    gst_ghost_pad_set_target (GST_GHOST_PAD (self->rtcp_sinkpad),
        self->srtpenc_rtcpsink);

    if (self->in_funnel) {
      GstIterator *it;

      it = gst_element_iterate_sink_pads (self->in_funnel);
      while (gst_iterator_foreach (it, release_funnel_pad, self->in_funnel) ==
          GST_ITERATOR_RESYNC)
        gst_iterator_resync (it);
      gst_iterator_free (it);
    }

    clear_pad_blocks (self);
  }
}

static GstStateChangeReturn
gst_dtls_srtp_enc_change_state (GstElement * element, GstStateChange transition)
{
  GstDtlsSrtpEnc *self = GST_DTLS_SRTP_ENC (element);
  GstStateChangeReturn ret;
  GstElementFactory *fact = NULL;
  gboolean is_client;

  fact = gst_element_factory_find ("srtpenc");
  if (!fact) {
    GST_ERROR_OBJECT (self, "Missing SRTP plugin");
  } else {
    gst_object_unref (fact);
  }

  if (fact == NULL || self->out_funnel == NULL || self->dtls_enc == NULL) {
    GST_ERROR_OBJECT (self, "Missing plugin is missing");
    return GST_STATE_CHANGE_FAILURE;
  }

  switch (transition) {
    case GST_STATE_CHANGE_READY_TO_PAUSED:
      self->rtp_probe_id = gst_pad_add_probe (self->rtp_sinkpad,
          GST_PAD_PROBE_TYPE_BLOCK_DOWNSTREAM, NULL, NULL, NULL);
      self->rtcp_probe_id = gst_pad_add_probe (self->rtcp_sinkpad,
          GST_PAD_PROBE_TYPE_BLOCK_DOWNSTREAM, NULL, NULL, NULL);
      gst_ghost_pad_set_target (GST_GHOST_PAD (self->rtp_sinkpad), NULL);
      gst_ghost_pad_set_target (GST_GHOST_PAD (self->rtcp_sinkpad), NULL);
      break;
    case GST_STATE_CHANGE_READY_TO_NULL:
      if (self->status_changed_id)
        g_signal_handler_disconnect (self->conn, self->status_changed_id);
      self->status_changed_id = 0;
      g_clear_object (&self->conn);
      break;
    default:
      break;
  }

  ret =
      GST_ELEMENT_CLASS (gst_dtls_srtp_enc_parent_class)->change_state (element,
      transition);
  if (ret == GST_STATE_CHANGE_FAILURE)
    return ret;

  switch (transition) {
    case GST_STATE_CHANGE_NULL_TO_READY:
      g_object_get (self->dtls_enc, "tls-connection", &self->conn, NULL);
      if (self->conn == NULL) {
        GST_ERROR_OBJECT (self, "Could not get TLS connection object");
        return GST_STATE_CHANGE_FAILURE;
      }

      if (self->profiles & GST_DTLS_SRTP_PROFILE_AES128_CM_HMAC_SHA1_80)
        g_tls_connection_add_srtp_profile (self->conn,
            G_TLS_SRTP_PROFILE_AES128_CM_HMAC_SHA1_80);
      else if (self->profiles & GST_DTLS_SRTP_PROFILE_AES128_CM_HMAC_SHA1_32)
        g_tls_connection_add_srtp_profile (self->conn,
            G_TLS_SRTP_PROFILE_AES128_CM_HMAC_SHA1_32);
      else if (self->profiles & GST_DTLS_SRTP_PROFILE_NULL_HMAC_SHA1_80)
        g_tls_connection_add_srtp_profile (self->conn,
            G_TLS_SRTP_PROFILE_NULL_HMAC_SHA1_80);
      else if (self->profiles & GST_DTLS_SRTP_PROFILE_NULL_HMAC_SHA1_32)
        g_tls_connection_add_srtp_profile (self->conn,
            G_TLS_SRTP_PROFILE_NULL_HMAC_SHA1_32);
      self->status_changed_id = g_signal_connect (self->conn, "notify::status",
          G_CALLBACK (tls_status_changed), self);

      break;
    case GST_STATE_CHANGE_PAUSED_TO_READY:
      clear_pad_blocks (self);
      gst_buffer_replace (&self->key_and_salt, NULL);
      self->srtp_profile = G_TLS_SRTP_PROFILE_NONE;
      break;
    case GST_STATE_CHANGE_READY_TO_PAUSED:
      g_object_get (self->dtls_enc, "is-client", &is_client, NULL);
      if (is_client)
        g_tls_connection_handshake_async (self->conn, G_PRIORITY_DEFAULT, NULL,
            NULL, NULL);
      break;
    case GST_STATE_CHANGE_READY_TO_NULL:
      if (self->srtp_enc)
        gst_bin_remove (GST_BIN (self), self->srtp_enc);
      self->srtp_enc = NULL;
      self->srtpenc_rtpsink = NULL;
      self->srtpenc_rtcpsink = NULL;
      break;
    default:
      break;
  }

  return ret;
}
