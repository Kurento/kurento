#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <gst/gst.h>

#include "gstagnosticbin.h"

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

static GstStaticCaps static_raw_audio_caps = GST_STATIC_CAPS (RAW_AUDIO_CAPS);
static GstStaticCaps static_raw_video_caps = GST_STATIC_CAPS (RAW_VIDEO_CAPS);

static GstStaticCaps static_raw_caps = GST_STATIC_CAPS (RAW_CAPS);

GST_DEBUG_CATEGORY_STATIC (gst_agnostic_bin_debug);
#define GST_CAT_DEFAULT gst_agnostic_bin_debug

#define gst_agnostic_bin_parent_class parent_class
G_DEFINE_TYPE (GstAgnosticBin, gst_agnostic_bin, GST_TYPE_BIN);

typedef void (*GstStartStopFunction) (GstAgnosticBin * agnosticbin,
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
    GST_STATIC_CAPS (AGNOSTIC_CAPS)
    );

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE ("src_%u",
    GST_PAD_SRC,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (AGNOSTIC_CAPS)
    );

static void
gst_agnostic_bin_valve_start_stop (GstAgnosticBin * agnosticbin,
    GstElement * valve, gboolean start)
{
  g_object_set (valve, "drop", !start, NULL);
}

static void
gst_agnostic_bin_decodebin_start_stop (GstAgnosticBin * agnosticbin,
    GstElement * decodebin, gboolean start)
{
  GstElement *valve =
      g_object_get_data (G_OBJECT (decodebin), DECODEBIN_VALVE_DATA);

  g_object_set (valve, "drop", !start, NULL);
}

static gboolean
gst_agnostic_bin_start_stop_event_handler (GstPad * pad, GstObject * parent,
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
        GstAgnosticBin *agnosticbin =
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
gst_agnostic_bin_set_start_stop_event_handler (GstAgnosticBin * agnosticbin,
    GstElement * element, const char *pad_name, GstStartStopFunction callback)
{
  GstPad *pad = gst_element_get_static_pad (element, pad_name);

  if (pad == NULL)
    return;

  g_object_set_data (G_OBJECT (pad), OLD_EVENT_FUNC_DATA, pad->eventfunc);
  g_object_set_data (G_OBJECT (pad), START_STOP_EVENT_FUNC_DATA, callback);
  g_object_set_data (G_OBJECT (pad), AGNOSTIC_BIN_DATA, agnosticbin);
  gst_pad_set_event_function (pad, gst_agnostic_bin_start_stop_event_handler);
  g_object_unref (pad);
}

static void
gst_agnostic_bin_send_start_stop_event (GstPad * pad, gboolean start)
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
gst_agnostic_bin_send_force_key_unit_event (GstPad * pad)
{
  GstStructure *s;
  GstEvent *force_key_unit_event;

  GST_DEBUG ("Sending key ");
  s = gst_structure_new ("GstForceKeyUnit",
      "all-headers", G_TYPE_BOOLEAN, TRUE, NULL);
  force_key_unit_event = gst_event_new_custom (GST_EVENT_CUSTOM_UPSTREAM, s);
  gst_pad_send_event (pad, force_key_unit_event);
}

static void
gst_agnostic_bin_unlink_from_tee (GstElement * element, const gchar * pad_name)
{
  GstPad *sink = gst_element_get_static_pad (element, pad_name);
  GstPad *tee_src = gst_pad_get_peer (sink);
  GstElement *tee;

  g_object_unref (sink);

  if (tee_src != NULL) {
    tee = gst_pad_get_parent_element (tee_src);

    if (tee != NULL) {
      gint n_pads = 0;
      GstPad *tee_sink = gst_element_get_static_pad (tee, "sink");

      GST_PAD_STREAM_LOCK (tee_sink);
      gst_element_unlink (tee, element);
      gst_element_release_request_pad (tee, tee_src);

      g_object_get (tee, "num-src-pads", &n_pads, NULL);
      if (n_pads == 0)
        gst_agnostic_bin_send_start_stop_event (tee_sink, FALSE);

      GST_PAD_STREAM_UNLOCK (tee_sink);
      g_object_unref (tee_sink);
    }

    g_object_unref (tee_src);
  }
}

static void
gst_agnostic_bin_link_to_tee (GstElement * tee, GstElement * element,
    const gchar * sink_name)
{
  GstPad *tee_src, *tee_sink = gst_element_get_static_pad (tee, "sink");

  gst_agnostic_bin_unlink_from_tee (element, sink_name);
  GST_PAD_STREAM_LOCK (tee_sink);
  tee_src = gst_element_get_request_pad (tee, "src_%u");
  if (tee_src != NULL) {
    if (!gst_element_link_pads (tee, GST_OBJECT_NAME (tee_src), element,
            sink_name)) {
      gst_element_release_request_pad (tee, tee_src);
    } else {
      GstEvent *event = gst_event_new_reconfigure ();

      gst_pad_send_event (tee_src, event);
      gst_agnostic_bin_send_start_stop_event (tee_sink, TRUE);

      gst_agnostic_bin_send_force_key_unit_event (tee_src);
    }

    g_object_unref (tee_src);
  }

  GST_PAD_STREAM_UNLOCK (tee_sink);
  g_object_unref (tee_sink);
}

static void
gst_agnostic_bin_encoder_start_stop (GstAgnosticBin * agnosticbin,
    GstElement * encoder, gboolean start)
{
  GstElement *queue, *tee, *convert;

  if (start)
    return;

  convert = g_object_get_data (G_OBJECT (encoder), ENCODER_CONVERT_DATA);
  queue = g_object_get_data (G_OBJECT (encoder), ENCODER_QUEUE_DATA);
  tee = g_object_get_data (G_OBJECT (encoder), ENCODER_TEE_DATA);

  g_hash_table_remove (agnosticbin->encoded_tees, GST_OBJECT_NAME (tee));

  if (queue != NULL)
    gst_agnostic_bin_unlink_from_tee (queue, "sink");

  gst_element_set_state (queue, GST_STATE_NULL);
  gst_element_set_state (convert, GST_STATE_NULL);
  gst_element_set_state (encoder, GST_STATE_NULL);
  gst_element_set_state (tee, GST_STATE_NULL);

  gst_bin_remove_many (GST_BIN (agnosticbin), queue, convert, encoder, tee,
      NULL);
}

static GstElement *
gst_agnostic_get_convert_element_for_raw_caps (GstCaps * raw_caps)
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

/* This functions is called with the GST_AGNOSTIC_BIN_LOCK held */
static GstElement *
gst_agnostic_bin_create_encoded_tee (GstAgnosticBin * agnosticbin,
    GstCaps * allowed_caps)
{
  GstElement *queue, *encoder, *decoded_tee, *tee = NULL, *convert;
  GList *encoder_list, *filtered_list, *l;
  GstElementFactory *encoder_factory = NULL;
  GstPad *decoded_tee_sink;
  GstCaps *raw_caps;

  decoded_tee = gst_bin_get_by_name (GST_BIN (agnosticbin), DECODED_TEE);
  if (decoded_tee == NULL)
    return NULL;

  decoded_tee_sink = gst_element_get_static_pad (decoded_tee, "sink");
  raw_caps = gst_pad_get_current_caps (decoded_tee_sink);

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

  convert = gst_agnostic_get_convert_element_for_raw_caps (raw_caps);
  encoder = gst_element_factory_create (encoder_factory, NULL);
  queue = gst_element_factory_make ("queue", NULL);
  tee = gst_element_factory_make ("tee", NULL);

  gst_bin_add_many (GST_BIN (agnosticbin), queue, convert, encoder, tee, NULL);
  gst_element_sync_state_with_parent (queue);
  gst_element_sync_state_with_parent (convert);
  gst_element_sync_state_with_parent (encoder);
  gst_element_sync_state_with_parent (tee);

  g_hash_table_insert (agnosticbin->encoded_tees, GST_OBJECT_NAME (tee),
      g_object_ref (tee));
  gst_element_link_many (queue, convert, encoder, tee, NULL);
  gst_agnostic_bin_link_to_tee (decoded_tee, queue, "sink");

  g_object_set_data (G_OBJECT (encoder), ENCODER_QUEUE_DATA, queue);
  g_object_set_data (G_OBJECT (encoder), ENCODER_TEE_DATA, tee);
  g_object_set_data (G_OBJECT (encoder), ENCODER_CONVERT_DATA, convert);

  gst_agnostic_bin_set_start_stop_event_handler (agnosticbin, encoder,
      "src", gst_agnostic_bin_encoder_start_stop);

end:
  gst_plugin_feature_list_free (filtered_list);
  gst_plugin_feature_list_free (encoder_list);

  gst_caps_unref (raw_caps);

release_decoded_tee:
  g_object_unref (decoded_tee_sink);
  g_object_unref (decoded_tee);

  return tee;
}

static GstElement *
gst_agnostic_bin_get_queue_for_pad (GstPad * pad)
{
  GstElement *queue;
  GstPad *target;

  target = gst_ghost_pad_get_target (GST_GHOST_PAD (pad));

  if (target == NULL)
    return NULL;

  queue = gst_pad_get_parent_element (target);
  g_object_unref (target);

  return queue;
}

static void
gst_agnostic_bin_disconnect_srcpad (GstAgnosticBin * agnosticbin,
    GstPad * srcpad)
{
  GstElement *queue = gst_agnostic_bin_get_queue_for_pad (srcpad);

  if (queue == NULL)
    return;

  gst_agnostic_bin_unlink_from_tee (queue, "sink");
  g_object_unref (queue);
}

static void
gst_agnostic_bin_connect_srcpad (GstAgnosticBin * agnosticbin, GstPad * srcpad,
    GstPad * peer, const GstCaps * current_caps)
{
  GstCaps *allowed_caps, *raw_caps;
  GstElement *tee = NULL, *queue;

  GST_DEBUG ("Connecting %P", srcpad);
  if (!GST_IS_GHOST_PAD (srcpad)) {
    GST_DEBUG ("%P is no gp", srcpad);
    return;
  }

  allowed_caps = gst_pad_get_allowed_caps (srcpad);
  if (allowed_caps == NULL) {
    GST_DEBUG ("Allowed caps for %P are NULL. "
        "The pad is not linked, disconnecting", srcpad);
    gst_agnostic_bin_disconnect_srcpad (agnosticbin, srcpad);
    return;
  }

  if (current_caps == NULL) {
    GST_DEBUG ("No current caps, disconnecting %P", srcpad);
    gst_agnostic_bin_disconnect_srcpad (agnosticbin, srcpad);
    return;
  }

  raw_caps = gst_static_caps_get (&static_raw_caps);
  if (gst_caps_can_intersect (current_caps, allowed_caps)) {
    tee = gst_bin_get_by_name (GST_BIN (agnosticbin), INPUT_TEE);
  } else if (gst_caps_can_intersect (raw_caps, allowed_caps)) {
    GST_DEBUG ("Raw caps, looking for a decodebin");
    GST_AGNOSTIC_BIN_LOCK (agnosticbin);
    tee = gst_bin_get_by_name (GST_BIN (agnosticbin), DECODED_TEE);
    GST_AGNOSTIC_BIN_UNLOCK (agnosticbin);
  } else {
    GstElement *raw_tee;

    GST_AGNOSTIC_BIN_LOCK (agnosticbin);
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

        tee_caps = gst_pad_get_current_caps (sink);

        if (tee_caps != NULL) {
          if (gst_caps_can_intersect (tee_caps, allowed_caps))
            tee = g_object_ref (l->data);

          gst_caps_unref (tee_caps);
        }

        g_object_unref (sink);
      }

      if (tee == NULL)
        tee = gst_agnostic_bin_create_encoded_tee (agnosticbin, allowed_caps);

      g_object_unref (raw_tee);
    }
    GST_AGNOSTIC_BIN_UNLOCK (agnosticbin);
  }
  gst_caps_unref (raw_caps);

  queue = gst_agnostic_bin_get_queue_for_pad (srcpad);

  if (queue != NULL) {
    if (tee != NULL) {
      gst_agnostic_bin_link_to_tee (tee, queue, "sink");

      g_object_unref (tee);
    } else {
      gst_agnostic_bin_unlink_from_tee (queue, "sink");
    }
    g_object_unref (queue);
  }

  gst_caps_unref (allowed_caps);
}

static void
gst_agnostic_bin_connect_previous_srcpads (GstAgnosticBin * agnosticbin,
    const GstCaps * current_caps)
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
          gst_agnostic_bin_connect_srcpad (agnosticbin, srcpad, peer,
              current_caps);
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
gst_agnostic_bin_sink_event (GstPad * pad, GstObject * parent, GstEvent * event)
{
  gboolean ret;
  GstCaps *caps, *old_caps;

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_CAPS:
      gst_event_parse_caps (event, &caps);
      old_caps = gst_pad_get_current_caps (pad);
      ret = gst_pad_event_default (pad, parent, event);
      GST_DEBUG ("Received new caps: %P, old was: %P", caps, old_caps);
      if (ret && (old_caps == NULL || !gst_caps_is_equal (old_caps, caps)))
        gst_agnostic_bin_connect_previous_srcpads (GST_AGNOSTIC_BIN (parent),
            caps);
      if (old_caps != NULL)
        gst_caps_unref (old_caps);
      break;
    default:
      ret = gst_pad_event_default (pad, parent, event);
      break;
  }
  return ret;
}

static GstPadLinkReturn
gst_agnostic_bin_src_linked (GstPad * pad, GstObject * parent, GstPad * peer)
{
  GstAgnosticBin *agnosticbin = GST_AGNOSTIC_BIN (parent);
  GstCaps *current_caps;

  GST_DEBUG ("%P linked", pad);
  current_caps = gst_pad_get_current_caps (agnosticbin->sinkpad);
  gst_agnostic_bin_connect_srcpad (agnosticbin, pad, peer, current_caps);
  if (current_caps != NULL)
    gst_caps_unref (current_caps);

  if (peer->linkfunc != NULL)
    peer->linkfunc (peer, GST_OBJECT_PARENT (peer), pad);
  return GST_PAD_LINK_OK;
}

static void
gst_agnostic_bin_src_unlinked (GstPad * pad, GstPad * peer,
    GstAgnosticBin * agnosticbin)
{
  GST_DEBUG ("%P unlinked", pad);
  gst_agnostic_bin_disconnect_srcpad (agnosticbin, pad);
}

static gboolean
gst_agnostic_bin_src_event (GstPad * pad, GstObject * parent, GstEvent * event)
{
  if (event->type == GST_EVENT_RECONFIGURE) {
    GstAgnosticBin *agnosticbin = GST_AGNOSTIC_BIN (parent);
    GstPad *peer;
    GstCaps *current_caps;

    peer = gst_pad_get_peer (pad);

    if (peer != NULL) {
      current_caps = gst_pad_get_current_caps (agnosticbin->sinkpad);
      gst_agnostic_bin_connect_srcpad (agnosticbin, pad, peer, current_caps);
      if (current_caps != NULL)
        gst_caps_unref (current_caps);
      g_object_unref (peer);
    }
    gst_event_unref (event);
    return TRUE;
  } else {
    return gst_pad_event_default (pad, parent, event);
  }
}

static GstPad *
gst_agnostic_bin_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps)
{
  GstPad *pad, *target;
  gchar *pad_name;
  GstAgnosticBin *agnosticbin = GST_AGNOSTIC_BIN (element);
  GstElement *queue = gst_element_factory_make ("queue", NULL);

  GST_OBJECT_LOCK (element);
  pad_name = g_strdup_printf ("src_%d", agnosticbin->pad_count++);
  GST_OBJECT_UNLOCK (element);

  gst_bin_add (GST_BIN (agnosticbin), queue);
  gst_element_sync_state_with_parent (queue);
  target = gst_element_get_static_pad (queue, "src");

  pad = gst_ghost_pad_new_from_template (pad_name, target, templ);
  g_object_unref (target);
  g_free (pad_name);

  gst_pad_set_link_function (pad, gst_agnostic_bin_src_linked);
  g_signal_connect (pad, "unlinked", G_CALLBACK (gst_agnostic_bin_src_unlinked),
      element);
  gst_pad_set_event_function (pad, gst_agnostic_bin_src_event);
  // TODO: Add callback for query caps changes

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
gst_agnostic_bin_release_pad (GstElement * element, GstPad * pad)
{
  GstElement *queue;

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, FALSE);

  queue = gst_agnostic_bin_get_queue_for_pad (pad);

  if (queue != NULL) {
    gst_agnostic_bin_unlink_from_tee (queue, "sink");
    gst_element_set_state (queue, GST_STATE_NULL);
    gst_bin_remove (GST_BIN (element), queue);

    g_object_unref (queue);
  }

  gst_element_remove_pad (element, pad);
}

static void
gst_agnostic_bin_decodebin_pad_added (GstElement * decodebin, GstPad * pad,
    GstAgnosticBin * agnosticbin)
{
  GstElement *tee;

  GST_AGNOSTIC_BIN_LOCK (agnosticbin);
  tee = gst_bin_get_by_name (GST_BIN (agnosticbin), DECODED_TEE);
  if (tee == NULL) {
    GstElement *valve;
    GstCaps *current_caps;

    tee = gst_element_factory_make ("tee", DECODED_TEE);
    gst_bin_add (GST_BIN (agnosticbin), tee);
    GST_AGNOSTIC_BIN_UNLOCK (agnosticbin);

    gst_element_sync_state_with_parent (tee);
    gst_element_link_pads (decodebin, GST_OBJECT_NAME (pad), tee, "sink");
    valve = g_object_get_data (G_OBJECT (decodebin), DECODEBIN_VALVE_DATA);
    g_object_set (valve, "drop", TRUE, NULL);

    gst_agnostic_bin_set_start_stop_event_handler (agnosticbin, decodebin,
        GST_OBJECT_NAME (pad), gst_agnostic_bin_decodebin_start_stop);

    current_caps = gst_pad_get_current_caps (agnosticbin->sinkpad);
    gst_agnostic_bin_connect_previous_srcpads (agnosticbin, current_caps);
    if (current_caps != NULL)
      gst_caps_unref (current_caps);
  } else {
    GstElement *fakesink;

    GST_AGNOSTIC_BIN_UNLOCK (agnosticbin);
    fakesink = gst_element_factory_make ("fakesink", NULL);
    gst_bin_add (GST_BIN (agnosticbin), fakesink);
    gst_element_sync_state_with_parent (fakesink);
    gst_element_link_pads (decodebin, GST_OBJECT_NAME (pad), fakesink, "sink");
  }
}

static void
gst_agnostic_bin_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  switch (prop_id) {
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_agnostic_bin_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  switch (prop_id) {
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_agnostic_bin_dispose (GObject * object)
{
  GstAgnosticBin *agnosticbin = GST_AGNOSTIC_BIN (object);

  g_rec_mutex_clear (&agnosticbin->media_mutex);
  g_hash_table_unref (agnosticbin->encoded_tees);
  G_OBJECT_CLASS (gst_agnostic_bin_parent_class)->dispose (object);
}

static void
gst_agnostic_bin_class_init (GstAgnosticBinClass * klass)
{
  GObjectClass *gobject_class;
  GstElementClass *gstelement_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gstelement_class = GST_ELEMENT_CLASS (klass);

  gobject_class->set_property = gst_agnostic_bin_set_property;
  gobject_class->get_property = gst_agnostic_bin_get_property;
  gobject_class->dispose = gst_agnostic_bin_dispose;

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
      GST_DEBUG_FUNCPTR (gst_agnostic_bin_request_new_pad);
  gstelement_class->release_pad =
      GST_DEBUG_FUNCPTR (gst_agnostic_bin_release_pad);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);
}

static void
gst_agnostic_bin_init (GstAgnosticBin * agnosticbin)
{
  GstPad *target_sink;
  GstElement *valve, *tee, *decodebin, *queue, *deco_valve;
  GstPadTemplate *templ;

  g_rec_mutex_init (&agnosticbin->media_mutex);

  agnosticbin->encoded_tees =
      g_hash_table_new_full (g_str_hash, g_str_equal, NULL, g_object_unref);

  valve = gst_element_factory_make ("valve", NULL);
  tee = gst_element_factory_make ("tee", INPUT_TEE);
  decodebin = gst_element_factory_make ("decodebin", NULL);
  queue = gst_element_factory_make ("queue", NULL);
  deco_valve = gst_element_factory_make ("valve", NULL);

  g_signal_connect (decodebin, "pad-added",
      G_CALLBACK (gst_agnostic_bin_decodebin_pad_added), agnosticbin);
  g_object_set_data (G_OBJECT (decodebin), DECODEBIN_VALVE_DATA, deco_valve);

  gst_bin_add_many (GST_BIN (agnosticbin), valve, tee, queue, deco_valve,
      decodebin, NULL);
  gst_element_link (valve, tee);
  gst_element_link_many (queue, deco_valve, decodebin, NULL);

  gst_agnostic_bin_set_start_stop_event_handler (agnosticbin, valve, "src",
      gst_agnostic_bin_valve_start_stop);

  target_sink = gst_element_get_static_pad (valve, "sink");
  templ = gst_static_pad_template_get (&sink_factory);

  agnosticbin->pad_count = 0;
  agnosticbin->sinkpad =
      gst_ghost_pad_new_from_template ("sink", target_sink, templ);

  g_object_unref (templ);

  g_object_unref (target_sink);

  gst_pad_set_event_function (agnosticbin->sinkpad,
      GST_DEBUG_FUNCPTR (gst_agnostic_bin_sink_event));
  GST_PAD_SET_PROXY_CAPS (agnosticbin->sinkpad);
  gst_element_add_pad (GST_ELEMENT (agnosticbin), agnosticbin->sinkpad);

  gst_element_link (tee, queue);
}

gboolean
gst_agnostic_bin_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      GST_TYPE_AGNOSTIC_BIN);
}
