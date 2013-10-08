/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include "kmsagnosticbin2.h"
#include "kmsagnosticcaps.h"
#include "kmsutils.h"

#define PLUGIN_NAME "agnosticbin2"

static GstStaticCaps static_audio_caps =
GST_STATIC_CAPS (KMS_AGNOSTIC_AUDIO_CAPS);
static GstStaticCaps static_video_caps =
GST_STATIC_CAPS (KMS_AGNOSTIC_VIDEO_CAPS);
static GstStaticCaps static_raw_audio_caps =
GST_STATIC_CAPS (KMS_AGNOSTIC_RAW_AUDIO_CAPS);
static GstStaticCaps static_raw_video_caps =
GST_STATIC_CAPS (KMS_AGNOSTIC_RAW_VIDEO_CAPS);

#define GST_CAT_DEFAULT kms_agnostic_bin2_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_agnostic_bin2_parent_class parent_class
G_DEFINE_TYPE (KmsAgnosticBin2, kms_agnostic_bin2, GST_TYPE_BIN);

#define KMS_AGNOSTIC_BIN2_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (              \
    (obj),                                   \
    KMS_TYPE_AGNOSTIC_BIN2,                  \
    KmsAgnosticBin2Private                   \
  )                                          \
)

#define KMS_AGNOSTIC_BIN2_GET_LOCK(obj) (       \
  &KMS_AGNOSTIC_BIN2 (obj)->priv->thread_mutex  \
)

#define KMS_AGNOSTIC_BIN2_GET_COND(obj) (       \
  &KMS_AGNOSTIC_BIN2 (obj)->priv->thread_cond   \
)

#define KMS_AGNOSTIC_BIN2_LOCK(obj) (                   \
  g_mutex_lock (KMS_AGNOSTIC_BIN2_GET_LOCK (obj))       \
)

#define KMS_AGNOSTIC_BIN2_UNLOCK(obj) (                 \
  g_mutex_unlock (KMS_AGNOSTIC_BIN2_GET_LOCK (obj))     \
)

#define KMS_AGNOSTIC_BIN2_WAIT(obj) (                   \
  g_cond_wait (KMS_AGNOSTIC_BIN2_GET_COND (obj),        \
    KMS_AGNOSTIC_BIN2_GET_LOCK (obj))                   \
)

#define KMS_AGNOSTIC_BIN2_SIGNAL(obj) (                 \
  g_cond_signal (KMS_AGNOSTIC_BIN2_GET_COND (obj))      \
)

struct _KmsAgnosticBin2Private
{
  GHashTable *tees;
  GQueue *pads_to_link;

  gboolean finish_thread;
  GThread *thread;
  GMutex thread_mutex;
  GCond thread_cond;

  GstPad *sink;
  guint pad_count;
};

/* the capabilities of the inputs and outputs. */
static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS (KMS_AGNOSTIC_CAPS_CAPS)
    );

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE ("src_%u",
    GST_PAD_SRC,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (KMS_AGNOSTIC_CAPS_CAPS)
    );

static gboolean
is_raw_caps (GstCaps * caps)
{
  gboolean ret;
  GstCaps *raw_caps = gst_caps_from_string (KMS_AGNOSTIC_RAW_CAPS);

  ret = gst_caps_can_intersect (caps, raw_caps);

  gst_caps_unref (raw_caps);
  return ret;
}

static GstPadProbeReturn
tee_src_probe (GstPad * pad, GstPadProbeInfo * info, gpointer user_data)
{
  if (~GST_PAD_PROBE_INFO_TYPE (info) & GST_PAD_PROBE_TYPE_BLOCK)
    return GST_PAD_PROBE_OK;

  if (GST_PAD_PROBE_INFO_TYPE (info) & GST_PAD_PROBE_TYPE_EVENT_BOTH) {
    GstEvent *event = gst_pad_probe_info_get_event (info);

    if (GST_EVENT_TYPE (event) == GST_EVENT_RECONFIGURE) {
      // We drop reconfigure events to avoid not negotiated error caused by
      // continious negotiations
      GST_DEBUG_OBJECT (pad, "Dropping reconfigure event");
      return GST_PAD_PROBE_DROP;
    }
  }

  return GST_PAD_PROBE_PASS;
}

static void
send_force_key_unit_event (GstPad * pad)
{
  GstStructure *s;
  GstEvent *force_key_unit_event;

  s = gst_structure_new ("GstForceKeyUnit",
      "all-headers", G_TYPE_BOOLEAN, TRUE, NULL);
  force_key_unit_event = gst_event_new_custom (GST_EVENT_CUSTOM_UPSTREAM, s);

  if (GST_PAD_DIRECTION (pad) == GST_PAD_SRC) {
    gst_pad_send_event (pad, force_key_unit_event);
  } else {
    gst_pad_push_event (pad, force_key_unit_event);
  }
}

static void
link_queue_to_tee (GstElement * tee, GstElement * queue)
{
  GstPad *tee_src = gst_element_get_request_pad (tee, "src_%u");
  GstPad *queue_sink = gst_element_get_static_pad (queue, "sink");
  GstPadLinkReturn ret;

  gst_pad_add_probe (tee_src,
      GST_PAD_PROBE_TYPE_BLOCK | GST_PAD_PROBE_TYPE_DATA_BOTH, tee_src_probe,
      NULL, NULL);
  ret = gst_pad_link (tee_src, queue_sink);

  if (G_UNLIKELY (GST_PAD_LINK_FAILED (ret)))
    GST_ERROR ("Linking %" GST_PTR_FORMAT " with %" GST_PTR_FORMAT " result %d",
        tee_src, queue_sink, ret);

  send_force_key_unit_event (tee_src);
  g_object_unref (queue_sink);
  g_object_unref (tee_src);
}

static GstElement *
create_convert_for_caps (GstCaps * caps)
{
  GstCaps *audio_caps = gst_static_caps_get (&static_audio_caps);
  GstElement *convert;

  if (gst_caps_can_intersect (caps, audio_caps))
    convert = gst_element_factory_make ("audioconvert", NULL);
  else
    convert = gst_element_factory_make ("videoconvert", NULL);

  gst_caps_unref (audio_caps);

  return convert;
}

static void
kms_agnostic_bin2_link_to_tee (KmsAgnosticBin2 * self, GstPad * pad,
    GstElement * tee, GstCaps * caps)
{
  GstElement *queue = gst_element_factory_make ("queue", NULL);
  GstPad *target;

  gst_bin_add (GST_BIN (self), queue);
  gst_element_sync_state_with_parent (queue);

  if (!gst_caps_is_any (caps) && is_raw_caps (caps)) {
    GstElement *convert = create_convert_for_caps (caps);

    gst_bin_add (GST_BIN (self), convert);
    gst_element_sync_state_with_parent (convert);
    gst_element_link (queue, convert);

    target = gst_element_get_static_pad (convert, "src");
  } else {
    target = gst_element_get_static_pad (queue, "src");
  }

  gst_ghost_pad_set_target (GST_GHOST_PAD (pad), target);
  g_object_unref (target);

  link_queue_to_tee (tee, queue);
}

static GstElement *
kms_agnostic_bin2_find_tee_for_caps (KmsAgnosticBin2 * self, GstCaps * caps)
{
  GList *tees, *l;
  GstElement *tee = NULL;

  tees = g_hash_table_get_values (self->priv->tees);
  for (l = tees; l != NULL && tee == NULL; l = l->next) {
    GstPad *tee_sink = gst_element_get_static_pad (l->data, "sink");
    GstCaps *current_caps = gst_pad_get_current_caps (tee_sink);

    GST_DEBUG_OBJECT (l->data, "Current caps are: %" GST_PTR_FORMAT,
        current_caps);

    if (current_caps != NULL) {
      if (gst_caps_can_intersect (caps, current_caps))
        tee = l->data;
      gst_caps_unref (current_caps);
    }

    g_object_unref (tee_sink);
  }

  return tee;
}

static GstCaps *
kms_agnostic_bin2_get_raw_caps (GstCaps * caps)
{
  GstCaps *audio_caps, *video_caps, *raw_caps = NULL;

  audio_caps = gst_static_caps_get (&static_audio_caps);
  video_caps = gst_static_caps_get (&static_video_caps);

  if (gst_caps_can_intersect (caps, audio_caps))
    raw_caps = gst_static_caps_get (&static_raw_audio_caps);
  else if (gst_caps_can_intersect (caps, video_caps))
    raw_caps = gst_static_caps_get (&static_raw_video_caps);

  gst_caps_unref (audio_caps);
  gst_caps_unref (video_caps);

  return raw_caps;
}

static GstElement *
kms_agnostic_bin2_get_or_create_raw_tee (KmsAgnosticBin2 * self, GstCaps * caps)
{
  GstCaps *raw_caps = kms_agnostic_bin2_get_raw_caps (caps);

  if (raw_caps != NULL) {
    GstElement *raw_tee;

    GST_DEBUG ("Raw caps: %" GST_PTR_FORMAT, raw_caps);
    raw_tee = kms_agnostic_bin2_find_tee_for_caps (self, raw_caps);

    if (raw_tee == NULL) {
      // TODO: Create and link raw tee
    }

    gst_caps_unref (raw_caps);

    return raw_tee;
  } else {
    GST_ELEMENT_WARNING (self, CORE, NEGOTIATION,
        ("Formats are not compatible"), (""));
    return NULL;
  }
}

static void
configure_encoder (GstElement * encoder, const gchar * factory_name)
{
  GST_DEBUG ("Configure encoder: %s", factory_name);
  if (g_strcmp0 ("vp8enc", factory_name) == 0) {
    g_object_set (G_OBJECT (encoder), "deadline", G_GINT64_CONSTANT (1),
        "threads", 1, "cpu-used", 16, NULL);
  } else if (g_strcmp0 ("x264enc", factory_name) == 0) {
    g_object_set (G_OBJECT (encoder), "speed-preset", 1 /* ultrafast */ ,
        "tune", 4 /* zerolatency */ , "threads", (guint) 1,
        NULL);
  }
}

static GstElement *
create_encoder_for_caps (GstCaps * caps)
{
  GList *encoder_list, *filtered_list, *l;
  GstElementFactory *encoder_factory = NULL;
  GstElement *encoder = NULL;

  encoder_list =
      gst_element_factory_list_get_elements (GST_ELEMENT_FACTORY_TYPE_ENCODER,
      GST_RANK_NONE);
  filtered_list =
      gst_element_factory_list_filter (encoder_list, caps, GST_PAD_SRC, FALSE);

  for (l = filtered_list; l != NULL && encoder_factory == NULL; l = l->next) {
    encoder_factory = GST_ELEMENT_FACTORY (l->data);
    if (gst_element_factory_get_num_pad_templates (encoder_factory) != 2)
      encoder_factory = NULL;
  }

  if (encoder_factory != NULL) {
    encoder = gst_element_factory_create (encoder_factory, NULL);
    configure_encoder (encoder, GST_OBJECT_NAME (encoder_factory));
  }

  gst_plugin_feature_list_free (filtered_list);
  gst_plugin_feature_list_free (encoder_list);

  return encoder;
}

static GstElement *
kms_agnostic_bin2_create_tee_for_caps (KmsAgnosticBin2 * self, GstCaps * caps)
{
  GstElement *tee;
  GstElement *raw_tee = kms_agnostic_bin2_get_or_create_raw_tee (self, caps);
  GstElement *encoder, *queue, *convert, *fakequeue, *fakesink;

  if (raw_tee == NULL)
    return NULL;

  encoder = create_encoder_for_caps (caps);

  if (encoder == NULL)
    return NULL;

  queue = gst_element_factory_make ("queue", NULL);
  convert = create_convert_for_caps (caps);
  tee = gst_element_factory_make ("tee", NULL);
  fakequeue = gst_element_factory_make ("queue", NULL);
  fakesink = gst_element_factory_make ("fakesink", NULL);

  gst_bin_add_many (GST_BIN (self), queue, convert, encoder, tee, fakequeue,
      fakesink, NULL);

  gst_element_sync_state_with_parent (queue);
  gst_element_sync_state_with_parent (convert);
  gst_element_sync_state_with_parent (encoder);
  gst_element_sync_state_with_parent (tee);
  gst_element_sync_state_with_parent (fakequeue);
  gst_element_sync_state_with_parent (fakesink);

  gst_element_link_many (queue, convert, encoder, tee, fakequeue, fakesink,
      NULL);
  link_queue_to_tee (raw_tee, queue);

  g_hash_table_insert (self->priv->tees, GST_OBJECT_NAME (tee),
      g_object_ref (tee));

  return tee;
}

/**
 * Link a pad internally
 *
 * @self: The #KmsAgnosticBin2 owner of the pad
 * @pad: (transfer full): The pad to be linked
 * @peer: (transfer full): The peer pad
 */
static void
kms_agnostic_bin2_link_pad (KmsAgnosticBin2 * self, GstPad * pad, GstPad * peer)
{
  GstCaps *caps;
  GstElement *tee;

  GST_DEBUG_OBJECT (self, "Linking: %" GST_PTR_FORMAT, pad);

  caps = gst_pad_query_caps (peer, NULL);

  if (caps == NULL)
    goto end;

  GST_DEBUG ("Query caps are: %" GST_PTR_FORMAT, caps);
  tee = kms_agnostic_bin2_find_tee_for_caps (self, caps);

  if (tee == NULL) {
    tee = kms_agnostic_bin2_create_tee_for_caps (self, caps);
    GST_DEBUG_OBJECT (self, "Created tee: %" GST_PTR_FORMAT, tee);
  }

  if (tee != NULL)
    kms_agnostic_bin2_link_to_tee (self, pad, tee, caps);

  gst_caps_unref (caps);

end:
  g_object_unref (pad);
  g_object_unref (peer);
}

/**
 * Unlink a pad internally
 *
 * @self: The #KmsAgnosticBin2 owner of the pad
 * @pad: (transfer full): The pad to be unlinked
 */
static void
kms_agnostic_bin2_unlink_pad (KmsAgnosticBin2 * self, GstPad * pad)
{
  GST_DEBUG_OBJECT (self, "Unlinking: %" GST_PTR_FORMAT, pad);

  // TODO: Implement this
  g_object_unref (pad);
}

/**
 * Process a pad for connecting or disconnecting, it should be always called
 * from the connect_thread.
 *
 * @self: The #KmsAgnosticBin2 owner of the pad
 * @pad: (transfer full): The pad to be processed
 */
static void
kms_agnostic_bin2_process_pad (KmsAgnosticBin2 * self, GstPad * pad)
{
  GstPad *peer = NULL;

  GST_DEBUG_OBJECT (self, "Processing pad: %" GST_PTR_FORMAT, pad);

  if (pad == NULL)
    return;

  peer = gst_pad_get_peer (pad);

  if (peer == NULL)
    kms_agnostic_bin2_unlink_pad (self, pad);
  else
    kms_agnostic_bin2_link_pad (self, pad, peer);

}

static gpointer
kms_agnostic_bin2_connect_thread (gpointer data)
{
  KmsAgnosticBin2 *self = KMS_AGNOSTIC_BIN2 (data);

  GST_DEBUG_OBJECT (self, "Thread start");

  while (1) {
    KMS_AGNOSTIC_BIN2_LOCK (self);
    while (!self->priv->finish_thread &&
        g_queue_is_empty (self->priv->pads_to_link)) {
      GST_DEBUG_OBJECT (self, "Waiting for pads to link");
      KMS_AGNOSTIC_BIN2_WAIT (self);
      GST_DEBUG_OBJECT (self, "Waked up");
    }

    GST_DEBUG_OBJECT (self, "Checking finish");

    if (self->priv->finish_thread) {
      KMS_AGNOSTIC_BIN2_UNLOCK (self);
      break;
    }

    kms_agnostic_bin2_process_pad (self,
        g_queue_pop_head (self->priv->pads_to_link));

    KMS_AGNOSTIC_BIN2_UNLOCK (self);
  }

  GST_DEBUG_OBJECT (self, "Thread finished");

  return NULL;
}

static void
iterate_src_pads (KmsAgnosticBin2 * self)
{
  GstIterator *it = gst_element_iterate_src_pads (GST_ELEMENT (self));
  gboolean done = FALSE;
  GstPad *pad;
  GValue item = G_VALUE_INIT;

  while (!done) {
    switch (gst_iterator_next (it, &item)) {
      case GST_ITERATOR_OK:
        pad = g_value_get_object (&item);
        KMS_AGNOSTIC_BIN2_LOCK (self);
        if (g_queue_index (self->priv->pads_to_link, pad) == -1) {
          GST_DEBUG_OBJECT (pad, "Adding pad to queue");
          g_queue_push_tail (self->priv->pads_to_link, g_object_ref (pad));
          KMS_AGNOSTIC_BIN2_SIGNAL (self);
        }
        KMS_AGNOSTIC_BIN2_UNLOCK (self);
        g_value_reset (&item);
        break;
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
      case GST_ITERATOR_DONE:
        done = TRUE;
        break;
    }
  }
}

static GstPadProbeReturn
kms_agnostic_bin2_sink_block_probe (GstPad * pad, GstPadProbeInfo * info,
    gpointer user_data)
{
  if (~GST_PAD_PROBE_INFO_TYPE (info) & GST_PAD_PROBE_TYPE_BLOCK)
    return GST_PAD_PROBE_OK;

  if (GST_PAD_PROBE_INFO_TYPE (info) & GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM) {
    GstEvent *event = gst_pad_probe_info_get_event (info);

    if (GST_EVENT_TYPE (event) == GST_EVENT_SEGMENT) {
      KmsAgnosticBin2 *self = KMS_AGNOSTIC_BIN2 (user_data);

      GST_INFO_OBJECT (self, "New segment, we can now connect sink pads");
      iterate_src_pads (self);
    }
  }

  return GST_PAD_PROBE_PASS;
}

static GstPadProbeReturn
kms_agnostic_bin2_src_reconfigure_probe (GstPad * pad, GstPadProbeInfo * info,
    gpointer user_data)
{
  GstEvent *event;

  if (~GST_PAD_PROBE_INFO_TYPE (info) & GST_PAD_PROBE_TYPE_BLOCK)
    return GST_PAD_PROBE_OK;

  event = gst_pad_probe_info_get_event (info);

  if (GST_EVENT_TYPE (event) == GST_EVENT_RECONFIGURE) {
    KmsAgnosticBin2 *self = user_data;

    GST_INFO_OBJECT (pad, "Received reconfigure event");
    KMS_AGNOSTIC_BIN2_LOCK (self);
    if (g_queue_index (self->priv->pads_to_link, pad) == -1) {
      GST_DEBUG_OBJECT (pad, "Adding pad to queue");
      g_queue_push_tail (self->priv->pads_to_link, g_object_ref (pad));
      KMS_AGNOSTIC_BIN2_SIGNAL (self);
    }
    KMS_AGNOSTIC_BIN2_UNLOCK (self);
    return GST_PAD_PROBE_DROP;
  }

  return GST_PAD_PROBE_PASS;
}

static GstPad *
kms_agnostic_bin2_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps)
{
  GstPad *pad;
  gchar *pad_name;
  KmsAgnosticBin2 *agnosticbin = KMS_AGNOSTIC_BIN2 (element);

  GST_OBJECT_LOCK (agnosticbin);
  pad_name = g_strdup_printf ("src_%d", agnosticbin->priv->pad_count++);
  GST_OBJECT_UNLOCK (agnosticbin);

  pad = gst_ghost_pad_new_no_target_from_template (pad_name, templ);
  g_free (pad_name);

  gst_pad_add_probe (pad, GST_PAD_PROBE_TYPE_BLOCK |
      GST_PAD_PROBE_TYPE_EVENT_UPSTREAM,
      kms_agnostic_bin2_src_reconfigure_probe, element, NULL);

  GST_OBJECT_LOCK (agnosticbin);

  if (GST_STATE (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, TRUE);

  GST_OBJECT_UNLOCK (agnosticbin);

  if (gst_element_add_pad (element, pad))
    return pad;

  g_object_unref (pad);

  return NULL;
}

static void
kms_agnostic_bin2_release_pad (GstElement * element, GstPad * pad)
{
  GST_OBJECT_LOCK (element);
  if (GST_STATE (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, FALSE);
  GST_OBJECT_UNLOCK (element);
  gst_element_remove_pad (element, pad);
}

static void
kms_agnostic_bin2_dispose (GObject * object)
{
  /* chain up */
  G_OBJECT_CLASS (kms_agnostic_bin2_parent_class)->dispose (object);
}

static void
kms_agnostic_bin2_finalize (GObject * object)
{
  KmsAgnosticBin2 *self = KMS_AGNOSTIC_BIN2 (object);

  KMS_AGNOSTIC_BIN2_LOCK (self);
  self->priv->finish_thread = TRUE;
  KMS_AGNOSTIC_BIN2_SIGNAL (self);
  KMS_AGNOSTIC_BIN2_UNLOCK (self);

  g_thread_join (self->priv->thread);
  g_thread_unref (self->priv->thread);

  g_cond_clear (&self->priv->thread_cond);
  g_mutex_clear (&self->priv->thread_mutex);

  g_queue_free_full (self->priv->pads_to_link, g_object_unref);
  g_hash_table_unref (self->priv->tees);

  /* chain up */
  G_OBJECT_CLASS (kms_agnostic_bin2_parent_class)->finalize (object);
}

static void
kms_agnostic_bin2_class_init (KmsAgnosticBin2Class * klass)
{
  GObjectClass *gobject_class;
  GstElementClass *gstelement_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gstelement_class = GST_ELEMENT_CLASS (klass);

  gobject_class->dispose = kms_agnostic_bin2_dispose;
  gobject_class->finalize = kms_agnostic_bin2_finalize;

  gst_element_class_set_details_simple (gstelement_class,
      "Agnostic connector 2nd version",
      "Generic/Bin/Connector",
      "Automatically encodes/decodes media to match sink and source pads caps",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>");

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&sink_factory));

  gstelement_class->request_new_pad =
      GST_DEBUG_FUNCPTR (kms_agnostic_bin2_request_new_pad);
  gstelement_class->release_pad =
      GST_DEBUG_FUNCPTR (kms_agnostic_bin2_release_pad);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  g_type_class_add_private (klass, sizeof (KmsAgnosticBin2Private));
}

static void
kms_agnostic_bin2_init (KmsAgnosticBin2 * self)
{
  GstPadTemplate *templ;
  GstElement *tee, *queue, *fakesink;
  GstPad *target;

  self->priv = KMS_AGNOSTIC_BIN2_GET_PRIVATE (self);
  self->priv->pad_count = 0;

  tee = gst_element_factory_make ("tee", NULL);
  queue = gst_element_factory_make ("queue", NULL);
  fakesink = gst_element_factory_make ("fakesink", NULL);

  gst_bin_add_many (GST_BIN (self), tee, queue, fakesink, NULL);
  gst_element_link_many (tee, queue, fakesink, NULL);

  target = gst_element_get_static_pad (tee, "sink");
  templ = gst_static_pad_template_get (&sink_factory);
  self->priv->sink = gst_ghost_pad_new_from_template ("sink", target, templ);
  g_object_unref (templ);
  g_object_unref (target);

  gst_pad_add_probe (self->priv->sink, GST_PAD_PROBE_TYPE_BLOCK |
      GST_PAD_PROBE_TYPE_DATA_BOTH | GST_PAD_PROBE_TYPE_QUERY_BOTH,
      kms_agnostic_bin2_sink_block_probe, self, NULL);

  gst_element_add_pad (GST_ELEMENT (self), self->priv->sink);

  g_object_set (G_OBJECT (self), "async-handling", TRUE, NULL);

  self->priv->tees =
      g_hash_table_new_full (g_str_hash, g_str_equal, NULL, g_object_unref);
  g_hash_table_insert (self->priv->tees, GST_OBJECT_NAME (tee),
      g_object_ref (tee));

  self->priv->pads_to_link = g_queue_new ();
  g_cond_init (&self->priv->thread_cond);
  g_mutex_init (&self->priv->thread_mutex);

  self->priv->thread =
      g_thread_new (GST_OBJECT_NAME (self), kms_agnostic_bin2_connect_thread,
      self);
}

gboolean
kms_agnostic_bin2_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_AGNOSTIC_BIN2);
}
