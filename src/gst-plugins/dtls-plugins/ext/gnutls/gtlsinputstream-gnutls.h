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

#ifndef __G_KMS_TLS_INPUT_STREAM_GNUTLS_H__
#define __G_KMS_TLS_INPUT_STREAM_GNUTLS_H__

#include <gio/gio.h>
#include "gtlsconnection-gnutls.h"

G_BEGIN_DECLS

#define G_TYPE_KMS_TLS_INPUT_STREAM_GNUTLS            (g_tls_input_stream_gnutls_get_type ())
#define G_KMS_TLS_INPUT_STREAM_GNUTLS(inst)           (G_TYPE_CHECK_INSTANCE_CAST ((inst), G_TYPE_KMS_TLS_INPUT_STREAM_GNUTLS, KmsGTlsInputStreamGnutls))
#define G_KMS_TLS_INPUT_STREAM_GNUTLS_CLASS(class)    (G_TYPE_CHECK_CLASS_CAST ((class), G_TYPE_KMS_TLS_INPUT_STREAM_GNUTLS, KmsGTlsInputStreamGnutlsClass))
#define G_IS_KMS_TLS_INPUT_STREAM_GNUTLS(inst)        (G_TYPE_CHECK_INSTANCE_TYPE ((inst), G_TYPE_KMS_TLS_INPUT_STREAM_GNUTLS))
#define G_IS_KMS_TLS_INPUT_STREAM_GNUTLS_CLASS(class) (G_TYPE_CHECK_CLASS_TYPE ((class), G_TYPE_KMS_TLS_INPUT_STREAM_GNUTLS))
#define G_KMS_TLS_INPUT_STREAM_GNUTLS_GET_CLASS(inst) (G_TYPE_INSTANCE_GET_CLASS ((inst), G_TYPE_KMS_TLS_INPUT_STREAM_GNUTLS, KmsGTlsInputStreamGnutlsClass))

typedef struct _KmsGTlsInputStreamGnutlsPrivate KmsGTlsInputStreamGnutlsPrivate;
typedef struct _KmsGTlsInputStreamGnutlsClass   KmsGTlsInputStreamGnutlsClass;
typedef struct _KmsGTlsInputStreamGnutls        KmsGTlsInputStreamGnutls;

struct _KmsGTlsInputStreamGnutlsClass
{
  GInputStreamClass parent_class;
};

struct _KmsGTlsInputStreamGnutls
{
  GInputStream parent_instance;
  KmsGTlsInputStreamGnutlsPrivate *priv;
};

GType         g_tls_input_stream_gnutls_get_type (void) G_GNUC_CONST;
GInputStream *g_tls_input_stream_gnutls_new      (KmsGTlsConnectionGnutls *conn);

G_END_DECLS

#endif /* __G_KMS_TLS_INPUT_STREAM_GNUTLS_H___ */
