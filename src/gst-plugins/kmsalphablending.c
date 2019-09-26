/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
#include "config.h"
#endif

#include "kmsalphablending.h"
#include <commons/kmsagnosticcaps.h>
#include <commons/kms-core-marshal.h>
#include <commons/kmshubport.h>
#include <commons/kmsloop.h>
#include <commons/kmsutils.h>
#include <commons/kmsrefstruct.h>

#define PLUGIN_NAME "alphablending"

#define KMS_ALPHA_BLENDING_LOCK(mixer) \
  (g_rec_mutex_lock (&( (KmsAlphaBlending *) (mixer))->priv->mutex))

#define KMS_ALPHA_BLENDING_UNLOCK(mixer) \
  (g_rec_mutex_unlock (&( (KmsAlphaBlending *) (mixer))->priv->mutex))

GST_DEBUG_CATEGORY_STATIC (kms_alpha_blending_debug_category);
#define GST_CAT_DEFAULT kms_alpha_blending_debug_category

#define KMS_ALPHA_BLENDING_GET_PRIVATE(obj) (\
  G_TYPE_INSTANCE_GET_PRIVATE (               \
    (obj),                                    \
    KMS_TYPE_ALPHA_BLENDING,                 \
    KmsAlphaBlendingPrivate                  \
  )                                           \
)

#define AUDIO_SINK_PAD_PREFIX_COMP "audio_sink_"
#define VIDEO_SINK_PAD_PREFIX_COMP "video_sink_"
#define AUDIO_SRC_PAD_PREFIX_COMP "audio_src_"
#define VIDEO_SRC_PAD_PREFIX_COMP "video_src_"
#define AUDIO_SINK_PAD_NAME_COMP AUDIO_SINK_PAD_PREFIX_COMP "%u"
#define VIDEO_SINK_PAD_NAME_COMP VIDEO_SINK_PAD_PREFIX_COMP "%u"
#define AUDIO_SRC_PAD_NAME_COMP AUDIO_SRC_PAD_PREFIX_COMP "%u"
#define VIDEO_SRC_PAD_NAME_COMP VIDEO_SRC_PAD_PREFIX_COMP "%u"

#define AUDIO_FAKESINK "audio_fakesink_%u"

static GstStaticPadTemplate audio_sink_factory =
GST_STATIC_PAD_TEMPLATE (AUDIO_SINK_PAD_NAME_COMP,
    GST_PAD_SINK,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (KMS_AGNOSTIC_RAW_AUDIO_CAPS)
    );

static GstStaticPadTemplate video_sink_factory =
GST_STATIC_PAD_TEMPLATE (VIDEO_SINK_PAD_NAME_COMP,
    GST_PAD_SINK,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (KMS_AGNOSTIC_RAW_VIDEO_CAPS)
    );

static GstStaticPadTemplate audio_src_factory =
GST_STATIC_PAD_TEMPLATE (AUDIO_SRC_PAD_NAME_COMP,
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (KMS_AGNOSTIC_RAW_AUDIO_CAPS)
    );

static GstStaticPadTemplate video_src_factory =
GST_STATIC_PAD_TEMPLATE (VIDEO_SRC_PAD_NAME_COMP,
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (KMS_AGNOSTIC_RAW_VIDEO_CAPS)
    );

enum
{
  PROP_0,
  PROP_SET_MASTER,
  N_PROPERTIES
};

enum
{
  SIGNAL_SET_PORT_PROPERTIES,
  LAST_SIGNAL
};

static guint kms_alpha_blending_signals[LAST_SIGNAL] = { 0 };

struct _KmsAlphaBlendingPrivate
{
  GstElement *videomixer;
  GstElement *audiomixer;
  GstElement *videotestsrc;
  GstElement *videotestsrc_capsfilter;
  GHashTable *ports;
  GstElement *mixer_audio_agnostic;
  GstElement *mixer_video_agnostic;
  KmsLoop *loop;
  GRecMutex mutex;
  gint n_elems;
  gint output_width, output_height;
  int master_port;
  int z_master;
};

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsAlphaBlending, kms_alpha_blending,
    KMS_TYPE_BASE_HUB,
    GST_DEBUG_CATEGORY_INIT (kms_alpha_blending_debug_category, PLUGIN_NAME,
        0, "debug category for alphablending element"));

typedef struct _KmsAlphaBlendingData
{
  KmsRefStruct parent;
  gint id;
  gint probe_id;
  gint link_probe_id;
  gboolean input;
  gboolean configured;
  gboolean removing;
  gboolean eos_managed;
  KmsAlphaBlending *mixer;
  GstElement *videoconvert;
  GstElement *capsfilter;
  GstElement *videoscale;
  GstElement *queue;
  GstElement *videorate;
  GstElement *videobox;
  GstPad *video_mixer_pad;
  GstPad *videoconvert_sink_pad;
  gfloat relative_x;
  gfloat relative_y;
  gfloat relative_width;
  gfloat relative_height;
  gint z_order;
} KmsAlphaBlendingData;

#define KMS_ALPHA_BLENDING_REF(data) \
  kms_ref_struct_ref (KMS_REF_STRUCT_CAST (data))
#define KMS_ALPHA_BLENDING_UNREF(data) \
  kms_ref_struct_unref (KMS_REF_STRUCT_CAST (data))

static void
kms_destroy_alpha_blending_data (KmsAlphaBlendingData * data)
{
  g_slice_free (KmsAlphaBlendingData, data);
}

static KmsAlphaBlendingData *
kms_create_alpha_blending_data ()
{
  KmsAlphaBlendingData *data;

  data = g_slice_new0 (KmsAlphaBlendingData);
  kms_ref_struct_init (KMS_REF_STRUCT_CAST (data),
      (GDestroyNotify) kms_destroy_alpha_blending_data);

  return data;
}

static gint
compare_port_data (KmsAlphaBlendingData * a, KmsAlphaBlendingData * b)
{
  return a->id - b->id;
}

static void
configure_port (KmsAlphaBlendingData * port_data)
{
  KmsAlphaBlending *mixer = port_data->mixer;
  GstCaps *filtercaps;

  if (port_data->configured) {
    gint _relative_x, _relative_y, _relative_width, _relative_height;

    _relative_x = port_data->relative_x * mixer->priv->output_width;
    _relative_y = port_data->relative_y * mixer->priv->output_height;
    _relative_width = port_data->relative_width * mixer->priv->output_width;
    _relative_height = port_data->relative_height * mixer->priv->output_height;

    filtercaps =
        gst_caps_new_simple ("video/x-raw", "format", G_TYPE_STRING, "AYUV",
        "width", G_TYPE_INT, _relative_width, "height",
        G_TYPE_INT, _relative_height, "framerate", GST_TYPE_FRACTION, 15, 1,
        NULL);

    if (port_data->videobox != NULL) {
      int left = 0;
      int right = 0;
      int top = 0;
      int bottom = 0;

      if (_relative_x > 0) {
        int width_size =
            (mixer->priv->output_width - (mixer->priv->output_width -
                _relative_x));
        if (_relative_width > width_size) {
          right = (_relative_width - width_size);
        }
      } else {
        left = -1 * _relative_x;
        if ((_relative_width - left) > mixer->priv->output_width) {
          right = (_relative_width - left) - mixer->priv->output_width;
        }
      }

      if (_relative_y > 0) {
        int height_size =
            (mixer->priv->output_height - (mixer->priv->output_height -
                _relative_y));
        if (_relative_height > height_size) {
          bottom = (_relative_height - height_size);
        }
      } else {
        top = -1 * _relative_y;
        if ((_relative_height - top) > mixer->priv->output_height) {
          bottom = (_relative_height - top) - mixer->priv->output_height;
        }
      }
      g_object_set (port_data->videobox, "right", right, "left", left, "top",
          top, "bottom", bottom, NULL);
    }

    if (port_data->video_mixer_pad != NULL) {
      int aux_x = _relative_x;
      int aux_y = _relative_y;

      if (aux_x < 0) {
        aux_x = 0;
      }

      if (aux_y < 0) {
        aux_y = 0;
      }

      g_object_set (port_data->video_mixer_pad, "xpos", aux_x, "ypos", aux_y,
          "zorder", port_data->z_order, "alpha", 1.0, NULL);
    }

  } else {
    filtercaps =
        gst_caps_new_simple ("video/x-raw", "format", G_TYPE_STRING, "AYUV",
        "width", G_TYPE_INT, mixer->priv->output_width, "height",
        G_TYPE_INT, mixer->priv->output_height,
        "framerate", GST_TYPE_FRACTION, 15, 1, NULL);
    if (port_data->video_mixer_pad != NULL) {
      g_object_set (port_data->video_mixer_pad, "xpos", 0, "ypos", 0, "alpha",
          1.0, "zorder", 1, NULL);
    }
  }

  if (port_data->capsfilter != NULL) {
    g_object_set (G_OBJECT (port_data->capsfilter), "caps", filtercaps, NULL);
  }

  gst_caps_unref (filtercaps);
}

static void
kms_alpha_blending_reconfigure_ports (KmsAlphaBlending * self)
{
  GstCaps *filtercaps;
  GList *l;
  GList *values = g_hash_table_get_values (self->priv->ports);

  values = g_list_sort (values, (GCompareFunc) compare_port_data);

  for (l = values; l != NULL; l = l->next) {
    KmsAlphaBlendingData *port_data = l->data;

    if (port_data->input == FALSE) {
      continue;
    }

    if (port_data->id == self->priv->master_port) {
      filtercaps =
          gst_caps_new_simple ("video/x-raw", "format", G_TYPE_STRING, "AYUV",
          "width", G_TYPE_INT, self->priv->output_width, "height",
          G_TYPE_INT, self->priv->output_height, NULL);

      if (port_data->capsfilter != NULL) {
        g_object_set (G_OBJECT (port_data->capsfilter), "caps", filtercaps,
            NULL);
      }

      if (port_data->video_mixer_pad != NULL) {
        g_object_set (port_data->video_mixer_pad, "xpos", 0, "ypos", 0, "alpha",
            1.0, "zorder", self->priv->z_master, NULL);
      }
    } else {
      configure_port (port_data);
    }
  }

  g_list_free (values);
  //reconfigure videotestsrc input
  filtercaps =
      gst_caps_new_simple ("video/x-raw", "format", G_TYPE_STRING, "AYUV",
      "width", G_TYPE_INT, self->priv->output_width, "height",
      G_TYPE_INT, self->priv->output_height, "framerate", GST_TYPE_FRACTION, 15,
      1, NULL);
  g_object_set (G_OBJECT (self->priv->videotestsrc_capsfilter), "caps",
      filtercaps, NULL);
  gst_caps_unref (filtercaps);
}

static void
kms_alpha_blending_set_master_port (KmsAlphaBlending * alpha_blending)
{
  GstPad *pad;
  KmsAlphaBlendingData *port_data;
  GstCaps *caps;
  gint width, height;
  const GstStructure *str;

  GST_DEBUG ("set master");

  //get the element with id == master_port
  port_data = g_hash_table_lookup (alpha_blending->priv->ports,
      GINT_TO_POINTER (alpha_blending->priv->master_port));

  if (port_data == NULL) {
    return;
  }
  //configure the output size to master size
  pad = gst_element_get_static_pad (port_data->videoconvert, "sink");
  caps = gst_pad_get_current_caps (pad);

  if (caps != NULL) {
    str = gst_caps_get_structure (caps, 0);
    if (gst_structure_get_int (str, "width", &width) &&
        gst_structure_get_int (str, "height", &height)) {
      port_data->mixer->priv->output_height = height;
      port_data->mixer->priv->output_width = width;
      kms_alpha_blending_reconfigure_ports (alpha_blending);
    }
    gst_caps_unref (caps);
  }
  g_object_unref (pad);
}

static void
kms_alpha_blending_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsAlphaBlending *self = KMS_ALPHA_BLENDING (object);

  KMS_ALPHA_BLENDING_LOCK (self);

  switch (property_id) {
    case PROP_SET_MASTER:
    {
      GstStructure *master;

      master = g_value_dup_boxed (value);
      gst_structure_get (master, "port", G_TYPE_INT,
          &self->priv->master_port, NULL);
      gst_structure_get (master, "z_order", G_TYPE_INT,
          &self->priv->z_master, NULL);

      kms_alpha_blending_set_master_port (self);
      gst_structure_free (master);
      break;
    }
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }

  KMS_ALPHA_BLENDING_UNLOCK (self);
}

static void
kms_alpha_blending_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsAlphaBlending *self = KMS_ALPHA_BLENDING (object);

  KMS_ALPHA_BLENDING_LOCK (self);

  switch (property_id) {
    case PROP_SET_MASTER:
    {
      GstStructure *data;

      data = gst_structure_new ("data",
          "master", G_TYPE_INT, self->priv->master_port,
          "z_order", G_TYPE_INT, self->priv->z_master, NULL);
      g_value_set_boxed (value, data);
      gst_structure_free (data);
      break;
    }
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }

  KMS_ALPHA_BLENDING_UNLOCK (self);
}

static gboolean
remove_elements_from_pipeline (KmsAlphaBlendingData * port_data)
{
  KmsAlphaBlending *self = port_data->mixer;
  GstElement *videoconvert, *videoscale, *videorate, *capsfilter, *queue,
      *videobox;

  KMS_ALPHA_BLENDING_LOCK (self);

  videobox = port_data->videobox;
  gst_element_unlink (videobox, self->priv->videomixer);

  if (port_data->video_mixer_pad != NULL) {
    gst_element_release_request_pad (self->priv->videomixer,
        port_data->video_mixer_pad);
    g_object_unref (port_data->video_mixer_pad);
    port_data->video_mixer_pad = NULL;
  }

  videoconvert = g_object_ref (port_data->videoconvert);
  videorate = g_object_ref (port_data->videorate);
  queue = g_object_ref (port_data->queue);
  videoscale = g_object_ref (port_data->videoscale);
  capsfilter = g_object_ref (port_data->capsfilter);
  g_object_ref (videobox);

  g_object_unref (port_data->videoconvert_sink_pad);

  port_data->videoconvert_sink_pad = NULL;
  port_data->videoconvert = NULL;
  port_data->videorate = NULL;
  port_data->queue = NULL;
  port_data->videoscale = NULL;
  port_data->capsfilter = NULL;
  port_data->videobox = NULL;

  gst_bin_remove_many (GST_BIN (self), videoconvert, videoscale, capsfilter,
      videorate, queue, videobox, NULL);

  kms_base_hub_unlink_video_src (KMS_BASE_HUB (self), port_data->id);

  KMS_ALPHA_BLENDING_UNLOCK (self);

  gst_element_set_state (videoconvert, GST_STATE_NULL);
  gst_element_set_state (videoscale, GST_STATE_NULL);
  gst_element_set_state (videorate, GST_STATE_NULL);
  gst_element_set_state (capsfilter, GST_STATE_NULL);
  gst_element_set_state (queue, GST_STATE_NULL);
  gst_element_set_state (videobox, GST_STATE_NULL);

  g_object_unref (videoconvert);
  g_object_unref (videoscale);
  g_object_unref (videorate);
  g_object_unref (capsfilter);
  g_object_unref (queue);
  g_object_unref (videobox);

  return G_SOURCE_REMOVE;
}

static GstPadProbeReturn
cb_EOS_received (GstPad * pad, GstPadProbeInfo * info,
    KmsAlphaBlendingData * port_data)
{
  KmsAlphaBlending *self = port_data->mixer;
  GstEvent *event;

  if (GST_EVENT_TYPE (GST_PAD_PROBE_INFO_EVENT (info)) != GST_EVENT_EOS) {
    return GST_PAD_PROBE_OK;
  }

  KMS_ALPHA_BLENDING_LOCK (self);

  if (!port_data->removing) {
    port_data->eos_managed = TRUE;
    KMS_ALPHA_BLENDING_UNLOCK (self);
    return GST_PAD_PROBE_OK;
  }

  if (port_data->probe_id > 0) {
    gst_pad_remove_probe (pad, port_data->probe_id);
    port_data->probe_id = 0;
  }

  KMS_ALPHA_BLENDING_UNLOCK (self);

  event = gst_event_new_eos ();
  gst_pad_send_event (pad, event);

  kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_DEFAULT,
      (GSourceFunc) remove_elements_from_pipeline,
      KMS_ALPHA_BLENDING_REF (port_data),
      (GDestroyNotify) kms_ref_struct_unref);

  return GST_PAD_PROBE_DROP;
}

static void
kms_alpha_blending_port_data_destroy (KmsAlphaBlendingData * port_data)
{
  KmsAlphaBlending *self = port_data->mixer;
  GstPad *audiosink;
  gchar *padname;

  KMS_ALPHA_BLENDING_LOCK (self);

  port_data->removing = TRUE;

  kms_base_hub_unlink_video_sink (KMS_BASE_HUB (self), port_data->id);
  kms_base_hub_unlink_audio_sink (KMS_BASE_HUB (self), port_data->id);

  if (port_data->input) {
    GstEvent *event;
    gboolean result;
    GstPad *pad;

    if (port_data->videorate == NULL) {
      KMS_ALPHA_BLENDING_UNLOCK (self);
      return;
    }

    pad = gst_element_get_static_pad (port_data->videorate, "sink");

    if (pad == NULL) {
      KMS_ALPHA_BLENDING_UNLOCK (self);
      return;
    }

    if (!GST_OBJECT_FLAG_IS_SET (pad, GST_PAD_FLAG_EOS)) {

      if (GST_PAD_IS_FLUSHING (pad)) {
        gst_pad_send_event (pad, gst_event_new_flush_stop (FALSE));
      }

      event = gst_event_new_eos ();
      result = gst_pad_send_event (pad, event);

      if (port_data->input && self->priv->n_elems > 0) {
        port_data->input = FALSE;
        self->priv->n_elems--;
      }

      if (!result) {
        GST_WARNING ("EOS event did not send");
      }

      gst_element_unlink (port_data->videoconvert, port_data->videorate);
      g_object_unref (pad);

      KMS_ALPHA_BLENDING_UNLOCK (self);
    } else {
      gboolean remove = FALSE;

      /* EOS callback was triggered before we could remove the port data */
      /* so we have to remove elements to avoid memory leaks. */
      remove = port_data->eos_managed;

      gst_element_unlink (port_data->videoconvert, port_data->videorate);
      g_object_unref (pad);

      KMS_ALPHA_BLENDING_UNLOCK (self);

      if (remove) {
        /* Remove pipeline without helding the mutex */
        kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_DEFAULT,
            (GSourceFunc) remove_elements_from_pipeline,
            KMS_ALPHA_BLENDING_REF (port_data),
            (GDestroyNotify) kms_ref_struct_unref);
      }
    }
  } else {
    GstElement *videoconvert;

    videoconvert = g_object_ref (port_data->videoconvert);
    port_data->videoconvert = NULL;

    if (port_data->probe_id > 0) {
      gst_pad_remove_probe (port_data->video_mixer_pad, port_data->probe_id);
    }

    if (port_data->link_probe_id > 0) {
      gst_pad_remove_probe (port_data->videoconvert_sink_pad,
          port_data->link_probe_id);
    }
    KMS_ALPHA_BLENDING_UNLOCK (self);

    gst_bin_remove (GST_BIN (self), videoconvert);
    gst_element_set_state (videoconvert, GST_STATE_NULL);
    g_object_unref (videoconvert);
  }

  padname = g_strdup_printf (AUDIO_SINK_PAD, port_data->id);
  audiosink = gst_element_get_static_pad (self->priv->audiomixer, padname);

  gst_element_release_request_pad (self->priv->audiomixer, audiosink);

  gst_object_unref (audiosink);
  g_free (padname);

  KMS_ALPHA_BLENDING_UNREF (port_data);
}

static GstPadProbeReturn
link_to_videomixer (GstPad * pad, GstPadProbeInfo * info,
    KmsAlphaBlendingData * data)
{
  GstPadTemplate *sink_pad_template;
  KmsAlphaBlending *mixer = data->mixer;

  if (GST_EVENT_TYPE (GST_PAD_PROBE_INFO_EVENT (info)) != GST_EVENT_CAPS) {
    return GST_PAD_PROBE_PASS;
  }

  GST_DEBUG ("stream start detected");

  KMS_ALPHA_BLENDING_LOCK (mixer);

  data->link_probe_id = 0;

  sink_pad_template =
      gst_element_class_get_pad_template (GST_ELEMENT_GET_CLASS (mixer->
          priv->videomixer), "sink_%u");

  if (G_UNLIKELY (sink_pad_template == NULL)) {
    GST_ERROR_OBJECT (mixer, "Error taking a new pad from videomixer");
    KMS_ALPHA_BLENDING_UNLOCK (mixer);
    return GST_PAD_PROBE_DROP;
  }

  if (mixer->priv->master_port == data->id) {
    //master_port, reconfigurate the output_width and heigth_width
    //and all the ports already created
    GstEvent *event;
    GstCaps *caps;
    gint width, height;
    const GstStructure *str;

    event = gst_pad_probe_info_get_event (info);
    gst_event_parse_caps (event, &caps);

    GST_DEBUG ("caps %" GST_PTR_FORMAT, caps);
    if (caps != NULL) {
      str = gst_caps_get_structure (caps, 0);
      if (gst_structure_get_int (str, "width", &width) &&
          gst_structure_get_int (str, "height", &height)) {
        mixer->priv->output_height = height;
        mixer->priv->output_width = width;
      }
    }
  }

  if (mixer->priv->videotestsrc == NULL) {
    GstCaps *filtercaps;
    GstPad *pad;

    mixer->priv->videotestsrc = gst_element_factory_make ("videotestsrc", NULL);
    mixer->priv->videotestsrc_capsfilter =
        gst_element_factory_make ("capsfilter", NULL);

    g_object_set (mixer->priv->videotestsrc, "is-live", TRUE, "pattern",
        /*black */ 2, NULL);

    filtercaps =
        gst_caps_new_simple ("video/x-raw", "format", G_TYPE_STRING, "AYUV",
        "width", G_TYPE_INT, mixer->priv->output_width,
        "height", G_TYPE_INT, mixer->priv->output_height,
        "framerate", GST_TYPE_FRACTION, 15, 1, NULL);
    g_object_set (G_OBJECT (mixer->priv->videotestsrc_capsfilter), "caps",
        filtercaps, NULL);
    gst_caps_unref (filtercaps);

    gst_bin_add_many (GST_BIN (mixer), mixer->priv->videotestsrc,
        mixer->priv->videotestsrc_capsfilter, NULL);

    gst_element_link (mixer->priv->videotestsrc,
        mixer->priv->videotestsrc_capsfilter);

    /*link capsfilter -> videomixer */
    pad = gst_element_request_pad (mixer->priv->videomixer,
        sink_pad_template, NULL, NULL);
    gst_element_link_pads (mixer->priv->videotestsrc_capsfilter, NULL,
        mixer->priv->videomixer, GST_OBJECT_NAME (pad));
    g_object_set (pad, "xpos", 0, "ypos", 0, "alpha", 0.0, "zorder", 0, NULL);
    g_object_unref (pad);

    gst_element_sync_state_with_parent (mixer->priv->videotestsrc_capsfilter);
    gst_element_sync_state_with_parent (mixer->priv->videotestsrc);
  }

  data->videoscale = gst_element_factory_make ("videoscale", NULL);
  data->capsfilter = gst_element_factory_make ("capsfilter", NULL);
  data->videorate = gst_element_factory_make ("videorate", NULL);
  data->queue = gst_element_factory_make ("queue", NULL);
  data->videobox = gst_element_factory_make ("videobox", NULL);
  data->input = TRUE;

  gst_bin_add_many (GST_BIN (mixer), data->queue, data->videorate,
      data->videoscale, data->capsfilter, data->videobox, NULL);

  g_object_set (data->videorate, "average-period", 200 * GST_MSECOND, NULL);
  g_object_set (data->queue, "flush-on-eos", TRUE, NULL);

  gst_element_link_many (data->videorate, data->queue, data->videoscale,
      data->capsfilter, data->videobox, NULL);

  /*link capsfilter -> videomixer */
  data->video_mixer_pad =
      gst_element_request_pad (mixer->priv->videomixer,
      sink_pad_template, NULL, NULL);

  gst_element_link_pads (data->videobox, NULL,
      mixer->priv->videomixer, GST_OBJECT_NAME (data->video_mixer_pad));

  gst_element_link (data->videoconvert, data->videorate);

  data->probe_id = gst_pad_add_probe (data->video_mixer_pad,
      GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM,
      (GstPadProbeCallback) cb_EOS_received,
      KMS_ALPHA_BLENDING_REF (data), (GDestroyNotify) kms_ref_struct_unref);

  gst_element_sync_state_with_parent (data->videoscale);
  gst_element_sync_state_with_parent (data->capsfilter);
  gst_element_sync_state_with_parent (data->videorate);
  gst_element_sync_state_with_parent (data->queue);
  gst_element_sync_state_with_parent (data->videobox);

  /* configure videomixer pad */
  mixer->priv->n_elems++;

  if (mixer->priv->master_port == data->id) {
    kms_alpha_blending_reconfigure_ports (mixer);
  } else {
    configure_port (data);
  }

  KMS_ALPHA_BLENDING_UNLOCK (mixer);

  return GST_PAD_PROBE_REMOVE;
}

static void
kms_alpha_blending_unhandle_port (KmsBaseHub * mixer, gint id)
{
  KmsAlphaBlending *self = KMS_ALPHA_BLENDING (mixer);

  GST_DEBUG ("unhandle id %d", id);

  KMS_ALPHA_BLENDING_LOCK (self);

  g_hash_table_remove (self->priv->ports, GINT_TO_POINTER (id));

  KMS_ALPHA_BLENDING_UNLOCK (self);

  KMS_BASE_HUB_CLASS (G_OBJECT_CLASS
      (kms_alpha_blending_parent_class))->unhandle_port (mixer, id);

  GST_DEBUG ("end unhandle port id %d", id);
}

static KmsAlphaBlendingData *
kms_alpha_blending_port_data_create (KmsAlphaBlending * mixer, gint id)
{
  KmsAlphaBlendingData *data;
  gchar *padname;

  data = kms_create_alpha_blending_data ();
  data->id = id;
  data->mixer = mixer;
  data->videoconvert = gst_element_factory_make ("videoconvert", NULL);

  gst_bin_add_many (GST_BIN (mixer), data->videoconvert, NULL);

  gst_element_sync_state_with_parent (data->videoconvert);

  /*link basemixer -> video_agnostic */
  kms_base_hub_link_video_sink (KMS_BASE_HUB (mixer), id, data->videoconvert,
      "sink", FALSE);

  padname = g_strdup_printf (AUDIO_SINK_PAD, id);
  kms_base_hub_link_audio_sink (KMS_BASE_HUB (mixer), id,
      mixer->priv->audiomixer, padname, FALSE);
  g_free (padname);

  data->videoconvert_sink_pad = gst_element_get_static_pad (data->videoconvert,
      "sink");

  data->link_probe_id = gst_pad_add_probe (data->videoconvert_sink_pad,
      GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM | GST_PAD_PROBE_TYPE_BLOCK,
      (GstPadProbeCallback) link_to_videomixer,
      KMS_ALPHA_BLENDING_REF (data), (GDestroyNotify) kms_ref_struct_unref);

  return data;
}

static gint
get_stream_id_from_padname (const gchar * name)
{
  gint64 id;

  if (name == NULL)
    return -1;

  if (!g_str_has_prefix (name, AUDIO_SRC_PAD_PREFIX))
    return -1;

  id = g_ascii_strtoll (name + LENGTH_AUDIO_SRC_PAD_PREFIX, NULL, 10);
  if (id > G_MAXINT)
    return -1;

  return id;
}

static void
pad_added_cb (GstElement * element, GstPad * pad, gpointer data)
{
  gint id;
  KmsAlphaBlending *self = KMS_ALPHA_BLENDING (data);

  if (gst_pad_get_direction (pad) != GST_PAD_SRC)
    return;

  id = get_stream_id_from_padname (GST_OBJECT_NAME (pad));

  if (id < 0) {
    GST_ERROR_OBJECT (self, "Invalid HubPort for %" GST_PTR_FORMAT, pad);
    return;
  }

  kms_base_hub_link_audio_src (KMS_BASE_HUB (self), id,
      self->priv->audiomixer, GST_OBJECT_NAME (pad), TRUE);
}

static void
pad_removed_cb (GstElement * element, GstPad * pad, gpointer data)
{
  GST_DEBUG ("Removed pad %" GST_PTR_FORMAT, pad);
}

static gint
kms_alpha_blending_handle_port (KmsBaseHub * mixer,
    GstElement * mixer_end_point)
{
  KmsAlphaBlending *self = KMS_ALPHA_BLENDING (mixer);
  KmsAlphaBlendingData *port_data;
  gint port_id;

  port_id = KMS_BASE_HUB_CLASS (G_OBJECT_CLASS
      (kms_alpha_blending_parent_class))->handle_port (mixer, mixer_end_point);

  if (port_id < 0) {
    return port_id;
  }

  KMS_ALPHA_BLENDING_LOCK (self);

  if (self->priv->videomixer == NULL) {
    GstElement *videorate_mixer;

    videorate_mixer = gst_element_factory_make ("videorate", NULL);
    self->priv->videomixer = gst_element_factory_make ("compositor", NULL);
    g_object_set (G_OBJECT (self->priv->videomixer), "background", 1, NULL);
    self->priv->mixer_video_agnostic =
        gst_element_factory_make ("agnosticbin", NULL);

    gst_bin_add_many (GST_BIN (mixer), self->priv->videomixer, videorate_mixer,
        self->priv->mixer_video_agnostic, NULL);

    gst_element_sync_state_with_parent (self->priv->videomixer);
    gst_element_sync_state_with_parent (videorate_mixer);
    gst_element_sync_state_with_parent (self->priv->mixer_video_agnostic);

    gst_element_link_many (self->priv->videomixer, videorate_mixer,
        self->priv->mixer_video_agnostic, NULL);
  }

  if (self->priv->audiomixer == NULL) {
    self->priv->audiomixer = gst_element_factory_make ("kmsaudiomixer", NULL);

    gst_bin_add (GST_BIN (mixer), self->priv->audiomixer);

    gst_element_sync_state_with_parent (self->priv->audiomixer);
    g_signal_connect (self->priv->audiomixer, "pad-added",
        G_CALLBACK (pad_added_cb), self);
    g_signal_connect (self->priv->audiomixer, "pad-removed",
        G_CALLBACK (pad_removed_cb), self);
  }

  kms_base_hub_link_video_src (KMS_BASE_HUB (self), port_id,
      self->priv->mixer_video_agnostic, "src_%u", TRUE);

  port_data = kms_alpha_blending_port_data_create (self, port_id);

  g_hash_table_insert (self->priv->ports, GINT_TO_POINTER (port_id), port_data);

  KMS_ALPHA_BLENDING_UNLOCK (self);

  return port_id;
}

static void
kms_alpha_blending_set_port_properties (KmsAlphaBlending * self,
    GstStructure * properties)
{
  gint port, z_order;
  gfloat relative_x, relative_y, relative_width, relative_height;
  KmsAlphaBlendingData *port_data;
  gboolean fields_ok = TRUE;

  GST_DEBUG ("setting port properties");

  fields_ok = fields_ok
      && gst_structure_get (properties, "relative_x", G_TYPE_FLOAT, &relative_x,
      NULL);
  fields_ok = fields_ok
      && gst_structure_get (properties, "relative_y", G_TYPE_FLOAT, &relative_y,
      NULL);
  fields_ok = fields_ok
      && gst_structure_get (properties, "relative_width", G_TYPE_FLOAT,
      &relative_width, NULL);
  fields_ok = fields_ok
      && gst_structure_get (properties, "relative_height", G_TYPE_FLOAT,
      &relative_height, NULL);
  fields_ok = fields_ok
      && gst_structure_get (properties, "port", G_TYPE_INT, &port, NULL);
  fields_ok = fields_ok
      && gst_structure_get (properties, "z_order", G_TYPE_INT, &z_order, NULL);

  if (!fields_ok) {
    GST_WARNING_OBJECT (self, "Invalid properties structure received");
    return;
  }

  KMS_ALPHA_BLENDING_LOCK (self);

  port_data = g_hash_table_lookup (self->priv->ports, GINT_TO_POINTER (port));

  if (port_data == NULL) {
    KMS_ALPHA_BLENDING_UNLOCK (self);
    return;
  }

  port_data->relative_x = relative_x;
  port_data->relative_y = relative_y;
  port_data->relative_width = relative_width;
  port_data->relative_height = relative_height;
  port_data->z_order = z_order;
  port_data->configured = TRUE;

  configure_port (port_data);

  KMS_ALPHA_BLENDING_UNLOCK (self);
}

static void
kms_alpha_blending_dispose (GObject * object)
{
  KmsAlphaBlending *self = KMS_ALPHA_BLENDING (object);

  KMS_ALPHA_BLENDING_LOCK (self);
  g_hash_table_remove_all (self->priv->ports);
  KMS_ALPHA_BLENDING_UNLOCK (self);
  g_clear_object (&self->priv->loop);

  G_OBJECT_CLASS (kms_alpha_blending_parent_class)->dispose (object);
}

static void
kms_alpha_blending_finalize (GObject * object)
{
  KmsAlphaBlending *self = KMS_ALPHA_BLENDING (object);

  g_rec_mutex_clear (&self->priv->mutex);

  if (self->priv->ports != NULL) {
    g_hash_table_unref (self->priv->ports);
    self->priv->ports = NULL;
  }

  G_OBJECT_CLASS (kms_alpha_blending_parent_class)->finalize (object);
}

static void
kms_alpha_blending_class_init (KmsAlphaBlendingClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  KmsBaseHubClass *base_hub_class = KMS_BASE_HUB_CLASS (klass);
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "AlphaBlending", "Generic", "Mixer element using the alpha channel "
      " in one output flow", "David Fernandez <d.fernandezlop@gmail.com>");

  klass->set_port_properties = GST_DEBUG_FUNCPTR
      (kms_alpha_blending_set_port_properties);

  gobject_class->set_property = kms_alpha_blending_set_property;
  gobject_class->get_property = kms_alpha_blending_get_property;
  gobject_class->dispose = GST_DEBUG_FUNCPTR (kms_alpha_blending_dispose);
  gobject_class->finalize = GST_DEBUG_FUNCPTR (kms_alpha_blending_finalize);

  base_hub_class->handle_port =
      GST_DEBUG_FUNCPTR (kms_alpha_blending_handle_port);
  base_hub_class->unhandle_port =
      GST_DEBUG_FUNCPTR (kms_alpha_blending_unhandle_port);

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&video_src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_sink_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&video_sink_factory));

  g_object_class_install_property (gobject_class, PROP_SET_MASTER,
      g_param_spec_boxed ("set-master", "set master",
          "Set the master port",
          GST_TYPE_STRUCTURE, (GParamFlags) G_PARAM_READWRITE));

  /* Signals initialization */
  kms_alpha_blending_signals[SIGNAL_SET_PORT_PROPERTIES] =
      g_signal_new ("set-port-properties",
      G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_ACTION | G_SIGNAL_RUN_LAST,
      G_STRUCT_OFFSET (KmsAlphaBlendingClass, set_port_properties), NULL, NULL,
      __kms_core_marshal_VOID__BOXED, G_TYPE_NONE, 1, GST_TYPE_STRUCTURE);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsAlphaBlendingPrivate));
}

static void
kms_alpha_blending_init (KmsAlphaBlending * self)
{
  self->priv = KMS_ALPHA_BLENDING_GET_PRIVATE (self);

  g_rec_mutex_init (&self->priv->mutex);

  self->priv->ports = g_hash_table_new_full (g_direct_hash, g_direct_equal,
      NULL, (GDestroyNotify) kms_alpha_blending_port_data_destroy);
  self->priv->n_elems = 0;
  self->priv->master_port = 0;
  self->priv->z_master = 5;
  self->priv->output_height = 480;
  self->priv->output_width = 640;

  self->priv->loop = kms_loop_new ();
}

gboolean
kms_alpha_blending_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_ALPHA_BLENDING);
}
