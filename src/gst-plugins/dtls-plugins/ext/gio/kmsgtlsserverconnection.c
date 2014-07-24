
#include "kmsgtlsserverconnection.h"

#include "ext/gnutls/gtlsserverconnection-gnutls.h"

/**
 * g_tls_server_connection_new:
 * @base_io_stream: the #GIOStream to wrap
 * @certificate: (allow-none): the default server certificate, or %NULL
 * @error: #GError for error reporting, or %NULL to ignore.
 *
 * Creates a new #GTlsServerConnection wrapping @base_io_stream (which
 * must have pollable input and output streams).
 *
 * Return value: (transfer full) (type GTlsServerConnection): the new
 * #GTlsServerConnection, or %NULL on error
 *
 * Since: 2.28
 */
GIOStream *
kms_g_tls_server_connection_new (GIOStream        *base_io_stream,
                             GTlsCertificate  *certificate,
                             GError          **error)
{
  GObject *conn;

#if 0 /* glib */
  GTlsBackend *backend;

  backend = g_tls_backend_get_default ();
  conn = g_initable_new (g_tls_backend_get_server_connection_type (backend),
                         NULL, error,
                         "base-io-stream", base_io_stream,
                         "certificate", certificate,
                         NULL);
#else
  conn = g_initable_new (G_TYPE_KMS_TLS_SERVER_CONNECTION_GNUTLS,
                         NULL, error,
                         "base-io-stream", base_io_stream,
                         "certificate", certificate,
                         NULL);
#endif

  return G_IO_STREAM (conn);
}
