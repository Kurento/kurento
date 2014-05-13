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

#include "kmscompositemixer.h"
#include "kmsagnosticcaps.h"
#include "kms-marshal.h"
#include "kmsmixerport.h"
#include "kmsloop.h"
#include "kmsaudiomixer.h"

#define N_ELEMENTS_WIDTH 2

#define PLUGIN_NAME "compositemixer"

#define KMS_COMPOSITE_MIXER_LOCK(mixer) \
  (g_rec_mutex_lock (&( (KmsCompositeMixer *) mixer)->priv->mutex))

#define KMS_COMPOSITE_MIXER_UNLOCK(mixer) \
  (g_rec_mutex_unlock (&( (KmsCompositeMixer *) mixer)->priv->mutex))

GST_DEBUG_CATEGORY_STATIC (kms_composite_mixer_debug_category);
#define GST_CAT_DEFAULT kms_composite_mixer_debug_category

#define KMS_COMPOSITE_MIXER_GET_PRIVATE(obj) (\
  G_TYPE_INSTANCE_GET_PRIVATE (               \
    (obj),                                    \
    KMS_TYPE_COMPOSITE_MIXER,                 \
    KmsCompositeMixerPrivate                  \
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

struct _KmsCompositeMixerPrivate
{
  GstElement *videomixer;
  GstElement *audiomixer;
  GstElement *videotestsrc;
  GHashTable *ports;
  GstElement *mixer_audio_agnostic;
  GstElement *mixer_video_agnostic;
  KmsLoop *loop;
  GRecMutex mutex;
  gint n_elems;
  gint output_width, output_height;
  GMutex mutex_recalculate;
  GCond cond_recalculate;
  gboolean recalculating;
};

typedef struct _KmsCompositeMixerPortData KmsCompositeMixerPortData;

struct _KmsCompositeMixerPortData
{
  KmsCompositeMixer *mixer;
  gint id;
  GstElement *videoconvert;
  GstElement *capsfilter;
  GstElement *videoscale;
  GstElement *videorate;
  GstElement *queue;
  GstPad *video_mixer_pad, *videoconvert_sink_pad;
  gboolean input;
  gint probe_id, link_probe_id;
};

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsCompositeMixer, kms_composite_mixer,
    KMS_TYPE_BASE_HUB,
    GST_DEBUG_CATEGORY_INIT (kms_composite_mixer_debug_category, PLUGIN_NAME,
        0, "debug category for compositemixer element"));

static void
kms_composite_mixer_recalculate_sizes (gpointer data)
{
  KmsCompositeMixer *self = KMS_COMPOSITE_MIXER (data);
  GstCaps *filtercaps;
  gint width, height, top, left, counter;
  GHashTableIter iter;
  gpointer key, value;

  counter = 0;

  g_hash_table_iter_init (&iter, self->priv->ports);
  while (g_hash_table_iter_next (&iter, &key, &value)) {
    KmsCompositeMixerPortData *port_data = (KmsCompositeMixerPortData *) value;

    if (port_data->input == FALSE) {
      continue;
    }

    if (self->priv->n_elems == 1) {
      width = self->priv->output_width;
    } else {
      width = self->priv->output_width / N_ELEMENTS_WIDTH;
    }

    if (self->priv->n_elems < N_ELEMENTS_WIDTH) {
      height = self->priv->output_height;
    } else {
      height =
          self->priv->output_height / ((self->priv->n_elems /
              N_ELEMENTS_WIDTH) + (self->priv->n_elems % N_ELEMENTS_WIDTH));
    }
    filtercaps =
        gst_caps_new_simple ("video/x-raw", "format", G_TYPE_STRING, "AYUV",
        "width", G_TYPE_INT, width, "height", G_TYPE_INT, height,
        "framerate", GST_TYPE_FRACTION, 15, 1, NULL);
    g_object_set (G_OBJECT (port_data->capsfilter), "caps", filtercaps, NULL);
    gst_caps_unref (filtercaps);

    top = ((counter / N_ELEMENTS_WIDTH) * height);
    left = ((counter % N_ELEMENTS_WIDTH) * width);

    g_object_set (port_data->video_mixer_pad, "xpos", left, "ypos", top,
        "alpha", 1.0, NULL);
    counter++;

    GST_DEBUG ("top %d left %d width %d height %d", top, left, width, height);
  }
}

static gboolean
remove_elements_from_pipeline (gpointer data)
{
  KmsCompositeMixerPortData *port_data = (KmsCompositeMixerPortData *) data;
  KmsCompositeMixer *self = port_data->mixer;

  KMS_COMPOSITE_MIXER_LOCK (self);

  gst_element_unlink (port_data->capsfilter,
      port_data->mixer->priv->videomixer);

  if (port_data->video_mixer_pad != NULL) {
    gst_element_release_request_pad (self->priv->videomixer,
        port_data->video_mixer_pad);
    g_object_unref (port_data->video_mixer_pad);
    port_data->video_mixer_pad = NULL;
  }

  g_object_ref (port_data->videoconvert);
  g_object_ref (port_data->videorate);
  g_object_ref (port_data->queue);
  g_object_ref (port_data->videoscale);
  g_object_ref (port_data->capsfilter);

  gst_bin_remove_many (GST_BIN (self),
      port_data->videoconvert, port_data->videoscale, port_data->capsfilter,
      port_data->videorate, port_data->queue, NULL);

  kms_base_hub_unlink_video_src (KMS_BASE_HUB (self), port_data->id);

  gst_element_set_state (port_data->videoconvert, GST_STATE_NULL);
  gst_element_set_state (port_data->videoscale, GST_STATE_NULL);
  gst_element_set_state (port_data->videorate, GST_STATE_NULL);
  gst_element_set_state (port_data->capsfilter, GST_STATE_NULL);
  gst_element_set_state (port_data->queue, GST_STATE_NULL);

  g_clear_object (&port_data->videoconvert_sink_pad);
  g_clear_object (&port_data->videoconvert);
  g_clear_object (&port_data->videoscale);
  g_clear_object (&port_data->videorate);
  g_clear_object (&port_data->capsfilter);
  g_clear_object (&port_data->queue);

  g_mutex_lock (&self->priv->mutex_recalculate);
  self->priv->recalculating = FALSE;
  g_cond_signal (&self->priv->cond_recalculate);
  g_mutex_unlock (&self->priv->mutex_recalculate);

  KMS_COMPOSITE_MIXER_UNLOCK (self);
  return G_SOURCE_REMOVE;
}

static void
destroy_port_data (gpointer data)
{
  g_slice_free (KmsCompositeMixerPortData, data);
}

static GstPadProbeReturn
cb_EOS_received (GstPad * pad, GstPadProbeInfo * info, gpointer data)
{
  GstEvent *event;

  if (GST_EVENT_TYPE (GST_PAD_PROBE_INFO_EVENT (info)) != GST_EVENT_EOS) {
    return GST_PAD_PROBE_PASS;
  }

  KmsCompositeMixerPortData *port_data = (KmsCompositeMixerPortData *) data;
  KmsCompositeMixer *self = port_data->mixer;

  KMS_COMPOSITE_MIXER_LOCK (self);

  if (port_data->probe_id > 0) {
    gst_pad_remove_probe (pad, port_data->probe_id);
    port_data->probe_id = 0;
  }
  event = gst_event_new_eos ();
  gst_pad_send_event (pad, event);

  KMS_COMPOSITE_MIXER_UNLOCK (self);

  kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_DEFAULT,
      remove_elements_from_pipeline, data, destroy_port_data);

  return GST_PAD_PROBE_OK;
}

static void
kms_composite_mixer_port_data_destroy (gpointer data)
{
  KmsCompositeMixerPortData *port_data = (KmsCompositeMixerPortData *) data;
  KmsCompositeMixer *self = port_data->mixer;
  GstPad *audiosink;
  gchar *padname;

  if (!KMS_IS_COMPOSITE_MIXER (self)) {
    destroy_port_data (port_data);
    return;
  }

  KMS_COMPOSITE_MIXER_LOCK (self);

  kms_base_hub_unlink_video_sink (KMS_BASE_HUB (self), port_data->id);
  kms_base_hub_unlink_audio_sink (KMS_BASE_HUB (self), port_data->id);

  padname = g_strdup_printf (AUDIO_SINK_PAD, port_data->id);
  audiosink = gst_element_get_static_pad (self->priv->audiomixer, padname);

  gst_element_release_request_pad (self->priv->audiomixer, audiosink);

  gst_object_unref (audiosink);
  g_free (padname);

  KMS_COMPOSITE_MIXER_UNLOCK (self);

  if (port_data->input) {
    GstEvent *event;
    gboolean result;
    GstPad *pad;

    pad = gst_element_get_static_pad (port_data->videorate, "sink");

    if (pad == NULL)
      return;

    if (!GST_OBJECT_FLAG_IS_SET (pad, GST_PAD_FLAG_EOS)) {

      event = gst_event_new_eos ();
      result = gst_pad_send_event (pad, event);

      if (port_data->input && self->priv->n_elems > 0) {
        port_data->input = FALSE;
        self->priv->n_elems--;
        kms_composite_mixer_recalculate_sizes (self);
      }

      if (!result) {
        GST_WARNING ("EOS event did not send");
      }
    } else {
      GST_WARNING ("EOS event already sent");
    }
    gst_element_unlink (port_data->videoconvert, port_data->videorate);

    g_mutex_lock (&self->priv->mutex_recalculate);
    while (self->priv->recalculating) {
      g_cond_wait (&self->priv->cond_recalculate,
          &self->priv->mutex_recalculate);
    }
    self->priv->recalculating = TRUE;
    g_mutex_unlock (&self->priv->mutex_recalculate);

    g_object_unref (pad);
  } else {
    if (port_data->probe_id > 0) {
      gst_pad_remove_probe (port_data->video_mixer_pad, port_data->probe_id);
    }
    if (port_data->link_probe_id > 0) {
      gst_pad_remove_probe (port_data->videoconvert_sink_pad,
          port_data->link_probe_id);
    }
    g_object_ref (port_data->videoconvert);
    gst_bin_remove (GST_BIN (self), port_data->videoconvert);
  }
}

static GstPadProbeReturn
link_to_videomixer (GstPad * pad, GstPadProbeInfo * info, gpointer user_data)
{
  GstPadTemplate *sink_pad_template;
  KmsCompositeMixerPortData *data = user_data;

  if (GST_EVENT_TYPE (GST_PAD_PROBE_INFO_EVENT (info)) !=
      GST_EVENT_STREAM_START) {
    return GST_PAD_PROBE_PASS;
  }

  GST_DEBUG ("stream start detected");
  KMS_COMPOSITE_MIXER_LOCK (data->mixer);

  data->link_probe_id = 0;
  sink_pad_template =
      gst_element_class_get_pad_template (GST_ELEMENT_GET_CLASS (data->mixer->
          priv->videomixer), "sink_%u");

  if (data->mixer->priv->videotestsrc == NULL) {
    GstElement *capsfilter;
    GstCaps *filtercaps;

    data->mixer->priv->videotestsrc =
        gst_element_factory_make ("videotestsrc", NULL);
    capsfilter = gst_element_factory_make ("capsfilter", NULL);

    g_object_set (data->mixer->priv->videotestsrc, "is-live", TRUE, "pattern",
        /*black */ 2, NULL);

    filtercaps =
        gst_caps_new_simple ("video/x-raw", "format", G_TYPE_STRING, "AYUV",
        "width", G_TYPE_INT, data->mixer->priv->output_width,
        "height", G_TYPE_INT, data->mixer->priv->output_height,
        "framerate", GST_TYPE_FRACTION, 15, 1, NULL);
    g_object_set (G_OBJECT (capsfilter), "caps", filtercaps, NULL);
    gst_caps_unref (filtercaps);

    gst_bin_add_many (GST_BIN (data->mixer), data->mixer->priv->videotestsrc,
        capsfilter, NULL);

    gst_element_link (data->mixer->priv->videotestsrc, capsfilter);
    gst_element_sync_state_with_parent (capsfilter);

    /*link capsfilter -> videomixer */
    if (sink_pad_template != NULL) {
      GstPad *pad = gst_element_request_pad (data->mixer->priv->videomixer,
          sink_pad_template, NULL, NULL);

      gst_element_link_pads (capsfilter, NULL,
          data->mixer->priv->videomixer, GST_OBJECT_NAME (pad));
      g_object_set (pad, "xpos", 0, "ypos", 0, "alpha", 0.0, NULL);

      g_object_unref (pad);
    }

    gst_element_sync_state_with_parent (data->mixer->priv->videotestsrc);
  }

  data->videoscale = gst_element_factory_make ("videoscale", NULL);
  data->capsfilter = gst_element_factory_make ("capsfilter", NULL);
  data->videorate = gst_element_factory_make ("videorate", NULL);
  data->queue = gst_element_factory_make ("queue", NULL);
  data->input = TRUE;

  gst_bin_add_many (GST_BIN (data->mixer), data->queue, data->videorate,
      data->videoscale, data->capsfilter, NULL);

  gst_element_sync_state_with_parent (data->videoscale);
  gst_element_sync_state_with_parent (data->capsfilter);
  gst_element_sync_state_with_parent (data->videorate);
  gst_element_sync_state_with_parent (data->queue);

  g_object_set (data->videorate, "average-period", 200 * GST_MSECOND, NULL);
  g_object_set (data->queue, "flush-on-eos", TRUE, NULL);

  gst_element_link_many (data->videorate, data->queue, data->videoscale,
      data->capsfilter, NULL);

  /*link capsfilter -> videomixer */
  if (sink_pad_template != NULL) {
    data->video_mixer_pad =
        gst_element_request_pad (data->mixer->priv->videomixer,
        sink_pad_template, NULL, NULL);
    gst_element_link_pads (data->capsfilter, NULL,
        data->mixer->priv->videomixer, GST_OBJECT_NAME (data->video_mixer_pad));
  } else {
    GST_ERROR ("Error taking a new pad from videomixer");
  }

  gst_element_link (data->videoconvert, data->videorate);

  data->probe_id = gst_pad_add_probe (data->video_mixer_pad,
      GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM,
      (GstPadProbeCallback) cb_EOS_received, data, NULL);

  /*recalculate the output sizes */
  data->mixer->priv->n_elems++;
  kms_composite_mixer_recalculate_sizes (data->mixer);

  KMS_COMPOSITE_MIXER_UNLOCK (data->mixer);

  return GST_PAD_PROBE_REMOVE;
}

static void
release_gint (gpointer data)
{
  g_slice_free (gint, data);
}

static gint *
create_gint (gint value)
{
  gint *p = g_slice_new (gint);

  *p = value;
  return p;
}

static void
kms_composite_mixer_unhandle_port (KmsBaseHub * mixer, gint id)
{
  KmsCompositeMixer *self = KMS_COMPOSITE_MIXER (mixer);

  GST_DEBUG ("unhandle id %d", id);

  KMS_COMPOSITE_MIXER_LOCK (self);

  g_hash_table_remove (self->priv->ports, &id);

  KMS_COMPOSITE_MIXER_UNLOCK (self);

  KMS_BASE_HUB_CLASS (G_OBJECT_CLASS
      (kms_composite_mixer_parent_class))->unhandle_port (mixer, id);
}

static KmsCompositeMixerPortData *
kms_composite_mixer_port_data_create (KmsCompositeMixer * mixer, gint id)
{
  KmsCompositeMixerPortData *data = g_slice_new0 (KmsCompositeMixerPortData);
  gchar *padname;

  data->mixer = mixer;
  data->videoconvert = gst_element_factory_make ("videoconvert", NULL);

  data->id = id;
  data->input = FALSE;

  gst_bin_add_many (GST_BIN (mixer), data->videoconvert, NULL);

  gst_element_sync_state_with_parent (data->videoconvert);

  /*link basemixer -> video_agnostic */
  kms_base_hub_link_video_sink (KMS_BASE_HUB (mixer), id,
      data->videoconvert, "sink", FALSE);

  padname = g_strdup_printf (AUDIO_SINK_PAD, id);
  kms_base_hub_link_audio_sink (KMS_BASE_HUB (mixer), id,
      mixer->priv->audiomixer, padname, FALSE);
  g_free (padname);

  data->videoconvert_sink_pad =
      gst_element_get_static_pad (data->videoconvert, "sink");

  data->link_probe_id = gst_pad_add_probe (data->videoconvert_sink_pad,
      GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM | GST_PAD_PROBE_TYPE_BLOCK,
      (GstPadProbeCallback) link_to_videomixer, data, NULL);

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
  KmsCompositeMixer *self = KMS_COMPOSITE_MIXER (data);

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
kms_composite_mixer_handle_port (KmsBaseHub * mixer,
    GstElement * mixer_end_point)
{
  KmsCompositeMixer *self = KMS_COMPOSITE_MIXER (mixer);
  KmsCompositeMixerPortData *port_data;
  gint port_id;

  port_id = KMS_BASE_HUB_CLASS (G_OBJECT_CLASS
      (kms_composite_mixer_parent_class))->handle_port (mixer, mixer_end_point);

  if (port_id < 0) {
    return port_id;
  }

  KMS_COMPOSITE_MIXER_LOCK (self);

  if (self->priv->videomixer == NULL) {
    GstElement *videorate_mixer;

    videorate_mixer = gst_element_factory_make ("videorate", NULL);
    self->priv->videomixer = gst_element_factory_make ("videomixer", NULL);
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
    self->priv->audiomixer = gst_element_factory_make ("audiomixer", NULL);

    gst_bin_add (GST_BIN (mixer), self->priv->audiomixer);

    gst_element_sync_state_with_parent (self->priv->audiomixer);
    g_signal_connect (self->priv->audiomixer, "pad-added",
        G_CALLBACK (pad_added_cb), self);
    g_signal_connect (self->priv->audiomixer, "pad-removed",
        G_CALLBACK (pad_removed_cb), self);
  }
  kms_base_hub_link_video_src (KMS_BASE_HUB (self), port_id,
      self->priv->mixer_video_agnostic, "src_%u", TRUE);

  port_data = kms_composite_mixer_port_data_create (self, port_id);
  g_hash_table_insert (self->priv->ports, create_gint (port_id), port_data);

  KMS_COMPOSITE_MIXER_UNLOCK (self);

  return port_id;
}

static void
kms_composite_mixer_dispose (GObject * object)
{
  KmsCompositeMixer *self = KMS_COMPOSITE_MIXER (object);

  KMS_COMPOSITE_MIXER_LOCK (self);
  g_hash_table_remove_all (self->priv->ports);
  KMS_COMPOSITE_MIXER_UNLOCK (self);
  g_clear_object (&self->priv->loop);

  G_OBJECT_CLASS (kms_composite_mixer_parent_class)->dispose (object);
}

static void
kms_composite_mixer_finalize (GObject * object)
{
  KmsCompositeMixer *self = KMS_COMPOSITE_MIXER (object);

  g_rec_mutex_clear (&self->priv->mutex);
  g_mutex_clear (&self->priv->mutex_recalculate);
  g_cond_clear (&self->priv->cond_recalculate);

  if (self->priv->ports != NULL) {
    g_hash_table_unref (self->priv->ports);
    self->priv->ports = NULL;
  }

  G_OBJECT_CLASS (kms_composite_mixer_parent_class)->finalize (object);
}

static void
kms_composite_mixer_class_init (KmsCompositeMixerClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  KmsBaseHubClass *base_hub_class = KMS_BASE_HUB_CLASS (klass);
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "CompositeMixer", "Generic", "Mixer element that composes n input flows"
      " in one output flow", "David Fernandez <d.fernandezlop@gmail.com>");

  gobject_class->dispose = GST_DEBUG_FUNCPTR (kms_composite_mixer_dispose);
  gobject_class->finalize = GST_DEBUG_FUNCPTR (kms_composite_mixer_finalize);

  base_hub_class->handle_port =
      GST_DEBUG_FUNCPTR (kms_composite_mixer_handle_port);
  base_hub_class->unhandle_port =
      GST_DEBUG_FUNCPTR (kms_composite_mixer_unhandle_port);

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&video_src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_sink_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&video_sink_factory));

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsCompositeMixerPrivate));
}

static void
kms_composite_mixer_init (KmsCompositeMixer * self)
{
  self->priv = KMS_COMPOSITE_MIXER_GET_PRIVATE (self);

  g_rec_mutex_init (&self->priv->mutex);

  self->priv->ports = g_hash_table_new_full (g_int_hash, g_int_equal,
      release_gint, kms_composite_mixer_port_data_destroy);
  //TODO:Obtain the dimensions of the bigger input stream
  self->priv->output_height = 600;
  self->priv->output_width = 800;
  self->priv->n_elems = 0;
  self->priv->recalculating = FALSE;

  g_mutex_init (&self->priv->mutex_recalculate);
  g_cond_init (&self->priv->cond_recalculate);

  self->priv->loop = kms_loop_new ();
}

gboolean
kms_composite_mixer_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_COMPOSITE_MIXER);
}
