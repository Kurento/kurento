/* GIO - GLib Input, Output and Streaming Library
 *
 * Copyright 2010 Red Hat, Inc.
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
#include "gtlsoutputstream-gnutls.h"

static void
g_tls_output_stream_gnutls_pollable_iface_init (GPollableOutputStreamInterface *
    iface);

G_DEFINE_TYPE_WITH_CODE (KmsGTlsOutputStreamGnutls, g_tls_output_stream_gnutls,
    G_TYPE_OUTPUT_STREAM, G_IMPLEMENT_INTERFACE (G_TYPE_POLLABLE_OUTPUT_STREAM,
        g_tls_output_stream_gnutls_pollable_iface_init)
    )

     struct _KmsGTlsOutputStreamGnutlsPrivate
     {
       KmsGTlsConnectionGnutls *conn;
     };

     static void g_tls_output_stream_gnutls_dispose (GObject * object)
{
  KmsGTlsOutputStreamGnutls *stream = G_KMS_TLS_OUTPUT_STREAM_GNUTLS (object);

  if (stream->priv->conn) {
    g_object_remove_weak_pointer (G_OBJECT (stream->priv->conn),
        (gpointer *) & stream->priv->conn);
    stream->priv->conn = NULL;
  }

  G_OBJECT_CLASS (g_tls_output_stream_gnutls_parent_class)->dispose (object);
}

static gssize
g_tls_output_stream_gnutls_write (GOutputStream * stream,
    const void *buffer,
    gsize count, GCancellable * cancellable, GError ** error)
{
  KmsGTlsOutputStreamGnutls *tls_stream =
      G_KMS_TLS_OUTPUT_STREAM_GNUTLS (stream);

  g_return_val_if_fail (tls_stream->priv->conn != NULL, -1);

  return g_tls_connection_gnutls_write (tls_stream->priv->conn,
      buffer, count, TRUE, cancellable, error);
}

static gboolean
g_tls_output_stream_gnutls_pollable_is_writable (GPollableOutputStream *
    pollable)
{
  KmsGTlsOutputStreamGnutls *tls_stream =
      G_KMS_TLS_OUTPUT_STREAM_GNUTLS (pollable);

  g_return_val_if_fail (tls_stream->priv->conn != NULL, FALSE);

  return g_tls_connection_gnutls_check (tls_stream->priv->conn, G_IO_OUT);
}

static GSource *
g_tls_output_stream_gnutls_pollable_create_source (GPollableOutputStream *
    pollable, GCancellable * cancellable)
{
  KmsGTlsOutputStreamGnutls *tls_stream =
      G_KMS_TLS_OUTPUT_STREAM_GNUTLS (pollable);

  g_return_val_if_fail (tls_stream->priv->conn != NULL, NULL);

  return g_tls_connection_gnutls_create_source (tls_stream->priv->conn,
      G_IO_OUT, cancellable);
}

static gssize
g_tls_output_stream_gnutls_pollable_write_nonblocking (GPollableOutputStream *
    pollable, const void *buffer, gsize size, GError ** error)
{
  KmsGTlsOutputStreamGnutls *tls_stream =
      G_KMS_TLS_OUTPUT_STREAM_GNUTLS (pollable);

  return g_tls_connection_gnutls_write (tls_stream->priv->conn,
      buffer, size, FALSE, NULL, error);
}

static void
g_tls_output_stream_gnutls_class_init (KmsGTlsOutputStreamGnutlsClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GOutputStreamClass *output_stream_class = G_OUTPUT_STREAM_CLASS (klass);

  g_type_class_add_private (klass, sizeof (KmsGTlsOutputStreamGnutlsPrivate));

  gobject_class->dispose = g_tls_output_stream_gnutls_dispose;

  output_stream_class->write_fn = g_tls_output_stream_gnutls_write;
}

static void
g_tls_output_stream_gnutls_pollable_iface_init (GPollableOutputStreamInterface *
    iface)
{
  iface->is_writable = g_tls_output_stream_gnutls_pollable_is_writable;
  iface->create_source = g_tls_output_stream_gnutls_pollable_create_source;
  iface->write_nonblocking =
      g_tls_output_stream_gnutls_pollable_write_nonblocking;
}

static void
g_tls_output_stream_gnutls_init (KmsGTlsOutputStreamGnutls * stream)
{
  stream->priv =
      G_TYPE_INSTANCE_GET_PRIVATE (stream, G_TYPE_KMS_TLS_OUTPUT_STREAM_GNUTLS,
      KmsGTlsOutputStreamGnutlsPrivate);
}

GOutputStream *
g_tls_output_stream_gnutls_new (KmsGTlsConnectionGnutls * conn)
{
  KmsGTlsOutputStreamGnutls *tls_stream;

  tls_stream = g_object_new (G_TYPE_KMS_TLS_OUTPUT_STREAM_GNUTLS, NULL);
  tls_stream->priv->conn = conn;
  g_object_add_weak_pointer (G_OBJECT (conn),
      (gpointer *) & tls_stream->priv->conn);

  return G_OUTPUT_STREAM (tls_stream);
}
