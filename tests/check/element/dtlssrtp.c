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
#define ROOT_TMP_DIR "/tmp/kms_dtlssrtp_tests"
#define TMP_DIR_TEMPLATE ROOT_TMP_DIR "/XXXXXX"

#define CERTTOOL_TEMPLATE "/tmp/certtool.tmpl"
#define CERT_KEY_PEM_FILE "certkey.pem"

#define CLIENT_RECEIVES_VIDEO "client-receives-video"
G_DEFINE_QUARK (CLIENT_RECEIVES_VIDEO, client_receives_video);

#define SERVER_RECEIVES_VIDEO "server-receives-video"
G_DEFINE_QUARK (SERVER_RECEIVES_VIDEO, server_receives_video);

G_LOCK_DEFINE_STATIC (check_receive_lock);

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

static GSocket *
open_socket (guint16 port)
{
  GSocket *socket;
  GSocketAddress *bind_saddr;
  GInetAddress *addr;

  socket = g_socket_new (G_SOCKET_FAMILY_IPV4, G_SOCKET_TYPE_DATAGRAM,
      G_SOCKET_PROTOCOL_UDP, NULL);
  if (socket == NULL)
    return NULL;

  addr = g_inet_address_new_any (G_SOCKET_FAMILY_IPV4);
  bind_saddr = g_inet_socket_address_new (addr, port);
  g_object_unref (addr);
  if (!g_socket_bind (socket, bind_saddr, TRUE, NULL)) {
    g_socket_close (socket, NULL);
    g_object_unref (socket);
    socket = NULL;
  }
  g_object_unref (bind_saddr);

  return socket;
}

static void
finalize_socket (GSocket ** socket)
{
  if (socket == NULL || *socket == NULL)
    return;

  g_socket_close (*socket, NULL);
  g_object_unref (*socket);
  *socket = NULL;
}

static guint16
get_socket_port (GSocket * socket)
{
  GInetSocketAddress *addr;
  guint16 port;

  addr = G_INET_SOCKET_ADDRESS (g_socket_get_local_address (socket, NULL));
  if (!addr)
    return 0;

  port = g_inet_socket_address_get_port (addr);
  g_inet_socket_address_get_address (addr);
  g_object_unref (addr);

  return port;
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
fakesink_client_hand_off (GstElement * fakesink, GstBuffer * buf,
    GstPad * pad, gpointer loop)
{
  GstElement *pipeline = GST_ELEMENT (gst_element_get_parent (fakesink));

  G_LOCK (check_receive_lock);
  if (GPOINTER_TO_INT (g_object_get_qdata (G_OBJECT (pipeline),
              server_receives_video_quark ()))) {
    g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
    g_idle_add (quit_main_loop, loop);
  } else {
    g_object_set_qdata (G_OBJECT (pipeline), client_receives_video_quark (),
        GINT_TO_POINTER (TRUE));
  }
  G_UNLOCK (check_receive_lock);

  g_object_unref (pipeline);
}

static void
fakesink_server_hand_off (GstElement * fakesink, GstBuffer * buf,
    GstPad * pad, gpointer loop)
{
  GstElement *pipeline = GST_ELEMENT (gst_element_get_parent (fakesink));

  G_LOCK (check_receive_lock);
  if (GPOINTER_TO_INT (g_object_get_qdata (G_OBJECT (pipeline),
              client_receives_video_quark ()))) {
    g_object_set (G_OBJECT (fakesink), "signal-handoffs", FALSE, NULL);
    g_idle_add (quit_main_loop, loop);
  } else {
    g_object_set_qdata (G_OBJECT (pipeline), server_receives_video_quark (),
        GINT_TO_POINTER (TRUE));
  }
  G_UNLOCK (check_receive_lock);

  g_object_unref (pipeline);
}

GST_START_TEST (test_dtlssrtp)
{
  gchar *cert_key_pem_file;

  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstElement *pipeline = gst_pipeline_new (__FUNCTION__);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  GstElement *videotestsrc_client =
      gst_element_factory_make ("videotestsrc", NULL);
  GstElement *vp8enc_client = gst_element_factory_make ("vp8enc", NULL);
  GstElement *rtpvp8pay_client = gst_element_factory_make ("rtpvp8pay", NULL);
  GstElement *dtlssrtpenc_client =
      gst_element_factory_make ("kmsdtlssrtpenc", NULL);
  GstElement *dtlssrtpdec_client =
      gst_element_factory_make ("kmsdtlssrtpdec", NULL);
  GSocket *socket_client;
  GstElement *udpsink_client = gst_element_factory_make ("udpsink", NULL);
  GstElement *udpsrc_client = gst_element_factory_make ("udpsrc", NULL);
  GstElement *fakesink_client = gst_element_factory_make ("fakesink", NULL);

  GstElement *videotestsrc_server =
      gst_element_factory_make ("videotestsrc", NULL);
  GstElement *vp8enc_server = gst_element_factory_make ("vp8enc", NULL);
  GstElement *rtpvp8pay_server = gst_element_factory_make ("rtpvp8pay", NULL);
  GstElement *dtlssrtpenc_server =
      gst_element_factory_make ("kmsdtlssrtpenc", NULL);
  GstElement *dtlssrtpdec_server =
      gst_element_factory_make ("kmsdtlssrtpdec", NULL);
  GSocket *socket_server;
  GstElement *udpsink_server = gst_element_factory_make ("udpsink", NULL);
  GstElement *udpsrc_server = gst_element_factory_make ("udpsrc", NULL);
  GstElement *fakesink_server = gst_element_factory_make ("fakesink", NULL);

  cert_key_pem_file = generate_certkey_pem_file_path ();
  generate_certkey_pem_file (cert_key_pem_file);

  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  g_object_set (G_OBJECT (fakesink_client), "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (fakesink_client), "handoff",
      G_CALLBACK (fakesink_client_hand_off), loop);
  g_object_set (G_OBJECT (fakesink_server), "signal-handoffs", TRUE, NULL);
  g_signal_connect (G_OBJECT (fakesink_server), "handoff",
      G_CALLBACK (fakesink_server_hand_off), loop);

  socket_client = open_socket (0);
  socket_server = open_socket (0);

  g_object_set (G_OBJECT (dtlssrtpenc_client), "channel-id", "client-id", NULL);
  g_object_set (G_OBJECT (dtlssrtpenc_client), "is-client", TRUE, NULL);
  g_object_set (G_OBJECT (dtlssrtpdec_client), "channel-id", "client-id", NULL);
  g_object_set (G_OBJECT (dtlssrtpdec_client), "is-client", TRUE, NULL);
  g_object_set (G_OBJECT (dtlssrtpdec_client), "certificate-pem-file",
      cert_key_pem_file, NULL);
  g_object_set (G_OBJECT (udpsink_client), "socket", socket_client, NULL);
  g_object_set (G_OBJECT (udpsink_client), "sync", FALSE, NULL);
  g_object_set (G_OBJECT (udpsink_client), "port",
      get_socket_port (socket_server), NULL);
  g_object_set (G_OBJECT (udpsrc_client), "socket", socket_client, NULL);

  g_object_set (G_OBJECT (dtlssrtpenc_server), "channel-id", "server-id", NULL);
  g_object_set (G_OBJECT (dtlssrtpdec_server), "channel-id", "server-id", NULL);
  g_object_set (G_OBJECT (dtlssrtpdec_server), "certificate-pem-file",
      cert_key_pem_file, NULL);
  g_object_set (G_OBJECT (udpsink_server), "socket", socket_server, NULL);
  g_object_set (G_OBJECT (udpsink_server), "sync", FALSE, NULL);
  g_object_set (G_OBJECT (udpsink_server), "port",
      get_socket_port (socket_client), NULL);
  g_object_set (G_OBJECT (udpsrc_server), "socket", socket_server, NULL);

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  mark_point ();
  gst_bin_add_many (GST_BIN (pipeline), videotestsrc_client, vp8enc_client,
      rtpvp8pay_client, dtlssrtpenc_client, dtlssrtpdec_client, udpsink_client,
      udpsrc_client, fakesink_client, NULL);
  gst_bin_add_many (GST_BIN (pipeline), videotestsrc_server, vp8enc_server,
      rtpvp8pay_server, dtlssrtpenc_server, dtlssrtpdec_server, udpsink_server,
      udpsrc_server, fakesink_server, NULL);
  mark_point ();

  gst_element_link_many (videotestsrc_client, vp8enc_client, rtpvp8pay_client,
      dtlssrtpenc_client, udpsink_client, NULL);
  gst_element_link_many (udpsrc_client, dtlssrtpdec_client, fakesink_client,
      NULL);
  mark_point ();

  gst_element_link_many (videotestsrc_server, vp8enc_server, rtpvp8pay_server,
      dtlssrtpenc_server, udpsink_server, NULL);
  gst_element_link_many (udpsrc_server, dtlssrtpdec_server, fakesink_server,
      NULL);
  mark_point ();

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "dtlssrtp_before_playing");

  gst_element_set_state (pipeline, GST_STATE_PLAYING);

  g_main_loop_run (loop);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, "dtlssrtp_after_playing");

  gst_element_set_state (pipeline, GST_STATE_NULL);
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);
  g_object_unref (pipeline);
  g_main_loop_unref (loop);

  finalize_socket (&socket_client);
  finalize_socket (&socket_server);

  g_remove (cert_key_pem_file);
  g_free (cert_key_pem_file);
}

GST_END_TEST
/*
 * End of test cases
 */
static Suite *
dtlssrtp_suite (void)
{
  Suite *s = suite_create ("dtlssrtp");
  TCase *tc_chain = tcase_create ("elements");

  suite_add_tcase (s, tc_chain);
  tcase_add_test (tc_chain, test_dtlssrtp);

  return s;
}

GST_CHECK_MAIN (dtlssrtp);
