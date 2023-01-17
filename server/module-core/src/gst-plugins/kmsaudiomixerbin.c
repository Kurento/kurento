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

#include <gst/gst.h>

#include "kmsaudiomixerbin.h"
#include "kmsloop.h"

#define PLUGIN_NAME "audiomixerbin"

#define KMS_AUDIO_MIXER_BIN_PROBE_ID_KEY "kms-audio-mixer-bin-probe-id"
G_DEFINE_QUARK (KMS_AUDIO_MIXER_BIN_PROBE_ID_KEY,
    kms_audio_mixer_bin_probe_id_key);

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
  GMutex *mutex;
  gboolean done;
};

#define OPERATION_DONE(wait) do {                  \
  g_mutex_lock ((wait)->mutex);                    \
  (wait)->done = TRUE;                             \
  g_cond_signal (&(wait)->cond);                   \
  g_mutex_unlock ((wait)->mutex);                  \
} while (0)

#define WAIT_UNTIL_DONE(wait) do {                 \
  while (!(wait)->done)                            \
      g_cond_wait (&(wait)->cond, (wait)->mutex); \
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
  g_cond_clear (&cond->cond);
  g_slice_free (WaitCond, cond);
}

static WaitCond *
create_wait_condition (GMutex * mutex)
{
  WaitCond *cond;

  cond = g_slice_new (WaitCond);
  cond->done = FALSE;
  cond->mutex = mutex;
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
  id = g_object_get_qdata (G_OBJECT (srcpad),
      kms_audio_mixer_bin_probe_id_key_quark ());

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
  if (id != NULL) {
    gst_pad_remove_probe (srcpad, *id);
    g_object_set_qdata_full (G_OBJECT (srcpad),
        kms_audio_mixer_bin_probe_id_key_quark (), NULL, NULL);
  }

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

static GstElement *
get_typefind_from_pad (GstPad * pad)
{
  GstElement *typefind;
  GstPad *sinkpad;

  sinkpad = gst_ghost_pad_get_target (GST_GHOST_PAD (pad));
  if (sinkpad == NULL) {
    GST_ERROR ("No target element connected to %" GST_PTR_FORMAT, pad);
    return NULL;
  }

  typefind = gst_pad_get_parent_element (sinkpad);
  gst_object_unref (sinkpad);

  return typefind;
}

static GstElement *
get_agnostic_from_pad (GstPad * pad)
{
  GstElement *typefind, *agnosticbin = NULL;
  GstPad *srcpad, *peerpad;

  typefind = get_typefind_from_pad (pad);
  if (typefind == NULL)
    return NULL;

  srcpad = gst_element_get_static_pad (typefind, "src");
  if (srcpad == NULL) {
    GST_ERROR ("No src pad got from %" GST_PTR_FORMAT, typefind);
    goto unref_typefind;
  }

  peerpad = gst_pad_get_peer (srcpad);
  if (peerpad == NULL) {
    GST_ERROR ("No agnosticbin connected to %" GST_PTR_FORMAT, typefind);
    goto unref_srcpad;
  }

  agnosticbin = gst_pad_get_parent_element (peerpad);
  gst_object_unref (peerpad);

unref_srcpad:
  gst_object_unref (srcpad);

unref_typefind:
  gst_object_unref (typefind);

  return agnosticbin;
}

static void
kms_audio_mixer_bin_remove_stream_group (KmsAudioMixerBin * self, GstPad * pad)
{
  GstElement *typefind, *agnosticbin;

  typefind = get_typefind_from_pad (pad);
  if (typefind == NULL)
    return;

  agnosticbin = get_agnostic_from_pad (pad);
  if (agnosticbin == NULL) {
    gst_object_unref (typefind);
    return;
  }

  kms_audio_mixer_bin_unlink_elements (self, typefind, agnosticbin);
  kms_audio_mixer_bin_remove_elements (self, typefind, agnosticbin);

  gst_object_unref (typefind);
  gst_object_unref (agnosticbin);
}

static GstPadProbeReturn
event_probe_cb (GstPad * pad, GstPadProbeInfo * info, gpointer user_data)
{
  GstEvent *event = gst_pad_probe_info_get_event (info);
  RefCounter *refdata;
  ProbeData *data;
  gulong *id;

  if (GST_EVENT_TYPE (event) != GST_EVENT_EOS)
    return GST_PAD_PROBE_OK;

  refdata = (RefCounter *) user_data;
  data = (ProbeData *) refdata->data;

  id = create_gulong (GST_PAD_PROBE_INFO_ID (info));
  g_object_set_qdata_full (G_OBJECT (pad),
      kms_audio_mixer_bin_probe_id_key_quark (), id,
      (GDestroyNotify) destroy_gulong);

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
  GstPad *sinkpad, *probepad;
  GstElement *typefind, *agnosticbin;
  WaitCond *wait = NULL;
  RefCounter *refdata;
  ProbeData *data;
  gulong probe_id;

  typefind = get_typefind_from_pad (pad);
  if (typefind == NULL)
    return;

  sinkpad = gst_element_get_static_pad (typefind, "sink");
  if (sinkpad == NULL) {
    GST_ERROR_OBJECT (self, "No sink pad got from %" GST_PTR_FORMAT, typefind);
    gst_object_unref (typefind);
    return;
  }
  agnosticbin = get_agnostic_from_pad (pad);
  if (agnosticbin == NULL) {
    gst_object_unref (sinkpad);
    gst_object_unref (typefind);
    return;
  }

  probepad = gst_element_get_static_pad (agnosticbin, "src_0");
  if (probepad == NULL) {
    GST_ERROR_OBJECT (self, "No src_0 pad found in %" GST_PTR_FORMAT,
        agnosticbin);
    gst_object_unref (sinkpad);
    gst_object_unref (typefind);
    gst_object_unref (agnosticbin);
    return;
  }

  wait = create_wait_condition (&GST_OBJECT (self)->lock);
  data = create_probe_data (self, typefind, agnosticbin, wait);
  refdata = create_ref_counter (data, (GDestroyNotify) destroy_probe_data);

  /* install probe for EOS */
  probe_id = gst_pad_add_probe (probepad, GST_PAD_PROBE_TYPE_BLOCK |
      GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM, event_probe_cb, refdata,
      (GDestroyNotify) ref_counter_dec);

  /* push EOS into the typefind's sink pad, the probe will be fired when the */
  /* EOS leaves the agnosticbin's src pad and both elements has thus drained */
  /* all their data */
  if (GST_PAD_IS_FLUSHING (sinkpad)) {
    GST_ERROR_OBJECT (sinkpad, "Pad is flushing");
  }
  gst_pad_send_event (sinkpad, gst_event_new_eos ());
  gst_object_unref (sinkpad);

  GST_OBJECT_LOCK (self);
  if ((GST_STATE (self) >= GST_STATE_PAUSED
          && (GST_STATE_PENDING (self) > GST_STATE_PAUSED
              || GST_STATE_PENDING (self) == GST_STATE_VOID_PENDING))
      || GST_STATE_PENDING (self) >= GST_STATE_PAUSED) {
    GST_INFO_OBJECT (sinkpad, "Waiting until done: %s, %s",
        gst_element_state_get_name (GST_STATE (self)),
        gst_element_state_get_name (GST_STATE_PENDING (self)));
    WAIT_UNTIL_DONE (wait);
  }
  GST_OBJECT_UNLOCK (self);

  gst_pad_remove_probe (probepad, probe_id);
  gst_object_unref (probepad);
  destroy_wait_condition (wait);

  gst_object_unref (typefind);
  gst_object_unref (agnosticbin);
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
  GST_DEBUG ("Unlinked pad %" GST_PTR_FORMAT, pad);

  if (gst_pad_get_direction (pad) != GST_PAD_SINK)
    return;

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (element) >= GST_STATE_PAUSED) {
    kms_audio_mixer_bin_unlink_pad_in_playing (KMS_AUDIO_MIXER_BIN (element),
        pad);
    gst_pad_set_active (pad, FALSE);
  } else {
    kms_audio_mixer_bin_remove_stream_group (KMS_AUDIO_MIXER_BIN (element),
        pad);
  }

  gst_ghost_pad_set_target (GST_GHOST_PAD (pad), NULL);
  gst_element_remove_pad (element, pad);
}

static void
kms_audio_mixer_bin_remove_stream_groups (KmsAudioMixerBin * self)
{
  GValue val = G_VALUE_INIT;
  GstIterator *it;
  gboolean done = FALSE;

  it = gst_element_iterate_sink_pads (GST_ELEMENT (self));
  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstPad *sinkpad;

        sinkpad = g_value_get_object (&val);
        kms_audio_mixer_bin_remove_stream_group (self, sinkpad);
        g_value_reset (&val);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR ("Error iterating over %s's src pads",
            GST_ELEMENT_NAME (self));
      case GST_ITERATOR_DONE:
        g_value_unset (&val);
        done = TRUE;
        break;
    }
  } while (!done);

  gst_iterator_free (it);
}

static void
kms_audio_mixer_bin_tear_down (KmsAudioMixerBin * self)
{
  kms_audio_mixer_bin_remove_stream_groups (self);

  /* Set ghostpad target to NULL */
  gst_ghost_pad_set_target (GST_GHOST_PAD (self->priv->srcpad), NULL);

  if (self->priv->adder) {
    gst_element_set_state (self->priv->adder, GST_STATE_NULL);
    gst_bin_remove (GST_BIN (self), self->priv->adder);
    self->priv->adder = NULL;
  }
}

static void
kms_audio_mixer_bin_dispose (GObject * object)
{
  KmsAudioMixerBin *self = KMS_AUDIO_MIXER_BIN (object);

  GST_DEBUG_OBJECT (self, "dispose");

  KMS_AUDIO_MIXER_BIN_LOCK (self);

  kms_audio_mixer_bin_tear_down (self);
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

  self->priv->adder = gst_element_factory_make ("audiomixer", NULL);
  gst_bin_add (GST_BIN (self), self->priv->adder);

  srcpad = gst_element_get_static_pad (self->priv->adder, "src");
  self->priv->srcpad = gst_ghost_pad_new (AUDIO_MIXER_BIN_SRC_PAD, srcpad);
  gst_object_unref (srcpad);

  gst_element_add_pad (GST_ELEMENT (self), self->priv->srcpad);
  gst_element_sync_state_with_parent (self->priv->adder);

  g_rec_mutex_init (&self->priv->mutex);
  self->priv->loop = kms_loop_new ();
}

gboolean
kms_audio_mixer_bin_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_AUDIO_MIXER_BIN);
}
