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
#include "config.h"
#endif

#include <gst/gst.h>
#include <gst/pbutils/encoding-profile.h>

#include "kms-marshal.h"
#include "kmshttpendpoint.h"
#include "kmsagnosticcaps.h"
#include "kmshttpendpointmethod.h"
#include "kmsrecordingprofile.h"
#include "kms-enumtypes.h"
#include "kmsutils.h"

#define PLUGIN_NAME "httpendpoint"

#define AUDIO_APPSINK "audio_appsink"
#define AUDIO_APPSRC "audio_appsrc"
#define VIDEO_APPSINK "video_appsink"
#define VIDEO_APPSRC "video_appsrc"

#define KEY_DESTINATION_PAD_NAME "kms-pad-key-destination-pad-name"
#define KEY_PAD_PROBE_ID "kms-pad-key-probe-id"

#define APPSRC_DATA "appsrc_data"
#define APPSINK_DATA "appsink_data"

#define GET_PIPELINE "get-pipeline"
#define POST_PIPELINE "post-pipeline"

#define DEFAULT_RECORDING_PROFILE KMS_RECORDING_PROFILE_WEBM

#define GST_CAT_DEFAULT kms_http_end_point_debug_category
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define KMS_HTTP_END_POINT_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (               \
    (obj),                                    \
    KMS_TYPE_HTTP_END_POINT,                  \
    KmsHttpEndPointPrivate                    \
  )                                           \
)
typedef struct _GetData GetData;
typedef struct _PostData PostData;

typedef enum
{
  UNCONFIGURED,
  CONFIGURING,
  CONFIGURED
} HttpGetState;

struct config_data
{
  guint padblocked;
  GSList *blockedpads;
  GSList *pendingpads;
};

struct _PostData
{
  GstElement *appsrc;
};

struct _GetData
{
  GstElement *encodebin;
  GstElement *appsink;
  HttpGetState state;
  struct config_data *confdata;
};

struct _KmsHttpEndPointPrivate
{
  KmsHttpEndPointMethod method;
  GstElement *pipeline;
  gboolean start;
  KmsRecordingProfile profile;
  union
  {
    GetData *get;
    PostData *post;
  };
};

/* Object properties */
enum
{
  PROP_0,
  PROP_METHOD,
  PROP_START,
  PROP_PROFILE,
  N_PROPERTIES
};

#define DEFAULT_HTTP_END_POINT_START FALSE

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

/* Object signals */
enum
{
  /* signals */
  SIGNAL_EOS,
  SIGNAL_NEW_SAMPLE,

  /* actions */
  SIGNAL_PULL_SAMPLE,
  SIGNAL_PUSH_BUFFER,
  SIGNAL_END_OF_STREAM,
  LAST_SIGNAL
};

static guint http_ep_signals[LAST_SIGNAL] = { 0 };

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsHttpEndPoint, kms_http_end_point,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME,
        0, "debug category for httpendpoint element"));

static void
destroy_ulong (gpointer data)
{
  g_slice_free (gulong, data);
}

static GstFlowReturn
new_sample_handler (GstElement * appsink, gpointer user_data)
{
  GstElement *element = GST_ELEMENT (user_data);
  GstFlowReturn ret;
  GstSample *sample = NULL;
  GstBuffer *buffer;
  GstCaps *caps;

  if (KMS_IS_HTTP_END_POINT (element)) {
    /* Data has been received in encodebin's source pad. */
    /* Raise new-sample signal so that application can */
    /* deal with this stuff. */
    g_signal_emit (G_OBJECT (element), http_ep_signals[SIGNAL_NEW_SAMPLE], 0,
        &ret);
    return ret;
  }

  g_signal_emit_by_name (appsink, "pull-sample", &sample);
  if (sample == NULL)
    return GST_FLOW_OK;

  /* element is an appsrc one */
  g_object_get (G_OBJECT (element), "caps", &caps, NULL);
  if (caps == NULL) {
    /* Appsrc has not yet caps defined */
    GstPad *sink_pad = gst_element_get_static_pad (appsink, "sink");

    if (sink_pad != NULL) {
      caps = gst_pad_get_current_caps (sink_pad);
      g_object_unref (sink_pad);
    }

    if (caps == NULL) {
      GST_ELEMENT_ERROR (element, CORE, CAPS, ("No caps found for %s",
              GST_ELEMENT_NAME (element)), GST_ERROR_SYSTEM);
      ret = GST_FLOW_ERROR;
      goto end;
    }

    g_object_set (element, "caps", caps, NULL);
  }

  gst_caps_unref (caps);

  buffer = gst_sample_get_buffer (sample);
  if (buffer == NULL) {
    ret = GST_FLOW_OK;
    goto end;
  }

  gst_buffer_ref (buffer);
  buffer = gst_buffer_make_writable (buffer);

  buffer->pts = GST_CLOCK_TIME_NONE;
  buffer->dts = GST_CLOCK_TIME_NONE;
  buffer->offset = GST_CLOCK_TIME_NONE;
  buffer->offset_end = GST_CLOCK_TIME_NONE;

  /* Pass the buffer through appsrc element which is */
  /* placed in a different pipeline */
  g_signal_emit_by_name (element, "push-buffer", buffer, &ret);

  gst_buffer_unref (buffer);

  if (ret != GST_FLOW_OK) {
    // something wrong
    GST_ERROR ("Could not send buffer to appsrc %s. Ret code %d",
        GST_ELEMENT_NAME (element), ret);
  }

end:
  if (sample != NULL)
    gst_sample_unref (sample);

  return ret;
}

static void
eos_handler (GstElement * appsink, gpointer user_data)
{
  if (KMS_IS_HTTP_END_POINT (user_data)) {
    KmsHttpEndPoint *httep = KMS_HTTP_END_POINT (user_data);

    GST_DEBUG ("EOS detected on %s", GST_ELEMENT_NAME (httep));
    g_signal_emit (httep, http_ep_signals[SIGNAL_EOS], 0);
  } else {
    GstElement *appsrc = GST_ELEMENT (user_data);
    GstFlowReturn ret;

    GST_DEBUG ("EOS detected on %s", GST_ELEMENT_NAME (appsink));
    g_signal_emit_by_name (appsrc, "end-of-stream", &ret);
    if (ret != GST_FLOW_OK)
      GST_ERROR ("Could not send EOS to %s", GST_ELEMENT_NAME (appsrc));
  }
}

static void
post_decodebin_pad_added_handler (GstElement * decodebin, GstPad * pad,
    KmsHttpEndPoint * self)
{
  GstElement *appsrc, *agnosticbin, *appsink;
  GstPad *sinkpad;
  GstCaps *audio_caps, *video_caps;
  GstCaps *src_caps;

  if (GST_PAD_IS_SINK (pad))
    return;

  GST_INFO ("pad %" GST_PTR_FORMAT " added", pad);

  /* Create and link appsrc--agnosticbin with proper caps */
  audio_caps = gst_caps_from_string (KMS_AGNOSTIC_AUDIO_CAPS);
  video_caps = gst_caps_from_string (KMS_AGNOSTIC_VIDEO_CAPS);
  src_caps = gst_pad_query_caps (pad, NULL);
  GST_DEBUG ("caps are %" GST_PTR_FORMAT, src_caps);

  if (gst_caps_can_intersect (audio_caps, src_caps))
    agnosticbin = kms_element_get_audio_agnosticbin (KMS_ELEMENT (self));
  else if (gst_caps_can_intersect (video_caps, src_caps))
    agnosticbin = kms_element_get_video_agnosticbin (KMS_ELEMENT (self));
  else {
    GST_ELEMENT_ERROR (self, CORE, CAPS, ("No agnostic caps provided"),
        GST_ERROR_SYSTEM);
    goto end;
  }

  /* Create appsrc element and link to agnosticbin */
  appsrc = gst_element_factory_make ("appsrc", NULL);
  g_object_set (G_OBJECT (appsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "min-latency", G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME,
      "caps", src_caps, NULL);

  gst_bin_add (GST_BIN (self), appsrc);
  gst_element_sync_state_with_parent (appsrc);
  gst_element_link (appsrc, agnosticbin);

  /* Create appsink and link to pad */
  appsink = gst_element_factory_make ("appsink", NULL);
  g_object_set (appsink, "sync", TRUE, "enable-last-sample",
      FALSE, "emit-signals", TRUE, "qos", TRUE, NULL);
  gst_bin_add (GST_BIN (self->priv->pipeline), appsink);
  gst_element_sync_state_with_parent (appsink);

  sinkpad = gst_element_get_static_pad (appsink, "sink");
  gst_pad_link (pad, sinkpad);
  GST_DEBUG_OBJECT (self, "Linked %s---%s", GST_ELEMENT_NAME (decodebin),
      GST_ELEMENT_NAME (appsink));
  g_object_unref (sinkpad);

  /* Connect new-sample signal to callback */
  g_signal_connect (appsink, "new-sample", G_CALLBACK (new_sample_handler),
      appsrc);
  g_object_set_data (G_OBJECT (pad), APPSRC_DATA, appsrc);
  g_object_set_data (G_OBJECT (pad), APPSINK_DATA, appsink);

end:
  if (src_caps != NULL)
    gst_caps_unref (src_caps);

  if (audio_caps != NULL)
    gst_caps_unref (audio_caps);

  if (video_caps != NULL)
    gst_caps_unref (video_caps);
}

static void
post_decodebin_pad_removed_handler (GstElement * decodebin, GstPad * pad,
    KmsHttpEndPoint * self)
{
  GstElement *appsink, *appsrc;

  if (GST_PAD_IS_SINK (pad))
    return;

  GST_DEBUG ("pad %" GST_PTR_FORMAT " removed", pad);

  appsink = g_object_steal_data (G_OBJECT (pad), APPSINK_DATA);
  appsrc = g_object_steal_data (G_OBJECT (pad), APPSRC_DATA);

  if (appsrc == NULL) {
    GST_ERROR ("No appsink was found associated with %" GST_PTR_FORMAT, pad);
    return;
  }

  if (GST_OBJECT_PARENT (appsrc) != NULL) {
    g_object_ref (appsrc);
    gst_bin_remove (GST_BIN (GST_OBJECT_PARENT (appsrc)), appsrc);
    gst_element_set_state (appsrc, GST_STATE_NULL);
    g_object_unref (appsrc);
  }

  if (appsink == NULL) {
    GST_ERROR ("No appsink was found associated with %" GST_PTR_FORMAT, pad);
    return;
  }

  if (!gst_element_set_locked_state (appsink, TRUE))
    GST_ERROR ("Could not block element %s", GST_ELEMENT_NAME (appsink));

  GST_DEBUG ("Removing appsink %s from %s", GST_ELEMENT_NAME (appsink),
      GST_ELEMENT_NAME (self->priv->pipeline));

  gst_element_set_state (appsink, GST_STATE_NULL);
  gst_bin_remove (GST_BIN (self->priv->pipeline), appsink);
}

static void
bus_message (GstBus * bus, GstMessage * msg, KmsHttpEndPoint * self)
{
  if (GST_MESSAGE_TYPE (msg) == GST_MESSAGE_EOS)
    g_signal_emit (G_OBJECT (self), http_ep_signals[SIGNAL_EOS], 0);
}

static void
kms_http_end_point_init_post_pipeline (KmsHttpEndPoint * self)
{
  GstElement *decodebin;
  GstBus *bus;
  GstCaps *deco_caps;

  self->priv->method = KMS_HTTP_END_POINT_METHOD_POST;
  self->priv->post = g_slice_new0 (PostData);

  self->priv->pipeline = gst_pipeline_new (POST_PIPELINE);
  self->priv->post->appsrc = gst_element_factory_make ("appsrc", NULL);
  decodebin = gst_element_factory_make ("decodebin", NULL);

  deco_caps = gst_caps_from_string (KMS_AGNOSTIC_CAPS_CAPS);
  g_object_set (G_OBJECT (decodebin), "caps", deco_caps, NULL);
  gst_caps_unref (deco_caps);
  /* configure appsrc */
  g_object_set (G_OBJECT (self->priv->post->appsrc), "is-live", TRUE,
      "do-timestamp", TRUE, "min-latency", G_GUINT64_CONSTANT (0),
      "max-latency", G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME, NULL);

  gst_bin_add_many (GST_BIN (self->priv->pipeline), self->priv->post->appsrc,
      decodebin, NULL);

  gst_element_link (self->priv->post->appsrc, decodebin);

  /* Connect decodebin signals */
  g_signal_connect (decodebin, "pad-added",
      G_CALLBACK (post_decodebin_pad_added_handler), self);
  g_signal_connect (decodebin, "pad-removed",
      G_CALLBACK (post_decodebin_pad_removed_handler), self);

  bus = gst_pipeline_get_bus (GST_PIPELINE (self->priv->pipeline));
  gst_bus_add_signal_watch (bus);
  g_signal_connect (G_OBJECT (bus), "message", G_CALLBACK (bus_message), self);
  g_object_unref (bus);

  /* Set pipeline to playing */
  gst_element_set_state (self->priv->pipeline, GST_STATE_PLAYING);
}

static void
kms_http_end_point_init_get_pipeline (KmsHttpEndPoint * self)
{
  self->priv->method = KMS_HTTP_END_POINT_METHOD_GET;
  self->priv->get = g_slice_new0 (GetData);
  self->priv->get->state = UNCONFIGURED;

  self->priv->pipeline = gst_pipeline_new (GET_PIPELINE);
  g_object_set (self->priv->pipeline, "async-handling", TRUE, NULL);
}

static GstSample *
kms_http_end_point_pull_sample_action (KmsHttpEndPoint * self)
{
  GstSample *sample;

  KMS_ELEMENT_LOCK (self);

  if (self->priv->method != KMS_HTTP_END_POINT_METHOD_GET) {
    KMS_ELEMENT_UNLOCK (self);
    GST_ELEMENT_ERROR (self, RESOURCE, FAILED,
        ("Trying to get data from a non-GET HttpEndPoint"), GST_ERROR_SYSTEM);
    return NULL;
  }

  KMS_ELEMENT_UNLOCK (self);

  g_signal_emit_by_name (self->priv->get->appsink, "pull-sample", &sample);

  return sample;
}

static GstFlowReturn
kms_http_end_point_push_buffer_action (KmsHttpEndPoint * self,
    GstBuffer * buffer)
{
  GstFlowReturn ret;

  KMS_ELEMENT_LOCK (self);

  if (self->priv->method != KMS_HTTP_END_POINT_METHOD_UNDEFINED &&
      self->priv->method != KMS_HTTP_END_POINT_METHOD_POST) {
    KMS_ELEMENT_UNLOCK (self);
    GST_ELEMENT_ERROR (self, RESOURCE, FAILED,
        ("Trying to push data in a non-POST HttpEndPoint"), GST_ERROR_SYSTEM);
    return GST_FLOW_ERROR;
  }

  if (self->priv->pipeline == NULL)
    kms_http_end_point_init_post_pipeline (self);

  KMS_ELEMENT_UNLOCK (self);

  g_signal_emit_by_name (self->priv->post->appsrc, "push-buffer", buffer, &ret);

  return ret;
}

static GstFlowReturn
kms_http_end_point_end_of_stream_action (KmsHttpEndPoint * self)
{
  GstFlowReturn ret;

  KMS_ELEMENT_LOCK (self);

  if (self->priv->pipeline == NULL) {
    KMS_ELEMENT_UNLOCK (self);
    GST_ELEMENT_ERROR (self, RESOURCE, FAILED,
        ("Pipeline is not initialized"), GST_ERROR_SYSTEM);
    return GST_FLOW_ERROR;
  }

  KMS_ELEMENT_UNLOCK (self);

  g_signal_emit_by_name (self->priv->post->appsrc, "end-of-stream", &ret);
  return ret;
}

static void
kms_http_end_point_add_sink (KmsHttpEndPoint * self)
{
  self->priv->get->appsink = gst_element_factory_make ("appsink", NULL);

  g_object_set (self->priv->get->appsink, "emit-signals", TRUE, "qos", TRUE,
      NULL);
  g_signal_connect (self->priv->get->appsink, "new-sample",
      G_CALLBACK (new_sample_handler), self);
  g_signal_connect (self->priv->get->appsink, "eos", G_CALLBACK (eos_handler),
      self);

  gst_bin_add (GST_BIN (self->priv->pipeline), self->priv->get->appsink);
  gst_element_sync_state_with_parent (self->priv->get->appsink);

  gst_element_link (self->priv->get->encodebin, self->priv->get->appsink);
}

static void
kms_http_end_point_set_profile_to_encodebin (KmsHttpEndPoint * self)
{
  GstEncodingContainerProfile *cprof;
  gboolean has_audio, has_video;
  const GList *profiles, *l;

  has_video = kms_element_get_video_valve (KMS_ELEMENT (self)) != NULL;
  has_audio = kms_element_get_audio_valve (KMS_ELEMENT (self)) != NULL;

  cprof =
      kms_recording_profile_create_profile (self->priv->profile, has_audio,
      has_video);

  profiles = gst_encoding_container_profile_get_profiles (cprof);

  for (l = profiles; l != NULL; l = l->next) {
    GstEncodingProfile *prof = l->data;
    GstCaps *caps;
    const gchar *appsink_name;
    GstElement *appsink;

    if (GST_IS_ENCODING_AUDIO_PROFILE (prof))
      appsink_name = AUDIO_APPSINK;
    else if (GST_IS_ENCODING_VIDEO_PROFILE (prof))
      appsink_name = VIDEO_APPSINK;
    else
      continue;

    appsink = gst_bin_get_by_name (GST_BIN (self), appsink_name);

    if (appsink == NULL)
      continue;

    caps = gst_encoding_profile_get_input_caps (prof);

    g_object_set (G_OBJECT (appsink), "caps", caps, NULL);

    g_object_unref (appsink);

    gst_caps_unref (caps);
  }

  g_object_set (G_OBJECT (self->priv->get->encodebin), "profile", cprof,
      "audio-jitter-tolerance", 100 * GST_MSECOND,
      "avoid-reencoding", TRUE, NULL);
  gst_encoding_profile_unref (cprof);
}

static void
kms_http_end_point_free_config_data (KmsHttpEndPoint * self)
{
  if (self->priv->get->confdata == NULL)
    return;

  g_slist_free (self->priv->get->confdata->blockedpads);
  g_slist_free (self->priv->get->confdata->pendingpads);
  g_slice_free (struct config_data, self->priv->get->confdata);

  self->priv->get->confdata = NULL;
}

static void
kms_http_end_point_reconnect_pads (KmsHttpEndPoint * self, GSList * pads)
{
  GSList *e;

  for (e = pads; e != NULL; e = e->next) {
    GstPad *srcpad = e->data;
    GstElement *appsrc = gst_pad_get_parent_element (srcpad);
    gchar *destpad = g_object_get_data (G_OBJECT (appsrc),
        KEY_DESTINATION_PAD_NAME);

    GST_DEBUG ("Relinking pad %" GST_PTR_FORMAT " %s", srcpad, destpad);
    if (!gst_element_link_pads (appsrc, "src", self->priv->get->encodebin,
            destpad)) {
      GST_ERROR ("Could not link srcpad %" GST_PTR_FORMAT " to %s", srcpad,
          GST_ELEMENT_NAME (self->priv->get->encodebin));
    }

    gst_object_unref (appsrc);
  }
}

static void
kms_http_end_point_unblock_pads (KmsHttpEndPoint * self, GSList * pads)
{
  GSList *e;

  for (e = pads; e != NULL; e = e->next) {
    GstPad *srcpad = e->data;
    gulong *probe_id = g_object_get_data (G_OBJECT (srcpad), KEY_PAD_PROBE_ID);

    gst_pad_remove_probe (srcpad, *probe_id);
  }
}

static GstPadProbeReturn
event_probe_cb (GstPad * pad, GstPadProbeInfo * info, gpointer user_data)
{
  KmsHttpEndPoint *httpep = KMS_HTTP_END_POINT (user_data);
  GstPad *srcpad, *sinkpad;

  if (GST_EVENT_TYPE (GST_PAD_PROBE_INFO_DATA (info)) != GST_EVENT_EOS)
    return GST_PAD_PROBE_OK;

  /* Old encodebin has been flushed out. It's time to remove it */
  GST_DEBUG ("Event EOS received");

  /* remove the probe first */
  gst_pad_remove_probe (pad, GST_PAD_PROBE_INFO_ID (info));

  KMS_ELEMENT_LOCK (KMS_ELEMENT (httpep));

  /* Unlink encodebin from sinkapp */
  srcpad = gst_element_get_static_pad (httpep->priv->get->encodebin, "src");
  sinkpad = gst_pad_get_peer (srcpad);

  if (!gst_pad_unlink (srcpad, sinkpad))
    GST_ERROR ("Encodebin %s could not be removed",
        GST_ELEMENT_NAME (httpep->priv->get->encodebin));

  g_object_unref (G_OBJECT (srcpad));
  g_object_unref (G_OBJECT (sinkpad));

  /* TODO: Remove old encodebin and sink elements */

  /* Add the new encodebin to the pipeline */
  httpep->priv->get->encodebin = gst_element_factory_make ("encodebin", NULL);
  kms_http_end_point_set_profile_to_encodebin (httpep);
  gst_bin_add (GST_BIN (httpep->priv->pipeline), httpep->priv->get->encodebin);

  /* Add new sink linked to the new encodebin */
  kms_http_end_point_add_sink (httpep);
  gst_element_sync_state_with_parent (httpep->priv->get->encodebin);

  /* Reconnect sources pads */
  kms_http_end_point_reconnect_pads (httpep,
      httpep->priv->get->confdata->blockedpads);

  /* Reconnect pending pads */
  kms_http_end_point_reconnect_pads (httpep,
      httpep->priv->get->confdata->pendingpads);

  /* Remove probes */
  kms_http_end_point_unblock_pads (httpep,
      httpep->priv->get->confdata->blockedpads);
  kms_http_end_point_unblock_pads (httpep,
      httpep->priv->get->confdata->pendingpads);

  httpep->priv->get->state = CONFIGURED;

  kms_http_end_point_free_config_data (httpep);

  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (httpep));

  /* Do not pass the EOS event downstream */
  return GST_PAD_PROBE_DROP;
}

static void
send_eos_to_sink_pads (GstElement * element)
{
  GstIterator *it;
  GValue val = G_VALUE_INIT;
  gboolean done = FALSE;

  it = gst_element_iterate_sink_pads (element);
  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstPad *sinkpad;

        sinkpad = g_value_get_object (&val);
        GST_DEBUG ("Sending event to %" GST_PTR_FORMAT, sinkpad);

        if (!gst_pad_send_event (sinkpad, gst_event_new_eos ()))
          GST_WARNING ("EOS event could not be sent");

        g_value_reset (&val);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR ("Error iterating over %s's sink pads",
            GST_ELEMENT_NAME (element));
      case GST_ITERATOR_DONE:
        g_value_unset (&val);
        done = TRUE;
        break;
    }
  } while (!done);

  gst_iterator_free (it);
}

static GstPadProbeReturn
pad_probe_cb (GstPad * srcpad, GstPadProbeInfo * info, gpointer user_data)
{
  KmsHttpEndPoint *httpep = KMS_HTTP_END_POINT (user_data);
  GstPad *sinkpad;

  GST_DEBUG ("Pad blocked %" GST_PTR_FORMAT, srcpad);
  sinkpad = gst_pad_get_peer (srcpad);

  if (sinkpad == NULL) {
    GST_ERROR ("TODO: This situation should not happen");
    return GST_PAD_PROBE_DROP;
  }

  gst_pad_unlink (srcpad, sinkpad);
  g_object_unref (G_OBJECT (sinkpad));

  KMS_ELEMENT_LOCK (KMS_ELEMENT (httpep));

  httpep->priv->get->confdata->blockedpads =
      g_slist_prepend (httpep->priv->get->confdata->blockedpads, srcpad);
  if (g_slist_length (httpep->priv->get->confdata->blockedpads) ==
      httpep->priv->get->confdata->padblocked) {
    GstPad *pad, *peer;
    gulong *probe_id;

    GST_DEBUG ("Encodebin source pads blocked");
    /* install new probe for EOS */
    pad = gst_element_get_static_pad (httpep->priv->get->encodebin, "src");
    peer = gst_pad_get_peer (pad);

    probe_id = g_object_get_data (G_OBJECT (peer), KEY_PAD_PROBE_ID);
    if (probe_id != NULL) {
      gst_pad_remove_probe (peer, *probe_id);
      g_object_set_data_full (G_OBJECT (sinkpad), KEY_PAD_PROBE_ID, NULL, NULL);
    }

    gst_pad_add_probe (peer, GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM,
        event_probe_cb, httpep, NULL);
    g_object_unref (G_OBJECT (pad));
    g_object_unref (G_OBJECT (peer));

    /* Flush out encodebin data by sending an EOS in all its sinkpads */
    send_eos_to_sink_pads (httpep->priv->get->encodebin);
  }

  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (httpep));

  return GST_PAD_PROBE_OK;
}

static void
kms_http_end_point_remove_encodebin (KmsHttpEndPoint * self)
{
  GstIterator *it;
  GValue val = G_VALUE_INIT;
  gboolean done = FALSE;

  GST_DEBUG ("Blocking encodebin %" GST_PTR_FORMAT, self->priv->get->encodebin);
  self->priv->get->confdata->padblocked = 0;

  it = gst_element_iterate_sink_pads (self->priv->get->encodebin);
  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstPad *sinkpad, *srcpad;

        sinkpad = g_value_get_object (&val);
        srcpad = gst_pad_get_peer (sinkpad);

        if (srcpad != NULL) {
          gulong *probe_id;

          probe_id = g_slice_new0 (gulong);
          *probe_id = gst_pad_add_probe (srcpad,
              GST_PAD_PROBE_TYPE_BLOCK_DOWNSTREAM, pad_probe_cb, self, NULL);
          g_object_set_data_full (G_OBJECT (srcpad), KEY_PAD_PROBE_ID, probe_id,
              destroy_ulong);
          self->priv->get->confdata->padblocked++;
          g_object_unref (srcpad);
        }

        g_value_reset (&val);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR ("Error iterating over %s's sink pads",
            GST_ELEMENT_NAME (self->priv->get->encodebin));
      case GST_ITERATOR_DONE:
        g_value_unset (&val);
        done = TRUE;
        break;
    }
  } while (!done);

  gst_iterator_free (it);
}

static GstPadProbeReturn
pad_probe_blocked_cb (GstPad * srcpad, GstPadProbeInfo * info,
    gpointer user_data)
{
  GST_DEBUG ("Blocked pad %" GST_PTR_FORMAT, srcpad);
  return GST_PAD_PROBE_OK;
}

static void
kms_http_end_point_block_appsrc (KmsHttpEndPoint * self,
    GstElement * appsrc, const gchar * destpadname)
{
  gulong *probe_id;
  GstPad *srcpad;

  srcpad = gst_element_get_static_pad (appsrc, "src");

  probe_id = g_slice_new0 (gulong);
  *probe_id = gst_pad_add_probe (srcpad, GST_PAD_PROBE_TYPE_BLOCK_DOWNSTREAM,
      pad_probe_blocked_cb, NULL, NULL);
  g_object_set_data_full (G_OBJECT (srcpad), KEY_PAD_PROBE_ID, probe_id,
      destroy_ulong);

  self->priv->get->confdata->pendingpads =
      g_slist_prepend (self->priv->get->confdata->pendingpads, srcpad);

  g_object_unref (G_OBJECT (srcpad));
}

static void
kms_http_end_point_add_appsrc (KmsHttpEndPoint * self, GstElement * valve,
    const gchar * sinkname, const gchar * srcname, const gchar * destpadname)
{
  GstElement *appsink, *appsrc;

  GST_DEBUG ("Adding valve %s", GST_ELEMENT_NAME (valve));

  if (self->priv->pipeline == NULL)
    kms_http_end_point_init_get_pipeline (self);

  appsink = gst_element_factory_make ("appsink", sinkname);

  g_object_set (appsink, "emit-signals", TRUE, NULL);
  g_object_set (appsink, "async", FALSE, NULL);
  g_object_set (appsink, "sync", FALSE, NULL);
  g_object_set (appsink, "qos", TRUE, NULL);

  gst_bin_add (GST_BIN (self), appsink);

  appsrc = gst_element_factory_make ("appsrc", srcname);
  g_object_set_data_full (G_OBJECT (appsrc), KEY_DESTINATION_PAD_NAME,
      g_strdup (destpadname), g_free);

  g_object_set (G_OBJECT (appsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "min-latency", G_GUINT64_CONSTANT (0), "max-latency",
      G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME, NULL);

  gst_bin_add (GST_BIN (self->priv->pipeline), appsrc);
  gst_element_sync_state_with_parent (appsrc);

  g_signal_connect (appsink, "new-sample", G_CALLBACK (new_sample_handler),
      appsrc);
  g_signal_connect (appsink, "eos", G_CALLBACK (eos_handler), appsrc);

  gst_element_sync_state_with_parent (appsink);
  gst_element_link (valve, appsink);

  switch (self->priv->get->state) {
    case UNCONFIGURED:
      self->priv->get->encodebin = gst_element_factory_make ("encodebin", NULL);
      kms_http_end_point_set_profile_to_encodebin (self);
      gst_bin_add (GST_BIN (self->priv->pipeline), self->priv->get->encodebin);

      kms_http_end_point_add_sink (self);
      gst_element_sync_state_with_parent (self->priv->get->encodebin);
      gst_element_link_pads (appsrc, "src", self->priv->get->encodebin,
          destpadname);
      self->priv->get->state = CONFIGURED;
      break;
    case CONFIGURED:
      self->priv->get->confdata = g_slice_new0 (struct config_data);

      kms_http_end_point_remove_encodebin (self);
      self->priv->get->state = CONFIGURING;
    default:
      kms_http_end_point_block_appsrc (self, appsrc, destpadname);
      break;
  }
}

static void
kms_http_end_point_audio_valve_added (KmsElement * self, GstElement * valve)
{
  KmsHttpEndPoint *httpep = KMS_HTTP_END_POINT (self);

  if (httpep->priv->method != KMS_HTTP_END_POINT_METHOD_UNDEFINED &&
      httpep->priv->method != KMS_HTTP_END_POINT_METHOD_GET) {
    GST_ERROR ("Trying to get data from a non-GET HttpEndPoint");
    return;
  }
  // TODO: This caps should be set using the profile data
  kms_http_end_point_add_appsrc (httpep, valve, AUDIO_APPSINK, AUDIO_APPSRC,
      "audio_%u");

  /* Drop buffers only if it isn't started */
  kms_utils_set_valve_drop (valve, !httpep->priv->start);
}

static void
kms_http_end_point_audio_valve_removed (KmsElement * self, GstElement * valve)
{
  KmsHttpEndPoint *httpep = KMS_HTTP_END_POINT (self);

  if (httpep->priv->method != KMS_HTTP_END_POINT_METHOD_GET)
    return;

  GST_INFO ("TODO: Implement this");
}

static void
kms_http_end_point_video_valve_added (KmsElement * self, GstElement * valve)
{
  KmsHttpEndPoint *httpep = KMS_HTTP_END_POINT (self);

  if (httpep->priv->method != KMS_HTTP_END_POINT_METHOD_UNDEFINED &&
      httpep->priv->method != KMS_HTTP_END_POINT_METHOD_GET) {
    GST_ELEMENT_ERROR (self, RESOURCE, FAILED,
        ("Trying to get data from a non-GET HttpEndPoint"), GST_ERROR_SYSTEM);
    return;
  }
  // TODO: This caps should be set using the profile data
  kms_http_end_point_add_appsrc (httpep, valve, VIDEO_APPSINK, VIDEO_APPSRC,
      "video_%u");

  /* Drop buffers only if it isn't started */
  kms_utils_set_valve_drop (valve, !httpep->priv->start);
}

static void
kms_http_end_point_video_valve_removed (KmsElement * self, GstElement * valve)
{
  KmsHttpEndPoint *httpep = KMS_HTTP_END_POINT (self);

  if (httpep->priv->method != KMS_HTTP_END_POINT_METHOD_GET)
    return;

  GST_INFO ("TODO: Implement this");
}

static void
kms_http_end_point_dispose_GET (KmsHttpEndPoint * self)
{
  if (self->priv->pipeline == NULL)
    return;

  gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
  g_object_unref (self->priv->pipeline);
  self->priv->pipeline = NULL;
}

static void
kms_http_end_point_dispose_POST (KmsHttpEndPoint * self)
{
  GstBus *bus;

  if (self->priv->pipeline == NULL)
    return;

  bus = gst_pipeline_get_bus (GST_PIPELINE (self->priv->pipeline));
  gst_bus_remove_signal_watch (bus);
  g_object_unref (bus);

  gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
  gst_object_unref (GST_OBJECT (self->priv->pipeline));
  self->priv->pipeline = NULL;
}

static void
kms_http_end_point_dispose (GObject * object)
{
  KmsHttpEndPoint *self = KMS_HTTP_END_POINT (object);

  GST_DEBUG_OBJECT (self, "dispose");

  switch (self->priv->method) {
    case KMS_HTTP_END_POINT_METHOD_GET:
      kms_http_end_point_dispose_GET (self);
      break;
    case KMS_HTTP_END_POINT_METHOD_POST:
      kms_http_end_point_dispose_POST (self);
      break;
    default:
      break;
  }

  /* clean up as possible. May be called multiple times */

  G_OBJECT_CLASS (kms_http_end_point_parent_class)->dispose (object);
}

static void
kms_http_end_point_finalize (GObject * object)
{
  KmsHttpEndPoint *httpendpoint = KMS_HTTP_END_POINT (object);

  GST_DEBUG_OBJECT (httpendpoint, "finalize");

  switch (httpendpoint->priv->method) {
    case KMS_HTTP_END_POINT_METHOD_GET:
      g_slice_free (GetData, httpendpoint->priv->get);
      break;
    case KMS_HTTP_END_POINT_METHOD_POST:
      g_slice_free (PostData, httpendpoint->priv->post);
      break;
    default:
      break;
  }

  /* clean up object here */

  G_OBJECT_CLASS (kms_http_end_point_parent_class)->finalize (object);
}

static void
kms_change_internal_pipeline_state (KmsHttpEndPoint * self,
    const GValue * value)
{
  gboolean prev_val = self->priv->start;
  gboolean start = g_value_get_boolean (value);
  GstElement *audio_v, *video_v;

  if (self->priv->pipeline == NULL) {
    GST_ERROR ("Element %s is not initialized", GST_ELEMENT_NAME (self));
    return;
  }

  if (prev_val == start) {
    /* Nothing to do */
    return;
  }

  audio_v = kms_element_get_audio_valve (KMS_ELEMENT (self));
  if (audio_v != NULL)
    kms_utils_set_valve_drop (audio_v, !start);

  video_v = kms_element_get_video_valve (KMS_ELEMENT (self));
  if (video_v != NULL)
    kms_utils_set_valve_drop (video_v, !start);

  if (start) {
    /* Set pipeline to PLAYING */
    GST_DEBUG ("Setting pipeline to PLAYING");
    if (gst_element_set_state (self->priv->pipeline, GST_STATE_PLAYING) ==
        GST_STATE_CHANGE_ASYNC)
      GST_DEBUG ("Change to PLAYING will be asynchronous");
  } else {
    /* Set pipeline to READY */
    GST_DEBUG ("Setting pipeline to READY.");
    if (gst_element_set_state (self->priv->pipeline, GST_STATE_READY) ==
        GST_STATE_CHANGE_ASYNC)
      GST_DEBUG ("Change to READY will be asynchronous");
  }

  self->priv->start = start;
}

static void
kms_http_end_point_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsHttpEndPoint *self = KMS_HTTP_END_POINT (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_START:{
      kms_change_internal_pipeline_state (self, value);
      break;
    }
    case PROP_PROFILE:
      self->priv->profile = g_value_get_enum (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static void
kms_http_end_point_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsHttpEndPoint *self = KMS_HTTP_END_POINT (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_METHOD:
      g_value_set_enum (value, self->priv->method);
      break;
    case PROP_START:
      g_value_set_boolean (value, self->priv->start);
      break;
    case PROP_PROFILE:
      g_value_set_enum (value, self->priv->profile);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}

static void
kms_http_end_point_class_init (KmsHttpEndPointClass * klass)
{
  KmsElementClass *kms_element_class = KMS_ELEMENT_CLASS (klass);
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "HttpEndPoint", "Generic", "Kurento http end point plugin",
      "Santiago Carot-Nemesio <sancane.kurento@gmail.com>");

  gobject_class->set_property = kms_http_end_point_set_property;
  gobject_class->get_property = kms_http_end_point_get_property;
  gobject_class->dispose = kms_http_end_point_dispose;
  gobject_class->finalize = kms_http_end_point_finalize;

  kms_element_class->audio_valve_added =
      GST_DEBUG_FUNCPTR (kms_http_end_point_audio_valve_added);
  kms_element_class->video_valve_added =
      GST_DEBUG_FUNCPTR (kms_http_end_point_video_valve_added);
  kms_element_class->audio_valve_removed =
      GST_DEBUG_FUNCPTR (kms_http_end_point_audio_valve_removed);
  kms_element_class->video_valve_removed =
      GST_DEBUG_FUNCPTR (kms_http_end_point_video_valve_removed);

  /* Install properties */
  obj_properties[PROP_METHOD] = g_param_spec_enum ("http-method",
      "Http method",
      "Http method used in requests",
      GST_TYPE_HTTP_END_POINT_METHOD,
      KMS_HTTP_END_POINT_METHOD_UNDEFINED, G_PARAM_READABLE);

  obj_properties[PROP_START] = g_param_spec_boolean ("start",
      "start media stream",
      "start media stream", DEFAULT_HTTP_END_POINT_START, G_PARAM_READWRITE);

  obj_properties[PROP_PROFILE] = g_param_spec_enum ("profile",
      "Recording profile",
      "The profile used for encapsulating the media",
      GST_TYPE_RECORDING_PROFILE, DEFAULT_RECORDING_PROFILE, G_PARAM_READWRITE);

  g_object_class_install_properties (gobject_class,
      N_PROPERTIES, obj_properties);

  /* set signals */
  http_ep_signals[SIGNAL_EOS] =
      g_signal_new ("eos",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsHttpEndPointClass, eos_signal), NULL, NULL,
      g_cclosure_marshal_VOID__VOID, G_TYPE_NONE, 0);

  http_ep_signals[SIGNAL_NEW_SAMPLE] =
      g_signal_new ("new-sample", G_TYPE_FROM_CLASS (klass), G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsHttpEndPointClass, new_sample),
      NULL, NULL, __kms_marshal_ENUM__VOID, GST_TYPE_FLOW_RETURN, 0,
      G_TYPE_NONE);

  /* set actions */
  http_ep_signals[SIGNAL_PULL_SAMPLE] =
      g_signal_new ("pull-sample", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsHttpEndPointClass, pull_sample),
      NULL, NULL, __kms_marshal_BOXED__VOID, GST_TYPE_SAMPLE, 0, G_TYPE_NONE);

  http_ep_signals[SIGNAL_PUSH_BUFFER] =
      g_signal_new ("push-buffer", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsHttpEndPointClass, push_buffer),
      NULL, NULL, __kms_marshal_ENUM__BOXED,
      GST_TYPE_FLOW_RETURN, 1, GST_TYPE_BUFFER);

  http_ep_signals[SIGNAL_END_OF_STREAM] =
      g_signal_new ("end-of-stream", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsHttpEndPointClass, end_of_stream),
      NULL, NULL, __kms_marshal_ENUM__VOID,
      GST_TYPE_FLOW_RETURN, 0, G_TYPE_NONE);

  klass->pull_sample = kms_http_end_point_pull_sample_action;
  klass->push_buffer = kms_http_end_point_push_buffer_action;
  klass->end_of_stream = kms_http_end_point_end_of_stream_action;

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsHttpEndPointPrivate));
}

static void
kms_http_end_point_init (KmsHttpEndPoint * self)
{
  self->priv = KMS_HTTP_END_POINT_GET_PRIVATE (self);

  self->priv->method = KMS_HTTP_END_POINT_METHOD_UNDEFINED;
  self->priv->pipeline = NULL;
  self->priv->start = FALSE;
}

gboolean
kms_http_end_point_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_HTTP_END_POINT);
}
