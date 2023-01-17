/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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
#include "webrtcendpoint/kmsicecandidate.h"

static void
check_candidate (KmsIceCandidate * c, const gchar * mid, const gchar * addr,
    gint port, guint ipv, const gchar * stream_id, const gchar * foundation,
    guint priority, KmsIceProtocol proto, KmsIceCandidateType type,
    KmsIceTcpCandidateType tcptype, const gchar * raddr, guint rport)
{
  gchar *sdp_line, *caddr, *cfoundation, *craddr;

  GST_DEBUG_OBJECT (c, "candidate: %s", kms_ice_candidate_get_candidate (c));

  GST_DEBUG_OBJECT (c, "mid: %s", kms_ice_candidate_get_sdp_mid (c));
  fail_if (g_strcmp0 (mid, kms_ice_candidate_get_sdp_mid (c)) != 0);

  caddr = kms_ice_candidate_get_address (c);
  GST_DEBUG_OBJECT (c, "address: %s", caddr);
  fail_if (g_strcmp0 (addr, caddr) != 0);
  g_free (caddr);

  GST_DEBUG_OBJECT (c, "port: %u", kms_ice_candidate_get_port (c));
  fail_if (port != kms_ice_candidate_get_port (c));

  GST_DEBUG_OBJECT (c, "IPv: %d", kms_ice_candidate_get_ip_version (c));
  fail_if (ipv != kms_ice_candidate_get_ip_version (c));

  GST_DEBUG_OBJECT (c, "StreamId: %s", kms_ice_candidate_get_stream_id (c));
  fail_if (g_strcmp0 (stream_id, kms_ice_candidate_get_stream_id (c)) != 0);

  cfoundation = kms_ice_candidate_get_foundation (c);
  GST_DEBUG_OBJECT (c, "foundation: %s", cfoundation);
  fail_if (g_strcmp0 (foundation, cfoundation) != 0);
  g_free (cfoundation);

  GST_DEBUG_OBJECT (c, "priority: %u", kms_ice_candidate_get_priority (c));
  fail_if (priority != kms_ice_candidate_get_priority (c));

  GST_DEBUG_OBJECT (c, "protocol: %u", kms_ice_candidate_get_protocol (c));
  fail_if (proto != kms_ice_candidate_get_protocol (c));

  GST_DEBUG_OBJECT (c, "candidate type: %u",
      kms_ice_candidate_get_candidate_type (c));
  fail_if (type != kms_ice_candidate_get_candidate_type (c));

  GST_DEBUG_OBJECT (c, "candidate tcp type: %u",
      kms_ice_candidate_get_candidate_tcp_type (c));
  fail_if (tcptype != kms_ice_candidate_get_candidate_tcp_type (c));

  craddr = kms_ice_candidate_get_related_address (c);
  GST_DEBUG_OBJECT (c, "raddr: %s", craddr);
  fail_if (g_strcmp0 (raddr, craddr) != 0);
  g_free (craddr);

  GST_DEBUG_OBJECT (c, "rport: %d", kms_ice_candidate_get_related_port (c));
  fail_if (rport != kms_ice_candidate_get_related_port (c));

  sdp_line = kms_ice_candidate_get_sdp_line (c);
  GST_DEBUG_OBJECT (c, "SDP Line: %s", sdp_line);

  g_free (sdp_line);
}

GST_START_TEST (test_expr)
{
  gchar *c1 =
      "candidate:1 1 TCP 1015022079 192.168.1.183 38907 typ host tcptype passive";
  gchar *c2 =
      "candidate:2 1 UDP 2013266431 fe80::a00:27ff:fee0:4ebf 45067 typ relay";
  gchar *c3 = "candidate:3 1 UDP 2013266431 192.168.1.183 55079 typ prflx";
  gchar *c4 =
      "candidate:4 1 UDP 2013266431 192.168.1.183 55079 typ relay raddr 127.0.0.1 rport 9999 tcptype active";
  gchar *c5 =
      "candidate:5 1 UDP 2013266431 192.168.1.183 55079 typ relay raddr 127.0.0.1 tcptype active";
  gchar *c6 =
      "candidate:6 1 UDP 2013266431 192.168.1.183 55079 typ relay rport 9999 tcptype active";
  gchar *c7 =
      "candidate:842163049 1 udp 1677729535 193.147.51.8 59803 typ srflx raddr 172.17.0.9 rport 59803 generation 0 ufrag B+z2Krpxf2R3uR0S";
  gchar *c8 =
      "candidate:qwert+/456 1 TCP 935331583 fe80::a00:27ff:fee0:4ebf 38878 typ prflx tcptype active";

  KmsIceCandidate *cand1, *cand2, *cand3, *cand4, *cand5, *cand6, *cand7,
      *cand8;

  cand1 = kms_ice_candidate_new (c1, "test", 1, "8");
  check_candidate (cand1, "test", "192.168.1.183", 38907, 4, "8", "1",
      1015022079, KMS_ICE_PROTOCOL_TCP, KMS_ICE_CANDIDATE_TYPE_HOST,
      KMS_ICE_TCP_CANDIDATE_TYPE_PASSIVE, NULL, -1);

  cand2 = kms_ice_candidate_new (c2, "test", 1, "8");
  check_candidate (cand2, "test", "fe80::a00:27ff:fee0:4ebf", 45067, 6, "8",
      "2", 2013266431, KMS_ICE_PROTOCOL_UDP, KMS_ICE_CANDIDATE_TYPE_RELAY,
      KMS_ICE_TCP_CANDIDATE_TYPE_NONE, NULL, -1);

  cand3 = kms_ice_candidate_new (c3, "test", 1, "8");
  check_candidate (cand3, "test", "192.168.1.183", 55079, 4, "8", "3",
      2013266431, KMS_ICE_PROTOCOL_UDP, KMS_ICE_CANDIDATE_TYPE_PRFLX,
      KMS_ICE_TCP_CANDIDATE_TYPE_NONE, NULL, -1);

  cand4 = kms_ice_candidate_new (c4, "test", 1, "8");
  check_candidate (cand4, "test", "192.168.1.183", 55079, 4, "8", "4",
      2013266431, KMS_ICE_PROTOCOL_UDP, KMS_ICE_CANDIDATE_TYPE_RELAY,
      KMS_ICE_TCP_CANDIDATE_TYPE_ACTIVE, "127.0.0.1", 9999);

  cand5 = kms_ice_candidate_new (c5, "test", 1, "8");
  check_candidate (cand5, "test", "192.168.1.183", 55079, 4, "8", "5",
      2013266431, KMS_ICE_PROTOCOL_UDP, KMS_ICE_CANDIDATE_TYPE_RELAY,
      KMS_ICE_TCP_CANDIDATE_TYPE_ACTIVE, "127.0.0.1", -1);

  cand6 = kms_ice_candidate_new (c6, "test", 1, "8");
  check_candidate (cand6, "test", "192.168.1.183", 55079, 4, "8", "6",
      2013266431, KMS_ICE_PROTOCOL_UDP, KMS_ICE_CANDIDATE_TYPE_RELAY,
      KMS_ICE_TCP_CANDIDATE_TYPE_ACTIVE, NULL, 9999);

  cand7 = kms_ice_candidate_new (c7, "test", 1, "8");
  check_candidate (cand7, "test", "193.147.51.8", 59803, 4, "8", "842163049",
      1677729535, KMS_ICE_PROTOCOL_UDP, KMS_ICE_CANDIDATE_TYPE_SRFLX,
      KMS_ICE_TCP_CANDIDATE_TYPE_NONE, "172.17.0.9", 59803);

  cand8 = kms_ice_candidate_new (c8, "test", 1, "8");
  check_candidate (cand8, "test", "fe80::a00:27ff:fee0:4ebf", 38878, 6, "8",
      "qwert+/456", 935331583, KMS_ICE_PROTOCOL_TCP,
      KMS_ICE_CANDIDATE_TYPE_PRFLX, KMS_ICE_TCP_CANDIDATE_TYPE_ACTIVE, NULL,
      -1);

  g_object_unref (cand1);
  g_object_unref (cand2);
  g_object_unref (cand3);
  g_object_unref (cand4);
  g_object_unref (cand5);
  g_object_unref (cand6);
  g_object_unref (cand7);
  g_object_unref (cand8);
}

GST_END_TEST;

static Suite *
ice_candidates_suite (void)
{
  Suite *s = suite_create ("ice_candidates");
  TCase *tc_chain = tcase_create ("general");

  suite_add_tcase (s, tc_chain);
  tcase_add_test (tc_chain, test_expr);

  return s;
}

GST_CHECK_MAIN (ice_candidates);
