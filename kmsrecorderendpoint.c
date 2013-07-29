#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <gst/gst.h>
#include "kmselement.h"
#include "kmsrecorderendpoint.h"

#define PLUGIN_NAME "kmsrecorderendpoint"

GST_DEBUG_CATEGORY_STATIC (kms_recorder_end_point_debug_category);
#define GST_CAT_DEFAULT kms_recorder_end_point_debug_category

/* prototypes */

static void kms_recorder_end_point_dispose (GObject * object);
static void kms_recorder_end_point_finalize (GObject * object);

/* pad templates */

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsRecorderEndPoint, kms_recorder_end_point,
    KMS_TYPE_ELEMENT,
    GST_DEBUG_CATEGORY_INIT (kms_recorder_end_point_debug_category, PLUGIN_NAME,
        0, "debug category for recorderendpoint element"));

static void
kms_recorder_end_point_class_init (KmsRecorderEndPointClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "RecorderEndPoint", "Sink/Generic", "Kurento plugin recorder end point",
      "Santiago Carot-Nemesio <sancane.kurento@gmail.com>");

  gobject_class->dispose = kms_recorder_end_point_dispose;
  gobject_class->finalize = kms_recorder_end_point_finalize;

}

static void
kms_recorder_end_point_init (KmsRecorderEndPoint * recorderendpoint)
{
}

void
kms_recorder_end_point_dispose (GObject * object)
{
  KmsRecorderEndPoint *recorderendpoint = KMS_RECORDER_END_POINT (object);

  GST_DEBUG_OBJECT (recorderendpoint, "dispose");

  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_recorder_end_point_parent_class)->dispose (object);
}

void
kms_recorder_end_point_finalize (GObject * object)
{
  KmsRecorderEndPoint *recorderendpoint = KMS_RECORDER_END_POINT (object);

  GST_DEBUG_OBJECT (recorderendpoint, "finalize");

  /* clean up object here */

  G_OBJECT_CLASS (kms_recorder_end_point_parent_class)->finalize (object);
}

gboolean
kms_recorder_end_point_plugin_init (GstPlugin * plugin)
{

  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_RECORDER_END_POINT);
}
