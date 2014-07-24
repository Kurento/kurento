
#include "kmsgtlscertificate.h"

#include "ext/gnutls/gtlscertificate-gnutls.h"
#include <string.h>

#define PEM_CERTIFICATE_HEADER     "-----BEGIN CERTIFICATE-----"
#define PEM_CERTIFICATE_FOOTER     "-----END CERTIFICATE-----"
#define PEM_PKCS1_PRIVKEY_HEADER   "-----BEGIN RSA PRIVATE KEY-----"
#define PEM_PKCS1_PRIVKEY_FOOTER   "-----END RSA PRIVATE KEY-----"
#define PEM_PKCS8_PRIVKEY_HEADER   "-----BEGIN PRIVATE KEY-----"
#define PEM_PKCS8_PRIVKEY_FOOTER   "-----END PRIVATE KEY-----"
#define PEM_PKCS8_ENCRYPTED_HEADER "-----BEGIN ENCRYPTED PRIVATE KEY-----"
#define PEM_PKCS8_ENCRYPTED_FOOTER "-----END ENCRYPTED PRIVATE KEY-----"

static gchar *
parse_next_pem_certificate (const gchar **data,
                            const gchar  *data_end,
                            gboolean      required,
                            GError      **error)
{
  const gchar *start, *end;

  start = g_strstr_len (*data, data_end - *data, PEM_CERTIFICATE_HEADER);
  if (!start)
    {
      if (required)
        {
          g_set_error_literal (error, G_TLS_ERROR, G_TLS_ERROR_BAD_CERTIFICATE,
                               "No PEM-encoded certificate found");
        }
      return NULL;
    }

  end = g_strstr_len (start, data_end - start, PEM_CERTIFICATE_FOOTER);
  if (!end)
    {
      g_set_error_literal (error, G_TLS_ERROR, G_TLS_ERROR_BAD_CERTIFICATE,
                           "Could not parse PEM-encoded certificate");
      return NULL;
    }
  end += strlen (PEM_CERTIFICATE_FOOTER);
  while (*end == '\r' || *end == '\n')
    end++;

  *data = end;

  return g_strndup (start, end - start);
}

static gchar *
parse_private_key (const gchar *data,
                   gsize data_len,
                   gboolean required,
                   GError **error)
{
  const gchar *start, *end, *footer;

  start = g_strstr_len (data, data_len, PEM_PKCS1_PRIVKEY_HEADER);
  if (start)
    footer = PEM_PKCS1_PRIVKEY_FOOTER;
  else
    {
      start = g_strstr_len (data, data_len, PEM_PKCS8_PRIVKEY_HEADER);
      if (start)
        footer = PEM_PKCS8_PRIVKEY_FOOTER;
      else
        {
          start = g_strstr_len (data, data_len, PEM_PKCS8_ENCRYPTED_HEADER);
          if (start)
            {
              g_set_error_literal (error, G_TLS_ERROR, G_TLS_ERROR_BAD_CERTIFICATE,
                                   "Cannot decrypt PEM-encoded private key");
            }
          else if (required)
            {
              g_set_error_literal (error, G_TLS_ERROR, G_TLS_ERROR_BAD_CERTIFICATE,
                                   "No PEM-encoded private key found");
            }
          return NULL;
        }
    }

  end = g_strstr_len (start, data_len - (data - start), footer);
  if (!end)
    {
      g_set_error_literal (error, G_TLS_ERROR, G_TLS_ERROR_BAD_CERTIFICATE,
                           "Could not parse PEM-encoded private key");
      return NULL;
    }
  end += strlen (footer);
  while (*end == '\r' || *end == '\n')
    end++;

  return g_strndup (start, end - start);
}

static GTlsCertificate *
kms_g_tls_certificate_new_internal (const gchar  *certificate_pem,
                                const gchar  *private_key_pem,
                                GError      **error)
{
  GObject *cert;
#if 0 /* glib */
  GTlsBackend *backend;

  backend = g_tls_backend_get_default ();

  cert = g_initable_new (g_tls_backend_get_certificate_type (backend),
                         NULL, error,
                         "certificate-pem", certificate_pem,
                         "private-key-pem", private_key_pem,
                         NULL);
#else
  cert = g_initable_new (G_TYPE_KMS_TLS_CERTIFICATE_GNUTLS,
                         NULL, error,
                         "certificate-pem", certificate_pem,
                         "private-key-pem", private_key_pem,
                         NULL);
#endif
  return G_TLS_CERTIFICATE (cert);
}

/**
 * g_tls_certificate_new_from_pem:
 * @data: PEM-encoded certificate data
 * @length: the length of @data, or -1 if it's 0-terminated.
 * @error: #GError for error reporting, or %NULL to ignore.
 *
 * Creates a new #GTlsCertificate from the PEM-encoded data in @data.
 * If @data includes both a certificate and a private key, then the
 * returned certificate will include the private key data as well. (See
 * the #GTlsCertificate:private-key-pem property for information about
 * supported formats.)
 *
 * If @data includes multiple certificates, only the first one will be
 * parsed.
 *
 * Return value: the new certificate, or %NULL if @data is invalid
 *
 * Since: 2.28
 */
static GTlsCertificate *
kms_g_tls_certificate_new_from_pem  (const gchar  *data,
                                 gssize        length,
                                 GError      **error)
{
  const gchar *data_end;
  gchar *key_pem, *cert_pem;
  GTlsCertificate *cert;

  g_return_val_if_fail (data != NULL, NULL);

  if (length == -1)
    length = strlen (data);

  data_end = data + length;

  key_pem = parse_private_key (data, length, FALSE, error);
  if (error && *error)
    return NULL;

  cert_pem = parse_next_pem_certificate (&data, data_end, TRUE, error);
  if (error && *error)
    {
      g_free (key_pem);
      return NULL;
    }

  cert = kms_g_tls_certificate_new_internal (cert_pem, key_pem, error);
  g_free (key_pem);
  g_free (cert_pem);

  return cert;
}

/**
 * g_tls_certificate_new_from_file:
 * @file: file containing a PEM-encoded certificate to import
 * @error: #GError for error reporting, or %NULL to ignore.
 *
 * Creates a #GTlsCertificate from the PEM-encoded data in @file. If
 * @file cannot be read or parsed, the function will return %NULL and
 * set @error. Otherwise, this behaves like
 * g_tls_certificate_new_from_pem().
 *
 * Return value: the new certificate, or %NULL on error
 *
 * Since: 2.28
 */
GTlsCertificate *
kms_g_tls_certificate_new_from_file (const gchar  *file,
                                 GError      **error)
{
  GTlsCertificate *cert;
  gchar *contents;
  gsize length;

  if (!g_file_get_contents (file, &contents, &length, error))
    return NULL;

  cert = kms_g_tls_certificate_new_from_pem (contents, length, error);
  g_free (contents);
  return cert;
}


/**
 * g_tls_certificate_list_new_from_file:
 * @file: file containing PEM-encoded certificates to import
 * @error: #GError for error reporting, or %NULL to ignore.
 *
 * Creates one or more #GTlsCertificate<!-- -->s from the PEM-encoded
 * data in @file. If @file cannot be read or parsed, the function will
 * return %NULL and set @error. If @file does not contain any
 * PEM-encoded certificates, this will return an empty list and not
 * set @error.
 *
 * Return value: (element-type Gio.TlsCertificate) (transfer full): a
 * #GList containing #GTlsCertificate objects. You must free the list
 * and its contents when you are done with it.
 *
 * Since: 2.28
 */
GList *
kms_g_tls_certificate_list_new_from_file (const gchar  *file,
                                      GError      **error)
{
  GQueue queue = G_QUEUE_INIT;
  gchar *contents, *end;
  const gchar *p;
  gsize length;

  if (!g_file_get_contents (file, &contents, &length, error))
    return NULL;

  end = contents + length;
  p = contents;
  while (p && *p)
    {
      gchar *cert_pem;
      GTlsCertificate *cert = NULL;

      cert_pem = parse_next_pem_certificate (&p, end, FALSE, error);
      if (cert_pem)
        {
          cert = kms_g_tls_certificate_new_internal (cert_pem, NULL, error);
          g_free (cert_pem);
        }
      if (!cert)
        {
          g_list_free_full (queue.head, g_object_unref);
          queue.head = NULL;
          break;
        }
      g_queue_push_tail (&queue, cert);
    }

  g_free (contents);
  return queue.head;
}
