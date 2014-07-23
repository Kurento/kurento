/* GIO - GLib Input, Output and Streaming Library
 *
 * Copyright 2009 Red Hat, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2 of the licence or (at
 * your option) any later version.
 *
 * See the included COPYING file for more information.
 */

#ifndef __G_KMS_TLS_CERTIFICATE_GNUTLS_H__
#define __G_KMS_TLS_CERTIFICATE_GNUTLS_H__

#include <gio/gio.h>
#include <gnutls/gnutls.h>

G_BEGIN_DECLS

#define G_TYPE_KMS_TLS_CERTIFICATE_GNUTLS            (g_tls_certificate_gnutls_get_type ())
#define G_KMS_TLS_CERTIFICATE_GNUTLS(inst)           (G_TYPE_CHECK_INSTANCE_CAST ((inst), G_TYPE_KMS_TLS_CERTIFICATE_GNUTLS, KmsGTlsCertificateGnutls))
#define G_KMS_TLS_CERTIFICATE_GNUTLS_CLASS(class)    (G_TYPE_CHECK_CLASS_CAST ((class), G_TYPE_KMS_TLS_CERTIFICATE_GNUTLS, KmsGTlsCertificateGnutlsClass))
#define G_IS_KMS_TLS_CERTIFICATE_GNUTLS(inst)        (G_TYPE_CHECK_INSTANCE_TYPE ((inst), G_TYPE_KMS_TLS_CERTIFICATE_GNUTLS))
#define G_IS_KMS_TLS_CERTIFICATE_GNUTLS_CLASS(class) (G_TYPE_CHECK_CLASS_TYPE ((class), G_TYPE_KMS_TLS_CERTIFICATE_GNUTLS))
#define G_KMS_TLS_CERTIFICATE_GNUTLS_GET_CLASS(inst) (G_TYPE_INSTANCE_GET_CLASS ((inst), G_TYPE_KMS_TLS_CERTIFICATE_GNUTLS, KmsGTlsCertificateGnutlsClass))

typedef struct _KmsGTlsCertificateGnutlsPrivate                   KmsGTlsCertificateGnutlsPrivate;
typedef struct _KmsGTlsCertificateGnutlsClass                     KmsGTlsCertificateGnutlsClass;
typedef struct _KmsGTlsCertificateGnutls                          KmsGTlsCertificateGnutls;

struct _KmsGTlsCertificateGnutlsClass
{
  GTlsCertificateClass parent_class;

  void              (*copy)               (KmsGTlsCertificateGnutls    *gnutls,
                                           const gchar              *interaction_id,
                                           gnutls_retr2_st          *st);
};

struct _KmsGTlsCertificateGnutls
{
  GTlsCertificate parent_instance;
  KmsGTlsCertificateGnutlsPrivate *priv;
};

GType g_tls_certificate_gnutls_get_type (void) G_GNUC_CONST;

GTlsCertificate *            g_tls_certificate_gnutls_new             (const gnutls_datum_t  *datum,
                                                                       GTlsCertificate       *issuer);

GBytes *                     g_tls_certificate_gnutls_get_bytes       (KmsGTlsCertificateGnutls *gnutls);

void                         g_tls_certificate_gnutls_set_data        (KmsGTlsCertificateGnutls *gnutls,
                                                                       const gnutls_datum_t  *datum);

const gnutls_x509_crt_t      g_tls_certificate_gnutls_get_cert        (KmsGTlsCertificateGnutls *gnutls);
gboolean                     g_tls_certificate_gnutls_has_key         (KmsGTlsCertificateGnutls *gnutls);

void                         g_tls_certificate_gnutls_copy            (KmsGTlsCertificateGnutls *gnutls,
                                                                       const gchar           *interaction_id,
                                                                       gnutls_retr2_st       *st);

GTlsCertificateFlags         g_tls_certificate_gnutls_verify_identity (KmsGTlsCertificateGnutls *gnutls,
								       GSocketConnectable    *identity);

GTlsCertificateFlags         g_tls_certificate_gnutls_convert_flags   (guint                  gnutls_flags);

void                         g_tls_certificate_gnutls_set_issuer      (KmsGTlsCertificateGnutls *gnutls,
                                                                       KmsGTlsCertificateGnutls *issuer);

KmsGTlsCertificateGnutls*       g_tls_certificate_gnutls_steal_issuer    (KmsGTlsCertificateGnutls *gnutls);

G_END_DECLS

#endif /* __G_KMS_TLS_CERTIFICATE_GNUTLS_H___ */
