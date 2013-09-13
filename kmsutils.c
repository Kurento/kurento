#include "kmsutils.h"

void
kms_utils_set_valve_drop (GstElement * valve, gboolean drop)
{
  GstPad *sink = gst_element_get_static_pad (valve, "sink");

  GST_PAD_STREAM_LOCK (sink);
  g_object_set (valve, "drop", drop, NULL);
  GST_PAD_STREAM_UNLOCK (sink);

  g_object_unref (sink);
}
