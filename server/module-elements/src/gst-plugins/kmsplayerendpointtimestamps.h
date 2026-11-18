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
#ifndef _KMS_PLAYER_ENDPOINT_TIMESTAMPS_H_
#define _KMS_PLAYER_ENDPOINT_TIMESTAMPS_H_

/* Timestamp arithmetic of the PlayerEndpoint in encoded mode. It is in a
 * header (and not in kmsplayerendpoint.c) so the unit tests can check it. */

#include <gst/gst.h>

/* In encoded mode the timestamps of each stream are translated to the running
 * time of their segment (the common timeline of the streams) before the offset
 * time shared by all of them is applied. The demuxers that use edit lists, such
 * as MP4, give each track a different segment start (and a new segment, with a
 * new base, at every edit), so raw timestamps of tracks that are aligned in the
 * timeline differ, and a segment that does not start at the end of the previous
 * one would make the raw timestamps go back.
 *
 * The result is unsigned, so a constant is added to keep it positive when the
 * buffer is before the start of its segment (a DTS below the PTS, audio
 * priming...). The constant is cancelled by the offset time, which is one of
 * the translated timestamps. Without a (time) segment the timestamp is only
 * shifted by the constant. */
#define KMS_PLAYER_SEGMENT_BIAS (3600 * GST_SECOND)

static inline GstClockTime
kms_player_endpoint_translate_to_segment (GstClockTime ts,
    const GstSegment * segment)
{
  GstClockTime running_time;
  gint64 translated;
  gint sign;

  if (!GST_CLOCK_TIME_IS_VALID (ts)) {
    return ts;
  }

  if (segment == NULL || segment->format != GST_FORMAT_TIME ||
      !GST_CLOCK_TIME_IS_VALID (segment->start) ||
      segment->start >= G_MAXINT64 / 4 || ts >= G_MAXINT64 / 4) {
    return ts + KMS_PLAYER_SEGMENT_BIAS;
  }

  sign = gst_segment_to_running_time_full (segment, GST_FORMAT_TIME, ts,
      &running_time);
  if (sign == 0 || running_time >= G_MAXINT64 / 4) {
    return ts + KMS_PLAYER_SEGMENT_BIAS;
  }

  translated = (gint64) KMS_PLAYER_SEGMENT_BIAS +
      (sign > 0 ? (gint64) running_time : -(gint64) running_time);

  return translated > 0 ? (GstClockTime) translated : 0;
}

/* Lowest base time that keeps the PTS of the buffers that come next (the first
 * one, which has the lowest, is "pts_orig") above the highest one pushed so
 * far. With B-frames pts_orig is ahead of the offset time, so that lead can be
 * taken into account. In encoded mode (use_lead) the offset time is shared and
 * a stream may start before it, so the lead is negative then: the base time has
 * to grow by that difference, otherwise the buffers would be translated below
 * the last ones and dropped (a reset keeps last_pts and last_dts). The same
 * goes for the DTS, which is the one that has to keep increasing. */
static inline GstClockTime
kms_player_endpoint_calc_min_base_time (GstClockTime last_pts,
    GstClockTime last_dts, GstClockTime pts_orig, GstClockTime dts_orig,
    GstClockTime offset_time, gboolean use_lead)
{
  gint64 lead = use_lead ? (gint64) pts_orig - (gint64) offset_time : 0;
  gint64 min_base = (gint64) last_pts + (gint64) GST_MSECOND - lead;

  if (use_lead && last_dts != GST_CLOCK_TIME_NONE) {
    gint64 min_base_dts = (gint64) last_dts + (gint64) GST_MSECOND +
        (gint64) offset_time - (gint64) dts_orig;

    min_base = MAX (min_base, min_base_dts);
  }

  return min_base > 0 ? (GstClockTime) min_base : 0;
}

#endif /* _KMS_PLAYER_ENDPOINT_TIMESTAMPS_H_ */
