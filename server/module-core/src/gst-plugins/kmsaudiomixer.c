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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "kmsagnosticbin.h"
#include "kmsaudiomixer.h"
#include "kmsloop.h"
#include "kmsrefstruct.h"
#include "kmsutils.h"

#include <gst/gst.h>
#include <gst/base/gstaggregator.h> // GstAggregatorStartTimeSelection

#define PLUGIN_NAME "kmsaudiomixer"

#define LATENCY 150             //ms

/* Input timing correction.
 *
 * Each adder is a live audiomixer whose output advances with the pipeline
 * clock; any input buffer that ends before the current output position is
 * silently dropped (GstAudioAggregator). If an input's timestamps fall behind
 * the clock by more than LATENCY (e.g. the jitterbuffer re-anchors after the
 * remote sender stalls), every following buffer of that input is dropped and
 * the input disappears from the mix for the rest of the session.
 *
 * To avoid that, the end running time of each input buffer is compared against
 * the clock. When an input stays more than INPUT_TIMING_LATE_THRESHOLD behind
 * for longer than INPUT_TIMING_GRACE, a pad offset is applied so that the
 * least-late buffer of that window lands INPUT_TIMING_TARGET behind the clock;
 * typical buffers land later (by their jitter), and since lateness is measured
 * at the end of a buffer, its start is one buffer duration further behind. The
 * target is small so that, with the adder latency of LATENCY, there is room for
 * that jitter before the adder starts dropping buffers. The input is then mixed
 * at its arrival time instead of being lost. Buffers keep arriving in real
 * time, only their timestamps are behind, so a large offset does not buffer or
 * delay anything: a sender that stalled for 15 s is compensated by an offset of
 * about 15 s.
 * INPUT_TIMING_MAX_OFFSET is only a sanity bound. If the offset needed to
 * compensate an input exceeds it, the offset is limited to the bound, the input
 * keeps being dropped by the adder, and a warning is logged. That warning is
 * not repeated until the offset changes or a new stream (or a flush) starts.
 * The offset only affects audio, so a corrected input is shifted against its
 * own video.
 *
 * If a corrected input later jumps ahead of the clock by more than
 * INPUT_TIMING_EARLY_THRESHOLD (the sender caught up, so its timestamps are
 * now on time but the old offset is still applied), the offset is reduced
 * immediately, on the first such buffer, so that this buffer lands
 * INPUT_TIMING_TARGET behind the clock; there is no grace period in this
 * direction. For live, jitter-buffered inputs, buffers arrive in real time and
 * their timestamps are contiguous, so an early reading can only come from a
 * forward jump that persists, there is no transient outlier to filter. The
 * buffer that triggers the reduction is passed. If the reduction is clamped at
 * 0 and the buffer is still early, it is passed as well: without an offset
 * there is nothing left to reduce.
 *
 * Before that buffer arrives, audiorate may fill the jump with gap buffers
 * (silence) that are stamped from the old position up to the new one and, with
 * the old offset, are in the future. One of them at the head of the adder pad
 * would block the input until its time comes (and the leaky queue behind it
 * would discard the audio that follows). They carry only silence, so gap
 * buffers that measure as early while an offset is applied are dropped. This
 * is the only place where the probe drops data. With the immediate reduction,
 * the old and the new timing both map to about the current clock minus
 * INPUT_TIMING_TARGET, so practically no audio is lost on catch-up.
 *
 * Gap buffers are measured for that drop check, but they do not take part in
 * the LATE/offset logic. The measurement restarts when an input stops sending
 * for longer than INPUT_TIMING_GRACE or a new stream starts. */
#define INPUT_TIMING_LATE_THRESHOLD (LATENCY * GST_MSECOND)
#define INPUT_TIMING_EARLY_THRESHOLD (500 * GST_MSECOND)
#define INPUT_TIMING_TARGET (20 * GST_MSECOND)
#define INPUT_TIMING_GRACE (500 * GST_MSECOND)
#define INPUT_TIMING_MAX_OFFSET (3600 * GST_SECOND)

#define KMS_AUDIO_MIXER_LOCK(mixer) \
  (g_rec_mutex_lock (&(mixer)->priv->mutex))

#define KMS_AUDIO_MIXER_UNLOCK(mixer) \
  (g_rec_mutex_unlock (&(mixer)->priv->mutex))

GST_DEBUG_CATEGORY_STATIC (kms_audio_mixer_debug_category);
#define GST_CAT_DEFAULT kms_audio_mixer_debug_category

#define KMS_AUDIO_MIXER_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (            \
    (obj),                                 \
    KMS_TYPE_AUDIO_MIXER,                  \
    KmsAudioMixerPrivate                   \
  )                                        \
)

#define KEY_SINK_PAD_NAME "kms-key-sink-pad-name"
G_DEFINE_QUARK (KEY_SINK_PAD_NAME, key_sink_pad_name);

#define KEY_TEE "tee-key"
G_DEFINE_QUARK (KEY_TEE, key_tee);

#define KEY_FAKESINK "fakesink-key"
G_DEFINE_QUARK (KEY_FAKESINK, key_fakesink);

#define KEY_SRC "src-key"
G_DEFINE_QUARK (KEY_SRC, key_src);

#define KEY_PAD "pad-key"
G_DEFINE_QUARK (KEY_PAD, key_pad);

struct _KmsAudioMixerPrivate
{
  GRecMutex mutex;
  GHashTable *adders;
  GHashTable *agnostics;
  GHashTable *typefinds;
  GstCaps *filtercaps;
  KmsLoop *loop;
  guint count;
};

#define RAW_AUDIO_CAPS "audio/x-raw;"

/* the capabilities of the inputs and outputs. */
static GstStaticPadTemplate audio_sink_factory =
GST_STATIC_PAD_TEMPLATE (AUDIO_SINK_PAD,
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (RAW_AUDIO_CAPS)
    );

static GstStaticPadTemplate audio_src_factory =
GST_STATIC_PAD_TEMPLATE (AUDIO_SRC_PAD,
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (RAW_AUDIO_CAPS)
    );

static void unlink_agnosticbin (GstElement * agnosticbin);
static void unlink_adder_sources (GstElement * adder);

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsAudioMixer, kms_audio_mixer,
    GST_TYPE_BIN,
    GST_DEBUG_CATEGORY_INIT (kms_audio_mixer_debug_category,
        PLUGIN_NAME, 0, "debug category for " PLUGIN_NAME " element"));

static GstPadProbeReturn
cb_latency (GstPad * pad, GstPadProbeInfo * info, gpointer data)
{
  GstQuery *query = gst_pad_probe_info_get_query (info);

  if (GST_QUERY_TYPE (query) != GST_QUERY_LATENCY) {
    return GST_PAD_PROBE_OK;
  }

  GST_LOG_OBJECT (pad, "Modifing latency query. New latency %" G_GUINT64_FORMAT,
      (guint64) (LATENCY * GST_MSECOND));

  gst_query_set_latency (query, TRUE, 0, LATENCY * GST_MSECOND);

  return GST_PAD_PROBE_HANDLED;
}

static GstElement *
kms_audio_selector_create_capsfilter (KmsAudioMixer * self)
{
  GstElement *capsfilter = kms_utils_element_factory_make ("capsfilter", PLUGIN_NAME);

  if (!self->priv->filtercaps) {
    const guint64 channel_mask = 0x03;
    self->priv->filtercaps =
        // clang-format off
        gst_caps_new_simple ("audio/x-raw",
            "format", G_TYPE_STRING, "S16LE",
            "layout", G_TYPE_STRING, "interleaved",
            "rate", G_TYPE_INT, 48000,
            "channels", G_TYPE_INT, 2,
            "channel-mask", GST_TYPE_BITMASK, channel_mask,
            NULL);
        // clang-format on
  }

  g_object_set (G_OBJECT (capsfilter), "caps", self->priv->filtercaps, NULL);

  return capsfilter;
}

static void
link_new_agnosticbin (gchar * key, GstElement * adder, GstElement * agnosticbin)
{
  GstPad *srcpad = NULL, *sinkpad = NULL;
  char *padname;
  KmsAudioMixer *self = KMS_AUDIO_MIXER (GST_OBJECT_PARENT (agnosticbin));
  GstElement *capsfilter;

  padname =
      g_object_get_qdata (G_OBJECT (agnosticbin), key_sink_pad_name_quark ());
  if (padname == NULL) {
    GST_ERROR ("No pad associated with %" GST_PTR_FORMAT, agnosticbin);
    goto end;
  }

  if (g_str_equal (key, padname)) {
    /* Do not connect the origin audio input */
    GST_TRACE ("Do not connect echo audio input %" GST_PTR_FORMAT, agnosticbin);
    goto end;
  }

  srcpad = gst_element_request_pad_simple (agnosticbin, "src_%u");
  if (srcpad == NULL) {
    GST_ERROR ("Could not get src pad in %" GST_PTR_FORMAT, agnosticbin);
    goto end;
  }

  sinkpad = gst_element_request_pad_simple (adder, "sink_%u");
  if (sinkpad == NULL) {
    GST_ERROR ("Could not get sink pad in %" GST_PTR_FORMAT, adder);
    gst_element_release_request_pad (agnosticbin, srcpad);
    goto end;
  }

  GST_DEBUG ("Linking %" GST_PTR_FORMAT " to %" GST_PTR_FORMAT, srcpad,
      sinkpad);

  gst_pad_add_probe (sinkpad,
      GST_PAD_PROBE_TYPE_QUERY_UPSTREAM,
      (GstPadProbeCallback) cb_latency, NULL, NULL);

  capsfilter = kms_audio_selector_create_capsfilter (self);

  gst_bin_add (GST_BIN (self), capsfilter);
  gst_element_sync_state_with_parent (capsfilter);

  gst_element_link_pads (capsfilter, NULL, adder, GST_OBJECT_NAME (sinkpad));
  gst_element_link_pads (agnosticbin, GST_OBJECT_NAME (srcpad), capsfilter,
      NULL);

end:
  if (srcpad != NULL) {
    g_object_unref (srcpad);
  }

  if (sinkpad != NULL) {
    g_object_unref (sinkpad);
  }
}

static void
link_new_adder (gchar * key, GstElement * agnosticbin, GstElement * adder)
{
  char *padname;
  GstPad *srcpad = NULL, *sinkpad = NULL;
  KmsAudioMixer *self = KMS_AUDIO_MIXER (GST_OBJECT_PARENT (adder));
  GstElement *capsfilter;

  padname = g_object_get_qdata (G_OBJECT (adder), key_sink_pad_name_quark ());
  if (padname == NULL) {
    GST_ERROR ("No pad associated with %" GST_PTR_FORMAT, adder);
    return;
  }

  if (g_str_equal (key, padname)) {
    /* Do not connect the origin audio input */
    GST_TRACE ("Do not connect echo audio input %" GST_PTR_FORMAT, agnosticbin);
    return;
  }

  GST_DEBUG ("Linking %s to %s", GST_ELEMENT_NAME (agnosticbin),
      GST_ELEMENT_NAME (adder));

  srcpad = gst_element_request_pad_simple (agnosticbin, "src_%u");
  if (srcpad == NULL) {
    GST_ERROR ("Could not get src pad in %" GST_PTR_FORMAT, agnosticbin);
    goto end;
  }

  sinkpad = gst_element_request_pad_simple (adder, "sink_%u");
  if (sinkpad == NULL) {
    GST_ERROR ("Could not get sink pad in %" GST_PTR_FORMAT, adder);
    gst_element_release_request_pad (agnosticbin, srcpad);
    goto end;
  }

  gst_pad_add_probe (sinkpad,
      GST_PAD_PROBE_TYPE_QUERY_UPSTREAM,
      (GstPadProbeCallback) cb_latency, NULL, NULL);

  capsfilter = kms_audio_selector_create_capsfilter (self);

  gst_bin_add (GST_BIN (self), capsfilter);
  gst_element_sync_state_with_parent (capsfilter);

  gst_element_link_pads (capsfilter, NULL, adder, GST_OBJECT_NAME (sinkpad));
  gst_element_link_pads (agnosticbin, GST_OBJECT_NAME (srcpad), capsfilter,
      NULL);

end:
  if (srcpad != NULL) {
    g_object_unref (srcpad);
  }

  if (sinkpad != NULL) {
    g_object_unref (sinkpad);
  }
}

static gint
get_stream_id_from_padname (const gchar * name)
{
  gint64 id;

  if (name == NULL)
    return -1;

  if (!g_str_has_prefix (name, AUDIO_SINK_PAD_PREFIX))
    return -1;

  id = g_ascii_strtoll (name + LENGTH_AUDIO_SINK_PAD_PREFIX, NULL, 10);
  if (id > G_MAXINT)
    return -1;

  return id;
}

static void
kms_audio_mixer_remove_sometimes_src_pad (KmsAudioMixer * self,
    GstElement * adder)
{
  GstPad *pad, *peer = NULL, *adder_src;

  pad = g_object_get_qdata (G_OBJECT (adder), key_pad_quark ());
  g_object_set_qdata (G_OBJECT (adder), key_pad_quark (), NULL);

  if (!pad) {
    return;
  }

  adder_src = gst_element_get_static_pad (adder, "src");
  if (adder_src) {
    peer = gst_pad_get_peer (adder_src);

    if (peer) {
      gst_pad_send_event (peer, gst_event_new_flush_start ());
    }
  }

  gst_ghost_pad_set_target (GST_GHOST_PAD (pad), NULL);

  if (GST_STATE (self) < GST_STATE_PAUSED
      || GST_STATE_PENDING (self) < GST_STATE_PAUSED
      || GST_STATE_TARGET (self) < GST_STATE_PAUSED) {
    gst_pad_set_active (GST_PAD (pad), FALSE);
  }

  GST_DEBUG ("Removing source pad %" GST_PTR_FORMAT, pad);

  gst_element_remove_pad (GST_ELEMENT (self), GST_PAD (pad));

  if (peer) {
    gst_pad_send_event (peer, gst_event_new_flush_stop (FALSE));
    g_object_unref (peer);
  }

  if (adder_src) {
    g_object_unref (adder_src);
  }
}

static void
remove_element (GstBin * bin, GstElement * element)
{
  gst_object_ref (element);
  gst_element_set_locked_state (element, TRUE);
  gst_element_set_state (element, GST_STATE_NULL);
  gst_bin_remove (bin, element);
  gst_object_unref (element);
}

static gboolean
remove_adder (GstElement * adder)
{
  KmsAudioMixer *self;
  GstElement *fakesink, *tee, *testsrc;

  self = (KmsAudioMixer *) gst_element_get_parent (adder);
  if (self == NULL) {
    GST_WARNING_OBJECT (adder, "No parent element");
    return FALSE;
  }

  GST_DEBUG ("Removing element %" GST_PTR_FORMAT, adder);

  kms_audio_mixer_remove_sometimes_src_pad (self, adder);

  tee = g_object_get_qdata (G_OBJECT (adder), key_tee_quark ());
  fakesink = g_object_get_qdata (G_OBJECT (adder), key_fakesink_quark ());
  testsrc = g_object_get_qdata (G_OBJECT (adder), key_src_quark ());

  if (testsrc) {
    remove_element (GST_BIN (self), testsrc);
  }

  remove_element (GST_BIN (self), adder);

  if (tee) {
    remove_element (GST_BIN (self), tee);
  }

  if (fakesink) {
    remove_element (GST_BIN (self), fakesink);
  }

  gst_object_unref (self);

  return G_SOURCE_REMOVE;
}

static void
remove_agnostic_bin (GstElement * agnosticbin)
{
  KmsAudioMixer *self;
  GstElement *audiorate = NULL, *typefind = NULL;
  GstPad *sinkpad, *peerpad;

  self = (KmsAudioMixer *) gst_element_get_parent (agnosticbin);

  if (self == NULL) {
    GST_WARNING_OBJECT (agnosticbin, "No parent element");
    return;
  }

  sinkpad = gst_element_get_static_pad (agnosticbin, "sink");
  peerpad = gst_pad_get_peer (sinkpad);
  if (peerpad == NULL) {
    GST_WARNING_OBJECT (sinkpad, "Not linked");
    gst_object_unref (sinkpad);
    goto end;
  }

  audiorate = gst_pad_get_parent_element (peerpad);
  gst_object_unref (sinkpad);
  gst_object_unref (peerpad);

  if (audiorate == NULL) {
    GST_WARNING_OBJECT (self, "No audiorate");
    goto end;
  }

  sinkpad = gst_element_get_static_pad (audiorate, "sink");
  peerpad = gst_pad_get_peer (sinkpad);
  if (peerpad == NULL) {
    GST_WARNING_OBJECT (sinkpad, "Not linked");
    gst_object_unref (sinkpad);
    goto end;
  }

  typefind = gst_pad_get_parent_element (peerpad);
  gst_object_unref (sinkpad);
  gst_object_unref (peerpad);

  if (typefind == NULL) {
    GST_WARNING_OBJECT (self, "No typefind");
    goto end;
  }

  gst_element_unlink_many (typefind, audiorate, agnosticbin, NULL);

  gst_element_set_locked_state (typefind, TRUE);
  gst_element_set_locked_state (audiorate, TRUE);
  gst_element_set_locked_state (agnosticbin, TRUE);

  gst_element_set_state (typefind, GST_STATE_NULL);
  gst_element_set_state (audiorate, GST_STATE_NULL);
  gst_element_set_state (agnosticbin, GST_STATE_NULL);

  gst_object_ref (agnosticbin);

  gst_bin_remove_many (GST_BIN (self), typefind, audiorate, agnosticbin, NULL);

  gst_object_unref (agnosticbin);

end:
  if (audiorate != NULL) {
    gst_object_unref (audiorate);
  }

  if (typefind != NULL) {
    gst_object_unref (typefind);
  }

  gst_object_unref (self);
}

static gboolean
remove_adder_cb (gpointer key, gpointer value, gpointer user_data)
{
  GstElement *adder = GST_ELEMENT (value);

  unlink_adder_sources (adder);
  remove_adder (adder);

  return TRUE;
}

static gboolean
remove_agnosticbin_cb (gpointer key, gpointer value, gpointer user_data)
{
  GstElement *agnostic = GST_ELEMENT (value);

  unlink_agnosticbin (agnostic);
  remove_agnostic_bin (agnostic);

  return TRUE;
}

static void
kms_audio_mixer_dispose (GObject * object)
{
  KmsAudioMixer *self = KMS_AUDIO_MIXER (object);

  GST_DEBUG_OBJECT (self, "dispose");

  KMS_AUDIO_MIXER_LOCK (self);

  if (self->priv->agnostics != NULL) {
    g_hash_table_foreach_remove (self->priv->agnostics, remove_agnosticbin_cb,
        NULL);
    g_hash_table_unref (self->priv->agnostics);
    self->priv->agnostics = NULL;
  }

  if (self->priv->adders != NULL) {
    g_hash_table_foreach_remove (self->priv->adders, remove_adder_cb, self);
    g_hash_table_unref (self->priv->adders);
    self->priv->adders = NULL;
  }

  if (self->priv->filtercaps) {
    gst_caps_unref (self->priv->filtercaps);
    self->priv->filtercaps = NULL;
  }

  g_clear_object (&self->priv->loop);

  KMS_AUDIO_MIXER_UNLOCK (self);

  G_OBJECT_CLASS (kms_audio_mixer_parent_class)->dispose (object);
}

static void
kms_audio_mixer_finalize (GObject * object)
{
  KmsAudioMixer *self = KMS_AUDIO_MIXER (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_hash_table_unref (self->priv->typefinds);
  g_rec_mutex_clear (&self->priv->mutex);

  G_OBJECT_CLASS (kms_audio_mixer_parent_class)->finalize (object);
}

typedef enum
{
  INPUT_TIMING_ON_TIME,
  INPUT_TIMING_LATE
} KmsAudioMixerInputTimingState;

typedef struct _KmsAudioMixerInputTiming
{
  gchar *padname;
  gchar *stream_id;
  KmsAudioMixerInputTimingState state;
  GstClockTime state_since;     /* clock running time the state started */
  GstClockTime last_buffer;     /* clock running time of the last buffer */
  /* Lateness closest to the clock seen since the state started, so a single
   * outlier never drives the correction */
  GstClockTimeDiff mildest_lateness;
  gboolean clamp_logged;        /* the "still late" warning was already emitted */
} KmsAudioMixerInputTiming;

static void
input_timing_reset (KmsAudioMixerInputTiming * timing)
{
  timing->state = INPUT_TIMING_ON_TIME;
  timing->state_since = GST_CLOCK_TIME_NONE;
  timing->last_buffer = GST_CLOCK_TIME_NONE;
  timing->mildest_lateness = 0;
}

static KmsAudioMixerInputTiming *
kms_audio_mixer_input_timing_new (const gchar * padname)
{
  KmsAudioMixerInputTiming *timing = g_slice_new0 (KmsAudioMixerInputTiming);

  timing->padname = g_strdup (padname);
  input_timing_reset (timing);

  return timing;
}

static void
kms_audio_mixer_input_timing_free (gpointer data)
{
  KmsAudioMixerInputTiming *timing = data;

  g_free (timing->padname);
  g_free (timing->stream_id);
  g_slice_free (KmsAudioMixerInputTiming, timing);
}

static GstPadProbeReturn
input_timing_event (GstPad * pad, GstEvent * event,
    KmsAudioMixerInputTiming * timing)
{
  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_STREAM_START:{
      const gchar *stream_id = NULL;

      /* Changing the offset re-sends the same STREAM_START; only a new source
       * must not inherit the correction of the previous one */
      gst_event_parse_stream_start (event, &stream_id);
      if (g_strcmp0 (stream_id, timing->stream_id) != 0) {
        g_free (timing->stream_id);
        timing->stream_id = g_strdup (stream_id);
        gst_pad_set_offset (pad, 0);
        input_timing_reset (timing);
        timing->clamp_logged = FALSE;
      }
      break;
    }
    case GST_EVENT_FLUSH_STOP:
      input_timing_reset (timing);
      timing->clamp_logged = FALSE;
      break;
    default:
      break;
  }

  return GST_PAD_PROBE_OK;
}

/* Returns the lateness of the end of @buffer against the clock, taking the pad
 * offset into account, or FALSE when it can not be measured */
static gboolean
input_timing_measure (GstPad * pad, GstBuffer * buffer, GstClockTime * now,
    GstClockTimeDiff * lateness)
{
  GstClockTime clock_time, base_time, position, running_time;
  GstElement *element;
  GstEvent *event;
  GstSegment segment;
  GstClock *clock;

  /* Sticky events are stored without the pad offset applied */
  event = gst_pad_get_sticky_event (pad, GST_EVENT_SEGMENT, 0);
  if (event == NULL) {
    return FALSE;
  }
  gst_event_copy_segment (event, &segment);
  gst_event_unref (event);

  if (segment.format != GST_FORMAT_TIME) {
    return FALSE;
  }

  position = GST_BUFFER_PTS (buffer);
  if (GST_BUFFER_DURATION_IS_VALID (buffer)) {
    position += GST_BUFFER_DURATION (buffer);
  }

  running_time =
      gst_segment_to_running_time (&segment, GST_FORMAT_TIME, position);
  if (!GST_CLOCK_TIME_IS_VALID (running_time)) {
    return FALSE;
  }

  element = gst_pad_get_parent_element (pad);
  if (element == NULL) {
    return FALSE;
  }

  clock = gst_element_get_clock (element);
  base_time = gst_element_get_base_time (element);
  gst_object_unref (element);

  if (clock == NULL) {
    return FALSE;
  }

  clock_time = gst_clock_get_time (clock);
  gst_object_unref (clock);

  if (!GST_CLOCK_TIME_IS_VALID (base_time) || clock_time < base_time) {
    return FALSE;
  }

  *now = clock_time - base_time;
  *lateness = GST_CLOCK_DIFF (running_time + gst_pad_get_offset (pad), *now);

  return TRUE;
}

static GstPadProbeReturn
input_timing_probe (GstPad * pad, GstPadProbeInfo * info, gpointer user_data)
{
  KmsAudioMixerInputTiming *timing = user_data;
  KmsAudioMixerInputTimingState state;
  GstClockTimeDiff lateness;
  GstClockTime now;
  GstBuffer *buffer;
  gint64 offset, new_offset, desired_offset;

  if (GST_PAD_PROBE_INFO_TYPE (info) & (GST_PAD_PROBE_TYPE_EVENT_BOTH |
          GST_PAD_PROBE_TYPE_EVENT_FLUSH)) {
    return input_timing_event (pad, GST_PAD_PROBE_INFO_EVENT (info), timing);
  }

  buffer = GST_PAD_PROBE_INFO_BUFFER (info);
  if (buffer == NULL || !GST_BUFFER_PTS_IS_VALID (buffer)) {
    return GST_PAD_PROBE_OK;
  }

  if (GST_BUFFER_FLAG_IS_SET (buffer, GST_BUFFER_FLAG_GAP)) {
    /* Gap buffers do not drive the state machine, but an early one can occur:
     * when an input with a large offset catches up, audiorate fills the jump
     * of the PTS with silence stamped from the old position up to the new one,
     * and with the old offset applied most of it is in the future. Such a
     * buffer at the head of the adder pad would block the input, and it only
     * carries silence, so drop it. The real buffer that follows reduces the
     * offset. */
    if (gst_pad_get_offset (pad) > 0
        && input_timing_measure (pad, buffer, &now, &lateness)
        && lateness < -(GstClockTimeDiff) INPUT_TIMING_EARLY_THRESHOLD) {
      GST_DEBUG_OBJECT (pad, "Audio input %s: dropping early gap buffer, %"
          GST_STIME_FORMAT " ahead of the mixer clock", timing->padname,
          GST_STIME_ARGS (-lateness));
      return GST_PAD_PROBE_DROP;
    }
    return GST_PAD_PROBE_OK;
  }

  if (!input_timing_measure (pad, buffer, &now, &lateness)) {
    return GST_PAD_PROBE_OK;
  }

  /* The input stopped sending for a while: start measuring again */
  if (GST_CLOCK_TIME_IS_VALID (timing->last_buffer)
      && (now < timing->last_buffer
          || now - timing->last_buffer > INPUT_TIMING_GRACE)) {
    input_timing_reset (timing);
  }
  timing->last_buffer = now;

  offset = gst_pad_get_offset (pad);

  if (offset > 0
      && lateness < -(GstClockTimeDiff) INPUT_TIMING_EARLY_THRESHOLD) {
    /* Caught up: reduce the offset right away and pass this buffer */
    new_offset = CLAMP (offset + lateness - (gint64) INPUT_TIMING_TARGET, 0,
        (gint64) INPUT_TIMING_MAX_OFFSET);
    input_timing_reset (timing);
    timing->last_buffer = now;
    timing->clamp_logged = FALSE;
    GST_WARNING_OBJECT (pad, "Audio input %s is %" GST_STIME_FORMAT
        " ahead of the mixer clock, changing its offset from %"
        GST_STIME_FORMAT " to %" GST_STIME_FORMAT, timing->padname,
        GST_STIME_ARGS (-lateness), GST_STIME_ARGS (offset),
        GST_STIME_ARGS (new_offset));
    /* The SEGMENT event is re-sent with the new offset before this buffer */
    gst_pad_set_offset (pad, new_offset);
    return GST_PAD_PROBE_OK;
  }

  if (lateness > (GstClockTimeDiff) INPUT_TIMING_LATE_THRESHOLD) {
    state = INPUT_TIMING_LATE;
  } else {
    state = INPUT_TIMING_ON_TIME;
  }

  if (state != timing->state) {
    timing->state = state;
    timing->state_since = now;
    timing->mildest_lateness = lateness;
    return GST_PAD_PROBE_OK;
  }

  if (state == INPUT_TIMING_ON_TIME) {
    return GST_PAD_PROBE_OK;
  }

  timing->mildest_lateness = MIN (timing->mildest_lateness, lateness);

  if (now - timing->state_since < INPUT_TIMING_GRACE) {
    return GST_PAD_PROBE_OK;
  }

  desired_offset = offset + timing->mildest_lateness -
      (gint64) INPUT_TIMING_TARGET;
  new_offset = CLAMP (desired_offset, 0, (gint64) INPUT_TIMING_MAX_OFFSET);
  lateness = timing->mildest_lateness;
  input_timing_reset (timing);
  timing->last_buffer = now;

  if (new_offset != offset) {
    timing->clamp_logged = FALSE;
  }

  if (desired_offset > (gint64) INPUT_TIMING_MAX_OFFSET
      && !timing->clamp_logged) {
    timing->clamp_logged = TRUE;
    GST_WARNING_OBJECT (pad, "Audio input %s is still %" GST_STIME_FORMAT
        " late against the mixer clock, its offset is limited to %"
        GST_STIME_FORMAT " and its audio is being dropped", timing->padname,
        GST_STIME_ARGS (desired_offset - new_offset +
            (gint64) INPUT_TIMING_TARGET),
        GST_STIME_ARGS ((gint64) INPUT_TIMING_MAX_OFFSET));
  }

  if (new_offset == offset) {
    return GST_PAD_PROBE_OK;
  }

  GST_WARNING_OBJECT (pad, "Audio input %s is %" GST_STIME_FORMAT
      " late against the mixer clock, changing its offset from %"
      GST_STIME_FORMAT " to %" GST_STIME_FORMAT, timing->padname,
      GST_STIME_ARGS (lateness), GST_STIME_ARGS (offset),
      GST_STIME_ARGS (new_offset));

  /* The SEGMENT event is re-sent with the new offset before this buffer */
  gst_pad_set_offset (pad, new_offset);

  return GST_PAD_PROBE_OK;
}

static void
kms_audio_mixer_have_type (GstElement * typefind, guint arg0, GstCaps * caps,
    gpointer data)
{
  KmsAudioMixer *self = KMS_AUDIO_MIXER (data);
  GstElement *audiorate, *agnosticbin;
  GstPad *audiorate_src;
  gchar *padname;
  gint id;

  padname =
      g_object_get_qdata (G_OBJECT (typefind), key_sink_pad_name_quark ());
  if ((id = get_stream_id_from_padname (padname)) < 0) {
    GST_ERROR_OBJECT (self, "Can not get pad id from element %" GST_PTR_FORMAT,
        typefind);
    return;
  }

  KMS_AUDIO_MIXER_LOCK (self);

  if (!g_hash_table_remove (self->priv->typefinds, padname)) {
    GST_WARNING_OBJECT (self, "Audio input %s is already managed", padname);
    KMS_AUDIO_MIXER_UNLOCK (self);
    return;
  }

  audiorate = kms_utils_element_factory_make ("audiorate", PLUGIN_NAME);
  g_object_set (audiorate, "skip-to-first", TRUE, NULL);

  /* The offset set on audiorate's src pad applies to every adder fed by this
   * input */
  audiorate_src = gst_element_get_static_pad (audiorate, "src");
  gst_pad_add_probe (audiorate_src, GST_PAD_PROBE_TYPE_BUFFER |
      GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM | GST_PAD_PROBE_TYPE_EVENT_FLUSH,
      input_timing_probe, kms_audio_mixer_input_timing_new (padname),
      kms_audio_mixer_input_timing_free);
  gst_object_unref (audiorate_src);

  agnosticbin = kms_utils_element_factory_make ("agnosticbin", PLUGIN_NAME);
  g_object_set_qdata_full (G_OBJECT (agnosticbin), key_sink_pad_name_quark (),
      g_strdup (padname), g_free);

  gst_bin_add_many (GST_BIN (self), audiorate, agnosticbin, NULL);
  gst_element_link_many (typefind, audiorate, agnosticbin, NULL);

  g_hash_table_foreach (self->priv->adders, (GHFunc) link_new_agnosticbin,
      agnosticbin);

  g_hash_table_insert (self->priv->agnostics, g_strdup (padname), agnosticbin);

  gst_bin_recalculate_latency (GST_BIN (self));
  KMS_AUDIO_MIXER_UNLOCK (self);

  gst_element_sync_state_with_parent (audiorate);
  gst_element_sync_state_with_parent (agnosticbin);
}

static void
unlink_agnosticbin_source (const GValue * item, gpointer user_data)
{
  GstElement *capsfilter = NULL, *agnosticbin = GST_ELEMENT (user_data);
  GstPad *srcpad, *sinkpad = NULL, *capsfilter_src = NULL, *capsfilter_sink;
  GstElement *adder = NULL;

  srcpad = g_value_get_object (item);

  capsfilter_sink = gst_pad_get_peer (srcpad);
  if (capsfilter_sink == NULL) {
    GST_WARNING_OBJECT (srcpad, "Not linked");
    goto end;
  }

  capsfilter = gst_pad_get_parent_element (capsfilter_sink);
  capsfilter_src = gst_element_get_static_pad (capsfilter, "src");
  sinkpad = gst_pad_get_peer (capsfilter_src);

  g_object_unref (capsfilter_sink);

  adder = gst_pad_get_parent_element (sinkpad);
  if (adder == NULL) {
    GST_ERROR_OBJECT (sinkpad, "No parent element");
    goto end;
  }

  GST_DEBUG ("Unlink %" GST_PTR_FORMAT " and %" GST_PTR_FORMAT,
      srcpad, sinkpad);

  if (!gst_pad_unlink (capsfilter_src, sinkpad)) {
    GST_ERROR ("Can not unlink %" GST_PTR_FORMAT " and %" GST_PTR_FORMAT,
        srcpad, sinkpad);
  }

  gst_element_release_request_pad (adder, sinkpad);
  gst_element_release_request_pad (agnosticbin, srcpad);

end:
  if (sinkpad != NULL) {
    gst_object_unref (sinkpad);
  }

  if (adder != NULL) {
    gst_object_unref (adder);
  }

  if (capsfilter_src) {
    g_object_unref (capsfilter_src);
  }

  if (capsfilter) {
    gst_bin_remove (GST_BIN (GST_OBJECT_PARENT (capsfilter)), capsfilter);
    gst_element_set_state (capsfilter, GST_STATE_NULL);
    g_object_unref (capsfilter);
  }
}

static void
unlink_agnosticbin (GstElement * agnosticbin)
{
  GstIterator *it;

  it = gst_element_iterate_src_pads (agnosticbin);

  while (gst_iterator_foreach (it, unlink_agnosticbin_source,
          agnosticbin) == GST_ITERATOR_RESYNC) {
    gst_iterator_resync (it);
  }

  gst_iterator_free (it);
}

static void
kms_audio_mixer_remove_elements (KmsAudioMixer * self,
    GstElement * agnosticbin, GstElement * adder)
{
  /* Unlink elements holding the mutex to avoid race */
  /* condition under massive disconnections */
  KMS_AUDIO_MIXER_LOCK (self);

  if (agnosticbin != NULL) {
    unlink_agnosticbin (agnosticbin);
  }

  if (adder != NULL) {
    unlink_adder_sources (adder);
  }

  KMS_AUDIO_MIXER_UNLOCK (self);

  if (agnosticbin != NULL) {
    remove_agnostic_bin (agnosticbin);
  }

  if (adder != NULL) {
    remove_adder (adder);
  }
}

static void
unlink_adder_sink (const GValue * item, gpointer user_data)
{
  GstElement *capsfilter = NULL, *adder = GST_ELEMENT (user_data);
  GstPad *sinkpad, *srcpad = NULL, *capsfilter_src = NULL, *capsfilter_sink;
  GstElement *src = NULL;

  sinkpad = g_value_get_object (item);

  capsfilter_src = gst_pad_get_peer (sinkpad);
  if (capsfilter_src == NULL) {
    GST_WARNING_OBJECT (sinkpad, "Not linked");
    goto end;
  }

  capsfilter = gst_pad_get_parent_element (capsfilter_src);
  capsfilter_sink = gst_element_get_static_pad (capsfilter, "sink");
  srcpad = gst_pad_get_peer (capsfilter_sink);

  g_object_unref (capsfilter_sink);

  src = gst_pad_get_parent_element (srcpad);
  if (src == NULL) {
    GST_ERROR_OBJECT (srcpad, "No parent element");
    goto end;
  }

  GST_DEBUG ("Unlink %" GST_PTR_FORMAT " and %" GST_PTR_FORMAT,
      srcpad, sinkpad);

  if (!gst_pad_unlink (capsfilter_src, sinkpad)) {
    GST_ERROR ("Can not unlink %" GST_PTR_FORMAT " and %" GST_PTR_FORMAT,
        srcpad, sinkpad);
  }

  gst_element_release_request_pad (adder, sinkpad);

  if (GST_PAD_TEMPLATE_PRESENCE (GST_PAD_PAD_TEMPLATE (srcpad)) ==
      GST_PAD_REQUEST) {
    gst_element_release_request_pad (src, srcpad);
  }

end:
  g_clear_object (&srcpad);
  g_clear_object (&src);

  if (capsfilter_src) {
    g_object_unref (capsfilter_src);
  }

  if (capsfilter) {
    gst_bin_remove (GST_BIN (GST_OBJECT_PARENT (capsfilter)), capsfilter);
    gst_element_set_state (capsfilter, GST_STATE_NULL);
    g_object_unref (capsfilter);
  }
}

static void
unlink_adder_sources (GstElement * adder)
{
  GstIterator *it;

  it = gst_element_iterate_sink_pads (adder);

  while (gst_iterator_foreach (it, unlink_adder_sink,
          adder) == GST_ITERATOR_RESYNC) {
    gst_iterator_resync (it);
  }

  gst_iterator_free (it);
}

static void
unlinked_pad (GstPad * pad, GstPad * peer, gpointer user_data)
{
  GstElement *agnostic = NULL, *adder = NULL, *typefind = NULL, *parent;
  KmsAudioMixer *self;
  gchar *padname;

  GST_DEBUG ("Unlinked pad %" GST_PTR_FORMAT, pad);
  parent = gst_pad_get_parent_element (pad);

  if (parent == NULL)
    return;

  self = KMS_AUDIO_MIXER (parent);

  if (gst_pad_get_direction (pad) != GST_PAD_SINK)
    goto end;

  padname = gst_pad_get_name (pad);

  KMS_AUDIO_MIXER_LOCK (self);

  typefind = g_hash_table_lookup (self->priv->typefinds, padname);
  if (typefind != NULL) {
    g_hash_table_remove (self->priv->typefinds, padname);
  }

  if (self->priv->agnostics != NULL) {
    agnostic = g_hash_table_lookup (self->priv->agnostics, padname);
    g_hash_table_remove (self->priv->agnostics, padname);
  }

  if (self->priv->adders != NULL) {
    adder = g_hash_table_lookup (self->priv->adders, padname);
    g_hash_table_remove (self->priv->adders, padname);
  }

  KMS_AUDIO_MIXER_UNLOCK (self);

  g_free (padname);

  if (GST_STATE (parent) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (parent) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (parent) >= GST_STATE_PAUSED) {
    if (typefind != NULL) {
      GST_WARNING_OBJECT (pad, "Removed before connecting branch");
      kms_audio_mixer_remove_elements (self, agnostic, adder);
      gst_object_ref (typefind);
      gst_element_set_locked_state (typefind, TRUE);
      gst_element_set_state (typefind, GST_STATE_NULL);
      gst_bin_remove (GST_BIN (self), typefind);
      gst_object_unref (typefind);
    } else {
      kms_audio_mixer_remove_elements (self, agnostic, adder);
    }
  } else {
    kms_audio_mixer_remove_elements (self, agnostic, adder);
  }

  gst_ghost_pad_set_target (GST_GHOST_PAD (pad), NULL);

end:
  gst_object_unref (parent);
}

static void
set_target_cb (GstPad * pad, GstPad * peer, gpointer tee)
{
  GstPad *srcpad = gst_element_request_pad_simple (GST_ELEMENT (tee), "src_%u");

  gst_ghost_pad_set_target (GST_GHOST_PAD (pad), srcpad);
}

static void
remove_target_cb (GstPad * pad, GstPad * peer, gpointer tee)
{
  GstPad *old_target = gst_ghost_pad_get_target (GST_GHOST_PAD (pad));

  gst_ghost_pad_set_target (GST_GHOST_PAD (pad), NULL);

  if (old_target != NULL) {
    gst_element_release_request_pad (GST_ELEMENT (tee), old_target);
    g_object_unref (old_target);
  }
}

static gboolean
kms_audio_mixer_add_src_pad (KmsAudioMixer * self, const char *padname)
{
  GstPad *srcpad = NULL, *pad, *sinkpad = NULL;
  GstElement *adder;
  GstElement *audiotestsrc;
  GstElement *tee, *fakesink;
  gchar *srcname;
  gint id;

  if ((id = get_stream_id_from_padname (padname)) < 0) {
    GST_ERROR_OBJECT (self, "Can not get pad id from element %s", padname);
    return FALSE;
  }

  adder = kms_utils_element_factory_make ("audiomixer", PLUGIN_NAME);
  tee = kms_utils_element_factory_make ("tee", PLUGIN_NAME);
  fakesink = kms_utils_element_factory_make ("fakesink", PLUGIN_NAME);

  // clang-format off
  g_object_set (adder,
      "latency", LATENCY * GST_MSECOND,
      "start-time-selection", 1 , // "first"
      NULL);
  // clang-format on
  g_object_set (tee, "allow-not-linked", TRUE, NULL);
  g_object_set (fakesink, "sync", FALSE, "async", FALSE, NULL);

  g_object_set_qdata_full (G_OBJECT (adder), key_sink_pad_name_quark (),
      g_strdup (padname), g_free);

  audiotestsrc = kms_utils_element_factory_make ("audiotestsrc", PLUGIN_NAME);
  g_object_set (audiotestsrc, "is-live", TRUE, NULL);
  gst_util_set_object_arg (G_OBJECT (audiotestsrc), "wave", "silence");

  GstElement* capsfilter_src = kms_audio_selector_create_capsfilter (self);
  GstElement* capsfilter_sink = kms_audio_selector_create_capsfilter (self);

  gst_bin_add_many (GST_BIN (self), audiotestsrc, capsfilter_src, adder,
      capsfilter_sink, tee, fakesink, NULL);

  gst_element_link (audiotestsrc, capsfilter_src);
  srcpad = gst_element_get_static_pad (capsfilter_src, "src");
  if (srcpad == NULL) {
    GST_ERROR ("Could not get src pad in %" GST_PTR_FORMAT, audiotestsrc);
    goto no_audiotestsrc;
  }

  sinkpad = gst_element_request_pad_simple (adder, "sink_%u");
  if (sinkpad == NULL) {
    GST_ERROR ("Could not get sink pad in %" GST_PTR_FORMAT, adder);
    goto no_audiotestsrc;
  }

  gst_pad_add_probe (sinkpad,
      GST_PAD_PROBE_TYPE_QUERY_UPSTREAM,
      (GstPadProbeCallback) cb_latency, NULL, NULL);

  GST_DEBUG ("Linking %" GST_PTR_FORMAT " to %" GST_PTR_FORMAT, srcpad,
      sinkpad);

  if (gst_pad_link (srcpad, sinkpad) != GST_PAD_LINK_OK) {
    GST_ERROR ("Could not link %" GST_PTR_FORMAT " to %" GST_PTR_FORMAT, srcpad,
        sinkpad);
    gst_element_release_request_pad (adder, sinkpad);
  }

  gst_element_link_many (adder, capsfilter_sink, tee, fakesink, NULL);

no_audiotestsrc:
  if (srcpad != NULL) {
    g_object_unref (srcpad);
  }
  if (sinkpad != NULL) {
    g_object_unref (sinkpad);
  }

  gst_element_sync_state_with_parent (fakesink);
  gst_element_sync_state_with_parent (tee);
  gst_element_sync_state_with_parent (capsfilter_sink);
  gst_element_sync_state_with_parent (adder);
  gst_element_sync_state_with_parent (capsfilter_src);
  gst_element_sync_state_with_parent (audiotestsrc);

  KMS_AUDIO_MIXER_LOCK (self);

  g_hash_table_foreach (self->priv->agnostics, (GHFunc) link_new_adder, adder);
  g_hash_table_insert (self->priv->adders, g_strdup (padname), adder);

  srcname = g_strdup_printf ("src_%u", id);
  pad = gst_ghost_pad_new_no_target (srcname, GST_PAD_SRC);
  g_signal_connect_object (pad, "linked", G_CALLBACK (set_target_cb), tee,
      (GConnectFlags)0);
  g_signal_connect_object (pad, "unlinked", G_CALLBACK (remove_target_cb), tee,
      (GConnectFlags)0);
  g_free (srcname);

  g_object_set_qdata (G_OBJECT (adder), key_tee_quark (), tee);
  g_object_set_qdata (G_OBJECT (adder), key_fakesink_quark (), fakesink);
  g_object_set_qdata (G_OBJECT (adder), key_pad_quark (), pad);
  g_object_set_qdata (G_OBJECT (adder), key_src_quark (), audiotestsrc);

  if (GST_STATE (self) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (self) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (self) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, TRUE);

  if (gst_element_add_pad (GST_ELEMENT (self), pad)) {
    KMS_AUDIO_MIXER_UNLOCK (self);
    gst_bin_recalculate_latency (GST_BIN (self));
    return TRUE;
  }

  /* ERROR */
  GST_ERROR_OBJECT (self, "Can not add pad %" GST_PTR_FORMAT, pad);
  g_hash_table_remove (self->priv->adders, padname);

  KMS_AUDIO_MIXER_UNLOCK (self);

  unlink_adder_sources (adder);

  gst_object_unref (pad);

  gst_element_set_locked_state (adder, TRUE);
  gst_element_set_state (adder, GST_STATE_NULL);

  gst_bin_remove (GST_BIN (self), adder);

  return FALSE;
}

static GstPad *
kms_audio_mixer_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps)
{
  KmsAudioMixer *self = KMS_AUDIO_MIXER (element);
  GstPad *sinkpad, *pad = NULL;
  GstElement *typefind;
  gchar *padname;

  if (templ !=
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS (G_OBJECT_GET_CLASS
              (element)), AUDIO_SINK_PAD))
    return NULL;

  typefind = kms_utils_element_factory_make ("typefind", PLUGIN_NAME);
  sinkpad = gst_element_get_static_pad (typefind, "sink");
  if (sinkpad == NULL) {
    gst_object_unref (typefind);
    return NULL;
  }

  gst_bin_add (GST_BIN (self), typefind);
  gst_element_sync_state_with_parent (typefind);

  KMS_AUDIO_MIXER_LOCK (self);

  padname = g_strdup_printf (AUDIO_SINK_PAD, self->priv->count++);

  pad = gst_ghost_pad_new (padname, sinkpad);
  g_object_unref (sinkpad);

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, TRUE);

  if (!gst_element_add_pad (element, pad)) {
    GST_ERROR_OBJECT (self, "Could not create sink pad");
  } else if (!kms_audio_mixer_add_src_pad (self, padname)) {
    GST_ERROR_OBJECT (self, "Could not create source pad");
    if (gst_pad_is_active (pad)) {
      gst_pad_set_active (pad, FALSE);
    }
    gst_element_remove_pad (element, pad);
  } else {
    g_hash_table_insert (self->priv->typefinds, g_strdup (padname), typefind);
    g_object_set_qdata_full (G_OBJECT (typefind), key_sink_pad_name_quark (),
        padname, g_free);
    g_signal_connect (G_OBJECT (typefind), "have-type",
        G_CALLBACK (kms_audio_mixer_have_type), self);
    goto end;
  }

  /* Error */
  g_object_unref (pad);
  gst_element_set_locked_state (typefind, TRUE);
  gst_element_set_state (typefind, GST_STATE_NULL);
  gst_bin_remove (GST_BIN (self), typefind);
  self->priv->count--;
  pad = NULL;
  g_free (padname);

end:
  KMS_AUDIO_MIXER_UNLOCK (self);

  if (pad != NULL) {
    g_signal_connect (G_OBJECT (pad), "unlinked",
        G_CALLBACK (unlinked_pad), NULL);
  }

  return pad;
}

static void
kms_audio_mixer_release_pad (GstElement * element, GstPad * pad)
{
  GST_DEBUG ("Release pad %" GST_PTR_FORMAT, pad);

  if (gst_pad_get_direction (pad) != GST_PAD_SINK)
    return;

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (element) >= GST_STATE_PAUSED) {
    gst_pad_set_active (pad, FALSE);
  }

  gst_element_remove_pad (element, pad);
}

static void
kms_audio_mixer_class_init (KmsAudioMixerClass * klass)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (gstelement_class,
      "AudioMixer", "Generic",
      "Audio mixer element",
      "Santiago Carot Nemesio <sancane at gmail dot com>");

  gstelement_class->request_new_pad =
      GST_DEBUG_FUNCPTR (kms_audio_mixer_request_new_pad);
  gstelement_class->release_pad =
      GST_DEBUG_FUNCPTR (kms_audio_mixer_release_pad);
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_sink_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_src_factory));

  gobject_class->dispose = GST_DEBUG_FUNCPTR (kms_audio_mixer_dispose);
  gobject_class->finalize = GST_DEBUG_FUNCPTR (kms_audio_mixer_finalize);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsAudioMixerPrivate));
}

static void
kms_audio_mixer_init (KmsAudioMixer * self)
{
  self->priv = KMS_AUDIO_MIXER_GET_PRIVATE (self);

  self->priv->adders = g_hash_table_new_full (g_str_hash, g_str_equal, g_free,
      NULL);
  self->priv->agnostics =
      g_hash_table_new_full (g_str_hash, g_str_equal, g_free, NULL);
  self->priv->typefinds =
      g_hash_table_new_full (g_str_hash, g_str_equal, g_free, NULL);

  g_rec_mutex_init (&self->priv->mutex);
  self->priv->loop = kms_loop_new ();
}

gboolean
kms_audio_mixer_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_AUDIO_MIXER);
}
