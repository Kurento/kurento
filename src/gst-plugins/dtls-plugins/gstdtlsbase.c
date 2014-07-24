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

#include "gstdtlsconnection.h"

#include "ext/gio/kmsgtlscertificate.h"

GST_DEBUG_CATEGORY_STATIC (dtls_base_debug);
#define GST_CAT_DEFAULT (dtls_base_debug)

static gpointer gst_dtls_base_parent_class = NULL;

static void gst_dtls_base_class_init (GstDtlsBaseClass * klass);
static void gst_dtls_base_init (GstDtlsBase * self,
    GstDtlsBaseClass * dtlsbase_class);

static void gst_dtls_base_finalize (GObject * object);
static void gst_dtls_base_set_property (GObject * object,
    guint prop_id, const GValue * value, GParamSpec * pspec);
static void gst_dtls_base_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec);

static GstStateChangeReturn gst_dtls_base_change_state (GstElement * element,
    GstStateChange transition);

static GstFlowReturn gst_dtls_base_chain (GstPad * pad, GstObject * parent,
    GstBuffer * buffer);

/* we can't use G_DEFINE_ABSTRACT_TYPE because we need the klass in the _init
 * method to get to the padtemplates */
GType
gst_dtls_base_get_type (void)
{
  static volatile gsize dtls_base_type = 0;

  if (g_once_init_enter (&dtls_base_type)) {
    GType _type = g_type_register_static_simple (GST_TYPE_ELEMENT,
        "GstDtlsBase", sizeof (GstDtlsBaseClass),
        (GClassInitFunc) gst_dtls_base_class_init, sizeof (GstDtlsBase),
        (GInstanceInitFunc) gst_dtls_base_init, G_TYPE_FLAG_ABSTRACT);

    g_once_init_leave (&dtls_base_type, _type);
  }
  return dtls_base_type;
}


enum
{
  PROP_CHANNEL_ID = 1,
  PROP_IS_CLIENT,
  PROP_TLS_CONNECTION,
  PROP_CERTIFICATE_PEM_FILE,
  PROP_CLIENT_VALIDATION_FLAGS
};


static void
gst_dtls_base_class_init (GstDtlsBaseClass * klass)
{
  GObjectClass *gobject_class = (GObjectClass *) klass;
  GstElementClass *gstelement_class = (GstElementClass *) klass;

  GST_DEBUG_CATEGORY_INIT (dtls_base_debug, "dtlsbase", 0, "DTLS Base Class");

  gst_dtls_base_parent_class = g_type_class_peek_parent (klass);

  gobject_class->finalize = gst_dtls_base_finalize;
  gobject_class->set_property = gst_dtls_base_set_property;
  gobject_class->get_property = gst_dtls_base_get_property;

  gstelement_class->change_state = gst_dtls_base_change_state;

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
}

static void
gst_dtls_base_init (GstDtlsBase * self, GstDtlsBaseClass * dtlsbase_class)
{
  GstElementClass *element_class = GST_ELEMENT_CLASS (dtlsbase_class);

  self->srcpad =
      gst_pad_new_from_template (gst_element_class_get_pad_template
      (element_class, "src"), "src");
  gst_element_add_pad (GST_ELEMENT (self), self->srcpad);

  self->sinkpad =
      gst_pad_new_from_template (gst_element_class_get_pad_template
      (element_class, "sink"), "sink");
  gst_pad_set_chain_function (self->sinkpad, gst_dtls_base_chain);
  gst_element_add_pad (GST_ELEMENT (self), self->sinkpad);
}


static void
gst_dtls_base_finalize (GObject * object)
{
  GstDtlsBase *self = GST_DTLS_BASE (object);

  g_free (self->channel_id);
  g_free (self->certificate_pem_file);

  G_OBJECT_CLASS (gst_dtls_base_parent_class)->finalize (object);
}


static void
gst_dtls_base_set_property (GObject * object,
    guint prop_id, const GValue * value, GParamSpec * pspec)
{
  GstDtlsBase *self = GST_DTLS_BASE (object);

  switch (prop_id) {
    case PROP_CHANNEL_ID:
      g_free (self->channel_id);
      self->channel_id = g_value_dup_string (value);
      break;
    case PROP_IS_CLIENT:
      self->is_client = g_value_get_boolean (value);
      break;
    case PROP_CERTIFICATE_PEM_FILE:
      g_free (self->certificate_pem_file);
      self->certificate_pem_file = g_value_dup_string (value);
      break;
    case PROP_CLIENT_VALIDATION_FLAGS:
      self->client_validation_flags = g_value_get_flags (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}


static void
gst_dtls_base_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec)
{
  GstDtlsBase *self = GST_DTLS_BASE (object);

  switch (prop_id) {
    case PROP_CHANNEL_ID:
      g_value_set_string (value, self->channel_id);
      break;
    case PROP_IS_CLIENT:
      g_value_set_boolean (value, self->is_client);
      break;
    case PROP_CERTIFICATE_PEM_FILE:
      g_value_set_string (value, self->certificate_pem_file);
      break;
    case PROP_TLS_CONNECTION:
      if (self->conn)
        g_value_set_object (value, self->conn->conn);
      break;
    case PROP_CLIENT_VALIDATION_FLAGS:
      g_value_set_flags (value, self->client_validation_flags);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_dtls_base_update_certificate (GstDtlsBase * self)
{
  GError *error = NULL;

  if (G_IS_TLS_CLIENT_CONNECTION (self->conn->conn))
    g_tls_client_connection_set_validation_flags (G_TLS_CLIENT_CONNECTION
        (self->conn->conn), self->client_validation_flags);

  if (self->certificate_pem_file && self->certificate_pem_file[0]) {
    GTlsCertificate *cert;

#if 0 /* glib */
    cert = g_tls_certificate_new_from_file (self->certificate_pem_file, &error);
#else
    cert = kms_g_tls_certificate_new_from_file (self->certificate_pem_file, &error);
#endif

    if (cert) {
      g_tls_connection_set_certificate (self->conn->conn, cert);
      g_object_unref (cert);
    } else {
      GST_WARNING_OBJECT (self,
          "Could not read valid certificate PEM file at %s: %s",
          self->certificate_pem_file, error->message);
      g_clear_error (&error);
    }
  }
}

static GstStateChangeReturn
gst_dtls_base_change_state (GstElement * element, GstStateChange transition)
{
  GstDtlsBase *self = GST_DTLS_BASE (element);
  GstStateChangeReturn ret;

  switch (transition) {
    case GST_STATE_CHANGE_NULL_TO_READY:
      if (self->channel_id == NULL) {
        GST_ERROR_OBJECT (element, "Need to specify a channel ID to match"
            " a decoder");
        return GST_STATE_CHANGE_FAILURE;
      }
      self->conn = gst_dtls_connection_get_by_id (self->channel_id,
          self->is_client, self);
      if (self->conn == NULL)
        return GST_STATE_CHANGE_FAILURE;
      break;
    case GST_STATE_CHANGE_READY_TO_PAUSED:
      gst_dtls_base_update_certificate (self);
      break;
    default:
      break;
  }

  ret = GST_ELEMENT_CLASS (gst_dtls_base_parent_class)->change_state (element,
      transition);
  if (ret == GST_STATE_CHANGE_FAILURE)
    return ret;

  switch (transition) {
    case GST_STATE_CHANGE_READY_TO_NULL:
      g_io_stream_close_async (G_IO_STREAM (self->conn->conn), 0, NULL, NULL,
          NULL);
       // g_clear_object (&self->conn);
      /* FIXME: this causes:
            "g_output_stream_close: assertion 'G_IS_OUTPUT_STREAM (stream)' failed"*/
      break;
    default:
      break;
  }
  return ret;
}



static GstFlowReturn
gst_dtls_base_chain (GstPad * pad, GstObject * parent, GstBuffer * buffer)
{
  GstDtlsBase *self = GST_DTLS_BASE (parent);

  return GST_DTLS_BASE_GET_CLASS (self)->chain (self, buffer);
}
