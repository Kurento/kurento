/* GIO - GLib Input, Output and Streaming Library
 *
 * Copyright 2010 Collabora, Ltd
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
 *
 * Author: Stef Walter <stefw@collabora.co.uk>
 */

#include "config.h"

#include <gnutls/gnutls.h>
#include <gnutls/x509.h>

#include "gtlsdatabase-gnutls.h"

#include "gtlscertificate-gnutls.h"

#include <glib/gi18n-lib.h>

G_DEFINE_ABSTRACT_TYPE (KmsGTlsDatabaseGnutls, g_tls_database_gnutls, G_TYPE_TLS_DATABASE);

enum {
  STATUS_FAILURE,
  STATUS_INCOMPLETE,
  STATUS_SELFSIGNED,
  STATUS_PINNED,
  STATUS_ANCHORED,
};

static void
g_tls_database_gnutls_init (KmsGTlsDatabaseGnutls *self)
{

}

static gboolean
is_self_signed (KmsGTlsCertificateGnutls *certificate)
{
  const gnutls_x509_crt_t cert = g_tls_certificate_gnutls_get_cert (certificate);
  return (gnutls_x509_crt_check_issuer (cert, cert) > 0);
}

static gint
build_certificate_chain (KmsGTlsDatabaseGnutls      *self,
                         KmsGTlsCertificateGnutls   *chain,
                         const gchar             *purpose,
                         GSocketConnectable      *identity,
                         GTlsInteraction         *interaction,
                         GTlsDatabaseVerifyFlags  flags,
                         GCancellable            *cancellable,
                         KmsGTlsCertificateGnutls  **anchor,
                         GError                 **error)
{

  KmsGTlsCertificateGnutls *certificate;
  KmsGTlsCertificateGnutls *previous;
  GTlsCertificate *issuer;
  gboolean certificate_is_from_db;

  g_assert (anchor);
  g_assert (chain);
  g_assert (purpose);
  g_assert (error);
  g_assert (!*error);

  /*
   * Remember that the first certificate never changes in the chain.
   * When we find a self-signed, pinned or anchored certificate, all
   * issuers are truncated from the chain.
   */

  *anchor = NULL;
  previous = NULL;
  certificate = chain;
  certificate_is_from_db = FALSE;

  /* First check for pinned certificate */
  if (g_tls_database_gnutls_lookup_assertion (self, certificate,
                                              G_KMS_TLS_DATABASE_GNUTLS_PINNED_CERTIFICATE,
                                              purpose, identity, cancellable, error))
    {
      g_tls_certificate_gnutls_set_issuer (certificate, NULL);
      return STATUS_PINNED;
    }
  else if (*error)
    {
      return STATUS_FAILURE;
    }

  for (;;)
    {
      if (g_cancellable_set_error_if_cancelled (cancellable, error))
        return STATUS_FAILURE;

      /* Look up whether this certificate is an anchor */
      if (g_tls_database_gnutls_lookup_assertion (self, certificate,
                                                  G_KMS_TLS_DATABASE_GNUTLS_ANCHORED_CERTIFICATE,
                                                  purpose, identity, cancellable, error))
        {
          g_tls_certificate_gnutls_set_issuer (certificate, NULL);
          *anchor = certificate;
          return STATUS_ANCHORED;
        }
      else if (*error)
        {
          return STATUS_FAILURE;
        }

      /* Is it self-signed? */
      if (is_self_signed (certificate))
        {
          /*
           * Since at this point we would fail with 'self-signed', can we replace
           * this certificate with one from the database and do better?
           */
          if (previous && !certificate_is_from_db)
            {
              issuer = g_tls_database_lookup_certificate_issuer (G_TLS_DATABASE (self),
                                                                 G_TLS_CERTIFICATE (previous),
                                                                 interaction,
                                                                 G_TLS_DATABASE_LOOKUP_NONE,
                                                                 cancellable, error);
              if (*error)
                {
                  return STATUS_FAILURE;
                }
              else if (issuer)
                {
                  /* Replaced with certificate in the db, restart step again with this certificate */
                  g_return_val_if_fail (G_IS_KMS_TLS_CERTIFICATE_GNUTLS (issuer), STATUS_FAILURE);
                  g_tls_certificate_gnutls_set_issuer (previous, G_KMS_TLS_CERTIFICATE_GNUTLS (issuer));
                  certificate = G_KMS_TLS_CERTIFICATE_GNUTLS (issuer);
                  certificate_is_from_db = TRUE;
                  continue;
                }
            }

          g_tls_certificate_gnutls_set_issuer (certificate, NULL);
          return STATUS_SELFSIGNED;
        }

      previous = certificate;

      /* Bring over the next certificate in the chain */
      issuer = g_tls_certificate_get_issuer (G_TLS_CERTIFICATE (certificate));
      if (issuer)
        {
          g_return_val_if_fail (G_IS_KMS_TLS_CERTIFICATE_GNUTLS (issuer), STATUS_FAILURE);
          certificate = G_KMS_TLS_CERTIFICATE_GNUTLS (issuer);
          certificate_is_from_db = FALSE;
        }

      /* Search for the next certificate in chain */
      else
        {
          issuer = g_tls_database_lookup_certificate_issuer (G_TLS_DATABASE (self),
                                                             G_TLS_CERTIFICATE (certificate),
                                                             interaction,
                                                             G_TLS_DATABASE_LOOKUP_NONE,
                                                             cancellable, error);
          if (*error)
            return STATUS_FAILURE;
          else if (!issuer)
            return STATUS_INCOMPLETE;

          /* Found a certificate in chain, use for next step */
          g_return_val_if_fail (G_IS_KMS_TLS_CERTIFICATE_GNUTLS (issuer), STATUS_FAILURE);
          g_tls_certificate_gnutls_set_issuer (certificate, G_KMS_TLS_CERTIFICATE_GNUTLS (issuer));
          certificate = G_KMS_TLS_CERTIFICATE_GNUTLS (issuer);
          certificate_is_from_db = TRUE;
          g_object_unref (issuer);
        }
    }

  g_assert_not_reached ();
}

static GTlsCertificateFlags
double_check_before_after_dates (KmsGTlsCertificateGnutls *chain)
{
  GTlsCertificateFlags gtls_flags = 0;
  gnutls_x509_crt_t cert;
  time_t t, now;

  now = time (NULL);
  while (chain)
    {
      cert = g_tls_certificate_gnutls_get_cert (chain);
      t = gnutls_x509_crt_get_activation_time (cert);
      if (t == (time_t) -1 || t > now)
        gtls_flags |= G_TLS_CERTIFICATE_NOT_ACTIVATED;

      t = gnutls_x509_crt_get_expiration_time (cert);
      if (t == (time_t) -1 || t < now)
        gtls_flags |= G_TLS_CERTIFICATE_EXPIRED;

      chain = G_KMS_TLS_CERTIFICATE_GNUTLS (g_tls_certificate_get_issuer
                                        (G_TLS_CERTIFICATE (chain)));
    }

  return gtls_flags;
}

static void
convert_certificate_chain_to_gnutls (KmsGTlsCertificateGnutls    *chain,
                                     gnutls_x509_crt_t       **gnutls_chain,
                                     guint                    *gnutls_chain_length)
{
  GTlsCertificate *cert;
  guint i;

  g_assert (gnutls_chain);
  g_assert (gnutls_chain_length);

  for (*gnutls_chain_length = 0, cert = G_TLS_CERTIFICATE (chain);
      cert; cert = g_tls_certificate_get_issuer (cert))
    ++(*gnutls_chain_length);

  *gnutls_chain = g_new0 (gnutls_x509_crt_t, *gnutls_chain_length);

  for (i = 0, cert = G_TLS_CERTIFICATE (chain);
      cert; cert = g_tls_certificate_get_issuer (cert), ++i)
    (*gnutls_chain)[i] = g_tls_certificate_gnutls_get_cert (G_KMS_TLS_CERTIFICATE_GNUTLS (cert));

  g_assert (i == *gnutls_chain_length);
}

static GTlsCertificateFlags
g_tls_database_gnutls_verify_chain (GTlsDatabase           *database,
                                    GTlsCertificate        *chain,
                                    const gchar            *purpose,
                                    GSocketConnectable     *identity,
                                    GTlsInteraction        *interaction,
                                    GTlsDatabaseVerifyFlags flags,
                                    GCancellable           *cancellable,
                                    GError                **error)
{
  KmsGTlsDatabaseGnutls *self;
  GTlsCertificateFlags result;
  GError *err = NULL;
  KmsGTlsCertificateGnutls *anchor;
  guint gnutls_result;
  gnutls_x509_crt_t *certs, *anchors;
  guint certs_length, anchors_length;
  gint status, gerr;

  g_return_val_if_fail (G_IS_KMS_TLS_CERTIFICATE_GNUTLS (chain),
                        G_TLS_CERTIFICATE_GENERIC_ERROR);

  self = G_KMS_TLS_DATABASE_GNUTLS (database);
  anchor = NULL;

  status = build_certificate_chain (self, G_KMS_TLS_CERTIFICATE_GNUTLS (chain), purpose,
                                    identity, interaction, flags, cancellable, &anchor, &err);
  if (status == STATUS_FAILURE)
    {
      g_propagate_error (error, err);
      return G_TLS_CERTIFICATE_GENERIC_ERROR;
    }

  /*
   * A pinned certificate is verified on its own, without any further
   * verification.
   */
  if (status == STATUS_PINNED)
      return 0;

  if (g_cancellable_set_error_if_cancelled (cancellable, error))
    return G_TLS_CERTIFICATE_GENERIC_ERROR;

  convert_certificate_chain_to_gnutls (G_KMS_TLS_CERTIFICATE_GNUTLS (chain),
                                       &certs, &certs_length);

  if (anchor)
    {
      g_assert (g_tls_certificate_get_issuer (G_TLS_CERTIFICATE (anchor)) == NULL);
      convert_certificate_chain_to_gnutls (G_KMS_TLS_CERTIFICATE_GNUTLS (anchor),
                                           &anchors, &anchors_length);
    }
  else
    {
      anchors = NULL;
      anchors_length = 0;
    }

  gerr = gnutls_x509_crt_list_verify (certs, certs_length,
                                      anchors, anchors_length,
                                      NULL, 0, GNUTLS_VERIFY_ALLOW_X509_V1_CA_CRT,
                                      &gnutls_result);

  g_free (certs);
  g_free (anchors);

  if (gerr != 0)
      return G_TLS_CERTIFICATE_GENERIC_ERROR;
  else if (g_cancellable_set_error_if_cancelled (cancellable, error))
    return G_TLS_CERTIFICATE_GENERIC_ERROR;

  result = g_tls_certificate_gnutls_convert_flags (gnutls_result);

  /*
   * We have to check these ourselves since gnutls_x509_crt_list_verify
   * won't bother if it gets an UNKNOWN_CA.
   */
  result |= double_check_before_after_dates (G_KMS_TLS_CERTIFICATE_GNUTLS (chain));

  if (identity)
    result |= g_tls_certificate_gnutls_verify_identity (G_KMS_TLS_CERTIFICATE_GNUTLS (chain),
                                                        identity);

  return result;
}

static void
g_tls_database_gnutls_class_init (KmsGTlsDatabaseGnutlsClass *klass)
{
  GTlsDatabaseClass *database_class = G_TLS_DATABASE_CLASS (klass);
  database_class->verify_chain = g_tls_database_gnutls_verify_chain;
}

gboolean
g_tls_database_gnutls_lookup_assertion (KmsGTlsDatabaseGnutls          *self,
                                        KmsGTlsCertificateGnutls       *certificate,
                                        KmsGTlsDatabaseGnutlsAssertion  assertion,
                                        const gchar                 *purpose,
                                        GSocketConnectable          *identity,
                                        GCancellable                *cancellable,
                                        GError                     **error)
{
  g_return_val_if_fail (G_IS_KMS_TLS_DATABASE_GNUTLS (self), FALSE);
  g_return_val_if_fail (G_IS_KMS_TLS_CERTIFICATE_GNUTLS (certificate), FALSE);
  g_return_val_if_fail (purpose, FALSE);
  g_return_val_if_fail (!identity || G_IS_SOCKET_CONNECTABLE (identity), FALSE);
  g_return_val_if_fail (!cancellable || G_IS_CANCELLABLE (cancellable), FALSE);
  g_return_val_if_fail (!error || !*error, FALSE);
  g_return_val_if_fail (G_KMS_TLS_DATABASE_GNUTLS_GET_CLASS (self)->lookup_assertion, FALSE);
  return G_KMS_TLS_DATABASE_GNUTLS_GET_CLASS (self)->lookup_assertion (self,
                                                                   certificate,
                                                                   assertion,
                                                                   purpose,
                                                                   identity,
                                                                   cancellable,
                                                                   error);
}
