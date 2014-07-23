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

GST_DEBUG_CATEGORY_STATIC (dtls_dec_debug);
#define GST_CAT_DEFAULT (dtls_dec_debug)

G_DEFINE_TYPE (GstDtlsDec, gst_dtls_dec, GST_TYPE_DTLS_BASE);

static GstStaticPadTemplate gst_dtls_dec_sink_template =
GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS ("application/x-dtls"));

static GstStaticPadTemplate gst_dtls_dec_src_template =
GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS_ANY);

static void gst_dtls_dec_dispose (GObject * object);

static gboolean gst_dtls_dec_event (GstPad * pad, GstObject * parent,
    GstEvent * event);
static GstFlowReturn gst_dtls_dec_chain (GstDtlsBase * base,
    GstBuffer * buffer);
static gboolean gst_dtls_dec_activatemode (GstPad * pad, GstObject * parent,
    GstPadMode mode, gboolean active);

static GstStateChangeReturn gst_dtls_dec_change_state (GstElement * element,
    GstStateChange transition);

static void
gst_dtls_dec_class_init (GstDtlsDecClass * klass)
{
  GObjectClass *gobject_class = (GObjectClass *) klass;
  GstElementClass *gstelement_class = (GstElementClass *) klass;
  GstDtlsBaseClass *base_class = (GstDtlsBaseClass *) klass;

  GST_DEBUG_CATEGORY_INIT (dtls_dec_debug, "dtlsdec", 0, "DTLS Decrypter");

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&gst_dtls_dec_src_template));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&gst_dtls_dec_sink_template));
  gst_element_class_set_static_metadata (gstelement_class, "DTLS Decrypter",
      "Generic",
      "Encrypts packets using DTLS",
      "Olivier Crete <olivier.crete@collabora.com>");

  gobject_class->dispose = gst_dtls_dec_dispose;

  base_class->chain = gst_dtls_dec_chain;

  gstelement_class->change_state = gst_dtls_dec_change_state;
}

static void
gst_dtls_dec_init (GstDtlsDec * dec)
{
  GstDtlsBase *base = GST_DTLS_BASE (dec);

  g_assert (base->srcpad);

  dec->cancellable = g_cancellable_new ();

  gst_pad_set_event_function (base->sinkpad, gst_dtls_dec_event);
  gst_pad_set_activatemode_function (base->srcpad, gst_dtls_dec_activatemode);
}

static void
gst_dtls_dec_dispose (GObject * object)
{
  GstDtlsDec *dec = GST_DTLS_DEC (object);

  if (dec->cancellable)
    g_object_unref (dec->cancellable);

  G_OBJECT_CLASS (gst_dtls_dec_parent_class)->dispose (object);
}

static GstFlowReturn
gst_dtls_dec_chain (GstDtlsBase * base, GstBuffer * buffer)
{
  GstDtlsDec *self = GST_DTLS_DEC (base);
  GstFlowReturn ret;

  /* Ignore zero sized buffers */
  if (gst_buffer_get_size (buffer) == 0) {
    gst_buffer_unref (buffer);
    return GST_FLOW_OK;
  }

  ret = gst_input_stream_push_buffer (self->gst_istream, buffer);

  if (ret != GST_FLOW_OK)
    return ret;

  return g_atomic_int_get (&self->flow_ret);
}


static void
gst_dtls_dec_loop (gpointer user_data)
{
  GstDtlsDec *self = GST_DTLS_DEC (user_data);
  GstDtlsBase *base = GST_DTLS_BASE (user_data);
  gssize bytes_read;
  GstMapInfo map;
  GError *error = NULL;
  GstBuffer *outbuf;
  GstFlowReturn flow_ret;
  GstBuffer *lastbuf;

  if (gst_pad_check_reconfigure (base->srcpad)) {
    GstCaps *caps;

    gst_object_replace ((GstObject **) & self->allocator, NULL);
    gst_allocation_params_init (&self->alloc_params);

    caps = gst_pad_peer_query_caps (base->srcpad, NULL);

    if (caps) {
      if (gst_caps_get_size (caps) > 0)
        caps = gst_caps_fixate (caps);

      if (!gst_caps_is_fixed (caps)) {
        gst_caps_unref (caps);
        caps = NULL;
      }
    }

    if (caps) {
      GstQuery *alloc_query;
      gchar *stream_id;

      stream_id = gst_pad_get_stream_id (base->srcpad);

      if (stream_id == NULL) {
        stream_id = gst_pad_get_stream_id (base->sinkpad);

        if (stream_id == NULL) {
          stream_id = gst_pad_create_stream_id (base->srcpad,
                                                GST_ELEMENT (base), NULL);
        }

        gst_pad_push_event (base->srcpad, gst_event_new_stream_start(stream_id));
      }

      g_free (stream_id);

      /* FIXME(mparis): hack to avoid "Sticky event misordering, got 'segment' before 'caps'" */
//    if (!gst_pad_set_caps (base->srcpad, caps)) {
//      gst_caps_unref (caps);
//      goto not_negotiated;
//    }

      alloc_query = gst_query_new_allocation (caps, FALSE);
      if (!gst_pad_peer_query (base->srcpad, alloc_query)) {
        /* not a problem, just debug a little */
        GST_DEBUG_OBJECT (base, "peer ALLOCATION query failed");
      } else {
        if (gst_query_get_n_allocation_params (alloc_query) > 0) {
          gst_query_parse_nth_allocation_param (alloc_query, 0,
              &self->allocator, &self->alloc_params);
          if (self->allocator)
            gst_object_ref (self->allocator);
        }
      }
      gst_query_unref (alloc_query);
      gst_caps_unref (caps);
    }
  }

  if (!gst_input_stream_wait_for_buffer (self->gst_istream))
    goto flushing;

  outbuf = gst_buffer_new_allocate (self->allocator, 65536,
      &self->alloc_params);
  gst_buffer_map (outbuf, &map, GST_MAP_READWRITE);
  bytes_read = g_input_stream_read (self->tls_istream, map.data, map.size,
      self->cancellable, &error);
  gst_buffer_unmap (outbuf, &map);

  if (bytes_read > 0) {
    gst_buffer_set_size (outbuf, bytes_read);
  } else if (bytes_read == 0) {
    gst_buffer_unref (outbuf);
    goto end;
  } else if (bytes_read < 0) {
    gst_buffer_unref (outbuf);

    if (g_cancellable_is_cancelled (self->cancellable)) {
      goto flushing;
    } else if (g_error_matches (error, G_IO_ERROR, G_IO_ERROR_NO_SPACE)) {
      /* Let's loop again! */
      goto end;
    } else if (!g_error_matches (error, G_IO_ERROR, G_IO_ERROR_WOULD_BLOCK)) {
      GST_ELEMENT_ERROR (base, STREAM, DECRYPT,
          ("Error decrypting DTLS stream: %s", error->message),
          ("Error decrypting DTLS stream: %s", error->message));
      goto error;
    }

    goto end;
  }

  lastbuf = gst_input_stream_get_last_buffer (self->gst_istream);

  if (lastbuf) {
    gst_buffer_copy_into (outbuf, lastbuf, GST_BUFFER_COPY_METADATA, 0, -1);
    gst_buffer_unref (lastbuf);
  }

  flow_ret = gst_pad_push (base->srcpad, outbuf);
  g_atomic_int_set (&self->flow_ret, flow_ret);

  goto end;

// not_negotiated:
//   g_atomic_int_set (&self->flow_ret, GST_FLOW_NOT_NEGOTIATED);
//   gst_pad_pause_task (base->srcpad);
//   goto end;

flushing:
  g_atomic_int_set (&self->flow_ret, GST_FLOW_FLUSHING);
  gst_pad_pause_task (base->srcpad);
  goto end;

error:
  g_atomic_int_set (&self->flow_ret, GST_FLOW_ERROR);
  gst_pad_pause_task (base->srcpad);
  goto end;

end:
  g_clear_error (&error);
}

static void
gst_dtls_dec_set_flushing (GstDtlsDec * self, gboolean flushing)
{
  if (flushing) {
    g_atomic_int_set (&self->flow_ret, GST_FLOW_FLUSHING);
    g_cancellable_cancel (self->cancellable);
    gst_input_stream_set_flushing (self->gst_istream, TRUE);
  } else {
    g_atomic_int_set (&self->flow_ret, GST_FLOW_OK);
    gst_input_stream_set_flushing (self->gst_istream, FALSE);
  }
}

static gboolean
gst_dtls_dec_activatemode (GstPad * pad, GstObject * parent, GstPadMode mode,
    gboolean active)
{
  GstDtlsDec *self = GST_DTLS_DEC (parent);

  g_return_val_if_fail (mode == GST_PAD_MODE_PUSH, FALSE);

  if (active) {
    gst_dtls_dec_set_flushing (self, FALSE);
    return gst_pad_start_task (pad, gst_dtls_dec_loop, gst_object_ref (parent),
        gst_object_unref);
  } else {
    gboolean ret;
    gst_dtls_dec_set_flushing (self, TRUE);
    ret = gst_pad_stop_task (pad);
    g_cancellable_reset (self->cancellable);
    return ret;
  }
}

static gboolean
gst_dtls_dec_event (GstPad * pad, GstObject * parent, GstEvent * event)
{
  GstDtlsDec *self = GST_DTLS_DEC (parent);
  GstDtlsBase *base = GST_DTLS_BASE (parent);

  if (GST_EVENT_IS_SERIALIZED (event))
    gst_input_stream_wait_for_empty (self->gst_istream);

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_FLUSH_START:
      gst_dtls_dec_set_flushing (self, TRUE);
      break;
    case GST_EVENT_FLUSH_STOP:
      gst_dtls_dec_activatemode (base->srcpad, GST_OBJECT (self),
          GST_PAD_MODE_PUSH, TRUE);
      break;
    default:
      break;
  }

  return gst_pad_event_default (pad, parent, event);
}

static GstStateChangeReturn
gst_dtls_dec_change_state (GstElement * element, GstStateChange transition)
{
  GstDtlsDec *self = GST_DTLS_DEC (element);
  GstDtlsBase *base = GST_DTLS_BASE (element);
  GstStateChangeReturn ret;

  switch (transition) {
    case GST_STATE_CHANGE_READY_TO_NULL:
      self->tls_istream = NULL;
      self->gst_istream = NULL;
      g_clear_object (&self->cancellable);
      break;
    default:
      break;
  }

  ret = GST_ELEMENT_CLASS (gst_dtls_dec_parent_class)->change_state (element,
      transition);
  if (ret == GST_STATE_CHANGE_FAILURE)
    return ret;

  switch (transition) {
    case GST_STATE_CHANGE_NULL_TO_READY:
      self->gst_istream = GST_INPUT_STREAM (g_io_stream_get_input_stream
          (G_IO_STREAM (base->conn->base_stream)));
      self->tls_istream = g_io_stream_get_input_stream (G_IO_STREAM
          (base->conn->conn));
      break;
    default:
      break;
  }
  return ret;
}
