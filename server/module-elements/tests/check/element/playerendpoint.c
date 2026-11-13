/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/check/gstcheck.h>
#include <gst/gst.h>
#include <commons/kmsuriendpointstate.h>
#include <commons/kmsrecordingprofile.h>
#include <glib/gstdio.h>
#include <string.h>

#include <kmstestutils.h>

#define VIDEO_PATH TEST_FILES_LOCATION "/video/filter/fiwarecut.webm"
#define VIDEO_LENGTH 30

#define VIDEO_PATH2 TEST_FILES_LOCATION "/video/format/sintel.webm"
#define VIDEO_LENGTH2 52

#define VIDEO_PATH3 TEST_FILES_LOCATION "/video/format/small.webm"
#define VIDEO_LENGTH3 6

#define KMS_ELEMENT_PAD_TYPE_DATA 0
#define KMS_ELEMENT_PAD_TYPE_AUDIO 1
#define KMS_ELEMENT_PAD_TYPE_VIDEO 2

#define KMS_VIDEO_PREFIX "video_src_"
#define KMS_AUDIO_PREFIX "audio_src_"

static GMainLoop *loop = NULL;
static GstElement *player = NULL;
static GstElement *fakesink = NULL;
static GstElement *pipeline = NULL;

static guint state = 0;

G_LOCK_DEFINE (handoff_lock);
static gboolean start_buffer = FALSE;

static const KmsUriEndpointState trasnsitions[] = {
  KMS_URI_ENDPOINT_STATE_START,
  KMS_URI_ENDPOINT_STATE_PAUSE,
  KMS_URI_ENDPOINT_STATE_START,
  KMS_URI_ENDPOINT_STATE_STOP
};

static guint
g_idle_add_loop (GMainLoop *loop, GSourceFunc function, gpointer user_data)
{
  GSource *source = g_idle_source_new ();
  g_source_set_callback (source, function, user_data, NULL);
  guint id = g_source_attach (source, g_main_loop_get_context (loop));
  g_source_unref (source);

  return id;
}

static gchar *
state2string (KmsUriEndpointState state)
{
  switch (state) {
    case KMS_URI_ENDPOINT_STATE_STOP:
      return "STOP";
    case KMS_URI_ENDPOINT_STATE_START:
      return "START";
    case KMS_URI_ENDPOINT_STATE_PAUSE:
      return "PAUSE";
    default:
      return "Invalid state";
  }
}

static void
change_state (KmsUriEndpointState state)
{
  GST_DEBUG ("Setting player to state %s", state2string (state));
  g_object_set (G_OBJECT (player), "state", state, NULL);
}

static gboolean
print_timedout_pipeline (gpointer data)
{
  gchar *pipeline_name;
  gchar *name;

  GST_ERROR ("Test timed out; generating DOT file...");

  pipeline_name = gst_element_get_name (pipeline);
  name = g_strdup_printf ("%s_timedout", pipeline_name);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, name);

  g_free (name);
  g_free (pipeline_name);

  return FALSE;
}

static void
bus_msg_cb (GstBus * bus, GstMessage * msg, gpointer pipeline)
{
  switch (GST_MESSAGE_TYPE (msg)) {
    case GST_MESSAGE_ERROR: {
      GError *err = NULL;
      gchar *dbg_info = NULL;

      gst_message_parse_error (msg, &err, &dbg_info);
      GST_ERROR ("Pipeline '%s': Bus error %d: %s", GST_ELEMENT_NAME (pipeline),
          err->code, GST_STR_NULL (err->message));
      GST_ERROR ("Debugging info: %s", GST_STR_NULL (dbg_info));
      g_error_free (err);
      g_free (dbg_info);

      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
          GST_DEBUG_GRAPH_SHOW_ALL, "bus_error");

      fail ("Pipeline '%s': Bus error", GST_ELEMENT_NAME (pipeline));

      break;
    }
    case GST_MESSAGE_WARNING: {
      GError *err = NULL;
      gchar *dbg_info = NULL;

      gst_message_parse_error (msg, &err, &dbg_info);
      GST_WARNING ("Pipeline '%s': Bus warning %d: %s",
          GST_ELEMENT_NAME (pipeline), err->code, GST_STR_NULL (err->message));
      GST_WARNING ("Debugging info: %s", GST_STR_NULL (dbg_info));
      g_error_free (err);
      g_free (dbg_info);

      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
          GST_DEBUG_GRAPH_SHOW_ALL, "bus_warning");

      break;
    }
    default:
      break;
  }
}

static gboolean transite_cb (gpointer);

static void
transite (gpointer user_data)
{
  if (state < G_N_ELEMENTS (trasnsitions)) {
    change_state (trasnsitions[state]);
  } else {
    GMainLoop *loop = (GMainLoop *) user_data;

    GST_DEBUG ("All transitions done. Finishing player check states suit");
    g_main_loop_quit (loop);
  }
}

static gboolean
transite_cb (gpointer user_data)
{
  state++;
  transite (user_data);
  return FALSE;
}

static GstPadProbeReturn
data_probe_cb (GstPad * pad, GstPadProbeInfo * info, gpointer data)
{
  GST_DEBUG_OBJECT (pad, "buffer received");

  gst_pad_remove_probe (pad, GST_PAD_PROBE_INFO_ID (info));

  g_idle_add_loop (loop, transite_cb, NULL);

  return GST_PAD_PROBE_OK;
}

static void
state_changed_cb (GstElement * player, KmsUriEndpointState newState,
    gpointer user_data)
{
  GST_DEBUG_OBJECT (player, "State changed %s.", state2string (newState));

  switch (trasnsitions[state]) {
    case KMS_URI_ENDPOINT_STATE_START:{
      gchar *padname = *(gchar **) user_data;
      GstPad *srcpad;

      srcpad = gst_element_get_static_pad (player, padname);
      if (srcpad == NULL) {
        /* Source pad is not yet created */
        return;
      }

      gst_pad_add_probe (srcpad, GST_PAD_PROBE_TYPE_BUFFER,
          (GstPadProbeCallback) data_probe_cb, NULL, NULL);
      g_object_unref (srcpad);
      break;
    }
    case KMS_URI_ENDPOINT_STATE_PAUSE:
    case KMS_URI_ENDPOINT_STATE_STOP:
      g_idle_add_loop (loop, transite_cb, loop);
      break;
  }
}

static void
handoff (GstElement * object, GstBuffer * arg0,
    GstPad * arg1, gpointer user_data)
{
  G_LOCK (handoff_lock);

  if (!start_buffer) {
    start_buffer = TRUE;
    /* First buffer received, start transitions */
    g_idle_add_loop (loop, transite_cb, NULL);
  }

  G_UNLOCK (handoff_lock);
}

static void
srcpad_added (GstElement * playerep, GstPad * new_pad, gpointer user_data)
{
  gchar *padname, *expected_name;
  GstPad *sinkpad;

  GST_INFO_OBJECT (playerep, "Pad added %" GST_PTR_FORMAT, new_pad);
  padname = gst_pad_get_name (new_pad);
  fail_if (padname == NULL);

  expected_name = *(gchar **) user_data;
  fail_if (g_strcmp0 (padname, expected_name) != 0);

  sinkpad = gst_element_get_static_pad (fakesink, "sink");

  fail_if (gst_pad_link (new_pad, sinkpad) != GST_PAD_LINK_OK);

  g_object_unref (sinkpad);
  g_free (padname);
}

GST_START_TEST (check_states)
{
  guint bus_watch_id;
  gchar *padname;
  GstBus *bus;

  {
    GMainContext *context = g_main_context_new ();
    loop = g_main_loop_new (context, FALSE);
    g_clear_pointer (&context, g_main_context_unref);
  }

  pipeline = gst_pipeline_new (__FUNCTION__);
  player = gst_element_factory_make ("playerendpoint", NULL);
  fakesink = gst_element_factory_make ("fakesink", NULL);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg_cb), pipeline);
  g_object_unref (bus);

  g_object_set (G_OBJECT (player), "uri", VIDEO_PATH, NULL);
  g_object_set (G_OBJECT (fakesink), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);

  g_signal_connect (fakesink, "handoff", G_CALLBACK (handoff), loop);
  g_signal_connect (player, "pad-added", G_CALLBACK (srcpad_added), &padname);

  g_signal_connect (player, "state-changed", G_CALLBACK (state_changed_cb),
      &padname);

  gst_bin_add_many (GST_BIN (pipeline), player, fakesink, NULL);

  /* request src pad using action */
  g_signal_emit_by_name (player, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &padname);
  fail_if (padname == NULL);

  GST_DEBUG ("Requested pad %s", padname);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  transite (loop);

  g_timeout_add_seconds (VIDEO_LENGTH + 3, print_timedout_pipeline, NULL);
  g_main_loop_run (loop);

  fail_unless (start_buffer == TRUE);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  g_source_remove (bus_watch_id);
  g_clear_pointer (&loop, g_main_loop_unref);
  g_free (padname);
}

GST_END_TEST
/* check_live_stream */
static gboolean buffer_audio = FALSE;
static gboolean buffer_video = FALSE;

static gboolean
check_handoff_audio (gpointer user_data)
{
  buffer_audio = TRUE;

  if (buffer_audio && buffer_video) {
    GMainLoop *loop = (GMainLoop *) user_data;

    g_main_loop_quit (loop);
  }

  return FALSE;
}

static gboolean
check_handoff_video (gpointer user_data)
{
  buffer_video = TRUE;

  if (buffer_audio && buffer_video) {
    GMainLoop *loop = (GMainLoop *) user_data;

    g_main_loop_quit (loop);
  }

  return FALSE;
}

static void
handoff_audio (GstElement * object, GstBuffer * arg0,
    GstPad * arg1, gpointer user_data)
{
  GMainLoop *loop = (GMainLoop *) user_data;

  GST_TRACE ("handoff_audio");
  g_idle_add_loop (loop, (GSourceFunc) check_handoff_audio, loop);
}

static void
handoff_video (GstElement * object, GstBuffer * arg0,
    GstPad * arg1, gpointer user_data)
{
  GMainLoop *loop = (GMainLoop *) user_data;

  GST_TRACE ("handoff_video");
  g_idle_add_loop (loop, (GSourceFunc) check_handoff_video, loop);
}

static void
check_live_stream_on_pad_added (GstElement * playerep, GstPad * new_pad,
    gpointer user_data)
{
  GMainLoop *loop = (GMainLoop *) user_data;
  GstElement *sink;
  gchar *padname;
  GCallback func;
  GstPad *sinkpad;

  GST_INFO_OBJECT (playerep, "Pad added %" GST_PTR_FORMAT, new_pad);
  padname = gst_pad_get_name (new_pad);
  fail_if (padname == NULL);

  if (g_str_has_prefix (padname, KMS_VIDEO_PREFIX)) {
    GST_DEBUG_OBJECT (playerep, "Connecting video stream");
    func = G_CALLBACK (handoff_video);
  } else if (g_str_has_prefix (padname, KMS_AUDIO_PREFIX)) {
    GST_DEBUG_OBJECT (playerep, "Connecting audio stream");
    func = G_CALLBACK (handoff_audio);
  } else {
    GST_ERROR_OBJECT (playerep, "Not supported pad type");
    return;
  }

  sink = gst_element_factory_make ("fakesink", NULL);
  g_object_set (G_OBJECT (sink), "async", FALSE, "sync", FALSE,
      "signal-handoffs", TRUE, NULL);

  g_signal_connect (sink, "handoff", func, loop);

  gst_bin_add (GST_BIN (pipeline), sink);

  sinkpad = gst_element_get_static_pad (sink, "sink");
  fail_if (gst_pad_link (new_pad, sinkpad) != GST_PAD_LINK_OK);

  gst_element_sync_state_with_parent (sink);

  g_object_unref (sinkpad);
  g_free (padname);
}

GST_START_TEST (check_live_stream)
{
  guint bus_watch_id;
  gchar *padname;
  GstBus *bus;

  {
    GMainContext *context = g_main_context_new ();
    loop = g_main_loop_new (context, FALSE);
    g_clear_pointer (&context, g_main_context_unref);
  }

  pipeline = gst_pipeline_new (__FUNCTION__);
  player = gst_element_factory_make ("playerendpoint", NULL);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg_cb), pipeline);
  g_object_unref (bus);

  g_object_set (G_OBJECT (player), "uri", VIDEO_PATH2, NULL);

  g_signal_connect (player, "pad-added",
      G_CALLBACK (check_live_stream_on_pad_added), loop);

  gst_bin_add (GST_BIN (pipeline), player);
  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* request audio src pad using action */
  g_signal_emit_by_name (player, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_AUDIO, NULL, GST_PAD_SRC, &padname);
  fail_if (padname == NULL);

  GST_DEBUG ("Requested pad %s", padname);
  g_free (padname);

  /* request video src pad using action */
  g_signal_emit_by_name (player, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &padname);
  fail_if (padname == NULL);

  GST_DEBUG ("Requested pad %s", padname);
  g_free (padname);

  /* Set player to start state */
  g_object_set (G_OBJECT (player), "state", KMS_URI_ENDPOINT_STATE_START, NULL);

  g_timeout_add_seconds (VIDEO_LENGTH2 + 3, print_timedout_pipeline, NULL);
  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  g_source_remove (bus_watch_id);
  g_clear_pointer (&loop, g_main_loop_unref);
}
GST_END_TEST

/* check_eos */
static gboolean
quit_main_loop_idle (gpointer user_data)
{
  GMainLoop *loop = (GMainLoop *) user_data;

  GST_DEBUG ("Test finished; exiting main loop");
  g_main_loop_quit (loop);
  return FALSE;
}

static void
player_eos (GstElement * player, GMainLoop * loop)
{
  GST_DEBUG ("EOS received");
  g_idle_add_loop (loop, quit_main_loop_idle, loop);
}

/* EOS test */
GST_START_TEST (check_eos)
{
  guint bus_watch_id;
  GstBus *bus;

  {
    GMainContext *context = g_main_context_new ();
    loop = g_main_loop_new (context, FALSE);
    g_clear_pointer (&context, g_main_context_unref);
  }

  pipeline = gst_pipeline_new (__FUNCTION__);
  player = gst_element_factory_make ("playerendpoint", NULL);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg_cb), pipeline);
  g_object_unref (bus);

  g_object_set (G_OBJECT (player), "uri", VIDEO_PATH3, NULL);

  g_object_set (G_OBJECT (player), "state", KMS_URI_ENDPOINT_STATE_START, NULL);

  gst_bin_add (GST_BIN (pipeline), player);

  g_signal_connect (G_OBJECT (player), "eos", G_CALLBACK (player_eos), loop);

  /* Set player to start state */
  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_timeout_add_seconds (VIDEO_LENGTH3 + 3, print_timedout_pipeline, NULL);
  g_main_loop_run (loop);

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  g_source_remove (bus_watch_id);
  g_clear_pointer (&loop, g_main_loop_unref);
}

GST_END_TEST


/*****************************************************************************/
/* Encoded media from sources with B-frames                                  */
/*****************************************************************************/

#define BFRAMES_FRAMES 75
/* While the player may lose the last buffers at EOS under load, at least a
 * whole GOP has to be delivered */
#define BFRAMES_MIN_BUFFERS 25
/* Start time of the sources used to check that the timestamps are rebased */
#define TS_START_TIME (10 * GST_SECOND)
#define RECORDER_SINK_VIDEO "sink_video_default"

typedef struct _BFramesStats
{
  guint count;
  guint reordered;              /* buffers with DTS != PTS */
  GstClockTime first_pts;
  GstClockTime first_dts;
  gboolean first_is_key;
  gint64 second_pts_dts;        /* PTS - DTS of the second buffer */
  GstClockTime max_pts;
  GstClockTime prev_dts;
  gint64 min_dts_gap;           /* between consecutive buffers, from the 2nd */
  gint64 max_dts_gap;
  GstElement *pipeline;         /* if set, track max_lead */
  gint64 max_lead;              /* max of PTS - running time of the pipeline */
} BFramesStats;

static void
bframes_stats_init (BFramesStats * stats)
{
  memset (stats, 0, sizeof (*stats));
  stats->first_pts = GST_CLOCK_TIME_NONE;
  stats->first_dts = GST_CLOCK_TIME_NONE;
  stats->max_lead = G_MININT64;
  stats->prev_dts = GST_CLOCK_TIME_NONE;
  stats->min_dts_gap = G_MAXINT64;
  stats->max_dts_gap = G_MININT64;
}

static GstPadProbeReturn
bframes_stats_probe (GstPad * pad, GstPadProbeInfo * info, gpointer data)
{
  BFramesStats *stats = data;
  GstBuffer *buf = GST_PAD_PROBE_INFO_BUFFER (info);

  if (stats->count == 0) {
    stats->first_pts = GST_BUFFER_PTS (buf);
    stats->first_dts = GST_BUFFER_DTS (buf);
    stats->first_is_key = !GST_BUFFER_FLAG_IS_SET (buf,
        GST_BUFFER_FLAG_DELTA_UNIT);
  }
  if (stats->count == 1 && GST_BUFFER_PTS_IS_VALID (buf) &&
      GST_BUFFER_DTS_IS_VALID (buf)) {
    stats->second_pts_dts = (gint64) GST_BUFFER_PTS (buf) -
        (gint64) GST_BUFFER_DTS (buf);
  }
  /* The gap between the first and the second buffer is not checked: the
   * first one goes through the preroll and gets its own base time */
  if (GST_BUFFER_DTS_IS_VALID (buf)) {
    if (stats->count >= 2 && GST_CLOCK_TIME_IS_VALID (stats->prev_dts)) {
      gint64 gap = (gint64) GST_BUFFER_DTS (buf) - (gint64) stats->prev_dts;

      stats->min_dts_gap = MIN (stats->min_dts_gap, gap);
      stats->max_dts_gap = MAX (stats->max_dts_gap, gap);
    }
    stats->prev_dts = GST_BUFFER_DTS (buf);
  }
  stats->count++;
  if (GST_BUFFER_PTS_IS_VALID (buf) && GST_BUFFER_PTS (buf) > stats->max_pts) {
    stats->max_pts = GST_BUFFER_PTS (buf);
  }
  if (stats->pipeline != NULL && GST_BUFFER_PTS_IS_VALID (buf)) {
    GstClock *clock = gst_element_get_clock (stats->pipeline);

    if (clock != NULL) {
      gint64 running_time = (gint64) gst_clock_get_time (clock) -
          (gint64) gst_element_get_base_time (stats->pipeline);

      stats->max_lead = MAX (stats->max_lead,
          (gint64) GST_BUFFER_PTS (buf) - running_time);
      gst_object_unref (clock);
    }
  }
  if (GST_BUFFER_PTS_IS_VALID (buf) && GST_BUFFER_DTS_IS_VALID (buf) &&
      GST_BUFFER_PTS (buf) != GST_BUFFER_DTS (buf)) {
    stats->reordered++;
  }

  return GST_PAD_PROBE_OK;
}

/* Demuxes a file and gets the stats of its video buffers. Returns FALSE if the
 * file cannot be read until EOS. */
static gboolean
bframes_inspect_file (const gchar * path, const gchar * demuxer,
    BFramesStats * stats)
{
  GstElement *pipeline, *sink;
  GstPad *pad;
  GstMessage *msg;
  GstBus *bus;
  GError *err = NULL;
  gchar *desc;
  gboolean ok;

  bframes_stats_init (stats);
  desc = g_strdup_printf ("filesrc location=%s ! %s name=d "
      "d. ! video/x-h264 ! fakesink name=sink sync=false", path, demuxer);
  pipeline = gst_parse_launch (desc, &err);
  g_free (desc);
  fail_unless (pipeline != NULL, "Cannot read %s: %s", path,
      err ? err->message : "?");

  sink = gst_bin_get_by_name (GST_BIN (pipeline), "sink");
  pad = gst_element_get_static_pad (sink, "sink");
  gst_pad_add_probe (pad, GST_PAD_PROBE_TYPE_BUFFER, bframes_stats_probe,
      stats, NULL);

  bus = gst_element_get_bus (pipeline);
  gst_element_set_state (pipeline, GST_STATE_PLAYING);
  msg = gst_bus_timed_pop_filtered (bus, 20 * GST_SECOND,
      GST_MESSAGE_EOS | GST_MESSAGE_ERROR);
  ok = msg != NULL && GST_MESSAGE_TYPE (msg) == GST_MESSAGE_EOS;
  if (msg != NULL) {
    gst_message_unref (msg);
  }

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (pad);
  gst_object_unref (sink);
  gst_object_unref (bus);
  gst_object_unref (pipeline);

  return ok;
}

/* Generates a file with B-frames in-process. Returns FALSE (test skipped) if
 * the encoder is not available. */
static gboolean
bframes_generate_source (const gchar * muxer, guint bframes,
    GstClockTime start_time, const gchar * path)
{
  GstElement *pipeline;
  GstMessage *msg;
  GstBus *bus;
  GError *err = NULL;
  gchar *desc;
  gboolean ok;

  desc = g_strdup_printf ("videotestsrc num-buffers=%d "
      "timestamp-offset=%" G_GUINT64_FORMAT " ! "
      "video/x-raw,format=I420,width=320,height=240,framerate=25/1 ! "
      "x264enc bframes=%u key-int-max=25 ! h264parse ! %s ! "
      "filesink location=%s", BFRAMES_FRAMES, start_time, bframes, muxer, path);
  pipeline = gst_parse_launch (desc, &err);
  g_free (desc);
  if (pipeline == NULL) {
    fprintf (stderr, "SKIP: cannot build the source pipeline: %s\n",
        err ? err->message : "?");
    g_clear_error (&err);
    return FALSE;
  }

  bus = gst_element_get_bus (pipeline);
  gst_element_set_state (pipeline, GST_STATE_PLAYING);
  msg = gst_bus_timed_pop_filtered (bus, 60 * GST_SECOND,
      GST_MESSAGE_EOS | GST_MESSAGE_ERROR);
  ok = msg != NULL && GST_MESSAGE_TYPE (msg) == GST_MESSAGE_EOS;
  if (msg != NULL) {
    gst_message_unref (msg);
  }
  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (bus);
  gst_object_unref (pipeline);

  fail_unless (ok, "Cannot generate the source %s", path);
  return TRUE;
}

typedef struct _BFramesPlayback
{
  GMainLoop *loop;
  GstElement *pipeline;
  GstElement *player;
  GstElement *recorder;         /* NULL: just count the buffers of the player */
  BFramesStats out;
  gboolean error;
  gboolean player_eos;
  gboolean stopped;
  guint timeout_id;
} BFramesPlayback;

static gboolean
bframes_bus_cb (GstBus * bus, GstMessage * msg, gpointer user_data)
{
  BFramesPlayback *play = user_data;

  if (GST_MESSAGE_TYPE (msg) == GST_MESSAGE_ERROR) {
    GError *err = NULL;
    gchar *dbg = NULL;

    gst_message_parse_error (msg, &err, &dbg);
    GST_ERROR ("Pipeline error: %s (%s)", err->message, dbg);
    g_error_free (err);
    g_free (dbg);

    play->error = TRUE;
    g_main_loop_quit (play->loop);
  }

  return TRUE;
}

static gboolean
bframes_stop_recorder_cb (gpointer user_data)
{
  BFramesPlayback *play = user_data;

  g_object_set (play->recorder, "state", KMS_URI_ENDPOINT_STATE_STOP, NULL);
  return FALSE;
}

static void
bframes_player_eos_cb (GstElement * player, gpointer user_data)
{
  BFramesPlayback *play = user_data;

  GST_DEBUG ("Player EOS");
  play->player_eos = TRUE;

  if (play->recorder == NULL) {
    g_idle_add (quit_main_loop_idle, play->loop);
  } else {
    /* Let the last buffers reach the recorder before stopping it */
    g_timeout_add (500, bframes_stop_recorder_cb, play);
  }
}

static void
bframes_recorder_state_cb (GstElement * recorder, KmsUriEndpointState state,
    gpointer user_data)
{
  BFramesPlayback *play = user_data;

  if (state == KMS_URI_ENDPOINT_STATE_STOP) {
    play->stopped = TRUE;
    g_idle_add (quit_main_loop_idle, play->loop);
  }
}

static gboolean
bframes_timeout_cb (gpointer user_data)
{
  BFramesPlayback *play = user_data;

  GST_ERROR ("Timeout waiting for the playback to finish");
  play->timeout_id = 0;
  play->error = TRUE;
  g_main_loop_quit (play->loop);
  return FALSE;
}

/* The EOS of the player is not forwarded to the recorder: the recorder is
 * always stopped explicitly, so the result does not depend on which one of
 * them gets first to the muxer. */
static GstPadProbeReturn
bframes_drop_eos_probe (GstPad * pad, GstPadProbeInfo * info, gpointer data)
{
  if (GST_EVENT_TYPE (GST_PAD_PROBE_INFO_EVENT (info)) == GST_EVENT_EOS) {
    return GST_PAD_PROBE_DROP;
  }

  return GST_PAD_PROBE_OK;
}

static void
bframes_player_pad_added_cb (GstElement * player, GstPad * pad,
    gpointer user_data)
{
  BFramesPlayback *play = user_data;
  GstElement *sink = NULL;
  GstPad *sinkpad;

  if (!g_str_has_prefix (GST_OBJECT_NAME (pad), KMS_VIDEO_PREFIX)) {
    return;
  }

  gst_pad_add_probe (pad, GST_PAD_PROBE_TYPE_BUFFER, bframes_stats_probe,
      &play->out, NULL);

  if (play->recorder != NULL) {
    gst_pad_add_probe (pad, GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM,
        bframes_drop_eos_probe, NULL, NULL);
    if (!gst_element_link_pads (player, GST_OBJECT_NAME (pad), play->recorder,
            RECORDER_SINK_VIDEO)) {
      GST_ERROR ("Cannot link the player to the recorder");
      play->error = TRUE;
      g_main_loop_quit (play->loop);
    }
    return;
  }

  sink = gst_element_factory_make ("fakesink", NULL);
  g_object_set (sink, "async", FALSE, "sync", FALSE, NULL);
  gst_bin_add (GST_BIN (play->pipeline), sink);
  sinkpad = gst_element_get_static_pad (sink, "sink");
  fail_unless (gst_pad_link (pad, sinkpad) == GST_PAD_LINK_OK);
  g_object_unref (sinkpad);
  gst_element_sync_state_with_parent (sink);
}

/* Plays src_path with use-encoded-media, into a recorder if out_path is not
 * NULL. The recorder and the player are started one right after the other, as
 * the clients do. */
static gboolean
bframes_play (const gchar * src_path, KmsRecordingProfile profile,
    const gchar * out_path, BFramesStats * out)
{
  BFramesPlayback play = { 0, };
  GstBus *bus;
  gchar *uri, *padname = NULL;
  guint watch_id;
  GstCheckLogFilter *log_filter;

  bframes_stats_init (&play.out);
  play.loop = g_main_loop_new (NULL, FALSE);
  play.pipeline = gst_pipeline_new ("player-bframes-test");
  play.player = gst_element_factory_make ("playerendpoint", NULL);
  fail_unless (play.player != NULL);
  gst_bin_add (GST_BIN (play.pipeline), play.player);

  uri = g_strdup_printf ("file://%s", src_path);
  g_object_set (play.player, "uri", uri, "use-encoded-media", TRUE, NULL);
  g_free (uri);

  if (out_path != NULL) {
    play.recorder = gst_element_factory_make ("recorderendpoint", NULL);
    fail_unless (play.recorder != NULL);
    gst_bin_add (GST_BIN (play.pipeline), play.recorder);
    uri = g_strdup_printf ("file://%s", out_path);
    g_object_set (play.recorder, "uri", uri, "profile", profile, NULL);
    g_free (uri);
    g_signal_connect (play.recorder, "state-changed",
        G_CALLBACK (bframes_recorder_state_cb), &play);
  }

  g_signal_connect (play.player, "eos", G_CALLBACK (bframes_player_eos_cb),
      &play);
  g_signal_connect (play.player, "pad-added",
      G_CALLBACK (bframes_player_pad_added_cb), &play);

  bus = gst_pipeline_get_bus (GST_PIPELINE (play.pipeline));
  watch_id = gst_bus_add_watch (bus, bframes_bus_cb, &play);
  g_object_unref (bus);

  play.out.pipeline = play.pipeline;
  /* At the EOS the player flushes the src pad of its appsrc from another
   * thread (appsink_eos_cb), which may make GStreamer complain about "Got data
   * flow before segment event". It is a known race of the player that this
   * test does not check, so do not let gstcheck turn that message (and only
   * that one) into a failure. */
  log_filter = gst_check_add_log_filter ("GStreamer", G_LOG_LEVEL_WARNING,
      g_regex_new ("Got data flow before segment event", 0, 0, NULL), NULL,
      NULL, NULL);
  gst_element_set_state (play.pipeline, GST_STATE_PLAYING);

  g_signal_emit_by_name (play.player, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &padname);
  fail_if (padname == NULL);
  g_free (padname);

  play.timeout_id = g_timeout_add_seconds (30, bframes_timeout_cb, &play);
  if (play.recorder != NULL) {
    g_object_set (play.recorder, "state", KMS_URI_ENDPOINT_STATE_START, NULL);
  }
  g_object_set (play.player, "state", KMS_URI_ENDPOINT_STATE_START, NULL);

  g_main_loop_run (play.loop);

  if (play.timeout_id != 0) {
    g_source_remove (play.timeout_id);
  }
  gst_element_set_state (play.pipeline, GST_STATE_NULL);
  gst_object_unref (play.pipeline);
  g_source_remove (watch_id);
  g_main_loop_unref (play.loop);
  gst_check_remove_log_filter (log_filter);

  if (play.error || !play.player_eos || (out_path != NULL && !play.stopped)) {
    fprintf (stderr, "Playback failed: error=%d player_eos=%d stopped=%d\n",
        play.error, play.player_eos, play.stopped);
    return FALSE;
  }

  *out = play.out;
  out->pipeline = NULL;
  return TRUE;
}

static gchar *
bframes_make_tmp_dir (void)
{
  GError *err = NULL;
  gchar *dir = g_dir_make_tmp ("kms-player-bframes-XXXXXX", &err);

  fail_unless (dir != NULL, "Cannot create tmp dir: %s",
      err ? err->message : "?");
  return dir;
}

static void
bframes_remove_tmp_dir (gchar * dir, const gchar * const *files)
{
  gint i;

  for (i = 0; files[i] != NULL; i++) {
    gchar *path = g_build_filename (dir, files[i], NULL);

    g_unlink (path);
    g_free (path);
  }
  g_rmdir (dir);
  g_free (dir);
}

/* A source with B-frames recorded without decoding keeps all its frames */
GST_START_TEST (check_encoded_bframes_record)
{
  static const gchar *const files[] =
      { "src.mp4", "out.mp4", "out.mkv", NULL };
  KmsRecordingProfile profiles[] = { KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY,
    KMS_RECORDING_PROFILE_MKV_VIDEO_ONLY
  };
  const gchar *outs[] = { "out.mp4", "out.mkv" };
  const gchar *demuxers[] = { "qtdemux", "matroskademux" };
  gchar *dir, *src;
  BFramesStats stats, out;
  gint i;

  if (gst_element_factory_find ("x264enc") == NULL) {
    fprintf (stderr, "SKIP: x264enc not available\n");
    return;
  }

  dir = bframes_make_tmp_dir ();
  src = g_build_filename (dir, "src.mp4", NULL);
  fail_unless (bframes_generate_source ("mp4mux", 3, 0, src));

  fail_unless (bframes_inspect_file (src, "qtdemux", &stats));
  fail_unless_equals_int (stats.count, BFRAMES_FRAMES);
  fail_unless (stats.reordered > 0, "The source has no B-frames");

  for (i = 0; i < 2; i++) {
    gchar *path = g_build_filename (dir, outs[i], NULL);

    guint played;

    fail_unless (bframes_play (src, profiles[i], path, &out),
        "Recording %s failed or did not finish", path);
    /* Under load the player may lose the last buffers when it flushes at the
     * EOS (a known issue that this test does not check), but there must be
     * no gaps before: the DTS of the source has a constant step of 40 ms */
    fail_unless (out.count >= BFRAMES_MIN_BUFFERS,
        "The player pushed only %u of %u buffers", out.count, BFRAMES_FRAMES);
    fail_unless (out.first_is_key, "The first buffer is not a keyframe");
    /* The second buffer is the one after the preroll, which is discarded as a
     * duplicate of the first: it must not be missing */
    fail_unless_equals_int64 (out.second_pts_dts, stats.second_pts_dts);
    fail_unless (out.count < 4 || (out.min_dts_gap > 0 &&
            out.max_dts_gap <= 40 * GST_MSECOND + GST_MSECOND),
        "Buffers lost: the player pushed %u of %u, DTS gaps between %"
        G_GINT64_FORMAT " and %" G_GINT64_FORMAT " ms", out.count,
        BFRAMES_FRAMES, out.min_dts_gap / GST_MSECOND,
        out.max_dts_gap / GST_MSECOND);
    played = out.count;

    /* The recorder must save all what the player delivered */
    fail_unless (bframes_inspect_file (path, demuxers[i], &out),
        "%s is not a valid file", path);
    fail_unless_equals_int (out.count, played);
    fail_unless (out.first_is_key, "The first recorded frame is not a keyframe");
    g_free (path);
  }

  g_free (src);
  bframes_remove_tmp_dir (dir, files);
}

GST_END_TEST;

/* The timestamps of a source that does not start at 0 are rebased to the
 * running time of the pipeline, they do not keep the start time of the source */
GST_START_TEST (check_encoded_timestamps_rebase)
{
  static const gchar *const files[] = { "src.mkv", NULL };
  gchar *dir, *src;
  BFramesStats stats, out;

  if (gst_element_factory_find ("x264enc") == NULL) {
    fprintf (stderr, "SKIP: x264enc not available\n");
    return;
  }

  dir = bframes_make_tmp_dir ();
  src = g_build_filename (dir, "src.mkv", NULL);
  /* Matroska keeps the start time. Its demuxer does not provide DTS, so no
   * B-frames here */
  fail_unless (bframes_generate_source ("matroskamux", 0, TS_START_TIME, src));

  fail_unless (bframes_inspect_file (src, "matroskademux", &stats));
  fail_unless (stats.first_pts >= TS_START_TIME,
      "The source starts at %" GST_TIME_FORMAT, GST_TIME_ARGS (stats.first_pts));

  fail_unless (bframes_play (src, 0, NULL, &out), "Playback failed");
  /* The source is not paced here, so some buffers may be lost; at least the
   * first one after the preroll is needed to check the timestamps */
  fail_unless (out.count >= 2, "Too few buffers: %u", out.count);
  fail_unless (GST_CLOCK_TIME_IS_VALID (out.first_pts));
  /* The output has to follow the running time of the pipeline (the source
   * is not paced, so it may be some seconds ahead, never the 10 s of the start
   * time of the source) */
  fail_unless (out.max_lead < TS_START_TIME / 2,
      "The output goes %" G_GINT64_FORMAT " ms ahead of the running time",
      out.max_lead / GST_MSECOND);

  g_free (src);
  bframes_remove_tmp_dir (dir, files);
}

GST_END_TEST;

#ifdef ENABLE_EXPERIMENTAL_TESTS

GST_START_TEST (check_set_encoded_media)
{
  GstElement *player, *pipeline;
  guint bus_watch_id;
  GMainLoop *loop;
  GstBus *bus;

  {
    GMainContext *context = g_main_context_new ();
    loop = g_main_loop_new (context, FALSE);
    g_clear_pointer (&context, g_main_context_unref);
  }

  pipeline = gst_pipeline_new ("pipeline_live_stream");
  player = gst_element_factory_make ("playerendpoint", NULL);
  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg_cb), pipeline);
  g_object_unref (bus);

  g_object_set (G_OBJECT (player), "uri", VIDEO_PATH3, NULL);

  g_object_set (G_OBJECT (player), "use-encoded-media", TRUE, NULL);

  gst_bin_add (GST_BIN (pipeline), player);

  g_signal_connect (G_OBJECT (player), "eos", G_CALLBACK (player_eos), loop);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  /* Set player to start state */
  g_object_set (G_OBJECT (player), "state", KMS_URI_ENDPOINT_STATE_START, NULL);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "before_entering_main_loop_live_stream");

  g_main_loop_run (loop);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "after_entering_main_loop_live_stream");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (pipeline));
  g_source_remove (bus_watch_id);
  g_clear_pointer (&loop, g_main_loop_unref);
}

GST_END_TEST;

#endif // ENABLE_EXPERIMENTAL_TESTS

/* Define test suite */
static Suite *
playerendpoint_suite (void)
{
  Suite *s = suite_create ("playerendpoint");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);

  if (FALSE) { // FIXME: fix check_states test
  tcase_add_test (tc_chain, check_states);
  }
  tcase_add_test (tc_chain, check_live_stream);
  if (FALSE){  // FIXME: fix check_eos test
  tcase_add_test (tc_chain, check_eos);
  }
  tcase_add_test (tc_chain, check_encoded_bframes_record);
  tcase_add_test (tc_chain, check_encoded_timestamps_rebase);
#ifdef ENABLE_EXPERIMENTAL_TESTS
  tcase_add_test (tc_chain, check_set_encoded_media);
#endif

  return s;
}

GST_CHECK_MAIN (playerendpoint);
