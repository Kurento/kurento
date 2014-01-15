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

#include "kmsmixerendpoint.h"
#include "kmsagnosticcaps.h"

#define PLUGIN_NAME "mixerendpoint"

#define MIXER_AUDIO_SINK_PAD "mixer_audio_sink"
#define MIXER_VIDEO_SINK_PAD "mixer_video_sink"
#define MIXER_AUDIO_SRC_PAD "mixer_audio_src"
#define MIXER_VIDEO_SRC_PAD "mixer_video_src"

GST_DEBUG_CATEGORY_STATIC (kms_mixer_end_point_debug_category);
#define GST_CAT_DEFAULT kms_mixer_end_point_debug_category

#define KMS_MIXER_END_POINT_GET_PRIVATE(obj) (  \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_MIXER_END_POINT,                   \
    KmsMixerEndPointPrivate                     \
  )                                             \
)

struct _KmsMixerEndPointPrivate
{
  GstPad *audio_internal;
  GstPad *vieo_internal;
};

/* Pad templates */
static GstStaticPadTemplate mixer_audio_sink_factory =
GST_STATIC_PAD_TEMPLATE (MIXER_AUDIO_SINK_PAD,
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (KMS_AGNOSTIC_AUDIO_CAPS)
    );

static GstStaticPadTemplate mixer_video_sink_factory =
GST_STATIC_PAD_TEMPLATE (MIXER_VIDEO_SINK_PAD,
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (KMS_AGNOSTIC_VIDEO_CAPS)
    );

static GstStaticPadTemplate mixer_audio_src_factory =
GST_STATIC_PAD_TEMPLATE (MIXER_AUDIO_SRC_PAD,
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (KMS_AGNOSTIC_AUDIO_CAPS)
    );

static GstStaticPadTemplate mixer_video_src_factory =
GST_STATIC_PAD_TEMPLATE (MIXER_VIDEO_SRC_PAD,
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (KMS_AGNOSTIC_VIDEO_CAPS)
    );

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsMixerEndPoint, kms_mixer_end_point,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_mixer_end_point_debug_category, PLUGIN_NAME,
        0, "debug category for mixerendpoint element"));

static void
kms_mixer_end_point_dispose (GObject * object)
{
  G_OBJECT_CLASS (kms_mixer_end_point_parent_class)->dispose (object);
}

static void
kms_mixer_end_point_finalize (GObject * object)
{
  G_OBJECT_CLASS (kms_mixer_end_point_parent_class)->finalize (object);
}

static void
kms_mixer_end_point_class_init (KmsMixerEndPointClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "MixerEndPoint", "Generic", "Kurento plugin for mixer connection",
      "Jose Antonio Santos Cadenas <santoscadenas@gmail.com>");

  gobject_class->dispose = kms_mixer_end_point_dispose;
  gobject_class->finalize = kms_mixer_end_point_finalize;

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&mixer_audio_src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&mixer_video_src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&mixer_audio_sink_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&mixer_video_sink_factory));

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsMixerEndPointPrivate));
}

static void
kms_mixer_end_point_init (KmsMixerEndPoint * self)
{
  self->priv = KMS_MIXER_END_POINT_GET_PRIVATE (self);
}

gboolean
kms_mixer_end_point_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_MIXER_END_POINT);
}
