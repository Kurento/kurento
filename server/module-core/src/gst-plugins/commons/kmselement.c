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

#include <gst/gst.h>

#include "kms-core-enumtypes.h"
#include "kms-core-marshal.h"
#include "kmselement.h"
#include "kmsagnosticcaps.h"
#include "kmsstats.h"
#include "kmsutils.h"
#include "kmsrefstruct.h"
#include "constants.h"

#define PLUGIN_NAME "kmselement"
#define DEFAULT_ACCEPT_EOS TRUE

#define TARGET_ENCODER_BITRATE "target-encoder-bitrate"
#define MIN_ENCODER_BITRATE "min-encoder-bitrate"
#define MAX_ENCODER_BITRATE "max-encoder-bitrate"

#define CODEC_CONFIG "codec-config"

#define DEFAULT_TARGET_ENCODER_BITRATE 300000
#define DEFAULT_MIN_ENCODER_BITRATE 0
#define DEFAULT_MAX_ENCODER_BITRATE G_MAXINT

#define MEDIA_FLOW_INTERNAL_TIME_MSEC 2000

GST_DEBUG_CATEGORY_STATIC (kms_element_debug_category);
#define GST_CAT_DEFAULT kms_element_debug_category

G_DEFINE_TYPE_WITH_CODE (KmsElement, kms_element,
    GST_TYPE_BIN,
    GST_DEBUG_CATEGORY_INIT (kms_element_debug_category, PLUGIN_NAME,
        0, "debug category for element"));

#define SINK_PAD "sink_%s_%s"
#define VIDEO_SRC_PAD "video_src_%s_%u"
#define AUDIO_SRC_PAD "audio_src_%s_%u"
#define DATA_SRC_PAD "data_src_%s_%u"

#define KMS_ELEMENT_DEFAULT_PAD_DESCRIPTION "default"
#define KMS_EMPTY_STRING ""
#define KMS_IS_EMPTY_DESCRIPTION(description) \
  (g_strcmp0((description), KMS_EMPTY_STRING) == 0)
#define KMS_IS_VALID_PAD_DESCRIPTION(description) \
  ((description) != NULL && !KMS_IS_EMPTY_DESCRIPTION(description))
#define KMS_FORMAT_PAD_DESCRIPTION(description) \
  (KMS_IS_VALID_PAD_DESCRIPTION(description)) ? (description) : \
  KMS_ELEMENT_DEFAULT_PAD_DESCRIPTION

#define KMS_ELEMENT_GET_PRIVATE(obj) \
  (G_TYPE_INSTANCE_GET_PRIVATE ((obj), KMS_TYPE_ELEMENT, KmsElementPrivate))

#define KMS_ELEMENT_SYNC_LOCK(obj) (                      \
  g_mutex_lock (&(((KmsElement *)(obj))->priv->sync_lock))  \
)

#define KMS_ELEMENT_SYNC_UNLOCK(obj) (                      \
  g_mutex_unlock (&(((KmsElement *)(obj))->priv->sync_lock))  \
)

#define KMS_SET_OBJECT_PROPERTY_SAFELY(obj,name,val) ({      \
  if (g_object_class_find_property (G_OBJECT_GET_CLASS (obj), \
      (name)) != NULL) {                                      \
    GST_DEBUG_OBJECT (obj, "Setting property %s", name);      \
    g_object_set ((obj), (name), (val), NULL);                \
  }                                                           \
});

typedef struct _StreamInputAvgStat
{
  KmsRefStruct ref;
  KmsMediaType type;
  gdouble avg;
} StreamInputAvgStat;

typedef struct _PendingPad
{
  KmsElementPadType type;
  GstPadDirection dir;
  gchar *desc;
} PendingPad;

typedef struct _KmsElementStats
{
  GSList *probes;
  /* Input average stream stats */
  GHashTable *avg_iss;          /* <"pad_name", StreamInputAvgStat> */
} KmsElementStats;

typedef struct _KmsOutputElementData
{
  GstElement *element;
  KmsElementPadType type;
  guint pad_count;
} KmsOutputElementData;

typedef enum _KmsMediaFlowType
{
  KMS_MEDIA_FLOW_IN,
  KMS_MEDIA_FLOW_OUT
} KmsMediaFlowType;

typedef struct _KmsMediaFlowData
{
  KmsRefStruct ref;

  GWeakRef element;
  KmsElementPadType type;
  char *pad_description;
  gint media_flowing;
  gint buffers;
  KmsMediaFlowType media_flow_type;
} KmsMediaFlowData;

typedef struct _KmsMediaFlowTimeoutData
{
  KmsRefStruct ref;

  KmsMediaFlowData *media_flow_data;

  /* Media Flow signal */
  GOnce init;
  KmsLoop *loop;
  guint source_id;
} KmsMediaFlowTimeoutData;

struct _KmsElementPrivate
{
  gchar *id;

  gboolean accept_eos;
  gboolean stats_enabled;

  GHashTable *output_elements;  /* KmsOutputElementData */

  /* Audio and video capabilities */
  GstCaps *audio_caps;
  GstCaps *video_caps;

  GHashTable *pendingpads;

  gint target_encoder_bitrate;
  gint min_encoder_bitrate;
  gint max_encoder_bitrate;

  GstStructure *codec_config;

  /* Statistics */
  KmsElementStats stats;
};

/* Signals and args */
enum
{
  /* Actions */
  REQUEST_NEW_SRCPAD,
  RELEASE_REQUESTED_SRCPAD,
  STATS,
  SIGNAL_FLOW_OUT_MEDIA,
  SIGNAL_FLOW_IN_MEDIA,
  SIGNAL_MEDIA_TRANSCODING,
  LAST_SIGNAL
};

static guint element_signals[LAST_SIGNAL] = { 0 };

enum
{
  PROP_0,
  PROP_ACCEPT_EOS,
  PROP_AUDIO_CAPS,
  PROP_VIDEO_CAPS,
  PROP_TARGET_ENCODER_BITRATE,
  PROP_MIN_ENCODER_BITRATE,
  PROP_MAX_ENCODER_BITRATE,
  PROP_MEDIA_STATS,
  PROP_CODEC_CONFIG,
  PROP_LAST
};

/* the capabilities of the inputs and outputs. */
static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE (SINK_PAD,
    GST_PAD_SINK,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS_ANY);

static GstStaticPadTemplate audio_src_factory =
GST_STATIC_PAD_TEMPLATE (AUDIO_SRC_PAD,
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (KMS_AGNOSTIC_AUDIO_CAPS)
    );

static GstStaticPadTemplate video_src_factory =
GST_STATIC_PAD_TEMPLATE (VIDEO_SRC_PAD,
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (KMS_AGNOSTIC_VIDEO_CAPS)
    );

static GstStaticPadTemplate data_src_factory =
GST_STATIC_PAD_TEMPLATE (DATA_SRC_PAD,
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (KMS_AGNOSTIC_DATA_CAPS)
    );

static KmsOutputElementData *
create_output_element_data (KmsElementPadType type)
{
  KmsOutputElementData *data;

  data = g_slice_new0 (KmsOutputElementData);
  data->type = type;

  return data;
}

static void
media_flow_data_destroy (KmsMediaFlowData * data)
{
  g_free (data->pad_description);
  g_weak_ref_clear (&data->element);

  g_slice_free (KmsMediaFlowData, data);
}

static KmsMediaFlowData *
media_flow_data_new (KmsElement * self, const gchar * description,
    KmsElementPadType type, KmsMediaFlowType media_flow_type)
{
  KmsMediaFlowData *data;

  data = g_slice_new0 (KmsMediaFlowData);

  kms_ref_struct_init (KMS_REF_STRUCT_CAST (data),
      (GDestroyNotify) media_flow_data_destroy);

  data->pad_description = g_strdup (description);
  data->media_flowing = 0;
  data->buffers = 0;
  g_weak_ref_init (&data->element, self);
  data->type = type;
  data->media_flow_type = media_flow_type;

  return data;
}

static void
media_flow_data_unref (KmsMediaFlowData * data)
{
  kms_ref_struct_unref ((KmsRefStruct *) data);
}

static KmsMediaFlowData *
media_flow_data_ref (KmsMediaFlowData * data)
{
  return (KmsMediaFlowData *) kms_ref_struct_ref ((KmsRefStruct *) data);
}

static void
media_flow_timeout_data_destroy (KmsMediaFlowTimeoutData * data)
{
  if (data->source_id != 0) {
    kms_loop_remove (data->loop, data->source_id);
  }

  media_flow_data_unref (data->media_flow_data);

  g_slice_free (KmsMediaFlowTimeoutData, data);
}

static KmsMediaFlowTimeoutData *
media_flow_timeout_data_new (KmsElement * self, const gchar * description,
    KmsElementPadType type, KmsMediaFlowType media_flow_type)
{
  KmsElementClass *klass = KMS_ELEMENT_GET_CLASS (self);
  KmsMediaFlowTimeoutData *data;

  data = g_slice_new0 (KmsMediaFlowTimeoutData);

  kms_ref_struct_init (KMS_REF_STRUCT_CAST (data),
      (GDestroyNotify) media_flow_timeout_data_destroy);

  data->media_flow_data =
      media_flow_data_new (self, description, type, media_flow_type);
  data->init.status = G_ONCE_STATUS_NOTCALLED;
  data->source_id = 0;
  data->loop = klass->loop;

  return data;
}

static void
media_flow_timeout_data_unref (KmsMediaFlowTimeoutData * data)
{
  kms_ref_struct_unref ((KmsRefStruct *) data);
}

static KmsMediaFlowTimeoutData *
media_flow_timeout_data_ref (KmsMediaFlowTimeoutData * data)
{
  return (KmsMediaFlowTimeoutData *) kms_ref_struct_ref ((KmsRefStruct *) data);
}

static void
stream_input_avg_stat_destroy (StreamInputAvgStat * stat)
{
  g_slice_free (StreamInputAvgStat, stat);
}

static StreamInputAvgStat *
stream_input_avg_stat_new (KmsMediaType type)
{
  StreamInputAvgStat *stat;

  stat = g_slice_new0 (StreamInputAvgStat);
  kms_ref_struct_init (KMS_REF_STRUCT_CAST (stat),
      (GDestroyNotify) stream_input_avg_stat_destroy);
  stat->type = type;

  return stat;
}

#define stream_input_avg_stat_ref(obj) \
  kms_ref_struct_ref (KMS_REF_STRUCT_CAST (obj))
#define stream_input_avg_stat_unref(obj) \
  kms_ref_struct_unref (KMS_REF_STRUCT_CAST (obj))

static void
destroy_output_element_data (KmsOutputElementData * data)
{
  g_slice_free (KmsOutputElementData, data);
}

static gchar *
create_id_from_pad_attrs (KmsElementPadType type, GstPadDirection dir,
    const gchar * description)
{
  const gchar *stream, *strdir, *desc;

  desc = KMS_FORMAT_PAD_DESCRIPTION (description);

  switch (dir) {
    case GST_PAD_SRC:
      strdir = "src";
      break;
    case GST_PAD_SINK:
      strdir = "sink";
      break;
    default:
      GST_WARNING ("Unknown pad direction");
      return NULL;
  }

  switch (type) {
    case KMS_ELEMENT_PAD_TYPE_DATA:
      stream = "data";
      break;
    case KMS_ELEMENT_PAD_TYPE_AUDIO:
      stream = "audio";
      break;
    case KMS_ELEMENT_PAD_TYPE_VIDEO:
      stream = "video";
      break;
    default:
      GST_WARNING ("Unsupported pad type %u", type);
      return NULL;
  }

  return g_strdup_printf ("%s_%s_%s", stream, strdir, desc);
}

static PendingPad *
create_pending_pad (KmsElementPadType type, GstPadDirection dir,
    const gchar * desc)
{
  PendingPad *data;

  data = g_slice_new0 (PendingPad);
  data->dir = dir;
  data->type = type;
  data->desc = g_strdup (desc);

  return data;
}

static void
destroy_pendingpads (PendingPad * data)
{
  if (data->desc != NULL) {
    g_free (data->desc);
  }

  g_slice_free (PendingPad, data);
}

static void
kms_element_set_target_on_linked (GstPad * pad, GstPad * peer,
    GstElement * element)
{
  GstPad *target;

  target = gst_element_get_request_pad (element, "src_%u");

  if (GST_PAD_IS_FLUSHING (peer)) {
    gst_pad_send_event (peer, gst_event_new_flush_start ());
    gst_pad_send_event (peer, gst_event_new_flush_stop (FALSE));
  }

  GST_DEBUG_OBJECT (pad, "Setting target %" GST_PTR_FORMAT, target);

  if (!gst_ghost_pad_set_target (GST_GHOST_PAD (pad), target)) {
    GST_ERROR_OBJECT (pad, "Can not set target pad");
  }

  g_object_unref (target);
}

static void
kms_element_remove_target_on_unlinked (GstPad * pad, GstPad * peer,
    GstElement * element)
{
  GstPad *target;

  target = gst_ghost_pad_get_target (GST_GHOST_PAD (pad));

  if (target == NULL) {
    GST_DEBUG_OBJECT (pad, "No target pad");
    return;
  }

  if (!gst_ghost_pad_set_target (GST_GHOST_PAD (pad), NULL)) {
    GST_ERROR_OBJECT (pad, "Can not remove target pad");
  }

  GST_DEBUG_OBJECT (element, "Removing requested pad %" GST_PTR_FORMAT, target);
  gst_element_release_request_pad (element, target);

  g_object_unref (target);
}

static const gchar *
get_pad_template_from_pad_type (KmsElementPadType type)
{
  switch (type) {
    case KMS_ELEMENT_PAD_TYPE_DATA:
      return DATA_SRC_PAD;
    case KMS_ELEMENT_PAD_TYPE_AUDIO:
      return AUDIO_SRC_PAD;
    case KMS_ELEMENT_PAD_TYPE_VIDEO:
      return VIDEO_SRC_PAD;
    default:
      GST_WARNING ("Unsupported pad type: %s", kms_element_pad_type_str (type));
      return NULL;
  }
}

static void
kms_element_add_src_pad (KmsElement * self, GstElement * element,
    const gchar * pad_name, const gchar * templ_name)
{
  GstPad *srcpad;

  srcpad =
      gst_ghost_pad_new_no_target_from_template (pad_name,
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS
          (G_OBJECT_GET_CLASS (self)), templ_name));

  if (GST_STATE (self) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (self) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (self) >= GST_STATE_PAUSED)
    gst_pad_set_active (srcpad, TRUE);

  g_signal_connect (srcpad, "linked",
      G_CALLBACK (kms_element_set_target_on_linked), element);

  g_signal_connect (srcpad, "unlinked",
      G_CALLBACK (kms_element_remove_target_on_unlinked), element);

  gst_element_add_pad (GST_ELEMENT (self), srcpad);
}

static void
kms_element_create_pending_src_pads (KmsElement * self, KmsElementPadType type,
    const gchar * description, GstElement * element)
{
  GHashTableIter iter;
  gpointer key, value;
  const gchar *templ_name;
  GSList *keys = NULL, *l;

  KMS_ELEMENT_LOCK (self);

  templ_name = get_pad_template_from_pad_type (type);
  if (templ_name == NULL) {
    KMS_ELEMENT_UNLOCK (self);
    return;
  }

  g_hash_table_iter_init (&iter, self->priv->pendingpads);
  while (g_hash_table_iter_next (&iter, &key, &value)) {
    PendingPad *pendingpad = value;

    if (pendingpad->type != type || pendingpad->dir != GST_PAD_SRC ||
        g_strcmp0 (pendingpad->desc, description) != 0) {
      continue;
    }

    keys = g_slist_prepend (keys, g_strdup (key));
    g_hash_table_iter_remove (&iter);
  }

  KMS_ELEMENT_UNLOCK (self);

  /* Create all pending pads */
  for (l = keys; l != NULL; l = l->next) {
    kms_element_add_src_pad (self, element, l->data, templ_name);
  }

  g_slist_free_full (keys, g_free);
}

static KmsOutputElementData *
kms_element_get_output_element_data (KmsElement * self, KmsElementPadType type,
    const gchar * description)
{
  KmsOutputElementData *odata;
  gchar *key;

  key = create_id_from_pad_attrs (type, GST_PAD_SRC, description);
  odata = g_hash_table_lookup (self->priv->output_elements, key);
  g_free (key);

  return odata;
}

GstElement *
kms_element_get_data_output_element (KmsElement * self,
    const gchar * description)
{
  return kms_element_get_output_element (self, KMS_ELEMENT_PAD_TYPE_DATA,
      description);
}

static GstPadProbeReturn
cb_buffer_received (GstPad * pad, GstPadProbeInfo * info, gpointer data)
{
  KmsMediaFlowTimeoutData *fdto_data = (KmsMediaFlowTimeoutData *) data;
  KmsMediaFlowData *fd_data = fdto_data->media_flow_data;
  gpointer weak_ptr = g_weak_ref_get (&fd_data->element);
  KmsElement *element;

  if (weak_ptr == NULL) {
    return FALSE;
  }

  element = KMS_ELEMENT (weak_ptr);
  if (g_atomic_int_compare_and_exchange (&fd_data->media_flowing, 0, 1)) {
    if (fd_data->media_flow_type == KMS_MEDIA_FLOW_IN) {
      g_signal_emit (G_OBJECT (element),
          element_signals[SIGNAL_FLOW_IN_MEDIA], 0, TRUE,
          fd_data->pad_description, fd_data->type);
    } else if (fd_data->media_flow_type == KMS_MEDIA_FLOW_OUT) {
      g_signal_emit (G_OBJECT (element),
          element_signals[SIGNAL_FLOW_OUT_MEDIA], 0, TRUE,
          fd_data->pad_description, fd_data->type);
    }
  }

  g_atomic_int_compare_and_exchange (&fd_data->buffers, 0, 1);

  g_object_unref (element);

  return GST_PAD_PROBE_OK;
}

gboolean
check_if_flow_media (gpointer user_data)
{
  KmsMediaFlowData *data = (KmsMediaFlowData *) user_data;
  gpointer weak_ptr;
  KmsElement *element;

  if (g_source_is_destroyed (g_main_current_source ())) {
    return G_SOURCE_REMOVE;
  }

  weak_ptr = g_weak_ref_get (&data->element);
  if (weak_ptr == NULL) {
    return G_SOURCE_REMOVE;
  }

  element = KMS_ELEMENT (weak_ptr);
  if (g_atomic_int_get (&data->media_flowing) == 1) {
    if (g_atomic_int_get (&data->buffers) == 0) {
      g_atomic_int_set (&data->media_flowing, 0);
      if (data->media_flow_type == KMS_MEDIA_FLOW_IN) {
        g_signal_emit (G_OBJECT (element),
            element_signals[SIGNAL_FLOW_IN_MEDIA], 0, FALSE,
            data->pad_description, data->type);
      } else if (data->media_flow_type == KMS_MEDIA_FLOW_OUT) {
        g_signal_emit (G_OBJECT (element),
            element_signals[SIGNAL_FLOW_OUT_MEDIA], 0, FALSE,
            data->pad_description, data->type);
      }
    } else {
      g_atomic_int_set (&data->buffers, 0);
    }
  }

  g_object_unref (element);

  return G_SOURCE_CONTINUE;
}

static gpointer
attach_timeout (gpointer data)
{
  KmsMediaFlowTimeoutData *fdto_data = data;
  KmsMediaFlowData *fd_data = fdto_data->media_flow_data;

  fdto_data->source_id = kms_loop_timeout_add_full (fdto_data->loop,
      G_PRIORITY_DEFAULT, MEDIA_FLOW_INTERNAL_TIME_MSEC, check_if_flow_media,
      media_flow_data_ref (fd_data), (GDestroyNotify) media_flow_data_unref);

  return NULL;
}

static void
add_flow_event_probes (GstPad * pad, KmsMediaFlowTimeoutData * fdto_data)
{
  gst_pad_add_probe (pad,
      GST_PAD_PROBE_TYPE_BUFFER | GST_PAD_PROBE_TYPE_BUFFER_LIST,
      (GstPadProbeCallback) cb_buffer_received,
      media_flow_timeout_data_ref (fdto_data),
      (GDestroyNotify) media_flow_timeout_data_unref);

  /* TODO: the timeout could be detached when all pads are removed,
     but it must be added if a new pad is added */
  g_once (&fdto_data->init, attach_timeout, fdto_data);
}

static void
add_flow_event_probes_pad_added (GstElement * element, GstPad * pad,
    KmsMediaFlowTimeoutData * fdto_data)
{
  if (!GST_PAD_IS_SINK (pad)) {
    return;
  }

  add_flow_event_probes (pad, fdto_data);
}

static void
media_flow_data_destroy_closure (gpointer data, GClosure * closure)
{
  KmsMediaFlowTimeoutData *fdto_data = data;

  media_flow_timeout_data_unref (fdto_data);
}

static void
add_flow_out_event_probes_to_element_sinks (GstElement * element,
    KmsMediaFlowTimeoutData * fdto_data)
{
  g_signal_connect_data (element, "pad-added",
      G_CALLBACK (add_flow_event_probes_pad_added),
      media_flow_timeout_data_ref (fdto_data), media_flow_data_destroy_closure,
      0);

  kms_element_for_each_sink_pad (element,
      (KmsPadCallback) add_flow_event_probes, fdto_data);
}

static void
kms_element_set_video_output_properties (KmsElement * self,
    GstElement * element)
{
  KMS_SET_OBJECT_PROPERTY_SAFELY (element, CODEC_CONFIG,
      self->priv->codec_config);

  KMS_SET_OBJECT_PROPERTY_SAFELY (element, TARGET_ENCODER_BITRATE,
      self->priv->target_encoder_bitrate);

  KMS_SET_OBJECT_PROPERTY_SAFELY (element, MAX_ENCODER_BITRATE,
      self->priv->max_encoder_bitrate);

  KMS_SET_OBJECT_PROPERTY_SAFELY (element, MIN_ENCODER_BITRATE,
      self->priv->min_encoder_bitrate);
}

static void
on_agnosticbin_media_transcoding (GstBin * bin, gboolean is_transcoding,
    KmsMediaType media_type, KmsElement * self)
{
  KmsElementPadType pad_type = kms_utils_convert_media_type (media_type);

  g_signal_emit (self,
      element_signals[SIGNAL_MEDIA_TRANSCODING], 0,
      is_transcoding, GST_ELEMENT_NAME (bin), pad_type);
}

GstElement *
kms_element_get_output_element (KmsElement * self, KmsElementPadType pad_type,
    const gchar * description)
{
  KmsOutputElementData *odata;
  const gchar *desc;

  desc = KMS_FORMAT_PAD_DESCRIPTION (description);

  GST_DEBUG_OBJECT (self, "Output element requested for track %s, stream %s",
      kms_element_pad_type_str (pad_type), desc);

  KMS_ELEMENT_LOCK (self);

  odata = kms_element_get_output_element_data (self, pad_type, desc);
  if (odata == NULL) {
    gchar *key;

    key = create_id_from_pad_attrs (pad_type, GST_PAD_SRC, desc);
    GST_DEBUG_OBJECT (self, "New output element for track %s, stream %s",
        kms_element_pad_type_str (pad_type), key);
    odata = create_output_element_data (pad_type);
    g_hash_table_insert (self->priv->output_elements, key, odata);
  }

  if (odata->element != NULL) {
    KMS_ELEMENT_UNLOCK (self);
    return odata->element;
  }

  if (pad_type == KMS_ELEMENT_PAD_TYPE_DATA) {
    GstElement *tee, *sink;

    gchar *name = gst_element_get_name (GST_ELEMENT (self));
    tee = kms_utils_element_factory_make ("tee", name);
    sink = kms_utils_element_factory_make ("fakesink", name);
    g_free (name);

    g_object_set (sink, "sync", FALSE, "async", FALSE, NULL);

    gst_bin_add_many (GST_BIN (self), tee, sink, NULL);
    gst_element_link (tee, sink);

    odata->element = tee;
    KMS_ELEMENT_UNLOCK (self);

    gst_element_sync_state_with_parent (sink);
    gst_element_sync_state_with_parent (tee);
  } else {
    KmsMediaFlowTimeoutData *fdto_data;

    odata->element = KMS_ELEMENT_GET_CLASS (self)->create_output_element (self);

    g_signal_connect (odata->element, "media-transcoding",
        G_CALLBACK (on_agnosticbin_media_transcoding), self);

    fdto_data =
        media_flow_timeout_data_new (self, desc, pad_type, KMS_MEDIA_FLOW_OUT);
    add_flow_out_event_probes_to_element_sinks (odata->element, fdto_data);
    media_flow_timeout_data_unref (fdto_data);

    /* Set video properties to the new element */
    if (pad_type == KMS_ELEMENT_PAD_TYPE_VIDEO) {
      kms_element_set_video_output_properties (self, odata->element);
    }

    gst_bin_add (GST_BIN (self), odata->element);
    gst_element_sync_state_with_parent (odata->element);
    KMS_ELEMENT_UNLOCK (self);
  }

  kms_element_create_pending_src_pads (self, pad_type, desc, odata->element);

  return odata->element;
}

GstElement *
kms_element_get_output_element_from_media_type (KmsElement * self,
    KmsMediaType media_type, const gchar * description)
{
  KmsElementPadType pad_type;

  switch (media_type) {
    case KMS_MEDIA_TYPE_AUDIO:
      pad_type = KMS_ELEMENT_PAD_TYPE_AUDIO;
      break;
    case KMS_MEDIA_TYPE_VIDEO:
      pad_type = KMS_ELEMENT_PAD_TYPE_VIDEO;
      break;
    case KMS_MEDIA_TYPE_DATA:
      pad_type = KMS_ELEMENT_PAD_TYPE_DATA;
      break;
    default:
      GST_ERROR_OBJECT (self,
          "Invalid media type while requesting output element");
      return NULL;
  }

  return kms_element_get_output_element (self, pad_type, description);
}

GstElement *
kms_element_get_audio_output_element (KmsElement * self,
    const gchar * description)
{
  return kms_element_get_output_element (self, KMS_ELEMENT_PAD_TYPE_AUDIO,
      description);
}

GstElement *
kms_element_get_video_output_element (KmsElement * self,
    const gchar * description)
{
  return kms_element_get_output_element (self, KMS_ELEMENT_PAD_TYPE_VIDEO,
      description);
}

static void
send_flush_on_unlink (GstPad * pad, GstPad * peer, gpointer user_data)
{
  if (GST_OBJECT_FLAG_IS_SET (pad, GST_PAD_FLAG_EOS)) {
    gst_pad_send_event (pad, gst_event_new_flush_start ());
    gst_pad_send_event (pad, gst_event_new_flush_stop (FALSE));
  }
}

static GstPadProbeReturn
accept_eos_probe (GstPad * pad, GstPadProbeInfo * info, gpointer data)
{
  GstEvent *event = gst_pad_probe_info_get_event (info);
  GstEventType type = GST_EVENT_TYPE (event);

  if (type == GST_EVENT_EOS || type == GST_EVENT_FLUSH_START
      || type == GST_EVENT_FLUSH_STOP) {
    KmsElement *self;
    GstPadProbeReturn ret;

    self = KMS_ELEMENT (data);

    if (!g_atomic_int_get (&self->priv->accept_eos)) {
      GST_DEBUG_OBJECT (pad, "Event %s dropped",
          gst_event_type_get_name (type));
      ret = GST_PAD_PROBE_DROP;
    } else {
      ret = GST_PAD_PROBE_OK;
    }

    return ret;
  }

  return GST_PAD_PROBE_OK;
}

static gboolean
kms_element_sink_query_default (KmsElement * self, GstPad * pad,
    GstQuery * query)
{
  /* Invoke default pad query function */
  return gst_pad_query_default (pad, GST_OBJECT (self), query);
}

static gboolean
kms_element_pad_query (GstPad * pad, GstObject * parent, GstQuery * query)
{
  KmsElementClass *klass;
  KmsElement *element;

  element = KMS_ELEMENT (parent);
  klass = KMS_ELEMENT_GET_CLASS (element);

  return klass->sink_query (element, pad, query);
}

const gchar *
kms_element_pad_type_str (KmsElementPadType type)
{
  switch (type) {
    case KMS_ELEMENT_PAD_TYPE_VIDEO:
      return "video";
    case KMS_ELEMENT_PAD_TYPE_AUDIO:
      return "audio";
    case KMS_ELEMENT_PAD_TYPE_DATA:
      return "data";
    default:
      return "";
  }
}

static gchar *
get_sink_pad_name (KmsElementPadType type, const gchar * description)
{
  const gchar *desc;

  desc = KMS_FORMAT_PAD_DESCRIPTION (description);

  return g_strdup_printf (SINK_PAD, kms_element_pad_type_str (type), desc);
}

static void
kms_element_calculate_stats (GstPad * pad, KmsMediaType type,
    GstClockTimeDiff t, KmsList * mdata, gpointer user_data)
{
  StreamInputAvgStat *sstat = (StreamInputAvgStat *) user_data;

  if ((sstat->type != KMS_MEDIA_TYPE_AUDIO &&
          sstat->type != KMS_MEDIA_TYPE_VIDEO)) {
    GST_DEBUG_OBJECT (pad, "No statistics calculated for media (%u)",
        sstat->type);

    return;
  }

  sstat->avg = KMS_STATS_CALCULATE_LATENCY_AVG (t, sstat->avg);
}

static void
kms_element_set_sink_input_stats (KmsElement * self, GstPad * pad,
    KmsElementPadType type)
{
  StreamInputAvgStat *sstat;
  KmsStatsProbe *s_probe;
  KmsMediaType media_type;
  gchar *padname;

  switch (type) {
    case KMS_ELEMENT_PAD_TYPE_AUDIO:
      media_type = KMS_MEDIA_TYPE_AUDIO;
      break;
    case KMS_ELEMENT_PAD_TYPE_VIDEO:
      media_type = KMS_MEDIA_TYPE_VIDEO;
      break;
    default:
      GST_DEBUG ("No stats collected for pad type %d", type);
      return;
  }

  s_probe = kms_stats_probe_new (pad, media_type);

  KMS_ELEMENT_LOCK (self);

  padname = gst_pad_get_name (pad);
  sstat = g_hash_table_lookup (self->priv->stats.avg_iss, padname);

  if (sstat != NULL) {
    g_free (padname);
  } else {
    GST_DEBUG_OBJECT (self, "Generating average stats for pad %" GST_PTR_FORMAT,
        pad);
    sstat = stream_input_avg_stat_new (media_type);
    g_hash_table_insert (self->priv->stats.avg_iss, padname, sstat);
  }

  self->priv->stats.probes = g_slist_prepend (self->priv->stats.probes,
      s_probe);

  if (self->priv->stats_enabled) {
    GST_INFO_OBJECT (self, "Enabling average stat for %" GST_PTR_FORMAT, pad);
    kms_stats_probe_add_latency (s_probe, kms_element_calculate_stats, FALSE,
        stream_input_avg_stat_ref (sstat),
        (GDestroyNotify) kms_ref_struct_unref);
  }

  KMS_ELEMENT_UNLOCK (self);
}

GstPad *
kms_element_connect_sink_target_full (KmsElement * self, GstPad * target,
    KmsElementPadType type, const gchar * description, KmsAddPadFunc func,
    gpointer user_data)
{
  GstPad *pad;
  gchar *pad_name;
  GstPadTemplate *templ;

  templ = gst_static_pad_template_get (&sink_factory);

  pad_name = get_sink_pad_name (type, description);

  pad = gst_ghost_pad_new_from_template (pad_name, target, templ);
  g_object_unref (templ);

  if (!pad) {
    goto end;
  }

  if (type == KMS_ELEMENT_PAD_TYPE_VIDEO) {
    kms_utils_drop_until_keyframe (pad, TRUE);
    kms_utils_pad_monitor_gaps (pad);
  }

  gst_pad_set_query_function (pad, kms_element_pad_query);
  gst_pad_add_probe (pad,
      GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM | GST_PAD_PROBE_TYPE_EVENT_FLUSH,
      accept_eos_probe, self, NULL);
  g_signal_connect (G_OBJECT (pad), "unlinked",
      G_CALLBACK (send_flush_on_unlink), NULL);

  if (func != NULL) {
    func (pad, user_data);
  }

  if (GST_STATE (self) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (self) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (self) >= GST_STATE_PAUSED) {
    gst_pad_set_active (pad, TRUE);
  }

  if (!gst_element_add_pad (GST_ELEMENT (self), pad)) {
    g_object_unref (pad);
    pad = NULL;
    goto end;
  }

  KMS_ELEMENT_LOCK (self);
  g_hash_table_remove (self->priv->pendingpads, pad_name);
  KMS_ELEMENT_UNLOCK (self);

  kms_element_set_sink_input_stats (self, pad, type);

end:

  g_free (pad_name);

  //add probe for media flow in signal
  if ((type == KMS_ELEMENT_PAD_TYPE_VIDEO)
      || (type == KMS_ELEMENT_PAD_TYPE_AUDIO)) {
    KmsMediaFlowTimeoutData *fdto_data;

    fdto_data =
        media_flow_timeout_data_new (self,
        KMS_FORMAT_PAD_DESCRIPTION (description), type, KMS_MEDIA_FLOW_IN);
    add_flow_event_probes (pad, fdto_data);
    media_flow_timeout_data_unref (fdto_data);
  }

  return pad;
}

GstPad *
kms_element_connect_sink_target_full_by_media_type (KmsElement * self,
    GstPad * target, KmsMediaType media_type, const gchar * description,
    KmsAddPadFunc func, gpointer user_data)
{
  KmsElementPadType pad_type;

  switch (media_type) {
    case KMS_MEDIA_TYPE_AUDIO:
      pad_type = KMS_ELEMENT_PAD_TYPE_AUDIO;
      break;
    case KMS_MEDIA_TYPE_VIDEO:
      pad_type = KMS_ELEMENT_PAD_TYPE_VIDEO;
      break;
    case KMS_MEDIA_TYPE_DATA:
      pad_type = KMS_ELEMENT_PAD_TYPE_DATA;
      break;
    default:
      GST_ERROR_OBJECT (self,
          "Invalid media type while requesting output element");
      return NULL;
  }

  return kms_element_connect_sink_target_full (self, target, pad_type,
      description, func, user_data);
}

static gint
find_stat_probe (KmsStatsProbe * probe, GstPad * pad)
{
  return (kms_stats_probe_watches (probe, pad)) ? 0 : 1;
}

void
kms_element_remove_sink (KmsElement * self, GstPad * pad)
{
  GSList *l;

  g_return_if_fail (self);
  g_return_if_fail (pad);

  KMS_ELEMENT_LOCK (self);

  l = g_slist_find_custom (self->priv->stats.probes, pad,
      (GCompareFunc) find_stat_probe);

  if (l != NULL) {
    KmsStatsProbe *probe = l->data;

    self->priv->stats.probes = g_slist_remove (self->priv->stats.probes,
        l->data);
    kms_stats_probe_destroy (probe);
  }

  KMS_ELEMENT_UNLOCK (self);

  // TODO: Unlink correctly pad before removing it
  gst_ghost_pad_set_target (GST_GHOST_PAD (pad), NULL);
  gst_element_remove_pad (GST_ELEMENT (self), pad);
}

void
kms_element_remove_sink_by_type_full (KmsElement * self,
    KmsElementPadType type, const gchar * description)
{
  gchar *pad_name = get_sink_pad_name (type, description);
  GstPad *pad = gst_element_get_static_pad (GST_ELEMENT (self), pad_name);

  if (pad == NULL) {
    GST_WARNING_OBJECT (self, "Cannot get pad %s", pad_name);
    goto end;
  }

  kms_element_remove_sink (self, pad);
  g_object_unref (pad);

end:
  g_free (pad_name);
}

static void
kms_element_release_pad (GstElement * element, GstPad * pad)
{
  GstPad *target;
  GstPad *peer;

  target = gst_ghost_pad_get_target (GST_GHOST_PAD (pad));

  if (target != NULL) {
    GstElement *output;

    output = gst_pad_get_parent_element (target);

    if (output != NULL) {
      gst_element_release_request_pad (output, target);
      g_object_unref (output);
    }
    g_object_unref (target);
  }

  peer = gst_pad_get_peer (pad);

  gst_pad_push_event (pad, gst_event_new_flush_start ());

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED) {
    gst_pad_set_active (pad, FALSE);
  }

  if (peer) {
    gst_pad_send_event (peer, gst_event_new_flush_stop (FALSE));
    g_object_unref (peer);
  }

  gst_element_remove_pad (element, pad);
}

static void
kms_element_endpoint_set_caps (KmsElement * self, const GstCaps * caps,
    GstCaps ** out)
{
  GstCaps *old;

  GST_DEBUG_OBJECT (self, "setting caps to %" GST_PTR_FORMAT, caps);

  old = *out;

  if (caps != NULL) {
    *out = gst_caps_copy (caps);
  } else {
    *out = NULL;
  }

  if (old != NULL) {
    gst_caps_unref (old);
  }
}

static GstCaps *
kms_element_endpoint_get_caps (KmsElement * self, GstCaps * caps)
{
  GstCaps *c;

  if ((c = caps) != NULL) {
    gst_caps_ref (caps);
  }

  GST_DEBUG_OBJECT (self, "caps: %" GST_PTR_FORMAT, c);

  return c;
}

static void
set_target_encoder_bitrate (gchar * id, KmsOutputElementData * odata, KmsElement * self)
{
  if (odata->type == KMS_ELEMENT_PAD_TYPE_VIDEO) {
    if (odata->element != NULL) {
      KMS_SET_OBJECT_PROPERTY_SAFELY (odata->element, TARGET_ENCODER_BITRATE,
          self->priv->target_encoder_bitrate);
    }
  }
}

static void
set_min_encoder_bitrate (gchar * id, KmsOutputElementData * odata, KmsElement * self)
{
  if (odata->type == KMS_ELEMENT_PAD_TYPE_VIDEO) {
    if (odata->element != NULL) {
      KMS_SET_OBJECT_PROPERTY_SAFELY (odata->element, MIN_ENCODER_BITRATE,
          self->priv->min_encoder_bitrate);
    }
  }
}

static void
set_max_encoder_bitrate (gchar * id, KmsOutputElementData * odata, KmsElement * self)
{
  if (odata->type == KMS_ELEMENT_PAD_TYPE_VIDEO) {
    if (odata->element != NULL) {
      KMS_SET_OBJECT_PROPERTY_SAFELY (odata->element, MAX_ENCODER_BITRATE,
          self->priv->max_encoder_bitrate);
    }
  }
}

static void
set_codec_config (gchar * id, KmsOutputElementData * odata, KmsElement * self)
{
  if (odata->type == KMS_ELEMENT_PAD_TYPE_AUDIO ||
      odata->type == KMS_ELEMENT_PAD_TYPE_VIDEO) {
    KMS_SET_OBJECT_PROPERTY_SAFELY (odata->element, CODEC_CONFIG,
        self->priv->codec_config);
  }
}

static void
kms_element_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsElement *self = KMS_ELEMENT (object);

  switch (property_id) {
    case PROP_ACCEPT_EOS:
      g_atomic_int_set (&self->priv->accept_eos, g_value_get_boolean (value));
      break;
    case PROP_AUDIO_CAPS:
      kms_element_endpoint_set_caps (self, gst_value_get_caps (value),
          &self->priv->audio_caps);
      break;
    case PROP_VIDEO_CAPS:
      kms_element_endpoint_set_caps (self, gst_value_get_caps (value),
          &self->priv->video_caps);
      break;
    case PROP_TARGET_ENCODER_BITRATE: {
      gint v = g_value_get_int (value);
      KMS_ELEMENT_LOCK (self);
      if (v < self->priv->min_encoder_bitrate
          || v > self->priv->max_encoder_bitrate) {
        v = DEFAULT_TARGET_ENCODER_BITRATE;
        GST_WARNING_OBJECT (self,
            "Ignoring out of bounds requested target bitrate; setting default: %d",
            v);
      }
      self->priv->target_encoder_bitrate = v;
      g_hash_table_foreach (self->priv->output_elements,
          (GHFunc)set_target_encoder_bitrate, self);
      KMS_ELEMENT_UNLOCK (self);
      break;
    }
    case PROP_MIN_ENCODER_BITRATE: {
      gint v = g_value_get_int (value);
      KMS_ELEMENT_LOCK (self);
      if (v > self->priv->max_encoder_bitrate) {
        v = self->priv->max_encoder_bitrate;
        GST_WARNING_OBJECT (self,
            "Ignoring requested bitrate min > max; setting max: %d", v);
      }
      self->priv->min_encoder_bitrate = v;
      g_hash_table_foreach (self->priv->output_elements,
          (GHFunc)set_min_encoder_bitrate, self);
      KMS_ELEMENT_UNLOCK (self);
      break;
    }
    case PROP_MAX_ENCODER_BITRATE: {
      gint v = g_value_get_int (value);
      KMS_ELEMENT_LOCK (self);
      if (v < self->priv->min_encoder_bitrate) {
        v = self->priv->min_encoder_bitrate;
        GST_WARNING_OBJECT (self,
            "Ignoring requested bitrate max < min; setting min: %d", v);
      }
      self->priv->max_encoder_bitrate = v;
      g_hash_table_foreach (self->priv->output_elements,
          (GHFunc)set_max_encoder_bitrate, self);
      KMS_ELEMENT_UNLOCK (self);
      break;
    }
    case PROP_CODEC_CONFIG:{
      KMS_ELEMENT_LOCK (self);
      if (self->priv->codec_config) {
        gst_structure_free (self->priv->codec_config);
        self->priv->codec_config = NULL;
      }

      self->priv->codec_config = g_value_dup_boxed (value);

      g_hash_table_foreach (self->priv->output_elements,
          (GHFunc) set_codec_config, self);
      KMS_ELEMENT_UNLOCK (self);
      break;
    }
    case PROP_MEDIA_STATS:{
      gboolean enable = g_value_get_boolean (value);

      KMS_ELEMENT_LOCK (self);
      if (enable != self->priv->stats_enabled) {
        self->priv->stats_enabled = enable;
        KMS_ELEMENT_GET_CLASS (self)->collect_media_stats (self, enable);
      }
      KMS_ELEMENT_UNLOCK (self);
      break;
    }
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_element_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsElement *self = KMS_ELEMENT (object);

  switch (property_id) {
    case PROP_ACCEPT_EOS:
      g_value_set_boolean (value, g_atomic_int_get (&self->priv->accept_eos));
      break;
    case PROP_AUDIO_CAPS:
      g_value_take_boxed (value, kms_element_endpoint_get_caps (self,
              self->priv->audio_caps));
      break;
    case PROP_VIDEO_CAPS:
      g_value_take_boxed (value, kms_element_endpoint_get_caps (self,
              self->priv->video_caps));
      break;
    case PROP_TARGET_ENCODER_BITRATE:
      KMS_ELEMENT_LOCK (self);
      g_value_set_int (value, self->priv->target_encoder_bitrate);
      KMS_ELEMENT_UNLOCK (self);
      break;
    case PROP_MIN_ENCODER_BITRATE:
      KMS_ELEMENT_LOCK (self);
      g_value_set_int (value, self->priv->min_encoder_bitrate);
      KMS_ELEMENT_UNLOCK (self);
      break;
    case PROP_MAX_ENCODER_BITRATE:
      KMS_ELEMENT_LOCK (self);
      g_value_set_int (value, self->priv->max_encoder_bitrate);
      KMS_ELEMENT_UNLOCK (self);
      break;
    case PROP_MEDIA_STATS:
      KMS_ELEMENT_LOCK (self);
      g_value_set_boolean (value, self->priv->stats_enabled);
      KMS_ELEMENT_UNLOCK (self);
      break;
    case PROP_CODEC_CONFIG:
      KMS_ELEMENT_LOCK (self);
      g_value_set_boxed (value, self->priv->codec_config);
      KMS_ELEMENT_UNLOCK (self);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_element_destroy_stats (KmsElement * self)
{
  g_slist_free_full (self->priv->stats.probes,
      (GDestroyNotify) kms_stats_probe_destroy);
}

static void
kms_element_finalize (GObject * object)
{
  KmsElement *element = KMS_ELEMENT (object);

  GST_DEBUG_OBJECT (object, "finalize");

  kms_element_destroy_stats (element);

  /* free resources allocated by this object */
  g_hash_table_unref (element->priv->pendingpads);
  g_hash_table_unref (element->priv->output_elements);
  g_hash_table_unref (element->priv->stats.avg_iss);

  g_rec_mutex_clear (&element->mutex);

  if (element->priv->video_caps != NULL) {
    gst_caps_unref (element->priv->video_caps);
  }

  if (element->priv->audio_caps != NULL) {
    gst_caps_unref (element->priv->audio_caps);
  }

  if (element->priv->codec_config) {
    gst_structure_free (element->priv->codec_config);
    element->priv->codec_config = NULL;
  }

  /* chain up */
  G_OBJECT_CLASS (kms_element_parent_class)->finalize (object);
}

static gchar *
kms_element_request_new_srcpad (KmsElement * self,
    KmsElementPadType type, const gchar * description)
{
  const gchar *templ_name, *desc;
  KmsOutputElementData *odata;
  gchar *pad_name, *key;
  guint counter = 0;
  gboolean added = TRUE;

  desc = KMS_FORMAT_PAD_DESCRIPTION (description);

  KMS_ELEMENT_LOCK (self);

  templ_name = get_pad_template_from_pad_type (type);
  if (templ_name == NULL) {
    KMS_ELEMENT_UNLOCK (self);
    return NULL;
  }

  key = create_id_from_pad_attrs (type, GST_PAD_SRC, desc);
  odata = g_hash_table_lookup (self->priv->output_elements, key);

  if (odata == NULL) {
    GST_DEBUG_OBJECT (self, "New output element for track %s, stream %s",
        kms_element_pad_type_str (type), desc);
    odata = create_output_element_data (type);
    g_hash_table_insert (self->priv->output_elements, key, odata);
  } else {
    g_free (key);
  }

  counter = odata->pad_count++;
  pad_name = g_strdup_printf (templ_name, desc, counter);

  if (odata->element == NULL) {
    KmsRequestNewSrcElementReturn ret =
        KMS_ELEMENT_GET_CLASS (self)->request_new_src_element (self, type, desc,
        pad_name);

    if (ret == KMS_REQUEST_NEW_SRC_ELEMENT_NOT_SUPPORTED) {
      GST_WARNING_OBJECT (self, "source pad '%s' forbidden", pad_name);
      odata->pad_count--;
      g_free (pad_name);
      KMS_ELEMENT_UNLOCK (self);
      return NULL;
    }

    added = ret != KMS_REQUEST_NEW_SRC_ELEMENT_NOT_SUPPORTED;
  }

  if (odata->element == NULL) {
    if (!g_hash_table_contains (self->priv->pendingpads, pad_name)) {
      PendingPad *pdata;

      pdata = create_pending_pad (type, GST_PAD_SRC, desc);
      g_hash_table_insert (self->priv->pendingpads, g_strdup (pad_name), pdata);
    }

    KMS_ELEMENT_UNLOCK (self);
  } else {
    KMS_ELEMENT_UNLOCK (self);
    if (added) {
      kms_element_add_src_pad (self, odata->element, pad_name, templ_name);
    }
  }

  return pad_name;
}

static KmsRequestNewSrcElementReturn
kms_element_request_new_src_element_default (KmsElement * self,
    KmsElementPadType type, const gchar * description, const gchar * name)
{
  GST_DEBUG_OBJECT (self, "src pads on sometimes (by default)");

  return KMS_REQUEST_NEW_SRC_ELEMENT_LATER;
}

static gboolean
kms_element_request_new_sink_pad_default (KmsElement * self,
    KmsElementPadType type, const gchar * description, const gchar * name)
{
  GST_WARNING_OBJECT (self, "Request sink pads not allowed");

  return FALSE;
}

static gboolean
kms_element_release_requested_sink_pad_default (KmsElement * self, GstPad * pad)
{
  /* Not implmented by this class */

  return FALSE;
}

static gchar *
kms_element_request_new_sinkpad (KmsElement * self,
    KmsElementPadType type, const gchar * description)
{
  gchar *pad_name = NULL;
  const gchar *desc;
  GstPad *pad;

  desc = KMS_FORMAT_PAD_DESCRIPTION (description);

  KMS_ELEMENT_LOCK (self);

  pad_name = get_sink_pad_name (type, desc);

  pad = gst_element_get_static_pad (GST_ELEMENT (self), pad_name);
  if (pad != NULL) {
    /* Pad already created */
    g_object_unref (pad);
    goto end;
  }

  if (g_hash_table_contains (self->priv->pendingpads, pad_name)) {
    /* This sink pad was allowed but it is still pending */
    goto end;
  } else {
    PendingPad *pdata;

    pdata = create_pending_pad (type, GST_PAD_SINK, desc);
    g_hash_table_insert (self->priv->pendingpads, g_strdup (pad_name), pdata);
  }

  if (!KMS_ELEMENT_GET_CLASS (self)->request_new_sink_pad (self, type, desc,
          pad_name)) {
    /* Sink pad is not allowed */
    g_hash_table_remove (self->priv->pendingpads, pad_name);
    g_free (pad_name);
    pad_name = NULL;
  }

end:
  KMS_ELEMENT_UNLOCK (self);

  return pad_name;
}

static gchar *
kms_element_request_new_pad_action (KmsElement * self, KmsElementPadType type,
    const gchar * desc, GstPadDirection dir)
{
  if (dir == GST_PAD_SRC) {
    return kms_element_request_new_srcpad (self, type, desc);
  } else if (dir == GST_PAD_SINK) {
    return kms_element_request_new_sinkpad (self, type, desc);
  } else {
    GST_ERROR_OBJECT (self, "Pad direction must be known");
    return NULL;
  }
}

static void
kms_element_remove_target_pad (KmsElement * self, GstPad * pad)
{
  GstElement *agnosticbin;
  GstPad *target;

  target = gst_ghost_pad_get_target (GST_GHOST_PAD (pad));
  if (target == NULL) {
    return;
  }

  agnosticbin = gst_pad_get_parent_element (target);
  if (agnosticbin == NULL) {
    GST_WARNING_OBJECT (self, "No agnosticbin owns %" GST_PTR_FORMAT, pad);
  } else {
    gst_element_release_request_pad (agnosticbin, target);
    g_object_unref (agnosticbin);
  }

  gst_ghost_pad_set_target (GST_GHOST_PAD (pad), NULL);
  g_object_unref (target);
}

static gboolean
kms_element_release_requested_pad_action (KmsElement *self, GstPad *pad)
{
  gboolean released = FALSE;

  KMS_ELEMENT_LOCK (self);
  released = g_hash_table_remove (self->priv->pendingpads, GST_PAD_NAME (pad));
  KMS_ELEMENT_UNLOCK (self);

  if (released) {
    /* Pad was not created yet */
    return TRUE;
  }

  /* Pad is not in the pending list so it may have been already created */
  switch (gst_pad_get_direction (pad)) {
  case GST_PAD_SRC:
    kms_element_remove_target_pad (self, pad);
    kms_element_release_pad (GST_ELEMENT (self), pad);
    released = TRUE;
    break;
  case GST_PAD_SINK:
    /* Dynamic sink pads are managed by subclasses */
    released =
        KMS_ELEMENT_GET_CLASS (self)->release_requested_sink_pad (self, pad);
    break;
  default:
    GST_WARNING_OBJECT (self, "Unknown pad direction: %" GST_PTR_FORMAT, pad);
  }

  return released;
}

static GstStructure *
kms_element_get_input_latency_stats (KmsElement * self, gchar * selector)
{
  gpointer key, value;
  GHashTableIter iter;
  GstStructure *stats;

  stats = gst_structure_new_empty ("input-latencies");

  KMS_ELEMENT_LOCK (self);

  g_hash_table_iter_init (&iter, self->priv->stats.avg_iss);

  while (g_hash_table_iter_next (&iter, &key, &value)) {
    StreamInputAvgStat *avg = value;
    GstStructure *pad_latency;
    gchar *padname = key;

    if (selector != NULL && ((g_strcmp0 (selector, AUDIO_STREAM_NAME) == 0 &&
                avg->type != KMS_MEDIA_TYPE_AUDIO) ||
            (g_strcmp0 (selector, VIDEO_STREAM_NAME) == 0 &&
                avg->type != KMS_MEDIA_TYPE_VIDEO))) {
      continue;
    }

    /* Video and audio latencies are measured in nano seconds. They */
    /* are such an small values so there is no harm in casting them */
    /* to uint64 even we might lose a bit of preccision.            */

    pad_latency = gst_structure_new (padname, "type", G_TYPE_STRING,
        (avg->type ==
            KMS_MEDIA_TYPE_AUDIO) ? AUDIO_STREAM_NAME : VIDEO_STREAM_NAME,
        "avg", G_TYPE_UINT64, (guint64) avg->avg, NULL);

    gst_structure_set (stats, padname, GST_TYPE_STRUCTURE, pad_latency, NULL);
    gst_structure_free (pad_latency);
  }

  KMS_ELEMENT_UNLOCK (self);

  return stats;
}

static GstStructure *
kms_element_stats_impl (KmsElement * self, gchar * selector)
{
  GstStructure *stats;

  stats = gst_structure_new_empty ("stats");

  if (self->priv->stats_enabled) {
    GstStructure *e_stats;
    GstStructure *l_stats;

    l_stats = kms_element_get_input_latency_stats (self, selector);

    e_stats = gst_structure_new (KMS_ELEMENT_STATS_STRUCT_NAME,
        "input-latencies", GST_TYPE_STRUCTURE, l_stats, NULL);
    gst_structure_free (l_stats);

    gst_structure_set (stats, KMS_MEDIA_ELEMENT_FIELD, GST_TYPE_STRUCTURE,
        e_stats, NULL);

    gst_structure_free (e_stats);
  }

  return stats;
}

static GstPad *
kms_element_get_probed_pad (KmsStatsProbe * probe, KmsElement * self)
{
  GValue item = G_VALUE_INIT;
  gboolean done, found;
  GstIterator *it;
  GstPad *pad;

  done = found = FALSE;
  it = gst_element_iterate_sink_pads (GST_ELEMENT (self));

  while (!done && !found) {
    switch (gst_iterator_next (it, &item)) {
      case GST_ITERATOR_OK:
        pad = g_value_get_object (&item);

        found = kms_stats_probe_watches (probe, pad);
        if (found) {
          /* Hold the reference */
          g_object_ref (pad);
        }
        g_value_reset (&item);
        break;
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
      case GST_ITERATOR_DONE:
        done = TRUE;
        break;
    }
  }

  gst_iterator_free (it);

  if (found) {
    return pad;
  }

  return NULL;
}

static StreamInputAvgStat *
kms_element_get_stat_for_probe (KmsStatsProbe * probe, KmsElement * self)
{
  StreamInputAvgStat *sstat;
  gchar *padname;
  GstPad *pad;

  pad = kms_element_get_probed_pad (probe, self);

  if (pad == NULL) {
    return NULL;
  }

  padname = gst_pad_get_name (pad);
  g_object_unref (pad);

  sstat = g_hash_table_lookup (self->priv->stats.avg_iss, padname);
  g_free (padname);

  return sstat;
}

static void
kms_element_enable_media_stats (KmsStatsProbe * probe, KmsElement * self)
{
  StreamInputAvgStat *sstat;

  sstat = kms_element_get_stat_for_probe (probe, self);

  if (sstat != NULL) {
    kms_stats_probe_add_latency (probe, kms_element_calculate_stats,
        FALSE, stream_input_avg_stat_ref (sstat),
        (GDestroyNotify) kms_ref_struct_unref);
  }
}

static void
kms_element_disable_media_stats (KmsStatsProbe * probe, KmsElement * self)
{
  kms_stats_probe_remove (probe);
}

static void
kms_element_collect_media_stats_impl (KmsElement * self, gboolean enable)
{
  if (enable) {
    g_slist_foreach (self->priv->stats.probes,
        (GFunc) kms_element_enable_media_stats, self);
  } else {
    g_slist_foreach (self->priv->stats.probes,
        (GFunc) kms_element_disable_media_stats, self);
  }
}

static GstElement *
kms_element_create_output_element_default (KmsElement *self)
{
  gchar *name = gst_element_get_name (GST_ELEMENT (self));
  GstElement *element = kms_utils_element_factory_make ("agnosticbin", name);
  g_free (name);

  return element;
}

static void
kms_element_class_init (KmsElementClass * klass)
{
  GstElementClass *gstelement_class;
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->set_property = kms_element_set_property;
  gobject_class->get_property = kms_element_get_property;
  gobject_class->finalize = kms_element_finalize;

  gstelement_class = GST_ELEMENT_CLASS (klass);
  gst_element_class_set_details_simple (gstelement_class,
      "KmsElement",
      "Base/Bin/KmsElement",
      "Base class for elements",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>");

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&video_src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&data_src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&sink_factory));

  g_object_class_install_property (gobject_class, PROP_ACCEPT_EOS,
      g_param_spec_boolean ("accept-eos",
          "Accept EOS",
          "Indicates if the element should accept EOS events.",
          DEFAULT_ACCEPT_EOS, G_PARAM_READWRITE));

  g_object_class_install_property (gobject_class, PROP_AUDIO_CAPS,
      g_param_spec_boxed ("audio-caps", "Audio capabilities",
          "The allowed caps for audio", GST_TYPE_CAPS,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_VIDEO_CAPS,
      g_param_spec_boxed ("video-caps", "Video capabilities",
          "The allowed caps for video", GST_TYPE_CAPS,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

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

  g_object_class_install_property (gobject_class, PROP_MEDIA_STATS,
      g_param_spec_boolean ("media-stats", "Media stats",
          "Indicates wheter this element is collecting stats or not",
          FALSE, G_PARAM_READWRITE | G_PARAM_CONSTRUCT));

  g_object_class_install_property (gobject_class, PROP_CODEC_CONFIG,
      g_param_spec_boxed ("codec-config", "codec config",
          "Codec configuration", GST_TYPE_STRUCTURE, G_PARAM_READWRITE));

  klass->sink_query = GST_DEBUG_FUNCPTR (kms_element_sink_query_default);
  klass->collect_media_stats =
      GST_DEBUG_FUNCPTR (kms_element_collect_media_stats_impl);
  klass->create_output_element =
      GST_DEBUG_FUNCPTR (kms_element_create_output_element_default);
  klass->request_new_src_element =
      GST_DEBUG_FUNCPTR (kms_element_request_new_src_element_default);
  klass->request_new_sink_pad =
      GST_DEBUG_FUNCPTR (kms_element_request_new_sink_pad_default);
  klass->release_requested_sink_pad =
      GST_DEBUG_FUNCPTR (kms_element_release_requested_sink_pad_default);

  /* set actions */
  element_signals[REQUEST_NEW_SRCPAD] =
      g_signal_new ("request-new-pad",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsElementClass, request_new_pad), NULL, NULL,
      __kms_core_marshal_STRING__ENUM_STRING_UINT,
      G_TYPE_STRING, 3, KMS_TYPE_ELEMENT_PAD_TYPE, G_TYPE_STRING, G_TYPE_UINT);

  element_signals[RELEASE_REQUESTED_SRCPAD] =
      g_signal_new ("release-requested-pad",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsElementClass, release_requested_pad), NULL, NULL,
      __kms_core_marshal_BOOLEAN__STRING, G_TYPE_BOOLEAN, 1, GST_TYPE_PAD);

  element_signals[STATS] =
      g_signal_new ("stats", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST | G_SIGNAL_ACTION,
      G_STRUCT_OFFSET (KmsElementClass, stats),
      NULL, NULL, __kms_core_marshal_BOXED__STRING, GST_TYPE_STRUCTURE, 1,
      G_TYPE_STRING);

  element_signals[SIGNAL_FLOW_OUT_MEDIA] =
      g_signal_new ("flow-out-media",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsElementClass, flow_out_state),
      NULL, NULL, __kms_core_marshal_VOID__BOOLEAN_STRING_ENUM, G_TYPE_NONE,
      3, G_TYPE_BOOLEAN, G_TYPE_STRING, KMS_TYPE_ELEMENT_PAD_TYPE);

  element_signals[SIGNAL_FLOW_IN_MEDIA] =
      g_signal_new ("flow-in-media",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsElementClass, flow_in_state),
      NULL, NULL, __kms_core_marshal_VOID__BOOLEAN_STRING_ENUM, G_TYPE_NONE,
      3, G_TYPE_BOOLEAN, G_TYPE_STRING, KMS_TYPE_ELEMENT_PAD_TYPE);

  /* Signal "KmsElement::media-transcoding"
   * Arguments:
   * - self
   * - Is transcoding?
   * - GstBin (KmsAgnosticBin) name
   * - Media type (audio/video)
   */
  element_signals[SIGNAL_MEDIA_TRANSCODING] =
      g_signal_new ("media-transcoding",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsElementClass, media_transcoding),
      NULL, NULL, NULL, G_TYPE_NONE,
      3, G_TYPE_BOOLEAN, G_TYPE_STRING, KMS_TYPE_ELEMENT_PAD_TYPE);

  klass->request_new_pad =
      GST_DEBUG_FUNCPTR (kms_element_request_new_pad_action);
  klass->release_requested_pad =
      GST_DEBUG_FUNCPTR (kms_element_release_requested_pad_action);
  klass->stats = GST_DEBUG_FUNCPTR (kms_element_stats_impl);

  g_type_class_add_private (klass, sizeof (KmsElementPrivate));

  klass->loop = kms_loop_new ();
}

static void
kms_element_init (KmsElement * element)
{
  g_rec_mutex_init (&element->mutex);

  element->priv = KMS_ELEMENT_GET_PRIVATE (element);

  element->priv->accept_eos = DEFAULT_ACCEPT_EOS;

  element->priv->target_encoder_bitrate = DEFAULT_TARGET_ENCODER_BITRATE;
  element->priv->min_encoder_bitrate = DEFAULT_MIN_ENCODER_BITRATE;
  element->priv->max_encoder_bitrate = DEFAULT_MAX_ENCODER_BITRATE;

  element->priv->pendingpads = g_hash_table_new_full (g_str_hash, g_str_equal,
      g_free, (GDestroyNotify) destroy_pendingpads);
  element->priv->output_elements =
      g_hash_table_new_full (g_str_hash, g_str_equal, g_free,
      (GDestroyNotify) destroy_output_element_data);
  element->priv->stats.avg_iss = g_hash_table_new_full (g_str_hash, g_str_equal,
      g_free, (GDestroyNotify) kms_ref_struct_unref);
}

KmsElementPadType
kms_element_get_pad_type (KmsElement * self, GstPad * pad)
{
  KmsElementPadType type;
  GstPadTemplate *templ;

  g_return_val_if_fail (self != NULL, KMS_ELEMENT_PAD_TYPE_DATA);

  templ = gst_pad_get_pad_template (pad);

  if (templ ==
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS (G_OBJECT_GET_CLASS
              (self)), AUDIO_SRC_PAD) ||
      g_strstr_len (GST_OBJECT_NAME (pad), -1, "audio")) {
    type = KMS_ELEMENT_PAD_TYPE_AUDIO;
  } else if (templ ==
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS (G_OBJECT_GET_CLASS
              (self)), VIDEO_SRC_PAD) ||
      g_strstr_len (GST_OBJECT_NAME (pad), -1, "video")) {
    type = KMS_ELEMENT_PAD_TYPE_VIDEO;
  } else {
    type = KMS_ELEMENT_PAD_TYPE_DATA;
  }

  gst_object_unref (templ);

  return type;
}
