/*
 * (C) Copyright 2023 Kurento (https://kurento.openvidu.io/)
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
 */
#define _XOPEN_SOURCE 500

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "kmsvideosampler.h"

#include <math.h>

#include <gst/gst.h>
#include <gst/gstbin.h>

#define MAX_FRAMES_STORED "50"
#define VIDEO_LEG_BIN " capsfilter name=\"input-caps\" caps=\"video/x-raw\" ! agnosticbin ! capsfilter name=\"video\" caps=\"%s\" ! tee name=t ! queue leaky=1 max-size-buffers=2  t. ! queue leaky=1 max-size-buffers="MAX_FRAMES_STORED" ! agnosticbin ! capsfilter name=\"encoder\" caps=\"%s\" ! appsink async=false sync=false emit-signals=true name=\"sink\""

#define PLUGIN_NAME "videosampler"

GST_DEBUG_CATEGORY_STATIC (kms_videosampler_debug_category);
#define GST_CAT_DEFAULT kms_videosampler_debug_category


#define KMS_VIDEOSAMPLER_GET_PRIVATE(obj) \
  (G_TYPE_INSTANCE_GET_PRIVATE ((obj), KMS_TYPE_VIDEOSAMPLER, KmsVideoSamplerPrivate))

struct _KmsVideoSamplerPrivate {
  gulong frame_period;
  gulong height;
  gulong width;
  gchar* encoding;
  gboolean finished;

  gulong appsink_signal;

  GstElement *video_leg;
};

/* Object properties */
enum
{
  PROP_0,
  PROP_FRAME_PERIOD,
  PROP_HEIGHT,
  PROP_WIDTH,
  PROP_IMAGE_ENCODING,
  N_PROPERTIES
};

static GParamSpec *obj_properties[N_PROPERTIES] = { NULL, };


enum {
    FRAME_SAMPLE,
    N_SIGNALS
};

static guint my_class_signals[N_SIGNALS];

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsVideoSampler,
    kms_videosampler,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_videosampler_debug_category,
        PLUGIN_NAME,
        0,
        "debug category for video sampler element"));


static void
kms_videosampler_connect_passthrough (KmsVideoSampler * self,
    KmsElementPadType type, GstElement * agnosticbin)
{
  GstPad *target = gst_element_get_static_pad (agnosticbin, "sink");

  kms_element_connect_sink_target (KMS_ELEMENT (self), target, type);
  gst_object_unref (target);
}

gboolean
send_buffer (GstBuffer ** buffer, guint idx, gpointer data)
{
  KmsVideoSampler *self = KMS_VIDEOSAMPLER (data);
  GstMapInfo map;

  if (gst_buffer_map(*buffer, &map, GST_MAP_READ)) {
    GByteArray *byte_array = g_byte_array_new();

    g_byte_array_append (byte_array, map.data, map.size);
    g_signal_emit(self, my_class_signals[FRAME_SAMPLE], 0, byte_array);
    g_byte_array_unref(byte_array);
    gst_buffer_unmap(*buffer, &map);
  }
  return TRUE;
}

static GstFlowReturn
kms_videosampler_handle_sample (GstElement * appsink, gpointer data)
{
  GstSample *sample;
  KmsVideoSampler *self = KMS_VIDEOSAMPLER (data);

  KMS_VIDEOSAMPLER_LOCK(self);
  if (self->priv->finished) {
    KMS_VIDEOSAMPLER_UNLOCK(self);
    return GST_FLOW_OK;
  }
  g_signal_emit_by_name (appsink, "pull-sample", &sample);
  if (sample != NULL) {
    GstBuffer *buff = gst_sample_get_buffer (sample);

    if (buff != NULL) {
      send_buffer (&buff, 0, self);
    } else {
      GstBufferList *buffer_list = gst_sample_get_buffer_list  (sample);

      if (buffer_list != NULL) {
        gst_buffer_list_foreach (buffer_list, send_buffer, self);
      }
    }
    gst_sample_unref (sample);
  }
  KMS_VIDEOSAMPLER_UNLOCK(self);

  return GST_FLOW_OK;
}

static gulong
kms_videosampler_connect_appsink (KmsVideoSampler *self)
{
  GstElement *appsink;
  gulong signal_id;

  if (self->priv->video_leg == NULL) {
    return 0;
  }

  appsink = gst_bin_get_by_name (GST_BIN(self->priv->video_leg), "sink");
  signal_id = g_signal_connect (G_OBJECT (appsink), "new-sample",
      G_CALLBACK (kms_videosampler_handle_sample), self);

  return signal_id;
}

static gchar*
calculate_framerate (gulong frame_period)
{
  if (frame_period > 0) {
    gchar* fr_str;
    gfloat period = frame_period;
    gfloat freq = 1000.0 / period; // frame_period is measured in miliseconds
    guint freq_uint = roundf (freq * 100.0);

    // Ensure no frequency less than 0 is allowed
    if (freq_uint <= 0) {
      freq_uint = 0;
    }
    fr_str = g_strdup_printf ("%d/100", freq_uint);
    return fr_str;
  } else {
    return NULL;
  }
}

static gchar* 
calculate_video_caps_for_output (gint width, gint height, gulong frame_period)
{
  gchar *framerate_str = NULL;
  GString *caps;
  gchar *caps_str;
  
  framerate_str = calculate_framerate (frame_period);
  caps = g_string_new ("video/x-raw");
  if (framerate_str != NULL) {
    g_string_append (caps, ",framerate=");
    g_string_append (caps, framerate_str);
    g_free (framerate_str);
  }
  if (height > 0) {
    gchar *number = g_strdup_printf ("%d", height);
    g_string_append (caps, ",height=");
    g_string_append (caps, number);
    g_free (number);
  }
  if (width > 0) {
    gchar *number = g_strdup_printf ("%d", width);
    g_string_append (caps, ",width=");
    g_string_append (caps, number);
    g_free (number);
  }
  caps_str = g_strdup_printf ("%s", caps->str);
  g_string_free (caps, TRUE);
  return caps_str;
}

static gchar*
calculate_encoder (gchar *encoding)
{
  gchar* encoder;

  if (encoding == NULL) {
    encoder = g_strdup_printf("%s", "video/x-raw");
  }else if (g_str_equal (encoding, "PNG")){
    encoder = g_strdup_printf("%s", "image/png");
  } else if (g_str_equal (encoding, "JPEG")){
    encoder = g_strdup_printf("%s", "image/jpeg");
  } else if (g_str_equal (encoding, "BMP")){
    encoder = g_strdup_printf("%s", "image/bmp");
  } else if (g_str_equal (encoding, "PBM")){
    encoder = g_strdup_printf("%s", "image/pbm");
  } else if (g_str_equal (encoding, "PPM")){
    encoder = g_strdup_printf("%s", "image/ppm");
  } else {
    encoder = g_strdup_printf("%s", "video/x-raw");
  }

  return encoder;
}



static gchar*
kms_videosampler_create_description (KmsVideoSampler *self)
{
  gchar* video_caps = calculate_video_caps_for_output (self->priv->width, self->priv->height, self->priv->frame_period);
  gchar* encoder_class = calculate_encoder (self->priv->encoding);
  gchar* description;

  description = g_strdup_printf  (VIDEO_LEG_BIN, video_caps, encoder_class);
  g_free (video_caps);
  g_free (encoder_class);
  return description;
}

static void
kms_videosampler_update_video_caps_for_output (KmsVideoSampler *self, gint width, gint height, gulong frame_period)
{
  gchar* video_caps = calculate_video_caps_for_output (width, height, frame_period);
  GstElement *capsfilter = gst_bin_get_by_name (GST_BIN(self->priv->video_leg), "video");
  GstCaps *caps = gst_caps_from_string(video_caps);

  g_object_set (capsfilter, "caps", caps, NULL);
  gst_caps_unref (caps);
  gst_object_unref (capsfilter);
  g_free (video_caps);
}


static void 
kms_videosampler_set_output_resolution_preserving_aspect_ratio (KmsVideoSampler *self, gint orig_width, gint orig_height)
{
  if (self->priv->height > 0) {
    // We need to calculate width for aspect ratio preservation
    gint calculated_width;

    calculated_width = orig_width * self->priv->height / orig_height;
    kms_videosampler_update_video_caps_for_output(self, calculated_width, self->priv->height, self->priv->frame_period);
  } else if (self->priv->width > 0) {
    // We need to calculate width for aspect ratio preservation
    gint calculated_height;

    calculated_height = orig_height * self->priv->width / orig_width;
    kms_videosampler_update_video_caps_for_output(self, self->priv->width, calculated_height, self->priv->frame_period);
  }
}

static gchar*
calculate_video_caps_for_input (gint width, gint height)
{
  GString *caps;
  gchar *caps_str;
  
  caps = g_string_new ("video/x-raw");
  if (height > 0) {
    gchar *number = g_strdup_printf ("%d", height);
    g_string_append (caps, ",height=");
    g_string_append (caps, number);
    g_free (number);
  }
  if (width > 0) {
    gchar *number = g_strdup_printf ("%d", width);
    g_string_append (caps, ",width=");
    g_string_append (caps, number);
    g_free (number);
  }
  caps_str = g_strdup_printf ("%s", caps->str);
  g_string_free (caps, TRUE);
  return caps_str;
}

static void 
kms_videosampler_set_input_resolution (KmsVideoSampler *self, gint orig_width, gint orig_height)
{
  if (self->priv->video_leg != NULL) {
    GstElement *input_caps;

    input_caps = gst_bin_get_by_name (GST_BIN(self->priv->video_leg), "input-caps");
    if (input_caps != NULL) {
      gchar *caps_str = calculate_video_caps_for_input (orig_width, orig_height);
      GstCaps *caps = gst_caps_from_string(caps_str);

      g_object_set (input_caps, "caps", caps, NULL);
      gst_caps_unref (caps);
      g_free (caps_str);
      gst_object_unref (input_caps);
    }
  }
}

static GstPadProbeReturn
kms_videosampler_resolution_change (GstPad *pad, GstPadProbeInfo *info, gpointer user_data) 
{
  KmsVideoSampler *self = (KmsVideoSampler*) user_data;
  GstEvent *event;

  // If no aspect ratio preservation is needed no action taken
  if ((self->priv->height == 0) && (self->priv->width == 0)) {
    return GST_PAD_PROBE_OK;
  }

  event = gst_pad_probe_info_get_event (info);
  if (GST_EVENT_TYPE(event) == GST_EVENT_CAPS) {
      GstCaps *caps;
      GstStructure *structure;
      gint width, height;

      gst_event_parse_caps(event, &caps);
      structure = gst_caps_get_structure(caps, 0);

      if (gst_structure_get_int(structure, "width", &width) && gst_structure_get_int(structure, "height", &height)) {
        if (!((self->priv->height > 0) && (self->priv->width > 0))) {
          // We have definitive souorce resolution, set the output resolution
          kms_videosampler_set_output_resolution_preserving_aspect_ratio (self, width, height);
        }
        kms_videosampler_set_input_resolution (self, width, height);
      }
  }

  return GST_PAD_PROBE_OK;
}

static void
kms_videosampler_connect_video_leg (KmsVideoSampler *self)
{
  gchar *description = kms_videosampler_create_description (self);
  GError *error= NULL;
  GstElement *output;
  GstPad *sinkpad;

  output = kms_element_get_video_agnosticbin (KMS_ELEMENT (self));
  self->priv->video_leg = gst_parse_bin_from_description (description, TRUE, &error);
  if (error != NULL) {
    GST_WARNING_OBJECT (self, "Cannot create video leg, no video will be provided");
    kms_videosampler_connect_passthrough (self, KMS_ELEMENT_PAD_TYPE_VIDEO, output);    
    self->priv->video_leg = NULL;
  } else {
    gst_bin_add (GST_BIN (self), gst_object_ref(self->priv->video_leg));
    gst_element_sync_state_with_parent (self->priv->video_leg);
    gst_element_link (self->priv->video_leg, output);
    kms_videosampler_connect_passthrough (self, KMS_ELEMENT_PAD_TYPE_VIDEO,
      self->priv->video_leg);    

    // Add probe to dynamically calculate resolutions in case neede to respect aspect ratio
    sinkpad = gst_element_get_static_pad(self->priv->video_leg, "sink");
    gst_pad_add_probe(sinkpad, GST_PAD_PROBE_TYPE_EVENT_DOWNSTREAM, (GstPadProbeCallback)kms_videosampler_resolution_change, (gpointer) self, NULL);
    gst_object_unref (sinkpad);
  }
  g_free (description);
}

static void
kms_videosampler_init (KmsVideoSampler *self)
{
  self->priv = KMS_VIDEOSAMPLER_GET_PRIVATE (self);

  self->priv->frame_period = 0;
  self->priv->height = 0;
  self->priv->width = 0;
  self->priv->encoding = NULL;
  self->priv->video_leg = NULL;
  self->priv->finished = FALSE;

  g_mutex_init (&self->mutex);

  kms_videosampler_connect_passthrough (self, KMS_ELEMENT_PAD_TYPE_AUDIO,
      kms_element_get_audio_agnosticbin (KMS_ELEMENT (self)));

  kms_videosampler_connect_video_leg (self);
  self->priv->appsink_signal = kms_videosampler_connect_appsink (self);
}

static void
kms_videosampler_update_frame_period (KmsVideoSampler *self)
{
  kms_videosampler_update_video_caps_for_output(self, self->priv->width, self->priv->height, self->priv->frame_period);
}

static void
kms_videosampler_update_height (KmsVideoSampler *self)
{
  kms_videosampler_update_video_caps_for_output(self, self->priv->width, self->priv->height, self->priv->frame_period);
}

static void
kms_videosampler_update_width (KmsVideoSampler *self)
{
  kms_videosampler_update_video_caps_for_output(self, self->priv->width, self->priv->height, self->priv->frame_period);
}

static void
kms_videosampler_update_image_encoding (KmsVideoSampler *self)
{
  gchar* encoder_class = calculate_encoder (self->priv->encoding);
  GstElement *capsfilter = gst_bin_get_by_name (GST_BIN(self->priv->video_leg), "encoder");
  GstCaps *caps = gst_caps_from_string(encoder_class);

  g_object_set (capsfilter, "caps", caps, NULL);
  gst_caps_unref (caps);
  gst_object_unref (capsfilter);
  g_free (encoder_class);
}


static void
kms_videosampler_set_property (GObject *object,
    guint property_id,
    const GValue *value,
    GParamSpec *pspec)
{
  KmsVideoSampler *self = KMS_VIDEOSAMPLER (object);
  gint v;

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_FRAME_PERIOD:
      v = g_value_get_int (value);
      if (self->priv->frame_period != v) {
        self->priv->frame_period = v;
        kms_videosampler_update_frame_period (self);
      }
      break;
    case PROP_HEIGHT:
      v = g_value_get_int (value);
      if (self->priv->height != v) {
        self->priv->height = v;
        kms_videosampler_update_height (self);
      }
      break;
    case PROP_WIDTH:
      v = g_value_get_int (value);
      if (self->priv->width != v) {
        self->priv->width = v;
        kms_videosampler_update_width (self);
      }
      break;
    case PROP_IMAGE_ENCODING:
      {
        gchar *enc;

        enc = g_value_dup_string (value);
        if (enc != NULL) {
          if ((self->priv->encoding == NULL) || (!g_str_equal(enc,  self->priv->encoding))) {
            self->priv->encoding = enc;
            kms_videosampler_update_image_encoding (self);
          } else {
            g_free(enc);
          }
        }
      }
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
  }
  KMS_ELEMENT_UNLOCK(KMS_ELEMENT (self));
}

static void
kms_videosampler_get_property (GObject *object,
    guint property_id,
    GValue *value,
    GParamSpec *pspec)
{
  KmsVideoSampler *self = KMS_VIDEOSAMPLER (object);

  KMS_ELEMENT_LOCK (KMS_ELEMENT (self));
  switch (property_id) {
    case PROP_FRAME_PERIOD:
      g_value_set_int (value, self->priv->frame_period);
      break;
    case PROP_HEIGHT:
      g_value_set_int (value, self->priv->height);
      break;
    case PROP_WIDTH:
      g_value_set_int (value, self->priv->width);
      break;
    case PROP_IMAGE_ENCODING:
      g_value_set_string (value, self->priv->encoding);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
  }
  KMS_ELEMENT_UNLOCK (KMS_ELEMENT (self));
}


void
kms_videosampler_finalize (GObject * object)
{
  KmsVideoSampler *self = KMS_VIDEOSAMPLER (object);
  GstElement *appsink;



  GST_DEBUG_OBJECT (self, "finalize");

  /* clean up object here */
  KMS_VIDEOSAMPLER_LOCK (self);
  self->priv->finished = TRUE;
  if (self->priv->video_leg != NULL) {
    if (self->priv->appsink_signal != 0) {
      appsink = gst_bin_get_by_name (GST_BIN(self->priv->video_leg), "sink");
      g_signal_handler_disconnect (appsink, self->priv->appsink_signal);
      gst_object_unref (appsink);
    }
  }
  KMS_VIDEOSAMPLER_UNLOCK (self);
  if (self->priv->encoding != NULL) {
    g_free (self->priv->encoding);
    self->priv->encoding = NULL;
  }
  if (self->priv->video_leg != NULL) {
    gst_object_unref (self->priv->video_leg);
    self->priv->video_leg = NULL;
  }

  G_OBJECT_CLASS (kms_videosampler_parent_class)->finalize (object);
}

static void
kms_videosampler_class_init (KmsVideoSamplerClass *klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "video sampler element", "Video/Filter",
      "Retrieves frames from video stream with a certaing period",
      "Saúl Labajo <slabajo@naevatec.com>");

  gobject_class->set_property = kms_videosampler_set_property;
  gobject_class->get_property = kms_videosampler_get_property;
  gobject_class->finalize = kms_videosampler_finalize;

  obj_properties[PROP_FRAME_PERIOD] =  g_param_spec_int ("frame-period", "frame-period",
      "Period in millilseconds between consecutive frames", 0, G_MAXINT, 0, G_PARAM_READWRITE);
  obj_properties[PROP_HEIGHT] =  g_param_spec_int ("height", "height",
      "Height of generated images", 0, G_MAXINT, 0, G_PARAM_READWRITE);
  obj_properties[PROP_WIDTH] =  g_param_spec_int ("width", "width",
      "WIdth of generated images", 0, G_MAXINT, 0, G_PARAM_READWRITE);
  obj_properties[PROP_IMAGE_ENCODING] =  g_param_spec_string ("image-encoding", "image-encoding",
      "Encoding used on generated images", NULL,
          G_PARAM_READWRITE | GST_PARAM_MUTABLE_READY);

  g_object_class_install_properties (gobject_class,
      N_PROPERTIES, obj_properties);

    my_class_signals[FRAME_SAMPLE] = g_signal_new(
        "frame-sample",
        G_TYPE_FROM_CLASS(klass),
        G_SIGNAL_RUN_LAST,
        0,
        NULL,
        NULL,
        g_cclosure_marshal_VOID__BOXED,
        G_TYPE_NONE,
        1,
        G_TYPE_BYTE_ARRAY
    );

  g_type_class_add_private (klass, sizeof (KmsVideoSamplerPrivate));
}

gboolean
kms_videosampler_plugin_init (GstPlugin *plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_VIDEOSAMPLER);
}
