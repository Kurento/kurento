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

#ifdef HAVE_CONFIG_H
#  include <config.h>
#endif

#include "kmsagnosticbin.h"
#include "kmsagnosticcaps.h"
#include "kmsutils.h"
#include "kmsparsetreebin.h"
#include "kmsdectreebin.h"
#include "kmsenctreebin.h"
#include "kmsrtppaytreebin.h"

#include "kms-core-enumtypes.h"

#define PLUGIN_NAME "agnosticbin"

#define UNLINKING_DATA "unlinking-data"
G_DEFINE_QUARK (UNLINKING_DATA, unlinking_data);

#define KMS_AGNOSTIC_PAD_STARTED (GST_PAD_FLAG_LAST << 1)

static GstStaticCaps static_raw_audio_caps =
GST_STATIC_CAPS (KMS_AGNOSTIC_RAW_AUDIO_CAPS);
static GstStaticCaps static_raw_video_caps =
GST_STATIC_CAPS (KMS_AGNOSTIC_RAW_VIDEO_CAPS);

#define GST_CAT_DEFAULT kms_agnostic_bin2_debug
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);

#define kms_agnostic_bin2_parent_class parent_class
G_DEFINE_TYPE (KmsAgnosticBin2, kms_agnostic_bin2, GST_TYPE_BIN);

#define KMS_AGNOSTIC_BIN2_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (              \
    (obj),                                   \
    KMS_TYPE_AGNOSTIC_BIN2,                  \
    KmsAgnosticBin2Private                   \
  )                                          \
)

#define KMS_AGNOSTIC_BIN2_LOCK(obj) (                           \
  g_rec_mutex_lock (&KMS_AGNOSTIC_BIN2 (obj)->priv->thread_mutex)   \
)

#define KMS_AGNOSTIC_BIN2_UNLOCK(obj) (                         \
  g_rec_mutex_unlock (&KMS_AGNOSTIC_BIN2 (obj)->priv->thread_mutex) \
)

#define CONFIGURED_KEY "kms-configured-key"

#define DEFAULT_TARGET_ENCODER_BITRATE 300000
#define DEFAULT_MIN_ENCODER_BITRATE 0
#define DEFAULT_MAX_ENCODER_BITRATE G_MAXINT

#define LEAKY_TIME 600000000    /*600 ms */

enum
{
  SIGNAL_MEDIA_TRANSCODING,
  LAST_SIGNAL
};

static guint kms_agnostic_bin2_signals[LAST_SIGNAL] = { 0 };

struct _KmsAgnosticBin2Private
{
  GHashTable *bins;

  GRecMutex thread_mutex;

  GstElement *input_tee;
  GstElement *input_fakesink;
  GstCaps *input_caps;
  GstBin *input_bin;
  GstCaps *input_bin_src_caps;

  GstPad *sink;
  guint pad_count;
  gboolean started;

  GThreadPool *remove_pool;

  gint target_encoder_bitrate;
  gint min_encoder_bitrate;
  gint max_encoder_bitrate;

  GstStructure *codec_config;
  gboolean bitrate_unlimited;

  gboolean transcoding_emitted;
};

enum
{
  PROP_0,
  PROP_TARGET_ENCODER_BITRATE,
  PROP_MIN_ENCODER_BITRATE,
  PROP_MAX_ENCODER_BITRATE,
  PROP_CODEC_CONFIG,
  N_PROPERTIES
};

/* the capabilities of the inputs and outputs. */
static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS (KMS_AGNOSTIC_NO_RTP_CAPS));

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE ("src_%u",
    GST_PAD_SRC,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS_ANY);

static gboolean kms_agnostic_bin2_process_pad (KmsAgnosticBin2 * self,
    GstPad * pad);

static GstBin *kms_agnostic_bin2_find_or_create_bin_for_caps (KmsAgnosticBin2 *
    self, GstCaps * caps);

static void
kms_agnostic_bin2_insert_bin (KmsAgnosticBin2 * self, GstBin * bin)
{
  g_hash_table_insert (self->priv->bins, GST_OBJECT_NAME (bin),
      g_object_ref (bin));
}

/*
 * This function sends a dummy event to force blocked probe to be called
 */
static void
send_dummy_event (GstPad * pad)
{
  GstElement *parent = gst_pad_get_parent_element (pad);

  if (parent == NULL) {
    return;
  }

  if (GST_PAD_IS_SINK (pad)) {
    gst_pad_send_event (pad,
        gst_event_new_custom (GST_EVENT_TYPE_DOWNSTREAM |
            GST_EVENT_TYPE_SERIALIZED,
            gst_structure_new_from_string ("dummy")));
  } else {
    gst_pad_send_event (pad,
        gst_event_new_custom (GST_EVENT_TYPE_UPSTREAM |
            GST_EVENT_TYPE_SERIALIZED,
            gst_structure_new_from_string ("dummy")));
  }

  g_object_unref (parent);
}

static GstPadProbeReturn
tee_src_probe (GstPad * pad, GstPadProbeInfo * info, gpointer user_data)
{
  if (GST_PAD_PROBE_INFO_TYPE (info) & GST_PAD_PROBE_TYPE_EVENT_UPSTREAM) {
    GstEvent *event = gst_pad_probe_info_get_event (info);

    if (GST_EVENT_TYPE (event) == GST_EVENT_RECONFIGURE) {
      // Request keyframe to upstream elements
      kms_utils_drop_until_keyframe (pad, TRUE);
      return GST_PAD_PROBE_DROP;
    }
  }

  return GST_PAD_PROBE_OK;
}

static void
remove_on_unlinked_async (gpointer data, gpointer not_used)
{
  GstElement *elem = GST_ELEMENT_CAST (data);
  GstObject *parent;

  gst_element_set_locked_state (elem, TRUE);
  if (g_strcmp0 (GST_OBJECT_NAME (gst_element_get_factory (elem)),
          "queue") == 0) {
    g_object_set (G_OBJECT (elem), "flush-on-eos", TRUE, NULL);
    gst_element_send_event (elem, gst_event_new_eos ());
  }

  parent = gst_object_get_parent (GST_OBJECT (elem));
  if (parent != NULL) {
    gst_bin_remove (GST_BIN (parent), elem);
    g_object_unref (parent);
  }

  gst_element_set_state (elem, GST_STATE_NULL);
  g_object_unref (elem);
}

static GstPadProbeReturn
remove_on_unlinked_blocked (GstPad * pad, GstPadProbeInfo * info, gpointer elem)
{
  KmsAgnosticBin2 *self;
  GstPad *sink;

  if (elem == NULL) {
    return GST_PAD_PROBE_REMOVE;
  }

  GST_TRACE_OBJECT (pad, "Unlinking pad");

  GST_OBJECT_LOCK (pad);
  if (g_object_get_qdata (G_OBJECT (pad), unlinking_data_quark ())) {
    GST_OBJECT_UNLOCK (pad);
    if (GST_PAD_PROBE_INFO_TYPE (info) & GST_PAD_PROBE_TYPE_QUERY_BOTH) {
      /* Queries must be answered */
      return GST_PAD_PROBE_PASS;
    } else {
      return GST_PAD_PROBE_DROP;
    }
  }

  g_object_set_qdata (G_OBJECT (pad), unlinking_data_quark (),
      GINT_TO_POINTER (TRUE));

  GST_OBJECT_UNLOCK (pad);

  sink = gst_pad_get_peer (pad);
  if (sink != NULL) {
    gst_pad_unlink (pad, sink);
    g_object_unref (sink);
  }

  self = KMS_AGNOSTIC_BIN2 (GST_OBJECT_PARENT (elem));

  g_thread_pool_push (self->priv->remove_pool, g_object_ref (elem), NULL);

  return GST_PAD_PROBE_PASS;
}

static void
remove_on_unlinked_cb (GstPad * pad, GstPad * peer, gpointer user_data)
{
  GstElement *elem = gst_pad_get_parent_element (pad);
  GstObject *parent;
  KmsAgnosticBin2 *self;

  if (elem == NULL) {
    return;
  }

  parent = GST_OBJECT_PARENT (elem);

  if (parent == NULL) {
    goto end;
  }

  if (KMS_IS_AGNOSTIC_BIN2 (parent)) {
    self = KMS_AGNOSTIC_BIN2 (parent);
  } else {
    self = KMS_AGNOSTIC_BIN2 (GST_OBJECT_PARENT (parent));
  }

  if (self != NULL) {
    GstPad *sink = gst_element_get_static_pad (elem, (gchar *) user_data);

    if (sink != NULL) {
      GstPad *peer = gst_pad_get_peer (sink);

      g_object_unref (sink);

      if (peer != NULL) {
        gst_pad_add_probe (peer, GST_PAD_PROBE_TYPE_BLOCK,
            remove_on_unlinked_blocked, g_object_ref (elem), g_object_unref);
        send_dummy_event (peer);
        gst_object_unref (peer);
        goto end;
      }
    }

    g_thread_pool_push (self->priv->remove_pool, g_object_ref (elem), NULL);
  }

end:
  g_object_unref (elem);
}

/* Sink name should be static memory */
static void
remove_element_on_unlinked (GstElement * element, const gchar * pad_name,
    gchar * sink_name)
{
  GstPad *pad = gst_element_get_static_pad (element, pad_name);

  if (pad == NULL) {
    return;
  }

  g_signal_connect (pad, "unlinked", G_CALLBACK (remove_on_unlinked_cb),
      sink_name);

  g_object_unref (pad);
}

static void
remove_tee_pad_on_unlink (GstPad * pad, GstPad * peer, gpointer user_data)
{
  GstElement *tee = gst_pad_get_parent_element (pad);

  if (tee == NULL) {
    return;
  }

  gst_element_release_request_pad (tee, pad);
  g_object_unref (tee);
}

static void
link_element_to_tee (GstElement * tee, GstElement * element)
{
  GstPad *tee_src = gst_element_get_request_pad (tee, "src_%u");
  GstPad *element_sink = gst_element_get_static_pad (element, "sink");
  GstPadLinkReturn ret;

  remove_element_on_unlinked (element, "src", "sink");
  g_signal_connect (tee_src, "unlinked", G_CALLBACK (remove_tee_pad_on_unlink),
      NULL);

  gst_pad_add_probe (tee_src, GST_PAD_PROBE_TYPE_EVENT_UPSTREAM, tee_src_probe,
      NULL, NULL);

  ret = gst_pad_link_full (tee_src, element_sink, GST_PAD_LINK_CHECK_NOTHING);

  if (G_UNLIKELY (GST_PAD_LINK_FAILED (ret))) {
    GST_ERROR ("Linking %" GST_PTR_FORMAT " with %" GST_PTR_FORMAT " result %d",
        tee_src, element_sink, ret);
  }

  g_object_unref (element_sink);
  g_object_unref (tee_src);
}

static GstPadProbeReturn
remove_target_pad_block (GstPad * pad, GstPadProbeInfo * info, gpointer gp)
{
  GST_TRACE_OBJECT (pad, "Drop");
  return GST_PAD_PROBE_DROP;
}

static void
remove_target_pad (GstPad * pad)
{
  // TODO: Remove target pad is just like a disconnection it should be done
  // with care, possibly blocking the pad, or at least disconnecting directly
  // from the tee
  GstPad *target = gst_ghost_pad_get_target (GST_GHOST_PAD (pad));

  GST_TRACE_OBJECT (pad, "Removing target pad");

  if (target == NULL) {
    return;
  }

  gst_pad_add_probe (target, GST_PAD_PROBE_TYPE_DATA_DOWNSTREAM,
      remove_target_pad_block, NULL, NULL);
  gst_ghost_pad_set_target (GST_GHOST_PAD (pad), NULL);

  g_object_unref (target);
}

static gboolean
proxy_src_pad_query_function (GstPad * pad, GstObject * parent,
    GstQuery * query)
{
  gboolean ret = gst_pad_query_default (pad, parent, query);

  if (!ret) {
    return ret;
  }

  if (GST_QUERY_TYPE (query) == GST_QUERY_ACCEPT_CAPS) {
    gboolean accepted;

    gst_query_parse_accept_caps_result (query, &accepted);

    if (!accepted) {
      GstProxyPad *gp = gst_proxy_pad_get_internal (GST_PROXY_PAD (pad));
      KmsAgnosticBin2 *self = NULL;

      GST_ERROR_OBJECT (pad, "Caps not accepted: %" GST_PTR_FORMAT, query);

      if (gp) {
        self = KMS_AGNOSTIC_BIN2 (GST_OBJECT_PARENT (gp));
        if (self) {
          KMS_AGNOSTIC_BIN2_LOCK (self);
          remove_target_pad (GST_PAD_CAST (gp));
          kms_agnostic_bin2_process_pad (self, GST_PAD_CAST (gp));
          KMS_AGNOSTIC_BIN2_UNLOCK (self);
        }
      }

      g_object_unref (gp);
    }
  }

  return ret;
}

static void
kms_agnostic_bin2_link_to_tee (KmsAgnosticBin2 * self, GstPad * pad,
    GstElement * tee, GstCaps * caps)
{
  GstElement *queue = kms_utils_element_factory_make ("queue", PLUGIN_NAME);
  GstPad *target;
  GstProxyPad *proxy;

  gst_bin_add (GST_BIN (self), queue);
  gst_element_sync_state_with_parent (queue);

  if (!(gst_caps_is_any (caps) || gst_caps_is_empty (caps))
      && kms_utils_caps_is_raw (caps)) {
    GstElement *convert = kms_utils_create_convert_for_caps (caps, PLUGIN_NAME);
    GstElement *rate = kms_utils_create_rate_for_caps (caps, PLUGIN_NAME);
    GstElement *mediator = kms_utils_create_mediator_element (caps, PLUGIN_NAME);

    g_object_set (queue, "leaky", 2, "max-size-time", LEAKY_TIME, NULL);

    remove_element_on_unlinked (convert, "src", "sink");
    if (rate) {
      remove_element_on_unlinked (rate, "src", "sink");
    }
    remove_element_on_unlinked (mediator, "src", "sink");

    if (rate) {
      gst_bin_add (GST_BIN (self), rate);
    }

    gst_bin_add_many (GST_BIN (self), convert, mediator, NULL);

    gst_element_sync_state_with_parent (mediator);
    gst_element_sync_state_with_parent (convert);
    if (rate) {
      gst_element_sync_state_with_parent (rate);
    }

    if (rate) {
      gst_element_link_many (queue, rate, mediator, NULL);
    } else {
      gst_element_link (queue, mediator);
    }

    gst_element_link_many (mediator, convert, NULL);
    target = gst_element_get_static_pad (convert, "src");
  } else {
    target = gst_element_get_static_pad (queue, "src");
  }

  gst_ghost_pad_set_target (GST_GHOST_PAD (pad), target);

  proxy = gst_proxy_pad_get_internal (GST_PROXY_PAD (pad));
  gst_pad_set_query_function (GST_PAD_CAST (proxy),
      proxy_src_pad_query_function);
  g_object_unref (proxy);

  g_object_unref (target);
  link_element_to_tee (tee, queue);
}

static gboolean
check_bin (KmsTreeBin * tree_bin, const GstCaps * caps)
{
  gboolean ret = FALSE;
  GstElement *output_tee = kms_tree_bin_get_output_tee (tree_bin);
  GstPad *tee_sink = gst_element_get_static_pad (output_tee, "sink");
  GstCaps *current_caps = kms_tree_bin_get_input_caps (tree_bin);

  GST_LOG_OBJECT (tree_bin,
      "Checking compatibility for bin '%" GST_PTR_FORMAT "'...", tree_bin);

  if (current_caps == NULL) {
    current_caps = gst_pad_get_allowed_caps (tee_sink);
  }

  GST_LOG_OBJECT (tree_bin, "TreeBin '%" GST_PTR_FORMAT "' caps: %"
      GST_PTR_FORMAT, tree_bin, current_caps);

  if (current_caps != NULL && gst_caps_get_size (current_caps) > 0) {
    //TODO: Remove this when problem in negotiation with features will be
    //resolved
    GstCaps *caps_without_features = gst_caps_make_writable (current_caps);

    gst_caps_set_features (caps_without_features, 0,
        gst_caps_features_new_empty ());
    if (gst_caps_can_intersect (caps, caps_without_features)) {
      ret = TRUE;
    }
    gst_caps_unref (caps_without_features);
  }

  g_object_unref (tee_sink);

  return ret;
}

static GstBin *
kms_agnostic_bin2_find_bin_for_caps (KmsAgnosticBin2 * self, GstCaps * caps)
{
  GList *bins, *l;
  GstBin *bin = NULL;

  if (gst_caps_is_any (caps) || gst_caps_is_empty (caps)) {
    return self->priv->input_bin;
  }

  if (check_bin (KMS_TREE_BIN (self->priv->input_bin), caps)) {
    bin = self->priv->input_bin;
  }

  bins = g_hash_table_get_values (self->priv->bins);
  for (l = bins; l != NULL && bin == NULL; l = l->next) {
    KmsTreeBin *tree_bin = KMS_TREE_BIN (l->data);

    if ((void *) tree_bin == (void *) self->priv->input_bin) {
      // Skip: self->priv->input_bin has been already checked
      continue;
    }

    if (check_bin (tree_bin, caps)) {
      bin = GST_BIN_CAST (tree_bin);
    }
  }
  g_list_free (bins);

  return bin;
}

static GstCaps *
kms_agnostic_bin2_get_raw_caps (const GstCaps * caps)
{
  GstCaps *raw_caps = NULL;

  if (kms_utils_caps_is_audio (caps)) {
    raw_caps = gst_static_caps_get (&static_raw_audio_caps);
  } else if (kms_utils_caps_is_video (caps)) {
    raw_caps = gst_static_caps_get (&static_raw_video_caps);
  }

  return raw_caps;
}

static GstBin *
kms_agnostic_bin2_create_dec_bin (KmsAgnosticBin2 * self,
    const GstCaps * raw_caps)
{
  KmsDecTreeBin *dec_bin;
  GstElement *output_tee, *input_element;
  GstCaps *caps = self->priv->input_bin_src_caps;

  if (caps == NULL || raw_caps == NULL) {
    return NULL;
  }

  dec_bin = kms_dec_tree_bin_new (caps, raw_caps);
  if (dec_bin == NULL) {
    return NULL;
  }

  gst_bin_add (GST_BIN (self), GST_ELEMENT (dec_bin));
  gst_element_sync_state_with_parent (GST_ELEMENT (dec_bin));

  output_tee =
      kms_tree_bin_get_output_tee (KMS_TREE_BIN (self->priv->input_bin));
  input_element = kms_tree_bin_get_input_element (KMS_TREE_BIN (dec_bin));
  gst_element_link (output_tee, input_element);

  return GST_BIN (dec_bin);
}

static GstBin *
kms_agnostic_bin2_get_or_create_dec_bin (KmsAgnosticBin2 * self, GstCaps * caps)
{
  GstCaps *raw_caps;

  if (kms_utils_caps_is_raw (self->priv->input_caps)
      || gst_caps_is_empty (caps) || gst_caps_is_any (caps)) {
    return self->priv->input_bin;
  }

  raw_caps = kms_agnostic_bin2_get_raw_caps (caps);

  if (raw_caps != NULL) {
    GstBin *dec_bin;

    GST_LOG ("Raw caps: %" GST_PTR_FORMAT, raw_caps);
    dec_bin = kms_agnostic_bin2_find_bin_for_caps (self, raw_caps);

    if (dec_bin == NULL) {
      dec_bin = kms_agnostic_bin2_create_dec_bin (self, raw_caps);

      if (dec_bin != NULL) {
        kms_agnostic_bin2_insert_bin (self, dec_bin);
      }
    }

    gst_caps_unref (raw_caps);

    return dec_bin;
  } else {
    GST_ELEMENT_WARNING (self, CORE, NEGOTIATION,
        ("Formats are not compatible"), ("Formats are not compatible"));
    return NULL;
  }
}

static GstBin *
kms_agnostic_bin2_create_rtp_pay_bin (KmsAgnosticBin2 * self, GstCaps * caps)
{
  KmsRtpPayTreeBin *bin;
  GstBin *enc_bin;
  GstElement *output_tee, *input_element;
  GstCaps *input_caps;
  GstPad *sink;

  bin = kms_rtp_pay_tree_bin_new (caps);

  if (bin == NULL) {
    return NULL;
  }

  gst_bin_add (GST_BIN (self), GST_ELEMENT (bin));
  gst_element_sync_state_with_parent (GST_ELEMENT (bin));

  input_element = kms_tree_bin_get_input_element (KMS_TREE_BIN (bin));
  sink = gst_element_get_static_pad (input_element, "sink");
  input_caps = gst_pad_query_caps (sink, NULL);
  g_object_unref (sink);

  enc_bin = kms_agnostic_bin2_find_or_create_bin_for_caps (self, input_caps);
  kms_agnostic_bin2_insert_bin (self, GST_BIN (bin));
  gst_caps_unref (input_caps);

  output_tee = kms_tree_bin_get_output_tee (KMS_TREE_BIN (enc_bin));
  gst_element_link (output_tee, input_element);

  return GST_BIN (bin);
}

static GstBin *
kms_agnostic_bin2_create_bin_for_caps (KmsAgnosticBin2 * self, GstCaps * caps)
{
  GstBin *dec_bin;
  KmsEncTreeBin *enc_bin;
  GstElement *input_element, *output_tee;

  if (kms_utils_caps_is_rtp (caps)) {
    return kms_agnostic_bin2_create_rtp_pay_bin (self, caps);
  }

  dec_bin = kms_agnostic_bin2_get_or_create_dec_bin (self, caps);
  if (dec_bin == NULL) {
    return NULL;
  }

  if (kms_utils_caps_is_raw (caps)) {
    return dec_bin;
  }

  enc_bin = kms_enc_tree_bin_new (caps, self->priv->target_encoder_bitrate,
      self->priv->min_encoder_bitrate, self->priv->max_encoder_bitrate,
      self->priv->codec_config);

  if (enc_bin == NULL) {
    return NULL;
  }

  gst_bin_add (GST_BIN (self), GST_ELEMENT (enc_bin));
  gst_element_sync_state_with_parent (GST_ELEMENT (enc_bin));

  output_tee = kms_tree_bin_get_output_tee (KMS_TREE_BIN (dec_bin));
  input_element = kms_tree_bin_get_input_element (KMS_TREE_BIN (enc_bin));
  gst_element_link (output_tee, input_element);

  kms_agnostic_bin2_insert_bin (self, GST_BIN (enc_bin));

  return GST_BIN (enc_bin);
}

static GstBin *
kms_agnostic_bin2_find_or_create_bin_for_caps (KmsAgnosticBin2 * self,
    GstCaps * caps)
{
  GstBin *bin;
  KmsMediaType type;
  gchar* media_type = NULL;

  if (kms_utils_caps_is_audio (caps)) {
    type = KMS_MEDIA_TYPE_AUDIO;
    media_type = g_strdup ("audio");
  }
  else {
    type = KMS_MEDIA_TYPE_VIDEO;
    media_type = g_strdup ("video");
  }

  GST_LOG_OBJECT (self, "Find TreeBin with wanted caps: %" GST_PTR_FORMAT, caps);

  bin = kms_agnostic_bin2_find_bin_for_caps (self, caps);

  if (bin == NULL) {
    GST_LOG_OBJECT (self, "TreeBin not found! Transcoding required for %s",
        media_type);

    bin = kms_agnostic_bin2_create_bin_for_caps (self, caps);
    GST_LOG_OBJECT (self, "Created TreeBin: %" GST_PTR_FORMAT, bin);

    if (!self->priv->transcoding_emitted) {
      self->priv->transcoding_emitted = TRUE;
      g_signal_emit (GST_BIN (self),
          kms_agnostic_bin2_signals[SIGNAL_MEDIA_TRANSCODING], 0, TRUE, type);
      GST_INFO_OBJECT (self, "TRANSCODING ACTIVE for %s", media_type);
    } else {
      GST_LOG_OBJECT (self, "Suppressed - TRANSCODING ACTIVE for %s",
          media_type);
    }
  } else {
    GST_LOG_OBJECT (self, "TreeBin found! Using '%" GST_PTR_FORMAT "' for %s",
        bin, media_type);

    if (!self->priv->transcoding_emitted) {
      self->priv->transcoding_emitted = TRUE;
      g_signal_emit (GST_BIN (self),
          kms_agnostic_bin2_signals[SIGNAL_MEDIA_TRANSCODING], 0, FALSE, type);
      GST_DEBUG_OBJECT (self, "TRANSCODING INACTIVE for %s", media_type);
    } else {
      GST_LOG_OBJECT (self, "Suppressed - TRANSCODING INACTIVE for %s",
          media_type);
    }
  }

  g_free (media_type);

  return bin;
}

/**
 * Link a pad internally
 *
 * @self: The #KmsAgnosticBin2 owner of the pad
 * @pad: (transfer full): The pad to be linked
 * @peer: (transfer full): The peer pad
 */
static void
kms_agnostic_bin2_link_pad (KmsAgnosticBin2 * self, GstPad * pad, GstPad * peer)
{
  GstCaps *pad_caps, *peer_caps;
  GstBin *bin;

  GST_TRACE_OBJECT (self, "Linking: %" GST_PTR_FORMAT
      " to %" GST_PTR_FORMAT, pad, peer);

  pad_caps = gst_pad_query_caps (pad, NULL);
  if (pad_caps != NULL) {
    GST_DEBUG_OBJECT (self, "Upstream provided caps: %" GST_PTR_FORMAT, pad_caps);
    gst_caps_unref (pad_caps);
  }

  peer_caps = gst_pad_query_caps (peer, NULL);
  if (peer_caps == NULL) {
    goto end;
  }

  GST_DEBUG_OBJECT (self, "Downstream wanted caps: %" GST_PTR_FORMAT, peer_caps);

  bin = kms_agnostic_bin2_find_or_create_bin_for_caps (self, peer_caps);

  if (bin != NULL) {
    GstElement *tee = kms_tree_bin_get_output_tee (KMS_TREE_BIN (bin));

    if (!kms_utils_caps_is_rtp (peer_caps)) {
      kms_utils_drop_until_keyframe (pad, TRUE);
    }
    kms_agnostic_bin2_link_to_tee (self, pad, tee, peer_caps);
  }

  gst_caps_unref (peer_caps);

end:
  g_object_unref (peer);
}

/**
 * Process a pad for connecting or disconnecting, it should be always called
 * with the agnostic lock hold.
 *
 * @self: The #KmsAgnosticBin2 owner of the pad
 * @pad: The pad to be processed
 */
static gboolean
kms_agnostic_bin2_process_pad (KmsAgnosticBin2 * self, GstPad * pad)
{
  GstPad *peer = NULL;

  if (!GST_OBJECT_FLAG_IS_SET (pad, KMS_AGNOSTIC_PAD_STARTED)) {
    return FALSE;
  }

  if (!self->priv->started) {
    return FALSE;
  }

  GST_TRACE_OBJECT (self, "Processing pad: %" GST_PTR_FORMAT, pad);

  if (pad == NULL) {
    return FALSE;
  }

  peer = gst_pad_get_peer (pad);

  if (peer != NULL) {
    GstPad *target = gst_ghost_pad_get_target (GST_GHOST_PAD (pad));

    if (target) {
      GstCaps *caps = gst_pad_get_current_caps (pad);

      if (caps != NULL) {
        gboolean accepted;

        accepted = gst_pad_query_accept_caps (peer, caps);
        gst_caps_unref (caps);

        if (accepted) {
          GST_TRACE_OBJECT (self, "No need to reconfigure pad %" GST_PTR_FORMAT,
              pad);
          g_object_unref (target);
          g_object_unref (peer);
          return FALSE;
        }

        remove_target_pad (pad);
      }

      g_object_unref (target);
    }

    kms_agnostic_bin2_link_pad (self, pad, peer);
  }

  return TRUE;
}

static void
add_linked_pads (GstPad * pad, KmsAgnosticBin2 * self)
{
  if (!gst_pad_is_linked (pad)) {
    return;
  }

  remove_target_pad (pad);
  kms_agnostic_bin2_process_pad (self, pad);
}

static GstPadProbeReturn
input_bin_src_caps_probe (GstPad * pad, GstPadProbeInfo * info, gpointer bin)
{
  KmsAgnosticBin2 *self = KMS_AGNOSTIC_BIN2 (GST_OBJECT_PARENT (bin));
  GstEvent *event = gst_pad_probe_info_get_event (info);
  GstCaps *current_caps;

  if (self == NULL) {
    GST_WARNING_OBJECT (bin, "Parent agnosticbin seems to be released");
    return GST_PAD_PROBE_OK;
  }

  GST_TRACE_OBJECT (self, "Event in parser pad: %" GST_PTR_FORMAT, event);

  if (GST_EVENT_TYPE (event) != GST_EVENT_CAPS) {
    return GST_PAD_PROBE_OK;
  }

  KMS_AGNOSTIC_BIN2_LOCK (self);

  self->priv->started = TRUE;
  if (self->priv->input_bin_src_caps != NULL) {
    gst_caps_unref (self->priv->input_bin_src_caps);
  }

  gst_event_parse_caps (event, &current_caps);
  GST_DEBUG_OBJECT (self, "Set input caps: %" GST_PTR_FORMAT, current_caps);
  self->priv->input_bin_src_caps = gst_caps_copy (current_caps);
  kms_agnostic_bin2_insert_bin (self, GST_BIN (bin));

  kms_element_for_each_src_pad (GST_ELEMENT (self),
      (KmsPadIterationAction) add_linked_pads, self);

  KMS_AGNOSTIC_BIN2_UNLOCK (self);

  return GST_PAD_PROBE_REMOVE;
}

static void
remove_bin (gpointer key, gpointer value, gpointer agnosticbin)
{
  GST_TRACE_OBJECT (agnosticbin, "Removing %" GST_PTR_FORMAT, value);
  gst_bin_remove (GST_BIN (agnosticbin), value);
  gst_element_set_state (value, GST_STATE_NULL);
}

static void
kms_agnostic_bin2_configure_input (KmsAgnosticBin2 * self, const GstCaps * caps)
{
  KmsParseTreeBin *parse_bin;
  GstElement *parser;
  GstPad *parser_src;
  GstElement *input_element;

  KMS_AGNOSTIC_BIN2_LOCK (self);

  if (self->priv->input_bin != NULL) {
    kms_tree_bin_unlink_input_element_from_tee (KMS_TREE_BIN (self->
            priv->input_bin));
  }

  parse_bin = kms_parse_tree_bin_new (caps);
  self->priv->input_bin = GST_BIN (parse_bin);

  parser = kms_parse_tree_bin_get_parser (KMS_PARSE_TREE_BIN (parse_bin));
  parser_src = gst_element_get_static_pad (parser, "src");
  gst_pad_add_probe (parser_src, GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM,
      input_bin_src_caps_probe, g_object_ref (parse_bin), g_object_unref);
  g_object_unref (parser_src);

  gst_bin_add (GST_BIN (self), GST_ELEMENT (parse_bin));
  gst_element_sync_state_with_parent (GST_ELEMENT (parse_bin));

  input_element = kms_tree_bin_get_input_element (KMS_TREE_BIN (parse_bin));
  gst_element_link (self->priv->input_tee, input_element);

  self->priv->started = FALSE;

  GST_TRACE_OBJECT (self, "Removing old treebins");
  g_hash_table_foreach (self->priv->bins, remove_bin, self);
  g_hash_table_remove_all (self->priv->bins);

  KMS_AGNOSTIC_BIN2_UNLOCK (self);
}

static GstPadProbeReturn
kms_agnostic_bin2_sink_caps_probe (GstPad * pad, GstPadProbeInfo * info,
    gpointer user_data)
{
  KmsAgnosticBin2 *self;
  GstCaps *current_caps;
  GstCaps *new_caps = NULL;
  GstEvent *event = gst_pad_probe_info_get_event (info);

  if (GST_EVENT_TYPE (event) != GST_EVENT_CAPS) {
    return GST_PAD_PROBE_OK;
  }

  self = KMS_AGNOSTIC_BIN2 (user_data);

  gst_event_parse_caps (event, &new_caps);

  if (!new_caps || gst_caps_get_size (new_caps) == 0) {
    GST_ERROR_OBJECT (self, "Invalid CAPS event: %" GST_PTR_FORMAT, event);
    return GST_PAD_PROBE_OK;
  }

  GST_LOG_OBJECT (self, "Processing CAPS event");

  KMS_AGNOSTIC_BIN2_LOCK (self);
  current_caps = self->priv->input_caps;
  self->priv->input_caps = gst_caps_copy (new_caps);
  KMS_AGNOSTIC_BIN2_UNLOCK (self);

  if (current_caps != NULL) {
    GST_LOG_OBJECT (self, "Current input caps: %" GST_PTR_FORMAT, current_caps);

    GstStructure *st = gst_caps_get_structure (current_caps, 0);

    // Remove fields that can change dynamically without needing
    // an agnostic bin reconstruction.
    gst_structure_remove_fields (st, "width", "height", "framerate",
        "streamheader", "codec_data", NULL);

    if (!gst_caps_can_intersect (new_caps, current_caps)
        && !kms_utils_caps_is_raw (current_caps)
        && !kms_utils_caps_is_raw (new_caps)) {
      GST_LOG_OBJECT (self, "Set new input caps: %" GST_PTR_FORMAT, new_caps);
      kms_agnostic_bin2_configure_input (self, new_caps);
    } else {
      // REVIEW: Why no need when old or new caps are RAW?
      GST_LOG_OBJECT (self, "No need to set new input caps");
    }

    gst_caps_unref (current_caps);
  } else {
    GST_LOG_OBJECT (self, "No previous input caps, starting");
    kms_agnostic_bin2_configure_input (self, new_caps);
  }

  // Monitor video gaps, to halt decoding when an intermediate frame is
  // missing, and handle the keyframe requests.
  if (kms_utils_caps_is_video (new_caps)) {
    kms_utils_pad_monitor_gaps (self->priv->sink);
  }

  return GST_PAD_PROBE_OK;
}

static GstPadProbeReturn
kms_agnostic_bin2_src_reconfigure_probe (GstPad * pad, GstPadProbeInfo * info,
    gpointer user_data)
{
  KmsAgnosticBin2 *self = KMS_AGNOSTIC_BIN2 (gst_pad_get_parent_element (pad));
  GstPadProbeReturn ret = GST_PAD_PROBE_OK;
  GstEvent *event;

  if (self == NULL) {
    return GST_PAD_PROBE_OK;
  }

  if (GST_PAD_PROBE_INFO_TYPE (info) & GST_PAD_PROBE_TYPE_EVENT_BOTH) {
    event = gst_pad_probe_info_get_event (info);

    if (GST_EVENT_TYPE (event) == GST_EVENT_RECONFIGURE) {
      KmsAgnosticBin2 *self = user_data;

      GST_LOG_OBJECT (pad, "Received reconfigure event");

      KMS_AGNOSTIC_BIN2_LOCK (self);
      GST_OBJECT_FLAG_SET (pad, KMS_AGNOSTIC_PAD_STARTED);
      kms_agnostic_bin2_process_pad (self, pad);
      KMS_AGNOSTIC_BIN2_UNLOCK (self);
    }
  }

  g_object_unref (self);

  return ret;
}

static void
kms_agnostic_bin2_src_unlinked (GstPad * pad, GstPad * peer,
    KmsAgnosticBin2 * self)
{
  GST_TRACE_OBJECT (pad, "Unlinked");
  KMS_AGNOSTIC_BIN2_LOCK (self);
  GST_OBJECT_FLAG_UNSET (pad, KMS_AGNOSTIC_PAD_STARTED);
  remove_target_pad (pad);
  KMS_AGNOSTIC_BIN2_UNLOCK (self);
}

static GstPad *
kms_agnostic_bin2_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps)
{
  GstPad *pad;
  gchar *pad_name;
  KmsAgnosticBin2 *self = KMS_AGNOSTIC_BIN2 (element);

  GST_OBJECT_LOCK (self);
  pad_name = g_strdup_printf ("src_%d", self->priv->pad_count++);
  GST_OBJECT_UNLOCK (self);

  pad = gst_ghost_pad_new_no_target_from_template (pad_name, templ);
  g_free (pad_name);

  GST_OBJECT_FLAG_UNSET (pad, KMS_AGNOSTIC_PAD_STARTED);

  gst_pad_add_probe (pad, GST_PAD_PROBE_TYPE_EVENT_UPSTREAM,
      kms_agnostic_bin2_src_reconfigure_probe, element, NULL);

  g_signal_connect (pad, "unlinked",
      G_CALLBACK (kms_agnostic_bin2_src_unlinked), self);

  gst_pad_set_active (pad, TRUE);

  if (gst_element_add_pad (element, pad)) {
    return pad;
  }

  g_object_unref (pad);

  return NULL;
}

static void
kms_agnostic_bin2_release_pad (GstElement * element, GstPad * pad)
{
  gst_element_remove_pad (element, pad);
}

static void
kms_agnostic_bin2_dispose (GObject * object)
{
  KmsAgnosticBin2 *self = KMS_AGNOSTIC_BIN2 (object);

  GST_LOG_OBJECT (object, "dispose");

  KMS_AGNOSTIC_BIN2_LOCK (self);
  g_thread_pool_free (self->priv->remove_pool, FALSE, FALSE);

  if (self->priv->input_bin_src_caps) {
    gst_caps_unref (self->priv->input_bin_src_caps);
    self->priv->input_bin_src_caps = NULL;
  }

  if (self->priv->input_caps) {
    gst_caps_unref (self->priv->input_caps);
    self->priv->input_caps = NULL;
  }

  if (self->priv->codec_config) {
    gst_structure_free (self->priv->codec_config);
    self->priv->codec_config = NULL;
  }

  KMS_AGNOSTIC_BIN2_UNLOCK (self);

  /* chain up */
  G_OBJECT_CLASS (kms_agnostic_bin2_parent_class)->dispose (object);
}

static void
kms_agnostic_bin2_finalize (GObject * object)
{
  KmsAgnosticBin2 *self = KMS_AGNOSTIC_BIN2 (object);

  GST_LOG_OBJECT (object, "finalize");

  g_rec_mutex_clear (&self->priv->thread_mutex);

  g_hash_table_unref (self->priv->bins);

  /* chain up */
  G_OBJECT_CLASS (kms_agnostic_bin2_parent_class)->finalize (object);
}

static void
kms_agnostic_bin_set_encoders_bitrate (KmsAgnosticBin2 * self)
{
  GList *bins, *l;

  bins = g_hash_table_get_values (self->priv->bins);
  for (l = bins; l != NULL; l = l->next) {
    if (KMS_IS_ENC_TREE_BIN (l->data)) {
      kms_enc_tree_bin_set_bitrate_limits (KMS_ENC_TREE_BIN (l->data),
          self->priv->min_encoder_bitrate, self->priv->max_encoder_bitrate);
    }
  }
}

void
kms_agnostic_bin2_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsAgnosticBin2 *self = KMS_AGNOSTIC_BIN2 (object);

  switch (property_id) {
    case PROP_TARGET_ENCODER_BITRATE: {
      gint v = g_value_get_int (value);
      KMS_AGNOSTIC_BIN2_LOCK (self);
      if (v < self->priv->min_encoder_bitrate
          || v > self->priv->max_encoder_bitrate) {
        v = DEFAULT_TARGET_ENCODER_BITRATE;
        GST_WARNING_OBJECT (self,
            "Ignoring out of bounds requested target bitrate; setting default: %d",
            v);
      }
      self->priv->target_encoder_bitrate = v;
      GST_DEBUG_OBJECT (self, "target-encoder-bitrate set: %d",
          self->priv->target_encoder_bitrate);
      kms_agnostic_bin_set_encoders_bitrate (self);
      KMS_AGNOSTIC_BIN2_UNLOCK (self);
      break;
    }
    case PROP_MIN_ENCODER_BITRATE: {
      gint v = g_value_get_int (value);
      KMS_AGNOSTIC_BIN2_LOCK (self);
      if (v > self->priv->max_encoder_bitrate) {
        v = self->priv->max_encoder_bitrate;
        GST_WARNING_OBJECT (self,
            "Ignoring requested bitrate min > max; setting max: %d", v);
      }
      self->priv->min_encoder_bitrate = v;
      GST_DEBUG_OBJECT (self, "min-encoder-bitrate set: %d",
          self->priv->min_encoder_bitrate);
      kms_agnostic_bin_set_encoders_bitrate (self);
      KMS_AGNOSTIC_BIN2_UNLOCK (self);
      break;
    }
    case PROP_MAX_ENCODER_BITRATE: {
      gint v = g_value_get_int (value);
      self->priv->bitrate_unlimited = FALSE;
      KMS_AGNOSTIC_BIN2_LOCK (self);
      if (v == 0) {
        self->priv->bitrate_unlimited = TRUE;
        v = DEFAULT_MAX_ENCODER_BITRATE;
      } else if (v < self->priv->min_encoder_bitrate) {
        v = self->priv->min_encoder_bitrate;
        GST_WARNING_OBJECT (self,
            "Ignoring requested bitrate max < min; setting min: %d", v);
      }
      self->priv->max_encoder_bitrate = v;
      GST_DEBUG_OBJECT (self, "max-encoder-bitrate set: %d",
          self->priv->max_encoder_bitrate);
      kms_agnostic_bin_set_encoders_bitrate (self);
      KMS_AGNOSTIC_BIN2_UNLOCK (self);
      break;
    }
    case PROP_CODEC_CONFIG:
      KMS_AGNOSTIC_BIN2_LOCK (self);
      if (self->priv->codec_config) {
        gst_structure_free (self->priv->codec_config);
        self->priv->codec_config = NULL;
      }
      self->priv->codec_config = g_value_dup_boxed (value);
      KMS_AGNOSTIC_BIN2_UNLOCK (self);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

void
kms_agnostic_bin2_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsAgnosticBin2 *self = KMS_AGNOSTIC_BIN2 (object);

  switch (property_id) {
    case PROP_TARGET_ENCODER_BITRATE:
      KMS_AGNOSTIC_BIN2_LOCK (self);
      g_value_set_int (value, self->priv->target_encoder_bitrate);
      KMS_AGNOSTIC_BIN2_UNLOCK (self);
      break;
    case PROP_MIN_ENCODER_BITRATE:
      KMS_AGNOSTIC_BIN2_LOCK (self);
      g_value_set_int (value, self->priv->min_encoder_bitrate);
      KMS_AGNOSTIC_BIN2_UNLOCK (self);
      break;
    case PROP_MAX_ENCODER_BITRATE:
      KMS_AGNOSTIC_BIN2_LOCK (self);
      if (self->priv->bitrate_unlimited) {
        g_value_set_int (value, 0);
      } else {
        g_value_set_int (value, self->priv->max_encoder_bitrate);
      }
      KMS_AGNOSTIC_BIN2_UNLOCK (self);
      break;
    case PROP_CODEC_CONFIG:
      KMS_AGNOSTIC_BIN2_LOCK (self);
      g_value_set_boxed (value, self->priv->codec_config);
      KMS_AGNOSTIC_BIN2_UNLOCK (self);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_agnostic_bin2_class_init (KmsAgnosticBin2Class * klass)
{
  GObjectClass *gobject_class;
  GstElementClass *gstelement_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gstelement_class = GST_ELEMENT_CLASS (klass);

  gobject_class->dispose = kms_agnostic_bin2_dispose;
  gobject_class->finalize = kms_agnostic_bin2_finalize;
  gobject_class->set_property = kms_agnostic_bin2_set_property;
  gobject_class->get_property = kms_agnostic_bin2_get_property;

  gst_element_class_set_details_simple (gstelement_class,
      "Agnostic connector 2nd version",
      "Generic/Bin/Connector",
      "Automatically encodes/decodes media to match sink and source pads caps",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>");

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&sink_factory));

  gstelement_class->request_new_pad =
      GST_DEBUG_FUNCPTR (kms_agnostic_bin2_request_new_pad);
  gstelement_class->release_pad =
      GST_DEBUG_FUNCPTR (kms_agnostic_bin2_release_pad);

  g_object_class_install_property (gobject_class, PROP_TARGET_ENCODER_BITRATE,
      g_param_spec_int ("target-encoder-bitrate", "target encoder bitrate",
          "Target video bitrate for media transcoding (in bps)",
          0, G_MAXINT, DEFAULT_TARGET_ENCODER_BITRATE, G_PARAM_READWRITE));

  g_object_class_install_property (gobject_class, PROP_MIN_ENCODER_BITRATE,
      g_param_spec_int ("min-encoder-bitrate", "min encoder bitrate",
          "Minimum video bitrate for media transcoding (in bps)",
          0, G_MAXINT, DEFAULT_MIN_ENCODER_BITRATE, G_PARAM_READWRITE));

  g_object_class_install_property (gobject_class, PROP_MAX_ENCODER_BITRATE,
      g_param_spec_int ("max-encoder-bitrate", "max encoder bitrate",
          "Maximum video bitrate for media transcoding (in bps)",
          0, G_MAXINT, DEFAULT_MAX_ENCODER_BITRATE, G_PARAM_READWRITE));

  g_object_class_install_property (gobject_class, PROP_CODEC_CONFIG,
      g_param_spec_boxed ("codec-config", "codec config",
          "Codec configuration", GST_TYPE_STRUCTURE, G_PARAM_READWRITE));

  /* Signal "KmsAgnosticBin::media-transcoding"
   * Arguments:
   * - Is transcoding?
   * - Media type (audio/video)
   */
  kms_agnostic_bin2_signals[SIGNAL_MEDIA_TRANSCODING] =
      g_signal_new ("media-transcoding",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsAgnosticBin2Class, media_transcoding), NULL,
      NULL, NULL, G_TYPE_NONE,
      2, G_TYPE_BOOLEAN, KMS_TYPE_MEDIA_TYPE);

  g_type_class_add_private (klass, sizeof (KmsAgnosticBin2Private));
}

static gboolean
kms_agnostic_bin2_sink_query (GstPad * pad, GstObject * parent,
    GstQuery * query)
{
  gboolean ret;

  if (GST_QUERY_TYPE (query) == GST_QUERY_ACCEPT_CAPS) {
    gst_query_set_accept_caps_result (query, TRUE);
    return TRUE;
  }

  ret = gst_pad_query_default (pad, parent, query);

  if (ret && GST_QUERY_TYPE (query) == GST_QUERY_LATENCY) {
    GstClockTime min_latency;
    GstClockTime max_latency;

    gst_query_parse_latency (query, NULL, &min_latency, &max_latency);

    gst_query_set_latency (query, TRUE, min_latency, max_latency);
  }

  return ret;
}

static GstFlowReturn
check_ret_error (GstPad * pad, GstFlowReturn ret)
{
  switch (ret) {
    case GST_FLOW_OK:
    case GST_FLOW_FLUSHING:
      break;
    case GST_FLOW_ERROR: {

      KmsAgnosticBin2 *self =
          KMS_AGNOSTIC_BIN2 (gst_pad_get_parent_element (pad));

      gchar *fakesink_message;
      g_object_get (self->priv->input_fakesink, "last-message",
          &fakesink_message, NULL);
      GST_FIXME_OBJECT (pad, "Handling flow error, fakesink message: %s",
          fakesink_message);
      g_free (fakesink_message);

      GST_FIXME_OBJECT (pad, "REPLACE FAKESINK");
      GstElement *fakesink = self->priv->input_fakesink;
      kms_utils_bin_remove (GST_BIN (self), fakesink);
      fakesink = kms_utils_element_factory_make ("fakesink", PLUGIN_NAME);
      self->priv->input_fakesink = fakesink;
      g_object_set (fakesink, "async", FALSE, "sync", FALSE,
          "silent", FALSE,
          NULL);

      gst_bin_add (GST_BIN (self), fakesink);
      gst_element_sync_state_with_parent (fakesink);
      gst_element_link (self->priv->input_tee, fakesink);

      // fakesink setup
      GstPad *sink = gst_element_get_static_pad (fakesink, "sink");
      gst_pad_add_probe (sink, GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM,
          kms_agnostic_bin2_sink_caps_probe, self, NULL);
      g_object_unref (sink);

      GST_FIXME_OBJECT (pad, "RECONFIGURE INPUT TREEBIN");
      kms_agnostic_bin2_configure_input (self, self->priv->input_caps);

      // TODO: We should notify this as an error to remote client
      GST_FIXME_OBJECT (pad, "Ignoring flow error");
      ret = GST_FLOW_OK;
      break;
    }
    case GST_FLOW_NOT_NEGOTIATED:
    case GST_FLOW_NOT_LINKED:
      // TODO: We should notify this as an error to remote client
      GST_WARNING_OBJECT (pad, "Ignoring flow status: %s",
          gst_flow_get_name (ret));
      ret = GST_FLOW_OK;
      break;
    default:
      GST_WARNING_OBJECT (pad, "Flow status: %s", gst_flow_get_name (ret));
      break;
  }

  return ret;
}

static GstFlowReturn
kms_agnostic_bin2_sink_chain (GstPad * pad,
    GstObject * parent, GstBuffer * buffer)
{
  GstFlowReturn ret;

  ret = gst_proxy_pad_chain_default (pad, parent, buffer);

  return check_ret_error (pad, ret);
}

static GstFlowReturn
kms_agnostic_bin2_sink_chain_list (GstPad * pad,
    GstObject * parent, GstBufferList * list)
{
  GstFlowReturn ret;

  ret = gst_proxy_pad_chain_list_default (pad, parent, list);

  return check_ret_error (pad, ret);
}

static void
kms_agnostic_bin2_init (KmsAgnosticBin2 * self)
{
  GstPadTemplate *templ;
  GstElement *tee, *fakesink;
  GstPad *target, *sink;

  self->priv = KMS_AGNOSTIC_BIN2_GET_PRIVATE (self);

  tee = kms_utils_element_factory_make ("tee", PLUGIN_NAME);
  self->priv->input_tee = tee;

  fakesink = kms_utils_element_factory_make ("fakesink", PLUGIN_NAME);
  self->priv->input_fakesink = fakesink;
  g_object_set (fakesink, "async", FALSE, "sync", FALSE,
      "silent", FALSE, // FIXME used to print log in check_ret_error()
      NULL);

  gst_bin_add_many (GST_BIN (self), tee, fakesink, NULL);
  gst_element_link_many (tee, fakesink, NULL);

  target = gst_element_get_static_pad (tee, "sink");
  templ = gst_static_pad_template_get (&sink_factory);
  self->priv->sink = gst_ghost_pad_new_from_template ("sink", target, templ);
  gst_pad_set_query_function (self->priv->sink, kms_agnostic_bin2_sink_query);
  gst_pad_set_chain_function (self->priv->sink, kms_agnostic_bin2_sink_chain);
  gst_pad_set_chain_list_function (self->priv->sink,
      kms_agnostic_bin2_sink_chain_list);
  g_object_unref (templ);
  g_object_unref (target);

  sink = gst_element_get_static_pad (fakesink, "sink");
  gst_pad_add_probe (sink, GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM,
      kms_agnostic_bin2_sink_caps_probe, self, NULL);
  g_object_unref (sink);

  gst_element_add_pad (GST_ELEMENT (self), self->priv->sink);

  self->priv->started = FALSE;
  self->priv->remove_pool =
      g_thread_pool_new (remove_on_unlinked_async, NULL, -1, FALSE, NULL);
  self->priv->bins =
      g_hash_table_new_full (g_str_hash, g_str_equal, NULL, g_object_unref);
  g_rec_mutex_init (&self->priv->thread_mutex);
  self->priv->target_encoder_bitrate = DEFAULT_TARGET_ENCODER_BITRATE;
  self->priv->min_encoder_bitrate = DEFAULT_MIN_ENCODER_BITRATE;
  self->priv->max_encoder_bitrate = DEFAULT_MAX_ENCODER_BITRATE;
  self->priv->bitrate_unlimited = TRUE;
  self->priv->transcoding_emitted = FALSE;
}

gboolean
kms_agnostic_bin2_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_AGNOSTIC_BIN2);
}
