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

#ifndef __G_KMS_TLS_SERVER_CONNECTION_GNUTLS_H__
#define __G_KMS_TLS_SERVER_CONNECTION_GNUTLS_H__

#include <gio/gio.h> 
#include "gtlsconnection-gnutls.h"

G_BEGIN_DECLS

#define G_TYPE_KMS_TLS_SERVER_CONNECTION_GNUTLS            (g_tls_server_connection_gnutls_get_type ())
#define G_KMS_TLS_SERVER_CONNECTION_GNUTLS(inst)           (G_TYPE_CHECK_INSTANCE_CAST ((inst), G_TYPE_KMS_TLS_SERVER_CONNECTION_GNUTLS, KmsGTlsServerConnectionGnutls))
#define G_KMS_TLS_SERVER_CONNECTION_GNUTLS_CLASS(class)    (G_TYPE_CHECK_CLASS_CAST ((class), G_TYPE_KMS_TLS_SERVER_CONNECTION_GNUTLS, KmsGTlsServerConnectionGnutlsClass))
#define G_IS_TLS_SERVER_CONNECTION_GNUTLS(inst)        (G_TYPE_CHECK_INSTANCE_TYPE ((inst), G_TYPE_KMS_TLS_SERVER_CONNECTION_GNUTLS))
#define G_IS_TLS_SERVER_CONNECTION_GNUTLS_CLASS(class) (G_TYPE_CHECK_CLASS_TYPE ((class), G_TYPE_KMS_TLS_SERVER_CONNECTION_GNUTLS))
#define G_KMS_TLS_SERVER_CONNECTION_GNUTLS_GET_CLASS(inst) (G_TYPE_INSTANCE_GET_CLASS ((inst), G_TYPE_KMS_TLS_SERVER_CONNECTION_GNUTLS, KmsGTlsServerConnectionGnutlsClass))

typedef struct _KmsGTlsServerConnectionGnutlsPrivate                   KmsGTlsServerConnectionGnutlsPrivate;
typedef struct _KmsGTlsServerConnectionGnutlsClass                     KmsGTlsServerConnectionGnutlsClass;
typedef struct _KmsGTlsServerConnectionGnutls                          KmsGTlsServerConnectionGnutls;

struct _KmsGTlsServerConnectionGnutlsClass
{
  KmsGTlsConnectionGnutlsClass parent_class;
};

struct _KmsGTlsServerConnectionGnutls
{
  KmsGTlsConnectionGnutls parent_instance;
  KmsGTlsServerConnectionGnutlsPrivate *priv;
};

GType g_tls_server_connection_gnutls_get_type (void) G_GNUC_CONST;

G_END_DECLS

#endif /* __G_KMS_TLS_SERVER_CONNECTION_GNUTLS_H___ */
