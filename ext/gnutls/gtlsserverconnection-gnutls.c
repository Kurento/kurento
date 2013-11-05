/* GIO - GLib Input, Output and Streaming Library
 *
 * Copyright 2010 Red Hat, Inc
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 */

#include "config.h"
#include "glib.h"

#include <errno.h>
#include <gnutls/gnutls.h>
#include <gnutls/x509.h>

#include "gtlsserverconnection-gnutls.h"
#include "gtlsbackend-gnutls.h"
#include "gtlscertificate-gnutls.h"
#include <glib/gi18n-lib.h>

enum
{
  PROP_0,
  PROP_AUTHENTICATION_MODE
};

static void     g_tls_server_connection_gnutls_initable_interface_init (GInitableIface  *iface);

static void g_tls_server_connection_gnutls_server_connection_interface_init (GTlsServerConnectionInterface *iface);

static int g_tls_server_connection_gnutls_retrieve_function (gnutls_session_t             session,
                                                             const gnutls_datum_t        *req_ca_rdn,
                                                             int                          nreqs,
                                                             const gnutls_pk_algorithm_t *pk_algos,
                                                             int                          pk_algos_length,
                                                             gnutls_retr2_st             *st);

static int            g_tls_server_connection_gnutls_db_store    (void            *user_data,
								  gnutls_datum_t   key,
								  gnutls_datum_t   data);
static int            g_tls_server_connection_gnutls_db_remove   (void            *user_data,
								  gnutls_datum_t   key);
static gnutls_datum_t g_tls_server_connection_gnutls_db_retrieve (void            *user_data,
								  gnutls_datum_t   key);

static GInitableIface *g_tls_server_connection_gnutls_parent_initable_iface;

G_DEFINE_TYPE_WITH_CODE (KmsGTlsServerConnectionGnutls, g_tls_server_connection_gnutls, G_TYPE_KMS_TLS_CONNECTION_GNUTLS,
			 G_IMPLEMENT_INTERFACE (G_TYPE_INITABLE,
						g_tls_server_connection_gnutls_initable_interface_init)
			 G_IMPLEMENT_INTERFACE (G_TYPE_TLS_SERVER_CONNECTION,
						g_tls_server_connection_gnutls_server_connection_interface_init)
)

struct _KmsGTlsServerConnectionGnutlsPrivate
{
  GTlsAuthenticationMode authentication_mode;
};

static void
g_tls_server_connection_gnutls_init (KmsGTlsServerConnectionGnutls *gnutls)
{
  gnutls_certificate_credentials_t creds;

  gnutls->priv = G_TYPE_INSTANCE_GET_PRIVATE (gnutls, G_TYPE_KMS_TLS_SERVER_CONNECTION_GNUTLS, KmsGTlsServerConnectionGnutlsPrivate);

  creds = g_tls_connection_gnutls_get_credentials (G_KMS_TLS_CONNECTION_GNUTLS (gnutls));
  gnutls_certificate_set_retrieve_function (creds, g_tls_server_connection_gnutls_retrieve_function);
}

static gboolean
g_tls_server_connection_gnutls_initable_init (GInitable       *initable,
					      GCancellable    *cancellable,
					      GError         **error)
{
  KmsGTlsConnectionGnutls *gnutls = G_KMS_TLS_CONNECTION_GNUTLS (initable);
  GTlsCertificate *cert;
  gnutls_session_t session;

  if (!g_tls_server_connection_gnutls_parent_initable_iface->
      init (initable, cancellable, error))
    return FALSE;

  session = g_tls_connection_gnutls_get_session (G_KMS_TLS_CONNECTION_GNUTLS (gnutls));
  gnutls_db_set_retrieve_function (session, g_tls_server_connection_gnutls_db_retrieve);
  gnutls_db_set_store_function (session, g_tls_server_connection_gnutls_db_store);
  gnutls_db_set_remove_function (session, g_tls_server_connection_gnutls_db_remove);

  cert = g_tls_connection_get_certificate (G_TLS_CONNECTION (initable));
  if (cert && !g_tls_certificate_gnutls_has_key (G_KMS_TLS_CERTIFICATE_GNUTLS (cert)))
    {
      g_set_error_literal (error, G_TLS_ERROR, G_TLS_ERROR_BAD_CERTIFICATE,
			   _("Certificate has no private key"));
      return FALSE;
    }

  return TRUE;
}

static void
g_tls_server_connection_gnutls_get_property (GObject    *object,
					     guint       prop_id,
					     GValue     *value,
					     GParamSpec *pspec)
{
  KmsGTlsServerConnectionGnutls *gnutls = G_KMS_TLS_SERVER_CONNECTION_GNUTLS (object);

  switch (prop_id)
    {
    case PROP_AUTHENTICATION_MODE:
      g_value_set_enum (value, gnutls->priv->authentication_mode);
      break;
      
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
    }
}

static void
g_tls_server_connection_gnutls_set_property (GObject      *object,
					     guint         prop_id,
					     const GValue *value,
					     GParamSpec   *pspec)
{
  KmsGTlsServerConnectionGnutls *gnutls = G_KMS_TLS_SERVER_CONNECTION_GNUTLS (object);

  switch (prop_id)
    {
    case PROP_AUTHENTICATION_MODE:
      gnutls->priv->authentication_mode = g_value_get_enum (value);
      break;

    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
    }
}

static int
g_tls_server_connection_gnutls_retrieve_function (gnutls_session_t             session,
                                                  const gnutls_datum_t        *req_ca_rdn,
                                                  int                          nreqs,
                                                  const gnutls_pk_algorithm_t *pk_algos,
                                                  int                          pk_algos_length,
                                                  gnutls_retr2_st             *st)
{
  g_tls_connection_gnutls_get_certificate (gnutls_transport_get_ptr (session), st);
  return 0;
}

static void
g_tls_server_connection_gnutls_failed (KmsGTlsConnectionGnutls *conn)
{
  gnutls_db_remove_session (g_tls_connection_gnutls_get_session (conn));
}

static void
g_tls_server_connection_gnutls_begin_handshake (KmsGTlsConnectionGnutls *conn)
{
  KmsGTlsServerConnectionGnutls *gnutls = G_KMS_TLS_SERVER_CONNECTION_GNUTLS (conn);
  gnutls_session_t session;
  gnutls_certificate_request_t req_mode;

  switch (gnutls->priv->authentication_mode)
    {
    case G_TLS_AUTHENTICATION_REQUESTED:
      req_mode = GNUTLS_CERT_REQUEST;
      break;
    case G_TLS_AUTHENTICATION_REQUIRED:
      req_mode = GNUTLS_CERT_REQUIRE;
      break;
    default:
      req_mode = GNUTLS_CERT_IGNORE;
      break;
    }

  session = g_tls_connection_gnutls_get_session (conn);
  gnutls_certificate_server_set_request (session, req_mode);
}

static void
g_tls_server_connection_gnutls_finish_handshake (KmsGTlsConnectionGnutls  *gnutls,
						 GError               **inout_error)
{
}

/* Session cache management */

static int
g_tls_server_connection_gnutls_db_store (void            *user_data,
					 gnutls_datum_t   key,
					 gnutls_datum_t   data)
{
  GBytes *session_id, *session_data;

  session_id = g_bytes_new (key.data, key.size);
  session_data = g_bytes_new (data.data, data.size);
  g_tls_backend_gnutls_store_session (GNUTLS_SERVER, session_id, session_data);
  g_bytes_unref (session_id);
  g_bytes_unref (session_data);

  return 0;
}

static int
g_tls_server_connection_gnutls_db_remove (void            *user_data,
					  gnutls_datum_t   key)
{
  GBytes *session_id;

  session_id = g_bytes_new (key.data, key.size);
  g_tls_backend_gnutls_remove_session (GNUTLS_SERVER, session_id);
  g_bytes_unref (session_id);

  return 0;
}

static gnutls_datum_t
g_tls_server_connection_gnutls_db_retrieve (void            *user_data,
					    gnutls_datum_t   key)
{
  GBytes *session_id, *session_data;
  gnutls_datum_t data;

  session_id = g_bytes_new (key.data, key.size);
  session_data = g_tls_backend_gnutls_lookup_session (GNUTLS_SERVER, session_id);
  g_bytes_unref (session_id);

  if (session_data)
    {
      data.size = g_bytes_get_size (session_data);
      data.data = gnutls_malloc (data.size);
      memcpy (data.data, g_bytes_get_data (session_data, NULL), data.size);
      g_bytes_unref (session_data);
    }
  else
    {
      data.size = 0;
      data.data = NULL;
    }

  return data;
}

static void
g_tls_server_connection_gnutls_class_init (KmsGTlsServerConnectionGnutlsClass *klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  KmsGTlsConnectionGnutlsClass *connection_gnutls_class = G_KMS_TLS_CONNECTION_GNUTLS_CLASS (klass);

  g_type_class_add_private (klass, sizeof (KmsGTlsServerConnectionGnutlsPrivate));

  gobject_class->get_property = g_tls_server_connection_gnutls_get_property;
  gobject_class->set_property = g_tls_server_connection_gnutls_set_property;

  connection_gnutls_class->failed           = g_tls_server_connection_gnutls_failed;
  connection_gnutls_class->begin_handshake  = g_tls_server_connection_gnutls_begin_handshake;
  connection_gnutls_class->finish_handshake = g_tls_server_connection_gnutls_finish_handshake;

  g_object_class_override_property (gobject_class, PROP_AUTHENTICATION_MODE, "authentication-mode");
}

static void
g_tls_server_connection_gnutls_server_connection_interface_init (GTlsServerConnectionInterface *iface)
{
}

static void
g_tls_server_connection_gnutls_initable_interface_init (GInitableIface  *iface)
{
  g_tls_server_connection_gnutls_parent_initable_iface = g_type_interface_peek_parent (iface);

  iface->init = g_tls_server_connection_gnutls_initable_init;
}
