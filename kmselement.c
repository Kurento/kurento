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

#include "kmselement.h"
#include "kmsagnosticbin.h"
#include "kmsagnosticcaps.h"

#define PLUGIN_NAME "kmselement"

GST_DEBUG_CATEGORY_STATIC (kms_element_debug_category);
#define GST_CAT_DEFAULT kms_element_debug_category

G_DEFINE_TYPE_WITH_CODE (KmsElement, kms_element,
    GST_TYPE_BIN,
    GST_DEBUG_CATEGORY_INIT (kms_element_debug_category, PLUGIN_NAME,
        0, "debug category for element"));

#define AUDIO_AGNOSTICBIN "audio_agnosticbin"
#define VIDEO_AGNOSTICBIN "video_agnosticbin"

#define AUDIO_SINK_PAD "audio_sink"
#define VIDEO_SINK_PAD "video_sink"

#define KMS_ELEMENT_GET_PRIVATE(obj) \
  (G_TYPE_INSTANCE_GET_PRIVATE ((obj), KMS_TYPE_ELEMENT, KmsElementPrivate))

struct _KmsElementPrivate
{
  guint audio_pad_count;
  guint video_pad_count;

  GstElement *audio_valve;
  GstElement *video_valve;

  GstElement *audio_agnosticbin;
  GstElement *video_agnosticbin;
};

/* Signals and args */
enum
{
  LAST_SIGNAL
};

enum
{
  PROP_0
};

/* the capabilities of the inputs and outputs. */
static GstStaticPadTemplate audio_sink_factory =
GST_STATIC_PAD_TEMPLATE (AUDIO_SINK_PAD,
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (KMS_AGNOSTIC_AUDIO_CAPS)
    );

static GstStaticPadTemplate video_sink_factory =
GST_STATIC_PAD_TEMPLATE (VIDEO_SINK_PAD,
    GST_PAD_SINK,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (KMS_AGNOSTIC_VIDEO_CAPS)
    );

static GstStaticPadTemplate audio_src_factory =
GST_STATIC_PAD_TEMPLATE ("audio_src_%u",
    GST_PAD_SRC,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (KMS_AGNOSTIC_AUDIO_CAPS)
    );

static GstStaticPadTemplate video_src_factory =
GST_STATIC_PAD_TEMPLATE ("video_src_%u",
    GST_PAD_SRC,
    GST_PAD_REQUEST,
    GST_STATIC_CAPS (KMS_AGNOSTIC_VIDEO_CAPS)
    );

static void
kms_element_set_target_pads (KmsElement * element, const gchar * pad_prefix,
    GstElement * agnosticbin)
{
  gboolean done = FALSE;
  GstIterator *it;
  GValue val = G_VALUE_INIT;

  it = gst_element_iterate_src_pads (GST_ELEMENT (element));
  do {
    switch (gst_iterator_next (it, &val)) {
      case GST_ITERATOR_OK:
      {
        GstPad *srcpad;

        srcpad = g_value_get_object (&val);
        if ((gst_ghost_pad_get_target (GST_GHOST_PAD (srcpad)) == NULL) &&
            g_str_has_prefix (GST_OBJECT_NAME (srcpad), pad_prefix)) {
          GstPad *target;

          target = gst_element_get_request_pad (agnosticbin, "src_%u");
          gst_ghost_pad_set_target (GST_GHOST_PAD (srcpad), target);
          g_object_unref (target);
        }

        g_value_reset (&val);
        break;
      }
      case GST_ITERATOR_RESYNC:
        gst_iterator_resync (it);
        break;
      case GST_ITERATOR_ERROR:
        GST_ERROR ("Error iterating over %s's src pads",
            GST_ELEMENT_NAME (element));
      case GST_ITERATOR_DONE:
        g_value_unset (&val);
        done = TRUE;
        break;
    }
  } while (!done);

  gst_iterator_free (it);
}

GstElement *
kms_element_get_audio_agnosticbin (KmsElement * self)
{
  GST_DEBUG ("Audio agnostic requested");
  KMS_ELEMENT_LOCK (self);
  if (self->priv->audio_agnosticbin != NULL)
    goto end;

  self->priv->audio_agnosticbin =
      gst_element_factory_make ("agnosticbin2", AUDIO_AGNOSTICBIN);
  gst_bin_add (GST_BIN (self), self->priv->audio_agnosticbin);
  gst_element_sync_state_with_parent (self->priv->audio_agnosticbin);
  kms_element_set_target_pads (self, "audio", self->priv->audio_agnosticbin);

end:
  KMS_ELEMENT_UNLOCK (self);
  return self->priv->audio_agnosticbin;
}

GstElement *
kms_element_get_video_agnosticbin (KmsElement * self)
{
  GST_DEBUG ("Video agnostic requested");
  KMS_ELEMENT_LOCK (self);
  if (self->priv->video_agnosticbin != NULL)
    goto end;

  self->priv->video_agnosticbin =
      gst_element_factory_make ("agnosticbin2", VIDEO_AGNOSTICBIN);
  gst_bin_add (GST_BIN (self), self->priv->video_agnosticbin);
  gst_element_sync_state_with_parent (self->priv->video_agnosticbin);
  kms_element_set_target_pads (self, "video", self->priv->video_agnosticbin);

end:
  KMS_ELEMENT_UNLOCK (self);
  return self->priv->video_agnosticbin;
}

GstElement *
kms_element_get_audio_valve (KmsElement * self)
{
  return self->priv->audio_valve;
}

GstElement *
kms_element_get_video_valve (KmsElement * self)
{
  return self->priv->video_valve;
}

static GstPad *
kms_element_generate_sink_pad (KmsElement * element, const gchar * name,
    GstElement ** target, GstPadTemplate * templ)
{
  GstElement *valve = *target;
  GstPad *sink, *ret_pad;

  if (valve != NULL)
    return NULL;

  valve = gst_element_factory_make ("valve", NULL);
  *target = valve;
  g_object_set (valve, "drop", TRUE, NULL);
  gst_bin_add (GST_BIN (element), valve);
  gst_element_sync_state_with_parent (valve);

  if (target == &element->priv->audio_valve) {
    KMS_ELEMENT_GET_CLASS (element)->audio_valve_added (element, valve);
  } else {
    KMS_ELEMENT_GET_CLASS (element)->video_valve_added (element, valve);
  }

  sink = gst_element_get_static_pad (valve, "sink");
  ret_pad = gst_ghost_pad_new_from_template (name, sink, templ);
  g_object_unref (sink);

  return ret_pad;
}

static GstPad *
kms_element_generate_src_pad (KmsElement * element, const gchar * name,
    GstElement * agnosticbin, GstPadTemplate * templ)
{
  GstPad *agnostic_pad;
  GstPad *ret_pad;

  if (agnosticbin != NULL) {
    agnostic_pad = gst_element_get_request_pad (agnosticbin, "src_%u");
    ret_pad = gst_ghost_pad_new_from_template (name, agnostic_pad, templ);
    g_object_unref (agnostic_pad);
  } else {
    ret_pad = gst_ghost_pad_new_no_target_from_template (name, templ);
  }

  return ret_pad;
}

static GstPad *
kms_element_request_new_pad (GstElement * element,
    GstPadTemplate * templ, const gchar * name, const GstCaps * caps)
{
  GstPad *ret_pad = NULL;
  gchar *pad_name;
  gboolean added;

  KMS_ELEMENT_LOCK (element);
  if (templ ==
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS (G_OBJECT_GET_CLASS
              (element)), "audio_src_%u")) {
    pad_name = g_strdup_printf ("audio_src_%d",
        KMS_ELEMENT (element)->priv->audio_pad_count++);

    ret_pad = kms_element_generate_src_pad (KMS_ELEMENT (element), pad_name,
        KMS_ELEMENT (element)->priv->audio_agnosticbin, templ);
    g_free (pad_name);

  } else if (templ ==
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS (G_OBJECT_GET_CLASS
              (element)), "video_src_%u")) {
    pad_name = g_strdup_printf ("video_src_%d",
        KMS_ELEMENT (element)->priv->video_pad_count++);

    ret_pad = kms_element_generate_src_pad (KMS_ELEMENT (element), pad_name,
        KMS_ELEMENT (element)->priv->video_agnosticbin, templ);
    g_free (pad_name);

  } else if (templ ==
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS (G_OBJECT_GET_CLASS
              (element)), AUDIO_SINK_PAD)) {
    ret_pad =
        kms_element_generate_sink_pad (KMS_ELEMENT (element), AUDIO_SINK_PAD,
        &KMS_ELEMENT (element)->priv->audio_valve, templ);
  } else if (templ ==
      gst_element_class_get_pad_template (GST_ELEMENT_CLASS (G_OBJECT_GET_CLASS
              (element)), VIDEO_SINK_PAD)) {
    ret_pad =
        kms_element_generate_sink_pad (KMS_ELEMENT (element), VIDEO_SINK_PAD,
        &KMS_ELEMENT (element)->priv->video_valve, templ);
  }

  if (ret_pad == NULL) {
    KMS_ELEMENT_UNLOCK (element);
    return NULL;
  }

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED
      || GST_STATE_TARGET (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (ret_pad, TRUE);

  added = gst_element_add_pad (element, ret_pad);
  KMS_ELEMENT_UNLOCK (element);

  if (added)
    return ret_pad;

  if (gst_pad_get_direction (ret_pad) == GST_PAD_SRC) {
    GstPad *target = gst_ghost_pad_get_target (GST_GHOST_PAD (ret_pad));

    if (target != NULL) {
      GstElement *agnostic = gst_pad_get_parent_element (target);

      gst_element_release_request_pad (agnostic, target);
      g_object_unref (target);
      g_object_unref (agnostic);
    }
  }

  g_object_unref (ret_pad);
  return NULL;
}

static void
kms_element_release_pad (GstElement * element, GstPad * pad)
{
  GstElement *agnosticbin;
  GstPad *target;

  if (g_str_has_prefix (GST_OBJECT_NAME (pad), "audio_src")) {
    agnosticbin = KMS_ELEMENT (element)->priv->audio_agnosticbin;
  } else if (g_str_has_prefix (GST_OBJECT_NAME (pad), "video_src")) {
    agnosticbin = KMS_ELEMENT (element)->priv->video_agnosticbin;
  } else {
    return;
  }

  // TODO: Remove pad if is a sinkpad

  target = gst_ghost_pad_get_target (GST_GHOST_PAD (pad));

  if (target != NULL && agnosticbin != NULL)
    gst_element_release_request_pad (agnosticbin, target);

  if (GST_STATE (element) >= GST_STATE_PAUSED
      || GST_STATE_PENDING (element) >= GST_STATE_PAUSED)
    gst_pad_set_active (pad, FALSE);

  gst_element_remove_pad (element, pad);
}

static void
kms_element_finalize (GObject * object)
{
  KmsElement *element = KMS_ELEMENT (object);

  /* free resources allocated by this object */
  g_rec_mutex_clear (&element->mutex);

  /* chain up */
  G_OBJECT_CLASS (kms_element_parent_class)->finalize (object);
}

static void
kms_element_audio_valve_added_default (KmsElement * self, GstElement * valve)
{
  GST_WARNING
      ("Element class %" GST_PTR_FORMAT
      " does not implement method \"audio_valve_added\"", self);
}

static void
kms_element_video_valve_added_default (KmsElement * self, GstElement * valve)
{
  GST_WARNING
      ("Element class %" GST_PTR_FORMAT
      " does not implement method \"video_valve_added\"", self);
}

static void
kms_element_audio_valve_removed_default (KmsElement * self, GstElement * valve)
{
  GST_WARNING
      ("Element class %" GST_PTR_FORMAT
      " does not implement method \"audio_valve_removed\"", self);
}

static void
kms_element_video_valve_removed_default (KmsElement * self, GstElement * valve)
{
  GST_WARNING
      ("Element class %" GST_PTR_FORMAT
      " does not implement method \"video_valve_removed\"", self);
}

static void
kms_element_class_init (KmsElementClass * klass)
{
  GstElementClass *gstelement_class;
  GObjectClass *gobject_class;

  gobject_class = G_OBJECT_CLASS (klass);
  gobject_class->finalize = kms_element_finalize;

  gstelement_class = GST_ELEMENT_CLASS (klass);
  gst_element_class_set_details_simple (gstelement_class,
      "KmsElement",
      "Base/Bin/KmsElement",
      "Base class for elements",
      "José Antonio Santos Cadenas <santoscadenas@kurento.com>");
  gstelement_class->request_new_pad =
      GST_DEBUG_FUNCPTR (kms_element_request_new_pad);
  gstelement_class->release_pad = GST_DEBUG_FUNCPTR (kms_element_release_pad);
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&video_src_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&audio_sink_factory));
  gst_element_class_add_pad_template (gstelement_class,
      gst_static_pad_template_get (&video_sink_factory));

  klass->audio_valve_added =
      GST_DEBUG_FUNCPTR (kms_element_audio_valve_added_default);
  klass->video_valve_added =
      GST_DEBUG_FUNCPTR (kms_element_video_valve_added_default);
  klass->audio_valve_removed =
      GST_DEBUG_FUNCPTR (kms_element_audio_valve_removed_default);
  klass->video_valve_removed =
      GST_DEBUG_FUNCPTR (kms_element_video_valve_removed_default);

  g_type_class_add_private (klass, sizeof (KmsElementPrivate));
}

static void
kms_element_init (KmsElement * element)
{
  g_rec_mutex_init (&element->mutex);

  element->priv = KMS_ELEMENT_GET_PRIVATE (element);

  g_object_set (G_OBJECT (element), "async-handling", TRUE, NULL);

  element->priv->audio_pad_count = 0;
  element->priv->video_pad_count = 0;
  element->priv->audio_agnosticbin = NULL;
  element->priv->video_agnosticbin = NULL;

  element->priv->audio_valve = NULL;
  element->priv->video_valve = NULL;
}
