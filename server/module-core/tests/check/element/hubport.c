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
#include <gst/check/gstcheck.h>
#include <gst/gst.h>
#include <glib.h>

#define HUB_AUDIO_SINK "hub_audio_sink"
#define HUB_VIDEO_SINK "hub_video_sink"

#define KMS_ELEMENT_PAD_TYPE_DATA 0
#define KMS_ELEMENT_PAD_TYPE_AUDIO 1
#define KMS_ELEMENT_PAD_TYPE_VIDEO 2

static void
pad_added (GstElement * element, GstPad * new_pad, gpointer user_data)
{
  GSList **added_apds = (GSList **) user_data;
  gchar *name;

  GST_DEBUG_OBJECT (element, "Added pad %" GST_PTR_FORMAT, new_pad);
  name = gst_pad_get_name (new_pad);

  *added_apds = g_slist_append (*added_apds, name);
}

GST_START_TEST (connect_srcs)
{
  GstElement *hubport = gst_element_factory_make ("hubport", NULL);
  gchar *video_pad_name, *audio_pad_name;
  GSList *added_apds = NULL;
  GstPad *sink, *src;

  g_signal_connect (hubport, "pad-added", G_CALLBACK (pad_added), &added_apds);

  src = gst_element_get_request_pad (hubport, "video_src_%u");
  fail_unless (src == NULL);

  sink = gst_element_get_request_pad (hubport, HUB_VIDEO_SINK);
  fail_unless (sink != NULL);
  fail_unless (g_strcmp0 (GST_OBJECT_NAME (sink), HUB_VIDEO_SINK) == 0);

  /* request src pad using action */
  g_signal_emit_by_name (hubport, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_VIDEO, NULL, GST_PAD_SRC, &video_pad_name);
  fail_if (video_pad_name == NULL);

  GST_DEBUG ("Pad name %s", video_pad_name);

  g_object_unref (sink);

  src = gst_element_get_request_pad (hubport, "audio_src_%u");
  fail_unless (src == NULL);
  sink = gst_element_get_request_pad (hubport, HUB_AUDIO_SINK);
  fail_unless (sink != NULL);
  fail_unless (g_strcmp0 (GST_OBJECT_NAME (sink), HUB_AUDIO_SINK) == 0);

  /* request src pad using action */
  g_signal_emit_by_name (hubport, "request-new-pad",
      KMS_ELEMENT_PAD_TYPE_AUDIO, NULL, GST_PAD_SRC, &audio_pad_name);
  fail_if (audio_pad_name == NULL);

  GST_DEBUG ("Pad name %s", audio_pad_name);

  g_object_unref (sink);
  g_object_unref (hubport);

  fail_if (g_slist_find_custom (added_apds, audio_pad_name,
          (GCompareFunc) g_strcmp0) == NULL);
  fail_if (g_slist_find_custom (added_apds, video_pad_name,
          (GCompareFunc) g_strcmp0) == NULL);

  g_free (audio_pad_name);
  g_free (video_pad_name);
  g_slist_free_full (added_apds, g_free);
}

GST_END_TEST
GST_START_TEST (connect_sinks)
{
  GstBin *pipe = (GstBin *) gst_pipeline_new ("connect_sinks");
  GstElement *hubport = gst_element_factory_make ("hubport", NULL);
  GstElement *videosrc = gst_element_factory_make ("videotestsrc", NULL);
  GstElement *audiosrc = gst_element_factory_make ("audiotestsrc", NULL);
  GstElement *fakesink1 = gst_element_factory_make ("fakesink", NULL);
  GstElement *fakesink2 = gst_element_factory_make ("fakesink", NULL);

  gst_bin_add_many (pipe, hubport, videosrc, audiosrc, fakesink1, fakesink2,
      NULL);

  /* By default sink pads does not exists until hub port is internally connected */

  fail_if (gst_element_get_static_pad (hubport, "sink_video_default"));
  fail_if (gst_element_get_static_pad (hubport, "sink_audio_default"));

  /* Check if pads have been created now that internal pads have been linked */

  fail_unless (gst_element_link_pads (hubport, "hub_video_src", fakesink1,
          "sink"));
  fail_unless (gst_element_link_pads (hubport, "hub_audio_src", fakesink2,
          "sink"));

  fail_unless (gst_element_link_pads (videosrc, "src", hubport,
          "sink_video_default"));
  fail_unless (gst_element_link_pads (audiosrc, "src", hubport,
          "sink_audio_default"));

  g_object_unref (pipe);
}

GST_END_TEST
GST_START_TEST (create_element)
{
  GstElement *hubport;

  hubport = gst_element_factory_make ("hubport", NULL);

  fail_unless (hubport != NULL);

  g_object_unref (hubport);
}

GST_END_TEST static Suite *
hubport_suite (void)
{
  Suite *s = suite_create ("hubport");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);

  tcase_add_test (tc_chain, create_element);
  tcase_add_test (tc_chain, connect_sinks);
  tcase_add_test (tc_chain, connect_srcs);

  return s;
}

GST_CHECK_MAIN (hubport);
