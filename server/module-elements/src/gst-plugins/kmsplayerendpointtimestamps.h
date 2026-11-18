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
#ifndef _KMS_PLAYER_ENDPOINT_TIMESTAMPS_H_
#define _KMS_PLAYER_ENDPOINT_TIMESTAMPS_H_

/* Timestamp arithmetic of the PlayerEndpoint in encoded mode. It is in a
 * header (and not in kmsplayerendpoint.c) so the unit tests can check it. */

#include <gst/gst.h>

/* In encoded mode the timestamps of each stream are translated by the start of
 * its segment before the offset time shared by all the streams is applied (the
 * demuxers that use edit lists, such as MP4, give each track a different
 * segment start, and the position of a buffer in the common timeline is its
 * timestamp minus that start). The translated values are unsigned, so a
 * constant is added to keep them positive when a buffer is before the start of
 * its segment (a DTS below the PTS, audio priming...). The constant is
 * cancelled by the offset time, which is one of the translated timestamps. */
#define KMS_PLAYER_SEGMENT_BIAS (3600 * GST_SECOND)

static inline GstClockTime
kms_player_endpoint_translate_to_segment (GstClockTime ts,
    GstClockTime seg_start)
{
  gint64 translated;

  if (!GST_CLOCK_TIME_IS_VALID (ts)) {
    return ts;
  }

  translated = (gint64) ts - (gint64) seg_start + (gint64) KMS_PLAYER_SEGMENT_BIAS;

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
