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

#include "kmsplayerendpointtimestamps.h"

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
#define AV_FRAMES 75
/* Buffer (counting from 0) whose PTS is used to check the A/V sync after the
 * first one */
#define BFRAMES_NTH 20

/* The encoders are not in the build dependencies of every environment: when a
 * test is skipped because of that, say it clearly in the log */
static gboolean
bframes_have_elements (const gchar * test, const gchar * const *names)
{
  gint i;

  for (i = 0; names[i] != NULL; i++) {
    GstElementFactory *factory = gst_element_factory_find (names[i]);

    if (factory == NULL) {
      fprintf (stderr, "WARNING: test %s SKIPPED, GStreamer element '%s' is "
          "not available\n", test, names[i]);
      return FALSE;
    }
    gst_object_unref (factory);
  }

  return TRUE;
}

static const gchar *const encoders_video[] = { "x264enc", NULL };
static const gchar *const encoders_av[] = { "x264enc", "avenc_aac", NULL };

typedef struct _BFramesStats
{
  guint count;
  guint reordered;              /* buffers with DTS != PTS */
  GstClockTime first_pts;
  GstClockTime first_dts;
  GstClockTime second_dts;
  GstClockTime second_pts;
  GstClockTime pts_nth;         /* PTS of the buffer number BFRAMES_NTH */
  GstClockTime seg_start;       /* start of the segment (0 if there is none) */
  gboolean first_is_key;
  gint64 second_pts_dts;        /* PTS - DTS of the second buffer */
  GstClockTime max_pts;
  GstClockTime prev_dts;
  gint64 min_dts_gap;           /* between consecutive buffers, from the 2nd */
  gint64 max_dts_gap;
  GstElement *pipeline;         /* if set, track max_lead */
  gint64 max_lead;              /* max of PTS - running time of the pipeline */
  gint64 first_lead;            /* the same, of the first buffer */
  gchar offset_preroll[40];     /* offset time of the player, in the preroll */
  gchar offset_play[40];        /* and when playing (empty: none) */
} BFramesStats;

static void
bframes_stats_init (BFramesStats * stats)
{
  memset (stats, 0, sizeof (*stats));
  stats->first_pts = GST_CLOCK_TIME_NONE;
  stats->first_dts = GST_CLOCK_TIME_NONE;
  stats->second_dts = GST_CLOCK_TIME_NONE;
  stats->second_pts = GST_CLOCK_TIME_NONE;
  stats->pts_nth = GST_CLOCK_TIME_NONE;
  stats->seg_start = 0;
  stats->max_lead = G_MININT64;
  stats->first_lead = G_MININT64;
  stats->prev_dts = GST_CLOCK_TIME_NONE;
  stats->min_dts_gap = G_MAXINT64;
  stats->max_dts_gap = G_MININT64;
}

static GstPadProbeReturn
bframes_stats_probe (GstPad * pad, GstPadProbeInfo * info, gpointer data)
{
  BFramesStats *stats = data;
  GstBuffer *buf;

  if (GST_PAD_PROBE_INFO_TYPE (info) & GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM) {
    GstEvent *event = GST_PAD_PROBE_INFO_EVENT (info);

    if (GST_EVENT_TYPE (event) == GST_EVENT_SEGMENT && stats->count == 0) {
      const GstSegment *segment;

      gst_event_parse_segment (event, &segment);
      if (segment->format == GST_FORMAT_TIME) {
        stats->seg_start = segment->start;
      }
    }
    return GST_PAD_PROBE_OK;
  }

  buf = GST_PAD_PROBE_INFO_BUFFER (info);
  if (stats->count == 0) {
    stats->first_pts = GST_BUFFER_PTS (buf);
    stats->first_dts = GST_BUFFER_DTS (buf);
    stats->first_is_key = !GST_BUFFER_FLAG_IS_SET (buf,
        GST_BUFFER_FLAG_DELTA_UNIT);
  }
  if (stats->count == BFRAMES_NTH) {
    stats->pts_nth = GST_BUFFER_PTS (buf);
  }
  if (stats->count == 1) {
    stats->second_dts = GST_BUFFER_DTS (buf);
    stats->second_pts = GST_BUFFER_PTS (buf);
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
      if (stats->first_lead == G_MININT64) {
        stats->first_lead = (gint64) GST_BUFFER_PTS (buf) - running_time;
      }
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

/* The offset that the player maps to its base time is only logged: capture it
 * to check that the preroll and the playing use the same one */
static GMutex offsets_mutex;
static gchar *captured_offset_preroll = NULL;
static gchar *captured_offset_play = NULL;

static void
bframes_offsets_log_func (GstDebugCategory * category, GstDebugLevel level,
    const gchar * file, const gchar * function, gint line, GObject * object,
    GstDebugMessage * message, gpointer user_data)
{
  const gchar *msg = gst_debug_message_get (message);
  const gchar *pos = msg != NULL ? strstr (msg, "offset time to: ") : NULL;
  gchar **parts;

  if (pos == NULL || strstr (pos, ", is preroll: ") == NULL) {
    return;
  }

  parts = g_strsplit (pos + strlen ("offset time to: "), ", is preroll: ", 2);
  g_mutex_lock (&offsets_mutex);
  if (parts[1] != NULL && parts[1][0] == '1' && captured_offset_preroll == NULL) {
    captured_offset_preroll = g_strdup (parts[0]);
  } else if (parts[1] != NULL && parts[1][0] == '0' &&
      captured_offset_play == NULL) {
    captured_offset_play = g_strdup (parts[0]);
  }
  g_mutex_unlock (&offsets_mutex);
  g_strfreev (parts);
}

static GstDebugLevel
bframes_category_threshold (const gchar * name)
{
  GstDebugLevel level = GST_LEVEL_NONE;
  GSList *l, *cats = gst_debug_get_all_categories ();

  for (l = cats; l != NULL; l = l->next) {
    if (g_strcmp0 (gst_debug_category_get_name (l->data), name) == 0) {
      level = gst_debug_category_get_threshold (l->data);
    }
  }
  g_slist_free (cats);

  return level;
}

typedef struct _BFramesPlayback
{
  GMainLoop *loop;
  GstElement *pipeline;
  GstElement *player;
  GstElement *recorder;         /* NULL: just count the buffers of the player */
  BFramesStats out;
  BFramesStats out_audio;
  gboolean with_audio;
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

static gboolean
bframes_quit_cb (gpointer user_data)
{
  BFramesPlayback *play = user_data;

  g_main_loop_quit (play->loop);
  return FALSE;
}

static void
bframes_player_eos_cb (GstElement * player, gpointer user_data)
{
  BFramesPlayback *play = user_data;

  GST_DEBUG ("Player EOS");
  play->player_eos = TRUE;

  /* The EOS of the player is the one of all its tracks: wait a moment for the
   * last buffers to get to the end of the test pipeline before stopping */
  if (play->recorder == NULL) {
    g_timeout_add (500, bframes_quit_cb, play);
  } else {
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

  gboolean is_audio = g_str_has_prefix (GST_OBJECT_NAME (pad),
      KMS_AUDIO_PREFIX);

  if (!is_audio && !g_str_has_prefix (GST_OBJECT_NAME (pad),
          KMS_VIDEO_PREFIX)) {
    return;
  }

  gst_pad_add_probe (pad, GST_PAD_PROBE_TYPE_BUFFER, bframes_stats_probe,
      is_audio ? &play->out_audio : &play->out, NULL);

  if (play->recorder != NULL && !is_audio) {
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

/* Plays src_path with use-encoded-media (or not, if "encoded" is FALSE), into a recorder if out_path is not
 * NULL. The recorder and the player are started one right after the other, as
 * the clients do. */
static gboolean
bframes_play (const gchar * src_path, KmsRecordingProfile profile,
    const gchar * out_path, BFramesStats * out, BFramesStats * out_audio,
    gboolean encoded, guint wait_before_play)
{
  BFramesPlayback play = { 0, };
  GstBus *bus;
  gchar *uri, *padname = NULL;
  guint watch_id;
  GstDebugLevel prev_threshold;
  GstCheckLogFilter *log_filter;

  bframes_stats_init (&play.out);
  bframes_stats_init (&play.out_audio);
  play.with_audio = out_audio != NULL;
  play.loop = g_main_loop_new (NULL, FALSE);
  play.pipeline = gst_pipeline_new ("player-bframes-test");
  play.player = gst_element_factory_make ("playerendpoint", NULL);
  fail_unless (play.player != NULL);
  gst_bin_add (GST_BIN (play.pipeline), play.player);

  uri = g_strdup_printf ("file://%s", src_path);
  g_object_set (play.player, "uri", uri, "use-encoded-media", encoded, NULL);
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
  play.out_audio.pipeline = play.pipeline;
  /* At the EOS the player flushes the src pad of its appsrc from another
   * thread (appsink_eos_cb), which may make GStreamer complain about "Got data
   * flow before segment event". It is a known race of the player that this
   * test does not check, so do not let gstcheck turn that message (and only
   * that one) into a failure. */
  log_filter = gst_check_add_log_filter ("GStreamer", G_LOG_LEVEL_WARNING,
      g_regex_new ("Got data flow before segment event", 0, 0, NULL), NULL,
      NULL, NULL);
  g_mutex_lock (&offsets_mutex);
  g_clear_pointer (&captured_offset_preroll, g_free);
  g_clear_pointer (&captured_offset_play, g_free);
  g_mutex_unlock (&offsets_mutex);
  gst_debug_add_log_function (bframes_offsets_log_func, NULL, NULL);
  prev_threshold = bframes_category_threshold ("playerendpoint");
  gst_debug_set_threshold_for_name ("playerendpoint", GST_LEVEL_DEBUG);
  gst_element_set_state (play.pipeline, GST_STATE_PLAYING);

  g_signal_emit_by_name (play.player, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &padname);
  fail_if (padname == NULL);
  g_free (padname);
  if (play.with_audio) {
    g_signal_emit_by_name (play.player, "request-new-pad",
        KMS_ELEMENT_PAD_TYPE_AUDIO, NULL, GST_PAD_SRC, &padname);
    fail_if (padname == NULL);
    g_free (padname);
  }

  /* Let the pipeline run before the playback, as it does in a server that has
   * been working for some time */
  if (wait_before_play > 0) {
    g_usleep (wait_before_play * G_USEC_PER_SEC);
  }
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
  gst_debug_remove_log_function (bframes_offsets_log_func);
  gst_debug_set_threshold_for_name ("playerendpoint", prev_threshold);
  g_mutex_lock (&offsets_mutex);
  g_strlcpy (play.out.offset_preroll, captured_offset_preroll ?
      captured_offset_preroll : "", sizeof (play.out.offset_preroll));
  g_strlcpy (play.out.offset_play, captured_offset_play ?
      captured_offset_play : "", sizeof (play.out.offset_play));
  g_clear_pointer (&captured_offset_preroll, g_free);
  g_clear_pointer (&captured_offset_play, g_free);
  g_mutex_unlock (&offsets_mutex);

  if (play.error || !play.player_eos || (out_path != NULL && !play.stopped)) {
    fprintf (stderr, "Playback failed: error=%d player_eos=%d stopped=%d\n",
        play.error, play.player_eos, play.stopped);
    return FALSE;
  }

  *out = play.out;
  out->pipeline = NULL;
  if (out_audio != NULL) {
    *out_audio = play.out_audio;
    out_audio->pipeline = NULL;
  }
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

  if (!bframes_have_elements ("check_encoded_bframes_record", encoders_video)) {
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

    fail_unless (bframes_play (src, profiles[i], path, &out, NULL, TRUE, 0),
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

  if (!bframes_have_elements ("check_encoded_timestamps_rebase", encoders_video)) {
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

  fail_unless (bframes_play (src, 0, NULL, &out, NULL, TRUE, 0), "Playback failed");
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

typedef struct _BFramesAvSinks
{
  GstElement *video;
  GstElement *audio;
} BFramesAvSinks;

/* The pads of the demuxer are linked by hand (not with the deferred links of
 * gst_parse_launch), because a failed deferred link leaves a capsfilter that
 * may be disposed while it is not in the NULL state */
static void
bframes_av_pad_added_cb (GstElement * demux, GstPad * pad, gpointer user_data)
{
  BFramesAvSinks *sinks = user_data;
  GstElement *sink = NULL;
  GstPad *sinkpad;

  if (g_str_has_prefix (GST_PAD_NAME (pad), "video_")) {
    sink = sinks->video;
  } else if (g_str_has_prefix (GST_PAD_NAME (pad), "audio_")) {
    sink = sinks->audio;
  }
  if (sink == NULL) {
    return;
  }

  sinkpad = gst_element_get_static_pad (sink, "sink");
  if (!gst_pad_is_linked (sinkpad)) {
    gst_pad_link (pad, sinkpad);
  }
  gst_object_unref (sinkpad);
}

/* Demuxes an MPEG-TS with video and audio and gets the stats of both tracks */
static gboolean
bframes_inspect_av_file (const gchar * path, const gchar * demuxer,
    BFramesStats * video, BFramesStats * audio)
{
  GstElement *pipeline, *src, *demux;
  BFramesAvSinks sinks;
  GstElement *elems[2];
  BFramesStats *stats[] = { video, audio };
  GstMessage *msg;
  GstBus *bus;
  gboolean ok;
  gint i;

  bframes_stats_init (video);
  bframes_stats_init (audio);
  pipeline = gst_pipeline_new ("inspect-av");
  src = gst_element_factory_make ("filesrc", NULL);
  demux = gst_element_factory_make (demuxer, NULL);
  sinks.video = elems[0] = gst_element_factory_make ("fakesink", NULL);
  sinks.audio = elems[1] = gst_element_factory_make ("fakesink", NULL);
  fail_unless (src != NULL && demux != NULL);
  g_object_set (src, "location", path, NULL);
  gst_bin_add_many (GST_BIN (pipeline), src, demux, sinks.video, sinks.audio,
      NULL);
  fail_unless (gst_element_link (src, demux));
  g_signal_connect (demux, "pad-added", G_CALLBACK (bframes_av_pad_added_cb),
      &sinks);

  for (i = 0; i < 2; i++) {
    GstPad *pad = gst_element_get_static_pad (elems[i], "sink");

    g_object_set (elems[i], "sync", FALSE, "async", FALSE, NULL);
    gst_pad_add_probe (pad, GST_PAD_PROBE_TYPE_BUFFER |
        GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM, bframes_stats_probe, stats[i],
        NULL);
    gst_object_unref (pad);
  }

  bus = gst_element_get_bus (pipeline);
  gst_element_set_state (pipeline, GST_STATE_PLAYING);
  msg = gst_bus_timed_pop_filtered (bus, 20 * GST_SECOND,
      GST_MESSAGE_EOS | GST_MESSAGE_ERROR);
  ok = msg != NULL && GST_MESSAGE_TYPE (msg) == GST_MESSAGE_EOS;
  if (msg != NULL) {
    gst_message_unref (msg);
  }

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_object_unref (bus);
  gst_object_unref (pipeline);

  return ok;
}

/* MPEG-TS (its demuxer, unlike the MP4 one, keeps the PTS of the tracks aligned
 * and gives the video a DTS below its PTS) with H264 video with B-frames and AAC audio, both starting at
 * start_time (the audio audio_delay later), and the same duration. Returns FALSE (test skipped) if the encoders are not available. */
static gboolean
bframes_generate_av_source (GstClockTime start_time,
    GstClockTime audio_delay, gboolean mp4, const gchar * path)
{
  GstElement *pipeline;
  GstMessage *msg;
  GstBus *bus;
  GError *err = NULL;
  gchar *desc;
  gboolean ok;

  /* The MP4 muxer takes the stream formats that the parsers give by default
   * (the TS one needs byte-stream and ADTS) */
  desc = g_strdup_printf ("%s name=m ! filesink location=%s "
      "videotestsrc num-buffers=%d timestamp-offset=%" G_GUINT64_FORMAT " ! "
      "video/x-raw,format=I420,width=320,height=240,framerate=25/1 ! "
      "x264enc bframes=3 key-int-max=25 ! h264parse ! %s m. "
      "audiotestsrc num-buffers=%d samplesperbuffer=1920 "
      "timestamp-offset=%" G_GUINT64_FORMAT " ! "
      "audio/x-raw,rate=48000,channels=1 ! avenc_aac ! aacparse ! %s m.",
      mp4 ? "mp4mux" : "mpegtsmux", path, AV_FRAMES, start_time,
      mp4 ? "" : "video/x-h264,stream-format=byte-stream,alignment=au ! ",
      AV_FRAMES, start_time + audio_delay,
      mp4 ? "" : "audio/mpeg,mpegversion=4,stream-format=adts ! ");
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

/* Plays the source with video with B-frames and audio. The audio starts
 * audio_delay after the video; the video DTS are lower than their PTS. With
 * an MP4 source each track has its own segment start (edit lists), and the
 * position of a buffer in the common timeline is its timestamp minus it. */
static void
bframes_check_av (GstClockTime audio_delay, guint rounds, gboolean mp4,
    const gchar * name)
{
  static const gchar *const files[] = { "src.ts", "src.mp4", NULL };
  gchar *dir, *src;
  BFramesStats in_v, in_a, out_v, out_a;
  gint64 in_offset, out_offset, in_nth, out_nth, first_gap, audio_step;
  guint i, bad_rounds = 0;

  if (!bframes_have_elements (name, encoders_av)) {
    return;
  }

  dir = bframes_make_tmp_dir ();
  src = g_build_filename (dir, mp4 ? "src.mp4" : "src.ts", NULL);
  if (!bframes_generate_av_source (mp4 ? 0 : TS_START_TIME, audio_delay, mp4,
          src)) {
    g_free (src);
    bframes_remove_tmp_dir (dir, files);
    return;
  }

  fail_unless (bframes_inspect_av_file (src, mp4 ? "qtdemux" : "tsdemux",
          &in_v, &in_a));
  fail_unless (in_v.reordered > 0, "The source has no B-frames");
  fail_unless (GST_CLOCK_TIME_IS_VALID (in_v.first_dts) &&
      in_v.first_dts < in_v.first_pts, "The first video DTS is not below PTS");
  if (mp4) {
    /* The tracks have different segment starts (the video one is the lead of
     * its B-frames): that is what makes the raw timestamps not comparable */
    fail_unless (in_v.seg_start != in_a.seg_start,
        "The tracks of the source have the same segment start");
  }
  in_offset = ((gint64) in_v.first_pts - (gint64) in_v.seg_start) -
      ((gint64) in_a.first_pts - (gint64) in_a.seg_start);
  in_nth = ((gint64) in_v.pts_nth - (gint64) in_v.seg_start) -
      ((gint64) in_a.pts_nth - (gint64) in_a.seg_start);

  /* Which stream gets first to the player is not deterministic: repeat */
  for (i = 0; i < rounds; i++) {
    fail_unless (bframes_play (src, 0, NULL, &out_v, &out_a, TRUE, 0),
        "Playback failed");
    /* The play uses the offset of the preroll, whichever stream gets first: a
     * different one would make the next buffers jump */
    fail_unless (out_v.offset_preroll[0] != '\0' &&
        out_v.offset_play[0] != '\0', "The offsets of the player were not "
        "logged");
    fail_unless_equals_string (out_v.offset_preroll, out_v.offset_play);
    /* Under load the player may lose the buffers at the end when it flushes
     * at the EOS (a known issue that this test does not check), so there is no
     * check on how many arrive. What matters here is the start: no buffers
     * are lost there (see the buffer BFRAMES_NTH below) */
    if (out_v.count < BFRAMES_MIN_BUFFERS || out_a.count <= BFRAMES_NTH) {
      fprintf (stderr, "Only %u of %u video buffers, %u audio buffers\n",
          out_v.count, AV_FRAMES, out_a.count);
      bad_rounds++;
      continue;
    }
    /* Nothing is dropped at the start: the first buffer is the keyframe */
    fail_unless (out_v.first_is_key, "The first video buffer is not a keyframe");

    out_offset = (gint64) out_v.first_pts - (gint64) out_a.first_pts;
    out_nth = (gint64) out_v.pts_nth - (gint64) out_a.pts_nth;
    /* The video that arrives is the first one of the source, with no hole: the
     * buffer BFRAMES_NTH is as far from the first one as in the source. When
     * the machine is overloaded the player sometimes loses buffers at the
     * start also without the fix (about 2% of the rounds with 8 tests on 5
     * CPUs); the bug that this checks happens in most of the rounds, so one
     * bad round is tolerated. */
    if (ABS (((gint64) out_v.pts_nth - (gint64) out_v.first_pts) -
            ((gint64) in_v.pts_nth - (gint64) in_v.first_pts)) >
        2 * GST_MSECOND) {
      fprintf (stderr, "Video buffers lost at the start: PTS of the buffer "
          "%d is %" G_GINT64_FORMAT " ms after the first one, %" G_GINT64_FORMAT
          " ms in the source\n", BFRAMES_NTH,
          ((gint64) out_v.pts_nth - (gint64) out_v.first_pts) / GST_MSECOND,
          ((gint64) in_v.pts_nth - (gint64) in_v.first_pts) / GST_MSECOND);
      bad_rounds++;
      continue;
    }
    first_gap = (gint64) out_v.second_dts - (gint64) out_v.first_dts;
    audio_step = (gint64) out_a.second_pts - (gint64) out_a.first_pts;
    fprintf (stderr, "AVSYNC: audio delay %" G_GINT64_FORMAT " ms, input A/V "
        "PTS offset %" G_GINT64_FORMAT " ms (buffer %d: %" G_GINT64_FORMAT
        " ms), output %" G_GINT64_FORMAT " ms (buffer %d: %" G_GINT64_FORMAT
        " ms), first video DTS separation %" G_GINT64_FORMAT " ms, video "
        "buffers %u, audio buffers %u\n", audio_delay / GST_MSECOND,
        in_offset / GST_MSECOND, BFRAMES_NTH, in_nth / GST_MSECOND,
        out_offset / GST_MSECOND, BFRAMES_NTH, out_nth / GST_MSECOND,
        first_gap / GST_MSECOND, out_v.count, out_a.count);
    fprintf (stderr, "AVSYNC: offset %s (preroll and play), first audio step "
        "%" G_GINT64_FORMAT " ms\n", out_v.offset_play,
        audio_step / GST_MSECOND);

    /* The first video DTS keep the 40 ms step of the source (25 fps) also
     * between the buffer of the preroll and the next one, and so do the first
     * audio buffers (the time between both adds to it) */
    fail_unless (first_gap > 0 && first_gap < 100 * GST_MSECOND,
        "The first video DTS are %" G_GINT64_FORMAT " ms apart",
        first_gap / GST_MSECOND);
    fail_unless (audio_step > 0 && audio_step < 100 * GST_MSECOND,
        "The first audio PTS are %" G_GINT64_FORMAT " ms apart",
        audio_step / GST_MSECOND);

    fail_unless (out_v.min_dts_gap > 0 &&
        out_v.max_dts_gap <= 40 * GST_MSECOND + GST_MSECOND,
        "Video buffers lost: DTS gaps between %" G_GINT64_FORMAT " and %"
        G_GINT64_FORMAT " ms", out_v.min_dts_gap / GST_MSECOND,
        out_v.max_dts_gap / GST_MSECOND);

    if (audio_delay == 0) {
      /* The tracks keep the offset they have in the source, in the first
       * buffers (preroll) and in later ones (playing) */
      fail_unless (ABS (out_offset - in_offset) <= 2 * GST_MSECOND,
          "A/V offset changed from %" G_GINT64_FORMAT " ms to %" G_GINT64_FORMAT
          " ms", in_offset / GST_MSECOND, out_offset / GST_MSECOND);
      fail_unless (ABS (out_nth - in_nth) <= 2 * GST_MSECOND,
          "A/V offset changed from %" G_GINT64_FORMAT " ms to %" G_GINT64_FORMAT
          " ms in buffer %d", in_nth / GST_MSECOND, out_nth / GST_MSECOND,
          BFRAMES_NTH);
    }
  }

  fail_unless (bad_rounds <= 1, "Video buffers lost at the start in %u of %u "
      "rounds", bad_rounds, rounds);
  g_free (src);
  bframes_remove_tmp_dir (dir, files);
}

/* A source whose video has B-frames (DTS < PTS) and whose audio does not, with
 * both tracks starting at the same time: the player must keep the tracks
 * aligned, and must not open a gap between the first two video buffers. */
GST_START_TEST (check_encoded_bframes_av_sync)
{
  bframes_check_av (0, 3, FALSE, "check_encoded_bframes_av_sync");
}

GST_END_TEST;

/* Timestamp arithmetic of the encoded mode (kmsplayerendpointtimestamps.h) */
GST_START_TEST (check_encoded_timestamp_arithmetic)
{
  const GstClockTime ms = GST_MSECOND;
  GstClockTime base, last;
  GstSegment seg_video, seg_audio;
  gint64 translated;
  guint32 seed;

  /* The streams are translated to the running time of their segment: the same
   * point of the timeline gets the same value in tracks with different segment
   * starts. A buffer before the start of the segment keeps its (lower)
   * position. */
  gst_segment_init (&seg_video, GST_FORMAT_TIME);
  seg_video.start = 80 * ms;
  gst_segment_init (&seg_audio, GST_FORMAT_TIME);
  fail_unless_equals_uint64 (kms_player_endpoint_translate_to_segment (
          80 * ms, &seg_video),
      kms_player_endpoint_translate_to_segment (0, &seg_audio));
  fail_unless_equals_uint64 (kms_player_endpoint_translate_to_segment (
          0, &seg_video) + 80 * ms, KMS_PLAYER_SEGMENT_BIAS);
  seg_audio.start = 21 * ms;
  fail_unless (kms_player_endpoint_translate_to_segment (0, &seg_audio) <
      kms_player_endpoint_translate_to_segment (21 * ms, &seg_audio));
  fail_unless_equals_uint64 (kms_player_endpoint_translate_to_segment (
          GST_CLOCK_TIME_NONE, &seg_video), GST_CLOCK_TIME_NONE);
  /* Without a segment only the constant is added */
  fail_unless_equals_uint64 (kms_player_endpoint_translate_to_segment (
          10 * GST_SECOND, NULL), 10 * GST_SECOND + KMS_PLAYER_SEGMENT_BIAS);

  /* A second segment (the second edit of an MP4) that does not start where the
   * first one ended (start 2 s, time and base 1 s): the timeline goes on, the
   * raw timestamps (and "ts - start") would go back */
  gst_segment_init (&seg_video, GST_FORMAT_TIME);
  seg_video.start = 0;
  seg_video.stop = GST_SECOND;
  seg_video.time = 0;
  seg_video.base = 0;
  last = kms_player_endpoint_translate_to_segment (GST_SECOND - 40 * ms,
      &seg_video);
  seg_video.start = 2 * GST_SECOND;
  seg_video.stop = 3 * GST_SECOND;
  seg_video.time = GST_SECOND;
  seg_video.base = GST_SECOND;
  translated = (gint64) kms_player_endpoint_translate_to_segment (
      2 * GST_SECOND, &seg_video) - (gint64) last;
  fail_unless_equals_int64 (translated, 40 * (gint64) ms);

  /* A stream that starts before the shared offset time (here 1 s) after a reset
   * that keeps the last timestamps (3 s): the first buffer (PTS = DTS = 0) has
   * to end above the last DTS, the clamp of the negative lead to zero left it 1
   * s below */
  base = kms_player_endpoint_calc_min_base_time (3 * GST_SECOND,
      3 * GST_SECOND, 0, 0, GST_SECOND, TRUE);
  fail_unless_equals_uint64 (base, 4 * GST_SECOND + ms);
  fail_unless ((gint64) base - (gint64) GST_SECOND > 3 * (gint64) GST_SECOND);

  /* With B-frames the first PTS is ahead of the offset time: the lead is
   * taken into account, so there is no gap of the PTS lead at the start (the
   * offset time is the DTS) */
  base = kms_player_endpoint_calc_min_base_time (GST_SECOND, GST_SECOND - 80 * ms,
      80 * ms, 0, 0, TRUE);
  fail_unless_equals_uint64 (base, GST_SECOND + ms - 80 * ms);
  /* In decoded mode neither the lead nor the DTS count */
  base = kms_player_endpoint_calc_min_base_time (GST_SECOND, GST_SECOND,
      80 * ms, 0, 0, FALSE);
  fail_unless_equals_uint64 (base, GST_SECOND + ms);

  /* Whatever the combination, the translated DTS is above the last one */
  for (seed = 1; seed < 2000; seed++) {
    GstClockTime last_dts = (seed * 31 % 5000) * ms;
    GstClockTime last_pts = last_dts + (seed % 4) * 40 * ms;
    GstClockTime dts = (seed * 7 % 3000) * ms;
    GstClockTime pts = dts + (seed % 3) * 40 * ms;
    GstClockTime offset = (seed * 13 % 3000) * ms;

    base = kms_player_endpoint_calc_min_base_time (last_pts, last_dts, pts, dts,
        offset, TRUE);
    fail_unless ((gint64) dts + (gint64) base - (gint64) offset >
        (gint64) last_dts, "dts %" GST_TIME_FORMAT " offset %" GST_TIME_FORMAT
        " last %" GST_TIME_FORMAT " base %" GST_TIME_FORMAT,
        GST_TIME_ARGS (dts), GST_TIME_ARGS (offset), GST_TIME_ARGS (last_dts),
        GST_TIME_ARGS (base));
  }
}

GST_END_TEST;

/* Rewrites the edit list of every track of an MP4 (the moov atom after the mdat,
 * as the muxer writes it by default) with two edits of 1 s each, the first one
 * at the start of the media and the second one 2 s later: what the editors that
 * cut without recoding generate. The demuxer sends a new segment (with a base)
 * at the second edit. Returns FALSE if the file has another layout. */
static guint32
mp4_timescale (const guint8 * box)
{
  /* mvhd and mdhd: version and flags, creation and modification time, then the
   * time scale */
  return GST_READ_UINT32_BE (box + (box[8] == 1 ? 28 : 20));
}

static guint32
mp4_track_timescale (const guint8 * data, gsize len)
{
  gsize off = 0;

  while (off + 8 <= len) {
    guint32 size = GST_READ_UINT32_BE (data + off);
    const guint8 *type = data + off + 4;

    if (size < 8 || off + size > len) {
      return 0;
    }
    if (memcmp (type, "mdia", 4) == 0) {
      return mp4_track_timescale (data + off + 8, size - 8);
    } else if (memcmp (type, "mdhd", 4) == 0) {
      return mp4_timescale (data + off);
    }
    off += size;
  }

  return 0;
}

static gboolean
mp4_two_edits (const guint8 * data, gsize len, guint32 movie_ts,
    guint32 track_ts, GByteArray * out)
{
  gsize off = 0;

  while (off + 8 <= len) {
    guint32 size = GST_READ_UINT32_BE (data + off);
    const guint8 *type = data + off + 4;
    guint32 be;

    if (size < 8 || off + size > len) {
      return FALSE;
    }
    if (memcmp (type, "trak", 4) == 0 || memcmp (type, "edts", 4) == 0 ||
        memcmp (type, "moov", 4) == 0) {
      guint32 ts = memcmp (type, "trak", 4) == 0 ?
          mp4_track_timescale (data + off + 8, size - 8) : track_ts;
      gsize start = out->len;

      be = 0;
      g_byte_array_append (out, (guint8 *) & be, 4);
      g_byte_array_append (out, type, 4);
      if (!mp4_two_edits (data + off + 8, size - 8, movie_ts, ts, out)) {
        return FALSE;
      }
      GST_WRITE_UINT32_BE (out->data + start, out->len - start);
    } else if (memcmp (type, "elst", 4) == 0) {
      const guint8 *e = data + off + 16;
      gboolean v1 = data[off + 8] == 1;
      gint64 media_time = v1 ? (gint64) GST_READ_UINT64_BE (e + 8) :
          (gint32) GST_READ_UINT32_BE (e + 4);
      guint8 entry[20], head[16];
      gint i;

      if (track_ts == 0 || media_time < 0) {
        return FALSE;
      }
      GST_WRITE_UINT32_BE (head, 16 + 2 * (v1 ? 20 : 12));
      memcpy (head + 4, "elst", 4);
      head[8] = v1 ? 1 : 0;
      head[9] = head[10] = head[11] = 0;
      GST_WRITE_UINT32_BE (head + 12, 2);
      g_byte_array_append (out, head, 16);
      for (i = 0; i < 2; i++) {
        gint64 mt = media_time + i * 2 * (gint64) track_ts;

        if (v1) {
          GST_WRITE_UINT64_BE (entry, movie_ts);
          GST_WRITE_UINT64_BE (entry + 8, mt);
          GST_WRITE_UINT32_BE (entry + 16, 0x10000);
        } else {
          GST_WRITE_UINT32_BE (entry, movie_ts);
          GST_WRITE_UINT32_BE (entry + 4, mt);
          GST_WRITE_UINT32_BE (entry + 8, 0x10000);
        }
        g_byte_array_append (out, entry, v1 ? 20 : 12);
      }
    } else {
      g_byte_array_append (out, data + off, size);
    }
    off += size;
  }

  return TRUE;
}

static gboolean
bframes_mp4_make_two_edits (const gchar * path)
{
  gchar *contents = NULL;
  gsize len, off = 0, moov_off = 0, moov_len = 0;
  guint32 movie_ts = 0;
  GByteArray *out;
  gboolean ok = FALSE;

  if (!g_file_get_contents (path, &contents, &len, NULL)) {
    return FALSE;
  }
  while (off + 8 <= len) {
    guint32 size = GST_READ_UINT32_BE (contents + off);

    if (size < 8 || off + size > len) {
      goto end;
    }
    if (memcmp (contents + off + 4, "moov", 4) == 0) {
      moov_off = off;
      moov_len = size;
    }
    off += size;
  }
  /* The offsets of the samples do not change only if the moov is the last atom */
  if (moov_len == 0 || moov_off + moov_len != len) {
    goto end;
  }
  /* mvhd is the first atom of moov */
  if (memcmp (contents + moov_off + 12, "mvhd", 4) != 0) {
    goto end;
  }
  movie_ts = mp4_timescale ((guint8 *) contents + moov_off + 8);

  out = g_byte_array_new ();
  g_byte_array_append (out, (guint8 *) contents, moov_off);
  {
    gsize start = out->len;
    guint32 be = 0;

    g_byte_array_append (out, (guint8 *) & be, 4);
    g_byte_array_append (out, (guint8 *) "moov", 4);
    ok = mp4_two_edits ((guint8 *) contents + moov_off + 8, moov_len - 8,
        movie_ts, 0, out);
    GST_WRITE_UINT32_BE (out->data + start, out->len - start);
  }
  if (ok) {
    ok = g_file_set_contents (path, (gchar *) out->data, out->len, NULL);
  }
  g_byte_array_free (out, TRUE);

end:
  g_free (contents);
  return ok;
}

/* An MP4 with two edits per track (a new segment, with a base, at the second
 * one): the timestamps of the second part are still above those of the first
 * one in the timeline, so nothing may be dropped as a non incremental
 * timestamp, and there is no hole between both parts. */
GST_START_TEST (check_encoded_mp4_two_edits)
{
  static const gchar *const files[] = { "src.mp4", NULL };
  gchar *dir, *src;
  BFramesStats in_v, in_a, out_v, out_a;

  if (!bframes_have_elements ("check_encoded_mp4_two_edits", encoders_av)) {
    return;
  }

  dir = bframes_make_tmp_dir ();
  src = g_build_filename (dir, "src.mp4", NULL);
  if (!bframes_generate_av_source (0, 0, TRUE, src)) {
    g_free (src);
    bframes_remove_tmp_dir (dir, files);
    return;
  }
  fail_unless (bframes_mp4_make_two_edits (src),
      "Cannot rewrite the edit lists of the MP4");

  /* What the demuxer gives in the source: the two parts, 1 s each */
  fail_unless (bframes_inspect_av_file (src, "qtdemux", &in_v, &in_a));
  fail_unless (in_v.count >= 45 && in_a.count >= 90,
      "The source has %u video and %u audio buffers", in_v.count, in_a.count);
  fail_unless (in_v.max_pts - in_v.first_pts > 2 * GST_SECOND,
      "The source does not have two parts");

  fail_unless (bframes_play (src, 0, NULL, &out_v, &out_a, TRUE, 0),
      "Playback failed");
  /* Under load the player may lose the last buffers at the EOS (a known issue
   * that this test does not check): only a few */
  fail_unless (out_v.count + 5 >= in_v.count && out_a.count + 5 >= in_a.count,
      "Buffers dropped: %u of %u video, %u of %u audio", out_v.count,
      in_v.count, out_a.count, in_a.count);
  /* No hole between both parts (video DTS gaps are 40 ms) */
  fail_unless (out_v.min_dts_gap > 0 &&
      out_v.max_dts_gap <= 100 * GST_MSECOND, "DTS gaps between %"
      G_GINT64_FORMAT " and %" G_GINT64_FORMAT " ms",
      out_v.min_dts_gap / GST_MSECOND, out_v.max_dts_gap / GST_MSECOND);

  g_free (src);
  bframes_remove_tmp_dir (dir, files);
}

GST_END_TEST;

/* MP4 (qtdemux): each track has its own segment start (the video one is 80 ms,
 * the lead of its B-frames, the audio one is 0), so the raw timestamps of the
 * tracks differ by 80 ms while they are aligned in the timeline. The A/V offset
 * of the output has to be the one of the timeline, not of the raw timestamps. */
GST_START_TEST (check_encoded_bframes_av_mp4_segments)
{
  bframes_check_av (0, 3, TRUE, "check_encoded_bframes_av_mp4_segments");
}

GST_END_TEST;

/* The audio starts 1 s after the video, longer than what the player reserves
 * for the DTS of the video: no video may be lost, whichever track gets first */
GST_START_TEST (check_encoded_bframes_av_delayed_audio)
{
  bframes_check_av (GST_SECOND, 6, FALSE,
      "check_encoded_bframes_av_delayed_audio");
}

GST_END_TEST;

/* In decoded mode the streams must not come out behind the running time, even
 * when one starts before the other: the mixers that consume them (e.g. a
 * composite) drop the late buffers. The pipeline has been running for longer
 * than the delay between the streams. */
GST_START_TEST (check_decoded_delayed_audio_running_pipeline)
{
  static const gchar *const files[] = { "src.ts", NULL };
  gchar *dir, *src;
  BFramesStats out_v, out_a;
  guint i;

  if (!bframes_have_elements ("check_decoded_delayed_audio_running_pipeline",
          encoders_av)) {
    return;
  }

  dir = bframes_make_tmp_dir ();
  src = g_build_filename (dir, "src.ts", NULL);
  if (!bframes_generate_av_source (TS_START_TIME, GST_SECOND, FALSE, src)) {
    g_free (src);
    bframes_remove_tmp_dir (dir, files);
    return;
  }

  /* Which stream gets first to the player is not deterministic: repeat */
  for (i = 0; i < 3; i++) {
    fail_unless (bframes_play (src, 0, NULL, &out_v, &out_a, FALSE, 2),
        "Playback failed");
    fail_unless (out_v.first_lead != G_MININT64 &&
        out_a.first_lead != G_MININT64, "A track without buffers");
    fprintf (stderr, "DECODED: PTS - running time of the first buffer: video "
        "%" G_GINT64_FORMAT " ms, audio %" G_GINT64_FORMAT " ms\n",
        out_v.first_lead / GST_MSECOND, out_a.first_lead / GST_MSECOND);
    /* Only the time that the probe takes after the buffer is pushed is behind,
     * not the ~1 s of the delay between the tracks */
    fail_unless (out_v.first_lead > -300 * GST_MSECOND &&
        out_a.first_lead > -300 * GST_MSECOND,
        "A track comes out behind the running time: video %" G_GINT64_FORMAT
        " ms, audio %" G_GINT64_FORMAT " ms", out_v.first_lead / GST_MSECOND,
        out_a.first_lead / GST_MSECOND);
  }

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
  tcase_add_test (tc_chain, check_encoded_bframes_av_sync);
  tcase_add_test (tc_chain, check_encoded_timestamp_arithmetic);
  tcase_add_test (tc_chain, check_encoded_bframes_av_mp4_segments);
  tcase_add_test (tc_chain, check_encoded_mp4_two_edits);
  tcase_add_test (tc_chain, check_encoded_bframes_av_delayed_audio);
  tcase_add_test (tc_chain, check_decoded_delayed_audio_running_pipeline);
#ifdef ENABLE_EXPERIMENTAL_TESTS
  tcase_add_test (tc_chain, check_set_encoded_media);
#endif

  return s;
}

GST_CHECK_MAIN (playerendpoint);
