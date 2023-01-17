/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

#include <gst/check/gstcheck.h>
#include <gst/gst.h>
#include <glib.h>

#define DATA "data"
#define AUDIO "audio"
#define VIDEO "video"
#define DATA_SRC_PAD_PREFIX DATA "_src_"
#define VIDEO_SRC_PAD_PREFIX VIDEO "_src_"
#define AUDIO_SRC_PAD_PREFIX AUDIO "_src_"

#define KMS_ELEMENT_PAD_TYPE_DATA 0
#define KMS_ELEMENT_PAD_TYPE_AUDIO 1
#define KMS_ELEMENT_PAD_TYPE_VIDEO 2

#define DISCONNECTED "disconnected"
G_DEFINE_QUARK (VIDEO_SINK, disconnected);

#define MAX_CHECKS 10

static GMainLoop *loop;
GstElement *pipeline;

typedef GstPadProbeReturn (*KmsProbeType) (GstPad *, GstPadProbeInfo *,
    gpointer);

typedef struct _KmsConnectData
{
  GRecMutex mutex;
  GstElement *src;
  GstElement *sink;
  gchar *audio_src;
  gchar *video_src;
  gchar *data_src;
  GstPad *audiosrc;
  GstPad *videosrc;
  GstPad *datasrc;
  GstPad *audiosink;
  GstPad *videosink;
  GstPad *datasink;
  gboolean audio_connected;
  gboolean video_connected;
  gboolean data_connected;
  gboolean audio_buff;
  gboolean video_buff;
  gboolean data_buff;
  guint audio_checks;
  guint video_checks;
  guint data_checks;
  KmsProbeType data_probe;
  KmsProbeType audio_probe;
  KmsProbeType video_probe;
} KmsConnectData;

#define CONNECT_DATA_LOCK(data) \
  (g_rec_mutex_lock (&(data)->mutex))
#define CONNECT_DATA_UNLOCK(data) \
  (g_rec_mutex_unlock (&(data)->mutex))

static void
kms_connect_data_destroy (KmsConnectData * data)
{
  g_free (data->audio_src);
  g_free (data->video_src);
  g_free (data->data_src);

  g_rec_mutex_clear (&data->mutex);

  g_slice_free (KmsConnectData, data);
}

static KmsConnectData *
kms_connect_data_create (guint checks)
{
  KmsConnectData *data = g_slice_new0 (KmsConnectData);

  g_rec_mutex_init (&data->mutex);
  data->data_checks = data->audio_checks = data->video_checks = checks;

  return data;
}

static void
bus_msg (GstBus * bus, GstMessage * msg, gpointer pipe)
{
  switch (GST_MESSAGE_TYPE (msg)) {
    case GST_MESSAGE_ERROR:{
      gchar *error_file = g_strdup_printf ("error-%s", GST_OBJECT_NAME (pipe));

      GST_ERROR ("Error: %" GST_PTR_FORMAT, msg);
      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipe),
          GST_DEBUG_GRAPH_SHOW_ALL, error_file);
      g_free (error_file);
      fail ("Error received on bus");
      break;
    }
    case GST_MESSAGE_WARNING:{
      gchar *warn_file = g_strdup_printf ("warning-%s", GST_OBJECT_NAME (pipe));

      GST_WARNING ("Warning: %" GST_PTR_FORMAT, msg);
      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipe),
          GST_DEBUG_GRAPH_SHOW_ALL, warn_file);
      g_free (warn_file);
      break;
    }
    default:
      break;
  }
}

static gboolean
quit_main_loop_idle (gpointer data)
{
  g_main_loop_quit (loop);
  return FALSE;
}

static gboolean
print_timedout_pipeline (gpointer data)
{
  gchar *pipeline_name;
  gchar *name;

  pipeline_name = gst_element_get_name (pipeline);
  name = g_strdup_printf ("%s_timedout", pipeline_name);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, name);

  g_free (name);
  g_free (pipeline_name);

  return G_SOURCE_REMOVE;
}

static void
pad_added_delayed (GstElement * element, GstPad * new_pad, gpointer user_data)
{
  gchar *padname = *(gchar **) user_data;
  gchar *name;

  GST_DEBUG_OBJECT (element, "Added pad %" GST_PTR_FORMAT, new_pad);
  name = gst_pad_get_name (new_pad);

  if (g_strcmp0 (padname, name) == 0) {
    g_idle_add (quit_main_loop_idle, NULL);
  }

  g_free (name);
}

static void
pad_added (GstElement * element, GstPad * new_pad, gpointer user_data)
{
  gchar *prefix = user_data;
  gchar *name;

  GST_DEBUG_OBJECT (element, "Added pad %" GST_PTR_FORMAT, new_pad);
  name = gst_pad_get_name (new_pad);

  if (g_str_has_prefix (name, prefix)) {
    g_idle_add (quit_main_loop_idle, NULL);
  }

  g_free (name);
}

GST_START_TEST (request_video_src_pad_pending)
{
  GstElement *dummysrc;
  gchar *padname = NULL;
  GstBus *bus;

  loop = g_main_loop_new (NULL, TRUE);
  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  dummysrc = gst_element_factory_make ("dummysrc", NULL);
  g_signal_connect (dummysrc, "pad-added", G_CALLBACK (pad_added_delayed),
      &padname);

  gst_bin_add (GST_BIN (pipeline), dummysrc);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* request src pad using action */
  g_signal_emit_by_name (dummysrc, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &padname);
  fail_if (padname == NULL);

  GST_DEBUG ("Pad name %s", padname);
  g_object_set (G_OBJECT (dummysrc), "video", TRUE, NULL);

  g_free (padname);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);
  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST
GST_START_TEST (request_audio_src_pad_pending)
{
  GstElement *dummysrc;
  gchar *padname = NULL;
  GstBus *bus;

  loop = g_main_loop_new (NULL, TRUE);
  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  dummysrc = gst_element_factory_make ("dummysrc", NULL);
  g_signal_connect (dummysrc, "pad-added", G_CALLBACK (pad_added_delayed),
      &padname);

  gst_bin_add (GST_BIN (pipeline), dummysrc);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* request src pad using action */
  g_signal_emit_by_name (dummysrc, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_AUDIO, NULL, GST_PAD_SRC, &padname);
  fail_if (padname == NULL);

  GST_DEBUG ("Pad name %s", padname);
  g_object_set (G_OBJECT (dummysrc), "audio", TRUE, NULL);

  g_free (padname);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);
  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST
GST_START_TEST (request_video_src_pad)
{
  gchar *padname = NULL, *prefix = "video_src";
  GstElement *dummysrc;
  GstBus *bus;

  loop = g_main_loop_new (NULL, TRUE);
  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  dummysrc = gst_element_factory_make ("dummysrc", NULL);
  g_object_set (G_OBJECT (dummysrc), "video", TRUE, NULL);
  g_signal_connect (dummysrc, "pad-added", G_CALLBACK (pad_added), prefix);

  gst_bin_add (GST_BIN (pipeline), dummysrc);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* request src pad using action */
  g_signal_emit_by_name (dummysrc, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &padname);
  fail_if (padname == NULL);

  GST_DEBUG ("Pad name %s", padname);
  g_free (padname);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);
  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST
GST_START_TEST (request_audio_src_pad)
{
  gchar *padname = NULL, *prefix = "audio_src";
  GstElement *dummysrc;
  GstBus *bus;

  loop = g_main_loop_new (NULL, TRUE);
  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  dummysrc = gst_element_factory_make ("dummysrc", NULL);
  g_object_set (G_OBJECT (dummysrc), "audio", TRUE, NULL);
  g_signal_connect (dummysrc, "pad-added", G_CALLBACK (pad_added), prefix);

  gst_bin_add (GST_BIN (pipeline), dummysrc);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* request src pad using action */
  g_signal_emit_by_name (dummysrc, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_AUDIO, NULL, GST_PAD_SRC, &padname);
  fail_if (padname == NULL);

  GST_DEBUG_OBJECT (dummysrc, "Pad name %s", padname);
  g_free (padname);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);
  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST
GST_START_TEST (request_data_src_pad)
{
  gchar *padname = NULL, *prefix = "data_src";
  GstElement *dummysrc;
  GstBus *bus;

  loop = g_main_loop_new (NULL, TRUE);
  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  dummysrc = gst_element_factory_make ("dummysrc", NULL);
  g_object_set (G_OBJECT (dummysrc), "data", TRUE, NULL);
  g_signal_connect (dummysrc, "pad-added", G_CALLBACK (pad_added), prefix);

  gst_bin_add (GST_BIN (pipeline), dummysrc);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* request src pad using action */
  g_signal_emit_by_name (dummysrc, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_DATA, NULL, GST_PAD_SRC, &padname);
  fail_if (padname == NULL);

  GST_DEBUG ("Pad name %s", padname);
  g_free (padname);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);
  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST static void
exit_on_handoff (GstElement * object, GstBuffer * buff, GstPad * pad,
    gboolean * success)
{
  /* We have received a buffer, finish test */
  GST_DEBUG_OBJECT (object, "Buffer received.");
  g_idle_add (quit_main_loop_idle, NULL);
}

static void
pad_added_connect_source (GstElement * element, GstPad * new_pad,
    gpointer user_data)
{
  GstElement *sink;
  GstPad *sinkpad;

  sink = gst_element_factory_make ("fakesink", NULL);
  g_object_set (G_OBJECT (sink), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);
  g_signal_connect (sink, "handoff", G_CALLBACK (exit_on_handoff), NULL);

  gst_bin_add (GST_BIN (pipeline), sink);

  sinkpad = gst_element_get_static_pad (sink, "sink");
  if (gst_pad_link (new_pad, sinkpad) != GST_PAD_LINK_OK) {
    fail ("Could not link pads");
  }

  g_object_unref (sinkpad);
  gst_element_sync_state_with_parent (sink);
}

GST_START_TEST (request_video_src_pad_connection)
{
  GstElement *dummysrc;
  gchar *padname = NULL;
  GstBus *bus;

  loop = g_main_loop_new (NULL, TRUE);
  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  dummysrc = gst_element_factory_make ("dummysrc", NULL);
  g_signal_connect (dummysrc, "pad-added",
      G_CALLBACK (pad_added_connect_source), NULL);

  gst_bin_add (GST_BIN (pipeline), dummysrc);

  /* request src pad using action */
  g_signal_emit_by_name (dummysrc, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &padname);
  fail_if (padname == NULL);

  GST_DEBUG ("Pad name %s", padname);
  g_object_set (G_OBJECT (dummysrc), "video", TRUE, NULL);

  g_free (padname);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);
  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST static gboolean
release_requested_srcpad (GstPad * pad)
{
  GstElement *dummysrc;
  gboolean success;

  dummysrc = gst_pad_get_parent_element (pad);
  fail_if (dummysrc == NULL);

  GST_DEBUG_OBJECT (dummysrc, "Release requested src pad %" GST_PTR_FORMAT,
      pad);

  g_signal_emit_by_name (dummysrc, "release-requested-pad", pad, &success);
  fail_if (!success);

  g_object_unref (dummysrc);

  return G_SOURCE_REMOVE;
}

static void
unlinked_srcpad_added (GstElement * dummysrc, GstPad * new_pad,
    gpointer user_data)
{
  GST_DEBUG_OBJECT (dummysrc, "Added pad %" GST_PTR_FORMAT, new_pad);

  g_idle_add_full (G_PRIORITY_DEFAULT_IDLE,
      (GSourceFunc) release_requested_srcpad,
      g_object_ref (new_pad), g_object_unref);
}

static void
unlinked_srcpad_removed (GstElement * element, GstPad * old_pad,
    gpointer user_data)
{
  GST_DEBUG ("Removed pad %" GST_PTR_FORMAT, old_pad);

  g_idle_add (quit_main_loop_idle, NULL);
}

GST_START_TEST (disconnect_requested_src_pad_not_linked)
{
  GstElement *dummysrc;
  gchar *padname = NULL;
  GstBus *bus;

  loop = g_main_loop_new (NULL, TRUE);
  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  dummysrc = gst_element_factory_make ("dummysrc", NULL);
  g_signal_connect (dummysrc, "pad-added", G_CALLBACK (unlinked_srcpad_added),
      NULL);
  g_signal_connect (dummysrc, "pad-removed",
      G_CALLBACK (unlinked_srcpad_removed), NULL);

  gst_bin_add (GST_BIN (pipeline), dummysrc);

  /* request src pad using action */
  g_signal_emit_by_name (dummysrc, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &padname);
  fail_if (padname == NULL);

  GST_DEBUG ("Pad name %s", padname);
  g_object_set (G_OBJECT (dummysrc), "video", TRUE, NULL);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);
  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_free (padname);
  g_main_loop_unref (loop);
}

GST_END_TEST G_LOCK_DEFINE (handoff_lock);
static gboolean done = FALSE;
static gulong signal_id;

static GstPadProbeReturn
pad_probe_cb (GstPad * pad, GstPadProbeInfo * info, gpointer user_data)
{
  GstElement *dummysrc;
  gboolean success;

  GST_DEBUG_OBJECT (pad, "pad is blocked now");

  /* remove the probe first */
  gst_pad_remove_probe (pad, GST_PAD_PROBE_INFO_ID (info));

  dummysrc = gst_pad_get_parent_element (pad);
  fail_if (dummysrc == NULL);

  GST_DEBUG_OBJECT (dummysrc, "Release requested src pad %" GST_PTR_FORMAT,
      pad);

  g_signal_emit_by_name (dummysrc, "release-requested-pad", pad, &success);
  fail_if (!success);

  g_object_unref (dummysrc);

  return GST_PAD_PROBE_OK;
}

static gboolean
release_linked_requested_srcpad (GstPad * pad)
{
  GstPad *peerpad;

  peerpad = gst_pad_get_peer (pad);
  fail_if (peerpad == NULL);

  /* Do well: Remove requested pad with the source pad blocked */
  gst_pad_add_probe (peerpad, GST_PAD_PROBE_TYPE_BLOCK_DOWNSTREAM,
      pad_probe_cb, g_object_ref (pad), g_object_unref);

  g_object_unref (peerpad);

  return G_SOURCE_REMOVE;
}

static void
unlink_on_handoff (GstElement * object, GstBuffer * buff, GstPad * pad,
    gpointer user_data)
{
  /* We have received a buffer, finish test */
  GST_DEBUG_OBJECT (object, "Buffer received.");

  G_LOCK (handoff_lock);
  if (!done) {
    g_idle_add_full (G_PRIORITY_DEFAULT_IDLE,
        (GSourceFunc) release_linked_requested_srcpad,
        g_object_ref (pad), g_object_unref);
    g_signal_handler_disconnect (object, signal_id);
    done = TRUE;
  }
  G_UNLOCK (handoff_lock);
}

static void
linked_srcpad_added (GstElement * dummysrc, GstPad * new_pad,
    gpointer user_data)
{
  GstElement *sink;
  GstPad *sinkpad;

  GST_DEBUG_OBJECT (dummysrc, "Added pad %" GST_PTR_FORMAT, new_pad);

  sink = gst_element_factory_make ("fakesink", NULL);
  g_object_set (G_OBJECT (sink), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);
  signal_id = g_signal_connect (sink, "handoff", G_CALLBACK (unlink_on_handoff),
      NULL);

  gst_bin_add (GST_BIN (pipeline), sink);

  sinkpad = gst_element_get_static_pad (sink, "sink");
  if (gst_pad_link (new_pad, sinkpad) != GST_PAD_LINK_OK) {
    fail ("Could not link pads");
  }

  g_object_unref (sinkpad);
  gst_element_sync_state_with_parent (sink);
}

static void
linked_srcpad_removed (GstElement * element, GstPad * old_pad,
    gpointer user_data)
{
  GST_DEBUG ("Removed pad %" GST_PTR_FORMAT, old_pad);

  g_idle_add (quit_main_loop_idle, NULL);
}

GST_START_TEST (disconnect_requested_src_pad_linked)
{
  GstElement *dummysrc;
  gchar *padname = NULL;
  GstBus *bus;

  loop = g_main_loop_new (NULL, TRUE);
  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  dummysrc = gst_element_factory_make ("dummysrc", NULL);
  g_signal_connect (dummysrc, "pad-added", G_CALLBACK (linked_srcpad_added),
      NULL);
  g_signal_connect (dummysrc, "pad-removed",
      G_CALLBACK (linked_srcpad_removed), NULL);

  gst_bin_add (GST_BIN (pipeline), dummysrc);

  /* request src pad using action */
  g_signal_emit_by_name (dummysrc, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &padname);
  fail_if (padname == NULL);

  GST_DEBUG ("Pad name %s", padname);
  g_object_set (G_OBJECT (dummysrc), "video", TRUE, NULL);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);
  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_free (padname);
  g_main_loop_unref (loop);
}

GST_END_TEST;

static gboolean
remove_src_pad (gpointer user_data)
{
  GstElement *dummysrc, *bufferinjector = user_data;
  GstPad *sink = gst_element_get_static_pad (bufferinjector, "sink");
  GstPad *src;
  gboolean success = FALSE;

  fail_unless (sink);
  src = gst_pad_get_peer (sink);
  fail_unless (src);
  dummysrc = gst_pad_get_parent_element (src);

  g_signal_emit_by_name (dummysrc, "release-requested-pad", src, &success);
  fail_unless (success);

  g_object_unref (src);
  g_object_unref (sink);
  g_object_unref (dummysrc);

  g_object_set_qdata (G_OBJECT (bufferinjector), disconnected_quark (),
      GINT_TO_POINTER (TRUE));

  return G_SOURCE_REMOVE;
}

static void
unlink_buffer_injector_on_handoff (GstElement * object, GstBuffer * buff,
    GstPad * pad, gpointer user_data)
{
  GstElement *bufferinjector = user_data;
  static gint count = 0;

  GST_DEBUG_OBJECT (object, "Buffer received");

  if (count == 0) {
    count++;
    g_idle_add_full (G_PRIORITY_DEFAULT_IDLE, remove_src_pad,
        g_object_ref (bufferinjector), g_object_unref);
  } else {
    gboolean disconnected;

    if (count > 10) {
      g_idle_add (quit_main_loop_idle, NULL);
    }

    disconnected =
        GPOINTER_TO_INT (g_object_get_qdata (G_OBJECT (bufferinjector),
            disconnected_quark ()));

    if (disconnected) {
      count++;
    }
  }
}

static void
linked_srcpad_added_link_buffer_injector (GstElement * dummysrc,
    GstPad * new_pad, gpointer user_data)
{
  GstElement *bufferinjector;
  GstElement *sink;
  GstPad *sinkpad;

  GST_DEBUG_OBJECT (dummysrc, "Added pad %" GST_PTR_FORMAT, new_pad);

  bufferinjector = gst_element_factory_make ("bufferinjector", NULL);
  sink = gst_element_factory_make ("fakesink", NULL);
  g_object_set (G_OBJECT (sink), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);
  signal_id =
      g_signal_connect (sink, "handoff",
      G_CALLBACK (unlink_buffer_injector_on_handoff), bufferinjector);

  gst_bin_add_many (GST_BIN (pipeline), bufferinjector, sink, NULL);
  gst_element_sync_state_with_parent (sink);
  gst_element_sync_state_with_parent (bufferinjector);
  gst_element_link (bufferinjector, sink);

  sinkpad = gst_element_get_static_pad (bufferinjector, "sink");
  if (gst_pad_link (new_pad, sinkpad) != GST_PAD_LINK_OK) {
    fail ("Could not link pads");
  }

  g_object_unref (sinkpad);
}

GST_START_TEST (disconnect_requested_src_pad_linked_with_buffer_injector)
{
  GstElement *dummysrc;
  gchar *padname = NULL;
  GstBus *bus;

  loop = g_main_loop_new (NULL, TRUE);
  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  dummysrc = gst_element_factory_make ("dummysrc", NULL);
  g_signal_connect (dummysrc, "pad-added",
      G_CALLBACK (linked_srcpad_added_link_buffer_injector), NULL);

  gst_bin_add (GST_BIN (pipeline), dummysrc);

  /* request src pad using action */
  g_signal_emit_by_name (dummysrc, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &padname);
  fail_if (padname == NULL);

  GST_DEBUG ("Pad name %s", padname);
  g_object_set (G_OBJECT (dummysrc), "video", TRUE, NULL);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);
  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_free (padname);
  g_main_loop_unref (loop);
}

GST_END_TEST;

static GstPadProbeReturn
audio_probe_cb (GstPad * pad, GstPadProbeInfo * info, KmsConnectData * data)
{
  GST_DEBUG_OBJECT (pad, "buffer received");

  gst_pad_remove_probe (pad, GST_PAD_PROBE_INFO_ID (info));

  CONNECT_DATA_LOCK (data);
  if (!data->audio_buff) {
    data->audio_buff = TRUE;
    CONNECT_DATA_UNLOCK (data);

    GST_DEBUG_OBJECT (data->sink, "Disabling reception of audio stream");
    /* Do not accept more audio data */
    g_object_set (G_OBJECT (data->sink), "audio", FALSE, NULL);
  } else {
    CONNECT_DATA_UNLOCK (data);
  }

  return GST_PAD_PROBE_OK;
}

static GstPadProbeReturn
data_probe_cb (GstPad * pad, GstPadProbeInfo * info, KmsConnectData * data)
{
  GstBuffer *buffer;
  GstMapInfo minfo;
  gchar *msg;

  buffer = GST_PAD_PROBE_INFO_BUFFER (info);

  if (!gst_buffer_map (buffer, &minfo, GST_MAP_READ)) {
    GST_ERROR_OBJECT (pad, "Failed to map input buffer");
    return GST_FLOW_ERROR;
  }

  msg = g_strndup ((const gchar *) minfo.data, minfo.size);
  GST_INFO ("Buffer content: (%s)", msg);
  g_free (msg);

  gst_buffer_unmap (buffer, &minfo);

  gst_pad_remove_probe (pad, GST_PAD_PROBE_INFO_ID (info));

  CONNECT_DATA_LOCK (data);
  if (!data->data_buff) {
    gboolean actived;

    data->data_buff = TRUE;
    CONNECT_DATA_UNLOCK (data);

    GST_DEBUG_OBJECT (data->sink, "Disabling reception of data stream");
    g_object_get (G_OBJECT (data->sink), "data", &actived, NULL);
    if (actived) {
      /* Do not accept more data */
      g_object_set (G_OBJECT (data->sink), "data", FALSE, NULL);
    } else {
      gboolean success;

      g_signal_emit_by_name (
          data->sink, "release-requested-pad", pad, &success);
      fail_if (!success);
    }
  } else {
    CONNECT_DATA_UNLOCK (data);
  }

  return GST_PAD_PROBE_OK;
}

static GstPadProbeReturn
video_probe_cb (GstPad * pad, GstPadProbeInfo * info, KmsConnectData * data)
{
  GST_DEBUG_OBJECT (pad, "buffer received");

  gst_pad_remove_probe (pad, GST_PAD_PROBE_INFO_ID (info));

  CONNECT_DATA_LOCK (data);
  if (!data->video_buff) {
    data->video_buff = TRUE;
    CONNECT_DATA_UNLOCK (data);

    GST_DEBUG_OBJECT (data->sink, "Disabling reception of video stream");
    /* Do not accept more video data */
    g_object_set (G_OBJECT (data->sink), "video", FALSE, NULL);
  } else {
    CONNECT_DATA_UNLOCK (data);
  }

  return GST_PAD_PROBE_OK;
}

static gboolean
connect_pads (gpointer user_data)
{
  KmsConnectData *data = user_data;

  CONNECT_DATA_LOCK (data);

  if (!data->audio_connected && data->audiosrc != NULL &&
      data->audiosink != NULL) {
    data->audio_connected = gst_pad_link (data->audiosrc, data->audiosink) ==
        GST_PAD_LINK_OK;
    fail_unless (data->audio_connected, "Could not connect audio pads");
    GST_DEBUG ("Connected audio stream");
    if (data->audio_probe != NULL) {
      gst_pad_add_probe (data->audiosink, GST_PAD_PROBE_TYPE_BUFFER,
          (GstPadProbeCallback) audio_probe_cb, data, NULL);
    }
  }

  if (!data->video_connected && data->videosrc != NULL &&
      data->videosink != NULL) {
    data->video_connected = gst_pad_link (data->videosrc, data->videosink) ==
        GST_PAD_LINK_OK;
    fail_unless (data->video_connected, "Could not connect video pads");
    GST_DEBUG ("Connected video stream");
    if (data->video_probe != NULL) {
      gst_pad_add_probe (data->videosink, GST_PAD_PROBE_TYPE_BUFFER,
          (GstPadProbeCallback) video_probe_cb, data, NULL);
    }
  }

  if (!data->data_connected && data->datasrc != NULL && data->datasink != NULL) {
    data->data_connected = gst_pad_link (data->datasrc, data->datasink) ==
        GST_PAD_LINK_OK;
    fail_unless (data->data_connected, "Could not connect data pads");
    GST_DEBUG ("Connected data stream");
    if (data->audio_probe != NULL) {
      gst_pad_add_probe (data->datasink, GST_PAD_PROBE_TYPE_BUFFER,
          (GstPadProbeCallback) data_probe_cb, data, NULL);
    }
  }

  CONNECT_DATA_UNLOCK (data);

  return G_SOURCE_REMOVE;
}

static void
src_pads_added (GstElement * element, GstPad * new_pad, gpointer user_data)
{
  KmsConnectData *data = user_data;
  gchar *name;

  GST_DEBUG_OBJECT (element, "Added pad %" GST_PTR_FORMAT, new_pad);
  name = gst_pad_get_name (new_pad);

  CONNECT_DATA_LOCK (data);

  if (g_str_has_prefix (name, VIDEO_SRC_PAD_PREFIX)) {
    data->videosrc = new_pad;
  } else if (g_str_has_prefix (name, AUDIO_SRC_PAD_PREFIX)) {
    data->audiosrc = new_pad;
  } else if (g_str_has_prefix (name, DATA_SRC_PAD_PREFIX)) {
    data->datasrc = new_pad;
  } else {
    GST_DEBUG ("Unsupported pad type %s", name);
  }

  CONNECT_DATA_UNLOCK (data);

  g_free (name);
  g_idle_add (connect_pads, data);
}

static void
sink_pads_added (GstElement * element, GstPad * new_pad, gpointer user_data)
{
  KmsConnectData *data = user_data;
  gchar *name;

  GST_DEBUG_OBJECT (element, "Added pad %" GST_PTR_FORMAT, new_pad);
  name = gst_pad_get_name (new_pad);

  CONNECT_DATA_LOCK (data);

  if (g_str_has_prefix (name, "sink_" AUDIO)) {
    data->audiosink = new_pad;
  } else if (g_str_has_prefix (name, "sink_" VIDEO)) {
    data->videosink = new_pad;
  } else if (g_str_has_prefix (name, "sink_" DATA)) {
    data->datasink = new_pad;
  } else {
    GST_DEBUG ("Unsupported pad type %s", name);
  }

  CONNECT_DATA_UNLOCK (data);

  g_free (name);
  g_idle_add (connect_pads, data);
}

static gboolean
enable_audio_stream (GstElement * dummysink)
{
  GST_DEBUG_OBJECT (dummysink, "Enabling audio");
  g_object_set (G_OBJECT (dummysink), "audio", TRUE, NULL);

  return G_SOURCE_REMOVE;
}

static gboolean
enable_video_stream (GstElement * dummysink)
{
  GST_DEBUG_OBJECT (dummysink, "Enabling video");
  g_object_set (G_OBJECT (dummysink), "video", TRUE, NULL);

  return G_SOURCE_REMOVE;
}

static gboolean
enable_data_stream (GstElement * dummysink)
{
  GST_DEBUG_OBJECT (dummysink, "Enabling data");
  g_object_set (G_OBJECT (dummysink), "data", TRUE, NULL);

  return G_SOURCE_REMOVE;
}

static void
sink_pads_removed (GstElement * element, GstPad * old_pad, gpointer user_data)
{
  KmsConnectData *data = user_data;
  gchar *name;

  GST_DEBUG ("Removed pad %" GST_PTR_FORMAT, old_pad);
  name = gst_pad_get_name (old_pad);

  CONNECT_DATA_LOCK (data);

  if (g_str_has_prefix (name, "sink_" VIDEO)) {
    data->videosink = NULL;
    data->video_checks--;
    data->video_connected = FALSE;
    data->video_buff = FALSE;
    if (data->video_checks > 0) {
      g_idle_add ((GSourceFunc) enable_video_stream, data->sink);
    }
  } else if (g_str_has_prefix (name, "sink_" AUDIO)) {
    data->audiosink = NULL;
    data->audio_checks--;
    data->audio_connected = FALSE;
    data->audio_buff = FALSE;
    if (data->audio_checks > 0) {
      g_idle_add ((GSourceFunc) enable_audio_stream, data->sink);
    }
  } else if (g_str_has_prefix (name, "sink_" DATA)) {
    data->datasink = NULL;
    data->data_checks--;
    data->data_connected = FALSE;
    data->data_buff = FALSE;
    if (data->data_checks > 0) {
      g_idle_add ((GSourceFunc) enable_data_stream, data->sink);
    }
  }

  GST_DEBUG ("Audio tests %u. Video tests: %u, Data tests %u",
      data->audio_checks, data->video_checks, data->data_checks);

  if (data->audio_checks == 0 && data->video_checks == 0 &&
      data->data_checks == 0) {
    g_idle_add (quit_main_loop_idle, NULL);
  }

  CONNECT_DATA_UNLOCK (data);

  g_free (name);
}

GST_START_TEST (connection_of_elements)
{
  gchar *padname = NULL;
  KmsConnectData *data;
  GstBus *bus;

  data = kms_connect_data_create (MAX_CHECKS);
  data->data_probe = (KmsProbeType) data_probe_cb;
  data->audio_probe = (KmsProbeType) audio_probe_cb;
  data->video_probe = (KmsProbeType) video_probe_cb;

  loop = g_main_loop_new (NULL, TRUE);
  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  data->src = gst_element_factory_make ("dummysrc", NULL);
  data->sink = gst_element_factory_make ("dummysink", NULL);
  g_signal_connect (data->src, "pad-added", G_CALLBACK (src_pads_added), data);
  g_signal_connect (data->sink, "pad-added",
      G_CALLBACK (sink_pads_added), data);

  g_signal_connect (data->sink, "pad-removed",
      G_CALLBACK (sink_pads_removed), data);

  gst_bin_add_many (GST_BIN (pipeline), data->src, data->sink, NULL);

  /* request src pad using action */
  g_signal_emit_by_name (data->src, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &data->video_src);
  fail_if (data->video_src == NULL);
  g_signal_emit_by_name (data->src, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_AUDIO, NULL, GST_PAD_SRC, &data->audio_src);
  fail_if (data->audio_src == NULL);
  g_signal_emit_by_name (data->src, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_DATA, NULL, GST_PAD_SRC, &data->data_src);
  fail_if (data->data_src == NULL);

  GST_DEBUG ("Data pad name %s", data->data_src);
  GST_DEBUG ("Audio pad name %s", data->audio_src);
  GST_DEBUG ("Video pad name %s", data->video_src);

  g_object_set (G_OBJECT (data->src), "video", TRUE, NULL);
  g_object_set (G_OBJECT (data->src), "audio", TRUE, NULL);
  g_object_set (G_OBJECT (data->src), "data", TRUE, NULL);

  g_object_set (G_OBJECT (data->sink), "video", TRUE, NULL);
  g_object_set (G_OBJECT (data->sink), "audio", TRUE, NULL);
  g_object_set (G_OBJECT (data->sink), "data", TRUE, NULL);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);
  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_free (padname);
  g_main_loop_unref (loop);
  kms_connect_data_destroy (data);
}

GST_END_TEST
GST_START_TEST (request_data_src_pad_pending)
{
  GstElement *dummysrc;
  gchar *padname = NULL;
  GstBus *bus;

  loop = g_main_loop_new (NULL, TRUE);
  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  dummysrc = gst_element_factory_make ("dummysrc", NULL);
  g_signal_connect (dummysrc, "pad-added", G_CALLBACK (pad_added_delayed),
      &padname);

  gst_bin_add (GST_BIN (pipeline), dummysrc);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* request src pad using action */
  g_signal_emit_by_name (dummysrc, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_DATA, NULL, GST_PAD_SRC, &padname);
  fail_if (padname == NULL);

  GST_DEBUG ("Pad name %s", padname);
  g_object_set (G_OBJECT (dummysrc), "data", TRUE, NULL);

  g_free (padname);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);
  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);
}

GST_END_TEST
GST_START_TEST (connection_of_elements_data)
{
  gchar *padname = NULL;
  KmsConnectData *data;
  GstBus *bus;

  data = kms_connect_data_create (MAX_CHECKS);
  data->data_probe = (KmsProbeType) data_probe_cb;
  data->audio_probe = (KmsProbeType) audio_probe_cb;
  data->video_probe = (KmsProbeType) video_probe_cb;

  /* Only tests data */
  data->video_checks = data->audio_checks = 0;

  loop = g_main_loop_new (NULL, TRUE);
  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  data->src = gst_element_factory_make ("dummysrc", NULL);
  data->sink = gst_element_factory_make ("dummysink", NULL);
  g_signal_connect (data->src, "pad-added", G_CALLBACK (src_pads_added), data);
  g_signal_connect (data->sink, "pad-added",
      G_CALLBACK (sink_pads_added), data);

  g_signal_connect (data->sink, "pad-removed",
      G_CALLBACK (sink_pads_removed), data);

  gst_bin_add_many (GST_BIN (pipeline), data->src, data->sink, NULL);

  /* request src pad using action */
  g_signal_emit_by_name (data->src, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_DATA, NULL, GST_PAD_SRC, &data->data_src);
  fail_if (data->data_src == NULL);

  GST_DEBUG ("Data pad name %s", data->data_src);

  g_object_set (G_OBJECT (data->src), "data", TRUE, NULL);
  g_object_set (G_OBJECT (data->sink), "data", TRUE, NULL);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);
  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_free (padname);
  g_main_loop_unref (loop);
  kms_connect_data_destroy (data);
}

GST_END_TEST
GST_START_TEST (connect_chain_of_elements)
{
  gchar *padname = NULL;
  KmsConnectData *data1, *data2;
  gchar *filter_factory;
  GstBus *bus;

  loop = g_main_loop_new (NULL, TRUE);
  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  data1 = kms_connect_data_create (0);
  data2 = kms_connect_data_create (MAX_CHECKS);
  data2->data_probe = (KmsProbeType) data_probe_cb;
  data2->audio_probe = (KmsProbeType) audio_probe_cb;
  data2->video_probe = (KmsProbeType) video_probe_cb;

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  data1->src = gst_element_factory_make ("dummysrc", NULL);
  data1->sink = gst_element_factory_make ("filterelement", NULL);

  data2->src = data1->sink;
  data2->sink = gst_element_factory_make ("dummysink", NULL);

  g_signal_connect (data1->src, "pad-added", G_CALLBACK (src_pads_added),
      data1);
  g_signal_connect (data1->sink, "pad-added", G_CALLBACK (sink_pads_added),
      data1);

  g_signal_connect (data2->src, "pad-added", G_CALLBACK (src_pads_added),
      data2);
  g_signal_connect (data2->sink, "pad-added", G_CALLBACK (sink_pads_added),
      data2);
  g_signal_connect (data2->sink, "pad-removed", G_CALLBACK (sink_pads_removed),
      data2);

  gst_bin_add_many (GST_BIN (pipeline), data1->src, data1->sink, data2->sink,
      NULL);

  /*******************************/
  /* Connect dummysrc to filter  */
  /*******************************/

  /* request src pad using action */
  g_signal_emit_by_name (data1->src, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &data1->video_src);
  fail_if (data1->video_src == NULL);
  g_signal_emit_by_name (data1->src, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_AUDIO, NULL, GST_PAD_SRC, &data1->audio_src);
  fail_if (data1->audio_src == NULL);
  g_signal_emit_by_name (data1->src, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_DATA, NULL, GST_PAD_SRC, &data1->data_src);
  fail_if (data1->data_src == NULL);

  GST_DEBUG ("Video pad name %s", data1->video_src);
  GST_DEBUG ("Audio pad name %s", data1->audio_src);
  GST_DEBUG ("Data pad name %s", data1->data_src);

  filter_factory = "videoflip";
  GST_DEBUG ("Setting property uri to : %s", filter_factory);
  g_object_set (G_OBJECT (data1->sink), "filter_factory", filter_factory, NULL);

  g_object_set (G_OBJECT (data1->src), "video", TRUE, "audio", TRUE, "data",
      TRUE, NULL);

  /*******************************/
  /* Connect filter to dummysink */
  /*******************************/

  /* request src pad using action */
  g_signal_emit_by_name (data2->src, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &data2->video_src);
  fail_if (data2->video_src == NULL);
  g_signal_emit_by_name (data2->src, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_AUDIO, NULL, GST_PAD_SRC, &data2->audio_src);
  fail_if (data2->audio_src == NULL);
  g_signal_emit_by_name (data2->src, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_DATA, NULL, GST_PAD_SRC, &data2->data_src);
  fail_if (data2->data_src == NULL);

  GST_DEBUG ("Video pad name %s", data2->video_src);
  GST_DEBUG ("Audio pad name %s", data2->audio_src);
  GST_DEBUG ("Data pad name %s", data2->data_src);

  g_object_set (G_OBJECT (data2->sink), "video", TRUE, "audio", TRUE, "data",
      TRUE, NULL);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  gst_element_sync_state_with_parent (data1->src);
  gst_element_sync_state_with_parent (data1->sink);
  gst_element_sync_state_with_parent (data2->sink);

  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_free (padname);
  g_main_loop_unref (loop);
  kms_connect_data_destroy (data1);
  kms_connect_data_destroy (data2);
}

GST_END_TEST
GST_START_TEST (request_data_sink_pad)
{
  gchar *padname = NULL;
  KmsConnectData *data;
  GstBus *bus;

  data = kms_connect_data_create (1);
  data->data_probe = (KmsProbeType) data_probe_cb;
  data->audio_probe = (KmsProbeType) audio_probe_cb;
  data->video_probe = (KmsProbeType) video_probe_cb;

  /* Only tests data */
  data->video_checks = data->audio_checks = 0;

  loop = g_main_loop_new (NULL, TRUE);
  pipeline = gst_pipeline_new (__FUNCTION__);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  data->src = gst_element_factory_make ("dummysrc", NULL);
  data->sink = gst_element_factory_make ("dummysink", NULL);
  g_signal_connect (data->src, "pad-added", G_CALLBACK (src_pads_added), data);
  g_signal_connect (data->sink, "pad-added",
      G_CALLBACK (sink_pads_added), data);

  g_signal_connect (data->sink, "pad-removed",
      G_CALLBACK (sink_pads_removed), data);

  gst_bin_add_many (GST_BIN (pipeline), data->src, data->sink, NULL);

  /* request src pad using action */
  g_signal_emit_by_name (data->src, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_DATA, NULL, GST_PAD_SRC, &data->data_src);
  fail_if (data->data_src == NULL);

  GST_DEBUG ("Data pad name: %s", data->data_src);

  /* request sink pad using action */
  g_signal_emit_by_name (data->sink, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_DATA, "test", GST_PAD_SINK, &padname);
  fail_if (padname == NULL);

  GST_DEBUG ("Data pad name: %s", padname);

  g_object_set (G_OBJECT (data->src), "data", TRUE, NULL);

  g_timeout_add_seconds (4, print_timedout_pipeline, NULL);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);
  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_free (padname);
  g_main_loop_unref (loop);
  kms_connect_data_destroy (data);
}

GST_END_TEST
/*
 * End of test cases
 */
static Suite *
pads_connection_suite (void)
{
  Suite *s = suite_create ("pad_connection");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);

  tcase_add_test (tc_chain, request_video_src_pad_pending);
  tcase_add_test (tc_chain, request_audio_src_pad_pending);
  tcase_add_test (tc_chain, request_data_src_pad_pending);
  tcase_add_test (tc_chain, request_video_src_pad);
  tcase_add_test (tc_chain, request_audio_src_pad);
  tcase_add_test (tc_chain, request_data_src_pad);
  tcase_add_test (tc_chain, request_video_src_pad_connection);
  tcase_add_test (tc_chain, disconnect_requested_src_pad_not_linked);
  tcase_add_test (tc_chain, disconnect_requested_src_pad_linked);
  tcase_add_test (tc_chain, connection_of_elements);
  tcase_add_test (tc_chain, connection_of_elements_data);
  tcase_add_test (tc_chain, connect_chain_of_elements);
  tcase_add_test (tc_chain,
      disconnect_requested_src_pad_linked_with_buffer_injector);
  tcase_add_test (tc_chain, request_data_sink_pad);

  return s;
}

GST_CHECK_MAIN (pads_connection);
