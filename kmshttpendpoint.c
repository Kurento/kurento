#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include <gst/pbutils/encoding-profile.h>

#include "kms-marshal.h"
#include "kmshttpendpoint.h"
#include "kmsagnosticcaps.h"
#include "kmshttpendpointmethod.h"
#include "kms-enumtypes.h"
#include "kmsutils.h"

#define PLUGIN_NAME "httpendpoint"

#define AUDIO_APPSINK "audio_appsink"
#define AUDIO_APPSRC "audio_appsrc"
#define VIDEO_APPSINK "video_appsink"
#define VIDEO_APPSRC "video_appsrc"

#define APPSRC_DATA "appsrc_data"
#define APPSINK_DATA "appsink_data"

#define GET_PIPELINE "get-pipeline"
#define POST_PIPELINE "post-pipeline"

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

struct _PostData
{
  GstElement *appsrc;
};

struct _GetData
{
  GstElement *encodebin;
  GstElement *appsink;
};

struct _KmsHttpEndPointPrivate
{
  KmsHttpEndPointMethod method;
  GstElement *pipeline;
  gboolean start;
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
    return GST_FLOW_ERROR;

  /* element is an appsrc one */
  g_object_get (G_OBJECT (element), "caps", &caps, NULL);
  if (caps == NULL) {
    /* Appsrc has not yet caps defined */
    GstPad *sink_pad = gst_element_get_static_pad (appsink, "sink");

    if (sink_pad != NULL) {
      caps = gst_pad_get_current_caps (sink_pad);
      g_object_unref (sink_pad);
    }

    if (caps != NULL) {
      GST_DEBUG ("Setting caps %" GST_PTR_FORMAT " to %s", caps,
          GST_ELEMENT_NAME (element));
      g_object_set (element, "caps", caps, NULL);
      gst_caps_unref (caps);
    } else
      GST_ERROR ("No caps found for %s", GST_ELEMENT_NAME (element));
  }

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

  GST_INFO ("pad %s added", gst_pad_get_name (pad));

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
    GST_ERROR_OBJECT (self, "No agnostic caps provided");
    gst_caps_unref (src_caps);
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

  GST_DEBUG ("pad %s removed", gst_pad_get_name (pad));

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

  deco_caps = gst_caps_from_string (KMS_AGNOSTIC_AGNOSTIC_CAPS);
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
    GST_ERROR ("Trying to get data from a non-GET HttpEndPoint");
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
    GST_ERROR ("Trying to push data in a non-POST HttpEndPoint");
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
  GstCaps *pc;

  has_video = kms_element_get_video_valve (KMS_ELEMENT (self)) != NULL;
  has_audio = kms_element_get_audio_valve (KMS_ELEMENT (self)) != NULL;

  // TODO: Add a property to select the profile, by now webm is used
  if (has_video)
    pc = gst_caps_from_string ("video/webm");
  else
    pc = gst_caps_from_string ("audio/webm");

  cprof = gst_encoding_container_profile_new ("Webm", NULL, pc, NULL);
  gst_caps_unref (pc);

  if (has_audio) {
    GstCaps *ac = gst_caps_from_string ("audio/x-vorbis");

    gst_encoding_container_profile_add_profile (cprof, (GstEncodingProfile *)
        gst_encoding_audio_profile_new (ac, NULL, NULL, 0));

    gst_caps_unref (ac);
  }

  if (has_video) {
    GstCaps *vc = gst_caps_from_string ("video/x-vp8");

    gst_encoding_container_profile_add_profile (cprof, (GstEncodingProfile *)
        gst_encoding_video_profile_new (vc, NULL, NULL, 0));

    gst_caps_unref (vc);
  }
  // HACK: this is the maximum time that the server can recor, I don't know
  // why but if synchronization is enabled, audio packets are droped
  g_object_set (G_OBJECT (self->priv->get->encodebin), "profile", cprof,
      "audio-jitter-tolerance", G_GUINT64_CONSTANT (0x0fffffffffffffff),
      "avoid-reencoding", TRUE, NULL);
}

static void
kms_http_end_point_update_links (KmsHttpEndPoint * self,
    GstElement * old_encodebin)
{
  GstIterator *it;
  GValue val = G_VALUE_INIT;
  gboolean done = FALSE;

  if (old_encodebin == NULL)
    return;

  it = gst_element_iterate_sink_pads (old_encodebin);
  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstPad *sinkpad, *peer;

        sinkpad = g_value_get_object (&val);
        peer = gst_pad_get_peer (sinkpad);

        if (peer != NULL) {
          GstElement *parent;

          parent = gst_pad_get_parent_element (peer);
          GST_PAD_STREAM_LOCK (peer);
          gst_element_release_request_pad (old_encodebin, sinkpad);
          gst_pad_unlink (peer, sinkpad);
          gst_element_link_pads (parent, GST_OBJECT_NAME (peer),
              self->priv->get->encodebin, GST_OBJECT_NAME (sinkpad));
          GST_PAD_STREAM_UNLOCK (peer);

          g_object_unref (parent);
          g_object_unref (peer);
        }

        g_value_reset (&val);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR ("Error iterating over %s's sink pads",
            GST_ELEMENT_NAME (old_encodebin));
      case GST_ITERATOR_DONE:
        g_value_unset (&val);
        done = TRUE;
        break;
    }
  } while (!done);

  gst_iterator_free (it);
}

static void
remove_encodebin (GstElement * encodebin)
{
  GstElement *peer_element;
  GstPad *src, *peer;
  GstBin *pipe;

  pipe = GST_BIN (gst_object_get_parent (GST_OBJECT (encodebin)));
  src = gst_element_get_static_pad (encodebin, "src");
  peer = gst_pad_get_peer (src);

  if (peer == NULL)
    goto end;

  peer_element = gst_pad_get_parent_element (peer);

  if (peer_element != NULL) {
    gst_bin_remove (pipe, peer_element);
    g_object_unref (peer_element);
  }

  g_object_unref (peer);

end:

  gst_bin_remove (pipe, encodebin);

  g_object_unref (pipe);
  g_object_unref (src);
}

static void
kms_http_end_point_add_appsrc (KmsHttpEndPoint * self, GstElement * valve,
    const gchar * agnostic_caps, const gchar * sinkname, const gchar * srcname,
    const gchar * destpadname)
{
  GstCaps *caps = gst_caps_from_string (agnostic_caps);
  GstElement *appsink, *appsrc;
  GstElement *old_encodebin = NULL;

  if (self->priv->pipeline == NULL)
    kms_http_end_point_init_get_pipeline (self);

  if (self->priv->get->encodebin != NULL)
    old_encodebin = self->priv->get->encodebin;

  self->priv->get->encodebin = gst_element_factory_make ("encodebin", NULL);
  kms_http_end_point_set_profile_to_encodebin (self);

  gst_bin_add (GST_BIN (self->priv->pipeline), self->priv->get->encodebin);
  gst_element_sync_state_with_parent (self->priv->get->encodebin);

  appsrc = gst_element_factory_make ("appsrc", srcname);

  g_object_set (G_OBJECT (appsrc), "is-live", TRUE, "do-timestamp", TRUE,
      "min-latency", G_GUINT64_CONSTANT (0), "max-latency",
      G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME, NULL);

  gst_bin_add (GST_BIN (self->priv->pipeline), appsrc);
  gst_element_sync_state_with_parent (appsrc);
  kms_http_end_point_add_sink (self);

  appsink = gst_element_factory_make ("appsink", sinkname);

  g_object_set (appsink, "emit-signals", TRUE, NULL);
  g_object_set (appsink, "caps", caps, NULL);
  g_object_set (appsink, "async", FALSE, NULL);
  g_object_set (appsink, "sync", FALSE, NULL);
  g_object_set (appsink, "qos", TRUE, NULL);

  gst_caps_unref (caps);
  /* FIXME: (Bug from recorderendpoint). */
  /* Next function get locked with audio and video */
  kms_http_end_point_update_links (self, old_encodebin);

  if (old_encodebin != NULL) {
    if (GST_STATE (old_encodebin) <= GST_STATE_PAUSED) {
      remove_encodebin (old_encodebin);
    } else {
      // TODO: Unlink encodebin and send EOS
      // TODO: Wait for EOS message in bus to destroy encodebin and sink
      gst_element_send_event (old_encodebin, gst_event_new_eos ());
    }
  }

  gst_element_link_pads (appsrc, "src", self->priv->get->encodebin,
      destpadname);

  g_signal_connect (appsink, "new-sample", G_CALLBACK (new_sample_handler),
      appsrc);
  g_signal_connect (appsink, "eos", G_CALLBACK (eos_handler), appsrc);

  gst_bin_add (GST_BIN (self), appsink);
  gst_element_sync_state_with_parent (appsink);
  gst_element_link (valve, appsink);
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
  kms_http_end_point_add_appsrc (httpep, valve, "audio/x-vorbis",
      AUDIO_APPSINK, AUDIO_APPSRC, "audio_%u");

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
    GST_ERROR ("Trying to get data from a non-GET HttpEndPoint");
    return;
  }
  // TODO: This caps should be set using the profile data
  kms_http_end_point_add_appsrc (httpep, valve, "video/x-vp8",
      VIDEO_APPSINK, VIDEO_APPSRC, "video_%u");

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
