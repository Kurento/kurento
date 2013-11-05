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

#include "gstiostream.h"

G_DEFINE_QUARK (GstIOStreamFlowReturn, gst_io_stream_flow_return);

static void gst_io_stream_dispose (GObject * object);
static GInputStream *gst_io_stream_get_input_stream (GIOStream * stream);
static GOutputStream *gst_io_stream_get_output_stream (GIOStream * stream);
#if 0 /* Disabled */
static gboolean gst_io_stream_is_datagram (GIOStream * stream);
#endif

G_DEFINE_TYPE (GstIOStream, gst_io_stream, G_TYPE_IO_STREAM)

     static void gst_io_stream_class_init (GstIOStreamClass * klass)
{
  GObjectClass *object_class = G_OBJECT_CLASS (klass);
  GIOStreamClass *iostream_class = G_IO_STREAM_CLASS (klass);

  object_class->dispose = gst_io_stream_dispose;

  iostream_class->get_input_stream = gst_io_stream_get_input_stream;
  iostream_class->get_output_stream = gst_io_stream_get_output_stream;
#if 0 /* Disabled */
  iostream_class->is_datagram = gst_io_stream_is_datagram;
#endif
}

static void
gst_io_stream_init (GstIOStream * iostream)
{
  iostream->istream = g_object_new (GST_TYPE_INPUT_STREAM, NULL);
  iostream->ostream = g_object_new (GST_TYPE_OUTPUT_STREAM, NULL);
}

static GInputStream *
gst_io_stream_get_input_stream (GIOStream * stream)
{
  GstIOStream *self = GST_IO_STREAM (stream);

  return self->istream;
}

static GOutputStream *
gst_io_stream_get_output_stream (GIOStream * stream)
{
  GstIOStream *self = GST_IO_STREAM (stream);

  return self->ostream;
}

#if 0 /* Disabled */
static gboolean
gst_io_stream_is_datagram (GIOStream * stream)
{
  return TRUE;
}
#endif

static void
gst_io_stream_dispose (GObject * object)
{
  GstIOStream *self = GST_IO_STREAM (object);

  g_object_unref (self->istream);
  g_object_unref (self->ostream);

  G_OBJECT_CLASS (gst_io_stream_parent_class)->dispose (object);
}

static gssize
gst_input_stream_read_fn (GInputStream * stream,
    void *buffer, gsize count, GCancellable * cancellable, GError ** error);

static void gst_input_stream_pollable_iface_init (GPollableInputStreamInterface
    * iface);

G_DEFINE_TYPE_WITH_CODE (GstInputStream, gst_input_stream, G_TYPE_INPUT_STREAM,
    G_IMPLEMENT_INTERFACE (G_TYPE_POLLABLE_INPUT_STREAM,
        gst_input_stream_pollable_iface_init));

enum
{
  HAVE_BUFFER_INPUT_SIGNAL,
  INPUT_SIGNAL_COUNT
};

guint input_signals[INPUT_SIGNAL_COUNT];

static void
gst_input_stream_init (GstInputStream * self)
{
  g_cond_init (&self->cond);
  g_mutex_init (&self->lock);

  self->flushing = TRUE;
}

static void
gst_input_stream_dispose (GObject * object)
{
  GstInputStream *self = GST_INPUT_STREAM (object);

  gst_input_stream_set_flushing (self, TRUE);
  g_cond_clear (&self->cond);
  g_mutex_clear (&self->lock);

  G_OBJECT_CLASS (gst_input_stream_parent_class)->dispose (object);
}

static void
gst_input_stream_class_init (GstInputStreamClass * klass)
{
  GInputStreamClass *istream_class = G_INPUT_STREAM_CLASS (klass);
  GObjectClass *object_class = G_OBJECT_CLASS (klass);

  object_class->dispose = gst_input_stream_dispose;

  istream_class->read_fn = gst_input_stream_read_fn;

  input_signals[HAVE_BUFFER_INPUT_SIGNAL] = g_signal_new ("have-buffer",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST, 0, NULL, NULL, NULL, G_TYPE_NONE, 0);
}

static gboolean
gst_input_stream_pollable_is_readable (GPollableInputStream * pollable)
{
  GstInputStream *self = GST_INPUT_STREAM (pollable);
  gboolean readable;

  g_mutex_lock (&self->lock);
  readable = (self->buffer != NULL);
  g_mutex_unlock (&self->lock);

  return readable;
}

typedef struct
{
  GSource source;

  GstInputStream *stream;
  GMutex lock;
  GMainContext *context;
} GstInputStreamSource;

static void
source_have_buffer (GstInputStream * istream, gpointer user_data)
{
  GSource *source = user_data;
  GstInputStreamSource *input_source = (GstInputStreamSource *) source;

  g_mutex_lock (&input_source->lock);
  if (input_source->context)
    g_main_context_wakeup (input_source->context);
  g_mutex_unlock (&input_source->lock);

}

static gboolean
input_source_prepare (GSource * source, gint * timeout)
{
  GstInputStreamSource *input_source = (GstInputStreamSource *) source;

  g_mutex_lock (&input_source->lock);
  input_source->context = g_main_context_ref (g_source_get_context (source));
  g_mutex_unlock (&input_source->lock);

  *timeout = -1;
  return
      gst_input_stream_pollable_is_readable (G_POLLABLE_INPUT_STREAM
      (input_source->stream));
}

static gboolean
input_source_check (GSource * source)
{
  GstInputStreamSource *input_source = (GstInputStreamSource *) source;

  g_mutex_lock (&input_source->lock);
  g_main_context_unref (input_source->context);
  input_source->context = NULL;
  g_mutex_unlock (&input_source->lock);

  return
      gst_input_stream_pollable_is_readable (G_POLLABLE_INPUT_STREAM
      (input_source->stream));
}

static gboolean
input_source_dispatch (GSource * source, GSourceFunc callback,
    gpointer user_data)
{
  return TRUE;
}

static void
input_source_finalize (GSource * source)
{
  GstInputStreamSource *input_source = (GstInputStreamSource *) source;

  g_signal_handlers_disconnect_by_func (input_source->stream,
      G_CALLBACK (source_have_buffer), input_source);
  g_mutex_clear (&input_source->lock);
  g_object_unref (input_source->stream);
}

static GSourceFuncs input_source_funcs = {
  input_source_prepare,
  input_source_check,
  input_source_dispatch,
  input_source_finalize
};

static GSource *
gst_input_stream_pollable_create_source (GPollableInputStream * pollable,
    GCancellable * cancellable)
{
  GstInputStream *input_stream = GST_INPUT_STREAM (pollable);
  GSource *pollable_source, *gst_source = NULL;
  GstInputStreamSource *input_source;

  gst_source = g_source_new (&input_source_funcs,
      sizeof (GstInputStreamSource));
  input_source = (GstInputStreamSource *) gst_source;
  g_source_set_name (gst_source, "GstInputSource");
  g_mutex_init (&input_source->lock);


  input_source->stream = g_object_ref (input_stream);
  g_signal_connect (input_source->stream, "have-buffer",
      G_CALLBACK (source_have_buffer), input_source);

  pollable_source = g_pollable_source_new_full (G_OBJECT (input_stream), NULL,
      cancellable);
  g_source_add_child_source (pollable_source, gst_source);
  return pollable_source;
}

static gssize
gst_input_stream_read_locked (GstInputStream * self, void *buffer, gsize size,
    GError ** error)
{
  gsize bufsize;
  gssize ret;

  bufsize = gst_buffer_get_size (self->buffer);
  if (bufsize > size) {
#if 0 /* Disabled */
    g_set_error (error, G_IO_ERROR, G_IO_ERROR_MESSAGE_TOO_LARGE,
        "buffer is too small");
#endif
    g_set_error (error, G_IO_ERROR, G_IO_ERROR_INVALID_DATA,
        "buffer is too small");
    return -1;
  }

  ret = gst_buffer_extract (self->buffer, 0, buffer, bufsize);
  g_assert (ret == bufsize);
  if (self->last_buffer)
    gst_buffer_unref (self->last_buffer);
  self->last_buffer = self->buffer;
  self->buffer = NULL;

  g_cond_broadcast (&self->cond);

  return ret;
}

static gssize
gst_input_stream_pollable_read_nonblocking (GPollableInputStream * pollable,
    void *buffer, gsize size, GError ** error)
{
  GstInputStream *self = GST_INPUT_STREAM (pollable);
  gssize ret;

  g_mutex_lock (&self->lock);

  if (self->buffer == NULL) {
    g_mutex_unlock (&self->lock);
    g_set_error (error, G_IO_ERROR, G_IO_ERROR_WOULD_BLOCK, "No buffers");
    return -1;
  }

  ret = gst_input_stream_read_locked (self, buffer, size, error);
  g_mutex_unlock (&self->lock);

  return ret;
}

static void
gst_input_stream_pollable_iface_init (GPollableInputStreamInterface * iface)
{
  iface->is_readable = gst_input_stream_pollable_is_readable;
  iface->create_source = gst_input_stream_pollable_create_source;
  iface->read_nonblocking = gst_input_stream_pollable_read_nonblocking;
}

static void
cancellable_cancelled (GCancellable * cancellable, gpointer user_data)
{
  GstInputStream *self = GST_INPUT_STREAM (user_data);

  g_cond_broadcast (&self->cond);
}

static gssize
gst_input_stream_read_fn (GInputStream * stream,
    void *buffer, gsize count, GCancellable * cancellable, GError ** error)
{
  GstInputStream *self = GST_INPUT_STREAM (stream);
  gssize ret;
  gulong id = 0;

  if (cancellable)
    id = g_cancellable_connect (cancellable,
        G_CALLBACK (cancellable_cancelled), g_object_ref (stream),
        g_object_unref);

  g_mutex_lock (&self->lock);

  while (self->buffer == NULL && !self->flushing &&
      !g_cancellable_is_cancelled (cancellable))
    g_cond_wait (&self->cond, &self->lock);


  g_cancellable_disconnect (cancellable, id);


  if (self->buffer == NULL || g_cancellable_is_cancelled (cancellable)) {
    g_mutex_unlock (&self->lock);

    if (g_cancellable_is_cancelled (cancellable))
      g_set_error (error, G_IO_ERROR, G_IO_ERROR_CANCELLED, "Cancelled");
    else
      g_set_error (error, GST_IO_STREAM_FLOW_RETURN, GST_FLOW_FLUSHING,
          "Flushing");

    return -1;
  }

  ret = gst_input_stream_read_locked (self, buffer, count, error);
  g_mutex_unlock (&self->lock);

  return ret;
}

GstFlowReturn
gst_input_stream_push_buffer (GstInputStream * self, GstBuffer * buffer)
{
  gboolean flushing;

  g_mutex_lock (&self->lock);
  while (self->buffer != NULL && !self->flushing)
    g_cond_wait (&self->cond, &self->lock);

  flushing = self->flushing;
  if (self->flushing)
    gst_buffer_unref (buffer);
  else if (self->buffer == NULL)
    self->buffer = buffer;
  else
    g_assert_not_reached ();
  g_cond_broadcast (&self->cond);
  g_mutex_unlock (&self->lock);

  if (flushing) {
    return GST_FLOW_FLUSHING;
  } else {
    g_signal_emit (self, input_signals[HAVE_BUFFER_INPUT_SIGNAL], 0);
    return GST_FLOW_OK;
  }
}

void
gst_input_stream_set_flushing (GstInputStream * self, gboolean flushing)
{
  g_mutex_lock (&self->lock);
  gst_buffer_replace (&self->buffer, NULL);
  gst_buffer_replace (&self->last_buffer, NULL);
  self->flushing = flushing;
  g_cond_broadcast (&self->cond);
  g_mutex_unlock (&self->lock);
}

gboolean
gst_input_stream_wait_for_buffer (GstInputStream * self)
{
  gboolean ret;

  g_mutex_lock (&self->lock);
  while (self->buffer == NULL && !self->flushing)
    g_cond_wait (&self->cond, &self->lock);

  ret = (self->buffer != NULL);
  g_mutex_unlock (&self->lock);

  return ret;
}

GstBuffer *
gst_input_stream_get_last_buffer (GstInputStream * self)
{
  GstBuffer *buffer;

  g_mutex_lock (&self->lock);
  buffer = self->last_buffer;
  self->last_buffer = NULL;
  g_cond_broadcast (&self->cond);
  g_mutex_unlock (&self->lock);

  return buffer;
}

void
gst_input_stream_wait_for_empty (GstInputStream * self)
{
  g_mutex_lock (&self->lock);
  while ((self->buffer != NULL || self->last_buffer != NULL) && !self->flushing)
    g_cond_wait (&self->cond, &self->lock);
  g_mutex_unlock (&self->lock);
}

static gssize
gst_output_stream_write_fn (GOutputStream * stream,
    const void *buffer,
    gsize count, GCancellable * cancellable, GError ** error);

static gssize
gst_output_stream_pollable_write_nonblocking (GPollableOutputStream * stream,
    const void *buffer, gsize count, GError ** error);

static void
gst_output_stream_pollable_iface_init (GPollableOutputStreamInterface * iface);

G_DEFINE_TYPE_WITH_CODE (GstOutputStream, gst_output_stream,
    G_TYPE_OUTPUT_STREAM,
    G_IMPLEMENT_INTERFACE (G_TYPE_POLLABLE_OUTPUT_STREAM,
        gst_output_stream_pollable_iface_init));

static void
gst_output_stream_init (GstOutputStream * self)
{
}

static void
gst_output_stream_dispose (GObject * object)
{
  G_OBJECT_CLASS (gst_output_stream_parent_class)->dispose (object);
}

static void
gst_output_stream_class_init (GstOutputStreamClass * klass)
{
  GObjectClass *object_class = G_OBJECT_CLASS (klass);
  GOutputStreamClass *ostream_class = G_OUTPUT_STREAM_CLASS (klass);

  object_class->dispose = gst_output_stream_dispose;

  ostream_class->write_fn = gst_output_stream_write_fn;
}

static gssize
gst_output_stream_write_fn (GOutputStream * stream,
    const void *buffer,
    gsize count, GCancellable * cancellable, GError ** error)
{
  return
      gst_output_stream_pollable_write_nonblocking (G_POLLABLE_OUTPUT_STREAM
      (stream), buffer, count, error);
}

static gboolean
gst_output_stream_pollable_is_writable (GPollableOutputStream * stream)
{
  return TRUE;
}

static GSource *
gst_output_stream_pollable_create_source (GPollableOutputStream * stream,
    GCancellable * cancellable)
{
  GstOutputStream *output_stream = GST_OUTPUT_STREAM (stream);
  GSource *pollable_source, *gst_source;

  gst_source = g_idle_source_new ();
  g_source_set_priority (gst_source, G_PRIORITY_DEFAULT);

  pollable_source = g_pollable_source_new_full (G_OBJECT (output_stream),
      gst_source, cancellable);

  return pollable_source;
}

static gssize
gst_output_stream_pollable_write_nonblocking (GPollableOutputStream * stream,
    const void *buffer, gsize count, GError ** error)
{
  GstOutputStream *self = GST_OUTPUT_STREAM (stream);
  GstBuffer *gstbuffer;
  GstFlowReturn ret;

  if (self->push_func == NULL) {
    g_set_error (error, GST_IO_STREAM_FLOW_RETURN, GST_FLOW_FLUSHING,
        "Flushing");
    return -1;
  }

  gstbuffer = gst_buffer_new_allocate (NULL, count, NULL);
  gst_buffer_fill (gstbuffer, 0, buffer, count);
  ret = self->push_func (self->user_data, gstbuffer);

  if (ret == GST_FLOW_OK) {
    return count;
  } else {
    g_set_error (error, GST_IO_STREAM_FLOW_RETURN, ret,
        "Streaming error: %s", gst_flow_get_name (ret));
    return -1;
  }
}

static void
gst_output_stream_pollable_iface_init (GPollableOutputStreamInterface * iface)
{
  iface->is_writable = gst_output_stream_pollable_is_writable;
  iface->create_source = gst_output_stream_pollable_create_source;
  iface->write_nonblocking = gst_output_stream_pollable_write_nonblocking;
}

void
gst_output_stream_set_push_function (GstOutputStream * self,
    GstOutputStreamPushFunc func, gpointer user_data)
{
  g_return_if_fail (GST_IS_OUTPUT_STREAM (self));

  self->push_func = func;
  self->user_data = user_data;
}
