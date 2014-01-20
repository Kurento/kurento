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

#include "kmsbasemixer.h"
#include "kmsagnosticcaps.h"
#include "kms-marshal.h"
#include "kmsmixerendpoint.h"

#define PLUGIN_NAME "basemixer"

#define KMS_BASE_MIXER_LOCK(mixer) \
  (g_rec_mutex_lock (&(mixer)->priv->mutex))

#define KMS_BASE_MIXER_UNLOCK(mixer) \
  (g_rec_mutex_unlock (&(mixer)->priv->mutex))

GST_DEBUG_CATEGORY_STATIC (kms_base_mixer_debug_category);
#define GST_CAT_DEFAULT kms_base_mixer_debug_category

#define KMS_BASE_MIXER_GET_PRIVATE(obj) (       \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_BASE_MIXER,                        \
    KmsBaseMixerPrivate                         \
  )                                             \
)

#define AUDIO_SINK_PAD_NAME "audio_sink_%u"
#define VIDEO_SINK_PAD_NAME "video_sink_%u"
#define AUDIO_SRC_PAD_NAME "audio_src_%u"
#define VIDEO_SRC_PAD_NAME "video_src_%u"

static GstStaticPadTemplate audio_sink_factory =
GST_STATIC_PAD_TEMPLATE (AUDIO_SINK_PAD_NAME,
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (KMS_AGNOSTIC_AUDIO_CAPS)
    );

static GstStaticPadTemplate video_sink_factory =
GST_STATIC_PAD_TEMPLATE (VIDEO_SINK_PAD_NAME,
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (KMS_AGNOSTIC_VIDEO_CAPS)
    );

static GstStaticPadTemplate audio_src_factory =
GST_STATIC_PAD_TEMPLATE (AUDIO_SRC_PAD_NAME,
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (KMS_AGNOSTIC_AUDIO_CAPS)
    );

static GstStaticPadTemplate video_src_factory =
GST_STATIC_PAD_TEMPLATE (VIDEO_SRC_PAD_NAME,
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (KMS_AGNOSTIC_VIDEO_CAPS)
    );

enum
{
  SIGNAL_HANDLE_PORT,
  LAST_SIGNAL
};

static guint kms_base_mixer_signals[LAST_SIGNAL] = { 0 };

struct _KmsBaseMixerPrivate
{
  GHashTable *ports;
  GRecMutex mutex;
  gint port_count;
};

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsBaseMixer, kms_base_mixer,
    GST_TYPE_BIN,
    GST_DEBUG_CATEGORY_INIT (kms_base_mixer_debug_category, PLUGIN_NAME,
        0, "debug category for basemixer element"));

static void
release_gint (gpointer data)
{
  g_slice_free (gint, data);
}

static gint *
kms_base_mixer_generate_port_id (KmsBaseMixer * mixer)
{
  gint *id;

  KMS_BASE_MIXER_LOCK (mixer);
  id = g_slice_new (gint);
  *id = mixer->priv->port_count++;
  KMS_BASE_MIXER_UNLOCK (mixer);

  return id;
}

static gint
kms_base_mixer_handle_port (KmsBaseMixer * mixer, GstElement * mixer_end_point)
{
  gint *id;

  if (!KMS_IS_MIXER_END_POINT (mixer_end_point)) {
    GST_INFO_OBJECT (mixer, "Invalid MixerEndPoint: %" GST_PTR_FORMAT,
        mixer_end_point);
    return -1;
  }

  if (GST_OBJECT_PARENT (mixer) == NULL ||
      GST_OBJECT_PARENT (mixer) != GST_OBJECT_PARENT (mixer_end_point)) {
    GST_ERROR_OBJECT (mixer,
        "Mixer and MixerEndPoint do not have the same parent");
    return -1;
  }

  GST_DEBUG_OBJECT (mixer, "Handle handle port: %" GST_PTR_FORMAT,
      mixer_end_point);

  id = kms_base_mixer_generate_port_id (mixer);

  GST_DEBUG_OBJECT (mixer, "Adding new port %d", *id);

  g_hash_table_insert (mixer->priv->ports, id, g_object_ref (mixer_end_point));

  return *id;
}

static void
kms_base_mixer_dispose (GObject * object)
{
  KmsBaseMixer *self = KMS_BASE_MIXER (object);

  if (self->priv->ports != NULL) {
    g_hash_table_unref (self->priv->ports);
    self->priv->ports = NULL;
  }

  G_OBJECT_CLASS (kms_base_mixer_parent_class)->dispose (object);
}

static void
kms_base_mixer_finalize (GObject * object)
{
  KmsBaseMixer *self = KMS_BASE_MIXER (object);

  g_rec_mutex_clear (&self->priv->mutex);

  G_OBJECT_CLASS (kms_base_mixer_parent_class)->finalize (object);
}

static void
kms_base_mixer_class_init (KmsBaseMixerClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "BaseMixer", "Generic", "Kurento plugin for mixer connection",
      "Jose Antonio Santos Cadenas <santoscadenas@gmail.com>");

  klass->handle_port = GST_DEBUG_FUNCPTR (kms_base_mixer_handle_port);

  gobject_class->dispose = GST_DEBUG_FUNCPTR (kms_base_mixer_dispose);
  gobject_class->finalize = GST_DEBUG_FUNCPTR (kms_base_mixer_finalize);

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&video_src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_sink_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&video_sink_factory));

  /* Signals initialization */
  kms_base_mixer_signals[SIGNAL_HANDLE_PORT] =
      g_signal_new ("handle-port",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsBaseMixerClass, handle_port), NULL, NULL,
      __kms_marshal_INT__OBJECT, G_TYPE_INT, 1, GST_TYPE_ELEMENT);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsBaseMixerPrivate));
}

static void
kms_base_mixer_init (KmsBaseMixer * self)
{
  self->priv = KMS_BASE_MIXER_GET_PRIVATE (self);

  g_rec_mutex_init (&self->priv->mutex);

  self->priv->port_count = 0;
  self->priv->ports = g_hash_table_new_full (g_int_hash, g_int_equal,
      release_gint, g_object_unref);
}
