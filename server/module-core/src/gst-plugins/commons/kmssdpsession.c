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

#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include "kmssdpsession.h"
#include "kmsutils.h"

#define GST_DEFAULT_NAME "kmssdpsession"
#define GST_CAT_DEFAULT kms_sdp_session_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_sdp_session_parent_class parent_class
G_DEFINE_TYPE (KmsSdpSession, kms_sdp_session, GST_TYPE_BIN);

KmsSdpSession *
kms_sdp_session_new (KmsBaseSdpEndpoint * ep, guint id)
{
  GObject *obj;
  KmsSdpSession *self;

  obj = g_object_new (KMS_TYPE_SDP_SESSION, NULL);
  self = KMS_SDP_SESSION (obj);
  KMS_SDP_SESSION_CLASS (G_OBJECT_GET_CLASS (self))->post_constructor (self, ep,
      id);

  return self;
}

GstSDPMessage *
kms_sdp_session_generate_offer (KmsSdpSession * self)
{
  GstSDPMessage *offer = NULL;
  GError *err = NULL;
  gchar *sdp_str = NULL;

  offer = kms_sdp_agent_create_offer (self->agent, &err);
  if (err != NULL) {
    GST_ERROR_OBJECT (self, "Generating SDP Offer: %s", err->message);
    goto error;
  }

  kms_sdp_agent_set_local_description (self->agent, offer, &err);
  if (err != NULL) {
    GST_ERROR_OBJECT (self, "Generating SDP Offer: %s", err->message);
    goto error;
  }

  if (gst_sdp_message_copy (offer, &self->local_sdp) != GST_SDP_OK) {
    GST_ERROR_OBJECT (self, "Generating SDP Offer: gst_sdp_message_copy");
    goto error;
  }

  GST_DEBUG_OBJECT (self, "Generated SDP Offer:\n%s",
      (sdp_str = gst_sdp_message_as_text (offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  return offer;

error:
  g_clear_error (&err);

  if (offer != NULL) {
    gst_sdp_message_free (offer);
  }

  return NULL;
}

GstSDPMessage *
kms_sdp_session_process_offer (KmsSdpSession * self, GstSDPMessage * offer)
{
  GstSDPMessage *answer = NULL, *copy;
  GError *err = NULL;
  gchar *sdp_str = NULL;

  GST_DEBUG_OBJECT (self, "Process SDP Offer:\n%s",
      (sdp_str = gst_sdp_message_as_text (offer)));
  g_free (sdp_str);
  sdp_str = NULL;

  if (self->remote_sdp != NULL) {
    gst_sdp_message_free (self->remote_sdp);
  }

  gst_sdp_message_copy (offer, &self->remote_sdp);

  if (gst_sdp_message_copy (offer, &copy) != GST_SDP_OK) {
    GST_ERROR_OBJECT (self, "Processing SDP Offer: Cannot copy SDP message");
    goto error;
  }

  kms_sdp_agent_set_remote_description (self->agent, copy, &err);
  if (err != NULL) {
    GST_ERROR_OBJECT (self, "Processing SDP Offer: %s", err->message);
    goto error;
  }

  answer = kms_sdp_agent_create_answer (self->agent, &err);
  if (err != NULL) {
    GST_ERROR_OBJECT (self, "Processing SDP Offer: %s", err->message);
    goto error;
  }

  /* REVIEW: a copy of answer? */
  kms_sdp_agent_set_local_description (self->agent, answer, &err);
  if (err != NULL) {
    GST_ERROR_OBJECT (self, "Processing SDP Offer: %s", err->message);
    goto error;
  }

  if (self->local_sdp != NULL) {
    gst_sdp_message_free (self->local_sdp);
  }
  gst_sdp_message_copy (answer, &self->local_sdp);

  if (self->neg_sdp != NULL) {
    gst_sdp_message_free (self->neg_sdp);
  }
  gst_sdp_message_copy (self->local_sdp, &self->neg_sdp);

  GST_DEBUG_OBJECT (self, "Generated SDP Answer:\n%s",
      (sdp_str = gst_sdp_message_as_text (answer)));
  g_free (sdp_str);
  sdp_str = NULL;

  return answer;

error:
  g_clear_error (&err);

  if (answer != NULL) {
    gst_sdp_message_free (answer);
  }

  return NULL;
}

gboolean
kms_sdp_session_process_answer (KmsSdpSession * self, GstSDPMessage * answer)
{
  GstSDPMessage *copy;
  GError *err = NULL;
  gchar *sdp_str = NULL;

  GST_DEBUG_OBJECT (self, "Process SDP Answer:\n%s",
      (sdp_str = gst_sdp_message_as_text (answer)));
  g_free (sdp_str);
  sdp_str = NULL;

  if (self->local_sdp == NULL) {
    GST_ERROR_OBJECT (self, "SDP Answer received without a previous Offer");
    return FALSE;
  }

  if (gst_sdp_message_copy (answer, &copy) != GST_SDP_OK) {
    GST_ERROR_OBJECT (self, "Processing SDP Answer: Cannot copy SDP message");
    return FALSE;
  }

  kms_sdp_agent_set_remote_description (self->agent, copy, &err);
  if (err != NULL) {
    GST_ERROR_OBJECT (self, "Processing SDP Answer: %s", err->message);
    return FALSE;
  }

  if (self->remote_sdp != NULL) {
    gst_sdp_message_free (self->remote_sdp);
  }
  gst_sdp_message_copy (answer, &self->remote_sdp);

  if (self->neg_sdp != NULL) {
    gst_sdp_message_free (self->neg_sdp);
  }
  gst_sdp_message_copy (self->remote_sdp, &self->neg_sdp);

  return TRUE;
}

GstSDPMessage *
kms_sdp_session_get_local_sdp (KmsSdpSession * self)
{
  GstSDPMessage *sdp = NULL;

  GST_LOG_OBJECT (self, "Get local SDP");

  if (self->local_sdp != NULL) {
    gst_sdp_message_copy (self->local_sdp, &sdp);
  }

  return sdp;
}

GstSDPMessage *
kms_sdp_session_get_remote_sdp (KmsSdpSession * self)
{
  GstSDPMessage *sdp = NULL;

  GST_LOG_OBJECT (self, "Get remote SDP");

  if (self->remote_sdp != NULL) {
    gst_sdp_message_copy (self->remote_sdp, &sdp);
  }

  return sdp;
}

void
kms_sdp_session_set_use_ipv6 (KmsSdpSession * self, gboolean use_ipv6)
{
  g_object_set (self->agent, "use-ipv6", use_ipv6, NULL);
}

gboolean
kms_sdp_session_get_use_ipv6 (KmsSdpSession * self)
{
  gboolean ret;

  g_object_get (self->agent, "use-ipv6", &ret, NULL);

  return ret;
}

void
kms_sdp_session_set_addr (KmsSdpSession * self, const gchar * addr)
{
  g_object_set (self->agent, "addr", addr, NULL);
}

static void
kms_sdp_session_finalize (GObject * object)
{
  KmsSdpSession *self = KMS_SDP_SESSION (object);

  GST_LOG_OBJECT (self, "finalize");

  if (self->local_sdp != NULL) {
    gst_sdp_message_free (self->local_sdp);
  }

  if (self->remote_sdp != NULL) {
    gst_sdp_message_free (self->remote_sdp);
  }

  if (self->neg_sdp != NULL) {
    gst_sdp_message_free (self->neg_sdp);
  }

  g_clear_object (&self->ptmanager);
  g_clear_object (&self->agent);
  g_free (self->id_str);

  g_rec_mutex_clear (&self->mutex);

  /* chain up */
  G_OBJECT_CLASS (kms_sdp_session_parent_class)->finalize (object);
}

static void
kms_sdp_session_post_constructor (KmsSdpSession * self, KmsBaseSdpEndpoint * ep,
    guint id)
{
  self->id = id;
  self->id_str = g_strdup_printf ("%s-sess%d", GST_ELEMENT_NAME (ep), id);
  self->ep = ep;
}

static void
kms_sdp_session_init (KmsSdpSession * self)
{
  g_rec_mutex_init (&self->mutex);

  self->agent = kms_sdp_agent_new ();
  self->ptmanager = kms_sdp_payload_manager_new ();
}

static void
kms_sdp_session_class_init (KmsSdpSessionClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);

  gobject_class->finalize = kms_sdp_session_finalize;

  klass->post_constructor = kms_sdp_session_post_constructor;

  gst_element_class_set_details_simple (gstelement_class,
      "SdpSession",
      "Generic",
      "Base bin to manage elements related with a SDP session.",
      "Miguel París Díaz <mparisdiaz@gmail.com>");
}
