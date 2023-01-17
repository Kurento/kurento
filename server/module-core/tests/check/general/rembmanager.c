/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

#include "kmsutils.h"

#include <gst/check/gstcheck.h>
#include <glib.h>

static void
bitrate_cb (RembEventManager * manager, guint bitrate, gpointer user_data)
{
  guint *min_br = (guint *) user_data;

  *min_br = bitrate;
}

GST_START_TEST (check_min_br_update)
{
  GstPad *pad;
  RembEventManager *manager;
  GstEvent *event;
  guint min_br;

  pad = gst_pad_new (NULL, GST_PAD_SRC);
  gst_pad_set_active (pad, TRUE);
  manager = kms_utils_remb_event_manager_create (pad);
  kms_utils_remb_event_manager_set_callback (manager, bitrate_cb, &min_br,
      NULL);

  /* SSRC_1: set min */
  event = kms_utils_remb_event_upstream_new (100, 1);
  gst_pad_send_event (pad, event);
  fail_unless (min_br == 100);

  /* SSRC_1: update min */
  event = kms_utils_remb_event_upstream_new (200, 1);
  gst_pad_send_event (pad, event);
  fail_unless (min_br == 200);

  /* SSRC_2: not update min */
  event = kms_utils_remb_event_upstream_new (300, 2);
  gst_pad_send_event (pad, event);
  fail_unless (min_br == 200);

  /* SSRC_2: update min */
  event = kms_utils_remb_event_upstream_new (100, 2);
  gst_pad_send_event (pad, event);
  fail_unless (min_br == 100);

  /* SSRC_1: not update min */
  event = kms_utils_remb_event_upstream_new (200, 1);
  gst_pad_send_event (pad, event);
  fail_unless (min_br == 100);

  /* SSRC_2: min is the last SSRC_1 br */
  event = kms_utils_remb_event_upstream_new (300, 2);
  gst_pad_send_event (pad, event);
  fail_unless (min_br == 200);

  /* SSRC_2: not update min */
  event = kms_utils_remb_event_upstream_new (300, 2);
  gst_pad_send_event (pad, event);
  fail_unless (min_br == 200);

  kms_utils_remb_event_manager_destroy (manager);
  g_object_unref (pad);
}

GST_END_TEST;

/*
 * Check that repeated high bitrate is took into account
 * and minimum value is updated after a lower bitrate
 * is removed in the clearing time.
 */
GST_START_TEST (check_take_into_account_after_clear_time)
{
  GstPad *pad;
  RembEventManager *manager;
  GstEvent *event;
  guint min_br;
  GstClockTime v;

  pad = gst_pad_new (NULL, GST_PAD_SRC);
  gst_pad_set_active (pad, TRUE);
  manager = kms_utils_remb_event_manager_create (pad);
  kms_utils_remb_event_manager_set_callback (manager, bitrate_cb, &min_br,
      NULL);

  /* SSRC_1: set min */
  event = kms_utils_remb_event_upstream_new (100, 1);
  gst_pad_send_event (pad, event);
  fail_unless (min_br == 100);

  /* SSRC_2: not update min */
  event = kms_utils_remb_event_upstream_new (200, 2);
  gst_pad_send_event (pad, event);
  fail_unless (min_br == 100);

  kms_utils_remb_event_manager_set_clear_interval (manager, 5000);
  v = kms_utils_remb_event_manager_get_clear_interval (manager);
  fail_unless (v == 5000);

  kms_utils_remb_event_manager_set_clear_interval (manager, 50);
  v = kms_utils_remb_event_manager_get_clear_interval (manager);
  fail_unless (v == 50);

  /* SSRC_2: clear entries and update min */
  event = kms_utils_remb_event_upstream_new (200, 2);
  gst_pad_send_event (pad, event);
  fail_unless (min_br == 200);

  kms_utils_remb_event_manager_destroy (manager);
  g_object_unref (pad);
}

GST_END_TEST;

/* Suite initialization */
static Suite *
rembmanager_suite (void)
{
  Suite *s = suite_create ("rembmanager");
  TCase *tc_chain = tcase_create ("element");

  suite_add_tcase (s, tc_chain);
  tcase_add_test (tc_chain, check_min_br_update);
  tcase_add_test (tc_chain, check_take_into_account_after_clear_time);

  return s;
}

GST_CHECK_MAIN (rembmanager);
