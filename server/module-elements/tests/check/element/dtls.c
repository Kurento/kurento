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
#include <gio/gio.h>
#include <glib/gstdio.h>

#define FILE_PERMISIONS (S_IRWXU | S_IRWXG | S_IRWXO)
#define ROOT_TMP_DIR "/tmp/kms_dtls_tests"
#define TMP_DIR_TEMPLATE ROOT_TMP_DIR "/XXXXXX"

#define CERTTOOL_TEMPLATE "/tmp/certtool.tmpl"
#define CERT_KEY_PEM_FILE "certkey.pem"

/* Temporaly disabled */
#if 0
#define CLIENT_RECEIVES "client-receives"
G_DEFINE_QUARK (CLIENT_RECEIVES, client_receives);

#define SERVER_RECEIVES "server-receives"
G_DEFINE_QUARK (SERVER_RECEIVES, server_receives);

G_LOCK_DEFINE_STATIC (check_receive_lock);
#endif /* Temporaly disabled */

static void
bus_msg (GstBus * bus, GstMessage * msg, gpointer pipe)
{
  switch (GST_MESSAGE_TYPE (msg)) {
    case GST_MESSAGE_ERROR:{
      gchar *error_file = g_strdup_printf ("error-%s", GST_OBJECT_NAME (pipe));

      GST_ERROR ("Error: %" GST_PTR_FORMAT, msg);
      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipe),
          GST_DEBUG_GRAPH_SHOW_ALL, error_file);
      g_free (error_file);
      fail ("Error received on bus");
      break;
    }
    case GST_MESSAGE_WARNING:{
      gchar *warn_file = g_strdup_printf ("warning-%s", GST_OBJECT_NAME (pipe));

      GST_WARNING ("Warning: %" GST_PTR_FORMAT, msg);
      GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipe),
          GST_DEBUG_GRAPH_SHOW_ALL, warn_file);
      g_free (warn_file);
      break;
    }
    default:
      break;
  }
}

static gchar *
generate_certkey_pem_file_path ()
{
  gchar t[] = TMP_DIR_TEMPLATE;
  gchar *dir;

  g_mkdir_with_parents (ROOT_TMP_DIR, FILE_PERMISIONS);
  dir = mkdtemp (t);

  return g_strdup_printf ("%s/%s", dir, CERT_KEY_PEM_FILE);
}

static gboolean
generate_certkey_pem_file (const gchar * cert_key_pem_file)
{
  gchar *cmd;
  int ret;

  cmd =
      g_strconcat ("/bin/sh -c \"certtool --generate-privkey --outfile ",
      cert_key_pem_file, "\"", NULL);
  ret = system (cmd);
  g_free (cmd);

  if (ret == -1)
    return FALSE;

  cmd =
      g_strconcat
      ("/bin/sh -c \"echo 'organization = kurento' > ", CERTTOOL_TEMPLATE,
      " && certtool --generate-self-signed --load-privkey ", cert_key_pem_file,
      " --template ", CERTTOOL_TEMPLATE, " >> ", cert_key_pem_file,
      " 2>/dev/null\"", NULL);
  ret = system (cmd);
  g_free (cmd);

  return (ret != -1);
}

static gboolean
quit_main_loop (gpointer loop)
{
  g_main_loop_quit (loop);

  return FALSE;
}

static void
fakesink_dtlsenc_client_preroll_hand_off (GstElement * fakesink,
    GstBuffer * buf, GstPad * pad, gpointer loop)
{
  GST_INFO_OBJECT (buf, "PREROLL received");
  g_idle_add (quit_main_loop, loop);
}

/* Temporaly disabled */
#if 0
static void
fakesink_dtls_client_hand_off (GstElement * fakesink, GstBuffer * buf,
    GstPad * pad, gpointer loop)
{
  GstElement *pipeline = GST_ELEMENT (gst_element_get_parent (fakesink));

  GST_INFO_OBJECT (fakesink, "BUF received");

  G_LOCK (check_receive_lock);
  if (GPOINTER_TO_INT (g_object_get_qdata (G_OBJECT (pipeline),
              server_receives_quark ()))) {
    g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
    g_idle_add (quit_main_loop, loop);
  } else {
    g_object_set_qdata (G_OBJECT (pipeline), client_receives_quark (),
        GINT_TO_POINTER (TRUE));
  }
  G_UNLOCK (check_receive_lock);

  g_object_unref (pipeline);
}

static void
fakesink_dtls_server_hand_off (GstElement * fakesink, GstBuffer * buf,
    GstPad * pad, gpointer loop)
{
  GstElement *pipeline = GST_ELEMENT (gst_element_get_parent (fakesink));

  GST_INFO_OBJECT (fakesink, "BUF received");

  G_LOCK (check_receive_lock);
  if (GPOINTER_TO_INT (g_object_get_qdata (G_OBJECT (pipeline),
              client_receives_quark ()))) {
    g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
    g_idle_add (quit_main_loop, loop);
  } else {
    g_object_set_qdata (G_OBJECT (pipeline), server_receives_quark (),
        GINT_TO_POINTER (TRUE));
  }
  G_UNLOCK (check_receive_lock);

  g_object_unref (pipeline);
}
#endif /* Temporaly disabled */

GST_START_TEST (test_dtlsenc_init_handshake_on_paused)
{
  gchar *cert_key_pem_file;
  GTlsConnection *conn;

  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstElement *pipeline = gst_pipeline_new (__FUNCTION__);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  GstElement *dtlsenc_client = gst_element_factory_make ("kmsdtlsenc", NULL);
  GstElement *dtlsdec_client = gst_element_factory_make ("kmsdtlsdec", NULL);
  GstElement *fakesink = gst_element_factory_make ("fakesink", NULL);

  cert_key_pem_file = generate_certkey_pem_file_path ();
  generate_certkey_pem_file (cert_key_pem_file);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (fakesink), "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (fakesink), "preroll-handoff",
      G_CALLBACK (fakesink_dtlsenc_client_preroll_hand_off), loop);

  g_object_set (G_OBJECT (dtlsenc_client), "channel-id", "client-id", NULL);
  g_object_set (G_OBJECT (dtlsenc_client), "is-client", TRUE, NULL);
  g_object_set (G_OBJECT (dtlsdec_client), "channel-id", "client-id", NULL);
  g_object_set (G_OBJECT (dtlsdec_client), "is-client", TRUE, NULL);
  g_object_set (G_OBJECT (dtlsdec_client), "certificate-pem-file",
      cert_key_pem_file, NULL);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), dtlsenc_client, fakesink, NULL);
  mark_point ();

  gst_element_link_many (dtlsenc_client, fakesink, NULL);
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL,
      "test_dtlsenc_init_handshake_on_paused_before_playing");

  gst_element_set_state (fakesink, GST_STATE_PLAYING);
  gst_element_set_state (dtlsenc_client, GST_STATE_PAUSED);

  /* Init DTLS handshake over dtlsenc connection */
  g_object_get (dtlsenc_client, "tls-connection", &conn, NULL);
  g_tls_connection_handshake_async (conn, G_PRIORITY_DEFAULT, NULL, NULL, NULL);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL,
      "test_dtlsenc_init_handshake_on_paused_before_entering_loop");

  g_main_loop_run (loop);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "test_dtlsenc_init_handshake_on_paused_end");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_object_unref (dtlsdec_client);
  g_main_loop_unref (loop);

  g_remove (cert_key_pem_file);
  g_free (cert_key_pem_file);
}

GST_END_TEST
/* Temporaly disabled */
#if 0
GST_START_TEST (test_dtls_send_recv_data)
{
  gchar *cert_key_pem_file;
  GTlsConnection *conn;

  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstElement *pipeline = gst_pipeline_new (__FUNCTION__);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  GstElement *fakesrc_client = gst_element_factory_make ("fakesrc", NULL);
  GstElement *dtlsenc_client = gst_element_factory_make ("kmsdtlsenc", NULL);
  GstElement *dtlsdec_client = gst_element_factory_make ("kmsdtlsdec", NULL);
  GstElement *fakesink_client = gst_element_factory_make ("fakesink", NULL);

  GstElement *fakesrc_server = gst_element_factory_make ("fakesrc", NULL);
  GstElement *dtlsenc_server = gst_element_factory_make ("kmsdtlsenc", NULL);
  GstElement *dtlsdec_server = gst_element_factory_make ("kmsdtlsdec", NULL);
  GstElement *fakesink_server = gst_element_factory_make ("fakesink", NULL);

  cert_key_pem_file = generate_certkey_pem_file_path ();
  generate_certkey_pem_file (cert_key_pem_file);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (fakesink_client), "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (fakesink_client), "handoff",
      G_CALLBACK (fakesink_dtls_client_hand_off), loop);
  g_object_set (G_OBJECT (fakesink_server), "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (fakesink_server), "handoff",
      G_CALLBACK (fakesink_dtls_server_hand_off), loop);

  g_object_set (G_OBJECT (fakesrc_client), "sizetype", 2, NULL);
  g_object_set (G_OBJECT (fakesrc_server), "sizetype", 2, NULL);

  g_object_set (G_OBJECT (dtlsenc_client), "channel-id", "client-id", NULL);
  g_object_set (G_OBJECT (dtlsenc_client), "is-client", TRUE, NULL);
  g_object_set (G_OBJECT (dtlsdec_client), "channel-id", "client-id", NULL);
  g_object_set (G_OBJECT (dtlsdec_client), "is-client", TRUE, NULL);
  g_object_set (G_OBJECT (dtlsdec_client), "certificate-pem-file",
      cert_key_pem_file, NULL);

  g_object_set (G_OBJECT (dtlsenc_server), "channel-id", "server-id", NULL);
  g_object_set (G_OBJECT (dtlsenc_server), "is-client", FALSE, NULL);
  g_object_set (G_OBJECT (dtlsdec_server), "channel-id", "server-id", NULL);
  g_object_set (G_OBJECT (dtlsdec_server), "is-client", FALSE, NULL);
  g_object_set (G_OBJECT (dtlsdec_server), "certificate-pem-file",
      cert_key_pem_file, NULL);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), fakesrc_client, dtlsdec_client,
      dtlsenc_client, fakesink_client, NULL);
  gst_bin_add_many (GST_BIN (pipeline), fakesrc_server, dtlsdec_server,
      dtlsenc_server, fakesink_server, NULL);
  mark_point ();

  gst_element_link_many (fakesrc_client, dtlsenc_client, dtlsdec_server,
      fakesink_server, NULL);
  gst_element_link_many (fakesrc_server, dtlsenc_server, dtlsdec_client,
      fakesink_client, NULL);
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "dtls_before_playing");

  gst_element_set_state (fakesink_server, GST_STATE_PLAYING);
  gst_element_set_state (dtlsdec_server, GST_STATE_PLAYING);
  gst_element_set_state (dtlsenc_client, GST_STATE_PLAYING);
  gst_element_set_state (fakesrc_client, GST_STATE_PLAYING);

  gst_element_set_state (fakesink_client, GST_STATE_PLAYING);
  gst_element_set_state (dtlsdec_client, GST_STATE_PLAYING);
  gst_element_set_state (dtlsenc_server, GST_STATE_PLAYING);
  gst_element_set_state (fakesrc_server, GST_STATE_PLAYING);

  /* Init DTLS handshake over dtlsenc connection */
  g_object_get (dtlsenc_client, "tls-connection", &conn, NULL);
  g_tls_connection_handshake_async (conn, G_PRIORITY_DEFAULT, NULL, NULL, NULL);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "dtls_before_entering_loop");

  g_main_loop_run (loop);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "dtls_end");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);

  g_remove (cert_key_pem_file);
  g_free (cert_key_pem_file);
}

GST_END_TEST
#endif /* Temporaly disabled */
/*
 * End of test cases
 */
static Suite *
dtls_suite (void)
{
  Suite *s = suite_create ("dtls");
  TCase *tc_chain = tcase_create ("elements");

  suite_add_tcase (s, tc_chain);
  tcase_add_test (tc_chain, test_dtlsenc_init_handshake_on_paused);
/* Temporaly disabled */
#if 0
  tcase_add_test (tc_chain, test_dtls_send_recv_data);
#endif /* Temporaly disabled */

  return s;
}

GST_CHECK_MAIN (dtls);
