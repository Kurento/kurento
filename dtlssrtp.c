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
#include <gst/gst.h>
#include <glib.h>
#include <gio/gio.h>

#define CERT_KEY_PEM_FILE "/tmp/certkey.pem"

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

static void
generate_certkey_pem_file ()
{
  gchar *cmd;
  int ret;

  cmd =
      g_strconcat ("/bin/sh -c \"certtool --generate-privkey --outfile ",
      CERT_KEY_PEM_FILE, "\"", NULL);
  ret = system (cmd);
  g_free (cmd);
  fail_unless (ret != -1);

  cmd =
      g_strconcat
      ("/bin/sh -c \"certtool --generate-self-signed --load-privkey ",
      CERT_KEY_PEM_FILE, " --template ", CERTTOOL_TEMPLATE,
      " >> /tmp/certkey.pem\"", NULL);
  ret = system (cmd);
  g_free (cmd);
  fail_unless (ret != -1);
}

static gboolean
quit_main_loop (gpointer loop)
{
  g_main_loop_quit (loop);

  return FALSE;
}

GST_START_TEST (test_dtlssrtp)
{
  GMainLoop *loop = g_main_loop_new (NULL, TRUE);
  GstElement *pipeline = gst_pipeline_new (__FUNCTION__);
  GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));

  GstElement *videotestsrc_client =
      gst_element_factory_make ("videotestsrc", NULL);
  GstElement *vp8enc_client = gst_element_factory_make ("vp8enc", NULL);
  GstElement *rtpvp8pay_client = gst_element_factory_make ("rtpvp8pay", NULL);
  GstElement *dtlssrtpenc_client =
      gst_element_factory_make ("dtlssrtpenc", NULL);
  GstElement *dtlssrtpdec_client =
      gst_element_factory_make ("dtlssrtpdec", NULL);
  GSocket *socket_client;
  GstElement *udpsink_client = gst_element_factory_make ("udpsink", NULL);
  GstElement *udpsrc_client = gst_element_factory_make ("udpsrc", NULL);
  GstElement *fakesink_client = gst_element_factory_make ("fakesink", NULL);

  GstElement *videotestsrc_server =
      gst_element_factory_make ("videotestsrc", NULL);
  GstElement *vp8enc_server = gst_element_factory_make ("vp8enc", NULL);
  GstElement *rtpvp8pay_server = gst_element_factory_make ("rtpvp8pay", NULL);
  GstElement *dtlssrtpenc_server =
      gst_element_factory_make ("dtlssrtpenc", NULL);
  GstElement *dtlssrtpdec_server =
      gst_element_factory_make ("dtlssrtpdec", NULL);
  GSocket *socket_server;
  GstElement *udpsink_server = gst_element_factory_make ("udpsink", NULL);
  GstElement *udpsrc_server = gst_element_factory_make ("udpsrc", NULL);
  GstElement *fakesink_server = gst_element_factory_make ("fakesink", NULL);

  generate_certkey_pem_file ();

  g_object_set (G_OBJECT (pipeline), "async-handling", TRUE, NULL);
  gst_bus_add_signal_watch (bus);
  g_signal_connect (bus, "message", G_CALLBACK (bus_msg), pipeline);

  socket_client = open_socket (0);
  socket_server = open_socket (0);

  g_object_set (G_OBJECT (dtlssrtpenc_client), "channel-id", "client-id", NULL);
  g_object_set (G_OBJECT (dtlssrtpenc_client), "is-client", TRUE, NULL);
  g_object_set (G_OBJECT (dtlssrtpdec_client), "channel-id", "client-id", NULL);
  g_object_set (G_OBJECT (dtlssrtpdec_client), "is-client", TRUE, NULL);
  g_object_set (G_OBJECT (dtlssrtpdec_client), "certificate-pem-file",
      CERT_KEY_PEM_FILE, NULL);
  g_object_set (G_OBJECT (udpsink_client), "socket", socket_client, NULL);
  g_object_set (G_OBJECT (udpsink_client), "sync", FALSE, NULL);
  g_object_set (G_OBJECT (udpsink_client), "port",
      get_socket_port (socket_server), NULL);
  g_object_set (G_OBJECT (udpsrc_client), "socket", socket_client, NULL);

  g_object_set (G_OBJECT (dtlssrtpenc_server), "channel-id", "server-id", NULL);
  g_object_set (G_OBJECT (dtlssrtpdec_server), "channel-id", "server-id", NULL);
  g_object_set (G_OBJECT (dtlssrtpdec_server), "certificate-pem-file",
      CERT_KEY_PEM_FILE, NULL);
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

  g_timeout_add (2000, quit_main_loop, loop);
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
