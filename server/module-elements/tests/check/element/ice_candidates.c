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
#include <nice/agent.h>
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

/*
 * Candidates that cannot be parsed, or that carry out-of-range values, must be
 * rejected (NULL), and must never crash the process.
 * One of them is the one sent by an ESP32, which uses a negative priority:
 * https://github.com/Kurento/kurento/issues/143
 */
GST_START_TEST (test_invalid_candidates)
{
  static const gchar *invalid[] = {
    /* Negative priority that a signed 32-bit integer cannot hold */
    "candidate:remote1 2 UDP -2147483649 192.168.66.55 54631 typ prflx",
    "candidate:remote1 2 UDP -99999999999 192.168.66.55 54631 typ prflx",
    "candidate:remote1 2 UDP -0 192.168.66.55 54631 typ prflx",
    "candidate:remote1 2 UDP - 192.168.66.55 54631 typ prflx",
    /* Priority does not fit in 32 bits */
    "candidate:1 1 UDP 4294967296 192.168.1.183 55079 typ host",
    "candidate:1 1 UDP 9999999999 192.168.1.183 55079 typ host",
    /* Port out of range */
    "candidate:1 1 UDP 2013266431 192.168.1.183 65536 typ host",
    "candidate:1 1 UDP 2013266431 192.168.1.183 99999999999999999999 typ host",
    "candidate:1 1 UDP 2013266431 192.168.1.183 -5 typ host",
    /* Related port out of range */
    "candidate:1 1 UDP 2013266431 192.168.1.183 55079 typ srflx raddr 10.0.0.1 rport 65536",
    /* Unsupported component */
    "candidate:1 3 UDP 2013266431 192.168.1.183 55079 typ host",
    "candidate:1 0 UDP 2013266431 192.168.1.183 55079 typ host",
    /* Unknown type, unknown transport */
    "candidate:1 1 UDP 2013266431 192.168.1.183 55079 typ bogus",
    "candidate:1 1 SCTP 2013266431 192.168.1.183 55079 typ host",
    /* Garbage */
    "",
    "candidate:",
    "a=candidate:1 1 UDP 2013266431 192.168.1.183 55079 typ host",
    "not a candidate at all",
    NULL
  };
  guint i;

  for (i = 0; invalid[i] != NULL; i++) {
    KmsIceCandidate *cand;

    GST_DEBUG ("Invalid candidate: '%s'", invalid[i]);
    cand = kms_ice_candidate_new (invalid[i], "test", 1, "8");
    fail_unless (cand == NULL, "Candidate should be rejected: '%s'",
        invalid[i]);
  }

  /* A NULL candidate string must not crash either */
  fail_unless (kms_ice_candidate_new (NULL, "test", 1, "8") == NULL);
}

GST_END_TEST;

/*
 * libnice formats its guint32 priority with "%d", so priorities >= 2^31 come
 * out as negative numbers. They must be accepted with the real value.
 */
GST_START_TEST (test_negative_priority)
{
  KmsIceCandidate *cand;
  gchar *sdp_line;

  /* Candidate from issue #143: 2^32 - 43452938 = 4251514358 */
  cand = kms_ice_candidate_new
      ("candidate:remote1 2 UDP -43452938 192.168.66.55 54631 typ prflx",
      "test", 1, "8");
  fail_if (cand == NULL);
  check_candidate (cand, "test", "192.168.66.55", 54631, 4, "8", "remote1",
      4251514358u, KMS_ICE_PROTOCOL_UDP, KMS_ICE_CANDIDATE_TYPE_PRFLX,
      KMS_ICE_TCP_CANDIDATE_TYPE_NONE, NULL, -1);
  /* The string is normalized, so that it is valid for anyone downstream */
  sdp_line = kms_ice_candidate_get_sdp_line (cand);
  fail_unless_equals_string (sdp_line,
      "a=candidate:remote1 2 UDP 4251514358 192.168.66.55 54631 typ prflx");
  g_free (sdp_line);
  g_object_unref (cand);

  /* Limits: -1 -> 2^32-1, -2^31 -> 2^31 */
  cand = kms_ice_candidate_new
      ("candidate:1 1 UDP -1 192.168.1.1 5000 typ host raddr 10.0.0.1 rport 1",
      "test", 1, "8");
  fail_if (cand == NULL);
  fail_unless_equals_uint64 (kms_ice_candidate_get_priority (cand),
      4294967295u);
  g_object_unref (cand);

  cand = kms_ice_candidate_new
      ("candidate:1 1 UDP -2147483648 192.168.1.1 5000 typ host", "test", 1,
      "8");
  fail_if (cand == NULL);
  fail_unless_equals_uint64 (kms_ice_candidate_get_priority (cand),
      2147483648u);
  g_object_unref (cand);
}

GST_END_TEST;

/*
 * Reproduces the flow of a peer-reflexive remote candidate: libnice reports a
 * NiceCandidate with a priority >= 2^31, KMS turns it into an SDP string with
 * nice_agent_generate_local_candidate_sdp() and parses it back.
 */
GST_START_TEST (test_libnice_candidate_roundtrip)
{
  NiceAgent *agent;
  NiceCandidate *ncand;
  KmsIceCandidate *cand;
  gchar *str, *kms_str;

  agent = nice_agent_new (g_main_context_default (), NICE_COMPATIBILITY_RFC5245);
  ncand = nice_candidate_new (NICE_CANDIDATE_TYPE_PEER_REFLEXIVE);
  ncand->component_id = 2;
  ncand->transport = NICE_CANDIDATE_TRANSPORT_UDP;
  ncand->priority = 4251514358u;
  g_strlcpy (ncand->foundation, "remote1", sizeof (ncand->foundation));
  fail_unless (nice_address_set_from_string (&ncand->addr, "192.168.66.55"));
  nice_address_set_port (&ncand->addr, 54631);
  ncand->base_addr = ncand->addr;

  str = nice_agent_generate_local_candidate_sdp (agent, ncand);
  GST_DEBUG ("libnice SDP: %s", str);
  /* Same conversion as kms_ice_nice_agent_get_candidate_sdp_string() */
  kms_str = g_strconcat (SDP_CANDIDATE_ATTR, ":", str + SDP_CANDIDATE_ATTR_LEN,
      NULL);

  cand = kms_ice_candidate_new (kms_str, "", 0, "8");
  fail_if (cand == NULL, "Candidate from libnice rejected: '%s'", kms_str);
  fail_unless_equals_uint64 (kms_ice_candidate_get_priority (cand),
      ncand->priority);
  fail_unless (kms_ice_candidate_get_candidate_type (cand) ==
      KMS_ICE_CANDIDATE_TYPE_PRFLX);

  g_object_unref (cand);
  g_free (kms_str);
  g_free (str);
  nice_candidate_free (ncand);
  g_object_unref (agent);
}

GST_END_TEST;

/*
 * Setting an invalid value in the "candidate" property of an already-parsed
 * object must not leave dangling pointers (double free on finalize).
 */
GST_START_TEST (test_reset_candidate_property)
{
  KmsIceCandidate *cand;
  gchar *foundation;

  cand = g_object_new (KMS_TYPE_ICE_CANDIDATE, "candidate",
      "candidate:4 1 UDP 2013266431 192.168.1.183 55079 typ relay raddr 127.0.0.1 rport 9999",
      "sdp-mid", "test", NULL);

  g_object_set (cand, "candidate",
      "candidate:5 3 UDP 2013266431 192.168.1.184 55080 typ host", NULL);
  g_object_set (cand, "candidate",
      "candidate:6 1 UDP -2147483649 192.168.1.185 55081 typ prflx", NULL);

  /* Reading from it must be safe */
  foundation = kms_ice_candidate_get_foundation (cand);
  g_free (foundation);

  g_object_unref (cand);
}

GST_END_TEST;

static Suite *
ice_candidates_suite (void)
{
  Suite *s = suite_create ("ice_candidates");
  TCase *tc_chain = tcase_create ("general");

  suite_add_tcase (s, tc_chain);
  tcase_add_test (tc_chain, test_expr);
  tcase_add_test (tc_chain, test_negative_priority);
  tcase_add_test (tc_chain, test_libnice_candidate_roundtrip);
  tcase_add_test (tc_chain, test_invalid_candidates);
  tcase_add_test (tc_chain, test_reset_candidate_property);

  return s;
}

GST_CHECK_MAIN (ice_candidates);
