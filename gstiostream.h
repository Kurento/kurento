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


#ifndef __GST_IO_STREAM_H__
#define __GST_IO_STREAM_H__

#include <gst/gst.h>
#include <gio/gio.h>

G_BEGIN_DECLS
#define GST_TYPE_IO_STREAM            (gst_io_stream_get_type())
#define GST_IO_STREAM(obj)            (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_IO_STREAM,GstIOStream))
#define GST_IS_IO_STREAM(obj)         (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_IO_STREAM))
#define GST_IO_STREAM_CLASS(klass)    (G_TYPE_CHECK_CLASS_CAST((klass) ,GST_TYPE_IO_STREAM,GstIOStreamClass))
#define GST_IS_IO_STREAM_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE((klass) ,GST_TYPE_IO_STREAM))
#define GST_IO_STREAM_GET_CLASS(obj)  (G_TYPE_INSTANCE_GET_CLASS((obj) ,GST_TYPE_IO_STREAM,GstIOStreamClass))
typedef struct _GstIOStream GstIOStream;
typedef struct _GstIOStreamClass GstIOStreamClass;

typedef struct _GstInputStream GstInputStream;
typedef struct _GstInputStreamClass GstInputStreamClass;
typedef struct _GstOutputStream GstOutputStream;
typedef struct _GstOutputStreamClass GstOutputStreamClass;


/**
 * GstIOStream:
 *
 * The adder object structure.
 */
struct _GstIOStream
{
  /*< private >*/
  GIOStream parent;

  GInputStream *istream;
  GOutputStream *ostream;
};

struct _GstIOStreamClass
{
  GIOStreamClass parent_class;
};

GType gst_io_stream_get_type (void);

/* Input Stream */

#define GST_TYPE_INPUT_STREAM            (gst_input_stream_get_type())
#define GST_INPUT_STREAM(obj)            (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_INPUT_STREAM,GstInputStream))
#define GST_IS_INPUT_STREAM(obj)         (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_INPUT_STREAM))
#define GST_INPUT_STREAM_CLASS(klass)    (G_TYPE_CHECK_CLASS_CAST((klass) ,GST_TYPE_INPUT_STREAM,GstInputStreamClass))
#define GST_IS_INPUT_STREAM_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE((klass) ,GST_TYPE_INPUT_STREAM))
#define GST_INPUT_STREAM_GET_CLASS(obj)  (G_TYPE_INSTANCE_GET_CLASS((obj) ,GST_TYPE_INPUT_STREAM,GstInputStreamClass))


/**
 * GstInputStream:
 *
 * The adder object structure.
 */
struct _GstInputStream
{
  /*< private >*/
  GInputStream parent;

  GMutex lock;
  GCond  cond;
  GstBuffer *buffer;
  GstBuffer *last_buffer;
  gboolean flushing;
};

struct _GstInputStreamClass
{
  GInputStreamClass parent_class;
};

GType gst_input_stream_get_type (void);

GstFlowReturn gst_input_stream_push_buffer (GstInputStream * self,
    GstBuffer * buffer);
void gst_input_stream_set_flushing (GstInputStream * self, gboolean flushing);

GstBuffer *gst_input_stream_get_last_buffer (GstInputStream * self);
gboolean gst_input_stream_wait_for_buffer (GstInputStream * self);
void gst_input_stream_wait_for_empty (GstInputStream * self);

/* Output Stream */

#define GST_TYPE_OUTPUT_STREAM            (gst_output_stream_get_type())
#define GST_OUTPUT_STREAM(obj)            (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_OUTPUT_STREAM,GstOutputStream))
#define GST_IS_OUTPUT_STREAM(obj)         (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_OUTPUT_STREAM))
#define GST_OUTPUT_STREAM_CLASS(klass)    (G_TYPE_CHECK_CLASS_CAST((klass) ,GST_TYPE_OUTPUT_STREAM,GstOutputStreamClass))
#define GST_IS_OUTPUT_STREAM_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE((klass) ,GST_TYPE_OUTPUT_STREAM))
#define GST_OUTPUT_STREAM_GET_CLASS(obj)  (G_TYPE_INSTANCE_GET_CLASS((obj) ,GST_TYPE_OUTPUT_STREAM,GstOutputStreamClass))


typedef GstFlowReturn (*GstOutputStreamPushFunc) (gpointer user_data,
    GstBuffer * buffer);
/**
 * GstOutputStream:
 *
 * The adder object structure.
 */
struct _GstOutputStream
{
  /*< private >*/
  GOutputStream parent;

  GstOutputStreamPushFunc push_func;
  gpointer user_data;
};

struct _GstOutputStreamClass
{
  GOutputStreamClass parent_class;
};

GType gst_output_stream_get_type (void);


void gst_output_stream_set_push_function (GstOutputStream * self,
    GstOutputStreamPushFunc func, gpointer user_data);

GQuark gst_io_stream_flow_return_quark (void);

#define GST_IO_STREAM_FLOW_RETURN gst_io_stream_flow_return_quark ()

G_END_DECLS
#endif /* __GST_IO_STREAM_H__ */
