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

#ifndef __G_KMS_TLS_CONNECTION_GNUTLS_H__
#define __G_KMS_TLS_CONNECTION_GNUTLS_H__

#include <gio/gio.h>
#include <gnutls/gnutls.h>

#include "ext/gio/kmsgioenums.h"

G_BEGIN_DECLS

#define G_TYPE_KMS_TLS_CONNECTION_GNUTLS            (g_tls_connection_gnutls_get_type ())
#define G_KMS_TLS_CONNECTION_GNUTLS(inst)           (G_TYPE_CHECK_INSTANCE_CAST ((inst), G_TYPE_KMS_TLS_CONNECTION_GNUTLS, KmsGTlsConnectionGnutls))
#define G_KMS_TLS_CONNECTION_GNUTLS_CLASS(class)    (G_TYPE_CHECK_CLASS_CAST ((class), G_TYPE_KMS_TLS_CONNECTION_GNUTLS, KmsGTlsConnectionGnutlsClass))
#define G_IS_KMS_TLS_CONNECTION_GNUTLS(inst)        (G_TYPE_CHECK_INSTANCE_TYPE ((inst), G_TYPE_KMS_TLS_CONNECTION_GNUTLS))
#define G_IS_KMS_TLS_CONNECTION_GNUTLS_CLASS(class) (G_TYPE_CHECK_CLASS_TYPE ((class), G_TYPE_KMS_TLS_CONNECTION_GNUTLS))
#define G_KMS_TLS_CONNECTION_GNUTLS_GET_CLASS(inst) (G_TYPE_INSTANCE_GET_CLASS ((inst), G_TYPE_KMS_TLS_CONNECTION_GNUTLS, KmsGTlsConnectionGnutlsClass))

typedef struct _KmsGTlsConnectionGnutlsPrivate                   KmsGTlsConnectionGnutlsPrivate;
typedef struct _KmsGTlsConnectionGnutlsClass                     KmsGTlsConnectionGnutlsClass;
typedef struct _KmsGTlsConnectionGnutls                          KmsGTlsConnectionGnutls;

struct _KmsGTlsConnectionGnutlsClass
{
  GTlsConnectionClass parent_class;

  void     (*failed)           (KmsGTlsConnectionGnutls  *gnutls);

  void     (*begin_handshake)  (KmsGTlsConnectionGnutls  *gnutls);
  void     (*finish_handshake) (KmsGTlsConnectionGnutls  *gnutls,
				GError               **inout_error);
};

struct _KmsGTlsConnectionGnutls
{
  GTlsConnection parent_instance;
  KmsGTlsConnectionGnutlsPrivate *priv;
};

GType g_tls_connection_gnutls_get_type (void) G_GNUC_CONST;

gnutls_certificate_credentials_t g_tls_connection_gnutls_get_credentials (KmsGTlsConnectionGnutls *connection);
gnutls_session_t                 g_tls_connection_gnutls_get_session     (KmsGTlsConnectionGnutls *connection);
void                             g_tls_connection_gnutls_get_certificate (KmsGTlsConnectionGnutls *gnutls,
                                                                        gnutls_retr2_st      *st);

gssize   g_tls_connection_gnutls_read          (KmsGTlsConnectionGnutls  *gnutls,
						void                  *buffer,
						gsize                  size,
						gboolean               blocking,
						GCancellable          *cancellable,
						GError               **error);
gssize   g_tls_connection_gnutls_write         (KmsGTlsConnectionGnutls  *gnutls,
						const void            *buffer,
						gsize                  size,
						gboolean               blocking,
						GCancellable          *cancellable,
						GError               **error);

gboolean g_tls_connection_gnutls_check         (KmsGTlsConnectionGnutls  *gnutls,
						GIOCondition           condition);
GSource *g_tls_connection_gnutls_create_source (KmsGTlsConnectionGnutls  *gnutls,
						GIOCondition           condition,
						GCancellable          *cancellable);

gboolean g_tls_connection_gnutls_add_srtp_profile (GTlsConnection         *conn,
                                                   GTlsSrtpProfile         profile);
GTlsSrtpProfile g_tls_connection_gnutls_get_selected_srtp_profile (GTlsConnection        *conn,
                                                                   GByteArray           **server_key,
                                                                   GByteArray           **server_salt,
                                                                   GByteArray           **client_key,
                                                                   GByteArray           **client_salt);

G_END_DECLS

#endif /* __G_KMS_TLS_CONNECTION_GNUTLS_H___ */
