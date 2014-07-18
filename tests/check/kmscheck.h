/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
