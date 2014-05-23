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

#include "kmsaudiomixer.h"
#include "kmsloop.h"

#define PLUGIN_NAME "audiomixer"
#define KEY_SINK_PAD_NAME "kms-key-sink-pad-name"
#define KEY_CONDITION "kms-key-condition"

#define KMS_AUDIO_MIXER_LOCK(mixer) \
  (g_rec_mutex_lock (&(mixer)->priv->mutex))

#define KMS_AUDIO_MIXER_UNLOCK(mixer) \
  (g_rec_mutex_unlock (&(mixer)->priv->mutex))

GST_DEBUG_CATEGORY_STATIC (kms_audio_mixer_debug_category);
#define GST_CAT_DEFAULT kms_audio_mixer_debug_category

#define KMS_AUDIO_MIXER_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (            \
    (obj),                                 \
    KMS_TYPE_AUDIO_MIXER,                  \
    KmsAudioMixerPrivate                   \
  )                                        \
)

struct _KmsAudioMixerPrivate
{
  GRecMutex mutex;
  GHashTable *adders;
  GHashTable *agnostics;
  KmsLoop *loop;
  guint count;
};

#define RAW_AUDIO_CAPS "audio/x-raw;"

/* the capabilities of the inputs and outputs. */
static GstStaticPadTemplate audio_sink_factory =
GST_STATIC_PAD_TEMPLATE (AUDIO_SINK_PAD,
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (RAW_AUDIO_CAPS)
    );

static GstStaticPadTemplate audio_src_factory =
GST_STATIC_PAD_TEMPLATE (AUDIO_SRC_PAD,
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS (RAW_AUDIO_CAPS)
    );

typedef struct _WaitCond WaitCond;
struct _WaitCond
{
  GCond cond;
  GMutex mutex;
  guint steps;
};

#define ONE_STEP_DONE(wait) do {                   \
  g_mutex_lock (&(wait)->mutex);                   \
  (wait)->steps--;                                 \
  g_cond_signal (&(wait)->cond);                   \
  g_mutex_unlock (&(wait)->mutex);                 \
} while (0)

#define WAIT_UNTIL_DONE(wait) do {                 \
  g_mutex_lock (&(wait)->mutex);                   \
  while ((wait)->steps > 0)                        \
      g_cond_wait (&(wait)->cond, &(wait)->mutex); \
  g_mutex_unlock (&(wait)->mutex);                 \
} while (0)

static void unlink_agnosticbin (GstElement * agnosticbin);
static void unlink_adder_sources (GstElement * adder);

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsAudioMixer, kms_audio_mixer,
    GST_TYPE_BIN,
    GST_DEBUG_CATEGORY_INIT (kms_audio_mixer_debug_category,
        PLUGIN_NAME, 0, "debug category for " PLUGIN_NAME " element"));

static void
destroy_wait_condition (WaitCond * cond)
{
  g_mutex_clear (&cond->mutex);
  g_cond_clear (&cond->cond);
  g_slice_free (WaitCond, cond);
}

static WaitCond *
create_wait_condition (guint steps)
{
  WaitCond *cond;

  cond = g_slice_new (WaitCond);
  cond->steps = steps;
  g_mutex_init (&cond->mutex);
  g_cond_init (&cond->cond);

  return cond;
}

static void
link_new_agnosticbin (gchar * key, GstElement * adder, GstElement * agnosticbin)
{
  char *padname;

  padname = g_object_get_data (G_OBJECT (agnosticbin), KEY_SINK_PAD_NAME);
  if (padname == NULL) {
    GST_ERROR ("No pad associated with %" GST_PTR_FORMAT, agnosticbin);
    return;
  }

  if (g_str_equal (key, padname)) {
    /* Do not connect the origin audio input */
    GST_TRACE ("Do not connect echo audio input %" GST_PTR_FORMAT, agnosticbin);
    return;
  }

  GST_DEBUG ("Linking %s to %s", GST_ELEMENT_NAME (agnosticbin),
      GST_ELEMENT_NAME (adder));

  if (!gst_element_link_pads (agnosticbin, "src_%u", adder, "sink_%u"))
    GST_ERROR ("Could not link %s to %s", GST_ELEMENT_NAME (agnosticbin),
        GST_ELEMENT_NAME (adder));
}

static void
link_new_adder (gchar * key, GstElement * agnosticbin, GstElement * adder)
{
  char *padname;

  padname = g_object_get_data (G_OBJECT (adder), KEY_SINK_PAD_NAME);
  if (padname == NULL) {
    GST_ERROR ("No pad associated with %" GST_PTR_FORMAT, adder);
    return;
  }

  if (g_str_equal (key, padname)) {
    /* Do not connect the origin audio input */
    GST_TRACE ("Do not connect echo audio input %" GST_PTR_FORMAT, agnosticbin);
    return;
  }

  GST_DEBUG ("Linking %s to %s", GST_ELEMENT_NAME (agnosticbin),
      GST_ELEMENT_NAME (adder));

  if (!gst_element_link_pads (agnosticbin, "src_%u", adder, "sink_%u"))
    GST_ERROR ("Could not link %s to %s", GST_ELEMENT_NAME (agnosticbin),
        GST_ELEMENT_NAME (adder));
}

static gint
get_stream_id_from_padname (const gchar * name)
{
  gint64 id;

  if (name == NULL)
    return -1;

  if (!g_str_has_prefix (name, AUDIO_SINK_PAD_PREFIX))
    return -1;

  id = g_ascii_strtoll (name + LENGTH_AUDIO_SINK_PAD_PREFIX, NULL, 10);
  if (id > G_MAXINT)
    return -1;

  return id;
}

static void
kms_audio_mixer_remove_sometimes_src_pad (KmsAudioMixer * self,
    GstElement * adder)
{
  GstProxyPad *internal;
  GstPad *srcpad, *peer;

  srcpad = gst_element_get_static_pad (adder, "src");
  peer = gst_pad_get_peer (srcpad);
  if (peer == NULL)
    goto end_phase_1;

  internal = gst_proxy_pad_get_internal ((GstProxyPad *) peer);
  if (internal == NULL)
    goto end_phase_2;

  gst_ghost_pad_set_target (GST_GHOST_PAD (internal), NULL);

  if (GST_STATE (self) < GST_STATE_PAUSED
      || GST_STATE_PENDING (self) < GST_STATE_PAUSED
      || GST_STATE_TARGET (self) < GST_STATE_PAUSED) {
    gst_pad_set_active (GST_PAD (internal), FALSE);
  }

  GST_DEBUG ("Removing source pad %p", internal);
  gst_element_remove_pad (GST_ELEMENT (self), GST_PAD (internal));
  gst_object_unref (internal);

end_phase_2:
  gst_object_unref (peer);

end_phase_1:
  gst_object_unref (srcpad);
}

static gboolean
remove_adder (GstElement * adder)
{
  KmsAudioMixer *self = KMS_AUDIO_MIXER (gst_element_get_parent (adder));
  WaitCond *wait;

  GST_DEBUG ("Removing element %" GST_PTR_FORMAT, adder);
  wait = g_object_get_data (G_OBJECT (adder), KEY_CONDITION);

  kms_audio_mixer_remove_sometimes_src_pad (self, adder);

  gst_object_ref (adder);
  gst_element_set_state (adder, GST_STATE_NULL);
  gst_bin_remove (GST_BIN (self), adder);
  gst_object_unref (adder);

  gst_object_unref (self);

  if (wait != NULL)
    ONE_STEP_DONE (wait);

  return G_SOURCE_REMOVE;
}

static void
remove_agnostic_bin (GstElement * agnosticbin)
{
  KmsAudioMixer *self = KMS_AUDIO_MIXER (gst_element_get_parent (agnosticbin));
  GstElement *audiorate, *typefind;
  GstPad *sinkpad, *peerpad;
  WaitCond *wait;

  wait = g_object_get_data (G_OBJECT (agnosticbin), KEY_CONDITION);

  sinkpad = gst_element_get_static_pad (agnosticbin, "sink");
  peerpad = gst_pad_get_peer (sinkpad);
  audiorate = gst_pad_get_parent_element (peerpad);
  gst_object_unref (sinkpad);
  gst_object_unref (peerpad);

  sinkpad = gst_element_get_static_pad (audiorate, "sink");
  peerpad = gst_pad_get_peer (sinkpad);
  typefind = gst_pad_get_parent_element (peerpad);
  gst_object_unref (sinkpad);
  gst_object_unref (peerpad);

  gst_element_unlink_many (typefind, audiorate, agnosticbin, NULL);
  gst_element_set_state (typefind, GST_STATE_NULL);
  gst_element_set_state (audiorate, GST_STATE_NULL);
  gst_element_set_state (agnosticbin, GST_STATE_NULL);
  gst_bin_remove_many (GST_BIN (self), typefind, audiorate, agnosticbin, NULL);

  gst_object_unref (audiorate);
  gst_object_unref (typefind);
  gst_object_unref (self);

  if (wait != NULL)
    ONE_STEP_DONE (wait);
}

static gboolean
remove_adder_cb (gpointer key, gpointer value, gpointer user_data)
{
  GstElement *adder = GST_ELEMENT (value);

  unlink_adder_sources (adder);
  remove_adder (adder);

  return TRUE;
}

static gboolean
remove_agnosticbin_cb (gpointer key, gpointer value, gpointer user_data)
{
  GstElement *agnostic = GST_ELEMENT (value);

  unlink_agnosticbin (agnostic);
  remove_agnostic_bin (agnostic);

  return TRUE;
}

static void
kms_audio_mixer_dispose (GObject * object)
{
  KmsAudioMixer *self = KMS_AUDIO_MIXER (object);

  GST_DEBUG_OBJECT (self, "dispose");

  KMS_AUDIO_MIXER_LOCK (self);

  if (self->priv->agnostics != NULL) {
    g_hash_table_foreach_remove (self->priv->agnostics, remove_agnosticbin_cb,
        NULL);
    g_hash_table_unref (self->priv->agnostics);
    self->priv->agnostics = NULL;
  }

  if (self->priv->adders != NULL) {
    g_hash_table_foreach_remove (self->priv->adders, remove_adder_cb, self);
    g_hash_table_unref (self->priv->adders);
    self->priv->adders = NULL;
  }

  g_clear_object (&self->priv->loop);

  KMS_AUDIO_MIXER_UNLOCK (self);

  G_OBJECT_CLASS (kms_audio_mixer_parent_class)->dispose (object);
}

static void
kms_audio_mixer_finalize (GObject * object)
{
  KmsAudioMixer *self = KMS_AUDIO_MIXER (object);

  GST_DEBUG_OBJECT (self, "finalize");

  g_rec_mutex_clear (&self->priv->mutex);

  G_OBJECT_CLASS (kms_audio_mixer_parent_class)->finalize (object);
}

static void
kms_audio_mixer_have_type (GstElement * typefind, guint arg0, GstCaps * caps,
    gpointer data)
{
  KmsAudioMixer *self = KMS_AUDIO_MIXER (data);
  GstElement *audiorate, *agnosticbin;
  gchar *padname;
  gint id;

  padname = g_object_get_data (G_OBJECT (typefind), KEY_SINK_PAD_NAME);
  if ((id = get_stream_id_from_padname (padname)) < 0) {
    GST_ERROR_OBJECT (self, "Can not get pad id from element %" GST_PTR_FORMAT,
        typefind);
    return;
  }

  audiorate = gst_element_factory_make ("audiorate", NULL);
  agnosticbin = gst_element_factory_make ("agnosticbin", NULL);
  g_object_set_data_full (G_OBJECT (agnosticbin), KEY_SINK_PAD_NAME,
      g_strdup (padname), g_free);

  gst_bin_add_many (GST_BIN (self), audiorate, agnosticbin, NULL);

  gst_element_sync_state_with_parent (audiorate);
  gst_element_sync_state_with_parent (agnosticbin);

  gst_element_link_many (typefind, audiorate, agnosticbin, NULL);

  KMS_AUDIO_MIXER_LOCK (self);

  g_hash_table_foreach (self->priv->adders, (GHFunc) link_new_agnosticbin,
      agnosticbin);

  g_hash_table_insert (self->priv->agnostics, g_strdup (padname), agnosticbin);

  KMS_AUDIO_MIXER_UNLOCK (self);
}

struct callback_counter
{
  guint count;
  gpointer data;
  GDestroyNotify notif;
  GMutex mutex;
};

struct callback_counter *
create_callback_counter (gpointer data, GDestroyNotify notif)
{
  struct callback_counter *counter;

  counter = g_slice_new (struct callback_counter);

  g_mutex_init (&counter->mutex);
  counter->notif = notif;
  counter->data = data;
  counter->count = 1;

  return counter;
}

static void
callback_count_inc (struct callback_counter *counter)
{
  g_mutex_lock (&counter->mutex);
  counter->count++;
  g_mutex_unlock (&counter->mutex);
}

static void
callback_count_dec (struct callback_counter *counter)
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

  g_slice_free (struct callback_counter, counter);
}

static GstPadProbeReturn
event_probe_cb (GstPad * pad, GstPadProbeInfo * info, gpointer user_data)
{
  if (GST_EVENT_TYPE (GST_PAD_PROBE_INFO_DATA (info)) != GST_EVENT_EOS)
    return GST_PAD_PROBE_OK;

  gst_pad_remove_probe (pad, GST_PAD_PROBE_INFO_ID (info));

  return GST_PAD_PROBE_DROP;
}

static void
unlink_agnosticbin (GstElement * agnosticbin)
{
  GValue val = G_VALUE_INIT;
  GstIterator *it;
  gboolean done = FALSE;

  it = gst_element_iterate_src_pads (agnosticbin);
  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstPad *srcpad, *sinkpad;
        GstElement *adder;

        srcpad = g_value_get_object (&val);
        sinkpad = gst_pad_get_peer (srcpad);
        adder = gst_pad_get_parent_element (sinkpad);

        GST_DEBUG ("Unlink %" GST_PTR_FORMAT " and %" GST_PTR_FORMAT,
            agnosticbin, adder);

        if (!gst_pad_unlink (srcpad, sinkpad)) {
          GST_ERROR ("Can not unlink %" GST_PTR_FORMAT " and %" GST_PTR_FORMAT,
              srcpad, sinkpad);
        }

        gst_element_release_request_pad (adder, sinkpad);
        gst_element_release_request_pad (agnosticbin, srcpad);

        gst_object_unref (sinkpad);
        gst_object_unref (adder);
        g_value_reset (&val);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR ("Error iterating over %s's src pads",
            GST_ELEMENT_NAME (agnosticbin));
      case GST_ITERATOR_DONE:
        g_value_unset (&val);
        done = TRUE;
        break;
    }
  } while (!done);

  gst_iterator_free (it);
}

static gboolean
remove_agnosticbin (GstElement * agnosticbin)
{
  unlink_agnosticbin (agnosticbin);
  remove_agnostic_bin (agnosticbin);

  return G_SOURCE_REMOVE;
}

static void
agnosticbin_counter_done (GstElement * agnosticbin)
{
  KmsAudioMixer *self = KMS_AUDIO_MIXER (gst_element_get_parent (agnosticbin));

  /* All EOS on agnosticbin are been received */
  if (self == NULL)
    return;

  /* We can not access to some GstPad functions because of mutex deadlocks */
  /* So we are going to manage all the stuff in a separate thread */
  kms_loop_idle_add_full (self->priv->loop, G_PRIORITY_DEFAULT,
      (GSourceFunc) remove_agnosticbin, gst_object_ref (agnosticbin),
      gst_object_unref);

  gst_object_unref (self);
}

static void
agnosticbin_set_EOS_cb (GstElement * agnostic)
{
  struct callback_counter *counter;
  GValue val = G_VALUE_INIT;
  GstIterator *it;
  gboolean done = FALSE;

  counter =
      create_callback_counter (agnostic,
      (GDestroyNotify) agnosticbin_counter_done);

  it = gst_element_iterate_src_pads (agnostic);
  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstPad *srcpad;

        srcpad = g_value_get_object (&val);

        /* install new probe for EOS */
        callback_count_inc (counter);
        gst_pad_add_probe (srcpad, GST_PAD_PROBE_TYPE_BLOCK |
            GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM, event_probe_cb, counter,
            (GDestroyNotify) callback_count_dec);

        g_value_reset (&val);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR ("Error iterating over %s's src pads",
            GST_ELEMENT_NAME (agnostic));
      case GST_ITERATOR_DONE:
        g_value_unset (&val);
        done = TRUE;
        break;
    }
  } while (!done);

  callback_count_dec (counter);
  gst_iterator_free (it);
}

static void
unlink_adder_sources (GstElement * adder)
{
  GValue val = G_VALUE_INIT;
  GstIterator *it;
  gboolean done = FALSE;

  it = gst_element_iterate_sink_pads (adder);
  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstPad *sinkpad, *srcpad;
        GstElement *agnosticbin;

        sinkpad = g_value_get_object (&val);
        srcpad = gst_pad_get_peer (sinkpad);
        agnosticbin = gst_pad_get_parent_element (srcpad);

        GST_DEBUG ("Unlink %" GST_PTR_FORMAT " and %" GST_PTR_FORMAT,
            agnosticbin, adder);

        if (!gst_pad_unlink (srcpad, sinkpad)) {
          GST_ERROR ("Can not unlink %" GST_PTR_FORMAT " and %" GST_PTR_FORMAT,
              srcpad, sinkpad);
        }

        gst_element_release_request_pad (adder, sinkpad);
        gst_element_release_request_pad (agnosticbin, srcpad);

        gst_object_unref (srcpad);
        gst_object_unref (agnosticbin);
        g_value_reset (&val);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR ("Error iterating over %s's src pads",
            GST_ELEMENT_NAME (adder));
      case GST_ITERATOR_DONE:
        g_value_unset (&val);
        done = TRUE;
        break;
    }
  } while (!done);

  gst_iterator_free (it);
}

static void
unlink_pad_in_playing (GstPad * pad, GstElement * agnosticbin,
    GstElement * adder)
{
  WaitCond *wait = NULL;
  guint steps = 0;

  if (agnosticbin != NULL)
    steps++;

  if (adder != NULL)
    steps++;

  wait = create_wait_condition (steps);

  if (agnosticbin != NULL) {
    g_object_set_data (G_OBJECT (agnosticbin), KEY_CONDITION, wait);
    agnosticbin_set_EOS_cb (agnosticbin);

    /* push EOS into the element, the probe will be fired when the */
    /* EOS leaves the effect and it has thus drained all of its data */
    gst_pad_send_event (pad, gst_event_new_eos ());
  }

  if (adder != NULL) {
    g_object_set_data (G_OBJECT (adder), KEY_CONDITION, wait);

    unlink_adder_sources (adder);
    remove_adder (adder);
  }

  WAIT_UNTIL_DONE (wait);

  destroy_wait_condition (wait);
}

static void
unlinked_pad (GstPad * pad, GstPad * peer, gpointer user_data)
{
  GstElement *agnostic = NULL, *adder = NULL, *parent;
  KmsAudioMixer *self;
  gchar *padname;

  GST_DEBUG ("Unlinked pad %" GST_PTR_FORMAT, pad);
  parent = gst_pad_get_parent_element (pad);

  if (parent == NULL)
    return;

  self = KMS_AUDIO_MIXER (parent);

  if (gst_pad_get_direction (pad) != GST_PAD_SINK)
    return;

  padname = gst_pad_get_name (pad);

  KMS_AUDIO_MIXER_LOCK (self);

  if (self->priv->agnostics != NULL) {
    agnostic = g_hash_table_lookup (self->priv->agnostics, padname);
    g_hash_table_remove (self->priv->agnostics, padname);
  }

  if (self->priv->adders != NULL) {
    adder = g_hash_table_lookup (self->priv->adders, padname);
    g_hash_table_remove (self->priv->adders, padname);
  }

  KMS_AUDIO_MIXER_UNLOCK (self);

  g_free (padname);

  if (GST_STATE (parent) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (parent) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (parent) >= GST_STATE_PAUSED) {
    unlink_pad_in_playing (pad, agnostic, adder);
  } else {
    if (agnostic != NULL) {
      unlink_agnosticbin (agnostic);
      remove_agnostic_bin (agnostic);
    }
    if (adder != NULL) {
      unlink_adder_sources (adder);
      remove_adder (adder);
    }
  }

  gst_ghost_pad_set_target (GST_GHOST_PAD (pad), NULL);
  gst_object_unref (parent);
}

static gboolean
kms_audio_mixer_add_src_pad (KmsAudioMixer * self, const char *padname)
{
  GstPad *srcpad, *pad;
  GstElement *adder;
  gchar *srcname;
  gint id;

  if ((id = get_stream_id_from_padname (padname)) < 0) {
    GST_ERROR_OBJECT (self, "Can not get pad id from element %s", padname);
    return FALSE;
  }

  adder = gst_element_factory_make ("liveadder", NULL);
  g_object_set_data_full (G_OBJECT (adder), KEY_SINK_PAD_NAME,
      g_strdup (padname), g_free);

  gst_bin_add (GST_BIN (self), adder);
  gst_element_sync_state_with_parent (adder);

  KMS_AUDIO_MIXER_LOCK (self);

  g_hash_table_foreach (self->priv->agnostics, (GHFunc) link_new_adder, adder);
  g_hash_table_insert (self->priv->adders, g_strdup (padname), adder);

  srcname = g_strdup_printf ("src_%u", id);
  srcpad = gst_element_get_static_pad (adder, "src");
  pad = gst_ghost_pad_new (srcname, srcpad);
  g_free (srcname);
  gst_object_unref (srcpad);

  if (GST_STATE (self) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (self) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (self) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, TRUE);

  if (gst_element_add_pad (GST_ELEMENT (self), pad)) {
    KMS_AUDIO_MIXER_UNLOCK (self);
    return TRUE;
  }

  /* ERROR */
  GST_ERROR_OBJECT (self, "Can not add pad %" GST_PTR_FORMAT, pad);
  g_hash_table_remove (self->priv->adders, padname);

  KMS_AUDIO_MIXER_UNLOCK (self);

  unlink_adder_sources (adder);

  gst_object_unref (pad);

  gst_element_set_locked_state (adder, TRUE);
  gst_element_set_state (adder, GST_STATE_NULL);

  gst_bin_remove (GST_BIN (self), adder);

  return FALSE;
}

static GstPad *
kms_audio_mixer_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps)
{
  KmsAudioMixer *self = KMS_AUDIO_MIXER (element);
  GstPad *sinkpad, *pad = NULL;
  GstElement *typefind;
  gchar *padname;

  if (templ !=
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS (G_OBJECT_GET_CLASS
              (element)), AUDIO_SINK_PAD))
    return NULL;

  typefind = gst_element_factory_make ("typefind", NULL);
  sinkpad = gst_element_get_static_pad (typefind, "sink");
  if (sinkpad == NULL) {
    gst_object_unref (typefind);
    return NULL;
  }

  gst_bin_add (GST_BIN (self), typefind);
  gst_element_sync_state_with_parent (typefind);

  KMS_AUDIO_MIXER_LOCK (self);

  padname = g_strdup_printf (AUDIO_SINK_PAD, self->priv->count++);

  pad = gst_ghost_pad_new (padname, sinkpad);
  g_object_unref (sinkpad);

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, TRUE);

  if (!gst_element_add_pad (element, pad)) {
    GST_ERROR_OBJECT (self, "Could not create sink pad");
  } else if (!kms_audio_mixer_add_src_pad (self, padname)) {
    GST_ERROR_OBJECT (self, "Could not create source pad");
  } else {
    g_object_set_data_full (G_OBJECT (typefind), KEY_SINK_PAD_NAME, padname,
        g_free);
    g_signal_connect (G_OBJECT (typefind), "have-type",
        G_CALLBACK (kms_audio_mixer_have_type), self);
    goto end;
  }

  /* Error */
  g_object_unref (pad);
  gst_element_set_locked_state (typefind, TRUE);
  gst_element_set_state (typefind, GST_STATE_NULL);
  gst_bin_remove (GST_BIN (self), typefind);
  self->priv->count--;
  pad = NULL;
  g_free (padname);

end:
  KMS_AUDIO_MIXER_UNLOCK (self);

  if (pad != NULL) {
    g_signal_connect (G_OBJECT (pad), "unlinked",
        G_CALLBACK (unlinked_pad), NULL);
  }

  return pad;
}

static void
kms_audio_mixer_release_pad (GstElement * element, GstPad * pad)
{
  GST_DEBUG ("Release pad %" GST_PTR_FORMAT, pad);

  if (gst_pad_get_direction (pad) != GST_PAD_SINK)
    return;

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (element) >= GST_STATE_PAUSED) {
    gst_pad_set_active (pad, FALSE);
  }

  gst_element_remove_pad (element, pad);
}

static void
kms_audio_mixer_class_init (KmsAudioMixerClass * klass)
{
  GstElementClass *gstelement_class = GST_ELEMENT_CLASS (klass);
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (gstelement_class,
      "AudioMixer", "Generic",
      "Audio mixer element",
      "Santiago Carot Nemesio <sancane at gmail dot com>");

  gstelement_class->request_new_pad =
      GST_DEBUG_FUNCPTR (kms_audio_mixer_request_new_pad);
  gstelement_class->release_pad =
      GST_DEBUG_FUNCPTR (kms_audio_mixer_release_pad);
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_sink_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_src_factory));

  gobject_class->dispose = GST_DEBUG_FUNCPTR (kms_audio_mixer_dispose);
  gobject_class->finalize = GST_DEBUG_FUNCPTR (kms_audio_mixer_finalize);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsAudioMixerPrivate));
}

static void
kms_audio_mixer_init (KmsAudioMixer * self)
{
  self->priv = KMS_AUDIO_MIXER_GET_PRIVATE (self);

  self->priv->adders = g_hash_table_new_full (g_str_hash, g_str_equal, g_free,
      NULL);
  self->priv->agnostics =
      g_hash_table_new_full (g_str_hash, g_str_equal, g_free, NULL);

  g_rec_mutex_init (&self->priv->mutex);
  self->priv->loop = kms_loop_new ();

  g_object_set (G_OBJECT (self), "async-handling", TRUE, NULL);
}

gboolean
kms_audio_mixer_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_AUDIO_MIXER);
}
