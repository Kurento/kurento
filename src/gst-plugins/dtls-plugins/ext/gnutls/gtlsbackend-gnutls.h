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

#ifndef __G_KMS_TLS_BACKEND_GNUTLS_H__
#define __G_KMS_TLS_BACKEND_GNUTLS_H__

#include <gio/gio.h>
#include <gnutls/gnutls.h>

G_BEGIN_DECLS

#define G_TYPE_KMS_TLS_BACKEND_GNUTLS            (g_tls_backend_gnutls_get_type ())
#define G_KMS_TLS_BACKEND_GNUTLS(inst)           (G_TYPE_CHECK_INSTANCE_CAST ((inst), G_TYPE_KMS_TLS_BACKEND_GNUTLS, KmsGTlsBackendGnutls))
#define G_KMS_TLS_BACKEND_GNUTLS_CLASS(class)    (G_TYPE_CHECK_CLASS_CAST ((class), G_TYPE_KMS_TLS_BACKEND_GNUTLS, KmsGTlsBackendGnutlsClass))
#define G_IS_KMS_TLS_BACKEND_GNUTLS(inst)        (G_TYPE_CHECK_INSTANCE_TYPE ((inst), G_TYPE_KMS_TLS_BACKEND_GNUTLS))
#define G_IS_KMS_TLS_BACKEND_GNUTLS_CLASS(class) (G_TYPE_CHECK_CLASS_TYPE ((class), G_TYPE_KMS_TLS_BACKEND_GNUTLS))
#define G_KMS_TLS_BACKEND_GNUTLS_GET_CLASS(inst) (G_TYPE_INSTANCE_GET_CLASS ((inst), G_TYPE_KMS_TLS_BACKEND_GNUTLS, KmsGTlsBackendGnutlsClass))

typedef struct _KmsGTlsBackendGnutls        KmsGTlsBackendGnutls;
typedef struct _KmsGTlsBackendGnutlsClass   KmsGTlsBackendGnutlsClass;
typedef struct _KmsGTlsBackendGnutlsPrivate KmsGTlsBackendGnutlsPrivate;

struct _KmsGTlsBackendGnutlsClass
{
  GObjectClass parent_class;

  GTlsDatabase*   (*create_database)      (KmsGTlsBackendGnutls          *self,
                                           GError                    **error);
};

struct _KmsGTlsBackendGnutls
{
  GObject parent_instance;
  KmsGTlsBackendGnutlsPrivate *priv;
};

GType g_tls_backend_gnutls_get_type (void) G_GNUC_CONST;
void  g_tls_backend_gnutls_register (GIOModule *module);

void    g_tls_backend_gnutls_store_session  (unsigned int             type,
					     GBytes                  *session_id,
					     GBytes                  *session_data);
void    g_tls_backend_gnutls_remove_session (unsigned int             type,
					     GBytes                  *session_id);
GBytes *g_tls_backend_gnutls_lookup_session (unsigned int             type,
					     GBytes                  *session_id);

GTlsDatabase* g_tls_backend_gnutls_get_default_database_static (void);

G_END_DECLS

#endif /* __G_KMS_TLS_BACKEND_GNUTLS_H___ */
