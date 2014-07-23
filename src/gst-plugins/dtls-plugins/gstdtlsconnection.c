/*
 * GStreamer
 *
 *  Copyright 2013 Collabora Ltd
 *   @author: Olivier Crete <olivier.crete@collabora.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 *
 */


#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gstdtlsconnection.h"
#include <src/ext/gio/kmsgtlsclientconnection.h>
#include <src/ext/gio/kmsgtlsserverconnection.h>

#define GST_DTLS_CONNECTION_LOCK(self)   g_mutex_lock (&self->lock)
#define GST_DTLS_CONNECTION_UNLOCK(self) g_mutex_unlock (&self->lock)

static void gst_dtls_connection_dispose (GObject * object);

G_DEFINE_TYPE (GstDtlsConnection, gst_dtls_connection, G_TYPE_OBJECT);

static GHashTable *connections = NULL;
static GMutex connections_lock;



static void
gst_dtls_connection_class_init (GstDtlsConnectionClass * klass)
{
  GObjectClass *object_class = G_OBJECT_CLASS (klass);

  object_class->dispose = gst_dtls_connection_dispose;
}

static void
gst_dtls_connection_init (GstDtlsConnection * self)
{
  self->base_stream = g_object_new (GST_TYPE_IO_STREAM, NULL);

  g_mutex_init (&self->lock);
}

static void
gst_dtls_connection_dispose (GObject * object)
{
  GstDtlsConnection *self = GST_DTLS_CONNECTION (object);

  g_object_unref (self->base_stream);
  if (self->conn)
    g_object_unref (self->conn);
  if (self->enc)
    gst_object_unref (self->enc);
  if (self->dec)
    gst_object_unref (self->dec);

  G_OBJECT_CLASS (gst_dtls_connection_parent_class)->dispose (object);
}


static void
free_weakref (gpointer data)
{
  GWeakRef *ref = data;

  g_weak_ref_clear (ref);
  g_slice_free (GWeakRef, ref);
}

GstDtlsConnection *
gst_dtls_connection_get_by_id (const gchar * id, gboolean is_client,
    GstDtlsBase * encdec)
{
  GstDtlsConnection *self = NULL;
  GWeakRef *ref;
  GError *error = NULL;

  g_mutex_lock (&connections_lock);

  if (connections == NULL)
    connections =
        g_hash_table_new_full (g_str_hash, g_str_equal, g_free, free_weakref);

  ref = g_hash_table_lookup (connections, id);

  if (ref) {
    self = g_weak_ref_get (ref);
    if (self == NULL)
      g_hash_table_remove (connections, id);
  }

  if (self) {
    if (self->is_client != is_client)
      goto wrong_direction;
  } else {
    self = g_object_new (GST_TYPE_DTLS_CONNECTION, NULL);
    self->is_client = is_client;
    if (is_client) {
      self->conn = (GTlsConnection *)
          kms_g_tls_client_connection_new (G_IO_STREAM (self->base_stream), NULL,
          &error);
    } else {
      self->conn = (GTlsConnection *)
          kms_g_tls_server_connection_new (G_IO_STREAM (self->base_stream), NULL,
          &error);
    }

    if (self->conn == NULL)
      goto cant_create_connection;

    ref = g_slice_new0 (GWeakRef);
    g_weak_ref_init (ref, self);
    g_hash_table_insert (connections, g_strdup (id), ref);
  }

  g_mutex_unlock (&connections_lock);

  if (GST_IS_DTLS_ENC (encdec)) {
    if (self->enc != NULL)
      goto already_exists;
    else
      self->enc = gst_object_ref (encdec);
  } else if (GST_IS_DTLS_DEC (encdec)) {
    if (self->dec != NULL)
      goto already_exists;
    else
      self->dec = gst_object_ref (encdec);
  } else {
    g_assert_not_reached ();
  }


  return self;

already_exists:

  GST_ERROR_OBJECT (encdec, "Channel ID is already used by another %s",
      GST_IS_DTLS_ENC (encdec) ? "encoder" : "decoder");
  g_object_unref (self);

  return NULL;

wrong_direction:

  g_mutex_unlock (&connections_lock);
  g_object_unref (self);

  GST_ERROR_OBJECT (encdec, "TLS Client/Server status does not match existing"
      " %s with channel-id %s",
      GST_IS_DTLS_ENC (encdec) ? "decoder" : "encoder", id);

  return NULL;

cant_create_connection:

  g_mutex_unlock (&connections_lock);
  g_object_unref (self);

  GST_ERROR_OBJECT (encdec, "Can't create connection: %s", error->message);
  g_clear_error (&error);

  return NULL;
}
