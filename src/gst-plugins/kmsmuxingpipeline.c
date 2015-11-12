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
#include <commons/kmsutils.h>
#include <commons/kmsagnosticcaps.h>

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

struct _KmsMuxingPipelinePrivate
{
  GstElement *videosrc;
  GstElement *audiosrc;
  GstElement *mux;
  GstElement *sink;
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

G_DEFINE_TYPE_WITH_CODE (KmsMuxingPipeline, kms_muxing_pipeline,
    KMS_TYPE_BASE_MEDIA_MUXER,
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

GstStateChangeReturn
kms_muxing_pipeline_set_state (KmsBaseMediaMuxer * obj, GstState state)
{
  KmsMuxingPipeline *self = KMS_MUXING_PIPELINE (obj);

  if (state == GST_STATE_NULL || state == GST_STATE_READY) {
    self->priv->lastAudioPts = 0;
    self->priv->lastVideoPts = 0;
  }

  return KMS_BASE_MEDIA_MUXER_CLASS (parent_class)->set_state (obj, state);
}

static void
kms_muxing_pipeline_class_init (KmsMuxingPipelineClass * klass)
{
  KmsBaseMediaMuxerClass *basemediamuxerclass;
  GObjectClass *objclass;

  objclass = G_OBJECT_CLASS (klass);
  objclass->set_property = kms_muxing_pipeline_set_property;
  objclass->get_property = kms_muxing_pipeline_get_property;

  basemediamuxerclass = KMS_BASE_MEDIA_MUXER_CLASS (klass);
  basemediamuxerclass->set_state = kms_muxing_pipeline_set_state;

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

  self->priv->lastVideoPts = G_GUINT64_CONSTANT (0);
  self->priv->lastAudioPts = G_GUINT64_CONSTANT (0);
}

static gboolean
kms_muxing_pipeline_check_pts (GstBuffer ** buffer, GstClockTime * lastPts)
{
  if (G_UNLIKELY (!GST_BUFFER_PTS_IS_VALID ((*buffer)))) {
    return TRUE;
  } else if (G_LIKELY (*lastPts <= (*buffer)->pts)) {
    *lastPts = (*buffer)->pts;

    return TRUE;
  } else {
    GST_WARNING ("Buffer pts %" G_GUINT64_FORMAT " is older than last pts %"
        G_GUINT64_FORMAT, GST_BUFFER_PTS (*buffer), *lastPts);

    return FALSE;
  }
}

static gboolean
kms_muxing_pipeline_injector (KmsMuxingPipeline * self, GstElement * elem,
    GstBuffer ** buffer)
{
  GstClockTime *lastPts = NULL;

  if (elem == self->priv->videosrc) {
    lastPts = &self->priv->lastVideoPts;
  } else if (elem == self->priv->audiosrc) {
    lastPts = &self->priv->lastAudioPts;
  }

  if (G_LIKELY (lastPts)) {
    gboolean ret;

    KMS_BASE_MEDIA_MUXER_LOCK (self);
    ret = kms_muxing_pipeline_check_pts (buffer, lastPts);
    KMS_BASE_MEDIA_MUXER_UNLOCK (self);

    return ret;
  }

  return FALSE;
}

static gboolean
kms_muxing_pipeline_injector_probe_it (GstBuffer ** buffer, guint idx,
    gpointer user_data)
{
  BufferListItData *data = user_data;

  return kms_muxing_pipeline_injector (data->self, data->elem, buffer);
}

static GstPadProbeReturn
kms_muxing_pipeline_injector_probe (GstPad * pad, GstPadProbeInfo * info,
    gpointer self)
{
  GstElement *elem;
  GstPadProbeReturn ret = GST_PAD_PROBE_OK;

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

    if (G_UNLIKELY (!gst_buffer_list_foreach (list,
                kms_muxing_pipeline_injector_probe_it, &itData))) {
      ret = GST_PAD_PROBE_DROP;
    }
  } else if (info->type & GST_PAD_PROBE_TYPE_BUFFER) {
    GstBuffer **buffer = (GstBuffer **) & info->data;

    if (G_UNLIKELY (!kms_muxing_pipeline_injector (self, elem, buffer))) {
      ret = GST_PAD_PROBE_DROP;
    }
  }

  g_object_unref (elem);

  return ret;
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

static GstElement *
kms_muxing_pipeline_create_muxer (KmsMuxingPipeline * self)
{
  switch (self->priv->profile) {
    case KMS_RECORDING_PROFILE_WEBM:
    case KMS_RECORDING_PROFILE_WEBM_VIDEO_ONLY:
    case KMS_RECORDING_PROFILE_WEBM_AUDIO_ONLY:
      return gst_element_factory_make ("webmmux", NULL);
    case KMS_RECORDING_PROFILE_MP4:
    case KMS_RECORDING_PROFILE_MP4_VIDEO_ONLY:
    case KMS_RECORDING_PROFILE_MP4_AUDIO_ONLY:
      return gst_element_factory_make ("qtmux", NULL);
    default:
      GST_ERROR_OBJECT (self, "No valid recording profile set");
      return NULL;
  }
}

static void
kms_muxing_pipeline_prepare_pipeline (KmsMuxingPipeline * self)
{
  self->priv->videosrc = gst_element_factory_make ("appsrc", "videoSrc");
  self->priv->audiosrc = gst_element_factory_make ("appsrc", "audioSrc");

  g_object_set (self->priv->videosrc, "format", 3 /* GST_FORMAT_TIME */ , NULL);
  g_object_set (self->priv->audiosrc, "format", 3 /* GST_FORMAT_TIME */ , NULL);

  kms_muxing_pipeline_add_injector_probe (self, self->priv->videosrc);
  kms_muxing_pipeline_add_injector_probe (self, self->priv->audiosrc);

  self->priv->mux = kms_muxing_pipeline_create_muxer (self);

  gst_bin_add_many (GST_BIN (KMS_BASE_MEDIA_MUXER_PIPELINE (self)),
      self->priv->videosrc, self->priv->audiosrc, self->priv->mux,
      self->priv->sink, NULL);

  if (!gst_element_link (self->priv->mux, self->priv->sink)) {
    GST_ERROR_OBJECT (self, "Could not link elements: %"
        GST_PTR_FORMAT ", %" GST_PTR_FORMAT, self->priv->mux, self->priv->sink);
  }

  if (kms_recording_profile_supports_type (self->priv->profile,
          KMS_ELEMENT_PAD_TYPE_VIDEO)) {
    if (!gst_element_link_pads (self->priv->videosrc, "src",
            self->priv->mux, "video_%u")) {
      GST_ERROR_OBJECT (self,
          "Could not link elements: %" GST_PTR_FORMAT ", %" GST_PTR_FORMAT,
          self->priv->videosrc, self->priv->mux);
    }
  }

  if (kms_recording_profile_supports_type (self->priv->profile,
          KMS_ELEMENT_PAD_TYPE_AUDIO)) {
    if (!gst_element_link_pads (self->priv->audiosrc, "src",
            self->priv->mux, "audio_%u")) {
      GST_ERROR_OBJECT (self,
          "Could not link elements: %" GST_PTR_FORMAT ", %" GST_PTR_FORMAT,
          self->priv->audiosrc, self->priv->mux);
    }
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
