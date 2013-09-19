#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <gst/gst.h>

#include "kmsagnosticbin.h"
#include "kmsagnosticcaps.h"
#include "kmsutils.h"

#define PLUGIN_NAME "agnosticbin"

#define INPUT_TEE "input_tee"
#define DECODED_TEE "decoded_tee"

#define OLD_EVENT_FUNC_DATA "old_event_func"
#define START_STOP_EVENT_FUNC_DATA "start_stop_event_func"
#define AGNOSTIC_BIN_DATA "agnostic_bin"
#define DECODEBIN_VALVE_DATA "decodebin_valve"

#define ENCODER_QUEUE_DATA "enc_queue"
#define ENCODER_TEE_DATA "enc_tee"
#define ENCODER_CONVERT_DATA "enc_convert"

#define START_STOP_EVENT_NAME "event/start-stop"
#define START "start"

#define QUEUE_DATA "queue"

static GstStaticCaps static_raw_audio_caps =
GST_STATIC_CAPS (KMS_AGNOSTIC_RAW_AUDIO_CAPS);
static GstStaticCaps static_raw_video_caps =
GST_STATIC_CAPS (KMS_AGNOSTIC_RAW_VIDEO_CAPS);

static GstStaticCaps static_raw_caps = GST_STATIC_CAPS (KMS_AGNOSTIC_RAW_CAPS);

GST_DEBUG_CATEGORY_STATIC (kms_agnostic_bin_debug);
#define GST_CAT_DEFAULT kms_agnostic_bin_debug

#define kms_agnostic_bin_parent_class parent_class
G_DEFINE_TYPE (KmsAgnosticBin, kms_agnostic_bin, GST_TYPE_BIN);

typedef void (*GstStartStopFunction) (KmsAgnosticBin * agnosticbin,
    GstElement * element, gboolean start);

/* Filter signals and args */
enum
{
  /* FILL ME */
  LAST_SIGNAL
};

enum
{
  PROP_0
};

/* the capabilities of the inputs and outputs. */
static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS (KMS_AGNOSTIC_AGNOSTIC_CAPS)
    );

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE ("src_%u",
    GST_PAD_SRC,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (KMS_AGNOSTIC_AGNOSTIC_CAPS)
    );

static void
kms_agnostic_bin_valve_start_stop (KmsAgnosticBin * agnosticbin,
    GstElement * valve, gboolean start)
{
  kms_utils_set_valve_drop (valve, !start);
}

static void
kms_agnostic_bin_decodebin_start_stop (KmsAgnosticBin * agnosticbin,
    GstElement * decodebin, gboolean start)
{
  GstElement *valve =
      g_object_get_data (G_OBJECT (decodebin), DECODEBIN_VALVE_DATA);

  kms_utils_set_valve_drop (valve, !start);
}

static gboolean
kms_agnostic_bin_start_stop_event_handler (GstPad * pad, GstObject * parent,
    GstEvent * event)
{
  gboolean ret = TRUE;
  GstPadEventFunction old_func =
      g_object_get_data (G_OBJECT (pad), OLD_EVENT_FUNC_DATA);

  if (GST_EVENT_TYPE (event) == GST_EVENT_CUSTOM_UPSTREAM) {
    const GstStructure *st = gst_event_get_structure (event);

    if (st != NULL
        && g_strcmp0 (gst_structure_get_name (st),
            START_STOP_EVENT_NAME) == 0) {
      gboolean start;

      if (gst_structure_get_boolean (st, START, &start)) {
        KmsAgnosticBin *agnosticbin =
            g_object_get_data (G_OBJECT (pad), AGNOSTIC_BIN_DATA);
        GstStartStopFunction callback =
            g_object_get_data (G_OBJECT (pad), START_STOP_EVENT_FUNC_DATA);
        GST_INFO ("Received event: %P", event);

        if (callback != NULL)
          callback (agnosticbin, GST_ELEMENT (parent), start);

        gst_event_unref (event);
        return TRUE;
      }
    }
  }

  if (old_func != NULL)
    ret = old_func (pad, parent, event);
  else
    ret = gst_pad_event_default (pad, parent, event);

  return ret;
}

static void
kms_agnostic_bin_set_start_stop_event_handler (KmsAgnosticBin * agnosticbin,
    GstElement * element, const char *pad_name, GstStartStopFunction callback)
{
  GstPad *pad = gst_element_get_static_pad (element, pad_name);

  if (pad == NULL)
    return;

  g_object_set_data (G_OBJECT (pad), OLD_EVENT_FUNC_DATA, pad->eventfunc);
  g_object_set_data (G_OBJECT (pad), START_STOP_EVENT_FUNC_DATA, callback);
  g_object_set_data (G_OBJECT (pad), AGNOSTIC_BIN_DATA, agnosticbin);
  gst_pad_set_event_function (pad, kms_agnostic_bin_start_stop_event_handler);
  g_object_unref (pad);
}

static void
kms_agnostic_bin_send_start_stop_event (GstPad * pad, gboolean start)
{
  GstStructure *data =
      gst_structure_new (START_STOP_EVENT_NAME, START, G_TYPE_BOOLEAN, start,
      NULL);
  GstEvent *event = gst_event_new_custom (GST_EVENT_CUSTOM_UPSTREAM, data);

  if (GST_PAD_DIRECTION (pad) == GST_PAD_SRC) {
    gst_pad_send_event (pad, event);
  } else {
    GstPad *peer = gst_pad_get_peer (pad);

    if (peer != NULL && GST_PAD_DIRECTION (peer) == GST_PAD_SRC)
      gst_pad_send_event (peer, event);
    else
      gst_event_unref (event);

    if (peer != NULL)
      g_object_unref (peer);
  }
}

static void
kms_agnostic_bin_send_force_key_unit_event (GstPad * pad)
{
  GstStructure *s;
  GstEvent *force_key_unit_event;

  GST_DEBUG ("Sending key ");
  s = gst_structure_new ("GstForceKeyUnit",
      "all-headers", G_TYPE_BOOLEAN, TRUE, NULL);
  force_key_unit_event = gst_event_new_custom (GST_EVENT_CUSTOM_UPSTREAM, s);
  gst_pad_send_event (pad, force_key_unit_event);
}

static GstPadProbeReturn
drop_probe (GstPad * pad, GstPadProbeInfo * info, gpointer data)
{
  // Drop all events:
  GST_DEBUG ("Info: %d", info->type);
  return GST_PAD_PROBE_DROP;
}

static void
kms_agnostic_bin_unlink_from_tee (GstElement * element, const gchar * pad_name)
{
  GstPad *sink = gst_element_get_static_pad (element, pad_name);
  GstPad *tee_src = gst_pad_get_peer (sink);
  GstElement *tee;

  g_object_unref (sink);

  if (tee_src == NULL)
    return;

  tee = gst_pad_get_parent_element (tee_src);

  if (tee != NULL) {
    gint n_pads = 0;
    GstPad *tee_sink = gst_element_get_static_pad (tee, "sink");
    gulong probe;
    GstPad *probe_pad;

    g_object_get (tee, "num-src-pads", &n_pads, NULL);

    if (n_pads == 1) {
      GST_DEBUG ("Adding probe: %P", tee_sink);
      probe_pad = tee_sink;
      probe =
          gst_pad_add_probe (probe_pad, GST_PAD_PROBE_TYPE_ALL_BOTH,
          drop_probe, NULL, NULL);
      kms_agnostic_bin_send_start_stop_event (tee_sink, FALSE);
    } else {
      probe_pad = tee_src;
      probe =
          gst_pad_add_probe (probe_pad, GST_PAD_PROBE_TYPE_ALL_BOTH,
          drop_probe, NULL, NULL);
    }

    gst_element_unlink (tee, element);

    gst_element_release_request_pad (tee, tee_src);
    gst_pad_remove_probe (probe_pad, probe);

    g_object_unref (tee_sink);
    g_object_unref (tee);
  }

  g_object_unref (tee_src);
}

static void
kms_agnostic_bin_link_to_tee (GstElement * tee, GstElement * element,
    const gchar * sink_name)
{
  GstPad *tee_sink = gst_element_get_static_pad (tee, "sink");

  GstPad *sink = gst_element_get_static_pad (element, sink_name);
  GstPad *tee_src = gst_pad_get_peer (sink);
  gboolean already_linked = FALSE;

  if (tee_src != NULL) {
    GstElement *old_tee;

    old_tee = gst_pad_get_parent_element (tee_src);

    if (tee != old_tee) {
      kms_agnostic_bin_unlink_from_tee (element, sink_name);
    } else {
      GstCaps *current_caps = gst_pad_get_current_caps (tee_sink);

      if (current_caps != NULL) {
        GstEvent *event = gst_event_new_caps (current_caps);

        gst_pad_push_event (tee_src, event);

        gst_caps_unref (current_caps);
      }
      already_linked = TRUE;
    }

    if (old_tee)
      g_object_unref (old_tee);

    g_object_unref (tee_src);
  }

  g_object_unref (sink);

  if (already_linked)
    goto end;

  GST_OBJECT_FLAG_SET (tee_sink, GST_PAD_FLAG_BLOCKED);
  tee_src = gst_element_get_request_pad (tee, "src_%u");
  if (tee_src != NULL) {
    GST_OBJECT_FLAG_SET (tee_src, GST_PAD_FLAG_BLOCKED);
    if (!gst_element_link_pads (tee, GST_OBJECT_NAME (tee_src), element,
            sink_name)) {
      GST_OBJECT_FLAG_UNSET (tee_src, GST_PAD_FLAG_BLOCKED);
      gst_element_release_request_pad (tee, tee_src);
    } else {
      GST_OBJECT_FLAG_UNSET (tee_src, GST_PAD_FLAG_BLOCKED);
      kms_agnostic_bin_send_start_stop_event (tee_sink, TRUE);

      kms_agnostic_bin_send_force_key_unit_event (tee_src);
    }

    g_object_unref (tee_src);
  }

  GST_OBJECT_FLAG_UNSET (tee_sink, GST_PAD_FLAG_BLOCKED);

end:
  g_object_unref (tee_sink);
}

static void
kms_agnostic_bin_encoder_start_stop (KmsAgnosticBin * agnosticbin,
    GstElement * encoder, gboolean start)
{
  GstElement *queue, *tee, *convert;

  queue = g_object_get_data (G_OBJECT (encoder), ENCODER_QUEUE_DATA);

  if (start) {
    GstElement *decoded_tee;
    GstPad *queue_sink;

    queue_sink = gst_element_get_static_pad (queue, "sink");

    if (!gst_pad_is_linked (queue_sink)) {
      GST_INFO ("Start received in encoded tee for the first time, linking");
      decoded_tee = gst_bin_get_by_name (GST_BIN (agnosticbin), DECODED_TEE);
      if (decoded_tee != NULL) {
        kms_agnostic_bin_link_to_tee (decoded_tee, queue, "sink");

        g_object_unref (decoded_tee);
      }
    }

    g_object_unref (queue_sink);
    return;
  }

  convert = g_object_get_data (G_OBJECT (encoder), ENCODER_CONVERT_DATA);
  tee = g_object_get_data (G_OBJECT (encoder), ENCODER_TEE_DATA);

  g_hash_table_remove (agnosticbin->encoded_tees, GST_OBJECT_NAME (tee));

  if (queue != NULL)
    kms_agnostic_bin_unlink_from_tee (queue, "sink");

  gst_element_set_locked_state (queue, TRUE);
  gst_element_set_locked_state (convert, TRUE);
  gst_element_set_locked_state (encoder, TRUE);
  gst_element_set_locked_state (tee, TRUE);

  gst_element_set_state (queue, GST_STATE_NULL);
  gst_element_set_state (convert, GST_STATE_NULL);
  gst_element_set_state (encoder, GST_STATE_NULL);
  gst_element_set_state (tee, GST_STATE_NULL);

  gst_bin_remove_many (GST_BIN (agnosticbin), queue, convert, encoder, tee,
      NULL);
}

static GstElement *
kms_agnostic_get_convert_element_for_raw_caps (GstCaps * raw_caps)
{
  GstElement *convert = NULL;
  GstCaps *audio_caps = gst_static_caps_get (&static_raw_audio_caps);
  GstCaps *video_caps = gst_static_caps_get (&static_raw_video_caps);

  if (gst_caps_can_intersect (raw_caps, video_caps)) {
    convert = gst_element_factory_make ("videoconvert", NULL);
  } else if (gst_caps_can_intersect (raw_caps, audio_caps)) {
    convert = gst_element_factory_make ("audioconvert", NULL);
  }

  if (convert == NULL)
    convert = gst_element_factory_make ("identity", NULL);

  gst_caps_unref (audio_caps);
  gst_caps_unref (video_caps);

  return convert;
}

static void
configure_encoder (GstElement * encoder, const gchar * factory_name)
{
  GST_DEBUG ("Configure encoder: %s", factory_name);
  if (g_strcmp0 ("vp8enc", factory_name) == 0) {
    g_object_set (G_OBJECT (encoder), "deadline", 1, "threads", 1, "cpu-used",
        16, NULL);
  } else if (g_strcmp0 ("x264enc", factory_name) == 0) {
    g_object_set (G_OBJECT (encoder), "speed-preset", 1 /* ultrafast */ ,
        "tune", 4 /* zerolatency */ , "threads", 1,
        NULL);
  }
}

/* This functions is called with the KMS_AGNOSTIC_BIN_LOCK held */
static GstElement *
kms_agnostic_bin_create_encoded_tee (KmsAgnosticBin * agnosticbin,
    GstCaps * allowed_caps)
{
  GstElement *queue, *encoder, *decoded_tee, *tee = NULL, *convert;
  GList *encoder_list, *filtered_list, *l;
  GstElementFactory *encoder_factory = NULL;
  GstPad *decoded_tee_sink;
  GstCaps *raw_caps;

  GST_DEBUG ("Creating a new encoded tee for caps: %P", allowed_caps);

  decoded_tee = gst_bin_get_by_name (GST_BIN (agnosticbin), DECODED_TEE);
  if (decoded_tee == NULL)
    return NULL;

  decoded_tee_sink = gst_element_get_static_pad (decoded_tee, "sink");
  raw_caps = gst_pad_get_allowed_caps (decoded_tee_sink);

  if (raw_caps == NULL)
    goto release_decoded_tee;

  encoder_list =
      gst_element_factory_list_get_elements (GST_ELEMENT_FACTORY_TYPE_ENCODER,
      GST_RANK_NONE);
  filtered_list =
      gst_element_factory_list_filter (encoder_list, allowed_caps, GST_PAD_SRC,
      FALSE);

  for (l = filtered_list; l != NULL && encoder_factory == NULL; l = l->next) {
    encoder_factory = GST_ELEMENT_FACTORY (l->data);
    if (gst_element_factory_get_num_pad_templates (encoder_factory) != 2) {
      encoder_factory = NULL;
    }
  }

  if (encoder_factory == NULL)
    goto end;

  GST_DEBUG ("Factory %P", encoder_factory);

  convert = kms_agnostic_get_convert_element_for_raw_caps (raw_caps);
  encoder = gst_element_factory_create (encoder_factory, NULL);
  queue = gst_element_factory_make ("queue", NULL);
  tee = gst_element_factory_make ("tee", NULL);

  configure_encoder (encoder, GST_OBJECT_NAME (encoder_factory));

  gst_bin_add_many (GST_BIN (agnosticbin), queue, convert, encoder,
      g_object_ref (tee), NULL);
  gst_element_sync_state_with_parent (queue);
  gst_element_sync_state_with_parent (convert);
  gst_element_sync_state_with_parent (encoder);
  gst_element_sync_state_with_parent (tee);

  g_hash_table_insert (agnosticbin->encoded_tees, GST_OBJECT_NAME (tee),
      g_object_ref (tee));
  gst_element_link_many (queue, convert, encoder, tee, NULL);

  g_object_set_data (G_OBJECT (encoder), ENCODER_QUEUE_DATA, queue);
  g_object_set_data (G_OBJECT (encoder), ENCODER_TEE_DATA, tee);
  g_object_set_data (G_OBJECT (encoder), ENCODER_CONVERT_DATA, convert);

  kms_agnostic_bin_set_start_stop_event_handler (agnosticbin, encoder,
      "src", kms_agnostic_bin_encoder_start_stop);

end:
  gst_plugin_feature_list_free (filtered_list);
  gst_plugin_feature_list_free (encoder_list);

  gst_caps_unref (raw_caps);

release_decoded_tee:
  g_object_unref (decoded_tee_sink);
  g_object_unref (decoded_tee);

  GST_DEBUG ("Created %P", tee);

  return tee;
}

static GstElement *
kms_agnostic_bin_get_queue_from_pad (GstPad * pad)
{
  GstElement *queue;
  gpointer data = g_object_get_data (G_OBJECT (pad), QUEUE_DATA);

  if (data == NULL || !GST_IS_ELEMENT (data))
    return NULL;

  queue = GST_ELEMENT (data);

  if (queue != NULL)
    g_object_ref (queue);

  return queue;
}

static void
kms_agnostic_bin_disconnect_srcpad (KmsAgnosticBin * agnosticbin,
    GstPad * srcpad)
{
  GstElement *queue = kms_agnostic_bin_get_queue_from_pad (srcpad);

  if (queue == NULL)
    return;

  kms_agnostic_bin_unlink_from_tee (queue, "sink");
  g_object_unref (queue);
}

static void
kms_agnostic_bin_connect_srcpad (KmsAgnosticBin * agnosticbin, GstPad * srcpad,
    GstPad * peer)
{
  GstCaps *allowed_caps, *raw_caps;
  GstElement *tee = NULL, *queue;
  GstPad *queue_sink;

  GST_DEBUG ("Connecting %P", srcpad);
  if (!GST_IS_GHOST_PAD (srcpad)) {
    GST_DEBUG ("%P is no gp", srcpad);
    return;
  }

  allowed_caps = gst_pad_query_caps (peer, NULL);
  if (allowed_caps == NULL) {
    GST_DEBUG ("Allowed caps for %P are NULL. "
        "The pad is not linked, disconnecting", srcpad);
    kms_agnostic_bin_disconnect_srcpad (agnosticbin, srcpad);
    return;
  }

  if (agnosticbin->current_caps == NULL) {
    GST_DEBUG ("No current caps, disconnecting %P", srcpad);
    kms_agnostic_bin_disconnect_srcpad (agnosticbin, srcpad);
    return;
  }

  KMS_AGNOSTIC_BIN_LOCK (agnosticbin);

  queue = kms_agnostic_bin_get_queue_from_pad (srcpad);

  if (queue != NULL && gst_caps_is_any (allowed_caps)) {
    queue_sink = gst_element_get_static_pad (queue, "sink");
    if (queue_sink != NULL) {
      if (gst_pad_is_linked (queue_sink)) {
        GstPad *peer;

        peer = gst_pad_get_peer (queue_sink);
        if (peer != NULL) {
          tee = gst_pad_get_parent_element (peer);
          g_object_unref (peer);
        }
      }
      g_object_unref (queue_sink);
    }
  }

  raw_caps = gst_static_caps_get (&static_raw_caps);
  if (tee != NULL) {
    GST_DEBUG ("Allowed caps are any and its already connected");
  } else if (gst_caps_can_intersect (agnosticbin->current_caps, allowed_caps)) {
    tee = gst_bin_get_by_name (GST_BIN (agnosticbin), INPUT_TEE);
  } else if (gst_caps_can_intersect (raw_caps, allowed_caps)) {
    GST_DEBUG ("Raw caps, looking for a decodebin");
    tee = gst_bin_get_by_name (GST_BIN (agnosticbin), DECODED_TEE);
  } else {
    GstElement *raw_tee;

    GST_DEBUG ("Looking for an encoded tee with caps: %P", allowed_caps);
    raw_tee = gst_bin_get_by_name (GST_BIN (agnosticbin), DECODED_TEE);
    if (raw_tee != NULL) {
      GList *tees, *l;

      tees = g_hash_table_get_values (agnosticbin->encoded_tees);
      for (l = tees; l != NULL && tee == NULL; l = l->next) {
        GstCaps *tee_caps;
        GstPad *sink =
            gst_element_get_static_pad (GST_ELEMENT (l->data), "sink");

        if (sink == NULL)
          continue;

        tee_caps = gst_pad_get_allowed_caps (sink);

        if (tee_caps != NULL) {
          if (gst_caps_can_intersect (tee_caps, allowed_caps))
            tee = g_object_ref (l->data);

          gst_caps_unref (tee_caps);
        }

        g_object_unref (sink);
      }

      if (tee == NULL)
        tee = kms_agnostic_bin_create_encoded_tee (agnosticbin, allowed_caps);

      g_object_unref (raw_tee);
    }
  }
  gst_caps_unref (raw_caps);

  if (queue != NULL) {
    if (tee != NULL) {
      kms_agnostic_bin_link_to_tee (tee, queue, "sink");
      g_object_unref (tee);
    } else {
      kms_agnostic_bin_unlink_from_tee (queue, "sink");
    }

    g_object_unref (queue);
  }

  KMS_AGNOSTIC_BIN_UNLOCK (agnosticbin);

  gst_caps_unref (allowed_caps);
}

static void
kms_agnostic_bin_connect_previous_srcpads (KmsAgnosticBin * agnosticbin)
{
  GValue item = { 0, };
  GstIterator *it;
  gboolean done;

  it = gst_element_iterate_src_pads (GST_ELEMENT (agnosticbin));
  done = FALSE;

  while (!done) {
    switch (gst_iterator_next (it, &item)) {
      case GST_ITERATOR_OK:
      {
        GstPad *srcpad, *peer;

        srcpad = g_value_get_object (&item);
        peer = gst_pad_get_peer (srcpad);

        if (peer != NULL) {
          kms_agnostic_bin_connect_srcpad (agnosticbin, srcpad, peer);
          g_object_unref (peer);
        }

        g_value_reset (&item);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        done = TRUE;
        break;
      case GST_ITERATOR_DONE:
        done = TRUE;
        break;
    }
  }

  g_value_unset (&item);
  gst_iterator_free (it);
}

/* this function handles sink events */
static gboolean
kms_agnostic_bin_sink_event (GstPad * pad, GstObject * parent, GstEvent * event)
{
  gboolean ret;
  GstCaps *caps, *old_caps;

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_CAPS:
      KMS_AGNOSTIC_BIN_LOCK (parent);
      gst_event_parse_caps (event, &caps);
      if (KMS_AGNOSTIC_BIN (parent)->current_caps != NULL)
        gst_caps_unref (KMS_AGNOSTIC_BIN (parent)->current_caps);
      KMS_AGNOSTIC_BIN (parent)->current_caps = gst_caps_copy (caps);
      old_caps = gst_pad_get_current_caps (pad);
      gst_event_ref (event);
      ret = gst_pad_event_default (pad, parent, event);
      GST_DEBUG ("Received new caps: %P, old was: %P", caps, old_caps);
      if (ret && (old_caps == NULL || !gst_caps_is_equal (old_caps, caps)))
        kms_agnostic_bin_connect_previous_srcpads (KMS_AGNOSTIC_BIN (parent));
      gst_event_unref (event);
      if (old_caps != NULL)
        gst_caps_unref (old_caps);
      KMS_AGNOSTIC_BIN_UNLOCK (parent);
      break;
    default:
      ret = gst_pad_event_default (pad, parent, event);
      break;
  }
  return ret;
}

static GstPadLinkReturn
kms_agnostic_bin_src_linked (GstPad * pad, GstObject * parent, GstPad * peer)
{
  KmsAgnosticBin *agnosticbin = KMS_AGNOSTIC_BIN (parent);

  KMS_AGNOSTIC_BIN_LOCK (agnosticbin);
  GST_DEBUG ("%P linked", pad);
  kms_agnostic_bin_connect_srcpad (agnosticbin, pad, peer);

  if (peer->linkfunc != NULL)
    peer->linkfunc (peer, GST_OBJECT_PARENT (peer), pad);
  KMS_AGNOSTIC_BIN_UNLOCK (agnosticbin);
  return GST_PAD_LINK_OK;
}

static void
kms_agnostic_bin_src_unlinked (GstPad * pad, GstObject * parent)
{
  KmsAgnosticBin *agnosticbin = KMS_AGNOSTIC_BIN (parent);

  GST_DEBUG ("%P unlinked", pad);
  KMS_AGNOSTIC_BIN_LOCK (agnosticbin);
  kms_agnostic_bin_disconnect_srcpad (agnosticbin, pad);
  KMS_AGNOSTIC_BIN_UNLOCK (agnosticbin);
}

static gboolean
kms_agnostic_bin_src_event (GstPad * pad, GstObject * parent, GstEvent * event)
{
  if (event->type == GST_EVENT_RECONFIGURE) {
    KmsAgnosticBin *agnosticbin = KMS_AGNOSTIC_BIN (parent);
    GstPad *peer;

    KMS_AGNOSTIC_BIN_LOCK (agnosticbin);
    GST_DEBUG ("Reconfiguring %P", pad);
    peer = gst_pad_get_peer (pad);

    if (peer != NULL) {
      kms_agnostic_bin_connect_srcpad (agnosticbin, pad, peer);
      g_object_unref (peer);
    }
    KMS_AGNOSTIC_BIN_UNLOCK (agnosticbin);

    return TRUE;
  } else {
    return gst_pad_event_default (pad, parent, event);
  }
}

static GstPad *
kms_agnostic_bin_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps)
{
  GstPad *pad, *target;
  gchar *pad_name;
  KmsAgnosticBin *agnosticbin = KMS_AGNOSTIC_BIN (element);
  GstElement *queue = gst_element_factory_make ("queue", NULL);

  g_rec_mutex_lock (&agnosticbin->mutex);
  pad_name = g_strdup_printf ("src_%d", agnosticbin->pad_count++);
  g_rec_mutex_unlock (&agnosticbin->mutex);

  gst_bin_add (GST_BIN (agnosticbin), queue);
  gst_element_sync_state_with_parent (queue);
  target = gst_element_get_static_pad (queue, "src");

  pad = gst_ghost_pad_new_from_template (pad_name, target, templ);
  g_object_set_data (G_OBJECT (pad), QUEUE_DATA, queue);

  g_object_unref (target);
  g_free (pad_name);

  gst_pad_set_link_function (pad, kms_agnostic_bin_src_linked);
  gst_pad_set_unlink_function (pad, kms_agnostic_bin_src_unlinked);
  gst_pad_set_event_function (pad, kms_agnostic_bin_src_event);

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, TRUE);

  if (gst_element_add_pad (element, pad))
    return pad;

  g_object_unref (pad);

  return NULL;
}

static void
kms_agnostic_bin_release_pad (GstElement * element, GstPad * pad)
{
  GstElement *queue;
  KmsAgnosticBin *agnosticbin;

  agnosticbin = KMS_AGNOSTIC_BIN (GST_OBJECT_PARENT (pad));

  KMS_AGNOSTIC_BIN_LOCK (agnosticbin);
  queue = kms_agnostic_bin_get_queue_from_pad (pad);

  g_object_set_data (G_OBJECT (pad), QUEUE_DATA, NULL);

  if (queue != NULL) {
    kms_agnostic_bin_unlink_from_tee (queue, "sink");
    gst_bin_remove (GST_BIN (element), queue);
    gst_element_set_state (queue, GST_STATE_NULL);

    g_object_unref (queue);
  }

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, FALSE);
  gst_element_remove_pad (element, pad);

  KMS_AGNOSTIC_BIN_UNLOCK (agnosticbin);
}

static void
kms_agnostic_bin_decodebin_pad_added (GstElement * decodebin, GstPad * pad,
    KmsAgnosticBin * agnosticbin)
{
  GstElement *tee;

  KMS_AGNOSTIC_BIN_LOCK (agnosticbin);
  tee = gst_bin_get_by_name (GST_BIN (agnosticbin), DECODED_TEE);
  if (tee == NULL) {
    GstElement *valve;
    GstCaps *caps, *video_caps;

    tee = gst_element_factory_make ("tee", DECODED_TEE);
    gst_bin_add (GST_BIN (agnosticbin), tee);
    gst_element_sync_state_with_parent (tee);

    video_caps = gst_static_caps_get (&static_raw_video_caps);
    caps = gst_pad_query_caps (pad, NULL);
    if (gst_caps_can_intersect (caps, video_caps)) {
      gint num = 15, denom = 1;
      GstElement *capsfilter = gst_element_factory_make ("capsfilter", NULL);
      GstElement *videorate = gst_element_factory_make ("videorate", NULL);
      GstCaps *fps_caps;

      if (gst_caps_is_fixed (caps) && gst_caps_get_size (caps) == 1) {
        GstStructure *st;

        st = gst_caps_get_structure (caps, 0);

        if (gst_structure_has_field_typed (st, "framerate", GST_TYPE_FRACTION)) {
          gst_structure_get_fraction (st, "framerate", &num, &denom);
        }
      }

      if (num == 0) {
        num = 15;
        denom = 1;
      }

      fps_caps = gst_caps_new_simple ("video/x-raw", "framerate",
          GST_TYPE_FRACTION, num, denom, NULL);

      g_object_set (G_OBJECT (capsfilter), "caps", fps_caps, NULL);
      g_object_set (G_OBJECT (videorate), "average-period", 200 * GST_MSECOND,
          NULL);

      gst_caps_unref (fps_caps);

      gst_bin_add_many (GST_BIN (agnosticbin), capsfilter, videorate, NULL);
      gst_element_sync_state_with_parent (videorate);
      gst_element_sync_state_with_parent (capsfilter);

      gst_element_link_pads (decodebin, GST_OBJECT_NAME (pad), videorate,
          "sink");
      gst_element_link_many (videorate, capsfilter, tee, NULL);

    } else {
      gst_element_link_pads (decodebin, GST_OBJECT_NAME (pad), tee, "sink");
    }

    gst_caps_unref (caps);
    gst_caps_unref (video_caps);

    valve = g_object_get_data (G_OBJECT (decodebin), DECODEBIN_VALVE_DATA);
    kms_utils_set_valve_drop (valve, TRUE);

    kms_agnostic_bin_set_start_stop_event_handler (agnosticbin, decodebin,
        GST_OBJECT_NAME (pad), kms_agnostic_bin_decodebin_start_stop);

    kms_agnostic_bin_connect_previous_srcpads (agnosticbin);
  } else {
    GstElement *fakesink;

    fakesink = gst_element_factory_make ("fakesink", NULL);
    gst_bin_add (GST_BIN (agnosticbin), fakesink);
    gst_element_sync_state_with_parent (fakesink);
    gst_element_link_pads (decodebin, GST_OBJECT_NAME (pad), fakesink, "sink");
  }
  KMS_AGNOSTIC_BIN_UNLOCK (agnosticbin);
}

static void
kms_agnostic_bin_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  switch (prop_id) {
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_agnostic_bin_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  switch (prop_id) {
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_agnostic_bin_dispose (GObject * object)
{
  KmsAgnosticBin *agnosticbin = KMS_AGNOSTIC_BIN (object);

  /* unref referencies held by this element */

  if (agnosticbin->encoded_tees) {
    g_hash_table_unref (agnosticbin->encoded_tees);
    agnosticbin->encoded_tees = NULL;
  }

  /* chain up */
  G_OBJECT_CLASS (kms_agnostic_bin_parent_class)->dispose (object);
}

static void
kms_agnostic_bin_finalize (GObject * object)
{
  KmsAgnosticBin *agnosticbin = KMS_AGNOSTIC_BIN (object);

  /* free resources allocated by this object */
  g_rec_mutex_clear (&agnosticbin->mutex);
  if (agnosticbin->current_caps != NULL) {
    gst_caps_unref (agnosticbin->current_caps);
    agnosticbin->current_caps = NULL;
  }

  /* chain up */
  G_OBJECT_CLASS (kms_agnostic_bin_parent_class)->finalize (object);
}

static void
kms_agnostic_bin_class_init (KmsAgnosticBinClass * klass)
{
  GObjectClass *gobject_class;
  GstElementClass *gstelement_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gstelement_class = GST_ELEMENT_CLASS (klass);

  gobject_class->set_property = kms_agnostic_bin_set_property;
  gobject_class->get_property = kms_agnostic_bin_get_property;
  gobject_class->dispose = kms_agnostic_bin_dispose;
  gobject_class->finalize = kms_agnostic_bin_finalize;

  gst_element_class_set_details_simple (gstelement_class,
      "Agnostic connector",
      "Generic/Bin/Connector",
      "Automatically encodes/decodes media to match sink and source pads caps",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>, "
      "Miguel París Díaz <mparisdiaz@gmail.com>");

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&sink_factory));

  gstelement_class->request_new_pad =
      GST_DEBUG_FUNCPTR (kms_agnostic_bin_request_new_pad);
  gstelement_class->release_pad =
      GST_DEBUG_FUNCPTR (kms_agnostic_bin_release_pad);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);
}

static void
kms_agnostic_bin_init (KmsAgnosticBin * agnosticbin)
{
  GstPad *target_sink;
  GstElement *valve, *tee, *decodebin, *queue, *deco_valve;
  GstPadTemplate *templ;

  g_rec_mutex_init (&agnosticbin->mutex);

  agnosticbin->encoded_tees =
      g_hash_table_new_full (g_str_hash, g_str_equal, NULL, g_object_unref);

  valve = gst_element_factory_make ("valve", NULL);
  tee = gst_element_factory_make ("tee", INPUT_TEE);
  decodebin = gst_element_factory_make ("decodebin", NULL);
  queue = gst_element_factory_make ("queue", NULL);
  deco_valve = gst_element_factory_make ("valve", NULL);

  g_signal_connect (decodebin, "pad-added",
      G_CALLBACK (kms_agnostic_bin_decodebin_pad_added), agnosticbin);
  g_object_set_data (G_OBJECT (decodebin), DECODEBIN_VALVE_DATA, deco_valve);

  gst_bin_add_many (GST_BIN (agnosticbin), valve, tee, queue, deco_valve,
      decodebin, NULL);
  gst_element_link (valve, tee);
  gst_element_link_many (queue, deco_valve, decodebin, NULL);

  kms_agnostic_bin_set_start_stop_event_handler (agnosticbin, valve, "src",
      kms_agnostic_bin_valve_start_stop);

  target_sink = gst_element_get_static_pad (valve, "sink");
  templ = gst_static_pad_template_get (&sink_factory);

  agnosticbin->pad_count = 0;
  agnosticbin->sinkpad =
      gst_ghost_pad_new_from_template ("sink", target_sink, templ);
  agnosticbin->current_caps = NULL;

  g_object_unref (templ);

  g_object_unref (target_sink);

  gst_pad_set_event_function (agnosticbin->sinkpad,
      GST_DEBUG_FUNCPTR (kms_agnostic_bin_sink_event));
  GST_PAD_SET_PROXY_CAPS (agnosticbin->sinkpad);
  gst_element_add_pad (GST_ELEMENT (agnosticbin), agnosticbin->sinkpad);

  gst_element_link (tee, queue);

  g_object_set (G_OBJECT (agnosticbin), "async-handling", TRUE, NULL);
}

gboolean
kms_agnostic_bin_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_AGNOSTIC_BIN);
}
