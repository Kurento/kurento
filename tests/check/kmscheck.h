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

#define KMS_CHECK_MAIN(name)                                    \
int main (int argc, char **argv)                                \
{                                                               \
  Suite *s;                                                     \
  int ret;                                                      \
  gst_check_init (&argc, &argv);                                \
  s = name ## _suite ();                                        \
  ret = gst_check_run_suite (s, # name, __FILE__);              \
  gst_deinit ();                                                \
  return ret;                                                   \
}

#define KMS_END_TEST GST_LOG ("cleaning up tasks"); \
                     gst_task_cleanup_all (); \
                     gst_deinit (); \
                     END_TEST
