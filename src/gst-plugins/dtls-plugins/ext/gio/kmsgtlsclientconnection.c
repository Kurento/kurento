
#include "kmsgtlsclientconnection.h"

#include <src/ext/gnutls/gtlsclientconnection-gnutls.h>

/**
 * g_tls_client_connection_new:
 * @base_io_stream: the #GIOStream to wrap
 * @server_identity: (allow-none): the expected identity of the server
 * @error: #GError for error reporting, or %NULL to ignore.
 *
 * Creates a new #GTlsClientConnection wrapping @base_io_stream (which
 * must have pollable input and output streams) which is assumed to
 * communicate with the server identified by @server_identity.
 *
 * Return value: (transfer full) (type GTlsClientConnection): the new
 * #GTlsClientConnection, or %NULL on error
 *
 * Since: 2.28
 */
GIOStream *
kms_g_tls_client_connection_new (GIOStream           *base_io_stream,
                             GSocketConnectable  *server_identity,
                             GError             **error)
{
  GObject *conn;

#if 0 /* glib */
  GTlsBackend *backend;

  backend = g_tls_backend_get_default ();
  conn = g_initable_new (g_tls_backend_get_client_connection_type (backend),
                         NULL, error,
                         "base-io-stream", base_io_stream,
                         "server-identity", server_identity,
                         NULL);
#else
  conn = g_initable_new (G_TYPE_KMS_TLS_CLIENT_CONNECTION_GNUTLS,
                         NULL, error,
                         "base-io-stream", base_io_stream,
                         "server-identity", server_identity,
                         NULL);
#endif

  return G_IO_STREAM (conn);
}
