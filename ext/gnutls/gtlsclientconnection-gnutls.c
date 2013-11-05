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
#include <string.h>

#include "gtlsclientconnection-gnutls.h"
#include "gtlsbackend-gnutls.h"
#include "gtlscertificate-gnutls.h"
#include <glib/gi18n-lib.h>

enum
{
  PROP_0,
  PROP_VALIDATION_FLAGS,
  PROP_SERVER_IDENTITY,
  PROP_USE_SSL3,
  PROP_ACCEPTED_CAS
};

static void     g_tls_client_connection_gnutls_initable_interface_init (GInitableIface  *iface);

static void g_tls_client_connection_gnutls_client_connection_interface_init (GTlsClientConnectionInterface *iface);

static int g_tls_client_connection_gnutls_retrieve_function (gnutls_session_t             session,
							     const gnutls_datum_t        *req_ca_rdn,
							     int                          nreqs,
							     const gnutls_pk_algorithm_t *pk_algos,
							     int                          pk_algos_length,
							     gnutls_retr2_st             *st);

static GInitableIface *g_tls_client_connection_gnutls_parent_initable_iface;

G_DEFINE_TYPE_WITH_CODE (KmsGTlsClientConnectionGnutls, g_tls_client_connection_gnutls, G_TYPE_KMS_TLS_CONNECTION_GNUTLS,
			 G_IMPLEMENT_INTERFACE (G_TYPE_INITABLE,
						g_tls_client_connection_gnutls_initable_interface_init)
			 G_IMPLEMENT_INTERFACE (G_TYPE_TLS_CLIENT_CONNECTION,
						g_tls_client_connection_gnutls_client_connection_interface_init));

struct _KmsGTlsClientConnectionGnutlsPrivate
{
  GTlsCertificateFlags validation_flags;
  GSocketConnectable *server_identity;
  gboolean use_ssl3;

  GBytes *session_id;

  gboolean cert_requested;
  GPtrArray *accepted_cas;
};


static void
g_tls_client_connection_gnutls_init (KmsGTlsClientConnectionGnutls *gnutls)
{
  gnutls_certificate_credentials_t creds;

  gnutls->priv = G_TYPE_INSTANCE_GET_PRIVATE (gnutls, G_TYPE_KMS_TLS_CLIENT_CONNECTION_GNUTLS, KmsGTlsClientConnectionGnutlsPrivate);

  creds = g_tls_connection_gnutls_get_credentials (G_KMS_TLS_CONNECTION_GNUTLS (gnutls));
  gnutls_certificate_set_retrieve_function (creds, g_tls_client_connection_gnutls_retrieve_function);
}

static const gchar *
get_server_identity (KmsGTlsClientConnectionGnutls *gnutls)
{
  if (G_IS_NETWORK_ADDRESS (gnutls->priv->server_identity))
    return g_network_address_get_hostname (G_NETWORK_ADDRESS (gnutls->priv->server_identity));
  else if (G_IS_NETWORK_SERVICE (gnutls->priv->server_identity))
    return g_network_service_get_domain (G_NETWORK_SERVICE (gnutls->priv->server_identity));
  else
    return NULL;
}

static void
g_tls_client_connection_gnutls_constructed (GObject *object)
{
  KmsGTlsClientConnectionGnutls *gnutls = G_KMS_TLS_CLIENT_CONNECTION_GNUTLS (object);
  GSocketConnection *base_conn;
  GSocketAddress *remote_addr;
  GInetAddress *iaddr;
  guint port;

  /* Create a TLS session ID. We base it on the IP address since
   * different hosts serving the same hostname/service will probably
   * not share the same session cache. We base it on the
   * server-identity because at least some servers will fail (rather
   * than just failing to resume the session) if we don't.
   * (https://bugs.launchpad.net/bugs/823325)
   */
  g_object_get (G_OBJECT (gnutls), "base-io-stream", &base_conn, NULL);
  if (G_IS_SOCKET_CONNECTION (base_conn))
    {
      remote_addr = g_socket_connection_get_remote_address (base_conn, NULL);
      if (G_IS_INET_SOCKET_ADDRESS (remote_addr))
	{
	  GInetSocketAddress *isaddr = G_INET_SOCKET_ADDRESS (remote_addr);
	  const gchar *server_hostname;
	  gchar *addrstr, *session_id;

	  iaddr = g_inet_socket_address_get_address (isaddr);
	  port = g_inet_socket_address_get_port (isaddr);

	  addrstr = g_inet_address_to_string (iaddr);
	  server_hostname = get_server_identity (gnutls);
	  session_id = g_strdup_printf ("%s/%s/%d", addrstr,
					server_hostname ? server_hostname : "",
					port);
	  gnutls->priv->session_id = g_bytes_new_take (session_id, strlen (session_id));
	  g_free (addrstr);
	}
      g_object_unref (remote_addr);
    }
  g_object_unref (base_conn);

  if (G_OBJECT_CLASS (g_tls_client_connection_gnutls_parent_class)->constructed)
    G_OBJECT_CLASS (g_tls_client_connection_gnutls_parent_class)->constructed (object);
}

static void
g_tls_client_connection_gnutls_finalize (GObject *object)
{
  KmsGTlsClientConnectionGnutls *gnutls = G_KMS_TLS_CLIENT_CONNECTION_GNUTLS (object);

  if (gnutls->priv->server_identity)
    g_object_unref (gnutls->priv->server_identity);
  if (gnutls->priv->accepted_cas)
    g_ptr_array_unref (gnutls->priv->accepted_cas);
  if (gnutls->priv->session_id)
    g_bytes_unref (gnutls->priv->session_id);

  G_OBJECT_CLASS (g_tls_client_connection_gnutls_parent_class)->finalize (object);
}

static gboolean
g_tls_client_connection_gnutls_initable_init (GInitable       *initable,
					      GCancellable    *cancellable,
					      GError         **error)
{
  KmsGTlsConnectionGnutls *gnutls = G_KMS_TLS_CONNECTION_GNUTLS (initable);
  gnutls_session_t session;
  const gchar *hostname;

  if (!g_tls_client_connection_gnutls_parent_initable_iface->
      init (initable, cancellable, error))
    return FALSE;

  session = g_tls_connection_gnutls_get_session (gnutls);
  hostname = get_server_identity (G_KMS_TLS_CLIENT_CONNECTION_GNUTLS (gnutls));
  if (hostname)
    gnutls_server_name_set (session, GNUTLS_NAME_DNS,
			    hostname, strlen (hostname));

  return TRUE;
}

static void
g_tls_client_connection_gnutls_get_property (GObject    *object,
					     guint       prop_id,
					     GValue     *value,
					     GParamSpec *pspec)
{
  KmsGTlsClientConnectionGnutls *gnutls = G_KMS_TLS_CLIENT_CONNECTION_GNUTLS (object);
  GList *accepted_cas;
  gint i;

  switch (prop_id)
    {
    case PROP_VALIDATION_FLAGS:
      g_value_set_flags (value, gnutls->priv->validation_flags);
      break;

    case PROP_SERVER_IDENTITY:
      g_value_set_object (value, gnutls->priv->server_identity);
      break;

    case PROP_USE_SSL3:
      g_value_set_boolean (value, gnutls->priv->use_ssl3);
      break;

    case PROP_ACCEPTED_CAS:
      accepted_cas = NULL;
      if (gnutls->priv->accepted_cas)
        {
          for (i = 0; i < gnutls->priv->accepted_cas->len; ++i)
            {
              accepted_cas = g_list_prepend (accepted_cas, g_byte_array_ref (
                                             gnutls->priv->accepted_cas->pdata[i]));
            }
          accepted_cas = g_list_reverse (accepted_cas);
        }
      g_value_set_pointer (value, accepted_cas);
      break;

    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
    }
}

static void
g_tls_client_connection_gnutls_set_property (GObject      *object,
					     guint         prop_id,
					     const GValue *value,
					     GParamSpec   *pspec)
{
  KmsGTlsClientConnectionGnutls *gnutls = G_KMS_TLS_CLIENT_CONNECTION_GNUTLS (object);
  const char *hostname;

  switch (prop_id)
    {
    case PROP_VALIDATION_FLAGS:
      gnutls->priv->validation_flags = g_value_get_flags (value);
      break;

    case PROP_SERVER_IDENTITY:
      if (gnutls->priv->server_identity)
	g_object_unref (gnutls->priv->server_identity);
      gnutls->priv->server_identity = g_value_dup_object (value);

      hostname = get_server_identity (gnutls);
      if (hostname)
	{
	  gnutls_session_t session = g_tls_connection_gnutls_get_session (G_KMS_TLS_CONNECTION_GNUTLS (gnutls));

	  /* This will only be triggered if the identity is set at the start */
	  if (session)
	    gnutls_server_name_set (session, GNUTLS_NAME_DNS,
				    hostname, strlen (hostname));
	}
      break;

    case PROP_USE_SSL3:
      gnutls->priv->use_ssl3 = g_value_get_boolean (value);
      break;

    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
    }
}

static int
g_tls_client_connection_gnutls_retrieve_function (gnutls_session_t             session,
						  const gnutls_datum_t        *req_ca_rdn,
						  int                          nreqs,
						  const gnutls_pk_algorithm_t *pk_algos,
						  int                          pk_algos_length,
						  gnutls_retr2_st             *st)
{
  KmsGTlsClientConnectionGnutls *gnutls = gnutls_transport_get_ptr (session);
  GPtrArray *accepted_cas;
  GByteArray *dn;
  int i;

  gnutls->priv->cert_requested = TRUE;

  accepted_cas = g_ptr_array_new_with_free_func ((GDestroyNotify)g_byte_array_unref);
  for (i = 0; i < nreqs; i++)
    {
      dn = g_byte_array_new ();
      g_byte_array_append (dn, req_ca_rdn[i].data, req_ca_rdn[i].size);
      g_ptr_array_add (accepted_cas, dn);
    }

  if (gnutls->priv->accepted_cas)
    g_ptr_array_unref (gnutls->priv->accepted_cas);
  gnutls->priv->accepted_cas = accepted_cas;
  g_object_notify (G_OBJECT (gnutls), "accepted-cas");

  g_tls_connection_gnutls_get_certificate (G_KMS_TLS_CONNECTION_GNUTLS (gnutls), st);
  return 0;
}

static void
g_tls_client_connection_gnutls_failed (KmsGTlsConnectionGnutls *conn)
{
  KmsGTlsClientConnectionGnutls *gnutls = G_KMS_TLS_CLIENT_CONNECTION_GNUTLS (conn);

  if (gnutls->priv->session_id)
    g_tls_backend_gnutls_remove_session (GNUTLS_CLIENT, gnutls->priv->session_id);
}

static void
g_tls_client_connection_gnutls_begin_handshake (KmsGTlsConnectionGnutls *conn)
{
  KmsGTlsClientConnectionGnutls *gnutls = G_KMS_TLS_CLIENT_CONNECTION_GNUTLS (conn);

  /* Try to get a cached session */
  if (gnutls->priv->session_id)
    {
      GBytes *session_data;

      session_data = g_tls_backend_gnutls_lookup_session (GNUTLS_CLIENT, gnutls->priv->session_id);
      if (session_data)
	{
	  gnutls_session_set_data (g_tls_connection_gnutls_get_session (conn),
				   g_bytes_get_data (session_data, NULL),
				   g_bytes_get_size (session_data));
	  g_bytes_unref (session_data);
	}
    }

  gnutls->priv->cert_requested = FALSE;
}

static void
g_tls_client_connection_gnutls_finish_handshake (KmsGTlsConnectionGnutls  *conn,
						 GError               **inout_error)
{
  KmsGTlsClientConnectionGnutls *gnutls = G_KMS_TLS_CLIENT_CONNECTION_GNUTLS (conn);

  g_assert (inout_error != NULL);

  if (g_error_matches (*inout_error, G_TLS_ERROR, G_TLS_ERROR_NOT_TLS) &&
      gnutls->priv->cert_requested)
    {
      g_clear_error (inout_error);
      g_set_error_literal (inout_error, G_TLS_ERROR, G_TLS_ERROR_CERTIFICATE_REQUIRED,
			   _("Server required TLS certificate"));
    }

  if (gnutls->priv->session_id)
    {
      gnutls_datum_t session_datum;

      if (!*inout_error &&
	  gnutls_session_get_data2 (g_tls_connection_gnutls_get_session (conn),
				    &session_datum) == 0)
	{
	  GBytes *session_data = g_bytes_new_with_free_func (session_datum.data, session_datum.size,
							     (GDestroyNotify)gnutls_free, session_datum.data);

	  g_tls_backend_gnutls_store_session (GNUTLS_CLIENT, gnutls->priv->session_id,
					      session_data);
	  g_bytes_unref (session_data);
	}
      else
	g_tls_backend_gnutls_remove_session (GNUTLS_CLIENT, gnutls->priv->session_id);
    }
}

static void
g_tls_client_connection_gnutls_class_init (KmsGTlsClientConnectionGnutlsClass *klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  KmsGTlsConnectionGnutlsClass *connection_gnutls_class = G_KMS_TLS_CONNECTION_GNUTLS_CLASS (klass);

  g_type_class_add_private (klass, sizeof (KmsGTlsClientConnectionGnutlsPrivate));

  gobject_class->get_property = g_tls_client_connection_gnutls_get_property;
  gobject_class->set_property = g_tls_client_connection_gnutls_set_property;
  gobject_class->constructed  = g_tls_client_connection_gnutls_constructed;
  gobject_class->finalize     = g_tls_client_connection_gnutls_finalize;

  connection_gnutls_class->failed           = g_tls_client_connection_gnutls_failed;
  connection_gnutls_class->begin_handshake  = g_tls_client_connection_gnutls_begin_handshake;
  connection_gnutls_class->finish_handshake = g_tls_client_connection_gnutls_finish_handshake;

  g_object_class_override_property (gobject_class, PROP_VALIDATION_FLAGS, "validation-flags");
  g_object_class_override_property (gobject_class, PROP_SERVER_IDENTITY, "server-identity");
  g_object_class_override_property (gobject_class, PROP_USE_SSL3, "use-ssl3");
  g_object_class_override_property (gobject_class, PROP_ACCEPTED_CAS, "accepted-cas");
}

static void
g_tls_client_connection_gnutls_client_connection_interface_init (GTlsClientConnectionInterface *iface)
{
}

static void
g_tls_client_connection_gnutls_initable_interface_init (GInitableIface  *iface)
{
  g_tls_client_connection_gnutls_parent_initable_iface = g_type_interface_peek_parent (iface);

  iface->init = g_tls_client_connection_gnutls_initable_init;
}
