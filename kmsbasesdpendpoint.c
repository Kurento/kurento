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
#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include <gst/gst.h>

#include "kmsbasesdpendpoint.h"
#include "kmsagnosticcaps.h"
#include "kms-marshal.h"
#include "sdp_utils.h"

#define PLUGIN_NAME "base_sdp_endpoint"

GST_DEBUG_CATEGORY_STATIC (kms_base_sdp_end_point_debug);
#define GST_CAT_DEFAULT kms_base_sdp_end_point_debug

#define kms_base_sdp_end_point_parent_class parent_class
G_DEFINE_TYPE (KmsBaseSdpEndPoint, kms_base_sdp_end_point, KMS_TYPE_ELEMENT);

#define USE_IPV6_DEFAULT FALSE

/* Signals and args */
enum
{
  SIGNAL_GENERATE_OFFER,
  SIGNAL_PROCESS_OFFER,
  SIGNAL_PROCESS_ANSWER,
  LAST_SIGNAL
};

enum
{
  PROP_0,
  PROP_USE_IPV6,
  PROP_PATTERN_SDP,
  PROP_LOCAL_OFFER_SDP,
  PROP_LOCAL_ANSWER_SDP,
  PROP_REMOTE_OFFER_SDP,
  PROP_REMOTE_ANSWER_SDP
};

static guint kms_base_sdp_end_point_signals[LAST_SIGNAL] = { 0 };

static void
kms_base_sdp_end_point_release_pattern_sdp (KmsBaseSdpEndPoint *
    base_sdp_end_point)
{
  if (base_sdp_end_point->pattern_sdp == NULL)
    return;

  gst_sdp_message_free (base_sdp_end_point->pattern_sdp);
  base_sdp_end_point->pattern_sdp = NULL;
}

static void
kms_base_sdp_end_point_release_local_offer_sdp (KmsBaseSdpEndPoint *
    base_sdp_end_point)
{
  if (base_sdp_end_point->local_offer_sdp == NULL)
    return;

  gst_sdp_message_free (base_sdp_end_point->local_offer_sdp);
  base_sdp_end_point->local_offer_sdp = NULL;
}

static void
kms_base_sdp_end_point_release_local_answer_sdp (KmsBaseSdpEndPoint *
    base_sdp_end_point)
{
  if (base_sdp_end_point->local_answer_sdp == NULL)
    return;

  gst_sdp_message_free (base_sdp_end_point->local_answer_sdp);
  base_sdp_end_point->local_answer_sdp = NULL;
}

static void
kms_base_sdp_end_point_release_remote_offer_sdp (KmsBaseSdpEndPoint *
    base_sdp_end_point)
{
  if (base_sdp_end_point->remote_offer_sdp == NULL)
    return;

  gst_sdp_message_free (base_sdp_end_point->remote_offer_sdp);
  base_sdp_end_point->remote_offer_sdp = NULL;
}

static void
kms_base_sdp_end_point_release_remote_answer_sdp (KmsBaseSdpEndPoint *
    base_sdp_end_point)
{
  if (base_sdp_end_point->remote_answer_sdp == NULL)
    return;

  gst_sdp_message_free (base_sdp_end_point->remote_answer_sdp);
  base_sdp_end_point->remote_answer_sdp = NULL;
}

static void
kms_base_sdp_end_point_set_local_offer_sdp (KmsBaseSdpEndPoint *
    base_sdp_end_point, GstSDPMessage * offer)
{
  KMS_ELEMENT_LOCK (base_sdp_end_point);
  kms_base_sdp_end_point_release_local_offer_sdp (base_sdp_end_point);
  gst_sdp_message_copy (offer, &base_sdp_end_point->local_offer_sdp);
  KMS_ELEMENT_UNLOCK (base_sdp_end_point);
  g_object_notify (G_OBJECT (base_sdp_end_point), "local-offer-sdp");
}

static void
kms_base_sdp_end_point_set_remote_offer_sdp (KmsBaseSdpEndPoint *
    base_sdp_end_point, GstSDPMessage * offer)
{
  KMS_ELEMENT_LOCK (base_sdp_end_point);
  kms_base_sdp_end_point_release_remote_offer_sdp (base_sdp_end_point);
  gst_sdp_message_copy (offer, &base_sdp_end_point->remote_offer_sdp);
  KMS_ELEMENT_UNLOCK (base_sdp_end_point);
  g_object_notify (G_OBJECT (base_sdp_end_point), "remote-offer-sdp");
}

static void
kms_base_sdp_end_point_set_local_answer_sdp (KmsBaseSdpEndPoint *
    base_sdp_end_point, GstSDPMessage * offer)
{
  KMS_ELEMENT_LOCK (base_sdp_end_point);
  kms_base_sdp_end_point_release_local_answer_sdp (base_sdp_end_point);
  gst_sdp_message_copy (offer, &base_sdp_end_point->local_answer_sdp);
  KMS_ELEMENT_UNLOCK (base_sdp_end_point);
  g_object_notify (G_OBJECT (base_sdp_end_point), "local-answer-sdp");
}

static void
kms_base_sdp_end_point_set_remote_answer_sdp (KmsBaseSdpEndPoint *
    base_sdp_end_point, GstSDPMessage * offer)
{
  KMS_ELEMENT_LOCK (base_sdp_end_point);
  kms_base_sdp_end_point_release_remote_answer_sdp (base_sdp_end_point);
  gst_sdp_message_copy (offer, &base_sdp_end_point->remote_answer_sdp);
  KMS_ELEMENT_UNLOCK (base_sdp_end_point);
  g_object_notify (G_OBJECT (base_sdp_end_point), "remote-answer-sdp");
}

static void
kms_base_sdp_end_point_start_transport_send (KmsBaseSdpEndPoint *
    base_sdp_end_point, const GstSDPMessage * offer,
    const GstSDPMessage * answer, gboolean local_offer)
{
  /* Defalut function, do nothing */
}

static void
kms_base_sdp_end_point_connect_input_elements (KmsBaseSdpEndPoint *
    base_sdp_end_point, const GstSDPMessage * answer)
{
  /* Defalut function, do nothing */
}

static void
kms_base_sdp_end_point_start_media (KmsBaseSdpEndPoint * base_sdp_end_point,
    const GstSDPMessage * offer, const GstSDPMessage * answer,
    gboolean local_offer)
{
  GST_DEBUG ("Start media");

  KmsBaseSdpEndPointClass *base_sdp_end_point_class =
      KMS_BASE_SDP_END_POINT_CLASS (G_OBJECT_GET_CLASS (base_sdp_end_point));

  if (base_sdp_end_point_class->start_transport_send ==
      kms_base_sdp_end_point_start_transport_send) {
    g_warning ("%s does not reimplement \"start_transport_send\"",
        G_OBJECT_CLASS_NAME (base_sdp_end_point_class));
  }

  base_sdp_end_point_class->start_transport_send (base_sdp_end_point, offer,
      answer, local_offer);

  if (base_sdp_end_point_class->connect_input_elements ==
      kms_base_sdp_end_point_connect_input_elements) {
    g_warning ("%s does not reimplement \"connect_input_elements\"",
        G_OBJECT_CLASS_NAME (base_sdp_end_point_class));
  }

  base_sdp_end_point_class->connect_input_elements (base_sdp_end_point, answer);
}

static gboolean
kms_base_sdp_end_point_set_transport_to_sdp (KmsBaseSdpEndPoint *
    base_sdp_end_point, GstSDPMessage * msg)
{
  /* Defalut function, do nothing */
  return TRUE;
}

static GstSDPMessage *
kms_base_sdp_end_point_generate_offer (KmsBaseSdpEndPoint * base_sdp_end_point)
{
  GstSDPMessage *offer = NULL;
  KmsBaseSdpEndPointClass *base_sdp_end_point_class =
      KMS_BASE_SDP_END_POINT_CLASS (G_OBJECT_GET_CLASS (base_sdp_end_point));

  GST_DEBUG ("generate_offer");

  KMS_ELEMENT_LOCK (base_sdp_end_point);
  if (base_sdp_end_point->pattern_sdp != NULL) {
    gst_sdp_message_copy (base_sdp_end_point->pattern_sdp, &offer);
  }
  KMS_ELEMENT_UNLOCK (base_sdp_end_point);

  if (offer == NULL)
    return NULL;

  if (base_sdp_end_point_class->set_transport_to_sdp ==
      kms_base_sdp_end_point_set_transport_to_sdp) {
    g_warning ("%s does not reimplement \"set_transport_to_sdp\"",
        G_OBJECT_CLASS_NAME (base_sdp_end_point_class));
  }

  if (!base_sdp_end_point_class->set_transport_to_sdp (base_sdp_end_point,
          offer)) {
    gst_sdp_message_free (offer);
    return NULL;
  }

  kms_base_sdp_end_point_set_local_offer_sdp (base_sdp_end_point, offer);

  return offer;
}

static GstSDPMessage *
kms_base_sdp_end_point_process_offer (KmsBaseSdpEndPoint * base_sdp_end_point,
    GstSDPMessage * offer)
{
  GstSDPMessage *answer = NULL, *intersec_offer, *intersect_answer;
  KmsBaseSdpEndPointClass *base_sdp_end_point_class =
      KMS_BASE_SDP_END_POINT_CLASS (G_OBJECT_GET_CLASS (base_sdp_end_point));

  kms_base_sdp_end_point_set_remote_offer_sdp (base_sdp_end_point, offer);
  GST_DEBUG ("process_offer");

  KMS_ELEMENT_LOCK (base_sdp_end_point);
  if (base_sdp_end_point->pattern_sdp != NULL) {
    gst_sdp_message_copy (base_sdp_end_point->pattern_sdp, &answer);
  }
  KMS_ELEMENT_UNLOCK (base_sdp_end_point);

  if (answer == NULL)
    return NULL;

  if (base_sdp_end_point_class->set_transport_to_sdp ==
      kms_base_sdp_end_point_set_transport_to_sdp) {
    g_warning ("%s does not reimplement \"set_transport_to_sdp\"",
        G_OBJECT_CLASS_NAME (base_sdp_end_point_class));
  }

  if (!base_sdp_end_point_class->set_transport_to_sdp (base_sdp_end_point,
          answer)) {
    gst_sdp_message_free (answer);
    return NULL;
  }

  if (sdp_utils_intersect_sdp_messages (offer, answer, &intersec_offer,
          &intersect_answer) != GST_SDP_OK) {
    gst_sdp_message_free (answer);
    return NULL;
  }
  gst_sdp_message_free (intersec_offer);
  gst_sdp_message_free (answer);

  kms_base_sdp_end_point_set_local_answer_sdp (base_sdp_end_point,
      intersect_answer);

  kms_base_sdp_end_point_start_media (base_sdp_end_point, offer,
      intersect_answer, FALSE);

  return intersect_answer;
}

static void
kms_base_sdp_end_point_process_answer (KmsBaseSdpEndPoint * base_sdp_end_point,
    GstSDPMessage * answer)
{
  GST_DEBUG ("process_answer");

  if (base_sdp_end_point->local_offer_sdp == NULL) {
    // TODO: This should raise an error
    GST_ERROR_OBJECT (base_sdp_end_point,
        "Answer received without a local offer generated");
    return;
  }

  kms_base_sdp_end_point_set_remote_answer_sdp (base_sdp_end_point, answer);

  kms_base_sdp_end_point_start_media (base_sdp_end_point,
      base_sdp_end_point->local_offer_sdp, answer, TRUE);
}

static void
kms_base_sdp_end_point_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsBaseSdpEndPoint *base_sdp_end_point = KMS_BASE_SDP_END_POINT (object);

  switch (prop_id) {
    case PROP_PATTERN_SDP:
      KMS_ELEMENT_LOCK (base_sdp_end_point);
      kms_base_sdp_end_point_release_pattern_sdp (base_sdp_end_point);
      base_sdp_end_point->pattern_sdp = g_value_dup_boxed (value);
      KMS_ELEMENT_UNLOCK (base_sdp_end_point);
      break;
    case PROP_USE_IPV6:
      KMS_ELEMENT_LOCK (base_sdp_end_point);
      base_sdp_end_point->use_ipv6 = g_value_get_boolean (value);
      KMS_ELEMENT_UNLOCK (base_sdp_end_point);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_base_sdp_end_point_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  KmsBaseSdpEndPoint *base_sdp_end_point = KMS_BASE_SDP_END_POINT (object);

  switch (prop_id) {
    case PROP_USE_IPV6:
      g_value_set_boolean (value, base_sdp_end_point->use_ipv6);
      break;
    case PROP_PATTERN_SDP:
      g_value_set_boxed (value, base_sdp_end_point->pattern_sdp);
      break;
    case PROP_LOCAL_OFFER_SDP:
      g_value_set_boxed (value, base_sdp_end_point->local_offer_sdp);
      break;
    case PROP_LOCAL_ANSWER_SDP:
      g_value_set_boxed (value, base_sdp_end_point->local_answer_sdp);
      break;
    case PROP_REMOTE_OFFER_SDP:
      g_value_set_boxed (value, base_sdp_end_point->remote_offer_sdp);
      break;
    case PROP_REMOTE_ANSWER_SDP:
      g_value_set_boxed (value, base_sdp_end_point->remote_answer_sdp);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
kms_base_sdp_end_point_finalize (GObject * object)
{
  KmsBaseSdpEndPoint *base_sdp_end_point = KMS_BASE_SDP_END_POINT (object);

  kms_base_sdp_end_point_release_pattern_sdp (base_sdp_end_point);
  kms_base_sdp_end_point_release_local_offer_sdp (base_sdp_end_point);
  kms_base_sdp_end_point_release_local_answer_sdp (base_sdp_end_point);
  kms_base_sdp_end_point_release_remote_offer_sdp (base_sdp_end_point);
  kms_base_sdp_end_point_release_remote_answer_sdp (base_sdp_end_point);

  /* chain up */
  G_OBJECT_CLASS (kms_base_sdp_end_point_parent_class)->finalize (object);
}

static void
kms_base_sdp_end_point_class_init (KmsBaseSdpEndPointClass * klass)
{
  GstElementClass *gstelement_class;
  GObjectClass *gobject_class;

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  gobject_class = G_OBJECT_CLASS (klass);

  gobject_class->get_property = kms_base_sdp_end_point_get_property;
  gobject_class->set_property = kms_base_sdp_end_point_set_property;
  gobject_class->finalize = kms_base_sdp_end_point_finalize;

  gstelement_class = GST_ELEMENT_CLASS (klass);
  gst_element_class_set_details_simple (gstelement_class,
      "BaseSdpEndPoint",
      "Base/Bin/BaseSdpEndPoint",
      "Base class for sdpEndPoints",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>");

  klass->set_transport_to_sdp = kms_base_sdp_end_point_set_transport_to_sdp;
  klass->start_transport_send = kms_base_sdp_end_point_start_transport_send;
  klass->connect_input_elements = kms_base_sdp_end_point_connect_input_elements;

  klass->generate_offer = kms_base_sdp_end_point_generate_offer;
  klass->process_offer = kms_base_sdp_end_point_process_offer;
  klass->process_answer = kms_base_sdp_end_point_process_answer;

  /* Signals initialization */
  kms_base_sdp_end_point_signals[SIGNAL_GENERATE_OFFER] =
      g_signal_new ("generate-offer",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsBaseSdpEndPointClass, generate_offer), NULL, NULL,
      __kms_marshal_BOXED__VOID, GST_TYPE_SDP_MESSAGE, 0);

  kms_base_sdp_end_point_signals[SIGNAL_PROCESS_OFFER] =
      g_signal_new ("process-offer",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsBaseSdpEndPointClass, process_offer), NULL, NULL,
      __kms_marshal_BOXED__BOXED, GST_TYPE_SDP_MESSAGE, 1,
      GST_TYPE_SDP_MESSAGE);

  kms_base_sdp_end_point_signals[SIGNAL_PROCESS_ANSWER] =
      g_signal_new ("process-answer",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsBaseSdpEndPointClass, process_answer), NULL, NULL,
      __kms_marshal_VOID__BOXED, G_TYPE_NONE, 1, GST_TYPE_SDP_MESSAGE);

  /* Properties initialization */
  g_object_class_install_property (gobject_class, PROP_USE_IPV6,
      g_param_spec_boolean ("use-ipv6", "Use ipv6 in SDPs",
          "Use ipv6 addresses in generated sdp offers and answers",
          USE_IPV6_DEFAULT, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_PATTERN_SDP,
      g_param_spec_boxed ("pattern-sdp", "Pattern to create local sdps",
          "Pattern to create \"local-offer-sdp\" and \"local-answer-sdp\"",
          GST_TYPE_SDP_MESSAGE,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS |
          GST_PARAM_MUTABLE_READY));

  g_object_class_install_property (gobject_class, PROP_LOCAL_OFFER_SDP,
      g_param_spec_boxed ("local-offer-sdp", "Local offer sdp",
          "The local generated offer, negotiated with \"remote-answer-sdp\"",
          GST_TYPE_SDP_MESSAGE, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_REMOTE_OFFER_SDP,
      g_param_spec_boxed ("remote-offer-sdp", "Remote offer sdp",
          "The remote offer, negotiated with \"local-answer-sdp\"",
          GST_TYPE_SDP_MESSAGE, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_LOCAL_ANSWER_SDP,
      g_param_spec_boxed ("local-answer-sdp", "Local answer sdp",
          "The local negotiated answer, negotiated with \"remote-offer-sdp\"",
          GST_TYPE_SDP_MESSAGE, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_REMOTE_ANSWER_SDP,
      g_param_spec_boxed ("remote-answer-sdp", "Remote answer sdp",
          "The remote answer, negotiated with \"local-offer-sdp\"",
          GST_TYPE_SDP_MESSAGE, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));
}

static void
kms_base_sdp_end_point_init (KmsBaseSdpEndPoint * base_sdp_end_point)
{
  base_sdp_end_point->use_ipv6 = USE_IPV6_DEFAULT;
  base_sdp_end_point->pattern_sdp = NULL;
  base_sdp_end_point->local_offer_sdp = NULL;
  base_sdp_end_point->local_answer_sdp = NULL;
  base_sdp_end_point->remote_offer_sdp = NULL;
  base_sdp_end_point->remote_answer_sdp = NULL;
}
