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

#include "kmswebrtctransportsrcnice.h"
#include <commons/constants.h>
#include "kmsiceniceagent.h"
#include <stdlib.h>

#define GST_DEFAULT_NAME "webrtctransportsrcnice"
#define GST_CAT_DEFAULT kms_webrtc_transport_src_nice_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_webrtc_transport_src__nice_parent_class parent_class

#define KMS_WEBRTC_TRANSPORT_SRC_NICE_LOCK(src_nice) \
  (g_rec_mutex_lock (&(src_nice)->priv->mutex))

#define KMS_WEBRTC_TRANSPORT_SRC_NICE_UNLOCK(src_nice) \
  (g_rec_mutex_unlock (&(src_nice)->priv->mutex))

#define KMS_WEBRTC_TRANSPORT_SRC_NICE_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (                       \
    (obj),                                            \
    KMS_TYPE_WEBRTC_TRANSPORT_SRC_NICE,                  \
    KmsWebrtcTransportSrcNicePrivate                    \
  )                                                   \
)

// First byte of a DTLS packet is in the range 19 < B < 64.
// Doc: https://datatracker.ietf.org/doc/html/rfc5764#section-5.1.2
#define PACKET_IS_DTLS(b) ((b) > 0x13 && (b) < 0x40)

struct _KmsWebrtcTransportSrcNicePrivate
{
  GRecMutex mutex;

  gboolean pending_buffers_delivered;

  GList *pending_buffers;
};

G_DEFINE_TYPE_WITH_CODE (KmsWebrtcTransportSrcNice, kms_webrtc_transport_src_nice, KMS_TYPE_WEBRTC_TRANSPORT_SRC,
    G_ADD_PRIVATE (KmsWebrtcTransportSrcNice));



static gboolean
gst_buffer_is_dtls (GstBuffer *buffer)
{
  guint8 first_byte;

  if (gst_buffer_get_size (buffer) == 0) {
    GST_DEBUG ("Ignoring buffer with size 0");
    return FALSE;
  }

  if (gst_buffer_extract (buffer, 0, &first_byte, 1) != 1) {
    GST_WARNING ("Could not extract first byte from buffer");
    return FALSE;
  }

  return PACKET_IS_DTLS (first_byte);
}

// Stores DTLS buffers for later use.
// `buffer` is unref and set to NULL only if stored; otherwise left as-is.
static gboolean
store_pending_dtls_buffer (GstBuffer **buffer, guint idx, gpointer user_data)
{
  if (gst_buffer_is_dtls (*buffer)) {
    KmsWebrtcTransportSrcNice *self = KMS_WEBRTC_TRANSPORT_SRC_NICE (user_data);

    GST_DEBUG_OBJECT (self, "Storing DTLS buffer until ICE is CONNECTED");
    self->priv->pending_buffers = g_list_append (self->priv->pending_buffers, *buffer);

    // Side effect: Remove the buffer from its buffer list, if any.
    *buffer = NULL;
  }

  return TRUE;
}



static void
kms_webrtc_transport_src_nice_init (KmsWebrtcTransportSrcNice * self)
{
  KmsWebrtcTransportSrc *parent = KMS_WEBRTC_TRANSPORT_SRC (self);

  self->priv = KMS_WEBRTC_TRANSPORT_SRC_NICE_GET_PRIVATE (self);
  parent->src = gst_element_factory_make ("nicesrc", NULL);

  kms_webrtc_transport_src_connect_elements (parent);
}

static void
kms_webrtc_transport_src_nice_finalize (GObject * object)
{
  KmsWebrtcTransportSrcNice *self = KMS_WEBRTC_TRANSPORT_SRC_NICE (object);

  if (self->priv->pending_buffers != NULL) {
    g_list_free_full (self->priv->pending_buffers, (GDestroyNotify)gst_buffer_unref);
    self->priv->pending_buffers = NULL;
  }
}

static void
kms_webrtc_transport_src_nice_send_pending_buffer (GstBuffer *buffer, KmsWebrtcTransportSrc *self)
{
  GstPad *nicesrc_src = gst_element_get_static_pad (self->src, "src");

  if (gst_pad_push (nicesrc_src, buffer) == GST_FLOW_ERROR ) {
    GST_INFO_OBJECT (self, "Cannot deliver delayed buffer");
  } else {
    GST_DEBUG_OBJECT (self, "Delivered delayed buffer");
  }
  gst_object_unref (nicesrc_src);
}


static gboolean 
do_send_pending_buffers (GstClock * clock,
                     GstClockTime time,
                     GstClockID id,
                     gpointer user_data)
{
  GList *pending_buffers;
  KmsWebrtcTransportSrcNice *self = KMS_WEBRTC_TRANSPORT_SRC_NICE (user_data);

  KMS_WEBRTC_TRANSPORT_SRC_NICE_LOCK (self);
  pending_buffers = self->priv->pending_buffers;
  self->priv->pending_buffers = NULL;
  KMS_WEBRTC_TRANSPORT_SRC_NICE_UNLOCK (self);

  if (pending_buffers != NULL) {
    g_list_foreach (pending_buffers, (GFunc) kms_webrtc_transport_src_nice_send_pending_buffer, self);
    g_list_free (pending_buffers);
  }
  return TRUE;
}

static void
kms_webrtc_transport_src_nice_component_state_changed (KmsIceBaseAgent * agent,
    char *stream_id, guint component_id, IceState state,
    KmsWebrtcTransportSrcNice * self)
{
  gboolean is_client;
  KmsWebrtcTransportSrc *parent = KMS_WEBRTC_TRANSPORT_SRC(self);

  GST_LOG_OBJECT (self,
      "[IceComponentStateChanged] state: %s, stream_id: %s, component_id: %u",
      kms_ice_base_agent_state_to_string (state), stream_id, component_id);

  is_client = parent->dtls_client;

  if (state == ICE_STATE_CONNECTED) {
    if (!is_client) {
      // Send all pending buffer, if any and signal probe to be removed on next Buffer
      KMS_WEBRTC_TRANSPORT_SRC_NICE_LOCK (self);
      self->priv->pending_buffers_delivered = TRUE;
      KMS_WEBRTC_TRANSPORT_SRC_NICE_UNLOCK (self);

      // we have observed that if we immediately send the delayed buffer, and the openssl negotiation process starts, the server hello gets the nicesink not ready yet
      // to send, so to avoid that we delayed the sending by 10 ms
      if (self->priv->pending_buffers != NULL) {
        GstClockTime now;
        GstClockTime filter_time;
        GstClockID filter_time_id;
        GstClock *clock;

        clock = gst_element_get_clock (parent->src);
        now = gst_clock_get_time (clock);
        filter_time = now + (GST_SECOND / 100);
        filter_time_id = gst_clock_new_single_shot_id (clock, filter_time);
        gst_clock_id_wait_async (filter_time_id, do_send_pending_buffers, gst_object_ref (self), gst_object_unref);
        gst_clock_id_unref (filter_time_id);
        gst_object_unref (clock);
      }
    }
  }
}

void
kms_webrtc_transport_src_nice_configure (KmsWebrtcTransportSrc * self,
    KmsIceBaseAgent * agent, const char *stream_id, guint component_id)
{
  KmsIceNiceAgent *nice_agent = KMS_ICE_NICE_AGENT (agent);
  guint id = atoi (stream_id);

  g_object_set (G_OBJECT (self->src),
      "agent", kms_ice_nice_agent_get_agent (nice_agent),
      "stream", id, "component", component_id, NULL);

  g_signal_connect (nice_agent, "on-ice-component-state-changed",
    G_CALLBACK (kms_webrtc_transport_src_nice_component_state_changed), self);
}

// Store DTLS buffers for later delivery when ICE gets to CONNECTED.
static GstPadProbeReturn
kms_webrtc_transport_src_nice_block_dtls_until_ice_connected (GstPad *pad, GstPadProbeInfo *info, gpointer user_data)
{
  KmsWebrtcTransportSrcNice *self = KMS_WEBRTC_TRANSPORT_SRC_NICE(user_data);
  GstPadProbeReturn ret = GST_PAD_PROBE_OK;

  KMS_WEBRTC_TRANSPORT_SRC_NICE_LOCK (self);
  if (self->priv->pending_buffers_delivered) {
    KMS_WEBRTC_TRANSPORT_SRC_NICE_UNLOCK (self);
    GST_DEBUG_OBJECT (self, "No more possible buffers pending, removing probe");
    return GST_PAD_PROBE_REMOVE;
  }

  if (GST_PAD_PROBE_INFO_TYPE(info) & GST_PAD_PROBE_TYPE_BUFFER) {
    GstBuffer *buffer = gst_pad_probe_info_get_buffer (info);

    if (buffer != NULL) {
      store_pending_dtls_buffer (&buffer, 0, self);

      if (buffer == NULL) {
        ret = GST_PAD_PROBE_HANDLED;
      }
    }
  }

  if (GST_PAD_PROBE_INFO_TYPE(info) & GST_PAD_PROBE_TYPE_BUFFER_LIST) {
    GstBufferList *buffer_list = gst_pad_probe_info_get_buffer_list (info);

    if (buffer_list != NULL) {
      gst_buffer_list_foreach (buffer_list, store_pending_dtls_buffer, self);

      if (gst_buffer_list_length (buffer_list) == 0) {
        ret = GST_PAD_PROBE_HANDLED;
      }
    }
  }
  KMS_WEBRTC_TRANSPORT_SRC_NICE_UNLOCK (self);

  return ret;
}



static void
kms_webrtc_transport_src_nice_set_dtls_is_client (KmsWebrtcTransportSrc * src,
    gboolean is_client)
{
  KmsWebrtcTransportSrcNiceClass *klass = 
      KMS_WEBRTC_TRANSPORT_SRC_NICE_CLASS (G_OBJECT_GET_CLASS (src));
  KmsWebrtcTransportSrcClass *parent_klass =
      KMS_WEBRTC_TRANSPORT_SRC_CLASS  (g_type_class_peek_parent(klass));

  parent_klass->set_dtls_is_client (src, is_client);

  if (!is_client) {
    // If is DTLS server (!is_client)
    //    install a blocking probe in dtlssrtpdec sink pad
    //    Blocking probe should buffer all GstBuffers sent to sink pad in dtlssrtpdec
    //    Then when ICE gets to CONNECTED state it should resend all pending buffers
    GstPad *nicesrc_src = gst_element_get_static_pad (src->src, "src");

    gst_pad_add_probe (nicesrc_src, GST_PAD_PROBE_TYPE_BUFFER | GST_PAD_PROBE_TYPE_BUFFER_LIST,  
                      kms_webrtc_transport_src_nice_block_dtls_until_ice_connected, src, NULL);
    gst_object_unref (nicesrc_src);
  }
 
}

static void
kms_webrtc_transport_src_nice_class_init (KmsWebrtcTransportSrcNiceClass *
    klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);
  KmsWebrtcTransportSrcClass *base_class;

  gobject_class->finalize = kms_webrtc_transport_src_nice_finalize;
  base_class = KMS_WEBRTC_TRANSPORT_SRC_CLASS (klass);
  base_class->configure = kms_webrtc_transport_src_nice_configure;
  base_class->set_dtls_is_client = kms_webrtc_transport_src_nice_set_dtls_is_client;

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
      GST_DEFAULT_NAME);

  gst_element_class_set_details_simple (gstelement_class,
      "WebrtcTransportSrcNice",
      "Generic",
      "WebRTC nice transport src elements.",
      "David Fernandez Lopez <d.fernandezlop@gmail.com>");
}

KmsWebrtcTransportSrcNice *
kms_webrtc_transport_src_nice_new ()
{
  GObject *obj;

  obj = g_object_new (KMS_TYPE_WEBRTC_TRANSPORT_SRC_NICE, NULL);

  return KMS_WEBRTC_TRANSPORT_SRC_NICE (obj);
}
