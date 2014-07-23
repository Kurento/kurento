/* GIO - GLib Input, Output and Streaming Library
 *
 * Copyright 2010 Red Hat, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2 of the licence or (at
 * your option) any later version.
 *
 * See the included COPYING file for more information.
 */

#ifndef __G_KMS_TLS_CLIENT_CONNECTION_GNUTLS_H__
#define __G_KMS_TLS_CLIENT_CONNECTION_GNUTLS_H__

#include "gtlsconnection-gnutls.h"

G_BEGIN_DECLS

#define G_TYPE_KMS_TLS_CLIENT_CONNECTION_GNUTLS            (g_tls_client_connection_gnutls_get_type ())
#define G_KMS_TLS_CLIENT_CONNECTION_GNUTLS(inst)           (G_TYPE_CHECK_INSTANCE_CAST ((inst), G_TYPE_KMS_TLS_CLIENT_CONNECTION_GNUTLS, KmsGTlsClientConnectionGnutls))
#define G_KMS_TLS_CLIENT_CONNECTION_GNUTLS_CLASS(class)    (G_TYPE_CHECK_CLASS_CAST ((class), G_TYPE_KMS_TLS_CLIENT_CONNECTION_GNUTLS, KmsGTlsClientConnectionGnutlsClass))
#define G_IS_TLS_CLIENT_CONNECTION_GNUTLS(inst)        (G_TYPE_CHECK_INSTANCE_TYPE ((inst), G_TYPE_KMS_TLS_CLIENT_CONNECTION_GNUTLS))
#define G_IS_TLS_CLIENT_CONNECTION_GNUTLS_CLASS(class) (G_TYPE_CHECK_CLASS_TYPE ((class), G_TYPE_KMS_TLS_CLIENT_CONNECTION_GNUTLS))
#define G_KMS_TLS_CLIENT_CONNECTION_GNUTLS_GET_CLASS(inst) (G_TYPE_INSTANCE_GET_CLASS ((inst), G_TYPE_KMS_TLS_CLIENT_CONNECTION_GNUTLS, KmsGTlsClientConnectionGnutlsClass))

typedef struct _KmsGTlsClientConnectionGnutlsPrivate KmsGTlsClientConnectionGnutlsPrivate;
typedef struct _KmsGTlsClientConnectionGnutlsClass   KmsGTlsClientConnectionGnutlsClass;
typedef struct _KmsGTlsClientConnectionGnutls        KmsGTlsClientConnectionGnutls;

struct _KmsGTlsClientConnectionGnutlsClass
{
  KmsGTlsConnectionGnutlsClass parent_class;
};

struct _KmsGTlsClientConnectionGnutls
{
  KmsGTlsConnectionGnutls parent_instance;
  KmsGTlsClientConnectionGnutlsPrivate *priv;
};

GType g_tls_client_connection_gnutls_get_type (void) G_GNUC_CONST;

G_END_DECLS

#endif /* __G_KMS_TLS_CLIENT_CONNECTION_GNUTLS_H___ */
