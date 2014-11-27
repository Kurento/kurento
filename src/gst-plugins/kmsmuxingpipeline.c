/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
#include <commons/kms-core-enumtypes.h>
#include <commons/kmsrecordingprofile.h>

#include "kmsmuxingpipeline.h"

#define OBJECT_NAME "muxingpipeline"
#define KMS_MUXING_PIPELINE_NAME OBJECT_NAME

#define parent_class kms_muxing_pipeline_parent_class

GST_DEBUG_CATEGORY_STATIC (kms_muxing_pipeline_debug_category);
#define GST_CAT_DEFAULT kms_muxing_pipeline_debug_category

#define KMS_MUXING_PIPELINE_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_MUXING_PIPELINE,                   \
    KmsMuxingPipelinePrivate                    \
  )                                             \
)

#define KMS_MUXING_PIPELINE_LOCK(elem) \
  (g_mutex_lock (&KMS_MUXING_PIPELINE_CAST ((elem))->priv->mutex))
#define KMS_MUXING_PIPELINE_UNLOCK(elem) \
  (g_mutex_unlock (&KMS_MUXING_PIPELINE_CAST ((elem))->priv->mutex))

struct _KmsMuxingPipelinePrivate
{
  GstElement *videosrc;
  GstElement *audiosrc;
  GstElement *encodebin;
  GstElement *sink;
  GstElement *pipeline;
  GstClockTime lastVideoPts;
  GstClockTime lastAudioPts;
  KmsRecordingProfile profile;
  GMutex mutex;
};

typedef struct _BufferListItData
{
  KmsMuxingPipeline *self;
  GstElement *elem;
} BufferListItData;

G_DEFINE_TYPE_WITH_CODE (KmsMuxingPipeline, kms_muxing_pipeline, G_TYPE_OBJECT,
    GST_DEBUG_CATEGORY_INIT (kms_muxing_pipeline_debug_category, OBJECT_NAME,
        0, "debug category for muxing pipeline object"));

/* Object properties */
enum
{
  PROP_0,
  PROP_VIDEO_APPSRC,
  PROP_AUDIO_APPSRC,
  PROP_SINK,
  PROP_PROFILE,
  N_PROPERTIES
};

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };

#define KMS_MUXING_PIPELINE_DEFAULT_RECORDING_PROFILE KMS_RECORDING_PROFILE_WEBM

static GstClockTime MAX_DELAY = GST_SECOND;

static void
kms_muxing_pipeline_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsMuxingPipeline *self = KMS_MUXING_PIPELINE (object);

  switch (property_id) {
    case PROP_SINK:
      self->priv->sink = gst_object_ref (g_value_get_object (value));
      break;
    case PROP_PROFILE:
      self->priv->profile = g_value_get_enum (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_muxing_pipeline_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsMuxingPipeline *self = KMS_MUXING_PIPELINE (object);

  switch (property_id) {
    case PROP_VIDEO_APPSRC:
      g_value_set_object (value, self->priv->videosrc);
      break;
    case PROP_AUDIO_APPSRC:
      g_value_set_object (value, self->priv->audiosrc);
      break;
    case PROP_SINK:
      g_value_set_object (value, self->priv->sink);
      break;
    case PROP_PROFILE:
      g_value_set_enum (value, self->priv->profile);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_muxing_pipeline_dispose (GObject * object)
{
  KmsMuxingPipeline *self = KMS_MUXING_PIPELINE (object);

  GST_DEBUG_OBJECT (self, "dispose");

  gst_element_set_state (self->priv->pipeline, GST_STATE_NULL);
  g_clear_object (&self->priv->pipeline);

  G_OBJECT_CLASS (parent_class)->finalize (object);
}

static void
kms_muxing_pipeline_finalize (GObject * object)
{
  KmsMuxingPipeline *self = KMS_MUXING_PIPELINE (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_mutex_clear (&self->priv->mutex);

  G_OBJECT_CLASS (parent_class)->dispose (object);
}

static void
kms_muxing_pipeline_class_init (KmsMuxingPipelineClass * klass)
{
  GObjectClass *objclass = G_OBJECT_CLASS (klass);

  objclass->finalize = kms_muxing_pipeline_finalize;
  objclass->dispose = kms_muxing_pipeline_dispose;
  objclass->set_property = kms_muxing_pipeline_set_property;
  objclass->get_property = kms_muxing_pipeline_get_property;

  /* Install properties */
  obj_properties[PROP_VIDEO_APPSRC] =
      g_param_spec_object (KMS_MUXING_PIPELINE_VIDEO_APPSRC,
      "Video appsrc element", "Video media input for the pipeline",
      GST_TYPE_ELEMENT, (G_PARAM_READABLE));

  obj_properties[PROP_AUDIO_APPSRC] =
      g_param_spec_object (KMS_MUXING_PIPELINE_AUDIO_APPSRC,
      "Audio appsrc element", "Audio media input for the pipeline",
      GST_TYPE_ELEMENT, (G_PARAM_READABLE));

  obj_properties[PROP_SINK] =
      g_param_spec_object (KMS_MUXING_PIPELINE_SINK, "Sink element",
      "Sink element", GST_TYPE_ELEMENT,
      (G_PARAM_CONSTRUCT_ONLY | G_PARAM_READWRITE));

  obj_properties[PROP_PROFILE] = g_param_spec_enum (KMS_MUXING_PIPELINE_PROFILE,
      "Recording profile",
      "The profile used to encapsulate media",
      KMS_TYPE_RECORDING_PROFILE, KMS_MUXING_PIPELINE_DEFAULT_RECORDING_PROFILE,
      (G_PARAM_CONSTRUCT_ONLY | G_PARAM_READWRITE));

  g_object_class_install_properties (objclass, N_PROPERTIES, obj_properties);

  g_type_class_add_private (klass, sizeof (KmsMuxingPipelinePrivate));
}

static void
kms_muxing_pipeline_init (KmsMuxingPipeline * self)
{
  self->priv = KMS_MUXING_PIPELINE_GET_PRIVATE (self);

  g_mutex_init (&self->priv->mutex);

  self->priv->lastVideoPts = G_GUINT64_CONSTANT (0);
  self->priv->lastAudioPts = G_GUINT64_CONSTANT (0);
}

static void
kms_muxing_pipeline_configure (KmsMuxingPipeline * self)
{
  GstEncodingContainerProfile *cprof;
  const GList *profiles, *l;
  GstElement *appsrc;

  cprof =
      kms_recording_profile_create_profile (self->priv->profile, TRUE, TRUE);

  profiles = gst_encoding_container_profile_get_profiles (cprof);

  for (l = profiles; l != NULL; l = l->next) {
    GstEncodingProfile *prof = l->data;
    GstCaps *caps;

    if (GST_IS_ENCODING_AUDIO_PROFILE (prof)) {
      appsrc = self->priv->audiosrc;
    } else if (GST_IS_ENCODING_VIDEO_PROFILE (prof)) {
      appsrc = self->priv->videosrc;
    } else
      continue;

    caps = gst_encoding_profile_get_input_caps (prof);

    g_object_set (G_OBJECT (appsrc), "is-live", TRUE, "do-timestamp", FALSE,
        "min-latency", G_GUINT64_CONSTANT (0), "max-latency",
        G_GUINT64_CONSTANT (0), "format", GST_FORMAT_TIME, "caps", caps, NULL);

    gst_caps_unref (caps);
  }

  g_object_set (G_OBJECT (self->priv->encodebin), "profile", cprof,
      "audio-jitter-tolerance", 100 * GST_MSECOND,
      "avoid-reencoding", TRUE, NULL);
  gst_encoding_profile_unref (cprof);

  if (self->priv->profile == KMS_RECORDING_PROFILE_MP4) {
    GstElement *mux =
        gst_bin_get_by_name (GST_BIN (self->priv->encodebin), "muxer");

    g_object_set (G_OBJECT (mux), "fragment-duration", 2000, "streamable", TRUE,
        NULL);

    g_object_unref (mux);
  } else if (self->priv->profile == KMS_RECORDING_PROFILE_WEBM) {
    GstElement *mux =
        gst_bin_get_by_name (GST_BIN (self->priv->encodebin), "muxer");

    g_object_set (G_OBJECT (mux), "streamable", TRUE, NULL);

    g_object_unref (mux);
  }
}

static gboolean
kms_muxing_pipeline_check_pts (GstBuffer ** buffer, GstClockTime * lastPts,
    GstClockTime otherPts)
{
  if (G_UNLIKELY (!GST_BUFFER_PTS_IS_VALID ((*buffer)))) {
    return FALSE;
  } else if (G_LIKELY (*lastPts <= (*buffer)->pts)) {
    *lastPts = (*buffer)->pts;

    if (otherPts < *lastPts) {
      return (*lastPts - otherPts) > MAX_DELAY;
    }

    return FALSE;
  } else {
    GST_WARNING ("Changing buffer pts from: %" G_GUINT64_FORMAT " to %"
        G_GUINT64_FORMAT, GST_BUFFER_PTS (*buffer), *lastPts);
    (*buffer) = gst_buffer_make_writable (*buffer);
    (*buffer)->pts = *lastPts;

    return FALSE;
  }
}

static void
kms_muxing_pipeline_injector (KmsMuxingPipeline * self, GstElement * elem,
    GstBuffer ** buffer)
{
  if (elem == self->priv->videosrc) {
    gboolean ret;

    KMS_MUXING_PIPELINE_LOCK (self);
    ret =
        kms_muxing_pipeline_check_pts (buffer, &self->priv->lastVideoPts,
        self->priv->lastAudioPts);
    KMS_MUXING_PIPELINE_UNLOCK (self);

    if (G_UNLIKELY (ret)) {
      // TODO: Inject data
      GST_DEBUG_OBJECT (self->priv->audiosrc,
          "Should inject audio %" G_GUINT64_FORMAT,
          (self->priv->lastVideoPts - self->priv->lastAudioPts));
    }
  } else if (elem == self->priv->audiosrc) {
    gboolean ret;

    KMS_MUXING_PIPELINE_LOCK (self);
    ret =
        kms_muxing_pipeline_check_pts (buffer, &self->priv->lastAudioPts,
        self->priv->lastVideoPts);
    KMS_MUXING_PIPELINE_UNLOCK (self);

    if (G_UNLIKELY (ret)) {
      // TODO: Inject data
      GST_DEBUG_OBJECT (self->priv->videosrc,
          "Should inject video %" G_GUINT64_FORMAT,
          (self->priv->lastAudioPts - self->priv->lastVideoPts));
    }
  }
}

static gboolean
kms_muxing_pipeline_injector_probe_it (GstBuffer ** buffer, guint idx,
    gpointer user_data)
{
  BufferListItData *data = user_data;

  kms_muxing_pipeline_injector (data->self, data->elem, buffer);

  return TRUE;
}

static GstPadProbeReturn
kms_muxing_pipeline_injector_probe (GstPad * pad, GstPadProbeInfo * info,
    gpointer self)
{
  GstElement *elem;

  if (info->type & GST_PAD_PROBE_TYPE_BLOCK) {
    return GST_PAD_PROBE_PASS;
  }

  elem = gst_pad_get_parent_element (pad);

  g_return_val_if_fail (elem != NULL, GST_PAD_PROBE_OK);

  if (info->type & GST_PAD_PROBE_TYPE_BUFFER_LIST) {
    GstBufferList *list = GST_PAD_PROBE_INFO_BUFFER_LIST (info);
    BufferListItData itData;

    itData.self = self;
    itData.elem = elem;

    gst_buffer_list_foreach (list, kms_muxing_pipeline_injector_probe_it,
        &itData);

  } else if (info->type & GST_PAD_PROBE_TYPE_BUFFER) {
    GstBuffer **buffer = (GstBuffer **) & info->data;

    kms_muxing_pipeline_injector (self, elem, buffer);
  }

  g_object_unref (elem);

  return GST_PAD_PROBE_OK;
}

static void
kms_muxing_pipeline_add_injector_probe (KmsMuxingPipeline * self,
    GstElement * appsrc)
{
  GstPad *src;

  src = gst_element_get_static_pad (appsrc, "src");

  g_return_if_fail (src != NULL);

  gst_pad_add_probe (src,
      GST_PAD_PROBE_TYPE_BUFFER | GST_PAD_PROBE_TYPE_BUFFER_LIST,
      kms_muxing_pipeline_injector_probe, self, NULL);

  g_object_unref (src);
}

static void
kms_muxing_pipeline_prepare_pipeline (KmsMuxingPipeline * self)
{
  self->priv->pipeline = gst_pipeline_new (KMS_MUXING_PIPELINE_NAME);
  self->priv->videosrc = gst_element_factory_make ("appsrc", NULL);
  self->priv->audiosrc = gst_element_factory_make ("appsrc", NULL);
  self->priv->encodebin = gst_element_factory_make ("encodebin", NULL);

  kms_muxing_pipeline_add_injector_probe (self, self->priv->videosrc);
  kms_muxing_pipeline_add_injector_probe (self, self->priv->audiosrc);

  kms_muxing_pipeline_configure (self);

  gst_bin_add_many (GST_BIN (self->priv->pipeline), self->priv->videosrc,
      self->priv->audiosrc, self->priv->encodebin, self->priv->sink, NULL);

  if (!gst_element_link (self->priv->encodebin, self->priv->sink)) {
    GST_ERROR_OBJECT (self, "Could not link elements: %"
        GST_PTR_FORMAT ", %" GST_PTR_FORMAT,
        self->priv->encodebin, self->priv->sink);
  }

  if (!gst_element_link_pads (self->priv->videosrc, "src",
          self->priv->encodebin, "video_%u")) {
    GST_ERROR_OBJECT (self, "Could not link elements: %"
        GST_PTR_FORMAT ", %" GST_PTR_FORMAT,
        self->priv->videosrc, self->priv->encodebin);
  }

  if (!gst_element_link_pads (self->priv->audiosrc, "src",
          self->priv->encodebin, "audio_%u")) {
    GST_ERROR_OBJECT (self, "Could not link elements: %"
        GST_PTR_FORMAT ", %" GST_PTR_FORMAT,
        self->priv->audiosrc, self->priv->encodebin);
  }
}

KmsMuxingPipeline *
kms_muxing_pipeline_new (const char *optname1, ...)
{
  KmsMuxingPipeline *obj;

  va_list ap;

  va_start (ap, optname1);
  obj = KMS_MUXING_PIPELINE (g_object_new_valist (KMS_TYPE_MUXING_PIPELINE,
          optname1, ap));
  va_end (ap);

  kms_muxing_pipeline_prepare_pipeline (obj);

  return obj;
}

GstStateChangeReturn
kms_muxing_pipeline_set_state (KmsMuxingPipeline * obj, GstState state)
{
  g_return_val_if_fail (obj != NULL, GST_STATE_CHANGE_FAILURE);

  if (state == GST_STATE_NULL || state == GST_STATE_READY) {
    obj->priv->lastAudioPts = 0;
    obj->priv->lastVideoPts = 0;
  }

  return gst_element_set_state (obj->priv->pipeline, state);
}

GstState
kms_muxing_pipeline_get_state (KmsMuxingPipeline * obj)
{
  return GST_STATE (obj->priv->pipeline);
}

GstClock *
kms_muxing_pipeline_get_clock (KmsMuxingPipeline * obj)
{
  g_return_val_if_fail (obj != NULL, GST_CLOCK_TIME_NONE);

  return GST_ELEMENT (obj->priv->pipeline)->clock;
}

GstBus *
kms_muxing_pipeline_get_bus (KmsMuxingPipeline * obj)
{
  g_return_val_if_fail (obj != NULL, NULL);

  return gst_pipeline_get_bus (GST_PIPELINE (obj->priv->pipeline));
}

void
kms_muxing_pipeline_dot_file (KmsMuxingPipeline * obj)
{
  g_return_if_fail (obj != NULL);

  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (GST_BIN (obj->priv->pipeline),
      GST_DEBUG_GRAPH_SHOW_ALL, GST_ELEMENT_NAME (obj->priv->pipeline));
}
