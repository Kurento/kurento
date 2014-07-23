/* GIO - GLib Input, Output and Streaming Library
 *
 * Copyright 2009 Red Hat, Inc
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

#include <gnutls/gnutls.h>
#include <gnutls/x509.h>
#include <string.h>

#include "gtlscertificate-gnutls.h"
#include <glib/gi18n-lib.h>

static void     g_tls_certificate_gnutls_initable_iface_init (GInitableIface  *iface);

G_DEFINE_TYPE_WITH_CODE (KmsGTlsCertificateGnutls, g_tls_certificate_gnutls, G_TYPE_TLS_CERTIFICATE,
			 G_IMPLEMENT_INTERFACE (G_TYPE_INITABLE,
						g_tls_certificate_gnutls_initable_iface_init);)

enum
{
  PROP_0,

  PROP_CERTIFICATE,
  PROP_CERTIFICATE_PEM,
  PROP_PRIVATE_KEY,
  PROP_PRIVATE_KEY_PEM,
  PROP_ISSUER
};

struct _KmsGTlsCertificateGnutlsPrivate
{
  gnutls_x509_crt_t cert;
  gnutls_x509_privkey_t key;

  KmsGTlsCertificateGnutls *issuer;

  GError *construct_error;

  guint have_cert : 1;
  guint have_key  : 1;
};

static void
g_tls_certificate_gnutls_finalize (GObject *object)
{
  KmsGTlsCertificateGnutls *gnutls = G_KMS_TLS_CERTIFICATE_GNUTLS (object);

  gnutls_x509_crt_deinit (gnutls->priv->cert);
  if (gnutls->priv->key)
    gnutls_x509_privkey_deinit (gnutls->priv->key);

  if (gnutls->priv->issuer)
    g_object_unref (gnutls->priv->issuer);

  g_clear_error (&gnutls->priv->construct_error);

  G_OBJECT_CLASS (g_tls_certificate_gnutls_parent_class)->finalize (object);
}

static void
g_tls_certificate_gnutls_get_property (GObject    *object,
				       guint       prop_id,
				       GValue     *value,
				       GParamSpec *pspec)
{
  KmsGTlsCertificateGnutls *gnutls = G_KMS_TLS_CERTIFICATE_GNUTLS (object);
  GByteArray *certificate;
  char *certificate_pem;
  int status;
  size_t size;

  switch (prop_id)
    {
    case PROP_CERTIFICATE:
      size = 0;
      status = gnutls_x509_crt_export (gnutls->priv->cert,
				       GNUTLS_X509_FMT_DER,
				       NULL, &size);
      if (status != GNUTLS_E_SHORT_MEMORY_BUFFER)
	certificate = NULL;
      else
	{
	  certificate = g_byte_array_sized_new (size);
	  certificate->len = size;
	  status = gnutls_x509_crt_export (gnutls->priv->cert,
					   GNUTLS_X509_FMT_DER,
					   certificate->data, &size);
	  if (status != 0)
	    {
	      g_byte_array_free (certificate, TRUE);
	      certificate = NULL;
	    }
	}
      g_value_take_boxed (value, certificate);
      break;

    case PROP_CERTIFICATE_PEM:
      size = 0;
      status = gnutls_x509_crt_export (gnutls->priv->cert,
				       GNUTLS_X509_FMT_PEM,
				       NULL, &size);
      if (status != GNUTLS_E_SHORT_MEMORY_BUFFER)
	certificate_pem = NULL;
      else
	{
	  certificate_pem = g_malloc (size);
	  status = gnutls_x509_crt_export (gnutls->priv->cert,
					   GNUTLS_X509_FMT_PEM,
					   certificate_pem, &size);
	  if (status != 0)
	    {
	      g_free (certificate_pem);
	      certificate_pem = NULL;
	    }
	}
      g_value_take_string (value, certificate_pem);
      break;

    case PROP_ISSUER:
      g_value_set_object (value, gnutls->priv->issuer);
      break;

    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
    }
}

static void
g_tls_certificate_gnutls_set_property (GObject      *object,
				       guint         prop_id,
				       const GValue *value,
				       GParamSpec   *pspec)
{
  KmsGTlsCertificateGnutls *gnutls = G_KMS_TLS_CERTIFICATE_GNUTLS (object);
  GByteArray *bytes;
  const char *string;
  gnutls_datum_t data;
  int status;

  switch (prop_id)
    {
    case PROP_CERTIFICATE:
      bytes = g_value_get_boxed (value);
      if (!bytes)
	break;
      g_return_if_fail (gnutls->priv->have_cert == FALSE);
      data.data = bytes->data;
      data.size = bytes->len;
      status = gnutls_x509_crt_import (gnutls->priv->cert, &data,
				       GNUTLS_X509_FMT_DER);
      if (status == 0)
	gnutls->priv->have_cert = TRUE;
      else if (!gnutls->priv->construct_error)
	{
	  gnutls->priv->construct_error =
	    g_error_new (G_TLS_ERROR, G_TLS_ERROR_BAD_CERTIFICATE,
			 _("Could not parse DER certificate: %s"),
			 gnutls_strerror (status));
	}

      break;

    case PROP_CERTIFICATE_PEM:
      string = g_value_get_string (value);
      if (!string)
	break;
      g_return_if_fail (gnutls->priv->have_cert == FALSE);
      data.data = (void *)string;
      data.size = strlen (string);
      status = gnutls_x509_crt_import (gnutls->priv->cert, &data,
				       GNUTLS_X509_FMT_PEM);
      if (status == 0)
	gnutls->priv->have_cert = TRUE;
      else if (!gnutls->priv->construct_error)
	{
	  gnutls->priv->construct_error =
	    g_error_new (G_TLS_ERROR, G_TLS_ERROR_BAD_CERTIFICATE,
			 _("Could not parse PEM certificate: %s"),
			 gnutls_strerror (status));
	}
      break;

    case PROP_PRIVATE_KEY:
      bytes = g_value_get_boxed (value);
      if (!bytes)
	break;
      g_return_if_fail (gnutls->priv->have_key == FALSE);
      data.data = bytes->data;
      data.size = bytes->len;
      if (!gnutls->priv->key)
        gnutls_x509_privkey_init (&gnutls->priv->key);
      status = gnutls_x509_privkey_import (gnutls->priv->key, &data,
					   GNUTLS_X509_FMT_DER);
      if (status != 0)
	{
	  int pkcs8_status =
	    gnutls_x509_privkey_import_pkcs8 (gnutls->priv->key, &data,
					      GNUTLS_X509_FMT_DER, NULL,
					      GNUTLS_PKCS_PLAIN);
	  if (pkcs8_status == 0)
	    status = 0;
	}
      if (status == 0)
	gnutls->priv->have_key = TRUE;
      else if (!gnutls->priv->construct_error)
	{
	  gnutls->priv->construct_error =
	    g_error_new (G_TLS_ERROR, G_TLS_ERROR_BAD_CERTIFICATE,
			 _("Could not parse DER private key: %s"),
			 gnutls_strerror (status));
	}
      break;

    case PROP_PRIVATE_KEY_PEM:
      string = g_value_get_string (value);
      if (!string)
	break;
      g_return_if_fail (gnutls->priv->have_key == FALSE);
      data.data = (void *)string;
      data.size = strlen (string);
      if (!gnutls->priv->key)
        gnutls_x509_privkey_init (&gnutls->priv->key);
      status = gnutls_x509_privkey_import (gnutls->priv->key, &data,
					   GNUTLS_X509_FMT_PEM);
      if (status != 0)
	{
	  int pkcs8_status =
	    gnutls_x509_privkey_import_pkcs8 (gnutls->priv->key, &data,
					      GNUTLS_X509_FMT_PEM, NULL,
					      GNUTLS_PKCS_PLAIN);
	  if (pkcs8_status == 0)
	    status = 0;
	}
      if (status == 0)
	gnutls->priv->have_key = TRUE;
      else if (!gnutls->priv->construct_error)
	{
	  gnutls->priv->construct_error =
	    g_error_new (G_TLS_ERROR, G_TLS_ERROR_BAD_CERTIFICATE,
			 _("Could not parse PEM private key: %s"),
			 gnutls_strerror (status));
	}
      break;

    case PROP_ISSUER:
      gnutls->priv->issuer = g_value_dup_object (value);
      break;

    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
    }
}

static void
g_tls_certificate_gnutls_init (KmsGTlsCertificateGnutls *gnutls)
{
  gnutls->priv = G_TYPE_INSTANCE_GET_PRIVATE (gnutls,
					      G_TYPE_KMS_TLS_CERTIFICATE_GNUTLS,
					      KmsGTlsCertificateGnutlsPrivate);

  gnutls_x509_crt_init (&gnutls->priv->cert);
}

static gboolean
g_tls_certificate_gnutls_initable_init (GInitable       *initable,
					GCancellable    *cancellable,
					GError         **error)
{
  KmsGTlsCertificateGnutls *gnutls = G_KMS_TLS_CERTIFICATE_GNUTLS (initable);

  if (gnutls->priv->construct_error)
    {
      g_propagate_error (error, gnutls->priv->construct_error);
      gnutls->priv->construct_error = NULL;
      return FALSE;
    }
  else if (!gnutls->priv->have_cert)
    {
      g_set_error_literal (error, G_TLS_ERROR, G_TLS_ERROR_BAD_CERTIFICATE,
			   _("No certificate data provided"));
      return FALSE;
    }
  else
    return TRUE;
}

static GTlsCertificateFlags
g_tls_certificate_gnutls_verify (GTlsCertificate     *cert,
				 GSocketConnectable  *identity,
				 GTlsCertificate     *trusted_ca)
{
  KmsGTlsCertificateGnutls *cert_gnutls;
  guint num_certs, i;
  gnutls_x509_crt_t *chain;
  GTlsCertificateFlags gtls_flags;
  time_t t, now;
  
  cert_gnutls = G_KMS_TLS_CERTIFICATE_GNUTLS (cert);
  for (num_certs = 0; cert_gnutls; cert_gnutls = cert_gnutls->priv->issuer)
    num_certs++;
  chain = g_new (gnutls_x509_crt_t, num_certs);
  cert_gnutls = G_KMS_TLS_CERTIFICATE_GNUTLS (cert);
  for (i = 0; cert_gnutls; cert_gnutls = cert_gnutls->priv->issuer, i++)
    chain[i] = cert_gnutls->priv->cert;

  if (trusted_ca)
    {
      gnutls_x509_crt_t ca;
      guint gnutls_flags;
      int status;

      ca = G_KMS_TLS_CERTIFICATE_GNUTLS (trusted_ca)->priv->cert;
      status = gnutls_x509_crt_list_verify (chain, num_certs,
					    &ca, 1,
					    NULL, 0,
					    GNUTLS_VERIFY_ALLOW_X509_V1_CA_CRT,
					    &gnutls_flags);
      if (status != 0)
	{
	  g_free (chain);
	  return G_TLS_CERTIFICATE_GENERIC_ERROR;
	}

      gtls_flags = g_tls_certificate_gnutls_convert_flags (gnutls_flags);
    }
  else
    gtls_flags = 0;

  /* We have to check these ourselves since gnutls_x509_crt_list_verify
   * won't bother if it gets an UNKNOWN_CA.
   */
  now = time (NULL);
  for (i = 0; i < num_certs; i++)
    {
      t = gnutls_x509_crt_get_activation_time (chain[i]);
      if (t == (time_t) -1 || t > now)
	gtls_flags |= G_TLS_CERTIFICATE_NOT_ACTIVATED;

      t = gnutls_x509_crt_get_expiration_time (chain[i]);
      if (t == (time_t) -1 || t < now)
	gtls_flags |= G_TLS_CERTIFICATE_EXPIRED;
    }

  g_free (chain);

  if (identity)
    gtls_flags |= g_tls_certificate_gnutls_verify_identity (G_KMS_TLS_CERTIFICATE_GNUTLS (cert), identity);

  return gtls_flags;
}

static void
g_tls_certificate_gnutls_real_copy (KmsGTlsCertificateGnutls    *gnutls,
                                    const gchar              *interaction_id,
                                    gnutls_retr2_st          *st)
{
  gnutls_x509_crt_t cert;
  gnutls_datum_t data;
  size_t size = 0;

  gnutls_x509_crt_export (gnutls->priv->cert, GNUTLS_X509_FMT_DER,
                          NULL, &size);
  data.data = g_malloc (size);
  data.size = size;
  gnutls_x509_crt_export (gnutls->priv->cert, GNUTLS_X509_FMT_DER,
                          data.data, &size);

  gnutls_x509_crt_init (&cert);
  gnutls_x509_crt_import (cert, &data, GNUTLS_X509_FMT_DER);
  g_free (data.data);

  st->ncerts = 1;
  st->cert.x509 = gnutls_malloc (sizeof (gnutls_x509_crt_t));
  st->cert.x509[0] = cert;

  if (gnutls->priv->key != NULL)
    {
      gnutls_x509_privkey_init (&st->key.x509);
      gnutls_x509_privkey_cpy (st->key.x509, gnutls->priv->key);
      st->key_type = GNUTLS_PRIVKEY_X509;
    }

  st->deinit_all = TRUE;
}

static void
g_tls_certificate_gnutls_class_init (KmsGTlsCertificateGnutlsClass *klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GTlsCertificateClass *certificate_class = G_TLS_CERTIFICATE_CLASS (klass);

  g_type_class_add_private (klass, sizeof (KmsGTlsCertificateGnutlsPrivate));

  gobject_class->get_property = g_tls_certificate_gnutls_get_property;
  gobject_class->set_property = g_tls_certificate_gnutls_set_property;
  gobject_class->finalize     = g_tls_certificate_gnutls_finalize;

  certificate_class->verify = g_tls_certificate_gnutls_verify;

  klass->copy = g_tls_certificate_gnutls_real_copy;

  g_object_class_override_property (gobject_class, PROP_CERTIFICATE, "certificate");
  g_object_class_override_property (gobject_class, PROP_CERTIFICATE_PEM, "certificate-pem");
  g_object_class_override_property (gobject_class, PROP_PRIVATE_KEY, "private-key");
  g_object_class_override_property (gobject_class, PROP_PRIVATE_KEY_PEM, "private-key-pem");
  g_object_class_override_property (gobject_class, PROP_ISSUER, "issuer");
}

static void
g_tls_certificate_gnutls_initable_iface_init (GInitableIface  *iface)
{
  iface->init = g_tls_certificate_gnutls_initable_init;
}

GTlsCertificate *
g_tls_certificate_gnutls_new (const gnutls_datum_t *datum,
			      GTlsCertificate      *issuer)
{
  KmsGTlsCertificateGnutls *gnutls;

  gnutls = g_object_new (G_TYPE_KMS_TLS_CERTIFICATE_GNUTLS,
			 "issuer", issuer,
			 NULL);
  g_tls_certificate_gnutls_set_data (gnutls, datum);

  return G_TLS_CERTIFICATE (gnutls);
}

void
g_tls_certificate_gnutls_set_data (KmsGTlsCertificateGnutls *gnutls,
                                   const gnutls_datum_t  *datum)
{
  g_return_if_fail (G_IS_KMS_TLS_CERTIFICATE_GNUTLS (gnutls));
  g_return_if_fail (!gnutls->priv->have_cert);

  if (gnutls_x509_crt_import (gnutls->priv->cert, datum,
                              GNUTLS_X509_FMT_DER) == 0)
    gnutls->priv->have_cert = TRUE;
}

const gnutls_x509_crt_t
g_tls_certificate_gnutls_get_cert (KmsGTlsCertificateGnutls *gnutls)
{
  return gnutls->priv->cert;
}

gboolean
g_tls_certificate_gnutls_has_key (KmsGTlsCertificateGnutls *gnutls)
{
  return gnutls->priv->have_key;
}

void
g_tls_certificate_gnutls_copy  (KmsGTlsCertificateGnutls *gnutls,
                                const gchar           *interaction_id,
                                gnutls_retr2_st       *st)
{
  g_return_if_fail (G_IS_KMS_TLS_CERTIFICATE_GNUTLS (gnutls));
  g_return_if_fail (st != NULL);
  g_return_if_fail (G_KMS_TLS_CERTIFICATE_GNUTLS_GET_CLASS (gnutls)->copy);
  G_KMS_TLS_CERTIFICATE_GNUTLS_GET_CLASS (gnutls)->copy (gnutls, interaction_id, st);
}

static const struct {
  int gnutls_flag;
  GTlsCertificateFlags gtls_flag;
} flags_map[] = {
  { GNUTLS_CERT_SIGNER_NOT_FOUND | GNUTLS_CERT_SIGNER_NOT_CA, G_TLS_CERTIFICATE_UNKNOWN_CA },
  { GNUTLS_CERT_NOT_ACTIVATED, G_TLS_CERTIFICATE_NOT_ACTIVATED },
  { GNUTLS_CERT_EXPIRED, G_TLS_CERTIFICATE_EXPIRED },
  { GNUTLS_CERT_REVOKED, G_TLS_CERTIFICATE_REVOKED },
  { GNUTLS_CERT_INSECURE_ALGORITHM, G_TLS_CERTIFICATE_INSECURE }
};
static const int flags_map_size = G_N_ELEMENTS (flags_map);

GTlsCertificateFlags
g_tls_certificate_gnutls_convert_flags (guint gnutls_flags)
{
  int i;
  GTlsCertificateFlags gtls_flags;

  /* Convert GNUTLS status to GTlsCertificateFlags. GNUTLS sets
   * GNUTLS_CERT_INVALID if it sets any other flag, so we want to
   * strip that out unless it's the only flag set. Then we convert
   * specific flags we recognize, and if there are any flags left over
   * at the end, we add G_TLS_CERTIFICATE_GENERIC_ERROR.
   */
  gtls_flags = 0;

  if (gnutls_flags != GNUTLS_CERT_INVALID)
    gnutls_flags = gnutls_flags & ~GNUTLS_CERT_INVALID;
  for (i = 0; i < flags_map_size && gnutls_flags != 0; i++)
    {
      if (gnutls_flags & flags_map[i].gnutls_flag)
	{
	  gnutls_flags &= ~flags_map[i].gnutls_flag;
	  gtls_flags |= flags_map[i].gtls_flag;
	}
    }
  if (gnutls_flags)
    gtls_flags |= G_TLS_CERTIFICATE_GENERIC_ERROR;

  return gtls_flags;
}

GTlsCertificateFlags
g_tls_certificate_gnutls_verify_identity (KmsGTlsCertificateGnutls *gnutls,
					  GSocketConnectable    *identity)
{
  const char *hostname;

  if (G_IS_NETWORK_ADDRESS (identity))
    hostname = g_network_address_get_hostname (G_NETWORK_ADDRESS (identity));
  else if (G_IS_NETWORK_SERVICE (identity))
    hostname = g_network_service_get_domain (G_NETWORK_SERVICE (identity));
  else
    hostname = NULL;

  if (hostname)
    {
      if (gnutls_x509_crt_check_hostname (gnutls->priv->cert, hostname))
	return 0;
    }

  /* FIXME: check sRVName and uniformResourceIdentifier
   * subjectAltNames, if appropriate for @identity.
   */

  return G_TLS_CERTIFICATE_BAD_IDENTITY;
}

void
g_tls_certificate_gnutls_set_issuer (KmsGTlsCertificateGnutls *gnutls,
                                     KmsGTlsCertificateGnutls *issuer)
{
  g_return_if_fail (G_IS_KMS_TLS_CERTIFICATE_GNUTLS (gnutls));
  g_return_if_fail (!issuer || G_IS_KMS_TLS_CERTIFICATE_GNUTLS (issuer));

  if (issuer)
    g_object_ref (issuer);
  if (gnutls->priv->issuer)
    g_object_unref (gnutls->priv->issuer);
  gnutls->priv->issuer = issuer;
  g_object_notify (G_OBJECT (gnutls), "issuer");
}

GBytes *
g_tls_certificate_gnutls_get_bytes (KmsGTlsCertificateGnutls *gnutls)
{
  GByteArray *array;

  g_return_val_if_fail (G_IS_KMS_TLS_CERTIFICATE_GNUTLS (gnutls), NULL);

  g_object_get (gnutls, "certificate", &array, NULL);
  return g_byte_array_free_to_bytes (array);
}
