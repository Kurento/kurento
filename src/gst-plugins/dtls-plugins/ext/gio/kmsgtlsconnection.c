
#include "kmsgtlsconnection.h"

#include "ext/gnutls/gtlsconnection-gnutls.h"

#include <glib.h>

/**
 * g_tls_connection_add_srtp_profile:
 * @conn: A #GTlsConnection with a underlying #GIOStream that is a datagram
 *  stream
 * @profile: A #GTlsSrtpProfile profile
 *
 * Informs the #GTlsConnection that the application wants to use
 * DTLS-SRTP and supports @profile, this can be called multiple times
 * before g_tls_connection_handshake() or
 * g_tls_connection_handshake_async() to add multiple profiles.
 *
 * Returns: %TRUE if the profile could be added to the connection.
 *
 * Since: 2.38
 */
gboolean
g_tls_connection_add_srtp_profile (GTlsConnection       *conn,
                                  GTlsSrtpProfile        profile)
{
#if 0 /* glib */
  GTlsConnectionClass *class;
#endif
  gboolean res;

  g_return_val_if_fail (G_IS_TLS_CONNECTION (conn), FALSE);

#if 0 /* glib */
  class = G_TLS_CONNECTION_GET_CLASS (conn);

  res = FALSE;
  if (class->add_srtp_profile)
    res = class->add_srtp_profile (conn, profile);
#else
  res = g_tls_connection_gnutls_add_srtp_profile (conn, profile);
#endif

  res = TRUE;
  return res;
}

/**
 * g_tls_connection_get_selected_srtp_profile:
 * @conn: a #GTlsConnection
 * @server_key: (allow-none): The location where to store the server encryption master SRTP key or %NULL
 * @server_salt: (allow-none): The location where to store the server encryption master SRTP salt or %NULL
 * @client_key: (allow-none): The location where to store the client encryption master SRTP key or %NULL
 * @client_salt: (allow-none): The location where to store the client encryption master SRTP salt or %NULL
 *
 * If g_tls_connection_add_srtp_profile() was called successfully and
 * the handshake succeeded, this will return the selected SRTP profile and key
 * material if SRTP negotiation succeeded. The success of the handshake can
 * be verified by looking at the #GTlsConnection::status property.
 *
 * Returns: The selected profile or #G_TLS_SRTP_PROFILE_NONE if no profile
 * was selected
 *
 * Since: 2.38
 */
GTlsSrtpProfile
g_tls_connection_get_selected_srtp_profile (GTlsConnection      *conn,
                                           GByteArray          **server_key,
                                           GByteArray          **server_salt,
                                           GByteArray          **client_key,
                                           GByteArray          **client_salt)
{
#if 0 /* glib */
  GTlsConnectionClass *class;
#endif
  GTlsSrtpProfile profile = G_TLS_SRTP_PROFILE_NONE;

  g_return_val_if_fail (G_IS_TLS_CONNECTION (conn), FALSE);

#if 0 /* glib */
  class = G_TLS_CONNECTION_GET_CLASS (conn);

  if (class->get_selected_srtp_profile)
    profile = class->get_selected_srtp_profile (conn, server_key, server_salt,
                                                client_key, client_salt);
#else
  profile = g_tls_connection_gnutls_get_selected_srtp_profile (conn, server_key, server_salt,
                                                client_key, client_salt);
#endif

  return profile;
}
