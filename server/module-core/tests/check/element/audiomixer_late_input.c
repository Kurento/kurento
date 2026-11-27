/*
 * (C) Copyright 2026 Kurento (http://kurento.org/)
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

/* An input whose timestamps fall behind the pipeline clock by more than the
 * mixer latency must stay audible in the mix instead of being dropped forever
 * by the live audiomixer.
 *
 * The lagging input is an appsrc fed in real time by a thread. Its PTS is the
 * sender position minus a configurable lag, which is what the RTP
 * jitterbuffer produces after re-anchoring on a stalled sender: the PTS jumps
 * back and stays behind the clock. A pause skips sender positions, so the PTS
 * jumps forward afterwards and audiorate fills the hole with gap buffers. */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/check/gstcheck.h>
#include <gst/gst.h>
#include <glib.h>

#define RATE 48000
#define BUFFER_DURATION (10 * GST_MSECOND)
#define BUFFER_SAMPLES (RATE / 100)
#define AUDIBLE_LEVEL 1000

typedef struct _TestData
{
  GMainLoop *loop;
  GstElement *pipeline;
  GstElement *appsrc;
  GstElement *mixer;
  GThread *feeder;
  gint running;

  /* Set from the main loop, read by the feeder thread */
  gint lag_ms;
  gint paused;
  gint stalled;                 /* the sender is silent, its timestamps do
                                 * not advance */

  /* Written by the streaming thread, read after the pipeline stops */
  gint64 last_audible;          /* monotonic time, microseconds */

  gint64 lag_changed_at;        /* monotonic time, microseconds */
  GMutex lag_lock;              /* protects lag_changed_at and
                                 * max_silence_after_lag */
  gint64 max_silence_after_lag; /* longest pause between audible outputs since
                                 * the last lag change, microseconds */

  /* Main loop only */
  gint64 max_offset;
  gint64 last_offset;
} TestData;

static GstClockTime
pipeline_running_time (GstElement * pipeline)
{
  GstClock *clock = gst_element_get_clock (pipeline);
  GstClockTime base_time = gst_element_get_base_time (pipeline);
  GstClockTime now;

  if (clock == NULL) {
    return GST_CLOCK_TIME_NONE;
  }

  now = gst_clock_get_time (clock);
  gst_object_unref (clock);

  if (!GST_CLOCK_TIME_IS_VALID (base_time) || base_time == 0 || now < base_time) {
    return GST_CLOCK_TIME_NONE;
  }

  return now - base_time;
}

/* Square wave, 440 Hz */
static GstBuffer *
create_tone_buffer (guint64 * sample_index)
{
  GstBuffer *buffer = gst_buffer_new_allocate (NULL, BUFFER_SAMPLES * 2, NULL);
  GstMapInfo map;
  gint16 *samples;
  guint i;

  gst_buffer_map (buffer, &map, GST_MAP_WRITE);
  samples = (gint16 *) map.data;
  for (i = 0; i < BUFFER_SAMPLES; i++, (*sample_index)++) {
    samples[i] = ((*sample_index * 440 * 2 / RATE) % 2) ? 16000 : -16000;
  }
  gst_buffer_unmap (buffer, &map);

  return buffer;
}

static gpointer
feeder_thread (gpointer user_data)
{
  TestData *data = user_data;
  GstClockTime sender_position = GST_CLOCK_TIME_NONE;
  guint64 sample_index = 0;
  GstClockTime stalled_time = 0;       /* total time skipped by stalls */

  while (g_atomic_int_get (&data->running)) {
    GstClockTime now = pipeline_running_time (data->pipeline);

    if (!GST_CLOCK_TIME_IS_VALID (now)) {
      g_usleep (5000);
      continue;
    }

    if (!GST_CLOCK_TIME_IS_VALID (sender_position)) {
      sender_position = now;
    }

    while (sender_position + BUFFER_DURATION <= now) {
      gint64 lag = g_atomic_int_get (&data->lag_ms) * GST_MSECOND;
      GstBuffer *buffer;
      GstFlowReturn ret;

      if (g_atomic_int_get (&data->stalled)) {
        /* Nothing is sent and the timestamps stop: after the stall the input
         * resumes behind the clock by the length of the stall, without any
         * jump of the timestamps */
        stalled_time += BUFFER_DURATION;
        sender_position += BUFFER_DURATION;
        continue;
      }

      if (g_atomic_int_get (&data->paused)
          || (gint64) sender_position < lag) {
        sender_position += BUFFER_DURATION;
        continue;
      }

      buffer = create_tone_buffer (&sample_index);
      GST_BUFFER_PTS (buffer) = sender_position - lag - stalled_time;
      GST_BUFFER_DURATION (buffer) = BUFFER_DURATION;
      sender_position += BUFFER_DURATION;

      g_signal_emit_by_name (data->appsrc, "push-buffer", buffer, &ret);
      gst_buffer_unref (buffer);
    }

    g_usleep (5000);
  }

  return NULL;
}

static gboolean
buffer_is_audible (GstBuffer * buffer)
{
  GstMapInfo map;
  gboolean audible = FALSE;
  const gint16 *samples;
  gsize i;

  if (!gst_buffer_map (buffer, &map, GST_MAP_READ)) {
    return FALSE;
  }

  samples = (const gint16 *) map.data;
  for (i = 0; i < map.size / sizeof (gint16); i++) {
    if (ABS (samples[i]) > AUDIBLE_LEVEL) {
      audible = TRUE;
      break;
    }
  }

  gst_buffer_unmap (buffer, &map);

  return audible;
}

static GstPadProbeReturn
mix_output_probe (GstPad * pad, GstPadProbeInfo * info, gpointer user_data)
{
  TestData *data = user_data;
  GstBuffer *buffer = GST_PAD_PROBE_INFO_BUFFER (info);

  if (buffer != NULL && buffer_is_audible (buffer)) {
    gint64 now = g_get_monotonic_time ();

    g_mutex_lock (&data->lag_lock);
    if (data->lag_changed_at > 0 && now > data->lag_changed_at) {
      data->max_silence_after_lag = MAX (data->max_silence_after_lag,
          now - MAX (data->last_audible, data->lag_changed_at));
    }
    g_mutex_unlock (&data->lag_lock);
    data->last_audible = now;
  }

  return GST_PAD_PROBE_OK;
}

static void
bus_msg (GstBus * bus, GstMessage * msg, gpointer user_data)
{
  if (GST_MESSAGE_TYPE (msg) == GST_MESSAGE_ERROR) {
    GError *err = NULL;
    gchar *dbg_info = NULL;

    gst_message_parse_error (msg, &err, &dbg_info);
    GST_ERROR ("Error: %s (%s)", err->message, dbg_info);
    fail ("Error received on bus: %s", err->message);
    g_error_free (err);
    g_free (dbg_info);
  }
}

/* src_1 is the mix of every input but the silent one on sink_1, that is, the
 * lagging input alone */
static void
pad_added_cb (GstElement * element, GstPad * pad, gpointer user_data)
{
  TestData *data = user_data;
  GstElement *sink;
  GstPad *sinkpad;

  if (gst_pad_get_direction (pad) != GST_PAD_SRC
      || g_strcmp0 (GST_OBJECT_NAME (pad), "src_1") != 0) {
    return;
  }

  sink = gst_element_factory_make ("fakesink", NULL);
  g_object_set (sink, "sync", FALSE, "async", FALSE, NULL);
  gst_bin_add (GST_BIN (data->pipeline), sink);

  sinkpad = gst_element_get_static_pad (sink, "sink");
  fail_unless (gst_pad_link (pad, sinkpad) == GST_PAD_LINK_OK);
  gst_pad_add_probe (sinkpad, GST_PAD_PROBE_TYPE_BUFFER, mix_output_probe,
      data, NULL);
  gst_object_unref (sinkpad);

  gst_element_sync_state_with_parent (sink);
}

static void
collect_audiorate_offset (const GValue * item, gpointer user_data)
{
  GstElement *element = g_value_get_object (item);
  gint64 *offset = user_data;
  GstElementFactory *factory = gst_element_get_factory (element);
  GstPad *srcpad;

  if (factory == NULL
      || g_strcmp0 (GST_OBJECT_NAME (factory), "audiorate") != 0) {
    return;
  }

  srcpad = gst_element_get_static_pad (element, "src");
  *offset = MAX (*offset, gst_pad_get_offset (srcpad));
  gst_object_unref (srcpad);
}

/* Offset applied by the mixer to the lagging input (the silent input never
 * gets one) */
static gboolean
sample_offset_cb (gpointer user_data)
{
  TestData *data = user_data;
  GstIterator *it = gst_bin_iterate_elements (GST_BIN (data->mixer));
  gint64 offset = 0;

  while (gst_iterator_foreach (it, collect_audiorate_offset,
          &offset) == GST_ITERATOR_RESYNC) {
    gst_iterator_resync (it);
    offset = 0;
  }
  gst_iterator_free (it);

  data->last_offset = offset;
  data->max_offset = MAX (data->max_offset, offset);

  return G_SOURCE_CONTINUE;
}

typedef struct _LagChange
{
  TestData *data;
  gint lag_ms;
} LagChange;

static gboolean
set_lag_cb (gpointer user_data)
{
  LagChange *change = user_data;

  g_atomic_int_set (&change->data->lag_ms, change->lag_ms);
  g_mutex_lock (&change->data->lag_lock);
  change->data->max_silence_after_lag = 0;
  change->data->lag_changed_at = g_get_monotonic_time ();
  g_mutex_unlock (&change->data->lag_lock);

  return G_SOURCE_REMOVE;
}

static void
schedule_lag (TestData * data, guint at_ms, gint lag_ms)
{
  LagChange *change = g_new0 (LagChange, 1);

  change->data = data;
  change->lag_ms = lag_ms;
  g_timeout_add_full (G_PRIORITY_DEFAULT, at_ms, set_lag_cb, change, g_free);
}

static gboolean
set_stalled_cb (gpointer user_data)
{
  TestData *data = user_data;

  g_atomic_int_set (&data->stalled, !g_atomic_int_get (&data->stalled));
  if (!g_atomic_int_get (&data->stalled)) {
    /* The sender resumed: measure the silence from now on */
    g_mutex_lock (&data->lag_lock);
    data->max_silence_after_lag = 0;
    data->lag_changed_at = g_get_monotonic_time ();
    g_mutex_unlock (&data->lag_lock);
  }

  return G_SOURCE_REMOVE;
}

/* The sender stops for @duration_ms starting at @at_ms and then resumes with
 * contiguous timestamps */
static void
schedule_stall (TestData * data, guint at_ms, guint duration_ms)
{
  g_timeout_add (at_ms, set_stalled_cb, data);
  g_timeout_add (at_ms + duration_ms, set_stalled_cb, data);
}

static gboolean
set_paused_cb (gpointer user_data)
{
  TestData *data = user_data;

  g_atomic_int_set (&data->paused, !g_atomic_int_get (&data->paused));

  return G_SOURCE_REMOVE;
}

static gboolean
quit_cb (gpointer user_data)
{
  g_main_loop_quit (((TestData *) user_data)->loop);

  return G_SOURCE_REMOVE;
}

static void
setup_pipeline (TestData * data, gint initial_lag_ms)
{
  GstElement *silent_src;
  GstCaps *caps;

  data->loop = g_main_loop_new (NULL, FALSE);
  data->pipeline = gst_pipeline_new (NULL);
  data->appsrc = gst_element_factory_make ("appsrc", NULL);
  silent_src = gst_element_factory_make ("audiotestsrc", NULL);
  data->mixer = gst_element_factory_make ("kmsaudiomixer", NULL);
  data->lag_ms = initial_lag_ms;
  g_mutex_init (&data->lag_lock);

  caps = gst_caps_new_simple ("audio/x-raw", "format", G_TYPE_STRING, "S16LE",
      "layout", G_TYPE_STRING, "interleaved", "rate", G_TYPE_INT, RATE,
      "channels", G_TYPE_INT, 1, NULL);
  g_object_set (data->appsrc, "is-live", TRUE, "format", GST_FORMAT_TIME,
      "caps", caps, NULL);
  gst_caps_unref (caps);
  g_object_set (silent_src, "is-live", TRUE, "wave", 4 /* silence */ ,
      "samplesperbuffer", BUFFER_SAMPLES, NULL);

  g_signal_connect (data->mixer, "pad-added", G_CALLBACK (pad_added_cb), data);

  gst_bin_add_many (GST_BIN (data->pipeline), data->appsrc, silent_src,
      data->mixer, NULL);
  /* lagging input -> sink_0, silent input -> sink_1 */
  fail_unless (gst_element_link (data->appsrc, data->mixer));
  fail_unless (gst_element_link (silent_src, data->mixer));
}

static void
run_pipeline (TestData * data, guint duration_ms)
{
  guint bus_watch_id, offset_source;
  GstBus *bus;

  bus = gst_pipeline_get_bus (GST_PIPELINE (data->pipeline));
  bus_watch_id = gst_bus_add_watch (bus, gst_bus_async_signal_func, NULL);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), data);
  gst_object_unref (bus);

  offset_source = g_timeout_add (100, sample_offset_cb, data);
  g_timeout_add (duration_ms, quit_cb, data);

  g_atomic_int_set (&data->running, 1);
  data->feeder = g_thread_new ("feeder", feeder_thread, data);

  gst_element_set_state (data->pipeline, GST_STATE_PLAYING);
  g_main_loop_run (data->loop);

  /* A silence that lasts until the end counts as well */
  g_mutex_lock (&data->lag_lock);
  if (data->lag_changed_at > 0) {
    data->max_silence_after_lag = MAX (data->max_silence_after_lag,
        g_get_monotonic_time () - MAX (data->last_audible,
            data->lag_changed_at));
  }
  g_mutex_unlock (&data->lag_lock);

  sample_offset_cb (data);
  g_atomic_int_set (&data->running, 0);
  g_thread_join (data->feeder);

  gst_element_set_state (data->pipeline, GST_STATE_NULL);

  g_source_remove (offset_source);
  g_source_remove (bus_watch_id);
  gst_object_unref (data->pipeline);
  g_main_loop_unref (data->loop);
  g_mutex_clear (&data->lag_lock);
}

static void
assert_audible_at_end (TestData * data)
{
  fail_unless (data->last_audible > 0, "Input was never mixed");
  fail_unless (g_get_monotonic_time () - data->last_audible < G_USEC_PER_SEC,
      "Input is not audible at the end of the test");
}

GST_START_TEST (check_on_time_input_is_not_shifted)
{
  TestData data = { 0 };

  setup_pipeline (&data, 0);
  run_pipeline (&data, 4000);

  assert_audible_at_end (&data);
  fail_unless (data.max_offset == 0, "On-time input was shifted by %"
      GST_STIME_FORMAT, GST_STIME_ARGS (data.max_offset));
}

GST_END_TEST;

GST_START_TEST (check_input_lagging_mid_stream_stays_audible)
{
  TestData data = { 0 };

  setup_pipeline (&data, 0);
  schedule_lag (&data, 1500, 2000);
  run_pipeline (&data, 6000);

  fail_unless (data.lag_changed_at > 0, "Lag was never injected");
  fail_unless (data.last_audible > data.lag_changed_at + 2 * G_USEC_PER_SEC,
      "Lagging input disappeared from the mix");
  assert_audible_at_end (&data);
  fail_unless (data.last_offset > 1500 * GST_MSECOND
      && data.last_offset < 2200 * GST_MSECOND,
      "Unexpected compensation %" GST_STIME_FORMAT,
      GST_STIME_ARGS (data.last_offset));
}

GST_END_TEST;

GST_START_TEST (check_input_lagging_from_start_is_audible)
{
  TestData data = { 0 };

  setup_pipeline (&data, 2000);
  run_pipeline (&data, 5000);

  assert_audible_at_end (&data);
  fail_unless (data.last_offset > 1500 * GST_MSECOND
      && data.last_offset < 2200 * GST_MSECOND,
      "Unexpected compensation %" GST_STIME_FORMAT,
      GST_STIME_ARGS (data.last_offset));
}

GST_END_TEST;

GST_START_TEST (check_input_catching_up_is_unshifted)
{
  TestData data = { 0 };

  setup_pipeline (&data, 0);
  schedule_lag (&data, 1500, 2000);
  schedule_lag (&data, 4500, 0);
  run_pipeline (&data, 9000);

  fail_unless (data.max_offset > 1500 * GST_MSECOND,
      "The lag was never compensated");
  fail_unless (data.last_offset < 500 * GST_MSECOND,
      "Compensation was not removed after catching up: %" GST_STIME_FORMAT,
      GST_STIME_ARGS (data.last_offset));
  fail_unless (data.last_audible > data.lag_changed_at + 2 * G_USEC_PER_SEC,
      "Input disappeared from the mix after catching up");
  assert_audible_at_end (&data);
}

GST_END_TEST;

/* The gap buffers audiorate inserts after a pause are stamped in the past and
 * must not be taken as lateness */
GST_START_TEST (check_pause_does_not_shift_input)
{
  TestData data = { 0 };

  setup_pipeline (&data, 300);
  g_timeout_add (2000, set_paused_cb, &data);
  g_timeout_add (5000, set_paused_cb, &data);
  run_pipeline (&data, 8000);

  fail_unless (data.max_offset < GST_SECOND,
      "Pause caused a compensation of %" GST_STIME_FORMAT,
      GST_STIME_ARGS (data.max_offset));
  assert_audible_at_end (&data);
}

GST_END_TEST;

/* Models the production incident: a mobile sender stopped for ~14 s and then
 * resumed with contiguous timestamps, so the input ended up behind the clock by
 * the length of the stall. The feeder does the same: it stops for 15 s and
 * resumes where its timestamps stopped. */
GST_START_TEST (check_input_resuming_after_long_stall_stays_audible)
{
  TestData data = { 0 };

  setup_pipeline (&data, 0);
  schedule_stall (&data, 1500, 15000);
  run_pipeline (&data, 21000);

  fail_unless (data.lag_changed_at > 0, "The sender never resumed");
  g_print ("last_offset %" GST_STIME_FORMAT ", longest silence after resume %"
      G_GINT64_FORMAT " ms\n", GST_STIME_ARGS (data.last_offset),
      data.max_silence_after_lag / 1000);
  assert_audible_at_end (&data);
  fail_unless (data.last_offset > 14900 * GST_MSECOND
      && data.last_offset < 15050 * GST_MSECOND,
      "Unexpected compensation %" GST_STIME_FORMAT,
      GST_STIME_ARGS (data.last_offset));
  /* The input is late until the grace period of the correction ends */
  fail_unless (data.max_silence_after_lag < 750 * 1000,
      "Input was silent for %" G_GINT64_FORMAT " ms after resuming",
      data.max_silence_after_lag / 1000);
}

GST_END_TEST;

/* After the input caught up, buffers stamped with the old offset are seconds in
 * the future. They must not block the input in the mix: the silence is bounded
 * by the grace period, not by the size of the old offset */
GST_START_TEST (check_catching_up_after_long_lag_loses_little_audio)
{
  TestData data = { 0 };

  setup_pipeline (&data, 0);
  schedule_lag (&data, 1500, 15000);
  schedule_lag (&data, 20000, 0);
  run_pipeline (&data, 25000);

  fail_unless (data.max_offset > 14500 * GST_MSECOND,
      "The lag was never compensated: %" GST_STIME_FORMAT,
      GST_STIME_ARGS (data.max_offset));
  fail_unless (data.last_offset < 500 * GST_MSECOND,
      "Compensation was not removed after catching up: %" GST_STIME_FORMAT,
      GST_STIME_ARGS (data.last_offset));
  g_print ("last_offset %" GST_STIME_FORMAT ", longest silence after "
      "catch-up %" G_GINT64_FORMAT " ms\n", GST_STIME_ARGS (data.last_offset),
      data.max_silence_after_lag / 1000);
  fail_unless (data.max_silence_after_lag < 250 * 1000,
      "Input was silent for %" G_GINT64_FORMAT " ms after catching up",
      data.max_silence_after_lag / 1000);
  assert_audible_at_end (&data);
}

GST_END_TEST;

/* Catching up only partly: the offset must follow the new lag */
GST_START_TEST (check_partial_catch_up_reduces_offset)
{
  TestData data = { 0 };

  setup_pipeline (&data, 0);
  schedule_lag (&data, 1500, 15000);
  schedule_lag (&data, 18500, 5000);
  run_pipeline (&data, 22500);

  g_print ("last_offset %" GST_STIME_FORMAT ", longest silence after "
      "change %" G_GINT64_FORMAT " ms\n", GST_STIME_ARGS (data.last_offset),
      data.max_silence_after_lag / 1000);
  fail_unless (data.max_offset > 14500 * GST_MSECOND,
      "The lag was never compensated: %" GST_STIME_FORMAT,
      GST_STIME_ARGS (data.max_offset));
  fail_unless (data.last_offset > 4500 * GST_MSECOND
      && data.last_offset < 5200 * GST_MSECOND,
      "Unexpected compensation %" GST_STIME_FORMAT,
      GST_STIME_ARGS (data.last_offset));
  fail_unless (data.max_silence_after_lag < 250 * 1000,
      "Input was silent for %" G_GINT64_FORMAT " ms after the lag change",
      data.max_silence_after_lag / 1000);
  assert_audible_at_end (&data);
}

GST_END_TEST;

/* The input becomes only slightly more than EARLY_THRESHOLD (500 ms) early:
 * the lag drops by 620 ms while the compensated input sits about 60 ms behind
 * the clock (observed: the 20 ms target plus the 40 ms tolerance audiorate
 * applies when the PTS jumps back), so it reads about 560 ms early. The offset
 * must still follow, without waiting for a steady reading */
GST_START_TEST (check_input_barely_early_reduces_offset)
{
  TestData data = { 0 };

  setup_pipeline (&data, 0);
  schedule_lag (&data, 1500, 2000);
  schedule_lag (&data, 6000, 1380);
  run_pipeline (&data, 10000);

  g_print ("last_offset %" GST_STIME_FORMAT ", longest silence after "
      "change %" G_GINT64_FORMAT " ms\n", GST_STIME_ARGS (data.last_offset),
      data.max_silence_after_lag / 1000);
  fail_unless (data.last_offset > 1250 * GST_MSECOND
      && data.last_offset < 1500 * GST_MSECOND,
      "Unexpected compensation %" GST_STIME_FORMAT,
      GST_STIME_ARGS (data.last_offset));
  fail_unless (data.max_silence_after_lag < 250 * 1000,
      "Input was silent for %" G_GINT64_FORMAT " ms after the lag change",
      data.max_silence_after_lag / 1000);
  assert_audible_at_end (&data);
}

GST_END_TEST;

static Suite *
audiomixer_late_input_suite (void)
{
  Suite *s = suite_create ("kmsaudiomixer_late_input");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);
  tcase_set_timeout (tc_chain, 40);

  tcase_add_test (tc_chain, check_on_time_input_is_not_shifted);
  tcase_add_test (tc_chain, check_input_lagging_mid_stream_stays_audible);
  tcase_add_test (tc_chain, check_input_lagging_from_start_is_audible);
  tcase_add_test (tc_chain, check_input_catching_up_is_unshifted);
  tcase_add_test (tc_chain, check_pause_does_not_shift_input);
  tcase_add_test (tc_chain, check_input_resuming_after_long_stall_stays_audible);
  tcase_add_test (tc_chain,
      check_catching_up_after_long_lag_loses_little_audio);
  tcase_add_test (tc_chain, check_partial_catch_up_reduces_offset);
  tcase_add_test (tc_chain, check_input_barely_early_reduces_offset);

  return s;
}

GST_CHECK_MAIN (audiomixer_late_input);
