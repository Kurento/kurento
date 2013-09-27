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
#  include <config.h>
#endif

#include <gst/gst.h>
#include "kmsagnosticbin.h"
#include "kmsautomuxerbin.h"
#include "kmsagnosticcaps.h"
#include "kmsutils.h"

#define PLUGIN_NAME "automuxerbin"

#define AUDIO_PAD_NAME "audio_"
#define VIDEO_PAD_NAME "video_"
#define SOURCE_PAD_NAME "src_"

GST_DEBUG_CATEGORY_STATIC (kms_automuxer_bin_debug);
#define GST_CAT_DEFAULT kms_automuxer_bin_debug

#define KMS_AUTOMUXER_BIN_LOCK(elem) \
  (g_rec_mutex_lock (&KMS_AUTOMUXER_BIN_CAST ((elem))->priv->mutex))
#define KMS_AUTOMUXER_BIN_UNLOCK(elem) \
  (g_rec_mutex_unlock (&KMS_AUTOMUXER_BIN_CAST ((elem))->priv->mutex))

typedef struct _PadCount PadCount;
struct _PadCount
{
  guint video;
  guint audio;
  guint source;
};

#define KMS_AUTOMUXER_BIN_GET_PRIVATE(obj) ( \
  G_TYPE_INSTANCE_GET_PRIVATE (              \
    (obj),                                   \
    KMS_TYPE_AUTOMUXER_BIN,                  \
    KmsAutoMuxerBinPrivate                   \
  )                                          \
)
struct _KmsAutoMuxerBinPrivate
{
  GRecMutex mutex;
  GstElement *muxer;
  GSList *valves;
  GList *muxers;
  PadCount *pads;
};

typedef enum
{
  VALVE_CLOSE,
  VALVE_OPEN
} ValveState;

typedef enum
{
  INVALID_PAD,
  SOURCE_PAD,
  /* sink pads */
  AUDIO_PAD,
  VIDEO_PAD
} PadType;

/* the capabilities of the inputs and outputs. */
static GstStaticPadTemplate sink_factory_audio =
GST_STATIC_PAD_TEMPLATE (AUDIO_PAD_NAME "%u",
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (KMS_AGNOSTIC_AUDIO_CAPS)
    );

static GstStaticPadTemplate sink_factory_video =
GST_STATIC_PAD_TEMPLATE (VIDEO_PAD_NAME "%u",
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (KMS_AGNOSTIC_VIDEO_CAPS)
    );

static GstStaticPadTemplate src_factory =
GST_STATIC_PAD_TEMPLATE (SOURCE_PAD_NAME "%u",
    GST_PAD_SRC,
    GST_PAD_SOMETIMES,
    GST_STATIC_CAPS ("ANY")
    );

#define kms_automuxer_bin_parent_class parent_class
G_DEFINE_TYPE (KmsAutoMuxerBin, kms_automuxer_bin, GST_TYPE_BIN);

static GstElementFactory *
get_muxer_factory_with_sink_caps (KmsAutoMuxerBin * automuxerbin,
    const GstCaps * caps)
{
  GstElementFactory *mfactory = NULL;
  GList *flist, *e;

  if (!automuxerbin->priv->muxers
      || g_list_length (automuxerbin->priv->muxers) <= 0)
    return NULL;

  flist =
      gst_element_factory_list_filter (automuxerbin->priv->muxers, caps,
      GST_PAD_SINK, TRUE);
  if (!flist || g_list_length (flist) <= 0)
    return NULL;

  gst_plugin_feature_list_free (automuxerbin->priv->muxers);
  automuxerbin->priv->muxers = flist;

  e = g_list_first (automuxerbin->priv->muxers);
  mfactory = GST_ELEMENT_FACTORY (e->data);
  g_object_ref (G_OBJECT (mfactory));

  return mfactory;
}

static GstElement *
connect_to_valve (GstBin * bin, GstElement * element)
{
  GstElement *valve;

  valve = gst_element_factory_make ("valve", NULL);
  if (valve == NULL)
    return NULL;

  if (!gst_bin_add (bin, valve)) {
    gst_object_unref (valve);
    return NULL;
  }

  gst_element_sync_state_with_parent (valve);
  gst_element_link (element, valve);
  return valve;
}

static void
set_valve (gpointer v, gpointer s)
{
  GstElement *valve = GST_ELEMENT (v);
  ValveState *state = s;

  if (*state == VALVE_CLOSE)
    kms_utils_set_valve_drop (valve, TRUE);
  else if (*state == VALVE_OPEN)
    kms_utils_set_valve_drop (valve, FALSE);
}

static void
unlink_muxer (gpointer v, gpointer m)
{
  GstElement *valve = GST_ELEMENT (v);
  GstElement *muxer = GST_ELEMENT (m);
  GstElement *capsfilter;
  GstElement *automuxerbin;
  GstPad *srcpad, *peer;

  srcpad = gst_element_get_static_pad (valve, "src");
  peer = gst_pad_get_peer (srcpad);

  /* Capsfilter element is automatically added when filtered link is used. */
  /* That element is inserted between the valve and the muxer, so we have */
  /* get rid of it so as to be able to link the valve to other element */
  /* whenever other muxer is selected */
  capsfilter = gst_pad_get_parent_element (peer);
  automuxerbin = GST_ELEMENT (GST_OBJECT_PARENT (capsfilter));

  GST_DEBUG ("Unlinking: %s--X-->%s--X-->%s\n", GST_ELEMENT_NAME (valve),
      GST_ELEMENT_NAME (capsfilter), GST_ELEMENT_NAME (muxer));

  gst_element_unlink_many (valve, capsfilter, muxer, NULL);

  gst_bin_remove (GST_BIN (automuxerbin), capsfilter);
  gst_element_set_state (capsfilter, GST_STATE_NULL);
  g_object_unref (G_OBJECT (capsfilter));

  gst_object_unref (G_OBJECT (srcpad));
  gst_object_unref (G_OBJECT (peer));
}

static void
link_muxer (gpointer v, gpointer m)
{
  GstElement *valve = GST_ELEMENT (v);
  GstElement *muxer = GST_ELEMENT (m);
  GstPad *sinkpad, *peer;
  GstCaps *caps;

  GST_DEBUG ("Linking %s---->%s\n", GST_ELEMENT_NAME (valve),
      GST_ELEMENT_NAME (muxer));

  sinkpad = gst_element_get_static_pad (valve, "sink");
  peer = gst_pad_get_peer (sinkpad);

  caps = gst_pad_get_current_caps (peer);

  /* Filtered caps are required here to connect the valve which has ANY caps */
  /* to the muxer in order to get the proper pad with the required caps. */
  gst_element_link_filtered (valve, muxer, caps);

  g_object_unref (G_OBJECT (sinkpad));
  g_object_unref (G_OBJECT (peer));
  gst_caps_unref (caps);
}

static gchar *
get_pad_name (KmsAutoMuxerBin * self, PadType type)
{
  gchar *name = NULL;

  switch (type) {
    case AUDIO_PAD:
      name = g_strdup_printf (AUDIO_PAD_NAME "%d", self->priv->pads->audio++);
      break;
    case VIDEO_PAD:
      name = g_strdup_printf (VIDEO_PAD_NAME "%d", self->priv->pads->video++);
      break;
    case SOURCE_PAD:
      name = g_strdup_printf (SOURCE_PAD_NAME "%d", self->priv->pads->source++);
      break;
    default:
      GST_WARNING ("Unknown pad type %d", type);
  }

  return name;
}

static PadType
get_pad_type_from_template (GstElement * element, GstPadTemplate * templ)
{
  GstElementClass *klass = GST_ELEMENT_GET_CLASS (element);

  if (templ == gst_element_class_get_pad_template (klass, AUDIO_PAD_NAME "%u"))
    return AUDIO_PAD;
  else if (templ == gst_element_class_get_pad_template (klass,
          VIDEO_PAD_NAME "%u"))
    return VIDEO_PAD;
  else {
    GST_ERROR ("Unable to manage template %s",
        GST_PAD_TEMPLATE_NAME_TEMPLATE (templ));
    return INVALID_PAD;
  }
}

static void
add_src_target_pad (GstElement * muxer)
{
  GstPadTemplate *templ;
  GstPad *src, *ghostpad;
  GstElement *self;
  gchar *padname;

  src = gst_element_get_static_pad (muxer, "src");
  templ = gst_static_pad_template_get (&src_factory);

  self = GST_ELEMENT (GST_OBJECT_PARENT (muxer));
  padname = get_pad_name (KMS_AUTOMUXER_BIN (self), SOURCE_PAD);
  ghostpad = gst_ghost_pad_new_from_template (padname, src, templ);
  g_free (padname);

  gst_object_unref (src);
  g_object_unref (templ);

  /* Set pad state before adding it */
  if (GST_STATE (self) >= GST_STATE_PAUSED)
    gst_pad_set_active (ghostpad, TRUE);

  gst_element_add_pad (self, ghostpad);
}

static void
remove_src_target_pad (GstElement * muxer)
{
  GstPad *srcpad, *peerpad;
  GstProxyPad *ppad = NULL;
  GstElement *self;

  srcpad = gst_element_get_static_pad (muxer, "src");
  if (srcpad == NULL)
    return;

  peerpad = gst_pad_get_peer (srcpad);
  if (peerpad == NULL)
    goto end;

  /* the peer's pad is a GstProxyPad element which is attached */
  /* to the GhostPad we are looking for. Next function gets the */
  /* ghostpad whose muxer's source pad is target of */
  ppad = gst_proxy_pad_get_internal ((GstProxyPad *) peerpad);
  if (ppad == NULL)
    goto end;

  self = GST_ELEMENT (GST_OBJECT_PARENT (muxer));
  if (GST_STATE (self) >= GST_STATE_PAUSED)
    gst_pad_set_active ((GstPad *) ppad, FALSE);

  gst_element_remove_pad (GST_ELEMENT (self), (GstPad *) ppad);

end:
  if (srcpad != NULL)
    gst_object_unref (G_OBJECT (srcpad));

  if (peerpad != NULL)
    gst_object_unref (G_OBJECT (peerpad));
}

static GstPadProbeReturn
event_eos_probe_cb (GstPad * pad, GstPadProbeInfo * info, gpointer user_data)
{
  GstElement *muxer;
  GstElement *self;

  if (GST_EVENT_TYPE (GST_PAD_PROBE_INFO_DATA (info)) != GST_EVENT_EOS) {
    /* ignore this event */
    return GST_PAD_PROBE_OK;
  }

  muxer = GST_ELEMENT (user_data);
  self = GST_ELEMENT (GST_OBJECT_PARENT (muxer));

  /* Prevent muxer from changing its state when its parent does */
  if (!gst_element_set_locked_state (muxer, TRUE))
    GST_ERROR ("Could not block element %s", GST_ELEMENT_NAME (muxer));

  GST_DEBUG ("Removing muxer %s from %s", GST_ELEMENT_NAME (muxer),
      GST_ELEMENT_NAME (self));

  /* Set downstream elements to state NULL */
  gst_element_set_state (muxer, GST_STATE_NULL);

  remove_src_target_pad (muxer);

  /* Remove old muxer */
  gst_bin_remove (GST_BIN (self), muxer);

  /* remove probe */
  return GST_PAD_PROBE_REMOVE;
}

static void
remove_muxer (GstElement * muxer)
{
  GValue elem = G_VALUE_INIT;
  gboolean done = FALSE;
  GstPad *srcpad;
  GstIterator *it;

  /* install probe for EOS */
  srcpad = gst_element_get_static_pad (muxer, "src");
  gst_pad_add_probe (srcpad, GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM,
      event_eos_probe_cb, g_object_ref (G_OBJECT (muxer)), g_object_unref);
  gst_object_unref (srcpad);

  /* push EOS in each sink pad that this muxer has, the probe will be fired */
  /* when the EOS leaves the muxer and it has thus drained all of its data */
  it = gst_element_iterate_sink_pads (muxer);
  do {
    switch (gst_iterator_next (it, &elem)) {
      case GST_ITERATOR_OK:
      {
        GstPad *sinkpad;

        sinkpad = g_value_get_object (&elem);
        gst_pad_send_event (sinkpad, gst_event_new_eos ());
        g_value_reset (&elem);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR ("Error iterating over %s's sink pads",
            GST_ELEMENT_NAME (muxer));
      case GST_ITERATOR_DONE:
        g_value_unset (&elem);
        done = TRUE;
        break;
    }
  } while (!done);

  gst_iterator_free (it);
}

static gboolean
reconfigure_pipeline (GstElement * typefind, GstCaps * caps,
    KmsAutoMuxerBin * self)
{
  GstElement *valve;
  GstElementFactory *mfactory;
  ValveState state;

  mfactory = get_muxer_factory_with_sink_caps (self, caps);

  if (mfactory == NULL) {
    GST_ERROR ("No factories capable of managing these caps were found\n");
    return FALSE;
  }

  /* Close valves before replacing the muxer */
  state = VALVE_CLOSE;
  g_slist_foreach (self->priv->valves, set_valve, &state);

  /* unlink elements from old muxer */
  g_slist_foreach (self->priv->valves, unlink_muxer, self->priv->muxer);

  if (self->priv->muxer != NULL)
    remove_muxer (self->priv->muxer);

  valve = connect_to_valve (GST_BIN (self), typefind);

  /* Add the new muxer to the pipeline */
  self->priv->muxer = gst_element_factory_create (mfactory, NULL);
  gst_bin_add (GST_BIN (self), self->priv->muxer);
  gst_element_sync_state_with_parent (self->priv->muxer);

  add_src_target_pad (self->priv->muxer);

  self->priv->valves = g_slist_append (self->priv->valves, valve);

  g_object_unref (mfactory);

  /* re-stablish internal connections */
  g_slist_foreach (self->priv->valves, link_muxer, self->priv->muxer);

  /* Open previously closed valves */
  state = VALVE_OPEN;
  g_slist_foreach (self->priv->valves, set_valve, &state);

  return TRUE;
}

static gboolean
has_valve (GstElement * typefind)
{
  GstPad *srcpad;
  gboolean ret;

  srcpad = gst_element_get_static_pad (typefind, "src");
  if (srcpad == NULL)
    return FALSE;

  ret = gst_pad_is_linked (srcpad);
  gst_object_unref (G_OBJECT (srcpad));
  return ret;
}

static void
found_type_cb (GstElement * typefind,
    guint prob, GstCaps * caps, KmsAutoMuxerBin * self)
{
  gboolean done;

  if (has_valve (typefind)) {
    GST_ERROR ("Typefind %s detected a change in the media type",
        GST_ELEMENT_NAME (typefind));
    return;
  }

  KMS_AUTOMUXER_BIN_LOCK (self);

  done = reconfigure_pipeline (typefind, caps, self);

  KMS_AUTOMUXER_BIN_UNLOCK (self);

  if (!done) {
    /* TODO: Add agnostic bin to the audio input and find a muxer which */
    /* is able to handle the video */
    GST_DEBUG ("TODO: Add agnostic bin");
  }
}

static GstPad *
kms_automuxer_bin_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps)
{
  GstPad *pad, *target;
  KmsAutoMuxerBin *automuxerbin = KMS_AUTOMUXER_BIN (element);
  GstElement *typefind;
  gchar *padname;
  PadType ptype;

  ptype = get_pad_type_from_template (element, templ);
  if (ptype == INVALID_PAD)
    return NULL;

  typefind = gst_element_factory_make ("typefind", NULL);

  gst_bin_add (GST_BIN (automuxerbin), typefind);
  gst_element_sync_state_with_parent (typefind);

  target = gst_element_get_static_pad (typefind, "sink");

  KMS_AUTOMUXER_BIN_LOCK (element);

  padname = get_pad_name (automuxerbin, ptype);

  KMS_AUTOMUXER_BIN_UNLOCK (element);

  pad = gst_ghost_pad_new_from_template (padname, target, templ);

  g_free (padname);
  g_object_unref (target);

  if (GST_STATE (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, TRUE);

  if (!gst_element_add_pad (element, pad)) {
    g_object_unref (pad);
    return NULL;
  }

  g_signal_connect (typefind, "have-type", G_CALLBACK (found_type_cb),
      automuxerbin);

  return pad;
}

static void
kms_automuxer_bin_release_pad (GstElement * element, GstPad * pad)
{

  GstElement *typefind;
  GstPad *target;

  target = gst_ghost_pad_get_target (GST_GHOST_PAD (pad));

  if (GST_STATE (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, FALSE);

  if (target != NULL) {
    typefind = gst_pad_get_parent_element (target);
    g_object_unref (target);

    if (typefind != NULL) {
      gst_element_set_state (typefind, GST_STATE_NULL);
      gst_bin_remove (GST_BIN (element), typefind);
      g_object_unref (typefind);
    }
  }

  gst_element_remove_pad (element, pad);
}

static void
kms_automuxer_bin_finalize (GObject * object)
{
  KmsAutoMuxerBin *automuxerbin = KMS_AUTOMUXER_BIN (object);

  g_rec_mutex_clear (&automuxerbin->priv->mutex);

  /* There is no need to release each valve here because they */
  /* are already been released in parent's bin dispose function  */
  /* which this object inherits from */

  if (automuxerbin->priv->valves) {
    g_slist_free (automuxerbin->priv->valves);
    automuxerbin->priv->valves = NULL;
  }

  if (automuxerbin->priv->muxers) {
    gst_plugin_feature_list_free (automuxerbin->priv->muxers);
    automuxerbin->priv->muxers = NULL;
  }

  g_slice_free (PadCount, automuxerbin->priv->pads);

  /* chain up */
  G_OBJECT_CLASS (kms_automuxer_bin_parent_class)->finalize (object);
}

/* initialize the automuxerbin's class */
static void
kms_automuxer_bin_class_init (KmsAutoMuxerBinClass * klass)
{
  GObjectClass *gobject_class;
  GstElementClass *gstelement_class;

  gobject_class = (GObjectClass *) klass;
  gstelement_class = (GstElementClass *) klass;

  gobject_class->finalize = kms_automuxer_bin_finalize;

  gst_element_class_set_details_simple (gstelement_class,
      "Automuxer",
      "Basic/Bin",
      "Kurento plugin automuxer",
      "Joaquin Mengual <kini.mengual@gmail.com>, "
      "Santiago Carot-Nemesio <sancane.kurento@gmail.com>");

  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&sink_factory_audio));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&sink_factory_video));

  gstelement_class->request_new_pad =
      GST_DEBUG_FUNCPTR (kms_automuxer_bin_request_new_pad);
  gstelement_class->release_pad =
      GST_DEBUG_FUNCPTR (kms_automuxer_bin_release_pad);

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsAutoMuxerBinPrivate));

  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, PLUGIN_NAME, 0, PLUGIN_NAME);
}

static void
kms_automuxer_bin_init (KmsAutoMuxerBin * self)
{
  self->priv = KMS_AUTOMUXER_BIN_GET_PRIVATE (self);

  g_rec_mutex_init (&self->priv->mutex);

  self->priv->pads = g_slice_new0 (PadCount);
  self->priv->valves = NULL;
  self->priv->muxer = NULL;
  self->priv->muxers =
      gst_element_factory_list_get_elements (GST_ELEMENT_FACTORY_TYPE_MUXER,
      GST_RANK_NONE);

  g_object_set (G_OBJECT (self), "async-handling", TRUE, NULL);
}

gboolean
kms_automuxer_bin_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_AUTOMUXER_BIN);
}
