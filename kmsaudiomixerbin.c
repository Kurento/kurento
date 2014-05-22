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

#include "kmsaudiomixerbin.h"
#include "kmsloop.h"

#define PLUGIN_NAME "audiomixerbin"
#define KMS_AUDIO_MIXER_BIN_PROBE_ID_KEY "kms-audio-mixer-bin-probe-id"

#define KMS_AUDIO_MIXER_BIN_LOCK(mixer) \
  (g_rec_mutex_lock (&(mixer)->priv->mutex))

#define KMS_AUDIO_MIXER_BIN_UNLOCK(mixer) \
  (g_rec_mutex_unlock (&(mixer)->priv->mutex))

GST_DEBUG_CATEGORY_STATIC (kms_audio_mixer_bin_debug_category);
#define GST_CAT_DEFAULT kms_audio_mixer_bin_debug_category

#define KMS_AUDIO_MIXER_BIN_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (                \
    (obj),                                     \
    KMS_TYPE_AUDIO_MIXER_BIN,                  \
    KmsAudioMixerBinPrivate                    \
  )                                            \
)

struct _KmsAudioMixerBinPrivate
{
  GstElement *adder;
  GRecMutex mutex;
  KmsLoop *loop;
  GstPad *srcpad;
  guint count;
};

#define RAW_AUDIO_CAPS "audio/x-raw;"

/* the capabilities of the inputs and outputs. */
static GstStaticPadTemplate audio_sink_factory =
GST_STATIC_PAD_TEMPLATE (AUDIO_MIXER_BIN_SINK_PAD,
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (RAW_AUDIO_CAPS)
    );

static GstStaticPadTemplate audio_src_factory =
GST_STATIC_PAD_TEMPLATE (AUDIO_MIXER_BIN_SRC_PAD,
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    GST_STATIC_CAPS (RAW_AUDIO_CAPS)
    );

typedef struct _WaitCond WaitCond;
struct _WaitCond
{
  GCond cond;
  GMutex mutex;
  gboolean done;
};

#define OPERATION_DONE(wait) do {                  \
  g_mutex_lock (&(wait)->mutex);                   \
  (wait)->done = TRUE;                             \
  g_cond_signal (&(wait)->cond);                   \
  g_mutex_unlock (&(wait)->mutex);                 \
} while (0)

#define WAIT_UNTIL_DONE(wait) do {                 \
  g_mutex_lock (&(wait)->mutex);                   \
  while (!(wait)->done)                            \
      g_cond_wait (&(wait)->cond, &(wait)->mutex); \
  g_mutex_unlock (&(wait)->mutex);                 \
} while (0)

typedef struct _ProbeData ProbeData;
struct _ProbeData
{
  KmsAudioMixerBin *audiomixer;
  GstElement *typefind;
  GstElement *agnosticbin;
  WaitCond *cond;
};

typedef struct _RefCounter RefCounter;
struct _RefCounter
{
  guint count;
  gpointer data;
  GDestroyNotify notif;
  GMutex mutex;
};

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsAudioMixerBin, kms_audio_mixer_bin,
    GST_TYPE_BIN,
    GST_DEBUG_CATEGORY_INIT (kms_audio_mixer_bin_debug_category,
        PLUGIN_NAME, 0, "debug category for " PLUGIN_NAME " element"));

static void
destroy_gulong (gulong * n)
{
  g_slice_free (gulong, n);
}

static gulong *
create_gulong (gulong n)
{
  gulong *number;

  number = g_slice_new (gulong);
  *number = n;

  return number;
}

static RefCounter *
create_ref_counter (gpointer data, GDestroyNotify notif)
{
  RefCounter *counter;

  counter = g_slice_new (RefCounter);

  g_mutex_init (&counter->mutex);
  counter->notif = notif;
  counter->data = data;
  counter->count = 1;

  return counter;
}

static RefCounter *
ref_counter_inc (RefCounter * counter)
{
  g_mutex_lock (&counter->mutex);
  counter->count++;
  g_mutex_unlock (&counter->mutex);

  return counter;
}

static void
ref_counter_dec (RefCounter * counter)
{
  g_mutex_lock (&counter->mutex);
  counter->count--;
  if (counter->count > 0) {
    g_mutex_unlock (&counter->mutex);
    return;
  }

  g_mutex_unlock (&counter->mutex);
  g_mutex_clear (&counter->mutex);

  if (counter->notif)
    counter->notif (counter->data);

  g_slice_free (RefCounter, counter);
}

static void
destroy_probe_data (ProbeData * data)
{
  gst_object_unref (data->typefind);
  gst_object_unref (data->agnosticbin);
  gst_object_unref (data->audiomixer);

  OPERATION_DONE (data->cond);

  g_slice_free (ProbeData, data);
}

static ProbeData *
create_probe_data (KmsAudioMixerBin * audiomixer, GstElement * typefind,
    GstElement * agnosticbin, WaitCond * cond)
{
  ProbeData *data;

  data = g_slice_new (ProbeData);
  data->audiomixer = gst_object_ref (audiomixer);
  data->typefind = gst_object_ref (typefind);
  data->agnosticbin = gst_object_ref (agnosticbin);
  data->cond = cond;

  return data;
}

static void
destroy_wait_condition (WaitCond * cond)
{
  g_mutex_clear (&cond->mutex);
  g_cond_clear (&cond->cond);
  g_slice_free (WaitCond, cond);
}

static WaitCond *
create_wait_condition ()
{
  WaitCond *cond;

  cond = g_slice_new (WaitCond);
  cond->done = FALSE;
  g_mutex_init (&cond->mutex);
  g_cond_init (&cond->cond);

  return cond;
}

static void
kms_audio_mixer_bin_have_type (GstElement * typefind, guint arg0,
    GstCaps * caps, gpointer data)
{
  KmsAudioMixerBin *self = KMS_AUDIO_MIXER_BIN (data);
  GstElement *agnosticbin;

  GST_DEBUG ("Found type connecting elements");

  agnosticbin = gst_element_factory_make ("agnosticbin", NULL);

  gst_bin_add_many (GST_BIN (self), agnosticbin, NULL);
  gst_element_sync_state_with_parent (agnosticbin);

  gst_element_link_pads (typefind, "src", agnosticbin, "sink");
  gst_element_link_pads (agnosticbin, "src_0", self->priv->adder, "sink_%u");
}

static void
kms_audio_mixer_bin_unlink_elements (KmsAudioMixerBin * self,
    GstElement * typefind, GstElement * agnosticbin)
{
  GstPad *srcpad, *sinkpad;
  gulong *id;

  srcpad = gst_element_get_static_pad (agnosticbin, "src_0");
  id = g_object_get_data (G_OBJECT (srcpad), KMS_AUDIO_MIXER_BIN_PROBE_ID_KEY);

  gst_element_unlink_pads (typefind, "src", agnosticbin, "sink");

  sinkpad = gst_pad_get_peer (srcpad);
  if (sinkpad == NULL) {
    GST_ERROR_OBJECT (self, "Can not get peer pad linked to %" GST_PTR_FORMAT,
        srcpad);
    goto end;
  }

  GST_DEBUG ("Unlink %" GST_PTR_FORMAT " and %" GST_PTR_FORMAT,
      agnosticbin, self->priv->adder);

  if (!gst_pad_unlink (srcpad, sinkpad)) {
    GST_ERROR ("Can not unlink %" GST_PTR_FORMAT " and %" GST_PTR_FORMAT,
        srcpad, sinkpad);
  }

  gst_element_release_request_pad (self->priv->adder, sinkpad);
  gst_element_release_request_pad (agnosticbin, srcpad);

  gst_object_unref (sinkpad);

end:
  gst_pad_remove_probe (srcpad, *id);
  g_object_set_data_full (G_OBJECT (srcpad), KMS_AUDIO_MIXER_BIN_PROBE_ID_KEY,
      NULL, NULL);
  gst_object_unref (srcpad);
}

static void
kms_audio_mixer_bin_remove_elements (KmsAudioMixerBin * self,
    GstElement * typefind, GstElement * agnosticbin)
{
  gst_element_set_locked_state (typefind, TRUE);
  gst_element_set_locked_state (agnosticbin, TRUE);

  gst_element_set_state (typefind, GST_STATE_NULL);
  gst_element_set_state (agnosticbin, GST_STATE_NULL);

  gst_bin_remove_many (GST_BIN (self), typefind, agnosticbin, NULL);
}

static gboolean
remove_elements (RefCounter * refdata)
{
  ProbeData *data;

  data = (ProbeData *) refdata->data;

  kms_audio_mixer_bin_unlink_elements (data->audiomixer, data->typefind,
      data->agnosticbin);
  kms_audio_mixer_bin_remove_elements (data->audiomixer, data->typefind,
      data->agnosticbin);

  return G_SOURCE_REMOVE;
}

static GstPadProbeReturn
event_probe_cb (GstPad * pad, GstPadProbeInfo * info, gpointer user_data)
{
  RefCounter *refdata;
  ProbeData *data;
  gulong *id;

  if (GST_EVENT_TYPE (GST_PAD_PROBE_INFO_DATA (info)) != GST_EVENT_EOS)
    return GST_PAD_PROBE_OK;

  refdata = (RefCounter *) user_data;
  data = (ProbeData *) refdata->data;

  id = create_gulong (GST_PAD_PROBE_INFO_ID (info));
  g_object_set_data_full (G_OBJECT (pad), KMS_AUDIO_MIXER_BIN_PROBE_ID_KEY,
      id, (GDestroyNotify) destroy_gulong);

  /* We can not access to some GstPad functions because of mutex deadlocks */
  /* So we are going to manage all the stuff in a separate thread */
  kms_loop_idle_add_full (data->audiomixer->priv->loop, G_PRIORITY_DEFAULT,
      (GSourceFunc) remove_elements, ref_counter_inc (refdata),
      (GDestroyNotify) ref_counter_dec);

  return GST_PAD_PROBE_DROP;
}

static void
kms_audio_mixer_bin_unlink_pad_in_playing (KmsAudioMixerBin * self,
    GstPad * pad)
{
  GstPad *sinkpad, *peerpad, *srcpad, *probepad;
  GstElement *typefind, *agnosticbin;
  WaitCond *wait = NULL;
  RefCounter *refdata;
  ProbeData *data;

  sinkpad = gst_ghost_pad_get_target (GST_GHOST_PAD (pad));
  if (sinkpad == NULL) {
    GST_WARNING_OBJECT (self, "No element chain connected to %" GST_PTR_FORMAT,
        pad);
    return;
  }

  typefind = gst_pad_get_parent_element (sinkpad);

  if (typefind == NULL) {
    GST_ERROR_OBJECT (self, "No typefind owns %" GST_PTR_FORMAT, pad);
    goto end_phase1;
  }

  srcpad = gst_element_get_static_pad (typefind, "src");
  if (srcpad == NULL) {
    GST_ERROR_OBJECT (self, "No src pad got from %" GST_PTR_FORMAT, typefind);
    goto end_phase2;
  }

  peerpad = gst_pad_get_peer (srcpad);
  if (peerpad == NULL) {
    GST_ERROR_OBJECT (self, "No agnosticbin connected to %" GST_PTR_FORMAT,
        typefind);
    goto end_phase3;
  }

  agnosticbin = gst_pad_get_parent_element (peerpad);
  if (agnosticbin == NULL) {
    GST_ERROR_OBJECT (self, "No agnosticbin owns %" GST_PTR_FORMAT, peerpad);
    goto end_phase4;
  }

  probepad = gst_element_get_static_pad (agnosticbin, "src_0");
  if (probepad == NULL) {
    GST_ERROR_OBJECT (self, "No src_0 pad found in %" GST_PTR_FORMAT,
        agnosticbin);
    goto end_phase5;
  }

  wait = create_wait_condition ();
  data = create_probe_data (self, typefind, agnosticbin, wait);
  refdata = create_ref_counter (data, (GDestroyNotify) destroy_probe_data);

  /* install probe for EOS */
  gst_pad_add_probe (probepad, GST_PAD_PROBE_TYPE_BLOCK |
      GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM, event_probe_cb, refdata,
      (GDestroyNotify) ref_counter_dec);

  /* push EOS into the typefind's sink pad, the probe will be fired when the */
  /* EOS leaves the agnosticbin's src pad and both elements has thus drained */
  /* all their data */
  gst_pad_send_event (sinkpad, gst_event_new_eos ());
  gst_object_unref (probepad);

  WAIT_UNTIL_DONE (wait);
  destroy_wait_condition (wait);

end_phase5:
  gst_object_unref (agnosticbin);

end_phase4:
  gst_object_unref (peerpad);

end_phase3:
  gst_object_unref (srcpad);

end_phase2:
  gst_object_unref (typefind);

end_phase1:
  gst_object_unref (sinkpad);
}

static GstPad *
kms_audio_mixer_bin_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps)
{
  KmsAudioMixerBin *self = KMS_AUDIO_MIXER_BIN (element);
  GstPad *sinkpad, *pad = NULL;
  GstElement *typefind;
  gchar *padname;

  if (templ !=
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS (G_OBJECT_GET_CLASS
              (element)), AUDIO_MIXER_BIN_SINK_PAD)) {
    /* Only sink pads are allowed to be requested */
    return NULL;
  }

  GST_DEBUG ("Creating pad");
  typefind = gst_element_factory_make ("typefind", NULL);
  sinkpad = gst_element_get_static_pad (typefind, "sink");
  if (sinkpad == NULL) {
    gst_object_unref (typefind);
    return NULL;
  }

  gst_bin_add (GST_BIN (self), typefind);
  gst_element_sync_state_with_parent (typefind);

  KMS_AUDIO_MIXER_BIN_LOCK (self);

  padname = g_strdup_printf (AUDIO_MIXER_BIN_SINK_PAD, self->priv->count++);
  pad = gst_ghost_pad_new (padname, sinkpad);
  g_object_unref (sinkpad);
  GST_DEBUG ("Creating pad %s", padname);
  g_free (padname);

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, TRUE);

  if (!gst_element_add_pad (element, pad)) {
    GST_ERROR_OBJECT (self, "Could not create pad");
    g_object_unref (pad);
    gst_element_set_locked_state (typefind, TRUE);
    gst_element_set_state (typefind, GST_STATE_NULL);
    gst_bin_remove (GST_BIN (self), typefind);
    self->priv->count--;
    pad = NULL;
  } else {
    g_signal_connect (G_OBJECT (typefind), "have-type",
        G_CALLBACK (kms_audio_mixer_bin_have_type), self);
  }

  KMS_AUDIO_MIXER_BIN_UNLOCK (self);

  return pad;
}

static void
kms_audio_mixer_bin_release_pad (GstElement * element, GstPad * pad)
{
  GstElement *agnostic = NULL, *adder = NULL;

  GST_DEBUG ("Unlinked pad %" GST_PTR_FORMAT, pad);

  if (gst_pad_get_direction (pad) != GST_PAD_SINK)
    return;

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (element) >= GST_STATE_PAUSED) {
    kms_audio_mixer_bin_unlink_pad_in_playing (KMS_AUDIO_MIXER_BIN (element),
        pad);
  } else {
    GST_DEBUG ("TODO: unlink pad in paused state");
  }

  gst_ghost_pad_set_target (GST_GHOST_PAD (pad), NULL);
}

static void
kms_audio_mixer_bin_dispose (GObject * object)
{
  KmsAudioMixerBin *self = KMS_AUDIO_MIXER_BIN (object);

  GST_DEBUG_OBJECT (self, "dispose");

  KMS_AUDIO_MIXER_BIN_LOCK (self);

  g_clear_object (&self->priv->loop);

  KMS_AUDIO_MIXER_BIN_UNLOCK (self);

  G_OBJECT_CLASS (kms_audio_mixer_bin_parent_class)->dispose (object);
}

static void
kms_audio_mixer_bin_finalize (GObject * object)
{
  KmsAudioMixerBin *self = KMS_AUDIO_MIXER_BIN (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_rec_mutex_clear (&self->priv->mutex);

  G_OBJECT_CLASS (kms_audio_mixer_bin_parent_class)->finalize (object);
}

static void
kms_audio_mixer_bin_class_init (KmsAudioMixerBinClass * klass)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (gstelement_class,
      "AudioMixerBin", "Generic",
      "Audio mixer element",
      "Santiago Carot Nemesio <sancane at gmail dot com>");

  gstelement_class->request_new_pad =
      GST_DEBUG_FUNCPTR (kms_audio_mixer_bin_request_new_pad);
  gstelement_class->release_pad =
      GST_DEBUG_FUNCPTR (kms_audio_mixer_bin_release_pad);
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_sink_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_src_factory));

  gobject_class->dispose = GST_DEBUG_FUNCPTR (kms_audio_mixer_bin_dispose);
  gobject_class->finalize = GST_DEBUG_FUNCPTR (kms_audio_mixer_bin_finalize);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsAudioMixerBinPrivate));
}

static void
kms_audio_mixer_bin_init (KmsAudioMixerBin * self)
{
  GstPad *srcpad;

  self->priv = KMS_AUDIO_MIXER_BIN_GET_PRIVATE (self);

  self->priv->adder = gst_element_factory_make ("liveadder", NULL);
  gst_bin_add (GST_BIN (self), self->priv->adder);

  srcpad = gst_element_get_static_pad (self->priv->adder, "src");
  self->priv->srcpad = gst_ghost_pad_new (AUDIO_MIXER_BIN_SRC_PAD, srcpad);
  gst_object_unref (srcpad);

  gst_element_add_pad (GST_ELEMENT (self), self->priv->srcpad);
  gst_element_sync_state_with_parent (self->priv->adder);

  g_rec_mutex_init (&self->priv->mutex);
  self->priv->loop = kms_loop_new ();

  g_object_set (G_OBJECT (self), "async-handling", TRUE, NULL);
}

gboolean
kms_audio_mixer_bin_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_AUDIO_MIXER_BIN);
}
