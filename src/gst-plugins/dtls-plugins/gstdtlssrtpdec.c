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

#include "gstdtlssrtpdec.h"

#include "gstdtls-enumtypes.h"

#include "ext/gio/kmsgtlsconnection.h"

GST_DEBUG_CATEGORY_STATIC (dtls_srtp_dec_debug);
#define GST_CAT_DEFAULT (dtls_srtp_dec_debug)

G_DEFINE_TYPE (GstDtlsSrtpDec, gst_dtls_srtp_dec, GST_TYPE_BIN);


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

static GstStaticPadTemplate gst_dtls_srtp_dec_src_template =
    GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("application/x-rtp;application/x-rtcp"));

static GstStaticPadTemplate gst_dtls_srtp_dec_sink_template =
    GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("application/x-dtls;application/x-srtp;"
        "application/x-srtcp"));

static void gst_dtls_srtp_dec_set_property (GObject * object,
    guint prop_id, const GValue * value, GParamSpec * pspec);
static void gst_dtls_srtp_dec_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec);

static GstStateChangeReturn gst_dtls_srtp_dec_change_state (GstElement *
    element, GstStateChange transition);

static void
gst_dtls_srtp_dec_class_init (GstDtlsSrtpDecClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  GST_DEBUG_CATEGORY_INIT (dtls_srtp_dec_debug, "dtlssrtpdec", 0,
      "DTLS-SRTP decrypter");

  gobject_class->set_property = gst_dtls_srtp_dec_set_property;
  gobject_class->get_property = gst_dtls_srtp_dec_get_property;

  gstelement_class->change_state = gst_dtls_srtp_dec_change_state;

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&gst_dtls_srtp_dec_src_template));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&gst_dtls_srtp_dec_sink_template));

  gst_element_class_set_static_metadata (gstelement_class,
      "DTLS-SRTP decrypter",
      "Dec/Network",
      "Decrypts DTLS and SRTP/SRTCP packets",
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
gst_dtls_srtp_dec_init (GstDtlsSrtpDec * self)
{
  GstPadTemplate *tmpl;
  GstPad *pad;

  self->profiles = DEFAULT_SRTP_PROFILES;

  self->srtp_dec = gst_element_factory_make ("srtpdec", NULL);
  if (!self->srtp_dec) {
    GST_ERROR_OBJECT (self, "Could not create required elements, "
        "missing SRTP plugin");
    return;
  }
  gst_bin_add (GST_BIN (self), self->srtp_dec);

  self->rtcp_demux = gst_element_factory_make ("rtcpdemux", NULL);
  if (!self->rtcp_demux) {
    GST_ERROR_OBJECT (self, "Could not create required elements, "
        "missing rtcpdemux plugins");
    return;
  }
  gst_bin_add (GST_BIN (self), self->rtcp_demux);

  self->funnel = gst_element_factory_make ("funnel", NULL);
  if (!self->funnel) {
    GST_ERROR_OBJECT (self, "Could not create required elements, "
        "missing core elements");
    return;
  }
  gst_bin_add (GST_BIN (self), self->funnel);

  self->queue = gst_element_factory_make ("queue", NULL);
  if (!self->queue) {
    GST_ERROR_OBJECT (self, "Could not create required elements, "
        "missing core elements");
    return;
  }

  g_object_set (self->queue, "leaky", 2, NULL);
  gst_bin_add (GST_BIN (self), self->queue);

  self->dtls_dec = gst_element_factory_make ("dtlsdec", NULL);
  gst_bin_add (GST_BIN (self), self->dtls_dec);

  self->demux = gst_element_factory_make ("dtlssrtpdemux", NULL);
  gst_bin_add (GST_BIN (self), self->demux);

  pad = gst_element_get_static_pad (self->funnel, "src");
  tmpl = gst_static_pad_template_get (&gst_dtls_srtp_dec_src_template);
  self->srcpad = gst_ghost_pad_new_from_template ("src", pad, tmpl);
  g_object_unref (tmpl);
  gst_object_unref (pad);
  gst_element_add_pad (GST_ELEMENT (self), self->srcpad);

  pad = gst_element_get_static_pad (self->demux, "sink");
  tmpl = gst_static_pad_template_get (&gst_dtls_srtp_dec_sink_template);
  self->sinkpad = gst_ghost_pad_new_from_template ("sink", pad, tmpl);
  g_object_unref (tmpl);
  gst_object_unref (pad);
  gst_element_add_pad (GST_ELEMENT (self), self->sinkpad);

  gst_element_link_pads (self->srtp_dec, "rtp_src", self->funnel, NULL);
  gst_element_link_pads (self->srtp_dec, "rtcp_src", self->funnel, NULL);
  gst_element_link (self->queue, self->rtcp_demux);
  gst_element_link_pads (self->rtcp_demux, "rtp_src", self->srtp_dec, "rtp_sink");
  gst_element_link_pads (self->rtcp_demux, "rtcp_src", self->srtp_dec, "rtcp_sink");

  gst_element_link_pads (self->demux, "srtp_src", self->queue, "sink");
  gst_element_link (self->dtls_dec, self->funnel);
  gst_element_link_pads (self->demux, "dtls_src", self->dtls_dec, "sink");

  self->queue_srcpad = gst_element_get_static_pad (self->queue, "src");
  gst_object_unref (self->queue_srcpad);
}

static void
gst_dtls_srtp_dec_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstDtlsSrtpDec *self = GST_DTLS_SRTP_DEC (object);

  switch (prop_id) {
    case PROP_CHANNEL_ID:
      g_object_set_property (G_OBJECT (self->dtls_dec), "channel-id", value);
      break;
    case PROP_IS_CLIENT:
      g_object_set_property (G_OBJECT (self->dtls_dec), "is-client", value);
      break;
    case PROP_CERTIFICATE_PEM_FILE:
      g_object_set_property (G_OBJECT (self->dtls_dec),
          "certificate-pem-file", value);
      break;
    case PROP_CLIENT_VALIDATION_FLAGS:
      g_object_set_property (G_OBJECT (self->dtls_dec),
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
gst_dtls_srtp_dec_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstDtlsSrtpDec *self = GST_DTLS_SRTP_DEC (object);

  switch (prop_id) {
    case PROP_CHANNEL_ID:
      g_object_get_property (G_OBJECT (self->dtls_dec), "channel-id", value);
      break;
    case PROP_IS_CLIENT:
      g_object_get_property (G_OBJECT (self->dtls_dec), "is-client", value);
      break;
    case PROP_CERTIFICATE_PEM_FILE:
      g_object_get_property (G_OBJECT (self->dtls_dec),
          "certificate-pem-file", value);
      break;
    case PROP_TLS_CONNECTION:
      g_object_get_property (G_OBJECT (self->dtls_dec),
          "tls-connection", value);
      break;
    case PROP_CLIENT_VALIDATION_FLAGS:
      g_object_get_property (G_OBJECT (self->dtls_dec),
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
block_queue (GstDtlsSrtpDec * self)
{
  if (self->queue_probe_id == 0)
    self->queue_probe_id = gst_pad_add_probe (self->queue_srcpad,
        GST_PAD_PROBE_TYPE_BLOCK_DOWNSTREAM, NULL, NULL, NULL);
}

static void
clear_queue_block (GstDtlsSrtpDec * self)
{
  if (self->queue_probe_id)
    gst_pad_remove_probe (self->queue_srcpad, self->queue_probe_id);
  self->queue_probe_id = 0;
}

static void
tls_status_changed (GTlsConnection * connection, GParamSpec * param,
    GstDtlsSrtpDec * self)
{
  GTlsStatus status;
  GTlsSrtpProfile profile;
  GByteArray *key = NULL;
  GByteArray *salt = NULL;
  gboolean is_client;

  g_object_get (connection, "status", &status, NULL);

  if (status != G_TLS_STATUS_CONNECTED && status != G_TLS_STATUS_REHANDSHAKING)
    return;

  g_object_get (self->dtls_dec, "is-client", &is_client, NULL);

  if (is_client)
    profile = g_tls_connection_get_selected_srtp_profile (connection,
        &key, &salt, NULL, NULL);
  else
    profile = g_tls_connection_get_selected_srtp_profile (connection,
        NULL, NULL, &key, &salt);

  if (profile == G_TLS_SRTP_PROFILE_NONE) {
    gst_buffer_replace (&self->key_and_salt, NULL);
    self->srtp_profile = G_TLS_SRTP_PROFILE_NONE;

    block_queue (self);
  } else {
    GstBuffer *key_and_salt;

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

    GST_OBJECT_LOCK (self);
    gst_buffer_replace (&self->key_and_salt, key_and_salt);
    self->srtp_profile = profile;
    GST_OBJECT_UNLOCK (self);
    gst_buffer_unref (key_and_salt);

    g_signal_emit_by_name (self->srtp_dec, "clear-keys");

    clear_queue_block (self);
  }
}

static GstCaps *
srtpdec_request_key (GstElement * srtpdec, guint ssrc, GstDtlsSrtpDec * self)
{
  GstCaps *caps;

  GST_OBJECT_LOCK (self);

  if (self->key_and_salt == NULL) {
    GST_OBJECT_UNLOCK (self);
    return NULL;
  }

  caps = gst_caps_new_simple ("application/x-srtp",
      "ssrc", G_TYPE_UINT, ssrc,
      "srtp-key", GST_TYPE_BUFFER, self->key_and_salt, NULL);

  switch (self->srtp_profile) {
    case G_TLS_SRTP_PROFILE_AES128_CM_HMAC_SHA1_80:
      gst_caps_set_simple (caps, "srtp-cipher", G_TYPE_STRING, "aes-128-icm",
          "srtcp-cipher", G_TYPE_STRING, "aes-128-icm",
          "srtp-auth", G_TYPE_STRING, "hmac-sha1-80",
          "srtcp-auth", G_TYPE_STRING, "hmac-sha1-80", NULL);
      break;
    case G_TLS_SRTP_PROFILE_AES128_CM_HMAC_SHA1_32:
      gst_caps_set_simple (caps, "srtp-cipher", G_TYPE_STRING, "aes-128-icm",
          "srtcp-cipher", G_TYPE_STRING, "aes-128-icm",
          "srtp-auth", G_TYPE_STRING, "hmac-sha1-32",
          "srtcp-auth", G_TYPE_STRING, "hmac-sha1-32", NULL);
      break;
    case G_TLS_SRTP_PROFILE_NULL_HMAC_SHA1_80:
      gst_caps_set_simple (caps, "srtp-cipher", G_TYPE_STRING, "null",
          "srtcp-cipher", G_TYPE_STRING, "null",
          "srtp-auth", G_TYPE_STRING, "hmac-sha1-80",
          "srtcp-auth", G_TYPE_STRING, "hmac-sha1-80", NULL);
      break;
    case G_TLS_SRTP_PROFILE_NULL_HMAC_SHA1_32:
      gst_caps_set_simple (caps, "srtp-cipher", G_TYPE_STRING, "null",
          "srtcp-cipher", G_TYPE_STRING, "null",
          "srtp-auth", G_TYPE_STRING, "hmac-sha1-32",
          "srtcp-auth", G_TYPE_STRING, "hmac-sha1-32", NULL);
      break;
    default:
      g_assert_not_reached ();
  }

  GST_OBJECT_UNLOCK (self);

  return caps;
}

static GstStateChangeReturn
gst_dtls_srtp_dec_change_state (GstElement * element, GstStateChange transition)
{
  GstDtlsSrtpDec *self = GST_DTLS_SRTP_DEC (element);
  GstStateChangeReturn ret;

  if (self->srtp_dec == NULL || self->funnel == NULL || self->queue == NULL ||
      self->dtls_dec == NULL)
    return GST_STATE_CHANGE_FAILURE;

  switch (transition) {
    case GST_STATE_CHANGE_READY_TO_PAUSED:
      block_queue (self);
      break;
    case GST_STATE_CHANGE_READY_TO_NULL:
      if (self->status_changed_id)
        g_signal_handler_disconnect (self->conn, self->status_changed_id);
      self->status_changed_id = 0;
      g_clear_object (&self->conn);
      if (self->srtpdec_request_id)
        g_signal_handler_disconnect (self->srtp_dec, self->srtpdec_request_id);
      self->srtpdec_request_id = 0;
      break;
    default:
      break;
  }

  ret =
      GST_ELEMENT_CLASS (gst_dtls_srtp_dec_parent_class)->change_state (element,
      transition);
  if (ret == GST_STATE_CHANGE_FAILURE)
    return ret;

  switch (transition) {
    case GST_STATE_CHANGE_NULL_TO_READY:
      g_object_get (self->dtls_dec, "tls-connection", &self->conn, NULL);
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

      self->status_changed_id =
          g_signal_connect (self->conn, "notify::status",
          G_CALLBACK (tls_status_changed), self);
      self->srtpdec_request_id =
          g_signal_connect (self->srtp_dec, "request-key",
          G_CALLBACK (srtpdec_request_key), self);
      break;
    case GST_STATE_CHANGE_PAUSED_TO_READY:
      clear_queue_block (self);
      gst_buffer_replace (&self->key_and_salt, NULL);
      self->srtp_profile = G_TLS_SRTP_PROFILE_NONE;
      break;
    default:
      break;
  }

  return ret;
}
